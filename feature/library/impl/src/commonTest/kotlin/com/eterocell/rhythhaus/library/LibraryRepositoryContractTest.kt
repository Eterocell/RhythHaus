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
        val second =
            first.copy(
                id = "track-2", title = "Second", updatedAtEpochMillis = 20L)

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
        repository.upsertTrack(
            testTrack(
                id = "seen",
                sourceLocalKey = "seen.mp3",
                lastSeenScanId = "scan-2"))
        repository.upsertTrack(
            testTrack(
                id = "missing",
                sourceLocalKey = "missing.mp3",
                lastSeenScanId = "scan-1"))
        repository.upsertTrack(
            testTrack(
                id = "unknown",
                sourceLocalKey = "unknown.mp3",
                lastSeenScanId = null))
        repository.insertScanSession(
            ScanSession(
                id = "scan-2",
                sourceId = source.id,
                status = ScanStatus.Completed,
                startedAtEpochMillis = 1L,
                completedAtEpochMillis = 2L,
            ))

        val removed =
            repository.removeMissingTracks(
                source.id, requestedScanId = "scan-2")

        assertEquals(RemoveMissingTracksResult.Removed(2), removed)
        assertEquals(listOf("seen"), repository.tracks().map { it.id })
    }

    @Test
    fun tracksForSourceOnlyReturnsRequestedSource() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource(id = "source-1"))
        repository.upsertSource(testSource(id = "source-2"))
        repository.upsertTrack(
            testTrack(
                id = "track-1",
                sourceId = "source-1",
                sourceLocalKey = "one.mp3"))
        repository.upsertTrack(
            testTrack(
                id = "track-2",
                sourceId = "source-2",
                sourceLocalKey = "two.mp3"))

        assertEquals(
            listOf("track-2"),
            repository.tracksForSource("source-2").map { it.id })
    }

    @Test
    fun scanErrorsAreStoredByScan() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource())
        repository.insertScanSession(
            testScanSession("scan-1", "source-1", ScanStatus.Scanning, 1L))
        repository.insertScanSession(
            testScanSession("scan-2", "source-1", ScanStatus.Scanning, 2L))
        repository.insertScanError(
            testScanError(
                id = "error-z", scanId = "scan-1", createdAtEpochMillis = 2L))
        repository.insertScanError(
            testScanError(
                id = "error-a", scanId = "scan-1", createdAtEpochMillis = 2L))
        repository.insertScanError(
            testScanError(
                id = "error-early",
                scanId = "scan-1",
                createdAtEpochMillis = 1L))
        repository.insertScanError(
            testScanError(id = "error-2", scanId = "scan-2"))

        assertEquals(
            listOf("error-early", "error-a", "error-z"),
            repository.scanErrors("scan-1").map { it.id })
    }

    @Test
    fun updateScanSessionMatchesSqlUpdateForUnknownSessionAndSource() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource())
        val original =
            testScanSession(
                id = "scan-1",
                sourceId = "source-1",
                status = ScanStatus.Scanning,
                startedAtEpochMillis = 1L,
            )
        repository.insertScanSession(original)

        repository.updateScanSession(
            testScanSession(
                id = "unknown",
                sourceId = "missing-source",
                status = ScanStatus.Completed,
                startedAtEpochMillis = 9L,
                completedAtEpochMillis = 10L,
            ),
        )
        assertEquals(null, repository.latestTerminalScanSession())

        repository.updateScanSession(
            original.copy(
                sourceId = "missing-source",
                status = ScanStatus.Completed,
                startedAtEpochMillis = 99L,
                completedAtEpochMillis = 10L,
                foldersVisited = 3,
                terminalMessage = "done",
            ),
        )

        assertEquals(
            original.copy(
                status = ScanStatus.Completed,
                completedAtEpochMillis = 10L,
                foldersVisited = 3,
                terminalMessage = "done",
            ),
            repository.latestTerminalScanSession(),
        )
    }

    @Test
    fun removeMissingRejectsEveryNonAuthoritativeRequestWithoutMutation() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource())
        repository.upsertSource(testSource(id = "source-2"))
        repository.upsertSource(testSource(id = "source-3"))
        repository.upsertSource(testSource(id = "other"))
        repository.upsertTrack(
            testTrack(id = "missing", lastSeenScanId = "old"))
        repository.insertScanSession(
            testScanSession("foreign", "other", ScanStatus.Completed, 1L, 5L))
        repository.insertScanSession(
            testScanSession("active", "source-1", ScanStatus.Scanning, 1L))
        repository.insertScanSession(
            testScanSession(
                "cancelling", "source-1", ScanStatus.Cancelling, 1L))
        repository.insertScanSession(
            testScanSession(
                "cancelled", "source-1", ScanStatus.Cancelled, 1L, 3L))
        repository.insertScanSession(
            testScanSession("failed", "source-1", ScanStatus.Failed, 1L, 4L))
        repository.insertScanSession(
            testScanSession(
                "malformed", "source-1", ScanStatus.Completed, 2L, null))
        repository.insertScanSession(
            testScanSession("stale", "source-1", ScanStatus.Completed, 1L, 5L))
        repository.insertScanSession(
            testScanSession("latest", "source-1", ScanStatus.Completed, 2L, 6L))

        val requests =
            listOf(
                "unknown" to RemoveMissingTracksRejectionReason.UnknownScan,
                "foreign" to RemoveMissingTracksRejectionReason.ForeignSource,
                "active" to RemoveMissingTracksRejectionReason.NotCompleted,
                "cancelling" to RemoveMissingTracksRejectionReason.NotCompleted,
                "cancelled" to RemoveMissingTracksRejectionReason.NotCompleted,
                "failed" to RemoveMissingTracksRejectionReason.NotCompleted,
                "malformed" to
                    RemoveMissingTracksRejectionReason
                        .MissingCompletionTimestamp,
                "stale" to
                    RemoveMissingTracksRejectionReason.StaleCompletedScan,
            )
        assertEquals(
            RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.UnknownSource),
            repository.removeMissingTracks("missing-source", "unknown"),
        )
        requests.forEach { (scanId, reason) ->
            assertEquals(
                RemoveMissingTracksResult.Rejected(reason),
                repository.removeMissingTracks("source-1", scanId),
            )
            assertEquals(
                listOf("missing"),
                repository.tracksForSource("source-1").map { it.id })
        }
    }

    @Test
    fun latestCompletedAndTerminalSessionsUseDeterministicOrdering() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource())
        repository.upsertSource(testSource(id = "source-2"))
        repository.upsertSource(testSource(id = "source-3"))
        repository.insertScanSession(
            testScanSession(
                "completed-a", "source-1", ScanStatus.Completed, 10L, 20L))
        repository.insertScanSession(
            testScanSession(
                "completed-b", "source-1", ScanStatus.Completed, 11L, 20L))
        repository.insertScanSession(
            testScanSession(
                "completed-c", "source-1", ScanStatus.Completed, 11L, 20L))
        assertEquals(
            RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.StaleCompletedScan),
            repository.removeMissingTracks("source-1", "completed-b"),
        )

        repository.insertScanSession(
            testScanSession(
                "cancelled", "source-2", ScanStatus.Cancelled, 30L, null))
        repository.insertScanSession(
            testScanSession("failed", "source-3", ScanStatus.Failed, 31L, null))
        assertEquals("failed", repository.latestTerminalScanSession()?.id)
    }

    @Test
    fun latestOrderingUsesEveryTieBreakKey() {
        val repository = InMemoryLibraryRepository()
        repository.upsertSource(testSource())
        repository.insertScanSession(
            testScanSession(
                "completed-old", "source-1", ScanStatus.Completed, 1L, 10L))
        repository.insertScanSession(
            testScanSession(
                "completed-new-start",
                "source-1",
                ScanStatus.Completed,
                2L,
                10L))
        repository.insertScanSession(
            testScanSession(
                "completed-z", "source-1", ScanStatus.Completed, 2L, 10L))
        repository.upsertTrack(testTrack(lastSeenScanId = "completed-z"))
        assertEquals(
            RemoveMissingTracksResult.Removed(0),
            repository.removeMissingTracks("source-1", "completed-z"))

        repository.insertScanSession(
            testScanSession(
                "terminal-old", "source-1", ScanStatus.Failed, 20L, 30L))
        repository.insertScanSession(
            testScanSession(
                "terminal-new-start",
                "source-1",
                ScanStatus.Cancelled,
                21L,
                30L))
        repository.insertScanSession(
            testScanSession(
                "terminal-z", "source-1", ScanStatus.Failed, 21L, 30L))
        repository.insertScanSession(
            testScanSession(
                "terminal-later-completion",
                "source-1",
                ScanStatus.Failed,
                1L,
                31L))
        assertEquals(
            "terminal-later-completion",
            repository.latestTerminalScanSession()?.id)
    }
}

private fun testSource(
    id: String = "source-1",
) =
    LibrarySource(
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
) =
    LibraryTrack(
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

private fun testScanSession(
    id: String,
    sourceId: String,
    status: ScanStatus,
    startedAtEpochMillis: Long,
    completedAtEpochMillis: Long? = 2L,
) =
    ScanSession(
        id = id,
        sourceId = sourceId,
        status = status,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
    )

private fun testScanError(
    id: String,
    scanId: String,
    createdAtEpochMillis: Long = 3L,
) =
    ScanError(
        id = id,
        scanId = scanId,
        sourceLocalKey = "bad.txt",
        displayPath = "/Music/bad.txt",
        reason = "Unsupported file",
        recoverable = true,
        createdAtEpochMillis = createdAtEpochMillis,
    )
