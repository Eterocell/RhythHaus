package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryModelsTest {
    @Test
    fun supportedAudioExtensionsAreCaseInsensitive() {
        assertTrue(isSupportedAudioName("Track.MP3"))
        assertTrue(isSupportedAudioName("mix.flac"))
        assertTrue(isSupportedAudioName("voice.m4a"))
        assertFalse(isSupportedAudioName("cover.jpg"))
        assertFalse(isSupportedAudioName("notes"))
    }

    @Test
    fun libraryTrackMapsToPlayableTrack() {
        val track = LibraryTrack(
            id = "track-1",
            sourceId = "source-1",
            sourceLocalKey = "Album/Track.mp3",
            audioSource = AudioSource.FilePath("/Music/Album/Track.mp3"),
            displayName = "Track.mp3",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 123_000L,
            sizeBytes = 1000L,
            modifiedAtEpochMillis = 456L,
            lastSeenScanId = "scan-1",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        )

        val playable = track.toPlayableTrack()

        assertEquals("track-1", playable.id)
        assertEquals("Track", playable.title)
        assertEquals("Artist", playable.artist)
        assertEquals("Album", playable.album)
        assertEquals(123_000L, playable.durationMillis)
        assertEquals(AudioSource.FilePath("/Music/Album/Track.mp3"), playable.source)
    }
}
