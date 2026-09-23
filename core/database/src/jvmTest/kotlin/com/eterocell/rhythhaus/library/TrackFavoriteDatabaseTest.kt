package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackFavoriteDatabaseTest {
    @Test
    fun currentDatabaseSupportsIdempotentFavoriteMembershipAndAbsentTrackRejection() {
        val databaseFile = temporaryDatabaseFile("rhythhaus-favorites")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            assertEquals(
                RhythHausDatabase.Schema.version,
                driverUserVersion(libraryDatabase.driver))
            assertEquals(3L, RhythHausDatabase.Schema.version)
            seedTrack(database)

            database.trackFavoriteQueries.setTrackFavorite("missing-track", 10)
            assertTrue(
                database.trackFavoriteQueries
                    .selectFavoriteTrackIds()
                    .executeAsList()
                    .isEmpty())

            database.trackFavoriteQueries.setTrackFavorite("track-1", 10)
            database.trackFavoriteQueries.setTrackFavorite("track-1", 20)
            assertEquals(
                listOf("track-1"),
                database.trackFavoriteQueries
                    .selectFavoriteTrackIds()
                    .executeAsList())
            assertEquals(
                10L,
                database.trackFavoriteQueries
                    .selectFavoriteTrack("track-1")
                    .executeAsOne()
                    .favoritedAtEpochMillis)

            database.trackFavoriteQueries.unsetTrackFavorite("track-1")
            database.trackFavoriteQueries.unsetTrackFavorite("track-1")
            assertNull(
                database.trackFavoriteQueries
                    .selectFavoriteTrack("track-1")
                    .executeAsOneOrNull())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun metadataUpsertPreservesFavoriteForTheSameTrackIdentity() {
        val databaseFile = temporaryDatabaseFile("rhythhaus-favorite-upsert")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database, title = "Before rescan")
            database.trackFavoriteQueries.setTrackFavorite("track-1", 10)

            seedTrack(database, title = "After rescan", updatedAt = 20)

            assertEquals(
                "After rescan",
                database.libraryTrackQueries
                    .selectAllTracks()
                    .executeAsOne()
                    .title)
            assertEquals(
                "track-1",
                database.trackFavoriteQueries
                    .selectFavoriteTrack("track-1")
                    .executeAsOne()
                    .trackId)
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun deletingTracksCascadesFavoriteRelationships() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-favorite-track-delete")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database)
            database.trackFavoriteQueries.setTrackFavorite("track-1", 10)

            database.libraryTrackQueries.removeTracksForSource("source-1")

            assertTrue(
                database.trackFavoriteQueries
                    .selectFavoriteTrackIds()
                    .executeAsList()
                    .isEmpty())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun deletingSourcesCascadesFavoriteRelationshipsThroughTracks() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-favorite-source-delete")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database)
            database.trackFavoriteQueries.setTrackFavorite("track-1", 10)

            database.librarySourceQueries.removeSource("source-1")

            assertTrue(
                database.trackFavoriteQueries
                    .selectFavoriteTrackIds()
                    .executeAsList()
                    .isEmpty())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    private fun seedTrack(
        database: RhythHausDatabase,
        title: String = "Track",
        updatedAt: Long = 2,
    ) {
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
            id = "track-1",
            sourceId = "source-1",
            sourceLocalKey = "track.mp3",
            audioSourceKind = "FilePath",
            audioSourceValue = "/Music/track.mp3",
            displayName = "track.mp3",
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = null,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = null,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = updatedAt,
            trackNumber = null,
            discNumber = null,
            artworkBytes = null,
            artworkMimeType = null,
        )
    }

    private fun temporaryDatabaseFile(prefix: String) =
        Files.createTempFile(prefix, ".db").toFile().also { it.delete() }

    private fun driverUserVersion(driver: SqlDriver): Long =
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
