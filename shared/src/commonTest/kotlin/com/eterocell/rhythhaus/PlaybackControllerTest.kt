package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class PlaybackControllerTest {
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
    fun stopAfterQueueAdvancesMiddleTrackAndStopsAtFinalTrackEnd() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.play()

        engine.listener?.onPlaybackCompleted()
        assertEquals("track-2", controller.state.value.currentTrack?.id)

        engine.listener?.onPlaybackCompleted()
        assertEquals("track-3", controller.state.value.currentTrack?.id)

        engine.listener?.onPlaybackCompleted()
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

        engine.listener?.onPlaybackCompleted()

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

        engine.listener?.onPlaybackCompleted()
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

        engine.listener?.onPlaybackCompleted()
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
            shuffleOrderFactory = { _, currentId -> listOf(currentId!!, "track-3", "track-1") },
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
            shuffleOrderFactory = { _, currentId -> listOf(currentId!!, "track-3", "track-1") },
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
        assertEquals(listOf("track-3", "track-4", "track-2", "track-1"), generatedOrders.last())
    }

    @Test
    fun autoAdvanceRemainsLoadingUntilEngineReportsPlaying() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")

        engine.listener?.onPlaybackCompleted()

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Loading, controller.state.value.status)

        engine.listener?.onPlaybackStatus(PlaybackStatus.Paused)
        assertEquals(PlaybackStatus.Paused, controller.state.value.status)

        engine.listener?.onPlaybackStatus(PlaybackStatus.Playing)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
    }

    @Test
    fun manualSkipRemainsLoadingUntilEngineReportsPlaying() {
        val engine = DelayedStatusPlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(2)
        controller.setQueue(tracks, selectedTrackId = "track-1")

        controller.skipToNext()

        assertEquals("track-2", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Loading, controller.state.value.status)

        engine.listener?.onPlaybackStatus(PlaybackStatus.Playing)
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

        assertEquals(tracks.map { it.id }, controller.state.value.queue.map { it.id })
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

    private suspend fun loadedController(
        engine: RecordingPlaybackEngine,
        status: PlaybackStatus,
    ): PlaybackController {
        val controller = PlaybackController(engine)
        val track = testTracks(1).single()
        controller.setQueue(listOf(track), selectedTrackId = track.id)
        engine.awaitLoad()
        engine.listener?.onPlaybackProgress(500L, track.durationMillis)
        if (status == PlaybackStatus.Error) {
            engine.listener?.onPlaybackError(PlaybackError("Test error"))
        } else {
            engine.listener?.onPlaybackStatus(status)
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

        override fun load(track: PlayableTrack) = Unit

        override fun play() = Unit

        override fun pause() = Unit

        override fun stop() = Unit

        override fun seekTo(positionMillis: Long) = Unit

        override fun release() = Unit
    }

    private sealed interface EngineEvent {
        data class Load(val trackId: String) : EngineEvent
        data class Seek(val positionMillis: Long) : EngineEvent
        data object Play : EngineEvent
        data object Pause : EngineEvent
    }

    private class RecordingPlaybackEngine(
        private val loadGate: CompletableDeferred<Unit>? = null,
        private val seekGate: CompletableDeferred<Unit>? = null,
    ) : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val loadedTracks = mutableListOf<PlayableTrack>()
        private val events = Channel<EngineEvent>(Channel.UNLIMITED)
        private val loadStarted = CompletableDeferred<Unit>()
        private val loadSignal = CompletableDeferred<Unit>()
        private val seekStarted = CompletableDeferred<Unit>()

        override fun load(track: PlayableTrack) {
            record(EngineEvent.Load(track.id))
            loadedTracks += track
            loadStarted.complete(Unit)
            loadGate?.let { runBlocking { it.await() } }
            listener?.onPlaybackProgress(0L, track.durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
            loadSignal.complete(Unit)
        }

        suspend fun awaitLoadStarted() = loadStarted.await()

        suspend fun awaitLoad() = loadSignal.await()

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
            listener?.onPlaybackStatus(PlaybackStatus.Playing)
        }

        override fun pause() {
            record(EngineEvent.Pause)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
        }

        override fun stop() = Unit

        override fun seekTo(positionMillis: Long) {
            seekStarted.complete(Unit)
            seekGate?.let { runBlocking { it.await() } }
            record(EngineEvent.Seek(positionMillis))
            listener?.onPlaybackProgress(positionMillis, loadedTracks.lastOrNull()?.durationMillis)
        }

        override fun release() = Unit

        private fun record(event: EngineEvent) {
            check(events.trySend(event).isSuccess)
        }
    }
}
