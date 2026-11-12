package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupController
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuard
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuardResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.PlaylistDocumentFormat
import com.eterocell.rhythhaus.playlistbackup.createPlaylistBackupController
import com.eterocell.rhythhaus.playlistbackup.rememberPlatformPlaylistBackupDocumentLauncher
import com.eterocell.rhythhaus.playlistbackup.rememberPlaylistTrackReadability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.playlist_backup_imported_suffix
import rhythhaus.shared.generated.resources.playlists

internal data class AppPlaylistBackupPresentation(
    val state: PlaylistBackupUiState,
    val documentAvailable: Boolean,
    val exportPlaylists: () -> Unit,
    val openBackup: () -> Unit,
    val exportDocument: (String?, PlaylistDocumentFormat) -> Unit,
    val openDocument: (PlaylistDocumentFormat) -> Unit,
    val confirm: () -> Unit,
    val dispatch: (PlaylistBackupUiAction) -> Unit,
)

/**
 * Composition-local document state; the feature controller remains
 * authoritative.
 */
@Composable
internal fun rememberAppPlaylistBackupHost(
    playlistStateOwner: PlaylistStateOwner,
    libraryPublicationOwner: AuthoritativeLibraryPublicationOwner,
    content: LibraryContentState,
    playlists: PlaylistState,
    revision: Long,
    scope: kotlinx.coroutines.CoroutineScope,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
): AppPlaylistBackupPresentation {
    val libraryContent by rememberUpdatedState(content)
    val playlistState by rememberUpdatedState(playlists)
    val libraryRevision by rememberUpdatedState(revision)
    val publishPlaylistAction by rememberUpdatedState(onPlaylistStateAction)
    var playlistBackupState by remember {
        mutableStateOf(PlaylistBackupUiState())
    }
    val importedSuffix =
        stringResource(Res.string.playlist_backup_imported_suffix)
    val backupControllerHolder = remember {
        arrayOfNulls<PlaylistBackupController>(1)
    }
    val backupDocumentLauncher =
        rememberPlatformPlaylistBackupDocumentLauncher(
            onSaveResult = { result ->
                scope.launch {
                    kotlinx.coroutines.yield()
                    backupControllerHolder[0]?.let { controller ->
                        playlistBackupState =
                            controller.receiveSave(playlistBackupState, result)
                    }
                }
            },
            onOpenResult = { result ->
                scope.launch {
                    kotlinx.coroutines.yield()
                    backupControllerHolder[0]?.let { controller ->
                        playlistBackupState =
                            runPlaylistBackupOperation(
                                currentState = { playlistBackupState },
                                publishState = { state ->
                                    playlistBackupState = state
                                },
                                reduce = controller::reduce,
                            ) {
                                controller.receiveOpen(
                                    state = playlistBackupState,
                                    result = result,
                                    destinationTracks = libraryContent.tracks,
                                    existingPlaylistNames =
                                        playlistState.confirmedSnapshot
                                            .playlists
                                            .map { it.name },
                                    importedSuffix = importedSuffix,
                                    libraryRevision = libraryRevision,
                                )
                            }
                    }
                }
            },
        )
    val backupController =
        remember(
            playlistStateOwner,
            backupDocumentLauncher,
            libraryPublicationOwner) {
                createPlaylistBackupController(
                    owner = playlistStateOwner,
                    dispatcher = Dispatchers.Default,
                    launcher = backupDocumentLauncher,
                    revisionGuard =
                        authoritativePlaylistBackupRevisionGuard(
                            libraryPublicationOwner),
                )
            }
    backupControllerHolder[0] = backupController

    val canReadPlaylistTrack = rememberPlaylistTrackReadability()

    fun exportPlaylistDocument(
        playlistId: String?,
        format: PlaylistDocumentFormat,
    ) {
        if (playlistBackupState.isBusy) return
        scope.launch {
            playlistBackupState =
                runPlaylistBackupOperation(
                    currentState = { playlistBackupState },
                    publishState = { state -> playlistBackupState = state },
                    reduce = backupController::reduce,
                    operation = PlaylistBackupOperation.Exporting,
                ) {
                    backupController.beginExport(
                        state = playlistBackupState,
                        snapshot = playlistState.confirmedSnapshot,
                        authoritativeTracks = libraryContent.tracks,
                        exportedAtEpochMillis =
                            com.eterocell.rhythhaus.library.currentTimeMillis(),
                        format = format,
                        playlistId = playlistId,
                        canReadTrack = canReadPlaylistTrack,
                    )
                }
        }
    }

    fun exportPlaylists() {
        exportPlaylistDocument(null, PlaylistDocumentFormat.RhythHausJson)
    }

    fun openPlaylistDocument(format: PlaylistDocumentFormat) {
        if (playlistBackupState.isBusy) return
        playlistBackupState =
            backupController.beginOpen(playlistBackupState, format)
    }

    fun openPlaylistBackup() {
        openPlaylistDocument(PlaylistDocumentFormat.RhythHausJson)
    }

    fun confirmPlaylistBackup() {
        if (playlistBackupState.isBusy) return
        playlistBackupState.preview ?: return
        playlistBackupState =
            backupController.reduce(
                playlistBackupState,
                PlaylistBackupUiAction.OperationStarted(
                    PlaylistBackupOperation.Importing),
            )
        scope.launch {
            runPlaylistBackupOperation(
                currentState = { playlistBackupState },
                publishState = { state -> playlistBackupState = state },
                reduce = backupController::reduce,
            ) {
                val confirmation =
                    backupController.confirm(
                        state = playlistBackupState,
                        lastConfirmedSnapshot = playlistState.confirmedSnapshot,
                    )
                // A confirm that lost the single-flight claim returns the
                // caller's busy state unchanged; publishing it would clobber
                // the winning import's terminal state. Terminal confirmations
                // always settle to Idle.
                if (confirmation.state.operation ==
                    PlaylistBackupOperation.Idle) {
                    playlistBackupState = confirmation.state
                    confirmation.confirmedSnapshot
                        ?.takeIf { confirmation.state.result != null }
                        ?.let { snapshot ->
                            publishPlaylistAction(
                                PlaylistStateAction.SnapshotConfirmed(
                                    snapshot,
                                    requireNotNull(
                                        confirmation
                                            .playlistPublicationRevision),
                                ),
                            )
                        }
                }
            }
        }
    }

    return AppPlaylistBackupPresentation(
        state = playlistBackupState,
        documentAvailable = backupDocumentLauncher.isAvailable,
        exportPlaylists = ::exportPlaylists,
        openBackup = ::openPlaylistBackup,
        exportDocument = ::exportPlaylistDocument,
        openDocument = ::openPlaylistDocument,
        confirm = ::confirmPlaylistBackup,
        dispatch = { action ->
            playlistBackupState =
                backupController.reduce(playlistBackupState, action)
        },
    )
}

internal suspend fun <T> runPlaylistBackupOperation(
    currentState: () -> PlaylistBackupUiState,
    publishState: (PlaylistBackupUiState) -> Unit,
    reduce:
        (
            PlaylistBackupUiState,
            PlaylistBackupUiAction) -> PlaylistBackupUiState,
    operation: PlaylistBackupOperation? = null,
    block: suspend () -> T,
): T =
    try {
        if (operation != null) {
            publishState(
                reduce(
                    currentState(),
                    PlaylistBackupUiAction.OperationStarted(operation)),
            )
        }
        block()
    } catch (cancelled: CancellationException) {
        publishState(
            reduce(
                currentState(),
                PlaylistBackupUiAction.OperationCancelled,
            ),
        )
        throw cancelled
    }

internal fun authoritativePlaylistBackupRevisionGuard(
    owner: AuthoritativeLibraryPublicationOwner,
): PlaylistBackupRevisionGuard =
    object : PlaylistBackupRevisionGuard {
        override suspend fun <T> withCurrentRevision(
            expectedRevision: Long,
            block: suspend () -> T,
        ): PlaylistBackupRevisionGuardResult<T> =
            when (val result =
                owner.withCurrentRevision(expectedRevision, block)) {
                is AuthoritativeRevisionResult.Current ->
                    PlaylistBackupRevisionGuardResult.Current(result.value)

                AuthoritativeRevisionResult.Stale ->
                    PlaylistBackupRevisionGuardResult.Stale
            }
    }
