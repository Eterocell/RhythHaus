package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import com.eterocell.rhythhaus.session.PlaybackSessionController
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.ProgressCheckpointKey
import com.eterocell.rhythhaus.session.RevisionedPlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.SessionQueueEntry
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMillis: Long?,
    val source: AudioSource,
    val artworkBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is PlayableTrack &&
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

data class QueueOccurrence(
    val id: String,
    val track: PlayableTrack,
)

sealed interface QueueMutationResult {
    data object Applied : QueueMutationResult

    data class Rejected(val reason: QueueMutationRejection) :
        QueueMutationResult
}

enum class QueueMutationRejection {
    CurrentOccurrence,
    StaleOccurrence,
    InvalidTargetIndex,
    CommandsDisabled,
}

data class PlaybackState(
    val currentOccurrenceId: String? = null,
    val queue: List<QueueOccurrence> = emptyList(),
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMillis: Long = 0L,
    val durationMillis: Long? = null,
    val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
    val error: PlaybackError? = null,
    internal val checkpointRevision: Long = 0L,
) {
    val currentOccurrence: QueueOccurrence?
        get() = queue.firstOrNull { it.id == currentOccurrenceId }

    val currentTrack: PlayableTrack?
        get() = currentOccurrence?.track

    val canPlay: Boolean =
        currentTrack != null &&
            status != PlaybackStatus.Loading &&
            status != PlaybackStatus.Buffering
    val isPlaying: Boolean = status == PlaybackStatus.Playing
    val progressFraction: Float
        get() {
            val duration = durationMillis ?: return 0f
            if (duration <= 0L) return 0f
            return (positionMillis.coerceIn(0L, duration).toFloat() /
                    duration.toFloat())
                .coerceIn(0f, 1f)
        }
}

private data class RevisionedShuffleOrder(
    val revision: Long = 0L,
    val sourceQueueIds: List<String> = emptyList(),
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
    val occurrenceIds: List<String> = emptyList(),
)

interface PlaybackEngineListener {
    fun onPlaybackStatus(generation: Long, status: PlaybackStatus)

    fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?
    )

    fun onPlaybackCompleted(generation: Long)

    fun onPlaybackError(generation: Long, error: PlaybackError)

    fun onSkipToNext(generation: Long)

    fun onSkipToPrevious(generation: Long)
}

data class LoadedPlayback(val generation: Long, val durationMillis: Long?)

interface PlatformPlaybackEngine {
    var listener: PlaybackEngineListener?

    suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback

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
    private val shuffleOrderFactory: (List<String>, String?) -> List<String> =
        ::defaultShuffleOrder,
    private val artworkLoader: (String) -> ByteArray? = { null },
) : PlaybackEngineListener, PlaybackSessionController {
    private val scope =
        CoroutineScope(SupervisorJob() + playbackEngineDispatcher)
    private val engineMutex = Mutex()
    private val sessionOperationMutex = Mutex()
    private var loadJob: Job? = null
    private var playWhenLoaded: Boolean = false
    private var activeGeneration: Long = 0L
    private val shuffledOrder = MutableStateFlow(RevisionedShuffleOrder())
    private val commandsEnabled = MutableStateFlow(true)
    private val nextCheckpointRevision = MutableStateFlow(0L)
    private var lastProgressCheckpointKey: ProgressCheckpointKey? = null
    private val occurrenceNamespace: String = uuid4()
    private var nextOccurrenceNumber: Long = 0L
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    // One process-owned persistence coordinator is the sole consumer. Unlimited
    // buffering keeps
    // synchronous controller methods and platform callbacks non-blocking
    // without dropping order.
    private val checkpointChannel =
        Channel<CheckpointEnvelope>(Channel.UNLIMITED)
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
            val failure =
                CancellationException("Playback checkpoint collector stopped")
            checkpointTransportMutex.withLock {
                checkpointCollectorActive = false
                checkpointTransportFailure = failure
                checkpointChannel.close(failure)
                while (true) {
                    val queued =
                        checkpointChannel.tryReceive().getOrNull() ?: break
                    if (queued is CheckpointEnvelope.Fence)
                        queued.reply.completeExceptionally(failure)
                }
            }
        }
    }

    init {
        engine.listener = this
    }

    fun setQueue(
        tracks: List<PlayableTrack>,
        selectedTrackId: String? = tracks.firstOrNull()?.id
    ) {
        val occurrences = tracks.map { track ->
            QueueOccurrence(freshOccurrenceId(), track)
        }
        val selectedOccurrenceId =
            occurrences.firstOrNull { it.track.id == selectedTrackId }?.id
        setOccurrenceQueue(occurrences, selectedOccurrenceId)
    }

    fun setOccurrenceQueue(
        occurrences: List<QueueOccurrence>,
        selectedOccurrenceId: String? = occurrences.firstOrNull()?.id,
    ) {
        if (!commandsEnabled.value) return
        require(occurrences.map { it.id }.distinct().size == occurrences.size)
        val selected =
            occurrences.firstOrNull { it.id == selectedOccurrenceId }
                ?: occurrences.firstOrNull()
        if (selected == null) {
            loadJob?.cancel()
            playWhenLoaded = false
            val generation = nextGeneration()
            resetProgressCheckpointKey()
            launchEngineAction { engine.clear(generation) }
            val published = publishState { previous ->
                PlaybackState(
                    queue = occurrences,
                    repeatMode = previous.repeatMode,
                    shuffleMode = previous.shuffleMode,
                )
            }
            emitImmediateCheckpoint(
                published.toSessionSnapshot(), published.checkpointRevision)
        } else {
            val published = publishState { previous ->
                PlaybackState(
                    currentOccurrenceId = selected.id,
                    queue = occurrences,
                    status = PlaybackStatus.Loading,
                    durationMillis = selected.track.durationMillis,
                    repeatMode = previous.repeatMode,
                    shuffleMode = previous.shuffleMode,
                )
            }
            publishRuntimeShuffleOrder(published, selected.id)
            loadSelected(selected, autoPlay = false)
            emitImmediateCheckpoint()
        }
    }

    fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        val occurrenceId =
            _state.value.queue.firstOrNull { it.track.id == trackId }?.id
                ?: return
        selectOccurrence(occurrenceId, autoPlay)
    }

    fun selectOccurrence(occurrenceId: String, autoPlay: Boolean = false) {
        if (!commandsEnabled.value) return
        val occurrence = occurrenceById(occurrenceId) ?: return
        resetProgressCheckpointKey()
        loadSelected(occurrence, autoPlay)
        emitImmediateCheckpoint()
    }

    fun setRepeatMode(mode: RepeatMode) {
        if (!commandsEnabled.value) return
        val previous = _state.value.repeatMode
        if (previous == mode) return
        val published = publishState { it.copy(repeatMode = mode) }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
        log.d { "RepeatMode changed: $previous -> $mode" }
    }

    fun cycleRepeatMode() {
        if (!commandsEnabled.value) return
        val previous = _state.value.repeatMode
        val next =
            when (previous) {
                RepeatMode.StopAfterQueue -> RepeatMode.RepeatPlaylist
                RepeatMode.RepeatPlaylist -> RepeatMode.RepeatOne
                RepeatMode.RepeatOne -> RepeatMode.StopAfterCurrent
                RepeatMode.StopAfterCurrent -> RepeatMode.StopAfterQueue
            }
        log.d { "Cycle repeat mode: $previous -> $next" }
        setRepeatMode(next)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        if (!commandsEnabled.value) return
        val previous = _state.value.shuffleMode
        if (previous == mode) return
        val published = publishState { it.copy(shuffleMode = mode) }
        log.d { "ShuffleMode changed: $previous -> $mode" }
        publishRuntimeShuffleOrder(published)
        log.d {
            "Shuffle mode applied, effective order: ${effectiveOrder(published)}"
        }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
    }

    fun toggleShuffleMode() {
        if (!commandsEnabled.value) return
        val previous = _state.value.shuffleMode
        val next =
            when (previous) {
                ShuffleMode.Off -> ShuffleMode.On
                ShuffleMode.On -> ShuffleMode.Off
            }
        log.d { "Toggle shuffle: $previous -> $next" }
        setShuffleMode(next)
    }

    fun play() {
        if (!commandsEnabled.value) return
        val current = _state.value.currentOccurrence ?: return
        if (_state.value.status == PlaybackStatus.Loading) {
            playWhenLoaded = true
            return
        }
        if (_state.value.status == PlaybackStatus.Idle ||
            _state.value.status == PlaybackStatus.Error) {
            loadSelected(current, autoPlay = true)
            return
        }
        launchEngineAction { engine.play() }
    }

    fun pause() {
        if (!commandsEnabled.value) return
        playWhenLoaded = false
        launchEngineAction { engine.pause() }
        emitImmediateCheckpoint()
    }

    fun stop() {
        if (!commandsEnabled.value) return
        playWhenLoaded = false
        resetProgressCheckpointKey()
        launchEngineAction { engine.stop() }
        emitImmediateCheckpoint()
    }

    fun seekTo(positionMillis: Long) {
        if (!commandsEnabled.value) return
        val duration = _state.value.durationMillis
        val safePosition =
            if (duration == null) max(0L, positionMillis)
            else positionMillis.coerceIn(0L, duration)
        val published = publishState {
            it.copy(positionMillis = safePosition, error = null)
        }
        resetProgressCheckpointKey()
        launchEngineAction { engine.seekTo(safePosition) }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
    }

    fun togglePlayPause() {
        if (!commandsEnabled.value) return
        if (_state.value.isPlaying) pause() else play()
    }

    fun restartCurrentTrack() {
        if (!commandsEnabled.value) return
        val current = _state.value.currentOccurrence ?: return
        val published = publishState {
            it.copy(positionMillis = 0L, error = null)
        }
        resetProgressCheckpointKey()
        when (_state.value.status) {
            PlaybackStatus.Loading -> playWhenLoaded = true

            PlaybackStatus.Idle,
            PlaybackStatus.Error,
            -> loadSelected(current, autoPlay = true)

            else ->
                launchEngineAction {
                    engine.seekTo(0L)
                    engine.play()
                }
        }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
    }

    fun skipToNext() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        nextTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    fun skipToPrevious() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        previousTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    suspend fun reorderUpcoming(
        occurrenceId: String,
        targetUpcomingIndex: Int
    ): QueueMutationResult = sessionOperationMutex.withLock {
        while (true) {
            if (!commandsEnabled.value)
                return@withLock QueueMutationResult.Rejected(
                    QueueMutationRejection.CommandsDisabled)
            val currentState = _state.value
            if (occurrenceId == currentState.currentOccurrenceId) {
                return@withLock QueueMutationResult.Rejected(
                    QueueMutationRejection.CurrentOccurrence)
            }
            val upcoming = currentState.upcomingOccurrences()
            val sourceIndex = upcoming.indexOfFirst { it.id == occurrenceId }
            if (sourceIndex < 0) {
                return@withLock QueueMutationResult.Rejected(
                    QueueMutationRejection.StaleOccurrence)
            }
            if (targetUpcomingIndex !in upcoming.indices) {
                return@withLock QueueMutationResult.Rejected(
                    QueueMutationRejection.InvalidTargetIndex)
            }
            val reordered =
                upcoming.toMutableList().apply {
                    add(targetUpcomingIndex, removeAt(sourceIndex))
                }
            if (applyUpcomingQueueMutation(currentState, reordered)) {
                return@withLock QueueMutationResult.Applied
            }
        }
        error("Unreachable queue mutation loop")
    }

    suspend fun removeUpcoming(occurrenceId: String): QueueMutationResult =
        sessionOperationMutex.withLock {
            while (true) {
                if (!commandsEnabled.value)
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.CommandsDisabled)
                val currentState = _state.value
                if (occurrenceId == currentState.currentOccurrenceId) {
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.CurrentOccurrence)
                }
                val upcoming = currentState.upcomingOccurrences()
                val sourceIndex = upcoming.indexOfFirst {
                    it.id == occurrenceId
                }
                if (sourceIndex < 0) {
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.StaleOccurrence)
                }
                val updated =
                    upcoming.toMutableList().apply { removeAt(sourceIndex) }
                if (applyUpcomingQueueMutation(currentState, updated)) {
                    return@withLock QueueMutationResult.Applied
                }
            }
            error("Unreachable queue mutation loop")
        }

    suspend fun clearUpcoming(): QueueMutationResult =
        sessionOperationMutex.withLock {
            while (true) {
                if (!commandsEnabled.value)
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.CommandsDisabled)
                if (applyUpcomingQueueMutation(_state.value, emptyList())) {
                    return@withLock QueueMutationResult.Applied
                }
            }
            error("Unreachable queue mutation loop")
        }

    fun release() {
        scope.cancel()
        engine.listener = null
        engine.release()
        checkpointChannel.close()
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped)
    }

    override fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled.value = enabled
        engine.setUserTransportEnabled(enabled)
    }

    override fun sessionSnapshot(): PlaybackSessionSnapshot =
        _state.value.toSessionSnapshot()

    override suspend fun awaitCheckpointFence() {
        val reply = CompletableDeferred<Unit>()
        checkpointTransportMutex.withLock {
            checkpointTransportFailure?.let { throw it }
            check(checkpointCollectorActive) {
                "Playback checkpoint collector is not active"
            }
            check(
                checkpointChannel
                    .trySend(CheckpointEnvelope.Fence(reply))
                    .isSuccess)
        }
        reply.await()
    }

    override suspend fun restoreSession(
        snapshot: PlaybackSessionSnapshot,
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot = sessionOperationMutex.withLock {
        loadJob?.cancel()
        playWhenLoaded = false
        resetProgressCheckpointKey()
        val tracksById = tracks.distinctBy { it.id }.associateBy { it.id }
        val reconciledQueue =
            snapshot.queue.mapNotNull { entry ->
                tracksById[entry.trackId]?.let {
                    QueueOccurrence(entry.occurrenceId, it)
                }
            }
        val restoredCurrent =
            snapshot.currentOccurrenceId?.let { currentId ->
                reconciledQueue.firstOrNull { it.id == currentId }
            } ?: reconciledQueue.firstOrNull()
        val restoredPosition =
            if (restoredCurrent?.id == snapshot.currentOccurrenceId)
                snapshot.positionMillis.coerceAtLeast(0L)
            else 0L
        applyModesAndQueue(
            reconciledQueue,
            restoredCurrent,
            snapshot.repeatMode,
            snapshot.shuffleMode)
        if (restoredCurrent == null) {
            clearPausedState(snapshot.repeatMode, snapshot.shuffleMode)
            emitImmediateCheckpoint()
            return@withLock revisionedSessionSnapshot()
        }
        try {
            engineMutex.withLock {
                val generation = nextGeneration()
                val loaded =
                    engine.loadPaused(
                        restoredCurrent.track.withLazyArtwork(), generation)
                check(loaded.generation == generation)
                check(generation == activeGeneration)
                val clamped =
                    loaded.durationMillis?.let {
                        restoredPosition.coerceIn(0L, it)
                    } ?: restoredPosition
                engine.seekTo(clamped)
                engine.pause()
                publishState {
                    it.copy(
                        status = PlaybackStatus.Paused,
                        positionMillis = clamped,
                        durationMillis =
                            loaded.durationMillis
                                ?: restoredCurrent.track.durationMillis,
                        error = null,
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            log.e { throwable.stackTraceToString() }
            clearPausedState(snapshot.repeatMode, snapshot.shuffleMode)
        }
        emitImmediateCheckpoint()
        revisionedSessionSnapshot()
    }

    override suspend fun reconcileSession(
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot = sessionOperationMutex.withLock {
        loadJob?.cancel()
        playWhenLoaded = false
        resetProgressCheckpointKey()
        val previous = _state.value
        val tracksById = tracks.distinctBy { it.id }.associateBy { it.id }
        val reconciledQueue =
            previous.queue.mapNotNull { occurrence ->
                tracksById[occurrence.track.id]?.let {
                    occurrence.copy(track = it)
                }
            }
        val current =
            previous.currentOccurrenceId?.let { currentId ->
                reconciledQueue.firstOrNull { it.id == currentId }
            }
        if (current != null) {
            val published = publishState { latest ->
                latest.copy(
                    currentOccurrenceId = current.id, queue = reconciledQueue)
            }
            publishRuntimeShuffleOrder(published)
            emitImmediateCheckpoint(
                published.toSessionSnapshot(), published.checkpointRevision)
            return@withLock published.toRevisionedSessionSnapshot()
        }
        val replacement = reconciledQueue.firstOrNull()
        if (replacement == null) {
            clearPausedState(previous.repeatMode, previous.shuffleMode)
            emitImmediateCheckpoint()
            return@withLock revisionedSessionSnapshot()
        }
        applyModesAndQueue(
            reconciledQueue,
            replacement,
            previous.repeatMode,
            previous.shuffleMode)
        try {
            engineMutex.withLock {
                val generation = nextGeneration()
                val loaded =
                    engine.loadPaused(
                        replacement.track.withLazyArtwork(), generation)
                check(loaded.generation == generation)
                check(generation == activeGeneration)
                engine.seekTo(0L)
                engine.pause()
                publishState {
                    it.copy(
                        status = PlaybackStatus.Paused,
                        positionMillis = 0L,
                        durationMillis =
                            loaded.durationMillis
                                ?: replacement.track.durationMillis,
                        error = null,
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            log.e { throwable.stackTraceToString() }
            clearPausedState(previous.repeatMode, previous.shuffleMode)
            throw throwable
        }
        emitImmediateCheckpoint()
        revisionedSessionSnapshot()
    }

    private fun loadSelected(occurrence: QueueOccurrence, autoPlay: Boolean) {
        loadJob?.cancel()
        val generation = nextGeneration()
        resetProgressCheckpointKey()
        playWhenLoaded = autoPlay
        val published = publishState { previous ->
            previous.copy(
                currentOccurrenceId = occurrence.id,
                status = PlaybackStatus.Loading,
                positionMillis = 0L,
                durationMillis = occurrence.track.durationMillis,
                error = null,
            )
        }
        publishRuntimeShuffleOrder(published, occurrence.id)
        loadJob = scope.launch {
            val trackWithArtwork = occurrence.track.withLazyArtwork()
            runEngineAction {
                if (_state.value.currentOccurrenceId != occurrence.id)
                    return@runEngineAction
                val loaded = engine.loadPaused(trackWithArtwork, generation)
                check(loaded.generation == generation)
                if (generation != activeGeneration) return@runEngineAction
                _state.value =
                    _state.value.copy(
                        status = PlaybackStatus.Paused,
                        durationMillis =
                            loaded.durationMillis
                                ?: _state.value.durationMillis,
                    )
                if (_state.value.currentOccurrenceId == occurrence.id &&
                    (autoPlay || playWhenLoaded)) {
                    playWhenLoaded = false
                    engine.play()
                }
            }
        }
    }

    private fun nextGeneration(): Long = ++activeGeneration

    private fun applyModesAndQueue(
        queue: List<QueueOccurrence>,
        current: QueueOccurrence?,
        repeatMode: RepeatMode,
        shuffleMode: ShuffleMode,
    ) {
        val published = publishState {
            PlaybackState(
                currentOccurrenceId = current?.id,
                queue = queue,
                status = PlaybackStatus.Paused,
                durationMillis = current?.track?.durationMillis,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
            )
        }
        publishRuntimeShuffleOrder(published)
    }

    private suspend fun clearPausedState(
        repeatMode: RepeatMode,
        shuffleMode: ShuffleMode
    ) {
        val generation = nextGeneration()
        engineMutex.withLock { engine.clear(generation) }
        val published = publishState {
            PlaybackState(
                status = PlaybackStatus.Paused,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
            )
        }
        publishRuntimeShuffleOrder(published)
    }

    private fun publishRuntimeShuffleOrder(
        state: PlaybackState,
        currentId: String? = state.currentOccurrenceId
    ) {
        val sourceQueueIds = state.queue.map { it.id }
        val previousOrder = shuffledOrder.value
        val occurrenceIds =
            if (previousOrder.sourceQueueIds == sourceQueueIds &&
                previousOrder.shuffleMode == state.shuffleMode) {
                previousOrder.occurrenceIds
            } else if (state.shuffleMode == ShuffleMode.On) {
                val generated =
                    shuffleOrderFactory(state.queue.map { it.id }, currentId)
                        .filter { id -> state.queue.any { it.id == id } }
                        .distinct()
                generated +
                    state.queue.map { it.id }.filterNot { it in generated }
            } else {
                emptyList()
            }
        val candidate =
            RevisionedShuffleOrder(
                revision = state.checkpointRevision,
                sourceQueueIds = sourceQueueIds,
                shuffleMode = state.shuffleMode,
                occurrenceIds = occurrenceIds,
            )
        while (true) {
            val previous = shuffledOrder.value
            if (previous.revision > candidate.revision) return
            if (shuffledOrder.compareAndSet(previous, candidate)) return
        }
    }

    private fun PlaybackState.upcomingOccurrences(): List<QueueOccurrence> {
        val currentIndex = queue.indexOfFirst { it.id == currentOccurrenceId }
        return if (currentIndex < 0) queue else queue.drop(currentIndex + 1)
    }

    private fun applyUpcomingQueueMutation(
        previous: PlaybackState,
        upcoming: List<QueueOccurrence>,
    ): Boolean {
        val currentIndex =
            previous.queue.indexOfFirst {
                it.id == previous.currentOccurrenceId
            }
        val preserved =
            if (currentIndex < 0) emptyList()
            else previous.queue.take(currentIndex + 1)
        val updated =
            previous.copy(
                queue = preserved + upcoming,
                checkpointRevision = reserveCheckpointRevision(),
            )
        if (!_state.compareAndSet(previous, updated)) return false
        publishRuntimeShuffleOrder(updated)
        emitImmediateCheckpoint(
            updated.toSessionSnapshot(), updated.checkpointRevision)
        return true
    }

    private fun publishState(
        transform: (PlaybackState) -> PlaybackState
    ): PlaybackState {
        while (true) {
            val previous = _state.value
            val updated =
                transform(previous)
                    .copy(checkpointRevision = reserveCheckpointRevision())
            if (_state.compareAndSet(previous, updated)) return updated
        }
    }

    private fun reserveCheckpointRevision(): Long {
        while (true) {
            val previous = nextCheckpointRevision.value
            val next = previous + 1L
            if (nextCheckpointRevision.compareAndSet(previous, next))
                return next
        }
    }

    private fun PlaybackState.toSessionSnapshot(): PlaybackSessionSnapshot =
        PlaybackSessionSnapshot(
            queue = queue.map { SessionQueueEntry(it.id, it.track.id) },
            currentOccurrenceId = currentOccurrenceId,
            positionMillis = positionMillis.coerceAtLeast(0L),
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )

    private fun revisionedSessionSnapshot(): RevisionedPlaybackSessionSnapshot =
        _state.value.toRevisionedSessionSnapshot()

    private fun PlaybackState.toRevisionedSessionSnapshot():
        RevisionedPlaybackSessionSnapshot =
        RevisionedPlaybackSessionSnapshot(
            toSessionSnapshot(), checkpointRevision)

    private fun emitImmediateCheckpoint(revision: Long? = null) {
        val current = _state.value
        emitImmediateCheckpoint(
            current.toSessionSnapshot(), revision ?: current.checkpointRevision)
    }

    private fun emitImmediateCheckpoint(
        snapshot: PlaybackSessionSnapshot,
        revision: Long? = null
    ) {
        check(
            checkpointChannel
                .trySend(
                    CheckpointEnvelope.Checkpoint(
                        PlaybackCheckpoint.Immediate(snapshot, revision)),
                )
                .isSuccess,
        )
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

    private fun effectiveOrder(
        state: PlaybackState = _state.value
    ): List<String> =
        when (state.shuffleMode) {
            ShuffleMode.Off -> state.queue.map { it.id }

            ShuffleMode.On ->
                shuffledOrder.value
                    .takeIf {
                        it.revision <= state.checkpointRevision &&
                            it.sourceQueueIds ==
                                state.queue.map { occurrence ->
                                    occurrence.id
                                } &&
                            it.shuffleMode == state.shuffleMode
                    }
                    ?.occurrenceIds
                    ?.ifEmpty { state.queue.map { it.id } }
                    ?: state.queue.map { it.id }
        }

    private fun occurrenceById(occurrenceId: String?): QueueOccurrence? =
        _state.value.queue.firstOrNull { it.id == occurrenceId }

    private fun currentEffectiveIndex(
        order: List<String> = effectiveOrder()
    ): Int = order.indexOf(_state.value.currentOccurrenceId)

    private fun nextTrack(wrap: Boolean): QueueOccurrence? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val nextId =
            order.getOrNull(currentIndex + 1)
                ?: if (wrap) order.firstOrNull() else null
        return occurrenceById(nextId)
    }

    private fun previousTrack(wrap: Boolean): QueueOccurrence? {
        val order = effectiveOrder()
        if (order.isEmpty()) return null
        val currentIndex = currentEffectiveIndex(order)
        if (currentIndex < 0) return null
        val previousId =
            order.getOrNull(currentIndex - 1)
                ?: if (wrap) order.lastOrNull() else null
        return occurrenceById(previousId)
    }

    private fun stopAtCurrentTrackEnd() {
        val duration = _state.value.durationMillis
        val published = publishState {
            it.copy(
                status = PlaybackStatus.Stopped,
                positionMillis = duration ?: max(0L, it.positionMillis),
                error = null,
            )
        }
        resetProgressCheckpointKey()
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
    }

    override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
        if (generation != activeGeneration) return
        _state.value = _state.value.copy(status = status, error = null)
    }

    override fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?
    ) {
        if (generation != activeGeneration) return
        val checkpointState = publishState {
            it.copy(
                positionMillis = max(0L, positionMillis),
                durationMillis = durationMillis ?: it.durationMillis,
            )
        }
        val currentId = checkpointState.currentOccurrenceId ?: return
        if (checkpointState.status != PlaybackStatus.Playing) return
        val key =
            ProgressCheckpointKey(
                generation, currentId, max(0L, positionMillis) / 1_000L)
        if (lastProgressCheckpointKey == key) return
        lastProgressCheckpointKey = key
        check(
            checkpointChannel
                .trySend(
                    CheckpointEnvelope.Checkpoint(
                        PlaybackCheckpoint.PlayingProgress(
                            key = key,
                            snapshot = checkpointState.toSessionSnapshot(),
                            revision = checkpointState.checkpointRevision,
                        ),
                    ),
                )
                .isSuccess,
        )
    }

    override fun onPlaybackCompleted(generation: Long) {
        if (generation != activeGeneration) return
        when (_state.value.repeatMode) {
            RepeatMode.RepeatOne -> {
                val current =
                    _state.value.currentOccurrence
                        ?: return stopAtCurrentTrackEnd()
                loadSelected(current, autoPlay = true)
                emitImmediateCheckpoint()
            }

            RepeatMode.RepeatPlaylist -> {
                val next = nextTrack(wrap = true)
                if (next != null) {
                    loadSelected(next, autoPlay = true)
                    emitImmediateCheckpoint()
                } else {
                    stopAtCurrentTrackEnd()
                }
            }

            RepeatMode.StopAfterCurrent -> stopAtCurrentTrackEnd()

            RepeatMode.StopAfterQueue -> {
                val next = nextTrack(wrap = false)
                if (next != null) {
                    loadSelected(next, autoPlay = true)
                    emitImmediateCheckpoint()
                } else {
                    stopAtCurrentTrackEnd()
                }
            }
        }
    }

    override fun onPlaybackError(generation: Long, error: PlaybackError) {
        if (generation != activeGeneration) return
        _state.value =
            _state.value.copy(status = PlaybackStatus.Error, error = error)
    }

    override fun onSkipToNext(generation: Long) {
        if (generation != activeGeneration) return
        skipToNext()
    }

    override fun onSkipToPrevious(generation: Long) {
        if (generation != activeGeneration) return
        skipToPrevious()
    }

    private fun freshOccurrenceId(): String =
        "queue-$occurrenceNamespace-${nextOccurrenceNumber++}"
}

private sealed interface CheckpointEnvelope {
    data class Checkpoint(val value: PlaybackCheckpoint) : CheckpointEnvelope

    data class Fence(val reply: CompletableDeferred<Unit>) : CheckpointEnvelope
}

private fun defaultShuffleOrder(
    ids: List<String>,
    currentId: String?
): List<String> {
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

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
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
