package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlaylistBackupUiStateTest {
    @Test
    fun reducerKeepsPreviewInspectableAndDisablesConfirmationWhenNothingIsRestorable() {
        val plan = plan(restorable = 0, unmatched = 2, ambiguous = 1)

        val state = reducePlaylistBackupUiState(
            PlaylistBackupUiState(),
            PlaylistBackupUiAction.PreviewReady(plan),
        )

        assertSame(plan, state.preview?.plan)
        assertFalse(state.preview!!.canConfirm)
        assertFalse(state.isBusy)
    }

    @Test
    fun previewDismissalAndPanelCancellationAreSilentAndNeverCreateResultState() {
        val preview = reducePlaylistBackupUiState(
            PlaylistBackupUiState(),
            PlaylistBackupUiAction.PreviewReady(plan(restorable = 1)),
        )

        val dismissed = reducePlaylistBackupUiState(preview, PlaylistBackupUiAction.DismissPreview)
        val cancelled = reducePlaylistBackupUiState(
            PlaylistBackupUiState(operation = PlaylistBackupOperation.Opening),
            PlaylistBackupUiAction.PanelCancelled,
        )

        assertNull(dismissed.preview)
        assertNull(dismissed.result)
        assertNull(dismissed.error)
        assertEquals(PlaylistBackupOperation.Idle, cancelled.operation)
        assertNull(cancelled.error)
    }

    @Test
    fun distinctRecoverableFailuresRemainDistinct() {
        val failures = listOf(
            PlaylistBackupUiError.Unavailable,
            PlaylistBackupUiError.ReadFailed,
            PlaylistBackupUiError.WriteFailed,
            PlaylistBackupUiError.Oversized,
            PlaylistBackupUiError.Malformed,
            PlaylistBackupUiError.InvalidData,
            PlaylistBackupUiError.Checksum,
            PlaylistBackupUiError.UnsupportedVersion,
            PlaylistBackupUiError.StalePreview,
            PlaylistBackupUiError.ExportMissingDuration,
            PlaylistBackupUiError.RepositoryFailed,
        )

        assertEquals(
            failures,
            failures.map { failure ->
                reducePlaylistBackupUiState(
                    PlaylistBackupUiState(operation = PlaylistBackupOperation.Planning),
                    PlaylistBackupUiAction.Failed(failure),
                ).error
            },
        )
    }

    @Test
    fun exportBuildsAndFullyDecodesBytesBeforeRequestingSave() = runBlocking {
        val events = mutableListOf<String>()
        val result = preparePlaylistBackupExport(
            snapshot = snapshot(),
            authoritativeTracks = listOf(track()),
            exportedAtEpochMillis = 42,
            dispatcher = Dispatchers.Unconfined,
            validate = { bytes ->
                events += "validate"
                PlaylistBackupCodec.decode(bytes)
            },
        )

        val ready = assertIs<PlaylistBackupExportPreparation.Ready>(result)
        assertEquals(listOf("validate"), events)
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(ready.bytes))
        Unit
    }

    @Test
    fun exportValidationFailureDoesNotProduceSaveableBytes() = runBlocking {
        val result = preparePlaylistBackupExport(
            snapshot = snapshot(),
            authoritativeTracks = listOf(track()),
            exportedAtEpochMillis = 42,
            dispatcher = Dispatchers.Unconfined,
            validate = { PlaylistBackupDecodeResult.Invalid(PlaylistBackupValidationError.INVALID_CHECKSUM) },
        )

        assertEquals(
            PlaylistBackupExportPreparation.Failed(PlaylistBackupUiError.Checksum),
            result,
        )
    }

    @Test
    fun exportMissingDurationMapsWithoutOpeningSave() = runBlocking {
        val result = preparePlaylistBackupExport(
            snapshot = snapshot(),
            authoritativeTracks = listOf(track(durationMillis = null)),
            exportedAtEpochMillis = 42,
            dispatcher = Dispatchers.Unconfined,
        )

        assertEquals(
            PlaylistBackupExportPreparation.Failed(PlaylistBackupUiError.ExportMissingDuration),
            result,
        )
    }

    @Test
    fun importDecodesAndPlansAgainstCurrentTracksWithoutMutation() = runBlocking {
        var mutations = 0
        val bytes = exportBytes()

        val result = preparePlaylistBackupImport(
            bytes = bytes,
            destinationTracks = listOf(track()),
            existingPlaylistNames = emptyList(),
            importedSuffix = "Imported",
            libraryRevision = 9,
            dispatcher = Dispatchers.Unconfined,
        )
        mutations += 0

        val ready = assertIs<PlaylistBackupImportPreparation.Ready>(result)
        assertEquals(9, ready.plan.libraryRevision)
        assertEquals(1, ready.plan.totals.entries.restorable)
        assertEquals(0, mutations)
    }

    @Test
    fun everyValidationErrorMapsToItsExactRecoveryClass() {
        val expected = mapOf(
            PlaylistBackupValidationError.INPUT_TOO_LARGE to PlaylistBackupUiError.Oversized,
            PlaylistBackupValidationError.MALFORMED_UTF8 to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.MALFORMED_JSON to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.DUPLICATE_FIELD to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.UNKNOWN_FIELD to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.MISSING_FIELD to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.UNSUPPORTED_FORMAT to PlaylistBackupUiError.UnsupportedVersion,
            PlaylistBackupValidationError.UNSUPPORTED_VERSION to PlaylistBackupUiError.UnsupportedVersion,
            PlaylistBackupValidationError.INVALID_CHECKSUM to PlaylistBackupUiError.Checksum,
            PlaylistBackupValidationError.INVALID_INTEGER to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.NUMERIC_OVERFLOW to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.TRAILING_CONTENT to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.NON_CANONICAL_JSON to PlaylistBackupUiError.Malformed,
            PlaylistBackupValidationError.PLAYLIST_LIMIT_EXCEEDED to PlaylistBackupUiError.InvalidData,
            PlaylistBackupValidationError.PLAYLIST_ENTRY_LIMIT_EXCEEDED to PlaylistBackupUiError.InvalidData,
            PlaylistBackupValidationError.TOTAL_ENTRY_LIMIT_EXCEEDED to PlaylistBackupUiError.InvalidData,
            PlaylistBackupValidationError.STRING_LIMIT_EXCEEDED to PlaylistBackupUiError.InvalidData,
            PlaylistBackupValidationError.BLANK_PLAYLIST_NAME to PlaylistBackupUiError.InvalidData,
            PlaylistBackupValidationError.INVALID_DURATION to PlaylistBackupUiError.InvalidData,
        )

        assertEquals(PlaylistBackupValidationError.entries.toSet(), expected.keys)
        PlaylistBackupValidationError.entries.forEach { error ->
            assertEquals(expected.getValue(error), playlistBackupUiError(error), error.name)
        }
    }

    @Test
    fun staleConfirmationRejectsBeforeRepositoryCallAndRetainsPreview() = runBlocking {
        var repositoryCalls = 0
        val state = PlaylistBackupUiState(
            preview = PlaylistBackupPreview(plan(restorable = 1, revision = 4)),
        )

        val outcome = confirmPlaylistBackupImportSerialized(
            state = state,
            currentLibraryRevision = 5,
            mutateAndRefresh = {
                repositoryCalls++
                error("must not mutate")
            },
        )

        assertEquals(0, repositoryCalls)
        assertEquals(PlaylistBackupUiError.StalePreview, outcome.state.error)
        assertSame(state.preview, outcome.state.preview)
    }

    @Test
    fun confirmationConvertsAllEligibleRowsToOneOrderedMutationAndRefreshesOnce() = runBlocking {
        val plan = plan(
            restorable = 3,
            playlists = listOf(
                PlaylistImportPlaylist(0, "First", listOf("a", "b", "a")),
                PlaylistImportPlaylist(1, "Second", listOf("c")),
            ),
        )
        val confirmedSnapshot = snapshot()
        var ownerCalls = 0
        var received = emptyList<PlaylistImportMutation>()

        val outcome = confirmPlaylistBackupImportSerialized(
            state = PlaylistBackupUiState(preview = PlaylistBackupPreview(plan)),
            currentLibraryRevision = plan.libraryRevision,
            mutateAndRefresh = { mutations ->
                ownerCalls++
                received = mutations
                PlaylistImportOwnerResult.Success(confirmedSnapshot, revision = 12)
            },
        )

        assertEquals(1, ownerCalls)
        assertEquals(
            listOf(
                PlaylistImportMutation("First", listOf("a", "b", "a")),
                PlaylistImportMutation("Second", listOf("c")),
            ),
            received,
        )
        assertNull(outcome.state.preview)
        assertSame(confirmedSnapshot, outcome.confirmedSnapshot)
        assertEquals(12, outcome.playlistPublicationRevision)
        assertEquals(2, outcome.state.result?.totals?.playlistsToCreate)
        assertEquals(3, outcome.state.result?.totals?.entries?.restorable)
    }

    @Test
    fun serializedConfirmationUsesOneOwnerCallThatReturnsTheRefreshedSnapshot() = runBlocking {
        val preview = PlaylistBackupPreview(plan(restorable = 2))
        val refreshed = snapshot()
        var ownerCalls = 0

        val outcome = confirmPlaylistBackupImportSerialized(
            state = PlaylistBackupUiState(preview = preview),
            currentLibraryRevision = preview.plan.libraryRevision,
            mutateAndRefresh = { mutations ->
                ownerCalls++
                assertEquals(1, mutations.size)
                PlaylistImportOwnerResult.Success(refreshed, revision = 11)
            },
        )

        assertEquals(1, ownerCalls)
        assertSame(refreshed, outcome.confirmedSnapshot)
        assertEquals(11, outcome.playlistPublicationRevision)
        assertNull(outcome.state.preview)
        assertEquals(2, outcome.state.result?.totals?.entries?.restorable)
    }

    @Test
    fun serializedStaleAndFailureOutcomesRetainPreviewAndConfirmedSnapshot() = runBlocking {
        val preview = PlaylistBackupPreview(plan(restorable = 1))
        val confirmed = snapshot()

        val stale = confirmPlaylistBackupImportSerialized(
            state = PlaylistBackupUiState(preview = preview),
            currentLibraryRevision = preview.plan.libraryRevision,
            lastConfirmedSnapshot = confirmed,
            mutateAndRefresh = { PlaylistImportOwnerResult.Stale },
        )
        val failed = confirmPlaylistBackupImportSerialized(
            state = PlaylistBackupUiState(preview = preview),
            currentLibraryRevision = preview.plan.libraryRevision,
            lastConfirmedSnapshot = confirmed,
            mutateAndRefresh = { PlaylistImportOwnerResult.Failure(IllegalStateException("failed")) },
        )

        assertSame(preview, stale.state.preview)
        assertSame(confirmed, stale.confirmedSnapshot)
        assertEquals(PlaylistBackupUiError.StalePreview, stale.state.error)
        assertSame(preview, failed.state.preview)
        assertSame(confirmed, failed.confirmedSnapshot)
        assertEquals(PlaylistBackupUiError.RepositoryFailed, failed.state.error)
    }

    @Test
    fun repositoryFailureRetainsPreviewAndLastConfirmedSnapshotWithoutRefresh() = runBlocking {
        val preview = PlaylistBackupPreview(plan(restorable = 1))
        val confirmedSnapshot = snapshot()
        var ownerCalls = 0

        val outcome = confirmPlaylistBackupImportSerialized(
            state = PlaylistBackupUiState(preview = preview),
            currentLibraryRevision = preview.plan.libraryRevision,
            lastConfirmedSnapshot = confirmedSnapshot,
            mutateAndRefresh = {
                ownerCalls++
                PlaylistImportOwnerResult.Failure(IllegalStateException("write failed"))
            },
        )

        assertSame(preview, outcome.state.preview)
        assertSame(confirmedSnapshot, outcome.confirmedSnapshot)
        assertEquals(PlaylistBackupUiError.RepositoryFailed, outcome.state.error)
        assertEquals(1, ownerCalls)
        assertNull(outcome.playlistPublicationRevision)
    }

    @Test
    fun confirmationRethrowsCancellation() {
        assertFailsWith<CancellationException> {
            runBlocking {
                confirmPlaylistBackupImportSerialized(
                    state = PlaylistBackupUiState(preview = PlaylistBackupPreview(plan(restorable = 1))),
                    currentLibraryRevision = 7,
                    mutateAndRefresh = { throw CancellationException("gone") },
                )
            }
        }
    }

    @Test
    fun cancellationCleanupReturnsIdleAndRetainsPreviewWithoutError() {
        val preview = PlaylistBackupPreview(plan(restorable = 1))
        val busy = PlaylistBackupUiState(
            operation = PlaylistBackupOperation.Importing,
            preview = preview,
        )

        val cleaned = reducePlaylistBackupUiState(busy, PlaylistBackupUiAction.OperationCancelled)

        assertEquals(PlaylistBackupOperation.Idle, cleaned.operation)
        assertSame(preview, cleaned.preview)
        assertNull(cleaned.error)
    }

    private fun exportBytes(): ByteArray = assertIs<PlaylistBackupExportResult.Success>(
        exportPlaylistBackup(snapshot(), listOf(track()), 42),
    ).bytes

    private fun plan(
        restorable: Int,
        unmatched: Int = 0,
        ambiguous: Int = 0,
        revision: Long = 7,
        playlists: List<PlaylistImportPlaylist> = if (restorable == 0) emptyList() else listOf(
            PlaylistImportPlaylist(0, "Mix", List(restorable) { "track-$it" }),
        ),
    ) = PlaylistImportPlan(
        libraryRevision = revision,
        playlists = playlists,
        reports = emptyList(),
        totals = PlaylistImportTotals(
            playlistsToCreate = playlists.size,
            playlistsSkipped = if (playlists.isEmpty()) 1 else 0,
            entries = PlaylistImportCounts(restorable, unmatched, ambiguous),
        ),
        issues = emptyList(),
    )

    private fun snapshot() = PlaylistSnapshot(
        playlists = listOf(Playlist("playlist", "Mix", 1, 1)),
        entriesByPlaylistId = mapOf(
            "playlist" to listOf(PlaylistEntry("entry", "playlist", "track", 0, 1)),
        ),
    )

    private fun track(durationMillis: Long? = 100_000) = LibraryTrack(
        id = "track",
        sourceId = "source",
        sourceLocalKey = "track.mp3",
        audioSource = AudioSource.FilePath("/track.mp3"),
        displayName = "track.mp3",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMillis = durationMillis,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
