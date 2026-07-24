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

        val result =
            saveAndroidPlaylistBackupDocument(uri, payload) {
                opens += 1
                output
            }

        assertIs<PlaylistBackupDocumentSaveResult.Success>(result)
        assertEquals(1, opens)
        assertContentEquals(payload, output.toByteArray())
    }

    @Test
    fun nullSaveSelectionIsCancellationAndNullStreamIsUnavailable() {
        assertIs<PlaylistBackupDocumentSaveResult.Cancelled>(
            saveAndroidPlaylistBackupDocument(null, byteArrayOf(1)) {
                error("unused")
            })
        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(
            saveAndroidPlaylistBackupDocument(uri, byteArrayOf(1)) { null })
    }

    @Test
    fun saveExceptionIsFailure() {
        assertIs<PlaylistBackupDocumentSaveResult.Failure>(
            saveAndroidPlaylistBackupDocument(uri, byteArrayOf(1)) {
                error("open failed")
            },
        )
    }

    @Test
    fun openAcceptsExactLimitAndRejectsLimitPlusOne() {
        val exact = ByteArray(PlaylistBackupMaxBytes)
        val success =
            openAndroidPlaylistBackupDocument(uri) {
                ByteArrayInputStream(exact)
            }
        assertContentEquals(
            exact,
            assertIs<PlaylistBackupDocumentOpenResult.Success>(success).bytes)

        val oversized =
            openAndroidPlaylistBackupDocument(uri) {
                ByteArrayInputStream(ByteArray(PlaylistBackupMaxBytes + 1))
            }
        assertEquals(
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes),
            oversized)
    }

    @Test
    fun nullOpenSelectionUnavailableStreamAndExceptionAreDistinct() {
        assertIs<PlaylistBackupDocumentOpenResult.Cancelled>(
            openAndroidPlaylistBackupDocument(null) { error("unused") })
        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(
            openAndroidPlaylistBackupDocument(uri) { null })
        assertIs<PlaylistBackupDocumentOpenResult.Failure>(
            openAndroidPlaylistBackupDocument(uri) { error("open failed") })
    }

    @Test
    fun boundedReaderMakesProgressAfterZeroLengthRead() {
        val input = ZeroThenBytesInputStream(byteArrayOf(1, 2, 3))

        assertContentEquals(
            byteArrayOf(1, 2, 3), readAndroidPlaylistBackupBounded(input))
    }

    @Test
    fun saveThenOverlappingOpenUsesSaveChannelAndReleasesPayloadOnCancellation() {
        val saveResults = mutableListOf<PlaylistBackupDocumentSaveResult>()
        val openResults = mutableListOf<PlaylistBackupDocumentOpenResult>()
        val coordinator =
            AndroidPlaylistBackupDocumentCoordinator(
                saveResults::add, openResults::add)
        val payload = byteArrayOf(1, 2)

        coordinator.launchSave(payload) {}
        coordinator.launchOpen { error("overlapping open must not launch") }
        coordinator.completeSave<String>(null) { _, _ ->
            error("cancel must not write")
        }

        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(
            openResults.single())
        assertIs<PlaylistBackupDocumentSaveResult.Cancelled>(
            saveResults.single())
        assertEquals(false, coordinator.hasPendingSavePayload)
        assertEquals(false, coordinator.isActive)
    }

    @Test
    fun openThenOverlappingSaveUsesSaveChannelAndReleasesOnSuccess() {
        val saveResults = mutableListOf<PlaylistBackupDocumentSaveResult>()
        val openResults = mutableListOf<PlaylistBackupDocumentOpenResult>()
        val coordinator =
            AndroidPlaylistBackupDocumentCoordinator(
                saveResults::add, openResults::add)

        coordinator.launchOpen {}
        coordinator.launchSave(byteArrayOf(1)) {
            error("overlapping save must not launch")
        }
        coordinator.completeOpen("uri") { ByteArrayInputStream(byteArrayOf(4)) }

        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(
            saveResults.single())
        assertContentEquals(
            byteArrayOf(4),
            assertIs<PlaylistBackupDocumentOpenResult.Success>(
                    openResults.single())
                .bytes)
        assertEquals(false, coordinator.hasPendingSavePayload)
        assertEquals(false, coordinator.isActive)
    }

    @Test
    fun successfulSaveCallbackConsumesPayloadOnceAndReleasesGate() {
        val results = mutableListOf<PlaylistBackupDocumentSaveResult>()
        val coordinator =
            AndroidPlaylistBackupDocumentCoordinator(results::add) {}
        val output = ByteArrayOutputStream()

        coordinator.launchSave(byteArrayOf(7, 8)) {}
        coordinator.completeSave("uri") { _, _ -> output }

        assertContentEquals(byteArrayOf(7, 8), output.toByteArray())
        assertIs<PlaylistBackupDocumentSaveResult.Success>(results.single())
        assertEquals(false, coordinator.hasPendingSavePayload)
        assertEquals(false, coordinator.isActive)
    }

    @Test
    fun synchronousLaunchExceptionsClearPayloadReleaseGateAndUseCorrectChannel() {
        val saveResults = mutableListOf<PlaylistBackupDocumentSaveResult>()
        val openResults = mutableListOf<PlaylistBackupDocumentOpenResult>()
        val coordinator =
            AndroidPlaylistBackupDocumentCoordinator(
                saveResults::add, openResults::add)

        coordinator.launchSave(byteArrayOf(1)) { error("save launch") }
        assertIs<PlaylistBackupDocumentSaveResult.Failure>(saveResults.single())
        assertEquals(false, coordinator.hasPendingSavePayload)
        assertEquals(false, coordinator.isActive)

        coordinator.launchOpen { error("open launch") }
        assertIs<PlaylistBackupDocumentOpenResult.Failure>(openResults.single())
        assertEquals(false, coordinator.isActive)
    }
}

private class ZeroThenBytesInputStream(private val bytes: ByteArray) :
    InputStream() {
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

    override fun read(): Int =
        if (index >= bytes.size) -1 else bytes[index++].toInt() and 0xff
}
