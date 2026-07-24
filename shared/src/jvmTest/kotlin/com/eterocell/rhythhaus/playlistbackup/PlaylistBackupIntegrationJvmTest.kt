package com.eterocell.rhythhaus.playlistbackup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.SqlDelightPlaylistRepository
import com.eterocell.rhythhaus.library.ui.AddToPlaylistPickerState
import com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.TrackSelectionAction
import com.eterocell.rhythhaus.library.ui.TrackSelectionPageKey
import com.eterocell.rhythhaus.library.ui.TrackSelectionState
import com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.orderedSelectedTrackIds
import com.eterocell.rhythhaus.library.ui.reduceTrackSelection
import java.io.File
import java.nio.file.Files
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PlaylistBackupIntegrationJvmTest {
    @Test
    fun orderedSelectionRoundTripsThroughAtomicBackupRestoreAndRepeatImport() =
        runBlocking {
            openHarness().use { harness ->
                harness.seedExportAndDestinationTracks()

                val page = TrackSelectionPageKey.HomeSongs
                val selected =
                    listOf(
                            "export-beta",
                            "export-alpha",
                            "export-unmatched",
                            "export-ambiguous")
                        .fold(TrackSelectionState()) { state, trackId ->
                            val action =
                                if (state.pageKey == null) {
                                    TrackSelectionAction.Start(page, trackId)
                                } else {
                                    TrackSelectionAction.Select(page, trackId)
                                }
                            reduceTrackSelection(state, action)
                        }
                val visibleIds =
                    listOf(
                        "export-alpha",
                        "export-beta",
                        "export-alpha",
                        "export-unmatched",
                        "export-ambiguous",
                        "not-selected",
                    )
                val orderedSelection =
                    orderedSelectedTrackIds(selected, page, visibleIds)
                assertEquals(
                    listOf(
                        "export-alpha",
                        "export-beta",
                        "export-unmatched",
                        "export-ambiguous"),
                    orderedSelection,
                )

                val picker =
                    AddToPlaylistPickerState(
                        trackIds = orderedSelection,
                        selectedPlaylistId = "export-road-mix",
                    )
                val append = requireNotNull(picker.confirmedAppend())
                assertEquals(orderedSelection, append.trackIds)

                val exportedPlaylist = harness.playlists.create("Road Mix")
                harness.playlists.append(
                    exportedPlaylist.id,
                    append.trackIds + listOf("export-alpha"))
                assertEquals(
                    listOf(
                        "export-alpha",
                        "export-beta",
                        "export-unmatched",
                        "export-ambiguous",
                        "export-alpha"),
                    harness.playlists
                        .entries(exportedPlaylist.id)
                        .map(PlaylistEntry::trackId),
                )
                val inlineCreate =
                    requireNotNull(
                        AddToPlaylistPickerState(
                                trackIds = listOf("export-second"),
                                enteredName = " Second ",
                            )
                            .confirmedInlineCreate(),
                    )
                val secondPlaylist =
                    harness.playlists.createWithEntries(
                        inlineCreate.name, inlineCreate.trackIds)
                assertEquals(
                    listOf("export-second"),
                    harness.playlists
                        .entries(secondPlaylist.id)
                        .map(PlaylistEntry::trackId))
                val skippedPlaylist =
                    harness.playlists.createWithEntries(
                        "Unavailable", listOf("export-unmatched"))
                val existingPlaylists = harness.playlists.playlists()
                val existingEntries = existingPlaylists.associate {
                    it.id to harness.playlists.entries(it.id)
                }
                val sourceSnapshot =
                    PlaylistSnapshot(
                        playlists =
                            listOf(
                                exportedPlaylist,
                                secondPlaylist,
                                skippedPlaylist),
                        entriesByPlaylistId = existingEntries,
                    )

                val export =
                    assertIs<PlaylistBackupExportResult.Success>(
                        exportPlaylistBackup(
                            sourceSnapshot,
                            harness.library.tracks(),
                            exportedAtEpochMillis = 42),
                    )
                val repeatedExport =
                    assertIs<PlaylistBackupExportResult.Success>(
                        exportPlaylistBackup(
                            sourceSnapshot,
                            harness.library.tracks(),
                            exportedAtEpochMillis = 42),
                    )
                assertContentEquals(export.bytes, repeatedExport.bytes)
                val encoded = export.bytes.decodeToString()
                assertTrue(
                    encoded.startsWith(
                        "{\"format\":\"rhythhaus-playlist-backup\",\"version\":1," +
                            "\"exportedAtEpochMillis\":42,\"playlists\":[{\"name\":\"Road Mix\",\"entries\":[" +
                            "{\"title\":\"Alpha\",\"artist\":\"Artist\",\"album\":\"Album\",\"durationSeconds\":100}",
                    ),
                )
                assertTrue(
                    Regex(",\"checksumCrc32\":\"[0-9a-f]{8}\"}$")
                        .containsMatchIn(encoded))
                assertEncodedBackupExcludesLocalCanaries(
                    encoded, harness.exportedLocalOnlyCanaries)
                listOf(
                        "\"id\"",
                        "\"path\"",
                        "\"source\"",
                        "\"artwork",
                        "\"createdAt",
                        "\"updatedAt")
                    .forEach { field ->
                        assertTrue(
                            !encoded.contains(field, ignoreCase = true),
                            "Backup leaked local field: $field")
                    }
                assertEquals(
                    2, "\"title\":\"Alpha\"".toRegex().findAll(encoded).count())
                assertTrue("\"artist\":\"Artist\"" in encoded)
                assertTrue("\"album\":\"Album\"" in encoded)
                assertTrue("\"durationSeconds\":500" in encoded)

                val decoded =
                    assertIs<PlaylistBackupDecodeResult.Success>(
                        PlaylistBackupCodec.decode(export.bytes))
                assertEquals(
                    listOf("Road Mix", "Second", "Unavailable"),
                    decoded.document.playlists.map { it.name })
                assertEquals(
                    listOf("Alpha", "Beta", "Missing", "Ambiguous", "Alpha"),
                    decoded.document.playlists.first().entries.map { it.title },
                )

                val revision = 7L
                val plan =
                    planPlaylistImport(
                        document = decoded.document,
                        destinationTracks = harness.destinationTracks(),
                        existingPlaylistNames =
                            existingPlaylists.map { it.name },
                        importedSuffix = "Imported",
                        libraryRevision = revision,
                    )
                assertEquals(
                    PlaylistImportTotals(
                        playlistsToCreate = 2,
                        playlistsSkipped = 1,
                        entries =
                            PlaylistImportCounts(
                                restorable = 4, unmatched = 2, ambiguous = 1),
                    ),
                    plan.totals,
                )
                assertEquals(
                    listOf("Road Mix (Imported)", "Second (Imported)"),
                    plan.playlists.map { it.name })
                assertEquals(
                    listOf(
                        "destination-alpha",
                        "destination-beta",
                        "destination-alpha"),
                    plan.playlists.first().trackIds,
                )
                assertEquals(
                    listOf(
                        PlaylistImportIssueKind.UNMATCHED,
                        PlaylistImportIssueKind.AMBIGUOUS,
                        PlaylistImportIssueKind.UNMATCHED),
                    plan.issues.map { it.kind },
                )
                assertEquals(
                    listOf(
                        "destination-ambiguous-a", "destination-ambiguous-b"),
                    plan.issues[1].candidateTrackIds)
                assertNull(
                    plan.reports
                        .single { it.sourceName == "Unavailable" }
                        .plannedName)

                val beforeStalePlaylistCount =
                    harness.playlists.playlists().size
                val beforeStaleEntryCount = harness.directPlaylistEntryCount()
                var staleRepositoryCalls = 0
                val previewState =
                    PlaylistBackupUiState(preview = PlaylistBackupPreview(plan))
                val stale =
                    confirmPlaylistBackupImportSerialized(
                        state = previewState,
                        currentLibraryRevision = revision + 1,
                        lastConfirmedSnapshot = sourceSnapshot,
                        mutateAndRefresh = {
                            staleRepositoryCalls++
                            error("stale confirmation must not mutate")
                        },
                    )
                assertEquals(0, staleRepositoryCalls)
                assertEquals(
                    PlaylistBackupUiError.StalePreview, stale.state.error)
                assertSame(previewState.preview, stale.state.preview)
                assertSame(sourceSnapshot, stale.confirmedSnapshot)
                assertEquals(
                    beforeStalePlaylistCount,
                    harness.playlists.playlists().size)
                assertEquals(
                    beforeStaleEntryCount, harness.directPlaylistEntryCount())

                harness.driver
                    .execute(
                        identifier = null,
                        sql =
                            "CREATE TRIGGER fail_second_playlist_restore BEFORE INSERT ON playlist_entry " +
                                "WHEN NEW.trackId = 'destination-second' BEGIN SELECT RAISE(ABORT, 'forced second playlist failure'); END",
                        parameters = 0,
                    )
                    .value
                val beforeFailurePlaylists = harness.playlists.playlists()
                val beforeFailureEntries = beforeFailurePlaylists.associate {
                    it.id to harness.playlists.entries(it.id)
                }
                val beforeFailureEntryCount = harness.directPlaylistEntryCount()
                val mutations =
                    plan.playlists.map {
                        PlaylistImportMutation(it.name, it.trackIds)
                    }

                assertFails { harness.playlists.importPlaylists(mutations) }

                assertEquals(
                    beforeFailurePlaylists, harness.playlists.playlists())
                assertEquals(
                    beforeFailureEntryCount, harness.directPlaylistEntryCount())
                beforeFailureEntries.forEach { (playlistId, entries) ->
                    assertEquals(entries, harness.playlists.entries(playlistId))
                }
                harness.driver
                    .execute(
                        null, "DROP TRIGGER fail_second_playlist_restore", 0)
                    .value

                var successfulOwnerCalls = 0
                val successful =
                    confirmPlaylistBackupImportSerialized(
                        state = previewState,
                        currentLibraryRevision = revision,
                        lastConfirmedSnapshot = sourceSnapshot,
                        mutateAndRefresh = { received ->
                            successfulOwnerCalls++
                            assertEquals(mutations, received)
                            harness.playlists.importPlaylists(received)
                            PlaylistImportOwnerResult.Success(
                                loadPlaylistSnapshot(harness.playlists),
                                revision = 11)
                        },
                    )
                assertEquals(1, successfulOwnerCalls)
                assertNull(successful.state.preview)
                assertEquals(plan.totals, successful.state.result?.totals)
                assertEquals(11, successful.playlistPublicationRevision)
                assertEquals(
                    beforeFailurePlaylists.size + 2,
                    harness.playlists.playlists().size)
                assertEquals(
                    beforeFailureEntryCount + 4,
                    harness.directPlaylistEntryCount())
                beforeFailureEntries.forEach { (playlistId, entries) ->
                    assertEquals(entries, harness.playlists.entries(playlistId))
                }
                val firstImported =
                    requireNotNull(successful.confirmedSnapshot)
                        .playlists
                        .takeLast(2)
                assertEquals(
                    listOf("Road Mix (Imported)", "Second (Imported)"),
                    firstImported.map { it.name })
                assertEquals(
                    "Road Mix",
                    harness.playlists.playlist(exportedPlaylist.id)?.name)
                assertEquals(
                    existingEntries.getValue(exportedPlaylist.id),
                    harness.playlists.entries(exportedPlaylist.id))
                assertEquals(
                    listOf(
                        "destination-alpha",
                        "destination-beta",
                        "destination-alpha"),
                    harness.playlists
                        .entries(firstImported[0].id)
                        .map(PlaylistEntry::trackId),
                )
                assertEquals(
                    listOf(0, 1, 2),
                    harness.playlists
                        .entries(firstImported[0].id)
                        .map(PlaylistEntry::position))
                assertEquals(
                    3,
                    harness.playlists
                        .entries(firstImported[0].id)
                        .map(PlaylistEntry::id)
                        .toSet()
                        .size)
                assertEquals(
                    listOf("destination-second"),
                    harness.playlists
                        .entries(firstImported[1].id)
                        .map(PlaylistEntry::trackId),
                )

                val repeatedPlan =
                    planPlaylistImport(
                        document = decoded.document,
                        destinationTracks = harness.destinationTracks(),
                        existingPlaylistNames =
                            harness.playlists.playlists().map { it.name },
                        importedSuffix = "Imported",
                        libraryRevision = revision,
                    )
                assertEquals(
                    listOf("Road Mix (Imported 2)", "Second (Imported 2)"),
                    repeatedPlan.playlists.map { it.name })
                val repeatedImported =
                    harness.playlists.importPlaylists(
                        repeatedPlan.playlists.map {
                            PlaylistImportMutation(it.name, it.trackIds)
                        },
                    )
                assertEquals(
                    listOf("Road Mix (Imported 2)", "Second (Imported 2)"),
                    repeatedImported.map { it.name })
                assertEquals(
                    listOf(
                        "destination-alpha",
                        "destination-beta",
                        "destination-alpha"),
                    harness.playlists
                        .entries(repeatedImported.first().id)
                        .map(PlaylistEntry::trackId),
                )
            }
        }
}

private class PlaylistBackupIntegrationHarness(
    private val database: LibraryDatabase,
) : AutoCloseable {
    private var nextId = 0
    private var nextTimestamp = 100L
    val driver: SqlDriver = database.driver
    val library = SqlDelightLibraryRepository(database)
    val playlists =
        SqlDelightPlaylistRepository(
            database,
            now = { nextTimestamp++ },
            idFactory = { "generated-${nextId++}" },
        )
    val exportedLocalOnlyCanaries: List<String>
        get() =
            exportedTrackFixtures.flatMap(ExportTrackFixture::localOnlyCanaries)

    fun seedExportAndDestinationTracks() {
        exportedTrackFixtures.forEach { fixture ->
            library.upsertSource(fixture.source)
            library.upsertTrack(fixture.track)
        }
        library.upsertSource(destinationSource)
        listOf(
                destinationTrack("destination-alpha", "  ALPHA ", 102_000),
                destinationTrack("destination-beta", "beta", 198_000),
                destinationTrack(
                    "destination-ambiguous-a", "ambiguous", 399_000),
                destinationTrack(
                    "destination-ambiguous-b", "AMBIGUOUS", 401_000),
                destinationTrack("destination-second", "second", 500_000),
            )
            .forEach(library::upsertTrack)
    }

    fun destinationTracks(): List<LibraryTrack> =
        library.tracks().filter { it.id.startsWith("destination-") }

    fun directPlaylistEntryCount(): Long =
        driver
            .executeQuery(
                identifier = null,
                sql = "SELECT COUNT(*) FROM playlist_entry",
                mapper = { cursor ->
                    check(cursor.next().value)
                    QueryResult.Value(requireNotNull(cursor.getLong(0)))
                },
                parameters = 0,
            )
            .value

    override fun close() = driver.close()

    private fun destinationTrack(
        id: String,
        title: String,
        durationMillis: Long,
    ) =
        LibraryTrack(
            id = id,
            sourceId = destinationSource.id,
            sourceLocalKey = "destination-local-key-$id",
            audioSource = AudioSource.FilePath("/destination/audio/$id.mp3"),
            displayName = "$id.mp3",
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = durationMillis,
            sizeBytes = 900_000_000_001,
            modifiedAtEpochMillis = 9_100_000_000_001,
            lastSeenScanId = "destination-scan",
            createdAtEpochMillis = 9_200_000_000_001,
            updatedAtEpochMillis = 9_300_000_000_001,
        )

    private companion object {
        val destinationSource =
            LibrarySource(
                id = "destination-source",
                platformKind = LibraryPlatformKind.JvmFolder,
                displayName = "Destination source",
                handle = "/destination",
                createdAtEpochMillis = 9_400_000_000_001,
            )

        val exportedTrackFixtures =
            listOf(
                exportTrackFixture(
                    "alpha",
                    "export-alpha",
                    "Alpha",
                    100_000,
                    1,
                    "private-scan"),
                exportTrackFixture(
                    "beta",
                    "export-beta",
                    "Beta",
                    200_000,
                    2,
                    "private-scan-beta"),
                exportTrackFixture(
                    "unmatched",
                    "export-unmatched",
                    "Missing",
                    300_000,
                    3,
                    "private-scan-unmatched"),
                exportTrackFixture(
                    "ambiguous",
                    "export-ambiguous",
                    "Ambiguous",
                    400_000,
                    4,
                    "private-scan-ambiguous"),
                exportTrackFixture(
                    "second",
                    "export-second",
                    "Second",
                    500_000,
                    5,
                    "private-scan-second"),
            )

        fun exportTrackFixture(
            label: String,
            id: String,
            title: String,
            durationMillis: Long,
            ordinal: Long,
            scanId: String,
        ): ExportTrackFixture {
            val source =
                LibrarySource(
                    id = "private-source-id-$label",
                    platformKind = LibraryPlatformKind.JvmFolder,
                    displayName = "private-source-display-$label",
                    handle = "/private/source-handle-$label",
                    createdAtEpochMillis = 8_100_000_000_000 + ordinal,
                )
            val artworkText = "private-artwork-payload-$label"
            val artworkBytes = artworkText.encodeToByteArray()
            val track =
                LibraryTrack(
                    id = id,
                    sourceId = source.id,
                    sourceLocalKey = "private-source-local-key-$label",
                    audioSource =
                        AudioSource.FilePath(
                            "/private/audio-source-path-$label.mp3"),
                    displayName = "private-track-display-$label.mp3",
                    title = title,
                    artist = "Artist",
                    album = "Album",
                    durationMillis = durationMillis,
                    sizeBytes = 8_200_000_000_000 + ordinal,
                    modifiedAtEpochMillis = 8_300_000_000_000 + ordinal,
                    lastSeenScanId = scanId,
                    createdAtEpochMillis = 8_400_000_000_000 + ordinal,
                    updatedAtEpochMillis = 8_500_000_000_000 + ordinal,
                    artworkBytes = artworkBytes,
                    artworkMimeType = "image/x-private-artwork-$label",
                )
            return ExportTrackFixture(source, track, artworkText)
        }
    }
}

private data class ExportTrackFixture(
    val source: LibrarySource,
    val track: LibraryTrack,
    val artworkText: String,
) {
    val localOnlyCanaries: List<String>
        get() =
            listOf(
                track.id,
                source.id,
                source.displayName,
                source.handle,
                source.createdAtEpochMillis.toString(),
                track.sourceLocalKey,
                track.audioSource.stableKey,
                track.displayName,
                requireNotNull(track.sizeBytes).toString(),
                requireNotNull(track.modifiedAtEpochMillis).toString(),
                requireNotNull(track.lastSeenScanId),
                track.createdAtEpochMillis.toString(),
                track.updatedAtEpochMillis.toString(),
                requireNotNull(track.artworkMimeType),
                artworkText,
                Base64.getEncoder()
                    .encodeToString(requireNotNull(track.artworkBytes)),
            )
}

private fun assertEncodedBackupExcludesLocalCanaries(
    encoded: String,
    canaries: List<String>
) {
    canaries.forEach { canary ->
        assertTrue(
            canary !in encoded, "Backup leaked local-only canary: $canary")
    }
}

private fun openHarness(): PlaylistBackupIntegrationHarness {
    val databaseFile: File =
        Files.createTempFile("rhythhaus-playlist-backup-integration", ".db")
            .toFile()
            .apply {
                delete()
                deleteOnExit()
            }
    return PlaylistBackupIntegrationHarness(LibraryDatabase(databaseFile))
}
