package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioMetadata
import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
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

    @Test
    fun scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.AndroidSafTree, "Music", "content://tree/music", 1L)
        var metadataSourceCleanedUp = false
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "albums/song.flac",
                        displayPath = "albums/song.flac",
                        displayName = "song.flac",
                        audioSource = AudioSource.Uri("content://provider/tree/music/document/song"),
                        metadataAudioSource = AudioSource.FileDescriptor(fd = 42, displayName = "song.flac"),
                        cleanupMetadataAudioSource = { metadataSourceCleanedUp = true },
                    ),
                ),
            ),
        )
        val scanner = LibraryScanner(
            repository = repository,
            platformScanner = platform,
            metadataReader = AudioMetadataReader(PathAwareTagLibReader),
            now = { 100L },
            idFactory = { prefix -> "$prefix-id" },
        )

        val result = scanner.scan(source)

        assertEquals(ScanStatus.Completed, result.status)
        val track = repository.tracks().single()
        assertEquals(AudioSource.Uri("content://provider/tree/music/document/song"), track.audioSource)
        assertEquals("Android TagLib Title", track.title)
        assertEquals("Android TagLib Artist", track.artist)
        assertEquals("Android TagLib Album", track.album)
        assertEquals(123_000L, track.durationMillis)
        assertEquals(true, metadataSourceCleanedUp)
    }

    @Test
    fun scannerFillsMissingDurationFromPlatformMetadataFallback() {
        val repository = InMemoryLibraryRepository()
        val source = LibrarySource("source-1", LibraryPlatformKind.AndroidSafTree, "Music", "content://tree/music", 1L)
        val platform = FakePlatformAudioScanner(
            events = listOf(
                PlatformScanEvent.AudioCandidate(
                    AudioScanCandidate(
                        sourceId = "source-1",
                        sourceLocalKey = "albums/song.m4a",
                        displayPath = "albums/song.m4a",
                        displayName = "song.m4a",
                        audioSource = AudioSource.Uri("content://provider/tree/music/document/song"),
                        metadataAudioSource = AudioSource.FileDescriptor(fd = 42, displayName = "song.m4a"),
                    ),
                ),
            ),
        )
        val scanner = LibraryScanner(
            repository = repository,
            platformScanner = platform,
            metadataReader = AudioMetadataReader(
                tagLibReader = DurationlessTagLibReader,
                platformMetadataReader = { source ->
                    assertEquals(AudioSource.FileDescriptor(fd = 42, displayName = "song.m4a"), source)
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

    private fun androidTagMetadata() = TagReadResult.Found(
        TagMetadata(
            title = "Android TagLib Title",
            artist = "Android TagLib Artist",
            album = "Android TagLib Album",
            durationMillis = 123_000L,
        ),
    )
}

private object DurationlessTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = error("Expected descriptor metadata source")

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
