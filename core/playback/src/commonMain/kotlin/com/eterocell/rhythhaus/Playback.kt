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

/** Represents the current lifecycle state of a playback request. */
public enum class PlaybackStatus {
    /** No media is selected for playback. */
    Idle,
    /** The selected media is being prepared by the engine. */
    Loading,
    /** The engine is waiting for playable media data. */
    Buffering,
    /** Media is advancing at the current playback position. */
    Playing,
    /** Media is loaded but not advancing. */
    Paused,
    /** Playback was stopped and its position reset. */
    Stopped,
    /** The most recent engine operation failed. */
    Error,
}

/** Selects how playback advances when a track completes. */
public enum class RepeatMode {
    /** Restarts the selected occurrence after it completes. */
    RepeatOne,
    /** Continues from the first occurrence after the queue ends. */
    RepeatPlaylist,
    /** Stops when the selected occurrence completes. */
    StopAfterCurrent,
    /** Continues through upcoming occurrences, then stops. */
    StopAfterQueue,
}

/** Selects whether queue traversal preserves insertion order. */
public enum class ShuffleMode {
    /** Traverses occurrences in their queue order. */
    Off,
    /** Traverses occurrences using the generated shuffled order. */
    On,
}

/** Describes an error reported while loading or playing audio. */
public data class PlaybackError(
    /** User-visible summary of the playback failure. */
    public val message: String,
    /** Optional underlying failure detail. */
    public val cause: String? = null,
)

/** Identifies one queue occurrence and its track, including duplicates. */
public data class QueueOccurrence(
    /** Stable identifier for this queue occurrence. */
    public val id: String,
    /** Track assigned to this queue occurrence. */
    public val track: PlayableTrack,
)

/** Result of a queue mutation. */
public sealed interface QueueMutationResult {
    /** Indicates that the mutation was applied. */
    public data object Applied : QueueMutationResult

    /** Indicates that the mutation was rejected. */
    public data class Rejected(
        /** Rejection reason. */
        public val reason: QueueMutationRejection,
    ) : QueueMutationResult
}

/** Explains why a requested queue mutation could not be applied. */
public enum class QueueMutationRejection {
    /** The selected occurrence cannot be moved or removed. */
    CurrentOccurrence,
    /** The requested occurrence is no longer in the upcoming queue. */
    StaleOccurrence,
    /** The requested destination is outside the upcoming queue. */
    InvalidTargetIndex,
    /** Session coordination has temporarily disabled mutations. */
    CommandsDisabled,
}

/** Immutable state published by [PlaybackController]. */
public data class PlaybackState(
    /** Identifier of the selected queue occurrence, when any. */
    public val currentOccurrenceId: String? = null,
    /** Ordered queue including duplicate track occurrences. */
    public val queue: List<QueueOccurrence> = emptyList(),
    /** Lifecycle state reported by the platform engine. */
    public val status: PlaybackStatus = PlaybackStatus.Idle,
    /** Current playback position in milliseconds. */
    public val positionMillis: Long = 0L,
    /** Known media duration in milliseconds, when available. */
    public val durationMillis: Long? = null,
    /** Completion behavior selected for the active queue. */
    public val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    /** Queue traversal behavior selected for the active queue. */
    public val shuffleMode: ShuffleMode = ShuffleMode.Off,
    /** Most recent playback error, cleared after a successful state change. */
    public val error: PlaybackError? = null,
    internal val checkpointRevision: Long = 0L,
) {
    /** Selected queue occurrence, when its identifier resolves in [queue]. */
    public val currentOccurrence: QueueOccurrence?
        get() = queue.firstOrNull { it.id == currentOccurrenceId }

    /** Track for [currentOccurrence], when one is selected. */
    public val currentTrack: PlayableTrack?
        get() = currentOccurrence?.track

    /** Whether the selected state can accept a play request. */
    public val canPlay: Boolean =
        currentTrack != null &&
            status != PlaybackStatus.Loading &&
            status != PlaybackStatus.Buffering
    /** Whether the engine has reported active playback. */
    public val isPlaying: Boolean = status == PlaybackStatus.Playing
    /** Normalized progress from zero to one when a duration is known. */
    public val progressFraction: Float
        get() {
            val duration = durationMillis ?: return 0f
            if (duration <= 0L) return 0f
            return (positionMillis.coerceIn(0L, duration).toFloat() /
                    duration.toFloat())
                .coerceIn(0f, 1f)
        }
}

private data class RevisionedShuffleOrder(
    internal val revision: Long = 0L,
    internal val sourceQueueIds: List<String> = emptyList(),
    internal val shuffleMode: ShuffleMode = ShuffleMode.Off,
    internal val occurrenceIds: List<String> = emptyList(),
)

/**
 * Receives lifecycle and transport callbacks from a platform playback engine.
 */
public interface PlaybackEngineListener {
    /** Reports a lifecycle [status] for the specified load [generation]. */
    public fun onPlaybackStatus(generation: Long, status: PlaybackStatus)

    /**
     * Reports position and optional duration for the specified load
     * [generation].
     */
    public fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?
    )

    /** Reports completion of the media associated with [generation]. */
    public fun onPlaybackCompleted(generation: Long)

    /** Reports a playback [error] associated with [generation]. */
    public fun onPlaybackError(generation: Long, error: PlaybackError)

    /** Requests navigation to the next occurrence for [generation]. */
    public fun onSkipToNext(generation: Long)

    /** Requests navigation to the previous occurrence for [generation]. */
    public fun onSkipToPrevious(generation: Long)
}

/** Captures the generation and duration produced by a paused load. */
public data class LoadedPlayback(
    /** Generation that owns the loaded media. */
    public val generation: Long,
    /** Duration reported by the engine, when known. */
    public val durationMillis: Long?,
)

/** Platform-specific engine controlled by [PlaybackController]. */
public interface PlatformPlaybackEngine {
    /** Listener that receives engine lifecycle and transport callbacks. */
    public var listener: PlaybackEngineListener?

    /**
     * Loads [track] without starting it and associates it with [generation].
     */
    public suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback

    /** Clears media and invalidates callbacks from [generation]. */
    public fun clear(generation: Long)

    /** Enables or disables user-initiated transport controls. */
    public fun setUserTransportEnabled(enabled: Boolean)

    /** Starts the currently loaded media. */
    public fun play()

    /** Pauses the currently loaded media. */
    public fun pause()

    /** Stops the currently loaded media. */
    public fun stop()

    /** Moves the current media position to [positionMillis]. */
    public fun seekTo(positionMillis: Long)

    /** Releases engine resources and stops future callbacks. */
    public fun release()
}

internal expect val playbackEngineDispatcher: CoroutineDispatcher

/** Coordinates queue state with one explicitly supplied playback engine. */
public class PlaybackController(
    private val engine: PlatformPlaybackEngine,
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
    /** Publishes immutable playback state to observers. */
    public val state: StateFlow<PlaybackState> = _state.asStateFlow()

    // One process-owned persistence coordinator is the sole consumer. Unlimited
    // buffering keeps
    // synchronous controller methods and platform callbacks non-blocking
    // without dropping order.
    private val checkpointChannel =
        Channel<CheckpointEnvelope>(Channel.UNLIMITED)
    private val checkpointTransportMutex = Mutex()
    private var checkpointCollectorActive = false
    private var checkpointTransportFailure: Throwable? = null
    /** Emits ordered persistence checkpoints to the session owner. */
    public override val checkpoints: Flow<PlaybackCheckpoint> = flow {
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

    /** Replaces the queue and selects the first matching [selectedTrackId]. */
    public fun setQueue(
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

    /** Replaces occurrences and begins loading the selected occurrence. */
    public fun setOccurrenceQueue(
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

    /** Selects the first queue occurrence for [trackId]. */
    public fun selectTrack(trackId: String, autoPlay: Boolean = false) {
        val occurrenceId =
            _state.value.queue.firstOrNull { it.track.id == trackId }?.id
                ?: return
        selectOccurrence(occurrenceId, autoPlay)
    }

    /** Loads [occurrenceId] and optionally starts playback when ready. */
    public fun selectOccurrence(
        occurrenceId: String,
        autoPlay: Boolean = false
    ) {
        if (!commandsEnabled.value) return
        val occurrence = occurrenceById(occurrenceId) ?: return
        resetProgressCheckpointKey()
        loadSelected(occurrence, autoPlay)
        emitImmediateCheckpoint()
    }

    /** Changes the completion behavior and persists the updated session. */
    public fun setRepeatMode(mode: RepeatMode) {
        if (!commandsEnabled.value) return
        val previous = _state.value.repeatMode
        if (previous == mode) return
        val published = publishState { it.copy(repeatMode = mode) }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
        playbackLog.d { "RepeatMode changed: $previous -> $mode" }
    }

    /** Advances to the next supported repeat mode. */
    public fun cycleRepeatMode() {
        if (!commandsEnabled.value) return
        val previous = _state.value.repeatMode
        val next =
            when (previous) {
                RepeatMode.StopAfterQueue -> RepeatMode.RepeatPlaylist
                RepeatMode.RepeatPlaylist -> RepeatMode.RepeatOne
                RepeatMode.RepeatOne -> RepeatMode.StopAfterCurrent
                RepeatMode.StopAfterCurrent -> RepeatMode.StopAfterQueue
            }
        playbackLog.d { "Cycle repeat mode: $previous -> $next" }
        setRepeatMode(next)
    }

    /** Changes queue traversal behavior and persists the updated session. */
    public fun setShuffleMode(mode: ShuffleMode) {
        if (!commandsEnabled.value) return
        val previous = _state.value.shuffleMode
        if (previous == mode) return
        val published = publishState { it.copy(shuffleMode = mode) }
        playbackLog.d { "ShuffleMode changed: $previous -> $mode" }
        publishRuntimeShuffleOrder(published)
        playbackLog.d {
            "Shuffle mode applied, effective order: ${effectiveOrder(published)}"
        }
        emitImmediateCheckpoint(
            published.toSessionSnapshot(), published.checkpointRevision)
    }

    /** Switches between ordered and shuffled queue traversal. */
    public fun toggleShuffleMode() {
        if (!commandsEnabled.value) return
        val previous = _state.value.shuffleMode
        val next =
            when (previous) {
                ShuffleMode.Off -> ShuffleMode.On
                ShuffleMode.On -> ShuffleMode.Off
            }
        playbackLog.d { "Toggle shuffle: $previous -> $next" }
        setShuffleMode(next)
    }

    /** Starts selected media, or requests playback after an active load. */
    public fun play() {
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

    /** Pauses media and emits a persistence checkpoint. */
    public fun pause() {
        if (!commandsEnabled.value) return
        playWhenLoaded = false
        launchEngineAction { engine.pause() }
        emitImmediateCheckpoint()
    }

    /** Stops media and emits a persistence checkpoint. */
    public fun stop() {
        if (!commandsEnabled.value) return
        playWhenLoaded = false
        resetProgressCheckpointKey()
        launchEngineAction { engine.stop() }
        emitImmediateCheckpoint()
    }

    /** Clamps and applies a new playback position. */
    public fun seekTo(positionMillis: Long) {
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

    /** Pauses active playback or starts the selected occurrence. */
    public fun togglePlayPause() {
        if (!commandsEnabled.value) return
        if (_state.value.isPlaying) pause() else play()
    }

    /** Restarts the selected occurrence from position zero. */
    public fun restartCurrentTrack() {
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

    /** Loads the next occurrence, wrapping only for playlist repeat. */
    public fun skipToNext() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        nextTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    /** Loads the previous occurrence, wrapping only for playlist repeat. */
    public fun skipToPrevious() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        previousTrack(wrap)?.let {
            loadSelected(it, autoPlay = true)
            emitImmediateCheckpoint()
        }
    }

    /** Moves an upcoming occurrence to [targetUpcomingIndex]. */
    public suspend fun reorderUpcoming(
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

    /** Removes an upcoming occurrence when it is still present. */
    public suspend fun removeUpcoming(
        occurrenceId: String
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

    /** Removes every occurrence after the selected occurrence. */
    public suspend fun clearUpcoming(): QueueMutationResult =
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

    /** Releases controller resources and stops checkpoint delivery. */
    public fun release() {
        scope.cancel()
        engine.listener = null
        engine.release()
        checkpointChannel.close()
        _state.value = _state.value.copy(status = PlaybackStatus.Stopped)
    }

    /** Enables or disables externally issued playback commands. */
    public override fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled.value = enabled
        engine.setUserTransportEnabled(enabled)
    }

    /** Returns the current queue and transport state for persistence. */
    public override fun sessionSnapshot(): PlaybackSessionSnapshot =
        _state.value.toSessionSnapshot()

    /** Waits until all previously emitted checkpoints are observed. */
    public override suspend fun awaitCheckpointFence() {
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

    /**
     * Restores persisted session state using the currently available [tracks].
     */
    public override suspend fun restoreSession(
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
            playbackLog.e { throwable.stackTraceToString() }
            clearPausedState(snapshot.repeatMode, snapshot.shuffleMode)
        }
        emitImmediateCheckpoint()
        revisionedSessionSnapshot()
    }

    /**
     * Removes unavailable tracks and reconciles the active persisted session.
     */
    public override suspend fun reconcileSession(
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
            playbackLog.e { throwable.stackTraceToString() }
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
            playbackLog.e { throwable.stackTraceToString() }
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

    /** Applies a status callback when it belongs to the active generation. */
    public override fun onPlaybackStatus(
        generation: Long,
        status: PlaybackStatus
    ) {
        if (generation != activeGeneration) return
        _state.value = _state.value.copy(status = status, error = null)
    }

    /**
     * Applies progress and emits at most one checkpoint per playback second.
     */
    public override fun onPlaybackProgress(
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

    /** Advances or stops the queue according to the selected repeat mode. */
    public override fun onPlaybackCompleted(generation: Long) {
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

    /** Publishes an engine error for the active generation. */
    public override fun onPlaybackError(
        generation: Long,
        error: PlaybackError
    ) {
        if (generation != activeGeneration) return
        _state.value =
            _state.value.copy(status = PlaybackStatus.Error, error = error)
    }

    /** Handles an engine request to advance the active queue. */
    public override fun onSkipToNext(generation: Long) {
        if (generation != activeGeneration) return
        skipToNext()
    }

    /** Handles an engine request to return to the previous occurrence. */
    public override fun onSkipToPrevious(generation: Long) {
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

/** In-memory engine for tests and compatibility callers. */
public class FakePlaybackEngine : PlatformPlaybackEngine {
    /** Listener notified by this in-memory engine. */
    public override var listener: PlaybackEngineListener? = null
    private var loaded: PlayableTrack? = null
    private var positionMillis: Long = 0L
    private var durationMillis: Long? = null
    private var generation: Long = 0L
    /** Whether [release] has been invoked. */
    public var released: Boolean = false
        private set

    /** Records a paused load and reports its initial engine state. */
    public override suspend fun loadPaused(
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

    /** Clears the loaded track for [generation]. */
    public override fun clear(generation: Long) {
        this.generation = generation
        loaded = null
        positionMillis = 0L
        durationMillis = null
    }

    /** Ignores user-transport availability in the in-memory engine. */
    public override fun setUserTransportEnabled(enabled: Boolean): Unit = Unit

    /** Reports that the loaded media is playing. */
    public override fun play() {
        requireNotNull(loaded) { "No track loaded" }
        listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
    }

    /** Reports that the loaded media is paused. */
    public override fun pause() {
        listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
    }

    /** Resets position and reports that the loaded media stopped. */
    public override fun stop() {
        positionMillis = 0L
        listener?.onPlaybackProgress(generation, positionMillis, durationMillis)
        listener?.onPlaybackStatus(generation, PlaybackStatus.Stopped)
    }

    /** Updates position and reports it to the listener. */
    public override fun seekTo(positionMillis: Long) {
        this.positionMillis = positionMillis
        listener?.onPlaybackProgress(generation, positionMillis, durationMillis)
    }

    /** Reports a synthetic playback failure to the listener. */
    public fun fail(message: String) {
        listener?.onPlaybackError(generation, PlaybackError(message))
    }

    /** Reports synthetic completion to the listener. */
    public fun complete() {
        listener?.onPlaybackCompleted(generation)
    }

    /** Returns the generation currently owned by this fake engine. */
    public fun activeGenerationForTest(): Long = generation

    /** Marks this engine released and clears its loaded media. */
    public override fun release() {
        released = true
        loaded = null
    }
}
