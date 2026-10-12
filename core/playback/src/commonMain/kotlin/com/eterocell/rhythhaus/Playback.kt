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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
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

/** Records an occurrence whose generation reached actual playback. */
public data class PlaybackStarted(
    /** Generation that owns the playing occurrence. */
    public val generation: Long,
    /** Queue occurrence that reached actual playback. */
    public val occurrenceId: String,
    /** Track assigned to [occurrenceId]. */
    public val trackId: String,
)

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

private data class PlaybackStartedKey(
    val generation: Long,
    val occurrenceId: String,
)

private data class SleepTimerCompletionKey(
    val token: Long,
    val generation: Long,
    val occurrenceId: String,
    val playbackRunId: Long,
)

private data class ArmedSleepTimer(
    val token: Long,
    val mode: SleepTimerMode,
    val deadlineMillis: Long? = null,
    var remainingTracks: Int? = null,
    val fadeEnabled: Boolean,
    var lastPublishedRemainingMillis: Long? = null,
    var consumedCompletionKey: SleepTimerCompletionKey? = null,
)

private data class TimerStopClaim(
    val token: Long,
    val generation: Long,
)

private data class PlaybackGainRequest(
    val version: Long,
    val generation: Long?,
    val gain: Float,
    val crossfadeToken: Long? = null,
    val outgoingGain: Float? = null,
)

private data class CrossfadeAttemptKey(
    val generation: Long,
    val playbackRunId: Long,
    val successorId: String,
)

private class CrossfadeTransition(
    val outgoing: QueueOccurrence,
    val outgoingGeneration: Long,
    var outgoingRunId: Long,
    val incoming: QueueOccurrence,
    val incomingGeneration: Long,
    val outgoingDurationMillis: Long,
    var outgoingPositionMillis: Long,
    var outgoingPositionUpdatedAtMillis: Long,
) {
    var incomingDurationMillis: Long? = null
    var incomingRunId: Long = 0L
    var incomingPositionMillis: Long = 0L
    var incomingPositionUpdatedAtMillis: Long = 0L
    var outgoingAdvancing: Boolean = true
    var incomingAdvancing: Boolean = false
    var prepared: Boolean = false
    var started: Boolean = false
    var promoted: Boolean = false
    var outgoingCompleted: Boolean = false
    var outgoingCompletionTimerToken: Long? = null
    var windowMillis: Long = 0L
    var preparationJob: Job? = null
    var tickJob: Job? = null
}

private class GaplessTransition(
    val outgoing: QueueOccurrence,
    val outgoingGeneration: Long,
    val outgoingRunId: Long,
    val incoming: QueueOccurrence,
    val incomingGeneration: Long,
) {
    var incomingDurationMillis: Long? = null
    var preparationError: PlaybackError? = null
    var prepared: Boolean = false
    var preparationJob: Job? = null
    var started: Boolean = false
    var promoted: Boolean = false
    var incomingRunId: Long = 0L
    var outgoingCompleted: Boolean = false
}

private const val gaplessLeadWindowMillis: Long = 2_000L

private data class TimerStopRequest(
    val token: Long,
    val generation: Long,
    val positionAtTrackEnd: Boolean,
)

private data class TimedTimerWakeup(
    val delayMillis: Long,
    val gainRequest: PlaybackGainRequest?,
    val stopRequest: TimerStopRequest?,
)

/**
 * Blocking mutual-exclusion lock for the selection-request ownership
 * transaction. Common controller code runs request replacement, generation
 * allocation, state claims and callback/settlement mutation inside [withLock];
 * each platform supplies a real blocking mutex (JVM monitor, Foundation
 * [platform.Foundation.NSLock]) because the common stdlib offers no portable
 * `synchronized` on every target.
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

    /** Reports natural completion of the media and playback run. */
    public fun onPlaybackCompleted(generation: Long, playbackRunId: Long)

    /** Reports a playback [error] associated with [generation]. */
    public fun onPlaybackError(generation: Long, error: PlaybackError)

    /** Requests navigation to the next occurrence for [generation]. */
    public fun onSkipToNext(generation: Long)

    /** Requests navigation to the previous occurrence for [generation]. */
    public fun onSkipToPrevious(generation: Long)

    /**
     * Requests a controller-owned playback run for an external Play command.
     */
    public fun onPlayRequested(generation: Long)

    /** Requests controller-owned Pause for an external transport. */
    public fun onPauseRequested(generation: Long)

    /** Requests controller-owned Stop for an external transport. */
    public fun onStopRequested(generation: Long)

    /** Requests controller-owned seeking for an external transport. */
    public fun onSeekRequested(generation: Long, positionMillis: Long)
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

    /**
     * Applies app playback gain without changing device or system volume.
     *
     * Implementations MUST reject a non-finite value or a value outside the
     * inclusive range from `0f` through `1f`.
     */
    public fun setPlaybackGain(gain: Float)

    /** Starts the currently loaded media for [playbackRunId]. */
    public fun play(playbackRunId: Long)

    /**
     * Refreshes an existing media label without changing its audio or position.
     */
    public fun refreshLoadedMetadata(
        track: PlayableTrack,
        generation: Long
    ): Boolean = false

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
public class PlaybackController
internal constructor(
    private val engine: PlatformPlaybackEngine,
    private val shuffleOrderFactory: (List<String>, String?) -> List<String>,
    private val artworkLoader: (String) -> ByteArray?,
    private val sleepTimerRuntime: SleepTimerRuntime,
) : PlaybackEngineListener, PlaybackSessionController {
    /** Creates a controller with process-monotonic sleep-timer time. */
    public constructor(
        engine: PlatformPlaybackEngine,
        shuffleOrderFactory: (List<String>, String?) -> List<String> =
            ::defaultShuffleOrder,
        artworkLoader: (String) -> ByteArray? = { null },
    ) : this(
        engine,
        shuffleOrderFactory,
        artworkLoader,
        MonotonicSleepTimerRuntime,
    )

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
    private val _sleepTimerState = MutableStateFlow(SleepTimerState())
    /** Publishes the process-local authoritative sleep timer projection. */
    public val sleepTimerState: StateFlow<SleepTimerState> =
        _sleepTimerState.asStateFlow()

    // Timer ownership lives under selectionGate with generation claims. The
    // timer itself never holds that gate while invoking a platform engine.
    private var nextSleepTimerToken: Long = 0L
    private var activeSleepTimer: ArmedSleepTimer? = null
    private var sleepTimerJob: Job? = null
    private var timerStopClaim: TimerStopClaim? = null
    private var replayGainMode: ReplayGainMode = ReplayGainMode.Off
    private var sleepAttenuation: Float = 1f
    private var desiredPlaybackGain: Float = 1f
    private var desiredOutgoingCrossfadeGain: Float? = null
    private var desiredCrossfadeToken: Long? = null
    private var desiredPlaybackGainGeneration: Long? = null
    private var playbackGainVersion: Long = 0L
    private var playbackGainMayBeAttenuated: Boolean = false
    private var released: Boolean = false
    private var nextPlaybackRunId = 0L
    private var activePlaybackRunId: Long? = null
    private var activePlaybackRunGeneration: Long? = null
    private val crossfadeEngine = engine as? CrossfadePlaybackEngine
    private val crossfadeRuntime = CrossfadeRuntime(sleepTimerRuntime)
    private var crossfadeDuration: CrossfadeDuration = CrossfadeDuration.Off
    private var gaplessEnabled: Boolean = false
    private var gaplessTransition: GaplessTransition? = null
    private var nativeGaplessTransition: GaplessTransition? = null
    // Transaction fields are gate-owned; the native slot is engine-mutex-owned.
    private var crossfadeTransition: CrossfadeTransition? = null
    private var nativeCrossfadeTransition: CrossfadeTransition? = null
    private var lastCrossfadeAttempt: CrossfadeAttemptKey? = null
    private var lastDeferredCompletionGeneration: Long? = null
    private var lastDeferredCompletionRunId: Long? = null

    // Unlimited buffering preserves events while keeping platform callbacks
    // non-blocking.
    private val playbackStartedChannel =
        Channel<PlaybackStarted>(Channel.UNLIMITED)
    private var lastPlaybackStartedKey: PlaybackStartedKey? = null
    /**
     * Emits each generation and queue occurrence that reaches actual playback
     * once.
     */
    public val playbackStarted: Flow<PlaybackStarted> =
        playbackStartedChannel.receiveAsFlow()

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

    /** Arms a monotonic elapsed-time sleep timer for positive [minutes]. */
    public fun armSleepTimer(minutes: Int, fadeEnabled: Boolean) {
        require(minutes > 0) { "Sleep-timer minutes must be positive" }
        if (!commandsEnabled.value) return
        val durationMillis = minutes.toLong() * 60_000L
        var job: Job? = null
        var gainRequest: PlaybackGainRequest? = null
        selectionGate.withLock {
            if (released) return@withLock
            cancelCrossfadeLocked()
            sleepTimerJob?.cancel()
            val timer =
                ArmedSleepTimer(
                    token = nextSleepTimerTokenLocked(),
                    mode = SleepTimerMode.Timed,
                    deadlineMillis =
                        saturatedDeadline(
                            sleepTimerRuntime.nowMillis(), durationMillis),
                    fadeEnabled = fadeEnabled,
                    lastPublishedRemainingMillis = durationMillis,
                )
            activeSleepTimer = timer
            _sleepTimerState.value = timer.toState(durationMillis)
            gainRequest =
                setSleepAttenuationLocked(
                    gain = 1f,
                    generation = _state.value.engineGeneration,
                    force = true,
                )
            job =
                scope.launch(start = CoroutineStart.LAZY) {
                    runTimedSleepTimer(timer.token)
                }
            sleepTimerJob = job
        }
        gainRequest?.let(::launchPlaybackGain)
        job?.start()
    }

    /** Arms a natural-completion sleep timer for positive [count]. */
    public fun armSleepTimerAfterCompletions(count: Int, fadeEnabled: Boolean) {
        require(count > 0) { "Sleep-timer completion count must be positive" }
        if (!commandsEnabled.value) return
        var gainRequest: PlaybackGainRequest? = null
        selectionGate.withLock {
            if (released) return@withLock
            cancelCrossfadeLocked()
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            val timer =
                ArmedSleepTimer(
                    token = nextSleepTimerTokenLocked(),
                    mode = SleepTimerMode.TrackCount,
                    remainingTracks = count,
                    fadeEnabled = fadeEnabled,
                )
            activeSleepTimer = timer
            _sleepTimerState.value = timer.toState()
            gainRequest =
                setSleepAttenuationLocked(
                    gain = 1f,
                    generation = _state.value.engineGeneration,
                    force = true,
                )
        }
        gainRequest?.let(::launchPlaybackGain)
    }

    /** Cancels the active sleep timer without changing playback selection. */
    public fun cancelSleepTimer() {
        if (!commandsEnabled.value) return
        cancelSleepTimerAndRestoreGain()
    }

    /** Changes normalization without changing transport or sleep ownership. */
    public fun setReplayGainMode(mode: ReplayGainMode) {
        val request = selectionGate.withLock {
            if (released || replayGainMode == mode) return@withLock null
            replayGainMode = mode
            recomputeDesiredPlaybackGainLocked(_state.value.engineGeneration)
        }
        request?.let(::launchPlaybackGain)
    }

    /** Selects process-local overlap policy without changing the selection. */
    public fun setCrossfadeDuration(duration: CrossfadeDuration) {
        selectionGate.withLock {
            if (released || crossfadeDuration == duration) return@withLock
            crossfadeDuration = duration
            if (duration != CrossfadeDuration.Off) cancelGaplessLocked()
            revalidateCrossfadeLocked()
            considerCrossfadeLocked()
        }
    }

    /**
     * Selects process-local gapless queue policy without changing selection.
     */
    public fun setGaplessEnabled(enabled: Boolean) {
        selectionGate.withLock {
            if (released || gaplessEnabled == enabled) return@withLock
            gaplessEnabled = enabled
            if (!enabled) cancelGaplessLocked()
            revalidateCrossfadeLocked()
            considerCrossfadeLocked()
        }
    }

    private fun gaplessPolicyEligibleLocked(state: PlaybackState): Boolean =
        !released &&
            gaplessEnabled &&
            crossfadeEngine != null &&
            crossfadeDuration == CrossfadeDuration.Off &&
            activeSleepTimer?.mode != SleepTimerMode.Timed &&
            timerStopClaim == null &&
            state.status == PlaybackStatus.Playing &&
            state.repeatMode != RepeatMode.RepeatOne &&
            state.repeatMode != RepeatMode.StopAfterCurrent

    private fun ownsGaplessLocked(transition: GaplessTransition): Boolean {
        if (gaplessTransition !== transition) return false
        val state = _state.value
        if (!gaplessPolicyEligibleLocked(state) ||
            state.engineGeneration != transition.outgoingGeneration ||
            state.currentOccurrenceId != transition.outgoing.id)
            return false
        return nextTrackFrom(
                state,
                wrap = state.repeatMode == RepeatMode.RepeatPlaylist,
                currentId = transition.outgoing.id,
            )
            ?.id == transition.incoming.id
    }

    private fun cancelGaplessLocked() {
        val transition = gaplessTransition ?: return
        gaplessTransition = null
        transition.preparationJob?.cancel()
        launchNonFatalEngineAction {
            val native = nativeGaplessTransition
            if (native === transition) {
                nativeGaplessTransition = null
                if (!transition.promoted)
                    crossfadeEngine?.cancelPreparedIncoming()
            }
        }
    }

    private fun considerGaplessLocked() {
        val state = _state.value
        if (state.status != PlaybackStatus.Playing) return
        if (state.durationMillis == null) return
        val remaining = state.durationMillis - state.positionMillis
        if (remaining > gaplessLeadWindowMillis || remaining <= 0L) return
        if (crossfadeDuration != CrossfadeDuration.Off) return
        val active = gaplessTransition
        if (active != null) {
            if (!ownsGaplessLocked(active)) cancelGaplessLocked()
            return
        }
        if (!gaplessPolicyEligibleLocked(state)) return
        val current = state.currentOccurrence ?: return
        val successor =
            nextTrackFrom(
                state,
                wrap = state.repeatMode == RepeatMode.RepeatPlaylist,
            ) ?: return
        if (successor.id == current.id) return
        val run =
            activePlaybackRunId?.takeIf {
                activePlaybackRunGeneration == state.engineGeneration
            } ?: return
        val transition =
            GaplessTransition(
                current,
                state.engineGeneration,
                run,
                successor,
                nextGeneration())
        gaplessTransition = transition
        transition.preparationJob =
            scope
                .launch(start = CoroutineStart.LAZY) {
                    prepareGapless(transition)
                }
                .also { it.start() }
    }

    private suspend fun prepareGapless(transition: GaplessTransition) {
        val capable = crossfadeEngine ?: return
        try {
            engineMutex.withLock {
                if (!selectionGate.withLock { ownsGaplessLocked(transition) })
                    return@withLock
                nativeGaplessTransition = transition
                val loaded =
                    capable.prepareIncoming(
                        transition.incoming.track.withLazyArtwork(),
                        transition.incomingGeneration,
                    )
                check(loaded.generation == transition.incomingGeneration)
                selectionGate.withLock {
                    if (ownsGaplessLocked(transition)) {
                        val duration =
                            loaded.durationMillis
                                ?: transition.incoming.track.durationMillis
                        if (duration == null) cancelGaplessLocked()
                        else {
                            transition.incomingDurationMillis = duration
                            transition.prepared = true
                        }
                    }
                }
            }
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            selectionGate.withLock {
                if (ownsGaplessLocked(transition)) {
                    transition.preparationError =
                        (failure as? PlaybackFailureException)?.error
                            ?: PlaybackError(
                                "Playback failed",
                                failure.message ?: failure::class.simpleName)
                    transition.prepared = false
                }
            }
        }
    }

    private fun crossfadePolicyEligibleLocked(state: PlaybackState): Boolean =
        !released &&
            crossfadeEngine != null &&
            crossfadeDuration != CrossfadeDuration.Off &&
            activeSleepTimer == null &&
            timerStopClaim == null &&
            state.repeatMode != RepeatMode.RepeatOne &&
            state.repeatMode != RepeatMode.StopAfterCurrent &&
            state.queue.size > 1

    private fun ownsCrossfadeLocked(transition: CrossfadeTransition): Boolean {
        if (crossfadeTransition !== transition) return false
        val state = _state.value
        if (!crossfadePolicyEligibleLocked(state)) return false
        val expectedGeneration =
            if (transition.promoted) transition.incomingGeneration
            else transition.outgoingGeneration
        val expectedOccurrence =
            if (transition.promoted) transition.incoming.id
            else transition.outgoing.id
        if (!ownsSelection(state, expectedGeneration, expectedOccurrence))
            return false
        val outgoing =
            state.queue.firstOrNull { it.id == transition.outgoing.id }
                ?: return false
        val incoming =
            state.queue.firstOrNull { it.id == transition.incoming.id }
                ?: return false
        if (outgoing.track.source != transition.outgoing.track.source ||
            incoming.track.source != transition.incoming.track.source)
            return false
        return nextTrackFrom(
                state,
                wrap = state.repeatMode == RepeatMode.RepeatPlaylist,
                currentId = transition.outgoing.id,
            )
            ?.id == incoming.id
    }

    private fun cancelCrossfadeLocked(
        settleDeferredCompletion: Boolean = true
    ) {
        val transition = crossfadeTransition ?: return
        crossfadeTransition = null
        transition.preparationJob?.cancel()
        transition.tickJob?.cancel()
        desiredCrossfadeToken = null
        desiredOutgoingCrossfadeGain = null
        playbackGainVersion++
        // Cleanup is bound to the native transaction slot, not today's engine
        // generation. A queued cleanup can therefore never retire a winner.
        launchNonFatalEngineAction {
            cleanupCrossfadeWhileEngineLocked()
            if (settleDeferredCompletion &&
                !transition.promoted &&
                transition.outgoingCompleted) {
                settlePlaybackCompletion(
                    transition.outgoingGeneration,
                    transition.outgoingRunId,
                    transition.outgoingCompletionTimerToken,
                    deferredOutgoingOccurrenceId = transition.outgoing.id,
                )
            }
        }
    }

    private fun cancelCrossfade() {
        selectionGate.withLock {
            // Explicit transport supersedes natural traversal; it must not
            // replay a completion after seek, restart, or manual selection.
            cancelCrossfadeLocked(settleDeferredCompletion = false)
        }
    }

    private fun revalidateCrossfadeLocked() {
        val transition = crossfadeTransition ?: return
        if (!ownsCrossfadeLocked(transition)) cancelCrossfadeLocked()
    }

    private fun considerCrossfadeLocked() {
        revalidateCrossfadeLocked()
        val state = _state.value
        val active = crossfadeTransition
        if (active != null) {
            if (state.status == PlaybackStatus.Playing &&
                (active.prepared || active.promoted)) {
                // Progress can jump straight to the start boundary while the
                // monotonic wakeup is pending (including deterministic tests).
                if (!active.started) {
                    active.tickJob?.cancel()
                    active.tickJob = null
                }
                startCrossfadeTicksLocked(active)
            } else if (state.status != PlaybackStatus.Playing) {
                active.tickJob?.cancel()
                active.tickJob = null
            }
            return
        }
        if (!crossfadePolicyEligibleLocked(state) ||
            state.status != PlaybackStatus.Playing)
            return
        val current = state.currentOccurrence ?: return
        val duration = state.durationMillis?.takeIf { it > 0L } ?: return
        val remaining = duration - state.positionMillis
        if (remaining < 250L || remaining > crossfadeDuration.millis + 2_000L)
            return
        val successor =
            nextTrackFrom(
                state, wrap = state.repeatMode == RepeatMode.RepeatPlaylist)
                ?: return
        if (successor.id == current.id) return
        val incomingDuration = successor.track.durationMillis ?: return
        if (effectiveCrossfadeWindowMillis(
            crossfadeDuration.millis, duration, incomingDuration, remaining) ==
            null)
            return
        val run =
            activePlaybackRunId?.takeIf {
                activePlaybackRunGeneration == state.engineGeneration
            } ?: return
        val key = CrossfadeAttemptKey(state.engineGeneration, run, successor.id)
        if (lastCrossfadeAttempt == key) return
        lastCrossfadeAttempt = key
        val transition =
            CrossfadeTransition(
                current,
                state.engineGeneration,
                run,
                successor,
                nextGeneration(),
                duration,
                state.positionMillis,
                crossfadeRuntime.nowMillis(),
            )
        crossfadeTransition = transition
        transition.preparationJob =
            scope
                .launch(start = CoroutineStart.LAZY) {
                    prepareCrossfade(transition)
                }
                .also { it.start() }
    }

    private fun cleanupCrossfadeWhileEngineLocked() {
        val native = nativeCrossfadeTransition ?: return
        val retained = selectionGate.withLock {
            !released && ownsCrossfadeLocked(native)
        }
        if (retained) return
        nativeCrossfadeTransition = null
        val isReleased = selectionGate.withLock { released }
        if (isReleased) return // release already disposed both native voices.
        if (native.promoted) crossfadeEngine?.finishCrossfade()
        else crossfadeEngine?.cancelPreparedIncoming()
        val generation =
            if (native.promoted) native.incomingGeneration
            else native.outgoingGeneration
        val gain = selectionGate.withLock {
            recomputeDesiredPlaybackGainLocked(generation, force = true)
        }
        gain?.let(::applyPlaybackGainWhileEngineLocked)
    }

    private suspend fun prepareCrossfade(transition: CrossfadeTransition) {
        val capable = crossfadeEngine ?: return
        try {
            engineMutex.withLock {
                cleanupCrossfadeWhileEngineLocked()
                if (!selectionGate.withLock { ownsCrossfadeLocked(transition) })
                    return@withLock
                nativeCrossfadeTransition = transition
                try {
                    val loaded =
                        capable.prepareIncoming(
                            transition.incoming.track.withLazyArtwork(),
                            transition.incomingGeneration,
                        )
                    check(loaded.generation == transition.incomingGeneration)
                    val ready = selectionGate.withLock {
                        if (!ownsCrossfadeLocked(transition))
                            return@withLock false
                        transition.incomingDurationMillis =
                            loaded.durationMillis
                                ?: transition.incoming.track.durationMillis
                        transition.prepared = true
                        if (crossfadeWindowLocked(transition) == null) {
                            cancelCrossfadeLocked()
                            return@withLock false
                        }
                        true
                    }
                    if (ready) {
                        startPreparedCrossfadeWhileEngineLocked(transition)
                        selectionGate.withLock {
                            if (ownsCrossfadeLocked(transition) &&
                                _state.value.status == PlaybackStatus.Playing)
                                startCrossfadeTicksLocked(transition)
                        }
                    }
                } finally {
                    // Includes non-cancellable late preparation and cancelled
                    // cooperative preparation, before another load gets the
                    // mutex.
                    cleanupCrossfadeWhileEngineLocked()
                }
            }
        } catch (failure: Throwable) {
            if (failure is CancellationException) {
                selectionGate.withLock {
                    if (crossfadeTransition === transition)
                        cancelCrossfadeLocked()
                }
                throw failure
            }
            playbackLog.e { failure.stackTraceToString() }
            onPlaybackError(
                transition.incomingGeneration,
                (failure as? PlaybackFailureException)?.error
                    ?: PlaybackError("Playback failed", failure.message),
            )
            launchNonFatalEngineAction { cleanupCrossfadeWhileEngineLocked() }
        }
    }

    private fun outgoingRemainingLocked(transition: CrossfadeTransition): Long {
        val elapsed =
            if (transition.outgoingAdvancing)
                (crossfadeRuntime.nowMillis() -
                        transition.outgoingPositionUpdatedAtMillis)
                    .coerceAtLeast(0L)
            else 0L
        return transition.outgoingDurationMillis -
            transition.outgoingPositionMillis -
            elapsed
    }

    private fun crossfadeWindowLocked(transition: CrossfadeTransition): Long? {
        val incomingDuration = transition.incomingDurationMillis ?: return null
        return effectiveCrossfadeWindowMillis(
            crossfadeDuration.millis,
            transition.outgoingDurationMillis,
            incomingDuration,
            outgoingRemainingLocked(transition),
        )
    }

    private fun incomingPositionLocked(transition: CrossfadeTransition): Long {
        val elapsed =
            if (transition.incomingAdvancing)
                (crossfadeRuntime.nowMillis() -
                        transition.incomingPositionUpdatedAtMillis)
                    .coerceAtLeast(0L)
            else 0L
        return transition.incomingPositionMillis + elapsed
    }

    private fun startPreparedCrossfadeWhileEngineLocked(
        transition: CrossfadeTransition
    ) {
        val admitted = selectionGate.withLock {
            if (!ownsCrossfadeLocked(transition) ||
                !transition.prepared ||
                transition.started ||
                _state.value.status != PlaybackStatus.Playing)
                return@withLock false
            val incomingDuration =
                transition.incomingDurationMillis ?: return@withLock false
            val fullWindow =
                effectiveCrossfadeWindowMillis(
                    crossfadeDuration.millis,
                    transition.outgoingDurationMillis,
                    incomingDuration,
                    transition.outgoingDurationMillis,
                ) ?: return@withLock false
            val remaining = outgoingRemainingLocked(transition)
            if (remaining > fullWindow) return@withLock false
            val window = crossfadeWindowLocked(transition)
            if (window == null) {
                cancelCrossfadeLocked()
                return@withLock false
            }
            transition.windowMillis = window
            transition.incomingRunId = ++nextPlaybackRunId
            transition.started = true
            true
        }
        if (!admitted) return
        val outgoingGain = selectionGate.withLock {
            replayGainFactor(
                _state.value.queue
                    .firstOrNull { it.id == transition.outgoing.id }
                    ?.track,
                replayGainMode,
            )
        }
        // Both values are written before native play, even when normalization
        // is Off; a staged voice must never start at unity.
        crossfadeEngine?.setCrossfadeGains(outgoingGain, 0f)
        crossfadeEngine?.startIncoming(transition.incomingRunId)
    }

    private fun startCrossfadeTicksLocked(transition: CrossfadeTransition) {
        if (transition.tickJob?.isActive == true) return
        transition.tickJob =
            scope
                .launch(start = CoroutineStart.LAZY) {
                    runCrossfadeTicks(transition)
                }
                .also { it.start() }
    }

    private suspend fun runCrossfadeTicks(transition: CrossfadeTransition) {
        try {
            while (true) {
                engineMutex.withLock {
                    cleanupCrossfadeWhileEngineLocked()
                    val active = selectionGate.withLock {
                        ownsCrossfadeLocked(transition) &&
                            _state.value.status == PlaybackStatus.Playing
                    }
                    if (!active) return
                    startPreparedCrossfadeWhileEngineLocked(transition)
                    val gain = selectionGate.withLock {
                        if (!ownsCrossfadeLocked(transition) ||
                            !transition.promoted)
                            return@withLock null
                        if (incomingPositionLocked(transition) >=
                            transition.windowMillis) {
                            cancelCrossfadeLocked()
                            null
                        } else
                            recomputeDesiredPlaybackGainLocked(
                                transition.incomingGeneration)
                    }
                    cleanupCrossfadeWhileEngineLocked()
                    gain?.let(::applyPlaybackGainWhileEngineLocked)
                }
                crossfadeRuntime.delay(50L)
            }
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            onPlaybackError(
                transition.incomingGeneration,
                (failure as? PlaybackFailureException)?.error
                    ?: PlaybackError("Playback failed", failure.message),
            )
            launchNonFatalEngineAction { cleanupCrossfadeWhileEngineLocked() }
            playbackLog.e { failure.stackTraceToString() }
        }
    }

    private fun nextSleepTimerTokenLocked(): Long = ++nextSleepTimerToken

    private fun saturatedDeadline(now: Long, duration: Long): Long =
        if (Long.MAX_VALUE - now < duration) Long.MAX_VALUE else now + duration

    private fun ArmedSleepTimer.toState(
        remaining: Long? = null
    ): SleepTimerState =
        SleepTimerState(
            mode = mode,
            remainingMillis = remaining,
            remainingTracks = remainingTracks,
            fadeEnabled = fadeEnabled,
        )

    private fun setSleepAttenuationLocked(
        gain: Float,
        generation: Long,
        force: Boolean = false,
    ): PlaybackGainRequest? {
        if (released || _state.value.engineGeneration != generation) return null
        sleepAttenuation = gain
        return recomputeDesiredPlaybackGainLocked(generation, force)
    }

    private fun recomputeDesiredPlaybackGainLocked(
        generation: Long,
        force: Boolean = false,
    ): PlaybackGainRequest? {
        if (released || _state.value.engineGeneration != generation) return null
        val state = _state.value
        val transition = crossfadeTransition?.takeIf {
            it.started && ownsCrossfadeLocked(it)
        }
        var outgoingGain: Float? = null
        val gain =
            if (transition == null) {
                replayGainFactor(state.currentTrack, replayGainMode) *
                    sleepAttenuation
            } else {
                val fraction =
                    if (transition.promoted)
                        (incomingPositionLocked(transition).toDouble() /
                                transition.windowMillis)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    else 0f
                outgoingGain =
                    replayGainFactor(
                        state.queue
                            .firstOrNull { it.id == transition.outgoing.id }
                            ?.track,
                        replayGainMode,
                    ) * (1f - fraction)
                replayGainFactor(
                    state.queue
                        .firstOrNull { it.id == transition.incoming.id }
                        ?.track,
                    replayGainMode,
                ) * fraction
            }
        val crossfadeToken = transition?.incomingGeneration
        if (!force &&
            desiredPlaybackGain == gain &&
            desiredPlaybackGainGeneration == generation &&
            desiredOutgoingCrossfadeGain == outgoingGain &&
            desiredCrossfadeToken == crossfadeToken)
            return null
        desiredPlaybackGain = gain
        desiredOutgoingCrossfadeGain = outgoingGain
        desiredCrossfadeToken = crossfadeToken
        if (gain != 1f) playbackGainMayBeAttenuated = true
        desiredPlaybackGainGeneration = generation
        val version = ++playbackGainVersion
        if (!force &&
            gain == 1f &&
            activeSleepTimer == null &&
            transition == null &&
            !playbackGainMayBeAttenuated)
            return null
        return PlaybackGainRequest(
            version, generation, gain, crossfadeToken, outgoingGain)
    }

    private fun applyPlaybackGainWhileEngineLocked(
        request: PlaybackGainRequest
    ) {
        val current = selectionGate.withLock {
            !released &&
                request.version == playbackGainVersion &&
                request.generation == _state.value.engineGeneration &&
                (request.crossfadeToken == null ||
                    crossfadeTransition?.incomingGeneration ==
                        request.crossfadeToken &&
                        nativeCrossfadeTransition === crossfadeTransition)
        }
        if (current) {
            if (request.outgoingGain != null) {
                crossfadeEngine?.setCrossfadeGains(
                    request.outgoingGain, request.gain)
            } else engine.setPlaybackGain(request.gain)
            selectionGate.withLock {
                if (request.version == playbackGainVersion)
                    playbackGainMayBeAttenuated =
                        request.gain != 1f || request.outgoingGain != null
            }
        }
    }

    private fun launchPlaybackGain(request: PlaybackGainRequest) {
        launchEngineAction(
            request.generation ?: _state.value.engineGeneration) {
                applyPlaybackGainWhileEngineLocked(request)
            }
    }

    private fun applyCurrentPlaybackGainBeforePlay(generation: Long) {
        val request = selectionGate.withLock {
            if (released ||
                _state.value.engineGeneration != generation ||
                timerStopClaim?.generation == generation)
                return@withLock null
            // Elapsed-time fades must be current before resuming; completion
            // fades instead retain their frozen paused attenuation.
            val updated =
                if (activeSleepTimer?.mode == SleepTimerMode.Timed)
                    setSleepAttenuationLocked(
                        selectionSleepAttenuationLocked(), generation)
                else
                    recomputeDesiredPlaybackGainLocked(
                        generation,
                        force = crossfadeTransition?.promoted == true)
            updated
                ?: if (activeSleepTimer != null ||
                    desiredPlaybackGain != 1f ||
                    playbackGainMayBeAttenuated)
                    PlaybackGainRequest(
                        playbackGainVersion, generation, desiredPlaybackGain)
                else null
        }
        request?.let(::applyPlaybackGainWhileEngineLocked)
    }

    private fun beginPlaybackRun(generation: Long): Long? =
        selectionGate.withLock {
            if (_state.value.engineGeneration != generation ||
                _state.value.status == PlaybackStatus.Stopped &&
                    timerStopClaim != null)
                return@withLock null
            activePlaybackRunGeneration = generation
            (++nextPlaybackRunId).also {
                activePlaybackRunId = it
                crossfadeTransition
                    ?.takeIf { transition ->
                        transition.promoted &&
                            transition.incomingGeneration == generation
                    }
                    ?.incomingRunId = it
                crossfadeTransition
                    ?.takeIf { transition ->
                        !transition.promoted &&
                            transition.outgoingGeneration == generation
                    }
                    ?.outgoingRunId = it
            }
        }

    private fun invalidatePlaybackRun(generation: Long) {
        selectionGate.withLock {
            if (activePlaybackRunGeneration == generation)
                activePlaybackRunId = null
        }
    }

    private fun resetSleepAttenuationForStop(generation: Long) =
        selectionGate.withLock {
            resetSleepAttenuationForStopLocked(generation)
        }

    private fun resetSleepAttenuationForStopLocked(generation: Long) {
        if (_state.value.engineGeneration != generation) return
        sleepAttenuation = 1f
        desiredPlaybackGain =
            replayGainFactor(_state.value.currentTrack, replayGainMode)
        desiredPlaybackGainGeneration = generation
        // Invalidate queued fade writes, retaining policy for the next resume.
        playbackGainVersion++
    }

    /** Called only after the native engine has stopped or cleared its media. */
    private fun resetStoppedPlaybackGainWhileEngineLocked(
        generation: Long,
        force: Boolean = false,
    ) {
        val version =
            selectionGate.withLock {
                playbackGainVersion.takeIf {
                    !released &&
                        _state.value.engineGeneration == generation &&
                        (force || playbackGainMayBeAttenuated)
                }
            } ?: return
        engine.setPlaybackGain(1f)
        selectionGate.withLock {
            if (_state.value.engineGeneration == generation &&
                playbackGainVersion == version)
                playbackGainMayBeAttenuated = false
        }
    }

    private fun cancelSleepTimerAndRestoreGain() {
        val reset = selectionGate.withLock {
            if (activeSleepTimer == null && sleepAttenuation == 1f)
                return@withLock null
            activeSleepTimer = null
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            _sleepTimerState.value = SleepTimerState()
            setSleepAttenuationLocked(
                1f, _state.value.engineGeneration, force = true)
        }
        reset?.let(::launchPlaybackGain)
    }

    private fun runTimerStop(request: TimerStopRequest) {
        launchEngineAction(request.generation) {
            finishClaimedTimerStopWhileEngineLocked(
                request.generation, request.token)
        }
    }

    private fun finishClaimedTimerStopWhileEngineLocked(
        generation: Long,
        token: Long? = null,
    ) {
        val claim =
            selectionGate.withLock {
                timerStopClaim?.takeIf {
                    it.generation == generation &&
                        (token == null || it.token == token) &&
                        _state.value.engineGeneration == generation &&
                        _state.value.status == PlaybackStatus.Stopped
                }
            } ?: return
        engine.stop()
        resetStoppedPlaybackGainWhileEngineLocked(generation, force = true)
        selectionGate.withLock {
            if (timerStopClaim == claim) {
                timerStopClaim = null
            }
        }
    }

    private fun cancelTimerForEmptyQueue(generation: Long) {
        selectionGate.withLock {
            val current = _state.value
            if (current.engineGeneration != generation ||
                current.queue.isNotEmpty())
                return@withLock null
            if (activeSleepTimer == null &&
                desiredPlaybackGain == 1f &&
                !playbackGainMayBeAttenuated)
                return@withLock null
            activeSleepTimer = null
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            timerStopClaim = null
            _sleepTimerState.value = SleepTimerState()
            sleepAttenuation = 1f
            desiredPlaybackGain = 1f
            desiredPlaybackGainGeneration = generation
            playbackGainVersion++
        }
    }

    private fun selectionSleepAttenuationLocked(): Float {
        val remaining =
            activeSleepTimer
                ?.takeIf { it.mode == SleepTimerMode.Timed && it.fadeEnabled }
                ?.deadlineMillis
                ?.minus(sleepTimerRuntime.nowMillis())
        return if (remaining != null &&
            remaining in 1 until sleepTimerFadeWindowMillis)
            remaining.toFloat() / sleepTimerFadeWindowMillis
        else 1f
    }

    /**
     * Claims the stop and its checkpoint before a delayed native load can
     * autoplay.
     */
    private fun claimTimerStopLocked(
        timer: ArmedSleepTimer,
        state: PlaybackState,
        atTrackEnd: Boolean,
    ): TimerStopRequest {
        activeSleepTimer = null
        sleepTimerJob = null
        _sleepTimerState.value = SleepTimerState()
        selectionRequest.value?.playWhenLoaded?.value = false
        val generation = state.engineGeneration
        val stopped =
            state.copy(
                status = PlaybackStatus.Stopped,
                positionMillis =
                    if (atTrackEnd)
                        state.durationMillis ?: max(0L, state.positionMillis)
                    else 0L,
                error = null,
                checkpointRevision = reserveCheckpointRevision(),
            )
        _state.value = stopped
        timerStopClaim = TimerStopClaim(timer.token, generation)
        resetProgressCheckpointKey()
        emitImmediateCheckpoint(
            stopped.toSessionSnapshot(), stopped.checkpointRevision)
        sleepAttenuation = 1f
        desiredPlaybackGain =
            replayGainFactor(stopped.currentTrack, replayGainMode)
        desiredPlaybackGainGeneration = generation
        playbackGainVersion++
        return TimerStopRequest(timer.token, generation, atTrackEnd)
    }

    private suspend fun runTimedSleepTimer(token: Long) {
        while (true) {
            val wake =
                selectionGate.withLock {
                    val timer =
                        activeSleepTimer?.takeIf {
                            it.token == token && it.mode == SleepTimerMode.Timed
                        } ?: return@withLock null
                    val remaining =
                        (checkNotNull(timer.deadlineMillis) -
                                sleepTimerRuntime.nowMillis())
                            .coerceAtLeast(0L)
                    if (remaining == 0L) {
                        TimedTimerWakeup(
                            0L,
                            null,
                            claimTimerStopLocked(
                                timer, _state.value, atTrackEnd = false))
                    } else {
                        if (timer.lastPublishedRemainingMillis != remaining) {
                            timer.lastPublishedRemainingMillis = remaining
                            _sleepTimerState.value = timer.toState(remaining)
                        }
                        val state = _state.value
                        val targetGain =
                            if (timer.fadeEnabled &&
                                state.status == PlaybackStatus.Playing &&
                                remaining < sleepTimerFadeWindowMillis)
                                remaining.toFloat() / sleepTimerFadeWindowMillis
                            else 1f
                        val gain =
                            setSleepAttenuationLocked(
                                targetGain, state.engineGeneration)
                        val interval =
                            if (timer.fadeEnabled &&
                                remaining <= sleepTimerFadeWindowMillis)
                                sleepTimerFadeUpdateMillis
                            else sleepTimerStateUpdateMillis
                        TimedTimerWakeup(minOf(remaining, interval), gain, null)
                    }
                } ?: return
            wake.gainRequest?.let(::launchPlaybackGain)
            wake.stopRequest?.let {
                runTimerStop(it)
                return
            }
            sleepTimerRuntime.delay(wake.delayMillis)
        }
    }

    private fun updateCompletionFadeLocked(
        state: PlaybackState,
        allowPausedSeek: Boolean = false,
    ): PlaybackGainRequest? {
        if (state.status == PlaybackStatus.Paused && !allowPausedSeek)
            return null
        val timer =
            activeSleepTimer?.takeIf {
                it.mode == SleepTimerMode.TrackCount &&
                    it.remainingTracks == 1 &&
                    it.fadeEnabled
            } ?: return null
        val duration = state.durationMillis ?: return null
        val remaining = (duration - state.positionMillis).coerceAtLeast(0L)
        val gain =
            if ((state.status == PlaybackStatus.Playing ||
                state.status == PlaybackStatus.Paused) &&
                remaining < sleepTimerFadeWindowMillis)
                remaining.toFloat() / sleepTimerFadeWindowMillis
            else 1f
        return setSleepAttenuationLocked(gain, state.engineGeneration)
    }

    private fun refreshCompletionFadeForProgress(generation: Long) {
        val request = selectionGate.withLock {
            _state.value
                .takeIf { it.engineGeneration == generation }
                ?.let { updateCompletionFadeLocked(it, allowPausedSeek = true) }
        }
        request?.let(::launchPlaybackGain)
    }

    private fun resetPlaybackGainForTrackRestart(
        generation: Long,
    ): PlaybackGainRequest? = selectionGate.withLock {
        if (activeSleepTimer?.mode != SleepTimerMode.TrackCount ||
            sleepAttenuation == 1f)
            null
        else setSleepAttenuationLocked(1f, generation, force = true)
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
        autoPlay: Boolean = false,
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
            val published = selectionGate.withLock {
                cancelCrossfadeLocked()
                val cleared = publishState { previous ->
                    PlaybackState(
                        queue = occurrences,
                        repeatMode = previous.repeatMode,
                        shuffleMode = previous.shuffleMode,
                        engineGeneration = generation,
                    )
                }
                activeSleepTimer = null
                sleepTimerJob?.cancel()
                sleepTimerJob = null
                timerStopClaim = null
                _sleepTimerState.value = SleepTimerState()
                sleepAttenuation = 1f
                desiredPlaybackGain = 1f
                desiredPlaybackGainGeneration = generation
                playbackGainVersion++
                cleared
            }
            emitImmediateCheckpoint(
                published.toSessionSnapshot(), published.checkpointRevision)
            launchEngineAction(generation) {
                if (_state.value.engineGeneration == generation) {
                    engine.clear(generation)
                    resetStoppedPlaybackGainWhileEngineLocked(generation)
                }
            }
        } else {
            if (loadSelected(
                selected,
                autoPlay = autoPlay,
                replacementQueue = occurrences,
            )) {
                emitImmediateCheckpoint()
            }
        }
    }

    /**
     * Updates indexed display metadata and ReplayGain facts, preserving
     * transport ownership and the current sleep attenuation.
     */
    public fun refreshTrackMetadata(tracks: List<PlayableTrack>) {
        val metadataById = tracks.associateBy { it.id }
        var loaded: Pair<Long, PlayableTrack>? = null
        var gainRequest: PlaybackGainRequest? = null
        selectionGate.withLock {
            if (released) return@withLock
            while (true) {
                val previous = _state.value
                val updatedQueue =
                    previous.queue.map { occurrence ->
                        val latest =
                            metadataById[occurrence.track.id]
                                ?: return@map occurrence
                        val original = occurrence.track
                        if (original.title == latest.title &&
                            original.artist == latest.artist &&
                            original.album == latest.album &&
                            original.replayGainTrackGainDb ==
                                latest.replayGainTrackGainDb &&
                            original.replayGainTrackPeak ==
                                latest.replayGainTrackPeak &&
                            original.replayGainAlbumGainDb ==
                                latest.replayGainAlbumGainDb &&
                            original.replayGainAlbumPeak ==
                                latest.replayGainAlbumPeak)
                            occurrence
                        else
                            occurrence.copy(
                                track =
                                    original.copy(
                                        title = latest.title,
                                        artist = latest.artist,
                                        album = latest.album,
                                        replayGainTrackGainDb =
                                            latest.replayGainTrackGainDb,
                                        replayGainTrackPeak =
                                            latest.replayGainTrackPeak,
                                        replayGainAlbumGainDb =
                                            latest.replayGainAlbumGainDb,
                                        replayGainAlbumPeak =
                                            latest.replayGainAlbumPeak,
                                    ))
                    }
                // Session persistence stores IDs, not metadata: no checkpoint.
                val updated =
                    if (updatedQueue == previous.queue) previous
                    else previous.copy(queue = updatedQueue)
                if (updated !== previous &&
                    !_state.compareAndSet(previous, updated))
                    continue
                updated.currentTrack
                    ?.takeIf { metadataById.containsKey(it.id) }
                    ?.let { current ->
                        // Reconciliation may already have published these
                        // facts.
                        // Recompute policy and refresh the native label
                        // regardless.
                        gainRequest =
                            recomputeDesiredPlaybackGainLocked(
                                updated.engineGeneration)
                        if (updated.status != PlaybackStatus.Loading &&
                            updated.status != PlaybackStatus.Idle) {
                            loaded = updated.engineGeneration to current
                        }
                    }
                break
            }
        }
        gainRequest?.let(::launchPlaybackGain)
        loaded?.let { (generation, current) ->
            launchNonFatalEngineAction {
                val stillCurrent = selectionGate.withLock {
                    _state.value.engineGeneration == generation &&
                        _state.value.currentTrack == current
                }
                if (stillCurrent) {
                    try {
                        engine.refreshLoadedMetadata(current, generation)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Throwable) {
                        // A system-label update must not turn healthy audio
                        // into playback Error.
                        playbackLog.e { failure.stackTraceToString() }
                    }
                }
            }
        }
    }

    /**
     * Refreshes the loaded native label even when queue reconciliation already
     * applied it.
     */
    public fun refreshCurrentTrackMetadata(tracks: List<PlayableTrack>) {
        val metadataById = tracks.associateBy { it.id }
        val snapshot = _state.value
        val current =
            snapshot.currentTrack?.let { metadataById[it.id] } ?: return
        val gainRequest = selectionGate.withLock {
            recomputeDesiredPlaybackGainLocked(snapshot.engineGeneration)
        }
        gainRequest?.let(::launchPlaybackGain)
        if (snapshot.status == PlaybackStatus.Loading ||
            snapshot.status == PlaybackStatus.Idle)
            return
        launchNonFatalEngineAction {
            val stillCurrent = selectionGate.withLock {
                val liveTrack = _state.value.currentTrack
                _state.value.engineGeneration == snapshot.engineGeneration &&
                    liveTrack?.id == current.id &&
                    liveTrack.title == current.title &&
                    liveTrack.artist == current.artist &&
                    liveTrack.album == current.album
            }
            if (stillCurrent) {
                engine.refreshLoadedMetadata(current, snapshot.engineGeneration)
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
        val published = selectionGate.withLock {
            publishState { it.copy(repeatMode = mode) }
                .also {
                    cancelGaplessLocked()
                    revalidateCrossfadeLocked()
                    considerCrossfadeLocked()
                    if (crossfadeTransition == null) considerGaplessLocked()
                }
        }
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
        val published = selectionGate.withLock {
            publishState { it.copy(shuffleMode = mode) }
                .also {
                    cancelGaplessLocked()
                    publishRuntimeShuffleOrder(it)
                    revalidateCrossfadeLocked()
                    considerCrossfadeLocked()
                    if (crossfadeTransition == null) considerGaplessLocked()
                }
        }
        playbackLog.d { "ShuffleMode changed: $previous -> $mode" }
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
        val generation = _state.value.engineGeneration
        launchEngineAction(generation) {
            finishClaimedTimerStopWhileEngineLocked(generation)
            applyCurrentPlaybackGainBeforePlay(generation)
            beginPlaybackRun(generation)?.let { runId ->
                engine.play(runId)
                selectionGate.withLock { considerGaplessLocked() }
            }
        }
    }

    /** Pauses media and emits a persistence checkpoint. */
    public fun pause() {
        pauseOwner()
    }

    private fun pauseOwner(externalGeneration: Long? = null) {
        selectionGate.withLock {
            val owner =
                transportOwnerLocked(externalGeneration) ?: return@withLock
            if (crossfadeTransition?.let { it.started && !it.promoted } == true)
                cancelCrossfadeLocked(settleDeferredCompletion = false)
            cancelGaplessLocked()
            setPendingAutoplayLocked(false, owner.engineGeneration)
            launchEngineAction(owner.engineGeneration) { engine.pause() }
            emitImmediateCheckpoint(
                owner.toSessionSnapshot(), owner.checkpointRevision)
        }
    }

    /** Stops media and emits a persistence checkpoint. */
    public fun stop() {
        stopOwner()
    }

    private fun stopOwner(externalGeneration: Long? = null) {
        selectionGate.withLock {
            val owner =
                transportOwnerLocked(externalGeneration) ?: return@withLock
            val generation = owner.engineGeneration
            cancelCrossfadeLocked(settleDeferredCompletion = false)
            setPendingAutoplayLocked(false, generation)
            resetProgressCheckpointKey()
            if (activePlaybackRunGeneration == generation)
                activePlaybackRunId = null
            resetSleepAttenuationForStopLocked(generation)
            launchEngineAction(generation) {
                engine.stop()
                resetStoppedPlaybackGainWhileEngineLocked(generation)
            }
            emitImmediateCheckpoint(
                owner.toSessionSnapshot(), owner.checkpointRevision)
        }
    }

    /** Clamps and applies a new playback position. */
    public fun seekTo(positionMillis: Long) {
        seekOwner(positionMillis)
    }

    private fun seekOwner(
        positionMillis: Long,
        externalGeneration: Long? = null
    ) {
        var published: PlaybackState? = null
        selectionGate.withLock {
            val owner =
                transportOwnerLocked(externalGeneration) ?: return@withLock
            cancelCrossfadeLocked(settleDeferredCompletion = false)
            val safePosition =
                owner.durationMillis?.let { positionMillis.coerceIn(0L, it) }
                    ?: max(0L, positionMillis)
            val updated = publishState {
                it.copy(positionMillis = safePosition, error = null)
            }
            published = updated
            resetProgressCheckpointKey()
            launchEngineAction(updated.engineGeneration) {
                engine.seekTo(safePosition)
            }
            emitImmediateCheckpoint(
                updated.toSessionSnapshot(), updated.checkpointRevision)
        }
        published?.let {
            refreshCompletionFadeForProgress(it.engineGeneration)
        }
    }

    /** Pauses active playback or starts the selected occurrence. */
    public fun togglePlayPause() {
        if (!commandsEnabled.value) return
        if (_state.value.isPlaying) pause() else play()
    }

    /** Restarts the selected occurrence from position zero. */
    public fun restartCurrentTrack() {
        if (!commandsEnabled.value) return
        cancelCrossfade()
        val current = _state.value.currentOccurrence ?: return
        val published = publishState {
            it.copy(positionMillis = 0L, error = null)
        }
        invalidatePlaybackRun(published.engineGeneration)
        resetProgressCheckpointKey()
        val gainReset =
            resetPlaybackGainForTrackRestart(published.engineGeneration)
        when (published.status) {
            // Restarting an idle or errored occurrence reloads it through the
            // selection transaction. A concurrent replacement that claims
            // before this admission aborts it, and no stale pre-replacement
            // checkpoint is emitted; when admission succeeds the claimed
            // Loading state is the intended restart checkpoint.
            PlaybackStatus.Idle,
            PlaybackStatus.Error,
            ->
                if (loadSelected(current, autoPlay = true)) {
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
                        finishClaimedTimerStopWhileEngineLocked(
                            published.engineGeneration)
                        engine.seekTo(0L)
                        gainReset?.let(::applyPlaybackGainWhileEngineLocked)
                        applyCurrentPlaybackGainBeforePlay(
                            published.engineGeneration)
                        beginPlaybackRun(published.engineGeneration)
                            ?.let(engine::play)
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
                    current.currentOccurrenceId == published.currentOccurrenceId
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
        cancelCrossfade()
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
        cancelCrossfade()
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
        val captured =
            _state.value.takeIf {
                commandsEnabled.value &&
                    it.status == PlaybackStatus.Error &&
                    it.error != null
            } ?: return
        val occurrence = captured.currentOccurrence ?: return
        if (loadSelectedFrom(captured, occurrence, autoPlay = true)) {
            emitImmediateCheckpoint()
        }
    }

    /**
     * Loads the effective successor of the failed current occurrence without
     * repeat wrapping, leaving the failure visible at the effective end.
     */
    public fun skipFailedTrack() {
        val captured =
            _state.value.takeIf {
                commandsEnabled.value &&
                    it.status == PlaybackStatus.Error &&
                    it.error != null
            } ?: return
        nextTrackFrom(captured, wrap = false)?.let {
            if (loadSelectedFrom(captured, it, autoPlay = true)) {
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
                if (remaining.isEmpty())
                    cancelTimerForEmptyQueue(cleanupGeneration)
                else resetSleepAttenuationForStop(cleanupGeneration)
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
                    cleanupCrossfadeWhileEngineLocked()
                    if (_state.value.engineGeneration == cleanupGeneration) {
                        engine.clear(cleanupGeneration)
                        resetStoppedPlaybackGainWhileEngineLocked(
                            cleanupGeneration)
                    }
                }
                return@withLock QueueMutationResult.Applied
            }
            error("Unreachable queue mutation loop")
        }

    /** Releases controller resources and stops checkpoint delivery. */
    public fun release() {
        selectionGate.withLock {
            released = true
            cancelGaplessLocked()
            cancelCrossfadeLocked()
            activeSleepTimer = null
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            timerStopClaim = null
            _sleepTimerState.value = SleepTimerState()
            sleepAttenuation = 1f
            desiredPlaybackGain = 1f
            playbackGainVersion++
        }
        scope.cancel()
        engine.listener = null
        engine.stop()
        engine.setPlaybackGain(1f)
        engine.release()
        playbackStartedChannel.close()
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
            selectionGate.withLock {
                claimPausedSessionState(
                    base,
                    reconciledQueue,
                    restoredCurrent,
                    snapshot.repeatMode,
                    snapshot.shuffleMode,
                    generation,
                )
            } ?: return@withLock revisionedSessionSnapshot()
        publishRuntimeShuffleOrder(claimed)
        if (restoredCurrent == null) {
            cancelTimerForEmptyQueue(generation)
            engineMutex.withLock {
                cleanupCrossfadeWhileEngineLocked()
                if (_state.value.engineGeneration == generation) {
                    engine.clear(generation)
                    resetStoppedPlaybackGainWhileEngineLocked(generation)
                }
            }
            emitImmediateCheckpoint()
            return@withLock revisionedSessionSnapshot()
        }
        try {
            engineMutex.withLock {
                cleanupCrossfadeWhileEngineLocked()
                if (!ownsSelection(generation, restoredCurrent.id))
                    return@withLock
                val loaded =
                    engine.loadPaused(
                        restoredCurrent.track.withLazyArtwork(), generation)
                check(loaded.generation == generation)
                if (!ownsSelection(generation, restoredCurrent.id))
                    return@withLock
                applyCurrentPlaybackGainBeforePlay(generation)
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
            val fallback = selectionGate.withLock {
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
            cancelTimerForEmptyQueue(fallback.engineGeneration)
            publishRuntimeShuffleOrder(fallback)
            engineMutex.withLock {
                cleanupCrossfadeWhileEngineLocked()
                if (_state.value.engineGeneration ==
                    fallback.engineGeneration) {
                    engine.clear(fallback.engineGeneration)
                    resetStoppedPlaybackGainWhileEngineLocked(
                        fallback.engineGeneration)
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
                // A surviving Error remains attached to the failed engine
                // session. Clearing it here would remove the recovery UI and
                // invent a generation that no platform load owns.
                if (previous.status == PlaybackStatus.Error) {
                    val published =
                        previous.copy(
                            currentOccurrenceId = current.id,
                            queue = reconciledQueue,
                            checkpointRevision = reserveCheckpointRevision(),
                        )
                    var gainRequest: PlaybackGainRequest? = null
                    val committed = selectionGate.withLock {
                        if (!_state.compareAndSet(previous, published)) false
                        else {
                            publishRuntimeShuffleOrder(published)
                            revalidateCrossfadeLocked()
                            gainRequest =
                                recomputeDesiredPlaybackGainLocked(
                                    published.engineGeneration)
                            true
                        }
                    }
                    if (!committed) continue
                    gainRequest?.let(::launchPlaybackGain)
                    emitImmediateCheckpoint(
                        published.toSessionSnapshot(),
                        published.checkpointRevision,
                    )
                    return@withLock published.toRevisionedSessionSnapshot()
                }
                // Loading and Buffering need a real engine rebind. Claim a
                // fresh token before loading so non-cancellable callbacks from
                // displaced session cannot settle or autoplay this state.
                if (previous.status == PlaybackStatus.Loading ||
                    previous.status == PlaybackStatus.Buffering) {
                    if (!loadSelectedFrom(
                        previous,
                        current,
                        autoPlay = false,
                        replacementQueue = reconciledQueue,
                    ))
                        continue
                    val published = _state.value
                    emitImmediateCheckpoint(
                        published.toSessionSnapshot(),
                        published.checkpointRevision,
                    )
                    return@withLock revisionedSessionSnapshot()
                }
                val published =
                    previous.copy(
                        currentOccurrenceId = current.id,
                        queue = reconciledQueue,
                        status = previous.status,
                        error = previous.error,
                        errorGeneration = previous.errorGeneration,
                        engineGeneration = previous.engineGeneration,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                var gainRequest: PlaybackGainRequest? = null
                val committed = selectionGate.withLock {
                    if (!_state.compareAndSet(previous, published)) false
                    else {
                        publishRuntimeShuffleOrder(published)
                        revalidateCrossfadeLocked()
                        considerCrossfadeLocked()
                        gainRequest =
                            recomputeDesiredPlaybackGainLocked(
                                published.engineGeneration)
                        true
                    }
                }
                if (!committed) continue
                gainRequest?.let(::launchPlaybackGain)
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
                    selectionGate.withLock {
                        claimPausedSessionState(
                            previous,
                            emptyList(),
                            null,
                            previous.repeatMode,
                            previous.shuffleMode,
                            generation,
                        )
                    } ?: continue
                cancelTimerForEmptyQueue(generation)
                publishRuntimeShuffleOrder(cleared)
                engineMutex.withLock {
                    cleanupCrossfadeWhileEngineLocked()
                    if (_state.value.engineGeneration == generation) {
                        engine.clear(generation)
                        resetStoppedPlaybackGainWhileEngineLocked(generation)
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
                selectionGate.withLock {
                    claimPausedSessionState(
                        previous,
                        reconciledQueue,
                        replacement,
                        previous.repeatMode,
                        previous.shuffleMode,
                        generation,
                    )
                } ?: return@withLock revisionedSessionSnapshot()
            publishRuntimeShuffleOrder(claimed, replacement.id)
            try {
                engineMutex.withLock {
                    cleanupCrossfadeWhileEngineLocked()
                    if (!ownsSelection(generation, replacement.id))
                        return@withLock
                    val loaded =
                        engine.loadPaused(
                            replacement.track.withLazyArtwork(), generation)
                    check(loaded.generation == generation)
                    if (!ownsSelection(generation, replacement.id))
                        return@withLock
                    applyCurrentPlaybackGainBeforePlay(generation)
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
                val failSafe = selectionGate.withLock {
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
                    cancelTimerForEmptyQueue(failSafe.engineGeneration)
                    publishRuntimeShuffleOrder(failSafe)
                    engineMutex.withLock {
                        cleanupCrossfadeWhileEngineLocked()
                        if (_state.value.engineGeneration ==
                            failSafe.engineGeneration) {
                            engine.clear(failSafe.engineGeneration)
                            resetStoppedPlaybackGainWhileEngineLocked(
                                failSafe.engineGeneration)
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
     * replacement (capturing the displaced request), generation allocation, the
     * Loading state claim that publishes the engine-generation token from the
     * exact captured state, and the job/autoplay ownership handoff. A claim
     * that loses restores the displaced request and cancels only its own lazy
     * Job; a claim that wins cancels the captured displaced request (never a
     * reread) and starts only its own Job. Engine calls happen on the lazy Job
     * outside this lock, so engine re-entrant callbacks can never deadlock the
     * ownership transaction.
     */
    private fun loadSelected(
        occurrence: QueueOccurrence,
        autoPlay: Boolean,
        replacementQueue: List<QueueOccurrence>? = null,
        from: PlaybackState? = null,
        requireCommandsEnabled: Boolean = false,
        requiredPlaybackRunId: Long? = null,
    ): Boolean = selectionGate.withLock {
        if (requireCommandsEnabled && !commandsEnabled.value) {
            return@withLock false
        }
        if (requiredPlaybackRunId != null &&
            (from == null ||
                activePlaybackRunGeneration != from.engineGeneration ||
                activePlaybackRunId != requiredPlaybackRunId))
            return@withLock false
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
                claimLoadingFrom(
                    from,
                    occurrence,
                    generation,
                    replacementQueue,
                )
            }
        if (claimed == null) {
            // The claim lost (the occurrence left the queue or the captured
            // prior state moved). Restore the displaced request and cancel
            // only this transaction's own lazy Job.
            selectionRequest.value = displaced
            request.job.cancel()
            return@withLock false
        }
        cancelCrossfadeLocked()
        cancelGaplessLocked()
        resetProgressCheckpointKey()
        publishRuntimeShuffleOrder(claimed, occurrence.id)
        timerStopClaim = null
        setSleepAttenuationLocked(selectionSleepAttenuationLocked(), generation)
            ?.let(::launchPlaybackGain)
        // Ownership transferred: supersede the captured displaced request.
        displaced?.job?.cancel()
        request.job.start()
        true
    }

    /**
     * Selects [occurrence] from the exact captured [PlaybackState]: the claim
     * publishes only when that captured state is still current, so callers that
     * derived a decision from it never commit against a newer state.
     */
    private fun loadSelectedFrom(
        captured: PlaybackState,
        occurrence: QueueOccurrence,
        autoPlay: Boolean,
        replacementQueue: List<QueueOccurrence>? = null,
        requiredPlaybackRunId: Long? = null,
    ): Boolean =
        loadSelected(
            occurrence,
            autoPlay,
            replacementQueue = replacementQueue,
            from = captured,
            requiredPlaybackRunId = requiredPlaybackRunId,
        )

    private fun claimLoadingFrom(
        from: PlaybackState,
        occurrence: QueueOccurrence,
        generation: Long,
        replacementQueue: List<QueueOccurrence>? = null,
    ): PlaybackState? {
        if (from.queue.none { it.id == occurrence.id }) return null
        val updated =
            (if (replacementQueue == null) from
                else from.copy(queue = replacementQueue))
                .copy(
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
                previous.queue.none { it.id == occurrence.id })
                return null
            val updated =
                (if (replacementQueue == null) previous
                    else previous.copy(queue = replacementQueue))
                    .copy(
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
            selectionGate.withLock {
                var settled: PlaybackState? = null
                while (settled == null) {
                    val state = _state.value
                    if (state.engineGeneration != generation ||
                        timerStopClaim?.generation == generation &&
                            state.status == PlaybackStatus.Stopped)
                        return@withLock null
                    val updated =
                        state.copy(
                            status = PlaybackStatus.Paused,
                            durationMillis =
                                loaded.durationMillis ?: state.durationMillis,
                        )
                    if (_state.compareAndSet(state, updated)) settled = updated
                }
                settled
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
            if (shouldPlay) {
                val stillPlayable = selectionGate.withLock {
                    ownsSelection(_state.value, generation, occurrence.id) &&
                        _state.value.status != PlaybackStatus.Stopped &&
                        timerStopClaim?.generation != generation
                }
                if (stillPlayable) {
                    applyCurrentPlaybackGainBeforePlay(generation)
                    beginPlaybackRun(generation)?.let { runId ->
                        engine.play(runId)
                        selectionGate.withLock { considerGaplessLocked() }
                    }
                }
            } else applyCurrentPlaybackGainBeforePlay(generation)
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
     * Claims [queue]/[current] and the repeat/shuffle modes as a paused session
     * state carrying [generation], CAS-published from the exact captured
     * [base]. Returns null when a concurrent transition superseded [base], in
     * which case the caller must not apply any engine effect. The caller holds
     * selectionGate so the gain owner changes with the session token.
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
        if (!_state.compareAndSet(base, updated)) return null
        cancelCrossfadeLocked()
        timerStopClaim = null
        if (current != null) {
            setSleepAttenuationLocked(
                    selectionSleepAttenuationLocked(), generation)
                ?.let(::launchPlaybackGain)
        } else {
            // Fence prior gain writes, but restore native unity only after
            // the caller clears the displaced media under the engine mutex.
            sleepAttenuation = 1f
            desiredPlaybackGain = 1f
            desiredPlaybackGainGeneration = generation
            playbackGainVersion++
        }
        return updated
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
    ): Boolean = selectionGate.withLock {
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
        if (!_state.compareAndSet(previous, updated)) return@withLock false
        publishRuntimeShuffleOrder(updated)
        revalidateCrossfadeLocked()
        considerCrossfadeLocked()
        emitImmediateCheckpoint(
            updated.toSessionSnapshot(), updated.checkpointRevision)
        true
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

    /**
     * Emits [state]'s checkpoint only while that exact state remains current.
     */
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
        setPendingAutoplayLocked(enabled, generation)
    }

    private fun setPendingAutoplayLocked(
        enabled: Boolean,
        generation: Long? = null,
    ): Boolean {
        val request =
            when {
                generation == null -> selectionRequest.value
                else ->
                    selectionRequest.value?.takeIf {
                        it.generation == generation
                    }
            } ?: return false
        val state = _state.value
        val owned =
            state.status == PlaybackStatus.Loading &&
                ownsSelection(state, request.generation, request.occurrenceId)
        if (owned) {
            request.playWhenLoaded.value = enabled
        }
        return owned
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

    private fun launchNonFatalEngineAction(action: suspend () -> Unit) {
        scope.launch {
            try {
                engineMutex.withLock {
                    cleanupCrossfadeWhileEngineLocked()
                    action()
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                playbackLog.e { throwable.stackTraceToString() }
            }
        }
    }

    private suspend fun runEngineAction(
        errorGeneration: Long,
        action: suspend () -> Unit,
    ) {
        try {
            engineMutex.withLock {
                cleanupCrossfadeWhileEngineLocked()
                val current = selectionGate.withLock {
                    !released &&
                        _state.value.engineGeneration == errorGeneration
                }
                if (current) action()
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
        currentId: String? = state.currentOccurrenceId,
    ): QueueOccurrence? {
        val order = effectiveOrder(state)
        if (order.isEmpty()) return null
        val currentIndex = order.indexOf(currentId)
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
    private fun stopAtCurrentTrackEnd(
        captured: PlaybackState,
        playbackRunId: Long,
    ): Boolean = selectionGate.withLock {
        val generation = captured.engineGeneration
        var published: PlaybackState? = null
        while (published == null) {
            val state = _state.value
            if (state != captured ||
                activePlaybackRunGeneration != generation ||
                activePlaybackRunId != playbackRunId)
                return@withLock false
            val candidate =
                state.copy(
                    status = PlaybackStatus.Stopped,
                    positionMillis =
                        state.durationMillis ?: max(0L, state.positionMillis),
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
        sleepAttenuation = 1f
        desiredPlaybackGain =
            replayGainFactor(published.currentTrack, replayGainMode)
        desiredPlaybackGainGeneration = generation
        playbackGainVersion++
        true
    }

    /** Applies a status callback only while [generation] owns the state. */
    public override fun onPlaybackStatus(
        generation: Long,
        status: PlaybackStatus
    ) {
        var statusGeneration = generation
        var gainRequest: PlaybackGainRequest? = null
        var metadataRefresh: PlayableTrack? = null
        selectionGate.withLock {
            if (released) return@withLock
            val gapless = gaplessTransition
            if (gapless != null &&
                gapless.started &&
                generation == gapless.incomingGeneration) {
                if (!ownsGaplessLocked(gapless)) {
                    cancelGaplessLocked()
                    return@withLock
                }
                if (status != PlaybackStatus.Playing) return@withLock
                val state = _state.value
                val promoted =
                    state.copy(
                        currentOccurrenceId = gapless.incoming.id,
                        engineGeneration = gapless.incomingGeneration,
                        status = PlaybackStatus.Playing,
                        positionMillis = 0L,
                        durationMillis = gapless.incomingDurationMillis,
                        error = null,
                        errorGeneration = null,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (!_state.compareAndSet(state, promoted)) return@withLock
                gapless.promoted = true
                gaplessTransition = null
                nativeGaplessTransition = null
                activePlaybackRunGeneration = gapless.incomingGeneration
                activePlaybackRunId = gapless.incomingRunId
                lastProgressCheckpointKey.value = null
                emitPlaybackStartedIfNew(promoted)
                emitImmediateCheckpoint(
                    promoted.toSessionSnapshot(), promoted.checkpointRevision)
                gainRequest =
                    recomputeDesiredPlaybackGainLocked(
                        gapless.incomingGeneration, force = true)
                metadataRefresh = promoted.currentTrack
                launchNonFatalEngineAction { crossfadeEngine?.finishGapless() }
                return@withLock
            }
            var transition = crossfadeTransition
            if (transition != null &&
                transition.started &&
                !transition.promoted &&
                generation == transition.incomingGeneration &&
                status == PlaybackStatus.Paused &&
                ownsCrossfadeLocked(transition)) {
                // Native transport already targets incoming after start, but
                // unconfirmed playback still belongs to outgoing authority.
                statusGeneration = transition.outgoingGeneration
                cancelCrossfadeLocked(settleDeferredCompletion = false)
                transition = null
            }
            if (transition != null &&
                transition.started &&
                generation == transition.incomingGeneration &&
                !transition.promoted) {
                if (!ownsCrossfadeLocked(transition)) {
                    cancelCrossfadeLocked()
                    return@withLock
                }
                if (status != PlaybackStatus.Playing) return@withLock
                val state = _state.value
                val promoted =
                    state.copy(
                        currentOccurrenceId = transition.incoming.id,
                        engineGeneration = transition.incomingGeneration,
                        status = PlaybackStatus.Playing,
                        positionMillis = transition.incomingPositionMillis,
                        durationMillis = transition.incomingDurationMillis,
                        error = null,
                        errorGeneration = null,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (!_state.compareAndSet(state, promoted)) return@withLock
                transition.promoted = true
                transition.incomingPositionUpdatedAtMillis =
                    crossfadeRuntime.nowMillis()
                transition.incomingAdvancing = true
                activePlaybackRunGeneration = transition.incomingGeneration
                activePlaybackRunId = transition.incomingRunId
                selectionRequest.value
                    ?.takeIf {
                        it.generation == transition.outgoingGeneration
                    }
                    ?.let { selectionRequest.value = null }
                lastProgressCheckpointKey.value =
                    ProgressCheckpointKey(
                        generation,
                        transition.incoming.id,
                        transition.incomingPositionMillis / 1_000L,
                    )
                emitPlaybackStartedIfNew(promoted)
                emitImmediateCheckpoint(
                    promoted.toSessionSnapshot(), promoted.checkpointRevision)
                gainRequest =
                    recomputeDesiredPlaybackGainLocked(generation, force = true)
                metadataRefresh = promoted.currentTrack
                if (transition.outgoingCompleted) cancelCrossfadeLocked()
                else startCrossfadeTicksLocked(transition)
                return@withLock
            }
            if (timerStopClaim?.generation == statusGeneration &&
                _state.value.status == PlaybackStatus.Stopped)
                return@withLock
            var published: PlaybackState? = null
            while (published == null) {
                val state = _state.value
                if (state.engineGeneration != statusGeneration) return@withLock
                if (state.status == PlaybackStatus.Error &&
                    status != PlaybackStatus.Error)
                    return@withLock
                val updated = state.copy(status = status, error = null)
                if (_state.compareAndSet(state, updated)) {
                    published = updated
                }
            }
            if (transition != null &&
                transition.started &&
                !transition.promoted &&
                statusGeneration == transition.outgoingGeneration &&
                status == PlaybackStatus.Paused) {
                // An async native start may be canceled by the pause. Keeping
                // started=true would prevent both a retry and normal
                // completion.
                cancelCrossfadeLocked(settleDeferredCompletion = false)
                transition = null
            }
            if (transition != null) {
                val now = crossfadeRuntime.nowMillis()
                if (transition.promoted &&
                    statusGeneration == transition.incomingGeneration) {
                    transition.incomingPositionMillis =
                        incomingPositionLocked(transition)
                    transition.incomingPositionUpdatedAtMillis = now
                    transition.incomingAdvancing =
                        status == PlaybackStatus.Playing
                }
                if (!transition.promoted &&
                    statusGeneration == transition.outgoingGeneration ||
                    transition.promoted &&
                        statusGeneration == transition.incomingGeneration) {
                    transition.outgoingPositionMillis =
                        transition.outgoingDurationMillis -
                            outgoingRemainingLocked(transition)
                    transition.outgoingPositionUpdatedAtMillis = now
                    transition.outgoingAdvancing =
                        status == PlaybackStatus.Playing
                }
            }
            emitPlaybackStartedIfNew(checkNotNull(published))
            gainRequest =
                updateCompletionFadeLocked(checkNotNull(published))
                    ?: recomputeDesiredPlaybackGainLocked(statusGeneration)
            if (status == PlaybackStatus.Stopped ||
                status == PlaybackStatus.Error) {
                cancelCrossfadeLocked()
            } else {
                considerCrossfadeLocked()
                if (crossfadeTransition == null) considerGaplessLocked()
            }
            if (statusGeneration == _state.value.engineGeneration &&
                status == PlaybackStatus.Paused) {
                val gapless = gaplessTransition
                if (gapless != null &&
                    generation == gapless.outgoingGeneration &&
                    _state.value.currentOccurrenceId == gapless.outgoing.id) {
                    cancelGaplessLocked()
                }
            }
            if (status == PlaybackStatus.Paused ||
                status == PlaybackStatus.Playing) {
                metadataRefresh = published.currentTrack
            }
        }
        gainRequest?.let(::launchPlaybackGain)
        metadataRefresh?.let { track ->
            launchNonFatalEngineAction {
                val stillCurrent = selectionGate.withLock {
                    val state = _state.value
                    state.engineGeneration == statusGeneration &&
                        state.currentTrack == track
                }
                if (stillCurrent)
                    engine.refreshLoadedMetadata(track, statusGeneration)
            }
        }
    }

    /**
     * Buffers a new actual-playing event after its state claim is committed.
     */
    private fun emitPlaybackStartedIfNew(state: PlaybackState) {
        if (state.status != PlaybackStatus.Playing) return
        val occurrence = state.currentOccurrence ?: return
        val key = PlaybackStartedKey(state.engineGeneration, occurrence.id)
        if (lastPlaybackStartedKey == key) return
        if (playbackStartedChannel
            .trySend(
                PlaybackStarted(
                    key.generation, key.occurrenceId, occurrence.track.id),
            )
            .isSuccess) {
            lastPlaybackStartedKey = key
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
        var gainRequest: PlaybackGainRequest? = null
        selectionGate.withLock {
            if (released) return@withLock
            val transition = crossfadeTransition
            if (transition != null) {
                if (generation == transition.incomingGeneration) {
                    transition.incomingPositionMillis = max(0L, positionMillis)
                    transition.incomingPositionUpdatedAtMillis =
                        crossfadeRuntime.nowMillis()
                    transition.incomingDurationMillis =
                        durationMillis ?: transition.incomingDurationMillis
                    if (!transition.promoted) return@withLock
                } else if (generation == transition.outgoingGeneration) {
                    transition.outgoingPositionMillis = max(0L, positionMillis)
                    transition.outgoingPositionUpdatedAtMillis =
                        crossfadeRuntime.nowMillis()
                    if (transition.promoted) return@withLock
                }
            }
            if (timerStopClaim?.generation == generation &&
                _state.value.status == PlaybackStatus.Stopped)
                return@withLock
            var applied: PlaybackState? = null
            while (applied == null) {
                val state = _state.value
                if (state.engineGeneration != generation) return@withLock
                val candidate =
                    state.copy(
                        positionMillis = max(0L, positionMillis),
                        durationMillis = durationMillis ?: state.durationMillis,
                        checkpointRevision = reserveCheckpointRevision(),
                    )
                if (_state.compareAndSet(state, candidate)) {
                    applied = candidate
                }
            }
            val settled = checkNotNull(applied)
            gainRequest =
                updateCompletionFadeLocked(settled)
                    ?: recomputeDesiredPlaybackGainLocked(generation)
            if (transition?.promoted == true &&
                transition.incomingPositionMillis >= transition.windowMillis) {
                cancelCrossfadeLocked()
            } else {
                considerCrossfadeLocked()
                if (crossfadeTransition == null) considerGaplessLocked()
            }
            val currentId = settled.currentOccurrenceId ?: return@withLock
            if (settled.status != PlaybackStatus.Playing) return@withLock
            val key =
                ProgressCheckpointKey(
                    generation, currentId, max(0L, positionMillis) / 1_000L)
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
        gainRequest?.let(::launchPlaybackGain)
    }

    /**
     * Advances or stops the queue according to the selected repeat mode. Every
     * decision derives from a state that still carries [generation] and is
     * committed from that exact captured state, so a stale completion can
     * neither advance a replacement's queue nor stop its session.
     */
    public override fun onPlaybackCompleted(
        generation: Long,
        playbackRunId: Long
    ) {
        var completionTimerToken: Long? = null
        var gaplessTimerStop: TimerStopRequest? = null
        val consumedByCrossfade = selectionGate.withLock {
            if (released) return@withLock true
            // Cancellation removes the transition before native rollback can
            // run. Duplicate receipts in that gap still belong to the original
            // deferred event, never to a newly armed completion timer.
            if (lastDeferredCompletionGeneration == generation &&
                lastDeferredCompletionRunId == playbackRunId)
                return@withLock true
            val gapless = gaplessTransition
            if (gapless != null &&
                generation == gapless.outgoingGeneration &&
                playbackRunId == gapless.outgoingRunId) {
                if (gapless.outgoingCompleted) return@withLock true
                if (!ownsGaplessLocked(gapless) || !gapless.prepared) {
                    val preparationError = gapless.preparationError
                    if (preparationError == null) cancelGaplessLocked()
                    else {
                        val current = _state.value
                        val timer = activeSleepTimer
                        var timerStop: TimerStopRequest? = null
                        if (timer?.mode == SleepTimerMode.TrackCount &&
                            current.status == PlaybackStatus.Playing &&
                            current.currentOccurrenceId ==
                                gapless.outgoing.id) {
                            val key =
                                SleepTimerCompletionKey(
                                    timer.token,
                                    generation,
                                    gapless.outgoing.id,
                                    playbackRunId,
                                )
                            if (timer.consumedCompletionKey != key) {
                                timer.consumedCompletionKey = key
                                val remaining =
                                    checkNotNull(timer.remainingTracks) - 1
                                timer.remainingTracks = remaining
                                if (remaining == 0) {
                                    timerStop =
                                        claimTimerStopLocked(
                                            timer, current, atTrackEnd = true)
                                } else {
                                    _sleepTimerState.value = timer.toState()
                                    setSleepAttenuationLocked(
                                        1f, generation, force = true)
                                }
                            }
                        }
                        if (timerStop != null) {
                            gaplessTransition = null
                            gapless.preparationJob?.cancel()
                            gaplessTimerStop = timerStop
                            return@withLock true
                        }
                        gaplessTransition = null
                        nativeGaplessTransition = null
                        gapless.preparationJob?.cancel()
                        activePlaybackRunGeneration = null
                        activePlaybackRunId = null
                        val failed =
                            _state.value.copy(
                                currentOccurrenceId = gapless.incoming.id,
                                engineGeneration = gapless.incomingGeneration,
                                status = PlaybackStatus.Error,
                                positionMillis = 0L,
                                durationMillis =
                                    gapless.incoming.track.durationMillis,
                                error = preparationError,
                                errorGeneration = gapless.incomingGeneration,
                                checkpointRevision =
                                    reserveCheckpointRevision(),
                            )
                        _state.value = failed
                        emitImmediateCheckpoint(
                            failed.toSessionSnapshot(),
                            failed.checkpointRevision)
                        return@withLock true
                    }
                } else if (!gapless.started) {
                    val current = _state.value
                    val timer = activeSleepTimer
                    if (timer?.mode == SleepTimerMode.TrackCount &&
                        current.status == PlaybackStatus.Playing &&
                        current.currentOccurrenceId == gapless.outgoing.id) {
                        val key =
                            SleepTimerCompletionKey(
                                timer.token,
                                generation,
                                gapless.outgoing.id,
                                playbackRunId,
                            )
                        if (timer.consumedCompletionKey != key) {
                            timer.consumedCompletionKey = key
                            val remaining =
                                checkNotNull(timer.remainingTracks) - 1
                            timer.remainingTracks = remaining
                            if (remaining == 0) {
                                gaplessTimerStop =
                                    claimTimerStopLocked(
                                        timer, current, atTrackEnd = true)
                                cancelGaplessLocked()
                            } else {
                                _sleepTimerState.value = timer.toState()
                                setSleepAttenuationLocked(
                                    1f, generation, force = true)
                            }
                        }
                    }
                    if (gaplessTimerStop != null) return@withLock true
                    gapless.started = true
                    gapless.outgoingCompleted = true
                    gapless.incomingRunId = ++nextPlaybackRunId
                    launchNonFatalEngineAction {
                        val admitted = selectionGate.withLock {
                            nativeGaplessTransition = gapless
                            gaplessTransition === gapless && gapless.prepared
                        }
                        if (admitted) {
                            val gains =
                                selectionGate.withLock {
                                    _state.value
                                        .takeIf {
                                            gaplessTransition === gapless &&
                                                it.engineGeneration ==
                                                    gapless
                                                        .outgoingGeneration &&
                                                it.currentOccurrenceId ==
                                                    gapless.outgoing.id
                                        }
                                        ?.let {
                                            val attenuation = sleepAttenuation
                                            replayGainFactor(
                                                gapless.outgoing.track,
                                                replayGainMode) * attenuation to
                                                replayGainFactor(
                                                    gapless.incoming.track,
                                                    replayGainMode) *
                                                    attenuation
                                        }
                                } ?: return@launchNonFatalEngineAction
                            try {
                                crossfadeEngine?.setCrossfadeGains(
                                    gains.first.coerceIn(0f, 1f),
                                    gains.second.coerceIn(0f, 1f))
                                crossfadeEngine?.startIncomingGapless(
                                    gapless.incomingRunId)
                            } catch (failure: Throwable) {
                                if (failure is CancellationException)
                                    throw failure
                                selectionGate.withLock {
                                    failGaplessPromotionLocked(
                                        gapless,
                                        (failure as? PlaybackFailureException)
                                            ?.error
                                            ?: PlaybackError(
                                                "Playback failed",
                                                failure.message),
                                    )
                                }
                            }
                        }
                    }
                    return@withLock true
                }
            }
            completionTimerToken = activeSleepTimer?.token
            val transition = crossfadeTransition ?: return@withLock false
            if (generation == transition.outgoingGeneration &&
                playbackRunId == transition.outgoingRunId) {
                if (transition.promoted) {
                    cancelCrossfadeLocked()
                    return@withLock true
                }
                if (transition.started) {
                    lastDeferredCompletionGeneration = generation
                    lastDeferredCompletionRunId = playbackRunId
                    transition.outgoingCompleted = true
                    transition.outgoingCompletionTimerToken =
                        completionTimerToken
                    return@withLock true
                }
                // Preparation did not start in time; ordinary outgoing
                // completion still owns traversal and any completion timer.
                cancelCrossfadeLocked()
            } else if (generation == transition.incomingGeneration) {
                if (!transition.promoted) return@withLock true
                if (playbackRunId == transition.incomingRunId)
                    cancelCrossfadeLocked()
            }
            false
        }
        if (gaplessTimerStop != null) {
            runTimerStop(checkNotNull(gaplessTimerStop))
            return
        }
        if (consumedByCrossfade) return
        settlePlaybackCompletion(
            generation, playbackRunId, completionTimerToken)
    }

    private fun settlePlaybackCompletion(
        generation: Long,
        playbackRunId: Long,
        completionTimerToken: Long?,
        deferredOutgoingOccurrenceId: String? = null,
    ) {
        var admittedCompletion: SleepTimerCompletionKey? = null
        while (true) {
            val state = _state.value
            if (state.engineGeneration != generation ||
                state.status == PlaybackStatus.Stopped)
                return
            var timerStop: TimerStopRequest? = null
            var gainReset: PlaybackGainRequest? = null
            val admitted = selectionGate.withLock {
                if (released) return@withLock false
                val current = _state.value
                if (current.engineGeneration != generation ||
                    current.status == PlaybackStatus.Stopped ||
                    current.currentOccurrenceId != state.currentOccurrenceId)
                    return@withLock false
                if (activePlaybackRunGeneration != generation ||
                    activePlaybackRunId != playbackRunId)
                    return@withLock false
                if (deferredOutgoingOccurrenceId != null &&
                    (current.currentOccurrenceId !=
                        deferredOutgoingOccurrenceId ||
                        current.status != PlaybackStatus.Playing))
                    return@withLock false
                val timer = activeSleepTimer
                // A completion received before a timer was armed cannot be
                // charged to that new timer during deferred rollback.
                if (timer?.mode != SleepTimerMode.TrackCount ||
                    timer.token != completionTimerToken)
                    return@withLock true
                if (current.status != PlaybackStatus.Playing)
                    return@withLock false
                val occurrenceId =
                    current.currentOccurrenceId ?: return@withLock true
                val key =
                    SleepTimerCompletionKey(
                        timer.token, generation, occurrenceId, playbackRunId)
                if (timer.consumedCompletionKey == key) {
                    return@withLock admittedCompletion == key
                }
                if (admittedCompletion != null) return@withLock false
                admittedCompletion = key
                timer.consumedCompletionKey = key
                val remaining = checkNotNull(timer.remainingTracks) - 1
                timer.remainingTracks = remaining
                if (remaining == 0) {
                    timerStop =
                        claimTimerStopLocked(timer, current, atTrackEnd = true)
                } else {
                    _sleepTimerState.value = timer.toState()
                    gainReset =
                        setSleepAttenuationLocked(1f, generation, force = true)
                }
                true
            }
            if (!admitted) return
            if (timerStop != null) {
                runTimerStop(checkNotNull(timerStop))
                return
            }
            gainReset?.let(::launchPlaybackGain)
            val committed =
                when (state.repeatMode) {
                    RepeatMode.RepeatOne -> {
                        val current = state.currentOccurrence
                        if (current == null) {
                            stopAtCurrentTrackEnd(state, playbackRunId)
                        } else if (loadSelectedFrom(
                            state,
                            current,
                            autoPlay = true,
                            requiredPlaybackRunId = playbackRunId)) {
                            emitImmediateCheckpoint()
                            true
                        } else {
                            false
                        }
                    }

                    RepeatMode.RepeatPlaylist -> {
                        val next = nextTrackFrom(state, wrap = true)
                        if (next == null) {
                            stopAtCurrentTrackEnd(state, playbackRunId)
                        } else if (loadSelectedFrom(
                            state,
                            next,
                            autoPlay = true,
                            requiredPlaybackRunId = playbackRunId)) {
                            emitImmediateCheckpoint()
                            true
                        } else {
                            false
                        }
                    }

                    RepeatMode.StopAfterCurrent ->
                        stopAtCurrentTrackEnd(state, playbackRunId)

                    RepeatMode.StopAfterQueue -> {
                        val next = nextTrackFrom(state, wrap = false)
                        if (next == null) {
                            stopAtCurrentTrackEnd(state, playbackRunId)
                        } else if (loadSelectedFrom(
                            state,
                            next,
                            autoPlay = true,
                            requiredPlaybackRunId = playbackRunId)) {
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
            if (released) return@withLock
            val gapless = gaplessTransition
            if (gapless != null &&
                generation == gapless.incomingGeneration &&
                gapless.started &&
                !gapless.promoted) {
                failGaplessPromotionLocked(gapless, error)
                return@withLock
            }
            val transition = crossfadeTransition
            if (transition != null &&
                generation == transition.incomingGeneration &&
                !transition.promoted) {
                cancelCrossfadeLocked()
                return@withLock
            }
            if (transition != null &&
                (generation == transition.incomingGeneration ||
                    generation == transition.outgoingGeneration)) {
                cancelCrossfadeLocked()
            }
            if (timerStopClaim?.generation == generation &&
                _state.value.status == PlaybackStatus.Stopped)
                return@withLock
            if (activePlaybackRunGeneration == generation)
                activePlaybackRunId = null
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

    private fun failGaplessPromotionLocked(
        transition: GaplessTransition,
        error: PlaybackError,
    ) {
        if (gaplessTransition !== transition ||
            !_state.value.let {
                it.engineGeneration == transition.outgoingGeneration &&
                    it.currentOccurrenceId == transition.outgoing.id
            })
            return
        gaplessTransition = null
        nativeGaplessTransition = null
        transition.promoted = true
        activePlaybackRunGeneration = null
        activePlaybackRunId = null
        val previous = _state.value
        _state.value =
            previous.copy(
                currentOccurrenceId = transition.incoming.id,
                engineGeneration = transition.incomingGeneration,
                status = PlaybackStatus.Error,
                positionMillis = 0L,
                durationMillis = transition.incomingDurationMillis,
                error = error,
                errorGeneration = transition.incomingGeneration,
                checkpointRevision = reserveCheckpointRevision(),
            )
        launchNonFatalEngineAction { crossfadeEngine?.cancelPreparedIncoming() }
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

    /** Routes external Play through controller-owned playback-run admission. */
    public override fun onPlayRequested(generation: Long) {
        if (commandsEnabled.value &&
            _state.value.engineGeneration == generation)
            play()
    }

    /** Routes external Pause through the canonical transport command. */
    public override fun onPauseRequested(generation: Long) {
        pauseOwner(generation)
    }

    /** Routes external Stop through the canonical transport command. */
    public override fun onStopRequested(generation: Long) {
        stopOwner(generation)
    }

    /** Routes external seek through the controller's bounded position. */
    public override fun onSeekRequested(
        generation: Long,
        positionMillis: Long
    ) {
        seekOwner(positionMillis, generation)
    }

    private fun transportOwnerLocked(generation: Long? = null): PlaybackState? {
        if (released || !commandsEnabled.value) return null
        val state = _state.value
        if (generation == null || state.engineGeneration == generation)
            return state
        val transition = crossfadeTransition ?: return null
        if (transition.started &&
            !transition.promoted &&
            generation == transition.incomingGeneration &&
            activePlaybackRunGeneration == transition.outgoingGeneration &&
            activePlaybackRunId == transition.outgoingRunId &&
            ownsCrossfadeLocked(transition))
            return state
        return null
    }

    private fun skipOwner(generation: Long, previous: Boolean) {
        val ownedGeneration =
            selectionGate.withLock {
                val owner =
                    transportOwnerLocked(generation) ?: return@withLock null
                cancelCrossfadeLocked(settleDeferredCompletion = false)
                owner.engineGeneration
            } ?: return
        while (true) {
            if (!commandsEnabled.value) return
            val state = _state.value
            if (state.engineGeneration != ownedGeneration) return
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
    /** App-local gain applied to this fake player. */
    public var playbackGain: Float = 1f
        private set

    /** Whether [release] has been invoked. */
    public var released: Boolean = false
        private set

    private var playbackRunId: Long = 0L

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

    /** Applies gain to the in-memory player without changing system volume. */
    public override fun setPlaybackGain(gain: Float) {
        require(gain.isFinite() && gain in 0f..1f)
        playbackGain = gain
    }

    /** Reports that the loaded media is playing. */
    public override fun play(playbackRunId: Long) {
        requireNotNull(loaded) { "No track loaded" }
        this.playbackRunId = playbackRunId
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
        listener?.onPlaybackCompleted(generation, playbackRunId)
    }

    /** Returns the generation currently owned by this fake engine. */
    public fun activeGenerationForTest(): Long = generation

    /** Marks this engine released and clears its loaded media. */
    public override fun release() {
        released = true
        loaded = null
        playbackGain = 1f
    }
}
