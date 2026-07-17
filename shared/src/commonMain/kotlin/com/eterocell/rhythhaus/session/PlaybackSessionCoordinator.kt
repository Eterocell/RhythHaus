package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
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

enum class PlaybackSessionPhase { NotRestored, Restoring, Ready, FailedSafe }

enum class PlaybackSessionReconcileResult { Applied, FailedSafeApplied }

internal fun interface PlaybackSessionReconciler {
    suspend fun reconcile(tracks: List<LibraryTrack>): PlaybackSessionReconcileResult
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
    private val actorJob: Job

    init {
        actorJob = processScope.launch(start = CoroutineStart.UNDISPATCHED) { runActor() }
    }

    suspend fun restoreOnce(tracks: List<PlayableTrack>) {
        val reply = CompletableDeferred<Unit>()
        enqueue(Command.Restore(tracks, reply))
        reply.await()
    }

    override suspend fun reconcile(tracks: List<LibraryTrack>): PlaybackSessionReconcileResult {
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
        throw terminalFailure ?: CancellationException("Playback session coordinator is not running")
    }

    private suspend fun runActor() {
        var pending: Command? = null
        var active: Command? = null
        var terminal: Throwable? = null
        var highestPersistedCheckpointRevision: Long? = null
        try {
            while (true) {
                val command = pending ?: commands.receiveCatching().getOrNull() ?: return
                active = command
                pending = null
                if (command is Command.Checkpoint) {
                    var newest = command.checkpoint
                    while (true) {
                        val next = commands.tryReceive().getOrNull() ?: break
                        if (next is Command.Checkpoint) {
                            newest = newerCheckpoint(newest, next.checkpoint)
                        } else {
                            pending = next
                            break
                        }
                    }
                    val revision = newest.revision
                    if (
                        revision == null ||
                        highestPersistedCheckpointRevision == null ||
                        revision >= highestPersistedCheckpointRevision
                    ) {
                        persist(newest.snapshot)
                        if (revision != null) highestPersistedCheckpointRevision = revision
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
                terminal ?: terminalFailure ?: CancellationException("Playback session coordinator stopped"),
                active,
                pending,
            )
        }
    }

    private fun newerCheckpoint(first: PlaybackCheckpoint, second: PlaybackCheckpoint): PlaybackCheckpoint {
        val firstRevision = first.revision
        val secondRevision = second.revision
        return if (firstRevision != null && secondRevision != null && firstRevision > secondRevision) first else second
    }

    private suspend fun processBarrier(command: Command) {
        when (command) {
            is Command.Restore -> restore(command)
            is Command.Reconcile -> reconcile(command)
            is Command.Flush -> command.reply.complete(Unit)
            is Command.Checkpoint -> error("Checkpoint runs are handled before barriers")
        }
    }

    private suspend fun restore(command: Command.Restore) {
        if (_phase.value != PlaybackSessionPhase.FailedSafe) _phase.value = PlaybackSessionPhase.Restoring
        controller.setCommandsEnabled(false)
        var cancelled = false
        try {
            if (_phase.value == PlaybackSessionPhase.FailedSafe) return
            val persisted = try {
                store.read()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                enterFailedSafe()
                applyEmptyPaused(command.tracks)
                return
            }
            controller.restoreSession(persisted, command.tracks)
            val normalized = controller.sessionSnapshot()
            persist(normalized)
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
            if (_phase.value != PlaybackSessionPhase.FailedSafe) _phase.value = PlaybackSessionPhase.Ready
            if (!command.reply.isCompleted) command.reply.complete(Unit)
        }
    }

    private suspend fun reconcile(command: Command.Reconcile) {
        try {
            controller.reconcileSession(command.tracks.map(LibraryTrack::toPlayableTrack))
            val result = if (_phase.value == PlaybackSessionPhase.FailedSafe) {
                PlaybackSessionReconcileResult.FailedSafeApplied
            } else if (persist(controller.sessionSnapshot())) {
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
            command.reply.complete(PlaybackSessionReconcileResult.FailedSafeApplied)
        }
    }

    private fun startCheckpointCollection() {
        if (checkpointCollectionStarted) return
        checkpointCollectionStarted = true
        checkpointCollectorJob = processScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                controller.checkpoints.collect(::accept)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                terminalFailure = throwable
                commands.close(throwable)
                actorJob.cancel(cancellation("Playback checkpoint collector failed", throwable))
            }
        }
        val collector = checkpointCollectorJob
        if (collector?.isActive == true) {
            checkpointCollectorReady.complete(Unit)
        } else if (!checkpointCollectorReady.isCompleted) {
            checkpointCollectorReady.completeExceptionally(
                terminalFailure ?: CancellationException("Playback checkpoint collector did not start"),
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

    private suspend fun applyEmptyPaused(tracks: List<PlayableTrack>) {
        try {
            controller.restoreSession(PlaybackSessionSnapshot(), tracks)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }

    private fun enterFailedSafe() {
        _phase.value = PlaybackSessionPhase.FailedSafe
    }

    private fun terminate(failure: Throwable, active: Command?, pending: Command?) {
        terminalFailure = failure
        commands.close(failure)
        checkpointCollectorReady.completeExceptionally(failure)
        checkpointCollectorJob?.cancel(cancellation("Playback session coordinator stopped", failure))
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
        data class Restore(val tracks: List<PlayableTrack>, val reply: CompletableDeferred<Unit>) : Command
        data class Reconcile(
            val tracks: List<LibraryTrack>,
            val reply: CompletableDeferred<PlaybackSessionReconcileResult>,
        ) : Command
        data class Flush(val reply: CompletableDeferred<Unit>) : Command
    }
}

private fun cancellation(message: String, cause: Throwable): CancellationException =
    CancellationException(message, cause)
