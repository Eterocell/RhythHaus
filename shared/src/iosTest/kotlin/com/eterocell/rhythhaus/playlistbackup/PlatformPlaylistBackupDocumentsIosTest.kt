package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlatformPlaylistBackupDocumentsIosTest {
    @Test
    fun jsonOnlyProviderRejectsInteroperabilityWithoutOpeningJson() {
        var saves = 0
        var opens = 0
        IOSPlaylistBackupDocumentBridge.provider =
            object : IOSPlaylistBackupDocumentProvider {
                override fun saveDocument(
                    fileName: String,
                    bytes: ByteArray,
                    completion: IOSPlaylistBackupDocumentCompletion
                ) {
                    saves++
                    completion.complete(
                        IOSPlaylistBackupDocumentStatus.SUCCESS, null, null)
                }

                override fun openDocument(
                    maxBytes: Int,
                    completion: IOSPlaylistBackupDocumentCompletion
                ) {
                    opens++
                    completion.complete(
                        IOSPlaylistBackupDocumentStatus.CANCELLED, null, null)
                }
            }
        var saved: PlaylistBackupDocumentSaveResult? = null
        var opened: PlaylistBackupDocumentOpenResult? = null
        val launcher =
            iosPlaylistBackupDocumentLauncher({ saved = it }, { opened = it })
        launcher.save("Mix", byteArrayOf(1), PlaylistDocumentFormat.Pls)
        launcher.open(PlaylistDocumentFormat.M3u)
        assertIs<PlaylistBackupDocumentSaveResult.Unavailable>(saved)
        assertIs<PlaylistBackupDocumentOpenResult.Unavailable>(opened)
        assertEquals(0, saves)
        assertEquals(0, opens)
        launcher.save("Mix", byteArrayOf(1))
        launcher.open()
        assertEquals(PlaylistBackupDocumentSaveResult.Success, saved)
        assertEquals(PlaylistBackupDocumentOpenResult.Cancelled, opened)
        assertEquals(1, saves)
        assertEquals(1, opens)
    }

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

    @Test
    fun formatAwareSavePassesSelectedExtensionWithoutJsonNormalization() {
        val provider = RecordingIosDocumentProvider()
        IOSPlaylistBackupDocumentBridge.provider = provider
        val launcher = iosPlaylistBackupDocumentLauncher({}, {})

        launcher.save(
            suggestedFileName = "mix",
            bytes = byteArrayOf(1),
            format = PlaylistDocumentFormat.Pls,
        )

        assertEquals("mix.pls", provider.fileName)
        assertEquals(".pls", provider.format)
    }
}

private class RecordingIosDocumentProvider :
    IOSFormattedPlaylistBackupDocumentProvider {
    var fileName: String? = null
    var format: String? = null

    override fun saveDocument(
        fileName: String,
        bytes: ByteArray,
        completion: IOSPlaylistBackupDocumentCompletion,
    ) = Unit

    override fun openDocument(
        maxBytes: Int,
        completion: IOSPlaylistBackupDocumentCompletion,
    ) = Unit

    override fun saveDocumentWithFormat(
        fileName: String,
        bytes: ByteArray,
        format: String,
        completion: IOSPlaylistBackupDocumentCompletion,
    ) {
        this.fileName = fileName
        this.format = format
    }

    override fun openDocumentWithFormats(
        maxBytes: Int,
        mimeTypes: List<String>,
        completion: IOSPlaylistBackupDocumentCompletion,
    ) =
        completion.complete(
            IOSPlaylistBackupDocumentStatus.CANCELLED, null, null)
}
