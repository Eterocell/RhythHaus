package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PlatformPlaylistBackupDocumentsTest {
    @Test
    fun backupExtensionIsAppendedOnlyWhenAbsent() {
        assertEquals(
            "playlists.rhythhaus-playlists.json",
            playlistBackupFileName("playlists"))
        assertEquals(
            "playlists.rhythhaus-playlists.json",
            playlistBackupFileName("playlists.rhythhaus-playlists.json"))
    }

    @Test
    fun backupExtensionComparisonIgnoresCase() {
        assertEquals(
            "PLAYLISTS.RHYTHHAUS-PLAYLISTS.JSON",
            playlistBackupFileName("PLAYLISTS.RHYTHHAUS-PLAYLISTS.JSON"))
    }

    @Test
    fun backupFileNameIsAlwaysOneSafePathComponent() {
        assertEquals(
            "backup.rhythhaus-playlists.json",
            playlistBackupFileName("../backup"))
        assertEquals(
            "backup.rhythhaus-playlists.json",
            playlistBackupFileName("folder\\backup"))
        assertEquals(
            "rhythhaus-playlists.rhythhaus-playlists.json",
            playlistBackupFileName(".."))
        assertEquals(
            "rhythhaus-playlists.rhythhaus-playlists.json",
            playlistBackupFileName("  "))
    }

    @Test
    fun operationGateRejectsOverlapUntilCompletion() {
        val gate = PlaylistBackupDocumentOperationGate()

        assertEquals(true, gate.tryStart())
        assertEquals(false, gate.tryStart())
        gate.finish()
        assertEquals(true, gate.tryStart())
    }

    @Test
    fun duplicateSaveTerminalCallbacksSettleOnceWithoutDuplicateAction() =
        runBlocking {
            val launcher = CallbackRecordingLauncher()
            val controller = controller(launcher)
            val saving =
                controller.beginExport(
                    PlaylistBackupUiState(), PlaylistSnapshot(), emptyList(), 0)
            val first =
                controller.receiveSave(
                    saving, PlaylistBackupDocumentSaveResult.Cancelled)
            val duplicate =
                controller.receiveSave(
                    first, PlaylistBackupDocumentSaveResult.Cancelled)

            assertEquals(1, launcher.saveDeliveries)
            assertEquals(PlaylistBackupOperation.Idle, first.operation)
            assertEquals(first, duplicate)
        }

    @Test
    fun duplicateOpenTerminalCallbacksSettleOnceWithoutDuplicateAction() =
        runBlocking {
            val launcher = CallbackRecordingLauncher()
            val controller = controller(launcher)
            val opening = controller.beginOpen(PlaylistBackupUiState())
            val first =
                controller.receiveOpen(
                    opening,
                    PlaylistBackupDocumentOpenResult.Cancelled,
                    emptyList(),
                    emptyList(),
                    " imported",
                    1)
            val duplicate =
                controller.receiveOpen(
                    first,
                    PlaylistBackupDocumentOpenResult.Cancelled,
                    emptyList(),
                    emptyList(),
                    " imported",
                    1)

            assertEquals(1, launcher.openDeliveries)
            assertEquals(PlaylistBackupOperation.Idle, first.operation)
            assertEquals(first, duplicate)
        }

    private fun controller(launcher: CallbackRecordingLauncher) =
        createPlaylistBackupController(
            PlaylistStateOwner(
                InMemoryPlaylistRepository(), Dispatchers.Default),
            Dispatchers.Default,
            launcher,
            object : PlaylistBackupRevisionGuard {
                override suspend fun <T> withCurrentRevision(
                    expectedRevision: Long,
                    block: suspend () -> T
                ) = PlaylistBackupRevisionGuardResult.Current(block())
            },
        )

    private class CallbackRecordingLauncher : PlaylistBackupDocumentLauncher {
        override val isAvailable = true
        var saveDeliveries = 0
        var openDeliveries = 0

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            saveDeliveries++
        }

        override fun open() {
            openDeliveries++
        }
    }
}
