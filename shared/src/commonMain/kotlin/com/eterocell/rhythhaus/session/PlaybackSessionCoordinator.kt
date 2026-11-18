package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.toPlayableTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class PlaybackSessionPhase {
    NotRestored,
    Restoring,
    Ready,
    FailedSafe
}

enum class PlaybackSessionReconcileResult {
    Applied,
    FailedSafeApplied
}

internal fun interface PlaybackSessionReconciler {
    suspend fun reconcile(
        tracks: List<LibraryTrack>
    ): PlaybackSessionReconcileResult
}

internal class PlaybackSessionCoordinator(
    private val controller: PlaybackSessionController,
    private val store: PlaybackSessionStore,
    private val processScope: CoroutineScope,
) : PlaybackSessionReconciler {
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val _phase = MutableStateFlow(PlaybackSessionPhase.NotRestored)
    val phase: StateFlow<PlaybackSessionPhase> = _phase.asStateFlow()
    private var durableSnapshot = PlaybackSessionSnapshot()
    private var checkpointCollectionStarted = false
    private var checkpointCollectorJob: Job? = null
    private val checkpointCollectorReady = CompletableDeferred<Unit>()
    private var terminalFailure: Throwable? = null
    private var highestPersistedSnapshotRevision: Long? = null
    private var highestPersistedPositionRevision: Long? = null
    private val actorJob: Job

    init {
        actorJob =
            processScope.launch(start = CoroutineStart.UNDISPATCHED) {
                runActor()
            }
    }

    suspend fun restoreOnce(tracks: List<PlayableTrack>) {
        val reply = CompletableDeferred<Unit>()
        enqueue(Command.Restore(tracks, reply))
        reply.await()
    }

    override suspend fun reconcile(
        tracks: List<LibraryTrack>
    ): PlaybackSessionReconcileResult {
        val reply = CompletableDeferred<PlaybackSessionReconcileResult>()
        enqueue(Command.Reconcile(tracks, reply))
        return reply.await()
    }

    fun accept(checkpoint: PlaybackCheckpoint) {
        commands.trySend(Command.Checkpoint(checkpoint))
    }

    suspend fun flush() {
        checkpointCollectorReady.await()
        controller.awaitCheckpointFence()
        val reply = CompletableDeferred<Unit>()
        enqueue(Command.Flush(reply))
        reply.await()
    }

    private fun enqueue(command: Command) {
        if (commands.trySend(command).isSuccess) return
        throw terminalFailure
            ?: CancellationException(
                "Playback session coordinator is not running")
    }

    private suspend fun runActor() {
        var pending: Command? = null
        var active: Command? = null
        var terminal: Throwable? = null
        try {
            while (true) {
                val command =
                    pending ?: commands.receiveCatching().getOrNull() ?: return
                active = command
                pending = null
                if (command is Command.Checkpoint) {
                    var newestImmediate: PlaybackCheckpoint.Immediate? = null
                    var newestPosition: PlaybackCheckpoint.Position? = null
                    fun offer(checkpoint: PlaybackCheckpoint) {
                        when (checkpoint) {
                            is PlaybackCheckpoint.Immediate ->
                                newestImmediate =
                                    newerImmediate(newestImmediate, checkpoint)

                            is PlaybackCheckpoint.Position ->
                                newestPosition =
                                    newerPosition(newestPosition, checkpoint)
                        }
                    }
                    offer(command.checkpoint)
                    while (true) {
                        val next = commands.tryReceive().getOrNull() ?: break
                        if (next is Command.Checkpoint) {
                            offer(next.checkpoint)
                        } else {
                            pending = next
                            break
                        }
                    }
                    // The structural lane is written first so a queue change
                    // that was captured before a newer position can never be
                    // dropped by coalescing the two together.
                    val immediate = newestImmediate
                    val immediateSaved =
                        immediate?.let {
                            persistSnapshot(
                                it.snapshot,
                                it.revision,
                                force = false,
                            )
                        } ?: false
                    val position = newestPosition
                    if (position != null) {
                        val immediateRevision = immediate?.revision
                        val coveredByImmediate =
                            immediateSaved &&
                                immediateRevision != null &&
                                immediateRevision >= position.revision
                        if (!coveredByImmediate) persistPosition(position)
                    }
                } else {
                    processBarrier(command)
                }
                active = null
            }
        } catch (cancelled: CancellationException) {
            terminal = cancelled
            throw cancelled
        } catch (throwable: Throwable) {
            terminal = throwable
            throw throwable
        } finally {
            terminate(
                terminal
                    ?: terminalFailure
                    ?: CancellationException(
                        "Playback session coordinator stopped"),
                active,
                pending,
            )
        }
    }

    private fun newerImmediate(
        first: PlaybackCheckpoint.Immediate?,
        second: PlaybackCheckpoint.Immediate,
    ): PlaybackCheckpoint.Immediate {
        if (first == null) return second
        val firstRevision = first.revision
        val secondRevision = second.revision
        return when {
            firstRevision == null && secondRevision == null -> second
            firstRevision == null -> second
            secondRevision == null -> first
            firstRevision > secondRevision -> first
            else -> second
        }
    }

    private fun newerPosition(
        first: PlaybackCheckpoint.Position?,
        second: PlaybackCheckpoint.Position,
    ): PlaybackCheckpoint.Position =
        if (first == null || second.revision >= first.revision) second
        else first

    private suspend fun processBarrier(command: Command) {
        when (command) {
            is Command.Restore -> restore(command)
            is Command.Reconcile -> reconcile(command)
            is Command.Flush -> command.reply.complete(Unit)
            is Command.Checkpoint ->
                error("Checkpoint runs are handled before barriers")
        }
    }

    private suspend fun restore(command: Command.Restore) {
        if (_phase.value != PlaybackSessionPhase.FailedSafe)
            _phase.value = PlaybackSessionPhase.Restoring
        controller.setCommandsEnabled(false)
        var cancelled = false
        try {
            if (_phase.value == PlaybackSessionPhase.FailedSafe) return
            val persisted =
                try {
                    store.read()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    enterFailedSafe()
                    applyEmptyPaused(command.tracks)
                    return
                }
            val normalized =
                controller.restoreSession(persisted, command.tracks)
            persistRevisioned(normalized)
        } catch (cancellation: CancellationException) {
            cancelled = true
            command.reply.completeExceptionally(cancellation)
            throw cancellation
        } catch (_: Throwable) {
            enterFailedSafe()
            applyEmptyPaused(command.tracks)
        } finally {
            if (!cancelled) startCheckpointCollection()
            controller.setCommandsEnabled(true)
            if (_phase.value != PlaybackSessionPhase.FailedSafe)
                _phase.value = PlaybackSessionPhase.Ready
            if (!command.reply.isCompleted) command.reply.complete(Unit)
        }
    }

    private suspend fun reconcile(command: Command.Reconcile) {
        try {
            val reconciled =
                controller.reconcileSession(
                    command.tracks.map { it.toPlayableTrack() })
            val result =
                if (_phase.value == PlaybackSessionPhase.FailedSafe) {
                    PlaybackSessionReconcileResult.FailedSafeApplied
                } else if (persistRevisioned(reconciled)) {
                    PlaybackSessionReconcileResult.Applied
                } else {
                    PlaybackSessionReconcileResult.FailedSafeApplied
                }
            command.reply.complete(result)
        } catch (cancelled: CancellationException) {
            command.reply.completeExceptionally(cancelled)
            throw cancelled
        } catch (_: Throwable) {
            enterFailedSafe()
            command.reply.complete(
                PlaybackSessionReconcileResult.FailedSafeApplied)
        }
    }

    private fun startCheckpointCollection() {
        if (checkpointCollectionStarted) return
        checkpointCollectionStarted = true
        checkpointCollectorJob =
            processScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    controller.checkpoints.collect(::accept)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (throwable: Throwable) {
                    terminalFailure = throwable
                    commands.close(throwable)
                    actorJob.cancel(
                        cancellation(
                            "Playback checkpoint collector failed", throwable))
                }
            }
        val collector = checkpointCollectorJob
        if (collector?.isActive == true) {
            checkpointCollectorReady.complete(Unit)
        } else if (!checkpointCollectorReady.isCompleted) {
            checkpointCollectorReady.completeExceptionally(
                terminalFailure
                    ?: CancellationException(
                        "Playback checkpoint collector did not start"),
            )
        }
    }

    private suspend fun persist(snapshot: PlaybackSessionSnapshot): Boolean {
        if (_phase.value == PlaybackSessionPhase.FailedSafe) return false
        return try {
            store.save(snapshot)
            durableSnapshot = snapshot
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            enterFailedSafe()
            false
        }
    }

    /**
     * Persists a complete snapshot and its queue.
     *
     * Checkpoints may be superseded by a newer full snapshot, while restore and
     * reconcile publications are authoritative and always persist. A position
     * that was already written for the same current occurrence is re-applied
     * after a delayed older snapshot so coalescing can preserve both the newest
     * structure and the newest position.
     */
    private suspend fun persistSnapshot(
        snapshot: PlaybackSessionSnapshot,
        revision: Long?,
        force: Boolean,
    ): Boolean {
        if (_phase.value == PlaybackSessionPhase.FailedSafe) return false
        val snapshotWatermark = highestPersistedSnapshotRevision
        if (!force) {
            if (revision == null) {
                if (snapshotWatermark != null) return false
            } else if (snapshotWatermark != null &&
                revision < snapshotWatermark) {
                return false
            }
        }
        val previousDurable = durableSnapshot
        val preservedOccurrenceId =
            previousDurable.currentOccurrenceId?.takeIf {
                it == snapshot.currentOccurrenceId
            }
        val positionWatermark = highestPersistedPositionRevision
        val preservesNewerPosition =
            positionWatermark != null &&
                (revision == null || positionWatermark > revision) &&
                preservedOccurrenceId != null
        val effectiveSnapshot =
            if (preservesNewerPosition) {
                snapshot.copy(positionMillis = previousDurable.positionMillis)
            } else snapshot
        if (!persist(effectiveSnapshot)) return false
        if (revision != null) {
            highestPersistedSnapshotRevision =
                maxOf(snapshotWatermark ?: revision, revision)
            if (!preservesNewerPosition) {
                highestPersistedPositionRevision =
                    maxOf(positionWatermark ?: revision, revision)
            }
        }
        return true
    }

    private suspend fun persistRevisioned(
        publication: RevisionedPlaybackSessionSnapshot
    ): Boolean =
        persistSnapshot(
            publication.snapshot, publication.revision, force = true)

    /**
     * Applies a position-only checkpoint without touching the queue.
     *
     * The revision guard rejects delayed progress that a newer full snapshot
     * already covers, and the durable current identity guard rejects progress
     * emitted for an occurrence that a structural save has replaced.
     */
    private suspend fun persistPosition(
        checkpoint: PlaybackCheckpoint.Position
    ): Boolean {
        if (_phase.value == PlaybackSessionPhase.FailedSafe) return false
        val watermark = highestPersistedPositionRevision
        if (watermark != null && checkpoint.revision <= watermark) return false
        if (durableSnapshot.currentOccurrenceId !=
            checkpoint.currentOccurrenceId) {
            return false
        }
        if (!writePosition(
            checkpoint.currentOccurrenceId,
            checkpoint.positionMillis,
        )) {
            return false
        }
        highestPersistedPositionRevision =
            maxOf(watermark ?: checkpoint.revision, checkpoint.revision)
        return true
    }

    /** Writes a position for an already validated current occurrence. */
    private suspend fun writePosition(
        currentOccurrenceId: String,
        positionMillis: Long,
    ): Boolean {
        val sanitizedPositionMillis = positionMillis.coerceAtLeast(0L)
        return try {
            store.savePosition(currentOccurrenceId, sanitizedPositionMillis)
            durableSnapshot =
                durableSnapshot.copy(positionMillis = sanitizedPositionMillis)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            enterFailedSafe()
            false
        }
    }

    private suspend fun applyEmptyPaused(tracks: List<PlayableTrack>) {
        try {
            controller.restoreSession(PlaybackSessionSnapshot(), tracks)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {}
    }

    private fun enterFailedSafe() {
        _phase.value = PlaybackSessionPhase.FailedSafe
    }

    private fun terminate(
        failure: Throwable,
        active: Command?,
        pending: Command?
    ) {
        terminalFailure = failure
        commands.close(failure)
        checkpointCollectorReady.completeExceptionally(failure)
        checkpointCollectorJob?.cancel(
            cancellation("Playback session coordinator stopped", failure))
        active?.completeExceptionally(failure)
        pending?.completeExceptionally(failure)
        while (true) {
            val queued = commands.tryReceive().getOrNull() ?: break
            queued.completeExceptionally(failure)
        }
    }

    private fun Command.completeExceptionally(failure: Throwable) {
        when (this) {
            is Command.Restore -> reply.completeExceptionally(failure)
            is Command.Reconcile -> reply.completeExceptionally(failure)
            is Command.Flush -> reply.completeExceptionally(failure)
            is Command.Checkpoint -> Unit
        }
    }

    private sealed interface Command {
        data class Checkpoint(val checkpoint: PlaybackCheckpoint) : Command

        data class Restore(
            val tracks: List<PlayableTrack>,
            val reply: CompletableDeferred<Unit>
        ) : Command

        data class Reconcile(
            val tracks: List<LibraryTrack>,
            val reply: CompletableDeferred<PlaybackSessionReconcileResult>,
        ) : Command

        data class Flush(val reply: CompletableDeferred<Unit>) : Command
    }
}

private fun cancellation(
    message: String,
    cause: Throwable
): CancellationException = CancellationException(message, cause)

// Library extraction
