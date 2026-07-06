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
) : PlaybackEngineListener {
    private val scope = CoroutineScope(SupervisorJob() + playbackEngineDispatcher)
    private val engineMutex = Mutex()
    private var loadJob: Job? = null
    private var playWhenLoaded: Boolean = false
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
            loadSelected(selected, autoPlay = false)
        }
    }

    fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        val track = _state.value.queue.firstOrNull { it.id == trackId } ?: return
        loadSelected(track, autoPlay)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    fun cycleRepeatMode() {
        setRepeatMode(
            when (_state.value.repeatMode) {
                RepeatMode.StopAfterQueue -> RepeatMode.RepeatPlaylist
                RepeatMode.RepeatPlaylist -> RepeatMode.RepeatOne
                RepeatMode.RepeatOne -> RepeatMode.StopAfterCurrent
                RepeatMode.StopAfterCurrent -> RepeatMode.StopAfterQueue
            },
        )
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _state.value = _state.value.copy(shuffleMode = mode)
    }

    fun toggleShuffleMode() {
        setShuffleMode(
            when (_state.value.shuffleMode) {
                ShuffleMode.Off -> ShuffleMode.On
                ShuffleMode.On -> ShuffleMode.Off
            },
        )
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
        val queue = _state.value.queue
        val currentId = _state.value.currentTrack?.id
        val currentIndex = queue.indexOfFirst { it.id == currentId }
        val nextTrack = queue.getOrNull(currentIndex + 1)
        if (nextTrack != null) {
            loadSelected(nextTrack, autoPlay = true)
        } else {
            _state.value = _state.value.copy(status = PlaybackStatus.Stopped, positionMillis = 0L)
        }
    }

    override fun onPlaybackError(error: PlaybackError) {
        _state.value = _state.value.copy(status = PlaybackStatus.Error, error = error)
    }

    override fun onSkipToNext() {
        val queue = _state.value.queue
        val currentId = _state.value.currentTrack?.id
        val currentIndex = queue.indexOfFirst { it.id == currentId }
        val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
        nextTrack?.let { loadSelected(it, autoPlay = true) }
    }

    override fun onSkipToPrevious() {
        val queue = _state.value.queue
        val currentId = _state.value.currentTrack?.id
        val currentIndex = queue.indexOfFirst { it.id == currentId }
        val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
        prevTrack?.let { loadSelected(it, autoPlay = true) }
    }
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

fun formatMillis(totalMillis: Long?): String {
    if (totalMillis == null) return "--:--"
    val totalSeconds = max(0L, totalMillis / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
