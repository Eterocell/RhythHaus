package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import com.eterocell.rhythhaus.session.PlaybackSessionController
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.ProgressCheckpointKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.Channel
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
    fun onPlaybackStatus(generation: Long, status: PlaybackStatus)
    fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?)
    fun onPlaybackCompleted(generation: Long)
    fun onPlaybackError(generation: Long, error: PlaybackError)
    fun onSkipToNext(generation: Long)
    fun onSkipToPrevious(generation: Long)
}

data class LoadedPlayback(val generation: Long, val durationMillis: Long?)

interface PlatformPlaybackEngine {
    var listener: PlaybackEngineListener?

    suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback
    fun clear(generation: Long)
    fun setUserTransportEnabled(enabled: Boolean)
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
    private val artworkLoader: (String) -> ByteArray? = { null },
) : PlaybackEngineListener, PlaybackSessionController {
    private val scope = CoroutineScope(SupervisorJob() + playbackEngineDispatcher)
    private val engineMutex = Mutex()
    private val sessionOperationMutex = Mutex()
    private var loadJob: Job? = null
    private var playWhenLoaded: Boolean = false
    private var activeGeneration: Long = 0L
    private var shuffledOrder: List<String> = emptyList()
    private var commandsEnabled: Boolean = true
    private var lastProgressCheckpointKey: ProgressCheckpointKey? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    // One process-owned persistence coordinator is the sole consumer. Unlimited buffering keeps
    // synchronous controller methods and platform callbacks non-blocking without dropping order.
    private val checkpointChannel = Channel<CheckpointEnvelope>(Channel.UNLIMITED)
    private val checkpointTransportMutex = Mutex()
    private var checkpointCollectorActive = false
    private var checkpointTransportFailure: Throwable? = null
    override val checkpoints: Flow<PlaybackCheckpoint> = flow {
        checkpointTransportMutex.withLock {
            check(!checkpointCollectorActive)
            checkpointTransportFailure?.let { throw it }
            checkpointCollectorActive = true
        }
        try {
            for (envelope in checkpointChannel) {
                when (envelope) {
                    is CheckpointEnvelope.Checkpoint -> emit(envelope.value)
                    is CheckpointEnvelope.Fence -> envelope.reply.complete(Unit)
                }
            }
        } finally {
            val failure = CancellationException("Playback checkpoint collector stopped")
            checkpointTransportMutex.withLock {
                checkpointCollectorActive = false
                checkpointTransportFailure = failure
                checkpointChannel.close(failure)
                while (true) {
                    val queued = checkpointChannel.tryReceive().getOrNull() ?: break
                    if (queued is CheckpointEnvelope.Fence) queued.reply.completeExceptionally(failure)
                }
            }
        }
    }

    init {
        engine.listener = this
    }

    fun setQueue(tracks: List<PlayableTrack>, selectedTrackId: String? = tracks.firstOrNull()?.id) {
        if (!commandsEnabled) return
        val selected = tracks.firstOrNull { it.id == selectedTrackId } ?: tracks.firstOrNull()
        if (selected == null) {
            loadJob?.cancel()
            playWhenLoaded = false
            shuffledOrder = emptyList()
            val generation = nextGeneration()
            resetProgressCheckpointKey()
            launchEngineAction { engine.clear(generation) }
            _state.value = PlaybackState(
                queue = tracks,
                repeatMode = _state.value.repeatMode,
                shuffleMode = _state.value.shuffleMode,
            )
            emitImmediateCheckpoint()
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
            emitImmediateCheckpoint()
        }
    }

    fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        if (!commandsEnabled) return
        val track = _state.value.queue.firstOrNull { it.id == trackId } ?: return
        resetProgressCheckpointKey()
        loadSelected(track, autoPlay)
        emitImmediateCheckpoint()
    }

    fun setRepeatMode(mode: RepeatMode) {
        if (!commandsEnabled) return
        val previous = _state.value.repeatMode
        if (previous == mode) return
        _state.value = _state.value.copy(repeatMode = mode)
        emitImmediateCheckpoint()
        log.d { "RepeatMode changed: $previous -> $mode" }
    }

    fun cycleRepeatMode() {
        if (!commandsEnabled) return
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
        if (!commandsEnabled) return
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
        emitImmediateCheckpoint()
    }

    fun toggleShuffleMode() {
        if (!commandsEnabled) return
        val previous = _state.value.shuffleMode
        val next = when (previous) {
            ShuffleMode.Off -> ShuffleMode.On
            ShuffleMode.On -> ShuffleMode.Off
        }
        log.d { "Toggle shuffle: $previous -> $next" }
        setShuffleMode(next)
    }

    fun play() {
        if (!commandsEnabled) return
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
        if (!commandsEnabled) return
        playWhenLoaded = false
        launchEngineAction { engine.pause() }
        emitImmediateCheckpoint()
    }

    fun stop() {
        if (!commandsEnabled) return
        playWhenLoaded = false
        resetProgressCheckpointKey()
        launchEngineAction { engine.stop() }
        emitImmediateCheckpoint()
    }

    fun seekTo(positionMillis: Long) {
        if (!commandsEnabled) return
        val duration = _state.value.durationMillis
        val safePosition = if (duration == null) max(0L, positionMillis) else positionMillis.coerceIn(0L, duration)
        _state.value = _state.value.copy(positionMillis = safePosition, error = null)
        resetProgressCheckpointKey()
        launchEngineAction { engine.seekTo(safePosition) }
        emitImmediateCheckpoint()
    }

    fun togglePlayPause() {
        if (!commandsEnabled) return
        if (_state.value.isPlaying) pause() else play()
    }

    fun restartCurrentTrack() {
        if (!commandsEnabled) return
        val current = _state.value.currentTrack ?: return
        _state.value = _state.value.copy(positionMillis = 0L, error = null)
        resetProgressCheckpointKey()
        when (_state.value.status) {
            PlaybackStatus.Loading -> playWhenLoaded = true
            PlaybackStatus.Idle,
            PlaybackStatus.Error,
            -> loadSelected(current, autoPlay = true)
            else -> launchEngineAction {
                engine.seekTo(0L)
                engine.play()
            }
        }
        emitImmediateCheckpoint()
    }

    fun skipToNext() {
        if (!commandsEnabled) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        nextTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    fun skipToPrevious() {
        if (!commandsEnabled) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        previousTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    fun release() {
        scope.cancel()
        engine.listener = null
        engine.release()
        checkpointChannel.close()
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped)
    }

    override fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled = enabled
        engine.setUserTransportEnabled(enabled)
    }

    override fun sessionSnapshot(): PlaybackSessionSnapshot = _state.value.toSessionSnapshot()

    override suspend fun awaitCheckpointFence() {
        val reply = CompletableDeferred<Unit>()
        checkpointTransportMutex.withLock {
            checkpointTransportFailure?.let { throw it }
            check(checkpointCollectorActive) { "Playback checkpoint collector is not active" }
            check(checkpointChannel.trySend(CheckpointEnvelope.Fence(reply)).isSuccess)
        }
        reply.await()
    }

    override suspend fun restoreSession(snapshot: PlaybackSessionSnapshot, tracks: List<PlayableTrack>) {
        sessionOperationMutex.withLock {
            loadJob?.cancel()
            playWhenLoaded = false
            resetProgressCheckpointKey()
            val stableTracks = tracks.distinctBy { it.id }
            val tracksById = stableTracks.associateBy { it.id }
            val reconciledQueue = snapshot.queueIds.distinct().mapNotNull(tracksById::get)
            val restoredCurrent = snapshot.currentTrackId?.let(tracksById::get)?.takeIf { it in reconciledQueue }
                ?: reconciledQueue.firstOrNull()
            val restoredPosition = if (restoredCurrent?.id == snapshot.currentTrackId) snapshot.positionMillis.coerceAtLeast(0L) else 0L
            applyModesAndQueue(reconciledQueue, restoredCurrent, snapshot.repeatMode, snapshot.shuffleMode)
            if (restoredCurrent == null) {
                clearPausedState(snapshot.repeatMode, snapshot.shuffleMode)
                emitImmediateCheckpoint()
                return@withLock
            }
            try {
                engineMutex.withLock {
                    val generation = nextGeneration()
                    val loaded = engine.loadPaused(restoredCurrent.withLazyArtwork(), generation)
                    check(loaded.generation == generation)
                    check(generation == activeGeneration)
                    val clamped = loaded.durationMillis?.let { restoredPosition.coerceIn(0L, it) } ?: restoredPosition
                    engine.seekTo(clamped)
                    engine.pause()
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.Paused,
                        positionMillis = clamped,
                        durationMillis = loaded.durationMillis ?: restoredCurrent.durationMillis,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                log.e { throwable.stackTraceToString() }
                clearPausedState(snapshot.repeatMode, snapshot.shuffleMode)
            }
            emitImmediateCheckpoint()
        }
    }

    override suspend fun reconcileSession(tracks: List<PlayableTrack>) {
        sessionOperationMutex.withLock {
            loadJob?.cancel()
            playWhenLoaded = false
            resetProgressCheckpointKey()
            val previous = _state.value
            val stableTracks = tracks.distinctBy { it.id }
            val tracksById = stableTracks.associateBy { it.id }
            val reconciledQueue = previous.queue.distinctBy { it.id }.mapNotNull { tracksById[it.id] }
            val current = previous.currentTrack?.id?.let(tracksById::get)?.takeIf { it in reconciledQueue }
            if (current != null) {
                _state.value = previous.copy(currentTrack = current, queue = reconciledQueue)
                regenerateRuntimeShuffleOrder()
                emitImmediateCheckpoint()
                return@withLock
            }
            val replacement = reconciledQueue.firstOrNull()
            if (replacement == null) {
                clearPausedState(previous.repeatMode, previous.shuffleMode)
                emitImmediateCheckpoint()
                return@withLock
            }
            applyModesAndQueue(reconciledQueue, replacement, previous.repeatMode, previous.shuffleMode)
            try {
                engineMutex.withLock {
                    val generation = nextGeneration()
                    val loaded = engine.loadPaused(replacement.withLazyArtwork(), generation)
                    check(loaded.generation == generation)
                    check(generation == activeGeneration)
                    engine.seekTo(0L)
                    engine.pause()
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.Paused,
                        positionMillis = 0L,
                        durationMillis = loaded.durationMillis ?: replacement.durationMillis,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                log.e { throwable.stackTraceToString() }
                clearPausedState(previous.repeatMode, previous.shuffleMode)
            }
            emitImmediateCheckpoint()
        }
    }

    private fun loadSelected(track: PlayableTrack, autoPlay: Boolean) {
        loadJob?.cancel()
        val generation = nextGeneration()
        resetProgressCheckpointKey()
        playWhenLoaded = autoPlay
        _state.value = _state.value.copy(
            currentTrack = track,
            status = PlaybackStatus.Loading,
            positionMillis = 0L,
            durationMillis = track.durationMillis,
            error = null,
        )
        loadJob = scope.launch {
            val trackWithArtwork = track.withLazyArtwork()
            runEngineAction {
                if (_state.value.currentTrack?.id != track.id) return@runEngineAction
                val loaded = engine.loadPaused(trackWithArtwork, generation)
                check(loaded.generation == generation)
                if (generation != activeGeneration) return@runEngineAction
                _state.value = _state.value.copy(
                    status = PlaybackStatus.Paused,
                    durationMillis = loaded.durationMillis ?: _state.value.durationMillis,
                )
                if (_state.value.currentTrack?.id == trackWithArtwork.id && (autoPlay || playWhenLoaded)) {
                    playWhenLoaded = false
                    engine.play()
                }
            }
        }
    }

    private fun nextGeneration(): Long = ++activeGeneration

    private fun applyModesAndQueue(
        queue: List<PlayableTrack>,
        current: PlayableTrack?,
        repeatMode: RepeatMode,
        shuffleMode: ShuffleMode,
    ) {
        _state.value = PlaybackState(
            currentTrack = current,
            queue = queue,
            status = PlaybackStatus.Paused,
            durationMillis = current?.durationMillis,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )
        regenerateRuntimeShuffleOrder()
    }

    private suspend fun clearPausedState(repeatMode: RepeatMode, shuffleMode: ShuffleMode) {
        val generation = nextGeneration()
        engineMutex.withLock { engine.clear(generation) }
        shuffledOrder = emptyList()
        _state.value = PlaybackState(
            status = PlaybackStatus.Paused,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )
    }

    private fun regenerateRuntimeShuffleOrder() {
        if (_state.value.shuffleMode == ShuffleMode.On) regenerateShuffleOrder() else shuffledOrder = emptyList()
    }

    private fun PlaybackState.toSessionSnapshot(): PlaybackSessionSnapshot = PlaybackSessionSnapshot(
        queueIds = queue.map { it.id },
        currentTrackId = currentTrack?.id,
        positionMillis = positionMillis.coerceAtLeast(0L),
        repeatMode = repeatMode,
        shuffleMode = shuffleMode,
    )

    private fun emitImmediateCheckpoint() {
        check(checkpointChannel.trySend(CheckpointEnvelope.Checkpoint(PlaybackCheckpoint.Immediate(sessionSnapshot()))).isSuccess)
    }

    private fun resetProgressCheckpointKey() {
        lastProgressCheckpointKey = null
    }

    private fun PlayableTrack.withLazyArtwork(): PlayableTrack {
        if (artworkBytes != null) return this
        val loadedArtwork = artworkLoader(id) ?: return this
        return copy(artworkBytes = loadedArtwork)
    }

    private fun launchEngineAction(action: suspend () -> Unit) {
        scope.launch {
            runEngineAction(action)
        }
    }

    private suspend fun runEngineAction(action: suspend () -> Unit) {
        try {
            engineMutex.withLock {
                action()
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            log.e { throwable.stackTraceToString() }
            onPlaybackError(
                activeGeneration,
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

    override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
        if (generation != activeGeneration) return
        _state.value = _state.value.copy(status = status, error = null)
    }

    override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) {
        if (generation != activeGeneration) return
        _state.value = _state.value.copy(
            positionMillis = max(0L, positionMillis),
            durationMillis = durationMillis ?: _state.value.durationMillis,
        )
        val currentId = _state.value.currentTrack?.id ?: return
        if (_state.value.status != PlaybackStatus.Playing) return
        val key = ProgressCheckpointKey(generation, currentId, max(0L, positionMillis) / 1_000L)
        if (lastProgressCheckpointKey == key) return
        lastProgressCheckpointKey = key
        check(
            checkpointChannel.trySend(
                CheckpointEnvelope.Checkpoint(PlaybackCheckpoint.PlayingProgress(key, sessionSnapshot())),
            ).isSuccess,
        )
    }

    override fun onPlaybackCompleted(generation: Long) {
        if (generation != activeGeneration) return
        when (_state.value.repeatMode) {
            RepeatMode.RepeatOne -> {
                val current = _state.value.currentTrack ?: return stopAtCurrentTrackEnd()
                loadSelected(current, autoPlay = true)
                emitImmediateCheckpoint()
            }
            RepeatMode.RepeatPlaylist -> {
                val next = nextTrack(wrap = true)
                if (next != null) {
                    loadSelected(next, autoPlay = true)
                    emitImmediateCheckpoint()
                } else stopAtCurrentTrackEnd()
            }
            RepeatMode.StopAfterCurrent -> stopAtCurrentTrackEnd()
            RepeatMode.StopAfterQueue -> {
                val next = nextTrack(wrap = false)
                if (next != null) {
                    loadSelected(next, autoPlay = true)
                    emitImmediateCheckpoint()
                } else stopAtCurrentTrackEnd()
            }
        }
    }

    override fun onPlaybackError(generation: Long, error: PlaybackError) {
        if (generation != activeGeneration) return
        _state.value = _state.value.copy(status = PlaybackStatus.Error, error = error)
    }

    override fun onSkipToNext(generation: Long) {
        if (generation != activeGeneration) return
        skipToNext()
    }

    override fun onSkipToPrevious(generation: Long) {
        if (generation != activeGeneration) return
        skipToPrevious()
    }
}

private sealed interface CheckpointEnvelope {
    data class Checkpoint(val value: PlaybackCheckpoint) : CheckpointEnvelope
    data class Fence(val reply: CompletableDeferred<Unit>) : CheckpointEnvelope
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
    private var generation: Long = 0L
    var released: Boolean = false
        private set

    override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
        this.generation = generation
        loaded = track
        positionMillis = 0L
        durationMillis = track.durationMillis
        listener?.onPlaybackProgress(generation, positionMillis, durationMillis)
        listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
        return LoadedPlayback(generation, durationMillis)
    }

    override fun clear(generation: Long) {
        this.generation = generation
        loaded = null
        positionMillis = 0L
        durationMillis = null
    }

    override fun setUserTransportEnabled(enabled: Boolean) = Unit

    override fun play() {
        requireNotNull(loaded) { "No track loaded" }
        listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
    }

    override fun pause() {
        listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
    }

    override fun stop() {
        positionMillis = 0L
        listener?.onPlaybackProgress(generation, positionMillis, durationMillis)
        listener?.onPlaybackStatus(generation, PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        this.positionMillis = positionMillis
        listener?.onPlaybackProgress(generation, positionMillis, durationMillis)
    }

    fun fail(message: String) {
        listener?.onPlaybackError(generation, PlaybackError(message))
    }

    fun complete() {
        listener?.onPlaybackCompleted(generation)
    }

    fun activeGenerationForTest(): Long = generation

    override fun release() {
        released = true
        loaded = null
    }
}
