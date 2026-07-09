package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArtworkLazyLoadingTest {
    @Test
    fun routineLibrarySnapshotDoesNotCarryArtworkButRepositoryCanLoadItByTrackId() {
        val repository = InMemoryLibraryRepository()
        val artworkBytes = byteArrayOf(1, 2, 3, 4)
        repository.upsertSource(testSource())
        repository.upsertTrack(
            testTrack(
                id = "track-with-artwork",
                sourceLocalKey = "track.mp3",
                title = "Track",
                artist = "Artist",
            ).copy(
                artworkBytes = artworkBytes,
                artworkMimeType = "image/jpeg",
            ),
        )

        val routineTrack = repository.tracks().single()
        assertNull(routineTrack.artworkBytes)
        assertNull(routineTrack.artworkMimeType)

        val artwork = repository.artworkForTrack("track-with-artwork")
        assertNotNull(artwork)
        assertContentEquals(artworkBytes, artwork.bytes)
        assertEquals("image/jpeg", artwork.mimeType)
    }
}

private fun testSource(
    id: String = "source-1",
) = LibrarySource(
    id = id,
    platformKind = LibraryPlatformKind.JvmFolder,
    displayName = "Music",
    handle = "/Music",
    createdAtEpochMillis = 1L,
)

private fun testTrack(
    id: String,
    sourceLocalKey: String,
    title: String,
    artist: String,
) = LibraryTrack(
    id = id,
    sourceId = "source-1",
    sourceLocalKey = sourceLocalKey,
    audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
    displayName = sourceLocalKey,
    title = title,
    artist = artist,
    album = "Imported audio",
    durationMillis = null,
    sizeBytes = null,
    modifiedAtEpochMillis = null,
    lastSeenScanId = "scan-1",
    createdAtEpochMillis = 1L,
    updatedAtEpochMillis = 2L,
)
