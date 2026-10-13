@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter

class IOSAudioPlayerBridgeTest {

    @Test
    fun swiftAudioPlayerProviderRetainsCompletionHandlerAndForwardsNativeCompletion() {
        val provider = FakeIOSAudioPlayerProvider()
        val events = mutableListOf<String>()
        val handler =
            object : IOSAudioPlayerCompletionHandler {
                override fun onPlaybackCompleted() {
                    events += "completed"
                }

                override fun onPlaybackFailed() {
                    events += "failed"
                }
            }

        provider.completionHandler = handler
        provider.simulateNativeCompletion()

        assertEquals(listOf("completed"), events)
        assertSame(handler, provider.completionHandler)
    }

    @Test
    fun swiftAudioPlayerProviderRetainsInterruptionHandlerAndForwardsNativeEvents() {
        val provider = FakeIOSAudioPlayerProvider()
        val events = mutableListOf<String>()
        val handler =
            object : IOSAudioInterruptionHandler {
                override fun onInterruptionBegan() {
                    events += "began"
                }

                override fun onInterruptionEnded(shouldResume: Boolean) {
                    events += "ended:$shouldResume"
                }

                override fun onRouteChanged() {
                    events += "changed"
                }

                override fun onRouteDisconnected() {
                    events += "route"
                }
            }

        provider.interruptionHandler = handler
        provider.simulateInterruptionBegan()
        provider.simulateInterruptionEnded(shouldResume = true)
        provider.simulateRouteDisconnected()

        assertEquals(listOf("began", "ended:true", "route"), events)
        assertSame(handler, provider.interruptionHandler)
    }

    @Test
    fun interruptionBeganWhilePlayingEmitsPaused() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 1L)

        provider.simulateInterruptionBegan()

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
    }

    @Test
    fun interruptionEndedWithShouldResumeAutoResumesWhenPlaying() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 2L)

        provider.simulateInterruptionBegan()
        provider.simulateInterruptionEnded(shouldResume = true)

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing),
            session.recording.statuses)
        assertTrue(provider.isPlaying())
    }

    @Test
    fun interruptionEndedWithoutShouldResumeStaysPaused() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 3L)

        provider.simulateInterruptionBegan()
        provider.simulateInterruptionEnded(shouldResume = false)

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
    }

    @Test
    fun interruptionEndedResumesOnlyAfterSuccessfulProviderPlay() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 4L)

        provider.simulateInterruptionBegan()
        provider.playSucceeds = false
        provider.simulateInterruptionEnded(shouldResume = true)

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
    }

    @Test
    fun playbackStatusWaitsForAsynchronousNativeActivation() {
        val provider = FakeIOSAudioPlayerProvider()
        provider.deferPlayStart = true
        val session = loadAndPlay(provider, generation = 40L)

        assertEquals(
            listOf(PlaybackStatus.Loading, PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())

        provider.completeDeferredPlayStart()

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
            ),
            session.recording.statuses)
        assertTrue(provider.isPlaying())
    }

    @Test
    fun loadingWaitsForAsynchronousNativePreparation() = runBlocking {
        val provider = FakeIOSAudioPlayerProvider()
        provider.deferLoad = true
        IOSAudioPlayerBridge.provider = provider
        val engine = createIOSPlaybackEngine(testResolver())
        val recording = RecordingListener()
        engine.listener = recording
        val load = launch {
            engine.loadPaused(testTrack("deferred-load"), generation = 53L)
        }

        kotlinx.coroutines.yield()
        assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
        assertFalse(provider.isLoaded)

        provider.completeDeferredLoad()
        load.join()

        assertEquals(
            listOf(PlaybackStatus.Loading, PlaybackStatus.Paused),
            recording.statuses)
        assertTrue(provider.isLoaded)
    }

    @Test
    fun staleAsynchronousLoadCannotPublishAfterClear() = runBlocking {
        val provider = FakeIOSAudioPlayerProvider()
        provider.deferLoad = true
        IOSAudioPlayerBridge.provider = provider
        val engine = createIOSPlaybackEngine(testResolver())
        val recording = RecordingListener()
        engine.listener = recording
        val load = launch {
            runCatching {
                engine.loadPaused(testTrack("stale-load"), generation = 54L)
            }
        }

        kotlinx.coroutines.yield()
        engine.clear(generation = 55L)
        provider.completeDeferredLoad()
        load.join()

        assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
    }

    @Test
    fun failedInterruptionResumeConsumesEligibility() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 41L)
        val playCallsBeforeInterruption = provider.playCallCount

        provider.simulateInterruptionBegan()
        provider.playSucceeds = false
        provider.simulateInterruptionEnded(shouldResume = true)
        provider.playSucceeds = true
        provider.simulateInterruptionEnded(shouldResume = true)

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertEquals(playCallsBeforeInterruption + 1, provider.playCallCount)
        assertFalse(provider.isPlaying())
    }

    @Test
    fun routeDisconnectPausesWithoutAutoResume() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 5L)

        provider.simulateRouteDisconnected()
        provider.simulateInterruptionEnded(shouldResume = true)

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
    }

    @Test
    fun pausedInterruptionDoesNotEmitSpuriousPause() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPause(provider, generation = 6L)

        provider.simulateInterruptionBegan()

        assertEquals(
            listOf(PlaybackStatus.Loading, PlaybackStatus.Paused),
            session.recording.statuses)
    }

    @Test
    fun staleInterruptionCallbacksDoNotMutateCurrentSource() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val recording = RecordingListener()
        val engine = createIOSPlaybackEngine(testResolver())
        engine.listener = recording

        runBlocking {
            engine.loadPaused(testTrack("first"), generation = 7L)
        }
        val staleHandler = provider.interruptionHandler
        runBlocking {
            engine.loadPaused(testTrack("second"), generation = 8L)
        }
        val statusesBeforeStaleCallback = recording.statuses.toList()

        assertSame(provider, IOSAudioPlayerBridge.provider)
        staleHandler?.onInterruptionBegan()

        assertEquals(statusesBeforeStaleCallback, recording.statuses)
        engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun failedLoadClearsBothHandlersBeforeCallbacksCanReachListener() {
        val provider = FakeIOSAudioPlayerProvider()
        provider.loadSucceeds = false
        IOSAudioPlayerBridge.provider = provider
        val recording = RecordingListener()
        val engine = createIOSPlaybackEngine(testResolver())
        engine.listener = recording

        val failure =
            assertFailsWith<PlaybackFailureException> {
                runBlocking {
                    engine.loadPaused(testTrack("failed"), generation = 42L)
                }
            }
        assertEquals(PlaybackFailureKind.Unknown, failure.error.kind)
        assertEquals(null, provider.completionHandler)
        assertEquals(null, provider.interruptionHandler)
        provider.simulateNativeCompletion()
        provider.simulateInterruptionBegan()
        provider.simulateInterruptionEnded(shouldResume = true)
        provider.simulateRouteDisconnected()
        assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun releaseAndTrackReplacementClearAndReplaceBothHandlers() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val engine = createIOSPlaybackEngine(testResolver())

        runBlocking { engine.loadPaused(testTrack("first"), generation = 43L) }
        engine.play(1L)
        val firstCompletion = provider.completionHandler
        val firstInterruption = provider.interruptionHandler
        runBlocking { engine.loadPaused(testTrack("second"), generation = 44L) }
        engine.play(2L)

        assertTrue(provider.completionHandler !== firstCompletion)
        assertTrue(provider.interruptionHandler !== firstInterruption)
        engine.release()
        assertEquals(null, provider.completionHandler)
        assertEquals(null, provider.interruptionHandler)
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun interruptionDuringTrackSwitchTeardownCannotMutateReplacementSource() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 45L)
        val oldInterruption = provider.interruptionHandler
        provider.onFadeOutAndStop = { oldInterruption?.onInterruptionBegan() }

        runBlocking {
            session.engine.loadPaused(
                testTrack("replacement"), generation = 46L)
        }

        assertEquals(
            listOf(
                PlaybackStatus.Loading,
                PlaybackStatus.Paused,
                PlaybackStatus.Playing,
                PlaybackStatus.Loading,
                PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
        session.engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun callbackDuringLoadCannotBecomeOwnedByTheLoadedSource() {
        val provider = FakeIOSAudioPlayerProvider()
        provider.onLoad = {
            provider.simulateInterruptionEnded(shouldResume = true)
        }
        val session = loadAndPause(provider, generation = 47L)

        assertEquals(
            listOf(PlaybackStatus.Loading, PlaybackStatus.Paused),
            session.recording.statuses)
        assertFalse(provider.isPlaying())
        session.engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun failedPostInstallSetupClearsBothHandlers() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val recording = RecordingListener()
        val engine = createIOSPlaybackEngine(testResolver())
        engine.listener = recording

        runBlocking {
            engine.loadPaused(testTrack("setup-previous"), generation = 47L)
        }
        provider.durationThrows = true

        assertFailsWith<IllegalStateException> {
            runBlocking {
                engine.loadPaused(
                    testTrack("setup-failure").copy(durationMillis = null),
                    generation = 48L,
                )
            }
        }
        assertEquals(null, provider.completionHandler)
        assertEquals(null, provider.interruptionHandler)
        assertEquals(
            null, MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo)
        assertFailsWith<IllegalArgumentException> { engine.play(1L) }
        assertEquals(0, provider.playCallCount)
        engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun failedPathResolutionClearsPreviousNowPlayingInfo() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val engine = createIOSPlaybackEngine(testResolver())

        runBlocking {
            engine.loadPaused(testTrack("path-previous"), generation = 51L)
        }
        assertTrue(
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo != null)

        val failingEngine =
            createIOSPlaybackEngine(
                object : IOSRelativeFilePathResolver {
                    override fun resolve(relativePath: String): String =
                        error("path resolution failed")
                },
            )
        val failure =
            assertFailsWith<PlaybackFailureException> {
                runBlocking {
                    failingEngine.loadPaused(
                        testTrack("path-failure"), generation = 52L)
                }
            }
        assertEquals(PlaybackFailureKind.Unknown, failure.error.kind)
        assertTrue(
            failure.error.message
                .orEmpty()
                .startsWith("Could not resolve player path:"))

        assertEquals(
            null, MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo)
        engine.release()
        failingEngine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun duplicateCompletionIsTerminalOnTheRealEnginePath() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 49L)

        provider.simulateNativeCompletion()
        provider.simulateNativeCompletion()

        assertEquals(1, session.recording.completionCount)
        assertTrue(session.recording.progressCount >= 1)
        assertFalse(provider.isPlaying())
        session.engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun duplicateInterruptionEndIsTerminalOnTheRealEnginePath() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 50L)
        val playCallsBeforeInterruption = provider.playCallCount

        provider.simulateInterruptionBegan()
        provider.simulateInterruptionEnded(shouldResume = true)
        provider.simulateInterruptionEnded(shouldResume = true)

        assertEquals(playCallsBeforeInterruption + 1, provider.playCallCount)
        assertEquals(
            2,
            session.recording.statuses.count { it == PlaybackStatus.Playing })
        session.engine.release()
        IOSAudioPlayerBridge.provider = null
    }

    @Test
    fun iosPlaybackEngineUsesSwiftNativeAudioProvider() {
        assertEquals(
            IOSAudioBackend.SwiftAVAudioPlayerDelegate, iosAudioBackend)
    }

    @Test
    fun playbackGainHeldDuringPendingLoadReachesProviderBeforePlayback() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            provider.deferLoad = true
            IOSAudioPlayerBridge.provider = provider
            val engine = createIOSPlaybackEngine(testResolver())
            try {
                val load = launch {
                    engine.loadPaused(
                        testTrack("pending-gain"), generation = 62L)
                }

                kotlinx.coroutines.yield()
                engine.setPlaybackGain(0.25f)
                provider.completeDeferredLoad()
                load.join()
                engine.play(1L)

                assertEquals(0.25f, provider.gainAtLoadCompletion)
                assertEquals(0.25f, provider.gainAtPlayStart)
                assertEquals(0.25f, provider.playbackGainChanges.last())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun playerReplacementRetainsRequestedPlaybackGainAndReleaseRestoresUnity() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val engine = createIOSPlaybackEngine(testResolver())
        try {
            runBlocking {
                engine.loadPaused(testTrack("first-gain"), generation = 63L)
            }
            engine.setPlaybackGain(0.25f)

            runBlocking {
                engine.loadPaused(
                    testTrack("replacement-gain"), generation = 64L)
            }

            assertEquals(0.25f, provider.gainAtLoadCompletion)
            engine.setPlaybackGain(0.4f)
            engine.release()

            assertEquals(1.0f, provider.playbackGainChanges.last())
        } finally {
            engine.release()
            IOSAudioPlayerBridge.provider = null
        }
    }

    @Test
    fun playbackGainRejectsNonFiniteAndOutOfRangeValues() {
        val engine = createIOSPlaybackEngine(testResolver())
        try {
            assertFailsWith<IllegalArgumentException> {
                engine.setPlaybackGain(Float.NaN)
            }
            assertFailsWith<IllegalArgumentException> {
                engine.setPlaybackGain(-0.01f)
            }
            assertFailsWith<IllegalArgumentException> {
                engine.setPlaybackGain(1.01f)
            }
        } finally {
            engine.release()
        }
    }

    @Test
    fun preparingIncomingKeepsOutgoingCurrentUntilConfirmedNativeStart() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 70L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                val statusesBefore = session.recording.statuses.toList()
                val loaded =
                    engine.prepareIncoming(
                        testTrack("incoming"), generation = 71L)

                assertEquals(LoadedPlayback(71L, 1_000L), loaded)
                assertEquals(statusesBefore, session.recording.statuses)
                assertTrue(provider.isPlaying())
                assertEquals(0f, provider.incomingPlaybackGain)
                provider.deferIncomingStart = true
                engine.setCrossfadeGains(0.4f, 0f)
                engine.startIncoming(playbackRunId = 2L)
                assertEquals(statusesBefore, session.recording.statuses)
                provider.completeDeferredIncomingStart()

                assertEquals(71L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertTrue(provider.retiringPlaying)
                assertTrue(provider.isPlaying())
                assertEquals(0f, provider.gainAtPlayStart)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun gaplessPromotionRetiresOutgoingAndIgnoresItsLateDelegate() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 74L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(testTrack("gapless"), generation = 75L)
                val outgoingCompletion = provider.completionHandler
                engine.startIncomingGapless(playbackRunId = 3L)

                assertFalse(provider.retiringPlaying)
                assertTrue(provider.isPlaying())
                assertEquals(75L, session.recording.statusGenerations.last())
                outgoingCompletion?.onPlaybackCompleted()
                assertEquals(emptyList(), session.recording.completions)
                engine.finishGapless()
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun retiringCompletionKeepsItsGenerationAndRunWithoutStoppingIncoming() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 72L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("completion-incoming"), generation = 73L)
                engine.startIncoming(playbackRunId = 2L)
                val statusesBefore = session.recording.statuses.toList()
                provider.simulateRetiringCompletion()
                provider.simulateRetiringCompletion()

                assertEquals(listOf(72L to 1L), session.recording.completions)
                assertEquals(statusesBefore, session.recording.statuses)
                assertTrue(provider.isPlaying())
                engine.finishCrossfade()
                provider.simulateRetiringCompletion()
                provider.simulateNativeCompletion()
                assertEquals(
                    listOf(72L to 1L, 73L to 2L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun outgoingCompletionDuringIncomingActivationStillPublishesIncomingPlaying() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 100L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("activation-after-completion"), generation = 101L)
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)

                provider.simulateNativeCompletion()
                provider.completeDeferredIncomingStart()

                assertEquals(listOf(100L to 1L), session.recording.completions)
                assertEquals(101L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertTrue(session.recording.errors.isEmpty())
                assertTrue(
                    engine.refreshLoadedMetadata(
                        testTrack("activation-after-completion"),
                        generation = 101L))
                engine.finishCrossfade()
                provider.simulateNativeCompletion()
                assertEquals(
                    listOf(100L to 1L, 101L to 2L),
                    session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun outgoingCompletionDuringIncomingActivationStillPublishesIncomingStartFailure() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 102L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("failed-after-completion"), generation = 103L)
                provider.deferIncomingStart = true
                provider.incomingPlaySucceeds = false
                engine.startIncoming(playbackRunId = 2L)

                provider.simulateNativeCompletion()
                provider.completeDeferredIncomingStart()

                assertEquals(listOf(102L to 1L), session.recording.completions)
                assertEquals(
                    listOf(103L), session.recording.errors.map { it.first })
                assertEquals(102L, session.recording.statusGenerations.last())
                assertFalse(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                assertTrue(
                    engine.refreshLoadedMetadata(
                        testTrack("track-102"), generation = 102L))
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun outgoingFailureDuringIncomingActivationDoesNotDropIncomingStartOutcome() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 104L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("activation-after-failure"), generation = 105L)
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)

                provider.simulateNativeFailure()
                provider.completeDeferredIncomingStart()

                assertEquals(
                    listOf(104L), session.recording.errors.map { it.first })
                assertEquals(105L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertTrue(session.recording.completions.isEmpty())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun pendingIncomingPauseKeepsNativeFailureOwnedUntilControllerSettlement() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 106L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("paused-activation"), generation = 107L)
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)

                engine.pause()
                provider.completeDeferredIncomingStart()

                assertEquals(106L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Paused, session.recording.statuses.last())
                assertEquals(
                    listOf(107L), session.recording.errors.map { it.first })
                assertFalse(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                engine.play(playbackRunId = 3L)
                provider.simulateNativeCompletion()
                assertEquals(listOf(106L to 3L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun pendingSystemPauseKeepsNativeFailureOwnedUntilControllerSettlement() =
        runBlocking {
            for (routeLoss in listOf(false, true)) {
                val provider = FakeIOSAudioPlayerProvider()
                val session = loadAndPlay(provider, generation = 108L)
                val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
                try {
                    engine.prepareIncoming(
                        testTrack("system-paused-activation"),
                        generation = 109L)
                    provider.deferIncomingStart = true
                    engine.startIncoming(playbackRunId = 2L)

                    if (routeLoss) provider.simulateRouteDisconnected()
                    else provider.simulateInterruptionBegan()
                    provider.completeDeferredIncomingStart()

                    assertEquals(
                        108L, session.recording.statusGenerations.last())
                    assertEquals(
                        PlaybackStatus.Paused,
                        session.recording.statuses.last())
                    assertEquals(
                        listOf(109L), session.recording.errors.map { it.first })
                    assertFalse(provider.isPlaying())
                    assertFalse(provider.retiringPlaying)
                    assertFalse(provider.incomingLoaded)
                } finally {
                    engine.release()
                    IOSAudioPlayerBridge.provider = null
                }
            }
        }

    @Test
    fun pendingPauseControllerCancellationAllowsOutgoingResumeAndRejectsStaleIncomingResult() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 110L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.setPlaybackGain(0.4f)
                engine.prepareIncoming(
                    testTrack("controller-paused-activation"),
                    generation = 111L)
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)
                session.recording.onStatus = { generation, status ->
                    if (generation == 110L && status == PlaybackStatus.Paused)
                        engine.cancelPreparedIncoming()
                }

                engine.pause()
                engine.play(playbackRunId = 3L)
                provider.completeDeferredIncomingStart()

                assertEquals(110L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertEquals(0.4f, provider.gainAtPlayStart)
                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                assertTrue(session.recording.errors.isEmpty())
                provider.simulateNativeCompletion()
                assertEquals(listOf(110L to 3L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun pauseAndSystemInterruptionPreserveBothGainsBeforeResuming() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 74L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("paused-incoming"), generation = 75L)
                engine.startIncoming(playbackRunId = 2L)
                engine.setCrossfadeGains(0.2f, 0.3f)
                engine.pause()
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.isPlaying())
                engine.play(playbackRunId = 3L)
                assertEquals(0.2f, provider.retiringGainAtPlayStart)
                assertEquals(0.3f, provider.gainAtPlayStart)
                provider.simulateInterruptionBegan()
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.isPlaying())
                assertEquals(75L, session.recording.statusGenerations.last())
                engine.play(playbackRunId = 4L)
                assertEquals(0.2f, provider.retiringGainAtPlayStart)
                assertEquals(0.3f, provider.gainAtPlayStart)
                provider.simulateRouteDisconnected()
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.isPlaying())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun preparationFailureIsSilentAndStartFailureKeepsOutgoingCurrent() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 76L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                provider.incomingLoadSucceeds = false
                assertFailsWith<PlaybackFailureException> {
                    engine.prepareIncoming(
                        testTrack("failed-incoming"), generation = 77L)
                }
                assertTrue(provider.isPlaying())
                assertTrue(session.recording.errors.isEmpty())
                provider.incomingLoadSucceeds = true
                engine.prepareIncoming(
                    testTrack("failed-start"), generation = 78L)
                provider.incomingPlaySucceeds = false
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)
                assertTrue(provider.isPlaying())
                assertTrue(session.recording.errors.isEmpty())
                provider.completeDeferredIncomingStart()
                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertEquals(76L, session.recording.statusGenerations.last())
                assertEquals(
                    listOf(78L), session.recording.errors.map { it.first })
                provider.simulateNativeCompletion()
                assertEquals(listOf(76L to 1L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun stalePreparedLoadAfterReplacementCannotCancelTheNewWinner() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 79L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                provider.deferIncomingLoad = true
                var staleFailure: Throwable? = null
                val prepare = launch {
                    try {
                        engine.prepareIncoming(
                            testTrack("late-incoming"), generation = 80L)
                    } catch (failure: Throwable) {
                        staleFailure = failure
                    }
                }
                kotlinx.coroutines.yield()
                engine.loadPaused(testTrack("winner"), generation = 81L)
                engine.play(playbackRunId = 3L)
                provider.completeDeferredIncomingLoad()
                prepare.join()

                assertIs<kotlinx.coroutines.CancellationException>(staleFailure)
                assertTrue(provider.isPlaying())
                assertEquals(81L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertTrue(session.recording.errors.isEmpty())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun staleIncomingStartAfterClearCannotPublishOrComplete() = runBlocking {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 82L)
        val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
        try {
            engine.prepareIncoming(testTrack("pending-start"), generation = 83L)
            provider.deferIncomingStart = true
            engine.startIncoming(playbackRunId = 2L)
            engine.clear(generation = 84L)
            val statusesBefore = session.recording.statuses.toList()
            provider.completeDeferredIncomingStart()
            provider.simulateNativeCompletion()

            assertEquals(statusesBefore, session.recording.statuses)
            assertTrue(session.recording.completions.isEmpty())
            assertFalse(provider.isPlaying())
            assertFalse(provider.retiringPlaying)
            assertFalse(provider.incomingLoaded)
        } finally {
            engine.release()
            IOSAudioPlayerBridge.provider = null
        }
    }

    @Test
    fun cancelledUncommittedIncomingStartPreservesOutgoingRunAndGain() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 88L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                provider.seekTo(350L)
                val outgoingCompletion = provider.captureCurrentCompletion()
                engine.prepareIncoming(
                    testTrack("cancelled-start"), generation = 89L)
                engine.setCrossfadeGains(0.4f, 0f)
                provider.deferIncomingStart = true
                engine.startIncoming(playbackRunId = 2L)
                engine.cancelPreparedIncoming()
                provider.completeDeferredIncomingStart()

                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertEquals(350L, provider.currentPositionMillis())
                assertEquals(0.4f, provider.currentPlaybackGain)
                assertSame(
                    outgoingCompletion, provider.captureCurrentCompletion())
                assertEquals(88L, session.recording.statusGenerations.last())
                assertTrue(session.recording.errors.isEmpty())
                assertEquals(1, provider.playCallCount)
                provider.simulateNativeCompletion()
                assertEquals(listOf(88L to 1L), session.recording.completions)
                engine.pause()
                engine.play(playbackRunId = 3L)
                assertEquals(0.4f, provider.gainAtPlayStart)
                provider.simulateNativeCompletion()
                assertEquals(
                    listOf(88L to 1L, 88L to 3L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun rejectedFirstPlayingRestoresOutgoingSourceRunGainAndPositionWithoutRestart() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 94L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.setPlaybackGain(0.45f)
                provider.seekTo(350L)
                val outgoingCompletion = provider.captureCurrentCompletion()
                val incomingTrack =
                    testTrack("rejected-incoming").copy(durationMillis = 2_000L)
                engine.prepareIncoming(incomingTrack, generation = 95L)
                var rejectedCompletion: IOSAudioPlayerCompletionHandler? = null
                var rejectedInterruption: IOSAudioInterruptionHandler? = null
                session.recording.onStatus = { generation, status ->
                    if (generation == 95L && status == PlaybackStatus.Playing) {
                        rejectedCompletion = provider.captureCurrentCompletion()
                        rejectedInterruption = provider.interruptionHandler
                        engine.setCrossfadeGains(0.1f, 0.7f)
                        engine.cancelPreparedIncoming()
                    }
                }
                val progressBeforeStart = session.recording.progressEvents.size

                engine.startIncoming(playbackRunId = 2L)

                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                assertEquals(1, provider.playCallCount)
                assertSame(
                    outgoingCompletion, provider.captureCurrentCompletion())
                assertEquals(350L, provider.currentPositionMillis())
                assertEquals(0.45f, provider.currentPlaybackGain)
                assertEquals(
                    "track-94",
                    MPNowPlayingInfoCenter.defaultCenter()
                        .nowPlayingInfo
                        ?.get(MPMediaItemPropertyTitle))
                assertEquals(
                    1.0,
                    MPNowPlayingInfoCenter.defaultCenter()
                        .nowPlayingInfo
                        ?.get(MPMediaItemPropertyPlaybackDuration))
                assertTrue(
                    session.recording.progressEvents
                        .drop(progressBeforeStart)
                        .none { it.first == 95L })
                assertFalse(
                    engine.refreshLoadedMetadata(
                        incomingTrack, generation = 95L))
                assertTrue(
                    engine.refreshLoadedMetadata(
                        testTrack("track-94"), generation = 94L))
                rejectedInterruption?.onRouteDisconnected()
                rejectedCompletion?.onPlaybackCompleted()
                assertTrue(provider.isPlaying())
                assertTrue(session.recording.completions.isEmpty())
                provider.simulateNativeCompletion()
                assertEquals(listOf(94L to 1L), session.recording.completions)
                engine.play(playbackRunId = 3L)
                assertEquals(0.45f, provider.gainAtPlayStart)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun acceptedFinishRetiresOnlyOutgoingAndLaterCancelCannotRestoreIt() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 96L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                val outgoingCompletion = provider.captureCurrentCompletion()
                engine.prepareIncoming(
                    testTrack("accepted-incoming"), generation = 97L)
                engine.startIncoming(playbackRunId = 2L)
                val incomingCompletion = provider.captureCurrentCompletion()
                provider.seekTo(250L)
                engine.setCrossfadeGains(0.2f, 0.6f)

                engine.finishCrossfade()
                engine.cancelPreparedIncoming()

                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertSame(
                    incomingCompletion, provider.captureCurrentCompletion())
                assertEquals(250L, provider.currentPositionMillis())
                assertEquals(0.6f, provider.currentPlaybackGain)
                assertTrue(
                    engine.refreshLoadedMetadata(
                        testTrack("accepted-incoming"), generation = 97L))
                outgoingCompletion.onPlaybackCompleted()
                assertTrue(session.recording.completions.isEmpty())
                provider.simulateNativeCompletion()
                assertEquals(listOf(97L to 2L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun stopInsideFirstPlayingDisposesBothWithoutRollingBackLogicalMedia() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 98L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                val outgoingCompletion = provider.captureCurrentCompletion()
                val incomingTrack = testTrack("stopped-incoming")
                engine.prepareIncoming(incomingTrack, generation = 99L)
                session.recording.onStatus = { generation, status ->
                    if (generation == 99L && status == PlaybackStatus.Playing)
                        engine.stop()
                }
                val progressBeforeStart = session.recording.progressEvents.size

                engine.startIncoming(playbackRunId = 2L)

                assertFalse(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                assertEquals(1f, provider.currentPlaybackGain)
                assertEquals(99L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Stopped, session.recording.statuses.last())
                assertEquals(
                    listOf(Triple(99L, 0L, 1_000L)),
                    session.recording.progressEvents.drop(progressBeforeStart))
                assertTrue(
                    engine.refreshLoadedMetadata(
                        incomingTrack, generation = 99L))
                outgoingCompletion.onPlaybackCompleted()
                assertTrue(session.recording.completions.isEmpty())
                engine.play(playbackRunId = 3L)
                assertEquals(99L, session.recording.statusGenerations.last())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun seekInsideFirstPlayingRetiresOutgoingWithoutChangingIncomingIdentity() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 112L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                val outgoingCompletion = provider.captureCurrentCompletion()
                val incomingTrack = testTrack("sought-incoming")
                engine.prepareIncoming(incomingTrack, generation = 113L)
                var incomingCompletion: IOSAudioPlayerCompletionHandler? = null
                session.recording.onStatus = { generation, status ->
                    if (generation == 113L &&
                        status == PlaybackStatus.Playing) {
                        incomingCompletion = provider.captureCurrentCompletion()
                        engine.setCrossfadeGains(0.2f, 0.6f)
                        engine.seekTo(400L)
                    }
                }

                engine.startIncoming(playbackRunId = 2L)

                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertSame(
                    incomingCompletion, provider.captureCurrentCompletion())
                assertEquals(400L, provider.currentPositionMillis())
                assertEquals(0.6f, provider.currentPlaybackGain)
                assertTrue(
                    engine.refreshLoadedMetadata(
                        incomingTrack, generation = 113L))
                assertEquals(
                    "sought-incoming",
                    MPNowPlayingInfoCenter.defaultCenter()
                        .nowPlayingInfo
                        ?.get(MPMediaItemPropertyTitle))
                outgoingCompletion.onPlaybackCompleted()
                assertTrue(session.recording.completions.isEmpty())
                provider.simulateNativeCompletion()
                assertEquals(listOf(113L to 2L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun replacementAfterFirstPlayingRetiresBothOldVoicesWithoutOverwritingReplacementIdentity() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 114L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                val outgoingCompletion = provider.captureCurrentCompletion()
                engine.prepareIncoming(
                    testTrack("replaced-incoming"), generation = 115L)
                engine.startIncoming(playbackRunId = 2L)
                val incomingCompletion = provider.captureCurrentCompletion()
                val replacement =
                    testTrack("replacement-winner")
                        .copy(durationMillis = 2_000L)

                val loaded = engine.loadPaused(replacement, generation = 116L)
                engine.play(playbackRunId = 3L)
                outgoingCompletion.onPlaybackCompleted()
                incomingCompletion.onPlaybackCompleted()

                assertEquals(LoadedPlayback(116L, 2_000L), loaded)
                assertTrue(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertFalse(provider.incomingLoaded)
                assertEquals(116L, session.recording.statusGenerations.last())
                assertEquals(
                    PlaybackStatus.Playing, session.recording.statuses.last())
                assertEquals(
                    "replacement-winner",
                    MPNowPlayingInfoCenter.defaultCenter()
                        .nowPlayingInfo
                        ?.get(MPMediaItemPropertyTitle))
                assertEquals(
                    2.0,
                    MPNowPlayingInfoCenter.defaultCenter()
                        .nowPlayingInfo
                        ?.get(MPMediaItemPropertyPlaybackDuration))
                assertTrue(
                    engine.refreshLoadedMetadata(
                        replacement, generation = 116L))
                assertTrue(session.recording.errors.isEmpty())
                assertTrue(session.recording.completions.isEmpty())
                provider.simulateNativeCompletion()
                assertEquals(listOf(116L to 3L), session.recording.completions)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun incomingFailureAfterFirstPlayingRetiresOutgoingAndDoesNotResurrectIt() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 90L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("failed-promoted"), generation = 91L)
                engine.startIncoming(playbackRunId = 2L)
                var outgoingRetainedAtError = false
                session.recording.onError = { generation, _ ->
                    if (generation == 91L) {
                        outgoingRetainedAtError = provider.retiringPlaying
                        engine.finishCrossfade()
                    }
                }
                provider.simulateNativeFailure()
                provider.simulateRetiringCompletion()

                assertTrue(outgoingRetainedAtError)
                assertEquals(
                    listOf(91L), session.recording.errors.map { it.first })
                assertEquals(91L, session.recording.statusGenerations.last())
                assertFalse(provider.isPlaying())
                assertFalse(provider.retiringPlaying)
                assertTrue(session.recording.completions.isEmpty())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun callbackCapturedBeforeResumeCannotCompleteTheNewRun() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 92L)
        try {
            val staleCompletion = provider.captureCurrentCompletion()
            session.engine.pause()
            session.engine.play(playbackRunId = 2L)
            staleCompletion.onPlaybackCompleted()

            assertTrue(session.recording.completions.isEmpty())
            assertTrue(provider.isPlaying())
            provider.simulateNativeCompletion()
            assertEquals(listOf(92L to 2L), session.recording.completions)
        } finally {
            session.engine.release()
            IOSAudioPlayerBridge.provider = null
        }
    }

    @Test
    fun releaseInvalidatesPendingOrdinaryNativeLoadWithoutPublishingFailure() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            provider.deferLoad = true
            IOSAudioPlayerBridge.provider = provider
            val engine = createIOSPlaybackEngine(testResolver())
            val recording = RecordingListener()
            engine.listener = recording
            try {
                val load = launch {
                    runCatching {
                        engine.loadPaused(
                            testTrack("release-pending-load"), generation = 93L)
                    }
                }
                kotlinx.coroutines.yield()
                engine.release()
                provider.completeDeferredLoad()
                load.join()

                assertFalse(provider.isLoaded)
                assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
                assertTrue(recording.errors.isEmpty())
                assertEquals(null, provider.completionHandler)
                assertEquals(null, provider.interruptionHandler)
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun seekRetiresOutgoingAndReleaseClearsBothCapturedHandlers() =
        runBlocking {
            val provider = FakeIOSAudioPlayerProvider()
            val session = loadAndPlay(provider, generation = 85L)
            val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
            try {
                engine.prepareIncoming(
                    testTrack("seek-incoming"), generation = 86L)
                engine.startIncoming(playbackRunId = 2L)
                val incomingCompletion = provider.captureCurrentCompletion()
                engine.setCrossfadeGains(0.2f, 0.6f)
                engine.seekTo(400L)
                assertFalse(provider.retiringPlaying)
                assertEquals(400L, provider.currentPositionMillis())
                assertEquals(0.6f, provider.currentPlaybackGain)
                assertSame(
                    incomingCompletion, provider.captureCurrentCompletion())
                assertTrue(
                    engine.refreshLoadedMetadata(
                        testTrack("seek-incoming"), generation = 86L))
                assertTrue(provider.isPlaying())
                provider.simulateRetiringCompletion()
                assertTrue(session.recording.completions.isEmpty())
                engine.release()
                provider.simulateRetiringCompletion()
                provider.simulateNativeCompletion()
                assertTrue(session.recording.completions.isEmpty())
                assertFalse(provider.isPlaying())
                assertFalse(provider.incomingLoaded)
                assertEquals(1f, provider.playbackGainChanges.last())
            } finally {
                engine.release()
                IOSAudioPlayerBridge.provider = null
            }
        }

    @Test
    fun crossfadeGainsRejectInvalidValuesBeforeTouchingEitherVoice() {
        val provider = FakeIOSAudioPlayerProvider()
        val session = loadAndPlay(provider, generation = 87L)
        val engine = assertIs<CrossfadePlaybackEngine>(session.engine)
        try {
            for (invalid in
                listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.01f, 1.01f)) {
                assertFailsWith<IllegalArgumentException> {
                    engine.setCrossfadeGains(invalid, 0.4f)
                }
                assertFailsWith<IllegalArgumentException> {
                    engine.setCrossfadeGains(0.4f, invalid)
                }
            }
            assertEquals(1f, provider.playbackGainChanges.last())
            assertTrue(provider.isPlaying())
        } finally {
            engine.release()
            IOSAudioPlayerBridge.provider = null
        }
    }

    @Test
    fun iosMissingManagedPathMapsToMissingFile() {
        val failure =
            iosLoadFailureError(
                path =
                    "/var/mobile/Containers/Data/Application/RhythHaus/Documents/RhythHaus/absent.wav",
                managedFileMissing = true,
            )

        assertEquals(PlaybackFailureKind.MissingFile, failure.kind)
        assertEquals(
            "/var/mobile/Containers/Data/Application/RhythHaus/Documents/RhythHaus/absent.wav",
            failure.cause)
    }

    @Test
    fun opaqueIosProviderFailureRemainsUnknown() {
        val failure =
            iosLoadFailureError(
                path =
                    "/var/mobile/Containers/Data/Application/RhythHaus/Documents/RhythHaus/present.wav",
                managedFileMissing = false,
            )

        assertEquals(PlaybackFailureKind.Unknown, failure.kind)
        assertEquals(
            "/var/mobile/Containers/Data/Application/RhythHaus/Documents/RhythHaus/present.wav",
            failure.cause)
    }

    @Test
    fun missingManagedRelativePathMapsToMissingFileBeforeProvider() {
        val provider = FakeIOSAudioPlayerProvider()
        IOSAudioPlayerBridge.provider = provider
        val recording = RecordingListener()
        val manager = NSFileManager.defaultManager
        val uniqueName =
            "rhythhaus-absent-${NSProcessInfo.processInfo.globallyUniqueString}"
        val absentRelativePath = "$uniqueName.wav"
        // Guarantee the fixture is absent and leave no residue either way.
        manager.removeItemAtPath(absentRelativePath, null)
        val engine = createIOSPlaybackEngine(relativePathResolver())
        engine.listener = recording
        try {
            val failure =
                assertFailsWith<PlaybackFailureException> {
                    runBlocking {
                        engine.loadPaused(
                            testTrack(uniqueName), generation = 60L)
                    }
                }

            assertEquals(PlaybackFailureKind.MissingFile, failure.error.kind)
            assertEquals(absentRelativePath, failure.error.cause)
            assertFalse(provider.isLoaded)
            assertEquals(null, provider.completionHandler)
            assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
        } finally {
            engine.release()
            IOSAudioPlayerBridge.provider = null
            manager.removeItemAtPath(absentRelativePath, null)
        }
    }

    @Test
    fun unavailableAudioProviderFailsLoadWithStructuredUnknown() {
        IOSAudioPlayerBridge.provider = null
        val recording = RecordingListener()
        val engine = createIOSPlaybackEngine(testResolver())
        engine.listener = recording
        try {
            val failure =
                assertFailsWith<PlaybackFailureException> {
                    runBlocking {
                        engine.loadPaused(
                            testTrack("no-provider"), generation = 61L)
                    }
                }

            assertEquals(PlaybackFailureKind.Unknown, failure.error.kind)
            assertEquals(
                "iOS audio player provider is unavailable",
                failure.error.message,
            )
            assertEquals(listOf(PlaybackStatus.Loading), recording.statuses)
        } finally {
            engine.release()
            IOSAudioPlayerBridge.provider = null
        }
    }
}

private class FakeIOSAudioPlayerProvider : IOSAudioPlayerProvider {
    override var completionHandler: IOSAudioPlayerCompletionHandler? = null
    override var interruptionHandler: IOSAudioInterruptionHandler? = null

    override fun currentOutputNames(): IOSAudioOutputSnapshot =
        IOSAudioOutputSnapshot(emptyList())

    var playSucceeds = true
    var loadSucceeds = true
    var incomingLoadSucceeds = true
    var incomingPlaySucceeds = true
    var playCallCount = 0
    var deferPlayStart = false
    var deferLoad = false
    var deferIncomingLoad = false
    var deferIncomingStart = false
    var onFadeOutAndStop: (() -> Unit)? = null
    var onLoad: (() -> Unit)? = null
    var durationThrows = false
    var gainAtLoadCompletion: Float? = null
    var gainAtPlayStart: Float? = null
    var retiringGainAtPlayStart: Float? = null
    var retiringPlaying = false
    var incomingLoaded = false
    var incomingPlaybackGain = 0f
    private var retiringPlaybackGain = 1f
    private var retiringOriginalGain = 1f
    private var retiringPositionMillis: Long = 0L
    private var currentCompletion: IOSAudioPlayerCompletionHandler? = null
    private var retiringCompletion: IOSAudioPlayerCompletionHandler? = null
    val playbackGainChanges = mutableListOf<Float>()
    private var positionMillis: Long = 0L
    private var durationMillis: Long? = null
    private var playing = false
    private var playbackGain = 1.0f
    val currentPlaybackGain: Float
        get() = playbackGain

    private var playRequest: Long = 0L
    private var pendingPlayStart:
        Pair<Long, IOSAudioPlayerPlaybackStartHandler>? =
        null
    private var loadRequest: Long = 0L
    private var pendingLoad: Pair<Long, IOSAudioPlayerLoadHandler>? = null
    private var incomingRequest: Long = 0L
    private var pendingIncomingLoad: Pair<Long, IOSAudioPlayerLoadHandler>? =
        null
    private var pendingIncomingStart: IncomingStart? = null
    var isLoaded = false

    private data class IncomingStart(
        val request: Long,
        val playRequest: Long,
        val completion: IOSAudioPlayerCompletionHandler,
        val handler: IOSAudioPlayerPlaybackStartHandler,
    )

    override fun loadAsync(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler
    ) {
        loadRequest++
        playRequest++
        durationMillis = 1_000L
        positionMillis = 0L
        playing = false
        if (deferLoad) {
            pendingLoad = loadRequest to handler
            return
        }
        completeLoad(filePath, handler)
    }

    private fun completeLoad(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler,
    ) {
        isLoaded = filePath.isNotBlank() && loadSucceeds
        gainAtLoadCompletion = playbackGain
        if (isLoaded) handler.onAudioLoaded() else handler.onAudioLoadFailed()
        onLoad?.invoke()
    }

    fun completeDeferredLoad() {
        val (request, handler) = requireNotNull(pendingLoad)
        pendingLoad = null
        if (request != loadRequest) {
            handler.onAudioLoadFailed()
            return
        }
        completeLoad("deferred-load", handler)
    }

    override fun playAsync(handler: IOSAudioPlayerPlaybackStartHandler) {
        playCallCount++
        playRequest++
        if (deferPlayStart) {
            pendingPlayStart = playRequest to handler
            return
        }
        completePlayStart(handler)
    }

    private fun completePlayStart(handler: IOSAudioPlayerPlaybackStartHandler) {
        if (!playSucceeds) {
            handler.onPlaybackStartFailed()
            return
        }
        gainAtPlayStart = playbackGain
        if (retiringCompletion != null) {
            retiringPlaying = true
            retiringGainAtPlayStart = retiringPlaybackGain
        }
        currentCompletion = completionHandler
        playing = true
        handler.onPlaybackStarted()
    }

    fun completeDeferredPlayStart() {
        val (request, handler) = requireNotNull(pendingPlayStart)
        pendingPlayStart = null
        if (request != playRequest) {
            handler.onPlaybackStartFailed()
            return
        }
        completePlayStart(handler)
    }

    override fun pause() {
        playRequest++
        playing = false
        retiringPlaying = false
    }

    override fun stop() {
        loadRequest++
        playRequest++
        incomingRequest++
        isLoaded = false
        playing = false
        positionMillis = 0L
        incomingLoaded = false
        retiringPlaying = false
        currentCompletion = null
        retiringCompletion = null
    }

    override fun prepareIncomingAsync(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler
    ) {
        incomingRequest++
        incomingPlaybackGain = 0f
        if (deferIncomingLoad) {
            pendingIncomingLoad = incomingRequest to handler
        } else {
            incomingLoaded = incomingLoadSucceeds && filePath.isNotBlank()
            if (incomingLoaded) handler.onAudioLoaded()
            else handler.onAudioLoadFailed()
        }
    }

    fun completeDeferredIncomingLoad() {
        val (request, handler) = requireNotNull(pendingIncomingLoad)
        pendingIncomingLoad = null
        if (request != incomingRequest) {
            handler.onAudioLoadFailed()
            return
        }
        incomingLoaded = incomingLoadSucceeds
        if (incomingLoaded) handler.onAudioLoaded()
        else handler.onAudioLoadFailed()
    }

    override fun incomingDurationMillis(): Long? = 1_000L

    override fun startIncomingAsync(
        completionHandler: IOSAudioPlayerCompletionHandler,
        handler: IOSAudioPlayerPlaybackStartHandler,
    ) {
        playRequest++
        if (deferIncomingStart) {
            pendingIncomingStart =
                IncomingStart(
                    incomingRequest, playRequest, completionHandler, handler)
        } else {
            completeIncomingStart(completionHandler, handler)
        }
    }

    private fun completeIncomingStart(
        completion: IOSAudioPlayerCompletionHandler,
        handler: IOSAudioPlayerPlaybackStartHandler,
    ) {
        if (!incomingLoaded || !incomingPlaySucceeds) {
            handler.onPlaybackStartFailed()
            return
        }
        retiringCompletion = currentCompletion
        retiringPlaying = playing
        retiringPlaybackGain = playbackGain
        retiringOriginalGain = playbackGain
        retiringPositionMillis = positionMillis
        currentCompletion = completion
        completionHandler = completion
        playbackGain = incomingPlaybackGain
        gainAtPlayStart = playbackGain
        positionMillis = 0L
        playing = true
        incomingLoaded = false
        handler.onPlaybackStarted()
    }

    fun completeDeferredIncomingStart() {
        val (request, activationRequest, completion, handler) =
            requireNotNull(pendingIncomingStart)
        pendingIncomingStart = null
        if (request != incomingRequest || activationRequest != playRequest) {
            handler.onPlaybackStartFailed()
            return
        }
        completeIncomingStart(completion, handler)
    }

    override fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        if (retiringCompletion != null) {
            retiringPlaybackGain = outgoingGain
            playbackGain = incomingGain
        } else {
            playbackGain = outgoingGain
            incomingPlaybackGain = incomingGain
        }
    }

    override fun finishCrossfade() {
        retiringPlaying = false
        retiringCompletion = null
    }

    override fun finishGapless() = finishCrossfade()

    override fun cancelPreparedIncoming() {
        incomingRequest++
        incomingLoaded = false
        val outgoingCompletion = retiringCompletion ?: return
        currentCompletion = outgoingCompletion
        completionHandler = outgoingCompletion
        playing = retiringPlaying
        playbackGain = retiringOriginalGain
        positionMillis = retiringPositionMillis
        retiringPlaying = false
        retiringCompletion = null
    }

    override fun seekTo(positionMillis: Long) {
        this.positionMillis = positionMillis
    }

    override fun currentPositionMillis(): Long {
        return positionMillis
    }

    override fun currentDurationMillis(): Long? {
        if (durationThrows) error("duration setup failed")
        return durationMillis
    }

    override fun isPlaying(): Boolean {
        return playing
    }

    override fun setPlaybackGain(gain: Float) {
        playbackGain = gain
        playbackGainChanges += gain
    }

    override fun fadeOutAndStop(
        fadeDurationSeconds: Double,
        silentVolume: Float
    ) {
        onFadeOutAndStop?.invoke()
        stop()
    }

    fun simulateNativeCompletion() {
        playing = false
        (currentCompletion ?: completionHandler)?.onPlaybackCompleted()
    }

    fun simulateNativeFailure() {
        playing = false
        currentCompletion?.onPlaybackFailed()
    }

    fun captureCurrentCompletion(): IOSAudioPlayerCompletionHandler =
        requireNotNull(currentCompletion)

    fun simulateRetiringCompletion() {
        retiringPlaying = false
        retiringCompletion?.onPlaybackCompleted()
    }

    fun simulateInterruptionBegan() = interruptionHandler?.onInterruptionBegan()

    fun simulateInterruptionEnded(shouldResume: Boolean) =
        interruptionHandler?.onInterruptionEnded(shouldResume)

    fun simulateRouteDisconnected() = interruptionHandler?.onRouteDisconnected()
}

private class RecordingListener : PlaybackEngineListener {
    val statuses = mutableListOf<PlaybackStatus>()
    val statusGenerations = mutableListOf<Long>()
    val completions = mutableListOf<Pair<Long, Long>>()
    val errors = mutableListOf<Pair<Long, PlaybackError>>()
    val progressEvents = mutableListOf<Triple<Long, Long, Long?>>()
    var onPlayRequest: (() -> Unit)? = null
    var onStatus: ((Long, PlaybackStatus) -> Unit)? = null
    var onError: ((Long, PlaybackError) -> Unit)? = null
    var completionCount = 0
    var progressCount = 0

    override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
        statuses += status
        statusGenerations += generation
        onStatus?.invoke(generation, status)
    }

    override fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?,
    ) {
        progressCount++
        progressEvents += Triple(generation, positionMillis, durationMillis)
    }

    override fun onPlaybackCompleted(generation: Long, playbackRunId: Long) {
        completionCount++
        completions += generation to playbackRunId
    }

    override fun onPlaybackError(generation: Long, error: PlaybackError) {
        errors += generation to error
        onError?.invoke(generation, error)
    }

    override fun onSkipToNext(generation: Long) = Unit

    override fun onSkipToPrevious(generation: Long) = Unit

    override fun onPlayRequested(generation: Long) {
        onPlayRequest?.invoke()
    }

    override fun onPauseRequested(generation: Long) = Unit

    override fun onStopRequested(generation: Long) = Unit

    override fun onSeekRequested(generation: Long, positionMillis: Long) = Unit
}

private data class IOSPlaybackTestSession(
    val engine: PlatformPlaybackEngine,
    val recording: RecordingListener,
)

private fun loadAndPlay(
    provider: FakeIOSAudioPlayerProvider,
    generation: Long,
): IOSPlaybackTestSession {
    val session = loadAndPause(provider, generation)
    session.engine.play(1L)
    return session
}

private fun loadAndPause(
    provider: FakeIOSAudioPlayerProvider,
    generation: Long,
): IOSPlaybackTestSession {
    IOSAudioPlayerBridge.provider = provider
    val recording = RecordingListener()
    val engine = createIOSPlaybackEngine(testResolver())
    engine.listener = recording
    var playbackRunId = 1L
    recording.onPlayRequest = { engine.play(++playbackRunId) }
    runBlocking {
        engine.loadPaused(testTrack("track-$generation"), generation)
    }
    return IOSPlaybackTestSession(engine, recording)
}

private fun testTrack(id: String) =
    PlayableTrack(
        id = id,
        title = id,
        artist = "Test",
        album = null,
        durationMillis = 1_000L,
        source = AudioSource.FilePath("$id.wav"),
    )

/**
 * Shared sandbox directory backing every successful test load. Managed FilePath
 * sources must exist before the native loader runs, so the fixture resolver
 * materializes each resolved relative path as a real (empty) file.
 */
private val iosFixtureDirectory: String by lazy {
    val manager = NSFileManager.defaultManager
    val directory = NSTemporaryDirectory() + "rhythhaus-playback-fixtures/"
    if (!manager.fileExistsAtPath(directory)) {
        manager.createDirectoryAtPath(
            directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    directory
}

private fun ensureFixtureFile(relativePath: String): String {
    val manager = NSFileManager.defaultManager
    val path = iosFixtureDirectory + relativePath
    if (!manager.fileExistsAtPath(path)) {
        manager.createFileAtPath(path, contents = null, attributes = null)
    }
    return path
}

private fun testResolver() =
    object : IOSRelativeFilePathResolver {
        override fun resolve(relativePath: String): String =
            ensureFixtureFile(relativePath)
    }

private fun relativePathResolver() =
    object : IOSRelativeFilePathResolver {
        override fun resolve(relativePath: String): String = relativePath
    }
