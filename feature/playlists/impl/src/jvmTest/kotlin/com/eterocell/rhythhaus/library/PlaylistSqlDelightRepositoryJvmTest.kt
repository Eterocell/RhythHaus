package com.eterocell.rhythhaus.library

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PlaylistSqlDelightRepositoryJvmTest {
    @Test
    fun sqlRepositoryUsesLegacyMissingPlaylistAndEntryMessages() {
        openRepositories().use { open ->
            assertEquals(
                "Playlist not found: missing-rename",
                assertFailsWith<IllegalArgumentException> {
                        open.playlists.rename("missing-rename", "Renamed")
                    }
                    .message,
            )
            assertEquals(
                "Playlist not found: missing-append",
                assertFailsWith<IllegalArgumentException> {
                        open.playlists.append(
                            "missing-append", listOf("track-a"))
                    }
                    .message,
            )
            assertEquals(
                "Playlist entry not found: missing-entry",
                assertFailsWith<IllegalArgumentException> {
                        open.playlists.removeEntry("missing-entry")
                    }
                    .message,
            )

            open.seedTrack("track-a", "scan-1")
            val playlist = open.playlists.create("Road trip")
            open.playlists.append(playlist.id, listOf("track-a"))
            assertEquals(
                "Playlist entry not found: missing-reorder-entry",
                assertFailsWith<IllegalArgumentException> {
                        open.playlists.reorder(
                            playlist.id, listOf("missing-reorder-entry"))
                    }
                    .message,
            )
        }
    }

    @Test
    fun concurrentAppendsCannotOverwriteFromAStaleSnapshot() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            open.seedTrack("track-b", "scan-1")
            val playlist = open.playlists.create("Road trip")
            val secondRepository =
                SqlDelightPlaylistRepository(
                    open.libraryDatabase,
                    now = { 200L },
                    idFactory = { "entry-b" },
                )
            open.playlists.mutationReadObserver = {
                open.playlists.mutationReadObserver = {}
                secondRepository.append(playlist.id, listOf("track-b"))
            }

            open.playlists.append(playlist.id, listOf("track-a"))

            assertEquals(
                listOf("track-b", "track-a"),
                open.playlists.entries(playlist.id).map(PlaylistEntry::trackId))
        }
    }

    @Test
    fun sqlRepositoryPreservesDuplicateEntriesAndTrimsNames() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            val playlist = open.playlists.create(" Road trip ")

            open.playlists.append(playlist.id, listOf("track-a", "track-a"))

            val entries = open.playlists.entries(playlist.id)
            assertEquals(
                "Road trip", open.playlists.playlist(playlist.id)?.name)
            assertEquals(
                listOf("track-a", "track-a"),
                entries.map(PlaylistEntry::trackId))
            assertEquals(2, entries.map(PlaylistEntry::id).toSet().size)
            assertEquals(listOf(0, 1), entries.map(PlaylistEntry::position))
        }
    }

    @Test
    fun sqlRepositoryRejectsBlankRenameWithoutChangingConfirmedState() {
        openRepositories().use { open ->
            val playlist = open.playlists.create("Road trip")

            assertFails { open.playlists.rename(playlist.id, "   ") }

            assertEquals(
                "Road trip", open.playlists.playlist(playlist.id)?.name)
        }
    }

    @Test
    fun sqlRepositoryRemoveAndReorderKeepContiguousPositions() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            open.seedTrack("track-b", "scan-1")
            val playlist = open.playlists.create("Road trip")
            open.playlists.append(
                playlist.id, listOf("track-a", "track-b", "track-a"))
            val entries = open.playlists.entries(playlist.id)

            open.playlists.removeEntry(entries[1].id)
            open.playlists.reorder(
                playlist.id, listOf(entries[2].id, entries[0].id))

            val reordered = open.playlists.entries(playlist.id)
            assertEquals(
                listOf(entries[2].id, entries[0].id),
                reordered.map(PlaylistEntry::id))
            assertEquals(listOf(0, 1), reordered.map(PlaylistEntry::position))
        }
    }

    @Test
    fun sqlRepositoryRemovingFinalEntryRetainsEmptyPlaylist() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            val playlist = open.playlists.create("Road trip")
            open.playlists.append(playlist.id, listOf("track-a"))

            open.playlists.removeEntry(
                open.playlists.entries(playlist.id).single().id)

            assertNotNull(open.playlists.playlist(playlist.id))
            assertEquals(emptyList(), open.playlists.entries(playlist.id))
        }
    }

    @Test
    fun playlistDeletionCascadesEntries() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            val playlist = open.playlists.create("Road trip")
            open.playlists.append(playlist.id, listOf("track-a"))

            open.playlists.delete(playlist.id)

            assertEquals(emptyList(), open.playlists.entries(playlist.id))
        }
    }

    @Test
    fun failedReorderRollsBackTheCompleteSequence() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")
            open.seedTrack("track-b", "scan-1")
            val playlist = open.playlists.create("Road trip")
            open.playlists.append(playlist.id, listOf("track-a", "track-b"))
            val before = open.playlists.entries(playlist.id)

            open.database.playlistQueries.deletePlaylist("failure-target")
            open.database.playlistQueries.insertPlaylist(
                "failure-target", "Failure", 1, 1)
            open.database.playlistQueries.insertEntry(
                "failure-entry", "failure-target", "track-a", 0, 1)
            open.driver.execute(
                identifier = null,
                sql =
                    "CREATE TRIGGER fail_second_entry BEFORE INSERT ON playlist_entry WHEN NEW.position = 1 BEGIN SELECT RAISE(ABORT, 'forced reorder failure'); END",
                parameters = 0,
            )

            assertFails {
                open.playlists.reorder(
                    playlist.id, listOf(before.last().id, before.first().id))
            }

            assertEquals(before, open.playlists.entries(playlist.id))
        }
    }

    @Test
    fun createWithEntriesRollsBackPlaylistWhenInitialEntryForeignKeyFails() {
        openRepositories().use { open ->
            val before = open.playlists.playlists()

            assertFails {
                open.playlists.createWithEntries(
                    "Transient", listOf("missing-track"))
            }

            assertEquals(before, open.playlists.playlists())
        }
    }

    @Test
    fun createWithEntriesPreservesDuplicateEntriesAndNonuniqueNamesInSql() {
        openRepositories().use { open ->
            open.seedTrack("track-a", "scan-1")

            val first =
                open.playlists.createWithEntries(
                    " Same ", listOf("track-a", "track-a"))
            val second =
                open.playlists.createWithEntries("Same", listOf("track-a"))

            assertEquals(
                listOf("Same", "Same"),
                open.playlists.playlists().map(PlaylistSummary::name))
            assertEquals(
                listOf("track-a", "track-a"),
                open.playlists.entries(first.id).map(PlaylistEntry::trackId))
            assertEquals(
                listOf(0, 1),
                open.playlists.entries(first.id).map(PlaylistEntry::position))
            assertEquals(
                listOf("track-a"),
                open.playlists.entries(second.id).map(PlaylistEntry::trackId))
        }
    }

    @Test
    fun importPlaylistsRollsBackEverySqlRowOnSecondPlaylistEntryFailureAndRetryCreatesAllOnce() {
        openRepositories().use { open ->
            listOf("track-existing", "track-a", "track-b", "track-c").forEach {
                open.seedTrack(it, "scan-1")
            }
            val existing =
                open.playlists.createWithEntries(
                    "Existing", listOf("track-existing"))
            val existingPlaylists = open.playlists.playlists()
            val existingEntries = open.playlists.entries(existing.id)
            val request =
                listOf(
                    PlaylistImportMutation(
                        " First ", listOf("track-a", "track-a")),
                    PlaylistImportMutation(
                        "Second", listOf("track-b", "track-c")),
                )
            open.driver.execute(
                identifier = null,
                sql =
                    "CREATE TRIGGER fail_second_import_entry BEFORE INSERT ON playlist_entry WHEN NEW.trackId = 'track-b' BEGIN SELECT RAISE(ABORT, 'forced second playlist entry failure'); END",
                parameters = 0,
            )

            assertFails { open.playlists.importPlaylists(request) }

            assertEquals(existingPlaylists, open.playlists.playlists())
            assertEquals(existingEntries, open.playlists.entries(existing.id))
            open.driver.execute(
                identifier = null,
                sql = "DROP TRIGGER fail_second_import_entry",
                parameters = 0,
            )

            val imported = open.playlists.importPlaylists(request)

            assertEquals(
                listOf("First", "Second"), imported.map(PlaylistSummary::name))
            assertEquals(
                setOf("Existing", "First", "Second"),
                open.playlists.playlists().map(PlaylistSummary::name).toSet())
            assertEquals(3, open.playlists.playlists().size)
            assertEquals(
                listOf("track-a", "track-a"),
                open.playlists
                    .entries(imported[0].id)
                    .map(PlaylistEntry::trackId))
            assertEquals(
                listOf(0, 1),
                open.playlists
                    .entries(imported[0].id)
                    .map(PlaylistEntry::position))
            assertEquals(
                2,
                open.playlists
                    .entries(imported[0].id)
                    .map(PlaylistEntry::id)
                    .toSet()
                    .size)
            assertEquals(
                listOf("track-b", "track-c"),
                open.playlists
                    .entries(imported[1].id)
                    .map(PlaylistEntry::trackId))
        }
    }

    private fun openRepositories(
        databaseFile: File = tempDatabase()
    ): OpenRepositories {
        val database = LibraryDatabase(databaseFile)
        return OpenRepositories(
            libraryDatabase = database,
            database = database.database,
            playlists =
                SqlDelightPlaylistRepository(
                    database, now = { 100L }, idFactory = ::uuid4),
            driver = database.driver,
            close = { database.driver.close() },
        )
    }

    private class OpenRepositories(
        val libraryDatabase: LibraryDatabase,
        val database: RhythHausDatabase,
        val playlists: SqlDelightPlaylistRepository,
        val driver: app.cash.sqldelight.db.SqlDriver,
        private val close: () -> Unit,
    ) : AutoCloseable {
        fun seedTrack(id: String, scanId: String) {
            database.librarySourceQueries.upsertSource(
                id = "source-1",
                platformKind = "JvmFolder",
                displayName = "Music",
                handle = "/Music",
                createdAtEpochMillis = 1,
                lastScanAtEpochMillis = null,
                accessStatus = "Available",
            )
            database.libraryTrackQueries.upsertTrack(
                id = id,
                sourceId = "source-1",
                sourceLocalKey = "$id.mp3",
                audioSourceKind = "FilePath",
                audioSourceValue = "/Music/$id.mp3",
                displayName = "$id.mp3",
                title = id,
                artist = "Artist",
                album = "Album",
                durationMillis = null,
                sizeBytes = null,
                modifiedAtEpochMillis = null,
                lastSeenScanId = scanId,
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 2,
                trackNumber = null,
                discNumber = null,
                artworkBytes = null,
                artworkMimeType = null,
            )
        }

        override fun close() = close.invoke()
    }
}

private fun tempDatabase(): File =
    Files.createTempFile("rhythhaus-playlist", ".db").toFile().apply {
        delete()
        deleteOnExit()
    }
