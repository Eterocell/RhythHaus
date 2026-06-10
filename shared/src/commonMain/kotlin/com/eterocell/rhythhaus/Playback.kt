package com.eterocell.rhythhaus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMillis: Long?,
    val source: AudioSource,
)

sealed interface AudioSource {
    val stableKey: String

    data class FilePath(val path: String) : AudioSource {
        override val stableKey: String = path
    }

    data class Uri(val value: String) : AudioSource {
        override val stableKey: String = value
    }
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

class PlaybackController(
    private val engine: PlatformPlaybackEngine = createPlatformPlaybackEngine(),
) : PlaybackEngineListener {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    init {
        engine.listener = this
    }

    fun setQueue(tracks: List<PlayableTrack>, selectedTrackId: String? = tracks.firstOrNull()?.id) {
        val selected = tracks.firstOrNull { it.id == selectedTrackId } ?: tracks.firstOrNull()
        _state.value = PlaybackState(
            currentTrack = selected,
            queue = tracks,
            status = if (selected == null) PlaybackStatus.Idle else PlaybackStatus.Stopped,
            durationMillis = selected?.durationMillis,
        )
        selected?.let { loadSelected(it, autoPlay = false) }
    }

    fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        val track = _state.value.queue.firstOrNull { it.id == trackId } ?: return
        loadSelected(track, autoPlay)
    }

    fun play() {
        val current = _state.value.currentTrack ?: return
        if (_state.value.status == PlaybackStatus.Idle || _state.value.status == PlaybackStatus.Error) {
            loadSelected(current, autoPlay = true)
            return
        }
        runEngineAction { engine.play() }
    }

    fun pause() {
        runEngineAction { engine.pause() }
    }

    fun stop() {
        runEngineAction { engine.stop() }
    }

    fun seekTo(positionMillis: Long) {
        val duration = _state.value.durationMillis
        val safePosition = if (duration == null) max(0L, positionMillis) else positionMillis.coerceIn(0L, duration)
        _state.value = _state.value.copy(positionMillis = safePosition, error = null)
        runEngineAction { engine.seekTo(safePosition) }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun release() {
        engine.listener = null
        engine.release()
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped)
    }

    private fun loadSelected(track: PlayableTrack, autoPlay: Boolean) {
        _state.value = _state.value.copy(
            currentTrack = track,
            status = PlaybackStatus.Loading,
            positionMillis = 0L,
            durationMillis = track.durationMillis,
            error = null,
        )
        runEngineAction {
            engine.load(track)
            if (autoPlay) engine.play()
        }
    }

    private fun runEngineAction(action: () -> Unit) {
        try {
            action()
        } catch (throwable: Throwable) {
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
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped, positionMillis = 0L)
    }

    override fun onPlaybackError(error: PlaybackError) {
        _state.value = _state.value.copy(status = PlaybackStatus.Error, error = error)
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
