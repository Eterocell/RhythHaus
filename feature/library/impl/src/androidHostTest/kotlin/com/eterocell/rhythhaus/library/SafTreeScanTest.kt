package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.impl.SafEntry
import com.eterocell.rhythhaus.library.impl.SafFileDescriptor
import com.eterocell.rhythhaus.library.impl.SafTreeReader
import com.eterocell.rhythhaus.library.impl.safTreeScanEvents
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MAX_LRC_BYTES = 512 * 1024L

class SafTreeScanTest {
    @Test
    fun mixedThousandFileTreeEnumeratesEachDirectoryOnce() {
        val source = safSource()
        val root = directory("root", "Music")
        val albumOne = directory("album-1", "Album One")
        val albumTwo = directory("album-2", "Album Two")
        val rootFiles = buildList {
            repeat(400) { index ->
                val stem = "song-%04d".format(index)
                add(file("root-$stem.mp3", "$stem.mp3"))
                add(
                    file(
                        "root-$stem.lrc",
                        "$stem.lrc",
                        size = 64L,
                    ),
                )
            }
            repeat(198) { index ->
                add(file("root-cover-$index.jpg", "cover-$index.jpg"))
            }
        }
        val reader =
            FakeSafTreeReader(
                listings =
                    mapOf(
                        "root" to listOf(albumTwo, albumOne) + rootFiles,
                        "album-1" to
                            listOf(
                                file("a1-song.mp3", "song.mp3"),
                                file("a1-song.lrc", "song.lrc", size = 64L),
                                file("a1-note.txt", "note.txt"),
                            ),
                        "album-2" to
                            listOf(
                                file("a2-song.MP3", "song.MP3"),
                                file("a2-song.LRC", "song.LRC", size = 64L),
                            ),
                    ),
                sidecarBytes =
                    buildMap {
                        repeat(400) { index ->
                            val stem = "song-%04d".format(index)
                            put(
                                "root-$stem.lrc",
                                "[00:01.00]root $stem".toByteArray(),
                            )
                        }
                        put(
                            "a1-song.lrc",
                            "[00:01.00]album one".toByteArray(),
                        )
                        put(
                            "a2-song.LRC",
                            "[00:01.00]album two".toByteArray(),
                        )
                    },
            )

        val events =
            safTreeScanEvents(source, reader, root, source.displayName).toList()
        val candidates =
            events.filterIsInstance<PlatformScanEvent.AudioCandidate>().map {
                it.candidate
            }
        val skipped = events.filterIsInstance<PlatformScanEvent.Skipped>()

        assertEquals(
            mapOf("root" to 1, "album-1" to 1, "album-2" to 1),
            reader.childListingCounts,
            "each directory must be enumerated exactly once",
        )
        assertEquals(402, candidates.size)
        assertEquals(
            402,
            reader.sidecarReadIds.size,
            "one bounded sidecar read per supported audio file, never per " +
                "directory listing",
        )
        assertEquals(199, skipped.size)
        assertTrue(skipped.all { it.reason == "Unsupported audio type" })
        assertTrue(skipped.none { it.recoverable })
        assertTrue(
            (candidates.map { it.displayName } + skipped.map { it.displayPath })
                .none { it.endsWith(".lrc", ignoreCase = true) },
            "lyric sidecars must never surface as tracks or skips",
        )

        assertEquals(
            listOf("Music", "Album One", "Album Two"),
            events.filterIsInstance<PlatformScanEvent.FolderVisited>().map {
                it.displayPath
            },
            "every directory is visited exactly once",
        )
        assertEquals(PlatformScanEvent.FolderVisited("Music"), events.first())
        assertEquals(
            "Album One/song.mp3",
            events
                .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                .first()
                .candidate
                .displayPath,
        )
        assertTrue(
            events.indexOfFirst {
                it is PlatformScanEvent.AudioCandidate &&
                    it.candidate.displayPath == "song-0000.mp3"
            } > events.indexOfLast { it is PlatformScanEvent.FolderVisited },
            "root files are yielded after directory subtrees (folders first)",
        )
        assertEquals(
            "[00:01.00]album two",
            candidates
                .single { it.displayPath == "Album Two/song.MP3" }
                .sidecarLyrics,
        )
    }

    @Test
    fun sameStemInNestedDirectoriesUsesThatDirectorySidecar() {
        val source = safSource()
        val root = directory("root", "Music")
        val albumA = directory("album-a", "A")
        val albumB = directory("album-b", "B")
        val reader =
            FakeSafTreeReader(
                listings =
                    mapOf(
                        "root" to
                            listOf(
                                albumA,
                                albumB,
                                file("root-song.mp3", "01 Song.mp3"),
                                file("root-song.lrc", "01 Song.lrc", 32L),
                            ),
                        "album-a" to
                            listOf(
                                file("a-song.mp3", "01 Song.mp3"),
                            ),
                        "album-b" to
                            listOf(
                                file("b-song.mp3", "01 Song.mp3"),
                                file("b-song.lrc", "01 Song.lrc", 32L),
                            ),
                    ),
                sidecarBytes =
                    mapOf(
                        "root-song.lrc" to "[00:01.00]root".toByteArray(),
                        "b-song.lrc" to "[00:01.00]b".toByteArray(),
                    ),
            )

        val candidates =
            safTreeScanEvents(source, reader, root, source.displayName)
                .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                .map { it.candidate }
                .associateBy { it.displayPath }

        assertEquals(
            setOf("01 Song.mp3", "A/01 Song.mp3", "B/01 Song.mp3"),
            candidates.keys,
        )
        assertEquals(
            "[00:01.00]root",
            candidates.getValue("01 Song.mp3").sidecarLyrics,
        )
        assertNull(
            candidates.getValue("A/01 Song.mp3").sidecarLyrics,
            "a stem match must not cross directory boundaries",
        )
        assertEquals(
            setOf("root-song.lrc", "b-song.lrc"),
            reader.sidecarReadIds.toSet(),
        )
        assertEquals(
            "[00:01.00]b",
            candidates.getValue("B/01 Song.mp3").sidecarLyrics,
        )
    }

    @Test
    fun sidecarsAreReadOnlyForSupportedAudioAndStayBounded() {
        val source = safSource()
        val root = directory("root", "Music")
        val oversized = ByteArray((MAX_LRC_BYTES + 1).toInt())
        val reader =
            FakeSafTreeReader(
                listings =
                    mapOf(
                        "root" to
                            listOf(
                                file("track-mp3", "track.mp3"),
                                file("track-lrc", "track.lrc", 16L),
                                file("lonely-mp3", "lonely.mp3"),
                                file("cover-jpg", "cover.jpg"),
                                file("cover-lrc", "cover.lrc", 16L),
                                file("huge-mp3", "huge.mp3"),
                                file(
                                    "huge-lrc",
                                    "huge.lrc",
                                    MAX_LRC_BYTES + 1,
                                ),
                                file("edge-mp3", "edge.mp3"),
                                file("edge-lrc", "edge.lrc", MAX_LRC_BYTES),
                                file("case-mp3", "case.MP3"),
                                file("case-lrc", "Case.LRC", 16L),
                                file("broken-mp3", "broken.mp3"),
                                file("broken-lrc", "broken.lrc", 16L),
                                file("grow-mp3", "grow.mp3"),
                                file("grow-lrc", "grow.lrc", null),
                                file("notes-txt", "notes.txt"),
                                file("notes-lrc", "notes.lrc", 16L),
                            ),
                    ),
                sidecarBytes =
                    mapOf(
                        "track-lrc" to "[00:01.00]track".toByteArray(),
                        "cover-lrc" to "[00:01.00]cover".toByteArray(),
                        "huge-lrc" to oversized,
                        "edge-lrc" to "[00:01.00]edge".toByteArray(),
                        "case-lrc" to "[00:01.00]case".toByteArray(),
                        "grow-lrc" to oversized,
                        "notes-lrc" to "[00:01.00]notes".toByteArray(),
                    ),
                sidecarFailures = setOf("broken-lrc"),
            )

        val events =
            safTreeScanEvents(source, reader, root, source.displayName).toList()
        val byPath =
            events
                .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                .map { it.candidate }
                .associateBy { it.displayPath }

        assertEquals(
            listOf(
                "broken-lrc",
                "case-lrc",
                "edge-lrc",
                "grow-lrc",
                "track-lrc",
            ),
            reader.sidecarReadIds,
            "unsupported files and oversized sidecars must not be read",
        )
        assertEquals(
            "[00:01.00]track",
            byPath.getValue("track.mp3").sidecarLyrics,
        )
        assertEquals(
            "[00:01.00]case",
            byPath.getValue("case.MP3").sidecarLyrics,
        )
        assertEquals(
            "[00:01.00]edge",
            byPath.getValue("edge.mp3").sidecarLyrics,
        )
        assertNull(
            byPath.getValue("lonely.mp3").sidecarLyrics,
            "a missing sidecar falls back to embedded lyrics",
        )
        assertNull(byPath.getValue("huge.mp3").sidecarLyrics)
        assertNull(
            byPath.getValue("grow.mp3").sidecarLyrics,
            "an unknown-size sidecar that exceeds the cap is rejected",
        )
        assertNull(
            byPath.getValue("broken.mp3").sidecarLyrics,
            "an unreadable sidecar must not fail the audio candidate",
        )
        assertTrue(
            events
                .filterIsInstance<PlatformScanEvent.Skipped>()
                .map { it.displayPath }
                .containsAll(listOf("cover.jpg", "notes.txt")),
        )
    }

    @Test
    fun metadataDescriptorsAreOpenedOnlyForSupportedAudioAndClosed() {
        val source = safSource()
        val root = directory("root", "Music")
        val reader =
            FakeSafTreeReader(
                listings =
                    mapOf(
                        "root" to
                            listOf(
                                file("alpha", "alpha.mp3"),
                                file("beta", "beta.flac"),
                                file("gamma", "gamma.ogg"),
                                file("delta", "delta.jpg"),
                            ),
                    ),
                descriptors =
                    mapOf(
                        "alpha" to 11,
                        "beta" to 12,
                        "delta" to 13,
                    ),
            )

        val events =
            safTreeScanEvents(source, reader, root, source.displayName).toList()
        val candidates =
            events
                .filterIsInstance<PlatformScanEvent.AudioCandidate>()
                .map { it.candidate }
                .associateBy { it.displayName }

        assertEquals(listOf("alpha", "beta"), reader.openedDescriptorIds)
        assertNull(
            candidates["delta.jpg"],
            "unsupported files are skipped instead of yielding candidates",
        )
        assertTrue(
            events.filterIsInstance<PlatformScanEvent.Skipped>().any {
                it.displayPath == "delta.jpg"
            },
        )
        assertEquals(
            AudioSource.FileDescriptor(fd = 11, displayName = "alpha.mp3"),
            candidates.getValue("alpha.mp3").metadataAudioSource,
        )
        assertEquals(
            AudioSource.FileDescriptor(fd = 12, displayName = "beta.flac"),
            candidates.getValue("beta.flac").metadataAudioSource,
        )
        assertEquals(
            AudioSource.Uri("content://fake/gamma"),
            candidates.getValue("gamma.ogg").metadataAudioSource,
            "a closed or inaccessible descriptor falls back to playback",
        )
        assertNull(candidates.getValue("gamma.ogg").cleanupMetadataAudioSource)

        candidates.getValue("alpha.mp3").cleanupMetadataAudioSource?.invoke()
        candidates.getValue("beta.flac").cleanupMetadataAudioSource?.invoke()
        assertEquals(2, reader.closedDescriptorCount)
    }
}

private fun safSource() =
    LibrarySource(
        id = "saf-tree",
        platformKind = LibraryPlatformKind.AndroidSafTree,
        displayName = "Music",
        handle = "content://com.example.documents/tree/music",
        createdAtEpochMillis = 1,
    )

private fun directory(id: String, name: String) =
    SafEntry(
        documentId = id,
        name = name,
        isDirectory = true,
        uri = "content://fake/$id",
    )

private fun file(id: String, name: String, size: Long? = 16L) =
    SafEntry(
        documentId = id,
        name = name,
        isDirectory = false,
        uri = "content://fake/$id",
        sizeBytes = size,
    )

private class FakeSafTreeReader(
    private val listings: Map<String, List<SafEntry>>,
    private val sidecarBytes: Map<String, ByteArray> = emptyMap(),
    private val sidecarFailures: Set<String> = emptySet(),
    private val descriptors: Map<String, Int> = emptyMap(),
) : SafTreeReader {
    val childListingCounts = linkedMapOf<String, Int>()
    val sidecarReadIds = mutableListOf<String>()
    val openedDescriptorIds = mutableListOf<String>()
    var closedDescriptorCount = 0

    override fun children(directory: SafEntry): List<SafEntry> {
        childListingCounts[directory.documentId] =
            (childListingCounts[directory.documentId] ?: 0) + 1
        return listings[directory.documentId].orEmpty()
    }

    override fun openMetadataDescriptor(entry: SafEntry): SafFileDescriptor? =
        descriptors[entry.documentId]?.let { fd ->
            openedDescriptorIds += entry.documentId
            SafFileDescriptor(fd) { closedDescriptorCount++ }
        }

    override fun readSidecar(entry: SafEntry, maxBytes: Long): ByteArray? {
        assertEquals(MAX_LRC_BYTES, maxBytes)
        sidecarReadIds += entry.documentId
        if (entry.documentId in sidecarFailures) {
            throw IOException("unreadable sidecar")
        }
        return sidecarBytes[entry.documentId]
    }
}
