package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        assertFailsWith<IllegalStateException> {
            runBlocking {
                engine.loadPaused(testTrack("failed"), generation = 42L)
            }
        }
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
        val firstCompletion = provider.completionHandler
        val firstInterruption = provider.interruptionHandler
        runBlocking { engine.loadPaused(testTrack("second"), generation = 44L) }

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
        assertFailsWith<IllegalArgumentException> { engine.play() }
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
        assertFailsWith<IllegalStateException> {
            runBlocking {
                failingEngine.loadPaused(
                    testTrack("path-failure"), generation = 52L)
            }
        }

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
}

private class FakeIOSAudioPlayerProvider : IOSAudioPlayerProvider {
    override var completionHandler: IOSAudioPlayerCompletionHandler? = null
    override var interruptionHandler: IOSAudioInterruptionHandler? = null
    var playSucceeds = true
    var loadSucceeds = true
    var playCallCount = 0
    var deferPlayStart = false
    var deferLoad = false
    var onFadeOutAndStop: (() -> Unit)? = null
    var onLoad: (() -> Unit)? = null
    var durationThrows = false
    private var positionMillis: Long = 0L
    private var durationMillis: Long? = null
    private var playing = false
    private var pendingPlayStart: IOSAudioPlayerPlaybackStartHandler? = null
    private var pendingLoad: IOSAudioPlayerLoadHandler? = null
    var isLoaded = false

    override fun loadAsync(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler
    ) {
        durationMillis = 1_000L
        positionMillis = 0L
        playing = false
        if (deferLoad) {
            pendingLoad = handler
            return
        }
        completeLoad(filePath, handler)
    }

    private fun completeLoad(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler,
    ) {
        isLoaded = filePath.isNotBlank() && loadSucceeds
        if (isLoaded) handler.onAudioLoaded() else handler.onAudioLoadFailed()
        onLoad?.invoke()
    }

    fun completeDeferredLoad() {
        val handler = requireNotNull(pendingLoad)
        pendingLoad = null
        completeLoad("deferred-load", handler)
    }

    override fun playAsync(handler: IOSAudioPlayerPlaybackStartHandler) {
        playCallCount++
        if (deferPlayStart) {
            pendingPlayStart = handler
            return
        }
        completePlayStart(handler)
    }

    private fun completePlayStart(handler: IOSAudioPlayerPlaybackStartHandler) {
        if (!playSucceeds) {
            handler.onPlaybackStartFailed()
            return
        }
        playing = true
        handler.onPlaybackStarted()
    }

    fun completeDeferredPlayStart() {
        val handler = requireNotNull(pendingPlayStart)
        pendingPlayStart = null
        completePlayStart(handler)
    }

    override fun pause() {
        playing = false
    }

    override fun stop() {
        playing = false
        positionMillis = 0L
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

    override fun fadeOutAndStop(
        fadeDurationSeconds: Double,
        silentVolume: Float
    ) {
        onFadeOutAndStop?.invoke()
        playing = false
        positionMillis = 0L
    }

    fun simulateNativeCompletion() {
        playing = false
        completionHandler?.onPlaybackCompleted()
    }

    fun simulateInterruptionBegan() = interruptionHandler?.onInterruptionBegan()

    fun simulateInterruptionEnded(shouldResume: Boolean) =
        interruptionHandler?.onInterruptionEnded(shouldResume)

    fun simulateRouteDisconnected() = interruptionHandler?.onRouteDisconnected()
}

private class RecordingListener : PlaybackEngineListener {
    val statuses = mutableListOf<PlaybackStatus>()
    var completionCount = 0
    var progressCount = 0

    override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) {
        statuses += status
    }

    override fun onPlaybackProgress(
        generation: Long,
        positionMillis: Long,
        durationMillis: Long?,
    ) {
        progressCount++
    }

    override fun onPlaybackCompleted(generation: Long) {
        completionCount++
    }

    override fun onPlaybackError(generation: Long, error: PlaybackError) = Unit

    override fun onSkipToNext(generation: Long) = Unit

    override fun onSkipToPrevious(generation: Long) = Unit
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
    session.engine.play()
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

private fun testResolver() =
    object : IOSRelativeFilePathResolver {
        override fun resolve(relativePath: String): String = relativePath
    }
