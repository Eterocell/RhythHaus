package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.ui.DesktopDropTargetPresentation
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
import com.eterocell.rhythhaus.library.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionController
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionController
import com.eterocell.rhythhaus.onboarding.OnboardingPreferenceStore
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.settings.SettingsAppearanceState
import com.eterocell.rhythhaus.settings.deviceAudioSourceDisplayName
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.ThemePreferenceOwner
import com.eterocell.rhythhaus.theme.ThemePreferenceState
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.isPlatformDynamicColorAvailable
import com.eterocell.rhythhaus.theme.platformDynamicColorRoles
import com.eterocell.rhythhaus.theme.resolveIsDark
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.desktop_drop_added_format
import rhythhaus.shared.generated.resources.desktop_drop_busy
import rhythhaus.shared.generated.resources.desktop_drop_duplicate
import rhythhaus.shared.generated.resources.desktop_drop_failed_format
import rhythhaus.shared.generated.resources.desktop_drop_rejected
import rhythhaus.shared.generated.resources.desktop_drop_summary_format
import rhythhaus.shared.generated.resources.ios_import_summary_format
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.scan_complete_format

internal data class AppLibraryServices(
    val repository: LibraryRepository,
    val playlistRepository: PlaylistRepository,
    val platformAccess: PlatformSourceAccess,
    val scanner: LibraryScanner,
    val tagLibReader: TagLibReader,
    val processState: AppProcessState,
    val playbackReconciler: PlaybackSessionReconciler,
    val playlistStateOwner: PlaylistStateOwner,
)

internal data class AppPlaybackServices(
    val controller: PlaybackController,
    val outputStatusProvider: PlaybackOutputStatusProvider?,
    val replayGainPreferenceOwner:
        com.eterocell.rhythhaus.playback.ReplayGainPreferenceOwner,
    val crossfadePreferenceOwner:
        com.eterocell.rhythhaus.playback.CrossfadePreferenceOwner,
    val gaplessPreferenceOwner:
        com.eterocell.rhythhaus.playback.GaplessPreferenceOwner,
    val automaticResumePreferenceOwner:
        com.eterocell.rhythhaus.playback.AutomaticResumePreferenceOwner,
)

internal data class AppPreferenceServices(
    val themePreferenceStore: ThemePreferenceStore,
    val themePreferenceOwner: ThemePreferenceOwner,
    val onboardingPreferenceStore: OnboardingPreferenceStore,
)

internal data class AppPlatformServices(
    val notificationPermissionController: MediaNotificationPermissionController,
    val mediaStorePermissionController: MediaStoreAudioPermissionController,
    val desktopDropEvents: Flow<List<String>>?,
    val desktopDropActive: Boolean,
    val onDesktopDropAvailabilityChanged: (Boolean) -> Unit,
)

@Composable
internal fun AppLibraryContentHost(
    library: AppLibraryServices,
    playback: AppPlaybackServices,
    preferences: AppPreferenceServices,
    platform: AppPlatformServices,
) {
    val repository = library.repository
    val playlistRepository = library.playlistRepository
    val platformAccess = library.platformAccess
    val scanner = library.scanner
    val tagLibReader = library.tagLibReader
    val processState = library.processState
    val playbackReconciler = library.playbackReconciler
    val playlistStateOwner = library.playlistStateOwner
    val controller = playback.controller
    val outputStatusProvider = playback.outputStatusProvider
    val replayGainPreferenceOwner = playback.replayGainPreferenceOwner
    val crossfadePreferenceOwner = playback.crossfadePreferenceOwner
    val gaplessPreferenceOwner = playback.gaplessPreferenceOwner
    val automaticResumePreferenceOwner = playback.automaticResumePreferenceOwner
    val themePreferenceStore = preferences.themePreferenceStore
    val themePreferenceOwner = preferences.themePreferenceOwner
    val onboardingPreferenceStore = preferences.onboardingPreferenceStore
    val notificationPermissionController =
        platform.notificationPermissionController
    val mediaStorePermissionController = platform.mediaStorePermissionController
    val desktopDropEvents = platform.desktopDropEvents
    val desktopDropActive = platform.desktopDropActive
    val onDesktopDropAvailabilityChanged =
        platform.onDesktopDropAvailabilityChanged
    var desktopModalFree by remember { mutableStateOf(false) }
    val outputStatus by
        outputStatusProvider
            ?.outputStatus
            ?.collectAsState(PlaybackOutputStatus.Unavailable)
            ?: remember { mutableStateOf(PlaybackOutputStatus.Unavailable) }
    val themePreferenceWriteState by themePreferenceOwner.state.collectAsState()
    val replayGainState by replayGainPreferenceOwner.state.collectAsState()
    val crossfadeState by crossfadePreferenceOwner.state.collectAsState()
    val gaplessState by gaplessPreferenceOwner.state.collectAsState()
    val automaticResumeState by
        automaticResumePreferenceOwner.state.collectAsState()
    val libraryPublicationOwner = processState.publications
    val initialPublication by processState.initial.collectAsState()
    val appLibraryContentState = remember { AppLibraryContentState() }
    val deviceAudioLabel = deviceAudioSourceDisplayName()
    val publishedLibraryContent = appLibraryContentState.content
    val libraryContent =
        remember(publishedLibraryContent, deviceAudioLabel) {
            if (publishedLibraryContent.sources.none {
                it.platformKind == LibraryPlatformKind.AndroidMediaStoreAudio &&
                    it.displayName != deviceAudioLabel
            })
                publishedLibraryContent
            else
                publishedLibraryContent.copy(
                    sources =
                        publishedLibraryContent.sources.map { source ->
                            if (source.platformKind ==
                                LibraryPlatformKind.AndroidMediaStoreAudio)
                                source.copy(displayName = deviceAudioLabel)
                            else source
                        })
        }
    var libraryRevision by remember { mutableStateOf(0L) }
    var playlistState by remember {
        mutableStateOf(PlaylistState(isLoading = true))
    }
    val scope = rememberCoroutineScope()
    val scanCompleteFormat = stringResource(Res.string.scan_complete_format)
    val desktopDropAddedFormat =
        stringResource(Res.string.desktop_drop_added_format)
    val desktopDropBusy = stringResource(Res.string.desktop_drop_busy)
    val desktopDropFailedFormat =
        stringResource(Res.string.desktop_drop_failed_format)
    val desktopDropDuplicate = stringResource(Res.string.desktop_drop_duplicate)
    val desktopDropRejected = stringResource(Res.string.desktop_drop_rejected)
    val desktopDropSummaryFormat =
        stringResource(Res.string.desktop_drop_summary_format)
    val iosImportSummaryFormat =
        stringResource(Res.string.ios_import_summary_format)
    val themePreferenceState by
        themePreferenceStore.state.collectAsState(
            ThemePreferenceState(
                mode = RhythHausThemeMode.System,
                paletteId = HausPaletteId.Moss,
                dynamicColorEnabled = false,
            ),
        )
    val dynamicColorAvailable = isPlatformDynamicColorAvailable()
    val systemIsDark = systemPrefersDarkTheme()
    val dynamicColorRequested =
        themePreferenceState.dynamicColorEnabled && dynamicColorAvailable
    val dynamicColorRoles =
        if (dynamicColorRequested) {
            platformDynamicColorRoles(
                themePreferenceState.mode.resolveIsDark(systemIsDark),
            )
        } else {
            null
        }
    val resolvedTheme =
        resolveSharedHausTheme(
            preferences = themePreferenceState,
            systemIsDark = systemIsDark,
            dynamicColorAvailable = dynamicColorAvailable,
            dynamicRoles = dynamicColorRoles,
        )
    val settingsAppearance =
        SettingsAppearanceState(
            themeMode = themePreferenceState.mode,
            paletteId = themePreferenceState.paletteId,
            isDark = resolvedTheme.isDark,
            dynamicColorAvailable = dynamicColorAvailable,
            dynamicColorEnabled = themePreferenceState.dynamicColorEnabled,
            colorSource = resolvedTheme.source,
            isSaving = themePreferenceWriteState.isBusy,
            saveFailed = themePreferenceWriteState.saveFailed,
        )
    val notificationPermissionState by
        notificationPermissionController.state.collectAsState()
    val mediaStorePermissionState by
        mediaStorePermissionController.state.collectAsState()
    suspend fun applyLibraryPublication(
        publication: AuthoritativeLibraryPublication,
    ): Boolean =
        withContext(Dispatchers.Main) {
            val applied = appLibraryContentState.apply(publication)
            if (applied) {
                libraryRevision = publication.revision
            }
            applied
        }
    suspend fun updateLibraryContent(content: LibraryContentState) {
        val publication =
            withContext(Dispatchers.Default) {
                libraryPublicationOwner.publishWithFavoriteReconciliation(
                    content = content,
                    favoriteTrackIds = {
                        repository.favoriteTrackIds().toSet()
                    },
                    playHistory = { repository.playHistory().toMap() },
                    createdAtByTrackId = {
                        repository.tracks().associate { track ->
                            track.id to track.createdAtEpochMillis
                        }
                    },
                    freshContent = {
                        loadLibraryContent(repository, platformAccess)
                    },
                )
            }
        applyLibraryPublication(publication)
    }

    val currentContent = rememberUpdatedState(libraryContent)
    val currentUpdate =
        rememberUpdatedState<suspend (LibraryContentState) -> Unit>(
            ::updateLibraryContent)
    val currentOperationLabels =
        rememberUpdatedState(
            AppLibraryOperationLabels(
                scanCompleteFormat = scanCompleteFormat,
                desktopDropAddedFormat = desktopDropAddedFormat,
                desktopDropBusy = desktopDropBusy,
                desktopDropFailedFormat = desktopDropFailedFormat,
                desktopDropDuplicate = desktopDropDuplicate,
                desktopDropRejected = desktopDropRejected,
                desktopDropSummaryFormat = desktopDropSummaryFormat,
                iosImportSummaryFormat = iosImportSummaryFormat,
            ))
    val operations = remember {
        AppLibraryOperationsHost(
            library = library,
            scope = scope,
            currentContent = { currentContent.value },
            currentInitial = { initialPublication },
            currentPlaylist = { playlistState },
            publishPlaylist = { playlistState = it },
            updateLibraryContent = { currentUpdate.value(it) },
            labels = { currentOperationLabels.value },
            isDesktopModalFree = { desktopModalFree },
            mediaStorePermissionController = mediaStorePermissionController,
        )
    }
    val importMessage = operations.importMessage
    val scanProgress = operations.scanProgress
    val latestCompletedScanSession = operations.latestCompletedScanSession
    val scanErrors = operations.scanErrors
    val scanJob = operations.scanJob
    val followUpScanPending = operations.followUpScanPending
    val libraryOrchestrator = operations.libraryOrchestrator

    LaunchedEffect(processState) {
        libraryPublicationOwner.publications.collect { publication ->
            publication?.let { applyLibraryPublication(it) }
        }
    }

    LaunchedEffect(controller) {
        outputStatusProvider?.refreshOutputStatus()
        processState.historyFailure.collect { message ->
            message?.let(operations::mutationError)
        }
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

    LaunchedEffect(initialPublication.isReady) {
        operations.restoreInitialLibrary()
    }

    LaunchedEffect(
        mediaStorePermissionState, initialPublication.mutationsAllowed) {
            if (initialPublication.mutationsAllowed &&
                libraryContent.sources.any {
                    it.platformKind ==
                        LibraryPlatformKind.AndroidMediaStoreAudio
                }) {
                updateLibraryContent(
                    loadLibraryContent(repository, platformAccess))
            }
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

    val playlistBackup =
        rememberAppPlaylistBackupHost(
            playlistStateOwner = playlistStateOwner,
            libraryPublicationOwner = libraryPublicationOwner,
            content = libraryContent,
            playlists = playlistState,
            revision = libraryRevision,
            scope = scope,
            onPlaylistStateAction = { action ->
                playlistState = reducePlaylistState(playlistState, action)
            },
        )

    val folderPickerLauncher =
        rememberPlatformFolderPickerLauncher(
            onResult = operations::receivePickerResult,
            onRecoveryResult = operations::recoverSource,
        )
    operations.folderPickerLauncher = folderPickerLauncher
    // The coordinator state and mutation gate are collected after the picker
    // launcher so the iOS import-active window can fold into the same gate
    // that source mutations already use: while the picker is shown or files
    // are being copied, competing mutations stay disabled even before the
    // follow-up scan is admitted.
    val operationState by operations.operationCoordinator.state.collectAsState()
    val mutationsEnabled =
        appLibraryMutationsEnabled(
            publicationMutationsAllowed = initialPublication.mutationsAllowed,
            coordinatorIdle = operationState is LibraryOperationState.Idle,
            importActive = folderPickerLauncher.isImportActive,
            followUpScanPending = followUpScanPending,
        )

    val currentDesktopDropHandler =
        rememberUpdatedState<suspend (List<String>) -> Unit>(
            newValue = operations::handleDesktopDrop,
        )
    LaunchedEffect(desktopDropEvents) {
        desktopDropEvents?.collect { paths ->
            currentDesktopDropHandler.value(paths)
        }
    }
    val snapshot =
        remember(libraryContent.tracks) {
            librarySnapshot(libraryContent.tracks)
        }
    RhythHausTheme(resolved = resolvedTheme) {
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
                    libraryTracks = libraryContent.tracks,
                    tagLibReader = tagLibReader,
                    playbackController = controller,
                    playlistRepository = playlistRepository,
                    playlistState = playlistState,
                    playlistBackupState = playlistBackup.state,
                    backupDocumentAvailable = playlistBackup.documentAvailable,
                    onPlaylistStateAction = { action ->
                        playlistState =
                            reducePlaylistState(playlistState, action)
                    },
                    onRefreshPlaylists = ::refreshPlaylists,
                    onPlaylistMutation = ::launchPlaylistMutation,
                    onExportPlaylists = playlistBackup.exportPlaylists,
                    onOpenPlaylistBackup = playlistBackup.openBackup,
                    onExportPlaylistFormat = playlistBackup.exportDocument,
                    onOpenPlaylistFormat = playlistBackup.openDocument,
                    onConfirmPlaylistBackup = playlistBackup.confirm,
                    onPlaylistBackupAction = playlistBackup.dispatch,
                    sources = libraryContent.sources,
                    folderPickerLauncher = folderPickerLauncher,
                    sourcePickerActionVisible =
                        sourcePickerActionVisible(
                            supportsAdditionalSources =
                                folderPickerLauncher.supportsAdditionalSources,
                            sourceCount = libraryContent.sources.size,
                        ),
                    importMessage = importMessage,
                    scanProgress = scanProgress,
                    latestCompletedScanSession = latestCompletedScanSession,
                    scanErrors = scanErrors,
                    scanJob = scanJob,
                    mediaNotificationPermission = notificationPermissionState,
                    mediaStorePermission = mediaStorePermissionState,
                    onRequestMediaStoreAudio = {
                        if (mutationsEnabled)
                            mediaStorePermissionController.requestPermission()
                    },
                    onOpenMediaStoreSettings =
                        mediaStorePermissionController::openSettings,
                    onAddMediaStoreAudio = operations::addMediaStoreAudio,
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
                    favoriteTrackIds = libraryContent.favoriteTrackIds,
                    playHistory = libraryContent.playHistory,
                    createdAtByTrackId = libraryContent.createdAtByTrackId,
                    sourceIdByTrackId = libraryContent.sourceIdByTrackId,
                    modifiedAtByTrackId = libraryContent.modifiedAtByTrackId,
                    artworkTrackIds = libraryContent.artworkTrackIds,
                    onLoadTrackMetadata = { trackId ->
                        withContext(Dispatchers.Default) {
                            repository.metadataForTrack(trackId)
                        }
                    },
                    onSetTrackMetadataOverride = { trackId, override ->
                        setTrackMetadataOverrideAndPublish(
                            owner = libraryPublicationOwner,
                            repository = repository,
                            platformAccess = platformAccess,
                            trackId = trackId,
                            override = override,
                            ioDispatcher = Dispatchers.Default,
                            publish = { publication ->
                                applyLibraryPublication(publication)
                            },
                        )
                    },
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
                    desktopDropTarget =
                        DesktopDropTargetPresentation(
                            isAvailable = desktopDropEvents != null,
                            isActive =
                                desktopDropEvents != null && desktopDropActive,
                            feedback = importMessage,
                        ),
                    onDesktopDropAvailabilityChanged = { modalFree ->
                        desktopModalFree = modalFree
                        onDesktopDropAvailabilityChanged(
                            modalFree && mutationsEnabled)
                    },
                    coordinatorMutationsEnabled = mutationsEnabled,
                    settingsAppearance = settingsAppearance,
                    replayGainState = replayGainState,
                    onReplayGainModeSelected = { mode ->
                        scope.launch { replayGainPreferenceOwner.select(mode) }
                    },
                    crossfadeState = crossfadeState,
                    gaplessState = gaplessState,
                    automaticResumeState = automaticResumeState,
                    outputStatus = outputStatus,
                    onGaplessModeSelected = { mode ->
                        scope.launch { gaplessPreferenceOwner.select(mode) }
                    },
                    onAutomaticResumePolicySelected = { policy ->
                        scope.launch {
                            automaticResumePreferenceOwner.select(policy)
                        }
                    },
                    onCrossfadeDurationSelected = { duration ->
                        scope.launch {
                            crossfadePreferenceOwner.select(duration)
                        }
                    },
                    onThemeModeSelected = { mode ->
                        scope.launch {
                            themePreferenceOwner.selectMode(mode)
                        }
                    },
                    onPaletteSelected = { paletteId ->
                        scope.launch {
                            themePreferenceOwner.selectPalette(paletteId)
                        }
                    },
                    onDynamicColorSelected = { enabled ->
                        scope.launch {
                            themePreferenceOwner.selectDynamicColor(enabled)
                        }
                    },
                    onRetryAppearanceSave = {
                        scope.launch { themePreferenceOwner.retry() }
                    },
                    onClearLibrary = operations::clearLibrary,
                    onRescanSource = { displayedSource ->
                        publishedLibraryContent.sources
                            .firstOrNull { it.id == displayedSource.id }
                            ?.let(operations::launchSourceScan)
                    },
                    onRecoverSource = { source ->
                        folderPickerLauncher.launchForRecovery(source.id)
                    },
                    onRemoveSource = operations::removeSource,
                    onRemoveMissingTracks = operations::removeMissingTracks,
                    onCancelScan = operations::cancelScan,
                )
            }
        }
    }
}
