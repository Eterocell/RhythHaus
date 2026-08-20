package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Current serialized backup operation; exactly one operation owns the
 * controller at a time.
 */
public enum class PlaylistBackupOperation {
    Idle,
    Exporting,
    Saving,
    Opening,
    Planning,
    Importing,
}

/**
 * User-visible backup failure category used to select the corresponding
 * localized error.
 */
public enum class PlaylistBackupUiError {
    Unavailable,
    ReadFailed,
    WriteFailed,
    Oversized,
    Malformed,
    InvalidData,
    Checksum,
    UnsupportedVersion,
    StalePreview,
    ExportMissingTrack,
    ExportMissingDuration,
    ExportInvalidDuration,
    ExportInvalidData,
    RepositoryFailed,
}

/**
 * Immutable source entry rendered in an import preview; value equality applies.
 */
public data class PlaylistBackupEntryView(
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int
)

/**
 * Classifies an import-preview entry as unmatched or as having ambiguous
 * candidates.
 */
public enum class PlaylistBackupIssueKind {
    Unmatched,
    Ambiguous
}

/**
 * Immutable source issue rendered in an import preview; value equality applies.
 */
public data class PlaylistBackupIssue(
    val playlistIndex: Int,
    val entryIndex: Int,
    val kind: PlaylistBackupIssueKind,
    val entry: PlaylistBackupEntryView
)

/** Immutable preview counts; value equality applies. */
public data class PlaylistBackupCounts(
    val restorable: Int,
    val unmatched: Int,
    val ambiguous: Int
)

/** Immutable per-playlist preview report; value equality applies. */
public data class PlaylistBackupPlaylistReport(
    val sourcePlaylistIndex: Int,
    val sourceName: String,
    val counts: PlaylistBackupCounts
)

/** Immutable import preview; value equality applies. */
public data class PlaylistBackupPreview(
    val libraryRevision: Long,
    val reports: List<PlaylistBackupPlaylistReport>,
    val issues: List<PlaylistBackupIssue>,
    val totals: PlaylistBackupCounts,
    val canConfirm: Boolean
)

/** Immutable completed import outcome; value equality applies. */
public data class PlaylistBackupImportResult(
    val playlistsToCreate: Int,
    val playlistsSkipped: Int,
    val entries: PlaylistBackupCounts
)

internal fun PlaylistImportPlan.toPreview(): PlaylistBackupPreview =
    PlaylistBackupPreview(
        libraryRevision = libraryRevision,
        reports =
            reports.map {
                PlaylistBackupPlaylistReport(
                    it.sourcePlaylistIndex,
                    it.sourceName,
                    PlaylistBackupCounts(
                        it.counts.restorable,
                        it.counts.unmatched,
                        it.counts.ambiguous))
            },
        issues =
            issues.map {
                PlaylistBackupIssue(
                    it.playlistIndex,
                    it.entryIndex,
                    when (it.kind) {
                        PlaylistImportIssueKind.UNMATCHED ->
                            PlaylistBackupIssueKind.Unmatched
                        PlaylistImportIssueKind.AMBIGUOUS ->
                            PlaylistBackupIssueKind.Ambiguous
                    },
                    PlaylistBackupEntryView(
                        it.entry.title,
                        it.entry.artist,
                        it.entry.album,
                        it.entry.durationSeconds))
            },
        totals =
            PlaylistBackupCounts(
                totals.entries.restorable,
                totals.entries.unmatched,
                totals.entries.ambiguous),
        canConfirm = playlists.isNotEmpty(),
    )

/** Immutable backup surface state; value equality applies. */
public data class PlaylistBackupUiState(
    val operation: PlaylistBackupOperation = PlaylistBackupOperation.Idle,
    val preview: PlaylistBackupPreview? = null,
    val result: PlaylistBackupImportResult? = null,
    val error: PlaylistBackupUiError? = null,
) {
    /** Whether a serialized document or import operation is active. */
    public val isBusy: Boolean
        get() = operation != PlaylistBackupOperation.Idle
}

/** Reducer action for the public backup surface. */
public sealed interface PlaylistBackupUiAction {
    /**
     * Starts [operation] and clears a prior error; value equality uses
     * [operation].
     */
    public data class OperationStarted(val operation: PlaylistBackupOperation) :
        PlaylistBackupUiAction

    /** Settles a platform panel cancellation. */
    public data object PanelCancelled : PlaylistBackupUiAction

    /** Settles a document operation without a result. */
    public data object OperationCancelled : PlaylistBackupUiAction

    /**
     * Publishes an import [preview]; value equality uses the immutable preview.
     */
    public data class PreviewReady(val preview: PlaylistBackupPreview) :
        PlaylistBackupUiAction

    /** Dismisses the active import preview. */
    public data object DismissPreview : PlaylistBackupUiAction

    /** Dismisses the completed import result. */
    public data object DismissResult : PlaylistBackupUiAction

    /**
     * Publishes the committed import [result]; value equality uses the
     * immutable result.
     */
    public data class ImportSucceeded(val result: PlaylistBackupImportResult) :
        PlaylistBackupUiAction

    /**
     * Settles the operation with [error]; value equality uses the failure
     * category.
     */
    public data class Failed(val error: PlaylistBackupUiError) :
        PlaylistBackupUiAction

    /** Clears the current recoverable error. */
    public data object ClearError : PlaylistBackupUiAction
}

internal fun reducePlaylistBackupUiState(
    state: PlaylistBackupUiState,
    action: PlaylistBackupUiAction,
): PlaylistBackupUiState =
    when (action) {
        is PlaylistBackupUiAction.OperationStarted ->
            state.copy(operation = action.operation, error = null)

        PlaylistBackupUiAction.PanelCancelled ->
            state.copy(operation = PlaylistBackupOperation.Idle, error = null)

        PlaylistBackupUiAction.OperationCancelled ->
            state.copy(operation = PlaylistBackupOperation.Idle, error = null)

        is PlaylistBackupUiAction.PreviewReady ->
            state.copy(
                operation = PlaylistBackupOperation.Idle,
                preview = action.preview,
                result = null,
                error = null,
            )

        PlaylistBackupUiAction.DismissPreview ->
            state.copy(preview = null, error = null)

        PlaylistBackupUiAction.DismissResult ->
            state.copy(result = null, error = null)

        is PlaylistBackupUiAction.ImportSucceeded ->
            state.copy(
                operation = PlaylistBackupOperation.Idle,
                preview = null,
                result = action.result,
                error = null,
            )

        is PlaylistBackupUiAction.Failed ->
            state.copy(
                operation = PlaylistBackupOperation.Idle, error = action.error)

        PlaylistBackupUiAction.ClearError -> state.copy(error = null)
    }

internal sealed interface PlaylistBackupExportPreparation {
    data class Ready(val bytes: ByteArray) : PlaylistBackupExportPreparation

    data class Failed(val error: PlaylistBackupUiError) :
        PlaylistBackupExportPreparation
}

internal suspend fun preparePlaylistBackupExport(
    snapshot: PlaylistSnapshot,
    authoritativeTracks: List<LibraryTrack>,
    exportedAtEpochMillis: Long,
    dispatcher: CoroutineDispatcher,
    validate: (ByteArray) -> PlaylistBackupDecodeResult =
        PlaylistBackupCodec::decode,
): PlaylistBackupExportPreparation =
    withContext(dispatcher) {
        when (val exported =
            exportPlaylistBackup(
                snapshot, authoritativeTracks, exportedAtEpochMillis)) {
            is PlaylistBackupExportResult.Failure ->
                PlaylistBackupExportPreparation.Failed(
                    when (exported.error) {
                        PlaylistBackupExportError.MISSING_TRACK ->
                            PlaylistBackupUiError.ExportMissingTrack
                        PlaylistBackupExportError.MISSING_DURATION ->
                            PlaylistBackupUiError.ExportMissingDuration
                        PlaylistBackupExportError.INVALID_DURATION ->
                            PlaylistBackupUiError.ExportInvalidDuration
                        PlaylistBackupExportError.CODEC_BOUNDS ->
                            PlaylistBackupUiError.ExportInvalidData
                    },
                )

            is PlaylistBackupExportResult.Success ->
                when (val decoded = validate(exported.bytes)) {
                    is PlaylistBackupDecodeResult.Success ->
                        PlaylistBackupExportPreparation.Ready(exported.bytes)
                    is PlaylistBackupDecodeResult.Invalid ->
                        PlaylistBackupExportPreparation.Failed(
                            playlistBackupUiError(decoded.error))
                }
        }
    }

internal sealed interface PlaylistBackupImportPreparation {
    data class Ready(val plan: PlaylistImportPlan) :
        PlaylistBackupImportPreparation

    data class Failed(val error: PlaylistBackupUiError) :
        PlaylistBackupImportPreparation
}

internal suspend fun preparePlaylistBackupImport(
    bytes: ByteArray,
    destinationTracks: List<LibraryTrack>,
    existingPlaylistNames: List<String>,
    importedSuffix: String,
    libraryRevision: Long,
    dispatcher: CoroutineDispatcher,
): PlaylistBackupImportPreparation =
    withContext(dispatcher) {
        when (val decoded = PlaylistBackupCodec.decode(bytes)) {
            is PlaylistBackupDecodeResult.Invalid ->
                PlaylistBackupImportPreparation.Failed(
                    playlistBackupUiError(decoded.error))

            is PlaylistBackupDecodeResult.Success ->
                PlaylistBackupImportPreparation.Ready(
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

internal fun playlistBackupUiError(
    error: PlaylistBackupValidationError
): PlaylistBackupUiError =
    when (error) {
        PlaylistBackupValidationError.INPUT_TOO_LARGE ->
            PlaylistBackupUiError.Oversized

        PlaylistBackupValidationError.INVALID_CHECKSUM ->
            PlaylistBackupUiError.Checksum

        PlaylistBackupValidationError.UNSUPPORTED_FORMAT,
        PlaylistBackupValidationError.UNSUPPORTED_VERSION,
        -> PlaylistBackupUiError.UnsupportedVersion

        PlaylistBackupValidationError.PLAYLIST_LIMIT_EXCEEDED,
        PlaylistBackupValidationError.PLAYLIST_ENTRY_LIMIT_EXCEEDED,
        PlaylistBackupValidationError.TOTAL_ENTRY_LIMIT_EXCEEDED,
        PlaylistBackupValidationError.STRING_LIMIT_EXCEEDED,
        PlaylistBackupValidationError.BLANK_PLAYLIST_NAME,
        PlaylistBackupValidationError.INVALID_DURATION,
        -> PlaylistBackupUiError.InvalidData

        else -> PlaylistBackupUiError.Malformed
    }

/**
 * Confirmation settlement and its authoritative publication snapshot.
 *
 * Value equality covers the feature state, optional confirmed snapshot, and
 * optional revision.
 */
public data class PlaylistBackupImportConfirmation(
    val state: PlaylistBackupUiState,
    val confirmedSnapshot: PlaylistSnapshot?,
    val playlistPublicationRevision: Long? = null,
)

/**
 * Atomically validates that an import preview still belongs to the
 * authoritative library revision.
 *
 * [withCurrentRevision] invokes its block only for the expected revision,
 * returns one terminal [PlaylistBackupRevisionGuardResult], and rethrows
 * cancellation unchanged.
 */
public interface PlaylistBackupRevisionGuard {
    /**
     * Runs [block] only while [expectedRevision] is authoritative, rethrowing
     * cancellation unchanged.
     */
    public suspend fun <T> withCurrentRevision(
        expectedRevision: Long,
        block: suspend () -> T,
    ): PlaylistBackupRevisionGuardResult<T>
}

/** Terminal result of one revision-guarded operation. */
public sealed interface PlaylistBackupRevisionGuardResult<out T> {
    /**
     * The block ran while its expected revision was current; [value] uses value
     * equality.
     */
    public data class Current<T>(val value: T) :
        PlaylistBackupRevisionGuardResult<T>

    /** The expected revision was rejected before the block could run. */
    public data object Stale : PlaylistBackupRevisionGuardResult<Nothing>
}

/**
 * Serializes document presentation and settles each terminal platform callback
 * once.
 *
 * All public transitions reduce feature state. It rejects stale results,
 * preserves the operation gate through panel presentation, performs imports
 * transactionally through [revisionGuard], and rethrows [CancellationException]
 * unchanged.
 */
public class PlaylistBackupController
internal constructor(
    private val owner: PlaylistStateOwner,
    private val dispatcher: CoroutineDispatcher,
    private val launcher: PlaylistBackupDocumentLauncher,
    private val revisionGuard: PlaylistBackupRevisionGuard,
) {
    private val gate = PlaylistBackupDocumentOperationGate()
    /**
     * The single retained import plan. Only the latest preview is reachable
     * through [PlaylistBackupUiState.preview], so retaining every plan would
     * leak the full track lists of superseded imports for the controller
     * lifetime. A new open replaces the slot and a successful confirm clears
     * it; a failed confirm keeps it so the caller can retry.
     */
    private var previewPlan: Pair<PlaylistBackupPreview, PlaylistImportPlan>? =
        null

    /** Test seam: number of retained import plans (0 or 1). */
    internal val previewPlanCountForTest: Int
        get() = if (previewPlan == null) 0 else 1

    /**
     * Single-flight claim for imports. Two confirmations can arrive from the
     * same UI frame (the app-level busy check is a TOCTOU snapshot); exactly
     * one may run the guarded import, the other returns the caller state
     * unchanged.
     */
    private val importInFlight = MutableStateFlow(false)

    /**
     * Prepares and validates export bytes before requesting save, rejecting
     * overlap and rethrowing cancellation.
     */
    public suspend fun beginExport(
        state: PlaylistBackupUiState,
        snapshot: PlaylistSnapshot,
        authoritativeTracks: List<LibraryTrack>,
        exportedAtEpochMillis: Long,
    ): PlaylistBackupUiState {
        if (!gate.tryStart()) return state
        val exporting =
            reduce(
                state,
                PlaylistBackupUiAction.OperationStarted(
                    PlaylistBackupOperation.Exporting))
        return try {
            when (val preparation =
                preparePlaylistBackupExport(
                    snapshot,
                    authoritativeTracks,
                    exportedAtEpochMillis,
                    dispatcher)) {
                is PlaylistBackupExportPreparation.Failed ->
                    settle(
                        exporting,
                        PlaylistBackupUiAction.Failed(preparation.error))
                is PlaylistBackupExportPreparation.Ready -> {
                    if (!launcher.isAvailable)
                        return settle(
                            exporting,
                            PlaylistBackupUiAction.Failed(
                                PlaylistBackupUiError.Unavailable))
                    val saving =
                        reduce(
                            exporting,
                            PlaylistBackupUiAction.OperationStarted(
                                PlaylistBackupOperation.Saving))
                    launcher.save("rhythhaus-playlists", preparation.bytes)
                    saving
                }
            }
        } catch (cancelled: CancellationException) {
            gate.finish()
            throw cancelled
        } catch (_: Throwable) {
            settle(
                exporting,
                PlaylistBackupUiAction.Failed(
                    PlaylistBackupUiError.WriteFailed))
        }
    }

    /**
     * Requests open when no other document operation is active, otherwise
     * retaining [state].
     */
    public fun beginOpen(state: PlaylistBackupUiState): PlaylistBackupUiState {
        if (!gate.tryStart()) return state
        val opening =
            reduce(
                state,
                PlaylistBackupUiAction.OperationStarted(
                    PlaylistBackupOperation.Opening))
        return try {
            if (!launcher.isAvailable)
                settle(
                    opening,
                    PlaylistBackupUiAction.Failed(
                        PlaylistBackupUiError.Unavailable))
            else {
                launcher.open()
                opening
            }
        } catch (_: Throwable) {
            settle(
                opening,
                PlaylistBackupUiAction.Failed(PlaylistBackupUiError.ReadFailed))
        }
    }

    /**
     * Settles one save result, ignoring duplicate or stale terminal delivery.
     */
    public fun receiveSave(
        state: PlaylistBackupUiState,
        result: PlaylistBackupDocumentSaveResult
    ): PlaylistBackupUiState {
        if (!gate.isActive || state.operation != PlaylistBackupOperation.Saving)
            return state
        return settle(
            state,
            when (result) {
                PlaylistBackupDocumentSaveResult.Success ->
                    PlaylistBackupUiAction.OperationCancelled
                PlaylistBackupDocumentSaveResult.Cancelled ->
                    PlaylistBackupUiAction.PanelCancelled
                is PlaylistBackupDocumentSaveResult.Unavailable ->
                    PlaylistBackupUiAction.Failed(
                        PlaylistBackupUiError.Unavailable)
                is PlaylistBackupDocumentSaveResult.Failure ->
                    PlaylistBackupUiAction.Failed(
                        PlaylistBackupUiError.WriteFailed)
            })
    }

    /**
     * Uses every open input to decode and plan a selected document before
     * exposing a confirmation preview.
     */
    public suspend fun receiveOpen(
        state: PlaylistBackupUiState,
        result: PlaylistBackupDocumentOpenResult,
        destinationTracks: List<LibraryTrack>,
        existingPlaylistNames: List<String>,
        importedSuffix: String,
        libraryRevision: Long
    ): PlaylistBackupUiState {
        if (!gate.isActive ||
            state.operation != PlaylistBackupOperation.Opening)
            return state
        return try {
            when (result) {
                PlaylistBackupDocumentOpenResult.Cancelled ->
                    settle(state, PlaylistBackupUiAction.PanelCancelled)
                is PlaylistBackupDocumentOpenResult.Unavailable ->
                    settle(
                        state,
                        PlaylistBackupUiAction.Failed(
                            PlaylistBackupUiError.Unavailable))
                is PlaylistBackupDocumentOpenResult.TooLarge ->
                    settle(
                        state,
                        PlaylistBackupUiAction.Failed(
                            PlaylistBackupUiError.Oversized))
                is PlaylistBackupDocumentOpenResult.Failure ->
                    settle(
                        state,
                        PlaylistBackupUiAction.Failed(
                            PlaylistBackupUiError.ReadFailed))
                is PlaylistBackupDocumentOpenResult.Success -> {
                    val planning =
                        reduce(
                            state,
                            PlaylistBackupUiAction.OperationStarted(
                                PlaylistBackupOperation.Planning))
                    when (val preparation =
                        preparePlaylistBackupImport(
                            result.bytes,
                            destinationTracks,
                            existingPlaylistNames,
                            importedSuffix,
                            libraryRevision,
                            dispatcher)) {
                        is PlaylistBackupImportPreparation.Failed ->
                            settle(
                                planning,
                                PlaylistBackupUiAction.Failed(
                                    preparation.error))
                        is PlaylistBackupImportPreparation.Ready -> {
                            val preview = preparation.plan.toPreview()
                            previewPlan = preview to preparation.plan
                            settle(
                                planning,
                                PlaylistBackupUiAction.PreviewReady(preview))
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            gate.finish()
            throw cancelled
        }
    }

    /**
     * Imports transactionally only through the revision guard and rethrows
     * cancellation unchanged.
     */
    public suspend fun confirm(
        state: PlaylistBackupUiState,
        lastConfirmedSnapshot: PlaylistSnapshot
    ): PlaylistBackupImportConfirmation {
        val preview =
            state.preview
                ?: return PlaylistBackupImportConfirmation(
                    state, lastConfirmedSnapshot)
        if (!importInFlight.compareAndSet(false, true)) {
            // A concurrent confirm is already importing; do not double-import
            // and do not let a stale busy snapshot replace its success state.
            // The returned confirmation carries no confirmed snapshot: only a
            // committed import may report one.
            return PlaylistBackupImportConfirmation(state, null)
        }
        return try {
            val plan =
                previewPlan?.takeIf { it.first === preview }?.second
                    ?: return PlaylistBackupImportConfirmation(
                        state.copy(
                            operation = PlaylistBackupOperation.Idle,
                            error = PlaylistBackupUiError.RepositoryFailed),
                        lastConfirmedSnapshot,
                    )
            try {
                when (val guarded =
                    revisionGuard.withCurrentRevision(preview.libraryRevision) {
                        owner.importPlaylists(
                            plan.playlists.map {
                                PlaylistImportMutation(it.name, it.trackIds)
                            })
                    }) {
                    PlaylistBackupRevisionGuardResult.Stale ->
                        PlaylistBackupImportConfirmation(
                            state.copy(
                                operation = PlaylistBackupOperation.Idle,
                                error = PlaylistBackupUiError.StalePreview),
                            lastConfirmedSnapshot)
                    is PlaylistBackupRevisionGuardResult.Current ->
                        when (val imported = guarded.value) {
                            is PlaylistImportOwnerResult.Success -> {
                                previewPlan = null
                                PlaylistBackupImportConfirmation(
                                    reduce(
                                        state,
                                        PlaylistBackupUiAction.ImportSucceeded(
                                            plan.importResult())),
                                    imported.snapshot,
                                    imported.revision)
                            }

                            else ->
                                PlaylistBackupImportConfirmation(
                                    state.copy(
                                        operation =
                                            PlaylistBackupOperation.Idle,
                                        error =
                                            PlaylistBackupUiError
                                                .RepositoryFailed),
                                    lastConfirmedSnapshot)
                        }
                }
            } finally {
                importInFlight.value = false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    /** Applies a public controller transition through the feature reducer. */
    public fun reduce(
        state: PlaylistBackupUiState,
        action: PlaylistBackupUiAction
    ): PlaylistBackupUiState = reducePlaylistBackupUiState(state, action)

    private fun settle(
        state: PlaylistBackupUiState,
        action: PlaylistBackupUiAction
    ): PlaylistBackupUiState {
        gate.finish()
        return reduce(state, action)
    }
}

internal fun PlaylistImportPlan.importResult(): PlaylistBackupImportResult =
    PlaylistBackupImportResult(
        playlistsToCreate = totals.playlistsToCreate,
        playlistsSkipped = totals.playlistsSkipped,
        entries =
            PlaylistBackupCounts(
                totals.entries.restorable,
                totals.entries.unmatched,
                totals.entries.ambiguous),
    )
