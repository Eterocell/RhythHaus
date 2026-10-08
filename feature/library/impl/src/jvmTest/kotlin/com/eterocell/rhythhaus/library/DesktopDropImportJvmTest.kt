package com.eterocell.rhythhaus.library

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDropImportJvmTest {
    @Test
    fun emptyPathCannotImportWorkingDirectoryOrDiscardValidSibling() {
        val root = Files.createTempDirectory("rhythhaus-drop-empty").toFile()
        try {
            val audio =
                root.resolve("Track.mp3").apply { writeBytes(byteArrayOf(1)) }
            val result = validateDesktopDrop(listOf("", audio.path), 1L)
            assertEquals(1, result.sources.size)
            assertEquals(
                listOf(audio.canonicalFile),
                droppedFilesFromSource(result.sources.single()))
            assertEquals(1, result.failures.size)
            assertFalse(validateDesktopDrop(listOf(""), 1L).accepted)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun oversizedDropDoesNotReadOverflowEntriesOrAllocateUnboundedDiagnostics() {
        var overflowReads = 0
        val paths =
            object : AbstractList<String>() {
                override val size = 100_000

                override fun get(index: Int): String {
                    if (index >= 128) overflowReads++
                    return "/nonexistent/rhythhaus-$index.mp3"
                }
            }
        val result = validateDesktopDrop(paths, 1L)
        assertEquals(0, overflowReads)
        assertTrue(result.failures.size <= 128)
    }

    @Test
    fun admitsFoldersAndAudioFilesWithoutCopyingAndRetainsInvalidSibling() {
        val root = Files.createTempDirectory("rhythhaus-drop").toFile()
        try {
            val folder = root.resolve("Music").apply { mkdirs() }
            val original =
                folder.resolve("Original.flac").apply {
                    writeBytes(byteArrayOf(4, 5))
                }
            val audio =
                root.resolve("Track.MP3").apply { writeBytes(byteArrayOf(1)) }
            val text =
                root.resolve("notes.txt").apply { writeText("not audio") }

            val result =
                validateDesktopDrop(
                    paths = listOf(folder.path, audio.path, text.path),
                    createdAtEpochMillis = 42L,
                )

            assertTrue(result.accepted)
            assertEquals(2, result.sources.size)
            assertEquals(folder.canonicalPath, result.sources[0].handle)
            assertTrue(result.sources[1].id.startsWith("jvm-dropped-files:"))
            assertEquals(
                listOf(DesktopDropFailureReason.Unsupported),
                result.failures.map(DesktopDropFailure::reason),
            )
            assertEquals(1, folder.listFiles()?.size ?: 0)
            assertTrue(original.readBytes().contentEquals(byteArrayOf(4, 5)))
            assertTrue(audio.readBytes().contentEquals(byteArrayOf(1)))
            assertTrue(audio.exists())
            assertTrue(text.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun canonicalDuplicateFilesReuseOneStableDroppedFilesSource() {
        val root = Files.createTempDirectory("rhythhaus-drop").toFile()
        try {
            val first =
                root.resolve("First.mp3").apply { writeBytes(byteArrayOf(1)) }
            val second =
                root.resolve("Second.flac").apply { writeBytes(byteArrayOf(2)) }

            val firstDrop =
                validateDesktopDrop(
                    paths =
                        listOf(
                            first.path,
                            second.path,
                            root.resolve("./First.mp3").path),
                    createdAtEpochMillis = 42L,
                )
            val repeatedDrop =
                validateDesktopDrop(
                    paths = listOf(second.path, first.path),
                    createdAtEpochMillis = 99L,
                )

            assertEquals(1, firstDrop.duplicatePathCount)
            assertEquals(1, firstDrop.sources.size)
            assertEquals(
                firstDrop.sources.single().id, repeatedDrop.sources.single().id)
            assertFalse(firstDrop.sources.single().handle.isBlank())
        } finally {
            root.deleteRecursively()
        }
    }
}
