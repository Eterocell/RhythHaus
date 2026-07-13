package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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

    init {
        processScope.launch(start = CoroutineStart.UNDISPATCHED) { runActor() }
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
        yield()
        val reply = CompletableDeferred<Unit>()
        enqueue(Command.Flush(reply))
        reply.await()
    }

    private suspend fun enqueue(command: Command) {
        if (commands.trySend(command).isSuccess) return
        completeSafely(command)
    }

    private suspend fun runActor() {
        var pending: Command? = null
        while (true) {
            val command = pending ?: commands.receiveCatching().getOrNull() ?: return
            pending = null
            try {
                if (command is Command.Checkpoint) {
                    var newest = command.checkpoint.snapshot
                    while (true) {
                        val next = commands.tryReceive().getOrNull() ?: break
                        if (next is Command.Checkpoint) {
                            newest = next.checkpoint.snapshot
                        } else {
                            pending = next
                            break
                        }
                    }
                    persist(newest)
                } else {
                    processBarrier(command)
                }
            } catch (_: Throwable) {
                enterFailedSafe()
                completeSafely(command)
            }
        }
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
        if (_phase.value == PlaybackSessionPhase.FailedSafe) {
            controller.setCommandsEnabled(true)
            command.reply.complete(Unit)
            return
        }
        _phase.value = PlaybackSessionPhase.Restoring
        controller.setCommandsEnabled(false)
        try {
            val persisted = try {
                store.read()
            } catch (_: Throwable) {
                enterFailedSafe()
                applyEmptyPaused(command.tracks)
                return
            }
            controller.restoreSession(persisted, command.tracks)
            val normalized = controller.sessionSnapshot()
            if (!persist(normalized)) return
            startCheckpointCollection()
        } catch (_: Throwable) {
            enterFailedSafe()
            applyEmptyPaused(command.tracks)
        } finally {
            controller.setCommandsEnabled(true)
            if (_phase.value != PlaybackSessionPhase.FailedSafe) {
                _phase.value = PlaybackSessionPhase.Ready
            }
            command.reply.complete(Unit)
        }
    }

    private suspend fun reconcile(command: Command.Reconcile) {
        controller.reconcileSession(command.tracks.map(LibraryTrack::toPlayableTrack))
        val result = if (_phase.value == PlaybackSessionPhase.FailedSafe) {
            PlaybackSessionReconcileResult.FailedSafeApplied
        } else {
            val saved = persist(controller.sessionSnapshot())
            if (saved) PlaybackSessionReconcileResult.Applied else PlaybackSessionReconcileResult.FailedSafeApplied
        }
        command.reply.complete(result)
    }

    private fun startCheckpointCollection() {
        if (checkpointCollectionStarted) return
        checkpointCollectionStarted = true
        processScope.launch(start = CoroutineStart.UNDISPATCHED) {
            controller.checkpoints.collect(::accept)
        }
    }

    private suspend fun persist(snapshot: PlaybackSessionSnapshot): Boolean {
        if (_phase.value == PlaybackSessionPhase.FailedSafe) return false
        return try {
            store.save(snapshot)
            durableSnapshot = snapshot
            true
        } catch (_: Throwable) {
            enterFailedSafe()
            false
        }
    }

    private suspend fun applyEmptyPaused(tracks: List<PlayableTrack>) {
        try {
            controller.restoreSession(PlaybackSessionSnapshot(), tracks)
        } catch (_: Throwable) {
        }
    }

    private fun enterFailedSafe() {
        _phase.value = PlaybackSessionPhase.FailedSafe
    }

    private fun completeSafely(command: Command) {
        when (command) {
            is Command.Restore -> {
                controller.setCommandsEnabled(true)
                command.reply.complete(Unit)
            }
            is Command.Reconcile -> command.reply.complete(PlaybackSessionReconcileResult.FailedSafeApplied)
            is Command.Flush -> command.reply.complete(Unit)
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
