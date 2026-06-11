package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun importedFileUsesTagLibMetadataWhenReaderFindsTags() {
        val imported = ImportedAudioFile(
            displayName = "Soft_Static.wav",
            source = AudioSource.FilePath("/Music/Soft_Static.wav"),
            durationMillis = 61_000L,
        )
        val fakeReader = FakeTagLibReader(
            TagReadResult.Found(
                TagMetadata(
                    title = "Soft Static (Tagged)",
                    artist = "Signal Artist",
                    album = "Noise Collection",
                    durationMillis = 125_000L,
                ),
            ),
        )

        val snapshot = importedLibrarySnapshot(
            enrichImportedAudioFiles(listOf(imported), AudioMetadataReader(fakeReader)),
        )
        val track = snapshot.tracks.single()

        assertEquals("/Music/Soft_Static.wav", fakeReader.lastPath)
        assertEquals("Soft Static (Tagged)", track.title)
        assertEquals("Signal Artist", track.artist)
        assertEquals("Noise Collection", track.album)
        assertEquals(125, track.durationSeconds)
    }

    @Test
    fun importedFileFallsBackToDisplayNameWhenTagLibIsUnsupported() {
        val imported = ImportedAudioFile(
            displayName = "Soft_Static.wav",
            source = AudioSource.FilePath("/Music/Soft_Static.wav"),
            durationMillis = 61_000L,
        )
        val reader = AudioMetadataReader(FakeTagLibReader(TagReadResult.Unsupported("native reader unavailable")))

        val snapshot = importedLibrarySnapshot(enrichImportedAudioFiles(listOf(imported), reader))
        val track = snapshot.tracks.single()

        assertEquals("Soft Static", track.title)
        assertEquals("Local file", track.artist)
        assertEquals("Imported audio", track.album)
        assertEquals(61, track.durationSeconds)
    }

    @Test
    fun importedFileFallsBackToDisplayNameWhenTagLibFails() {
        val imported = ImportedAudioFile(
            displayName = "Broken_Tag.mp3",
            source = AudioSource.FilePath("/Music/Broken_Tag.mp3"),
        )
        val reader = AudioMetadataReader(FakeTagLibReader(TagReadResult.Failed("read failed")))

        val snapshot = importedLibrarySnapshot(enrichImportedAudioFiles(listOf(imported), reader))
        val track = snapshot.tracks.single()

        assertEquals("Broken Tag", track.title)
        assertEquals("Local file", track.artist)
        assertEquals(0, track.durationSeconds)
    }

    @Test
    fun metadataReaderDoesNotCallTagLibForUriSources() {
        val fakeReader = FakeTagLibReader(TagReadResult.Failed("should not be called"))
        val reader = AudioMetadataReader(fakeReader)

        val metadata = reader.read(AudioSource.Uri("content://media/audio/1"))

        assertNull(metadata)
        assertNull(fakeReader.lastPath)
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

    private class FakeTagLibReader(
        private val result: TagReadResult,
    ) : TagLibReader {
        var lastPath: String? = null
            private set

        override fun readPath(path: String): TagReadResult {
            lastPath = path
            return result
        }
    }
}
