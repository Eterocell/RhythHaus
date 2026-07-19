package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

enum class PlaylistBackupOperation {
    Idle,
    Exporting,
    Saving,
    Opening,
    Planning,
    Importing,
}

enum class PlaylistBackupUiError {
    Unavailable,
    ReadFailed,
    WriteFailed,
    Oversized,
    Malformed,
    Checksum,
    UnsupportedVersion,
    StalePreview,
    ExportMissingTrack,
    ExportMissingDuration,
    ExportInvalidDuration,
    ExportInvalidData,
    RepositoryFailed,
}

data class PlaylistBackupPreview(val plan: PlaylistImportPlan) {
    val canConfirm: Boolean = plan.playlists.isNotEmpty()
}

data class PlaylistBackupImportResult(val totals: PlaylistImportTotals)

data class PlaylistBackupUiState(
    val operation: PlaylistBackupOperation = PlaylistBackupOperation.Idle,
    val preview: PlaylistBackupPreview? = null,
    val result: PlaylistBackupImportResult? = null,
    val error: PlaylistBackupUiError? = null,
) {
    val isBusy: Boolean get() = operation != PlaylistBackupOperation.Idle
}

sealed interface PlaylistBackupUiAction {
    data class OperationStarted(val operation: PlaylistBackupOperation) : PlaylistBackupUiAction
    data object PanelCancelled : PlaylistBackupUiAction
    data object OperationCancelled : PlaylistBackupUiAction
    data class PreviewReady(val plan: PlaylistImportPlan) : PlaylistBackupUiAction
    data object DismissPreview : PlaylistBackupUiAction
    data object DismissResult : PlaylistBackupUiAction
    data class ImportSucceeded(val totals: PlaylistImportTotals) : PlaylistBackupUiAction
    data class Failed(val error: PlaylistBackupUiError) : PlaylistBackupUiAction
    data object ClearError : PlaylistBackupUiAction
}

fun reducePlaylistBackupUiState(
    state: PlaylistBackupUiState,
    action: PlaylistBackupUiAction,
): PlaylistBackupUiState = when (action) {
    is PlaylistBackupUiAction.OperationStarted -> state.copy(operation = action.operation, error = null)
    PlaylistBackupUiAction.PanelCancelled -> state.copy(operation = PlaylistBackupOperation.Idle, error = null)
    PlaylistBackupUiAction.OperationCancelled -> state.copy(operation = PlaylistBackupOperation.Idle, error = null)
    is PlaylistBackupUiAction.PreviewReady -> state.copy(
        operation = PlaylistBackupOperation.Idle,
        preview = PlaylistBackupPreview(action.plan),
        result = null,
        error = null,
    )
    PlaylistBackupUiAction.DismissPreview -> state.copy(preview = null, error = null)
    PlaylistBackupUiAction.DismissResult -> state.copy(result = null, error = null)
    is PlaylistBackupUiAction.ImportSucceeded -> state.copy(
        operation = PlaylistBackupOperation.Idle,
        preview = null,
        result = PlaylistBackupImportResult(action.totals),
        error = null,
    )
    is PlaylistBackupUiAction.Failed -> state.copy(operation = PlaylistBackupOperation.Idle, error = action.error)
    PlaylistBackupUiAction.ClearError -> state.copy(error = null)
}

sealed interface PlaylistBackupExportPreparation {
    data class Ready(val bytes: ByteArray) : PlaylistBackupExportPreparation
    data class Failed(val error: PlaylistBackupUiError) : PlaylistBackupExportPreparation
}

suspend fun preparePlaylistBackupExport(
    snapshot: PlaylistSnapshot,
    authoritativeTracks: List<LibraryTrack>,
    exportedAtEpochMillis: Long,
    dispatcher: CoroutineDispatcher,
    validate: (ByteArray) -> PlaylistBackupDecodeResult = PlaylistBackupCodec::decode,
): PlaylistBackupExportPreparation = withContext(dispatcher) {
    when (val exported = exportPlaylistBackup(snapshot, authoritativeTracks, exportedAtEpochMillis)) {
        is PlaylistBackupExportResult.Failure -> PlaylistBackupExportPreparation.Failed(
            when (exported.error) {
                PlaylistBackupExportError.MISSING_TRACK -> PlaylistBackupUiError.ExportMissingTrack
                PlaylistBackupExportError.MISSING_DURATION -> PlaylistBackupUiError.ExportMissingDuration
                PlaylistBackupExportError.INVALID_DURATION -> PlaylistBackupUiError.ExportInvalidDuration
                PlaylistBackupExportError.CODEC_BOUNDS -> PlaylistBackupUiError.ExportInvalidData
            },
        )
        is PlaylistBackupExportResult.Success -> when (val decoded = validate(exported.bytes)) {
            is PlaylistBackupDecodeResult.Success -> PlaylistBackupExportPreparation.Ready(exported.bytes)
            is PlaylistBackupDecodeResult.Invalid -> PlaylistBackupExportPreparation.Failed(playlistBackupUiError(decoded.error))
        }
    }
}

sealed interface PlaylistBackupImportPreparation {
    data class Ready(val plan: PlaylistImportPlan) : PlaylistBackupImportPreparation
    data class Failed(val error: PlaylistBackupUiError) : PlaylistBackupImportPreparation
}

suspend fun preparePlaylistBackupImport(
    bytes: ByteArray,
    destinationTracks: List<LibraryTrack>,
    existingPlaylistNames: List<String>,
    importedSuffix: String,
    libraryRevision: Long,
    dispatcher: CoroutineDispatcher,
): PlaylistBackupImportPreparation = withContext(dispatcher) {
    when (val decoded = PlaylistBackupCodec.decode(bytes)) {
        is PlaylistBackupDecodeResult.Invalid -> PlaylistBackupImportPreparation.Failed(playlistBackupUiError(decoded.error))
        is PlaylistBackupDecodeResult.Success -> PlaylistBackupImportPreparation.Ready(
            planPlaylistImport(
                document = decoded.document,
                destinationTracks = destinationTracks,
                existingPlaylistNames = existingPlaylistNames,
                importedSuffix = importedSuffix,
                libraryRevision = libraryRevision,
            ),
        )
    }
}

fun playlistBackupUiError(error: PlaylistBackupValidationError): PlaylistBackupUiError = when (error) {
    PlaylistBackupValidationError.INPUT_TOO_LARGE -> PlaylistBackupUiError.Oversized
    PlaylistBackupValidationError.INVALID_CHECKSUM -> PlaylistBackupUiError.Checksum
    PlaylistBackupValidationError.UNSUPPORTED_FORMAT,
    PlaylistBackupValidationError.UNSUPPORTED_VERSION,
    -> PlaylistBackupUiError.UnsupportedVersion
    else -> PlaylistBackupUiError.Malformed
}

data class PlaylistBackupImportConfirmation(
    val state: PlaylistBackupUiState,
    val confirmedSnapshot: PlaylistSnapshot?,
    val playlistPublicationRevision: Long? = null,
)

suspend fun confirmPlaylistBackupImportSerialized(
    state: PlaylistBackupUiState,
    currentLibraryRevision: Long,
    lastConfirmedSnapshot: PlaylistSnapshot? = null,
    mutateAndRefresh: suspend (List<PlaylistImportMutation>) -> PlaylistImportOwnerResult,
): PlaylistBackupImportConfirmation {
    val preview = state.preview ?: return PlaylistBackupImportConfirmation(state, lastConfirmedSnapshot)
    if (preview.plan.libraryRevision != currentLibraryRevision) {
        return PlaylistBackupImportConfirmation(
            state.copy(operation = PlaylistBackupOperation.Idle, error = PlaylistBackupUiError.StalePreview),
            lastConfirmedSnapshot,
        )
    }
    if (!preview.canConfirm) return PlaylistBackupImportConfirmation(state, lastConfirmedSnapshot)
    val mutations = preview.plan.playlists.map { playlist ->
        PlaylistImportMutation(playlist.name, playlist.trackIds)
    }
    return try {
        when (val result = mutateAndRefresh(mutations)) {
            is PlaylistImportOwnerResult.Success -> PlaylistBackupImportConfirmation(
                reducePlaylistBackupUiState(state, PlaylistBackupUiAction.ImportSucceeded(preview.plan.totals)),
                result.snapshot,
                result.revision,
            )
            PlaylistImportOwnerResult.Stale -> PlaylistBackupImportConfirmation(
                state.copy(operation = PlaylistBackupOperation.Idle, error = PlaylistBackupUiError.StalePreview),
                lastConfirmedSnapshot,
            )
            is PlaylistImportOwnerResult.Failure -> PlaylistBackupImportConfirmation(
                state.copy(operation = PlaylistBackupOperation.Idle, error = PlaylistBackupUiError.RepositoryFailed),
                lastConfirmedSnapshot,
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    }
}

suspend fun confirmPlaylistBackupImport(
    state: PlaylistBackupUiState,
    currentLibraryRevision: Long,
    lastConfirmedSnapshot: PlaylistSnapshot? = null,
    mutate: suspend (List<PlaylistImportMutation>) -> Result<Unit>,
    refresh: suspend () -> PlaylistSnapshot,
): PlaylistBackupImportConfirmation {
    val preview = state.preview ?: return PlaylistBackupImportConfirmation(state, lastConfirmedSnapshot)
    if (preview.plan.libraryRevision != currentLibraryRevision) {
        return PlaylistBackupImportConfirmation(
            state.copy(operation = PlaylistBackupOperation.Idle, error = PlaylistBackupUiError.StalePreview),
            lastConfirmedSnapshot,
        )
    }
    if (!preview.canConfirm) return PlaylistBackupImportConfirmation(state, lastConfirmedSnapshot)
    val mutations = preview.plan.playlists.map { playlist ->
        PlaylistImportMutation(playlist.name, playlist.trackIds)
    }
    return try {
        val mutation = mutate(mutations)
        if (mutation.isFailure) {
            PlaylistBackupImportConfirmation(
                state.copy(operation = PlaylistBackupOperation.Idle, error = PlaylistBackupUiError.RepositoryFailed),
                lastConfirmedSnapshot,
            )
        } else {
            val snapshot = refresh()
            PlaylistBackupImportConfirmation(
                reducePlaylistBackupUiState(state, PlaylistBackupUiAction.ImportSucceeded(preview.plan.totals)),
                snapshot,
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    }
}
