package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ExistingDatabaseMigrationTest {
    @Test
    fun versionOneFixtureMigratesWithoutLosingRowsAndPreservesForeignKeys() {
        val databaseFile = copyVersionOneDatabase()
        assertEquals(1L, jdbcUserVersion(databaseFile))
        seedVersionOneLibrary(databaseFile)

        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            assertEquals(
                "source-1",
                database.librarySourceQueries
                    .selectAllSources()
                    .executeAsOne()
                    .id)
            assertEquals(
                "legacy-track",
                database.libraryTrackQueries
                    .selectAllTracks()
                    .executeAsOne()
                    .id)
            assertEquals(
                RhythHausDatabase.Schema.version,
                driverUserVersion(libraryDatabase.driver))
            assertEquals(RhythHausDatabase.Schema.version, 2L)

            database.playlistQueries.insertPlaylist(
                "playlist-1", "Migrated", 1, 1)
            database.playlistQueries.insertEntry(
                "entry-1", "playlist-1", "legacy-track", 0, 1)
            assertEquals(
                "legacy-track",
                database.playlistQueries
                    .selectEntries("playlist-1")
                    .executeAsOne()
                    .trackId)
            assertFails {
                database.playlistQueries.insertEntry(
                    "invalid", "playlist-1", "missing-track", 1, 1)
            }

            database.libraryTrackQueries.removeTracksForSource("source-1")
            assertTrue(
                database.playlistQueries
                    .selectEntries("playlist-1")
                    .executeAsList()
                    .isEmpty())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun legacyVersionZeroDatabaseBootstrapsAtTheCurrentSchemaVersion() {
        val databaseFile =
            Files.createTempFile("rhythhaus-legacy-v0", ".db").toFile()
        databaseFile.delete()
        val rawDriver =
            JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        try {
            RhythHausDatabase.Schema.create(rawDriver).value
            rawDriver.execute(
                null,
                "INSERT INTO library_source(id, platformKind, displayName, handle, createdAtEpochMillis, accessStatus) VALUES ('source-1', 'JvmFolder', 'Music', '/Music', 1, 'Available')",
                0,
            )
            rawDriver.execute(
                null,
                "INSERT INTO library_track(id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('legacy-track', 'source-1', 'legacy.mp3', 'FilePath', '/Music/legacy.mp3', 'legacy.mp3', 'Legacy', 'Artist', 'Album', 1, 2)",
                0,
            )
        } finally {
            rawDriver.close()
        }

        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            assertEquals(
                "source-1",
                libraryDatabase.database.librarySourceQueries
                    .selectAllSources()
                    .executeAsOne()
                    .id)
            assertEquals(
                "legacy-track",
                libraryDatabase.database.libraryTrackQueries
                    .selectAllTracks()
                    .executeAsOne()
                    .id)
            assertEquals(
                RhythHausDatabase.Schema.version,
                driverUserVersion(libraryDatabase.driver))
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun generatedDatabaseIdentityAndFilenameRemainStable() {
        val generatedType: RhythHausDatabase? = null

        assertEquals(null, generatedType)
        assertEquals("rhythhaus.db", libraryDatabaseFileName)
    }

    private fun copyVersionOneDatabase(): File {
        val target = Files.createTempFile("rhythhaus-v1", ".db").toFile()
        Files.copy(
            File("src/commonMain/sqldelight/databases/1.db").toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
        return target
    }

    private fun seedVersionOneLibrary(databaseFile: File) {
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
            .use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA foreign_keys = ON")
                    statement.execute(
                        "INSERT INTO library_source(id, platformKind, displayName, handle, createdAtEpochMillis, accessStatus) VALUES ('source-1', 'JvmFolder', 'Music', '/Music', 1, 'Available')",
                    )
                    statement.execute(
                        "INSERT INTO library_track(id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('legacy-track', 'source-1', 'legacy.mp3', 'FilePath', '/Music/legacy.mp3', 'legacy.mp3', 'Legacy', 'Artist', 'Album', 1, 2)",
                    )
                }
            }
    }

    private fun jdbcUserVersion(databaseFile: File): Long =
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
            .use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version").use { result
                        ->
                        check(result.next())
                        result.getLong(1)
                    }
                }
            }

    private fun driverUserVersion(
        driver: app.cash.sqldelight.db.SqlDriver
    ): Long =
        driver
            .executeQuery(
                identifier = null,
                sql = "PRAGMA user_version",
                mapper = { cursor ->
                    QueryResult.Value(
                        if (cursor.next().value) cursor.getLong(0) ?: 0 else 0)
                },
                parameters = 0,
            )
            .value
}
