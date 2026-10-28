package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.CrossfadeDuration
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.selectDynamicOccurrenceForPlayback
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.library.selectOccurrenceForPlayback
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState
import com.eterocell.rhythhaus.onboarding.OnboardingScreen
import com.eterocell.rhythhaus.onboarding.currentOnboardingPlatform
import com.eterocell.rhythhaus.onboarding.toGuidance
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsHost
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsLabels
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsSection
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.PlaylistDocumentFormat
import com.eterocell.rhythhaus.search.SearchContent
import com.eterocell.rhythhaus.search.SearchSharedLabels
import com.eterocell.rhythhaus.settings.MediaNotificationRecovery
import com.eterocell.rhythhaus.settings.MediaStoreAudioRecovery
import com.eterocell.rhythhaus.settings.OpenSourceLibrariesScreen
import com.eterocell.rhythhaus.settings.SettingsAboutScreen
import com.eterocell.rhythhaus.settings.SettingsCrossfadeDuration
import com.eterocell.rhythhaus.settings.SettingsGaplessMode
import com.eterocell.rhythhaus.settings.SettingsScreen
import com.eterocell.rhythhaus.settings.SettingsSharedLabels
import com.eterocell.rhythhaus.settings.SettingsSourceItem
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.HausButton as Button
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.close
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.output_status_title
import rhythhaus.shared.generated.resources.output_status_unavailable
import rhythhaus.shared.generated.resources.playlist_changed
import rhythhaus.shared.generated.resources.playlist_load_failed
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.playlist_mutation_failed
import rhythhaus.shared.generated.resources.playlist_retry
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.remove
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.settings_output_session
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

private data class RemoveMissingTracksConfirmation(
    val source: LibrarySource,
    val session: ScanSession,
)

/**
 * Projects the shared media-notification permission state onto the Settings
 * recovery surface. Only the two actionable denied states render a card;
 * unavailable and granted permission stay absent so no card, action, or
 * accessibility node is composed on non-Android or already-granted hosts.
 */
internal fun MediaNotificationPermissionState.toSettingsNotificationRecovery():
    MediaNotificationRecovery? =
    when (this) {
        MediaNotificationPermissionState.Unavailable -> null
        MediaNotificationPermissionState.Granted -> null
        MediaNotificationPermissionState.Requestable ->
            MediaNotificationRecovery.Requestable
        MediaNotificationPermissionState.SettingsRequired ->
            MediaNotificationRecovery.SettingsRequired
    }

@Composable
internal fun LibraryRouteScreen(
    route: LibraryRoute,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    playlistRepository: PlaylistRepository,
    playlistState: PlaylistState,
    playlistBackupState: PlaylistBackupUiState,
    backupDocumentAvailable: Boolean,
    backupHostActive: Boolean = true,
    interactive: Boolean = true,
    destinationId: LibraryDestinationId,
    playlistAppearanceSource: PlaylistFeatureAppearanceSource,
    registerBackSurface: (LibraryBackSurfacePort) -> () -> Unit,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
    onExportPlaylists: () -> Unit,
    onOpenPlaylistBackup: () -> Unit,
    onExportPlaylistFormat: ((String, PlaylistDocumentFormat) -> Unit)? = null,
    onOpenPlaylistFormat: ((PlaylistDocumentFormat) -> Unit)? = null,
    onConfirmPlaylistBackup: () -> Unit,
    onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    latestCompletedScanSession: ScanSession?,
    scanErrors: List<ScanError> = emptyList(),
    scanJob: Job?,
    mutationsEnabled: Boolean = true,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    replayGainState: com.eterocell.rhythhaus.playback.ReplayGainPreferenceState,
    onReplayGainModeSelected: (com.eterocell.rhythhaus.ReplayGainMode) -> Unit,
    crossfadeState: com.eterocell.rhythhaus.playback.CrossfadePreferenceState,
    onCrossfadeDurationSelected: (CrossfadeDuration) -> Unit,
    gaplessState: com.eterocell.rhythhaus.playback.GaplessPreferenceState =
        com.eterocell.rhythhaus.playback.GaplessPreferenceState(isBusy = false),
    onGaplessModeSelected:
        (com.eterocell.rhythhaus.playback.GaplessPlaybackMode) -> Unit =
        {},
    automaticResumeState:
        com.eterocell.rhythhaus.playback.AutomaticResumePreferenceState =
        com.eterocell.rhythhaus.playback.AutomaticResumePreferenceState(
            isBusy = false),
    onAutomaticResumePolicySelected:
        (com.eterocell.rhythhaus.playback.AutomaticResumePolicy) -> Unit =
        {},
    outputStatus: com.eterocell.rhythhaus.PlaybackOutputStatus =
        com.eterocell.rhythhaus.PlaybackOutputStatus.Unavailable,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRecoverSource: (LibrarySource) -> Unit = {
        folderPickerLauncher.launchForRecovery(it.id)
    },
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit = { _, _ -> },
    onRemoveSource: (LibrarySource) -> Unit,
    onCancelScan: () -> Unit,
    mediaNotificationPermission: MediaNotificationPermissionState =
        MediaNotificationPermissionState.Unavailable,
    onRequestNotificationPermission: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    mediaStorePermission: MediaStoreAudioPermissionState =
        MediaStoreAudioPermissionState.Unavailable,
    onRequestMediaStoreAudio: () -> Unit = {},
    onOpenMediaStoreSettings: () -> Unit = {},
    onAddMediaStoreAudio: () -> Unit = {},
    settingsListState: LazyListState? = null,
    settingsReportVisible: Boolean? = null,
    onSettingsReportVisibleChanged: (Boolean) -> Unit = {},
    onSettingsExclusiveModalChanged: (Boolean) -> Unit = {},
    onboardingSaving: Boolean = false,
    onboardingCompletionError: String? = null,
    onCompleteOnboarding: () -> Unit = {},
    onReplaceTop: (LibraryRoute) -> Unit = {},
    onRejectInvalidOnboarding: () -> Unit = {},
    onCloseOnboarding: (() -> Unit)? = null,
    pushRoute: (LibraryRoute) -> Unit,
    onShowSettingsAbout: () -> Unit,
    onShowOpenSourceLibraries: () -> Unit,
    onDismiss: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    searchFirstVisibleItemIndex: Int = 0,
    searchFirstVisibleItemScrollOffset: Int = 0,
    onEditTrackMetadata: ((String) -> Unit)? = null,
) {
    when (route) {
        LibraryRoute.Settings -> {
            var clearLibraryConfirmation by
                remember(destinationId, playlistAppearanceSource) {
                    mutableStateOf<Any?>(null)
                }
            var clearLibraryConfirmationInFlight by
                remember(destinationId, playlistAppearanceSource) {
                    mutableStateOf(false)
                }
            var removeMissingConfirmation by
                remember(destinationId, playlistAppearanceSource) {
                    mutableStateOf<RemoveMissingTracksConfirmation?>(null)
                }
            var removeMissingConfirmationInFlight by
                remember(destinationId, playlistAppearanceSource) {
                    mutableStateOf(false)
                }
            val currentMutationsEnabled by
                rememberUpdatedState(mutationsEnabled)
            val currentTrackCount by rememberUpdatedState(snapshot.tracks.size)
            val currentSourceCount by rememberUpdatedState(sources.size)
            val currentClearLibrary by rememberUpdatedState(onClearLibrary)
            val currentRemoveMissingTracks by
                rememberUpdatedState(onRemoveMissingTracks)
            val currentSources by rememberUpdatedState(sources)
            val currentLatestCompletedScanSession by
                rememberUpdatedState(latestCompletedScanSession)
            fun eligibleRemoveMissingSource(
                sourceId: String,
                session: ScanSession,
            ): LibrarySource? {
                if (session.status != ScanStatus.Completed ||
                    session.sourceId != sourceId ||
                    currentLatestCompletedScanSession?.id != session.id) {
                    return null
                }
                return currentSources.firstOrNull { it.id == sourceId }
            }
            val sourceTrackCounts =
                remember(libraryTracks) {
                    libraryTracks.groupingBy(LibraryTrack::sourceId).eachCount()
                }
            val clearConfirmation = clearLibraryConfirmation
            val pendingRemoveMissingConfirmation = removeMissingConfirmation
            var localReportVisible by
                remember(scanProgress?.session?.id) { mutableStateOf(false) }
            val reportVisible = settingsReportVisible ?: localReportVisible
            val terminalSession =
                scanProgress?.session?.takeIf { session ->
                    session.status in
                        setOf(
                            ScanStatus.Completed,
                            ScanStatus.Failed,
                            ScanStatus.Cancelled,
                        )
                }
            val scanOutcomeContent: (@Composable () -> Unit)? =
                if (terminalSession != null) {
                    {
                        ScanOutcomePanel(
                            session = terminalSession,
                            completedSummarySession =
                                latestCompletedScanSession?.takeIf {
                                    it.id != terminalSession.id
                                },
                            completedSummarySource =
                                latestCompletedScanSession
                                    ?.takeIf { it.id != terminalSession.id }
                                    ?.let { summary ->
                                        sources.firstOrNull {
                                            it.id == summary.sourceId
                                        }
                                    },
                            source =
                                sources.firstOrNull {
                                    it.id == terminalSession.sourceId
                                },
                            errors = scanErrors,
                            reportVisible = reportVisible,
                            mutationsEnabled = mutationsEnabled,
                            onToggleReport = {
                                val next = !reportVisible
                                if (settingsReportVisible == null) {
                                    localReportVisible = next
                                } else {
                                    onSettingsReportVisibleChanged(next)
                                }
                            },
                            onRescanSource = onRescanSource,
                            onRemoveMissingTracks = { source, session ->
                                val eligibleSource =
                                    eligibleRemoveMissingSource(
                                        source.id,
                                        session,
                                    )
                                if (eligibleSource != null &&
                                    currentMutationsEnabled &&
                                    clearLibraryConfirmation == null &&
                                    removeMissingConfirmation == null) {
                                    removeMissingConfirmationInFlight = false
                                    removeMissingConfirmation =
                                        RemoveMissingTracksConfirmation(
                                            source = eligibleSource,
                                            session = session,
                                        )
                                }
                            },
                        )
                    }
                } else {
                    null
                }
            SettingsScreen(
                labels =
                    SettingsSharedLabels(
                        title = stringResource(Res.string.settings),
                        addMusicFolder =
                            stringResource(Res.string.add_music_folder),
                        folderPickerUnavailable =
                            stringResource(
                                Res.string.folder_picker_unavailable),
                        clearLibrary = stringResource(Res.string.clear_library),
                        cancel = stringResource(Res.string.cancel),
                        remove = stringResource(Res.string.remove),
                        outputSettings =
                            stringResource(Res.string.settings_output_session),
                        outputStatusTitle =
                            stringResource(Res.string.output_status_title),
                        outputStatusUnavailable =
                            stringResource(
                                Res.string.output_status_unavailable),
                    ),
                currentThemeMode = currentThemeMode,
                replayGainMode =
                    com.eterocell.rhythhaus.settings.SettingsReplayGainMode
                        .valueOf(replayGainState.selectedMode.name),
                replayGainBusy = replayGainState.isBusy,
                replayGainError = replayGainState.saveFailed,
                onReplayGainModeSelected = { mode ->
                    onReplayGainModeSelected(
                        com.eterocell.rhythhaus.ReplayGainMode.valueOf(
                            mode.name))
                },
                crossfadeDuration =
                    when (crossfadeState.selectedDuration) {
                        CrossfadeDuration.Off -> SettingsCrossfadeDuration.Off
                        CrossfadeDuration.Seconds3 ->
                            SettingsCrossfadeDuration.Seconds3
                        CrossfadeDuration.Seconds5 ->
                            SettingsCrossfadeDuration.Seconds5
                        CrossfadeDuration.Seconds8 ->
                            SettingsCrossfadeDuration.Seconds8
                    },
                crossfadeBusy = crossfadeState.isBusy,
                crossfadeError = crossfadeState.saveFailed,
                gaplessMode =
                    if (gaplessState.selectedMode ==
                        com.eterocell.rhythhaus.playback.GaplessPlaybackMode.On)
                        SettingsGaplessMode.On
                    else SettingsGaplessMode.Off,
                gaplessBusy = gaplessState.isBusy,
                gaplessError = gaplessState.saveFailed,
                onGaplessModeSelected = { mode ->
                    onGaplessModeSelected(
                        if (mode == SettingsGaplessMode.On)
                            com.eterocell.rhythhaus.playback.GaplessPlaybackMode
                                .On
                        else
                            com.eterocell.rhythhaus.playback.GaplessPlaybackMode
                                .Off)
                },
                automaticResumePolicy =
                    com.eterocell.rhythhaus.settings
                        .SettingsAutomaticResumePolicy
                        .valueOf(automaticResumeState.selectedPolicy.name),
                automaticResumeBusy = automaticResumeState.isBusy,
                automaticResumeError = automaticResumeState.saveFailed,
                outputStatus =
                    (outputStatus
                            as?
                            com.eterocell.rhythhaus.PlaybackOutputStatus.Available)
                        ?.names
                        ?.joinToString(),
                onAutomaticResumePolicySelected = { policy ->
                    onAutomaticResumePolicySelected(
                        com.eterocell.rhythhaus.playback.AutomaticResumePolicy
                            .valueOf(policy.name))
                },
                onCrossfadeDurationSelected = { duration ->
                    onCrossfadeDurationSelected(
                        when (duration) {
                            SettingsCrossfadeDuration.Off ->
                                CrossfadeDuration.Off
                            SettingsCrossfadeDuration.Seconds3 ->
                                CrossfadeDuration.Seconds3
                            SettingsCrossfadeDuration.Seconds5 ->
                                CrossfadeDuration.Seconds5
                            SettingsCrossfadeDuration.Seconds8 ->
                                CrossfadeDuration.Seconds8
                        },
                    )
                },
                sources =
                    sources.map {
                        SettingsSourceItem(
                            id = it.id,
                            displayName = it.displayName,
                            accessAvailable =
                                it.accessStatus ==
                                    com.eterocell.rhythhaus.library
                                        .LibrarySourceAccessStatus
                                        .Available,
                            hasBeenScanned = it.lastScanAtEpochMillis != null,
                            trackCount = sourceTrackCounts[it.id] ?: 0,
                        )
                    },
                sourcePickerActionVisible = sourcePickerActionVisible,
                sourcePickerAvailable = folderPickerLauncher.isAvailable,
                importMessage = importMessage,
                mutationsEnabled = mutationsEnabled,
                hasImportedTracks = snapshot.tracks.isNotEmpty(),
                playlistBackupContent = {
                    if (playlistState.hasConfirmedSnapshot) {
                        PlaylistBackupSettingsSection(
                            state = playlistBackupState,
                            launcherAvailable =
                                backupDocumentAvailable && backupHostActive,
                            labels =
                                PlaylistBackupSettingsLabels(
                                    cancel = stringResource(Res.string.cancel),
                                    close = stringResource(Res.string.close),
                                ),
                            onExport = onExportPlaylists,
                            onOpen = onOpenPlaylistBackup,
                            staticPlaylists =
                                playlistState.confirmedSnapshot.playlists,
                            onExportPlaylistFormat = onExportPlaylistFormat,
                            onOpenPlaylistFormat = onOpenPlaylistFormat,
                            onAction = onPlaylistBackupAction,
                        )
                    }
                },
                activeScanContent =
                    if (scanProgress?.isActive == true) {
                        {
                            val session = scanProgress.session!!
                            ScanningCard(
                                foldersVisited = session.foldersVisited,
                                filesVisited = session.filesVisited,
                                tracksAdded = session.tracksAdded,
                                latestItem = scanProgress.latestItem,
                                labels = librarySharedLabels(),
                                onCancel = onCancelScan,
                            )
                        }
                    } else {
                        null
                    },
                scanOutcomeContent = scanOutcomeContent,
                onExclusiveModalChanged = onSettingsExclusiveModalChanged,
                clearLibraryDialog =
                    if (clearConfirmation != null) {
                        {
                            AnimatedClearLibraryDialogRoute(
                                sourceCount = currentSourceCount,
                                trackCount = currentTrackCount,
                                confirmEnabled =
                                    clearLibraryConfirmation ===
                                        clearConfirmation &&
                                        !clearLibraryConfirmationInFlight &&
                                        currentMutationsEnabled &&
                                        (currentTrackCount > 0 ||
                                            currentSourceCount > 0),
                                onDismiss = {
                                    if (clearLibraryConfirmation ===
                                        clearConfirmation &&
                                        !clearLibraryConfirmationInFlight) {
                                        clearLibraryConfirmation = null
                                    }
                                },
                                onClearLibrary = {
                                    if (clearLibraryConfirmation ===
                                        clearConfirmation &&
                                        !clearLibraryConfirmationInFlight &&
                                        currentMutationsEnabled &&
                                        (currentTrackCount > 0 ||
                                            currentSourceCount > 0)) {
                                        clearLibraryConfirmationInFlight = true
                                        try {
                                            currentClearLibrary()
                                        } catch (failure: Throwable) {
                                            if (clearLibraryConfirmation ===
                                                clearConfirmation) {
                                                clearLibraryConfirmationInFlight =
                                                    false
                                            }
                                            throw failure
                                        }
                                        if (clearLibraryConfirmation ===
                                            clearConfirmation) {
                                            clearLibraryConfirmation = null
                                            clearLibraryConfirmationInFlight =
                                                false
                                        }
                                    }
                                },
                            )
                        }
                    } else if (pendingRemoveMissingConfirmation != null) {
                        val confirmation = pendingRemoveMissingConfirmation
                        {
                            AnimatedRemoveMissingTracksDialogRoute(
                                sourceName =
                                    (eligibleRemoveMissingSource(
                                            confirmation.source.id,
                                            confirmation.session,
                                        ) ?: confirmation.source)
                                        .displayName
                                        .ifBlank {
                                            confirmation.source.id
                                        },
                                trackCount =
                                    confirmation.session.changeSummary
                                        ?.missingCount ?: 0,
                                confirmEnabled =
                                    removeMissingConfirmation ===
                                        confirmation &&
                                        !removeMissingConfirmationInFlight &&
                                        currentMutationsEnabled &&
                                        eligibleRemoveMissingSource(
                                            confirmation.source.id,
                                            confirmation.session,
                                        ) != null,
                                onDismiss = {
                                    if (removeMissingConfirmation ===
                                        confirmation &&
                                        !removeMissingConfirmationInFlight) {
                                        removeMissingConfirmation = null
                                    }
                                },
                                onRemoveMissingTracks = {
                                    val currentSource =
                                        eligibleRemoveMissingSource(
                                            confirmation.source.id,
                                            confirmation.session,
                                        )
                                    if (removeMissingConfirmation ===
                                        confirmation &&
                                        !removeMissingConfirmationInFlight &&
                                        currentMutationsEnabled &&
                                        currentSource != null) {
                                        removeMissingConfirmationInFlight = true
                                        try {
                                            currentRemoveMissingTracks(
                                                currentSource,
                                                confirmation.session,
                                            )
                                        } catch (failure: Throwable) {
                                            if (removeMissingConfirmation ===
                                                confirmation) {
                                                removeMissingConfirmationInFlight =
                                                    false
                                            }
                                            throw failure
                                        }
                                        if (removeMissingConfirmation ===
                                            confirmation) {
                                            removeMissingConfirmation = null
                                            removeMissingConfirmationInFlight =
                                                false
                                        }
                                    }
                                },
                            )
                        }
                    } else {
                        null
                    },
                notificationRecovery =
                    mediaNotificationPermission
                        .toSettingsNotificationRecovery(),
                mediaStoreAudioRecovery =
                    when (mediaStorePermission) {
                        MediaStoreAudioPermissionState.Unavailable -> null
                        MediaStoreAudioPermissionState.Granted ->
                            MediaStoreAudioRecovery.Add.takeIf {
                                sources.none {
                                    it.platformKind ==
                                        LibraryPlatformKind
                                            .AndroidMediaStoreAudio
                                }
                            }
                        MediaStoreAudioPermissionState.Requestable ->
                            MediaStoreAudioRecovery.Requestable
                        MediaStoreAudioPermissionState.SettingsRequired ->
                            MediaStoreAudioRecovery.SettingsRequired
                    },
                onRequestMediaStoreAudio = onRequestMediaStoreAudio,
                onOpenMediaStoreSettings = onOpenMediaStoreSettings,
                onAddMediaStoreAudio = onAddMediaStoreAudio,
                onThemeModeSelected = onThemeModeSelected,
                onAddMusicFolder = folderPickerLauncher::launch,
                onRescanSource = { id ->
                    sources
                        .firstOrNull { it.id == id }
                        ?.let { source ->
                            onRescanSource(source)
                        }
                },
                onRecoverSource = { id ->
                    val source = sources.firstOrNull { it.id == id }
                    if (source?.platformKind == LibraryPlatformKind.JvmFolder &&
                        source.handle.startsWith(
                            "rhythhaus:jvm-dropped-files:v1:")) {
                        onRescanSource(source)
                    } else if (source?.platformKind ==
                        LibraryPlatformKind.AndroidMediaStoreAudio) {
                        if (mediaStorePermission ==
                            MediaStoreAudioPermissionState.SettingsRequired) {
                            onOpenMediaStoreSettings()
                        } else {
                            onRequestMediaStoreAudio()
                        }
                    } else {
                        source?.let(onRecoverSource)
                    }
                },
                onRemoveSource = { id ->
                    sources
                        .firstOrNull { it.id == id }
                        ?.let { source ->
                            onRemoveSource(source)
                        }
                },
                onRequestClearLibrary = {
                    if (currentMutationsEnabled &&
                        (currentTrackCount > 0 || currentSourceCount > 0) &&
                        clearLibraryConfirmation == null &&
                        removeMissingConfirmation == null) {
                        clearLibraryConfirmationInFlight = false
                        clearLibraryConfirmation = Any()
                    }
                },
                onAboutClick = onShowSettingsAbout,
                onReviewOnboarding = {
                    pushRoute(
                        LibraryRoute.Onboarding(
                            OnboardingLaunchMode.Review,
                            pageIndex = 0,
                        ))
                },
                onRequestNotificationPermission =
                    onRequestNotificationPermission,
                onOpenNotificationSettings = onOpenNotificationSettings,
                listState = settingsListState,
                onDismiss = {
                    clearLibraryConfirmation = null
                    clearLibraryConfirmationInFlight = false
                    removeMissingConfirmation = null
                    removeMissingConfirmationInFlight = false
                    onDismiss()
                },
            )
        }

        LibraryRoute.Search ->
            SearchContent(
                libraryTracks = libraryTracks,
                currentTrackId = playbackState.currentTrack?.id,
                isPlaying = playbackState.isPlaying,
                labels =
                    SearchSharedLabels(
                        stringResource(Res.string.search),
                        stringResource(Res.string.clear),
                        stringResource(Res.string.now_playing_badge)),
                selectTrackLabel = { title ->
                    stringResource(Res.string.select_track_format, title)
                },
                selectionModeActive =
                    trackSelectionState.pageKey ==
                        TrackSelectionPageKey.Search &&
                        trackSelectionState.selectedTrackIds.isNotEmpty(),
                selectedTrackIds =
                    if (trackSelectionState.pageKey ==
                        TrackSelectionPageKey.Search)
                        trackSelectionState.selectedTrackIds
                    else emptySet(),
                onStartSelection = { id ->
                    onTrackSelectionAction(
                        TrackSelectionAction.Start(
                            TrackSelectionPageKey.Search, id))
                },
                onToggleSelection = { id ->
                    onTrackSelectionAction(
                        TrackSelectionAction.Toggle(
                            TrackSelectionPageKey.Search, id))
                },
                onVisibleTrackIdsChanged = { ids ->
                    onTrackSelectionAction(
                        TrackSelectionAction.ReconcileVisible(
                            TrackSelectionPageKey.Search, ids))
                },
                onScrollPositionChanged = { index, offset ->
                    onScrollPositionChanged(
                        LibraryScrollPosition(index, offset))
                },
                onPlayTrack = { orderedResults, selectedTrack ->
                    playSearchTrack(
                        playbackController,
                        orderedResults,
                        selectedTrack,
                        onDismiss)
                },
                onEditTrackMetadata = onEditTrackMetadata,
                onDismiss = onDismiss,
                playingIndicator = {
                    Box(Modifier.testTag("shared-search-equalizer")) {
                        EqualizerStrip(active = true)
                    }
                },
                interactive = interactive,
                bottomContentPadding = bottomContentPadding,
                initialFirstVisibleItemIndex = searchFirstVisibleItemIndex,
                initialFirstVisibleItemScrollOffset =
                    searchFirstVisibleItemScrollOffset,
            )

        LibraryRoute.SettingsAbout ->
            SettingsAboutScreen(
                onOpenLibraries = onShowOpenSourceLibraries,
                onDismiss = onDismiss,
            )

        LibraryRoute.OpenSourceLibraries ->
            OpenSourceLibrariesScreen(
                readCatalogJson = {
                    Res.readBytes("files/aboutlibraries.json").decodeToString()
                },
                onDismiss = onDismiss,
            )

        LibraryRoute.Home,
        is LibraryRoute.AlbumDetail,
        is LibraryRoute.ArtistDetail,
        LibraryRoute.NowPlaying,
        LibraryRoute.ClearLibraryDialog,
        LibraryRoute.PlaylistHub,
        is LibraryRoute.PlaylistDetail,
        is LibraryRoute.SmartPlaylistDetail,
        -> Unit

        is LibraryRoute.Onboarding ->
            if (isValidOnboardingPageIndex(route.pageIndex)) {
                OnboardingScreen(
                    pageIndex = route.pageIndex,
                    platform = currentOnboardingPlatform().toGuidance(),
                    saving = onboardingSaving,
                    completionError = onboardingCompletionError,
                    reviewMode =
                        route.launchMode == OnboardingLaunchMode.Review,
                    onBack = {
                        if (route.pageIndex > 0) {
                            onReplaceTop(
                                LibraryRoute.Onboarding(
                                    route.launchMode, route.pageIndex - 1))
                        } else if (route.launchMode ==
                            OnboardingLaunchMode.Review)
                            onDismiss()
                    },
                    onNext = {
                        if (route.pageIndex < OnboardingPageCount - 1) {
                            onReplaceTop(
                                LibraryRoute.Onboarding(
                                    route.launchMode, route.pageIndex + 1))
                        }
                    },
                    onSkip = onCompleteOnboarding,
                    onFinish = onCompleteOnboarding,
                    onClose = onCloseOnboarding ?: onDismiss,
                )
            } else {
                LaunchedEffect(route) { onRejectInvalidOnboarding() }
                Box(modifier = Modifier.fillMaxSize())
            }
    }
    if (backupHostActive &&
        (route == LibraryRoute.PlaylistHub || route == LibraryRoute.Settings)) {
        PlaylistBackupSettingsHost(
            state = playlistBackupState,
            launcherAvailable = backupDocumentAvailable,
            showEntryActions = false,
            destination =
                PlaylistFeatureDestination(destinationId.instanceToken),
            appearanceSource = playlistAppearanceSource,
            dismissalPublisher =
                featureDismissalPublisher(destinationId, registerBackSurface),
            labels =
                PlaylistBackupSettingsLabels(
                    cancel = stringResource(Res.string.cancel),
                    close = stringResource(Res.string.close),
                ),
            onExport = onExportPlaylists,
            onOpen = onOpenPlaylistBackup,
            staticPlaylists = playlistState.confirmedSnapshot.playlists,
            onExportPlaylistFormat = onExportPlaylistFormat,
            onOpenPlaylistFormat = onOpenPlaylistFormat,
            onAction = onPlaylistBackupAction,
            onDismissPreview = {
                onPlaylistBackupAction(PlaylistBackupUiAction.DismissPreview)
            },
            onConfirmPreview = onConfirmPlaylistBackup,
            onDismissResult = {
                onPlaylistBackupAction(PlaylistBackupUiAction.DismissResult)
            },
        )
    }
}

internal fun playSearchTrack(
    playbackController: PlaybackController,
    orderedResults: List<LibraryTrack>,
    selectedTrack: LibraryTrack,
    onDismiss: () -> Unit,
) {
    selectLibraryTrackForPlayback(
        playbackController = playbackController,
        visibleQueue = orderedResults.map { it.toPlayableTrack() },
        selectedTrackId = selectedTrack.id,
    )
    onDismiss()
}

/**
 * Resolves the ordered album track sequence for a detail route, matching the
 * feature's internal grouping order (disc, track, lowercase title).
 */
private fun albumDetailTracks(tracks: List<Track>, album: String): List<Track> =
    tracks
        .filter { it.album == album }
        .sortedWith(
            compareBy<Track> { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
                .thenBy { it.title.lowercase() })

/**
 * Resolves the ordered artist track sequence for a detail route, matching the
 * feature's internal grouping order (disc, track, lowercase title).
 */
private fun artistDetailTracks(
    tracks: List<Track>,
    artist: String
): List<Track> =
    tracks
        .filter { it.artist == artist }
        .sortedWith(
            compareBy<Track> { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
                .thenBy { it.title.lowercase() })

@Composable
internal fun LibraryRouteContent(
    route: LibraryRoute,
    tracks: List<Track>,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    playlistRepository: PlaylistRepository,
    playlistState: PlaylistState,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
    onRecoverStalePlaylistDetail: (String) -> Unit,
    onDisplayedPlaylistDeleteConfirmed: (PlaylistSnapshot) -> Unit = {},
    selectedTrackId: String?,
    isNowPlayingBarVisible: Boolean,
    onBack: () -> Unit,
    destinationId: LibraryDestinationId? = null,
    playlistAppearanceSource: PlaylistFeatureAppearanceSource,
    registerBackSurface: (LibraryBackSurfacePort) -> () -> Unit = { {} },
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
    onTrackClickFromTracks: (List<Track>, Track) -> Unit,
    onExpandNowPlaying: (Track) -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    artworkLoader: suspend (String) -> ByteArray?,
    homeContent: @Composable ((LibraryRoute) -> Unit) -> Unit,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    documentActions: @Composable () -> Unit,
    playHistory: Map<String, TrackPlayHistory> = emptyMap(),
    onEditTrackMetadata: ((String) -> Unit)? = null,
    metadataEditorOpen: Boolean = false,
) {
    val playlistDestinationId =
        destinationId ?: LibraryDestinationId(route, "unpresented")
    val playlistDestination =
        PlaylistFeatureDestination(playlistDestinationId.instanceToken)
    val playlistDismissalPublisher =
        featureDismissalPublisher(playlistDestinationId, registerBackSurface)
    val playableTracksById =
        remember(libraryTracks) {
            libraryTracks.associate { it.id to it.toPlayableTrack() }
        }
    when (route) {
        is LibraryRoute.AlbumDetail -> {
            val albumTracks = albumDetailTracks(tracks, route.album)
            if (albumTracks.isEmpty()) {
                if (!metadataEditorOpen) LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                DrillDownView(
                    title = route.album,
                    summary =
                        LibraryDetailSummary.Album(
                            trackCount = albumTracks.size,
                            artist = albumTracks.firstOrNull()?.artist,
                        ),
                    tracks = albumTracks,
                    topBarArtworkTrack = albumTracks.firstOrNull(),
                    currentTrackId = playbackState.currentTrack?.id,
                    selectionPage = LibrarySelectionPage.Album(route.album),
                    selectionModeActive =
                        trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Album(route.album) &&
                            trackSelectionState.selectedTrackIds.isNotEmpty(),
                    selectedTrackIds =
                        if (trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Album(route.album))
                            trackSelectionState.selectedTrackIds
                        else emptySet(),
                    labels = librarySharedLabels(),
                    artworkLoader = artworkLoader,
                    onBack = onBack,
                    onPlayTrack = { orderedTracks, selectedTrack ->
                        onTrackSelected(selectedTrack.id)
                        onTrackClickFromTracks(orderedTracks, selectedTrack)
                    },
                    onToggleSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Toggle(
                                TrackSelectionPageKey.Album(route.album),
                                trackId,
                            ),
                        )
                    },
                    onStartSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Start(
                                TrackSelectionPageKey.Album(route.album),
                                trackId,
                            ),
                        )
                    },
                    onVisibleTrackIdsChanged = { ids ->
                        onTrackSelectionAction(
                            TrackSelectionAction.ReconcileVisible(
                                TrackSelectionPageKey.Album(route.album), ids))
                    },
                    onScrollPositionChanged = { index, offset ->
                        onScrollPositionChanged(
                            LibraryScrollPosition(index, offset))
                    },
                    bottomContentPadding = bottomContentPadding,
                    favoriteTrackIds = favoriteTrackIds,
                    onSetTrackFavorite = onSetTrackFavorite,
                    onEditTrackMetadata = onEditTrackMetadata,
                )
            }
        }

        is LibraryRoute.ArtistDetail -> {
            val artistTracks = artistDetailTracks(tracks, route.artist)
            if (artistTracks.isEmpty()) {
                if (!metadataEditorOpen) LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                DrillDownView(
                    title = route.artist,
                    summary =
                        LibraryDetailSummary.Artist(
                            albumCount =
                                artistTracks.map { it.album }.distinct().size,
                            trackCount = artistTracks.size,
                        ),
                    tracks = artistTracks,
                    topBarArtworkTrack = artistTracks.firstOrNull(),
                    currentTrackId = playbackState.currentTrack?.id,
                    selectionPage = LibrarySelectionPage.Artist(route.artist),
                    selectionModeActive =
                        trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Artist(route.artist) &&
                            trackSelectionState.selectedTrackIds.isNotEmpty(),
                    selectedTrackIds =
                        if (trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Artist(route.artist))
                            trackSelectionState.selectedTrackIds
                        else emptySet(),
                    labels = librarySharedLabels(),
                    artworkLoader = artworkLoader,
                    onBack = onBack,
                    onPlayTrack = { orderedTracks, selectedTrack ->
                        onTrackSelected(selectedTrack.id)
                        onTrackClickFromTracks(orderedTracks, selectedTrack)
                    },
                    onToggleSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Toggle(
                                TrackSelectionPageKey.Artist(route.artist),
                                trackId,
                            ),
                        )
                    },
                    onStartSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Start(
                                TrackSelectionPageKey.Artist(route.artist),
                                trackId,
                            ),
                        )
                    },
                    onVisibleTrackIdsChanged = { ids ->
                        onTrackSelectionAction(
                            TrackSelectionAction.ReconcileVisible(
                                TrackSelectionPageKey.Artist(route.artist),
                                ids,
                            ),
                        )
                    },
                    onScrollPositionChanged = { index, offset ->
                        onScrollPositionChanged(
                            LibraryScrollPosition(index, offset))
                    },
                    bottomContentPadding = bottomContentPadding,
                    favoriteTrackIds = favoriteTrackIds,
                    onSetTrackFavorite = onSetTrackFavorite,
                    onEditTrackMetadata = onEditTrackMetadata,
                )
            }
        }

        LibraryRoute.NowPlaying -> {
            // Now Playing is shown as an overlay, not a navigation route
        }

        LibraryRoute.PlaylistHub -> {
            PlaylistHubScreen(
                state = playlistState,
                libraryTracks = libraryTracks,
                onOpenSmartPlaylist = {
                    onOpenDetailRoute(LibraryRoute.SmartPlaylistDetail(it))
                },
                onCreateSmartPlaylist = { name, rule, onOutcome ->
                    onPlaylistMutation(
                        { createSmartPlaylist(name, rule) }, onOutcome)
                },
                destination = playlistDestination,
                appearanceSource = playlistAppearanceSource,
                dismissalPublisher = playlistDismissalPublisher,
                playlistsLabel = stringResource(Res.string.playlists),
                loadingLabel = stringResource(Res.string.playlist_loading),
                loadFailedLabel =
                    stringResource(Res.string.playlist_load_failed),
                retryLabel = stringResource(Res.string.playlist_retry),
                mutationFailedLabel =
                    stringResource(Res.string.playlist_mutation_failed),
                onBack = onBack,
                onRetry = onRefreshPlaylists,
                onOpenPlaylist = {
                    onOpenDetailRoute(LibraryRoute.PlaylistDetail(it))
                },
                onCreate = { name, onSuccess ->
                    onPlaylistMutation({ create(name) }, onSuccess)
                },
                bottomContentPadding = bottomContentPadding,
                documentActions = documentActions,
            )
        }

        is LibraryRoute.SmartPlaylistDetail -> {
            val smart =
                playlistState.confirmedSnapshot.smartPlaylist(
                    route.smartPlaylistId)
            if (!playlistState.hasConfirmedSnapshot) {
                PlaylistRoutePlaceholder(
                    stringResource(Res.string.playlists),
                    playlistState,
                    onBack,
                    onRefreshPlaylists)
            } else if (smart == null) {
                val message = stringResource(Res.string.playlist_changed)
                LaunchedEffect(route, playlistState.publicationRevision) {
                    onRecoverStalePlaylistDetail(message)
                }
            } else {
                SmartPlaylistDetailScreen(
                    playlist = smart,
                    projection =
                        projectSmartPlaylist(
                            smart,
                            libraryTracks,
                            favoriteTrackIds,
                            playHistory,
                            playlistState.confirmedSnapshot),
                    playableTracksById = playableTracksById,
                    libraryTracks = libraryTracks,
                    state = playlistState,
                    destination = playlistDestination,
                    appearanceSource = playlistAppearanceSource,
                    dismissalPublisher = playlistDismissalPublisher,
                    mutationFailedLabel =
                        stringResource(Res.string.playlist_mutation_failed),
                    onBack = onBack,
                    onRetry = onRefreshPlaylists,
                    onUpdate = { name, rule, onOutcome ->
                        onPlaylistMutation(
                            { updateSmartPlaylist(smart.id, name, rule) },
                            onOutcome)
                    },
                    onDelete = { onOutcome ->
                        onPlaylistMutation(
                            { deleteSmartPlaylist(smart.id) }, onOutcome)
                    },
                    onDeleteConfirmed = onDisplayedPlaylistDeleteConfirmed,
                    onPlayEntry = { request ->
                        selectDynamicOccurrenceForPlayback(
                            playbackController,
                            request.occurrences,
                            request.selectedOccurrenceId)
                    },
                    bottomContentPadding = bottomContentPadding,
                )
            }
        }
        is LibraryRoute.PlaylistDetail -> {
            when (val resolution =
                sharedPlaylistDetailResolution(
                    route.playlistId, playlistState)) {
                SharedPlaylistDetailResolution.AwaitConfirmation ->
                    PlaylistRoutePlaceholder(
                        title = stringResource(Res.string.playlists),
                        state = playlistState,
                        onBack = onBack,
                        onRetry = onRefreshPlaylists,
                    )

                is SharedPlaylistDetailResolution.Show ->
                    PlaylistDetailRouteContent(
                        playlist = resolution.playlist,
                        entries =
                            playlistState.confirmedSnapshot.entries(
                                resolution.playlist.id),
                        playableTracksById = playableTracksById,
                        artworkLoader = artworkLoader,
                        state = playlistState,
                        destination = playlistDestination,
                        appearanceSource = playlistAppearanceSource,
                        dismissalPublisher = playlistDismissalPublisher,
                        mutationFailedLabel =
                            stringResource(Res.string.playlist_mutation_failed),
                        onBack = onBack,
                        onRetry = onRefreshPlaylists,
                        onRename = { name, onSuccess ->
                            onPlaylistMutation(
                                { rename(resolution.playlist.id, name) },
                                onSuccess)
                        },
                        onDeleteMutation = { onOutcome ->
                            onPlaylistMutation(
                                { delete(resolution.playlist.id) },
                                { outcome ->
                                    onOutcome(outcome)
                                },
                            )
                        },
                        onDisplayedPlaylistDeleteConfirmed =
                            onDisplayedPlaylistDeleteConfirmed,
                        onOpenBrowser = {
                            onPlaylistStateAction(
                                PlaylistStateAction.OpenBrowser(
                                    PlaylistBrowserState(
                                        playlistId = resolution.playlist.id),
                                ),
                            )
                        },
                        onPlayEntry = { request ->
                            selectOccurrenceForPlayback(
                                playbackController,
                                request.occurrences,
                                request.selectedOccurrenceId,
                            )
                        },
                        onRemoveEntry = { entryId ->
                            onPlaylistMutation({ removeEntry(entryId) }) {}
                        },
                        onReorder = { entryIds ->
                            onPlaylistMutation({
                                reorder(resolution.playlist.id, entryIds)
                            }) {}
                        },
                        bottomContentPadding = bottomContentPadding,
                        onScrollPositionChanged = onScrollPositionChanged,
                    )

                is SharedPlaylistDetailResolution.ReturnToHub ->
                    PlaylistDetailRouteResolutionEffect(
                        route = route,
                        state = playlistState,
                        onRecoverStalePlaylistDetail =
                            onRecoverStalePlaylistDetail,
                    )
            }
        }

        LibraryRoute.Home -> homeContent(onOpenDetailRoute)

        LibraryRoute.Settings,
        LibraryRoute.SettingsAbout,
        LibraryRoute.OpenSourceLibraries,
        LibraryRoute.Search,
        is LibraryRoute.Onboarding,
        -> error("$route must be rendered through LibraryRouteScreen")

        LibraryRoute.ClearLibraryDialog -> {
            LaunchedEffect(route) { onBack() }
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

/** The actual stale-route recovery seam used by [LibraryRouteContent]. */
@Composable
internal fun PlaylistDetailRouteResolutionEffect(
    route: LibraryRoute.PlaylistDetail,
    state: PlaylistState,
    onRecoverStalePlaylistDetail: (String) -> Unit,
) {
    val resolution = sharedPlaylistDetailResolution(route.playlistId, state)
    if (resolution is SharedPlaylistDetailResolution.ReturnToHub) {
        LaunchedEffect(route, state.publicationRevision) {
            onRecoverStalePlaylistDetail(resolution.message)
        }
    }
}

/**
 * Production seam for the displayed playlist-detail route. A confirmed deletion
 * is routed only through [onDisplayedPlaylistDeleteConfirmed]; stale-detail
 * recovery remains a separate route resolution path in [LibraryRouteContent].
 */
@Composable
internal fun PlaylistDetailRouteContent(
    playlist: PlaylistSummary,
    entries: List<PlaylistEntry>,
    playableTracksById: Map<String, PlayableTrack>,
    artworkLoader: suspend (String) -> ByteArray?,
    state: PlaylistState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    mutationFailedLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRename: (String, (PlaylistStateAction) -> Unit) -> Unit,
    onDeleteMutation: ((PlaylistStateAction) -> Unit) -> Unit,
    onDisplayedPlaylistDeleteConfirmed: (PlaylistSnapshot) -> Unit,
    onOpenBrowser: () -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
) {
    PlaylistDetailScreen(
        playlist = playlist,
        entries = entries,
        playableTracksById = playableTracksById,
        artworkLoader = artworkLoader,
        state = state,
        destination = destination,
        appearanceSource = appearanceSource,
        dismissalPublisher = dismissalPublisher,
        mutationFailedLabel = mutationFailedLabel,
        onBack = onBack,
        onRetry = onRetry,
        onRename = onRename,
        onDelete = onDeleteMutation,
        onDeleteConfirmed = onDisplayedPlaylistDeleteConfirmed,
        onOpenBrowser = onOpenBrowser,
        onPlayEntry = onPlayEntry,
        onRemoveEntry = onRemoveEntry,
        onReorder = onReorder,
        bottomContentPadding = bottomContentPadding,
        onScrollPositionChanged = { index, offset ->
            onScrollPositionChanged(LibraryScrollPosition(index, offset))
        },
    )
}

@Composable
private fun PlaylistRoutePlaceholder(
    title: String,
    state: PlaylistState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val scrollBehavior =
        rememberMiuixTopAppBarScrollBehavior(canScroll = { false })
    val retryLabel = stringResource(Res.string.playlist_retry)
    Box(modifier = Modifier.fillMaxSize()) {
        DrillDownMiuixScrollChrome(
            scrollBehavior = scrollBehavior,
            title = title,
            onBack = onBack,
            backdrop = null,
            modifier = Modifier,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                state.isLoading ->
                    Text(
                        text = stringResource(Res.string.playlist_loading),
                        color = HausColors.current.muted,
                    )

                state.readErrorMessage != null -> {
                    Text(
                        text = stringResource(Res.string.playlist_load_failed),
                        color = HausColors.current.muted,
                    )
                    Button(
                        onClick = onRetry,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(48.dp)
                                .semantics { contentDescription = retryLabel },
                        cornerRadius = 16.dp,
                        colors =
                            ButtonDefaults.buttonColors(
                                color = HausColors.current.ink,
                                contentColor = HausColors.current.paper,
                            ),
                    ) {
                        Text(retryLabel)
                    }
                }
            }
            when (sharedPlaylistRouteNotice(state)) {
                SharedPlaylistRouteNotice.PlaylistChanged ->
                    Text(
                        text = stringResource(Res.string.playlist_changed),
                        color = HausColors.current.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                SharedPlaylistRouteNotice.MutationFailed ->
                    Text(
                        text =
                            stringResource(Res.string.playlist_mutation_failed),
                        color = HausColors.current.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                null -> Unit
            }
        }
    }
}

private enum class SharedPlaylistRouteNotice {
    PlaylistChanged,
    MutationFailed
}

private fun sharedPlaylistRouteNotice(
    state: PlaylistState
): SharedPlaylistRouteNotice? =
    when {
        state.recoverableMessage != null ->
            SharedPlaylistRouteNotice.PlaylistChanged
        state.mutationErrorMessage != null ->
            SharedPlaylistRouteNotice.MutationFailed
        else -> null
    }

private sealed interface SharedPlaylistDetailResolution {
    data object AwaitConfirmation : SharedPlaylistDetailResolution

    data class Show(val playlist: PlaylistSummary) :
        SharedPlaylistDetailResolution

    data class ReturnToHub(val message: String) : SharedPlaylistDetailResolution
}

private fun sharedPlaylistDetailResolution(
    playlistId: String,
    state: PlaylistState,
): SharedPlaylistDetailResolution {
    val playlist = state.confirmedSnapshot.playlist(playlistId)
    return when {
        playlist != null -> SharedPlaylistDetailResolution.Show(playlist)
        state.isLoading || !state.hasConfirmedSnapshot ->
            SharedPlaylistDetailResolution.AwaitConfirmation
        else -> SharedPlaylistDetailResolution.ReturnToHub("playlist_changed")
    }
}
