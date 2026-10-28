package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackMetadataEditorData
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState
import com.eterocell.rhythhaus.nowplaying.NowPlayingBar
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarLabels
import com.eterocell.rhythhaus.nowplaying.NowPlayingPanel
import com.eterocell.rhythhaus.nowplaying.NowPlayingScreen
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsLabels
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsSection
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.PlaylistDocumentFormat
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.ui.HausButton
import com.eterocell.rhythhaus.ui.HausWindowOverlayHost
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayPublisher
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayVisible
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberHausReducedMotion
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import com.eterocell.rhythhaus.ui.songArtistAlbumSubtitle
import com.eterocell.rhythhaus.ui.verticalSheetGesture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.album_art
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.close
import rhythhaus.shared.generated.resources.desktop_drop_active
import rhythhaus.shared.generated.resources.desktop_drop_choose_folder
import rhythhaus.shared.generated.resources.desktop_drop_hint
import rhythhaus.shared.generated.resources.desktop_drop_title
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.library
import rhythhaus.shared.generated.resources.next_track
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.queue
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.track_artist_album_format
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

internal const val NowPlayingShellPlacementTestTag = "NowPlayingShellPlacement"

private data class MetadataEditorSession(
    val token: Long,
    val destination: LibraryDestinationId,
    val data: TrackMetadataEditorData?,
    val draft: TrackMetadataDraft,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

internal const val SelectionShellPlacementTestTag = "SelectionShellPlacement"

/**
 * Shared composition-local artwork loader resolving artwork for a track ID. App
 * provides it once from the repository; the bottom bar and Now Playing adapters
 * consume it while feature leaves receive their own loader callback.
 */
internal val LocalTrackArtworkLoader =
    staticCompositionLocalOf<suspend (String) -> TrackArtwork?> { { null } }

/** Shared-owned localized wording for the feature library composables. */
@Composable
internal fun librarySharedLabels(): LibrarySharedLabels =
    LibrarySharedLabels(
        addMusicFolder = stringResource(Res.string.add_music_folder),
        folderPickerUnavailable =
            stringResource(Res.string.folder_picker_unavailable),
        clearLibrary = stringResource(Res.string.clear_library),
        cancel = stringResource(Res.string.cancel),
        libraryTitle = stringResource(Res.string.library),
        albumArt = stringResource(Res.string.album_art),
        albumArtwork = stringResource(Res.string.album_artwork),
        nowPlayingBadge = stringResource(Res.string.now_playing_badge),
        selectTrack = { title ->
            stringResource(Res.string.select_track_format, title)
        },
        trackArtistAlbum = { artist, album ->
            songArtistAlbumSubtitle(artist, album)
        },
    )

private fun LazyListState.toLibraryScrollPosition(): LibraryScrollPosition =
    LibraryScrollPosition(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )

/**
 * Applies a home browse-mode change, clearing flat-list selection through the
 * route-change reducer exactly when leaving a flat home surface for a grouped
 * browse mode.
 */
internal fun dispatchHomeBrowseModeChange(
    currentMode: BrowseMode,
    nextMode: BrowseMode,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
) {
    val currentModeIsFlat = currentMode.isFlatHomeBrowseMode()
    val nextModeIsFlat = nextMode.isFlatHomeBrowseMode()
    if (currentModeIsFlat && !nextModeIsFlat) {
        onTrackSelectionAction(TrackSelectionAction.RouteChanged(null))
    }
    onBrowseModeChange(nextMode)
}

/**
 * The shell-owned callbacks for one rendered playlist detail navigation entry.
 */
internal class PlaylistDetailRouteOrchestrator(
    private val appState: LibraryAppState,
    private val clearSelection: () -> Unit,
    private val onPlaylistStateAction: (PlaylistStateAction) -> Unit,
) {
    fun recoverStalePlaylistDetail(
        entry: LibraryNavigationEntry,
        message: String
    ) {
        if (appState.navigation.currentEntry != entry) return
        clearSelection()
        appState.recoverStalePlaylistDetail(message) { recoverableMessage ->
            onPlaylistStateAction(
                PlaylistStateAction.ShowRecoverableMessage(recoverableMessage),
            )
        }
    }

    fun completeDisplayedPlaylistDeletion(
        entry: LibraryNavigationEntry,
        confirmedSnapshot: PlaylistSnapshot,
    ) {
        (entry.route as? LibraryRoute.PlaylistDetail)?.let { route ->
            appState.completeDisplayedPlaylistDeletion(
                confirmedSnapshot,
                route.playlistId,
                entry,
            )
        }
        (entry.route as? LibraryRoute.SmartPlaylistDetail)?.let { route ->
            appState.completeDisplayedSmartPlaylistDeletion(
                confirmedSnapshot,
                route.smartPlaylistId,
                entry,
            )
        }
    }

    fun displayedPlaylistDeleteCompletion(
        entry: LibraryNavigationEntry,
    ): (PlaylistSnapshot) -> Unit = { confirmedSnapshot ->
        completeDisplayedPlaylistDeletion(entry, confirmedSnapshot)
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
fun LibraryHomeScreen(
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playlistRepository: PlaylistRepository,
    playlistState: PlaylistState,
    playlistBackupState: PlaylistBackupUiState,
    backupDocumentAvailable: Boolean,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
    onExportPlaylists: () -> Unit,
    onOpenPlaylistBackup: () -> Unit,
    onExportPlaylistFormat: (String, PlaylistDocumentFormat) -> Unit,
    onOpenPlaylistFormat: (PlaylistDocumentFormat) -> Unit,
    onConfirmPlaylistBackup: () -> Unit,
    onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    latestCompletedScanSession: ScanSession?,
    scanErrors: List<ScanError>,
    scanJob: Job?,
    coordinatorMutationsEnabled: Boolean,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    replayGainState: com.eterocell.rhythhaus.playback.ReplayGainPreferenceState,
    onReplayGainModeSelected: (com.eterocell.rhythhaus.ReplayGainMode) -> Unit,
    crossfadeState: com.eterocell.rhythhaus.playback.CrossfadePreferenceState,
    onCrossfadeDurationSelected:
        (com.eterocell.rhythhaus.CrossfadeDuration) -> Unit,
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
    onRemoveSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
    onCancelScan: () -> Unit,
    sourceIdByTrackId: Map<String, String>,
    modifiedAtByTrackId: Map<String, Long?>,
    mediaNotificationPermission: MediaNotificationPermissionState =
        MediaNotificationPermissionState.Unavailable,
    onRequestNotificationPermission: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    mediaStorePermission: MediaStoreAudioPermissionState =
        MediaStoreAudioPermissionState.Unavailable,
    onRequestMediaStoreAudio: () -> Unit = {},
    onOpenMediaStoreSettings: () -> Unit = {},
    onAddMediaStoreAudio: () -> Unit = {},
    initialOnboarding: OnboardingLaunchMode? = null,
    onboardingSaving: Boolean = false,
    onboardingCompletionError: String? = null,
    onCompleteOnboarding: () -> Unit = {},
    favoriteTrackIds: Set<String> = emptySet(),
    artworkTrackIds: Set<String> = emptySet(),
    playHistory: Map<String, TrackPlayHistory> = emptyMap(),
    createdAtByTrackId: Map<String, Long> = emptyMap(),
    onSetTrackFavorite: (trackId: String, favorite: Boolean) -> Unit = { _, _ ->
    },
    onLoadTrackMetadata: (suspend (String) -> TrackMetadataEditorData?)? = null,
    onSetTrackMetadataOverride:
        (suspend (String, TrackMetadataOverride) -> Boolean)? =
        null,
    desktopDropTarget: DesktopDropTargetPresentation =
        DesktopDropTargetPresentation.Disabled,
    onDesktopDropAvailabilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val playbackState by playbackController.state.collectAsState()
    val sleepTimerState by playbackController.sleepTimerState.collectAsState()
    val appState =
        rememberLibraryAppState(
            snapshot = snapshot,
            initialOnboarding = initialOnboarding,
        )
    val routeStateHolder = rememberSaveableStateHolder()
    val activeDestinationTokens =
        remember(appState.navigation.entries) {
            appState.navigation.entries
                .map { it.destinationId.instanceToken }
                .toSet()
        }
    var retainedDestinationTokens by remember {
        mutableStateOf(activeDestinationTokens)
    }
    LaunchedEffect(routeStateHolder, activeDestinationTokens) {
        (retainedDestinationTokens - activeDestinationTokens).forEach {
            routeStateHolder.removeState(it)
        }
        retainedDestinationTokens = activeDestinationTokens
    }
    val sourceOptions =
        remember(sources) {
            sources.map { source ->
                LibraryBrowseSourceOption(
                    id = source.id,
                    displayName = source.displayName,
                )
            }
        }
    val settingsDestinationToken =
        appState.navigation.entries
            .lastOrNull { it.route == LibraryRoute.Settings }
            ?.destinationId
            ?.instanceToken
    val settingsListState =
        rememberSaveable(
            settingsDestinationToken,
            saver = LazyListState.Saver,
        ) {
            LazyListState()
        }
    val searchDestinationToken =
        appState.navigation.entries
            .lastOrNull { it.route == LibraryRoute.Search }
            ?.destinationId
            ?.instanceToken
    var searchFirstVisibleItemIndex by
        rememberSaveable(searchDestinationToken) { mutableStateOf(0) }
    var searchFirstVisibleItemScrollOffset by
        rememberSaveable(searchDestinationToken) { mutableStateOf(0) }
    var settingsExclusiveModal by
        remember(settingsDestinationToken) { mutableStateOf(false) }
    var desktopBannerHeight by remember { mutableFloatStateOf(0f) }
    var settingsReportVisible by
        remember(settingsDestinationToken, scanProgress?.session?.id) {
            mutableStateOf(false)
        }
    val activePlaylistDestination =
        remember(appState.activeDestinationId) {
            PlaylistFeatureDestination(
                appState.activeDestinationId.instanceToken)
        }
    val activePlaylistAppearanceSource =
        rememberPlaylistFeatureAppearanceSource(activePlaylistDestination)
    LaunchedEffect(playbackState.currentTrack?.id) {
        appState.syncSelectedTrackWithPlayback(playbackState.currentTrack?.id)
    }
    val selectedTrack =
        playbackState.currentTrack?.id?.let { currentTrackId ->
            snapshot.tracks.firstOrNull { it.id == currentTrackId }
        }
    val reducedMotion = rememberHausReducedMotion()
    val expandProgress = remember { Animatable(0f) }
    val nowPlayingDockFocusRequester = remember { FocusRequester() }
    var playerWasOpened by remember { mutableStateOf(false) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(appState.showNowPlaying) {
        val target = if (appState.showNowPlaying) 1f else 0f
        if (reducedMotion) expandProgress.snapTo(target)
        else expandProgress.animateTo(target, tween(200))
    }
    LaunchedEffect(appState.showNowPlaying) {
        if (appState.showNowPlaying) {
            playerWasOpened = true
        } else if (playerWasOpened) {
            snapshotFlow { expandProgress.value }.first { it <= 0.001f }
            withFrameNanos {}
            if (selectedTrack != null) {
                nowPlayingDockFocusRequester.requestFocus()
            }
            playerWasOpened = false
        }
    }
    val albums =
        remember(snapshot.tracks) {
            snapshot.tracks.sortedWith(
                compareBy<Track> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            )
        }
    val artists =
        remember(snapshot.tracks) {
            snapshot.tracks.sortedWith(
                compareBy<Track> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            )
        }
    var trackSelectionState by remember {
        mutableStateOf(TrackSelectionState())
    }
    var homeVisibleTrackIds by remember {
        mutableStateOf(emptyList<String>())
    }
    var searchVisibleTrackIds by remember {
        mutableStateOf(emptyList<String>())
    }
    val bottomBarContent =
        libraryBottomBarContent(
            route = appState.navigation.current,
            selectionState = trackSelectionState,
            isNowPlayingVisible = appState.isNowPlayingBarVisible,
            hasSelectedTrack = selectedTrack != null,
        )
    val density = LocalDensity.current
    val artworkLoader = LocalTrackArtworkLoader.current
    fun dispatchTrackSelection(action: TrackSelectionAction) {
        if (action is TrackSelectionAction.ReconcileVisible) {
            when (action.pageKey) {
                TrackSelectionPageKey.HomeSongs ->
                    homeVisibleTrackIds = action.visibleTrackIds

                TrackSelectionPageKey.Search ->
                    searchVisibleTrackIds = action.visibleTrackIds

                else -> Unit
            }
        }
        trackSelectionState = reduceTrackSelection(trackSelectionState, action)
    }
    fun clearSelection() {
        dispatchTrackSelection(TrackSelectionAction.RouteChanged(null))
    }
    val editScope = rememberCoroutineScope()
    var metadataEditor by remember {
        mutableStateOf<MetadataEditorSession?>(null)
    }
    var metadataEditRequest by remember { mutableStateOf(0L) }
    LaunchedEffect(appState.activeDestinationId) {
        metadataEditRequest++
        metadataEditor = null
    }
    fun requestMetadataEdit(trackId: String) {
        val load = onLoadTrackMetadata ?: return
        if (onSetTrackMetadataOverride == null) return
        val destination = appState.activeDestinationId
        val request = ++metadataEditRequest
        editScope.launch {
            val data =
                try {
                    load(trackId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    null
                }
            if (request == metadataEditRequest &&
                destination == appState.activeDestinationId) {
                metadataEditor =
                    MetadataEditorSession(
                        token = request,
                        destination = destination,
                        data = data,
                        draft =
                            data?.let(TrackMetadataDraft::from)
                                ?: TrackMetadataDraft(),
                        failed = data == null,
                    )
            }
        }
    }
    val editAction: ((String) -> Unit)? =
        if (onLoadTrackMetadata != null && onSetTrackMetadataOverride != null)
            ::requestMetadataEdit
        else null
    val playlistDetailRouteOrchestrator =
        PlaylistDetailRouteOrchestrator(
            appState = appState,
            clearSelection = ::clearSelection,
            onPlaylistStateAction = onPlaylistStateAction,
        )
    var nextSelectionAppearanceToken by remember { mutableStateOf(0L) }
    val selectionPort =
        trackSelectionState.pageKey
            ?.takeIf {
                trackSelectionState.selectedTrackIds.isNotEmpty() &&
                    it ==
                        trackSelectionPageKeyFor(
                            appState.navigation.current, appState.browseMode)
            }
            ?.let { pageKey ->
                val token =
                    remember(appState.activeDestinationId, pageKey) {
                        "selection-${++nextSelectionAppearanceToken}"
                    }
                LibraryBackSelectionPort(
                    destinationId = appState.activeDestinationId,
                    target =
                        LibraryBackTarget.PageSelection(
                            LibraryBackTargetId(
                                appState.activeDestinationId, token),
                            pageKey,
                        ),
                    cancel = {
                        dispatchTrackSelection(TrackSelectionAction.Cancel)
                    },
                )
            }
    fun pushRoute(route: LibraryRoute) {
        if (route == appState.navigation.current) return
        metadataEditor = null
        metadataEditRequest++
        clearSelection()
        appState.pushRoute(route)
    }
    fun pageTrackIds(pageKey: TrackSelectionPageKey): List<String> =
        when (pageKey) {
            TrackSelectionPageKey.HomeSongs -> homeVisibleTrackIds
            is TrackSelectionPageKey.Album ->
                albums.filter { it.album == pageKey.album }.map(Track::id)
            is TrackSelectionPageKey.Artist ->
                artists.filter { it.artist == pageKey.artist }.map(Track::id)
            TrackSelectionPageKey.Search -> searchVisibleTrackIds
        }
    fun openSelectedTracksPicker() {
        val pageKey = trackSelectionState.pageKey ?: return
        val visibleTrackIds = pageTrackIds(pageKey)
        val orderedIds =
            orderedSelectedTrackIds(
                trackSelectionState, pageKey, visibleTrackIds)
        if (orderedIds.isNotEmpty())
            onPlaylistStateAction(
                PlaylistStateAction.OpenPicker(PlaylistPickerState(orderedIds)))
    }
    fun selectAllOnCurrentPage() {
        val pageKey = trackSelectionState.pageKey ?: return
        val trackIds = pageTrackIds(pageKey)
        if (trackIds.isNotEmpty()) {
            dispatchTrackSelection(
                TrackSelectionAction.SelectAll(pageKey, trackIds))
        }
    }
    SideEffect {
        appState.publishSelectionPort(selectionPort)
        appState.reconcileBackSession(selectionPort)
    }
    val activeEditor = metadataEditor
    val desktopModalFree =
        appState.navigation.current !is LibraryRoute.Onboarding &&
            !settingsExclusiveModal &&
            metadataEditor == null &&
            !appState.hasExclusiveFeatureSurface &&
            !appState.showNowPlaying
    val foregroundLayerVisible =
        appState.showNowPlaying ||
            settingsExclusiveModal ||
            metadataEditor != null ||
            appState.hasExclusiveFeatureSurface ||
            playlistState.picker != null ||
            playlistState.browser != null
    val appShellInteractive =
        !foregroundLayerVisible && expandProgress.value <= 0.001f
    val latestDropAvailability =
        androidx.compose.runtime.rememberUpdatedState(
            onDesktopDropAvailabilityChanged)
    SideEffect {
        latestDropAvailability.value(
            desktopDropTarget.isAvailable && desktopModalFree)
    }
    DisposableEffect(Unit) { onDispose { latestDropAvailability.value(false) } }
    fun currentDetailIsEmpty(): Boolean {
        val route = appState.navigation.current
        return when (route) {
            is LibraryRoute.AlbumDetail ->
                snapshot.tracks.none { it.album == route.album }
            is LibraryRoute.ArtistDetail ->
                snapshot.tracks.none { it.artist == route.artist }
            else -> false
        }
    }
    LaunchedEffect(
        metadataEditor, appState.navigation.current, snapshot.tracks) {
            if (metadataEditor == null && currentDetailIsEmpty()) {
                appState.popRoute()
            }
        }
    LaunchedEffect(activeEditor?.token, libraryTracks) {
        val editor = metadataEditor ?: return@LaunchedEffect
        val load = onLoadTrackMetadata ?: return@LaunchedEffect
        val trackId = editor.data?.scannedTrack?.id ?: return@LaunchedEffect
        val refreshed = load(trackId) ?: return@LaunchedEffect
        metadataEditor
            ?.takeIf { it.token == editor.token }
            ?.let { liveEditor ->
                metadataEditor = liveEditor.copy(data = refreshed)
            }
    }
    DisposableEffect(activeEditor?.token, appState.activeDestinationId) {
        val editor = activeEditor
        val dispose =
            if (editor != null &&
                editor.destination == appState.activeDestinationId) {
                val target =
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(
                            editor.destination, "metadata-${editor.token}"),
                    )
                appState.registerBackSurface(
                    LibraryBackSurfacePort(editor.destination, target) {
                        requested ->
                        if (requested == target &&
                            metadataEditor?.token == editor.token) {
                            metadataEditor = null
                            LibraryBackFeatureRequestResult.Started
                        } else LibraryBackFeatureRequestResult.Rejected
                    },
                )
            } else ({})
        onDispose { dispose() }
    }
    val requestLibraryBack: () -> Unit = {
        performLibraryBack(
            appState,
            selectionPort,
            {},
        )
    }
    val navigationEventDispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "LibraryHomeScreen requires a NavigationEventDispatcher owner"
            }
            .navigationEventDispatcher
    val navigationEventBackHandler =
        rememberLibraryNavigationEventBackHandler(
            dispatcher = navigationEventDispatcher,
            beginBack = { appState.beginBack(selectionPort) },
            enabled = appState.canBeginBack(selectionPort),
        )
    val predictiveBackProgress = navigationEventBackHandler.predictiveProgress
    val predictiveBackOffset = remember { Animatable(0f) }
    LaunchedEffect(predictiveBackProgress) {
        if (predictiveBackProgress > 0f) {
            predictiveBackOffset.snapTo(40 * predictiveBackProgress)
        } else if (reducedMotion) {
            predictiveBackOffset.snapTo(0f)
        } else {
            predictiveBackOffset.animateTo(0f, tween(150))
        }
    }

    val previousEntry = navigationEventBackHandler.routePreview()?.incomingEntry

    fun selectTrackFromTracks(tracks: List<Track>, track: Track) {
        selectLibraryTrackForPlayback(
            playbackController = playbackController,
            visibleQueue = tracks.map { it.toPlayableTrack() },
            selectedTrackId = track.id,
        )
    }

    fun expandNowPlaying(
        track: Track,
        initialPanel: NowPlayingPanel = NowPlayingPanel.Controls,
    ) {
        appState.setSelectedTrackId(track.id)
        appState.showNowPlaying(
            initialPanel = initialPanel,
            trackId = track.id,
            destinationId = appState.activeDestinationId,
        )
    }

    @Composable
    fun RouteScreen(
        route: LibraryRoute,
        bottomContentPadding: Dp,
        backupHostActive: Boolean,
        interactive: Boolean,
    ) {
        LibraryRouteScreen(
            route = route,
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            playbackController = playbackController,
            playbackState = playbackState,
            playlistRepository = playlistRepository,
            playlistState = playlistState,
            playlistBackupState = playlistBackupState,
            backupDocumentAvailable = backupDocumentAvailable,
            backupHostActive = backupHostActive,
            interactive = interactive,
            destinationId = appState.activeDestinationId,
            playlistAppearanceSource = activePlaylistAppearanceSource,
            registerBackSurface = appState::registerBackSurface,
            onPlaylistStateAction = onPlaylistStateAction,
            onRefreshPlaylists = onRefreshPlaylists,
            onPlaylistMutation = onPlaylistMutation,
            onExportPlaylists = onExportPlaylists,
            onOpenPlaylistBackup = onOpenPlaylistBackup,
            onExportPlaylistFormat = onExportPlaylistFormat,
            onOpenPlaylistFormat = onOpenPlaylistFormat,
            onConfirmPlaylistBackup = onConfirmPlaylistBackup,
            onPlaylistBackupAction = onPlaylistBackupAction,
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            importMessage = importMessage,
            scanProgress = scanProgress,
            latestCompletedScanSession = latestCompletedScanSession,
            scanErrors = scanErrors,
            scanJob = scanJob,
            mutationsEnabled = coordinatorMutationsEnabled,
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            replayGainState = replayGainState,
            onReplayGainModeSelected = onReplayGainModeSelected,
            crossfadeState = crossfadeState,
            onCrossfadeDurationSelected = onCrossfadeDurationSelected,
            gaplessState = gaplessState,
            onGaplessModeSelected = onGaplessModeSelected,
            automaticResumeState = automaticResumeState,
            onAutomaticResumePolicySelected = onAutomaticResumePolicySelected,
            outputStatus = outputStatus,
            onClearLibrary = onClearLibrary,
            onRescanSource = onRescanSource,
            onRecoverSource = onRecoverSource,
            onRemoveMissingTracks = onRemoveMissingTracks,
            onRemoveSource = onRemoveSource,
            onCancelScan = onCancelScan,
            mediaNotificationPermission = mediaNotificationPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenNotificationSettings = onOpenNotificationSettings,
            mediaStorePermission = mediaStorePermission,
            onRequestMediaStoreAudio = onRequestMediaStoreAudio,
            onOpenMediaStoreSettings = onOpenMediaStoreSettings,
            onAddMediaStoreAudio = onAddMediaStoreAudio,
            settingsListState = settingsListState,
            searchFirstVisibleItemIndex = searchFirstVisibleItemIndex,
            searchFirstVisibleItemScrollOffset =
                searchFirstVisibleItemScrollOffset,
            settingsReportVisible = settingsReportVisible,
            onSettingsReportVisibleChanged = {
                settingsReportVisible = it
            },
            onSettingsExclusiveModalChanged = { settingsExclusiveModal = it },
            onboardingSaving = onboardingSaving,
            onboardingCompletionError = onboardingCompletionError,
            onCompleteOnboarding = onCompleteOnboarding,
            onReplaceTop = appState::replaceTopRoute,
            onRejectInvalidOnboarding = appState::popRoute,
            onCloseOnboarding = appState::popRoute,
            pushRoute = ::pushRoute,
            onShowSettingsAbout = { pushRoute(LibraryRoute.SettingsAbout) },
            onShowOpenSourceLibraries = {
                pushRoute(LibraryRoute.OpenSourceLibraries)
            },
            onDismiss = requestLibraryBack,
            onScrollPositionChanged = { scrollPosition ->
                appState.updateNowPlayingBarVisibilityForScroll(scrollPosition)
                if (route == LibraryRoute.Search) {
                    searchFirstVisibleItemIndex =
                        scrollPosition.firstVisibleItemIndex
                    searchFirstVisibleItemScrollOffset =
                        scrollPosition.firstVisibleItemScrollOffset
                }
            },
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = ::dispatchTrackSelection,
            bottomContentPadding = bottomContentPadding,
            onEditTrackMetadata = editAction,
        )
    }

    @Composable
    fun RouteContent(
        entry: LibraryNavigationEntry,
        bottomContentPadding: Dp,
        onOpenDetailRoute: (LibraryRoute) -> Unit = ::pushRoute,
    ) {
        val route = entry.route
        LibraryRouteContent(
            route = route,
            tracks = snapshot.tracks,
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            playbackController = playbackController,
            playbackState = playbackState,
            playlistRepository = playlistRepository,
            playlistState = playlistState,
            onPlaylistStateAction = onPlaylistStateAction,
            onRefreshPlaylists = onRefreshPlaylists,
            onPlaylistMutation = onPlaylistMutation,
            onRecoverStalePlaylistDetail = { message ->
                playlistDetailRouteOrchestrator.recoverStalePlaylistDetail(
                    entry, message)
            },
            onDisplayedPlaylistDeleteConfirmed =
                playlistDetailRouteOrchestrator
                    .displayedPlaylistDeleteCompletion(entry),
            selectedTrackId = appState.selectedTrackId,
            isNowPlayingBarVisible = appState.isNowPlayingBarVisible,
            onBack = requestLibraryBack,
            destinationId =
                entry.destinationId.takeIf {
                    entry == appState.navigation.currentEntry
                },
            playlistAppearanceSource = activePlaylistAppearanceSource,
            registerBackSurface = appState::registerBackSurface,
            onOpenDetailRoute = onOpenDetailRoute,
            onTrackSelected = appState::setSelectedTrackId,
            onTrackClickFromTracks = ::selectTrackFromTracks,
            onExpandNowPlaying = ::expandNowPlaying,
            onScrollPositionChanged =
                appState::updateNowPlayingBarVisibilityForScroll,
            artworkLoader = { id -> artworkLoader(id)?.bytes },
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = ::dispatchTrackSelection,
            bottomContentPadding = bottomContentPadding,
            favoriteTrackIds = favoriteTrackIds,
            playHistory = playHistory,
            onSetTrackFavorite = onSetTrackFavorite,
            onEditTrackMetadata = editAction,
            metadataEditorOpen = metadataEditor != null,
            documentActions = {
                if (route == LibraryRoute.PlaylistHub &&
                    playlistState.hasConfirmedSnapshot) {
                    PlaylistBackupSettingsSection(
                        state = playlistBackupState,
                        launcherAvailable =
                            backupDocumentAvailable &&
                                entry == appState.navigation.currentEntry,
                        labels =
                            PlaylistBackupSettingsLabels(
                                cancel = stringResource(Res.string.cancel),
                                close = stringResource(Res.string.close),
                            ),
                        onExport = onExportPlaylists,
                        onOpen = onOpenPlaylistBackup,
                        onAction = onPlaylistBackupAction,
                        staticPlaylists =
                            playlistState.confirmedSnapshot.playlists,
                        onExportPlaylistFormat = onExportPlaylistFormat,
                        onOpenPlaylistFormat = onOpenPlaylistFormat,
                    )
                }
            },
            homeContent = { onOpenDetailRoute ->
                LibraryHomeContent(
                    tracks = snapshot.tracks,
                    browseMode = appState.browseMode,
                    folderPickerLauncher = folderPickerLauncher,
                    sourcePickerActionVisible = sourcePickerActionVisible,
                    importMessage = importMessage,
                    scanProgress = scanProgress,
                    mutationsEnabled = coordinatorMutationsEnabled,
                    currentTrackId = playbackState.currentTrack?.id,
                    selectionModeActive =
                        trackSelectionState.pageKey ==
                            TrackSelectionPageKey.HomeSongs &&
                            trackSelectionState.selectedTrackIds.isNotEmpty(),
                    selectedTrackIds =
                        if (trackSelectionState.pageKey ==
                            TrackSelectionPageKey.HomeSongs)
                            trackSelectionState.selectedTrackIds
                        else emptySet(),
                    favoriteTrackIds = favoriteTrackIds,
                    artworkTrackIds = artworkTrackIds,
                    playHistory = playHistory,
                    createdAtByTrackId = createdAtByTrackId,
                    browseQuery = appState.browseQuery,
                    sourceOptions = sourceOptions,
                    sourceIdByTrackId = sourceIdByTrackId,
                    modifiedAtByTrackId = modifiedAtByTrackId,
                    onSetTrackFavorite = onSetTrackFavorite,
                    onEditTrackMetadata = editAction,
                    onBrowseSortChange = appState::setBrowseSort,
                    onBrowseSortDirectionChange =
                        appState::setBrowseSortDirection,
                    onBrowseFavoriteOnlyChange =
                        appState::setBrowseFavoriteOnly,
                    onBrowseArtworkOnlyChange = appState::setBrowseArtworkOnly,
                    onBrowseSourceIdChange = appState::setBrowseSourceId,
                    labels = librarySharedLabels(),
                    homeBackdrop = rememberRhythHausBackdrop(),
                    artworkLoader = { id -> artworkLoader(id)?.bytes },
                    onBrowseModeChange = { next ->
                        dispatchHomeBrowseModeChange(
                            appState.browseMode,
                            next,
                            ::dispatchTrackSelection,
                            appState::setBrowseMode,
                        )
                    },
                    onClearLibrary = onClearLibrary,
                    onCancelScan = onCancelScan,
                    onOpenAlbum = { album ->
                        onOpenDetailRoute(LibraryRoute.AlbumDetail(album))
                    },
                    onOpenArtist = { artist ->
                        onOpenDetailRoute(LibraryRoute.ArtistDetail(artist))
                    },
                    onPlayTrack = { orderedTracks, selectedTrack ->
                        appState.setSelectedTrackId(selectedTrack.id)
                        selectTrackFromTracks(orderedTracks, selectedTrack)
                    },
                    onToggleSelection = { id ->
                        dispatchTrackSelection(
                            TrackSelectionAction.Toggle(
                                TrackSelectionPageKey.HomeSongs, id))
                    },
                    onStartSelection = { id ->
                        dispatchTrackSelection(
                            TrackSelectionAction.Start(
                                TrackSelectionPageKey.HomeSongs, id))
                    },
                    onVisibleTrackIdsChanged = { ids ->
                        dispatchTrackSelection(
                            TrackSelectionAction.ReconcileVisible(
                                TrackSelectionPageKey.HomeSongs, ids))
                    },
                    onScrollPositionChanged = { index, offset ->
                        appState.updateNowPlayingBarVisibilityForScroll(
                            LibraryScrollPosition(index, offset))
                    },
                    bottomContentPadding = bottomContentPadding,
                )
            },
        )
    }

    @Composable
    fun DestinationContent(
        entry: LibraryNavigationEntry,
        content: @Composable () -> Unit,
    ) {
        val token = entry.destinationId.instanceToken
        routeStateHolder.SaveableStateProvider(token, content)
    }

    @Composable
    fun RenderEntry(
        entry: LibraryNavigationEntry,
        bottomContentPadding: Dp,
    ) {
        val interactive = entry == appState.navigation.currentEntry
        val routeInteractive =
            interactive &&
                appShellInteractive &&
                !LocalHausWindowOverlayVisible.current
        DestinationInteractionBoundary(interactive) {
            DestinationContent(entry) {
                when (entry.route) {
                    LibraryRoute.Settings,
                    LibraryRoute.SettingsAbout,
                    LibraryRoute.OpenSourceLibraries,
                    LibraryRoute.Search,
                    is LibraryRoute.Onboarding,
                    ->
                        RouteScreen(
                            entry.route,
                            bottomContentPadding,
                            backupHostActive = interactive,
                            interactive = routeInteractive,
                        )

                    LibraryRoute.PlaylistHub -> {
                        RouteContent(
                            entry,
                            bottomContentPadding,
                        )
                        RouteScreen(
                            entry.route,
                            bottomContentPadding,
                            backupHostActive = interactive,
                            interactive = routeInteractive,
                        )
                    }

                    else ->
                        RouteContent(
                            entry,
                            bottomContentPadding,
                        )
                }
            }
        }
    }

    HausWindowOverlayHost(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxSize()
                    .background(HausColors.current.paper)
                    .onSizeChanged { screenHeightPx = it.height.toFloat() },
        ) {
            val rootBackdrop = rememberRhythHausBackdrop()
            val currentRoute = appState.navigation.current
            val windowWidthDp = maxWidth.value
            val windowHeightDp = maxHeight.value
            val desktopDensity =
                appNavigationLayoutFor(windowWidthDp) !=
                    AppNavigationLayout.Bottom
            val hasSelectedDetail =
                currentRoute is LibraryRoute.AlbumDetail ||
                    currentRoute is LibraryRoute.ArtistDetail
            val adaptiveLayoutMode =
                libraryAdaptiveLayoutModeFor(
                    widthDp = windowWidthDp,
                    heightDp = windowHeightDp,
                    hasDetail = hasSelectedDetail,
                )
            val detailPaneWidths =
                remember(windowWidthDp) {
                    libraryDetailPaneWidthsFor(
                        libraryMainContentWidthDp(windowWidthDp))
                }
            fun openDetailRoute(route: LibraryRoute) {
                clearSelection()
                appState.openDetailRoute(
                    route = route,
                    adaptiveLayoutMode =
                        libraryAdaptiveLayoutModeFor(
                            widthDp = windowWidthDp,
                            heightDp = windowHeightDp,
                            hasDetail =
                                route is LibraryRoute.AlbumDetail ||
                                    route is LibraryRoute.ArtistDetail,
                        ),
                )
            }

            Box(
                Modifier.fillMaxSize()
                    .padding(
                        top =
                            if (desktopDropTarget.isAvailable &&
                                desktopModalFree)
                                with(density) { desktopBannerHeight.toDp() }
                            else 0.dp)) {
                    ForegroundInteractionBoundary(
                        interactive = appShellInteractive,
                        restoreFocusWhenInteractive = !playerWasOpened,
                    ) {
                        AppShellLayout(
                            windowWidthDp = windowWidthDp,
                            navigationVisible =
                                desktopModalFree &&
                                    currentRoute != LibraryRoute.NowPlaying,
                            selectedDestination =
                                primaryDestinationFor(currentRoute),
                            activeBrowseMode =
                                appState.browseMode.takeIf {
                                    currentRoute == LibraryRoute.Home
                                },
                            onNavigate = { destination ->
                                when (destination) {
                                    LibraryPrimaryDestination.Library ->
                                        pushRoute(LibraryRoute.Home)
                                    LibraryPrimaryDestination.Search ->
                                        pushRoute(LibraryRoute.Search)
                                    LibraryPrimaryDestination.Playlists ->
                                        pushRoute(LibraryRoute.PlaylistHub)
                                }
                            },
                            onBrowseModeChange = { mode ->
                                if (appState.navigation.current !=
                                    LibraryRoute.Home) {
                                    clearSelection()
                                    appState.popToRoot()
                                }
                                dispatchHomeBrowseModeChange(
                                    appState.browseMode,
                                    mode,
                                    ::dispatchTrackSelection,
                                    appState::setBrowseMode,
                                )
                            },
                            onSettings = { pushRoute(LibraryRoute.Settings) },
                            showSettingsAction =
                                primaryDestinationFor(currentRoute) != null,
                            contentHandlesSafeBottomInset =
                                currentRoute == LibraryRoute.NowPlaying ||
                                    currentRoute is LibraryRoute.Onboarding,
                            bottomBarContent = bottomBarContent,
                            content = {
                                val routeBottomContentPadding = 0.dp
                                when {
                                    currentRoute is LibraryRoute.Onboarding ->
                                        RenderEntry(
                                            appState.navigation.currentEntry,
                                            routeBottomContentPadding,
                                        )

                                    adaptiveLayoutMode ==
                                        LibraryAdaptiveLayoutMode
                                            .ListDetail -> {
                                        val homeEntry =
                                            appState.navigation.entries.first()
                                        Row(
                                            Modifier.fillMaxSize()
                                                .recordRhythHausBackdrop(
                                                    rootBackdrop)) {
                                                Box(
                                                    Modifier.fillMaxHeight()
                                                        .width(
                                                            detailPaneWidths
                                                                .browsingWidthDp
                                                                .dp)
                                                        .testTag(
                                                            AppShellHomePaneTestTag)) {
                                                        DestinationInteractionBoundary(
                                                            interactive = true,
                                                        ) {
                                                            DestinationContent(
                                                                homeEntry) {
                                                                    RouteContent(
                                                                        homeEntry,
                                                                        routeBottomContentPadding,
                                                                        ::
                                                                            openDetailRoute,
                                                                    )
                                                                }
                                                        }
                                                    }
                                                Spacer(
                                                    Modifier.fillMaxHeight()
                                                        .width(
                                                            detailPaneWidths
                                                                .gapWidthDp
                                                                .dp))
                                                Box(
                                                    Modifier.fillMaxHeight()
                                                        .width(
                                                            detailPaneWidths
                                                                .detailWidthDp
                                                                .dp)
                                                        .testTag(
                                                            AppShellDetailPaneTestTag)) {
                                                        RenderEntry(
                                                            appState.navigation
                                                                .currentEntry,
                                                            routeBottomContentPadding,
                                                        )
                                                    }
                                            }
                                    }

                                    else -> {
                                        if (predictiveBackProgress > 0f &&
                                            previousEntry != null) {
                                            RenderEntry(
                                                previousEntry,
                                                routeBottomContentPadding,
                                            )
                                        }
                                        AnimatedContent(
                                            targetState =
                                                appState.navigation
                                                    .currentEntry,
                                            transitionSpec = {
                                                routeContentTransform(
                                                    appState
                                                        .lastNavigationTransition,
                                                    reducedMotion,
                                                )
                                            },
                                            label = "LibraryRouteTransition",
                                            modifier =
                                                Modifier.fillMaxSize()
                                                    .recordRhythHausBackdrop(
                                                        rootBackdrop)
                                                    .offset(
                                                        x =
                                                            predictiveBackOffset
                                                                .value
                                                                .dp),
                                        ) { currentEntry ->
                                            RenderEntry(
                                                currentEntry,
                                                routeBottomContentPadding,
                                            )
                                        }
                                    }
                                }
                            },
                            bottomBar = { presentation, minimumHeight ->
                                LibraryShellBottomBar(
                                    content = bottomBarContent,
                                    presentation = presentation,
                                    selectedTrack = selectedTrack,
                                    playbackState = playbackState,
                                    artworkLoader = { trackId ->
                                        artworkLoader(trackId)?.bytes
                                    },
                                    onCancelSelection = {
                                        dispatchTrackSelection(
                                            TrackSelectionAction.Cancel)
                                    },
                                    onSelectAll = ::selectAllOnCurrentPage,
                                    onAddToPlaylist =
                                        ::openSelectedTracksPicker,
                                    onPlayPause =
                                        playbackController::togglePlayPause,
                                    onSkipNext = playbackController::skipToNext,
                                    onOpenQueue = {
                                        selectedTrack?.let {
                                            expandNowPlaying(
                                                it, NowPlayingPanel.Queue)
                                        }
                                    },
                                    onExpand = {
                                        selectedTrack?.let {
                                            expandNowPlaying(it)
                                        }
                                    },
                                    expandProgress = expandProgress,
                                    isExpanded = appState.showNowPlaying,
                                    screenHeightPx = screenHeightPx,
                                    minimumHeight = minimumHeight,
                                    focusRequester =
                                        nowPlayingDockFocusRequester,
                                    modifier = Modifier,
                                )
                            },
                        )
                    }
                }
            if (desktopDropTarget.isAvailable && desktopModalFree) {
                val title = stringResource(Res.string.desktop_drop_title)
                val hint =
                    stringResource(
                        if (desktopDropTarget.isActive)
                            Res.string.desktop_drop_active
                        else Res.string.desktop_drop_hint)
                val choose =
                    stringResource(Res.string.desktop_drop_choose_folder)
                Surface(
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .onSizeChanged {
                                desktopBannerHeight = it.height.toFloat()
                            }
                            .padding(8.dp)) {
                        Column(
                            modifier =
                                Modifier.padding(12.dp).semantics {
                                    contentDescription = "$title $hint"
                                    stateDescription = hint
                                }) {
                                Text(title)
                                Text(hint)
                                desktopDropTarget.feedback?.let { feedback ->
                                    Text(
                                        feedback,
                                        modifier =
                                            Modifier.semantics {
                                                liveRegion =
                                                    LiveRegionMode.Polite
                                            })
                                }
                                HausButton(
                                    onClick = folderPickerLauncher::launch,
                                    enabled =
                                        coordinatorMutationsEnabled &&
                                            folderPickerLauncher.isAvailable,
                                    modifier =
                                        Modifier.semantics {
                                            contentDescription = choose
                                        },
                                ) {
                                    Text(choose)
                                }
                            }
                    }
            }

            // Now Playing expand overlay (outside AnimatedContent)
            CompositionLocalProvider(
                LocalHausDesktopDensity provides desktopDensity,
            ) {
                NowPlayingExpandOverlay(
                    track = selectedTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    sleepTimerState = sleepTimerState,
                    onArmSleepTimer = playbackController::armSleepTimer,
                    onArmSleepTimerAfterCompletions =
                        playbackController::armSleepTimerAfterCompletions,
                    onCancelSleepTimer = playbackController::cancelSleepTimer,
                    tagLibReader = tagLibReader,
                    currentLibraryTrack =
                        libraryTracks.firstOrNull {
                            it.id == selectedTrack?.id
                        },
                    appState = appState,
                    playlistRepository = playlistRepository,
                    onPlaylistMutation = onPlaylistMutation,
                    playlistDestination = activePlaylistDestination,
                    playlistAppearanceSource = activePlaylistAppearanceSource,
                    artworkLoader = { trackId ->
                        artworkLoader(trackId)?.bytes
                    },
                    isVisible = appState.showNowPlaying,
                    expandProgress = expandProgress,
                    onBack = requestLibraryBack,
                    favoriteTrackIds = favoriteTrackIds,
                    onSetTrackFavorite = onSetTrackFavorite,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            playlistState.picker?.let { picker ->
                AddToPlaylistPickerOverlay(
                    playlists = playlistState.confirmedSnapshot.playlists,
                    state = picker,
                    destination = activePlaylistDestination,
                    appearanceSource = activePlaylistAppearanceSource,
                    dismissalPublisher =
                        featureDismissalPublisher(
                            appState.activeDestinationId,
                            appState::registerBackSurface),
                    onStateChange = { updated ->
                        onPlaylistStateAction(
                            PlaylistStateAction.OpenPicker(
                                PlaylistPickerState(
                                    trackIds = updated.trackIds,
                                    selectedPlaylistId =
                                        updated.selectedPlaylistId,
                                    enteredName = updated.enteredName,
                                ),
                            ),
                        )
                    },
                    onDismiss = {
                        onPlaylistStateAction(PlaylistStateAction.ClosePicker)
                    },
                    onConfirmed = {
                        onPlaylistStateAction(PlaylistStateAction.ClosePicker)
                        dispatchTrackSelection(TrackSelectionAction.Cancel)
                    },
                    onAppend = { playlistId, trackIds, onOutcome ->
                        onPlaylistMutation(
                            { append(playlistId, trackIds) },
                            onOutcome,
                        )
                    },
                    onInlineCreate = { name, trackIds, onOutcome ->
                        onPlaylistMutation(
                            {
                                createWithEntries(name, trackIds)
                            },
                            onOutcome,
                        )
                    },
                )
            }

            playlistState.browser?.let { browser ->
                val playlist =
                    playlistState.confirmedSnapshot.playlist(browser.playlistId)
                if (playlist != null) {
                    PlaylistTrackBrowserOverlay(
                        playlistName = playlist.name,
                        libraryTracks = libraryTracks,
                        state = browser,
                        destination = activePlaylistDestination,
                        appearanceSource = activePlaylistAppearanceSource,
                        dismissalPublisher =
                            featureDismissalPublisher(
                                appState.activeDestinationId,
                                appState::registerBackSurface),
                        onStateChange = { updated ->
                            onPlaylistStateAction(
                                PlaylistStateAction.OpenBrowser(
                                    PlaylistBrowserState(
                                        playlistId = updated.playlistId,
                                        query = updated.query,
                                        visibleTrackIds =
                                            updated.visibleTrackIds,
                                        selectedTrackIds =
                                            updated.selectedTrackIds,
                                    ),
                                ),
                            )
                        },
                        onDismiss = {
                            onPlaylistStateAction(
                                PlaylistStateAction.CloseBrowser)
                        },
                        onConfirmed = {
                            onPlaylistStateAction(
                                PlaylistStateAction.CloseBrowser)
                        },
                        onConfirm = { playlistId, trackIds, onOutcome ->
                            onPlaylistMutation(
                                { append(playlistId, trackIds) },
                                onOutcome,
                            )
                        },
                    )
                }
            }
            metadataEditor?.let { session ->
                TrackMetadataEditDialog(
                    data = session.data,
                    draft = session.draft,
                    saving = session.saving,
                    failed = session.failed,
                    onDraft = { next ->
                        if (metadataEditor?.token == session.token &&
                            !session.saving) {
                            metadataEditor =
                                session.copy(draft = next, failed = false)
                        }
                    },
                    onDismiss = {
                        if (metadataEditor?.token == session.token) {
                            metadataEditRequest++
                            metadataEditor = null
                        }
                    },
                    onSave = { correction ->
                        val save = onSetTrackMetadataOverride
                        val trackId = session.data?.scannedTrack?.id
                        if (save != null &&
                            trackId != null &&
                            metadataEditor?.token == session.token &&
                            !session.saving) {
                            metadataEditor =
                                session.copy(saving = true, failed = false)
                            editScope.launch {
                                val succeeded =
                                    try {
                                        save(trackId, correction)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Throwable) {
                                        false
                                    }
                                if (metadataEditor?.token == session.token) {
                                    metadataEditor =
                                        if (succeeded) null
                                        else
                                            session.copy(
                                                saving = false, failed = true)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun LibraryShellBottomBar(
    content: LibraryBottomBarContent,
    presentation: LibraryBottomBarPresentation,
    selectedTrack: Track?,
    playbackState: PlaybackState,
    artworkLoader: suspend (String) -> ByteArray?,
    onCancelSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onExpand: () -> Unit,
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    screenHeightPx: Float = 0f,
    minimumHeight: Dp,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    when (content) {
        is LibraryBottomBarContent.Selection ->
            TrackSelectionBar(
                selectedCount = content.selectedCount,
                onCancel = onCancelSelection,
                onSelectAll = onSelectAll,
                onAddToPlaylist = onAddToPlaylist,
                interactive = presentation.isInteractive,
                modifier = modifier.testTag(SelectionShellPlacementTestTag),
            )

        LibraryBottomBarContent.NowPlaying -> {
            val currentTrack =
                checkNotNull(selectedTrack) {
                    "The now-playing dock requires the current library track"
                }
            NowPlayingBar(
                modifier =
                    modifier
                        .focusRequester(focusRequester)
                        .heightIn(min = minimumHeight),
                track = currentTrack,
                playbackState = playbackState,
                labels =
                    NowPlayingBarLabels(
                        play = stringResource(Res.string.play),
                        pause = stringResource(Res.string.pause),
                        albumArt = stringResource(Res.string.album_art),
                        currentTrackArtistAlbum =
                            stringResource(
                                Res.string.track_artist_album_format,
                                currentTrack.artist,
                                currentTrack.album,
                            ),
                        nextTrack = stringResource(Res.string.next_track),
                        queue = stringResource(Res.string.queue),
                    ),
                artworkLoader = artworkLoader,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onOpenQueue = onOpenQueue,
                onExpand = onExpand,
                expandProgress = expandProgress,
                isExpanded = isExpanded,
                interactive = presentation.isInteractive,
                screenHeightPx = screenHeightPx,
            )
        }

        LibraryBottomBarContent.Hidden -> Unit
    }
}

@Composable
private fun NowPlayingExpandOverlay(
    track: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    sleepTimerState: SleepTimerState,
    onArmSleepTimer: (minutes: Int, fadeEnabled: Boolean) -> Unit,
    onArmSleepTimerAfterCompletions: (count: Int, fadeEnabled: Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    appState: LibraryAppState,
    playlistRepository: PlaylistRepository,
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
    playlistDestination: PlaylistFeatureDestination,
    playlistAppearanceSource: PlaylistFeatureAppearanceSource,
    artworkLoader: suspend (String) -> ByteArray?,
    isVisible: Boolean,
    expandProgress: Animatable<Float, AnimationVector1D>,
    onBack: () -> Unit,
    favoriteTrackIds: Set<String> = emptySet(),
    onSetTrackFavorite: (trackId: String, favorite: Boolean) -> Unit = { _, _ ->
    },
    modifier: Modifier = Modifier,
) {
    val gestureScope = rememberCoroutineScope()
    val panelPresentation = rememberNowPlayingPanelPresentation(appState)
    val queueMutations =
        remember(playbackController) {
            QueueMutationDispatcher(
                state = playbackController.state,
                reorderCommand = playbackController::reorderUpcoming,
                removeCommand = playbackController::removeUpcoming,
                clearCommand = playbackController::clearUpcoming,
            )
        }
    val queueDismissalPublisher =
        remember(appState, appState.activeDestinationId) {
            featureDismissalPublisher(
                appState.activeDestinationId,
                appState::registerBackSurface,
            )
        }
    if (expandProgress.value > 0.001f && track != null) {
        val fraction = expandProgress.value
        val playerFocusRequester = remember { FocusRequester() }
        val windowOverlayVisible = LocalHausWindowOverlayVisible.current
        val playerInteractive = isVisible
        LaunchedEffect(isVisible) {
            if (isVisible && !windowOverlayVisible) {
                playerFocusRequester.requestFocus()
            }
        }
        Box(
            Modifier.fillMaxSize()
                .testTag("NowPlayingFocusTarget")
                .focusRequester(playerFocusRequester)
                .focusRestorer()
                .focusGroup()
                .focusProperties { canFocus = playerInteractive }
                .focusable(),
        ) {
            val cornerRadius =
                HausShapes.overlay * (1f - fraction).coerceAtLeast(0f)
            Surface(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .align(Alignment.BottomCenter)
                        .verticalSheetGesture(
                            expandProgress = expandProgress,
                            isActive = isVisible,
                            scope = gestureScope,
                            direction =
                                com.eterocell.rhythhaus.ui
                                    .VerticalSheetGestureDirection
                                    .Downward,
                            onTerminal =
                                nowPlayingSwipeCollapseAction(appState),
                        ),
                shape =
                    RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp,
                    ),
                color = HausColors.current.paper,
            ) {
                DestinationInteractionBoundary(
                    interactive = playerInteractive,
                    clearFocusWhenInactive = !windowOverlayVisible,
                ) {
                    NowPlayingScreen(
                        track = track,
                        playbackState = playbackState,
                        playbackController = playbackController,
                        tagLibReader = tagLibReader,
                        currentLibraryTrack = currentLibraryTrack,
                        onBack = onBack,
                        sleepTimerState = sleepTimerState,
                        onArmSleepTimer = onArmSleepTimer,
                        onArmSleepTimerAfterCompletions =
                            onArmSleepTimerAfterCompletions,
                        onCancelSleepTimer = onCancelSleepTimer,
                        activePanel = panelPresentation.panel,
                        onPanelChange = panelPresentation.onPanelChange,
                        queueContent = {
                            PlaybackQueueContent(
                                playbackState = playbackState,
                                destination = playlistDestination,
                                appearanceSource = playlistAppearanceSource,
                                dismissalPublisher = queueDismissalPublisher,
                                windowOverlayPublisher =
                                    LocalHausWindowOverlayPublisher.current,
                                onClose = {
                                    panelPresentation.onPanelChange(
                                        NowPlayingPanel.Controls,
                                    )
                                },
                                onSaveQueueAsPlaylist = {
                                    name,
                                    trackIds,
                                    onOutcome ->
                                    onPlaylistMutation(
                                        {
                                            playlistRepository
                                                .createWithEntries(
                                                    name,
                                                    trackIds,
                                                )
                                        },
                                        onOutcome,
                                    )
                                },
                                onReorderUpcoming = queueMutations::reorder,
                                onRemoveUpcoming = queueMutations::remove,
                                onClearUpcoming = queueMutations::clear,
                                artworkLoader = artworkLoader,
                            )
                        },
                        favoriteTrackIds = favoriteTrackIds,
                        onSetTrackFavorite = onSetTrackFavorite,
                    )
                }
            }
        }
    }
}

internal fun nowPlayingSwipeCollapseAction(
    appState: LibraryAppState,
): () -> Unit = appState::hideNowPlaying

private const val NavigationAnimationMillis = 200
private const val NavigationSlideDistancePx = 90

@Composable
internal fun DestinationInteractionBoundary(
    interactive: Boolean,
    clearFocusWhenInactive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(interactive, clearFocusWhenInactive) {
        if (!interactive && clearFocusWhenInactive) {
            focusManager.clearFocus(force = true)
        }
    }
    Box(
        Modifier.fillMaxSize()
            .then(
                if (interactive) Modifier
                else
                    Modifier.focusProperties { canFocus = false }
                        .clearAndSetSemantics {},
            )
            .then(
                if (interactive) Modifier
                else
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes
                                    .forEach { it.consume() }
                            }
                        }
                    },
            ),
    ) {
        content()
    }
}

@Composable
internal fun ForegroundInteractionBoundary(
    interactive: Boolean,
    restoreFocusWhenInteractive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val backgroundFocusRequester = remember { FocusRequester() }
    var wasInactive by remember { mutableStateOf(false) }
    LaunchedEffect(interactive, restoreFocusWhenInteractive) {
        if (wasInactive && interactive && restoreFocusWhenInteractive) {
            withFrameNanos {}
            backgroundFocusRequester.requestFocus()
        }
        wasInactive = !interactive
    }
    Box(
        Modifier.fillMaxSize()
            .focusRequester(backgroundFocusRequester)
            .focusRestorer()
            .focusGroup()
            .then(
                if (interactive) Modifier
                else
                    Modifier.focusProperties { canFocus = false }
                        .clearAndSetSemantics {},
            )
            .then(
                if (interactive) Modifier
                else
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes
                                    .forEach { it.consume() }
                            }
                        }
                    },
            ),
    ) {
        content()
    }
}

private fun routeContentTransform(
    transition: LibraryNavigationTransition,
    reducedMotion: Boolean,
): ContentTransform =
    when (transition) {
        LibraryNavigationTransition.Push ->
            routeSlideContentTransform(
                forward = true,
                reducedMotion = reducedMotion,
            )

        LibraryNavigationTransition.Pop,
        LibraryNavigationTransition.Root,
        ->
            routeSlideContentTransform(
                forward = false,
                reducedMotion = reducedMotion,
            )

        LibraryNavigationTransition.Replace ->
            routeFadeContentTransform(reducedMotion)

        LibraryNavigationTransition.None ->
            routeFadeContentTransform(reducedMotion)
    }

private fun routeSlideContentTransform(
    forward: Boolean,
    reducedMotion: Boolean,
): ContentTransform {
    val direction = if (forward) 1 else -1
    val fadeAnimationSpec: FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(NavigationAnimationMillis)
    val slideAnimationSpec: FiniteAnimationSpec<IntOffset> =
        if (reducedMotion) snap() else tween(NavigationAnimationMillis)
    return (fadeIn(animationSpec = fadeAnimationSpec) +
            slideInHorizontally(
                animationSpec = slideAnimationSpec,
                initialOffsetX = { NavigationSlideDistancePx * direction },
            ))
        .togetherWith(
            fadeOut(animationSpec = fadeAnimationSpec) +
                slideOutHorizontally(
                    animationSpec = slideAnimationSpec,
                    targetOffsetX = { -NavigationSlideDistancePx * direction },
                ),
        )
}

private fun routeFadeContentTransform(
    reducedMotion: Boolean
): ContentTransform {
    val animationSpec: FiniteAnimationSpec<Float> =
        if (reducedMotion) snap() else tween(NavigationAnimationMillis)
    return fadeIn(
            animationSpec = animationSpec,
        )
        .togetherWith(
            fadeOut(animationSpec = animationSpec),
        )
}
