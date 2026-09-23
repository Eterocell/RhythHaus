package com.eterocell.rhythhaus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.RemoveMissingTracksRejectionReason
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.defaultPlatformLibrarySource
import com.eterocell.rhythhaus.library.iosImportSummaryMessage
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.registerMissingDefaultLibrarySource
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
import com.eterocell.rhythhaus.library.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.library.ui.OnboardingLaunchMode
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionController
import com.eterocell.rhythhaus.notificationpermission.UnavailableMediaNotificationPermissionController
import com.eterocell.rhythhaus.onboarding.OnboardingEligibility
import com.eterocell.rhythhaus.onboarding.OnboardingPreferenceStore
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupController
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuard
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuardResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.createPlaylistBackupController
import com.eterocell.rhythhaus.playlistbackup.rememberPlatformPlaylistBackupDocumentLauncher
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.resolveHausPalette
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.ios_import_summary_format
import rhythhaus.shared.generated.resources.playlist_backup_imported_suffix
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.scan_complete_format
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

internal data class AppOnboardingPresentation(
    val initialOnboarding: OnboardingLaunchMode?,
    val saving: Boolean,
    val completionError: String?,
    val completeOnboarding: () -> Unit,
)

@Composable
internal fun AppOnboardingGate(
    onboardingPreferenceStore: OnboardingPreferenceStore,
    loadingContent: @Composable () -> Unit,
    content: @Composable (AppOnboardingPresentation) -> Unit,
) {
    val eligibility by
        onboardingPreferenceStore.eligibility.collectAsState(
            OnboardingEligibility.Loading)
    var saving by remember { mutableStateOf(false) }
    var completionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val completeOnboarding: () -> Unit = {
        if (!saving) {
            saving = true
            completionError = null
            scope.launch {
                try {
                    onboardingPreferenceStore.markCurrentVersionCompleted()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    completionError =
                        failure.message ?: "Unable to save onboarding"
                } finally {
                    saving = false
                }
            }
        }
    }
    if (eligibility == OnboardingEligibility.Loading) {
        loadingContent()
    } else {
        content(
            AppOnboardingPresentation(
                initialOnboarding =
                    OnboardingLaunchMode.FirstRun.takeIf {
                        eligibility == OnboardingEligibility.Required
                    },
                saving = saving,
                completionError = completionError,
                completeOnboarding = completeOnboarding,
            ),
        )
    }
}

@Composable
@Preview
fun App(
    notificationPermissionController: MediaNotificationPermissionController =
        UnavailableMediaNotificationPermissionController,
) {
    val controller = koinInject<PlaybackController>()
    val tagLibReader = koinInject<TagLibReader>()
    val repository = koinInject<LibraryRepository>()
    val playlistRepository = koinInject<PlaylistRepository>()
    val platformAccess = koinInject<PlatformSourceAccess>()
    val scanner = koinInject<LibraryScanner>()
    val themePreferenceStore = koinInject<ThemePreferenceStore>()
    val onboardingPreferenceStore = koinInject<OnboardingPreferenceStore>()
    val playbackLifecycle = koinInject<PlaybackProcessLifecycle>()
    val playbackReconciler = koinInject<PlaybackSessionReconciler>()
    val initialLibraryContent = remember {
        loadLibraryContent(repository, platformAccess)
    }
    val playlistStateOwner = koinInject<PlaylistStateOwner>()
    val libraryPublicationOwner = remember {
        AuthoritativeLibraryPublicationOwner()
    }
    var initialPublication by remember {
        mutableStateOf(InitialLibraryPublicationState())
    }
    var librarySources by remember {
        mutableStateOf(emptyList<LibrarySource>())
    }
    var libraryTracks by remember { mutableStateOf(emptyList<LibraryTrack>()) }
    var favoriteTrackIds by remember { mutableStateOf(emptySet<String>()) }
    var libraryRevision by remember { mutableStateOf(0L) }
    var playlistState by remember {
        mutableStateOf(PlaylistState(isLoading = true))
    }
    var playlistBackupState by remember {
        mutableStateOf(PlaylistBackupUiState())
    }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var followUpScanPending by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanErrors by remember { mutableStateOf(emptyList<ScanError>()) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scanCancellationRequested = remember { MutableStateFlow(false) }
    val scope = rememberCoroutineScope()
    val operationCoordinator = remember {
        AppLibraryOperationCoordinator {
            scanCancellationRequested.value = true
            scanProgress = scanProgress.requestScanCancellation()
            scanJob?.join()
        }
    }
    val libraryOrchestrator = remember {
        AppLibraryOrchestrator(
            coordinator = operationCoordinator,
            publishError = { message ->
                withContext(Dispatchers.Main) { importMessage = message }
            },
        )
    }
    val scanCompleteFormat = stringResource(Res.string.scan_complete_format)
    val iosImportSummaryFormat =
        stringResource(Res.string.ios_import_summary_format)
    val importedSuffix =
        stringResource(Res.string.playlist_backup_imported_suffix)
    val selectedThemeMode by
        themePreferenceStore.selectedThemeMode.collectAsState(
            RhythHausThemeMode.System)
    val notificationPermissionState by
        notificationPermissionController.state.collectAsState()
    suspend fun applyLibraryPublication(
        publication: AuthoritativeLibraryPublication,
    ) {
        withContext(Dispatchers.Main) {
            librarySources = publication.content.sources
            libraryTracks = publication.content.tracks
            favoriteTrackIds = publication.content.favoriteTrackIds
            libraryRevision = publication.revision
        }
    }
    suspend fun updateLibraryContent(content: LibraryContentState) {
        val publication =
            libraryPublicationOwner.publishWithFavoriteReconciliation(
                content = content,
                favoriteTrackIds = {
                    withContext(Dispatchers.Default) {
                        repository.favoriteTrackIds().toSet()
                    }
                },
            )
        applyLibraryPublication(publication)
    }

    fun refreshPlaylists() {
        playlistState =
            reducePlaylistState(playlistState, PlaylistStateAction.LoadStarted)
        scope.launch {
            playlistState =
                reducePlaylistState(
                    playlistState,
                    playlistStateOwner.refresh(),
                )
        }
    }

    LaunchedEffect(playlistRepository) {
        refreshPlaylists()
    }

    /**
     * Runs one source scan under an already-admitted coordinator token. Shared
     * by [launchSourceScan] and [launchFollowUpSourceScan] so the follow-up
     * scan uses the exact launch/admission flow as every other scan.
     */
    suspend fun performSourceScan(
        source: LibrarySource,
        token: LibraryOperationToken,
    ) {
        scanJob = currentCoroutineContext()[Job]
        scanCancellationRequested.value = false
        var progressCallbacks: OrderedScanProgressCallbacks? = null
        try {
            val progress =
                ScanProgress(
                    session =
                        ScanSession(
                            id = "",
                            sourceId = source.id,
                            status = ScanStatus.Scanning,
                            startedAtEpochMillis = 0L),
                )
            libraryOrchestrator.publishIfCurrent(token) {
                withContext(Dispatchers.Main) {
                    scanProgress = progress
                }
            }

            progressCallbacks =
                OrderedScanProgressCallbacks(scope) { latestProgress ->
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            scanProgress = latestProgress
                        }
                    }
                }
            val session =
                scanner.scan(
                    source = source,
                    isCancelled = { scanCancellationRequested.value },
                    onProgress = { latestProgress ->
                        progressCallbacks.offer(latestProgress)
                    },
                )

            progressCallbacks.awaitPublished()
            val content = loadLibraryContent(repository, platformAccess)
            publishScanContentAfterReconcile(
                reconciler = playbackReconciler,
                playlistStateOwner = playlistStateOwner,
                content = content,
                session = session,
                loadScanErrors = repository::scanErrors,
                ownerIsActive = { currentCoroutineContext().isActive },
                publish = { publication ->
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            scanProgress = publication.progress
                            scanErrors = publication.scanErrors
                            importMessage =
                                publication.errorMessage
                                    ?: scanCompleteFormat
                                        .replaceFirst(
                                            "%1\$d",
                                            session.tracksAdded.toString())
                                        .replaceFirst(
                                            "%2\$d",
                                            session.tracksUpdated.toString())
                            updateLibraryContent(publication.content)
                            publication.playlists?.let { action ->
                                playlistState =
                                    reducePlaylistState(
                                        playlistState,
                                        action.requireSuccessfulPublication(),
                                    )
                            }
                        }
                    }
                },
            )
        } finally {
            withContext(NonCancellable) {
                progressCallbacks?.awaitPublished()
            }
        }
    }

    fun launchSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed) return
        scope.launch(Dispatchers.Default) {
            libraryOrchestrator.launchScan { token ->
                performSourceScan(source, token)
            }
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
        val restoredSession = repository.latestTerminalScanSession()
        val restoredState =
            restoredTerminalScanState(
                restoredSession,
                initialLibraryContent.sources,
                restoredSession?.let { repository.scanErrors(it.id) }.orEmpty(),
            )
        scanProgress = restoredState.progress
        scanErrors = restoredState.errors

        defaultPlatformLibrarySource()?.let { defaultSource ->
            val sourceToScan =
                registerMissingDefaultLibrarySource(repository, defaultSource)
            if (sourceToScan != null) {
                updateLibraryContent(
                    loadLibraryContent(repository, platformAccess))
                launchSourceScan(sourceToScan)
            }
        }
    }

    /**
     * Launches the follow-up scan of a successful iOS import terminal.
     *
     * The platform import-active flag is cleared before the terminal result
     * reaches App, so the caller raises [followUpScanPending] first to keep
     * source mutations excluded across the handoff. This launch goes through
     * the coordinator admission exactly like any other scan; the admission call
     * returns only after it has conclusively settled, so the pending exclusion
     * is released there via [settleFollowUpScanPending]: when the follow-up
     * scan was admitted the coordinator state holds the gate until completion,
     * and when it was rejected no second scan is started. A cancelled scan
     * still releases the gate (the coordinator completes the token before
     * [AppLibraryOrchestrator.launchScan] rethrows the cancellation), and the
     * cancellation keeps propagating silently. Cancellation terminals never
     * reach this function and stay silent.
     */
    fun launchFollowUpSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed) {
            followUpScanPending = false
            return
        }
        scope.launch(Dispatchers.Default) {
            settleFollowUpScanPending(
                scan = {
                    libraryOrchestrator.launchScan { token ->
                        performSourceScan(source, token)
                    }
                },
                releasePending = { followUpScanPending = false },
            )
        }
    }

    fun mutationError(message: String) {
        importMessage = message
    }

    fun launchPlaylistMutation(
        mutation: PlaylistRepository.() -> Unit,
        onOutcome: (PlaylistStateAction) -> Unit,
    ) {
        scope.launch {
            val outcome = playlistStateOwner.mutate(mutation = mutation)
            playlistState = reducePlaylistState(playlistState, outcome)
            onOutcome(outcome)
        }
    }

    val backupControllerHolder = remember {
        arrayOfNulls<PlaylistBackupController>(1)
    }
    val backupDocumentLauncher =
        rememberPlatformPlaylistBackupDocumentLauncher(
            onSaveResult = { result ->
                backupControllerHolder[0]?.let { controller ->
                    playlistBackupState =
                        controller.receiveSave(playlistBackupState, result)
                }
            },
            onOpenResult = { result ->
                scope.launch {
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
                                    destinationTracks = libraryTracks,
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

    fun exportPlaylists() {
        if (playlistBackupState.isBusy) return
        scope.launch {
            playlistBackupState =
                runPlaylistBackupOperation(
                    currentState = { playlistBackupState },
                    publishState = { state -> playlistBackupState = state },
                    reduce = backupController::reduce,
                ) {
                    backupController.beginExport(
                        state = playlistBackupState,
                        snapshot = playlistState.confirmedSnapshot,
                        authoritativeTracks = libraryTracks,
                        exportedAtEpochMillis =
                            com.eterocell.rhythhaus.library.currentTimeMillis(),
                    )
                }
        }
    }

    fun openPlaylistBackup() {
        if (playlistBackupState.isBusy) return
        playlistBackupState = backupController.beginOpen(playlistBackupState)
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
                            playlistState =
                                reducePlaylistState(
                                    playlistState,
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

    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        val action =
            resolveLibraryPickerTerminal(
                result = result,
                existingSources = librarySources,
                importSummaryFormat = iosImportSummaryFormat,
            )
        action.message?.let { importMessage = it }
        action.scanSource?.let { source ->
            if (action.holdsFollowUpScanGate) {
                // The iOS import terminal already cleared the platform
                // import-active flag; hold the source-mutation exclusion until
                // exactly this follow-up scan is admitted or rejected by the
                // operation coordinator.
                followUpScanPending = true
                launchFollowUpSourceScan(source)
            } else {
                launchSourceScan(source)
            }
        }
    }
    // The coordinator state and mutation gate are collected after the picker
    // launcher so the iOS import-active window can fold into the same gate
    // that source mutations already use: while the picker is shown or files
    // are being copied, competing mutations stay disabled even before the
    // follow-up scan is admitted.
    val operationState by operationCoordinator.state.collectAsState()
    val mutationsEnabled =
        appLibraryMutationsEnabled(
            publicationMutationsAllowed = initialPublication.mutationsAllowed,
            coordinatorIdle = operationState is LibraryOperationState.Idle,
            importActive = folderPickerLauncher.isImportActive,
            followUpScanPending = followUpScanPending,
        )
    val snapshot = remember(libraryTracks) { librarySnapshot(libraryTracks) }
    RhythHausTheme(selectedThemeMode = selectedThemeMode) {
        CompositionLocalProvider(
            LocalTrackArtworkLoader provides
                { trackId ->
                    repository.artworkForTrack(trackId)
                },
        ) {
            AppOnboardingGate(
                onboardingPreferenceStore = onboardingPreferenceStore,
                loadingContent = { OnboardingLoadingSurface() },
            ) { onboarding ->
                LibraryHomeScreen(
                    snapshot = snapshot,
                    libraryTracks = libraryTracks,
                    tagLibReader = tagLibReader,
                    playbackController = controller,
                    playlistRepository = playlistRepository,
                    playlistState = playlistState,
                    playlistBackupState = playlistBackupState,
                    backupDocumentAvailable =
                        backupDocumentLauncher.isAvailable,
                    onPlaylistStateAction = { action ->
                        playlistState =
                            reducePlaylistState(playlistState, action)
                    },
                    onRefreshPlaylists = ::refreshPlaylists,
                    onPlaylistMutation = ::launchPlaylistMutation,
                    onExportPlaylists = ::exportPlaylists,
                    onOpenPlaylistBackup = ::openPlaylistBackup,
                    onConfirmPlaylistBackup = ::confirmPlaylistBackup,
                    onPlaylistBackupAction = { action ->
                        playlistBackupState =
                            backupController.reduce(playlistBackupState, action)
                    },
                    sources = librarySources,
                    folderPickerLauncher = folderPickerLauncher,
                    sourcePickerActionVisible =
                        sourcePickerActionVisible(
                            supportsAdditionalSources =
                                folderPickerLauncher.supportsAdditionalSources,
                            sourceCount = librarySources.size,
                        ),
                    importMessage = importMessage,
                    scanProgress = scanProgress,
                    scanErrors = scanErrors,
                    scanJob = scanJob,
                    mediaNotificationPermission = notificationPermissionState,
                    onRequestNotificationPermission = {
                        notificationPermissionController.requestPermission()
                    },
                    onOpenNotificationSettings = {
                        notificationPermissionController
                            .openAppNotificationSettings()
                    },
                    initialOnboarding = onboarding.initialOnboarding,
                    onboardingSaving = onboarding.saving,
                    onboardingCompletionError = onboarding.completionError,
                    onCompleteOnboarding = onboarding.completeOnboarding,
                    favoriteTrackIds = favoriteTrackIds,
                    onSetTrackFavorite = { trackId, favorite ->
                        scope.launch {
                            setTrackFavoriteAndPublish(
                                orchestrator = libraryOrchestrator,
                                publicationOwner = libraryPublicationOwner,
                                repository = repository,
                                platformAccess = platformAccess,
                                trackId = trackId,
                                favorite = favorite,
                                ioDispatcher = Dispatchers.Default,
                                publish = { publication ->
                                    applyLibraryPublication(publication)
                                },
                            )
                        }
                    },
                    coordinatorMutationsEnabled = mutationsEnabled,
                    currentThemeMode = selectedThemeMode,
                    onThemeModeSelected = { mode ->
                        scope.launch {
                            themePreferenceStore.setSelectedThemeMode(mode)
                        }
                    },
                    onClearLibrary = {
                        if (mutationsEnabled) {
                            scope.launch {
                                libraryOrchestrator.launch(
                                    LibraryOperationKind.Clear) { token ->
                                        clearLibraryInBackground(
                                            repository = repository,
                                            platformAccess = platformAccess,
                                            reconciler = playbackReconciler,
                                            ioDispatcher = Dispatchers.Default,
                                            ownerIsActive = {
                                                currentCoroutineContext()
                                                    .isActive
                                            },
                                            playlistStateOwner =
                                                playlistStateOwner,
                                            publish = { publication ->
                                                libraryOrchestrator
                                                    .publishIfCurrent(token) {
                                                        withContext(
                                                            Dispatchers.Main) {
                                                                publication
                                                                    .content
                                                                    ?.let {
                                                                        updateLibraryContent(
                                                                            it)
                                                                    }
                                                                scanErrors =
                                                                    emptyList()
                                                                publication
                                                                    .playlists
                                                                    ?.let {
                                                                        action
                                                                        ->
                                                                        playlistState =
                                                                            reducePlaylistState(
                                                                                playlistState,
                                                                                action
                                                                                    .requireSuccessfulPublication(),
                                                                            )
                                                                    }
                                                                publication
                                                                    .errorMessage
                                                                    ?.let(
                                                                        ::
                                                                            mutationError)
                                                            }
                                                    }
                                            },
                                        )
                                    }
                            }
                        }
                    },
                    onRescanSource = ::launchSourceScan,
                    onRemoveSource = { source ->
                        if (mutationsEnabled) {
                            scope.launch {
                                libraryOrchestrator.launch(
                                    LibraryOperationKind.RemoveSource) { token
                                        ->
                                        removeSourceInBackground(
                                            sourceId = source.id,
                                            repository = repository,
                                            platformAccess = platformAccess,
                                            reconciler = playbackReconciler,
                                            ioDispatcher = Dispatchers.Default,
                                            ownerIsActive = {
                                                currentCoroutineContext()
                                                    .isActive
                                            },
                                            playlistStateOwner =
                                                playlistStateOwner,
                                            publish = { publication ->
                                                libraryOrchestrator
                                                    .publishIfCurrent(token) {
                                                        withContext(
                                                            Dispatchers.Main) {
                                                                publication
                                                                    .content
                                                                    ?.let {
                                                                        updateLibraryContent(
                                                                            it)
                                                                    }
                                                                scanErrors =
                                                                    emptyList()
                                                                publication
                                                                    .playlists
                                                                    ?.let {
                                                                        action
                                                                        ->
                                                                        playlistState =
                                                                            reducePlaylistState(
                                                                                playlistState,
                                                                                action
                                                                                    .requireSuccessfulPublication(),
                                                                            )
                                                                    }
                                                                publication
                                                                    .errorMessage
                                                                    ?.let(
                                                                        ::
                                                                            mutationError)
                                                            }
                                                    }
                                            },
                                        )
                                    }
                            }
                        }
                    },
                    onRemoveMissingTracks = { source, session ->
                        if (mutationsEnabled) {
                            scope.launch {
                                libraryOrchestrator.launch(
                                    LibraryOperationKind.RemoveMissingTracks) {
                                        token ->
                                        removeMissingTracksInBackground(
                                            sourceId = source.id,
                                            latestScanId = session.id,
                                            repository = repository,
                                            platformAccess = platformAccess,
                                            reconciler = playbackReconciler,
                                            ioDispatcher = Dispatchers.Default,
                                            ownerIsActive = {
                                                currentCoroutineContext()
                                                    .isActive
                                            },
                                            playlistStateOwner =
                                                playlistStateOwner,
                                            publish = { publication ->
                                                libraryOrchestrator
                                                    .publishIfCurrent(token) {
                                                        withContext(
                                                            Dispatchers.Main) {
                                                                publication
                                                                    .content
                                                                    ?.let {
                                                                        updateLibraryContent(
                                                                            it)
                                                                    }
                                                                scanErrors =
                                                                    resolveMutationScanErrors(
                                                                        scanErrors,
                                                                        publication)
                                                                publication
                                                                    .playlists
                                                                    ?.let {
                                                                        action
                                                                        ->
                                                                        playlistState =
                                                                            reducePlaylistState(
                                                                                playlistState,
                                                                                action
                                                                                    .requireSuccessfulPublication(),
                                                                            )
                                                                    }
                                                                publication
                                                                    .errorMessage
                                                                    ?.let(
                                                                        ::
                                                                            mutationError)
                                                            }
                                                    }
                                            },
                                        )
                                    }
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
}

@Composable
private fun OnboardingLoadingSurface() {
    Box(
        modifier =
            androidx.compose.ui.Modifier.fillMaxSize()
                .testTag("onboarding-loading"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.playlist_loading))
    }
}

internal suspend fun <T> runPlaylistBackupOperation(
    currentState: () -> PlaylistBackupUiState,
    publishState: (PlaylistBackupUiState) -> Unit,
    reduce:
        (
            PlaylistBackupUiState,
            PlaylistBackupUiAction) -> PlaylistBackupUiState,
    block: suspend () -> T,
): T =
    try {
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

internal data class LibraryContentState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
    val favoriteTrackIds: Set<String> = emptySet(),
)

internal data class AuthoritativeLibraryPublication(
    val content: LibraryContentState,
    val revision: Long,
)

internal class AuthoritativeLibraryPublicationOwner {
    private val mutex = Mutex()

    var revision: Long = 0L
        private set

    suspend fun publish(
        content: LibraryContentState
    ): AuthoritativeLibraryPublication = mutex.withLock {
        nextPublication(content)
    }

    suspend fun publishWithFavoriteReconciliation(
        content: LibraryContentState,
        favoriteTrackIds: suspend () -> Set<String>,
    ): AuthoritativeLibraryPublication = mutex.withLock {
        nextPublication(
            content.copy(favoriteTrackIds = favoriteTrackIds()),
        )
    }

    suspend fun publishIfCurrentRevision(
        expectedRevision: Long,
        content: LibraryContentState,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication> =
        mutex.withLock {
            if (revision != expectedRevision) {
                AuthoritativeRevisionResult.Stale
            } else {
                AuthoritativeRevisionResult.Current(nextPublication(content))
            }
        }

    /**
     * Keeps an accepted persistence mutation and its visible authoritative
     * publication in one revision-guarded critical section. Returning null from
     * [mutation] leaves the current publication unchanged.
     */
    suspend fun mutateAndPublishIfCurrentRevision(
        expectedRevision: Long,
        mutation: suspend () -> LibraryContentState?,
        publish: suspend (AuthoritativeLibraryPublication) -> Unit,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication?> =
        mutex.withLock {
            if (revision != expectedRevision) {
                AuthoritativeRevisionResult.Stale
            } else {
                val content =
                    mutation()
                        ?: return@withLock AuthoritativeRevisionResult.Current(
                            null)
                val publication = nextPublication(content)
                publish(publication)
                AuthoritativeRevisionResult.Current(publication)
            }
        }

    suspend fun mutateAndPublish(
        mutation: suspend () -> LibraryContentState?,
        publish: suspend (AuthoritativeLibraryPublication) -> Unit,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication?> =
        mutex.withLock {
            val content =
                mutation()
                    ?: return@withLock AuthoritativeRevisionResult.Current(null)
            val publication = nextPublication(content)
            publish(publication)
            AuthoritativeRevisionResult.Current(publication)
        }

    private fun nextPublication(
        content: LibraryContentState
    ): AuthoritativeLibraryPublication =
        AuthoritativeLibraryPublication(content, ++revision)

    suspend fun <T> withCurrentRevision(
        expectedRevision: Long,
        block: suspend () -> T,
    ): AuthoritativeRevisionResult<T> = mutex.withLock {
        if (revision != expectedRevision) {
            AuthoritativeRevisionResult.Stale
        } else {
            AuthoritativeRevisionResult.Current(block())
        }
    }
}

internal sealed interface AuthoritativeRevisionResult<out T> {
    data class Current<T>(val value: T) : AuthoritativeRevisionResult<T>

    data object Stale : AuthoritativeRevisionResult<Nothing>
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

internal data class InitialLibraryPublicationState(
    val content: LibraryContentState? = null,
    val errorMessage: String? = null,
    val isReady: Boolean = false,
) {
    val mutationsAllowed: Boolean
        get() = isReady

    fun complete(content: LibraryContentState): InitialLibraryPublicationState =
        copy(
            content = content,
            errorMessage = null,
            isReady = true,
        )

    fun failSafe(
        content: LibraryContentState,
        failure: Throwable
    ): InitialLibraryPublicationState =
        copy(
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
        lifecycle.restoreOnce(content.tracks.map { it.toPlayableTrack() })
        reconciler.reconcile(content.tracks)
        updateState(pending.complete(content))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        updateState(pending.failSafe(content, failure))
    }
}

internal data class ScanPublicationState(
    val content: LibraryContentState,
    val progress: ScanProgress,
    val playlists: PlaylistStateAction? = null,
    val errorMessage: String? = null,
    val scanErrors: List<ScanError> = emptyList(),
)

internal data class LibraryMutationPublication(
    val content: LibraryContentState?,
    val playlists: PlaylistStateAction?,
    val errorMessage: String?,
    val scanErrors: List<ScanError>? = emptyList(),
)

internal fun resolveMutationScanErrors(
    currentScanErrors: List<ScanError>,
    publication: LibraryMutationPublication,
): List<ScanError> = publication.scanErrors ?: currentScanErrors

internal suspend fun publishScanContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    playlistStateOwner: PlaylistStateOwner,
    content: LibraryContentState,
    session: ScanSession,
    loadScanErrors: ((String) -> List<ScanError>)? = null,
    ownerIsActive: suspend () -> Boolean,
    publish: suspend (ScanPublicationState) -> Unit,
) {
    try {
        reconciler.reconcile(content.tracks)
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            withContext(NonCancellable) {
                val playlists = runCatching {
                    playlistStateOwner.refresh().requireSuccessfulPublication()
                }
                    .getOrNull()
                val scanErrors = runCatching {
                    loadScanErrors?.invoke(session.id).orEmpty()
                }
                    .getOrDefault(emptyList())
                runCatching {
                    publish(
                        ScanPublicationState(
                            content = content,
                            progress =
                                ScanProgress(
                                    session.terminalAfterCancellation()),
                            playlists = playlists,
                            scanErrors = scanErrors,
                        ),
                    )
                }
            }
        }
        throw cancelled
    }
    val publication =
        ScanPublicationState(
            content = content, progress = ScanProgress(session))
    val playlists = playlistStateOwner.refresh().requireSuccessfulPublication()
    publish(
        publication.copy(
            playlists = playlists,
            scanErrors = loadScanErrors?.invoke(session.id).orEmpty(),
        ),
    )
}

internal fun loadLibraryContent(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
): LibraryContentState =
    LibraryContentState(
        sources =
            repository.sources().map { source ->
                source.copy(accessStatus = platformAccess.accessStatus(source))
            },
        tracks = repository.tracks(),
        favoriteTrackIds = repository.favoriteTrackIds().toSet(),
    )

/**
 * Applies a desired favorite state through the App-owned mutation coordinator
 * and republishes the combined library projection only while its revision is
 * still current.
 */
internal suspend fun setTrackFavoriteAndPublish(
    orchestrator: AppLibraryOrchestrator,
    publicationOwner: AuthoritativeLibraryPublicationOwner,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    trackId: String,
    favorite: Boolean,
    expectedRevision: Long? = null,
    ioDispatcher: CoroutineDispatcher,
    publish: suspend (AuthoritativeLibraryPublication) -> Unit,
): Boolean {
    var published = false
    orchestrator.launch(LibraryOperationKind.SetTrackFavorite) { token ->
        orchestrator.publishIfCurrent(token) {
            currentCoroutineContext().ensureActive()
        } ?: return@launch
        val result =
            withContext(NonCancellable) {
                publicationOwner.mutateAndPublish(
                    mutation = {
                        withContext(ioDispatcher) {
                            if (!repository.setTrackFavorite(
                                trackId, favorite)) {
                                null
                            } else {
                                loadLibraryContent(repository, platformAccess)
                            }
                        }
                    },
                    publish = publish,
                )
            }
        published =
            result is AuthoritativeRevisionResult.Current &&
                result.value != null
    }
    return published
}

internal suspend fun removeSourceInBackground(
    sourceId: String,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val content =
        withContext(ioDispatcher) {
            val source = repository.sources().firstOrNull { it.id == sourceId }
            repository.removeSource(sourceId)
            source?.let(platformAccess::releaseAccess)
            loadLibraryContent(repository, platformAccess)
        }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        playlistStateOwner = playlistStateOwner,
        publish = publish,
    )
}

internal suspend fun removeMissingTracksInBackground(
    sourceId: String,
    latestScanId: String,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val resultAndContent =
        withContext(ioDispatcher) {
            val result = repository.removeMissingTracks(sourceId, latestScanId)
            result to
                if (result is RemoveMissingTracksResult.Removed) {
                    loadLibraryContent(repository, platformAccess)
                } else {
                    null
                }
        }
    val (result, content) = resultAndContent
    val errorMessage =
        when (result) {
            is RemoveMissingTracksResult.Removed -> null
            is RemoveMissingTracksResult.Rejected ->
                removeMissingTracksRejectionMessage(result.reason)
        }
    if (result is RemoveMissingTracksResult.Rejected) {
        publish(
            LibraryMutationPublication(
                null, null, errorMessage, scanErrors = null))
    } else {
        publishLibraryContentAfterReconcileFailureSafe(
            reconciler = reconciler,
            content = requireNotNull(content),
            ownerIsActive = ownerIsActive,
            playlistStateOwner = playlistStateOwner,
            initialError = errorMessage,
            publish = publish,
        )
    }
}

internal suspend fun clearLibraryInBackground(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val content =
        withContext(ioDispatcher) {
            val sources = repository.sources()
            repository.clearAll()
            sources.forEach(platformAccess::releaseAccess)
            loadLibraryContent(repository, platformAccess)
        }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        playlistStateOwner = playlistStateOwner,
        publish = publish,
    )
}

private suspend fun publishLibraryContentAfterReconcileFailureSafe(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    ownerIsActive: suspend () -> Boolean,
    playlistStateOwner: PlaylistStateOwner,
    initialError: String? = null,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    suspend fun publishAuthoritativeContent(errorMessage: String? = null) {
        val playlists =
            playlistStateOwner.refresh().requireSuccessfulPublication()
        publish(LibraryMutationPublication(content, playlists, errorMessage))
    }
    val reconciliationError =
        try {
            reconciler.reconcile(content.tracks)
            null
        } catch (cancelled: CancellationException) {
            if (ownerIsActive()) {
                withContext(NonCancellable) {
                    runCatching {
                        publishAuthoritativeContent(
                            cancelled.appFailureMessage())
                    }
                }
            }
            throw cancelled
        } catch (failure: Throwable) {
            failure.appFailureMessage()
        }
    publishAuthoritativeContent(reconciliationError ?: initialError)
}

internal fun removeMissingTracksRejectionMessage(
    reason: RemoveMissingTracksRejectionReason,
): String = "Unable to remove missing tracks: ${reason.name}"

/**
 * App effects resolved from one terminal folder-pick/import result.
 *
 * @property message transient import message to publish, or null to leave the
 *   current message unchanged (plain folder-pick successes and silent
 *   cancellations publish nothing).
 * @property scanSource normalized source to scan, or null when no scan may
 *   start (cancellation, unavailable, and failure never scan).
 * @property holdsFollowUpScanGate true only for a successful iOS import
 *   terminal (one carrying an import summary): its scan is a follow-up scan
 *   whose admission must release the App source-mutation exclusion, so App
 *   holds the gate from the terminal until the coordinator settles it.
 */
internal data class LibraryPickerTerminalAction(
    val message: String?,
    val scanSource: LibrarySource?,
    val holdsFollowUpScanGate: Boolean = false,
)

/**
 * Resolves one terminal picker result into App effects.
 *
 * A successful iOS import publishes its aggregate count summary and requests
 * exactly one scan of the normalized managed app-local source. Folder-pick
 * successes without a summary (Android/JVM) keep the legacy behavior: scan
 * without touching the transient message. Cancellation is silent and never
 * scans; unavailable and failure publish their message and never scan.
 *
 * @param result the terminal picker result.
 * @param existingSources the currently configured library sources, used to
 *   normalize the picked source identity.
 */
internal fun resolveLibraryPickerTerminal(
    result: PlatformFolderPickResult,
    existingSources: List<LibrarySource>,
    importSummaryFormat: String =
        "Imported %1\$d, duplicates %2\$d, unsupported %3\$d, failed %4\$d",
): LibraryPickerTerminalAction =
    when (result) {
        is PlatformFolderPickResult.Success ->
            LibraryPickerTerminalAction(
                message =
                    result.importSummary?.let {
                        iosImportSummaryMessage(it, importSummaryFormat)
                    },
                scanSource =
                    normalizePickedSource(result.source, existingSources),
                holdsFollowUpScanGate = result.importSummary != null,
            )

        is PlatformFolderPickResult.Cancelled ->
            LibraryPickerTerminalAction(message = null, scanSource = null)

        is PlatformFolderPickResult.Unavailable ->
            LibraryPickerTerminalAction(
                message = result.message,
                scanSource = null,
            )

        is PlatformFolderPickResult.Failure ->
            LibraryPickerTerminalAction(
                message = result.message,
                scanSource = null,
            )
    }

/**
 * App-owned source-mutation gating.
 *
 * Mutations require the initial publication to allow them, the operation
 * coordinator to be idle, and no platform import to be active. Folding the iOS
 * import-active window into this existing gate blocks competing source
 * mutations while the picker is shown or files are being copied, before the
 * follow-up scan is even admitted; the follow-up pending flag keeps the
 * exclusion in force after the import terminal clears the import-active flag
 * and until exactly that follow-up scan is admitted or rejected by the
 * coordinator. Android and JVM launchers report their import as never active
 * and never hold a follow-up gate, so their gating is unchanged.
 *
 * @param publicationMutationsAllowed whether the initial library publication
 *   permits mutations.
 * @param coordinatorIdle whether the operation coordinator is idle.
 * @param importActive whether a platform import operation is active.
 * @param followUpScanPending whether a successful import's follow-up scan has
 *   not yet been conclusively admitted or rejected by the coordinator.
 */
internal fun appLibraryMutationsEnabled(
    publicationMutationsAllowed: Boolean,
    coordinatorIdle: Boolean,
    importActive: Boolean,
    followUpScanPending: Boolean = false,
): Boolean =
    publicationMutationsAllowed &&
        coordinatorIdle &&
        !importActive &&
        !followUpScanPending

/**
 * Runs one follow-up scan admission and guarantees the pending exclusion is
 * released once that admission has conclusively settled.
 *
 * [AppLibraryOrchestrator.launchScan] completes its coordinator token and then
 * rethrows a CancellationException when the scan is cancelled, so a plain
 * statement after the call would leak the pending gate forever on that path.
 * The release therefore runs in a non-cancellable finally; the original
 * cancellation keeps propagating, so a cancelled follow-up stays silent and is
 * never retried.
 *
 * @param scan the actual follow-up scan launch through the App-owned
 *   coordinator admission flow.
 * @param releasePending clears the App follow-up pending state.
 */
internal suspend fun settleFollowUpScanPending(
    scan: suspend () -> Unit,
    releasePending: () -> Unit,
) {
    try {
        scan()
    } finally {
        withContext(NonCancellable) {
            releasePending()
        }
    }
}

internal fun ScanProgress?.requestScanCancellation(): ScanProgress? {
    val session = this?.session ?: return this
    if (session.status != ScanStatus.Scanning) return this
    return copy(session = session.copy(status = ScanStatus.Cancelling))
}

internal fun restoredTerminalScanProgress(
    session: ScanSession?,
    sources: List<LibrarySource>,
): ScanProgress? =
    session
        ?.takeIf {
            it.status in
                setOf(
                    ScanStatus.Completed,
                    ScanStatus.Cancelled,
                    ScanStatus.Failed)
        }
        ?.takeIf { terminal -> sources.any { it.id == terminal.sourceId } }
        ?.let(::ScanProgress)

internal data class RestoredTerminalScanState(
    val progress: ScanProgress?,
    val errors: List<ScanError>,
)

internal fun restoredTerminalScanState(
    session: ScanSession?,
    sources: List<LibrarySource>,
    errors: List<ScanError>,
): RestoredTerminalScanState {
    val progress = restoredTerminalScanProgress(session, sources)
    return RestoredTerminalScanState(
        progress = progress,
        errors =
            if (progress?.session?.id == session?.id) errors else emptyList(),
    )
}

internal fun removeMissingTracksPublication(
    result: RemoveMissingTracksResult,
    content: LibraryContentState,
): LibraryMutationPublication =
    when (result) {
        is RemoveMissingTracksResult.Removed ->
            LibraryMutationPublication(content, null, null)
        is RemoveMissingTracksResult.Rejected ->
            LibraryMutationPublication(
                content = null,
                playlists = null,
                errorMessage =
                    removeMissingTracksRejectionMessage(result.reason),
                scanErrors = null,
            )
    }

internal fun Throwable.appFailureMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "Playback session unavailable"

private fun ScanSession.terminalAfterCancellation(): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Cancelled, terminalMessage = "Scan cancelled")
    } else {
        this
    }

private fun ScanSession.terminalAfterFailure(failure: Throwable): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(
            status = ScanStatus.Failed,
            terminalMessage = failure.appFailureMessage())
    } else {
        this
    }

@Composable
private fun RhythHausTheme(
    selectedThemeMode: RhythHausThemeMode,
    content: @Composable () -> Unit,
) {
    val colors =
        resolveHausPalette(
            mode = selectedThemeMode,
            systemIsDark = systemPrefersDarkTheme(),
        )
    val colorScheme =
        if (colors == DarkHausPalette) {
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
