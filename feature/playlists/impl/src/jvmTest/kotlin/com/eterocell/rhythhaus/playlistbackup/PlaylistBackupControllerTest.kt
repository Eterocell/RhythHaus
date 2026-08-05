package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PlaylistBackupControllerTest {
    @Test
    fun exportAwaitsPreparationAndCancellationRethrows() = runBlocking {
        val launcher = RecordingLauncher()
        val state =
            controller(launcher)
                .beginExport(
                    PlaylistBackupUiState(), PlaylistSnapshot(), emptyList(), 0)
        assertEquals(PlaylistBackupOperation.Saving, state.operation)
        assertEquals(1, launcher.saveCalls)
    }

    @Test
    fun terminalCallbacksSettleControllerOperationOnce() = runBlocking {
        val controller = controller(RecordingLauncher())
        val saving =
            controller.beginExport(
                PlaylistBackupUiState(), PlaylistSnapshot(), emptyList(), 0)
        val settled =
            controller.receiveSave(
                saving, PlaylistBackupDocumentSaveResult.Success)
        assertEquals(PlaylistBackupOperation.Idle, settled.operation)
        assertEquals(
            settled,
            controller.receiveSave(
                settled, PlaylistBackupDocumentSaveResult.Success))
    }

    private fun controller(launcher: RecordingLauncher) =
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

    private class RecordingLauncher : PlaylistBackupDocumentLauncher {
        override val isAvailable = true
        var saveCalls = 0

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            saveCalls++
        }

        override fun open() = Unit
    }
}
