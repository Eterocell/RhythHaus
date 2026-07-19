package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.PlaylistReadFailedMessage
import com.eterocell.rhythhaus.library.ui.PlaylistMutationFailedMessage
import com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.mutatePlaylistAndRefresh
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentOpenResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentSaveResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupExportPreparation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupImportPreparation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiError
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.confirmPlaylistBackupImportSerialized
import com.eterocell.rhythhaus.playlistbackup.playlistBackupFileName
import com.eterocell.rhythhaus.playlistbackup.preparePlaylistBackupExport
import com.eterocell.rhythhaus.playlistbackup.preparePlaylistBackupImport
import com.eterocell.rhythhaus.playlistbackup.reducePlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.rememberPlatformPlaylistBackupDocumentLauncher
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.theme.resolveHausPalette
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.scan_complete_format
import rhythhaus.shared.generated.resources.playlist_backup_imported_suffix
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
@Preview
fun App() {
    val controller = koinInject<PlaybackController>()
    val tagLibReader = koinInject<TagLibReader>()
    val repository = koinInject<LibraryRepository>()
    val playlistRepository = koinInject<PlaylistRepository>()
    val platformAccess = koinInject<PlatformSourceAccess>()
    val scanner = koinInject<LibraryScanner>()
    val themePreferenceStore = koinInject<ThemePreferenceStore>()
    val playbackLifecycle = koinInject<PlaybackProcessLifecycle>()
    val playbackReconciler = koinInject<PlaybackSessionReconciler>()
    val initialLibraryContent = remember { loadLibraryContent(repository, platformAccess) }
    val playlistStateOwner = remember { PlaylistStateOwner(playlistRepository, Dispatchers.Default) }
    val libraryPublicationOwner = remember { AuthoritativeLibraryPublicationOwner() }
    var initialPublication by remember { mutableStateOf(InitialLibraryPublicationState()) }
    var librarySources by remember { mutableStateOf(emptyList<LibrarySource>()) }
    var libraryTracks by remember { mutableStateOf(emptyList<LibraryTrack>()) }
    var libraryRevision by remember { mutableStateOf(0L) }
    var playlistState by remember { mutableStateOf(PlaylistState(isLoading = true)) }
    var playlistBackupState by remember { mutableStateOf(PlaylistBackupUiState()) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scanCancellationRequested = remember { MutableStateFlow(false) }
    val scope = rememberCoroutineScope()
    val scanCompleteFormat = stringResource(Res.string.scan_complete_format)
    val importedSuffix = stringResource(Res.string.playlist_backup_imported_suffix)
    val selectedThemeMode by themePreferenceStore.selectedThemeMode.collectAsState(RhythHausThemeMode.System)

    suspend fun updateLibraryContent(content: LibraryContentState) {
        val publication = libraryPublicationOwner.publish(content)
        withContext(Dispatchers.Main) {
            librarySources = publication.content.sources
            libraryTracks = publication.content.tracks
            libraryRevision = publication.revision
        }
    }

    LaunchedEffect(initialLibraryContent) {
        publishInitialLibraryContent(
            lifecycle = playbackLifecycle,
            reconciler = playbackReconciler,
            content = initialLibraryContent,
            updateState = { state ->
                initialPublication = state
                state.content?.let { updateLibraryContent(it) }
                state.errorMessage?.let { importMessage = it }
            },
        )
    }

    fun refreshPlaylists() {
        playlistState = reducePlaylistState(playlistState, PlaylistStateAction.LoadStarted)
        scope.launch {
            playlistState = reducePlaylistState(
                playlistState,
                playlistStateOwner.refresh(PlaylistReadFailedMessage),
            )
        }
    }

    LaunchedEffect(playlistRepository) {
        refreshPlaylists()
    }

    fun launchSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed || !sourceMutationsAllowed(
                isProgressActive = scanProgress?.isActive == true,
                isJobActive = scanJob?.isActive == true,
            )
        ) return
        scanCancellationRequested.value = false
        scanJob = scope.launch(Dispatchers.Default) {
            val progress = ScanProgress(
                session = ScanSession(id = "", sourceId = source.id, status = ScanStatus.Scanning, startedAtEpochMillis = 0L),
            )
            withContext(Dispatchers.Main) { scanProgress = progress }

            val session = scanner.scan(
                source = source,
                isCancelled = { scanCancellationRequested.value },
                onProgress = { latestProgress ->
                    scope.launch(Dispatchers.Main) {
                        scanProgress = latestProgress
                    }
                },
            )

            val content = loadLibraryContent(repository, platformAccess)
            publishScanContentAfterReconcile(
                reconciler = playbackReconciler,
                loadPlaylists = playlistStateOwner::refresh,
                content = content,
                session = session,
                ownerIsActive = { currentCoroutineContext().isActive },
                publish = { publication ->
                    withContext(Dispatchers.Main) {
                        scanProgress = publication.progress
                        importMessage = publication.errorMessage ?: scanCompleteFormat
                            .replaceFirst("%1\$d", session.tracksAdded.toString())
                            .replaceFirst("%2\$d", session.tracksUpdated.toString())
                        updateLibraryContent(publication.content)
                        publication.playlists?.let { action ->
                            playlistState = reducePlaylistState(playlistState, action)
                        }
                    }
                },
            )
        }
    }

    fun mutationError(message: String) {
        importMessage = message
    }

    fun launchLibraryMutation(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                mutationError(failure.appFailureMessage())
            }
        }
    }

    fun launchPlaylistMutation(
        mutation: PlaylistRepository.() -> Unit,
        onOutcome: (PlaylistStateAction) -> Unit,
    ) {
        scope.launch {
            val outcome = playlistStateOwner.mutate(PlaylistMutationFailedMessage, mutation)
            playlistState = reducePlaylistState(playlistState, outcome)
            onOutcome(outcome)
        }
    }

    val backupDocumentLauncher = rememberPlatformPlaylistBackupDocumentLauncher(
        onSaveResult = { result ->
            playlistBackupState = when (result) {
                PlaylistBackupDocumentSaveResult.Success,
                PlaylistBackupDocumentSaveResult.Cancelled,
                -> reducePlaylistBackupUiState(playlistBackupState, PlaylistBackupUiAction.PanelCancelled)
                is PlaylistBackupDocumentSaveResult.Unavailable -> reducePlaylistBackupUiState(
                    playlistBackupState,
                    PlaylistBackupUiAction.Failed(PlaylistBackupUiError.Unavailable),
                )
                is PlaylistBackupDocumentSaveResult.Failure -> reducePlaylistBackupUiState(
                    playlistBackupState,
                    PlaylistBackupUiAction.Failed(PlaylistBackupUiError.WriteFailed),
                )
            }
        },
        onOpenResult = { result ->
            when (result) {
                PlaylistBackupDocumentOpenResult.Cancelled -> {
                    playlistBackupState = reducePlaylistBackupUiState(
                        playlistBackupState,
                        PlaylistBackupUiAction.PanelCancelled,
                    )
                }
                is PlaylistBackupDocumentOpenResult.Unavailable -> {
                    playlistBackupState = reducePlaylistBackupUiState(
                        playlistBackupState,
                        PlaylistBackupUiAction.Failed(PlaylistBackupUiError.Unavailable),
                    )
                }
                is PlaylistBackupDocumentOpenResult.TooLarge -> {
                    playlistBackupState = reducePlaylistBackupUiState(
                        playlistBackupState,
                        PlaylistBackupUiAction.Failed(PlaylistBackupUiError.Oversized),
                    )
                }
                is PlaylistBackupDocumentOpenResult.Failure -> {
                    playlistBackupState = reducePlaylistBackupUiState(
                        playlistBackupState,
                        PlaylistBackupUiAction.Failed(PlaylistBackupUiError.ReadFailed),
                    )
                }
                is PlaylistBackupDocumentOpenResult.Success -> scope.launch {
                    try {
                        playlistBackupState = reducePlaylistBackupUiState(
                            playlistBackupState,
                            PlaylistBackupUiAction.OperationStarted(PlaylistBackupOperation.Planning),
                        )
                        when (val prepared = preparePlaylistBackupImport(
                            bytes = result.bytes,
                            destinationTracks = libraryTracks,
                            existingPlaylistNames = playlistState.confirmedSnapshot.playlists.map { it.name },
                            importedSuffix = importedSuffix,
                            libraryRevision = libraryRevision,
                            dispatcher = Dispatchers.Default,
                        )) {
                            is PlaylistBackupImportPreparation.Ready -> playlistBackupState = reducePlaylistBackupUiState(
                                playlistBackupState,
                                PlaylistBackupUiAction.PreviewReady(prepared.plan),
                            )
                            is PlaylistBackupImportPreparation.Failed -> playlistBackupState = reducePlaylistBackupUiState(
                                playlistBackupState,
                                PlaylistBackupUiAction.Failed(prepared.error),
                            )
                        }
                    } catch (cancelled: CancellationException) {
                        playlistBackupState = reducePlaylistBackupUiState(
                            playlistBackupState,
                            PlaylistBackupUiAction.OperationCancelled,
                        )
                        throw cancelled
                    }
                }
            }
        },
    )

    fun exportPlaylists() {
        if (playlistBackupState.isBusy) return
        playlistBackupState = reducePlaylistBackupUiState(
            playlistBackupState,
            PlaylistBackupUiAction.OperationStarted(PlaylistBackupOperation.Exporting),
        )
        scope.launch {
            try {
                when (val prepared = preparePlaylistBackupExport(
                    snapshot = playlistState.confirmedSnapshot,
                    authoritativeTracks = libraryTracks,
                    exportedAtEpochMillis = com.eterocell.rhythhaus.library.currentTimeMillis(),
                    dispatcher = Dispatchers.Default,
                )) {
                    is PlaylistBackupExportPreparation.Ready -> {
                        playlistBackupState = reducePlaylistBackupUiState(
                            playlistBackupState,
                            PlaylistBackupUiAction.OperationStarted(PlaylistBackupOperation.Saving),
                        )
                        backupDocumentLauncher.save(playlistBackupFileName("rhythhaus-playlists"), prepared.bytes)
                    }
                    is PlaylistBackupExportPreparation.Failed -> playlistBackupState = reducePlaylistBackupUiState(
                        playlistBackupState,
                        PlaylistBackupUiAction.Failed(prepared.error),
                    )
                }
            } catch (cancelled: CancellationException) {
                playlistBackupState = reducePlaylistBackupUiState(
                    playlistBackupState,
                    PlaylistBackupUiAction.OperationCancelled,
                )
                throw cancelled
            }
        }
    }

    fun openPlaylistBackup() {
        if (playlistBackupState.isBusy) return
        playlistBackupState = reducePlaylistBackupUiState(
            playlistBackupState,
            PlaylistBackupUiAction.OperationStarted(PlaylistBackupOperation.Opening),
        )
        backupDocumentLauncher.open()
    }

    fun confirmPlaylistBackup() {
        if (playlistBackupState.isBusy) return
        val preview = playlistBackupState.preview ?: return
        val expectedLibraryRevision = preview.plan.libraryRevision
        playlistBackupState = reducePlaylistBackupUiState(
            playlistBackupState,
            PlaylistBackupUiAction.OperationStarted(PlaylistBackupOperation.Importing),
        )
        scope.launch {
            try {
                val confirmation = confirmPlaylistBackupImportSerialized(
                    state = playlistBackupState,
                    currentLibraryRevision = libraryRevision,
                    lastConfirmedSnapshot = playlistState.confirmedSnapshot,
                    mutateAndRefresh = { mutations ->
                        when (val guarded = libraryPublicationOwner.withCurrentRevision(expectedLibraryRevision) {
                            playlistStateOwner.importPlaylists(mutations)
                        }) {
                            is AuthoritativeRevisionResult.Current -> guarded.value
                            AuthoritativeRevisionResult.Stale -> com.eterocell.rhythhaus.library.ui.PlaylistImportOwnerResult.Stale
                        }
                    },
                )
                playlistBackupState = confirmation.state
                confirmation.confirmedSnapshot?.takeIf { confirmation.state.result != null }?.let { snapshot ->
                    playlistState = reducePlaylistState(
                        playlistState,
                        PlaylistStateAction.SnapshotConfirmed(
                            snapshot,
                            requireNotNull(confirmation.playlistPublicationRevision),
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                playlistBackupState = reducePlaylistBackupUiState(
                    playlistBackupState,
                    PlaylistBackupUiAction.OperationCancelled,
                )
                throw cancelled
            }
        }
    }

    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        when (result) {
            is PlatformFolderPickResult.Success -> launchSourceScan(
                normalizePickedSource(result.source, librarySources),
            )

            is PlatformFolderPickResult.Unavailable -> importMessage = result.message

            is PlatformFolderPickResult.Failure -> importMessage = result.message
        }
    }
    val snapshot = remember(libraryTracks) { librarySnapshot(libraryTracks) }

    RhythHausTheme(selectedThemeMode = selectedThemeMode) {
        CompositionLocalProvider(
            LocalTrackArtworkLoader provides { trackId -> repository.artworkForTrack(trackId) },
        ) {
            LibraryHomeScreen(
                snapshot = snapshot,
                libraryTracks = libraryTracks,
                tagLibReader = tagLibReader,
                playbackController = controller,
                playlistRepository = playlistRepository,
                playlistState = playlistState,
                playlistBackupState = playlistBackupState,
                backupDocumentAvailable = backupDocumentLauncher.isAvailable,
                onPlaylistStateAction = { action -> playlistState = reducePlaylistState(playlistState, action) },
                onRefreshPlaylists = ::refreshPlaylists,
                onPlaylistMutation = ::launchPlaylistMutation,
                onExportPlaylists = ::exportPlaylists,
                onOpenPlaylistBackup = ::openPlaylistBackup,
                onConfirmPlaylistBackup = ::confirmPlaylistBackup,
                onPlaylistBackupAction = { action ->
                    playlistBackupState = reducePlaylistBackupUiState(playlistBackupState, action)
                },
                sources = librarySources,
                folderPickerLauncher = folderPickerLauncher,
                sourcePickerActionVisible = sourcePickerActionVisible(
                    supportsAdditionalSources = folderPickerLauncher.supportsAdditionalSources,
                    sourceCount = librarySources.size,
                ),
                importMessage = importMessage,
                scanProgress = scanProgress,
                scanJob = scanJob,
                currentThemeMode = selectedThemeMode,
                onThemeModeSelected = { mode ->
                    scope.launch {
                        themePreferenceStore.setSelectedThemeMode(mode)
                    }
                },
                onClearLibrary = {
                    if (initialPublication.mutationsAllowed && sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        launchLibraryMutation {
                            clearLibraryInBackground(
                                repository = repository,
                                platformAccess = platformAccess,
                                reconciler = playbackReconciler,
                                ioDispatcher = Dispatchers.Default,
                                ownerIsActive = { currentCoroutineContext().isActive },
                                updateLibrary = ::updateLibraryContent,
                                loadPlaylists = playlistStateOwner::refresh,
                                updatePlaylists = { action ->
                                    playlistState = reducePlaylistState(playlistState, action)
                                },
                                updateError = ::mutationError,
                            )
                        }
                    }
                },
                onRescanSource = ::launchSourceScan,
                onRemoveSource = { source ->
                    if (initialPublication.mutationsAllowed && sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        launchLibraryMutation {
                            removeSourceInBackground(
                                sourceId = source.id,
                                repository = repository,
                                platformAccess = platformAccess,
                                reconciler = playbackReconciler,
                                ioDispatcher = Dispatchers.Default,
                                ownerIsActive = { currentCoroutineContext().isActive },
                                updateLibrary = ::updateLibraryContent,
                                loadPlaylists = playlistStateOwner::refresh,
                                updatePlaylists = { action ->
                                    playlistState = reducePlaylistState(playlistState, action)
                                },
                                updateError = ::mutationError,
                            )
                        }
                    }
                },
                onCancelScan = {
                    scanCancellationRequested.value = true
                    scanProgress = scanProgress.requestScanCancellation()
                },
            )
        }
    }
}

internal data class LibraryContentState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
)

internal data class AuthoritativeLibraryPublication(
    val content: LibraryContentState,
    val revision: Long,
)

internal class AuthoritativeLibraryPublicationOwner {
    private val mutex = Mutex()

    var revision: Long = 0L
        private set

    suspend fun publish(content: LibraryContentState): AuthoritativeLibraryPublication = mutex.withLock {
        AuthoritativeLibraryPublication(content, ++revision)
    }

    suspend fun <T> withCurrentRevision(
        expectedRevision: Long,
        block: suspend () -> T,
    ): AuthoritativeRevisionResult<T> = mutex.withLock {
        if (revision != expectedRevision) AuthoritativeRevisionResult.Stale
        else AuthoritativeRevisionResult.Current(block())
    }
}

internal sealed interface AuthoritativeRevisionResult<out T> {
    data class Current<T>(val value: T) : AuthoritativeRevisionResult<T>
    data object Stale : AuthoritativeRevisionResult<Nothing>
}

internal data class InitialLibraryPublicationState(
    val content: LibraryContentState? = null,
    val errorMessage: String? = null,
    val isReady: Boolean = false,
) {
    val mutationsAllowed: Boolean get() = isReady

    fun complete(content: LibraryContentState): InitialLibraryPublicationState = copy(
        content = content,
        errorMessage = null,
        isReady = true,
    )

    fun failSafe(content: LibraryContentState, failure: Throwable): InitialLibraryPublicationState = copy(
        content = content,
        errorMessage = failure.appFailureMessage(),
        isReady = true,
    )
}

internal suspend fun publishInitialLibraryContent(
    lifecycle: PlaybackSessionRestorer,
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    updateState: suspend (InitialLibraryPublicationState) -> Unit,
) {
    val pending = InitialLibraryPublicationState()
    try {
        lifecycle.restoreOnce(content.tracks.map(LibraryTrack::toPlayableTrack))
        reconciler.reconcile(content.tracks)
        updateState(pending.complete(content))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        updateState(pending.failSafe(content, failure))
    }
}

internal suspend fun publishLibraryContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    updateLibrary: suspend (LibraryContentState) -> Unit,
) {
    reconciler.reconcile(content.tracks)
    updateLibrary(content)
}

internal data class ScanPublicationState(
    val content: LibraryContentState,
    val progress: ScanProgress,
    val playlists: PlaylistStateAction? = null,
    val errorMessage: String? = null,
)

internal suspend fun publishScanContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    loadPlaylists: (suspend () -> PlaylistStateAction)? = null,
    content: LibraryContentState,
    session: ScanSession,
    ownerIsActive: suspend () -> Boolean,
    publish: suspend (ScanPublicationState) -> Unit,
) {
    val publication = try {
        reconciler.reconcile(content.tracks)
        ScanPublicationState(
            content = content,
            progress = ScanProgress(session),
        )
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            withContext(NonCancellable) {
                val playlists = loadPlaylists?.invoke()
                publish(
                    ScanPublicationState(
                        content = content,
                        progress = ScanProgress(session.terminalAfterCancellation()),
                        playlists = playlists,
                    ),
                )
            }
        }
        throw cancelled
    } catch (failure: Throwable) {
        ScanPublicationState(
            content = content,
            progress = ScanProgress(session.terminalAfterFailure(failure)),
            errorMessage = failure.appFailureMessage(),
        )
    }
    val playlists = loadPlaylists?.invoke()
    publish(publication.copy(playlists = playlists))
}

internal fun loadLibraryContent(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
): LibraryContentState = LibraryContentState(
    sources = repository.sources().map { source ->
        source.copy(accessStatus = platformAccess.accessStatus(source))
    },
    tracks = repository.tracks(),
)

internal suspend fun removeSourceInBackground(
    sourceId: String,
    repository: LibraryRepository,
    loadPlaylists: (suspend () -> PlaylistStateAction)? = null,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    updateLibrary: suspend (LibraryContentState) -> Unit,
    updatePlaylists: (PlaylistStateAction) -> Unit = {},
    updateError: (String) -> Unit = {},
) {
    val content = withContext(ioDispatcher) {
        val source = repository.sources().firstOrNull { it.id == sourceId }
        repository.removeSource(sourceId)
        source?.let(platformAccess::releaseAccess)
        loadLibraryContent(repository, platformAccess)
    }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        loadPlaylists = loadPlaylists,
        updateLibrary = updateLibrary,
        updatePlaylists = updatePlaylists,
        updateError = updateError,
    )
}

internal suspend fun clearLibraryInBackground(
    repository: LibraryRepository,
    loadPlaylists: (suspend () -> PlaylistStateAction)? = null,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    updateLibrary: suspend (LibraryContentState) -> Unit,
    updatePlaylists: (PlaylistStateAction) -> Unit = {},
    updateError: (String) -> Unit = {},
) {
    val content = withContext(ioDispatcher) {
        val sources = repository.sources()
        repository.clearAll()
        sources.forEach(platformAccess::releaseAccess)
        loadLibraryContent(repository, platformAccess)
    }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        loadPlaylists = loadPlaylists,
        updateLibrary = updateLibrary,
        updatePlaylists = updatePlaylists,
        updateError = updateError,
    )
}

private suspend fun publishLibraryContentAfterReconcileFailureSafe(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    ownerIsActive: suspend () -> Boolean,
    loadPlaylists: (suspend () -> PlaylistStateAction)?,
    updateLibrary: suspend (LibraryContentState) -> Unit,
    updatePlaylists: (PlaylistStateAction) -> Unit,
    updateError: (String) -> Unit,
) {
    suspend fun publishAuthoritativeContent(errorMessage: String? = null) {
        val playlists = loadPlaylists?.invoke()
        updateLibrary(content)
        playlists?.let(updatePlaylists)
        errorMessage?.let(updateError)
    }
    val reconciliationError = try {
        reconciler.reconcile(content.tracks)
        null
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            withContext(NonCancellable) {
                publishAuthoritativeContent(cancelled.appFailureMessage())
            }
        }
        throw cancelled
    } catch (failure: Throwable) {
        failure.appFailureMessage()
    }
    publishAuthoritativeContent(reconciliationError)
}

internal fun ScanProgress?.requestScanCancellation(): ScanProgress? {
    val session = this?.session ?: return this
    if (session.status != ScanStatus.Scanning) return this
    return copy(session = session.copy(status = ScanStatus.Cancelling))
}

private fun Throwable.appFailureMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "Playback session unavailable"


private fun ScanSession.terminalAfterCancellation(): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Cancelled, terminalMessage = "Scan cancelled")
    } else {
        this
    }

private fun ScanSession.terminalAfterFailure(failure: Throwable): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Failed, terminalMessage = failure.appFailureMessage())
    } else {
        this
    }

@Composable
private fun RhythHausTheme(
    selectedThemeMode: RhythHausThemeMode,
    content: @Composable () -> Unit,
) {
    val colors = resolveHausPalette(
        mode = selectedThemeMode,
        systemIsDark = systemPrefersDarkTheme(),
    )
    val colorScheme = if (colors == DarkHausPalette) {
        darkColorScheme(
            primary = colors.ink,
            onPrimary = colors.paper,
            secondary = colors.pulse,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceContainer = colors.panel,
            onSurfaceContainer = colors.ink,
            secondaryVariant = colors.pulse,
            onSecondaryVariant = colors.paper,
            disabledSecondaryVariant = colors.pulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = colors.paper.copy(alpha = 0.28f),
        )
    } else {
        lightColorScheme(
            primary = colors.ink,
            onPrimary = colors.paper,
            secondary = colors.pulse,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceContainer = colors.panel,
            onSurfaceContainer = colors.ink,
            secondaryVariant = colors.pulse,
            onSecondaryVariant = colors.paper,
            disabledSecondaryVariant = colors.pulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = colors.paper.copy(alpha = 0.28f),
        )
    }

    MiuixTheme(
        colors = colorScheme,
    ) {
        CompositionLocalProvider(LocalHausColors provides colors) {
            content()
        }
    }
}
