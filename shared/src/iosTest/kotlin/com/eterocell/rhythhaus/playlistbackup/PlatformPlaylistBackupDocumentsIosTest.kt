package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformPlaylistBackupDocumentsIosTest {
    @AfterTest
    fun clearProvider() {
        IOSPlaylistBackupDocumentBridge.provider = null
    }

    @Test
    fun unavailableProviderProducesDistinctResults() {
        assertEquals(
            PlaylistBackupDocumentSaveResult.Unavailable(
                "iOS document provider is unavailable"),
            iosPlaylistBackupUnavailableSaveResult(),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.Unavailable(
                "iOS document provider is unavailable"),
            iosPlaylistBackupUnavailableOpenResult(),
        )
    }

    @Test
    fun completionMapsSuccessCancellationFailureAndOversized() {
        assertEquals(
            PlaylistBackupDocumentSaveResult.Success,
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.SUCCESS, null),
        )
        assertEquals(
            PlaylistBackupDocumentSaveResult.Cancelled,
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.CANCELLED, null),
        )
        assertEquals(
            PlaylistBackupDocumentSaveResult.Failure("failed"),
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.FAILURE, "failed"),
        )
        assertEquals(
            PlaylistBackupDocumentSaveResult.Unavailable("unavailable"),
            iosPlaylistBackupSaveResult(
                IOSPlaylistBackupDocumentStatus.UNAVAILABLE, "unavailable"),
        )

        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(
            PlaylistBackupDocumentOpenResult.Success(bytes),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.SUCCESS, bytes, null),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.Failure(
                "Document provider returned no bytes"),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.SUCCESS, null, null),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.Cancelled,
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.CANCELLED, null, null),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.TOO_LARGE, null, null),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.Failure("failed"),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.FAILURE, null, "failed"),
        )
        assertEquals(
            PlaylistBackupDocumentOpenResult.Unavailable("unavailable"),
            iosPlaylistBackupOpenResult(
                IOSPlaylistBackupDocumentStatus.UNAVAILABLE,
                null,
                "unavailable"),
        )
    }
}
