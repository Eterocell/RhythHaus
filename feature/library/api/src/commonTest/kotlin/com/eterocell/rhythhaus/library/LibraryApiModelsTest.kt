package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryApiModelsTest {
    @Test
    fun libraryTrackUsesContentEqualityAndHashCodeForNullableArtworkBytes() {
        val withArtwork = track(artworkBytes = byteArrayOf(1, 2))
        assertEquals(withArtwork, track(artworkBytes = byteArrayOf(1, 2)))
        assertEquals(
            withArtwork.hashCode(),
            track(artworkBytes = byteArrayOf(1, 2)).hashCode())
        assertEquals(track(artworkBytes = null), track(artworkBytes = null))
        assertEquals(
            track(artworkBytes = null).hashCode(),
            track(artworkBytes = null).hashCode())
    }

    @Test
    fun trackArtworkUsesContentEqualityAndHashCodeForNullableByteArrays() {
        val artwork = TrackArtwork(byteArrayOf(1, 2), "image/jpeg")
        assertEquals(artwork, TrackArtwork(byteArrayOf(1, 2), "image/jpeg"))
        assertEquals(
            artwork.hashCode(),
            TrackArtwork(byteArrayOf(1, 2), "image/jpeg").hashCode())
    }

    private fun track(
        durationMillis: Long? = 1L,
        artworkBytes: ByteArray? = null,
    ) =
        LibraryTrack(
            id = "track",
            sourceId = "source",
            sourceLocalKey = "Track.mp3",
            audioSource = AudioSource.FilePath("/Music/Track.mp3"),
            displayName = "Track.mp3",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = durationMillis,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
            artworkBytes = artworkBytes,
            artworkMimeType = "image/jpeg",
        )
}
