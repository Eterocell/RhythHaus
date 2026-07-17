package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.SessionQueueEntry
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class PlaybackControllerTest {
    @Test
    fun duplicateOccurrencesSkipAndShuffleByOccurrenceWhileLoadingTrackIdentity() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val duplicate = testTracks(1).single()
        val queue = listOf(
            QueueOccurrence("entry-1", duplicate),
            QueueOccurrence("entry-2", duplicate),
            QueueOccurrence("entry-3", testTracks(2)[1]),
        )
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { _, currentId -> listOf(currentId!!, "entry-1", "entry-3") },
        )
        controller.setOccurrenceQueue(queue, "entry-2")
        engine.awaitLoad()

        controller.skipToPrevious()
        engine.awaitLoadCount(2)
        assertEquals("entry-1", controller.state.value.currentOccurrenceId)
        assertEquals("track-1", engine.loadedTracks.last().id)

        controller.selectOccurrence("entry-2")
        engine.awaitLoadCount(3)
        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        engine.awaitLoadCount(4)
        assertEquals("entry-1", controller.state.value.currentOccurrenceId)
        assertEquals("track-1", engine.loadedTracks.last().id)
    }

    @Test
    fun progressCheckpointUsesCurrentOccurrenceIdentity() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val occurrence = QueueOccurrence("entry-1", testTracks(1).single())
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        controller.setOccurrenceQueue(listOf(occurrence), occurrence.id)
        engine.awaitLoad()
        checkpoints.receive()
        engine.listener?.onPlaybackStatus(engine.activeGeneration, PlaybackStatus.Playing)

        engine.listener?.onPlaybackProgress(engine.activeGeneration, 1_100L, 10_000L)

        val checkpoint = checkpoints.receive() as PlaybackCheckpoint.PlayingProgress
        assertEquals("entry-1", checkpoint.key.currentOccurrenceId)
        assertEquals("entry-1", checkpoint.snapshot.currentOccurrenceId)
        assertEquals("track-1", checkpoint.snapshot.queue.single().trackId)
        collection.cancelAndJoin()
    }

    @Test
    fun restoreAndReconcilePreserveDuplicateOccurrencesAndSurvivingCurrentWithoutReload() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val duplicate = testTracks(1).single()
        controller.restoreSession(
            PlaybackSessionSnapshot(
                queue = listOf(
                    SessionQueueEntry("entry-1", duplicate.id),
                    SessionQueueEntry("entry-2", duplicate.id),
                ),
                currentOccurrenceId = "entry-2",
                positionMillis = 500L,
            ),
            listOf(duplicate),
        )
        engine.clearEvents()

        controller.reconcileSession(listOf(duplicate.copy(title = "Updated")))

        assertEquals(listOf("entry-1", "entry-2"), controller.state.value.queue.map { it.id })
        assertEquals(listOf("track-1", "track-1"), controller.state.value.queue.map { it.track.id })
        assertEquals("entry-2", controller.state.value.currentOccurrenceId)
        assertEquals("Updated", controller.state.value.currentTrack?.title)
        assertEquals(500L, controller.state.value.positionMillis)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    @Test
    fun playbackStateDefaultsToStopAfterQueueAndShuffleOff() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCanSetRepeatAndShuffleModes() {
        val controller = PlaybackController(FakePlaybackEngine())

        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.setShuffleMode(ShuffleMode.On)

        assertEquals(RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCyclesRepeatModeInSpecifiedOrder() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.StopAfterCurrent, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
    }

    @Test
    fun controllerTogglesShuffleMode() {
        val controller = PlaybackController(FakePlaybackEngine())

        controller.toggleShuffleMode()
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        controller.toggleShuffleMode()
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
    }

    @Test
    fun stopAfterQueueAdvancesMiddleTrackAndStopsAtFinalTrackEnd() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        engine.awaitLoadCount(1)
        controller.play()

        engine.complete()
        engine.awaitLoadCount(2)
        assertEquals("track-2", controller.state.value.currentTrack?.id)

        engine.complete()
        engine.awaitLoadCount(3)
        assertEquals("track-3", controller.state.value.currentTrack?.id)

        engine.complete()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(3_000L, controller.state.value.positionMillis)
    }

    @Test
    fun stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.setRepeatMode(RepeatMode.StopAfterCurrent)
        controller.play()

        engine.complete()

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(1_000L, controller.state.value.positionMillis)
    }

    @Test
    fun repeatPlaylistWrapsCompletionAndManualTransport() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.play()

        engine.complete()
        assertEquals("track-1", controller.state.value.currentTrack?.id)

        controller.skipToPrevious()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun repeatOneReplaysCurrentTrackButManualTransportCanMoveWithoutWrapping() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.play()

        engine.complete()
        assertEquals("track-1", controller.state.value.currentTrack?.id)

        controller.skipToPrevious()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
    }

    @Test
    fun shuffleUsesGeneratedOrderAndKeepsCurrentTrackActive() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { ids, currentId -> listOf(currentId!!) + ids.filterNot { it == currentId }.reversed() },
        )
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")

        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { ids, currentId -> listOf(currentId!!) + ids.filterNot { it == currentId }.reversed() },
        )
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        assertEquals("track-3", controller.state.value.currentTrack?.id)

        controller.setShuffleMode(ShuffleMode.Off)
        controller.skipToPrevious()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
    }

    @Test
    fun shuffledQueueReplacementRegeneratesOrderAndPreservesSelectedTrack() {
        val generatedOrders = mutableListOf<List<String>>()
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(
            engine = engine,
            shuffleOrderFactory = { ids, currentId ->
                val order = listOf(currentId!!) + ids.filterNot { it == currentId }.reversed()
                generatedOrders += order
                order
            },
        )
        controller.setQueue(testTracks(3), selectedTrackId = "track-2")
        controller.setShuffleMode(ShuffleMode.On)

        controller.setQueue(testTracks(4), selectedTrackId = "track-3")

        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(
            listOf("track-3", "track-4", "track-2", "track-1"),
            generatedOrders.last().map { occurrenceId -> occurrenceId.substringAfterLast("-track-").let { "track-$it" } },
        )
    }

    @Test
    fun autoAdvanceRemainsLoadingUntilEngineReportsPlaying() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        engine.awaitLoadCount(1)

        engine.complete()
        engine.awaitLoadCount(2)

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)

        engine.listener?.onPlaybackStatus(engine.activeGeneration, PlaybackStatus.Paused)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)

        engine.listener?.onPlaybackStatus(engine.activeGeneration, PlaybackStatus.Playing)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
    }

    @Test
    fun manualSkipRemainsLoadingUntilEngineReportsPlaying() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        engine.awaitLoadCount(1)

        controller.skipToNext()
        engine.awaitLoadCount(2)

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)

        engine.reportStatus(PlaybackStatus.Playing)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
    }

    @Test
    fun playbackLoadsLazyArtworkBeforeHandingTrackToEngine() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val lazyArtwork = byteArrayOf(9, 8, 7, 6)
        val controller = PlaybackController(
            engine = engine,
            artworkLoader = { trackId -> if (trackId == "track-1") lazyArtwork else null },
        )
        val track = testTracks(1).single()

        controller.setQueue(listOf(track), selectedTrackId = track.id)

        engine.awaitLoad()

        assertEquals("track-1", engine.loadedTracks.single().id)
        assertContentEquals(lazyArtwork, engine.loadedTracks.single().artworkBytes)
    }

    @Test
    fun restartCurrentPlayingTrackSeeksToZeroBeforePlaying() = runBlocking {
        val engine = RecordingPlaybackEngine(seekGate = CompletableDeferred())
        val controller = loadedController(engine, PlaybackStatus.Playing)

        engine.clearEvents()
        controller.restartCurrentTrack()
        engine.awaitSeekStarted()

        assertEquals(0L, controller.state.value.positionMillis)
        assertNull(controller.state.value.error)
        assertEquals(emptyList(), engine.eventSnapshot())
        engine.releaseSeek()
        assertEquals(listOf(EngineEvent.Seek(0L), EngineEvent.Play), engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentPausedTrackSeeksToZeroAndPlays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Paused)

        engine.clearEvents()
        controller.restartCurrentTrack()

        assertEquals(listOf(EngineEvent.Seek(0L), EngineEvent.Play), engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentStoppedTrackSeeksToZeroAndPlays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Stopped)

        engine.clearEvents()
        controller.restartCurrentTrack()

        assertEquals(listOf(EngineEvent.Seek(0L), EngineEvent.Play), engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentLoadingTrackWaitsForLoadWithoutSeekingStaleEngineItem() = runBlocking {
        val engine = RecordingPlaybackEngine(loadGate = CompletableDeferred())
        val controller = PlaybackController(engine)
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), selectedTrackId = track.id)
        engine.awaitLoadStarted()

        controller.restartCurrentTrack()

        assertEquals(0L, controller.state.value.positionMillis)
        assertNull(controller.state.value.error)
        assertFalse(engine.eventSnapshot().any { it is EngineEvent.Seek })
        engine.releaseLoad()
        assertEquals(listOf(EngineEvent.Play), engine.awaitEvents(1))
    }

    @Test
    fun restartCurrentErrorTrackReloadsAndAutoplays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Error)
        assertNotNull(controller.state.value.error)

        engine.clearEvents()
        controller.restartCurrentTrack()

        assertEquals(0L, controller.state.value.positionMillis)
        assertNull(controller.state.value.error)
        assertEquals(listOf(EngineEvent.Load("track-1"), EngineEvent.Play), engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentTrackPreservesQueueRepeatAndShuffle() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val tracks = testTracks(3)
        val controller = PlaybackController(engine)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        engine.awaitLoad()
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)

        engine.clearEvents()
        controller.restartCurrentTrack()
        engine.awaitEvents(2)

        assertEquals(tracks.map { it.id }, controller.state.value.queue.map { it.track.id })
        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun restartWithoutCurrentTrackIsNoOp() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val initialState = controller.state.value

        controller.restartCurrentTrack()

        assertEquals(initialState, controller.state.value)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    @Test
    fun togglePlayPauseStillDoesNotSeek() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Playing)

        engine.clearEvents()
        controller.togglePlayPause()

        assertEquals(listOf<EngineEvent>(EngineEvent.Pause), engine.awaitEvents(1))
    }

    @Test
    fun staleGenerationCallbacksCannotMutateCurrentPlayback() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(testTracks(2), selectedTrackId = "track-1")
        engine.awaitLoad()
        val firstGeneration = engine.activeGeneration

        controller.selectTrack("track-2", autoPlay = false)
        engine.awaitLoadCount(2)
        withTimeout(5_000) {
            while (controller.state.value.status != PlaybackStatus.Paused) kotlinx.coroutines.yield()
        }
        val secondGeneration = engine.activeGeneration
        assertFalse(firstGeneration == secondGeneration)

        engine.listener?.onPlaybackStatus(firstGeneration, PlaybackStatus.Playing)
        engine.listener?.onPlaybackProgress(firstGeneration, 999L, 1_000L)
        engine.listener?.onPlaybackCompleted(firstGeneration)
        engine.listener?.onPlaybackError(firstGeneration, PlaybackError("stale"))
        engine.listener?.onSkipToPrevious(firstGeneration)

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(0L, controller.state.value.positionMillis)
        assertNull(controller.state.value.error)
    }

    @Test
    fun controllerAllocatesDistinctGenerationsForSameTrackReloads() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val track = testTracks(1).single()

        controller.setQueue(listOf(track), selectedTrackId = track.id)
        engine.awaitLoad()
        val firstGeneration = engine.activeGeneration
        controller.selectTrack(track.id)
        engine.awaitLoadCount(2)

        assertFalse(firstGeneration == engine.activeGeneration)
        assertEquals(listOf(firstGeneration, engine.activeGeneration), engine.loadedGenerations)
    }

    @Test
    fun supersededBlockedLoadCancelsNormallyAndReplacementCanBecomeReady() = runBlocking {
        val engine = ReplacingLoadPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)

        controller.setQueue(tracks, selectedTrackId = "track-1")
        engine.firstStarted.await()
        controller.selectTrack("track-2")
        engine.secondStarted.await()
        engine.completeSecond()

        withTimeout(5_000) {
            while (controller.state.value.status != PlaybackStatus.Paused) kotlinx.coroutines.yield()
        }
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertNull(controller.state.value.error)
        assertTrue(engine.firstCancelled)
        assertFalse(engine.oldCompletionWasPublished)
    }

    @Test
    fun disabledCommandsPropagateToTransportAndEveryPublicUserCommandIsNoOp() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        engine.awaitLoad()
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)
        repeat(3) { checkpoints.receive() }
        val before = controller.state.value
        engine.clearEvents()

        controller.setCommandsEnabled(false)
        assertEquals(listOf(EngineEvent.TransportEnabled(false)), engine.awaitEvents(1))
        engine.clearEvents()
        controller.setQueue(testTracks(2), selectedTrackId = "track-1")
        controller.selectTrack("track-1", autoPlay = true)
        controller.setRepeatMode(RepeatMode.StopAfterQueue)
        controller.cycleRepeatMode()
        controller.setShuffleMode(ShuffleMode.Off)
        controller.toggleShuffleMode()
        controller.play()
        controller.pause()
        controller.stop()
        controller.seekTo(900L)
        controller.togglePlayPause()
        controller.restartCurrentTrack()
        controller.skipToNext()
        controller.skipToPrevious()

        assertEquals(before, controller.state.value)
        assertEquals(emptyList(), engine.eventSnapshot())
        assertNull(checkpoints.tryReceive().getOrNull())
        collection.cancelAndJoin()
    }

    @Test
    fun restoreLoadsClampsSeeksAndPausesWithoutPlayAndEmitsNormalizedSnapshot() = runBlocking {
        val engine = RecordingPlaybackEngine(loadedDurationMillis = 1_000L)
        val controller = PlaybackController(engine)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }

        controller.restoreSession(
            PlaybackSessionSnapshot(
                queueIds = listOf("missing", "track-1"),
                currentTrackId = "track-1",
                positionMillis = 2_000L,
                repeatMode = RepeatMode.RepeatOne,
                shuffleMode = ShuffleMode.On,
            ),
            testTracks(2),
        )

        assertEquals(
            listOf(EngineEvent.Load("track-1"), EngineEvent.Seek(1_000L), EngineEvent.Pause),
            engine.awaitEvents(3),
        )
        assertFalse(engine.eventSnapshot().any { it == EngineEvent.Play })
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(1_000L, controller.state.value.positionMillis)
        assertEquals(listOf("track-1"), controller.state.value.queue.map { it.track.id })
        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        val normalized = checkpoints.receive() as PlaybackCheckpoint.Immediate
        assertEquals(listOf("track-1"), normalized.snapshot.queue.map { it.trackId })
        assertEquals("track-1", normalized.snapshot.currentTrackId)
        assertEquals(1_000L, normalized.snapshot.positionMillis)
        assertEquals(RepeatMode.RepeatOne, normalized.snapshot.repeatMode)
        assertEquals(ShuffleMode.On, normalized.snapshot.shuffleMode)
        collection.cancelAndJoin()
    }

    @Test
    fun restoreFallsBackToFirstSurvivorAtZeroAndClearsWhenNoneSurvive() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.restoreSession(
            PlaybackSessionSnapshot(
                queueIds = listOf("missing", "track-2", "track-1"),
                currentTrackId = "missing",
                positionMillis = 700L,
                repeatMode = RepeatMode.StopAfterCurrent,
                shuffleMode = ShuffleMode.On,
            ),
            testTracks(2),
        )

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(0L, controller.state.value.positionMillis)
        assertEquals(listOf(EngineEvent.Load("track-2"), EngineEvent.Seek(0L), EngineEvent.Pause), engine.awaitEvents(3))
        engine.clearEvents()

        controller.restoreSession(
            PlaybackSessionSnapshot(
                queueIds = listOf("missing"),
                currentTrackId = "missing",
                positionMillis = 700L,
                repeatMode = RepeatMode.StopAfterCurrent,
                shuffleMode = ShuffleMode.On,
            ),
            testTracks(2),
        )

        assertEquals(listOf(EngineEvent.Clear), engine.awaitEvents(1))
        assertEquals(emptyList(), controller.state.value.queue)
        assertNull(controller.state.value.currentTrack)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(RepeatMode.StopAfterCurrent, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun restoreLoadFailureAppliesEmptyPausedFailSafeState() = runBlocking {
        val engine = RecordingPlaybackEngine(loadFailure = IllegalStateException("load failed"))
        val controller = PlaybackController(engine)

        controller.restoreSession(
            PlaybackSessionSnapshot(
                queueIds = listOf("track-1"),
                currentTrackId = "track-1",
                positionMillis = 500L,
                repeatMode = RepeatMode.RepeatPlaylist,
                shuffleMode = ShuffleMode.On,
            ),
            testTracks(1),
        )

        assertEquals(emptyList(), controller.state.value.queue)
        assertNull(controller.state.value.currentTrack)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        assertTrue(engine.eventSnapshot().none { it == EngineEvent.Play })
    }

    @Test
    fun reconcilePreservesSurvivingCurrentWithoutReloadPositionOrStatusChange() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Playing)
        engine.clearEvents()

        controller.reconcileSession(testTracks(2))

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(500L, controller.state.value.positionMillis)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    @Test
    fun reconcileMissingCurrentLoadsFirstSurvivorPausedAndNoSurvivorsClear() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(testTracks(3), selectedTrackId = "track-2")
        engine.awaitLoad()
        engine.clearEvents()

        controller.reconcileSession(listOf(testTracks(3)[2], testTracks(3)[0]))

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(0L, controller.state.value.positionMillis)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(listOf(EngineEvent.Load("track-1"), EngineEvent.Seek(0L), EngineEvent.Pause), engine.awaitEvents(3))
        engine.clearEvents()

        controller.reconcileSession(emptyList())

        assertEquals(listOf(EngineEvent.Clear), engine.awaitEvents(1))
        assertEquals(emptyList(), controller.state.value.queue)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
    }

    @Test
    fun reconcileReplacementLoadFailurePropagatesAfterApplyingPausedFailSafeState() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(testTracks(2), selectedTrackId = "track-2")
        engine.awaitLoad()
        engine.nextLoadFailure = IllegalStateException("replacement failed")
        val survivor = testTracks(2).first()

        assertFailsWith<IllegalStateException> {
            controller.reconcileSession(listOf(survivor))
        }

        assertEquals(emptyList(), controller.state.value.queue)
        assertNull(controller.state.value.currentTrack)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
    }

    @Test
    fun discreteCommandsEmitCompleteImmediateSnapshots() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }

        controller.setQueue(testTracks(1))
        engine.awaitLoad()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        controller.seekTo(500L)
        assertEquals(500L, checkpoints.receive().snapshot.positionMillis)
        controller.pause()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        controller.setRepeatMode(RepeatMode.RepeatOne)
        assertEquals(RepeatMode.RepeatOne, checkpoints.receive().snapshot.repeatMode)
        controller.setShuffleMode(ShuffleMode.On)
        assertEquals(ShuffleMode.On, checkpoints.receive().snapshot.shuffleMode)
        controller.stop()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)

        collection.cancelAndJoin()
    }

    @Test
    fun playingProgressUsesGenerationTrackAndSecondBucketAndResetsAfterSeek() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Playing)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        val generation = engine.activeGeneration
        checkpoints.receive()

        engine.listener?.onPlaybackProgress(generation, 1_100L, 10_000L)
        engine.listener?.onPlaybackProgress(generation, 1_900L, 10_000L)
        val first = checkpoints.receive()
        assertTrue(first is PlaybackCheckpoint.PlayingProgress)
        assertNull(checkpoints.tryReceive().getOrNull())

        controller.seekTo(1_500L)
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        engine.listener?.onPlaybackProgress(generation, 1_600L, 10_000L)
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
        collection.cancelAndJoin()
    }

    @Test
    fun progressKeyResetsAcrossStopAndSurvivingCurrentReconciliation() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Playing)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        val generation = engine.activeGeneration
        checkpoints.receive()

        engine.listener?.onPlaybackProgress(generation, 1_100L, 10_000L)
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
        controller.stop()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        engine.listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
        engine.listener?.onPlaybackProgress(generation, 1_200L, 10_000L)
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)

        controller.reconcileSession(testTracks(1))
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        engine.listener?.onPlaybackProgress(generation, 1_300L, 10_000L)
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
        collection.cancelAndJoin()
    }

    @Test
    fun staleCallbacksDoNotEmitCheckpoints() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(testTracks(2), selectedTrackId = "track-1")
        engine.awaitLoad()
        val staleGeneration = engine.activeGeneration
        controller.selectTrack("track-2")
        engine.awaitLoadCount(2)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        repeat(2) { checkpoints.receive() }

        engine.listener?.onPlaybackStatus(staleGeneration, PlaybackStatus.Playing)
        engine.listener?.onPlaybackProgress(staleGeneration, 1_500L, 2_000L)
        engine.listener?.onPlaybackCompleted(staleGeneration)
        engine.listener?.onPlaybackError(staleGeneration, PlaybackError("stale"))
        engine.listener?.onSkipToNext(staleGeneration)

        assertNull(checkpoints.tryReceive().getOrNull())
        collection.cancelAndJoin()
    }

    @Test
    fun restoreAndReconcileEngineTransactionsCannotInterleave() = runBlocking {
        val engine = SerializedSessionPlaybackEngine()
        val controller = PlaybackController(engine)
        val restore = launch {
            controller.restoreSession(
                PlaybackSessionSnapshot(
                    queueIds = listOf("track-1", "track-2"),
                    currentTrackId = "track-1",
                    positionMillis = 700L,
                ),
                testTracks(2),
            )
        }
        engine.firstLoadStarted.await()

        val reconcile = launch { controller.reconcileSession(listOf(testTracks(2)[1])) }
        kotlinx.coroutines.yield()

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(listOf(EngineEvent.Load("track-1")), engine.eventSnapshot())

        engine.releaseFirstLoad.complete(Unit)
        restore.join()
        reconcile.join()

        assertEquals(
            listOf(
                EngineEvent.Seek(700L),
                EngineEvent.Pause,
                EngineEvent.Load("track-2"),
                EngineEvent.Seek(0L),
                EngineEvent.Pause,
            ),
            engine.eventSnapshot(),
        )
        assertEquals("track-2", controller.state.value.currentTrack?.id)
    }

    @Test
    fun restartSkipAndCompletionEmitImmediateCurrentPositionBeforeProgress() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
        val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
        controller.setQueue(testTracks(3), "track-2")
        engine.awaitLoad()
        checkpoints.receive()
        engine.listener?.onPlaybackStatus(engine.activeGeneration, PlaybackStatus.Paused)
        engine.listener?.onPlaybackProgress(engine.activeGeneration, 500L, 2_000L)

        controller.restartCurrentTrack()
        val restarted = checkpoints.receive().snapshot
        assertEquals(listOf("track-1", "track-2", "track-3"), restarted.queue.map { it.trackId })
        assertEquals("track-2", restarted.currentTrackId)
        assertEquals(0L, restarted.positionMillis)

        controller.skipToNext()
        assertEquals("track-3", checkpoints.receive().snapshot.currentTrackId)
        assertEquals(0L, controller.sessionSnapshot().positionMillis)
        engine.awaitLoadCount(2)

        controller.skipToPrevious()
        assertEquals("track-2", checkpoints.receive().snapshot.currentTrackId)
        engine.awaitLoadCount(3)

        engine.listener?.onPlaybackCompleted(engine.activeGeneration)
        val completion = checkpoints.receive()
        assertTrue(completion is PlaybackCheckpoint.Immediate)
        assertEquals("track-3", completion.snapshot.currentTrackId)
        assertEquals(0L, completion.snapshot.positionMillis)
        collection.cancelAndJoin()
    }

    @Test
    fun terminalCompletionEmitsExactlyOneImmediateCheckpointAtExactDuration() = runBlocking {
        for (mode in listOf(RepeatMode.StopAfterCurrent, RepeatMode.StopAfterQueue)) {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection = launch(start = CoroutineStart.UNDISPATCHED) { controller.checkpoints.collect(checkpoints::send) }
            val track = testTracks(1).single().copy(durationMillis = 1_234L)
            controller.setQueue(listOf(track))
            engine.awaitLoad()
            checkpoints.receive()
            controller.setRepeatMode(mode)
            if (mode != RepeatMode.StopAfterQueue) checkpoints.receive()

            engine.listener?.onPlaybackCompleted(engine.activeGeneration)

            val terminal = withTimeout(1_000) { checkpoints.receive() }
            assertTrue(terminal is PlaybackCheckpoint.Immediate)
            assertEquals(1_234L, terminal.snapshot.positionMillis)
            assertEquals("track-1", terminal.snapshot.currentTrackId)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }
    }

    @Test
    fun checkpointsProducedBeforeCollectorStartsAreDeliveredInOrder() = runBlocking {
        val controller = PlaybackController(RecordingPlaybackEngine())
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)
        controller.setRepeatMode(RepeatMode.StopAfterCurrent)

        val received = mutableListOf<PlaybackCheckpoint>()
        controller.checkpoints.take(3).collect(received::add)

        assertEquals(
            listOf(RepeatMode.RepeatOne, RepeatMode.RepeatOne, RepeatMode.StopAfterCurrent),
            received.map { it.snapshot.repeatMode },
        )
        assertEquals(listOf(ShuffleMode.Off, ShuffleMode.On, ShuffleMode.On), received.map { it.snapshot.shuffleMode })
    }

    @Test
    fun checkpointFenceCompletesAfterCollectorConsumesEveryPriorCheckpoint() = runBlocking {
        val controller = PlaybackController(RecordingPlaybackEngine())
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)
        val received = mutableListOf<PlaybackCheckpoint>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            controller.checkpoints.collect { checkpoint ->
                received += checkpoint
            }
        }

        controller.awaitCheckpointFence()

        assertEquals(2, received.size)
        assertEquals(RepeatMode.RepeatOne, received.first().snapshot.repeatMode)
        assertEquals(ShuffleMode.On, received.last().snapshot.shuffleMode)
        collector.cancelAndJoin()
    }

    @Test
    fun slowCollectorReceivesMoreThanSixtyFourDiscreteCheckpointsWithoutLoss() = runBlocking {
        val controller = PlaybackController(RecordingPlaybackEngine())
        repeat(80) { index ->
            controller.setRepeatMode(if (index % 2 == 0) RepeatMode.RepeatOne else RepeatMode.StopAfterQueue)
        }

        val received = mutableListOf<PlaybackCheckpoint>()
        controller.checkpoints.take(80).collect { checkpoint ->
            received += checkpoint
            kotlinx.coroutines.yield()
        }

        assertEquals(80, received.size)
        assertEquals(RepeatMode.RepeatOne, received.first().snapshot.repeatMode)
        assertEquals(RepeatMode.StopAfterQueue, received.last().snapshot.repeatMode)
    }

    @Test
    fun restoreAndReconcileDeduplicateRuntimeTrackIdsWithStableFirstWinsMetadata() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val first = testTracks(1).single().copy(title = "First")
        val duplicate = first.copy(title = "Duplicate")
        val second = testTracks(2)[1]

        controller.restoreSession(
            PlaybackSessionSnapshot(queueIds = listOf("track-1", "track-1", "track-2"), currentTrackId = "track-1"),
            listOf(first, duplicate, second),
        )

        assertEquals(listOf("track-1", "track-1", "track-2"), controller.state.value.queue.map { it.track.id })
        assertEquals("First", controller.state.value.currentTrack?.title)
        assertEquals("First", controller.state.value.queue.first().track.title)

        controller.setCommandsEnabled(true)
        controller.setQueue(listOf(first, duplicate, second), selectedTrackId = "track-2")
        engine.awaitLoadCount(2)
        controller.reconcileSession(listOf(first, duplicate, second))

        assertEquals(listOf("track-1", "track-1", "track-2"), controller.state.value.queue.map { it.track.id })
        assertEquals("First", controller.state.value.queue.first().track.title)
    }

    private suspend fun loadedController(
        engine: RecordingPlaybackEngine,
        status: PlaybackStatus,
    ): PlaybackController {
        val controller = PlaybackController(engine)
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), selectedTrackId = track.id)
        engine.awaitLoad()
        engine.listener?.onPlaybackProgress(engine.activeGeneration, 500L, track.durationMillis)
        if (status == PlaybackStatus.Error) {
            engine.listener?.onPlaybackError(engine.activeGeneration, PlaybackError("Test error"))
        } else {
            engine.listener?.onPlaybackStatus(engine.activeGeneration, status)
        }
        return controller
    }

    private fun testTracks(count: Int): List<PlayableTrack> = (1..count).map { index ->
        PlayableTrack(
            id = "track-$index",
            title = "Track $index",
            artist = "Test Artist",
            album = "Test Album",
            durationMillis = index * 1_000L,
            source = AudioSource.FilePath("/tmp/track-$index.mp3"),
        )
    }

    private class DelayedStatusPlaybackEngine : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        var activeGeneration: Long = 0L
            private set
        private var loadCount: Int = 0

        override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
            activeGeneration = generation
            loadCount++
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun clear(generation: Long) {
            activeGeneration = generation
        }

        override fun setUserTransportEnabled(enabled: Boolean) = Unit

        fun complete() {
            listener?.onPlaybackCompleted(activeGeneration)
        }

        fun reportStatus(status: PlaybackStatus) {
            listener?.onPlaybackStatus(activeGeneration, status)
        }

        suspend fun awaitLoadCount(count: Int) = withTimeout(5_000) {
            while (loadCount < count) kotlinx.coroutines.yield()
        }

        override fun play() = Unit

        override fun pause() = Unit

        override fun stop() = Unit

        override fun seekTo(positionMillis: Long) = Unit

        override fun release() = Unit
    }

    private sealed interface EngineEvent {
        data class Load(val trackId: String) : EngineEvent
        data class Seek(val positionMillis: Long) : EngineEvent
        data class TransportEnabled(val enabled: Boolean) : EngineEvent
        data object Play : EngineEvent
        data object Pause : EngineEvent
        data object Stop : EngineEvent
        data object Clear : EngineEvent
    }

    private class RecordingPlaybackEngine(
        private val loadGate: CompletableDeferred<Unit>? = null,
        private val seekGate: CompletableDeferred<Unit>? = null,
        private val loadedDurationMillis: Long? = null,
        private val loadFailure: Throwable? = null,
    ) : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val loadedTracks = mutableListOf<PlayableTrack>()
        val loadedGenerations = mutableListOf<Long>()
        var activeGeneration: Long = 0L
            private set
        var nextLoadFailure: Throwable? = null
        private val events = Channel<EngineEvent>(Channel.UNLIMITED)
        private val loadStarted = CompletableDeferred<Unit>()
        private val loadSignal = CompletableDeferred<Unit>()
        private val seekStarted = CompletableDeferred<Unit>()

        override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
            activeGeneration = generation
            record(EngineEvent.Load(track.id))
            loadedTracks += track
            loadedGenerations += generation
            loadStarted.complete(Unit)
            loadGate?.await()
            loadFailure?.let { throw it }
            nextLoadFailure?.also { nextLoadFailure = null }?.let { throw it }
            val durationMillis = loadedDurationMillis ?: track.durationMillis
            listener?.onPlaybackProgress(generation, 0L, durationMillis)
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            loadSignal.complete(Unit)
            return LoadedPlayback(generation, durationMillis)
        }

        suspend fun awaitLoadStarted() = loadStarted.await()

        suspend fun awaitLoad() = loadSignal.await()

        suspend fun awaitLoadCount(count: Int) = withTimeout(5_000) {
            while (loadedGenerations.size < count) kotlinx.coroutines.yield()
        }

        fun releaseLoad() {
            loadGate?.complete(Unit)
        }

        suspend fun awaitSeekStarted() = seekStarted.await()

        fun releaseSeek() {
            seekGate?.complete(Unit)
        }

        fun clearEvents() {
            while (events.tryReceive().isSuccess) {}
        }

        fun eventSnapshot(): List<EngineEvent> = buildList {
            while (true) add(events.tryReceive().getOrNull() ?: break)
        }

        suspend fun awaitEvents(count: Int): List<EngineEvent> = withTimeout(5_000) {
            List(count) { events.receive() }
        }

        override fun play() {
            record(EngineEvent.Play)
            listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Playing)
        }

        override fun pause() {
            record(EngineEvent.Pause)
            listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Paused)
        }

        override fun stop() {
            record(EngineEvent.Stop)
        }

        override fun seekTo(positionMillis: Long) {
            seekStarted.complete(Unit)
            seekGate?.let { runBlocking { it.await() } }
            record(EngineEvent.Seek(positionMillis))
            listener?.onPlaybackProgress(activeGeneration, positionMillis, loadedTracks.lastOrNull()?.durationMillis)
        }

        override fun clear(generation: Long) {
            activeGeneration = generation
            record(EngineEvent.Clear)
        }

        override fun setUserTransportEnabled(enabled: Boolean) {
            record(EngineEvent.TransportEnabled(enabled))
        }

        override fun release() = Unit

        private fun record(event: EngineEvent) {
            check(events.trySend(event).isSuccess)
        }
    }

    private class ReplacingLoadPlaybackEngine : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        private val firstResult = CompletableDeferred<LoadedPlayback>()
        private val secondResult = CompletableDeferred<LoadedPlayback>()
        var firstCancelled: Boolean = false
            private set
        var oldCompletionWasPublished: Boolean = false
            private set
        private var firstGeneration: Long = 0L
        private var secondGeneration: Long = 0L

        override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
            return if (firstGeneration == 0L) {
                firstGeneration = generation
                firstStarted.complete(Unit)
                try {
                    firstResult.await()
                } catch (cancelled: CancellationException) {
                    firstCancelled = true
                    throw cancelled
                }
            } else {
                secondGeneration = generation
                secondStarted.complete(Unit)
                secondResult.await()
            }
        }

        fun completeFirst(): Boolean = firstResult.complete(LoadedPlayback(firstGeneration, 1_000L)).also {
            oldCompletionWasPublished = it && !firstCancelled
        }

        fun completeSecond() {
            secondResult.complete(LoadedPlayback(secondGeneration, 2_000L))
        }

        override fun clear(generation: Long) = Unit
        override fun setUserTransportEnabled(enabled: Boolean) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMillis: Long) = Unit
        override fun release() = Unit
    }

    private class SerializedSessionPlaybackEngine : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val firstLoadStarted = CompletableDeferred<Unit>()
        val releaseFirstLoad = CompletableDeferred<Unit>()
        private val events = Channel<EngineEvent>(Channel.UNLIMITED)
        private var loadCount = 0

        override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
            record(EngineEvent.Load(track.id))
            loadCount++
            if (loadCount == 1) {
                firstLoadStarted.complete(Unit)
                releaseFirstLoad.await()
            }
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun seekTo(positionMillis: Long) = record(EngineEvent.Seek(positionMillis))
        override fun pause() = record(EngineEvent.Pause)
        override fun clear(generation: Long) = record(EngineEvent.Clear)
        override fun setUserTransportEnabled(enabled: Boolean) = Unit
        override fun play() = record(EngineEvent.Play)
        override fun stop() = record(EngineEvent.Stop)
        override fun release() = Unit

        fun eventSnapshot(): List<EngineEvent> = buildList {
            while (true) add(events.tryReceive().getOrNull() ?: break)
        }

        private fun record(event: EngineEvent) {
            check(events.trySend(event).isSuccess)
        }
    }
}
