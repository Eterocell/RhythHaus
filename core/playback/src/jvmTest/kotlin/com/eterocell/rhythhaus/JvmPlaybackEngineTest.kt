package com.eterocell.rhythhaus

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

class JvmPlaybackEngineTest {
    @Test
    fun macOSNativeStartRejectionReportsPlaybackFailureWithoutPublishingPlaying() =
        runBlocking {
            val file = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            try {
                engine.loadPaused(macWavTrack("loaded", file), 10L)
                val statuses = listener.statuses.toList()
                bridge.withLifetimeBoundaryForTest {
                    nativeInvalidateCurrentPlayerForTest(nativeHandle(bridge))
                }
                val failure =
                    assertFailsWith<PlaybackFailureException> {
                        engine.play(100L)
                    }
                assertEquals(PlaybackFailureKind.Unknown, failure.error.kind)
                assertNotNull(failure.error.cause)
                assertEquals(statuses, listener.statuses.toList())
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
                assertTrue(listener.completions.isEmpty())
            } finally {
                engine.release()
                file.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeAdvancesBothVoicesAndPreservesHalfFadeOnResume() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val outgoingHash = wavSha256(outgoingFile)
            val incomingHash = wavSha256(incomingFile)
            val bridge = MacAudioPlayerBridge()
            val engine = createJvmPlaybackEngine(bridge)
            try {
                assertTrue(engine is CrossfadePlaybackEngine)
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 11L)
                engine.play(101L)
                val handle = bridge.currentHandleIdentityForTest()
                val loaded =
                    engine.prepareIncoming(
                        macWavTrack("incoming", incomingFile), 12L)
                assertEquals(12L, loaded.generation)
                assertEquals(5_000L, loaded.durationMillis)
                assertEquals(listOf(1f, 0f), crossfadeVolumes(bridge).toList())
                assertEquals(0L, crossfadeSnapshot(bridge)[2])

                engine.startIncoming(102L)
                assertEquals(listOf(0f, 1f), crossfadeVolumes(bridge).toList())
                val initial = crossfadeSnapshot(bridge)
                val advancing =
                    awaitCrossfadeSnapshot(bridge) {
                        it[0] >= initial[0] + 100L && it[1] >= initial[1] + 100L
                    }
                assertEquals(1L, advancing[3])
                assertEquals(1L, advancing[4])
                assertEquals(2L, advancing[5])
                assertEquals(handle, bridge.currentHandleIdentityForTest())
                assertEquals(2L, bridge.liveRouteListenerCountForTest())
                assertEquals(5L, bridge.liveRemoteHandlerCountForTest())

                engine.setCrossfadeGains(0.5f, 0.5f)
                engine.pause()
                val paused = crossfadeSnapshot(bridge)
                assertEquals(0L, paused[3])
                assertEquals(0L, paused[4])
                delay(200)
                val stillPaused = crossfadeSnapshot(bridge)
                assertEquals(paused[0], stillPaused[0])
                assertEquals(paused[1], stillPaused[1])
                assertEquals(
                    listOf(0.5f, 0.5f), crossfadeVolumes(bridge).toList())
                engine.play(103L)
                awaitCrossfadeSnapshot(bridge) {
                    it[0] >= paused[0] + 100L && it[1] >= paused[1] + 100L
                }
                assertEquals(
                    listOf(0.5f, 0.5f), crossfadeVolumes(bridge).toList())
                engine.setPlaybackGain(0.3f)
                assertEquals(
                    listOf(0.3f, 0.5f), crossfadeVolumes(bridge).toList())
                assertTrue(outgoingHash.contentEquals(wavSha256(outgoingFile)))
                assertTrue(incomingHash.contentEquals(wavSha256(incomingFile)))
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSControllerCrossfadePromotesOneOccurrenceWhileBothNativeVoicesAdvance() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val outgoingHash = wavSha256(outgoingFile)
            val incomingHash = wavSha256(incomingFile)
            val bridge = MacAudioPlayerBridge()
            val engine = createJvmPlaybackEngine(bridge)
            val controller = PlaybackController(engine)
            val started =
                kotlinx.coroutines.channels.Channel<PlaybackStarted>(
                    kotlinx.coroutines.channels.Channel.UNLIMITED,
                )
            val startedJob =
                kotlinx.coroutines
                    .CoroutineScope(
                        kotlinx.coroutines.Dispatchers.Default,
                    )
                    .launch(
                        start =
                            kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                            controller.playbackStarted.collect {
                                started.send(it)
                            }
                        }
            try {
                controller.setCrossfadeDuration(CrossfadeDuration.Seconds3)
                controller.setOccurrenceQueue(
                    listOf(
                        QueueOccurrence(
                            "outgoing",
                            macWavTrack("outgoing", outgoingFile)
                                .copy(durationMillis = 5_000L),
                        ),
                        QueueOccurrence(
                            "incoming",
                            macWavTrack("incoming", incomingFile)
                                .copy(durationMillis = 5_000L),
                        ),
                    ),
                    autoPlay = true,
                )
                assertEquals(
                    "outgoing",
                    withTimeout(5_000) { started.receive() }.occurrenceId)
                val outgoingGeneration = controller.state.value.engineGeneration
                controller.onPlaybackProgress(
                    outgoingGeneration, 3_000L, 5_000L)
                try {
                    withTimeout(5_000) {
                        while (controller.state.value.currentOccurrenceId !=
                            "incoming") delay(10)
                    }
                } catch (
                    timeout: kotlinx.coroutines.TimeoutCancellationException) {
                    throw AssertionError(
                        "Crossfade promotion stalled: ${controller.state.value}; " +
                            "native=${crossfadeSnapshot(bridge).contentToString()}",
                        timeout,
                    )
                }
                assertEquals(
                    "incoming",
                    withTimeout(5_000) { started.receive() }.occurrenceId)
                val initial = crossfadeSnapshot(bridge)
                val advancing =
                    awaitCrossfadeSnapshot(bridge) {
                        it[0] >= initial[0] + 100L && it[1] >= initial[1] + 100L
                    }
                assertEquals(1L, advancing[3])
                assertEquals(1L, advancing[4])
                assertEquals(
                    "incoming", controller.state.value.currentOccurrenceId)
                assertTrue(outgoingHash.contentEquals(wavSha256(outgoingFile)))
                assertTrue(incomingHash.contentEquals(wavSha256(incomingFile)))
            } finally {
                controller.release()
                startedJob.cancel()
                started.close()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeCancelledAndFailedPreparationPreservesOutgoing() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val invalidFile = createTempFile(suffix = ".wav")
            val missingFile = createTempFile(suffix = ".wav")
            missingFile.deleteIfExists()
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 21L)
                engine.setPlaybackGain(0.37f)
                engine.play(201L)
                val handle = bridge.currentHandleIdentityForTest()
                val statuses = listener.statuses.toList()
                val loaded =
                    engine.prepareIncoming(
                        macWavTrack("prepared", incomingFile), 22L)
                assertEquals(22L, loaded.generation)
                assertEquals(statuses, listener.statuses.toList())
                assertEquals(2L, crossfadeSnapshot(bridge)[5])
                engine.cancelPreparedIncoming()
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(-1L, crossfadeSnapshot(bridge)[2])
                assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)

                val missing =
                    assertFailsWith<PlaybackFailureException> {
                        engine.prepareIncoming(
                            macWavTrack("missing", missingFile), 23L)
                    }
                assertEquals(
                    PlaybackFailureKind.MissingFile, missing.error.kind)
                val invalid =
                    assertFailsWith<PlaybackFailureException> {
                        engine.prepareIncoming(
                            macWavTrack("invalid", invalidFile), 24L)
                    }
                assertEquals(PlaybackFailureKind.Unknown, invalid.error.kind)
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(handle, bridge.currentHandleIdentityForTest())
                assertEquals(statuses, listener.statuses.toList())
                val position = bridge.currentPositionMillis()
                awaitCrossfadeSnapshot(bridge) { it[0] >= position + 100L }
                assertTrue(listener.errors.isEmpty())
                assertTrue(listener.completions.isEmpty())
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
                invalidFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeStartFailureBeforePlayingDisposesIncomingAndPreservesOutgoing() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 25L)
                engine.setPlaybackGain(0.37f)
                engine.play(251L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 26L)
                val statuses = listener.statuses.toList()
                bridge.withLifetimeBoundaryForTest {
                    nativeInvalidatePreparedPlayerForTest(nativeHandle(bridge))
                }
                assertFailsWith<PlaybackFailureException> {
                    engine.startIncoming(261L)
                }
                engine.cancelPreparedIncoming()
                engine.publishProgressForTest()
                assertEquals(statuses, listener.statuses.toList())
                assertEquals(25L, listener.progress.last().first)
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
                assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)
                assertTrue(listener.completions.isEmpty())
                val position = bridge.currentPositionMillis()
                awaitCrossfadeSnapshot(bridge) { it[0] >= position + 100L }
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeRejectedPlayingCancellationRestoresOutgoingWithoutRestart() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            var callback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 27L)
                engine.setPlaybackGain(0.37f)
                engine.play(271L)
                engine.seekTo(600L)
                callback = captureMacTerminalCallback(bridge)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 28L)
                val outgoingPosition = bridge.currentPositionMillis()
                val handle = bridge.currentHandleIdentityForTest()
                engine.startIncoming(281L)
                assertTrue(
                    listener.statuses.contains(28L to PlaybackStatus.Playing))
                engine.cancelPreparedIncoming()
                engine.pauseProgressUpdatesForTest()
                engine.publishProgressForTest()
                assertEquals(27L, listener.progress.last().first)
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
                assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)
                assertEquals(handle, bridge.currentHandleIdentityForTest())
                assertEquals(
                    1,
                    listener.statuses.count {
                        it == 27L to PlaybackStatus.Playing
                    })
                nativeDeliverCapturedTerminalCallbackForTest(callback, true)
                callback = 0L
                engine.publishProgressForTest()
                assertEquals(listOf(27L to 271L), listener.completions.toList())
            } finally {
                if (callback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(callback, true)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeCancellationInsidePlayingCallbackRestoresOutgoingWithoutRestart() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener =
                object : RecordingMacPlaybackListener() {
                    override fun onPlaybackStatus(
                        generation: Long,
                        status: PlaybackStatus
                    ) {
                        super.onPlaybackStatus(generation, status)
                        if (generation == 28L &&
                            status == PlaybackStatus.Playing)
                            engine.cancelPreparedIncoming()
                    }
                }
            engine.listener = listener
            var callback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 27L)
                engine.setPlaybackGain(0.37f)
                engine.play(271L)
                engine.seekTo(600L)
                callback = captureMacTerminalCallback(bridge)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 28L)
                val outgoingPosition = bridge.currentPositionMillis()
                engine.startIncoming(281L)
                assertTrue(
                    listener.statuses.contains(28L to PlaybackStatus.Playing))
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
                assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)
                awaitCrossfadeSnapshot(bridge) {
                    it[0] >= outgoingPosition + 100L
                }
                engine.pauseProgressUpdatesForTest()
                engine.publishProgressForTest()
                assertEquals(27L, listener.progress.last().first)
                assertEquals(
                    1,
                    listener.statuses.count {
                        it == 27L to PlaybackStatus.Playing
                    })
                nativeDeliverCapturedTerminalCallbackForTest(callback, true)
                callback = 0L
                engine.publishProgressForTest()
                assertEquals(listOf(27L to 271L), listener.completions.toList())
            } finally {
                if (callback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(callback, true)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
        }

    @Test
    fun macOSCrossfadeNativeStartedIncomingRollsBackWithoutRestartingOutgoing() {
        val outgoingFile = createSilentWavFile(durationMillis = 5_000)
        val incomingFile = createSilentWavFile(durationMillis = 5_000)
        val bridge = MacAudioPlayerBridge()
        try {
            assertTrue(bridge.load(outgoingFile.toString(), 27L))
            bridge.setPlaybackGain(0.37f)
            assertTrue(bridge.play(271L))
            bridge.seekTo(600L)
            assertTrue(bridge.prepareIncoming(incomingFile.toString(), 28L))
            val handle = bridge.currentHandleIdentityForTest()
            val outgoingPosition = bridge.currentPositionMillis()
            assertTrue(bridge.startIncoming(281L))
            bridge.cancelPreparedIncoming()
            assertEquals(1L, crossfadeSnapshot(bridge)[5])
            assertEquals(1L, crossfadeSnapshot(bridge)[3])
            assertTrue(bridge.isPlayingForTest())
            assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)
            assertEquals(handle, bridge.currentHandleIdentityForTest())
            val callback = captureMacTerminalCallback(bridge)
            nativeDeliverCapturedTerminalCallbackForTest(callback, true)
            val completion = assertNotNull(bridge.consumePlaybackEvent())
            assertEquals(27L, completion.generation)
            assertEquals(271L, completion.playbackRunId)
            assertFalse(completion.failed)
        } finally {
            bridge.releasePlayer()
            outgoingFile.deleteIfExists()
            incomingFile.deleteIfExists()
        }
        assertEquals(0L, nativeLiveAudioVoiceCountForTest())
    }

    @Test
    fun macOSGaplessPromotionReleasesOutgoingVoiceWithoutIdleOrFileMutation() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val outgoingHash = wavSha256(outgoingFile)
            val incomingHash = wavSha256(incomingFile)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 401L)
                engine.play(4011L)
                engine.prepareIncoming(macWavTrack("incoming", incomingFile), 402L)
                engine.startIncomingGapless(4021L)
                val snapshot = crossfadeSnapshot(bridge)
                assertEquals(1L, snapshot[5])
                assertEquals(-1L, snapshot[1])
                assertTrue(listener.statuses.contains(402L to PlaybackStatus.Playing))
                assertFalse(listener.statuses.any { it.second == PlaybackStatus.Stopped })
                assertEquals(outgoingHash, wavSha256(outgoingFile))
                assertEquals(incomingHash, wavSha256(incomingFile))
                engine.finishGapless()
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertTrue(listener.errors.isEmpty())
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeRetiringCompletionKeepsOriginalGenerationAndRun() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 450)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            var capturedCallback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 31L)
                engine.play(301L)
                engine.pauseProgressUpdatesForTest()
                capturedCallback = captureMacTerminalCallback(bridge)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 32L)
                engine.startIncoming(302L)
                engine.pauseProgressUpdatesForTest()
                engine.setCrossfadeGains(0.2f, 0.42f)
                awaitCrossfadeSnapshot(bridge) { it[4] == 0L && it[0] >= 450L }
                engine.publishProgressForTest()
                engine.publishProgressForTest()
                assertEquals(listOf(31L to 301L), listener.completions.toList())
                assertEquals(32L, listener.progress.last().first)

                val handle = bridge.currentHandleIdentityForTest()
                val position = bridge.currentPositionMillis()
                engine.finishCrossfade()
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(-1L, crossfadeSnapshot(bridge)[1])
                assertTrue(bridge.currentPositionMillis() >= position)
                assertEquals(0.42f, bridge.playerVolumeForTest(), 0.001f)
                assertEquals(handle, bridge.currentHandleIdentityForTest())
                assertEquals(2L, bridge.liveRouteListenerCountForTest())
                assertEquals(5L, bridge.liveRemoteHandlerCountForTest())
                nativeDeliverCapturedTerminalCallbackForTest(
                    capturedCallback, true)
                capturedCallback = 0L
                engine.publishProgressForTest()
                assertEquals(listOf(31L to 301L), listener.completions.toList())
                val incomingPosition = bridge.currentPositionMillis()
                awaitCrossfadeSnapshot(bridge) {
                    it[0] >= incomingPosition + 100L
                }
            } finally {
                if (capturedCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        capturedCallback, true)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            Unit
        }

    @Test
    fun macOSCrossfadeCapturedOutgoingCallbackRetainsRunAcrossIncomingResume() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            var outgoingCallback = 0L
            var oldIncomingCallback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 35L)
                engine.play(351L)
                outgoingCallback = captureMacTerminalCallback(bridge)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 36L)
                engine.startIncoming(361L)
                oldIncomingCallback = captureMacTerminalCallback(bridge)
                engine.pause()
                engine.play(362L)
                engine.pauseProgressUpdatesForTest()
                nativeDeliverCapturedTerminalCallbackForTest(
                    outgoingCallback, true)
                outgoingCallback = 0L
                nativeDeliverCapturedTerminalCallbackForTest(
                    oldIncomingCallback, true)
                oldIncomingCallback = 0L
                engine.publishProgressForTest()
                assertEquals(listOf(35L to 351L), listener.completions.toList())
                assertEquals(36L, listener.progress.last().first)
                assertTrue(listener.errors.isEmpty())
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
            } finally {
                if (outgoingCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        outgoingCallback, true)
                if (oldIncomingCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        oldIncomingCallback, true)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeStaleNativeCallbacksCannotCompleteOrFailReplacement() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            var completedCallback = 0L
            var failedCallback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 41L)
                engine.play(401L)
                completedCallback = captureMacTerminalCallback(bridge)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 42L)
                engine.startIncoming(402L)
                failedCallback = captureMacTerminalCallback(bridge)
                engine.loadPaused(macWavTrack("replacement", outgoingFile), 43L)
                engine.play(403L)
                engine.pauseProgressUpdatesForTest()
                nativeDeliverCapturedTerminalCallbackForTest(
                    completedCallback, true)
                completedCallback = 0L
                nativeDeliverCapturedTerminalCallbackForTest(
                    failedCallback, false)
                failedCallback = 0L
                engine.publishProgressForTest()
                assertTrue(listener.completions.isEmpty())
                assertTrue(listener.errors.isEmpty())
                assertEquals(43L, listener.progress.last().first)
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
            } finally {
                if (completedCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        completedCallback, true)
                if (failedCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        failedCallback, false)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeIncomingFailureReportsIncomingWithoutResurrectingOutgoing() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            var failedCallback = 0L
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 51L)
                engine.play(501L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 52L)
                engine.startIncoming(502L)
                engine.pauseProgressUpdatesForTest()
                failedCallback = captureMacTerminalCallback(bridge)
                nativeDeliverCapturedTerminalCallbackForTest(
                    failedCallback, false)
                failedCallback = 0L
                engine.publishProgressForTest()
                assertEquals(listOf(52L), listener.errors.map { it.first })
                assertTrue(listener.completions.isEmpty())
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
                assertEquals(0L, crossfadeSnapshot(bridge)[4])
                assertEquals(52L, listener.progress.last().first)
                engine.finishCrossfade()
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
            } finally {
                if (failedCallback != 0L)
                    nativeDeliverCapturedTerminalCallbackForTest(
                        failedCallback, false)
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeRemoteCommandsTargetPromotedGenerationAndBothVoices() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val remoteGenerations = mutableListOf<Long>()
            engine.listener =
                object : RecordingMacPlaybackListener() {
                    override fun onPlayRequested(generation: Long) {
                        remoteGenerations += generation
                        engine.play(603L)
                    }

                    override fun onPauseRequested(generation: Long) {
                        remoteGenerations += generation
                        engine.pause()
                    }

                    override fun onSeekRequested(
                        generation: Long,
                        positionMillis: Long
                    ) {
                        remoteGenerations += generation
                        engine.seekTo(positionMillis)
                    }

                    override fun onStopRequested(generation: Long) {
                        remoteGenerations += generation
                        engine.stop()
                    }
                }
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 61L)
                engine.play(601L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 62L)
                engine.startIncoming(602L)
                engine.setCrossfadeGains(0.5f, 0.5f)
                engine.setUserTransportEnabled(false)
                assertFalse(bridge.invokeRemotePauseForTest())
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
                assertEquals(1L, crossfadeSnapshot(bridge)[4])
                engine.setUserTransportEnabled(true)
                assertTrue(bridge.invokeRemotePauseForTest())
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
                assertEquals(0L, crossfadeSnapshot(bridge)[4])
                assertTrue(bridge.invokeRemotePlayForTest())
                assertEquals(1L, crossfadeSnapshot(bridge)[3])
                assertEquals(1L, crossfadeSnapshot(bridge)[4])
                assertEquals(
                    listOf(0.5f, 0.5f), crossfadeVolumes(bridge).toList())
                val handle = bridge.currentHandleIdentityForTest()
                assertTrue(bridge.invokeRemoteSeekForTest(800L))
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertTrue(bridge.currentPositionMillis() >= 800L)
                assertEquals(handle, bridge.currentHandleIdentityForTest())
                assertTrue(bridge.invokeRemoteStopForTest())
                assertEquals(0L, crossfadeSnapshot(bridge)[5])
                assertEquals(listOf(62L, 62L, 62L, 62L), remoteGenerations)
                assertEquals(5L, bridge.liveRemoteHandlerCountForTest())
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeRouteLossPausesBothOnceWithoutChangingGains() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            val listener = RecordingMacPlaybackListener()
            engine.listener = listener
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 71L)
                bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L)
                engine.play(701L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 72L)
                engine.startIncoming(702L)
                engine.pauseProgressUpdatesForTest()
                engine.setCrossfadeGains(0.5f, 0.5f)
                bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L)
                engine.publishProgressForTest()
                engine.publishProgressForTest()
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
                assertEquals(0L, crossfadeSnapshot(bridge)[4])
                assertEquals(
                    listOf(0.5f, 0.5f), crossfadeVolumes(bridge).toList())
                assertEquals(
                    1,
                    listener.statuses.count {
                        it == 72L to PlaybackStatus.Paused
                    })
                bridge.simulateRouteSnapshotForTest(longArrayOf(10L, 20L), 10L)
                engine.publishProgressForTest()
                assertEquals(0L, crossfadeSnapshot(bridge)[3])
                assertEquals(0L, crossfadeSnapshot(bridge)[4])
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeStopDisposesBothAndReplayReloadsCurrentAtUnity() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 81L)
                engine.play(801L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 82L)
                engine.startIncoming(802L)
                engine.setCrossfadeGains(0.25f, 0.4f)
                engine.stop()
                assertEquals(0L, crossfadeSnapshot(bridge)[5])
                assertEquals(1f, bridge.playerVolumeForTest())
                engine.play(803L)
                awaitCrossfadeSnapshot(bridge) { it[0] >= 100L }
                assertEquals(1L, crossfadeSnapshot(bridge)[5])
                assertEquals(1f, bridge.playerVolumeForTest())
                assertEquals(-1L, crossfadeSnapshot(bridge)[1])
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun macOSCrossfadeClearAndReleaseDisposePreparedAndRetiringVoices() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 91L)
                engine.play(901L)
                engine.prepareIncoming(
                    macWavTrack("prepared", incomingFile), 92L)
                engine.clear(93L)
                assertEquals(0L, crossfadeSnapshot(bridge)[5])
                assertEquals(1f, bridge.playerVolumeForTest())
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 94L)
                engine.play(904L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 95L)
                engine.startIncoming(905L)
                assertEquals(2L, crossfadeSnapshot(bridge)[5])
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
            assertEquals(0L, nativeLiveAudioVoiceCountForTest())
            assertEquals(0L, bridge.liveRouteListenerCountForTest())
            assertEquals(0L, bridge.liveRemoteHandlerCountForTest())
        }

    @Test
    fun macOSCrossfadeRejectsInvalidGainsWithoutMutatingEitherVoice() =
        runBlocking {
            val outgoingFile = createSilentWavFile(durationMillis = 5_000)
            val incomingFile = createSilentWavFile(durationMillis = 5_000)
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            try {
                engine.loadPaused(macWavTrack("outgoing", outgoingFile), 101L)
                engine.prepareIncoming(
                    macWavTrack("incoming", incomingFile), 102L)
                engine.startIncoming(1_002L)
                engine.setCrossfadeGains(0.4f, 0.3f)
                for (invalid in
                    listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.1f, 1.1f)) {
                    assertFailsWith<IllegalArgumentException> {
                        engine.setCrossfadeGains(invalid, 0.7f)
                    }
                    assertFailsWith<IllegalArgumentException> {
                        engine.setCrossfadeGains(0.7f, invalid)
                    }
                    assertEquals(
                        listOf(0.3f, 0.4f), crossfadeVolumes(bridge).toList())
                }
            } finally {
                engine.release()
                outgoingFile.deleteIfExists()
                incomingFile.deleteIfExists()
            }
        }

    @Test
    fun replayGainChangesNativeVolumeWithoutReloadAndTimerCancelKeepsNormalization() =
        runBlocking {
            val file = createSilentWavFile(durationMillis = 10_000)
            val originalBytes = Files.readAllBytes(file)
            val bridge = MacAudioPlayerBridge()
            val controller =
                PlaybackController(MacOSNativePlaybackEngine(bridge))
            try {
                controller.setReplayGainMode(ReplayGainMode.Track)
                controller.setQueue(
                    listOf(
                        macWavTrack("normalized-native", file)
                            .copy(replayGainTrackGainDb = -6.0)))
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Paused
                }
                controller.play()
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Playing
                }
                withTimeout(5_000) {
                    while (kotlin.math.abs(
                        bridge.playerVolumeForTest() - 0.5011872f) >
                        0.001f) yield()
                }
                val generation = controller.state.value.engineGeneration
                controller.armSleepTimerAfterCompletions(1, false)
                controller.cancelSleepTimer()
                withTimeout(5_000) {
                    while (kotlin.math.abs(
                        bridge.playerVolumeForTest() - 0.5011872f) >
                        0.001f) yield()
                }
                controller.setReplayGainMode(ReplayGainMode.Off)
                withTimeout(5_000) {
                    while (bridge.playerVolumeForTest() != 1f) yield()
                }
                assertEquals(
                    generation, controller.state.value.engineGeneration)
                assertEquals(
                    PlaybackStatus.Playing, controller.state.value.status)
                assertEquals(
                    "normalized-native",
                    controller.state.value.currentTrack?.id)
                assertTrue(
                    originalBytes.contentEquals(Files.readAllBytes(file)))
            } finally {
                controller.release()
                file.deleteIfExists()
            }
        }

    @Test
    fun seekingToNativeEndpointDoesNotConsumeSleepTimerCompletion() =
        runBlocking {
            val file = createSilentWavFile(durationMillis = 2_000)
            val engine = MacOSNativePlaybackEngine(MacAudioPlayerBridge())
            val controller = PlaybackController(engine)
            try {
                controller.setQueue(
                    listOf(
                        PlayableTrack(
                            "seek-timer",
                            "Seek",
                            "Test",
                            null,
                            2_000L,
                            AudioSource.FilePath(file.toString()))))
                withTimeout(5_000) {
                    while (controller.state.value.status !=
                        PlaybackStatus.Paused) yield()
                }
                engine.play(1L)
                engine.pauseProgressUpdatesForTest()
                controller.armSleepTimerAfterCompletions(1, false)
                engine.seekTo(2_000L)
                engine.publishProgressForTest()
                assertEquals(
                    1, controller.sleepTimerState.value.remainingTracks)
                assertEquals(
                    PlaybackStatus.Playing, controller.state.value.status)
                assertEquals(
                    "seek-timer", controller.state.value.currentTrack?.id)
            } finally {
                controller.release()
                file.deleteIfExists()
            }
        }

    @Test
    fun macOSNowPlayingInfoUpdateAcceptsTrackMetadata() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.updateNowPlayingInfo(
                title = "Night Drive",
                artist = "Rhyth Haus",
                album = "Local Sessions",
                durationMillis = 181_000L,
                positionMillis = 42_000L,
            )
            bridge.clearNowPlayingInfo()
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSNowPlayingPlaybackStateUpdatesForControlCenterVisibility() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Stopped)
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSNowPlayingRegistersRemoteCommandsForControlCenter() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.registerNowPlayingRemoteCommands()
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSRouteLifecycleDrainsPartialRegistrationWhenReleaseStartsOnRouteQueue() {
        val bridge = MacAudioPlayerBridge()
        try {
            assertEquals(2L, bridge.liveRouteListenerCountForTest())
            assertTrue(bridge.routeLifecyclePartialRegistrationForTest())
            assertEquals(2L, bridge.liveRouteListenerCountForTest())
        } finally {
            bridge.releasePlayer()
        }
        assertEquals(0L, bridge.liveRouteListenerCountForTest())
    }

    @Test
    fun macOSNativeReleaseCompletesWhenStartedOnRouteQueue() {
        val bridge = MacAudioPlayerBridge()
        val handleField =
            bridge.javaClass.getDeclaredField("handle").apply {
                isAccessible = true
            }
        try {
            val handle = handleField.getLong(bridge)
            assertTrue(nativeReleaseOnRouteQueueForTest(handle))
            handleField.setLong(bridge, 0L)
            assertEquals(0L, bridge.liveRouteListenerCountForTest())
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSRouteSnapshotsClassifyRemovalButIgnoreBenignDefaultSwitch() {
        val bridge = MacAudioPlayerBridge()
        try {
            assertEquals(2L, bridge.liveRouteListenerCountForTest())
            bridge.simulateRouteSnapshotForTest(longArrayOf(10L, 20L), 10L)
            bridge.setRouteExpectedActiveForTest(true)
            bridge.simulateRouteSnapshotForTest(longArrayOf(10L, 20L), 20L)
            assertFalse(bridge.consumeRouteDisconnected())

            bridge.resetPlayer()
            bridge.resetPlayer()
            bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L)
            bridge.setRouteExpectedActiveForTest(true)
            bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L)
            assertTrue(bridge.consumeRouteDisconnected())
            assertFalse(bridge.consumeRouteDisconnected())

            bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L)
            bridge.setRouteExpectedActiveForTest(true)
            bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L)
            assertTrue(bridge.consumeRouteDisconnected())
        } finally {
            bridge.releasePlayer()
        }
        assertEquals(0L, bridge.liveRouteListenerCountForTest())
    }

    @Test
    fun macOSRouteDisconnectIsIgnoredWhenPlaybackIsInactive() {
        val bridge = MacAudioPlayerBridge()
        try {
            assertTrue(
                bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L))
            bridge.pause()
            assertTrue(
                bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L))
            assertFalse(bridge.consumeRouteDisconnected())
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSRouteSnapshotClassificationIsStableAcrossRepeatedFinalCallbacks() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L)
            bridge.setRouteExpectedActiveForTest(true)
            bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L)
            bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L)
            assertTrue(bridge.consumeRouteDisconnected())
            assertFalse(bridge.consumeRouteDisconnected())
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSRouteDisconnectPausesActiveEngineExactlyOnceAndDoesNotResume() {
        val wavPath = createSilentWavFile(durationMillis = 800)
        val bridge = MacAudioPlayerBridge()
        val engine = createJvmPlaybackEngine(bridge)
        val statuses = mutableListOf<PlaybackStatus>()
        val pausedLatch = CountDownLatch(1)
        engine.listener =
            object : RejectingRemoteTransportListener() {
                override fun onPlaybackStatus(
                    generation: Long,
                    status: PlaybackStatus
                ) {
                    synchronized(statuses) {
                        statuses += status
                        if (status == PlaybackStatus.Paused &&
                            PlaybackStatus.Playing in statuses) {
                            pausedLatch.countDown()
                        }
                    }
                }

                override fun onPlaybackProgress(
                    generation: Long,
                    positionMillis: Long,
                    durationMillis: Long?
                ) = Unit

                override fun onPlaybackCompleted(
                    generation: Long,
                    playbackRunId: Long
                ) = Unit

                override fun onPlaybackError(
                    generation: Long,
                    error: PlaybackError
                ) = Unit

                override fun onSkipToNext(generation: Long) = Unit

                override fun onSkipToPrevious(generation: Long) = Unit
            }
        try {
            runBlocking {
                engine.loadPaused(
                    PlayableTrack(
                        id = "route-loss",
                        title = "Route Loss",
                        artist = "Test",
                        album = null,
                        durationMillis = null,
                        source = AudioSource.FilePath(wavPath.toString()),
                    ),
                    generation = 3L,
                )
            }
            engine.play(1L)
            engine.seekTo(200L)
            assertTrue(bridge.invokeRouteDisconnectForTest())
            assertTrue(pausedLatch.await(1, TimeUnit.SECONDS))
            synchronized(statuses) {
                val playingIndex = statuses.indexOf(PlaybackStatus.Playing)
                assertEquals(
                    1,
                    statuses.drop(playingIndex + 1).count {
                        it == PlaybackStatus.Paused
                    })
            }
            assertTrue(bridge.nowPlayingPositionMillisForTest() >= 200L)
            assertFalse(bridge.isPlayingForTest())
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun macOSRouteLossDoesNotPublishStalePausedAfterProgressListenerReplacesSource() {
        val firstWavPath = createSilentWavFile(durationMillis = 800)
        val secondWavPath = createSilentWavFile(durationMillis = 800)
        val bridge = MacAudioPlayerBridge()
        val engine = MacOSNativePlaybackEngine(bridge)
        val statuses = mutableListOf<Pair<Long, PlaybackStatus>>()
        val replaced = CountDownLatch(1)
        val routeLossProgress = CountDownLatch(1)
        val replaceOnRouteLossProgress = AtomicBoolean(false)
        engine.listener =
            object : RejectingRemoteTransportListener() {
                override fun onPlaybackStatus(
                    generation: Long,
                    status: PlaybackStatus
                ) {
                    synchronized(statuses) { statuses += generation to status }
                }

                override fun onPlaybackProgress(
                    generation: Long,
                    positionMillis: Long,
                    durationMillis: Long?
                ) {
                    if (generation == 3L &&
                        replaceOnRouteLossProgress.get() &&
                        routeLossProgress.count == 1L) {
                        routeLossProgress.countDown()
                        runBlocking {
                            engine.loadPaused(
                                PlayableTrack(
                                    id = "replacement",
                                    title = "Replacement",
                                    artist = "Test",
                                    album = null,
                                    durationMillis = null,
                                    source =
                                        AudioSource.FilePath(
                                            secondWavPath.toString()),
                                ),
                                generation = 4L,
                            )
                        }
                        replaced.countDown()
                    }
                }

                override fun onPlaybackCompleted(
                    generation: Long,
                    playbackRunId: Long
                ) = Unit

                override fun onPlaybackError(
                    generation: Long,
                    error: PlaybackError
                ) = Unit

                override fun onSkipToNext(generation: Long) = Unit

                override fun onSkipToPrevious(generation: Long) = Unit
            }
        try {
            runBlocking {
                engine.loadPaused(
                    PlayableTrack(
                        id = "route-loss-source",
                        title = "Route Loss Source",
                        artist = "Test",
                        album = null,
                        durationMillis = null,
                        source = AudioSource.FilePath(firstWavPath.toString()),
                    ),
                    generation = 3L,
                )
            }
            engine.play(1L)
            engine.pauseProgressUpdatesForTest()
            replaceOnRouteLossProgress.set(true)
            assertTrue(bridge.invokeRouteDisconnectForTest())
            engine.publishProgressForTest()
            assertTrue(routeLossProgress.await(1, TimeUnit.SECONDS))
            assertTrue(replaced.await(1, TimeUnit.SECONDS))
            synchronized(statuses) {
                assertFalse(
                    statuses
                        .dropWhile { it != (3L to PlaybackStatus.Playing) }
                        .drop(1)
                        .any { it == 3L to PlaybackStatus.Paused })
                assertTrue((4L to PlaybackStatus.Paused) in statuses)
            }
        } finally {
            engine.release()
            firstWavPath.deleteIfExists()
            secondWavPath.deleteIfExists()
        }
    }

    @Test
    fun macOSLoadPausedDoesNotPublishInitialEventsAfterLoadingListenerReplacesSource() {
        val firstWavPath = createSilentWavFile(durationMillis = 800)
        val secondWavPath = createSilentWavFile(durationMillis = 800)
        val bridge = MacAudioPlayerBridge()
        val engine = createJvmPlaybackEngine(bridge)
        val events = mutableListOf<Pair<Long, String>>()
        var replacing = false
        engine.listener =
            object : RejectingRemoteTransportListener() {
                override fun onPlaybackStatus(
                    generation: Long,
                    status: PlaybackStatus
                ) {
                    synchronized(events) { events += generation to status.name }
                    if (generation == 3L &&
                        status == PlaybackStatus.Loading &&
                        !replacing) {
                        replacing = true
                        runBlocking {
                            engine.loadPaused(
                                PlayableTrack(
                                    id = "replacement",
                                    title = "Replacement",
                                    artist = "Test",
                                    album = null,
                                    durationMillis = null,
                                    source =
                                        AudioSource.FilePath(
                                            secondWavPath.toString()),
                                ),
                                generation = 4L,
                            )
                        }
                    }
                }

                override fun onPlaybackProgress(
                    generation: Long,
                    positionMillis: Long,
                    durationMillis: Long?
                ) {
                    synchronized(events) { events += generation to "Progress" }
                }

                override fun onPlaybackCompleted(
                    generation: Long,
                    playbackRunId: Long
                ) = Unit

                override fun onPlaybackError(
                    generation: Long,
                    error: PlaybackError
                ) = Unit

                override fun onSkipToNext(generation: Long) = Unit

                override fun onSkipToPrevious(generation: Long) = Unit
            }
        try {
            runBlocking {
                engine.loadPaused(
                    PlayableTrack(
                        id = "original",
                        title = "Original",
                        artist = "Test",
                        album = null,
                        durationMillis = null,
                        source = AudioSource.FilePath(firstWavPath.toString()),
                    ),
                    generation = 3L,
                )
            }
            synchronized(events) {
                assertFalse(
                    events
                        .dropWhile { it != (3L to PlaybackStatus.Loading.name) }
                        .drop(1)
                        .any { it.first == 3L })
                assertTrue(events.contains(4L to PlaybackStatus.Loading.name))
                assertTrue(events.contains(4L to PlaybackStatus.Paused.name))
            }
        } finally {
            engine.release()
            firstWavPath.deleteIfExists()
            secondWavPath.deleteIfExists()
        }
    }

    @Test
    fun macOSRemoteHandlersFollowNativeHandleLifetimeAcrossResets() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.registerNowPlayingRemoteCommands()
            bridge.registerNowPlayingRemoteCommands()
            assertEquals(5, bridge.liveRemoteHandlerCountForTest())

            repeat(3) {
                bridge.resetPlayer()
                assertEquals(0, bridge.liveRemoteHandlerCountForTest())
                bridge.registerNowPlayingRemoteCommands()
                assertEquals(5, bridge.liveRemoteHandlerCountForTest())
            }
        } finally {
            bridge.releasePlayer()
        }
        assertEquals(0, bridge.liveRemoteHandlerCountForTest())
    }

    @Test
    fun macOSNativePlayerGainSurvivesNativeAndHandleReplacement() {
        val firstWavPath = createSilentWavFile()
        val secondWavPath = createSilentWavFile()
        val bridge = MacAudioPlayerBridge()
        try {
            assertTrue(bridge.load(firstWavPath.toString()))

            bridge.setPlaybackGain(0.37f)
            assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)

            assertTrue(bridge.load(secondWavPath.toString()))
            assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)

            bridge.resetPlayer()
            assertTrue(bridge.load(firstWavPath.toString()))
            assertEquals(0.37f, bridge.playerVolumeForTest(), 0.001f)
        } finally {
            bridge.releasePlayer()
            firstWavPath.deleteIfExists()
            secondWavPath.deleteIfExists()
        }
    }

    @Test
    fun macOSRemoteStopRestoresUnityGainBeforeLaterPlayerReplacement() =
        runBlocking {
            val firstWavPath = createSilentWavFile()
            val secondWavPath = createSilentWavFile()
            val bridge = MacAudioPlayerBridge()
            val controller =
                PlaybackController(MacOSNativePlaybackEngine(bridge))
            try {
                controller.setQueue(
                    listOf(macWavTrack("remote-stop", firstWavPath)))
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Paused
                }
                bridge.setPlaybackGain(0.37f)

                assertTrue(bridge.invokeRemoteStopForTest())
                withTimeout(5_000) {
                    while (bridge.playerVolumeForTest() != 1f) yield()
                }

                bridge.resetPlayer()
                assertTrue(bridge.load(secondWavPath.toString()))
                assertEquals(1f, bridge.playerVolumeForTest(), 0.001f)
            } finally {
                controller.release()
                firstWavPath.deleteIfExists()
                secondWavPath.deleteIfExists()
            }
        }

    @Test
    fun macOSPlaybackEngineRestoresUnityGainAfterStop() = runBlocking {
        val wavPath = createSilentWavFile()
        val bridge = MacAudioPlayerBridge()
        val engine = MacOSNativePlaybackEngine(bridge)
        try {
            engine.loadPaused(
                macWavTrack("stop-gain", wavPath), generation = 1L)
            engine.setPlaybackGain(0.24f)
            assertEquals(0.24f, bridge.playerVolumeForTest(), 0.001f)

            engine.stop()

            assertEquals(1f, bridge.playerVolumeForTest(), 0.001f)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun macOSPlaybackEngineRestoresUnityGainAfterClearBeforeLaterLoad() =
        runBlocking {
            val firstWavPath = createSilentWavFile()
            val secondWavPath = createSilentWavFile()
            val bridge = MacAudioPlayerBridge()
            val engine = MacOSNativePlaybackEngine(bridge)
            try {
                engine.loadPaused(
                    macWavTrack("clear-gain-first", firstWavPath),
                    generation = 1L,
                )
                engine.setPlaybackGain(0.61f)
                assertEquals(0.61f, bridge.playerVolumeForTest(), 0.001f)

                engine.clear(generation = 2L)
                engine.loadPaused(
                    macWavTrack("clear-gain-second", secondWavPath),
                    generation = 3L,
                )

                assertEquals(1f, bridge.playerVolumeForTest(), 0.001f)
            } finally {
                engine.release()
                firstWavPath.deleteIfExists()
                secondWavPath.deleteIfExists()
            }
        }

    @Test
    fun inFlightBridgeOperationBlocksResetAndKeepsOneHandleIdentity() {
        val bridge = MacAudioPlayerBridge()
        val operationEntered = CountDownLatch(1)
        val releaseOperation = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val originalHandle = bridge.currentHandleIdentityForTest()
            val operation =
                executor.submit<Long> {
                    bridge.withLifetimeBoundaryForTest { ownedHandle ->
                        operationEntered.countDown()
                        assertTrue(releaseOperation.await(1, TimeUnit.SECONDS))
                        ownedHandle
                    }
                }
            assertTrue(operationEntered.await(1, TimeUnit.SECONDS))
            val reset = executor.submit { bridge.resetPlayer() }

            assertFailsWith<TimeoutException> {
                reset.get(100, TimeUnit.MILLISECONDS)
            }
            releaseOperation.countDown()
            assertEquals(originalHandle, operation.get(1, TimeUnit.SECONDS))
            reset.get(1, TimeUnit.SECONDS)
            assertFalse(originalHandle == bridge.currentHandleIdentityForTest())
        } finally {
            releaseOperation.countDown()
            executor.shutdownNow()
            bridge.releasePlayer()
        }
    }

    @Test
    fun inFlightBridgeOperationBlocksFinalReleaseAndPreventsLaterEntry() {
        val bridge = MacAudioPlayerBridge()
        val operationEntered = CountDownLatch(1)
        val releaseOperation = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val operation = executor.submit {
                bridge.withLifetimeBoundaryForTest {
                    operationEntered.countDown()
                    assertTrue(releaseOperation.await(1, TimeUnit.SECONDS))
                }
            }
            assertTrue(operationEntered.await(1, TimeUnit.SECONDS))
            val release = executor.submit { bridge.releasePlayer() }

            assertFailsWith<TimeoutException> {
                release.get(100, TimeUnit.MILLISECONDS)
            }
            releaseOperation.countDown()
            operation.get(1, TimeUnit.SECONDS)
            release.get(1, TimeUnit.SECONDS)

            assertEquals(0L, bridge.currentHandleIdentityForTest())
            assertFailsWith<IllegalArgumentException> {
                bridge.currentPositionMillis()
            }
            assertFailsWith<IllegalArgumentException> {
                bridge.withLifetimeBoundaryForTest {}
            }
        } finally {
            releaseOperation.countDown()
            executor.shutdownNow()
            bridge.releasePlayer()
        }
    }

    @Test
    fun finalReleasePermanentlyRejectsResetWithoutRecreatingHandlers() {
        val bridge = MacAudioPlayerBridge()
        bridge.registerNowPlayingRemoteCommands()

        bridge.releasePlayer()
        bridge.releasePlayer()

        assertFailsWith<IllegalArgumentException> { bridge.resetPlayer() }
        assertEquals(0L, bridge.currentHandleIdentityForTest())
        assertEquals(0L, bridge.liveRemoteHandlerCountForTest())
    }

    @Test
    fun queuedReleaseWinsBeforeQueuedResetAndPermanentlyPreventsResurrection() {
        val bridge = MacAudioPlayerBridge()
        bridge.registerNowPlayingRemoteCommands()
        val operationEntered = CountDownLatch(1)
        val releaseOperation = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val operation = executor.submit {
                bridge.withLifetimeBoundaryForTest {
                    operationEntered.countDown()
                    assertTrue(releaseOperation.await(1, TimeUnit.SECONDS))
                }
            }
            assertTrue(operationEntered.await(1, TimeUnit.SECONDS))
            val release = executor.submit { bridge.releasePlayer() }
            val reset = executor.submit { bridge.resetPlayer() }

            releaseOperation.countDown()
            operation.get(1, TimeUnit.SECONDS)
            release.get(1, TimeUnit.SECONDS)
            assertFailsWith<java.util.concurrent.ExecutionException> {
                reset.get(1, TimeUnit.SECONDS)
            }

            assertEquals(0L, bridge.currentHandleIdentityForTest())
            assertEquals(0L, bridge.liveRemoteHandlerCountForTest())
        } finally {
            releaseOperation.countDown()
            executor.shutdownNow()
            bridge.releasePlayer()
        }
    }

    @Test
    fun nativeMacPlaybackEngineLoadsGeneratedWavFile() {
        val wavPath = createSilentWavFile()
        val engine = createJvmPlaybackEngine()
        val events = mutableListOf<PlaybackStatus>()
        var latestDuration: Long? = null
        var latestError: PlaybackError? = null
        engine.listener =
            object : RejectingRemoteTransportListener() {
                override fun onPlaybackStatus(
                    generation: Long,
                    status: PlaybackStatus
                ) {
                    synchronized(events) { events += status }
                }

                override fun onPlaybackProgress(
                    generation: Long,
                    positionMillis: Long,
                    durationMillis: Long?
                ) {
                    synchronized(events) { latestDuration = durationMillis }
                }

                override fun onPlaybackCompleted(
                    generation: Long,
                    playbackRunId: Long
                ) = Unit

                override fun onPlaybackError(
                    generation: Long,
                    error: PlaybackError
                ) {
                    synchronized(events) { latestError = error }
                }

                override fun onSkipToNext(generation: Long) = Unit

                override fun onSkipToPrevious(generation: Long) = Unit
            }

        try {
            runBlocking {
                engine.loadPaused(
                    PlayableTrack(
                        id = "generated-wav",
                        title = "Generated WAV",
                        artist = "Test",
                        album = null,
                        durationMillis = null,
                        source = AudioSource.FilePath(wavPath.toString()),
                    ),
                    generation = 1L,
                )
            }
            engine.setUserTransportEnabled(true)
            engine.play(1L)
            engine.pause()
            engine.seekTo(10L)
            engine.stop()

            synchronized(events) {
                assertEquals(null, latestError)
                assertTrue(PlaybackStatus.Loading in events)
                assertTrue(PlaybackStatus.Paused in events)
                assertTrue(PlaybackStatus.Playing in events)
                assertTrue(PlaybackStatus.Stopped in events)
                assertNotNull(latestDuration)
                assertTrue(latestDuration!! > 0L)
            }
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun nativeMacPlaybackEnginePublishesProgressWhilePlaying() {
        val wavPath = createSilentWavFile(durationMillis = 800)
        val engine = createJvmPlaybackEngine()
        val progressPositions = mutableListOf<Long>()
        val progressLatch = CountDownLatch(1)
        var latestError: PlaybackError? = null
        engine.listener =
            object : RejectingRemoteTransportListener() {
                override fun onPlaybackStatus(
                    generation: Long,
                    status: PlaybackStatus
                ) = Unit

                override fun onPlaybackProgress(
                    generation: Long,
                    positionMillis: Long,
                    durationMillis: Long?
                ) {
                    if (positionMillis > 0L) {
                        progressPositions += positionMillis
                        progressLatch.countDown()
                    }
                }

                override fun onPlaybackCompleted(
                    generation: Long,
                    playbackRunId: Long
                ) = Unit

                override fun onPlaybackError(
                    generation: Long,
                    error: PlaybackError
                ) {
                    latestError = error
                }

                override fun onSkipToNext(generation: Long) = Unit

                override fun onSkipToPrevious(generation: Long) = Unit
            }

        try {
            runBlocking {
                engine.loadPaused(
                    PlayableTrack(
                        id = "generated-wav-progress",
                        title = "Generated WAV Progress",
                        artist = "Test",
                        album = null,
                        durationMillis = null,
                        source = AudioSource.FilePath(wavPath.toString()),
                    ),
                    generation = 2L,
                )
            }
            engine.setUserTransportEnabled(true)
            engine.play(1L)
            engine.seekTo(100L)

            assertTrue(
                progressLatch.await(1, TimeUnit.SECONDS),
                "Expected periodic playback progress events while playing")
            assertEquals(null, latestError)
            assertTrue(progressPositions.maxOrNull()!! > 0L)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun missingMacPathMapsToMissingFile() {
        val absent = Files.createTempFile("rhythhaus-mac-absent", ".wav")
        Files.deleteIfExists(absent)
        val engine = createJvmPlaybackEngine()
        try {
            val failure =
                assertFailsWith<PlaybackFailureException> {
                    runBlocking {
                        engine.loadPaused(
                            PlayableTrack(
                                id = "missing-mac",
                                title = "Missing Mac",
                                artist = "Test",
                                album = null,
                                durationMillis = null,
                                source =
                                    AudioSource.FilePath(absent.toString()),
                            ),
                            generation = 90L,
                        )
                    }
                }

            assertEquals(PlaybackFailureKind.MissingFile, failure.error.kind)
        } finally {
            engine.release()
            Files.deleteIfExists(absent)
        }
    }

    @Test
    fun opaqueMacNativeLoadFailureRemainsUnknown() {
        // An existing but empty file is a valid managed path that the native
        // macOS audio player cannot decode, so the bridge reports a bare
        // Boolean failure with no reliable native reason.
        val undecodable = Files.createTempFile("rhythhaus-mac-opaque", ".wav")
        val engine = createJvmPlaybackEngine()
        try {
            val failure =
                assertFailsWith<PlaybackFailureException> {
                    runBlocking {
                        engine.loadPaused(
                            PlayableTrack(
                                id = "opaque-mac",
                                title = "Opaque Mac",
                                artist = "Test",
                                album = null,
                                durationMillis = null,
                                source =
                                    AudioSource.FilePath(
                                        undecodable.toString()),
                            ),
                            generation = 91L,
                        )
                    }
                }

            assertEquals(PlaybackFailureKind.Unknown, failure.error.kind)
        } finally {
            engine.release()
            Files.deleteIfExists(undecodable)
        }
    }

    @Test
    fun nativeMacControllerRecoversWhenPlayingFileBecomesUnavailable() =
        runBlocking {
            val unavailablePath = createSilentWavFile(durationMillis = 2_000)
            val successorPath = createSilentWavFile(durationMillis = 2_000)
            val tailPath = createSilentWavFile(durationMillis = 2_000)
            val engine = createJvmPlaybackEngine()
            val controller = PlaybackController(engine)
            val unavailable =
                QueueOccurrence(
                    id = "unavailable-occurrence",
                    track =
                        PlayableTrack(
                            id = "unavailable-track",
                            title = "Unavailable Track",
                            artist = "Test",
                            album = null,
                            durationMillis = null,
                            source =
                                AudioSource.FilePath(
                                    unavailablePath.toString()),
                        ),
                )
            val successor =
                QueueOccurrence(
                    id = "successor-occurrence",
                    track =
                        PlayableTrack(
                            id = "successor-track",
                            title = "Successor Track",
                            artist = "Test",
                            album = null,
                            durationMillis = null,
                            source =
                                AudioSource.FilePath(successorPath.toString()),
                        ),
                )
            val duplicateUnavailable =
                QueueOccurrence(
                    id = "duplicate-unavailable-occurrence",
                    track = unavailable.track,
                )
            val tail =
                QueueOccurrence(
                    id = "tail-occurrence",
                    track =
                        PlayableTrack(
                            id = "tail-track",
                            title = "Tail Track",
                            artist = "Test",
                            album = null,
                            durationMillis = null,
                            source = AudioSource.FilePath(tailPath.toString()),
                        ),
                )

            try {
                controller.setOccurrenceQueue(
                    listOf(
                        unavailable,
                        successor,
                        duplicateUnavailable,
                        tail,
                    ),
                    unavailable.id,
                )
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Paused &&
                        it.currentOccurrenceId == unavailable.id
                }
                controller.play()
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Playing &&
                        it.currentOccurrenceId == unavailable.id
                }

                assertTrue(unavailablePath.deleteIfExists())
                controller.selectOccurrence(unavailable.id, autoPlay = true)
                val firstError =
                    awaitPlaybackState(controller) {
                        it.status == PlaybackStatus.Error &&
                            it.error?.kind == PlaybackFailureKind.MissingFile &&
                            it.currentOccurrenceId == unavailable.id
                    }

                controller.retryFailedTrack()
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Error &&
                        it.error?.kind == PlaybackFailureKind.MissingFile &&
                        it.currentOccurrenceId == unavailable.id &&
                        it.engineGeneration != firstError.engineGeneration
                }

                controller.skipFailedTrack()
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Playing &&
                        it.currentOccurrenceId == successor.id &&
                        it.error == null
                }

                controller.selectOccurrence(tail.id, autoPlay = true)
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Playing &&
                        it.currentOccurrenceId == tail.id
                }
                assertTrue(tailPath.deleteIfExists())
                controller.selectOccurrence(tail.id, autoPlay = true)
                val tailError =
                    awaitPlaybackState(controller) {
                        it.status == PlaybackStatus.Error &&
                            it.error?.kind == PlaybackFailureKind.MissingFile &&
                            it.currentOccurrenceId == tail.id
                    }
                controller.skipFailedTrack()
                assertEquals(tailError, controller.state.value)

                controller.selectOccurrence(unavailable.id, autoPlay = true)
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Error &&
                        it.currentOccurrenceId == unavailable.id
                }
                assertEquals(
                    QueueMutationResult.Applied,
                    controller.removeFailedTrack(),
                )
                val recovered =
                    awaitPlaybackState(controller) {
                        it.status == PlaybackStatus.Playing &&
                            it.currentOccurrenceId == successor.id
                    }

                assertEquals(
                    listOf(successor.id, duplicateUnavailable.id, tail.id),
                    recovered.queue.map { it.id },
                )
                assertEquals(
                    unavailable.track.id,
                    recovered.queue
                        .single {
                            it.id == duplicateUnavailable.id
                        }
                        .track
                        .id,
                )
                assertEquals(null, recovered.error)
                assertTrue(Files.exists(successorPath))
            } finally {
                controller.release()
                unavailablePath.deleteIfExists()
                successorPath.deleteIfExists()
                tailPath.deleteIfExists()
            }
        }

    @Test
    fun controllerAutoAdvancesToNextTrackOnCompletion() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val track1 =
            PlayableTrack(
                id = "track-1",
                title = "First Track",
                artist = "Test Artist",
                album = "Test Album",
                durationMillis = 1000L,
                source = AudioSource.FilePath("/tmp/track1.mp3"),
            )
        val track2 =
            PlayableTrack(
                id = "track-2",
                title = "Second Track",
                artist = "Test Artist",
                album = "Test Album",
                durationMillis = 2000L,
                source = AudioSource.FilePath("/tmp/track2.mp3"),
            )
        controller.setQueue(listOf(track1, track2), selectedTrackId = "track-1")
        assertTrue(awaitPlaybackStatus(controller, PlaybackStatus.Paused))
        controller.play()
        assertTrue(awaitPlaybackStatus(controller, PlaybackStatus.Playing))
        assertEquals("track-1", controller.state.value.currentTrack?.id)

        engine.complete()
        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertTrue(
            awaitPlaybackStatus(controller, PlaybackStatus.Playing),
            "Expected controller to auto-play the next track")
        assertFalse(engine.released)
    }

    @Test
    fun controllerStopsWhenLastTrackCompletes() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val track =
            PlayableTrack(
                id = "track-1",
                title = "Only Track",
                artist = "Test Artist",
                album = null,
                durationMillis = 1000L,
                source = AudioSource.FilePath("/tmp/track1.mp3"),
            )
        controller.setQueue(listOf(track), selectedTrackId = "track-1")
        assertTrue(awaitPlaybackStatus(controller, PlaybackStatus.Paused))
        controller.play()
        assertTrue(awaitPlaybackStatus(controller, PlaybackStatus.Playing))
        engine.complete()
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
    }

    @Test
    fun controllerSetQueueDoesNotBlockCallerWhileEngineLoads() {
        val loadStarted = CountDownLatch(1)
        val releaseLoad = CountDownLatch(1)
        val engine =
            object : PlatformPlaybackEngine {
                override var listener: PlaybackEngineListener? = null

                override suspend fun loadPaused(
                    track: PlayableTrack,
                    generation: Long
                ): LoadedPlayback {
                    loadStarted.countDown()
                    assertTrue(
                        releaseLoad.await(1, TimeUnit.SECONDS),
                        "Test timed out waiting to release fake load")
                    listener?.onPlaybackProgress(
                        generation, 0L, track.durationMillis)
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Paused)
                    return LoadedPlayback(generation, track.durationMillis)
                }

                override fun play(playbackRunId: Long) {
                    listener?.onPlaybackStatus(1L, PlaybackStatus.Playing)
                }

                override fun pause() = Unit

                override fun stop() = Unit

                override fun seekTo(positionMillis: Long) = Unit

                override fun clear(generation: Long) = Unit

                override fun setUserTransportEnabled(enabled: Boolean) = Unit

                override fun setPlaybackGain(gain: Float) = Unit

                override fun release() = Unit
            }
        val controller = PlaybackController(engine)
        val track =
            PlayableTrack(
                id = "blocking-track",
                title = "Blocking Track",
                artist = "Test Artist",
                album = null,
                durationMillis = 1000L,
                source = AudioSource.FilePath("/tmp/blocking-track.mp3"),
            )
        val executor = Executors.newSingleThreadExecutor()
        var blockedCaller = false

        try {
            val future = executor.submit {
                controller.setQueue(listOf(track), selectedTrackId = track.id)
            }
            assertTrue(
                loadStarted.await(1, TimeUnit.SECONDS),
                "Expected fake engine load to start")
            try {
                future.get(100, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                blockedCaller = true
            }
            assertFalse(
                blockedCaller,
                "setQueue should return without waiting for backend load to finish")
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)
        } finally {
            releaseLoad.countDown()
            executor.shutdownNow()
            controller.release()
        }
    }

    @Test
    fun macTransportGateSurvivesResetAndRejectsRemoteActionsUntilReenabled() =
        runBlocking {
            val wavPath = createSilentWavFile(durationMillis = 500)
            val bridge = MacAudioPlayerBridge()
            val controller =
                PlaybackController(MacOSNativePlaybackEngine(bridge))
            try {
                controller.setQueue(listOf(macWavTrack("remote-gate", wavPath)))
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Paused
                }
                controller.setCommandsEnabled(false)

                assertFalse(bridge.invokeRemotePlayForTest())
                assertFalse(bridge.invokeRemoteSeekForTest(200L))
                assertFalse(bridge.isPlayingForTest())
                assertEquals(0L, bridge.currentPositionMillis())

                controller.setCommandsEnabled(true)
                assertTrue(bridge.invokeRemoteSeekForTest(200L))
                awaitPlaybackState(controller) { it.positionMillis >= 150L }
                assertTrue(bridge.invokeRemotePlayForTest())
                awaitPlaybackState(controller) {
                    it.status == PlaybackStatus.Playing
                }
                Unit
            } finally {
                controller.release()
                wavPath.deleteIfExists()
            }
        }

    @Test
    fun macRemotePlayAfterPausedLoadClaimsControllerRunAndCountsNaturalCompletion() =
        runBlocking {
            val wavPath = createSilentWavFile(durationMillis = 350)
            val bridge = MacAudioPlayerBridge()
            val controller =
                PlaybackController(MacOSNativePlaybackEngine(bridge))
            try {
                controller.setQueue(
                    listOf(
                        PlayableTrack(
                            id = "remote-run",
                            title = "Remote run",
                            artist = "Test artist",
                            album = null,
                            durationMillis = 350L,
                            source = AudioSource.FilePath(wavPath.toString()),
                        ),
                    ),
                    selectedTrackId = "remote-run",
                )
                withTimeout(5_000) {
                    while (controller.state.value.status !=
                        PlaybackStatus.Paused) yield()
                }
                controller.armSleepTimerAfterCompletions(1, false)
                assertTrue(bridge.invokeRemotePlayForTest())
                withTimeout(5_000) {
                    while (controller.state.value.status !=
                        PlaybackStatus.Stopped ||
                        controller.sleepTimerState.value.mode != null) yield()
                }
                withTimeout(5_000) {
                    while (bridge.isPlayingForTest()) yield()
                }
            } finally {
                controller.release()
                wavPath.deleteIfExists()
            }
        }

    @Test
    fun staleMacProgressPublicationIsRejectedAfterSourceReplacement() {
        val events = mutableListOf<Long>()
        val publication = MacProgressPublicationGate()
        publication.activate(generation = 70L, sourceVersion = 1L)
        val callbackPassedInitialCheck = CyclicBarrier(2)
        val allowCallbackToPublish = CyclicBarrier(2)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val oldCallback = executor.submit {
                publication.publish(
                    generation = 70L,
                    sourceVersion = 1L,
                    beforeEmit = {
                        callbackPassedInitialCheck.await(1, TimeUnit.SECONDS)
                        allowCallbackToPublish.await(1, TimeUnit.SECONDS)
                    },
                    emitProgress = { events += it },
                    emitCompletion = { events += it },
                )
            }
            callbackPassedInitialCheck.await(1, TimeUnit.SECONDS)
            publication.activate(generation = 71L, sourceVersion = 2L)
            allowCallbackToPublish.await(1, TimeUnit.SECONDS)
            oldCallback.get(1, TimeUnit.SECONDS)

            assertEquals(emptyList(), events)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createSilentWavFile(durationMillis: Int = 100) =
        createTempFile(prefix = "rhythhaus-silence", suffix = ".wav").also {
            path ->
            val sampleRate = 8_000
            val sampleCount = sampleRate * durationMillis / 1_000
            val dataSize = sampleCount * 2
            val buffer =
                ByteBuffer.allocate(44 + dataSize)
                    .order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
            buffer.putInt(36 + dataSize)
            buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
            buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
            buffer.putInt(16)
            buffer.putShort(1) // PCM
            buffer.putShort(1) // mono
            buffer.putInt(sampleRate)
            buffer.putInt(sampleRate * 2)
            buffer.putShort(2)
            buffer.putShort(16)
            buffer.put("data".toByteArray(Charsets.US_ASCII))
            buffer.putInt(dataSize)
            repeat(sampleCount) { buffer.putShort(0) }
            Files.write(path, buffer.array())
        }

    private fun wavSha256(path: Path): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))

    private fun nativeHandle(bridge: MacAudioPlayerBridge): Long =
        nativeHandleField.getLong(bridge)

    // Current/outgoing/prepared positions, current/outgoing playing, live
    // voices.
    private fun crossfadeSnapshot(bridge: MacAudioPlayerBridge): LongArray =
        bridge.withLifetimeBoundaryForTest {
            nativeCrossfadeSnapshotForTest(nativeHandle(bridge))
        }

    // Current then outgoing, or prepared when promotion has not happened.
    private fun crossfadeVolumes(bridge: MacAudioPlayerBridge): FloatArray =
        bridge.withLifetimeBoundaryForTest {
            nativeCrossfadeVolumesForTest(nativeHandle(bridge))
        }

    private fun captureMacTerminalCallback(bridge: MacAudioPlayerBridge): Long =
        bridge.withLifetimeBoundaryForTest {
            nativeCaptureTerminalCallbackForTest(nativeHandle(bridge)).also {
                assertTrue(it != 0L)
            }
        }

    private suspend fun awaitCrossfadeSnapshot(
        bridge: MacAudioPlayerBridge,
        predicate: (LongArray) -> Boolean,
    ): LongArray =
        withTimeout(5_000) {
            while (true) {
                val snapshot = crossfadeSnapshot(bridge)
                if (predicate(snapshot)) return@withTimeout snapshot
                delay(10)
            }
            error("Unreachable native crossfade wait")
        }

    private fun macWavTrack(id: String, wavPath: Path): PlayableTrack =
        PlayableTrack(
            id = id,
            title = id,
            artist = "Test",
            album = null,
            durationMillis = null,
            source = AudioSource.FilePath(wavPath.toString()),
        )

    private fun awaitPlaybackStatus(
        controller: PlaybackController,
        status: PlaybackStatus
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (System.nanoTime() < deadline) {
            if (controller.state.value.status == status) return true
            Thread.sleep(10)
        }
        return controller.state.value.status == status
    }

    private suspend fun awaitPlaybackState(
        controller: PlaybackController,
        predicate: (PlaybackState) -> Boolean,
    ): PlaybackState =
        withTimeout(5_000) {
            while (true) {
                val state = controller.state.value
                if (predicate(state)) return@withTimeout state
                yield()
            }
            error("Unreachable playback-state wait")
        }

    private companion object {
        val nativeHandleField =
            MacAudioPlayerBridge::class.java.getDeclaredField("handle").apply {
                isAccessible = true
            }

        @JvmStatic
        private external fun nativeInvalidateCurrentPlayerForTest(handle: Long)

        @JvmStatic
        private external fun nativeInvalidatePreparedPlayerForTest(handle: Long)

        @JvmStatic
        private external fun nativeCrossfadeSnapshotForTest(
            handle: Long
        ): LongArray

        @JvmStatic
        private external fun nativeCrossfadeVolumesForTest(
            handle: Long
        ): FloatArray

        @JvmStatic private external fun nativeLiveAudioVoiceCountForTest(): Long

        @JvmStatic
        private external fun nativeCaptureTerminalCallbackForTest(
            handle: Long
        ): Long

        @JvmStatic
        private external fun nativeDeliverCapturedTerminalCallbackForTest(
            callback: Long,
            completed: Boolean,
        )

        @JvmStatic
        private external fun nativeReleaseOnRouteQueueForTest(
            handle: Long
        ): Boolean
    }
}

private open class RecordingMacPlaybackListener :
    RejectingRemoteTransportListener() {
    val statuses =
        java.util.concurrent.CopyOnWriteArrayList<Pair<Long, PlaybackStatus>>()
    val progress = java.util.concurrent.CopyOnWriteArrayList<Pair<Long, Long>>()
    val completions =
        java.util.concurrent.CopyOnWriteArrayList<Pair<Long, Long>>()
    val errors =
        java.util.concurrent.CopyOnWriteArrayList<Pair<Long, PlaybackError>>()

    override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
        statuses += generation to status
    }

    override fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?
    ) {
        progress += generation to positionMillis
    }

    override fun onPlaybackCompleted(generation: Long, playbackRunId: Long) {
        completions += generation to playbackRunId
    }

    override fun onPlaybackError(generation: Long, error: PlaybackError) {
        errors += generation to error
    }

    override fun onSkipToNext(generation: Long) = Unit

    override fun onSkipToPrevious(generation: Long) = Unit
}

private abstract class RejectingRemoteTransportListener :
    PlaybackEngineListener {
    override fun onPlayRequested(generation: Long): Unit =
        error("Unexpected external play")

    override fun onPauseRequested(generation: Long): Unit =
        error("Unexpected external pause")

    override fun onStopRequested(generation: Long): Unit =
        error("Unexpected external stop")

    override fun onSeekRequested(generation: Long, positionMillis: Long): Unit =
        error("Unexpected external seek")
}
