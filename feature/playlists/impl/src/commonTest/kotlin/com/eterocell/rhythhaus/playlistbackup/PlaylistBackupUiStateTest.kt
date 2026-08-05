package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PlaylistBackupUiStateTest {
    private fun preview(
        revision: Long = 1,
        restorable: Int = 1
    ): PlaylistBackupPreview =
        PlaylistImportPlan(
                revision,
                if (restorable > 0)
                    listOf(PlaylistImportPlaylist(0, "Restored", emptyList()))
                else emptyList(),
                emptyList(),
                PlaylistImportTotals(
                    restorable, 0, PlaylistImportCounts(restorable, 0, 0)),
                emptyList())
            .toPreview()

    @Test
    fun staleRevisionDoesNotImport() = runBlocking {
        var calls = 0
        val result =
            confirmPlaylistBackupImportSerialized(
                PlaylistBackupUiState(preview = preview()), 2) {
                    calls++
                    error("unexpected")
                }
        assertEquals(0, calls)
        assertEquals(PlaylistBackupUiError.StalePreview, result.state.error)
    }

    @Test
    fun revisionGuardRunsBlockOnlyWhenCurrent() = runBlocking {
        var calls = 0
        confirmPlaylistBackupImportSerialized(
            PlaylistBackupUiState(preview = preview()), 1) {
                calls++
                com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult
                    .Success(PlaylistSnapshot(), 1)
            }
        assertEquals(1, calls)
    }

    @Test
    fun revisionChangeWhileConfirmationIsWaitingDoesNotImport() = runBlocking {
        val guard = RecordingStaleGuard { it.revision++ }
        val fixture = confirmationFixture(guard)

        val confirmation =
            fixture.controller.confirm(fixture.state, PlaylistSnapshot())

        assertEquals(0, guard.blockCalls)
        assertEquals(0, fixture.repository.importCalls)
        assertEquals(
            PlaylistBackupUiError.StalePreview, confirmation.state.error)
        assertEquals(PlaylistBackupOperation.Idle, confirmation.state.operation)
    }

    @Test
    fun revisionChangeWhileTransactionIsAttemptedDoesNotImport() = runBlocking {
        val guard = RecordingStaleGuard {
            it.transactionAttempts++
            it.revision++
        }
        val fixture = confirmationFixture(guard)

        val confirmation =
            fixture.controller.confirm(fixture.state, PlaylistSnapshot())

        assertEquals(1, guard.transactionAttempts)
        assertEquals(0, guard.blockCalls)
        assertEquals(0, fixture.repository.importCalls)
        assertEquals(
            PlaylistBackupUiError.StalePreview, confirmation.state.error)
        assertEquals(PlaylistBackupOperation.Idle, confirmation.state.operation)
    }

    @Test
    fun confirmationRethrowsCancellationExactly() = runBlocking {
        val cancellation = CancellationException("cancel")
        val thrown =
            assertFailsWith<CancellationException> {
                confirmPlaylistBackupImportSerialized(
                    PlaylistBackupUiState(preview = preview()), 1) {
                        throw cancellation
                    }
            }
        assertEquals(cancellation, thrown)
    }

    @Test
    fun reducerKeepsClosedPreviewInspectable() {
        val state =
            reducePlaylistBackupUiState(
                PlaylistBackupUiState(),
                PlaylistBackupUiAction.PreviewReady(preview(restorable = 0)))
        assertFalse(state.preview!!.canConfirm)
    }

    private suspend fun confirmationFixture(
        guard: RecordingStaleGuard
    ): ConfirmationFixture {
        val repository = CountingRepository()
        val launcher = RecordingLauncher()
        val controller =
            createPlaylistBackupController(
                PlaylistStateOwner(repository, Dispatchers.Default),
                Dispatchers.Default,
                launcher,
                guard)
        val saving =
            controller.beginExport(
                PlaylistBackupUiState(), PlaylistSnapshot(), emptyList(), 0)
        val opening =
            controller.beginOpen(
                controller.receiveSave(
                    saving, PlaylistBackupDocumentSaveResult.Success))
        val state =
            controller.receiveOpen(
                opening,
                PlaylistBackupDocumentOpenResult.Success(
                    checkNotNull(launcher.bytes)),
                emptyList(),
                emptyList(),
                " imported",
                1,
            )
        return ConfirmationFixture(controller, state, repository)
    }

    private data class ConfirmationFixture(
        val controller: PlaylistBackupController,
        val state: PlaylistBackupUiState,
        val repository: CountingRepository,
    )

    private class RecordingStaleGuard(
        private val reviseBeforeTransaction: (RecordingStaleGuard) -> Unit,
    ) : PlaylistBackupRevisionGuard {
        var revision = 1L
        var transactionAttempts = 0
        var blockCalls = 0

        override suspend fun <T> withCurrentRevision(
            expectedRevision: Long,
            block: suspend () -> T,
        ): PlaylistBackupRevisionGuardResult<T> {
            reviseBeforeTransaction(this)
            return PlaylistBackupRevisionGuardResult.Stale
        }
    }

    private class CountingRepository : PlaylistRepository {
        var importCalls = 0

        override fun playlists(): List<PlaylistSummary> = emptyList()

        override fun playlist(id: String): PlaylistSummary? = null

        override fun entries(playlistId: String): List<PlaylistEntry> =
            emptyList()

        override fun create(name: String): PlaylistSummary = error("unused")

        override fun createWithEntries(
            name: String,
            trackIds: List<String>
        ): PlaylistSummary = error("unused")

        override fun importPlaylists(
            playlists: List<PlaylistImportMutation>
        ): List<PlaylistSummary> {
            importCalls++
            return emptyList()
        }

        override fun rename(id: String, name: String) = error("unused")

        override fun delete(id: String) = error("unused")

        override fun append(playlistId: String, trackIds: List<String>) =
            error("unused")

        override fun removeEntry(entryId: String) = error("unused")

        override fun reorder(playlistId: String, entryIds: List<String>) =
            error("unused")
    }

    private class RecordingLauncher : PlaylistBackupDocumentLauncher {
        override val isAvailable = true
        var bytes: ByteArray? = null

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            this.bytes = bytes
        }

        override fun open() = Unit
    }
}
