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
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.coroutines.flow.update
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

/** Classifies a playback failure so callers can offer recovery actions. */
public enum class PlaybackFailureKind {
    /** The underlying media file can no longer be located. */
    MissingFile,
    /** Access to the media file was lost after it was located. */
    AccessLost,
    /** The media format is not supported by the active engine. */
    UnsupportedFormat,
    /** The decoder failed while preparing or decoding the media. */
    DecoderFailure,
    /** The failure does not map to a specific recoverable category. */
    Unknown,
}

/** Describes an error reported while loading or playing audio. */
public data class PlaybackError(
    /** User-visible summary of the playback failure. */
    public val message: String,
    /** Optional underlying failure detail. */
    public val cause: String? = null,
    /** Failure category that gates recovery actions. */
    public val kind: PlaybackFailureKind = PlaybackFailureKind.Unknown,
)

/**
 * Thrown by engines to carry a structured [PlaybackError] to the controller.
 */
public class PlaybackFailureException(
    /** Structured failure transported across the engine boundary. */
    public val error: PlaybackError,
) : IllegalStateException(error.message)

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
    /**
     * Generation that produced the current [error], recorded atomically with
     * its publication so queue-only recovery can bind engine cleanup to the
     * exact failed load instead of a concurrently advanced active generation.
     */
    internal val errorGeneration: Long? = null,
    /** Generation token owned by the current engine selection. */
    internal val engineGeneration: Long = 0L,
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

private data class SelectionRequest(
    val generation: Long,
    val occurrenceId: String,
    val playWhenLoaded: MutableStateFlow<Boolean>,
    val job: Job,
)

/**
 * Blocking mutual-exclusion lock for the selection-request ownership
 * transaction. Common controller code runs request replacement, generation
 * allocation, state claims and callback/settlement mutation inside
 * [withLock]; each platform supplies a real blocking mutex (JVM monitor,
 * Foundation [platform.Foundation.NSLock]) because the common stdlib offers
 * no portable `synchronized` on every target.
 */
internal expect class OwnershipLock() {
    fun <T> withLock(block: () -> T): T
}

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
    // Serializes the selection-request ownership transaction: request-slot
    // installation, generation allocation, the state Loading claim, and the
    // job/autoplay ownership handoff, plus callback and settlement state
    // mutation, happen atomically so a displaced request can never cancel or
    // overwrite a winner. Leaf lock: never acquired while engineMutex or
    // sessionOperationMutex is held, and engine calls always happen outside
    // this critical section.
    private val selectionGate = OwnershipLock()
    // These fields are mutated from the controller dispatcher, the session
    // coordinator actor, and platform engine callback threads. StateFlow
    // provides both atomic updates and cross-thread visibility; a plain field
    // would let a stale engine callback pass the active-generation guard.
    private val selectionRequest = MutableStateFlow<SelectionRequest?>(null)
    private val activeGeneration = MutableStateFlow(0L)
    private val shuffledOrder = MutableStateFlow(RevisionedShuffleOrder())
    private val commandsEnabled = MutableStateFlow(true)
    private val nextCheckpointRevision = MutableStateFlow(0L)
    private val lastProgressCheckpointKey =
        MutableStateFlow<ProgressCheckpointKey?>(null)
    private val occurrenceNamespace: String = uuid4()
    private val nextOccurrenceNumber = MutableStateFlow(0L)
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
            cancelSelectionRequest()
            val generation = nextGeneration()
            resetProgressCheckpointKey()
            launchEngineAction(generation) { engine.clear(generation) }
            val published = publishState { previous ->
                PlaybackState(
                    queue = occurrences,
                    repeatMode = previous.repeatMode,
                    shuffleMode = previous.shuffleMode,
                    engineGeneration = generation,
                )
            }
            emitImmediateCheckpoint(
                published.toSessionSnapshot(), published.checkpointRevision)
        } else {
            if (loadSelected(
                    selected,
                    autoPlay = false,
                    replacementQueue = occurrences,
                )) {
                emitImmediateCheckpoint()
            }
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
        if (loadSelected(occurrence, autoPlay)) {
            emitImmediateCheckpoint()
        }
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
            setPendingAutoplay(true)
            return
        }
        if (_state.value.status == PlaybackStatus.Idle ||
            _state.value.status == PlaybackStatus.Error) {
            loadSelected(current, autoPlay = true)
            return
        }
        launchEngineAction(_state.value.engineGeneration) { engine.play() }
    }

    /** Pauses media and emits a persistence checkpoint. */
    public fun pause() {
        if (!commandsEnabled.value) return
        setPendingAutoplay(false)
        launchEngineAction(_state.value.engineGeneration) { engine.pause() }
        emitImmediateCheckpoint()
    }

    /** Stops media and emits a persistence checkpoint. */
    public fun stop() {
        if (!commandsEnabled.value) return
        setPendingAutoplay(false)
        resetProgressCheckpointKey()
        launchEngineAction(_state.value.engineGeneration) { engine.stop() }
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
        launchEngineAction(_state.value.engineGeneration) {
            engine.seekTo(safePosition)
        }
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
        when (published.status) {
            // Restarting an idle or errored occurrence reloads it through the
            // selection transaction. A concurrent replacement that claims
            // before this admission aborts it, and no stale pre-replacement
            // checkpoint is emitted; when admission succeeds the claimed
            // Loading state is the intended restart checkpoint.
            PlaybackStatus.Idle,
            PlaybackStatus.Error,
            -> if (loadSelected(current, autoPlay = true)) {
                emitImmediateCheckpoint()
            }

            PlaybackStatus.Loading ->
                // Request autoplay on the loading request only while it still
                // owns the reset state; a superseded reset emits nothing.
                if (setPendingAutoplay(true, published.engineGeneration) &&
                    emitPublishedIfStillCurrent(published)) {
                    // The reset checkpoint was emitted under the ownership
                    // lock only while the reset state is still current.
                }

            else ->
                launchEngineAction(published.engineGeneration) {
                    engine.seekTo(0L)
                    engine.play()
                }
                    .let {
                        emitPublishedIfStillCurrent(published)
                    }
        }
    }

    /**
     * Emits the captured [published] checkpoint only while that state is still
     * current, atomically under the ownership lock, so a superseded restart
     * reset can never append a stale checkpoint after a replacement.
     */
    private fun emitPublishedIfStillCurrent(published: PlaybackState): Boolean =
        selectionGate.withLock {
            val current = _state.value
            val stillCurrent =
                current.checkpointRevision == published.checkpointRevision &&
                    current.engineGeneration == published.engineGeneration &&
                    current.currentOccurrenceId ==
                        published.currentOccurrenceId
            if (stillCurrent) {
                emitImmediateCheckpoint(
                    published.toSessionSnapshot(),
                    published.checkpointRevision,
                )
            }
            stillCurrent
        }

    /**
     * Emits an immediate checkpoint of the current state only while it still
     * carries [generation], atomically under the ownership lock.
     */
    private fun emitCheckpointIfOwner(generation: Long) {
        selectionGate.withLock {
            val current = _state.value
            if (current.engineGeneration == generation) {
                emitImmediateCheckpoint(
                    current.toSessionSnapshot(),
                    current.checkpointRevision,
                )
            }
        }
    }

    /** Loads the next occurrence, wrapping only for playlist repeat. */
    public fun skipToNext() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        nextTrack(wrap)?.let {
            if (loadSelected(it, autoPlay = true)) {
                emitImmediateCheckpoint()
            }
        }
    }

    /** Loads the previous occurrence, wrapping only for playlist repeat. */
    public fun skipToPrevious() {
        if (!commandsEnabled.value) return
        val wrap = _state.value.repeatMode == RepeatMode.RepeatPlaylist
        previousTrack(wrap)?.let {
            if (loadSelected(it, autoPlay = true)) {
                emitImmediateCheckpoint()
            }
        }
    }

    /**
     * Reloads the failed current occurrence with autoplay, clearing the
     * reported error once the reload begins.
     */
    public fun retryFailedTrack() {
        val occurrence = failedCurrentOccurrence() ?: return
        if (loadSelected(occurrence, autoPlay = true)) {
            emitImmediateCheckpoint()
        }
    }

    /**
     * Loads the effective successor of the failed current occurrence without
     * repeat wrapping, leaving the failure visible at the effective end.
     */
    public fun skipFailedTrack() {
        if (failedCurrentOccurrence() == null) return
        nextTrack(wrap = false)?.let {
            if (loadSelected(it, autoPlay = true)) {
                emitImmediateCheckpoint()
            }
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

    /**
     * Removes exactly the failed current occurrence and autoplays its
     * pre-removal effective successor. When no successor remains, clears the
     * engine and publishes an idle queue that retains every other occurrence.
     *
     * Rejected as [QueueMutationRejection.CommandsDisabled] while commands are
     * disabled, or [QueueMutationRejection.StaleOccurrence] when the state has
     * no enabled failed current occurrence.
     */
    public suspend fun removeFailedTrack(): QueueMutationResult =
        sessionOperationMutex.withLock {
            while (true) {
                if (!commandsEnabled.value)
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.CommandsDisabled)
                val previous = _state.value
                val current = previous.currentOccurrence
                if (previous.status != PlaybackStatus.Error ||
                    previous.error == null ||
                    current == null) {
                    return@withLock QueueMutationResult.Rejected(
                        QueueMutationRejection.StaleOccurrence)
                }
                val successor = nextTrack(wrap = false)
                val remaining = previous.queue.filterNot { it.id == current.id }
                if (successor != null) {
                    // The prune publishes an engine token no live request owns
                    // so late callbacks from the failed load (still carrying
                    // the error generation) can never mutate the pruned state
                    // before the successor's claim publishes its own token.
                    val published =
                        previous.copy(
                            currentOccurrenceId = null,
                            queue = remaining,
                            positionMillis = 0L,
                            durationMillis = null,
                            error = null,
                            errorGeneration = null,
                            engineGeneration = nextGeneration(),
                            checkpointRevision = reserveCheckpointRevision(),
                        )
                    if (!_state.compareAndSet(previous, published)) continue
                    publishRuntimeShuffleOrder(published, successor.id)
                    emitImmediateCheckpoint(
                        published.toSessionSnapshot(),
                        published.checkpointRevision,
                    )
                    loadSelected(successor, autoPlay = true)
                    return@withLock QueueMutationResult.Applied
                }
                // No successor: clear the engine and publish an idle queue
                // retaining every other occurrence. The idle transition
                // claims the exact failed-load generation: it is CAS-published
                // only from a state still carrying that token, so an aborted
                // retry's unused generation allocation can never block the
                // cleanup, and a replacement that already claimed the session
                // supersedes the removal instead of being cleared.
                val recorded =
                    previous.errorGeneration ?: previous.engineGeneration
                // The idle token is claimed from the allocation counter so it
                // can never equal a concurrent replacement's later token.
                val cleanupGeneration = nextGeneration()
                val idle =
                    previous.copy(
                        currentOccurrenceId = null,
                        queue = remaining,
                        status = PlaybackStatus.Idle,
                        positionMillis = 0L,
                        durationMillis = null,
                        error = null,
                        errorGeneration = null,
                        engineGeneration = cleanupGeneration,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (!_state.compareAndSet(previous, idle)) continue
                selectionGate.withLock {
                    val failedRequest = selectionRequest.value
                    if (failedRequest?.generation == recorded) {
                        failedRequest.job.cancel()
                        if (selectionRequest.value === failedRequest) {
                            selectionRequest.value = null
                        }
                    }
                }
                setPendingAutoplay(false, recorded)
                resetProgressCheckpointKey()
                publishRuntimeShuffleOrder(idle)
                emitImmediateCheckpoint(
                    idle.toSessionSnapshot(), idle.checkpointRevision)
                engineMutex.withLock {
                    if (_state.value.engineGeneration == cleanupGeneration) {
                        engine.clear(cleanupGeneration)
                    }
                }
                return@withLock QueueMutationResult.Applied
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
        cancelSelectionRequest()
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
        val base = _state.value
        val generation = nextGeneration()
        val claimed =
            claimPausedSessionState(
                base,
                reconciledQueue,
                restoredCurrent,
                snapshot.repeatMode,
                snapshot.shuffleMode,
                generation,
            ) ?: return@withLock revisionedSessionSnapshot()
        publishRuntimeShuffleOrder(claimed)
        if (restoredCurrent == null) {
            engineMutex.withLock {
                if (_state.value.engineGeneration == generation) {
                    engine.clear(generation)
                }
            }
            emitImmediateCheckpoint()
            return@withLock revisionedSessionSnapshot()
        }
        try {
            engineMutex.withLock {
                val loaded =
                    engine.loadPaused(
                        restoredCurrent.track.withLazyArtwork(), generation)
                check(loaded.generation == generation)
                val clamped =
                    loaded.durationMillis?.let {
                        restoredPosition.coerceIn(0L, it)
                    } ?: restoredPosition
                engine.seekTo(clamped)
                engine.pause()
                mutateIfOwner(generation, advanceRevision = true) { state ->
                    state.copy(
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
            // Claim the empty paused fail-safe only while this restore still
            // owns the session token; a concurrent winner must never be
            // cleared or clobbered by the failed restore.
            val fallback =
                selectionGate.withLock {
                    val current = _state.value
                    if (current.engineGeneration != generation) {
                        return@withLock null
                    }
                    claimPausedSessionState(
                        current,
                        emptyList(),
                        null,
                        snapshot.repeatMode,
                        snapshot.shuffleMode,
                        nextGeneration(),
                    )
                }
            if (fallback == null) {
                return@withLock revisionedSessionSnapshot()
            }
            publishRuntimeShuffleOrder(fallback)
            engineMutex.withLock {
                if (_state.value.engineGeneration == fallback.engineGeneration) {
                    engine.clear(fallback.engineGeneration)
                }
            }
            if (!emitImmediateCheckpointIfOwner(fallback)) {
                return@withLock revisionedSessionSnapshot()
            }
            return@withLock fallback.toRevisionedSessionSnapshot()
        }
        emitCheckpointIfOwner(generation)
        revisionedSessionSnapshot()
    }

    /**
     * Removes unavailable tracks and reconciles the active persisted session.
     */
    public override suspend fun reconcileSession(
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot = sessionOperationMutex.withLock {
        cancelSelectionRequest()
        resetProgressCheckpointKey()
        val tracksById = tracks.distinctBy { it.id }.associateBy { it.id }
        while (true) {
            val previous = _state.value
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
                // A settled surviving current remains attached to the engine
                // generation that loaded it. Re-tagging the state without
                // reloading the engine would drop all subsequent callbacks
                // (progress, status, and errors) from that engine session.
                // Loading, Buffering, and Error states are invalidated with a
                // fresh token so in-flight or trailing callbacks cannot apply
                // to the reconciled state.
                val published =
                    previous.copy(
                        currentOccurrenceId = current.id,
                        queue = reconciledQueue,
                        status =
                            if (previous.status == PlaybackStatus.Loading ||
                                previous.status == PlaybackStatus.Buffering ||
                                previous.status == PlaybackStatus.Error) {
                                PlaybackStatus.Paused
                            } else {
                                previous.status
                            },
                        error = null,
                        errorGeneration = null,
                        engineGeneration =
                            if (previous.status == PlaybackStatus.Loading ||
                                previous.status == PlaybackStatus.Buffering ||
                                previous.status == PlaybackStatus.Error) {
                                nextGeneration()
                            } else {
                                previous.engineGeneration
                            },
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (!_state.compareAndSet(previous, published)) continue
                publishRuntimeShuffleOrder(published)
                emitImmediateCheckpoint(
                    published.toSessionSnapshot(),
                    published.checkpointRevision,
                )
                return@withLock published.toRevisionedSessionSnapshot()
            }
            val replacement = reconciledQueue.firstOrNull()
            if (replacement == null) {
                // No available occurrence survives: clear the engine and
                // publish an empty paused session from the captured state.
                val generation = nextGeneration()
                val cleared =
                    claimPausedSessionState(
                        previous,
                        emptyList(),
                        null,
                        previous.repeatMode,
                        previous.shuffleMode,
                        generation,
                    ) ?: continue
                publishRuntimeShuffleOrder(cleared)
                engineMutex.withLock {
                    if (_state.value.engineGeneration == generation) {
                        engine.clear(generation)
                    }
                }
                emitImmediateCheckpoint()
                return@withLock revisionedSessionSnapshot()
            }
            // A survivor replaces the missing current: claim it paused with a
            // fresh token, then load it; the settle is CAS-gated on that token
            // so a concurrent replacement can never be overwritten or cleared.
            val generation = nextGeneration()
            val claimed =
                claimPausedSessionState(
                    previous,
                    reconciledQueue,
                    replacement,
                    previous.repeatMode,
                    previous.shuffleMode,
                    generation,
                ) ?: return@withLock revisionedSessionSnapshot()
            publishRuntimeShuffleOrder(claimed, replacement.id)
            try {
                engineMutex.withLock {
                    val loaded =
                        engine.loadPaused(
                            replacement.track.withLazyArtwork(), generation)
                    check(loaded.generation == generation)
                    engine.seekTo(0L)
                    engine.pause()
                    mutateIfOwner(generation, advanceRevision = true) { state ->
                        state.copy(
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
                // Publish the empty paused fail-safe only while this reconcile
                // still owns the session token; a concurrent winner must never
                // be cleared or clobbered before the failure propagates.
                val failSafe =
                    selectionGate.withLock {
                        val current = _state.value
                        if (current.engineGeneration != generation) {
                            return@withLock null
                        }
                        claimPausedSessionState(
                            current,
                            emptyList(),
                            null,
                            previous.repeatMode,
                            previous.shuffleMode,
                            nextGeneration(),
                        )
                    }
                if (failSafe != null) {
                    publishRuntimeShuffleOrder(failSafe)
                    engineMutex.withLock {
                        if (_state.value.engineGeneration ==
                            failSafe.engineGeneration) {
                            engine.clear(failSafe.engineGeneration)
                        }
                    }
                    if (!emitImmediateCheckpointIfOwner(failSafe)) {
                        throw throwable
                    }
                }
                throw throwable
            }
            emitCheckpointIfOwner(generation)
            return@withLock revisionedSessionSnapshot()
        }
        error("Unreachable reconcile loop")
    }

    /**
     * Begins loading [occurrence] (autoplaying when [autoPlay] is set) and
     * returns true when the selection was admitted. The whole ownership
     * transfer is one critical section under [selectionGate]: request-slot
     * replacement (capturing the displaced request), generation allocation,
     * the Loading state claim that publishes the engine-generation token from
     * the exact captured state, and the job/autoplay ownership handoff. A
     * claim that loses restores the displaced request and cancels only its
     * own lazy Job; a claim that wins cancels the captured displaced request
     * (never a reread) and starts only its own Job. Engine calls happen on the
     * lazy Job outside this lock, so engine re-entrant callbacks can never
     * deadlock the ownership transaction.
     */
    private fun loadSelected(
        occurrence: QueueOccurrence,
        autoPlay: Boolean,
        replacementQueue: List<QueueOccurrence>? = null,
        from: PlaybackState? = null,
        requireCommandsEnabled: Boolean = false,
    ): Boolean = selectionGate.withLock {
        if (requireCommandsEnabled && !commandsEnabled.value) {
            return@withLock false
        }
        val displaced = selectionRequest.value
        val generation = nextGeneration()
        val intent = MutableStateFlow(autoPlay)
        // Lazy: nothing runs until the claim wins and starts the Job.
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                settleLoad(occurrence, generation, intent)
            }
        val request =
            SelectionRequest(
                generation,
                occurrence.id,
                intent,
                job,
            )
        // Request replacement precedes the state claim so a concurrent
        // transaction can always cancel the request it displaces.
        selectionRequest.value = request
        val claimed =
            if (from == null) {
                claimLoading(occurrence, replacementQueue, generation)
            } else {
                claimLoadingFrom(from, occurrence, generation)
            }
        if (claimed == null) {
            // The claim lost (the occurrence left the queue or the captured
            // prior state moved). Restore the displaced request and cancel
            // only this transaction's own lazy Job.
            selectionRequest.value = displaced
            request.job.cancel()
            return@withLock false
        }
        resetProgressCheckpointKey()
        publishRuntimeShuffleOrder(claimed, occurrence.id)
        // Ownership transferred: supersede the captured displaced request.
        displaced?.job?.cancel()
        request.job.start()
        true
    }

    /**
     * Selects [occurrence] from the exact captured [PlaybackState]: the claim
     * publishes only when that captured state is still current, so callers
     * that derived a decision from it never commit against a newer state.
     */
    private fun loadSelectedFrom(
        captured: PlaybackState,
        occurrence: QueueOccurrence,
        autoPlay: Boolean,
    ): Boolean = loadSelected(occurrence, autoPlay, from = captured)

    private fun claimLoadingFrom(
        from: PlaybackState,
        occurrence: QueueOccurrence,
        generation: Long,
    ): PlaybackState? {
        if (from.queue.none { it.id == occurrence.id }) return null
        val updated =
            from.copy(
                currentOccurrenceId = occurrence.id,
                status = PlaybackStatus.Loading,
                positionMillis = 0L,
                durationMillis = occurrence.track.durationMillis,
                error = null,
                engineGeneration = generation,
                checkpointRevision = reserveCheckpointRevision(),
            )
        return if (_state.compareAndSet(from, updated)) updated else null
    }

    /**
     * CAS-publishes the Loading transition for [occurrence] from the exact
     * captured state under [selectionGate], returning the published state or
     * null when the state the transition would originate from no longer
     * contains the occurrence. Retries against benign concurrent writers that
     * are not selection claims (engine callbacks and transport publishes).
     */
    private fun claimLoading(
        occurrence: QueueOccurrence,
        replacementQueue: List<QueueOccurrence>?,
        generation: Long,
    ): PlaybackState? {
        while (true) {
            val previous = _state.value
            if (replacementQueue == null &&
                previous.queue.none { it.id == occurrence.id }) return null
            val updated =
                (if (replacementQueue == null) previous else
                    previous.copy(queue = replacementQueue)).copy(
                    currentOccurrenceId = occurrence.id,
                    status = PlaybackStatus.Loading,
                    positionMillis = 0L,
                    durationMillis = occurrence.track.durationMillis,
                    error = null,
                    engineGeneration = generation,
                    checkpointRevision = reserveCheckpointRevision(),
                )
            if (_state.compareAndSet(previous, updated)) return updated
        }
    }

    private suspend fun settleLoad(
        occurrence: QueueOccurrence,
        generation: Long,
        intent: MutableStateFlow<Boolean>,
    ) {
        val trackWithArtwork = occurrence.track.withLazyArtwork()
        runEngineAction(generation) {
            if (!ownsSelection(generation, occurrence.id))
                return@runEngineAction
            val loaded = engine.loadPaused(trackWithArtwork, generation)
            check(loaded.generation == generation)
            // Settle only while this request still owns the state token.
            mutateIfOwner(generation) { state ->
                state.copy(
                    status = PlaybackStatus.Paused,
                    durationMillis =
                        loaded.durationMillis ?: state.durationMillis,
                )
            } ?: return@runEngineAction
            var shouldPlay = false
            selectionGate.withLock {
                val state = _state.value
                if (ownsSelection(state, generation, occurrence.id) &&
                    intent.value) {
                    intent.value = false
                    shouldPlay = true
                }
            }
            // Autoplay after load: engine dispatch happens outside the
            // ownership lock; the decision above was captured atomically.
            if (shouldPlay) engine.play()
        }
    }

    /**
     * Applies [transform] under the ownership lock while the state still
     * carries [generation], returning the applied state or null when a newer
     * request superseded it. Called from engine callbacks and load settlement
     * while the caller may hold the engine mutex; the ownership lock is a leaf
     * lock, so this ordering can never deadlock a selection transaction.
     */
    private fun mutateIfOwner(
        generation: Long,
        advanceRevision: Boolean = false,
        transform: (PlaybackState) -> PlaybackState,
    ): PlaybackState? = selectionGate.withLock {
        while (true) {
            val previous = _state.value
            if (previous.engineGeneration != generation) {
                return@withLock null
            }
            val updated =
                if (advanceRevision) {
                    transform(previous)
                        .copy(checkpointRevision = reserveCheckpointRevision())
                } else {
                    transform(previous)
                }
            if (_state.compareAndSet(previous, updated)) {
                return@withLock updated
            }
        }
        error("Unreachable ownership mutation")
    }

    private fun ownsSelection(
        generation: Long,
        occurrenceId: String,
    ): Boolean = ownsSelection(_state.value, generation, occurrenceId)

    private fun ownsSelection(
        state: PlaybackState,
        generation: Long,
        occurrenceId: String,
    ): Boolean =
        state.engineGeneration == generation &&
            state.currentOccurrenceId == occurrenceId

    private fun nextGeneration(): Long {
        while (true) {
            val previous = activeGeneration.value
            val next = previous + 1L
            if (activeGeneration.compareAndSet(previous, next)) return next
        }
    }

    /**
     * Claims [queue]/[current] and the repeat/shuffle modes as a paused
     * session state carrying [generation], CAS-published from the exact
     * captured [base]. Returns null when a concurrent transition superseded
     * [base], in which case the caller must not apply any engine effect.
     */
    private fun claimPausedSessionState(
        base: PlaybackState,
        queue: List<QueueOccurrence>,
        current: QueueOccurrence?,
        repeatMode: RepeatMode,
        shuffleMode: ShuffleMode,
        generation: Long,
    ): PlaybackState? {
        val updated =
            base.copy(
                currentOccurrenceId = current?.id,
                queue = queue,
                status = PlaybackStatus.Paused,
                positionMillis = 0L,
                durationMillis = current?.track?.durationMillis,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                error = null,
                errorGeneration = null,
                engineGeneration = generation,
                checkpointRevision = reserveCheckpointRevision(),
            )
        return if (_state.compareAndSet(base, updated)) updated else null
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

    /** Emits [state]'s checkpoint only while that exact state remains current. */
    private fun emitImmediateCheckpointIfOwner(state: PlaybackState): Boolean =
        selectionGate.withLock {
            if (_state.value !== state) return@withLock false
            emitImmediateCheckpoint(
                state.toSessionSnapshot(), state.checkpointRevision)
            true
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
        lastProgressCheckpointKey.value = null
    }

    private fun cancelSelectionRequest() {
        selectionGate.withLock {
            selectionRequest.value?.let { request ->
                // Clear the request-owned autoplay intent (the per-request
                // equivalent of the old shared playWhenLoaded=false) so a
                // cancelled load that slips past cancellation can never start
                // playback against a superseding session state.
                request.playWhenLoaded.value = false
                request.job.cancel()
            }
            selectionRequest.value = null
        }
    }

    private fun setPendingAutoplay(
        enabled: Boolean,
        generation: Long? = null,
    ): Boolean = selectionGate.withLock {
        val request =
            when {
                generation == null -> selectionRequest.value
                else ->
                    selectionRequest.value?.takeIf {
                        it.generation == generation
                    }
            } ?: return@withLock false
        val state = _state.value
        val owned =
            state.status == PlaybackStatus.Loading &&
                ownsSelection(
                    state, request.generation, request.occurrenceId)
        if (owned) {
            request.playWhenLoaded.value = enabled
        }
        owned
    }

    private fun PlayableTrack.withLazyArtwork(): PlayableTrack {
        if (artworkBytes != null) return this
        val loadedArtwork = artworkLoader(id) ?: return this
        return copy(artworkBytes = loadedArtwork)
    }

    private fun launchEngineAction(
        errorGeneration: Long,
        action: suspend () -> Unit,
    ) {
        scope.launch {
            runEngineAction(errorGeneration, action)
        }
    }

    private suspend fun runEngineAction(
        errorGeneration: Long,
        action: suspend () -> Unit,
    ) {
        try {
            engineMutex.withLock {
                action()
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            playbackLog.e { throwable.stackTraceToString() }
            val failure =
                (throwable as? PlaybackFailureException)?.error
                    ?: PlaybackError(
                        message = "Playback failed",
                        cause =
                            throwable.message ?: throwable::class.simpleName,
                    )
            onPlaybackError(
                errorGeneration,
                failure,
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

    /** Successor of the current occurrence within [state]'s effective order. */
    private fun nextTrackFrom(
        state: PlaybackState,
        wrap: Boolean,
    ): QueueOccurrence? {
        val order = effectiveOrder(state)
        if (order.isEmpty()) return null
        val currentIndex = order.indexOf(state.currentOccurrenceId)
        if (currentIndex < 0) return null
        val nextId =
            order.getOrNull(currentIndex + 1)
                ?: if (wrap) order.firstOrNull() else null
        return state.queue.firstOrNull { it.id == nextId }
    }

    /** Predecessor of the current occurrence within [state]'s order. */
    private fun previousTrackFrom(
        state: PlaybackState,
        wrap: Boolean,
    ): QueueOccurrence? {
        val order = effectiveOrder(state)
        if (order.isEmpty()) return null
        val currentIndex = order.indexOf(state.currentOccurrenceId)
        if (currentIndex < 0) return null
        val previousId =
            order.getOrNull(currentIndex - 1)
                ?: if (wrap) order.lastOrNull() else null
        return state.queue.firstOrNull { it.id == previousId }
    }

    /**
     * Returns the current occurrence only while recovery commands are enabled
     * and the latest state reports an error with a structured cause.
     */
    private fun failedCurrentOccurrence(): QueueOccurrence? =
        _state.value
            .takeIf {
                commandsEnabled.value &&
                    it.status == PlaybackStatus.Error &&
                    it.error != null
            }
            ?.currentOccurrence

    /**
     * Publishes the terminal Stopped state and its checkpoint as one critical
     * section under the ownership lock, so a replacement claiming right after
     * the mutation can never receive a stale Stopped checkpoint out of order.
     */
    private fun stopAtCurrentTrackEnd(generation: Long): Boolean =
        selectionGate.withLock {
            var published: PlaybackState? = null
            while (published == null) {
                val state = _state.value
                if (state.engineGeneration != generation) return@withLock false
                val candidate =
                    state.copy(
                        status = PlaybackStatus.Stopped,
                        positionMillis =
                            state.durationMillis
                                ?: max(0L, state.positionMillis),
                        error = null,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (_state.compareAndSet(state, candidate)) {
                    published = candidate
                }
            }
            resetProgressCheckpointKey()
            emitImmediateCheckpoint(
                published.toSessionSnapshot(),
                published.checkpointRevision,
            )
            true
        }

    /** Applies a status callback only while [generation] owns the state. */
    public override fun onPlaybackStatus(
        generation: Long,
        status: PlaybackStatus
    ) {
        selectionGate.withLock {
            _state.update { state ->
                if (state.engineGeneration == generation) {
                    state.copy(status = status, error = null)
                } else {
                    state
                }
            }
        }
    }

    /**
     * Applies progress while [generation] owns the state and emits at most one
     * checkpoint per playback second. The apply, the per-second key dedupe and
     * the emission are one critical section under the ownership lock, so a
     * stale or duplicate progress callback can neither mutate a replacement's
     * state nor append a checkpoint after the replacement claimed.
     */
    public override fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?
    ) {
        selectionGate.withLock {
            var applied: PlaybackState? = null
            while (applied == null) {
                val state = _state.value
                if (state.engineGeneration != generation) return@withLock
                val candidate =
                    state.copy(
                        positionMillis = max(0L, positionMillis),
                        durationMillis =
                            durationMillis ?: state.durationMillis,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (_state.compareAndSet(state, candidate)) {
                    applied = candidate
                }
            }
            val settled = checkNotNull(applied)
            val currentId =
                settled.currentOccurrenceId ?: return@withLock
            if (settled.status != PlaybackStatus.Playing) return@withLock
            val key =
                ProgressCheckpointKey(
                    generation,
                    currentId,
                    max(0L, positionMillis) / 1_000L)
            if (lastProgressCheckpointKey.value == key) return@withLock
            lastProgressCheckpointKey.value = key
            check(
                checkpointChannel
                    .trySend(
                        CheckpointEnvelope.Checkpoint(
                            PlaybackCheckpoint.PlayingProgress(
                                key = key,
                                snapshot = settled.toSessionSnapshot(),
                                revision = settled.checkpointRevision,
                            ),
                        ),
                    )
                    .isSuccess,
            )
        }
    }

    /**
     * Advances or stops the queue according to the selected repeat mode.
     * Every decision derives from a state that still carries [generation] and
     * is committed from that exact captured state, so a stale completion can
     * neither advance a replacement's queue nor stop its session.
     */
    public override fun onPlaybackCompleted(generation: Long) {
        while (true) {
            val state = _state.value
            if (state.engineGeneration != generation) return
            val committed =
                when (state.repeatMode) {
                    RepeatMode.RepeatOne -> {
                        val current =
                            state.currentOccurrence
                        if (current == null) {
                            stopAtCurrentTrackEnd(generation)
                        } else if (loadSelectedFrom(
                                state, current, autoPlay = true)) {
                            emitImmediateCheckpoint()
                            true
                        } else {
                            false
                        }
                    }

                    RepeatMode.RepeatPlaylist -> {
                        val next = nextTrackFrom(state, wrap = true)
                        if (next == null) {
                            stopAtCurrentTrackEnd(generation)
                        } else if (loadSelectedFrom(
                                state, next, autoPlay = true)) {
                            emitImmediateCheckpoint()
                            true
                        } else {
                            false
                        }
                    }

                    RepeatMode.StopAfterCurrent ->
                        stopAtCurrentTrackEnd(generation)

                    RepeatMode.StopAfterQueue -> {
                        val next = nextTrackFrom(state, wrap = false)
                        if (next == null) {
                            stopAtCurrentTrackEnd(generation)
                        } else if (loadSelectedFrom(
                                state, next, autoPlay = true)) {
                            emitImmediateCheckpoint()
                            true
                        } else {
                            false
                        }
                    }
                }
            if (committed) return
            // The claim or stop lost to a benign concurrent writer; re-derive
            // from the latest state. A superseding claim exits at the top.
        }
    }

    /** Publishes an engine error only while [generation] owns the state. */
    public override fun onPlaybackError(
        generation: Long,
        error: PlaybackError
    ) {
        selectionGate.withLock {
            _state.update { state ->
                if (state.engineGeneration == generation) {
                    state.copy(
                        status = PlaybackStatus.Error,
                        error = error,
                        errorGeneration = generation,
                    )
                } else {
                    state
                }
            }
        }
    }

    /**
     * Handles an engine request to advance the active queue. The skip derives
     * from a state that still carries [generation] and is committed from that
     * exact captured state, so a stale transport request can never advance a
     * replacement's queue.
     */
    public override fun onSkipToNext(generation: Long) {
        skipOwner(generation, previous = false)
    }

    /** Handles an engine request to return to the previous occurrence. */
    public override fun onSkipToPrevious(generation: Long) {
        skipOwner(generation, previous = true)
    }

    private fun skipOwner(generation: Long, previous: Boolean) {
        while (true) {
            if (!commandsEnabled.value) return
            val state = _state.value
            if (state.engineGeneration != generation) return
            val wrap = state.repeatMode == RepeatMode.RepeatPlaylist
            val target =
                if (previous) {
                    previousTrackFrom(state, wrap)
                } else {
                    nextTrackFrom(state, wrap)
                }
            if (target == null) return
            if (loadSelected(
                    target,
                    autoPlay = true,
                    from = state,
                    requireCommandsEnabled = true,
                )) {
                emitImmediateCheckpoint()
                return
            }
            // The claim lost to a benign writer; re-derive from the latest
            // state. A superseding claim exits at the top of the loop.
        }
    }

    private fun freshOccurrenceId(): String {
        while (true) {
            val previous = nextOccurrenceNumber.value
            val next = previous + 1L
            if (nextOccurrenceNumber.compareAndSet(previous, next)) {
                return "queue-$occurrenceNamespace-$next"
            }
        }
    }
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
