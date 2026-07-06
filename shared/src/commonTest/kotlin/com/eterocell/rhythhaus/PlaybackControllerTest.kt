package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

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
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        val tracks = testTracks(3)
        controller.setQueue(tracks, selectedTrackId = "track-1")
        controller.play()

        engine.complete()
        assertEquals("track-2", controller.state.value.currentTrack?.id)

        engine.complete()
        assertEquals("track-3", controller.state.value.currentTrack?.id)

        engine.complete()
        assertEquals("track-3", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
        assertEquals(3_000L, controller.state.value.positionMillis)
    }

    @Test
    fun stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing() {
        val engine = FakePlaybackEngine()
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
        val engine = FakePlaybackEngine()
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
        val engine = FakePlaybackEngine()
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
        val engine = FakePlaybackEngine()
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
        val engine = FakePlaybackEngine()
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
        val engine = FakePlaybackEngine()
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
}
