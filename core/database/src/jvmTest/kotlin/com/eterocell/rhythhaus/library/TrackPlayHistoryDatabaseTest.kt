package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackPlayHistoryDatabaseTest {
    @Test
    fun currentDatabaseIncrementsPlayHistoryAndUpdatesTheTimestamp() {
        val databaseFile = temporaryDatabaseFile("rhythhaus-play-history")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            assertEquals(
                RhythHausDatabase.Schema.version,
                driverUserVersion(libraryDatabase.driver))
            assertEquals(4L, RhythHausDatabase.Schema.version)
            seedTrack(database)

            database.trackPlayHistoryQueries.recordTrackPlayed("track-1", 10L)
            database.trackPlayHistoryQueries.recordTrackPlayed("track-1", 20L)

            val history =
                database.trackPlayHistoryQueries
                    .selectPlayHistory()
                    .executeAsOne()
            assertEquals("track-1", history.trackId)
            assertEquals(2L, history.playCount)
            assertEquals(20L, history.lastPlayedAtEpochMillis)
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun recordTrackPlayedRejectsMissingTracksWithoutCreatingHistory() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-play-history-missing")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database

            database.trackPlayHistoryQueries.recordTrackPlayed(
                "missing-track", 10L)

            assertTrue(
                database.trackPlayHistoryQueries
                    .selectPlayHistory()
                    .executeAsList()
                    .isEmpty())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun metadataUpsertPreservesPlayHistoryForTheSameTrackIdentity() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-play-history-upsert")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database, title = "Before rescan")
            database.trackPlayHistoryQueries.recordTrackPlayed("track-1", 10L)

            seedTrack(database, title = "After rescan", updatedAt = 20L)

            assertEquals(
                "After rescan",
                database.libraryTrackQueries
                    .selectAllTracks()
                    .executeAsOne()
                    .title)
            assertEquals(
                "track-1",
                database.trackPlayHistoryQueries
                    .selectPlayHistory()
                    .executeAsOne()
                    .trackId)
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun deletingTracksCascadesPlayHistoryRelationships() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-play-history-track-delete")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database)
            database.trackPlayHistoryQueries.recordTrackPlayed("track-1", 10L)

            database.libraryTrackQueries.removeTracksForSource("source-1")

            assertTrue(
                database.trackPlayHistoryQueries
                    .selectPlayHistory()
                    .executeAsList()
                    .isEmpty())
        } finally {
            libraryDatabase.driver.close()
            databaseFile.delete()
        }
    }

    @Test
    fun deletingSourcesCascadesPlayHistoryRelationshipsThroughTracks() {
        val databaseFile =
            temporaryDatabaseFile("rhythhaus-play-history-source-delete")
        val libraryDatabase = LibraryDatabase(databaseFile)
        try {
            val database = libraryDatabase.database
            seedTrack(database)
            database.trackPlayHistoryQueries.recordTrackPlayed("track-1", 10L)

            database.librarySourceQueries.removeSource("source-1")

            assertTrue(
                database.trackPlayHistoryQueries
                    .selectPlayHistory()
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
        updatedAt: Long = 2L,
    ) {
        database.librarySourceQueries.upsertSource(
            id = "source-1",
            platformKind = "JvmFolder",
            displayName = "Music",
            handle = "/Music",
            createdAtEpochMillis = 1L,
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
            createdAtEpochMillis = 1L,
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
                        if (cursor.next().value) cursor.getLong(0) ?: 0L
                        else 0L)
                },
                parameters = 0,
            )
            .value
}
