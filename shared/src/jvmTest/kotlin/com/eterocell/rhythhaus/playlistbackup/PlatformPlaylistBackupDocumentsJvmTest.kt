package com.eterocell.rhythhaus.playlistbackup

import java.io.File
import java.io.InputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class PlatformPlaylistBackupDocumentsJvmTest {
    @Test
    fun saveWritesCompletePayloadExactlyOnceAndAppendsExtension() {
        val selected = File(createTempDirectory().toFile(), "backup")
        val payload = byteArrayOf(1, 2, 3, 4)
        var writes = 0
        var writtenFile: File? = null
        var writtenBytes: ByteArray? = null

        val result = saveJvmPlaylistBackupDocument(
            bytes = payload,
            selectFile = { selected },
            writeFile = { file, bytes ->
                writes += 1
                writtenFile = file
                writtenBytes = bytes.copyOf()
            },
        )

        assertIs<PlaylistBackupDocumentSaveResult.Success>(result)
        assertEquals(1, writes)
        assertEquals("backup$PlaylistBackupFileExtension", writtenFile?.name)
        assertContentEquals(payload, writtenBytes)
    }

    @Test
    fun saveCancellationIsSilentAndDoesNotWrite() {
        var writes = 0
        val result = saveJvmPlaylistBackupDocument(byteArrayOf(1), { null }) { _, _ -> writes += 1 }

        assertIs<PlaylistBackupDocumentSaveResult.Cancelled>(result)
        assertEquals(0, writes)
    }

    @Test
    fun saveSelectionExceptionIsFailure() {
        val result = saveJvmPlaylistBackupDocument(byteArrayOf(1), { error("panel failed") }) { _, _ -> }

        assertIs<PlaylistBackupDocumentSaveResult.Failure>(result)
    }

    @Test
    fun openAcceptsExactLimitAndRejectsLimitPlusOne() {
        val selected = File("backup$PlaylistBackupFileExtension")
        val exact = ByteArray(PlaylistBackupMaxBytes)
        assertContentEquals(
            exact,
            assertIs<PlaylistBackupDocumentOpenResult.Success>(
                openJvmPlaylistBackupDocument({ selected }) { exact },
            ).bytes,
        )

        val oversized = openJvmPlaylistBackupDocument({ selected }) { ByteArray(PlaylistBackupMaxBytes + 1) }
        assertEquals(PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes), oversized)
    }

    @Test
    fun openCancellationAndReadExceptionAreDistinct() {
        assertIs<PlaylistBackupDocumentOpenResult.Cancelled>(openJvmPlaylistBackupDocument({ null }) { error("unused") })
        assertIs<PlaylistBackupDocumentOpenResult.Failure>(
            openJvmPlaylistBackupDocument({ File("backup") }) { error("read failed") },
        )
    }

    @Test
    fun temporaryAppleDirectoryPropertyIsRestored() {
        val key = "apple.awt.fileDialogForDirectories"
        val previous = System.getProperty(key)
        System.setProperty(key, "custom")
        try {
            withJvmDocumentDialogMode { assertNull(System.getProperty(key)) }
            assertEquals("custom", System.getProperty(key))
        } finally {
            if (previous == null) System.clearProperty(key) else System.setProperty(key, previous)
        }
    }

    @Test
    fun boundedReaderMakesProgressAfterZeroLengthRead() {
        val input = ZeroThenBytesInputStream(byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(1, 2, 3), readJvmPlaylistBackupBounded(input))
    }

    @Test
    fun boundedReaderReadsExactLimitAndOnlyLimitPlusOneFromLargerStreams() {
        val exact = CountingInputStream(PlaylistBackupMaxBytes)
        assertEquals(PlaylistBackupMaxBytes, readJvmPlaylistBackupBounded(exact).size)
        assertEquals(PlaylistBackupMaxBytes, exact.bytesRead)

        val plusOne = CountingInputStream(PlaylistBackupMaxBytes + 1)
        assertEquals(PlaylistBackupMaxBytes + 1, readJvmPlaylistBackupBounded(plusOne).size)
        assertEquals(PlaylistBackupMaxBytes + 1, plusOne.bytesRead)

        val muchLarger = CountingInputStream(PlaylistBackupMaxBytes * 2)
        assertEquals(PlaylistBackupMaxBytes + 1, readJvmPlaylistBackupBounded(muchLarger).size)
        assertEquals(PlaylistBackupMaxBytes + 1, muchLarger.bytesRead)
    }

    @Test
    fun dialogModeRestoresOriginallyAbsentPropertyAfterThrowingBlock() {
        val properties = FakeJvmSystemPropertyAccess()

        assertFailsWith<IllegalStateException> {
            withJvmDocumentDialogMode(properties) {
                assertNull(properties.get("apple.awt.fileDialogForDirectories"))
                error("dialog failed")
            }
        }

        assertNull(properties.get("apple.awt.fileDialogForDirectories"))
    }

    @Test
    fun dialogModeRestoresExactExistingPropertyAfterThrowingBlock() {
        val properties = FakeJvmSystemPropertyAccess("custom")

        assertFailsWith<IllegalStateException> {
            withJvmDocumentDialogMode(properties) { error("dialog failed") }
        }

        assertEquals("custom", properties.get("apple.awt.fileDialogForDirectories"))
    }
}

private class CountingInputStream(private val size: Int) : InputStream() {
    var bytesRead = 0

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (bytesRead >= size) return -1
        val count = minOf(length, size - bytesRead)
        target.fill(1, offset, offset + count)
        bytesRead += count
        return count
    }

    override fun read(): Int = if (bytesRead >= size) -1 else 1.also { bytesRead += 1 }
}

private class FakeJvmSystemPropertyAccess(initialValue: String? = null) : JvmSystemPropertyAccess {
    private var value = initialValue

    override fun get(key: String): String? = value

    override fun set(key: String, value: String) {
        this.value = value
    }

    override fun clear(key: String) {
        value = null
    }
}

private class ZeroThenBytesInputStream(private val bytes: ByteArray) : InputStream() {
    private var returnedZero = false
    private var index = 0

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (!returnedZero) {
            returnedZero = true
            return 0
        }
        if (index >= bytes.size) return -1
        val count = minOf(length, bytes.size - index)
        bytes.copyInto(target, offset, index, index + count)
        index += count
        return count
    }

    override fun read(): Int = if (index >= bytes.size) -1 else bytes[index++].toInt() and 0xff
}
