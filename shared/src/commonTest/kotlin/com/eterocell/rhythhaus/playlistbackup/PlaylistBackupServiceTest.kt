package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaylistBackupServiceTest {
    @Test
    fun exportUsesConfirmedPlaylistAndEntryOrderWithOnlyAuthoritativePortableMetadata() {
        val snapshot = snapshot(
            playlist("second", "Second") to listOf(entry("second", "track-b", 0), entry("second", "track-a", 1)),
            playlist("first", "First") to listOf(entry("first", "track-a", 0)),
        )
        val tracks = listOf(
            track("track-a", title = "A", durationMillis = 125_999),
            track("track-b", title = "B", durationMillis = 60_000),
        )

        val first = assertIs<PlaylistBackupExportResult.Success>(
            exportPlaylistBackup(snapshot, tracks, exportedAtEpochMillis = 42),
        )
        val second = assertIs<PlaylistBackupExportResult.Success>(
            exportPlaylistBackup(snapshot, tracks.reversed(), exportedAtEpochMillis = 42),
        )

        assertContentEquals(first.bytes, second.bytes)
        val document = assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(first.bytes)).document
        assertEquals(listOf("Second", "First"), document.playlists.map { it.name })
        assertEquals(listOf("B", "A"), document.playlists.first().entries.map { it.title })
        assertEquals(125, document.playlists.first().entries.last().durationSeconds)
    }

    @Test
    fun exportFailsBeforeBytesForMissingUnknownInvalidOrCodecBoundMetadata() {
        val snapshot = snapshot(playlist("p", "P") to listOf(entry("p", "track", 0)))

        assertEquals(
            PlaylistBackupExportResult.Failure(PlaylistBackupExportError.MISSING_TRACK, "track"),
            exportPlaylistBackup(snapshot, emptyList(), 1),
        )
        assertEquals(
            PlaylistBackupExportResult.Failure(PlaylistBackupExportError.MISSING_DURATION, "track"),
            exportPlaylistBackup(snapshot, listOf(track("track", durationMillis = null)), 1),
        )
        assertEquals(
            PlaylistBackupExportResult.Failure(PlaylistBackupExportError.INVALID_DURATION, "track"),
            exportPlaylistBackup(snapshot, listOf(track("track", durationMillis = -1)), 1),
        )
        assertEquals(
            PlaylistBackupExportError.CODEC_BOUNDS,
            assertIs<PlaylistBackupExportResult.Failure>(
                exportPlaylistBackup(
                    snapshot(playlist("p", " ") to emptyList()),
                    emptyList(),
                    1,
                ),
            ).error,
        )
    }

    @Test
    fun planPreservesOrderAndDuplicateResolvedIdsWhileCountingAndRecordingIssues() {
        val backup = PlaylistBackupDocument(
            format = PlaylistBackupCodec.FORMAT,
            version = PlaylistBackupCodec.VERSION,
            exportedAtEpochMillis = 1,
            playlists = listOf(
                PlaylistBackupPlaylist(
                    "Mix",
                    listOf(entry(), entry(), entry(title = "Missing"), entry(title = "Ambiguous")),
                ),
                PlaylistBackupPlaylist("None", listOf(entry(title = "Missing"))),
            ),
            checksumCrc32 = "00000000",
        )
        val tracks = listOf(
            track("resolved", durationMillis = 100_000),
            track("ambiguous-a", title = "Ambiguous", durationMillis = 100_000),
            track("ambiguous-b", title = "Ambiguous", durationMillis = 100_000),
        )

        val plan = planPlaylistImport(
            document = backup,
            destinationTracks = tracks,
            existingPlaylistNames = listOf(" mix "),
            importedSuffix = "Imported",
            libraryRevision = 73,
        )

        assertEquals(73, plan.libraryRevision)
        assertEquals(listOf("Mix (Imported)"), plan.playlists.map { it.name })
        assertEquals(listOf("resolved", "resolved"), plan.playlists.single().trackIds)
        assertEquals(
            listOf(PlaylistImportCounts(2, 1, 1), PlaylistImportCounts(0, 1, 0)),
            plan.reports.map { it.counts },
        )
        assertEquals(PlaylistImportCounts(2, 2, 1), plan.totals.entries)
        assertEquals(1, plan.totals.playlistsToCreate)
        assertEquals(1, plan.totals.playlistsSkipped)
        assertEquals(
            listOf(
                PlaylistImportIssue(0, 2, entry(title = "Missing"), PlaylistImportIssueKind.UNMATCHED, emptyList()),
                PlaylistImportIssue(
                    0,
                    3,
                    entry(title = "Ambiguous"),
                    PlaylistImportIssueKind.AMBIGUOUS,
                    listOf("ambiguous-a", "ambiguous-b"),
                ),
                PlaylistImportIssue(1, 0, entry(title = "Missing"), PlaylistImportIssueKind.UNMATCHED, emptyList()),
            ),
            plan.issues,
        )
    }

    @Test
    fun conflictNamingReservesExistingAndEarlierPlannedNamesAcrossRepeatedImports() {
        val backup = PlaylistBackupDocument(
            PlaylistBackupCodec.FORMAT,
            PlaylistBackupCodec.VERSION,
            1,
            listOf(
                PlaylistBackupPlaylist("Mix", listOf(entry())),
                PlaylistBackupPlaylist(" MIX ", listOf(entry())),
            ),
            "00000000",
        )
        val tracks = listOf(track("resolved", durationMillis = 100_000))

        val first = planPlaylistImport(backup, tracks, listOf("Mix"), "Imported", 1)
        assertEquals(listOf("Mix (Imported)", " MIX  (Imported 2)"), first.playlists.map { it.name })

        val repeated = planPlaylistImport(
            backup,
            tracks,
            listOf("Mix") + first.playlists.map { it.name },
            "Imported",
            2,
        )
        assertEquals(listOf("Mix (Imported 3)", " MIX  (Imported 4)"), repeated.playlists.map { it.name })
    }

    private fun entry(
        title: String = "Title",
        durationSeconds: Int = 100,
    ) = PlaylistBackupEntry(title, "Artist", "Album", durationSeconds)

    private fun track(
        id: String,
        title: String = "Title",
        durationMillis: Long?,
    ) = LibraryTrack(
        id = id,
        sourceId = "source",
        sourceLocalKey = id,
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = "$id.mp3",
        title = title,
        artist = "Artist",
        album = "Album",
        durationMillis = durationMillis,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    private fun playlist(id: String, name: String) = Playlist(id, name, 1, 1)

    private fun entry(playlistId: String, trackId: String, position: Int) = PlaylistEntry(
        id = "$playlistId-$position",
        playlistId = playlistId,
        trackId = trackId,
        position = position,
        createdAtEpochMillis = 1,
    )

    private fun snapshot(vararg playlists: Pair<Playlist, List<PlaylistEntry>>) = PlaylistSnapshot(
        playlists = playlists.map { it.first },
        entriesByPlaylistId = playlists.associate { it.first.id to it.second },
    )
}
