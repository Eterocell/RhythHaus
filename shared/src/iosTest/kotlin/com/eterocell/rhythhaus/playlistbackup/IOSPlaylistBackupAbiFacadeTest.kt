package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IOSPlaylistBackupAbiFacadeTest {
    @AfterTest
    fun clearProvider() {
        IOSPlaylistBackupDocumentBridge.provider = null
    }

    @Test
    fun statusesAndNullabilityMapToInjectedLauncher() {
        val provider = RecordingProvider()
        IOSPlaylistBackupDocumentBridge.provider = provider
        val saveResults = mutableListOf<PlaylistBackupDocumentSaveResult>()
        val openResults = mutableListOf<PlaylistBackupDocumentOpenResult>()
        val launcher =
            iosPlaylistBackupDocumentLauncher(
                onSaveResult = saveResults::add,
                onOpenResult = openResults::add,
            )

        assertEquals(
            "application/vnd.rhythhaus.playlists+json", PlaylistBackupMimeType)
        assertEquals(4 * 1024 * 1024, PlaylistBackupMaxBytes)
        assertEquals(0, IOSPlaylistBackupDocumentStatus.SUCCESS)
        assertEquals(1, IOSPlaylistBackupDocumentStatus.CANCELLED)
        assertEquals(2, IOSPlaylistBackupDocumentStatus.TOO_LARGE)
        assertEquals(3, IOSPlaylistBackupDocumentStatus.FAILURE)
        assertEquals(4, IOSPlaylistBackupDocumentStatus.UNAVAILABLE)
        assertEquals(provider, IOSPlaylistBackupDocumentBridge.provider)
        assertTrue(launcher.isAvailable)

        provider.saveStatus = IOSPlaylistBackupDocumentStatus.SUCCESS
        launcher.save("backup", byteArrayOf(1))
        provider.saveStatus = IOSPlaylistBackupDocumentStatus.CANCELLED
        launcher.save("backup", byteArrayOf(1))
        provider.saveStatus = IOSPlaylistBackupDocumentStatus.FAILURE
        provider.message = "save failed"
        launcher.save("backup", byteArrayOf(1))
        provider.saveStatus = IOSPlaylistBackupDocumentStatus.UNAVAILABLE
        provider.message = null
        launcher.save("backup", byteArrayOf(1))

        assertEquals(
            listOf(
                PlaylistBackupDocumentSaveResult.Success,
                PlaylistBackupDocumentSaveResult.Cancelled,
                PlaylistBackupDocumentSaveResult.Failure("save failed"),
                PlaylistBackupDocumentSaveResult.Unavailable(
                    "iOS document provider is unavailable"),
            ),
            saveResults,
        )

        provider.openStatus = IOSPlaylistBackupDocumentStatus.SUCCESS
        provider.bytes = byteArrayOf(3, 4)
        provider.message = "ignored"
        launcher.open()
        provider.bytes = null
        provider.message = null
        launcher.open()
        provider.openStatus = IOSPlaylistBackupDocumentStatus.CANCELLED
        launcher.open()
        provider.openStatus = IOSPlaylistBackupDocumentStatus.TOO_LARGE
        launcher.open()
        provider.openStatus = IOSPlaylistBackupDocumentStatus.FAILURE
        provider.message = "open failed"
        launcher.open()
        provider.openStatus = IOSPlaylistBackupDocumentStatus.UNAVAILABLE
        provider.message = null
        launcher.open()

        assertTrue(openResults[0] is PlaylistBackupDocumentOpenResult.Success)
        assertTrue(
            (openResults[0] as PlaylistBackupDocumentOpenResult.Success)
                .bytes
                .contentEquals(byteArrayOf(3, 4)))
        assertEquals(
            PlaylistBackupDocumentOpenResult.Failure(
                "Document provider returned no bytes"),
            openResults[1],
        )
        assertEquals(PlaylistBackupDocumentOpenResult.Cancelled, openResults[2])
        assertEquals(
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes),
            openResults[3])
        assertEquals(
            PlaylistBackupDocumentOpenResult.Failure("open failed"),
            openResults[4])
        assertEquals(
            PlaylistBackupDocumentOpenResult.Unavailable(
                "iOS document provider is unavailable"),
            openResults[5],
        )
        assertEquals(PlaylistBackupMaxBytes, provider.maxBytes)

        IOSPlaylistBackupDocumentBridge.provider = null
        assertFalse(launcher.isAvailable)
        launcher.save("backup", byteArrayOf(1))
        launcher.open()
        assertEquals(
            PlaylistBackupDocumentSaveResult.Unavailable(
                "iOS document provider is unavailable"),
            saveResults.last())
        assertEquals(
            PlaylistBackupDocumentOpenResult.Unavailable(
                "iOS document provider is unavailable"),
            openResults.last())
    }
}

private class RecordingProvider : IOSPlaylistBackupDocumentProvider {
    var maxBytes: Int? = null
    var saveStatus = IOSPlaylistBackupDocumentStatus.SUCCESS
    var openStatus = IOSPlaylistBackupDocumentStatus.SUCCESS
    var bytes: ByteArray? = null
    var message: String? = null

    override fun saveDocument(
        fileName: String,
        bytes: ByteArray,
        completion: IOSPlaylistBackupDocumentCompletion
    ) {
        completion.complete(saveStatus, null, message)
    }

    override fun openDocument(
        maxBytes: Int,
        completion: IOSPlaylistBackupDocumentCompletion
    ) {
        this.maxBytes = maxBytes
        completion.complete(openStatus, bytes, message)
    }
}
