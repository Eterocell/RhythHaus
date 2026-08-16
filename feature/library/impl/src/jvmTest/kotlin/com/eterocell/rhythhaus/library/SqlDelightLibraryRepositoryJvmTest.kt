package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.AudioMetadataReader
import com.eterocell.rhythhaus.library.impl.PlatformAudioScanner
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlDelightLibraryRepositoryJvmTest {
    @Test
    fun completedScanTerminalSourceUpdatePreservesPersistedChildren() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-scan-cascade", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            val source = testSource()
            val scanner =
                LibraryScanner(
                    repository = open.repository,
                    platformScanner =
                        PlatformAudioScanner {
                            sequenceOf(
                                PlatformScanEvent.FolderVisited("/Music"),
                                PlatformScanEvent.AudioCandidate(
                                    AudioScanCandidate(
                                        sourceId = source.id,
                                        sourceLocalKey = "song.mp3",
                                        displayPath = "/Music/song.mp3",
                                        displayName = "song.mp3",
                                        audioSource =
                                            AudioSource.FilePath(
                                                "/Music/song.mp3"),
                                    ),
                                ),
                                PlatformScanEvent.Skipped(
                                    sourceLocalKey = "unsupported.txt",
                                    displayPath = "/Music/unsupported.txt",
                                    reason = "Unsupported file",
                                    recoverable = true,
                                ),
                            )
                        },
                    metadataReader =
                        AudioMetadataReader(
                            tagLibReader = UnsupportedTagLibReader,
                            platformMetadataReader = { null },
                        ),
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-id" },
                )

            val result = scanner.scan(source)

            assertEquals(
                100L, open.repository.sources().single().lastScanAtEpochMillis)
            assertEquals(ScanStatus.Completed, result.status)
            assertEquals(
                listOf(
                    listOf("track-id"),
                    ScanStatus.Completed.name,
                    listOf("scan-error-id")),
                listOf(
                    open.repository.tracksForSource(source.id).map { it.id },
                    open.database.scanSessionQueries
                        .selectScanSessionById("scan-id")
                        .executeAsOneOrNull()
                        ?.status,
                    open.repository.scanErrors("scan-id").map { it.id },
                ),
            )
        }
    }

    @Test
    fun persistedDatabaseCanBeOpenedTwice() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { firstOpen ->
            firstOpen.repository.upsertSource(testSource())
            firstOpen.repository.upsertTrack(
                testTrack(
                    id = "track-b",
                    sourceLocalKey = "b.mp3",
                    title = "Same",
                    artist = "Beta"))
            firstOpen.repository.upsertTrack(
                testTrack(
                    id = "track-a",
                    sourceLocalKey = "a.mp3",
                    title = "Same",
                    artist = "Alpha"))
            assertEquals(
                listOf("track-a", "track-b"),
                firstOpen.repository.tracksForSource("source-1").map { it.id })
        }

        openRepository(databaseFile).use { secondOpen ->
            assertEquals(
                listOf("source-1"),
                secondOpen.repository.sources().map { it.id })
            assertEquals(
                listOf("track-a", "track-b"),
                secondOpen.repository.tracksForSource("source-1").map { it.id })
        }
    }

    @Test
    fun oversizedArtworkIsNotLoadedWithTrackRows() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-large-artwork", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                        id = "track-large-artwork",
                        sourceLocalKey = "large-artwork.mp3",
                        title = "Large Artwork",
                        artist = "Artist",
                    )
                    .copy(
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
        val databaseFile =
            Files.createTempFile("rhythhaus-library-bounded-artwork", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                        id = "track-bounded-artwork",
                        sourceLocalKey = "bounded-artwork.mp3",
                        title = "Bounded Artwork",
                        artist = "Artist",
                    )
                    .copy(
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
        val databaseFile =
            Files.createTempFile("rhythhaus-library-lazy-artwork", ".db")
                .toFile()
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
                    )
                    .copy(
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
        val databaseFile =
            Files.createTempFile("rhythhaus-library-chunked-artwork", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        val artworkBytes =
            ByteArray(3 * 1024 * 1024 + 137) { index ->
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
                    )
                    .copy(
                        artworkBytes = artworkBytes,
                        artworkMimeType = "image/png",
                    ),
            )

            val metadata =
                open.database.libraryTrackQueries
                    .selectArtworkMetadataForTrack("track-chunked-artwork")
                    .executeAsOne()
            assertEquals(artworkBytes.size.toLong(), metadata.artworkByteLength)
            assertEquals("image/png", metadata.artworkMimeType)

            val firstChunk =
                open.database.libraryTrackQueries
                    .selectArtworkChunkForTrack(
                        id = "track-chunked-artwork",
                        startPosition = "1",
                        chunkLength = ARTWORK_CHUNK_SIZE_BYTES.toString(),
                    )
                    .executeAsOne()
                    .artworkChunk
            val finalChunk =
                open.database.libraryTrackQueries
                    .selectArtworkChunkForTrack(
                        id = "track-chunked-artwork",
                        startPosition =
                            ((12L * ARTWORK_CHUNK_SIZE_BYTES) + 1L).toString(),
                        chunkLength = ARTWORK_CHUNK_SIZE_BYTES.toString(),
                    )
                    .executeAsOne()
                    .artworkChunk
            assertNotNull(firstChunk)
            assertNotNull(finalChunk)
            assertEquals(ARTWORK_CHUNK_SIZE_BYTES, firstChunk.size)
            assertEquals(137, finalChunk.size)

            val artwork =
                open.repository.artworkForTrack("track-chunked-artwork")
            assertNotNull(artwork)
            assertContentEquals(artworkBytes, artwork.bytes)
            assertEquals("image/png", artwork.mimeType)
        }
    }

    @Test
    fun removeSourceDeletesOnlySelectedSourceData() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-remove-source", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource(id = "source-1"))
            open.repository.upsertSource(testSource(id = "source-2"))
            open.repository.upsertTrack(
                testTrack(
                    id = "track-1",
                    sourceId = "source-1",
                    sourceLocalKey = "one.mp3",
                    title = "One",
                    artist = "Artist",
                    lastSeenScanId = "scan-1"))
            open.repository.upsertTrack(
                testTrack(
                    id = "track-2",
                    sourceId = "source-2",
                    sourceLocalKey = "two.mp3",
                    title = "Two",
                    artist = "Artist",
                    lastSeenScanId = "scan-2"))
            open.repository.insertScanSession(
                testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanSession(
                testScanSession(id = "scan-2", sourceId = "source-2"))
            open.repository.insertScanError(
                testScanError(id = "error-1", scanId = "scan-1"))
            open.repository.insertScanError(
                testScanError(id = "error-2", scanId = "scan-2"))

            open.repository.removeSource("source-1")

            assertEquals(
                listOf("source-2"), open.repository.sources().map { it.id })
            assertEquals(
                listOf("track-2"), open.repository.tracks().map { it.id })
            assertEquals(
                null,
                open.database.scanSessionQueries
                    .selectScanSessionById("scan-1")
                    .executeAsOneOrNull())
            assertEquals(
                "scan-2",
                open.database.scanSessionQueries
                    .selectScanSessionById("scan-2")
                    .executeAsOneOrNull()
                    ?.id)
            assertEquals(emptyList(), open.repository.scanErrors("scan-1"))
            assertEquals(
                listOf("error-2"),
                open.repository.scanErrors("scan-2").map { it.id })
        }
    }

    @Test
    fun clearAllAtomicallyRemovesChildRowsBeforeSources() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-clear-all", ".db").toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "One",
                    artist = "Artist"))
            open.repository.insertScanSession(
                testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanError(
                testScanError(id = "error-1", scanId = "scan-1"))

            open.repository.clearAll()

            assertEquals(emptyList(), open.repository.sources())
            assertEquals(emptyList(), open.repository.tracks())
            assertEquals(
                null,
                open.database.scanSessionQueries
                    .selectScanSessionById("scan-1")
                    .executeAsOneOrNull())
            assertEquals(emptyList(), open.repository.scanErrors("scan-1"))
        }
    }

    @Test
    fun clearAllRollsBackEveryTableWhenSourceDeletionFails() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-clear-all-rollback", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "One",
                    artist = "Artist"))
            open.repository.insertScanSession(
                testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanError(
                testScanError(id = "error-1", scanId = "scan-1"))
            open.driver.execute(
                identifier = null,
                sql =
                    "CREATE TRIGGER reject_source_clear BEFORE DELETE ON library_source BEGIN SELECT RAISE(ABORT, 'reject source clear'); END",
                parameters = 0,
            )

            assertFails { open.repository.clearAll() }

            assertEquals(
                listOf("source-1"), open.repository.sources().map { it.id })
            assertEquals(
                listOf("track-1"), open.repository.tracks().map { it.id })
            assertEquals(
                "scan-1",
                open.database.scanSessionQueries
                    .selectScanSessionById("scan-1")
                    .executeAsOneOrNull()
                    ?.id)
            assertEquals(
                listOf("error-1"),
                open.repository.scanErrors("scan-1").map { it.id })
        }
    }

    @Test
    fun removeMissingRequiresLatestCompletedSessionAndReturnsZeroWhenNothingIsMissing() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-remove-missing", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "One",
                    artist = "Artist",
                    lastSeenScanId = "scan-latest",
                ),
            )
            open.repository.insertScanSession(
                testScanSession(
                    id = "scan-stale",
                    sourceId = "source-1",
                    startedAtEpochMillis = 1L,
                    completedAtEpochMillis = 2L,
                ),
            )
            open.repository.insertScanSession(
                testScanSession(
                    id = "scan-latest",
                    sourceId = "source-1",
                    startedAtEpochMillis = 2L,
                    completedAtEpochMillis = 3L,
                ),
            )

            assertEquals(
                RemoveMissingTracksResult.Rejected(
                    RemoveMissingTracksRejectionReason.StaleCompletedScan),
                open.repository.removeMissingTracks("source-1", "scan-stale"),
            )
            assertEquals(
                RemoveMissingTracksResult.Removed(0),
                open.repository.removeMissingTracks("source-1", "scan-latest"),
            )
            assertEquals(
                listOf("track-1"),
                open.repository.tracksForSource("source-1").map { it.id })
        }
    }

    @Test
    fun removeMissingRejectsCompleteMatrixWithoutDeletingTracks() {
        val databaseFile =
            Files.createTempFile(
                    "rhythhaus-library-remove-missing-matrix", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertSource(testSource(id = "source-2"))
            open.repository.upsertTrack(
                testTrack(
                    "missing",
                    "source-1",
                    "missing.mp3",
                    "Missing",
                    "Artist",
                    "old"))
            val sessions =
                listOf(
                    testScanSession(
                        "foreign", "source-2", completedAtEpochMillis = 5L),
                    testScanSession(
                        "active",
                        "source-1",
                        ScanStatus.Scanning,
                        completedAtEpochMillis = null),
                    testScanSession(
                        "cancelling",
                        "source-1",
                        ScanStatus.Cancelling,
                        completedAtEpochMillis = null),
                    testScanSession(
                        "cancelled",
                        "source-1",
                        ScanStatus.Cancelled,
                        completedAtEpochMillis = 3L),
                    testScanSession(
                        "failed",
                        "source-1",
                        ScanStatus.Failed,
                        completedAtEpochMillis = 4L),
                    testScanSession(
                        "malformed",
                        "source-1",
                        ScanStatus.Completed,
                        startedAtEpochMillis = 2L,
                        completedAtEpochMillis = null),
                    testScanSession(
                        "stale", "source-1", completedAtEpochMillis = 5L),
                    testScanSession(
                        "latest",
                        "source-1",
                        startedAtEpochMillis = 2L,
                        completedAtEpochMillis = 6L),
                )
            sessions.forEach(open.repository::insertScanSession)

            val requests =
                listOf(
                    Triple(
                        "missing-source",
                        "unknown",
                        RemoveMissingTracksRejectionReason.UnknownSource),
                    Triple(
                        "source-1",
                        "unknown",
                        RemoveMissingTracksRejectionReason.UnknownScan),
                    Triple(
                        "source-1",
                        "foreign",
                        RemoveMissingTracksRejectionReason.ForeignSource),
                    Triple(
                        "source-1",
                        "active",
                        RemoveMissingTracksRejectionReason.NotCompleted),
                    Triple(
                        "source-1",
                        "cancelling",
                        RemoveMissingTracksRejectionReason.NotCompleted),
                    Triple(
                        "source-1",
                        "cancelled",
                        RemoveMissingTracksRejectionReason.NotCompleted),
                    Triple(
                        "source-1",
                        "failed",
                        RemoveMissingTracksRejectionReason.NotCompleted),
                    Triple(
                        "source-1",
                        "malformed",
                        RemoveMissingTracksRejectionReason
                            .MissingCompletionTimestamp),
                    Triple(
                        "source-1",
                        "stale",
                        RemoveMissingTracksRejectionReason.StaleCompletedScan),
                )
            requests.forEach { (sourceId, scanId, reason) ->
                assertEquals(
                    RemoveMissingTracksResult.Rejected(reason),
                    open.repository.removeMissingTracks(sourceId, scanId),
                )
                assertEquals(
                    listOf("missing"),
                    open.repository.tracksForSource("source-1").map { it.id })
            }
            assertEquals(
                RemoveMissingTracksResult.Removed(1),
                open.repository.removeMissingTracks("source-1", "latest"))
            assertEquals(
                emptyList(), open.repository.tracksForSource("source-1"))
        }
    }

    @Test
    fun sqlOrderingUsesAllAuthorityAndTerminalTieBreakKeysAndOrdersErrors() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-ordering", ".db").toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.insertScanSession(
                testScanSession(
                    "completed-a", "source-1", completedAtEpochMillis = 10L))
            open.repository.insertScanSession(
                testScanSession(
                    "completed-b",
                    "source-1",
                    startedAtEpochMillis = 2L,
                    completedAtEpochMillis = 10L))
            open.repository.insertScanSession(
                testScanSession(
                    "completed-z",
                    "source-1",
                    startedAtEpochMillis = 2L,
                    completedAtEpochMillis = 10L))
            open.repository.upsertTrack(
                testTrack(
                    "seen",
                    "source-1",
                    "seen.mp3",
                    "Seen",
                    "Artist",
                    "completed-z"))
            assertEquals(
                RemoveMissingTracksResult.Removed(0),
                open.repository.removeMissingTracks("source-1", "completed-z"))

            open.repository.insertScanSession(
                testScanSession(
                    "terminal-a", "source-1", ScanStatus.Failed, 20L, 30L))
            open.repository.insertScanSession(
                testScanSession(
                    "terminal-b", "source-1", ScanStatus.Cancelled, 21L, 30L))
            open.repository.insertScanSession(
                testScanSession(
                    "terminal-z", "source-1", ScanStatus.Failed, 21L, 30L))
            open.repository.insertScanSession(
                testScanSession(
                    "terminal-later-completion",
                    "source-1",
                    ScanStatus.Failed,
                    1L,
                    31L))
            assertEquals(
                "terminal-later-completion",
                open.repository.latestTerminalScanSession()?.id)

            open.repository.insertScanError(
                testScanError(
                    "error-z",
                    "terminal-later-completion",
                    createdAtEpochMillis = 20L))
            open.repository.insertScanError(
                testScanError(
                    "error-a",
                    "terminal-later-completion",
                    createdAtEpochMillis = 10L))
            open.repository.insertScanError(
                testScanError(
                    "error-b",
                    "terminal-later-completion",
                    createdAtEpochMillis = 10L))
            assertEquals(
                listOf("error-a", "error-b", "error-z"),
                open.repository.scanErrors("terminal-later-completion").map {
                    it.id
                })
        }
    }

    @Test
    fun removeMissingDeleteRollsBackWhenTrackDeletionFails() {
        val databaseFile =
            Files.createTempFile(
                    "rhythhaus-library-remove-missing-rollback", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "One",
                    artist = "Artist",
                    lastSeenScanId = "old",
                ),
            )
            open.repository.insertScanSession(
                testScanSession(
                    id = "scan-latest",
                    sourceId = "source-1",
                    startedAtEpochMillis = 2L,
                    completedAtEpochMillis = 3L,
                ),
            )
            open.driver.execute(
                identifier = null,
                sql =
                    "CREATE TRIGGER reject_missing BEFORE DELETE ON library_track BEGIN SELECT RAISE(ABORT, 'reject missing'); END",
                parameters = 0,
            )

            assertFails {
                open.repository.removeMissingTracks("source-1", "scan-latest")
            }
            assertEquals(
                listOf("track-1"),
                open.repository.tracksForSource("source-1").map { it.id })
        }
    }

    @Test
    fun latestTerminalScanIgnoresActiveSessionsAndUsesTerminalOrdering() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-terminal", ".db").toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.insertScanSession(
                testScanSession(
                    "completed", "source-1", ScanStatus.Completed, 10L, 20L),
            )
            open.repository.insertScanSession(
                testScanSession(
                    "cancelled", "source-1", ScanStatus.Cancelled, 30L, null),
            )
            open.repository.insertScanSession(
                testScanSession(
                    "active", "source-1", ScanStatus.Scanning, 40L, null),
            )
            assertEquals(
                "cancelled", open.repository.latestTerminalScanSession()?.id)
        }
    }

    @Test
    fun latestTerminalScanAndErrorsSurviveDatabaseReopen() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-terminal-reopen", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.insertScanSession(
                testScanSession(
                    "failed", "source-1", ScanStatus.Failed, 10L, 20L),
            )
            open.repository.insertScanError(testScanError("error-1", "failed"))
        }
        openRepository(databaseFile).use { reopened ->
            assertEquals(
                "failed", reopened.repository.latestTerminalScanSession()?.id)
            assertEquals(
                listOf("error-1"),
                reopened.repository.scanErrors("failed").map { it.id })
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

private object UnsupportedTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult =
        TagReadResult.Unsupported("not used")

    override fun readProperties(path: String): Map<String, String> = emptyMap()
}

private fun testSource(
    id: String = "source-1",
) =
    LibrarySource(
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
) =
    LibraryTrack(
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
    status: ScanStatus = ScanStatus.Completed,
    startedAtEpochMillis: Long = 1L,
    completedAtEpochMillis: Long? = 2L,
) =
    ScanSession(
        id = id,
        sourceId = sourceId,
        status = status,
        startedAtEpochMillis = startedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
    )

private fun testScanError(
    id: String,
    scanId: String,
    createdAtEpochMillis: Long = 2L,
) =
    ScanError(
        id = id,
        scanId = scanId,
        sourceLocalKey = "$id.mp3",
        displayPath = "/Music/$id.mp3",
        reason = "Test error",
        recoverable = true,
        createdAtEpochMillis = createdAtEpochMillis,
    )
