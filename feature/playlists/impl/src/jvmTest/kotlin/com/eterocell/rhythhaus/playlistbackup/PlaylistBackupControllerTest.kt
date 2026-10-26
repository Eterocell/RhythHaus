package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PlaylistBackupControllerTest {
    @Test
    fun cancellationAndReadFailureReleaseTheSameExclusiveDocumentOwner() =
        runBlocking {
            val launcher = RecordingLauncher()
            val controller = controller(launcher)
            for (format in
                listOf(
                    PlaylistDocumentFormat.RhythHausJson,
                    PlaylistDocumentFormat.M3u,
                    PlaylistDocumentFormat.M3u8,
                    PlaylistDocumentFormat.Pls)) {
                val opening =
                    controller.beginOpen(PlaylistBackupUiState(), format)
                controller.beginOpen(opening, format)
                assertEquals(1, launcher.openCalls)
                val cancelled =
                    controller.receiveOpen(
                        opening,
                        PlaylistBackupDocumentOpenResult.Cancelled,
                        emptyList(),
                        emptyList(),
                        "Imported",
                        1)
                assertEquals(PlaylistBackupOperation.Idle, cancelled.operation)
                assertNull(cancelled.preview)
                assertNull(cancelled.error)
                val retry = controller.beginOpen(cancelled, format)
                assertEquals(2, launcher.openCalls)
                val failed =
                    controller.receiveOpen(
                        retry,
                        PlaylistBackupDocumentOpenResult.Failure(
                            "read failure"),
                        emptyList(),
                        emptyList(),
                        "Imported",
                        1)
                assertEquals(PlaylistBackupUiError.ReadFailed, failed.error)
                assertNull(failed.preview)
                val secondRetry = controller.beginOpen(failed, format)
                assertEquals(3, launcher.openCalls)
                assertEquals(
                    PlaylistBackupOperation.Opening, secondRetry.operation)
                controller.receiveOpen(
                    secondRetry,
                    PlaylistBackupDocumentOpenResult.Cancelled,
                    emptyList(),
                    emptyList(),
                    "Imported",
                    1)
                launcher.openCalls = 0
            }
        }

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
        var openCalls = 0

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            saveCalls++
        }

        override fun open() {
            openCalls++
        }
    }
}
