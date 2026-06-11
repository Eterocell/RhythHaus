package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRepositoryContractTest {
    @Test
    fun upsertTrackDoesNotDuplicateSourceLocalKey() {
        val repository = InMemoryLibraryRepository()
        val source = testSource()
        repository.upsertSource(source)
        val first = testTrack(title = "First")
        val second = first.copy(id = "track-2", title = "Second", updatedAtEpochMillis = 20L)

        assertEquals(TrackUpsertResult.Added, repository.upsertTrack(first))
        assertEquals(TrackUpsertResult.Updated, repository.upsertTrack(second))

        val tracks = repository.tracks()
        assertEquals(1, tracks.size)
        assertEquals("track-1", tracks.single().id)
        assertEquals("Second", tracks.single().title)
        assertEquals(1L, tracks.single().createdAtEpochMillis)
    }

    @Test
    fun removeMissingDeletesTracksNotSeenInLatestScan() {
        val repository = InMemoryLibraryRepository()
        val source = testSource()
        repository.upsertSource(source)
        repository.upsertTrack(testTrack(id = "seen", sourceLocalKey = "seen.mp3", lastSeenScanId = "scan-2"))
        repository.upsertTrack(testTrack(id = "missing", sourceLocalKey = "missing.mp3", lastSeenScanId = "scan-1"))
        repository.upsertTrack(testTrack(id = "unknown", sourceLocalKey = "unknown.mp3", lastSeenScanId = null))

        val removed = repository.removeMissingTracks(source.id, latestScanId = "scan-2")

        assertEquals(2, removed)
        assertEquals(listOf("seen"), repository.tracks().map { it.id })
    }

    @Test
    fun tracksForSourceOnlyReturnsRequestedSource() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource(id = "source-1"))
        repository.upsertSource(testSource(id = "source-2"))
        repository.upsertTrack(testTrack(id = "track-1", sourceId = "source-1", sourceLocalKey = "one.mp3"))
        repository.upsertTrack(testTrack(id = "track-2", sourceId = "source-2", sourceLocalKey = "two.mp3"))

        assertEquals(listOf("track-2"), repository.tracksForSource("source-2").map { it.id })
    }

    @Test
    fun scanErrorsAreStoredByScan() {
        val repository = InMemoryLibraryRepository()
        repository.insertScanError(testScanError(id = "error-1", scanId = "scan-1"))
        repository.insertScanError(testScanError(id = "error-2", scanId = "scan-2"))

        assertEquals(listOf("error-1"), repository.scanErrors("scan-1").map { it.id })
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
    id: String = "track-1",
    sourceId: String = "source-1",
    sourceLocalKey: String = "Track.mp3",
    title: String = "Track",
    lastSeenScanId: String? = "scan-1",
) = LibraryTrack(
    id = id,
    sourceId = sourceId,
    sourceLocalKey = sourceLocalKey,
    audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
    displayName = sourceLocalKey,
    title = title,
    artist = "Local file",
    album = "Imported audio",
    durationMillis = null,
    sizeBytes = null,
    modifiedAtEpochMillis = null,
    lastSeenScanId = lastSeenScanId,
    createdAtEpochMillis = 1L,
    updatedAtEpochMillis = 2L,
)

private fun testScanError(
    id: String,
    scanId: String,
) = ScanError(
    id = id,
    scanId = scanId,
    sourceLocalKey = "bad.txt",
    displayPath = "/Music/bad.txt",
    reason = "Unsupported file",
    recoverable = true,
    createdAtEpochMillis = 3L,
)
