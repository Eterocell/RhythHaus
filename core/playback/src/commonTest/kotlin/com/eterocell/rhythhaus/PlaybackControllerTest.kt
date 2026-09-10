package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import com.eterocell.rhythhaus.session.PlaybackSessionCodec
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.SessionQueueEntry
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class PlaybackControllerTest {
    @Test
    fun upcomingMutationsRejectCurrentStaleAndInvalidTargetsWithoutChangingState() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val queue = occurrenceQueue()
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            controller.setOccurrenceQueue(queue, "current")
            engine.awaitLoad()
            checkpoints.receive()
            engine.listener?.onPlaybackProgress(
                engine.activeGeneration, 750L, 1_000L)
            engine.listener?.onPlaybackStatus(
                engine.activeGeneration, PlaybackStatus.Paused)
            val before = controller.state.value

            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.CurrentOccurrence),
                controller.removeUpcoming("current"),
            )
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.StaleOccurrence),
                controller.removeUpcoming("missing"),
            )
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.InvalidTargetIndex),
                controller.reorderUpcoming("upcoming-1", 2),
            )

            assertEquals(before, controller.state.value)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    @Test
    fun reorderUpcomingTargetsOneDuplicateOccurrenceAndEmitsOneCompleteCheckpoint() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val duplicate = testTracks(1).single()
            val queue =
                listOf(
                    QueueOccurrence("current", testTracks(3)[1]),
                    QueueOccurrence("duplicate-1", duplicate),
                    QueueOccurrence("other", testTracks(3)[2]),
                    QueueOccurrence("duplicate-2", duplicate),
                )
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            controller.setOccurrenceQueue(queue, "current")
            engine.awaitLoad()
            checkpoints.receive()

            assertEquals(
                QueueMutationResult.Applied,
                controller.reorderUpcoming("duplicate-2", 0))

            assertEquals(
                listOf("current", "duplicate-2", "duplicate-1", "other"),
                controller.state.value.queue.map { it.id },
            )
            val checkpoint = checkpoints.receive()
            assertTrue(checkpoint is PlaybackCheckpoint.Immediate)
            assertEquals(controller.sessionSnapshot(), checkpoint.snapshot)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    @Test
    fun concurrentRemovalOfSameOccurrenceSerializesAgainstLatestState() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            controller.setOccurrenceQueue(occurrenceQueue(), "current")
            engine.awaitLoad()

            val first = async { controller.removeUpcoming("upcoming-1") }
            val second = async { controller.removeUpcoming("upcoming-1") }

            assertEquals(
                setOf(
                    QueueMutationResult.Applied,
                    QueueMutationResult.Rejected(
                        QueueMutationRejection.StaleOccurrence),
                ),
                setOf(first.await(), second.await()),
            )
            assertEquals(
                listOf("current", "upcoming-2"),
                controller.state.value.queue.map { it.id })
        }

    @Test
    fun inFlightMutationCannotOverwriteACompletedQueueReplacement() =
        runBlocking {
            repeat(20) { attempt ->
                val controller = PlaybackController(RecordingPlaybackEngine())
                val track = testTracks(1).single()
                val largeQueue = buildList {
                    add(QueueOccurrence("current-$attempt", track))
                    repeat(100_000) { index ->
                        add(QueueOccurrence("upcoming-$attempt-$index", track))
                    }
                }
                controller.setOccurrenceQueue(largeQueue, "current-$attempt")
                val started = CompletableDeferred<Unit>()
                val mutation =
                    async(Dispatchers.Default) {
                        started.complete(Unit)
                        controller.removeUpcoming("upcoming-$attempt-99999")
                    }
                started.await()
                kotlinx.coroutines.yield()
                val replacement = QueueOccurrence("replacement-$attempt", track)

                controller.setOccurrenceQueue(
                    listOf(replacement), replacement.id)
                val result = mutation.await()

                assertEquals(
                    replacement.id,
                    controller.state.value.currentOccurrenceId,
                    "Attempt $attempt lost a completed replacement after mutation result $result",
                )
            }
        }

    @Test
    fun upcomingMutationsPreserveHistoryBeforeCurrentAndRejectHistoryTargets() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val tracks = testTracks(4)
            val controller = PlaybackController(engine)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("history", tracks[0]),
                    QueueOccurrence("current", tracks[1]),
                    QueueOccurrence("upcoming-1", tracks[2]),
                    QueueOccurrence("upcoming-2", tracks[3]),
                ),
                "current",
            )
            engine.awaitLoad()

            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.StaleOccurrence),
                controller.removeUpcoming("history"),
            )
            assertEquals(
                QueueMutationResult.Applied, controller.clearUpcoming())

            assertEquals(
                listOf("history", "current"),
                controller.state.value.queue.map { it.id })
            assertEquals("current", controller.state.value.currentOccurrenceId)
        }

    @Test
    fun acceptedUpcomingEditsPreserveTransportGenerationAndStaleCallbackSafety() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        listOf(currentId!!) +
                            ids.filterNot { it == currentId }.reversed()
                    },
                )
            controller.setOccurrenceQueue(occurrenceQueue(), "current")
            engine.awaitLoad()
            engine.listener?.onPlaybackProgress(
                engine.activeGeneration, 750L, 1_000L)
            engine.listener?.onPlaybackStatus(
                engine.activeGeneration, PlaybackStatus.Paused)
            controller.setRepeatMode(RepeatMode.RepeatPlaylist)
            controller.setShuffleMode(ShuffleMode.On)
            val generation = engine.activeGeneration
            engine.clearEvents()

            assertEquals(
                QueueMutationResult.Applied,
                controller.removeUpcoming("upcoming-1"))
            assertEquals(
                QueueMutationResult.Applied, controller.clearUpcoming())

            val state = controller.state.value
            assertEquals(listOf("current"), state.queue.map { it.id })
            assertEquals("current", state.currentOccurrenceId)
            assertEquals(750L, state.positionMillis)
            assertEquals(PlaybackStatus.Paused, state.status)
            assertEquals(RepeatMode.RepeatPlaylist, state.repeatMode)
            assertEquals(ShuffleMode.On, state.shuffleMode)
            assertEquals(generation, engine.activeGeneration)
            assertEquals(emptyList(), engine.eventSnapshot())

            engine.listener?.onPlaybackStatus(
                generation - 1L, PlaybackStatus.Playing)
            engine.listener?.onPlaybackProgress(generation - 1L, 999L, 1_000L)
            assertEquals(state, controller.state.value)
        }

    @Test
    fun clearUpcomingDuringLoadingDoesNotRestartOrReplaceCurrentOccurrence() =
        runBlocking {
            val loadGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(loadGate = loadGate)
            val controller = PlaybackController(engine)
            controller.setOccurrenceQueue(occurrenceQueue(), "current")
            engine.awaitLoadStarted()
            val generation = engine.activeGeneration
            engine.clearEvents()

            assertEquals(
                QueueMutationResult.Applied, controller.clearUpcoming())

            assertEquals("current", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)
            assertEquals(0L, controller.state.value.positionMillis)
            assertEquals(generation, engine.activeGeneration)
            assertEquals(1, engine.loadedGenerations.size)
            assertEquals(emptyList(), engine.eventSnapshot())
            engine.releaseLoad()
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals("current", controller.state.value.currentOccurrenceId)
        }

    @Test
    fun duplicateOccurrencesSkipAndShuffleByOccurrenceWhileLoadingTrackIdentity() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val duplicate = testTracks(1).single()
            val queue =
                listOf(
                    QueueOccurrence("entry-1", duplicate),
                    QueueOccurrence("entry-2", duplicate),
                    QueueOccurrence("entry-3", testTracks(2)[1]),
                )
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { _, currentId ->
                        listOf(currentId!!, "entry-1", "entry-3")
                    },
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
        val collection =
            launch(start = CoroutineStart.UNDISPATCHED) {
                controller.checkpoints.collect(checkpoints::send)
            }
        controller.setOccurrenceQueue(listOf(occurrence), occurrence.id)
        engine.awaitLoad()
        checkpoints.receive()
        engine.listener?.onPlaybackStatus(
            engine.activeGeneration, PlaybackStatus.Playing)

        engine.listener?.onPlaybackProgress(
            engine.activeGeneration, 1_100L, 10_000L)

        val checkpoint =
            checkpoints.receive() as PlaybackCheckpoint.PlayingProgress
        assertEquals("entry-1", checkpoint.key.currentOccurrenceId)
        assertEquals("entry-1", checkpoint.snapshot.currentOccurrenceId)
        assertEquals("track-1", checkpoint.snapshot.queue.single().trackId)
        collection.cancelAndJoin()
    }

    @Test
    fun restoreAndReconcilePreserveDuplicateOccurrencesAndSurvivingCurrentWithoutReload() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val duplicate = testTracks(1).single()
            controller.restoreSession(
                PlaybackSessionSnapshot(
                    queue =
                        listOf(
                            SessionQueueEntry("entry-1", duplicate.id),
                            SessionQueueEntry("entry-2", duplicate.id),
                        ),
                    currentOccurrenceId = "entry-2",
                    positionMillis = 500L,
                ),
                listOf(duplicate),
            )
            engine.clearEvents()

            controller.reconcileSession(
                listOf(duplicate.copy(title = "Updated")))

            assertEquals(
                listOf("entry-1", "entry-2"),
                controller.state.value.queue.map { it.id })
            assertEquals(
                listOf("track-1", "track-1"),
                controller.state.value.queue.map { it.track.id })
            assertEquals("entry-2", controller.state.value.currentOccurrenceId)
            assertEquals("Updated", controller.state.value.currentTrack?.title)
            assertEquals(500L, controller.state.value.positionMillis)
            assertEquals(emptyList(), engine.eventSnapshot())
        }

    @Test
    fun playbackStateDefaultsToStopAfterQueueAndShuffleOff() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(
            RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCanSetRepeatAndShuffleModes() {
        val controller = PlaybackController(FakePlaybackEngine())

        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.setShuffleMode(ShuffleMode.On)

        assertEquals(
            RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun controllerCyclesRepeatModeInSpecifiedOrder() {
        val controller = PlaybackController(FakePlaybackEngine())

        assertEquals(
            RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(
            RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(
            RepeatMode.StopAfterCurrent, controller.state.value.repeatMode)
        controller.cycleRepeatMode()
        assertEquals(
            RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
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
    fun stopAfterQueueAdvancesMiddleTrackAndStopsAtFinalTrackEnd() =
        runBlocking {
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
    fun stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        engine.awaitLoadCount(1)
        controller.setRepeatMode(RepeatMode.StopAfterCurrent)
        controller.play()

        engine.complete()

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(1_000L, controller.state.value.positionMillis)
    }

    @Test
    fun repeatPlaylistWrapsCompletionAndManualTransport() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        engine.awaitLoadCount(1)
        controller.setRepeatMode(RepeatMode.RepeatPlaylist)
        controller.play()

        engine.complete()
        engine.awaitLoadCount(2)
        assertEquals("track-1", controller.state.value.currentTrack?.id)

        controller.skipToPrevious()
        engine.awaitLoadCount(3)
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        engine.awaitLoadCount(4)
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun repeatOneReplaysCurrentTrackButManualTransportCanMoveWithoutWrapping() =
        runBlocking {
            val engine = DelayedStatusPlaybackEngine()
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            controller.setQueue(tracks, selectedTrackId = "track-1")
            engine.awaitLoadCount(1)
            controller.setRepeatMode(RepeatMode.RepeatOne)
            controller.play()

            engine.complete()
            engine.awaitLoadCount(2)
            assertEquals("track-1", controller.state.value.currentTrack?.id)

            controller.skipToPrevious()
            assertEquals("track-1", controller.state.value.currentTrack?.id)
            controller.skipToNext()
            engine.awaitLoadCount(3)
            assertEquals("track-2", controller.state.value.currentTrack?.id)
        }

    @Test
    fun shuffleUsesGeneratedOrderAndKeepsCurrentTrackActive() = runBlocking {
        val engine = DelayedStatusPlaybackEngine()
        val controller =
            PlaybackController(
                engine = engine,
                shuffleOrderFactory = { ids, currentId ->
                    listOf(currentId!!) +
                        ids.filterNot { it == currentId }.reversed()
                },
            )
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-2")
        engine.awaitLoadCount(1)

        controller.setShuffleMode(ShuffleMode.On)
        controller.skipToNext()
        engine.awaitLoadCount(2)
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        engine.awaitLoadCount(3)
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        controller.skipToNext()
        assertEquals("track-1", controller.state.value.currentTrack?.id)
    }

    @Test
    fun disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack() =
        runBlocking {
            val engine = DelayedStatusPlaybackEngine()
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        listOf(currentId!!) +
                            ids.filterNot { it == currentId }.reversed()
                    },
                )
            val tracks = testTracks(3)
            controller.setQueue(tracks, selectedTrackId = "track-2")
            engine.awaitLoadCount(1)
            controller.setShuffleMode(ShuffleMode.On)
            controller.skipToNext()
            engine.awaitLoadCount(2)
            assertEquals("track-3", controller.state.value.currentTrack?.id)

            controller.setShuffleMode(ShuffleMode.Off)
            controller.skipToPrevious()
            engine.awaitLoadCount(3)
            assertEquals("track-2", controller.state.value.currentTrack?.id)
        }

    @Test
    fun shuffledQueueReplacementRegeneratesOrderAndPreservesSelectedTrack() =
        runBlocking {
            val generatedOrders = mutableListOf<List<String>>()
            val engine = DelayedStatusPlaybackEngine()
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        val order =
                            listOf(currentId!!) +
                                ids.filterNot { it == currentId }.reversed()
                        generatedOrders += order
                        order
                    },
                )
            controller.setQueue(testTracks(3), selectedTrackId = "track-2")
            engine.awaitLoadCount(1)
            controller.setShuffleMode(ShuffleMode.On)

            controller.setQueue(testTracks(4), selectedTrackId = "track-3")
            engine.awaitLoadCount(2)

            assertEquals("track-3", controller.state.value.currentTrack?.id)
            assertEquals(
                listOf("track-3", "track-4", "track-2", "track-1"),
                generatedOrders.last().map { occurrenceId ->
                    controller.state.value.queue
                        .single { it.id == occurrenceId }
                        .track
                        .id
                },
            )
            assertEquals(
                controller.state.value.queue.size,
                controller.state.value.queue.map { it.id }.distinct().size)
            assertTrue(
                controller.state.value.queue.all {
                    it.id.length <= PlaybackSessionCodec.maxIdCharacters
                })
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

        engine.listener?.onPlaybackStatus(
            engine.activeGeneration, PlaybackStatus.Paused)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)

        engine.listener?.onPlaybackStatus(
            engine.activeGeneration, PlaybackStatus.Playing)
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
        val controller =
            PlaybackController(
                engine = engine,
                artworkLoader = { trackId ->
                    if (trackId == "track-1") lazyArtwork else null
                },
            )
        val track = testTracks(1).single()

        controller.setQueue(listOf(track), selectedTrackId = track.id)

        engine.awaitLoad()

        assertEquals("track-1", engine.loadedTracks.single().id)
        assertContentEquals(
            lazyArtwork, engine.loadedTracks.single().artworkBytes)
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
        assertEquals(
            listOf(EngineEvent.Seek(0L), EngineEvent.Play),
            engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentPausedTrackSeeksToZeroAndPlays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Paused)

        engine.clearEvents()
        controller.restartCurrentTrack()

        assertEquals(
            listOf(EngineEvent.Seek(0L), EngineEvent.Play),
            engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentStoppedTrackSeeksToZeroAndPlays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = loadedController(engine, PlaybackStatus.Stopped)

        engine.clearEvents()
        controller.restartCurrentTrack()

        assertEquals(
            listOf(EngineEvent.Seek(0L), EngineEvent.Play),
            engine.awaitEvents(2))
    }

    @Test
    fun restartCurrentLoadingTrackWaitsForLoadWithoutSeekingStaleEngineItem() =
        runBlocking {
            val engine =
                RecordingPlaybackEngine(loadGate = CompletableDeferred())
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
        assertEquals(
            listOf(EngineEvent.Load("track-1"), EngineEvent.Play),
            engine.awaitEvents(2))
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

        assertEquals(
            tracks.map { it.id },
            controller.state.value.queue.map { it.track.id })
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

        assertEquals(
            listOf<EngineEvent>(EngineEvent.Pause), engine.awaitEvents(1))
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
            while (controller.state.value.status !=
                PlaybackStatus.Paused) kotlinx.coroutines.yield()
        }
        val secondGeneration = engine.activeGeneration
        assertFalse(firstGeneration == secondGeneration)

        engine.listener?.onPlaybackStatus(
            firstGeneration, PlaybackStatus.Playing)
        engine.listener?.onPlaybackProgress(firstGeneration, 999L, 1_000L)
        engine.listener?.onPlaybackCompleted(firstGeneration)
        engine.listener?.onPlaybackError(
            firstGeneration, PlaybackError("stale"))
        engine.listener?.onSkipToPrevious(firstGeneration)

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals(0L, controller.state.value.positionMillis)
        assertNull(controller.state.value.error)
    }

    @Test
    fun controllerAllocatesDistinctGenerationsForSameTrackReloads() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val track = testTracks(1).single()

            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitLoad()
            val firstGeneration = engine.activeGeneration
            controller.selectTrack(track.id)
            engine.awaitLoadCount(2)

            assertFalse(firstGeneration == engine.activeGeneration)
            assertEquals(
                listOf(firstGeneration, engine.activeGeneration),
                engine.loadedGenerations)
        }

    @Test
    fun supersededBlockedLoadCancelsNormallyAndReplacementCanBecomeReady() =
        runBlocking {
            val engine = ReplacingLoadPlaybackEngine()
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)

            controller.setQueue(tracks, selectedTrackId = "track-1")
            engine.firstStarted.await()
            controller.selectTrack("track-2")
            engine.secondStarted.await()
            engine.completeSecond()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals("track-2", controller.state.value.currentTrack?.id)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertNull(controller.state.value.error)
            assertTrue(engine.firstCancelled)
            assertFalse(engine.oldCompletionWasPublished)
        }

    @Test
    fun disabledCommandsPropagateToTransportAndEveryPublicUserCommandIsNoOp() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(3)
            controller.setQueue(tracks, selectedTrackId = "track-2")
            engine.awaitLoad()
            controller.setRepeatMode(RepeatMode.RepeatOne)
            controller.setShuffleMode(ShuffleMode.On)
            repeat(3) { checkpoints.receive() }
            val before = controller.state.value
            engine.clearEvents()

            controller.setCommandsEnabled(false)
            assertEquals(
                listOf(EngineEvent.TransportEnabled(false)),
                engine.awaitEvents(1))
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
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.CommandsDisabled),
                controller.reorderUpcoming(before.queue.last().id, 0),
            )
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.CommandsDisabled),
                controller.removeUpcoming(before.queue.last().id),
            )
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.CommandsDisabled),
                controller.clearUpcoming(),
            )

            assertEquals(before, controller.state.value)
            assertEquals(emptyList(), engine.eventSnapshot())
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    @Test
    fun retryFailedTrackReloadsTheSameOccurrenceWithAutoplay() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val queue = occurrenceQueue()
        controller.setOccurrenceQueue(queue, "current")
        engine.awaitLoad()
        engine.listener?.onPlaybackError(
            engine.activeGeneration, PlaybackError("Test error"))
        assertEquals(PlaybackStatus.Error, controller.state.value.status)
        assertNotNull(controller.state.value.error)
        engine.clearEvents()

        controller.retryFailedTrack()

        assertEquals(
            listOf(EngineEvent.Load("track-1"), EngineEvent.Play),
            engine.awaitEvents(2))
        withTimeout(5_000) {
            while (controller.state.value.status != PlaybackStatus.Playing) {
                kotlinx.coroutines.yield()
            }
        }
        assertEquals("current", controller.state.value.currentOccurrenceId)
        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertNull(controller.state.value.error)
        assertEquals(
            queue.map { it.id }, controller.state.value.queue.map { it.id })
    }

    @Test
    fun skipFailedTrackLoadsEffectiveSuccessorWithoutWrapping() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller =
            PlaybackController(
                engine = engine,
                shuffleOrderFactory = { ids, currentId ->
                    listOf(currentId!!) +
                        ids.filterNot { it == currentId }.reversed()
                },
            )
        val tracks = testTracks(4)
        val queue =
            listOf(
                QueueOccurrence("current", tracks[0]),
                QueueOccurrence("upcoming-1", tracks[1]),
                QueueOccurrence("upcoming-2", tracks[2]),
                QueueOccurrence("upcoming-3", tracks[3]),
            )
        controller.setOccurrenceQueue(queue, "current")
        engine.awaitLoad()
        controller.setShuffleMode(ShuffleMode.On)
        engine.listener?.onPlaybackError(
            engine.activeGeneration, PlaybackError("Test error"))
        assertEquals(PlaybackStatus.Error, controller.state.value.status)
        engine.clearEvents()

        controller.skipFailedTrack()

        assertEquals(
            listOf(EngineEvent.Load("track-4"), EngineEvent.Play),
            engine.awaitEvents(2))
        assertEquals("upcoming-3", controller.state.value.currentOccurrenceId)
        assertNull(controller.state.value.error)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        assertEquals(
            queue.map { it.id }, controller.state.value.queue.map { it.id })
    }

    @Test
    fun skipFailedTrackAtEffectiveEndLeavesErrorUnchanged() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller =
            PlaybackController(
                engine = engine,
                shuffleOrderFactory = { ids, currentId ->
                    ids.filterNot { it == currentId } + listOf(currentId!!)
                },
            )
        controller.setOccurrenceQueue(occurrenceQueue(), "current")
        engine.awaitLoad()
        controller.setShuffleMode(ShuffleMode.On)
        engine.listener?.onPlaybackError(
            engine.activeGeneration, PlaybackError("Test error"))
        val errorState = controller.state.value
        engine.clearEvents()

        controller.skipFailedTrack()

        assertEquals(errorState, controller.state.value)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    @Test
    fun removeFailedTrackRemovesOnlyCurrentOccurrenceAndAutoplaysSuccessor() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val duplicate = testTracks(1).single()
            val queue =
                listOf(
                    QueueOccurrence("duplicate-current", duplicate),
                    QueueOccurrence("other", testTracks(2)[1]),
                    QueueOccurrence("duplicate-2", duplicate),
                )
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            controller.setOccurrenceQueue(queue, "duplicate-current")
            engine.awaitLoad()
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()

            assertEquals(
                QueueMutationResult.Applied, controller.removeFailedTrack())

            assertEquals(
                listOf(EngineEvent.Load("track-2"), EngineEvent.Play),
                engine.awaitEvents(2))
            assertEquals(
                listOf("other", "duplicate-2"),
                controller.state.value.queue.map { it.id })
            assertEquals("other", controller.state.value.currentOccurrenceId)
            assertEquals("track-2", controller.state.value.currentTrack?.id)
            assertNull(controller.state.value.error)

            val checkpoint = checkpoints.receive()
            assertTrue(checkpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                listOf("other", "duplicate-2"),
                checkpoint.snapshot.queue.map { it.occurrenceId })
            assertNull(checkpoint.snapshot.currentOccurrenceId)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    @Test
    fun removeFinalFailedTrackClearsEngineAndPublishesIdleWithRemainingQueue() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val queue = occurrenceQueue()
            controller.setOccurrenceQueue(
                listOf(queue[1], queue[2], queue[0]), "current")
            engine.awaitLoad()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()

            assertEquals(
                QueueMutationResult.Applied, controller.removeFailedTrack())

            assertEquals(listOf(EngineEvent.Clear), engine.awaitEvents(1))
            assertEquals(PlaybackStatus.Idle, controller.state.value.status)
            assertNull(controller.state.value.currentOccurrenceId)
            assertNull(controller.state.value.error)
            assertEquals(
                listOf("upcoming-1", "upcoming-2"),
                controller.state.value.queue.map { it.id })
        }

    @Test
    fun recoveryCommandsAreNoOpsOutsideEnabledCurrentErrorState() =
        runBlocking {
            val idleEngine = RecordingPlaybackEngine()
            val idleController = PlaybackController(idleEngine)
            val idleState = idleController.state.value

            idleController.retryFailedTrack()
            idleController.skipFailedTrack()
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.StaleOccurrence),
                idleController.removeFailedTrack())

            assertEquals(idleState, idleController.state.value)
            assertEquals(emptyList(), idleEngine.eventSnapshot())

            val pausedEngine = RecordingPlaybackEngine()
            val pausedController =
                loadedController(pausedEngine, PlaybackStatus.Paused)
            pausedEngine.clearEvents()
            val pausedState = pausedController.state.value

            pausedController.retryFailedTrack()
            pausedController.skipFailedTrack()
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.StaleOccurrence),
                pausedController.removeFailedTrack())

            assertEquals(pausedState, pausedController.state.value)
            assertEquals(emptyList(), pausedEngine.eventSnapshot())

            val disabledEngine = RecordingPlaybackEngine()
            val disabledController = PlaybackController(disabledEngine)
            disabledController.setQueue(
                testTracks(2), selectedTrackId = "track-1")
            disabledEngine.awaitLoad()
            disabledEngine.listener?.onPlaybackError(
                disabledEngine.activeGeneration, PlaybackError("Test error"))
            disabledController.setCommandsEnabled(false)
            disabledEngine.clearEvents()
            val disabledState = disabledController.state.value

            disabledController.retryFailedTrack()
            disabledController.skipFailedTrack()
            assertEquals(
                QueueMutationResult.Rejected(
                    QueueMutationRejection.CommandsDisabled),
                disabledController.removeFailedTrack())

            assertEquals(disabledState, disabledController.state.value)
            assertEquals(emptyList(), disabledEngine.eventSnapshot())
        }

    @Test
    fun removeFinalFailedTrackRacingReplacementKeepsReplacementIntact() =
        runBlocking {
            val clearGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(clearGate = clearGate)
            val controller = PlaybackController(engine)
            val failedQueue = occurrenceQueue()
            controller.setOccurrenceQueue(
                listOf(failedQueue[1], failedQueue[2], failedQueue[0]),
                "current")
            engine.awaitLoad()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            engine.awaitClearStarted()
            val replacementTrack = testTracks(4)[3]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("replacement-1", replacementTrack)),
                "replacement-1")
            engine.releaseClear()

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "replacement-1", controller.state.value.currentOccurrenceId)
            assertEquals(
                "replacement-1", controller.state.value.queue.single().id)
            assertEquals("track-4", engine.loadedTracks.last().id)
        }

    @Test
    fun removeFailedTrackUnderShuffleKeepsRemainingOccurrencesReachable() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        if (currentId == null) ids.reversed()
                        else
                            listOf(currentId) +
                                ids.filterNot { it == currentId }
                    },
                )
            val tracks = testTracks(4)
            val queue =
                listOf(
                    QueueOccurrence("current", tracks[0]),
                    QueueOccurrence("upcoming-1", tracks[1]),
                    QueueOccurrence("upcoming-2", tracks[2]),
                    QueueOccurrence("upcoming-3", tracks[3]),
                )
            controller.setOccurrenceQueue(queue, "current")
            engine.awaitLoad()
            controller.setShuffleMode(ShuffleMode.On)
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()

            assertEquals(
                QueueMutationResult.Applied, controller.removeFailedTrack())
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Playing) {
                    kotlinx.coroutines.yield()
                }
            }
            assertEquals(
                "upcoming-1", controller.state.value.currentOccurrenceId)

            controller.skipToNext()
            engine.awaitLoadCount(3)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Playing) {
                    kotlinx.coroutines.yield()
                }
            }
            assertEquals(
                "upcoming-2", controller.state.value.currentOccurrenceId)
            controller.skipToNext()
            engine.awaitLoadCount(4)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Playing) {
                    kotlinx.coroutines.yield()
                }
            }
            assertEquals(
                "upcoming-3", controller.state.value.currentOccurrenceId)

            engine.clearEvents()
            controller.skipToNext()
            assertEquals(
                "upcoming-3", controller.state.value.currentOccurrenceId)
            assertEquals(emptyList(), engine.eventSnapshot())
        }

    /**
     * Regression guard for the successor-load race: [removeFailedTrack]
     * CAS-publishes its pruned queue and only then asks loadSelected to load
     * the pre-removal successor. A concurrent [setOccurrenceQueue] committed
     * in that window replaces the queue with one that no longer contains the
     * successor. loadSelected must publish its Loading transition atomically
     * only from a state that still contains the requested occurrence; a stale
     * successor must never be reinstated as current or reach the engine, and
     * the aborting selection must not append a checkpoint beyond the
     * replacement's own.
     *
     * The removal is paused deterministically after its prune publish: with
     * shuffle on, the removal's post-CAS shuffle-order regeneration invokes
     * the injected factory on the removal thread before loadSelected runs.
     * The test commits the competing replacement while the removal is paused
     * there, then releases it.
     */
    @Test
    fun removeFailedTrackRacingReplacementNeverLoadsSupersededSuccessor() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val prunePublished = CompletableDeferred<Unit>()
            val releasePrunePublish = CompletableDeferred<Unit>()
            var blockedPrunePublish = false
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        if (!blockedPrunePublish &&
                            "failed-current" !in ids && "successor" in ids
                        ) {
                            blockedPrunePublish = true
                            prunePublished.complete(Unit)
                            runBlocking { releasePrunePublish.await() }
                        }
                        if (currentId == null) ids
                        else
                            listOf(currentId) +
                                ids.filterNot { it == currentId }
                    },
                )
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(4)
            val queue =
                listOf(
                    QueueOccurrence("failed-current", tracks[0]),
                    QueueOccurrence("successor", tracks[1]),
                    QueueOccurrence("tail", tracks[2]),
                )
            controller.setOccurrenceQueue(queue, "failed-current")
            engine.awaitLoad()
            checkpoints.receive()
            controller.setShuffleMode(ShuffleMode.On)
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()
            assertNull(checkpoints.tryReceive().getOrNull())

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            try {
                withTimeout(5_000) { prunePublished.await() }
                // The removal has CAS-published the pruned queue with the
                // successor still present and is blocked regenerating its
                // shuffle order, before loadSelected can transition the
                // successor to Loading.
                assertEquals(
                    listOf("successor", "tail"),
                    controller.state.value.queue.map { it.id })
                assertNull(controller.state.value.currentOccurrenceId)

                val replacementTrack = tracks[3]
                controller.setOccurrenceQueue(
                    listOf(
                        QueueOccurrence("replacement-1", replacementTrack)),
                    "replacement-1")
            } finally {
                releasePrunePublish.complete(Unit)
            }

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status ==
                    PlaybackStatus.Loading) kotlinx.coroutines.yield()
            }
            assertEquals(
                "replacement-1", controller.state.value.currentOccurrenceId)
            assertEquals(
                listOf("replacement-1"),
                controller.state.value.queue.map { it.id })
            assertNull(controller.state.value.error)
            assertEquals(
                listOf("track-1", "track-4"),
                engine.loadedTracks.map { it.id },
                "the superseded successor must never reach the engine",
            )
            // The race emits exactly two checkpoints: the replacement's own
            // selection checkpoint and the removal's intentional queue-only
            // prune checkpoint. The aborted stale successor load appends
            // nothing beyond them.
            val replacementCheckpoint = checkpoints.receive()
            assertTrue(replacementCheckpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                "replacement-1",
                replacementCheckpoint.snapshot.currentOccurrenceId)
            assertEquals(
                listOf("replacement-1"),
                replacementCheckpoint.snapshot.queue.map {
                    it.occurrenceId
                })
            val pruneCheckpoint = checkpoints.receive()
            assertTrue(pruneCheckpoint is PlaybackCheckpoint.Immediate)
            assertNull(pruneCheckpoint.snapshot.currentOccurrenceId)
            assertEquals(
                listOf("successor", "tail"),
                pruneCheckpoint.snapshot.queue.map { it.occurrenceId })
            assertNull(checkpoints.tryReceive().getOrNull())
            assertEquals(
                listOf(EngineEvent.Load("track-4")),
                engine.eventSnapshot())
            collection.cancelAndJoin()
        }

    /**
     * Regression guard for the retry failed-track path racing a committed
     * queue replacement (requirement B): a retry admitted only after the
     * replacement removed the failed occurrence must leave the winner's
     * queue, current occurrence, engine load, generation and checkpoints
     * completely untouched.
     */
    @Test
    fun retryFailedTrackAfterQueueReplacementLeavesWinnerUntouched() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(3)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("failed", tracks[0]),
                    QueueOccurrence("upcoming", tracks[1]),
                ),
                "failed")
            engine.awaitLoad()
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))
            assertNotNull(controller.state.value.error)
            engine.clearEvents()
            assertNull(checkpoints.tryReceive().getOrNull())

            val winnerTrack = tracks[2]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", winnerTrack)), "winner")
            engine.awaitLoadCount(2)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            checkpoints.receive()
            val winnerState = controller.state.value
            val winnerGeneration = engine.activeGeneration
            engine.clearEvents()
            assertNull(checkpoints.tryReceive().getOrNull())

            controller.retryFailedTrack()

            assertEquals(winnerState, controller.state.value)
            assertEquals(winnerGeneration, engine.activeGeneration)
            assertNull(controller.state.value.error)
            assertEquals(emptyList(), engine.eventSnapshot())
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * Regression guard for the skip failed-track path racing a committed
     * queue replacement (requirement B): the stale skip must not advance the
     * replacement's queue or start any load.
     */
    @Test
    fun skipFailedTrackAfterQueueReplacementDoesNotAdvanceWinner() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(4)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("failed", tracks[0]),
                    QueueOccurrence("tail", tracks[1]),
                    QueueOccurrence("upcoming", tracks[2]),
                ),
                "failed")
            engine.awaitLoad()
            checkpoints.receive()
            controller.setShuffleMode(ShuffleMode.On)
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))
            engine.clearEvents()
            assertNull(checkpoints.tryReceive().getOrNull())

            val winnerTrack = tracks[3]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", winnerTrack)), "winner")
            engine.awaitLoadCount(2)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            checkpoints.receive()
            val winnerState = controller.state.value
            val winnerGeneration = engine.activeGeneration
            engine.clearEvents()
            assertNull(checkpoints.tryReceive().getOrNull())

            controller.skipFailedTrack()

            assertEquals(winnerState, controller.state.value)
            assertEquals(winnerGeneration, engine.activeGeneration)
            assertNull(controller.state.value.error)
            assertEquals(emptyList(), engine.eventSnapshot())
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * A retry admitted while its engine load is paused must be cancelled by a
     * superseding replacement without altering the replacement's queue,
     * current occurrence, load, generation or checkpoints (requirement B).
     */
    @Test
    fun supersededRetryLoadCannotAlterCommittedReplacement() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(3)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("failed", tracks[0]),
                    QueueOccurrence("tail", tracks[1]),
                ),
                "failed")
            // The failed load holds the engine mutex inside its non-cancellable
            // hold while the error is reported.
            engine.awaitHoldEntered()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))
            engine.clearEvents()

            // The stale retry is admitted and its engine load pauses at the
            // engine mutex while the Loading state is current.
            controller.retryFailedTrack()
            assertEquals(
                "failed", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)

            val winnerTrack = tracks[2]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", winnerTrack)), "winner")
            assertEquals(
                "winner", controller.state.value.currentOccurrenceId)
            assertEquals(
                listOf("winner"),
                controller.state.value.queue.map { it.id })
            assertNull(controller.state.value.error)

            engine.releaseHold()
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "winner", controller.state.value.currentOccurrenceId)
            assertEquals("track-3", engine.loadedTracks.last().id)
            assertNull(controller.state.value.error)
            // Only the winner's load reaches the engine after the gate opens.
            assertEquals(
                listOf(EngineEvent.Load("track-3")), engine.eventSnapshot())
        }

    /**
     * Old-generation error, status, progress and completion callbacks emitted
     * after a replacement's Loading state cannot alter the replacement's
     * state or start another load (requirement C).
     */
    @Test
    fun staleGenerationCallbacksAfterReplacementLoadingCannotAlterWinner() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(2)
            controller.setQueue(listOf(tracks[0]), selectedTrackId = "track-1")
            engine.awaitHoldEntered()
            val staleGeneration = engine.activeGeneration
            checkpoints.receive()
            engine.clearEvents()

            // The winner's claim makes Loading current while its engine load
            // waits behind the engine mutex held by the stuck first load.
            controller.setQueue(listOf(tracks[1]), selectedTrackId = "track-2")
            assertEquals(
                "track-2", controller.state.value.currentTrack?.id)
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)
            checkpoints.receive()
            val loadingState = controller.state.value

            engine.listener?.onPlaybackStatus(
                staleGeneration, PlaybackStatus.Playing)
            engine.listener?.onPlaybackProgress(
                staleGeneration, 900L, 1_000L)
            engine.listener?.onPlaybackCompleted(staleGeneration)
            engine.listener?.onPlaybackError(
                staleGeneration, PlaybackError("stale"))
            engine.listener?.onSkipToNext(staleGeneration)

            assertEquals(loadingState, controller.state.value)
            assertNull(checkpoints.tryReceive().getOrNull())

            engine.releaseHold()
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals("track-2", controller.state.value.currentTrack?.id)
            assertNull(controller.state.value.error)
            // Only the winner's load reaches the engine: the stale callbacks
            // started no further load.
            assertEquals(
                listOf(EngineEvent.Load("track-2")), engine.eventSnapshot())
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    @Test
    fun trailingSkipCallbackAfterCommandsDisabledCannotAdvanceQueue() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = loadedController(engine, PlaybackStatus.Playing)
            awaitState { controller.state.value.status == PlaybackStatus.Playing }
            val generation = engine.activeGeneration
            val before = controller.state.value
            engine.clearEvents()

            controller.setCommandsEnabled(false)
            engine.clearEvents()
            engine.listener?.onSkipToNext(generation)

            assertEquals(before, controller.state.value)
            assertEquals(emptyList(), engine.eventSnapshot())
        }

    /**
     * A stale load whose engine failure surfaces after a superseding claim
     * cannot publish an error against the winner's state (requirement C).
     */
    @Test
    fun staleLoadFailureAfterSupersedeCannotErrorTheWinner() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            controller.setQueue(listOf(tracks[0]), selectedTrackId = "track-1")
            engine.awaitHoldEntered()

            controller.setQueue(listOf(tracks[1]), selectedTrackId = "track-2")
            withTimeout(5_000) {
                while (controller.state.value.currentTrack?.id != "track-2") {
                    kotlinx.coroutines.yield()
                }
            }
            // The stale first load still holds the engine mutex inside its
            // non-cancellable hold; make it fail when released.
            engine.nextLoadFailure =
                PlaybackFailureException(
                    PlaybackError(
                        "stale failure",
                        kind = PlaybackFailureKind.Unknown,
                    ),
                )
            engine.releaseHold()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals("track-2", controller.state.value.currentTrack?.id)
            assertNull(
                controller.state.value.error,
                "a stale load failure must never surface as the winner's error",
            )
            assertEquals("track-2", engine.loadedTracks.last().id)
        }

    /**
     * The winner's autoplay intent survives a stale selection request, and
     * ordinary play/pause during Loading keeps working (requirement D).
     */
    @Test
    fun winnerAutoplaySurvivesStaleSelectionAndPausePlayWhileLoading() =
        runBlocking {
            val loadGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(loadGate = loadGate)
            val controller = PlaybackController(engine)
            val track = testTracks(1).single()
            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitLoadStarted()

            // User presses play while the winner is still loading.
            controller.play()
            // A stale request that lost its claim cannot reset the intent: a
            // later selection of an occurrence absent from the queue is a
            // no-op, and the winner keeps its pending play intent.
            controller.selectTrack("missing-track", autoPlay = false)
            engine.releaseLoad()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Playing) kotlinx.coroutines.yield()
            }
            assertEquals(track.id, controller.state.value.currentTrack?.id)
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
        }

    /**
     * Pause during Loading suppresses autoplay, and a later play while still
     * Loading re-enables it (requirement D, ordinary transport behavior).
     */
    @Test
    fun pauseDuringLoadingSuppressesAutoplayUntilPlayIsReissued() =
        runBlocking {
            val loadGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(loadGate = loadGate)
            val controller = PlaybackController(engine)
            val track = testTracks(1).single()
            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitLoadStarted()

            controller.pause()
            engine.releaseLoad()
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)

            // A second load with pause applied while loading.
            val secondGate = CompletableDeferred<Unit>()
            val gated = RecordingPlaybackEngine(loadGate = secondGate)
            val second = PlaybackController(gated)
            second.setQueue(listOf(track), selectedTrackId = track.id)
            gated.awaitLoadStarted()
            second.play()
            gated.releaseLoad()
            withTimeout(5_000) {
                while (second.state.value.status !=
                    PlaybackStatus.Playing) kotlinx.coroutines.yield()
            }
            assertEquals(PlaybackStatus.Playing, second.state.value.status)
        }

    /**
     * Restart of a failed track is admitted through the selection transaction:
     * admission emits exactly the intended restart checkpoint and reloads the
     * occurrence with autoplay (reviewer restart finding).
     */
    @Test
    fun restartFailedTrackAdmissionEmitsIntendedRestartCheckpoint() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val track = testTracks(1).single()
            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitLoad()
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))
            engine.clearEvents()

            controller.restartCurrentTrack()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Playing) kotlinx.coroutines.yield()
            }
            assertEquals(track.id, controller.state.value.currentTrack?.id)
            assertEquals(0L, controller.state.value.positionMillis)
            assertNull(controller.state.value.error)
            assertEquals(
                listOf(
                    EngineEvent.Load("track-1"),
                    EngineEvent.Play,
                ),
                engine.eventSnapshot(),
            )
            val restartCheckpoint = checkpoints.receive()
            assertTrue(restartCheckpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                track.id, restartCheckpoint.snapshot.currentTrackId)
            assertEquals(0L, restartCheckpoint.snapshot.positionMillis)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * Restart whose admitted load is superseded by a replacement never emits a
     * stale post-replacement checkpoint: the checkpoint stream ends with the
     * replacement's own state (reviewer restart finding).
     */
    @Test
    fun supersededRestartEmitsNoStalePreReplacementCheckpoint() =
        runBlocking {
            val loadGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(loadGate = loadGate)
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(2)
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("failed", tracks[0])), "failed")
            // The initial load pauses at the engine gate (holding the engine
            // mutex) so restart and the winner's claims stay serialized here.
            engine.awaitLoadStarted()
            checkpoints.receive()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))

            controller.restartCurrentTrack()
            assertEquals(
                "failed", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)

            val winnerTrack = tracks[1]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", winnerTrack)), "winner")
            engine.releaseLoad()
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "winner", controller.state.value.currentOccurrenceId)
            assertEquals("track-2", engine.loadedTracks.last().id)
            // The stream ends with the winner's checkpoint; the superseded
            // restart appends nothing after it.
            val restartCheckpoint = checkpoints.receive()
            assertTrue(restartCheckpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                "failed", restartCheckpoint.snapshot.currentOccurrenceId)
            val winnerCheckpoint = checkpoints.receive()
            assertTrue(winnerCheckpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                "winner",
                winnerCheckpoint.snapshot.currentOccurrenceId)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * Late callbacks from the failed load emitted between the removal's prune
     * publish and the successor admission cannot mutate the pruned state: the
     * prune invalidates the failed engine-generation token atomically with its
     * publication (audit finding 4).
     */
    @Test
    fun staleFailedCallbacksAfterRemovalPruneCannotMutatePrunedState() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val prunePublished = CompletableDeferred<Unit>()
            val releasePrunePublish = CompletableDeferred<Unit>()
            var blockedPrunePublish = false
            val controller =
                PlaybackController(
                    engine = engine,
                    shuffleOrderFactory = { ids, currentId ->
                        if (!blockedPrunePublish &&
                            "failed-current" !in ids && "successor" in ids
                        ) {
                            blockedPrunePublish = true
                            prunePublished.complete(Unit)
                            runBlocking { releasePrunePublish.await() }
                        }
                        if (currentId == null) ids
                        else
                            listOf(currentId) +
                                ids.filterNot { it == currentId }
                    },
                )
            val tracks = testTracks(2)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("failed-current", tracks[0]),
                    QueueOccurrence("successor", tracks[1]),
                ),
                "failed-current")
            engine.awaitLoad()
            controller.setShuffleMode(ShuffleMode.On)
            val staleGeneration = engine.activeGeneration
            engine.listener?.onPlaybackError(
                staleGeneration, PlaybackError("boom"))
            engine.clearEvents()

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            try {
                withTimeout(5_000) { prunePublished.await() }
                assertEquals(
                    listOf("successor"),
                    controller.state.value.queue.map { it.id })
                assertNull(controller.state.value.currentOccurrenceId)
                val prunedState = controller.state.value

                // Late callbacks from the failed load must be dropped.
                engine.listener?.onPlaybackStatus(
                    staleGeneration, PlaybackStatus.Paused)
                engine.listener?.onPlaybackProgress(
                    staleGeneration, 123L, 1_000L)
                engine.listener?.onPlaybackError(
                    staleGeneration, PlaybackError("late failure"))

                assertEquals(
                    prunedState, controller.state.value,
                    "late failed-load callbacks must not mutate pruned state",
                )
            } finally {
                releasePrunePublish.complete(Unit)
            }

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status ==
                    PlaybackStatus.Loading) kotlinx.coroutines.yield()
            }
            assertEquals(
                "successor", controller.state.value.currentOccurrenceId)
            assertEquals("track-2", engine.loadedTracks.last().id)
            assertEquals(
                listOf(
                    EngineEvent.Load("track-2"),
                    EngineEvent.Play,
                ),
                engine.awaitEvents(2),
            )
        }

    /**
     * A failed restore publishes its empty paused fail-safe checkpoint so the
     * durable session reflects the fail-safe state (audit finding 3).
     */
    @Test
    fun restoreLoadFailureFallbackPublishesCheckpoint() =
        runBlocking {
            val engine =
                RecordingPlaybackEngine(
                    loadFailure = IllegalStateException("load failed"))
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val track = testTracks(1).single()

            controller.restoreSession(
                PlaybackSessionSnapshot(
                    queueIds = listOf(track.id),
                    currentTrackId = track.id,
                    positionMillis = 500L,
                ),
                listOf(track),
            )

            assertEquals(emptyList(), controller.state.value.queue)
            assertNull(controller.state.value.currentTrack)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            val fallback = checkpoints.receive()
            assertTrue(fallback is PlaybackCheckpoint.Immediate)
            assertEquals(emptyList(), fallback.snapshot.queue)
            assertNull(fallback.snapshot.currentTrackId)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * A failed restore superseded by a concurrent selection must not clear or
     * clobber the winner with its empty fail-safe state (audit finding 3).
     */
    @Test
    fun restoreFailureSupersededBySelectionDoesNotClobberWinner() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            val track = tracks[0]

            val restore =
                async(Dispatchers.Default) {
                    controller.restoreSession(
                        PlaybackSessionSnapshot(
                            queueIds = listOf(track.id),
                            currentTrackId = track.id,
                            positionMillis = 500L,
                        ),
                        listOf(track),
                    )
                }
            engine.awaitHoldEntered()
            // The restore load fails only when released; the winner's later
            // load must succeed.
            engine.nextLoadFailure = IllegalStateException("load failed")
            // A selection claims the session while the restore load is paused.
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", tracks[1])), "winner")
            engine.releaseHold()
            restore.await()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "winner", controller.state.value.currentOccurrenceId)
            assertEquals(
                listOf("winner"),
                controller.state.value.queue.map { it.id })
            assertNull(controller.state.value.error)
            assertEquals("track-2", engine.loadedTracks.last().id)
        }

    /**
     * While the winner plays, stale old-generation progress (even at a new
     * per-second bucket), completion and error callbacks can neither mutate
     * the state nor append checkpoints, while fresh owner progress still emits
     * exactly one PlayingProgress checkpoint (audit: progress ownership).
     */
    @Test
    fun staleProgressAndCompletionCannotAppendCheckpointsWhileWinnerPlays() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(2)
            controller.setQueue(listOf(tracks[0]), selectedTrackId = "track-1")
            engine.awaitLoad()
            val staleGeneration = engine.activeGeneration
            engine.listener?.onPlaybackStatus(
                staleGeneration, PlaybackStatus.Playing)
            checkpoints.receive() // setQueue Loading checkpoint

            controller.setQueue(listOf(tracks[1]), selectedTrackId = "track-2")
            engine.awaitLoadCount(2)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            checkpoints.receive()
            val winnerGeneration = engine.activeGeneration
            engine.listener?.onPlaybackStatus(
                winnerGeneration, PlaybackStatus.Playing)
            engine.clearEvents()

            // A stale progress at a brand-new second bucket, plus stale
            // completion and error, must be dropped entirely.
            engine.listener?.onPlaybackProgress(
                staleGeneration, 9_500L, 10_000L)
            engine.listener?.onPlaybackCompleted(staleGeneration)
            engine.listener?.onPlaybackError(
                staleGeneration, PlaybackError("stale"))
            assertEquals(
                "track-2", controller.state.value.currentTrack?.id)
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertNull(controller.state.value.error)
            assertNull(checkpoints.tryReceive().getOrNull())

            // Fresh owner progress in a new bucket still emits exactly one.
            engine.listener?.onPlaybackProgress(
                winnerGeneration, 1_100L, 10_000L)
            val progress = checkpoints.receive()
            assertTrue(progress is PlaybackCheckpoint.PlayingProgress)
            assertEquals(1_100L, progress.snapshot.positionMillis)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * A non-cancellable load cancelled by a reconcile (which republishes the
     * surviving current with a fresh token and settles Loading to Paused) can
     * neither settle nor start playback against the reconciled state, even
     * though the user had requested autoplay for that load (audit: reconcile
     * stale settlement and cancelled autoplay intent).
     */
    @Test
    fun cancelledNonCancellableLoadCannotSettleOrAutoplayAfterReconcile() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("current-a", tracks[0]),
                    QueueOccurrence("upcoming", tracks[1]),
                ),
                "current-a")
            engine.awaitHoldEntered()
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)
            controller.play()
            engine.clearEvents()

            val reconcile =
                async(Dispatchers.Default) {
                    controller.reconcileSession(
                        listOf(
                            tracks[0].copy(title = "Updated"),
                            tracks[1],
                        ))
                }
            reconcile.await()
            assertEquals(
                "current-a", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(
                "Updated", controller.state.value.currentTrack?.title)

            engine.releaseHold()
            // Wait until the cancelled (non-cancellable) load has actually
            // completed its engine load and attempted its settlement.
            engine.awaitLoad()
            kotlinx.coroutines.yield()
            assertEquals(
                "current-a", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(
                "Updated", controller.state.value.currentTrack?.title)
            assertEquals(
                emptyList(), engine.eventSnapshot(),
                "the cancelled load must neither settle nor autoplay",
            )
        }

    /**
     * Clearing the queue cancels the request-owned autoplay intent too: a
     * non-cancellable load that slips past cancellation never starts playback
     * (audit: cancelled autoplay intent).
     */
    @Test
    fun clearingQueueDuringLoadingCancelsAutoplayIntent() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val track = testTracks(1).single()
            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitHoldEntered()
            controller.play()
            engine.clearEvents()

            controller.setOccurrenceQueue(emptyList())
            assertNull(controller.state.value.currentOccurrenceId)
            engine.releaseHold()
            assertEquals(
                listOf(EngineEvent.Clear),
                engine.awaitEvents(1),
            )
            repeat(20) { kotlinx.coroutines.yield() }
            assertEquals(
                emptyList(), engine.eventSnapshot(),
                "the cancelled load must never autoplay after the clear",
            )
            assertNull(controller.state.value.error)
            assertNull(controller.state.value.currentOccurrenceId)
        }

    /**
     * A completion-driven terminal stop emits its Stopped checkpoint before a
     * racing replacement's checkpoint, and a replacement never receives a
     * stale Stopped checkpoint afterwards (audit: terminal stop ownership).
     */
    @Test
    fun terminalStopCheckpointPrecedesRacingReplacement() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val tracks = testTracks(2)
            val track = tracks[0]
            controller.setQueue(listOf(track), selectedTrackId = track.id)
            engine.awaitLoad()
            checkpoints.receive()
            controller.setRepeatMode(RepeatMode.StopAfterCurrent)
            checkpoints.receive()
            val generation = engine.activeGeneration

            engine.listener?.onPlaybackCompleted(generation)

            val stopped = checkpoints.receive()
            assertTrue(stopped is PlaybackCheckpoint.Immediate)
            assertEquals(track.id, stopped.snapshot.currentTrackId)
            assertEquals(
                track.durationMillis, stopped.snapshot.positionMillis)

            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", tracks[1])), "winner")
            engine.awaitLoadCount(2)
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            val winnerCheckpoint = checkpoints.receive()
            assertTrue(winnerCheckpoint is PlaybackCheckpoint.Immediate)
            assertEquals(
                "winner",
                winnerCheckpoint.snapshot.currentOccurrenceId)
            assertNull(checkpoints.tryReceive().getOrNull())
            collection.cancelAndJoin()
        }

    /**
     * Clearing the queue while a selection is loading cancels that request and
     * publishes the cleared session without stale effects: the cleared state
     * wins and the cancelled load never settles over it (request clear race).
     */
    @Test
    fun clearingQueueDuringLoadingCancelsRequestWithoutStaleEffects() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("stuck", tracks[0])), "stuck")
            engine.awaitHoldEntered()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("boom"))
            engine.clearEvents()

            // The retry is admitted while its engine load waits behind the
            // stuck load's engine mutex.
            controller.retryFailedTrack()
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)

            controller.setOccurrenceQueue(emptyList())
            engine.releaseHold()
            withTimeout(5_000) {
                while (controller.state.value.currentOccurrenceId != null &&
                    controller.state.value.status != PlaybackStatus.Paused &&
                    controller.state.value.status != PlaybackStatus.Idle) {
                    kotlinx.coroutines.yield()
                }
            }
            assertNull(controller.state.value.currentOccurrenceId)
            assertNull(controller.state.value.error)
            assertEquals(emptyList(), controller.state.value.queue)
            // The cancelled retry never reached the engine; the clear did.
            assertEquals(
                listOf(EngineEvent.Clear),
                engine.awaitEvents(1),
                "the queue clear dispatches asynchronously and must be awaited",
            )
        }

    /**
     * A reconcile whose paused engine load is superseded by a concurrent
     * selection cannot overwrite the winner's state: settlement and
     * checkpoint emission are token-gated (reconcile race).
     */
    @Test
    fun reconcileReplacementLoadCannotOverwriteConcurrentSelection() =
        runBlocking {
            val loadGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(loadGate = loadGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(3)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("gone", tracks[0]),
                    QueueOccurrence("keep", tracks[1]),
                ),
                "gone")
            // The initial load pauses at the gate holding the engine mutex.
            engine.awaitLoadStarted()

            // Reconcile (track-1 unavailable) replaces the missing current
            // with "keep"; its claim is current and its engine load waits at
            // the gate behind the initial load's engine mutex.
            val reconcile =
                async(Dispatchers.Default) {
                    controller.reconcileSession(listOf(tracks[1]))
                }
            withTimeout(5_000) {
                while (controller.state.value.currentOccurrenceId != "keep") {
                    kotlinx.coroutines.yield()
                }
            }
            val winnerTrack = tracks[2]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", winnerTrack)), "winner")
            engine.releaseLoad()
            reconcile.await()

            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "winner", controller.state.value.currentOccurrenceId)
            assertEquals(
                listOf("winner"),
                controller.state.value.queue.map { it.id })
            assertEquals("track-3", engine.loadedTracks.last().id)
            assertNull(controller.state.value.error)
        }

    @Test
    fun removeFailedTrackCleanupDoesNotCancelReplacementCommittedDuringClear() =
        runBlocking {
            val clearGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(clearGate = clearGate)
            val controller = PlaybackController(engine)
            val failedTrack = testTracks(1).single()
            controller.setQueue(listOf(failedTrack), failedTrack.id)
            engine.awaitLoad()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            engine.awaitClearStarted()
            val replacementTrack = testTracks(2)[1]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("replacement-1", replacementTrack)),
                "replacement-1")
            engine.releaseClear()

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "replacement-1", controller.state.value.currentOccurrenceId)
            assertEquals("track-2", engine.loadedTracks.last().id)
            assertEquals(
                listOf(
                    EngineEvent.Clear,
                    EngineEvent.Load("track-2"),
                ),
                engine.eventSnapshot())
        }

    @Test
    fun removeFailedTrackSkipsEngineClearWhenReplacementClaimsGenerationFirst() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val failedTrack = testTracks(1).single()
            controller.setQueue(listOf(failedTrack), failedTrack.id)
            engine.awaitHoldEntered()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
            engine.clearEvents()

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Idle) kotlinx.coroutines.yield()
            }
            val replacementTrack = testTracks(2)[1]
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("replacement-1", replacementTrack)),
                "replacement-1")
            engine.releaseHold()

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "replacement-1", controller.state.value.currentOccurrenceId)
            assertEquals("track-2", engine.loadedTracks.last().id)
            assertEquals(
                listOf(EngineEvent.Load("track-2")), engine.eventSnapshot())
        }

    @Test
    fun removeFinalFailedTrackDoesNotClearSelectionAllocatedWhileRemovalWaits() =
        runBlocking {
            val holdGate = CompletableDeferred<Unit>()
            val engine = RecordingPlaybackEngine(holdGate = holdGate)
            val controller = PlaybackController(engine)
            val tracks = testTracks(2)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("survivor-1", tracks[0]),
                    QueueOccurrence("failed-1", tracks[1]),
                ),
                "failed-1")
            engine.awaitHoldEntered()
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))

            val removal =
                async(Dispatchers.Default) {
                    controller.removeFailedTrack()
                }
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Idle) kotlinx.coroutines.yield()
            }
            // The failed load still holds the engine mutex, so the removal is
            // waiting to claim its cleanup generation. This selection allocates
            // its generation while the Error snapshot was still current.
            controller.selectOccurrence("survivor-1")
            engine.releaseHold()

            assertEquals(QueueMutationResult.Applied, removal.await())
            withTimeout(5_000) {
                while (controller.state.value.status !=
                    PlaybackStatus.Paused) kotlinx.coroutines.yield()
            }
            assertEquals(
                "survivor-1", controller.state.value.currentOccurrenceId)
            assertEquals("track-1", engine.loadedTracks.last().id)
            assertEquals(
                listOf(
                    EngineEvent.Load("track-2"),
                    EngineEvent.Load("track-1"),
                ),
                engine.eventSnapshot())
        }

    @Test
    fun restoreLoadsClampsSeeksAndPausesWithoutPlayAndEmitsNormalizedSnapshot() =
        runBlocking {
            val engine = RecordingPlaybackEngine(loadedDurationMillis = 1_000L)
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }

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
                listOf(
                    EngineEvent.Load("track-1"),
                    EngineEvent.Seek(1_000L),
                    EngineEvent.Pause),
                engine.awaitEvents(3),
            )
            assertFalse(engine.eventSnapshot().any { it == EngineEvent.Play })
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(1_000L, controller.state.value.positionMillis)
            assertEquals(
                listOf("track-1"),
                controller.state.value.queue.map { it.track.id })
            assertEquals(
                RepeatMode.RepeatOne, controller.state.value.repeatMode)
            assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
            val normalized =
                checkpoints.receive() as PlaybackCheckpoint.Immediate
            assertEquals(
                listOf("track-1"), normalized.snapshot.queue.map { it.trackId })
            assertEquals("track-1", normalized.snapshot.currentTrackId)
            assertEquals(1_000L, normalized.snapshot.positionMillis)
            assertEquals(RepeatMode.RepeatOne, normalized.snapshot.repeatMode)
            assertEquals(ShuffleMode.On, normalized.snapshot.shuffleMode)
            collection.cancelAndJoin()
        }

    @Test
    fun restoreFallsBackToFirstSurvivorAtZeroAndClearsWhenNoneSurvive() =
        runBlocking {
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
            assertEquals(
                listOf(
                    EngineEvent.Load("track-2"),
                    EngineEvent.Seek(0L),
                    EngineEvent.Pause),
                engine.awaitEvents(3))
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
            assertEquals(
                RepeatMode.StopAfterCurrent, controller.state.value.repeatMode)
            assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        }

    @Test
    fun restoreLoadFailureAppliesEmptyPausedFailSafeState() = runBlocking {
        val engine =
            RecordingPlaybackEngine(
                loadFailure = IllegalStateException("load failed"))
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
        assertEquals(
            RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        assertTrue(engine.eventSnapshot().none { it == EngineEvent.Play })
    }

    @Test
    fun structuredLoadFailureSurvivesControllerAsyncCatch() = runBlocking {
        val controller =
            PlaybackController(
                RecordingPlaybackEngine(
                    loadFailure =
                        PlaybackFailureException(
                            PlaybackError(
                                "File is no longer available",
                                kind = PlaybackFailureKind.MissingFile,
                            ),
                        ),
                ),
            )
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), track.id)
        awaitState { controller.state.value.status == PlaybackStatus.Error }

        assertEquals(
            PlaybackFailureKind.MissingFile, controller.state.value.error?.kind)
        assertEquals(
            "File is no longer available",
            controller.state.value.error?.message)
    }

    @Test
    fun opaqueLoadFailureUsesUnknownKind() = runBlocking {
        val controller =
            PlaybackController(
                RecordingPlaybackEngine(
                    loadFailure = IllegalStateException("opaque"),
                ),
            )
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), track.id)
        awaitState { controller.state.value.status == PlaybackStatus.Error }

        assertEquals(
            PlaybackFailureKind.Unknown, controller.state.value.error?.kind)
        assertEquals("Playback failed", controller.state.value.error?.message)
        assertEquals("opaque", controller.state.value.error?.cause)
    }

    @Test
    fun reconcilePreservesSurvivingCurrentWithoutReloadPositionOrStatusChange() =
        runBlocking {
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
    fun reconcileMissingCurrentLoadsFirstSurvivorPausedAndNoSurvivorsClear() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            controller.setQueue(testTracks(3), selectedTrackId = "track-2")
            engine.awaitLoad()
            engine.clearEvents()

            controller.reconcileSession(
                listOf(testTracks(3)[2], testTracks(3)[0]))

            assertEquals("track-1", controller.state.value.currentTrack?.id)
            assertEquals(0L, controller.state.value.positionMillis)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(
                listOf(
                    EngineEvent.Load("track-1"),
                    EngineEvent.Seek(0L),
                    EngineEvent.Pause),
                engine.awaitEvents(3))
            engine.clearEvents()

            controller.reconcileSession(emptyList())

            assertEquals(listOf(EngineEvent.Clear), engine.awaitEvents(1))
            assertEquals(emptyList(), controller.state.value.queue)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        }

    @Test
    fun reconcileReplacementLoadFailurePropagatesAfterApplyingPausedFailSafeState() =
        runBlocking {
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
        val collection =
            launch(start = CoroutineStart.UNDISPATCHED) {
                controller.checkpoints.collect(checkpoints::send)
            }

        controller.setQueue(testTracks(1))
        engine.awaitLoad()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        controller.seekTo(500L)
        assertEquals(500L, checkpoints.receive().snapshot.positionMillis)
        controller.pause()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
        controller.setRepeatMode(RepeatMode.RepeatOne)
        assertEquals(
            RepeatMode.RepeatOne, checkpoints.receive().snapshot.repeatMode)
        controller.setShuffleMode(ShuffleMode.On)
        assertEquals(ShuffleMode.On, checkpoints.receive().snapshot.shuffleMode)
        controller.stop()
        assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)

        collection.cancelAndJoin()
    }

    @Test
    fun playingProgressUsesGenerationTrackAndSecondBucketAndResetsAfterSeek() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = loadedController(engine, PlaybackStatus.Playing)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
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
            assertTrue(
                checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
            collection.cancelAndJoin()
        }

    @Test
    fun progressKeyResetsAcrossStopAndSurvivingCurrentReconciliation() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = loadedController(engine, PlaybackStatus.Playing)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            val generation = engine.activeGeneration
            checkpoints.receive()

            engine.listener?.onPlaybackProgress(generation, 1_100L, 10_000L)
            assertTrue(
                checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
            controller.stop()
            assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
            engine.listener?.onPlaybackStatus(
                generation, PlaybackStatus.Playing)
            engine.listener?.onPlaybackProgress(generation, 1_200L, 10_000L)
            assertTrue(
                checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)

            controller.reconcileSession(testTracks(1))
            assertTrue(checkpoints.receive() is PlaybackCheckpoint.Immediate)
            engine.listener?.onPlaybackProgress(generation, 1_300L, 10_000L)
            assertTrue(
                checkpoints.receive() is PlaybackCheckpoint.PlayingProgress)
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
        val collection =
            launch(start = CoroutineStart.UNDISPATCHED) {
                controller.checkpoints.collect(checkpoints::send)
            }
        repeat(2) { checkpoints.receive() }

        engine.listener?.onPlaybackStatus(
            staleGeneration, PlaybackStatus.Playing)
        engine.listener?.onPlaybackProgress(staleGeneration, 1_500L, 2_000L)
        engine.listener?.onPlaybackCompleted(staleGeneration)
        engine.listener?.onPlaybackError(
            staleGeneration, PlaybackError("stale"))
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

        val reconcile = launch {
            controller.reconcileSession(listOf(testTracks(2)[1]))
        }
        kotlinx.coroutines.yield()

        assertEquals("track-1", controller.state.value.currentTrack?.id)
        assertEquals(
            listOf(EngineEvent.Load("track-1")), engine.eventSnapshot())

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
    fun restartSkipAndCompletionEmitImmediateCurrentPositionBeforeProgress() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val collection =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect(checkpoints::send)
                }
            controller.setQueue(testTracks(3), "track-2")
            engine.awaitLoad()
            checkpoints.receive()
            engine.listener?.onPlaybackStatus(
                engine.activeGeneration, PlaybackStatus.Paused)
            engine.listener?.onPlaybackProgress(
                engine.activeGeneration, 500L, 2_000L)

            controller.restartCurrentTrack()
            val restarted = checkpoints.receive().snapshot
            assertEquals(
                listOf("track-1", "track-2", "track-3"),
                restarted.queue.map { it.trackId })
            assertEquals("track-2", restarted.currentTrackId)
            assertEquals(0L, restarted.positionMillis)

            controller.skipToNext()
            assertEquals(
                "track-3", checkpoints.receive().snapshot.currentTrackId)
            assertEquals(0L, controller.sessionSnapshot().positionMillis)
            engine.awaitLoadCount(2)

            controller.skipToPrevious()
            assertEquals(
                "track-2", checkpoints.receive().snapshot.currentTrackId)
            engine.awaitLoadCount(3)

            engine.listener?.onPlaybackCompleted(engine.activeGeneration)
            val completion = checkpoints.receive()
            assertTrue(completion is PlaybackCheckpoint.Immediate)
            assertEquals("track-3", completion.snapshot.currentTrackId)
            assertEquals(0L, completion.snapshot.positionMillis)
            collection.cancelAndJoin()
        }

    @Test
    fun terminalCompletionEmitsExactlyOneImmediateCheckpointAtExactDuration() =
        runBlocking {
            for (mode in
                listOf(
                    RepeatMode.StopAfterCurrent, RepeatMode.StopAfterQueue)) {
                val engine = RecordingPlaybackEngine()
                val controller = PlaybackController(engine)
                val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
                val collection =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        controller.checkpoints.collect(checkpoints::send)
                    }
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
    fun checkpointsProducedBeforeCollectorStartsAreDeliveredInOrder() =
        runBlocking {
            val controller = PlaybackController(RecordingPlaybackEngine())
            controller.setRepeatMode(RepeatMode.RepeatOne)
            controller.setShuffleMode(ShuffleMode.On)
            controller.setRepeatMode(RepeatMode.StopAfterCurrent)

            val received = mutableListOf<PlaybackCheckpoint>()
            controller.checkpoints.take(3).collect(received::add)

            assertEquals(
                listOf(
                    RepeatMode.RepeatOne,
                    RepeatMode.RepeatOne,
                    RepeatMode.StopAfterCurrent),
                received.map { it.snapshot.repeatMode },
            )
            assertEquals(
                listOf(ShuffleMode.Off, ShuffleMode.On, ShuffleMode.On),
                received.map { it.snapshot.shuffleMode })
        }

    @Test
    fun checkpointFenceCompletesAfterCollectorConsumesEveryPriorCheckpoint() =
        runBlocking {
            val controller = PlaybackController(RecordingPlaybackEngine())
            controller.setRepeatMode(RepeatMode.RepeatOne)
            controller.setShuffleMode(ShuffleMode.On)
            val received = mutableListOf<PlaybackCheckpoint>()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect { checkpoint ->
                        received += checkpoint
                    }
                }

            controller.awaitCheckpointFence()

            assertEquals(2, received.size)
            assertEquals(
                RepeatMode.RepeatOne, received.first().snapshot.repeatMode)
            assertEquals(ShuffleMode.On, received.last().snapshot.shuffleMode)
            collector.cancelAndJoin()
        }

    @Test
    fun slowCollectorReceivesMoreThanSixtyFourDiscreteCheckpointsWithoutLoss() =
        runBlocking {
            val controller = PlaybackController(RecordingPlaybackEngine())
            repeat(80) { index ->
                controller.setRepeatMode(
                    if (index % 2 == 0) RepeatMode.RepeatOne
                    else RepeatMode.StopAfterQueue)
            }

            val received = mutableListOf<PlaybackCheckpoint>()
            controller.checkpoints.take(80).collect { checkpoint ->
                received += checkpoint
                kotlinx.coroutines.yield()
            }

            assertEquals(80, received.size)
            assertEquals(
                RepeatMode.RepeatOne, received.first().snapshot.repeatMode)
            assertEquals(
                RepeatMode.StopAfterQueue, received.last().snapshot.repeatMode)
        }

    @Test
    fun restoreAndReconcileDeduplicateRuntimeTrackIdsWithStableFirstWinsMetadata() =
        runBlocking {
            val engine = RecordingPlaybackEngine()
            val controller = PlaybackController(engine)
            val first = testTracks(1).single().copy(title = "First")
            val duplicate = first.copy(title = "Duplicate")
            val second = testTracks(2)[1]

            controller.restoreSession(
                PlaybackSessionSnapshot(
                    queueIds = listOf("track-1", "track-1", "track-2"),
                    currentTrackId = "track-1"),
                listOf(first, duplicate, second),
            )

            assertEquals(
                listOf("track-1", "track-1", "track-2"),
                controller.state.value.queue.map { it.track.id })
            assertEquals("First", controller.state.value.currentTrack?.title)
            assertEquals(
                "First", controller.state.value.queue.first().track.title)

            controller.setCommandsEnabled(true)
            controller.setQueue(
                listOf(first, duplicate, second), selectedTrackId = "track-2")
            engine.awaitLoadCount(2)
            controller.reconcileSession(listOf(first, duplicate, second))

            assertEquals(
                listOf("track-1", "track-1", "track-2"),
                controller.state.value.queue.map { it.track.id })
            assertEquals(
                "First", controller.state.value.queue.first().track.title)
        }

    /**
     * Regression guard for cross-thread generation allocation. The controller
     * is driven from one caller thread in production, but the session
     * coordinator actor (reconcile/restore), engine callback threads
     * (completion/skip), and UI event handlers can all issue queue mutations
     * concurrently. Every load that actually reaches the engine must carry a
     * unique generation; a reused generation would let a stale engine callback
     * pass the active-generation guard.
     */
    @Test
    fun concurrentQueueSelectionsNeverReuseGenerations() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        val workers =
            (1..12).map { _ ->
                async(Dispatchers.Default) {
                    repeat(20) { index ->
                        controller.setQueue(
                            tracks,
                            selectedTrackId =
                                if (index % 2 == 0) "track-1" else "track-2",
                        )
                        controller.play()
                        controller.pause()
                    }
                }
            }
        workers.awaitAll()

        // The controller's loads run on its own dispatcher, so drain the
        // engine's generation signals until one quiet second proves the storm
        // has settled. Only loads that actually ran are observed.
        val observed = mutableListOf<Long>()
        withTimeout(10_000) {
            while (true) {
                val next =
                    withTimeoutOrNull(1_000) {
                        engine.generationSignals.receive()
                    } ?: break
                observed += next
            }
        }
        assertTrue(
            observed.isNotEmpty(),
            "the storm must produce at least one engine load",
        )
        assertEquals(
            observed.size,
            observed.distinct().size,
            "concurrent queue selections must never reuse a generation",
        )
        val state = controller.state.value
        assertNotNull(state.currentOccurrenceId)
        assertEquals(2, state.queue.size)
        assertTrue(
            state.queue.any { it.id == state.currentOccurrenceId },
            "selected occurrence must remain in the queue",
        )
    }

    private suspend fun awaitState(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) kotlinx.coroutines.yield()
        }
    }

    private suspend fun loadedController(
        engine: RecordingPlaybackEngine,
        status: PlaybackStatus,
    ): PlaybackController {
        val controller = PlaybackController(engine)
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), selectedTrackId = track.id)
        engine.awaitLoad()
        engine.listener?.onPlaybackProgress(
            engine.activeGeneration, 500L, track.durationMillis)
        if (status == PlaybackStatus.Error) {
            engine.listener?.onPlaybackError(
                engine.activeGeneration, PlaybackError("Test error"))
        } else {
            engine.listener?.onPlaybackStatus(engine.activeGeneration, status)
        }
        return controller
    }

    private fun testTracks(count: Int): List<PlayableTrack> =
        (1..count).map { index ->
            PlayableTrack(
                id = "track-$index",
                title = "Track $index",
                artist = "Test Artist",
                album = "Test Album",
                durationMillis = index * 1_000L,
                source = AudioSource.FilePath("/tmp/track-$index.mp3"),
            )
        }

    private fun occurrenceQueue(): List<QueueOccurrence> {
        val tracks = testTracks(3)
        return listOf(
            QueueOccurrence("current", tracks[0]),
            QueueOccurrence("upcoming-1", tracks[1]),
            QueueOccurrence("upcoming-2", tracks[2]),
        )
    }

    private class DelayedStatusPlaybackEngine : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        var activeGeneration: Long = 0L
            private set

        private var loadCount: Int = 0
        private val loadSignals = Channel<Int>(Channel.UNLIMITED)

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            activeGeneration = generation
            loadCount++
            check(loadSignals.trySend(loadCount).isSuccess)
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

        suspend fun awaitLoadCount(count: Int) =
            withTimeout(5_000) {
                while (loadSignals.receive() < count) {}
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
        private val clearGate: CompletableDeferred<Unit>? = null,
        private val holdGate: CompletableDeferred<Unit>? = null,
        private val loadedDurationMillis: Long? = null,
        private val loadFailure: Throwable? = null,
    ) : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val loadedTracks = mutableListOf<PlayableTrack>()
        val loadedGenerations = mutableListOf<Long>()
        val generationSignals = Channel<Long>(Channel.UNLIMITED)
        var activeGeneration: Long = 0L
            private set

        var nextLoadFailure: Throwable? = null
        private val events = Channel<EngineEvent>(Channel.UNLIMITED)
        private val loadStarted = CompletableDeferred<Unit>()
        private val loadSignal = CompletableDeferred<Unit>()
        private val loadCountSignals = Channel<Int>(Channel.UNLIMITED)
        private val seekStarted = CompletableDeferred<Unit>()
        private val clearStarted = CompletableDeferred<Unit>()
        private val holdEntered = CompletableDeferred<Unit>()

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            activeGeneration = generation
            record(EngineEvent.Load(track.id))
            loadedTracks += track
            loadedGenerations += generation
            check(loadCountSignals.trySend(loadedGenerations.size).isSuccess)
            check(generationSignals.trySend(generation).isSuccess)
            loadStarted.complete(Unit)
            loadGate?.await()
            holdGate?.let {
                holdEntered.complete(Unit)
                // Non-cancellable: keeps the engine mutex held so a racing
                // removal stays blocked until the test releases the hold.
                runBlocking { it.await() }
            }
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

        suspend fun awaitLoadCount(count: Int) =
            withTimeout(5_000) {
                while (loadCountSignals.receive() < count) {}
            }

        fun releaseLoad() {
            loadGate?.complete(Unit)
        }

        suspend fun awaitSeekStarted() = seekStarted.await()

        fun releaseSeek() {
            seekGate?.complete(Unit)
        }

        suspend fun awaitClearStarted() = clearStarted.await()

        fun releaseClear() {
            clearGate?.complete(Unit)
        }

        suspend fun awaitHoldEntered() = holdEntered.await()

        fun releaseHold() {
            holdGate?.complete(Unit)
        }

        fun clearEvents() {
            while (events.tryReceive().isSuccess) {}
        }

        fun eventSnapshot(): List<EngineEvent> = buildList {
            while (true) add(events.tryReceive().getOrNull() ?: break)
        }

        suspend fun awaitEvents(count: Int): List<EngineEvent> =
            withTimeout(5_000) {
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
            listener?.onPlaybackProgress(
                activeGeneration,
                positionMillis,
                loadedTracks.lastOrNull()?.durationMillis)
        }

        override fun clear(generation: Long) {
            clearStarted.complete(Unit)
            clearGate?.let { runBlocking { it.await() } }
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

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback =
            if (firstGeneration == 0L) {
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

        fun completeFirst(): Boolean =
            firstResult.complete(LoadedPlayback(firstGeneration, 1_000L)).also {
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

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            record(EngineEvent.Load(track.id))
            loadCount++
            if (loadCount == 1) {
                firstLoadStarted.complete(Unit)
                releaseFirstLoad.await()
            }
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun seekTo(positionMillis: Long) =
            record(EngineEvent.Seek(positionMillis))

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
