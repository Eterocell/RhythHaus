package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackMetadataOverrideDatabaseTest {
    @Test
    fun versionFiveAndUnversionedSmartPlaylistDatabasesMigrateWithoutLosingLibraryRows() {
        for (priorVersion in listOf(5, 0)) {
            val file =
                Files.createTempFile("rhythhaus-metadata-v$priorVersion", ".db")
                    .toFile()
            Files.copy(
                File("src/commonMain/sqldelight/databases/4.db").toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING)
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
                .use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute(
                            "CREATE TABLE smart_playlist (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, ruleKind TEXT NOT NULL, argument TEXT, secondArgument TEXT, itemCount INTEGER, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
                        statement.execute(
                            "INSERT INTO library_source(id, platformKind, displayName, handle, createdAtEpochMillis, accessStatus) VALUES ('source', 'JvmFolder', 'Music', '/Music', 1, 'Available')")
                        statement.execute(
                            "INSERT INTO library_track(id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('track', 'source', 'music.mp3', 'FilePath', '/Music/music.mp3', 'music.mp3', 'Original', 'Artist', 'Album', 1, 2)")
                        statement.execute("PRAGMA user_version = $priorVersion")
                    }
                }
            val database = LibraryDatabase(file)
            try {
                assertEquals(
                    RhythHausDatabase.Schema.version, version(database.driver))
                assertEquals(
                    "Original",
                    database.database.libraryTrackQueries
                        .selectAllTracks()
                        .executeAsOne()
                        .title)
                assertNull(
                    database.database.trackMetadataOverrideQueries
                        .selectOverrideForTrack("track")
                        .executeAsOneOrNull())
                database.database.trackMetadataOverrideQueries.upsertOverride(
                    "track", "Corrected", null, null, null, null)
                assertEquals(
                    "Corrected",
                    database.database.trackMetadataOverrideQueries
                        .selectOverrideForTrack("track")
                        .executeAsOne()
                        .title)
            } finally {
                database.driver.close()
                file.delete()
            }
        }
    }

    @Test
    fun trackDeletionCascadesCorrectionThroughSourceAndRemoveMissing() {
        val file =
            Files.createTempFile("rhythhaus-metadata-cascade", ".db")
                .toFile()
                .also { it.delete() }
        val database = LibraryDatabase(file)
        try {
            val queries = database.database
            queries.librarySourceQueries.upsertSource(
                "source", "JvmFolder", "Music", "/Music", 1, null, "Available")
            fun insert(id: String) {
                queries.libraryTrackQueries.upsertTrack(
                    id,
                    "source",
                    "$id.mp3",
                    "FilePath",
                    "/Music/$id.mp3",
                    "$id.mp3",
                    "Raw",
                    "Artist",
                    "Album",
                    null,
                    null,
                    null,
                    "previous-scan",
                    1,
                    1,
                    null,
                    null,
                    null,
                    null)
                queries.trackMetadataOverrideQueries.upsertOverride(
                    id, "Corrected", null, null, 2L, null)
            }
            insert("missing")
            queries.libraryTrackQueries.removeMissingTracks(
                "source", "latest-scan")
            assertNull(
                queries.trackMetadataOverrideQueries
                    .selectOverrideForTrack("missing")
                    .executeAsOneOrNull())
            insert("source-deleted")
            queries.librarySourceQueries.removeSource("source")
            assertNull(
                queries.trackMetadataOverrideQueries
                    .selectOverrideForTrack("source-deleted")
                    .executeAsOneOrNull())
            assertTrue(
                queries.trackMetadataOverrideQueries
                    .selectAllOverrides()
                    .executeAsList()
                    .isEmpty())
        } finally {
            database.driver.close()
            file.delete()
        }
    }

    private fun version(driver: SqlDriver): Long =
        driver
            .executeQuery(
                null,
                "PRAGMA user_version",
                { cursor ->
                    QueryResult.Value(
                        if (cursor.next().value) cursor.getLong(0) ?: 0L
                        else 0L)
                },
                0)
            .value
}
