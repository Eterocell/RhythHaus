package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class PlaybackSessionCoordinatorTest {
    @Test
    fun terminalCompletionFlushPersistsExactNonWholeSecondDurationWithoutProgress() = runBlocking {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val store = RecordingStore()
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())
        val track = playableTracks("terminal").single().copy(durationMillis = 1_234L)
        controller.setQueue(listOf(track))
        withTimeout(5_000) {
            while (controller.state.value.status != PlaybackStatus.Paused) kotlinx.coroutines.yield()
        }
        controller.setRepeatMode(RepeatMode.StopAfterCurrent)
        coordinator.flush()

        engine.complete()
        coordinator.flush()

        assertEquals(1_234L, store.saved.last().positionMillis)
        assertEquals("terminal", store.saved.last().currentTrackId)
        scope.cancel()
    }

    @Test
    fun flushDuringFirstRestoreWaitsForCollectorReadinessThenCompletes() = runBlocking {
        val controller = RecordingSessionController(blockRestore = true, failFenceWhenInactive = true)
        val store = RecordingStore()
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        val restore = async { coordinator.restoreOnce(emptyList()) }
        controller.restoreStarted.await()

        val flush = async { runCatching { coordinator.flush() } }
        kotlinx.coroutines.yield()
        assertFalse(flush.isCompleted)
        controller.allowRestore.complete(Unit)

        restore.await()
        assertTrue(flush.await().isSuccess)
        assertEquals(1, controller.collectionCount)
        scope.cancel()
    }

    @Test
    fun flushDuringThrowingReadWaitsForFailedSafeCollectorThenCompletes() = runBlocking {
        val controller = RecordingSessionController(failFenceWhenInactive = true)
        val store = RecordingStore(readFailure = BlockingReadFailure())
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        val restore = async { coordinator.restoreOnce(emptyList()) }
        store.readStarted.await()

        val flush = async { runCatching { coordinator.flush() } }
        kotlinx.coroutines.yield()
        assertFalse(flush.isCompleted)
        store.allowReadFailure.complete(Unit)

        restore.await()
        assertTrue(flush.await().isSuccess)
        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(1, controller.collectionCount)
        scope.cancel()
    }

    @Test
    fun preRestoreReconcileSaveFailureStillStartsOneCollectorOnRestore() = runBlocking {
        val controller = RecordingSessionController(failFenceWhenInactive = true)
        val store = RecordingStore(failSaveNumber = 1)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        assertEquals(
            PlaybackSessionReconcileResult.FailedSafeApplied,
            coordinator.reconcile(libraryTracks("pre-restore")),
        )
        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        coordinator.restoreOnce(playableTracks("pre-restore"))
        coordinator.flush()

        assertEquals(1, controller.collectionCount)
        assertTrue(controller.commandsEnabled)
        assertEquals(1, store.saveAttempts)
        scope.cancel()
    }

    @Test
    fun cancellationWhileFlushWaitsForCollectorReadinessFailsWithoutHanging() = runBlocking {
        val controller = RecordingSessionController(blockRestore = true, failFenceWhenInactive = true)
        val processJob = SupervisorJob()
        val scope = CoroutineScope(coroutineContext.minusKey(Job) + processJob)
        val coordinator = PlaybackSessionCoordinator(controller, RecordingStore(), scope)
        val restore = async { runCatching { coordinator.restoreOnce(emptyList()) } }
        controller.restoreStarted.await()
        val flush = async { runCatching { coordinator.flush() } }
        kotlinx.coroutines.yield()
        assertFalse(flush.isCompleted)

        processJob.cancel()

        withTimeout(5_000) {
            assertTrue(flush.await().isFailure)
            assertTrue(restore.await().isFailure)
        }
    }

    @Test
    fun flushWaitsForCollectorFenceBeforeEnqueueingItsBarrier() = runBlocking {
        val controller = RecordingSessionController(blockCheckpointCollection = true)
        val store = RecordingStore()
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())

        controller.emit(snapshot("fenced"))
        controller.checkpointOffered.await()
        val flush = async { coordinator.flush() }

        controller.fenceRequested.await()
        assertFalse(flush.isCompleted)
        assertEquals(listOf(PlaybackSessionSnapshot()), store.saved)
        controller.allowCheckpointCollection.complete(Unit)
        flush.await()

        assertEquals(snapshot("fenced"), store.saved.last())
        scope.cancel()
    }

    @Test
    fun controlledCheckpointFenceOrderingIsStableAcrossRepeatedFlushes() = runBlocking {
        repeat(25) { index ->
            val controller = RecordingSessionController(blockCheckpointCollection = true)
            val store = RecordingStore()
            val scope = detachedScope(coroutineContext)
            val coordinator = PlaybackSessionCoordinator(controller, store, scope)
            coordinator.restoreOnce(emptyList())

            controller.emit(snapshot("fenced-$index"))
            controller.checkpointOffered.await()
            val flush = async { coordinator.flush() }
            controller.fenceRequested.await()
            assertFalse(flush.isCompleted)
            controller.allowCheckpointCollection.complete(Unit)
            flush.await()
            assertEquals("fenced-$index", store.saved.last().currentTrackId)
            scope.cancel()
        }
    }

    @Test
    fun processCancellationCompletesQueuedAndFutureCallersWithoutOrphanedAwait() = runBlocking {
        val controller = RecordingSessionController(blockRestore = true)
        val store = RecordingStore()
        val processJob = SupervisorJob()
        val scope = CoroutineScope(coroutineContext.minusKey(Job) + processJob)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        val restore = async { runCatching { coordinator.restoreOnce(emptyList()) } }
        controller.restoreStarted.await()
        val reconcile = async { runCatching { coordinator.reconcile(libraryTracks("queued")) } }
        val flush = async { runCatching { coordinator.flush() } }

        processJob.cancel()

        withTimeout(5_000) {
            assertTrue(restore.await().isFailure)
            assertTrue(reconcile.await().isFailure)
            assertTrue(flush.await().isFailure)
            assertFails { coordinator.restoreOnce(emptyList()) }
            assertFails { coordinator.reconcile(emptyList()) }
            assertFails { coordinator.flush() }
        }
        Unit
    }

    @Test
    fun checkpointCollectorFailureTerminatesCoordinatorAndRejectsFutureCommands() = runBlocking {
        val controller = RecordingSessionController(failCheckpointCollection = true)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, RecordingStore(), scope)
        coordinator.restoreOnce(emptyList())
        controller.collectionFailed.await()

        withTimeout(5_000) {
            assertFails { coordinator.flush() }
            assertFails { coordinator.reconcile(emptyList()) }
            assertFails { coordinator.restoreOnce(emptyList()) }
        }
        scope.cancel()
    }

    @Test
    fun throwingControllerRestoreAttemptsEmptyFallbackAndCompletesFailedSafeCallers() = runBlocking {
        val controller = RecordingSessionController(throwFirstRestore = true)
        val store = RecordingStore(initial = snapshot("persisted"))
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        withTimeout(5_000) { coordinator.restoreOnce(playableTracks("persisted")) }

        assertEquals(listOf(snapshot("persisted"), PlaybackSessionSnapshot()), controller.restoredSnapshots)
        assertEquals(PlaybackSessionSnapshot(), controller.snapshot)
        assertTrue(controller.commandsEnabled)
        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(0, store.saveAttempts)
        assertEquals(
            PlaybackSessionReconcileResult.FailedSafeApplied,
            withTimeout(5_000) { coordinator.reconcile(libraryTracks("future")) },
        )
        withTimeout(5_000) { coordinator.flush() }
        controller.emit(snapshot("ignored"))
        withTimeout(5_000) { coordinator.flush() }
        assertEquals(0, store.saveAttempts)
        scope.cancel()
    }

    @Test
    fun restoreDisablesCommandsBeforeReadAndRestoreThenSavesNormalizedStateBeforeCollecting() = runBlocking {
        val events = mutableListOf<String>()
        val persisted = snapshot("persisted", positionMillis = 900L)
        val normalized = snapshot("normalized", positionMillis = 400L)
        val controller = RecordingSessionController(
            events = events,
            initialSnapshot = normalized,
            normalizedRestoreSnapshot = normalized,
        )
        val store = RecordingStore(initial = persisted, events = events)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        coordinator.restoreOnce(playableTracks("persisted", "normalized"))

        assertEquals(
            listOf(
                "commands:false",
                "read",
                "restore:persisted",
                "snapshot:normalized",
                "save:normalized",
                "collect",
                "commands:true",
            ),
            events,
        )
        assertEquals(PlaybackSessionPhase.Ready, coordinator.phase.value)
        assertTrue(controller.commandsEnabled)
        scope.cancel()
    }

    @Test
    fun reconcileQueuedDuringRestoreRunsAfterRestoreBeforeCallerCompletes() = runBlocking {
        val events = mutableListOf<String>()
        val controller = RecordingSessionController(events = events, blockRestore = true)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, RecordingStore(events = events), scope)
        val restore = async { coordinator.restoreOnce(playableTracks("one")) }
        controller.restoreStarted.await()

        val reconcile = async { coordinator.reconcile(libraryTracks("one")) }
        assertFalse(reconcile.isCompleted)
        controller.allowRestore.complete(Unit)

        restore.await()
        assertEquals(PlaybackSessionReconcileResult.Applied, reconcile.await())
        assertTrue(events.indexOf("restore:empty") < events.indexOf("reconcile:one"))
        assertTrue(events.indexOf("commands:true") < events.indexOf("reconcile:one"))
        scope.cancel()
    }

    @Test
    fun adjacentCheckpointsCollapseToNewestCompleteSnapshotOnly() = runBlocking {
        val controller = RecordingSessionController(blockRestore = true)
        val store = RecordingStore()
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        val restore = async { coordinator.restoreOnce(emptyList()) }
        controller.restoreStarted.await()

        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("one")))
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("two")))
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("three")))
        controller.allowRestore.complete(Unit)
        restore.await()
        coordinator.flush()

        assertEquals(listOf(PlaybackSessionSnapshot(), snapshot("three")), store.saved)
        scope.cancel()
    }

    @Test
    fun restoreReconcileAndFlushAreStrictCheckpointBarriers() = runBlocking {
        val events = mutableListOf<String>()
        val controller = RecordingSessionController(events = events)
        val store = RecordingStore(events = events, blockSaveNumber = 2)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())
        store.saved.clear()
        events.clear()

        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("before-flush")))
        store.blockedSaveStarted.await()
        val flush = async { coordinator.flush() }
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("after-flush")))
        assertFalse(flush.isCompleted)
        store.allowBlockedSave.complete(Unit)
        flush.await()
        assertEquals(listOf(snapshot("before-flush"), snapshot("after-flush")), store.saved)

        controller.snapshot = snapshot("reconciled")
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("before-reconcile")))
        assertEquals(PlaybackSessionReconcileResult.Applied, coordinator.reconcile(libraryTracks("reconciled")))
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("after-reconcile")))
        coordinator.flush()

        assertEquals(
            listOf("before-flush", "after-flush", "before-reconcile", "reconciled", "after-reconcile"),
            store.saved.map { it.currentTrackId },
        )
        scope.cancel()
    }

    @Test
    fun flushWaitsForPriorSaveAndCompletesWhenDurable() = runBlocking {
        val controller = RecordingSessionController()
        val store = RecordingStore(blockSaveNumber = 2)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())

        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("one")))
        store.blockedSaveStarted.await()
        val flush = async { coordinator.flush() }
        assertFalse(flush.isCompleted)
        store.allowBlockedSave.complete(Unit)

        flush.await()
        assertEquals(snapshot("one"), store.saved.last())
        scope.cancel()
    }

    @Test
    fun saveFailureCompletesFlushAndFutureReconcileAndRestoreSafely() = runBlocking {
        val controller = RecordingSessionController()
        val store = RecordingStore(failSaveNumber = 2)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())

        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("fails")))
        coordinator.flush()

        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(PlaybackSessionReconcileResult.FailedSafeApplied, coordinator.reconcile(libraryTracks("future")))
        coordinator.restoreOnce(playableTracks("future"))
        coordinator.flush()
        assertEquals(2, store.saveAttempts)
        assertEquals(1, store.readAttempts)
        assertTrue(controller.commandsEnabled)
        scope.cancel()
    }

    @Test
    fun replacementLoadFailureEntersFailedSafeWithoutOverwritingDurableSnapshot() = runBlocking {
        val durable = snapshot("missing")
        val controller = RecordingSessionController(
            initialSnapshot = durable,
            reconcileFailure = IllegalStateException("replacement load failed"),
        )
        val store = RecordingStore(initial = durable)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(playableTracks("missing", "survivor"))
        val savesBeforeFailure = store.saveAttempts
        val contentBeforeFailure = store.saved.toList()

        assertEquals(
            PlaybackSessionReconcileResult.FailedSafeApplied,
            coordinator.reconcile(libraryTracks("survivor")),
        )
        coordinator.flush()
        assertEquals(PlaybackSessionReconcileResult.FailedSafeApplied, coordinator.reconcile(libraryTracks("future")))
        coordinator.flush()

        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(savesBeforeFailure, store.saveAttempts)
        assertEquals(contentBeforeFailure, store.saved)
        scope.cancel()
    }

    @Test
    fun throwingReadAppliesEmptyPausedBoundaryReenablesCommandsAndCompletesRestore() = runBlocking {
        val controller = RecordingSessionController(initialSnapshot = snapshot("old"))
        val store = RecordingStore(readFailure = IllegalStateException("read failed"))
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        coordinator.restoreOnce(playableTracks("old"))

        assertEquals(listOf(PlaybackSessionSnapshot()), controller.restoredSnapshots)
        assertEquals(PlaybackSessionSnapshot(), controller.snapshot)
        assertTrue(controller.commandsEnabled)
        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(0, store.saveAttempts)
        scope.cancel()
    }

    @Test
    fun restoreLoadFailureSavesControllerNormalizedEmptyPausedState() = runBlocking {
        val controller = RecordingSessionController(
            initialSnapshot = snapshot("old"),
            normalizeRestoreToEmpty = true,
        )
        val store = RecordingStore(initial = snapshot("old"))
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        coordinator.restoreOnce(playableTracks("old"))

        assertEquals(listOf(PlaybackSessionSnapshot()), store.saved)
        assertEquals(PlaybackSessionPhase.Ready, coordinator.phase.value)
        assertTrue(controller.commandsEnabled)
        scope.cancel()
    }

    @Test
    fun failedSafeIsProcessLifetimeAndFutureCheckpointsNeverPersist() = runBlocking {
        val controller = RecordingSessionController()
        val store = RecordingStore(failSaveNumber = 2)
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)
        coordinator.restoreOnce(emptyList())
        coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("failure")))
        coordinator.flush()

        repeat(3) { index ->
            coordinator.accept(PlaybackCheckpoint.Immediate(snapshot("ignored-$index")))
            coordinator.flush()
            assertEquals(
                PlaybackSessionReconcileResult.FailedSafeApplied,
                coordinator.reconcile(libraryTracks("memory-$index")),
            )
        }

        assertEquals(PlaybackSessionPhase.FailedSafe, coordinator.phase.value)
        assertEquals(2, store.saveAttempts)
        assertEquals("memory-2", controller.snapshot.currentTrackId)
        scope.cancel()
    }

    @Test
    fun repeatedRestoreDoesNotCreateDuplicateCheckpointCollectors() = runBlocking {
        val controller = RecordingSessionController()
        val store = RecordingStore()
        val scope = detachedScope(coroutineContext)
        val coordinator = PlaybackSessionCoordinator(controller, store, scope)

        coordinator.restoreOnce(emptyList())
        coordinator.restoreOnce(emptyList())
        controller.emit(snapshot("one"))
        coordinator.flush()

        assertEquals(1, controller.collectionCount)
        assertEquals(1, store.saved.count { it.currentTrackId == "one" })
        scope.cancel()
    }
}

private class RecordingSessionController(
    private val events: MutableList<String> = mutableListOf(),
    initialSnapshot: PlaybackSessionSnapshot = PlaybackSessionSnapshot(),
    private val blockRestore: Boolean = false,
    private val normalizeRestoreToEmpty: Boolean = false,
    private val normalizedRestoreSnapshot: PlaybackSessionSnapshot? = null,
    private val blockCheckpointCollection: Boolean = false,
    private val failCheckpointCollection: Boolean = false,
    private val throwFirstRestore: Boolean = false,
    private val failFenceWhenInactive: Boolean = false,
    private val reconcileFailure: Throwable? = null,
) : PlaybackSessionController {
    private val checkpointChannel = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
    var collectionCount: Int = 0
        private set
    override val checkpoints: Flow<PlaybackCheckpoint> = flow {
        collectionCount++
        events += "collect"
        if (failCheckpointCollection) {
            collectionFailed.complete(Unit)
            error("checkpoint collection failed")
        }
        checkpointChannel.receiveAsFlow().collect {
            checkpointOffered.complete(Unit)
            if (blockCheckpointCollection) allowCheckpointCollection.await()
            emit(it)
            checkpointForwarded.complete(Unit)
        }
    }
    var snapshot: PlaybackSessionSnapshot = initialSnapshot
    var commandsEnabled: Boolean = true
        private set
    val restoreStarted = CompletableDeferred<Unit>()
    val allowRestore = CompletableDeferred<Unit>()
    val restoredSnapshots = mutableListOf<PlaybackSessionSnapshot>()
    val checkpointOffered = CompletableDeferred<Unit>()
    val allowCheckpointCollection = CompletableDeferred<Unit>()
    val fenceRequested = CompletableDeferred<Unit>()
    val checkpointForwarded = CompletableDeferred<Unit>()
    val collectionFailed = CompletableDeferred<Unit>()
    private var restoreAttempts = 0

    override fun sessionSnapshot(): PlaybackSessionSnapshot {
        events += "snapshot:${snapshot.currentTrackId ?: "empty"}"
        return snapshot
    }

    override suspend fun restoreSession(snapshot: PlaybackSessionSnapshot, tracks: List<PlayableTrack>) {
        events += "restore:${snapshot.currentTrackId ?: "empty"}"
        restoreStarted.complete(Unit)
        if (blockRestore) allowRestore.await()
        restoredSnapshots += snapshot
        restoreAttempts++
        if (throwFirstRestore && restoreAttempts == 1) throw IllegalStateException("restore failed")
        this.snapshot = when {
            normalizeRestoreToEmpty -> PlaybackSessionSnapshot()
            normalizedRestoreSnapshot != null -> normalizedRestoreSnapshot
            else -> snapshot
        }
    }

    override suspend fun reconcileSession(tracks: List<PlayableTrack>) {
        val current = tracks.firstOrNull()?.id
        events += "reconcile:${current ?: "empty"}"
        snapshot = PlaybackSessionSnapshot(
            queueIds = tracks.map { it.id },
            currentTrackId = current,
        )
        reconcileFailure?.let { throw it }
    }

    override fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled = enabled
        events += "commands:$enabled"
    }

    override suspend fun awaitCheckpointFence() {
        if (failFenceWhenInactive && collectionCount == 0) error("collector inactive")
        fenceRequested.complete(Unit)
        if (checkpointOffered.isCompleted) checkpointForwarded.await()
    }

    fun emit(snapshot: PlaybackSessionSnapshot) {
        check(checkpointChannel.trySend(PlaybackCheckpoint.Immediate(snapshot)).isSuccess)
    }
}

private class RecordingStore(
    private val initial: PlaybackSessionSnapshot = PlaybackSessionSnapshot(),
    private val events: MutableList<String> = mutableListOf(),
    private val readFailure: Throwable? = null,
    private val failSaveNumber: Int? = null,
    private val blockSaveNumber: Int? = null,
) : PlaybackSessionStore {
    val saved = mutableListOf<PlaybackSessionSnapshot>()
    var readAttempts: Int = 0
        private set
    var saveAttempts: Int = 0
        private set
    val blockedSaveStarted = CompletableDeferred<Unit>()
    val allowBlockedSave = CompletableDeferred<Unit>()
    val readStarted = CompletableDeferred<Unit>()
    val allowReadFailure = CompletableDeferred<Unit>()

    override suspend fun read(): PlaybackSessionSnapshot {
        readAttempts++
        events += "read"
        readFailure?.let {
            if (it is BlockingReadFailure) {
                readStarted.complete(Unit)
                allowReadFailure.await()
            }
            throw it
        }
        return initial
    }

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        saveAttempts++
        events += "save:${snapshot.currentTrackId ?: "empty"}"
        if (blockSaveNumber == saveAttempts) {
            blockedSaveStarted.complete(Unit)
            allowBlockedSave.await()
        }
        if (failSaveNumber == saveAttempts) throw IllegalStateException("save failed")
        saved += snapshot
    }
}

private class BlockingReadFailure : IllegalStateException("read failed")

private fun snapshot(id: String, positionMillis: Long = 0L) = PlaybackSessionSnapshot(
    queueIds = listOf(id),
    currentTrackId = id,
    positionMillis = positionMillis,
)

private fun playableTracks(vararg ids: String): List<PlayableTrack> = ids.map { id ->
    PlayableTrack(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMillis = 1_000L,
        source = AudioSource.FilePath("/$id.mp3"),
    )
}

private fun libraryTracks(vararg ids: String): List<LibraryTrack> = ids.map { id ->
    LibraryTrack(
        id = id,
        sourceId = "source",
        sourceLocalKey = "$id.mp3",
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = "$id.mp3",
        title = id,
        artist = "Artist",
        album = "Album",
        durationMillis = 1_000L,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}

private fun detachedScope(context: kotlin.coroutines.CoroutineContext): CoroutineScope =
    CoroutineScope(context.minusKey(Job) + SupervisorJob())
