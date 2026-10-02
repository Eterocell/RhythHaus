package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistInteroperabilityServiceTest {
    @Test
    fun importPlanUsesGuardedProjectionAndPreservesIssueOrderAndDuplicates() {
        val track = track("one", "Song", 10_000)
        val document =
            PlaylistInteroperabilityDocument(
                name = "External",
                format = PlaylistInteroperabilityFormat.M3U,
                entries =
                    listOf(
                        PlaylistInteroperabilityEntry(
                            "Song", "Artist", "Album", 10, "/one.mp3"),
                        PlaylistInteroperabilityEntry(
                            "Missing", "Artist", "Album", 10, "/missing.mp3"),
                        PlaylistInteroperabilityEntry(
                            "Choice", "Artist", "Album", 10, "/choice.mp3"),
                        PlaylistInteroperabilityEntry(
                            "Song", "Artist", "Album", 10, "/one-again.mp3"),
                    ),
            )

        val plan =
            planPlaylistInteroperabilityImport(
                document = document,
                destinationTracks =
                    listOf(
                        track,
                        track("two", "Choice", 10_000),
                        track("three", "Choice", 10_000),
                    ),
                existingPlaylistNames = listOf("External"),
                importedSuffix = "Imported",
                libraryRevision = 37L,
            )

        assertEquals(37L, plan.libraryRevision)
        assertEquals(
            listOf(
                PlaylistImportPlaylist(
                    sourcePlaylistIndex = 0,
                    name = "External (Imported)",
                    trackIds = listOf("one", "one"),
                ),
            ),
            plan.playlists,
        )
        assertEquals(
            listOf(
                PlaylistImportPlaylistReport(
                    sourcePlaylistIndex = 0,
                    sourceName = "External",
                    plannedName = "External (Imported)",
                    counts =
                        PlaylistImportCounts(
                            restorable = 2,
                            unmatched = 1,
                            ambiguous = 1,
                        ),
                ),
            ),
            plan.reports,
        )
        assertEquals(
            listOf(
                PlaylistImportIssue(
                    playlistIndex = 0,
                    entryIndex = 1,
                    entry =
                        PlaylistBackupEntry("Missing", "Artist", "Album", 10),
                    kind = PlaylistImportIssueKind.UNMATCHED,
                    candidateTrackIds = emptyList(),
                ),
                PlaylistImportIssue(
                    playlistIndex = 0,
                    entryIndex = 2,
                    entry =
                        PlaylistBackupEntry("Choice", "Artist", "Album", 10),
                    kind = PlaylistImportIssueKind.AMBIGUOUS,
                    candidateTrackIds = listOf("two", "three"),
                ),
            ),
            plan.issues,
        )
    }

    @Test
    fun formatDetectionAcceptsOnlyValidSupportedDocuments() {
        val bytes =
            PlaylistInteroperabilityCodec.encode(
                PlaylistInteroperabilityDocument(
                    name = "Road trip",
                    format = PlaylistInteroperabilityFormat.PLS,
                    entries =
                        listOf(
                            PlaylistInteroperabilityEntry(
                                "Song", "Artist", "Album", 10, "/one.mp3"),
                        ),
                ),
            )

        assertEquals(
            PlaylistInteroperabilityFormat.PLS,
            detectPlaylistInteroperabilityFormat(bytes))
        assertEquals(
            null,
            detectPlaylistInteroperabilityFormat(
                "not a playlist".encodeToByteArray()))
    }

    private fun track(id: String, title: String, durationMillis: Long) =
        LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = "$title.mp3",
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = durationMillis,
            sizeBytes = 1,
            modifiedAtEpochMillis = 1,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )
}
