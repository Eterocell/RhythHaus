package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.AudioMetadata
import com.eterocell.rhythhaus.library.impl.AudioMetadataReader
import com.eterocell.rhythhaus.library.impl.PlatformAudioScanner
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LibraryScannerTest {
    @Test
    fun completedScanPublishesBoundedChangeSummary() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = source.id,
                                sourceLocalKey = "one.mp3",
                                displayPath = "/Music/one.mp3",
                                displayName = "one.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/one.mp3"),
                            ),
                        ),
                    ),
            )

        val result =
            LibraryScanner(
                    repository = repository,
                    platformScanner = platform,
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-id" },
                )
                .scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.changeSummary?.addedCount)
        assertEquals(0, result.changeSummary?.modifiedCount)
        assertEquals(0, result.changeSummary?.unchangedCount)
        assertEquals(0, result.changeSummary?.missingCount)
        assertEquals(
            listOf("/Music/one.mp3"), result.changeSummary?.addedDetails)
    }

    @Test
    fun completedScanClassifiesKnownFactChangesWithoutTreatingUnknownFactsAsModified() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        repository.upsertSource(source)
        listOf(
                storedTrack(
                    "same.mp3", sizeBytes = 10L, modifiedAtEpochMillis = 1L),
                storedTrack(
                    "size-before-unknown.mp3",
                    sizeBytes = null,
                    modifiedAtEpochMillis = 1L),
                storedTrack(
                    "size-after-unknown.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = 1L),
                storedTrack(
                    "time-before-unknown.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = null),
                storedTrack(
                    "time-after-unknown.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = 1L),
                storedTrack(
                    "size-modified.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = 1L),
                storedTrack(
                    "time-modified.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = 1L),
                storedTrack(
                    "nested/missing.mp3",
                    sizeBytes = 10L,
                    modifiedAtEpochMillis = 1L),
            )
            .forEach(repository::upsertTrack)

        val result =
            LibraryScanner(
                    repository = repository,
                    platformScanner =
                        FakePlatformAudioScanner(
                            listOf(
                                candidate("same.mp3", 10L, 1L),
                                candidate("size-before-unknown.mp3", 10L, 1L),
                                candidate("size-after-unknown.mp3", null, 1L),
                                candidate("time-before-unknown.mp3", 10L, 1L),
                                candidate("time-after-unknown.mp3", 10L, null),
                                candidate("size-modified.mp3", 11L, 1L),
                                candidate("time-modified.mp3", 10L, 2L),
                                candidate("new.mp3", 10L, 1L),
                            ),
                        ),
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-id" },
                )
                .scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.changeSummary?.addedCount)
        assertEquals(2, result.changeSummary?.modifiedCount)
        assertEquals(5, result.changeSummary?.unchangedCount)
        assertEquals(1, result.changeSummary?.missingCount)
        assertEquals(
            listOf("nested/missing.mp3"),
            result.changeSummary?.missingDetails,
        )
    }

    @Test
    fun completedScanCapsAffectedPathDetailsWithoutCappingCounts() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        var nextId = 0
        val result =
            LibraryScanner(
                    repository = repository,
                    platformScanner =
                        FakePlatformAudioScanner(
                            (1..101).map { index ->
                                candidate("added-$index.mp3", 10L, 1L)
                            },
                        ),
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-${nextId++}" },
                )
                .scan(source)

        assertEquals(101, result.changeSummary?.addedCount)
        assertEquals(100, result.changeSummary?.addedDetails?.size)
        assertEquals(
            "/Music/added-1.mp3", result.changeSummary?.addedDetails?.first())
        assertEquals(
            "/Music/added-100.mp3", result.changeSummary?.addedDetails?.last())
    }

    @Test
    fun recoverableMediaStoreSkipObservesExistingTrackAndPreventsMissingRemoval() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.AndroidMediaStoreAudio,
                "Music",
                "media-store",
                1L)
        repository.upsertSource(source)
        repository.upsertTrack(
            storedTrack(
                sourceLocalKey = "42",
                audioPath = "content://media/external/audio/media/42",
            ),
        )
        val trackId = repository.tracksForSource(source.id).single().id
        repository.setTrackFavorite(trackId, favorite = true)
        repository.recordTrackPlayed(trackId, 2L)
        repository.setTrackMetadataOverride(
            trackId,
            TrackMetadataOverride(title = "Pinned MediaStore title"),
        )

        val result =
            LibraryScanner(
                    repository = repository,
                    platformScanner =
                        FakePlatformAudioScanner(
                            listOf(
                                PlatformScanEvent.Skipped(
                                    sourceLocalKey = "42",
                                    displayPath = "Song.mp3",
                                    reason = "Temporary metadata failure",
                                    recoverable = true,
                                ),
                            ),
                        ),
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-id" },
                )
                .scan(source)

        assertEquals(0, result.changeSummary?.missingCount)
        assertEquals(
            "scan-id",
            repository.tracksForSource(source.id).single().lastSeenScanId)
        assertEquals(
            RemoveMissingTracksResult.Removed(0),
            repository.removeMissingTracks(source.id, result.id),
        )
        assertEquals(1, repository.tracksForSource(source.id).size)
        assertEquals(setOf(trackId), repository.favoriteTrackIds())
        assertEquals(1L, repository.playHistory()[trackId]?.playCount)
        assertEquals(
            "Pinned MediaStore title",
            repository.metadataForTrack(trackId)?.overrides?.title,
        )
    }

    @Test
    fun failedAndCancelledSessionsDoNotCarryAnOlderCompletedSummary() {
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)

        fun repositoryWithCompletedSummary() =
            InMemoryLibraryRepository().also { repository ->
                repository.upsertSource(source)
                repository.insertScanSession(
                    ScanSession(
                        id = "completed-scan",
                        sourceId = source.id,
                        status = ScanStatus.Completed,
                        startedAtEpochMillis = 1L,
                        completedAtEpochMillis = 2L,
                        changeSummary = ScanChangeSummary(addedCount = 1),
                    ),
                )
            }

        val cancelled =
            LibraryScanner(
                    repository = repositoryWithCompletedSummary(),
                    platformScanner =
                        FakePlatformAudioScanner(
                            listOf(candidate("cancelled.mp3", 10L, 1L)),
                        ),
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-cancelled" },
                )
                .scan(source, isCancelled = { true })
        val failed =
            LibraryScanner(
                    repository = repositoryWithCompletedSummary(),
                    platformScanner =
                        object : PlatformAudioScanner {
                            override fun scan(
                                source: LibrarySource,
                            ): Sequence<PlatformScanEvent> = sequence {
                                error("disk failure")
                            }
                        },
                    now = { 100L },
                    idFactory = { prefix -> "$prefix-failed" },
                )
                .scan(source)

        assertEquals(ScanStatus.Cancelled, cancelled.status)
        assertNull(cancelled.changeSummary)
        assertEquals(ScanStatus.Failed, failed.status)
        assertNull(failed.changeSummary)
    }

    @Test
    fun scannerImportsCandidatesAndRecordsSkippedFiles() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.FolderVisited("/Music"),
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "ok.mp3",
                                displayPath = "/Music/ok.mp3",
                                displayName = "ok.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/ok.mp3"),
                            ),
                        ),
                        PlatformScanEvent.Skipped(
                            "bad.txt",
                            "/Music/bad.txt",
                            "Unsupported file",
                            true),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.foldersVisited)
        assertEquals(2, result.filesVisited)
        assertEquals(1, result.tracksAdded)
        assertEquals(0, result.tracksUpdated)
        assertEquals(1, result.filesSkipped)
        assertEquals(listOf("ok"), repository.tracks().map { it.title })
        assertEquals(
            "Unsupported file",
            repository.scanErrors("scan-id").single().reason)
    }

    @Test
    fun scannerReportsProgressAfterEachScanEvent() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.FolderVisited("/Music"),
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "first.mp3",
                                displayPath = "/Music/first.mp3",
                                displayName = "first.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/first.mp3"),
                            ),
                        ),
                        PlatformScanEvent.Skipped(
                            "bad.txt",
                            "/Music/bad.txt",
                            "Unsupported file",
                            true),
                    ),
            )
        val progress = mutableListOf<ScanProgress>()
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        scanner.scan(source, onProgress = progress::add)

        assertEquals(
            listOf(0, 0, 1, 2, 2),
            progress.map { it.session?.filesVisited },
        )
        assertEquals(
            listOf(null, "/Music", "/Music/first.mp3", "/Music/bad.txt", null),
            progress.map { it.latestItem },
        )
        assertEquals(ScanStatus.Completed, progress.last().session?.status)
    }

    @Test
    fun cancellationStopsBeforeLaterCandidatesAndPreservesImportedTracks() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        var cancel = false
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "first.mp3",
                                displayPath = "first.mp3",
                                displayName = "first.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/first.mp3"),
                            ),
                        ),
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "second.mp3",
                                displayPath = "second.mp3",
                                displayName = "second.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/second.mp3"),
                            ),
                        ),
                    ),
                afterFirst = { cancel = true },
            )
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source, isCancelled = { cancel })

        assertEquals(ScanStatus.Cancelled, result.status)
        assertEquals(1, result.filesVisited)
        assertEquals(1, result.tracksAdded)
        assertEquals(listOf("first"), repository.tracks().map { it.title })
    }

    @Test
    fun cancellationBeforeCandidateImportCleansUpMetadataAudioSource() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.AndroidSafTree,
                "Music",
                "content://tree/music",
                1L)
        var cleanupCount = 0
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = source.id,
                                sourceLocalKey = "cancelled.mp3",
                                displayPath = "cancelled.mp3",
                                displayName = "cancelled.mp3",
                                audioSource =
                                    AudioSource.Uri(
                                        "content://provider/cancelled.mp3"),
                                metadataAudioSource =
                                    AudioSource.FileDescriptor(
                                        fd = 42, displayName = "cancelled.mp3"),
                                cleanupMetadataAudioSource = {
                                    cleanupCount += 1
                                },
                            ),
                        ),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source, isCancelled = { true })

        assertEquals(ScanStatus.Cancelled, result.status)
        assertEquals(1, cleanupCount)
        assertEquals(emptyList(), repository.tracks())
    }

    @Test
    fun thrownCancellationExceptionIsNotSwallowedAsFailedScan() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            object : PlatformAudioScanner {
                override fun scan(
                    source: LibrarySource
                ): Sequence<PlatformScanEvent> = sequence {
                    throw CancellationException("coroutine cancelled")
                }
            }
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        assertFailsWith<CancellationException> {
            scanner.scan(source)
        }
    }

    @Test
    fun genericScannerFailureProducesFailedTerminalSession() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            object : PlatformAudioScanner {
                override fun scan(
                    source: LibrarySource
                ): Sequence<PlatformScanEvent> = sequence {
                    yield(PlatformScanEvent.FolderVisited("/Music"))
                    error("disk failure")
                }
            }
        val progress = mutableListOf<ScanProgress>()
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source, onProgress = progress::add)

        assertEquals(ScanStatus.Failed, result.status)
        assertEquals(100L, result.completedAtEpochMillis)
        assertEquals("disk failure", result.terminalMessage)
        assertEquals(1, result.foldersVisited)
        assertEquals(ScanStatus.Failed, progress.last().session?.status)
        val stored = repository.latestTerminalScanSession()
        assertEquals("scan-id", stored?.id)
        assertEquals(ScanStatus.Failed, stored?.status)
        assertEquals("disk failure", stored?.terminalMessage)
    }

    @Test
    fun completedScanDoesNotAutomaticallyRemoveMissingTracks() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        repository.upsertSource(source)
        repository.upsertTrack(
            LibraryTrack(
                id = "existing-track",
                sourceId = "source-1",
                sourceLocalKey = "missing.mp3",
                audioSource = AudioSource.FilePath("/Music/missing.mp3"),
                displayName = "missing.mp3",
                title = "Missing",
                artist = "Local file",
                album = "Imported audio",
                durationMillis = null,
                sizeBytes = null,
                modifiedAtEpochMillis = null,
                lastSeenScanId = "previous-scan",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
        repository.setTrackFavorite("existing-track", favorite = true)
        repository.recordTrackPlayed("existing-track", 2L)
        repository.setTrackMetadataOverride(
            "existing-track",
            TrackMetadataOverride(title = "Pinned missing title"),
        )
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "found.mp3",
                                displayPath = "found.mp3",
                                displayName = "found.mp3",
                                audioSource =
                                    AudioSource.FilePath("/Music/found.mp3"),
                            ),
                        ),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository,
                platform,
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.changeSummary?.missingCount)
        assertEquals(
            listOf("missing.mp3"), result.changeSummary?.missingDetails)
        assertEquals(
            setOf("existing-track", "track-id"),
            repository.tracks().map { it.id }.toSet())
        assertEquals(setOf("existing-track"), repository.favoriteTrackIds())
        assertEquals(1L, repository.playHistory()["existing-track"]?.playCount)
        assertEquals(
            "Pinned missing title",
            repository.metadataForTrack("existing-track")?.overrides?.title,
        )
    }

    @Test
    fun metadataReaderFailureFallsBackToDisplayNameMetadata() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.JvmFolder,
                "Music",
                "/Music",
                1L)
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "broken-tags.mp3",
                                displayPath = "broken-tags.mp3",
                                displayName = "broken-tags.mp3",
                                audioSource =
                                    AudioSource.FilePath(
                                        "/Music/broken-tags.mp3"),
                            ),
                        ),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository = repository,
                platformScanner = platform,
                metadataReader = AudioMetadataReader(ThrowingTagLibReader),
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" },
            )

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(
            listOf("broken tags"), repository.tracks().map { it.title })
        assertEquals(emptyList(), repository.scanErrors("scan-id"))
    }

    @Test
    fun scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.AndroidSafTree,
                "Music",
                "content://tree/music",
                1L)
        var metadataSourceCleanupCount = 0
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "albums/song.flac",
                                displayPath = "albums/song.flac",
                                displayName = "song.flac",
                                audioSource =
                                    AudioSource.Uri(
                                        "content://provider/tree/music/document/song"),
                                metadataAudioSource =
                                    AudioSource.FileDescriptor(
                                        fd = 42, displayName = "song.flac"),
                                cleanupMetadataAudioSource = {
                                    metadataSourceCleanupCount += 1
                                },
                            ),
                        ),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository = repository,
                platformScanner = platform,
                metadataReader = AudioMetadataReader(PathAwareTagLibReader),
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" },
            )

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        val track = repository.tracks().single()
        assertEquals(
            AudioSource.Uri("content://provider/tree/music/document/song"),
            track.audioSource)
        assertEquals("Android TagLib Title", track.title)
        assertEquals("Android TagLib Artist", track.artist)
        assertEquals("Android TagLib Album", track.album)
        assertEquals(123_000L, track.durationMillis)
        assertEquals(1, metadataSourceCleanupCount)
    }

    @Test
    fun scannerFillsMissingDurationFromPlatformMetadataFallback() {
        val repository = InMemoryLibraryRepository()
        val source =
            LibrarySource(
                "source-1",
                LibraryPlatformKind.AndroidSafTree,
                "Music",
                "content://tree/music",
                1L)
        val platform =
            FakePlatformAudioScanner(
                events =
                    listOf(
                        PlatformScanEvent.AudioCandidate(
                            AudioScanCandidate(
                                sourceId = "source-1",
                                sourceLocalKey = "albums/song.m4a",
                                displayPath = "albums/song.m4a",
                                displayName = "song.m4a",
                                audioSource =
                                    AudioSource.Uri(
                                        "content://provider/tree/music/document/song"),
                                metadataAudioSource =
                                    AudioSource.FileDescriptor(
                                        fd = 42, displayName = "song.m4a"),
                            ),
                        ),
                    ),
            )
        val scanner =
            LibraryScanner(
                repository = repository,
                platformScanner = platform,
                metadataReader =
                    AudioMetadataReader(
                        tagLibReader = DurationlessTagLibReader,
                        platformMetadataReader = { source ->
                            assertEquals(
                                AudioSource.FileDescriptor(
                                    fd = 42, displayName = "song.m4a"),
                                source)
                            AudioMetadata(durationMillis = 187_000L)
                        },
                    ),
                now = { 100L },
                idFactory = { prefix -> "$prefix-id" },
            )

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        val track = repository.tracks().single()
        assertEquals("TagLib Title", track.title)
        assertEquals("TagLib Artist", track.artist)
        assertEquals("TagLib Album", track.album)
        assertEquals(187_000L, track.durationMillis)
    }
}

private class FakePlatformAudioScanner(
    private val events: List<PlatformScanEvent>,
    private val afterFirst: () -> Unit = {},
) : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        sequence {
            events.forEachIndexed { index, event ->
                yield(event)
                if (index == 0) afterFirst()
            }
        }
}

private fun candidate(
    sourceLocalKey: String,
    sizeBytes: Long?,
    modifiedAtEpochMillis: Long?,
): PlatformScanEvent.AudioCandidate =
    PlatformScanEvent.AudioCandidate(
        AudioScanCandidate(
            sourceId = "source-1",
            sourceLocalKey = sourceLocalKey,
            displayPath = "/Music/$sourceLocalKey",
            displayName = sourceLocalKey.substringAfterLast('/'),
            audioSource = AudioSource.FilePath("/Music/$sourceLocalKey"),
            sizeBytes = sizeBytes,
            modifiedAtEpochMillis = modifiedAtEpochMillis,
        ),
    )

private fun storedTrack(
    sourceLocalKey: String,
    sizeBytes: Long? = 10L,
    modifiedAtEpochMillis: Long? = 1L,
    audioPath: String = "/Music/$sourceLocalKey",
): LibraryTrack =
    LibraryTrack(
        id = "track-$sourceLocalKey",
        sourceId = "source-1",
        sourceLocalKey = sourceLocalKey,
        audioSource = AudioSource.FilePath(audioPath),
        displayName = sourceLocalKey.substringAfterLast('/'),
        title = sourceLocalKey.substringAfterLast('/').substringBeforeLast('.'),
        artist = "Artist",
        album = "Album",
        durationMillis = null,
        sizeBytes = sizeBytes,
        modifiedAtEpochMillis = modifiedAtEpochMillis,
        lastSeenScanId = "previous-scan",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

private object ThrowingTagLibReader : TagLibReader {
    override fun readPath(path: String) =
        throw IllegalStateException("metadata failed")

    override fun readProperties(path: String): Map<String, String> = emptyMap()
}

private object PathAwareTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult {
        assertEquals("/cache/rhythhaus-metadata/song.flac", path)
        return androidTagMetadata()
    }

    override fun readFd(fd: Int, displayName: String): TagReadResult {
        assertEquals(42, fd)
        assertEquals("song.flac", displayName)
        return androidTagMetadata()
    }

    override fun readProperties(path: String): Map<String, String> = emptyMap()

    private fun androidTagMetadata() =
        TagReadResult.Found(
            TagMetadata(
                title = "Android TagLib Title",
                artist = "Android TagLib Artist",
                album = "Android TagLib Album",
                durationMillis = 123_000L,
            ),
        )
}

private object DurationlessTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult =
        error("Expected descriptor metadata source")

    override fun readFd(fd: Int, displayName: String): TagReadResult {
        assertEquals(42, fd)
        assertEquals("song.m4a", displayName)
        return TagReadResult.Found(
            TagMetadata(
                title = "TagLib Title",
                artist = "TagLib Artist",
                album = "TagLib Album",
                durationMillis = null,
            ),
        )
    }

    override fun readProperties(path: String): Map<String, String> = emptyMap()
}
