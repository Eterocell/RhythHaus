package com.eterocell.rhythhaus

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMillis: Long?,
    val source: AudioSource,
    val artworkBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean = other is PlayableTrack &&
        id == other.id &&
        title == other.title &&
        artist == other.artist &&
        album == other.album &&
        durationMillis == other.durationMillis &&
        source == other.source &&
        artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + source.hashCode()
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

sealed interface AudioSource {
    val stableKey: String

    data class FilePath(val path: String) : AudioSource {
        override val stableKey: String = path
    }

    data class Uri(val value: String) : AudioSource {
        override val stableKey: String = value
    }

    data class FileDescriptor(
        val fd: Int,
        val displayName: String,
        override val stableKey: String = displayName,
    ) : AudioSource
}

enum class PlaybackStatus {
    Idle,
    Loading,
    Buffering,
    Playing,
    Paused,
    Stopped,
    Error,
}

enum class RepeatMode {
    RepeatOne,
    RepeatPlaylist,
    StopAfterCurrent,
    StopAfterQueue,
}

enum class ShuffleMode {
    Off,
    On,
}

data class PlaybackError(
    val message: String,
    val cause: String? = null,
)

data class PlaybackState(
    val currentTrack: PlayableTrack? = null,
    val queue: List<PlayableTrack> = emptyList(),
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMillis: Long = 0L,
    val durationMillis: Long? = null,
    val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
    val error: PlaybackError? = null,
) {
    val canPlay: Boolean = currentTrack != null && status != PlaybackStatus.Loading && status != PlaybackStatus.Buffering
    val isPlaying: Boolean = status == PlaybackStatus.Playing
    val progressFraction: Float
        get() {
            val duration = durationMillis ?: return 0f
            if (duration <= 0L) return 0f
            return (positionMillis.coerceIn(0L, duration).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
}

interface PlaybackEngineListener {
    fun onPlaybackStatus(status: PlaybackStatus)
    fun onPlaybackProgress(positionMillis: Long, durationMillis: Long?)
    fun onPlaybackCompleted()
    fun onPlaybackError(error: PlaybackError)
    fun onSkipToNext()
    fun onSkipToPrevious()
}

interface PlatformPlaybackEngine {
    var listener: PlaybackEngineListener?

    fun load(track: PlayableTrack)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMillis: Long)
    fun release()
}

expect fun createPlatformPlaybackEngine(): PlatformPlaybackEngine

internal expect val playbackEngineDispatcher: CoroutineDispatcher

class PlaybackController(
    private val engine: PlatformPlaybackEngine = createPlatformPlaybackEngine(),
    private val shuffleOrderFactory: (List<String>, String?) -> List<String> = ::defaultShuffleOrder,
) : PlaybackEngineListener {
    private val scope = CoroutineScope(SupervisorJob() + playbackEngineDispatcher)
    private val engineMutex = Mutex()
    private var loadJob: Job? = null
    private var playWhenLoaded: Boolean = false
    private var shuffledOrder: List<String> = emptyList()
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    init {
        engine.listener = this
    }

    fun setQueue(tracks: List<PlayableTrack>, selectedTrackId: String? = tracks.firstOrNull()?.id) {
        val selected = tracks.firstOrNull { it.id == selectedTrackId } ?: tracks.firstOrNull()
        if (selected == null) {
            loadJob?.cancel()
            playWhenLoaded = false
            shuffledOrder = emptyList()
            _state.value = PlaybackState(
                queue = tracks,
                repeatMode = _state.value.repeatMode,
                shuffleMode = _state.value.shuffleMode,
            )
        } else {
            _state.value = PlaybackState(
                currentTrack = selected,
                queue = tracks,
                status = PlaybackStatus.Loading,
                durationMillis = selected.durationMillis,
                repeatMode = _state.value.repeatMode,
                shuffleMode = _state.value.shuffleMode,
            )
            if (_state.value.shuffleMode == ShuffleMode.On) {
                regenerateShuffleOrder(selected.id)
            } else {
                shuffledOrder = emptyList()
            }
            loadSelected(selected, autoPlay = false)
        }
    }

    fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        val track = _state.value.queue.firstOrNull { it.id == trackId } ?: return
        loadSelected(track, autoPlay)
    }

    fun setRepeatMode(mode: RepeatMode) {
        val previous = _state.value.repeatMode
        if (previous == mode) return
        _state.value = _state.value.copy(repeatMode = mode)
        log.d { "RepeatMode changed: $previous -> $mode" }
    }

    fun cycleRepeatMode() {
        val previous = _state.value.repeatMode
        val next = when (previous) {
            RepeatMode.StopAfterQueue -> RepeatMode.RepeatPlaylist
            RepeatMode.RepeatPlaylist -> RepeatMode.RepeatOne
            RepeatMode.RepeatOne -> RepeatMode.StopAfterCurrent
            RepeatMode.StopAfterCurrent -> RepeatMode.StopAfterQueue
        }
        log.d { "Cycle repeat mode: $previous -> $next" }
        setRepeatMode(next)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        val previous = _state.value.shuffleMode
        if (previous == mode) return
        _state.value = _state.value.copy(shuffleMode = mode)
        log.d { "ShuffleMode changed: $previous -> $mode" }
        when (mode) {
            ShuffleMode.On -> {
                regenerateShuffleOrder()
                log.d { "Shuffle enabled, effective order: $shuffledOrder" }
            }
            ShuffleMode.Off -> {
                shuffledOrder = emptyList()
                log.d { "Shuffle disabled, restoring original queue order" }
            }
        }
    }

    fun toggleShuffleMode() {
        val previous = _state.value.shuffleMode
        val next = when (previous) {
            ShuffleMode.Off -> ShuffleMode.On
            ShuffleMode.On -> ShuffleMode.Off
        }
        log.d { "Toggle shuffle: $previous -> $next" }
        setShuffleMode(next)
    }

    fun play() {
        val current = _state.value.currentTrack ?: return
        if (_state.value.status == PlaybackStatus.Loading) {
            playWhenLoaded = true
            return
        }
        if (_state.value.status == PlaybackStatus.Idle || _state.value.status == PlaybackStatus.Error) {
            loadSelected(current, autoPlay = true)
            return
        }
        launchEngineAction { engine.play() }
    }

    fun pause() {
        playWhenLoaded = false
        launchEngineAction { engine.pause() }
    }

    fun stop() {
        playWhenLoaded = false
        launchEngineAction { engine.stop() }
    }

    fun seekTo(positionMillis: Long) {
        val duration = _state.value.durationMillis
        val safePosition = if (duration == null) max(0L, positionMillis) else positionMillis.coerceIn(0L, duration)
        _state.value = _state.value.copy(positionMillis = safePosition, error = null)
        launchEngineAction { engine.seekTo(safePosition) }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun skipToNext() {
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        nextTrack(wrap)?.let { loadSelected(it, autoPlay = true) }
    }

    fun skipToPrevious() {
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        previousTrack(wrap)?.let { loadSelected(it, autoPlay = true) }
    }

    fun release() {
        scope.cancel()
        engine.listener = null
        engine.release()
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped)
    }

    private fun loadSelected(track: PlayableTrack, autoPlay: Boolean) {
        loadJob?.cancel()
        playWhenLoaded = autoPlay
        _state.value = _state.value.copy(
            currentTrack = track,
            status = PlaybackStatus.Loading,
            positionMillis = 0L,
            durationMillis = track.durationMillis,
            error = null,
        )
        loadJob = scope.launch {
            runEngineAction {
                engine.load(track)
                if (_state.value.currentTrack?.id == track.id && (autoPlay || playWhenLoaded)) {
                    playWhenLoaded = false
                    engine.play()
                }
            }
        }
    }

    private fun launchEngineAction(action: () -> Unit) {
        scope.launch {
            runEngineAction(action)
        }
    }

    private suspend fun runEngineAction(action: () -> Unit) {
        try {
            engineMutex.withLock {
                action()
            }
        } catch (throwable: Throwable) {
            log.e { throwable.stackTraceToString() }
            onPlaybackError(
                PlaybackError(
                    message = "Playback failed",
                    cause = throwable.message ?: throwable::class.simpleName,
                ),
            )
        }
    }

    private fun effectiveOrder(): List<String> = when (_state.value.shuffleMode) {
        ShuffleMode.Off -> _state.value.queue.map { it.id }
        ShuffleMode.On -> shuffledOrder.ifEmpty { _state.value.queue.map { it.id } }
    }

    private fun trackById(trackId: String?): PlayableTrack? = _state.value.queue.firstOrNull { it.id == trackId }

    private fun currentEffectiveIndex(order: List<String> = effectiveOrder()): Int =
        order.indexOf(_state.value.currentTrack?.id)

    private fun nextTrack(wrap: Boolean): PlayableTrack? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val nextId = order.getOrNull(currentIndex + 1) ?: if (wrap) order.firstOrNull() else null
        return trackById(nextId)
    }

    private fun previousTrack(wrap: Boolean): PlayableTrack? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val previousId = order.getOrNull(currentIndex - 1) ?: if (wrap) order.lastOrNull() else null
        return trackById(previousId)
    }

    private fun regenerateShuffleOrder(currentId: String? = _state.value.currentTrack?.id) {
        shuffledOrder = shuffleOrderFactory(_state.value.queue.map { it.id }, currentId)
            .filter { id -> _state.value.queue.any { it.id == id } }
            .distinct()
        val missing = _state.value.queue.map { it.id }.filterNot { it in shuffledOrder }
        shuffledOrder = shuffledOrder + missing
    }

    private fun stopAtCurrentTrackEnd() {
        val duration = _state.value.durationMillis
        _state.value = _state.value.copy(
            status = PlaybackStatus.Stopped,
            positionMillis = duration ?: max(0L, _state.value.positionMillis),
            error = null,
        )
    }

    override fun onPlaybackStatus(status: PlaybackStatus) {
        _state.value = _state.value.copy(status = status, error = null)
    }

    override fun onPlaybackProgress(positionMillis: Long, durationMillis: Long?) {
        _state.value = _state.value.copy(
            positionMillis = max(0L, positionMillis),
            durationMillis = durationMillis ?: _state.value.durationMillis,
        )
    }

    override fun onPlaybackCompleted() {
        when (_state.value.repeatMode) {
            RepeatMode.RepeatOne -> {
                val current = _state.value.currentTrack ?: return stopAtCurrentTrackEnd()
                loadSelected(current, autoPlay = true)
            }
            RepeatMode.RepeatPlaylist -> {
                val next = nextTrack(wrap = true)
                if (next != null) loadSelected(next, autoPlay = true) else stopAtCurrentTrackEnd()
            }
            RepeatMode.StopAfterCurrent -> stopAtCurrentTrackEnd()
            RepeatMode.StopAfterQueue -> {
                val next = nextTrack(wrap = false)
                if (next != null) loadSelected(next, autoPlay = true) else stopAtCurrentTrackEnd()
            }
        }
    }

    override fun onPlaybackError(error: PlaybackError) {
        _state.value = _state.value.copy(status = PlaybackStatus.Error, error = error)
    }

    override fun onSkipToNext() {
        skipToNext()
    }

    override fun onSkipToPrevious() {
        skipToPrevious()
    }
}

private fun defaultShuffleOrder(ids: List<String>, currentId: String?): List<String> {
    if (ids.size <= 1) return ids
    val shuffled = ids.shuffled()
    if (currentId == null || currentId !in shuffled) return shuffled
    return listOf(currentId) + shuffled.filterNot { it == currentId }
}

class FakePlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var loaded: PlayableTrack? = null
    private var positionMillis: Long = 0L
    private var durationMillis: Long? = null
    var released: Boolean = false
        private set

    override fun load(track: PlayableTrack) {
        loaded = track
        positionMillis = 0L
        durationMillis = track.durationMillis
        listener?.onPlaybackProgress(positionMillis, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        requireNotNull(loaded) { "No track loaded" }
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
    }

    override fun pause() {
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        positionMillis = 0L
        listener?.onPlaybackProgress(positionMillis, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        this.positionMillis = positionMillis
        listener?.onPlaybackProgress(positionMillis, durationMillis)
    }

    fun fail(message: String) {
        listener?.onPlaybackError(PlaybackError(message))
    }

    fun complete() {
        listener?.onPlaybackCompleted()
    }

    override fun release() {
        released = true
        loaded = null
    }
}
