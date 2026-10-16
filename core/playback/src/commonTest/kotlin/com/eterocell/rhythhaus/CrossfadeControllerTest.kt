package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackCheckpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/** Exercises controller authority at the real two-voice engine boundary. */
class CrossfadeControllerTest {
    @Test
    fun gaplessPreparesEarlyAndPromotesOnPlayingWithoutStoppedGap() =
        runBlocking {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            val statuses = Channel<PlaybackStatus>(Channel.UNLIMITED)
            val observer =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.state.collect { statuses.send(it.status) }
                }
            controller.setGaplessEnabled(true)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            assertEquals(0, engine.prepareCount)
            engine.progress(18_000L)
            engine.awaitEvent("prepare:b")
            engine.awaitEvent("prepared")
            engine.awaitPreparationSettled()
            assertEquals(1, engine.prepareCount)
            val outgoing = engine.current()
            assertEquals("a", outgoing.track.id)
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertEquals("a", controller.state.value.currentTrack?.id)
            engine.complete(outgoing)
            engine.awaitEvent("incoming-play")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertFalse(controller.state.value.status == PlaybackStatus.Stopped)
            engine.confirmIncoming()
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            val observedStatuses = mutableListOf<PlaybackStatus>()
            while (true) {
                val status = statuses.tryReceive().getOrNull() ?: break
                observedStatuses.add(status)
            }
            assertFalse(observedStatuses.contains(PlaybackStatus.Stopped))
            controller.release()
            observer.cancelAndJoin()
        }

    @Test
    fun trackCountSleepTimerStopsAtFirstGaplessBoundaryWithoutPromotion() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setGaplessEnabled(true)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(18_000L)
            engine.awaitEvent("prepared")
            controller.armSleepTimerAfterCompletions(1, fadeEnabled = false)
            engine.complete(engine.current())
            engine.awaitEvent("stop")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
            assertEquals(0, engine.incomingPlayCount)
            assertEquals(SleepTimerState(), controller.sleepTimerState.value)
            controller.release()
        }

    @Test
    fun trackCountSleepTimerCountsGaplessOutgoingCompletionOnce() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setGaplessEnabled(true)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(18_000L)
            engine.awaitEvent("prepared")
            controller.armSleepTimerAfterCompletions(2, fadeEnabled = true)
            val outgoing = engine.current()
            engine.complete(outgoing)
            engine.awaitEvent("incoming-play")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            engine.complete(outgoing)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            engine.confirmIncoming()
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            engine.progress(11_000L)
            withTimeout(5_000) {
                while (engine.current().gain != 0.9f) yield()
            }
            assertEquals(0.9f, engine.current().gain)
            engine.complete(engine.current())
            engine.awaitEvent("stop")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
            assertEquals(SleepTimerState(), controller.sleepTimerState.value)
            controller.release()
        }

    @Test
    fun pausingCancelsPreparedGaplessSuccessor() = runBlocking {
        val engine = TwoVoiceEngine()
        val controller = controller(engine)
        controller.setGaplessEnabled(true)
        controller.setOccurrenceQueue(queue(), autoPlay = true)
        engine.awaitEvent("play")
        engine.progress(18_000L)
        engine.awaitEvent("prepared")
        controller.pause()
        engine.awaitEvent("cancel-prepared")
        engine.awaitEvent("pause")
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)
        assertEquals("a", controller.state.value.currentOccurrenceId)
        assertTrue(engine.pendingIncomingOrNull() == null)
        controller.release()
    }

    @Test
    fun gaplessDuplicateSuccessorUsesOccurrenceIdentity() = runBlocking {
        val engine = TwoVoiceEngine()
        val controller = controller(engine)
        val repeatedTrack = track("same")
        controller.setGaplessEnabled(true)
        controller.setOccurrenceQueue(
            listOf(
                QueueOccurrence("first", repeatedTrack),
                QueueOccurrence("second", repeatedTrack),
                QueueOccurrence("third", track("third")),
            ),
            autoPlay = true,
        )
        engine.awaitEvent("play")
        engine.progress(18_000L)
        engine.awaitEvent("prepared")
        engine.awaitPreparationSettled()
        engine.complete(engine.current())
        engine.awaitEvent("incoming-play")
        engine.confirmIncoming()
        awaitState { controller.state.value.currentOccurrenceId == "second" }
        engine.progress(18_000L)
        engine.awaitEvent("prepare:third")
        engine.awaitEvent("prepared")
        assertEquals(2, engine.prepareCount)
        assertEquals("same", controller.state.value.currentTrack?.id)
        controller.release()
    }

    @Test
    fun gaplessUsesOrdinaryCompletionWhenEitherDurationIsUnknown() =
        runBlocking {
            for (unknownCurrent in listOf(true, false)) {
                val engine = TwoVoiceEngine()
                val controller = controller(engine)
                controller.setGaplessEnabled(true)
                val queue =
                    listOf(
                        QueueOccurrence(
                            "a",
                            track(
                                "a",
                                duration =
                                    if (unknownCurrent) null else 20_000L)),
                        QueueOccurrence(
                            "b",
                            track(
                                "b",
                                duration =
                                    if (unknownCurrent) 20_000L else null)),
                    )
                controller.setOccurrenceQueue(queue, autoPlay = true)
                engine.awaitEvent("play")
                if (unknownCurrent) {
                    assertEquals(0, engine.prepareCount)
                } else {
                    engine.progress(18_000L)
                    engine.awaitEvent("prepare:b")
                    engine.awaitEvent("prepared")
                    engine.awaitEvent("cancel-prepared")
                }
                engine.complete(engine.current())
                engine.awaitEvent("load:b")
                awaitState { controller.state.value.currentOccurrenceId == "b" }
                assertEquals(0, engine.incomingPlayCount)
                controller.release()
            }
        }

    @Test
    fun gaplessPromotionFailurePublishesErrorForSuccessor() = runBlocking {
        val engine = TwoVoiceEngine(failGaplessStart = true)
        val controller = controller(engine)
        controller.setGaplessEnabled(true)
        controller.setOccurrenceQueue(queue(), autoPlay = true)
        engine.awaitEvent("play")
        engine.progress(18_000L)
        engine.awaitEvent("prepared")
        val outgoingGeneration = controller.state.value.engineGeneration
        engine.complete(engine.current())
        awaitState { controller.state.value.status == PlaybackStatus.Error }
        assertEquals("b", controller.state.value.currentOccurrenceId)
        assertTrue(
            outgoingGeneration != controller.state.value.engineGeneration)
        assertEquals(
            "Incoming start failed", controller.state.value.error?.message)
        assertEquals(0, engine.incomingPlayCount)
        controller.release()
    }

    @Test
    fun gaplessPreparationFailurePublishesSuccessorErrorAtCompletion() =
        runBlocking {
            val engine = TwoVoiceEngine(prepareFailure = true)
            val controller = controller(engine)
            controller.setGaplessEnabled(true)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            controller.armSleepTimerAfterCompletions(2, fadeEnabled = false)
            engine.awaitEvent("play")
            engine.progress(18_000L)
            engine.awaitEvent("prepare:b")
            engine.awaitEvent("prepare-failed")
            engine.awaitPreparationSettled()
            engine.complete(engine.current())
            awaitState {
                controller.state.value.status == PlaybackStatus.Error &&
                    controller.state.value.currentOccurrenceId == "b"
            }
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(
                "Incoming preparation failed",
                controller.state.value.error?.message)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            controller.release()
        }

    @Test
    fun crossfadeTakesPrecedenceOverGaplessPreparation() = runBlocking {
        val engine = TwoVoiceEngine()
        val controller = controller(engine)
        controller.setGaplessEnabled(true)
        controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
        controller.setOccurrenceQueue(queue(), autoPlay = true)
        engine.awaitEvent("play")
        assertEquals(0, engine.prepareCount)
        engine.progress(17_000L)
        engine.awaitEvent("incoming-play")
        assertEquals(1, engine.prepareCount)
        assertEquals("b", controller.state.value.currentOccurrenceId)
        controller.release()
    }

    @Test
    fun disablingGaplessCancelsPreparationAndFencesLateIncomingStatus() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setGaplessEnabled(true)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(18_000L)
            engine.awaitEvent("prepared")
            val staleIncoming = engine.pendingIncoming()
            controller.setGaplessEnabled(false)
            engine.awaitEvent("cancel-prepared")
            engine.listener?.onPlaybackStatus(
                staleIncoming.generation, PlaybackStatus.Playing)
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals("a", controller.state.value.currentTrack?.id)
            controller.release()
        }

    @Test
    fun incomingActuallyPlaysAlongsideOutgoingAndPromotesOnlyOnPlaying() =
        runBlocking {
            val engine =
                TwoVoiceEngine(
                    confirmIncoming = false, incomingStartPosition = 120L)
            val controller = controller(engine)
            val started = Channel<PlaybackStarted>(Channel.UNLIMITED)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val startedJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.playbackStarted.collect { started.send(it) }
                }
            val checkpointJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect { checkpoints.send(it) }
                }
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            assertEquals(
                "a", withTimeout(5_000) { started.receive() }.occurrenceId)
            drain(checkpoints)
            val oldGeneration = controller.state.value.engineGeneration
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            val voices = engine.voices()
            assertEquals(2, voices.size)
            assertTrue(voices.all { it.playing })
            assertEquals(listOf(1f, 0f), voices.map { it.gain })
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(oldGeneration, controller.state.value.engineGeneration)
            assertNull(started.tryReceive().getOrNull())
            drain(checkpoints)

            engine.confirmIncoming()
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            val promoted = withTimeout(5_000) { started.receive() }
            assertEquals("b", promoted.occurrenceId)
            assertEquals(engine.current().generation, promoted.generation)
            val checkpoint = withTimeout(5_000) { checkpoints.receive() }
            assertTrue(checkpoint is PlaybackCheckpoint.Immediate)
            assertEquals("b", checkpoint.snapshot.currentOccurrenceId)
            assertEquals(120L, checkpoint.snapshot.positionMillis)
            engine.confirmIncoming()
            controller.awaitCheckpointFence()
            assertNull(started.tryReceive().getOrNull())
            assertNull(checkpoints.tryReceive().getOrNull())
            controller.release()
            startedJob.cancelAndJoin()
            checkpointJob.cancelAndJoin()
        }

    @Test
    fun shuffledSuccessorUsesOccurrenceIdentityEvenForDuplicateTrackIds() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller =
                PlaybackController(
                    engine,
                    { _, _ -> listOf("a", "duplicate", "b") },
                    { null },
                    Clock())
            val duplicate = track("a")
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(
                listOf(
                    QueueOccurrence("a", duplicate),
                    QueueOccurrence("b", track("b")),
                    QueueOccurrence("duplicate", duplicate)),
                autoPlay = true,
            )
            engine.awaitEvent("play")
            controller.setShuffleMode(ShuffleMode.On)
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState {
                controller.state.value.currentOccurrenceId == "duplicate"
            }
            assertEquals("a", controller.state.value.currentTrack?.id)
            assertEquals(listOf("a", "a"), engine.voices().map { it.track.id })
            controller.release()
        }

    @Test
    fun retiringCompletionCannotAdvanceTwiceOrConsumeNewSleepTimer() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            val old = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            controller.armSleepTimerAfterCompletions(1, fadeEnabled = false)
            engine.awaitEvent("retire")
            engine.complete(old)
            engine.complete(old)
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            engine.complete(engine.current())
            engine.awaitEvent("stop")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
            controller.release()
        }

    @Test
    fun naturalRetiringCompletionReleasesOnlyOldVoiceAndNextCompletionAdvancesOnce() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            val old = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            val incoming = engine.current()
            engine.complete(old)
            engine.awaitEvent("retire")
            assertEquals(incoming.generation, engine.current().generation)
            assertEquals("b", controller.state.value.currentOccurrenceId)
            engine.complete(old)
            engine.complete(incoming)
            engine.awaitEvent("load:c")
            engine.awaitEvent("play")
            assertEquals("c", controller.state.value.currentOccurrenceId)
            engine.complete(incoming)
            assertEquals("c", controller.state.value.currentOccurrenceId)
            controller.release()
        }

    @Test
    fun blockedNonCancellablePreparationIsSupersededWithoutTouchingWinner() =
        runBlocking {
            val preparation = CompletableDeferred<Unit>()
            val engine = TwoVoiceEngine(prepareGate = preparation)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(17_000L)
            engine.awaitEvent("prepare:b")
            controller.selectOccurrence("c", autoPlay = true)
            assertEquals("c", controller.state.value.currentOccurrenceId)
            preparation.complete(Unit)
            engine.awaitEvent("load:c")
            engine.awaitEvent("play")
            assertEquals("c", engine.current().track.id)
            assertEquals(listOf("c"), engine.voices().map { it.track.id })
            assertEquals(0, engine.incomingPlayCount)
            controller.release()
        }

    @Test
    fun queueReplacementWinsAgainstBlockedPreparationAndOldCompletion() =
        runBlocking {
            val preparation = CompletableDeferred<Unit>()
            val engine = TwoVoiceEngine(prepareGate = preparation)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            val old = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("prepare:b")
            controller.setOccurrenceQueue(
                listOf(QueueOccurrence("winner", track("winner"))),
                autoPlay = true)
            engine.complete(old)
            preparation.complete(Unit)
            engine.awaitEvent("load:winner")
            engine.awaitEvent("play")
            assertEquals("winner", controller.state.value.currentOccurrenceId)
            assertEquals(listOf("winner"), engine.voices().map { it.track.id })
            controller.release()
        }

    @Test
    fun clearAndReleaseInvalidateBlockedPreparation() = runBlocking {
        for (release in listOf(false, true)) {
            val preparation = CompletableDeferred<Unit>()
            val engine = TwoVoiceEngine(prepareGate = preparation)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(17_000L)
            engine.awaitEvent("prepare:b")
            if (release) controller.release()
            else controller.setOccurrenceQueue(emptyList())
            preparation.complete(Unit)
            engine.awaitEvent(if (release) "released-prepare" else "clear")
            assertTrue(engine.voices().isEmpty())
            assertEquals(0, engine.incomingPlayCount)
            if (!release) {
                assertNull(controller.state.value.currentOccurrenceId)
                controller.release()
            }
        }
    }

    @Test
    fun linearPerTrackNormalizationIsRestoredBeforeBothVoicesResume() =
        runBlocking {
            val clock = Clock()
            val engine = TwoVoiceEngine()
            val controller = controller(engine, clock)
            controller.setReplayGainMode(ReplayGainMode.Track)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(
                queue(outgoingPeak = 2.0, incomingPeak = 4.0), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            engine.progress(1_500L)
            engine.awaitGains(0.25f, 0.125f)
            controller.pause()
            engine.awaitEvent("pause")
            val paused = engine.voices()
            clock.advanceBy(10_000L)
            assertEquals(paused, engine.voices())
            controller.play()
            engine.awaitEvent("play")
            assertEquals(listOf(0.25f, 0.125f), engine.lastPlayedGains)
            assertTrue(engine.voices().all { it.playing })
            controller.setReplayGainMode(ReplayGainMode.Off)
            engine.awaitGains(0.5f, 0.5f)
            assertEquals("b", controller.state.value.currentOccurrenceId)
            engine.progress(3_000L)
            engine.awaitEvent("retire")
            assertEquals(listOf(1f), engine.voices().map { it.gain })
            controller.release()
        }

    @Test
    fun shortIncomingDurationControlsWindowAndPreparationDoesNotStartTooEarly() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds5)
            controller.setOccurrenceQueue(
                queue(incomingDuration = 4_000L), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(14_000L)
            engine.awaitEvent("prepared")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(0, engine.incomingPlayCount)
            engine.progress(18_000L)
            engine.awaitEvent("incoming-play")
            engine.progress(1_000L)
            engine.awaitGains(0.5f, 0.5f)
            engine.progress(2_000L)
            engine.awaitEvent("retire")
            controller.release()
        }

    @Test
    fun preparationFinishingWithLessThan250MillisDoesNotStartAnOverlap() =
        runBlocking {
            val preparation = CompletableDeferred<Unit>()
            val engine = TwoVoiceEngine(prepareGate = preparation)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(17_000L)
            engine.awaitEvent("prepare:b")
            engine.progress(19_751L)
            preparation.complete(Unit)
            engine.awaitEvent("cancel-prepared")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(0, engine.incomingPlayCount)
            assertEquals(listOf(1f), engine.voices().map { it.gain })
            controller.release()
        }

    @Test
    fun seekAndManualNextRetireOverlapBeforeChangingLogicalCurrent() =
        runBlocking {
            for (seek in listOf(false, true)) {
                val engine = TwoVoiceEngine()
                val controller = controller(engine)
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                controller.setOccurrenceQueue(queue(), autoPlay = true)
                engine.awaitEvent("play")
                engine.progress(17_000L)
                engine.awaitEvent("incoming-play")
                awaitState { controller.state.value.currentOccurrenceId == "b" }
                engine.progress(1_500L)
                engine.awaitGains(0.5f, 0.5f)
                if (seek) {
                    controller.seekTo(4_000L)
                    engine.awaitEvent("seek")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(4_000L, engine.current().position)
                    assertEquals(listOf(1f), engine.voices().map { it.gain })
                } else {
                    controller.skipToNext()
                    engine.awaitEvent("load:c")
                    engine.awaitEvent("play")
                    assertEquals(
                        "c", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        listOf("c"), engine.voices().map { it.track.id })
                }
                controller.release()
            }
        }

    @Test
    fun preferenceRepeatAndTimerChangesCancelPreparedOrRetireStartedVoice() =
        runBlocking {
            for (change in
                listOf("off", "repeat", "stop-after", "timer", "timed")) {
                for (started in listOf(false, true)) {
                    val engine = TwoVoiceEngine()
                    val controller = controller(engine)
                    controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                    controller.setOccurrenceQueue(queue(), autoPlay = true)
                    engine.awaitEvent("play")
                    engine.progress(if (started) 17_000L else 16_000L)
                    engine.awaitEvent(
                        if (started) "incoming-play" else "prepared")
                    if (started)
                        awaitState {
                            controller.state.value.currentOccurrenceId == "b"
                        }
                    when (change) {
                        "off" ->
                            controller.setCrossfadeDuration(
                                CrossfadeDuration.Off)
                        "repeat" ->
                            controller.setRepeatMode(RepeatMode.RepeatOne)
                        "stop-after" ->
                            controller.setRepeatMode(
                                RepeatMode.StopAfterCurrent)
                        "timer" ->
                            controller.armSleepTimerAfterCompletions(
                                1, fadeEnabled = false)
                        else -> controller.armSleepTimer(1, fadeEnabled = false)
                    }
                    engine.awaitEvent(
                        if (started) "retire" else "cancel-prepared")
                    awaitState {
                        engine.voices().size == 1 && engine.current().gain == 1f
                    }
                    assertEquals(
                        if (started) "b" else "a",
                        controller.state.value.currentOccurrenceId)
                    controller.release()
                }
            }
        }

    @Test
    fun reconciliationRemovesEitherMemberWithoutClearingSurvivingPromotedVoice() =
        runBlocking {
            for (removed in listOf("a", "b")) {
                val engine = TwoVoiceEngine()
                val controller = controller(engine)
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                controller.setOccurrenceQueue(queue(), autoPlay = true)
                engine.awaitEvent("play")
                engine.progress(17_000L)
                engine.awaitEvent("incoming-play")
                awaitState { controller.state.value.currentOccurrenceId == "b" }
                val incoming = engine.current()
                controller.reconcileSession(
                    queue().map { it.track }.filterNot { it.id == removed })
                if (removed == "a") {
                    engine.awaitEvent("retire")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        incoming.generation, engine.current().generation)
                    assertEquals(
                        PlaybackStatus.Playing, controller.state.value.status)
                } else {
                    assertEquals(
                        "a", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        PlaybackStatus.Paused, controller.state.value.status)
                    assertEquals("a", engine.current().track.id)
                }
                assertEquals(1, engine.voices().size)
                controller.release()
            }
        }

    @Test
    fun queueMutationPreservesSameSuccessorButCancelsChangedSuccessor() =
        runBlocking {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(
                queue() + QueueOccurrence("d", track("d")), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(16_000L)
            engine.awaitEvent("prepared")
            controller.removeUpcoming("d")
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            assertEquals(2, engine.voices().size)
            controller.release()

            val stagedEngine = TwoVoiceEngine()
            val stagedController = controller(stagedEngine)
            stagedController.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            stagedController.setOccurrenceQueue(queue(), autoPlay = true)
            stagedEngine.awaitEvent("play")
            stagedEngine.progress(16_000L)
            stagedEngine.awaitEvent("prepared")
            stagedController.reorderUpcoming("c", 0)
            stagedEngine.awaitEvent("cancel-prepared")
            assertEquals("a", stagedController.state.value.currentOccurrenceId)
            assertEquals(0, stagedEngine.incomingPlayCount)
            stagedController.release()
        }

    @Test
    fun preparationFailureKeepsOutgoingAndIncomingFailureCannotResurrectIt() =
        runBlocking {
            val preparationEngine = TwoVoiceEngine(prepareFailure = true)
            val preparationController = controller(preparationEngine)
            preparationController.setCrossfadeDuration(
                CrossfadeDuration.Seconds3)
            preparationController.setOccurrenceQueue(queue(), autoPlay = true)
            preparationEngine.awaitEvent("play")
            preparationEngine.progress(17_000L)
            preparationEngine.awaitEvent("cancel-prepared")
            assertEquals(
                PlaybackStatus.Playing,
                preparationController.state.value.status)
            assertNull(preparationController.state.value.error)
            assertEquals(
                listOf("a"), preparationEngine.voices().map { it.track.id })
            preparationEngine.complete(preparationEngine.current())
            preparationEngine.awaitEvent("load:b")
            preparationEngine.awaitEvent("play")
            assertEquals(
                "b", preparationController.state.value.currentOccurrenceId)
            preparationController.release()

            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            val old = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            engine.failCurrent()
            engine.awaitEvent("retire")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Error, controller.state.value.status)
            assertNotNull(controller.state.value.error)
            engine.complete(old)
            engine.confirmIncoming()
            assertEquals(PlaybackStatus.Error, controller.state.value.status)
            assertEquals("b", controller.state.value.currentOccurrenceId)
            controller.release()
        }

    @Test
    fun incomingErrorBeforePlayingRollsBackWithoutRestartingOutgoing() =
        runBlocking {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            controller.setReplayGainMode(ReplayGainMode.Track)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(
                queue(outgoingPeak = 2.0), autoPlay = true)
            engine.awaitEvent("play")
            val outgoing = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            engine.failCurrent()
            engine.awaitEvent("cancel-prepared")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertNull(controller.state.value.error)
            assertEquals(outgoing.generation, engine.current().generation)
            assertEquals(outgoing.run, engine.current().run)
            assertEquals(0.5f, engine.current().gain)
            assertTrue(engine.current().playing)
            engine.complete(outgoing)
            engine.awaitEvent("load:b")
            engine.awaitEvent("play")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            controller.release()
        }

    @Test
    fun incomingPauseBeforePlayingRollsBackAndPublishesOutgoingPaused() =
        runBlocking {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            val started = Channel<PlaybackStarted>(Channel.UNLIMITED)
            val startedJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.playbackStarted.collect { started.send(it) }
                }
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            assertEquals(
                "a", withTimeout(5_000) { started.receive() }.occurrenceId)
            val outgoing = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            val rejectedIncoming = engine.current()

            // A native focus/route pause reports the engine's incoming token
            // before
            // the controller has accepted Playing for that occurrence.
            engine.pause()
            engine.awaitEvent("cancel-prepared")
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(
                outgoing.generation, controller.state.value.engineGeneration)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(listOf("a"), engine.voices().map { it.track.id })
            assertFalse(engine.current().playing)
            controller.onPlaybackStatus(
                rejectedIncoming.generation, PlaybackStatus.Playing)
            engine.complete(rejectedIncoming)
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertNull(started.tryReceive().getOrNull())

            controller.setCrossfadeDuration(CrossfadeDuration.Off)
            controller.play()
            engine.awaitEvent("play")
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertTrue(engine.current().playing)
            engine.complete(outgoing)
            assertEquals("a", controller.state.value.currentOccurrenceId)
            engine.complete(engine.current())
            engine.awaitEvent("load:b")
            engine.awaitEvent("play")
            assertEquals(
                "b", withTimeout(5_000) { started.receive() }.occurrenceId)
            assertNull(started.tryReceive().getOrNull())
            controller.release()
            startedJob.cancelAndJoin()
        }

    @Test
    fun outgoingPauseDuringAsyncIncomingStartRollsBackAndResumeCanCompleteOnce() =
        runBlocking {
            val engine =
                TwoVoiceEngine(
                    confirmIncoming = false, deferIncomingStart = true)
            val controller = controller(engine)
            val started = Channel<PlaybackStarted>(Channel.UNLIMITED)
            val startedJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.playbackStarted.collect { started.send(it) }
                }
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            assertEquals(
                "a", withTimeout(5_000) { started.receive() }.occurrenceId)
            val outgoing = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            val canceledIncoming = engine.pendingIncoming()
            engine.pause()
            engine.awaitEvents("cancel-prepared", "pause")
            assertEquals(PlaybackStatus.Paused, controller.state.value.status)
            assertEquals(outgoing.generation, engine.current().generation)
            assertFalse(engine.current().playing)

            controller.play()
            engine.awaitEvent("play")
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertTrue(engine.current().run != outgoing.run)
            engine.awaitEvent("incoming-play")
            controller.onPlaybackStatus(
                canceledIncoming.generation, PlaybackStatus.Playing)
            engine.complete(outgoing)
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertNull(started.tryReceive().getOrNull())
            val resumedOutgoing = engine.current()
            engine.complete(resumedOutgoing)
            engine.confirmIncoming()
            awaitState { controller.state.value.currentOccurrenceId == "b" }
            engine.awaitEvent("retire")
            assertEquals(
                "b", withTimeout(5_000) { started.receive() }.occurrenceId)
            engine.complete(resumedOutgoing)
            engine.confirmIncoming()
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertNull(started.tryReceive().getOrNull())
            engine.complete(engine.current())
            engine.awaitEvent("load:c")
            engine.awaitEvent("play")
            assertEquals(
                "c", withTimeout(5_000) { started.receive() }.occurrenceId)
            assertNull(started.tryReceive().getOrNull())
            controller.release()
            startedJob.cancelAndJoin()
        }

    @Test
    fun cancelingUnconfirmedCompletedOutgoingSettlesPolicyWithoutConsumingNewTimer() =
        runBlocking {
            for (change in
                listOf(
                    "off", "repeat", "stop-after", "timer", "timed", "error")) {
                val engine = TwoVoiceEngine(confirmIncoming = false)
                val controller = controller(engine)
                val started = Channel<PlaybackStarted>(Channel.UNLIMITED)
                val startedJob =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        controller.playbackStarted.collect { started.send(it) }
                    }
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                controller.setOccurrenceQueue(queue(), autoPlay = true)
                engine.awaitEvent("play")
                assertEquals(
                    "a", withTimeout(5_000) { started.receive() }.occurrenceId)
                val outgoing = engine.current()
                engine.progress(17_000L)
                engine.awaitEvent("incoming-play")
                val rejectedIncoming = engine.current()
                engine.complete(outgoing)
                assertEquals("a", controller.state.value.currentOccurrenceId)
                when (change) {
                    "off" ->
                        controller.setCrossfadeDuration(CrossfadeDuration.Off)
                    "repeat" -> controller.setRepeatMode(RepeatMode.RepeatOne)
                    "stop-after" ->
                        controller.setRepeatMode(RepeatMode.StopAfterCurrent)
                    "timer" ->
                        controller.armSleepTimerAfterCompletions(
                            1, fadeEnabled = false)
                    "error" -> engine.failCurrent()
                    else -> controller.armSleepTimer(1, fadeEnabled = false)
                }
                engine.awaitEvent("cancel-prepared")
                if (change == "stop-after") {
                    awaitState {
                        controller.state.value.status == PlaybackStatus.Stopped
                    }
                    assertEquals(
                        "a", controller.state.value.currentOccurrenceId)
                    assertFalse(engine.current().playing)
                } else {
                    val expected = if (change == "repeat") "a" else "b"
                    engine.awaitEvent("load:$expected")
                    engine.awaitEvent("play")
                    assertEquals(
                        expected, controller.state.value.currentOccurrenceId)
                    assertEquals(
                        expected,
                        withTimeout(5_000) { started.receive() }.occurrenceId)
                }
                engine.complete(outgoing)
                controller.onPlaybackStatus(
                    rejectedIncoming.generation, PlaybackStatus.Playing)
                engine.complete(rejectedIncoming)
                assertNull(started.tryReceive().getOrNull())
                assertEquals(1, engine.voices().size)
                if (change == "timer") {
                    assertEquals(
                        1, controller.sleepTimerState.value.remainingTracks)
                    engine.complete(engine.current())
                    engine.awaitEvent("stop")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        PlaybackStatus.Stopped, controller.state.value.status)
                } else if (change == "timed") {
                    assertEquals(
                        SleepTimerMode.Timed,
                        controller.sleepTimerState.value.mode)
                    assertEquals(
                        60_000L,
                        controller.sleepTimerState.value.remainingMillis)
                }
                controller.release()
                startedJob.cancelAndJoin()
            }
        }

    @Test
    fun deferredCompletionCannotAdvanceAReplacementOrAnInvalidatedOutgoingRun() =
        runBlocking {
            for (replacement in listOf(false, true)) {
                val engine = TwoVoiceEngine(confirmIncoming = false)
                val controller = controller(engine)
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                controller.setOccurrenceQueue(queue(), autoPlay = true)
                engine.awaitEvent("play")
                val outgoing = engine.current()
                engine.afterIncomingStarted = {
                    // These mutations happen before startIncoming returns,
                    // while
                    // engineMutex still prevents queued rollback/settlement.
                    engine.complete(outgoing)
                    controller.setCrossfadeDuration(CrossfadeDuration.Off)
                    if (replacement)
                        controller.selectOccurrence("c", autoPlay = true)
                    else controller.stop()
                }
                engine.progress(17_000L)
                engine.awaitEvent("incoming-play")
                if (replacement) {
                    engine.awaitEvent("load:c")
                    engine.awaitEvent("play")
                    assertEquals(
                        "c", controller.state.value.currentOccurrenceId)
                    assertEquals("c", engine.current().track.id)
                } else {
                    engine.awaitEvent("stop")
                    assertEquals(
                        "a", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        PlaybackStatus.Stopped, controller.state.value.status)
                    controller.play()
                    engine.awaitEvent("play")
                    assertTrue(engine.current().run != outgoing.run)
                    engine.complete(outgoing)
                    assertEquals(
                        "a", controller.state.value.currentOccurrenceId)
                    engine.complete(engine.current())
                    engine.awaitEvent("load:b")
                    engine.awaitEvent("play")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                }
                engine.complete(outgoing)
                assertEquals(
                    if (replacement) "c" else "b",
                    controller.state.value.currentOccurrenceId)
                controller.release()
            }
        }

    @Test
    fun duplicateDeferredCompletionCannotConsumeTimerBeforeRollbackSettles() =
        runBlocking {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            val outgoing = engine.current()
            engine.afterIncomingStarted = {
                engine.complete(outgoing)
                controller.armSleepTimerAfterCompletions(1, fadeEnabled = false)
                // Rollback is still blocked by this native start's engineMutex.
                // A duplicate must retain the first receipt's pre-timer
                // ownership.
                engine.complete(outgoing)
            }
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            engine.awaitEvent("cancel-prepared")
            engine.awaitEvent("load:b")
            engine.awaitEvent("play")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            engine.complete(outgoing)
            assertEquals(1, controller.sleepTimerState.value.remainingTracks)
            engine.complete(engine.current())
            engine.awaitEvent("stop")
            assertEquals("b", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
            controller.release()
        }

    @Test
    fun externalPendingIncomingTransportUsesOutgoingAuthority() = runBlocking {
        for (command in listOf("pause", "stop", "seek", "next", "previous")) {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(
                queue(), selectedOccurrenceId = "b", autoPlay = true)
            engine.awaitEvent("play")
            val outgoing = engine.current()
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            val pending = engine.current()
            requestTransport(controller, pending.generation, command)
            engine.awaitEvent("cancel-prepared")
            when (command) {
                "pause" -> {
                    engine.awaitEvent("pause")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        PlaybackStatus.Paused, controller.state.value.status)
                    assertFalse(engine.current().playing)
                }
                "stop" -> {
                    engine.awaitEvent("stop")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(
                        PlaybackStatus.Stopped, controller.state.value.status)
                    assertFalse(engine.current().playing)
                }
                "seek" -> {
                    engine.awaitEvent("seek")
                    assertEquals(
                        "b", controller.state.value.currentOccurrenceId)
                    assertEquals(4_000L, controller.state.value.positionMillis)
                    assertEquals(4_000L, engine.current().position)
                }
                else -> {
                    val expected = if (command == "previous") "a" else "c"
                    engine.awaitEvent("load:$expected")
                    engine.awaitEvent("play")
                    assertEquals(
                        expected, controller.state.value.currentOccurrenceId)
                }
            }
            if (command in listOf("pause", "stop", "seek")) {
                assertEquals(
                    outgoing.generation,
                    controller.state.value.engineGeneration)
                assertEquals(outgoing.generation, engine.current().generation)
            }
            assertEquals(1, engine.voices().size)
            controller.release()
        }
    }

    @Test
    fun externalTransportRejectsCanceledIncomingAndDisabledOwnedTokens() =
        runBlocking {
            val engine = TwoVoiceEngine(confirmIncoming = false)
            val controller = controller(engine)
            val checkpoints = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
            val checkpointJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    controller.checkpoints.collect { checkpoints.send(it) }
                }
            controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            controller.setOccurrenceQueue(queue(), autoPlay = true)
            engine.awaitEvent("play")
            engine.progress(17_000L)
            engine.awaitEvent("incoming-play")
            val pending = engine.current()
            controller.setCommandsEnabled(false)
            controller.awaitCheckpointFence()
            drain(checkpoints)
            for (command in
                listOf("pause", "stop", "seek", "next", "previous")) {
                requestTransport(controller, pending.generation, command)
            }
            controller.awaitCheckpointFence()
            assertNull(checkpoints.tryReceive().getOrNull())
            assertEquals("a", controller.state.value.currentOccurrenceId)
            assertEquals(PlaybackStatus.Playing, controller.state.value.status)
            assertEquals(2, engine.voices().size)

            controller.setCommandsEnabled(true)
            controller.selectOccurrence("c", autoPlay = true)
            engine.awaitEvent("load:c")
            engine.awaitEvent("play")
            controller.awaitCheckpointFence()
            drain(checkpoints)
            val winner = controller.state.value
            for (command in
                listOf("pause", "stop", "seek", "next", "previous")) {
                requestTransport(controller, pending.generation, command)
            }
            controller.awaitCheckpointFence()
            assertNull(checkpoints.tryReceive().getOrNull())
            assertEquals(winner, controller.state.value)
            assertEquals(listOf("c"), engine.voices().map { it.track.id })
            assertTrue(engine.current().playing)
            controller.release()
            checkpointJob.cancelAndJoin()
        }

    @Test
    fun ineligiblePolicyAndSingleVoiceCompatibilityNeverStage() = runBlocking {
        for (reason in
            listOf("off", "one", "unknown", "repeat", "stop-after", "timer")) {
            val engine = TwoVoiceEngine()
            val controller = controller(engine)
            if (reason != "off")
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
            val queue =
                when (reason) {
                    "one" -> queue().take(1)
                    "unknown" ->
                        queue().map {
                            it.copy(
                                track = it.track.copy(durationMillis = null))
                        }
                    else -> queue()
                }
            controller.setOccurrenceQueue(queue, autoPlay = true)
            engine.awaitEvent("play")
            when (reason) {
                "repeat" -> controller.setRepeatMode(RepeatMode.RepeatOne)
                "stop-after" ->
                    controller.setRepeatMode(RepeatMode.StopAfterCurrent)
                "timer" ->
                    controller.armSleepTimerAfterCompletions(
                        1, fadeEnabled = false)
            }
            engine.progress(19_000L, if (reason == "unknown") null else 20_000L)
            controller.pause()
            engine.awaitEvent("pause")
            assertEquals(0, engine.prepareCount)
            controller.release()
        }
        val singleVoice = FakePlaybackEngine()
        val controller = PlaybackController(singleVoice)
        controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
        controller.setOccurrenceQueue(queue(), autoPlay = true)
        awaitState { controller.state.value.status == PlaybackStatus.Playing }
        controller.onPlaybackProgress(
            controller.state.value.engineGeneration, 19_000L, 20_000L)
        assertEquals("a", controller.state.value.currentOccurrenceId)
        controller.release()
    }

    private fun controller(
        engine: PlatformPlaybackEngine,
        clock: Clock = Clock()
    ): PlaybackController =
        PlaybackController(engine, { ids, _ -> ids }, { null }, clock)

    private fun requestTransport(
        controller: PlaybackController,
        generation: Long,
        command: String
    ) {
        when (command) {
            "pause" -> controller.onPauseRequested(generation)
            "stop" -> controller.onStopRequested(generation)
            "seek" -> controller.onSeekRequested(generation, 4_000L)
            "next" -> controller.onSkipToNext(generation)
            "previous" -> controller.onSkipToPrevious(generation)
            else -> error("Unknown transport command: $command")
        }
    }

    private fun track(
        id: String,
        duration: Long? = 20_000L,
        peak: Double = 1.0
    ): PlayableTrack =
        PlayableTrack(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            durationMillis = duration,
            source = AudioSource.FilePath("/tmp/$id.wav"),
            replayGainTrackGainDb = 0.0,
            replayGainTrackPeak = peak,
        )

    private fun queue(
        outgoingPeak: Double = 1.0,
        incomingPeak: Double = 1.0,
        incomingDuration: Long = 20_000L
    ): List<QueueOccurrence> =
        listOf(
            QueueOccurrence("a", track("a", peak = outgoingPeak)),
            QueueOccurrence("b", track("b", incomingDuration, incomingPeak)),
            QueueOccurrence("c", track("c")))

    private fun <T> drain(channel: Channel<T>) {
        while (channel.tryReceive().isSuccess) Unit
    }

    private suspend fun awaitState(predicate: () -> Boolean) {
        withTimeout(5_000) { while (!predicate()) yield() }
    }

    private class Clock : SleepTimerRuntime {
        private val gate = OwnershipLock()
        private var time = 0L
        private val wakeups = mutableListOf<CompletableDeferred<Unit>>()

        override fun nowMillis(): Long = gate.withLock { time }

        override suspend fun delay(millis: Long) {
            val wakeup = CompletableDeferred<Unit>()
            gate.withLock { wakeups.add(wakeup) }
            try {
                wakeup.await()
            } finally {
                gate.withLock { wakeups.remove(wakeup) }
            }
        }

        fun advanceBy(millis: Long) {
            val pending = gate.withLock {
                time += millis
                wakeups.toList()
            }
            pending.forEach { it.complete(Unit) }
        }
    }

    private data class VoiceSnapshot(
        val track: PlayableTrack,
        val generation: Long,
        val run: Long,
        val position: Long,
        val gain: Float,
        val playing: Boolean,
    )

    private class TwoVoiceEngine(
        private val prepareGate: CompletableDeferred<Unit>? = null,
        private val prepareFailure: Boolean = false,
        private val confirmIncoming: Boolean = true,
        private val incomingStartPosition: Long = 0L,
        private val deferIncomingStart: Boolean = false,
        private val failGaplessStart: Boolean = false,
    ) : CrossfadePlaybackEngine {
        private val gate = OwnershipLock()
        private var currentVoice: VoiceSnapshot? = null
        private var retiring: VoiceSnapshot? = null
        private var prepared: VoiceSnapshot? = null
        private var released = false
        private val events = Channel<String>(Channel.UNLIMITED)
        private val pendingEvents = mutableListOf<String>()
        private var preparationJob: Job? = null
        private val gainEvents = Channel<Pair<Float, Float>>(Channel.UNLIMITED)
        private var starts = 0
        private var prepares = 0
        var lastPlayedGains: List<Float> = emptyList()
            private set

        var afterIncomingStarted: (() -> Unit)? = null
        val incomingPlayCount: Int
            get() = gate.withLock { starts }

        val prepareCount: Int
            get() = gate.withLock { prepares }

        override var listener: PlaybackEngineListener? = null

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            gate.withLock {
                prepared = null
                retiring = null
                currentVoice =
                    VoiceSnapshot(track, generation, 0L, 0L, 1f, false)
            }
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            emit("load:${track.id}")
            return LoadedPlayback(generation, track.durationMillis)
        }

        override suspend fun prepareIncoming(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            preparationJob = currentCoroutineContext()[Job]
            gate.withLock { prepares++ }
            emit("prepare:${track.id}")
            prepareGate?.let { withContext(NonCancellable) { it.await() } }
            if (gate.withLock { released }) {
                emit("released-prepare")
                return LoadedPlayback(generation, track.durationMillis)
            }
            if (prepareFailure) {
                emit("prepare-failed")
                throw PlaybackFailureException(
                    PlaybackError("Incoming preparation failed"))
            }
            gate.withLock {
                prepared =
                    VoiceSnapshot(
                        track, generation, 0L, incomingStartPosition, 0f, false)
            }
            emit("prepared")
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun startIncoming(playbackRunId: Long) {
            val incoming = gate.withLock {
                val staged = checkNotNull(prepared)
                if (deferIncomingStart) {
                    prepared = staged.copy(run = playbackRunId)
                } else {
                    retiring = currentVoice
                    currentVoice =
                        staged.copy(run = playbackRunId, playing = true)
                    prepared = null
                }
                starts++
                if (deferIncomingStart) checkNotNull(prepared)
                else checkNotNull(currentVoice)
            }
            if (!deferIncomingStart)
                listener?.onPlaybackProgress(
                    incoming.generation,
                    incoming.position,
                    incoming.track.durationMillis)
            if (confirmIncoming) confirmIncoming()
            afterIncomingStarted?.invoke()
            emit("incoming-play")
        }

        override fun startIncomingGapless(playbackRunId: Long) {
            if (failGaplessStart)
                throw PlaybackFailureException(
                    PlaybackError("Incoming start failed"))
            startIncoming(playbackRunId)
        }

        override fun setCrossfadeGains(
            outgoingGain: Float,
            incomingGain: Float
        ) {
            require(outgoingGain.isFinite() && outgoingGain in 0f..1f)
            require(incomingGain.isFinite() && incomingGain in 0f..1f)
            gate.withLock {
                if (retiring != null) {
                    retiring = retiring?.copy(gain = outgoingGain)
                    currentVoice = currentVoice?.copy(gain = incomingGain)
                } else {
                    currentVoice = currentVoice?.copy(gain = outgoingGain)
                    prepared = prepared?.copy(gain = incomingGain)
                }
            }
            gainEvents.trySend(outgoingGain to incomingGain)
        }

        override fun finishCrossfade() {
            gate.withLock { retiring = null }
            emit("retire")
        }

        override fun finishGapless() = finishCrossfade()

        override fun cancelPreparedIncoming() {
            gate.withLock {
                prepared = null
                if (retiring != null) {
                    currentVoice = retiring
                    retiring = null
                }
            }
            emit("cancel-prepared")
        }

        override fun clear(generation: Long) {
            gate.withLock {
                currentVoice = null
                retiring = null
                prepared = null
            }
            emit("clear")
        }

        override fun setUserTransportEnabled(enabled: Boolean) = Unit

        override fun setPlaybackGain(gain: Float) {
            require(gain.isFinite() && gain in 0f..1f)
            gate.withLock { currentVoice = currentVoice?.copy(gain = gain) }
        }

        override fun play(playbackRunId: Long) {
            val current = gate.withLock {
                lastPlayedGains =
                    listOfNotNull(retiring, currentVoice).map { it.gain }
                currentVoice =
                    checkNotNull(currentVoice)
                        .copy(run = playbackRunId, playing = true)
                retiring = retiring?.copy(playing = true)
                checkNotNull(currentVoice)
            }
            listener?.onPlaybackStatus(
                current.generation, PlaybackStatus.Playing)
            emit("play")
        }

        override fun pause() {
            val generation = gate.withLock {
                currentVoice = currentVoice?.copy(playing = false)
                retiring = retiring?.copy(playing = false)
                currentVoice?.generation
            }
            generation?.let {
                listener?.onPlaybackStatus(it, PlaybackStatus.Paused)
            }
            emit("pause")
        }

        override fun stop() {
            val generation = gate.withLock {
                retiring = null
                prepared = null
                currentVoice =
                    currentVoice?.copy(playing = false, position = 0L)
                currentVoice?.generation
            }
            generation?.let {
                listener?.onPlaybackStatus(it, PlaybackStatus.Stopped)
            }
            emit("stop")
        }

        override fun seekTo(positionMillis: Long) {
            val current = gate.withLock {
                retiring = null
                currentVoice = currentVoice?.copy(position = positionMillis)
                currentVoice
            }
            current?.let {
                listener?.onPlaybackProgress(
                    it.generation, it.position, it.track.durationMillis)
            }
            emit("seek")
        }

        override fun release() {
            gate.withLock {
                released = true
                currentVoice = null
                prepared = null
                retiring = null
            }
        }

        fun voices(): List<VoiceSnapshot> = gate.withLock {
            listOfNotNull(retiring, currentVoice)
        }

        fun current(): VoiceSnapshot = gate.withLock {
            checkNotNull(currentVoice)
        }

        fun pendingIncoming(): VoiceSnapshot = gate.withLock {
            checkNotNull(prepared)
        }

        fun pendingIncomingOrNull(): VoiceSnapshot? = gate.withLock { prepared }

        fun progress(
            position: Long,
            duration: Long? = current().track.durationMillis
        ) {
            val current = gate.withLock {
                currentVoice =
                    checkNotNull(currentVoice).copy(position = position)
                checkNotNull(currentVoice)
            }
            listener?.onPlaybackProgress(current.generation, position, duration)
        }

        fun confirmIncoming() {
            val incoming = gate.withLock {
                prepared
                    ?.takeIf { it.run != 0L }
                    ?.let {
                        retiring = currentVoice
                        currentVoice = it.copy(playing = true)
                        prepared = null
                    }
                checkNotNull(currentVoice)
            }
            listener?.onPlaybackProgress(
                incoming.generation,
                incoming.position,
                incoming.track.durationMillis)
            listener?.onPlaybackStatus(
                incoming.generation, PlaybackStatus.Playing)
        }

        fun complete(voice: VoiceSnapshot) {
            gate.withLock {
                if (currentVoice?.generation == voice.generation &&
                    currentVoice?.run == voice.run) {
                    currentVoice = currentVoice?.copy(playing = false)
                }
                if (retiring?.generation == voice.generation &&
                    retiring?.run == voice.run) {
                    retiring = retiring?.copy(playing = false)
                }
            }
            listener?.onPlaybackCompleted(voice.generation, voice.run)
        }

        fun failCurrent() {
            listener?.onPlaybackError(
                current().generation, PlaybackError("Incoming failed"))
        }

        suspend fun awaitEvent(expected: String) {
            withTimeout(5_000) {
                val buffered = pendingEvents.indexOf(expected)
                if (buffered >= 0) {
                    pendingEvents.removeAt(buffered)
                    return@withTimeout
                }
                while (true) {
                    val event = events.receive()
                    if (event == expected) return@withTimeout
                    pendingEvents.add(event)
                }
            }
        }

        suspend fun awaitEvents(vararg expected: String) {
            withTimeout(5_000) {
                val remaining = expected.toMutableSet()
                val buffered = pendingEvents.toList()
                pendingEvents.clear()
                buffered.forEach(remaining::remove)
                while (remaining.isNotEmpty()) {
                    val event = events.receive()
                    if (!remaining.remove(event)) pendingEvents.add(event)
                }
            }
        }

        suspend fun awaitGains(outgoing: Float, incoming: Float) {
            withTimeout(5_000) {
                while (gainEvents.receive() != (outgoing to incoming)) Unit
            }
        }

        suspend fun awaitPreparationSettled() {
            val job = gate.withLock { preparationJob }
            job?.join()
        }

        private fun emit(event: String) {
            check(events.trySend(event).isSuccess)
        }
    }
}
