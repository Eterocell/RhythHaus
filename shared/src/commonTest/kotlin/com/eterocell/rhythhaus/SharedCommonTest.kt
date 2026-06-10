package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun durationFormattingPadsSeconds() {
        assertEquals("4:07", formatDuration(247))
    }

    @Test
    fun durationFormattingClampsNegativeValues() {
        assertEquals("0:00", formatDuration(-12))
    }

    @Test
    fun millisFormattingHandlesUnknownAndPadsSeconds() {
        assertEquals("--:--", formatMillis(null))
        assertEquals("4:07", formatMillis(247_000L))
    }

    @Test
    fun demoLibraryHasANowPlayingTrack() {
        assertEquals("Midnight Index", demoLibrarySnapshot().nowPlaying?.title)
    }

    @Test
    fun controllerMovesFromLoadingToPlayingForSelectedTrack() {
        val controller = PlaybackController(FakePlaybackEngine())
        val tracks = listOf(testPlayableTrack())

        controller.setQueue(tracks, selectedTrackId = "test-track")
        controller.play()

        val state = controller.state.value
        assertEquals("test-track", state.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, state.status)
        assertTrue(state.isPlaying)
        assertEquals(120_000L, state.durationMillis)
    }

    @Test
    fun controllerPausesWithoutLosingCurrentTrackOrPosition() {
        val controller = PlaybackController(FakePlaybackEngine())
        controller.setQueue(listOf(testPlayableTrack()))
        controller.play()
        controller.seekTo(42_000L)

        controller.pause()

        val state = controller.state.value
        assertEquals(PlaybackStatus.Paused, state.status)
        assertEquals("test-track", state.currentTrack?.id)
        assertEquals(42_000L, state.positionMillis)
    }

    @Test
    fun controllerClampsSeekToTrackDuration() {
        val controller = PlaybackController(FakePlaybackEngine())
        controller.setQueue(listOf(testPlayableTrack(durationMillis = 120_000L)))

        controller.seekTo(999_000L)

        assertEquals(120_000L, controller.state.value.positionMillis)
        assertEquals(1f, controller.state.value.progressFraction)
    }

    @Test
    fun controllerReportsRecoverableEngineErrors() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(listOf(testPlayableTrack()))

        engine.fail("Unsupported format")

        val state = controller.state.value
        assertEquals(PlaybackStatus.Error, state.status)
        assertEquals("Unsupported format", state.error?.message)
        assertFalse(state.isPlaying)
    }

    @Test
    fun controllerReleaseStopsStateAndReleasesEngine() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(listOf(testPlayableTrack()))
        controller.play()

        controller.release()

        assertTrue(engine.released)
        assertEquals(PlaybackStatus.Stopped, controller.state.value.status)
    }

    @Test
    fun importedFileDerivesDisplayTrackAndPlayableSource() {
        val imported = ImportedAudioFile(
            displayName = "Soft_Static.wav",
            source = AudioSource.FilePath("/Music/Soft_Static.wav"),
            durationMillis = 61_000L,
        )

        val snapshot = importedLibrarySnapshot(listOf(imported))

        assertEquals("Soft Static", snapshot.tracks.single().title)
        assertEquals("Local file", snapshot.tracks.single().artist)
        assertEquals(61, snapshot.tracks.single().durationSeconds)
        assertEquals(AudioSource.FilePath("/Music/Soft_Static.wav"), snapshot.tracks.single().source)
    }

    @Test
    fun displayTitleFallsBackForBlankNames() {
        assertEquals("Untitled audio", "...".toDisplayTitle())
    }

    private fun testPlayableTrack(durationMillis: Long = 120_000L): PlayableTrack = PlayableTrack(
        id = "test-track",
        title = "Test Track",
        artist = "Test Artist",
        album = "Test Album",
        durationMillis = durationMillis,
        source = AudioSource.FilePath("/tmp/test.wav"),
    )
}
