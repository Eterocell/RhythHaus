package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
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

    @Test
    fun staleGenerationCallbacksCannotMutateCurrentPlayback() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(testTracks(2), selectedTrackId = "track-1")
        engine.awaitLoad()
        val firstGeneration = engine.activeGeneration

        controller.selectTrack("track-2", autoPlay = false)
        engine.awaitLoadCount(2)
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
        data object Play : EngineEvent
        data object Pause : EngineEvent
    }

    private class RecordingPlaybackEngine(
        private val loadGate: CompletableDeferred<Unit>? = null,
        private val seekGate: CompletableDeferred<Unit>? = null,
    ) : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        val loadedTracks = mutableListOf<PlayableTrack>()
        val loadedGenerations = mutableListOf<Long>()
        var activeGeneration: Long = 0L
            private set
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
            listener?.onPlaybackProgress(generation, 0L, track.durationMillis)
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            loadSignal.complete(Unit)
            return LoadedPlayback(generation, track.durationMillis)
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

        override fun stop() = Unit

        override fun seekTo(positionMillis: Long) {
            seekStarted.complete(Unit)
            seekGate?.let { runBlocking { it.await() } }
            record(EngineEvent.Seek(positionMillis))
            listener?.onPlaybackProgress(activeGeneration, positionMillis, loadedTracks.lastOrNull()?.durationMillis)
        }

        override fun clear(generation: Long) {
            activeGeneration = generation
        }

        override fun setUserTransportEnabled(enabled: Boolean) = Unit

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
}
