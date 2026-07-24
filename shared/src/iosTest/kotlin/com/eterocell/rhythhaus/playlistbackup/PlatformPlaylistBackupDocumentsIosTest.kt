package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlatformPlaylistBackupDocumentsIosTest {
    @AfterTest
    fun clearProvider() {
        IOSPlaylistBackupDocumentBridge.provider = null
    }

    @Test
    fun unavailableProviderProducesDistinctResults() {
        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(
            iosPlaylistBackupUnavailableSaveResult())
        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(
            iosPlaylistBackupUnavailableOpenResult())
    }

    @Test
    fun completionMapsSuccessCancellationFailureAndOversized() {
        assertIs<PlaylistBackupDocumentSaveResult.Success>(
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.SUCCESS, null))
        assertIs<PlaylistBackupDocumentSaveResult.Cancelled>(
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.CANCELLED, null))
        assertIs<PlaylistBackupDocumentSaveResult.Failure>(
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.FAILURE, "failed"))
        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.UNAVAILABLE, "unavailable"),
        )

        val bytes = byteArrayOf(1, 2, 3)
        assertContentEquals(
            bytes,
            assertIs<PlaylistBackupDocumentOpenResult.Success>(
                    iosPlaylistBackupOpenResult(
                        IOSPlaylistBackupDocumentStatus.SUCCESS, bytes, null),
                )
                .bytes,
        )
        assertIs<PlaylistBackupDocumentOpenResult.Cancelled>(
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.CANCELLED, null, null),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.TOO_LARGE, null, null),
        )
        assertIs<PlaylistBackupDocumentOpenResult.Failure>(
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.FAILURE, null, "failed"),
        )
        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.UNAVAILABLE,
                null,
                "unavailable"),
        )
    }

    @Test
    fun bridgeRetainsRegisteredProvider() {
        val provider = FakeProvider()
        IOSPlaylistBackupDocumentBridge.provider = provider
        assertEquals(provider, IOSPlaylistBackupDocumentBridge.provider)
    }
}

private class FakeProvider : IOSPlaylistBackupDocumentProvider {
    override fun saveDocument(
        fileName: String,
        bytes: ByteArray,
        completion: IOSPlaylistBackupDocumentCompletion
    ) = Unit

    override fun openDocument(
        maxBytes: Int,
        completion: IOSPlaylistBackupDocumentCompletion
    ) = Unit
}
