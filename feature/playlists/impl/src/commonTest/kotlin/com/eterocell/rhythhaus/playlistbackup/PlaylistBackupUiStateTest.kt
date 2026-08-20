package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

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
    fun reducerKeepsClosedPreviewInspectable() {
        val state =
            reducePlaylistBackupUiState(
                PlaylistBackupUiState(),
                PlaylistBackupUiAction.PreviewReady(preview(restorable = 0)))
        assertFalse(state.preview!!.canConfirm)
    }

    /**
     * Two confirmations issued from the same UI frame must import exactly once.
     * The losing confirmation returns the caller state without touching the
     * repository, so the winning import cannot be duplicated and its success
     * state cannot be clobbered by a stale busy snapshot.
     */
    @Test
    fun concurrentConfirmationsImportExactlyOnce() = runBlocking {
        val repository = CountingRepository()
        val launcher = RecordingLauncher()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val controller =
            createPlaylistBackupController(
                PlaylistStateOwner(repository, Dispatchers.Default),
                Dispatchers.Default,
                launcher,
                SuspendingSucceedingGuard(started, release))
        val state = openedState(controller, launcher)

        val first = async { controller.confirm(state, PlaylistSnapshot()) }
        started.await()
        val second =
            withTimeout(5_000) { controller.confirm(state, PlaylistSnapshot()) }
        assertEquals(0, repository.importCalls)
        assertNull(second.confirmedSnapshot)

        release.complete(Unit)
        val firstResult = first.await()
        assertEquals(1, repository.importCalls)
        assertNotNull(firstResult.confirmedSnapshot)
        Unit
    }

    @Test
    fun repeatedOpensRetainOnlyTheLatestPlan() = runBlocking {
        val repository = CountingRepository()
        val launcher = RecordingLauncher()
        val controller =
            createPlaylistBackupController(
                PlaylistStateOwner(repository, Dispatchers.Default),
                Dispatchers.Default,
                launcher,
                SucceedingGuard())
        val firstState = openedState(controller, launcher)
        val secondState = openedState(controller, launcher)

        assertEquals(1, controller.previewPlanCountForTest)

        val confirmation = controller.confirm(secondState, PlaylistSnapshot())
        assertNotNull(confirmation.confirmedSnapshot)
        assertEquals(1, repository.importCalls)
        assertEquals(0, controller.previewPlanCountForTest)
    }

    private suspend fun openedState(
        controller: PlaylistBackupController,
        launcher: RecordingLauncher,
    ): PlaylistBackupUiState {
        val saving =
            controller.beginExport(
                PlaylistBackupUiState(), PlaylistSnapshot(), emptyList(), 0)
        val opening =
            controller.beginOpen(
                controller.receiveSave(
                    saving, PlaylistBackupDocumentSaveResult.Success))
        return controller.receiveOpen(
            opening,
            PlaylistBackupDocumentOpenResult.Success(
                checkNotNull(launcher.bytes)),
            emptyList(),
            emptyList(),
            " imported",
            1,
        )
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

    private class SucceedingGuard : PlaylistBackupRevisionGuard {
        override suspend fun <T> withCurrentRevision(
            expectedRevision: Long,
            block: suspend () -> T,
        ): PlaylistBackupRevisionGuardResult<T> =
            PlaylistBackupRevisionGuardResult.Current(block())
    }

    private class SuspendingSucceedingGuard(
        private val started: CompletableDeferred<Unit>,
        private val release: CompletableDeferred<Unit>,
    ) : PlaylistBackupRevisionGuard {
        override suspend fun <T> withCurrentRevision(
            expectedRevision: Long,
            block: suspend () -> T,
        ): PlaylistBackupRevisionGuardResult<T> {
            started.complete(Unit)
            release.await()
            return PlaylistBackupRevisionGuardResult.Current(block())
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
