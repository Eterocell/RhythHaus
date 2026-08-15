package com.eterocell.rhythhaus

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
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
import kotlinx.coroutines.runBlocking

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
        val handleField = bridge.javaClass.getDeclaredField("handle").apply { isAccessible = true }
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
            assertTrue(bridge.simulateRouteSnapshotForTest(longArrayOf(10L), 10L))
            bridge.pause()
            assertTrue(bridge.simulateRouteSnapshotForTest(longArrayOf(20L), 20L))
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
            object : PlaybackEngineListener {
                override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
                    synchronized(statuses) {
                        statuses += status
                        if (status == PlaybackStatus.Paused && PlaybackStatus.Playing in statuses) {
                            pausedLatch.countDown()
                        }
                    }
                }

                override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) = Unit
                override fun onPlaybackCompleted(generation: Long) = Unit
                override fun onPlaybackError(generation: Long, error: PlaybackError) = Unit
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
            engine.play()
            engine.seekTo(200L)
            assertTrue(bridge.invokeRouteDisconnectForTest())
            assertTrue(pausedLatch.await(1, TimeUnit.SECONDS))
            synchronized(statuses) {
                val playingIndex = statuses.indexOf(PlaybackStatus.Playing)
                assertEquals(1, statuses.drop(playingIndex + 1).count { it == PlaybackStatus.Paused })
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
            object : PlaybackEngineListener {
                override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
                    synchronized(statuses) { statuses += generation to status }
                }

                override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) {
                    if (generation == 3L && replaceOnRouteLossProgress.get() && routeLossProgress.count == 1L) {
                        routeLossProgress.countDown()
                        runBlocking {
                            engine.loadPaused(
                                PlayableTrack(
                                    id = "replacement",
                                    title = "Replacement",
                                    artist = "Test",
                                    album = null,
                                    durationMillis = null,
                                    source = AudioSource.FilePath(secondWavPath.toString()),
                                ),
                                generation = 4L,
                            )
                        }
                        replaced.countDown()
                    }
                }

                override fun onPlaybackCompleted(generation: Long) = Unit
                override fun onPlaybackError(generation: Long, error: PlaybackError) = Unit
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
            engine.play()
            engine.pauseProgressUpdatesForTest()
            replaceOnRouteLossProgress.set(true)
            assertTrue(bridge.invokeRouteDisconnectForTest())
            engine.publishProgressForTest()
            assertTrue(routeLossProgress.await(1, TimeUnit.SECONDS))
            assertTrue(replaced.await(1, TimeUnit.SECONDS))
            synchronized(statuses) {
                assertFalse(statuses.dropWhile { it != (3L to PlaybackStatus.Playing) }
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
            object : PlaybackEngineListener {
                override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
                    synchronized(events) { events += generation to status.name }
                    if (generation == 3L && status == PlaybackStatus.Loading && !replacing) {
                        replacing = true
                        runBlocking {
                            engine.loadPaused(
                                PlayableTrack(
                                    id = "replacement",
                                    title = "Replacement",
                                    artist = "Test",
                                    album = null,
                                    durationMillis = null,
                                    source = AudioSource.FilePath(secondWavPath.toString()),
                                ),
                                generation = 4L,
                            )
                        }
                    }
                }

                override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) {
                    synchronized(events) { events += generation to "Progress" }
                }

                override fun onPlaybackCompleted(generation: Long) = Unit
                override fun onPlaybackError(generation: Long, error: PlaybackError) = Unit
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
                assertFalse(events.dropWhile { it != (3L to PlaybackStatus.Loading.name) }
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
            object : PlaybackEngineListener {
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

                override fun onPlaybackCompleted(generation: Long) = Unit

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
            engine.play()
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
            object : PlaybackEngineListener {
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

                override fun onPlaybackCompleted(generation: Long) = Unit

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
            engine.play()
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
            assertTrue(
                bridge.invokeRemotePlayForTest() || bridge.isPlayingForTest())
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
    private companion object {
        @JvmStatic
        private external fun nativeReleaseOnRouteQueueForTest(handle: Long): Boolean
    }
}
