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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightLibraryRepositoryJvmTest {
    @Test
    fun observedUnreadableMediaStoreTrackSurvivesRemoveMissingWithUserState() {
        val file =
            Files.createTempFile("rhythhaus-mediastore-skipped", ".db").toFile()
        file.deleteOnExit()
        openRepository(file).use { open ->
            val source =
                testSource()
                    .copy(
                        platformKind =
                            LibraryPlatformKind.AndroidMediaStoreAudio)
            val original =
                testTrack(
                        "seen",
                        sourceLocalKey = "mediastore:7",
                        title = "Raw",
                        artist = "Artist")
                    .copy(
                        artworkBytes = byteArrayOf(1, 2, 3),
                        artworkMimeType = "image/png")
            open.repository.upsertSource(source)
            open.repository.upsertTrack(original)
            open.repository.upsertTrack(
                testTrack(
                    "absent",
                    sourceLocalKey = "mediastore:8",
                    title = "Absent",
                    artist = "Artist"))
            open.repository.setTrackFavorite("seen", true)
            open.repository.recordTrackPlayed("seen", 50L)
            open.repository.setTrackMetadataOverride(
                "seen", TrackMetadataOverride(title = "Edited"))
            val scanner =
                LibraryScanner(
                    open.repository,
                    object : PlatformAudioScanner {
                        override fun scan(source: LibrarySource) =
                            sequenceOf(
                                PlatformScanEvent.Skipped(
                                    "mediastore:7",
                                    "seven.mp3",
                                    "Temporary I/O failure",
                                    true))
                    },
                    now = { 100L },
                    idFactory = { "$it-current" })
            val session = scanner.scan(source)
            assertEquals(ScanStatus.Completed, session.status)
            assertEquals(
                RemoveMissingTracksResult.Removed(1),
                open.repository.removeMissingTracks(source.id, session.id))
            assertEquals(listOf("seen"), open.repository.tracks().map { it.id })
            assertEquals("Edited", open.repository.tracks().single().title)
            assertEquals(setOf("seen"), open.repository.favoriteTrackIds())
            assertEquals(
                1L, open.repository.playHistory().getValue("seen").playCount)
            val retained =
                assertNotNull(open.repository.metadataForTrack("seen"))
                    .scannedTrack
            assertEquals("Raw", retained.title)
            assertEquals(
                original.updatedAtEpochMillis, retained.updatedAtEpochMillis)
            assertContentEquals(
                original.artworkBytes,
                assertNotNull(open.repository.artworkForTrack("seen")).bytes)
        }
    }

    @Test
    fun persistedCorrectionSurvivesRescanAndRestartWhileRestoreUsesNewRawTag() {
        val databaseFile =
            Files.createTempFile("rhythhaus-correction", ".db").toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            val scanned =
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "Old title",
                    artist = "Raw artist")
            open.repository.upsertTrack(scanned)
            assertTrue(
                open.repository.setTrackMetadataOverride(
                    "track-1",
                    TrackMetadataOverride(
                        title = "Corrected",
                        artist = "  Editor  ",
                        discNumber = 2)))
            assertEquals("Corrected", open.repository.tracks().single().title)
            assertEquals(
                "Raw artist",
                open.repository
                    .metadataForTrack("track-1")
                    ?.scannedTrack
                    ?.artist)
            open.repository.upsertTrack(
                scanned.copy(
                    id = "new-id",
                    title = "New raw",
                    artist = "New raw artist"))
        }
        openRepository(databaseFile).use { open ->
            assertEquals("track-1", open.repository.tracks().single().id)
            assertEquals(
                "Editor",
                open.repository.tracksForSource("source-1").single().artist)
            assertEquals(
                "New raw",
                open.repository
                    .metadataForTrack("track-1")
                    ?.scannedTrack
                    ?.title)
            assertTrue(
                open.repository.setTrackMetadataOverride(
                    "track-1",
                    TrackMetadataOverride(artist = "Editor", discNumber = 2)))
            assertEquals("New raw", open.repository.tracks().single().title)
            assertEquals("Editor", open.repository.tracks().single().artist)
            assertFalse(
                open.repository.setTrackMetadataOverride(
                    "missing", TrackMetadataOverride(title = "ghost")))
        }
    }

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
    fun favoriteTrackIdsAreEmptyForNewDatabase() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-favorites-empty", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            assertEquals(emptySet(), open.repository.favoriteTrackIds())
        }
    }

    @Test
    fun setTrackFavoriteIsIdempotentForExistingTrack() {
        val databaseFile =
            Files.createTempFile(
                    "rhythhaus-library-favorites-idempotent", ".db")
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

            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = true))
            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = true))
            assertEquals(setOf("track-1"), open.repository.favoriteTrackIds())

            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = false))
            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = false))
            assertEquals(emptySet(), open.repository.favoriteTrackIds())
        }
    }

    @Test
    fun setTrackFavoriteRejectsMissingTrackWithoutLeavingFavoriteRow() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-favorites-missing", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            assertFalse(
                open.repository.setTrackFavorite(
                    "missing-track", favorite = true))
            assertFalse(
                open.repository.setTrackFavorite(
                    "missing-track", favorite = false))

            assertEquals(emptySet(), open.repository.favoriteTrackIds())
            assertEquals(
                emptyList(),
                open.database.trackFavoriteQueries
                    .selectFavoriteTrackIds()
                    .executeAsList(),
            )
        }
    }

    @Test
    fun favoriteStateSurvivesDatabaseReopen() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-favorites-reopen", ".db")
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
            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = true))
        }

        openRepository(databaseFile).use { reopened ->
            assertEquals(
                setOf("track-1"), reopened.repository.favoriteTrackIds())
        }
    }

    @Test
    fun favoriteStateCascadesWhenSourceIsRemoved() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-favorites-cascade", ".db")
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
                    artist = "Artist"))
            open.repository.upsertTrack(
                testTrack(
                    id = "track-2",
                    sourceId = "source-2",
                    sourceLocalKey = "two.mp3",
                    title = "Two",
                    artist = "Artist"))
            assertTrue(
                open.repository.setTrackFavorite("track-1", favorite = true))
            assertTrue(
                open.repository.setTrackFavorite("track-2", favorite = true))

            open.repository.removeSource("source-1")

            assertEquals(
                listOf("track-2"), open.repository.tracks().map { it.id })
            assertEquals(setOf("track-2"), open.repository.favoriteTrackIds())
        }
    }

    @Test
    fun playHistoryIsEmptyForNewDatabase() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-play-history-empty", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            assertEquals(emptyMap(), open.repository.playHistory())
        }
    }

    @Test
    fun recordTrackPlayedIncrementsAndUpdatesTimestampAtomically() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-play-history", ".db")
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
                ),
            )

            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))
            assertTrue(open.repository.recordTrackPlayed("track-1", 20L))

            assertEquals(
                mapOf(
                    "track-1" to TrackPlayHistory("track-1", 2L, 20L),
                ),
                open.repository.playHistory(),
            )
        }
    }

    @Test
    fun recordTrackPlayedRejectsMissingTrackWithoutLeavingHistoryRow() {
        val databaseFile =
            Files.createTempFile(
                    "rhythhaus-library-play-history-missing", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            assertFalse(open.repository.recordTrackPlayed("missing-track", 10L))

            assertEquals(emptyMap(), open.repository.playHistory())
            assertEquals(
                emptyList(),
                open.database.trackPlayHistoryQueries
                    .selectPlayHistory()
                    .executeAsList(),
            )
        }
    }

    @Test
    fun playHistorySurvivesDatabaseReopen() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-play-history-reopen", ".db")
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
                ),
            )
            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))
        }

        openRepository(databaseFile).use { reopened ->
            assertEquals(
                mapOf(
                    "track-1" to TrackPlayHistory("track-1", 1L, 10L),
                ),
                reopened.repository.playHistory(),
            )
        }
    }

    @Test
    fun metadataUpsertPreservesPlayHistoryForTheSameTrackIdentity() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-play-history-upsert", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            val first =
                testTrack(
                    id = "track-1",
                    sourceLocalKey = "one.mp3",
                    title = "Before rescan",
                    artist = "Artist",
                )
            open.repository.upsertTrack(first)
            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))

            assertEquals(
                TrackUpsertResult.Updated,
                open.repository.upsertTrack(
                    first.copy(
                        id = "replacement-id",
                        title = "After rescan",
                        updatedAtEpochMillis = 20L,
                    ),
                ),
            )

            assertEquals(
                mapOf(
                    "track-1" to TrackPlayHistory("track-1", 1L, 10L),
                ),
                open.repository.playHistory(),
            )
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

    /**
     * A persisted FileDescriptor row is metadata-only and ephemeral; it must
     * rehydrate as a Uri carrying the descriptor's stable key so a legacy row
     * never presents a dangling descriptor.
     */
    @Test
    fun fileDescriptorRowsRehydrateAsUriStableKeys() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-fd-roundtrip", ".db")
                .toFile()
        databaseFile.deleteOnExit()

        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.upsertTrack(
                testTrack(
                        id = "track-fd",
                        sourceLocalKey = "fd.mp3",
                        title = "FD",
                        artist = "Local file",
                    )
                    .copy(
                        audioSource =
                            AudioSource.FileDescriptor(
                                fd = 42, displayName = "fd.mp3"),
                    ),
            )

            assertEquals(
                AudioSource.Uri("fd.mp3"),
                open.repository.tracks().single().audioSource,
            )
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
    fun artworkPresenceDoesNotLoadBlobAndRejectsEmptyArtwork() {
        val databaseFile =
            Files.createTempFile("rhythhaus-artwork-presence", ".db").toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            listOf(
                    "present" to byteArrayOf(1),
                    "empty" to byteArrayOf(),
                    "absent" to null)
                .forEach { (id, artwork) ->
                    open.repository.upsertTrack(
                        testTrack(
                                id = id,
                                sourceLocalKey = "$id.mp3",
                                title = id,
                                artist = "Artist")
                            .copy(artworkBytes = artwork),
                    )
                }
            assertEquals(setOf("present"), open.repository.artworkTrackIds())
            assertTrue(open.repository.tracks().all { it.artworkBytes == null })
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
            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))
            assertTrue(open.repository.recordTrackPlayed("track-2", 20L))
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
                mapOf(
                    "track-2" to TrackPlayHistory("track-2", 1L, 20L),
                ),
                open.repository.playHistory(),
            )
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
            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))
            open.repository.insertScanSession(
                testScanSession(id = "scan-1", sourceId = "source-1"))
            open.repository.insertScanError(
                testScanError(id = "error-1", scanId = "scan-1"))

            open.repository.clearAll()

            assertEquals(emptyList(), open.repository.sources())
            assertEquals(emptyList(), open.repository.tracks())
            assertEquals(emptyMap(), open.repository.playHistory())
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
            assertTrue(open.repository.recordTrackPlayed("track-1", 10L))
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
                mapOf(
                    "track-1" to TrackPlayHistory("track-1", 1L, 10L),
                ),
                open.repository.playHistory(),
            )
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
            assertTrue(open.repository.recordTrackPlayed("missing", 10L))
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
                assertEquals(
                    mapOf(
                        "missing" to TrackPlayHistory("missing", 1L, 10L),
                    ),
                    open.repository.playHistory(),
                )
            }
            assertEquals(
                RemoveMissingTracksResult.Removed(1),
                open.repository.removeMissingTracks("source-1", "latest"))
            assertEquals(
                emptyList(), open.repository.tracksForSource("source-1"))
            assertEquals(emptyMap(), open.repository.playHistory())
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

    @Test
    fun completedSummaryRoundTripsAcrossReopenAndOutlivesLaterFailure() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-summary-reopen", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        val expected =
            ScanChangeSummary(
                addedCount = 7,
                modifiedCount = 11,
                unchangedCount = 13,
                missingCount = 17,
                addedDetails = listOf("/Music/pipe|separator\u001f.mp3"),
                modifiedDetails = listOf("/Music/100%/音楽.mp3"),
                missingDetails = emptyList(),
            )
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.repository.insertScanSession(
                ScanSession(
                    id = "completed",
                    sourceId = "source-1",
                    status = ScanStatus.Completed,
                    startedAtEpochMillis = 10L,
                    completedAtEpochMillis = 20L,
                    changeSummary = expected,
                ),
            )
            assertTrue(
                open.database.scanSessionQueries
                    .selectScanSessionById("completed")
                    .executeAsOne()
                    .changeSummaryJson
                    ?.startsWith("scan-summary-v1:") == true,
            )
            open.repository.insertScanSession(
                testScanSession(
                    id = "failed",
                    sourceId = "source-1",
                    status = ScanStatus.Failed,
                    startedAtEpochMillis = 30L,
                    completedAtEpochMillis = 40L,
                ),
            )
        }

        openRepository(databaseFile).use { reopened ->
            assertEquals(
                "failed",
                reopened.repository.latestTerminalScanSession()?.id,
            )
            assertNull(
                reopened.repository.latestTerminalScanSession()?.changeSummary,
            )
            val completed =
                assertNotNull(reopened.repository.latestCompletedScanSession())
            assertEquals("completed", completed.id)
            assertEquals(
                expected.addedCount, completed.changeSummary?.addedCount)
            assertEquals(
                expected.modifiedCount,
                completed.changeSummary?.modifiedCount,
            )
            assertEquals(
                expected.unchangedCount,
                completed.changeSummary?.unchangedCount,
            )
            assertEquals(
                expected.missingCount,
                completed.changeSummary?.missingCount,
            )
            assertEquals(
                expected.addedDetails,
                completed.changeSummary?.addedDetails,
            )
            assertEquals(
                expected.modifiedDetails,
                completed.changeSummary?.modifiedDetails,
            )
            assertEquals(
                expected.missingDetails,
                completed.changeSummary?.missingDetails,
            )
        }
    }

    @Test
    fun legacyDelimitedSummaryRemainsReadableAfterDatabaseReopen() {
        val databaseFile =
            Files.createTempFile("rhythhaus-library-legacy-summary", ".db")
                .toFile()
        databaseFile.deleteOnExit()
        openRepository(databaseFile).use { open ->
            open.repository.upsertSource(testSource())
            open.database.scanSessionQueries.insertScanSession(
                id = "legacy",
                sourceId = "source-1",
                status = ScanStatus.Completed.name,
                startedAtEpochMillis = 10L,
                completedAtEpochMillis = 20L,
                foldersVisited = 0L,
                filesVisited = 0L,
                tracksAdded = 0L,
                tracksUpdated = 0L,
                filesSkipped = 0L,
                changeSummaryJson =
                    "1|2|3|4|/Music/added.mp3\u001f/Music/added-2.mp3|/Music/changed.mp3|",
                terminalMessage = null,
            )
        }

        openRepository(databaseFile).use { reopened ->
            assertEquals(
                ScanChangeSummary(
                    addedCount = 1,
                    modifiedCount = 2,
                    unchangedCount = 3,
                    missingCount = 4,
                    addedDetails =
                        listOf("/Music/added.mp3", "/Music/added-2.mp3"),
                    modifiedDetails = listOf("/Music/changed.mp3"),
                    missingDetails = emptyList(),
                ),
                reopened.repository.latestTerminalScanSession()?.changeSummary,
            )
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
