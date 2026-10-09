package com.eterocell.rhythhaus.library

import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayGainDatabaseTest {
    @Test
    fun scannedFactsSurviveReopenInEveryTrackProjectionAndRemovedTagsClear() {
        val file = Files.createTempFile("rhythhaus-replaygain", ".db").toFile()
        try {
            LibraryDatabase(file).let { opened ->
                try {
                    opened.database.librarySourceQueries.upsertSource(
                        "source",
                        "JvmFolder",
                        "Music",
                        "/Music",
                        1L,
                        null,
                        "Available")
                    upsertTrack(opened.database, -6.0, 1.2, -4.0, 1.1)
                } finally {
                    opened.driver.close()
                }
            }
            LibraryDatabase(file).let { reopened ->
                try {
                    assertFactsInEveryProjection(
                        reopened.database, listOf(-6.0, 1.2, -4.0, 1.1))
                    upsertTrack(reopened.database)
                } finally {
                    reopened.driver.close()
                }
            }
            LibraryDatabase(file).let { cleared ->
                try {
                    assertFactsInEveryProjection(
                        cleared.database, listOf(null, null, null, null))
                    assertEquals(
                        "track",
                        cleared.database.libraryTrackQueries
                            .selectAllTracks()
                            .executeAsOne()
                            .id)
                } finally {
                    cleared.driver.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun unversionedReplayGainDatabaseReopensWithoutReapplyingMigrationOrLosingFacts() {
        val file =
            Files.createTempFile("rhythhaus-replaygain-v0", ".db").toFile()
        try {
            LibraryDatabase(file).let { opened ->
                try {
                    opened.database.librarySourceQueries.upsertSource(
                        "source",
                        "JvmFolder",
                        "Music",
                        "/Music",
                        1L,
                        null,
                        "Available")
                    upsertTrack(opened.database, -6.0, 1.2, -4.0, 1.1)
                } finally {
                    opened.driver.close()
                }
            }
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
                .use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("PRAGMA user_version = 0")
                    }
                }
            LibraryDatabase(file).let { reopened ->
                try {
                    assertFactsInEveryProjection(
                        reopened.database, listOf(-6.0, 1.2, -4.0, 1.1))
                    assertVersionEight(file)
                } finally {
                    reopened.driver.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    private fun upsertTrack(
        database: RhythHausDatabase,
        trackGain: Double? = null,
        trackPeak: Double? = null,
        albumGain: Double? = null,
        albumPeak: Double? = null,
    ) {
        database.libraryTrackQueries.upsertTrack(
            id = "track",
            sourceId = "source",
            sourceLocalKey = "song.flac",
            audioSourceKind = "FilePath",
            audioSourceValue = "/Music/song.flac",
            displayName = "song.flac",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationMillis = 123_000L,
            sizeBytes = 100L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            trackNumber = 1L,
            discNumber = 1L,
            artworkBytes = null,
            artworkMimeType = null,
            replayGainTrackGainDb = trackGain,
            replayGainTrackPeak = trackPeak,
            replayGainAlbumGainDb = albumGain,
            replayGainAlbumPeak = albumPeak,
        )
    }

    private fun assertFactsInEveryProjection(
        database: RhythHausDatabase,
        expected: List<Double?>
    ) {
        val all = database.libraryTrackQueries.selectAllTracks().executeAsOne()
        assertEquals(
            expected,
            listOf(
                all.replayGainTrackGainDb,
                all.replayGainTrackPeak,
                all.replayGainAlbumGainDb,
                all.replayGainAlbumPeak))
        val source =
            database.libraryTrackQueries
                .selectTracksForSource("source")
                .executeAsOne()
        assertEquals(
            expected,
            listOf(
                source.replayGainTrackGainDb,
                source.replayGainTrackPeak,
                source.replayGainAlbumGainDb,
                source.replayGainAlbumPeak))
        val key =
            database.libraryTrackQueries
                .selectTrackBySourceKey("source", "song.flac")
                .executeAsOne()
        assertEquals(
            expected,
            listOf(
                key.replayGainTrackGainDb,
                key.replayGainTrackPeak,
                key.replayGainAlbumGainDb,
                key.replayGainAlbumPeak))
        val editor =
            database.libraryTrackQueries
                .selectTrackForMetadataEditor("track")
                .executeAsOne()
        assertEquals(
            expected,
            listOf(
                editor.replayGainTrackGainDb,
                editor.replayGainTrackPeak,
                editor.replayGainAlbumGainDb,
                editor.replayGainAlbumPeak))
    }

    private fun assertVersionEight(file: File) {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA user_version").use { result ->
                    check(result.next())
                    assertEquals(8, result.getInt(1))
                }
            }
        }
    }
}
