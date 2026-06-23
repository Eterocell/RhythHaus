package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.taglib.TagLibReader
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LibraryScannerTest {
    @Test
    fun scannerImportsCandidatesAndRecordsSkippedFiles() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.FolderVisited("/Music"),
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "ok.mp3",
                        displayPath = "/Music/ok.mp3",
                        displayName = "ok.mp3",
                        audioSource = AudioSource.FilePath("/Music/ok.mp3"),
                    ),
                ),
                PlatformScanEvent.Skipped("bad.txt", "/Music/bad.txt", "Unsupported file", true),
            ),
        )
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(1, result.foldersVisited)
        assertEquals(2, result.filesVisited)
        assertEquals(1, result.tracksAdded)
        assertEquals(0, result.tracksUpdated)
        assertEquals(1, result.filesSkipped)
        assertEquals(listOf("ok"), repository.tracks().map { it.title })
        assertEquals("Unsupported file", repository.scanErrors("scan-id").single().reason)
    }

    @Test
    fun cancellationStopsBeforeLaterCandidatesAndPreservesImportedTracks() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        var cancel = false
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "first.mp3",
                        displayPath = "first.mp3",
                        displayName = "first.mp3",
                        audioSource = AudioSource.FilePath("/Music/first.mp3"),
                    ),
                ),
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "second.mp3",
                        displayPath = "second.mp3",
                        displayName = "second.mp3",
                        audioSource = AudioSource.FilePath("/Music/second.mp3"),
                    ),
                ),
            ),
            afterFirst = { cancel = true },
        )
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source, isCancelled = { cancel })

        assertEquals(ScanStatus.Cancelled, result.status)
        assertEquals(1, result.filesVisited)
        assertEquals(1, result.tracksAdded)
        assertEquals(listOf("first"), repository.tracks().map { it.title })
    }

    @Test
    fun thrownCancellationExceptionIsNotSwallowedAsFailedScan() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        val platform = object : PlatformAudioScanner {
            override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
                throw CancellationException("coroutine cancelled")
            }
        }
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        assertFailsWith<CancellationException> {
            scanner.scan(source)
        }
    }

    @Test
    fun completedScanDoesNotAutomaticallyRemoveMissingTracks() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
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
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "found.mp3",
                        displayPath = "found.mp3",
                        displayName = "found.mp3",
                        audioSource = AudioSource.FilePath("/Music/found.mp3"),
                    ),
                ),
            ),
        )
        val scanner = LibraryScanner(repository, platform, now = { 100L }, idFactory = { prefix -> "$prefix-id" })

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(listOf("found", "Missing"), repository.tracks().map { it.title })
    }

    @Test
    fun metadataReaderFailureFallsBackToDisplayNameMetadata() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "broken-tags.mp3",
                        displayPath = "broken-tags.mp3",
                        displayName = "broken-tags.mp3",
                        audioSource = AudioSource.FilePath("/Music/broken-tags.mp3"),
                    ),
                ),
            ),
        )
        val scanner = LibraryScanner(
            repository = repository,
            platformScanner = platform,
            metadataReader = AudioMetadataReader(ThrowingTagLibReader),
            now = { 100L },
            idFactory = { prefix -> "$prefix-id" },
        )

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        assertEquals(listOf("broken tags"), repository.tracks().map { it.title })
        assertEquals(emptyList(), repository.scanErrors("scan-id"))
    }
}

private class FakePlatformAudioScanner(
    private val events: List<PlatformScanEvent>,
    private val afterFirst: () -> Unit = {},
) : PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        events.forEachIndexed { index, event ->
            yield(event)
            if (index == 0) afterFirst()
        }
    }
}

private object ThrowingTagLibReader : TagLibReader {
    override fun readPath(path: String) = throw IllegalStateException("metadata failed")
    override fun readProperties(path: String): Map<String, String> = emptyMap()
}
