package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import com.eterocell.rhythhaus.AudioSource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightLibraryRepositoryJvmTest {
    @Test
    fun persistedDatabaseCanBeOpenedTwice() {
        val databaseFile = Files.createTempFile("rhythhaus-library", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { firstOpen ->
            firstOpen.repository.upsertSource(testSource())
            firstOpen.repository.upsertTrack(testTrack(id = "track-b", sourceLocalKey = "b.mp3", title = "Same", artist = "Beta"))
            firstOpen.repository.upsertTrack(testTrack(id = "track-a", sourceLocalKey = "a.mp3", title = "Same", artist = "Alpha"))
            assertEquals(listOf("track-a", "track-b"), firstOpen.repository.tracksForSource("source-1").map { it.id })
        }

        openRepository(databaseFile).use { secondOpen ->
            assertEquals(listOf("source-1"), secondOpen.repository.sources().map { it.id })
            assertEquals(listOf("track-a", "track-b"), secondOpen.repository.tracksForSource("source-1").map { it.id })
        }
    }

    private fun openRepository(databaseFile: java.io.File): OpenRepository {
        val database = LibraryDatabase(databaseFile)
        return OpenRepository(
            repository = SqlDelightLibraryRepository(database),
            driver = database.driver,
        )
    }

    private class OpenRepository(
        val repository: SqlDelightLibraryRepository,
        private val driver: SqlDriver,
    ) : AutoCloseable {
        override fun close() {
            driver.close()
        }
    }
}

private fun testSource(
    id: String = "source-1",
) = LibrarySource(
    id = id,
    platformKind = LibraryPlatformKind.JvmFolder,
    displayName = "Music",
    handle = "/Music",
    createdAtEpochMillis = 1L,
)

private fun testTrack(
    id: String,
    sourceLocalKey: String,
    title: String,
    artist: String,
) = LibraryTrack(
    id = id,
    sourceId = "source-1",
    sourceLocalKey = sourceLocalKey,
    audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
    displayName = sourceLocalKey,
    title = title,
    artist = artist,
    album = "Imported audio",
    durationMillis = null,
    sizeBytes = null,
    modifiedAtEpochMillis = null,
    lastSeenScanId = "scan-1",
    createdAtEpochMillis = 1L,
    updatedAtEpochMillis = 2L,
)
