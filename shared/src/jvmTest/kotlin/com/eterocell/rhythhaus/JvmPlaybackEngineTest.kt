package com.eterocell.rhythhaus

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CyclicBarrier

class JvmPlaybackEngineTest {
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
    fun inFlightBridgeOperationBlocksResetAndKeepsOneHandleIdentity() {
        val bridge = MacAudioPlayerBridge()
        val operationEntered = CountDownLatch(1)
        val releaseOperation = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val originalHandle = bridge.currentHandleIdentityForTest()
            val operation = executor.submit<Long> {
                bridge.withLifetimeBoundaryForTest { ownedHandle ->
                    operationEntered.countDown()
                    assertTrue(releaseOperation.await(1, TimeUnit.SECONDS))
                    ownedHandle
                }
            }
            assertTrue(operationEntered.await(1, TimeUnit.SECONDS))
            val reset = executor.submit { bridge.resetPlayer() }

            assertFailsWith<TimeoutException> { reset.get(100, TimeUnit.MILLISECONDS) }
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

            assertFailsWith<TimeoutException> { release.get(100, TimeUnit.MILLISECONDS) }
            releaseOperation.countDown()
            operation.get(1, TimeUnit.SECONDS)
            release.get(1, TimeUnit.SECONDS)

            assertEquals(0L, bridge.currentHandleIdentityForTest())
            assertFailsWith<IllegalArgumentException> { bridge.currentPositionMillis() }
            assertFailsWith<IllegalArgumentException> { bridge.withLifetimeBoundaryForTest { } }
        } finally {
            releaseOperation.countDown()
            executor.shutdownNow()
            bridge.releasePlayer()
        }
    }

    @Test
    fun nativeMacPlaybackEngineLoadsGeneratedWavFile() {
        val wavPath = createSilentWavFile()
        val engine = createPlatformPlaybackEngine()
        val events = mutableListOf<PlaybackStatus>()
        var latestDuration: Long? = null
        var latestError: PlaybackError? = null
        engine.listener = object : PlaybackEngineListener {
            override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
                events += status
            }

            override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) {
                latestDuration = durationMillis
            }

            override fun onPlaybackCompleted(generation: Long) = Unit

            override fun onPlaybackError(generation: Long, error: PlaybackError) {
                latestError = error
            }

            override fun onSkipToNext(generation: Long) = Unit

            override fun onSkipToPrevious(generation: Long) = Unit
        }

        try {
            runBlocking { engine.loadPaused(
                PlayableTrack(
                    id = "generated-wav",
                    title = "Generated WAV",
                    artist = "Test",
                    album = null,
                    durationMillis = null,
                    source = AudioSource.FilePath(wavPath.toString()),
                ),
                generation = 1L,
            ) }
            engine.setUserTransportEnabled(true)
            engine.play()
            engine.pause()
            engine.seekTo(10L)
            engine.stop()

            assertEquals(null, latestError)
            assertTrue(PlaybackStatus.Loading in events)
            assertTrue(PlaybackStatus.Paused in events)
            assertTrue(PlaybackStatus.Playing in events)
            assertTrue(PlaybackStatus.Stopped in events)
            assertNotNull(latestDuration)
            assertTrue(latestDuration!! > 0L)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun nativeMacPlaybackEnginePublishesProgressWhilePlaying() {
        val wavPath = createSilentWavFile(durationMillis = 800)
        val engine = createPlatformPlaybackEngine()
        val progressPositions = mutableListOf<Long>()
        val progressLatch = CountDownLatch(1)
        var latestError: PlaybackError? = null
        engine.listener = object : PlaybackEngineListener {
            override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) = Unit

            override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) {
                if (positionMillis > 0L) {
                    progressPositions += positionMillis
                    progressLatch.countDown()
                }
            }

            override fun onPlaybackCompleted(generation: Long) = Unit

            override fun onPlaybackError(generation: Long, error: PlaybackError) {
                latestError = error
            }

            override fun onSkipToNext(generation: Long) = Unit

            override fun onSkipToPrevious(generation: Long) = Unit
        }

        try {
            runBlocking { engine.loadPaused(
                PlayableTrack(
                    id = "generated-wav-progress",
                    title = "Generated WAV Progress",
                    artist = "Test",
                    album = null,
                    durationMillis = null,
                    source = AudioSource.FilePath(wavPath.toString()),
                ),
                generation = 2L,
            ) }
            engine.setUserTransportEnabled(true)
            engine.play()
            engine.seekTo(100L)

            assertTrue(progressLatch.await(1, TimeUnit.SECONDS), "Expected periodic playback progress events while playing")
            assertEquals(null, latestError)
            assertTrue(progressPositions.maxOrNull()!! > 0L)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun controllerAutoAdvancesToNextTrackOnCompletion() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val track1 = PlayableTrack(
            id = "track-1",
            title = "First Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMillis = 1000L,
            source = AudioSource.FilePath("/tmp/track1.mp3"),
        )
        val track2 = PlayableTrack(
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
        assertTrue(awaitPlaybackStatus(controller, PlaybackStatus.Playing), "Expected controller to auto-play the next track")
        assertFalse(engine.released)
    }

    @Test
    fun controllerStopsWhenLastTrackCompletes() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val track = PlayableTrack(
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
        val engine = object : PlatformPlaybackEngine {
            override var listener: PlaybackEngineListener? = null

            override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
                loadStarted.countDown()
                assertTrue(releaseLoad.await(1, TimeUnit.SECONDS), "Test timed out waiting to release fake load")
                listener?.onPlaybackProgress(generation, 0L, track.durationMillis)
                listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
                return LoadedPlayback(generation, track.durationMillis)
            }

            override fun play() {
                listener?.onPlaybackStatus(1L, PlaybackStatus.Playing)
            }

            override fun pause() = Unit
            override fun stop() = Unit
            override fun seekTo(positionMillis: Long) = Unit
            override fun clear(generation: Long) = Unit
            override fun setUserTransportEnabled(enabled: Boolean) = Unit
            override fun release() = Unit
        }
        val controller = PlaybackController(engine)
        val track = PlayableTrack(
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
            assertTrue(loadStarted.await(1, TimeUnit.SECONDS), "Expected fake engine load to start")
            try {
                future.get(100, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                blockedCaller = true
            }
            assertFalse(blockedCaller, "setQueue should return without waiting for backend load to finish")
            assertEquals(PlaybackStatus.Loading, controller.state.value.status)
        } finally {
            releaseLoad.countDown()
            executor.shutdownNow()
            controller.release()
        }
    }

    @Test
    fun macTransportGateSurvivesResetAndRejectsRemoteActionsUntilReenabled() {
        val wavPath = createSilentWavFile(durationMillis = 500)
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.setTransportEnabled(false)
            bridge.resetPlayer()
            assertTrue(bridge.load(wavPath.toString()))
            bridge.registerNowPlayingRemoteCommands()

            assertFalse(bridge.invokeRemotePlayForTest())
            assertFalse(bridge.invokeRemoteSeekForTest(200L))
            assertFalse(bridge.isPlayingForTest())
            assertEquals(0L, bridge.currentPositionMillis())

            bridge.setTransportEnabled(true)
            assertTrue(bridge.invokeRemoteSeekForTest(200L))
            assertTrue(bridge.currentPositionMillis() >= 150L)
            assertTrue(bridge.invokeRemotePlayForTest() || bridge.isPlayingForTest())
        } finally {
            bridge.releasePlayer()
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

    @Test
    fun nativeRemoteOperationsShareProductionGateForAllCommands() {
        val wavPath = createSilentWavFile(durationMillis = 500)
        val bridge = MacAudioPlayerBridge()
        try {
            assertTrue(bridge.load(wavPath.toString()))
            bridge.setTransportEnabled(false)

            assertFalse(bridge.invokeRemotePlayForTest())
            assertFalse(bridge.invokeRemotePauseForTest())
            assertFalse(bridge.invokeRemoteToggleForTest())
            assertFalse(bridge.invokeRemoteStopForTest())
            assertFalse(bridge.invokeRemoteSeekForTest(200L))
            assertEquals(0L, bridge.currentPositionMillis())

            bridge.setTransportEnabled(true)
            assertTrue(bridge.invokeRemoteSeekForTest(200L))
            assertTrue(bridge.invokeRemotePlayForTest())
            assertTrue(bridge.invokeRemotePauseForTest())
            assertTrue(bridge.invokeRemoteToggleForTest())
            assertTrue(bridge.invokeRemoteStopForTest())
            assertEquals(0L, bridge.currentPositionMillis())
        } finally {
            bridge.releasePlayer()
            wavPath.deleteIfExists()
        }
    }

    private fun createSilentWavFile(durationMillis: Int = 100) = createTempFile(prefix = "rhythhaus-silence", suffix = ".wav").also { path ->
        val sampleRate = 8_000
        val sampleCount = sampleRate * durationMillis / 1_000
        val dataSize = sampleCount * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
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
    private fun awaitPlaybackStatus(controller: PlaybackController, status: PlaybackStatus): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (System.nanoTime() < deadline) {
            if (controller.state.value.status == status) return true
            Thread.sleep(10)
        }
        return controller.state.value.status == status
    }
}
