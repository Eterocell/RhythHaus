package com.eterocell.rhythhaus.playlistbackup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlatformPlaylistBackupDocumentsAndroidTest {
    private val uri = "content://rhythhaus/backup"

    @Test
    fun saveWritesCompletePayloadExactlyOnce() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val output = ByteArrayOutputStream()
        var opens = 0

        val result = saveAndroidPlaylistBackupDocument(uri, payload) {
            opens += 1
            output
        }

        assertIs<PlaylistBackupDocumentSaveResult.Success>(result)
        assertEquals(1, opens)
        assertContentEquals(payload, output.toByteArray())
    }

    @Test
    fun nullSaveSelectionIsCancellationAndNullStreamIsUnavailable() {
        assertIs<PlaylistBackupDocumentSaveResult.Cancelled>(saveAndroidPlaylistBackupDocument(null, byteArrayOf(1)) { error("unused") })
        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(saveAndroidPlaylistBackupDocument(uri, byteArrayOf(1)) { null })
    }

    @Test
    fun saveExceptionIsFailure() {
        assertIs<PlaylistBackupDocumentSaveResult.Failure>(
            saveAndroidPlaylistBackupDocument(uri, byteArrayOf(1)) { error("open failed") },
        )
    }

    @Test
    fun openAcceptsExactLimitAndRejectsLimitPlusOne() {
        val exact = ByteArray(PlaylistBackupMaxBytes)
        val success = openAndroidPlaylistBackupDocument(uri) { ByteArrayInputStream(exact) }
        assertContentEquals(exact, assertIs<PlaylistBackupDocumentOpenResult.Success>(success).bytes)

        val oversized = openAndroidPlaylistBackupDocument(uri) {
            ByteArrayInputStream(ByteArray(PlaylistBackupMaxBytes + 1))
        }
        assertEquals(PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes), oversized)
    }

    @Test
    fun nullOpenSelectionUnavailableStreamAndExceptionAreDistinct() {
        assertIs<PlaylistBackupDocumentOpenResult.Cancelled>(openAndroidPlaylistBackupDocument(null) { error("unused") })
        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(openAndroidPlaylistBackupDocument(uri) { null })
        assertIs<PlaylistBackupDocumentOpenResult.Failure>(openAndroidPlaylistBackupDocument(uri) { error("open failed") })
    }

    @Test
    fun boundedReaderMakesProgressAfterZeroLengthRead() {
        val input = ZeroThenBytesInputStream(byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(1, 2, 3), readAndroidPlaylistBackupBounded(input))
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
