package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import com.eterocell.rhythhaus.AudioSource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun oversizedArtworkIsNotLoadedWithTrackRows() {
        val databaseFile = Files.createTempFile("rhythhaus-library-large-artwork", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-large-artwork",
                    sourceLocalKey = "large-artwork.mp3",
                    title = "Large Artwork",
                    artist = "Artist",
                ).copy(
                    artworkBytes = ByteArray(600_000) { 1 },
                    artworkMimeType = "image/jpeg",
                ),
            )

            val track = open.repository.tracks().single()

            assertNull(track.artworkBytes)
            assertNull(track.artworkMimeType)
        }
    }

    @Test
    fun boundedArtworkIsNotLoadedWithRoutineTrackRows() {
        val databaseFile = Files.createTempFile("rhythhaus-library-bounded-artwork", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-bounded-artwork",
                    sourceLocalKey = "bounded-artwork.mp3",
                    title = "Bounded Artwork",
                    artist = "Artist",
                ).copy(
                    artworkBytes = ByteArray(128_000) { 1 },
                    artworkMimeType = "image/jpeg",
                ),
            )

            val track = open.repository.tracks().single()

            assertNull(track.artworkBytes)
            assertNull(track.artworkMimeType)
        }
    }

    @Test
    fun artworkCanBeLoadedLazilyByTrackId() {
        val databaseFile = Files.createTempFile("rhythhaus-library-lazy-artwork", ".db").toFile()
        databaseFile.deleteOnExit()
        val artworkBytes = ByteArray(128_000) { 7 }

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-lazy-artwork",
                    sourceLocalKey = "lazy-artwork.mp3",
                    title = "Lazy Artwork",
                    artist = "Artist",
                ).copy(
                    artworkBytes = artworkBytes,
                    artworkMimeType = "image/jpeg",
                ),
            )

            val routineTrack = open.repository.tracks().single()
            assertNull(routineTrack.artworkBytes)
            assertNull(routineTrack.artworkMimeType)

            val artwork = open.repository.artworkForTrack("track-lazy-artwork")
            assertNotNull(artwork)
            assertContentEquals(artworkBytes, artwork.bytes)
            assertEquals("image/jpeg", artwork.mimeType)
        }
    }

    @Test
    fun largeArtworkIsLoadedLazilyInMultipleBoundedChunks() {
        val databaseFile = Files.createTempFile("rhythhaus-library-chunked-artwork", ".db").toFile()
        databaseFile.deleteOnExit()
        val artworkBytes = ByteArray(3 * 1024 * 1024 + 137) { index ->
            (index * 31 + 17).toByte()
        }

        assertEquals(13, artworkChunkCount(artworkBytes.size.toLong()))
        assertEquals(256 * 1024, ARTWORK_CHUNK_SIZE_BYTES)

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-chunked-artwork",
                    sourceLocalKey = "chunked-artwork.mp3",
                    title = "Chunked Artwork",
                    artist = "Artist",
                ).copy(
                    artworkBytes = artworkBytes,
                    artworkMimeType = "image/png",
                ),
            )

            val metadata = open.database.libraryTrackQueries
                .selectArtworkMetadataForTrack("track-chunked-artwork")
                .executeAsOne()
            assertEquals(artworkBytes.size.toLong(), metadata.artworkByteLength)
            assertEquals("image/png", metadata.artworkMimeType)

            val firstChunk = open.database.libraryTrackQueries
                .selectArtworkChunkForTrack(
                    id = "track-chunked-artwork",
                    startPosition = "1",
                    chunkLength = ARTWORK_CHUNK_SIZE_BYTES.toString(),
                )
                .executeAsOne().artworkChunk
            val finalChunk = open.database.libraryTrackQueries
                .selectArtworkChunkForTrack(
                    id = "track-chunked-artwork",
                    startPosition = ((12L * ARTWORK_CHUNK_SIZE_BYTES) + 1L).toString(),
                    chunkLength = ARTWORK_CHUNK_SIZE_BYTES.toString(),
                )
                .executeAsOne().artworkChunk
            assertNotNull(firstChunk)
            assertNotNull(finalChunk)
            assertEquals(ARTWORK_CHUNK_SIZE_BYTES, firstChunk.size)
            assertEquals(137, finalChunk.size)

            val artwork = open.repository.artworkForTrack("track-chunked-artwork")
            assertNotNull(artwork)
            assertContentEquals(artworkBytes, artwork.bytes)
            assertEquals("image/png", artwork.mimeType)
        }
    }

    @Test
    fun removeSourceDeletesOnlySelectedSourceData() {
        val databaseFile = Files.createTempFile("rhythhaus-library-remove-source", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource(id = "source-1"))
            open.repository.upsertSource(testSource(id = "source-2"))
            open.repository.upsertTrack(testTrack(id = "track-1", sourceId = "source-1", sourceLocalKey = "one.mp3", title = "One", artist = "Artist", lastSeenScanId = "scan-1"))
            open.repository.upsertTrack(testTrack(id = "track-2", sourceId = "source-2", sourceLocalKey = "two.mp3", title = "Two", artist = "Artist", lastSeenScanId = "scan-2"))
            open.repository.insertScanSession(testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanSession(testScanSession(id = "scan-2", sourceId = "source-2"))
            open.repository.insertScanError(testScanError(id = "error-1", scanId = "scan-1"))
            open.repository.insertScanError(testScanError(id = "error-2", scanId = "scan-2"))

            open.repository.removeSource("source-1")

            assertEquals(listOf("source-2"), open.repository.sources().map { it.id })
            assertEquals(listOf("track-2"), open.repository.tracks().map { it.id })
            assertEquals(null, open.database.scanSessionQueries.selectScanSessionById("scan-1").executeAsOneOrNull())
            assertEquals("scan-2", open.database.scanSessionQueries.selectScanSessionById("scan-2").executeAsOneOrNull()?.id)
            assertEquals(emptyList(), open.repository.scanErrors("scan-1"))
            assertEquals(listOf("error-2"), open.repository.scanErrors("scan-2").map { it.id })
        }
    }

    @Test
    fun clearAllAtomicallyRemovesChildRowsBeforeSources() {
        val databaseFile = Files.createTempFile("rhythhaus-library-clear-all", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(testTrack(id = "track-1", sourceLocalKey = "one.mp3", title = "One", artist = "Artist"))
            open.repository.insertScanSession(testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanError(testScanError(id = "error-1", scanId = "scan-1"))

            open.repository.clearAll()

            assertEquals(emptyList(), open.repository.sources())
            assertEquals(emptyList(), open.repository.tracks())
            assertEquals(null, open.database.scanSessionQueries.selectScanSessionById("scan-1").executeAsOneOrNull())
            assertEquals(emptyList(), open.repository.scanErrors("scan-1"))
        }
    }

    @Test
    fun clearAllRollsBackEveryTableWhenSourceDeletionFails() {
        val databaseFile = Files.createTempFile("rhythhaus-library-clear-all-rollback", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(testTrack(id = "track-1", sourceLocalKey = "one.mp3", title = "One", artist = "Artist"))
            open.repository.insertScanSession(testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanError(testScanError(id = "error-1", scanId = "scan-1"))
            open.driver.execute(
                identifier = null,
                sql = "CREATE TRIGGER reject_source_clear BEFORE DELETE ON library_source BEGIN SELECT RAISE(ABORT, 'reject source clear'); END",
                parameters = 0,
            )

            assertFails { open.repository.clearAll() }

            assertEquals(listOf("source-1"), open.repository.sources().map { it.id })
            assertEquals(listOf("track-1"), open.repository.tracks().map { it.id })
            assertEquals("scan-1", open.database.scanSessionQueries.selectScanSessionById("scan-1").executeAsOneOrNull()?.id)
            assertEquals(listOf("error-1"), open.repository.scanErrors("scan-1").map { it.id })
        }
    }

    private fun openRepository(databaseFile: java.io.File): OpenRepository {
        val database = LibraryDatabase(databaseFile)
        return OpenRepository(
            repository = SqlDelightLibraryRepository(database),
            database = database.database,
            driver = database.driver,
        )
    }

    private class OpenRepository(
        val repository: SqlDelightLibraryRepository,
        val database: RhythHausDatabase,
        val driver: SqlDriver,
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
    sourceId: String = "source-1",
    sourceLocalKey: String,
    title: String,
    artist: String,
    lastSeenScanId: String = "scan-1",
) = LibraryTrack(
    id = id,
    sourceId = sourceId,
    sourceLocalKey = sourceLocalKey,
    audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
    displayName = sourceLocalKey,
    title = title,
    artist = artist,
    album = "Imported audio",
    durationMillis = null,
    sizeBytes = null,
    modifiedAtEpochMillis = null,
    lastSeenScanId = lastSeenScanId,
    createdAtEpochMillis = 1L,
    updatedAtEpochMillis = 2L,
)

private fun testScanSession(
    id: String,
    sourceId: String,
) = ScanSession(
    id = id,
    sourceId = sourceId,
    status = ScanStatus.Completed,
    startedAtEpochMillis = 1L,
    completedAtEpochMillis = 2L,
)

private fun testScanError(
    id: String,
    scanId: String,
) = ScanError(
    id = id,
    scanId = scanId,
    sourceLocalKey = "$id.mp3",
    displayPath = "/Music/$id.mp3",
    reason = "Test error",
    recoverable = true,
    createdAtEpochMillis = 2L,
)
