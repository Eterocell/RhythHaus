package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.nowplaying.NowPlayingBar
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarLabels
import com.eterocell.rhythhaus.nowplaying.NowPlayingScreen
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.ui.RhythHausBackdrop
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import com.eterocell.rhythhaus.ui.verticalSheetGesture
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.adaptive_detail_placeholder
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.album_art
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.library
import rhythhaus.shared.generated.resources.library_queue
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.playlists_accessibility
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.track_artist_album_format
import top.yukonga.miuix.kmp.basic.Surface

internal const val NowPlayingShellPlacementTestTag = "NowPlayingShellPlacement"
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
        playlists = stringResource(Res.string.playlists),
        playlistsAccessibility =
            stringResource(Res.string.playlists_accessibility),
        libraryQueue = stringResource(Res.string.library_queue),
        albumArt = stringResource(Res.string.album_art),
        albumArtwork = stringResource(Res.string.album_artwork),
        nowPlayingBadge = stringResource(Res.string.now_playing_badge),
        selectTrack = { title ->
            stringResource(Res.string.select_track_format, title)
        },
        trackArtistAlbum = { artist, album ->
            stringResource(Res.string.track_artist_album_format, artist, album)
        },
    )

private fun LazyListState.toLibraryScrollPosition(): LibraryScrollPosition =
    LibraryScrollPosition(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    )

/**
 * Applies a home browse-mode change, clearing the songs selection through the
 * route-change reducer exactly when leaving the songs browse mode.
 */
internal fun dispatchHomeBrowseModeChange(
    currentMode: BrowseMode,
    nextMode: BrowseMode,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
) {
    if (currentMode == BrowseMode.Songs && nextMode != BrowseMode.Songs) {
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
    fun recoverStalePlaylistDetail(message: String) {
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
    onConfirmPlaylistBackup: () -> Unit,
    onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanErrors: List<ScanError>,
    scanJob: Job?,
    coordinatorMutationsEnabled: Boolean,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by playbackController.state.collectAsState()
    val appState = rememberLibraryAppState(snapshot = snapshot)
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
        snapshot.tracks.firstOrNull { it.id == appState.selectedTrackId }
            ?: snapshot.tracks.firstOrNull()
    val expandProgress = remember { Animatable(0f) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(appState.showNowPlaying) {
        val target = if (appState.showNowPlaying) 1f else 0f
        expandProgress.animateTo(target, tween(300))
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
    var searchVisibleTrackIds by remember {
        mutableStateOf(emptyList<String>())
    }
    var bottomBarMeasurement by remember {
        mutableStateOf<LibraryBottomBarMeasurement?>(null)
    }
    val bottomBarContent =
        libraryBottomBarContent(
            route = appState.navigation.current,
            selectionState = trackSelectionState,
            isNowPlayingVisible = appState.isNowPlayingBarVisible,
        )
    val density = LocalDensity.current
    val artworkLoader = LocalTrackArtworkLoader.current
    val activeBottomBarClearance =
        with(density) {
            activeBottomBarClearancePx(bottomBarContent, bottomBarMeasurement)
                .toDp()
        }
    fun dispatchTrackSelection(action: TrackSelectionAction) {
        if (action is TrackSelectionAction.ReconcileVisible &&
            action.pageKey == TrackSelectionPageKey.Search) {
            searchVisibleTrackIds = action.visibleTrackIds
        }
        trackSelectionState = reduceTrackSelection(trackSelectionState, action)
    }
    fun clearSelection() {
        dispatchTrackSelection(TrackSelectionAction.RouteChanged(null))
    }
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
        clearSelection()
        appState.pushRoute(route)
    }
    fun openSelectedTracksPicker() {
        val pageKey = trackSelectionState.pageKey ?: return
        val visibleTrackIds =
            when (pageKey) {
                TrackSelectionPageKey.HomeSongs ->
                    snapshot.tracks.map(Track::id)
                is TrackSelectionPageKey.Album ->
                    albums.filter { it.album == pageKey.album }.map(Track::id)
                is TrackSelectionPageKey.Artist ->
                    artists
                        .filter { it.artist == pageKey.artist }
                        .map(Track::id)
                TrackSelectionPageKey.Search -> searchVisibleTrackIds
            }
        val orderedIds =
            orderedSelectedTrackIds(
                trackSelectionState, pageKey, visibleTrackIds)
        if (orderedIds.isNotEmpty())
            onPlaylistStateAction(
                PlaylistStateAction.OpenPicker(PlaylistPickerState(orderedIds)))
    }
    SideEffect {
        appState.publishSelectionPort(selectionPort)
        appState.reconcileBackSession(selectionPort)
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

    fun expandNowPlaying(track: Track) {
        appState.setSelectedTrackId(track.id)
        appState.showNowPlaying()
    }

    @Composable
    fun RouteOverlays(route: LibraryRoute) {
        LibraryRouteOverlays(
            route = route,
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            playbackController = playbackController,
            playbackState = playbackState,
            playlistRepository = playlistRepository,
            playlistState = playlistState,
            playlistBackupState = playlistBackupState,
            backupDocumentAvailable = backupDocumentAvailable,
            destinationId = appState.activeDestinationId,
            playlistAppearanceSource = activePlaylistAppearanceSource,
            registerBackSurface = appState::registerBackSurface,
            onPlaylistStateAction = onPlaylistStateAction,
            onRefreshPlaylists = onRefreshPlaylists,
            onPlaylistMutation = onPlaylistMutation,
            onExportPlaylists = onExportPlaylists,
            onOpenPlaylistBackup = onOpenPlaylistBackup,
            onConfirmPlaylistBackup = onConfirmPlaylistBackup,
            onPlaylistBackupAction = onPlaylistBackupAction,
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            mutationsEnabled = coordinatorMutationsEnabled,
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            onClearLibrary = onClearLibrary,
            onRescanSource = onRescanSource,
            onRemoveSource = onRemoveSource,
            onCancelScan = onCancelScan,
            onShowSettingsAbout = { pushRoute(LibraryRoute.SettingsAbout) },
            onShowOpenSourceLibraries = {
                pushRoute(LibraryRoute.OpenSourceLibraries)
            },
            onDismiss = requestLibraryBack,
            onScrollPositionChanged =
                appState::updateNowPlayingBarVisibilityForScroll,
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = ::dispatchTrackSelection,
            bottomContentPadding = activeBottomBarClearance,
        )
    }

    @Composable
    fun RouteContent(entry: LibraryNavigationEntry) {
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
                    message)
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
            onOpenDetailRoute = ::pushRoute,
            onTrackSelected = appState::setSelectedTrackId,
            onTrackClickFromTracks = ::selectTrackFromTracks,
            onExpandNowPlaying = ::expandNowPlaying,
            onShowSettings = { pushRoute(LibraryRoute.Settings) },
            onShowSearch = { pushRoute(LibraryRoute.Search) },
            onScrollPositionChanged =
                appState::updateNowPlayingBarVisibilityForScroll,
            artworkLoader = { id -> artworkLoader(id)?.bytes },
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = ::dispatchTrackSelection,
            bottomContentPadding = activeBottomBarClearance,
            homeContent = { onOpenDetailRoute ->
                LibraryHomeContent(
                    title = snapshot.title,
                    subtitle = snapshot.subtitle,
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
                    onShowPlaylists = { pushRoute(LibraryRoute.PlaylistHub) },
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
                    bottomContentPadding = activeBottomBarClearance,
                )
                if (libraryRouteRendersAsActiveOverlay(
                    route = route,
                    mode = LibraryAdaptiveLayoutMode.Compact,
                )) {
                    RouteOverlays(route = route)
                }
            },
        )
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(HausColors.current.paper)
                .onSizeChanged { screenHeightPx = it.height.toFloat() },
    ) {
        val rootBackdrop = rememberRhythHausBackdrop()
        val adaptiveLayoutMode =
            libraryAdaptiveLayoutModeFor(
                widthDp = maxWidth.value,
                heightDp = maxHeight.value,
            )
        fun openDetailRoute(route: LibraryRoute) {
            clearSelection()
            appState.openDetailRoute(
                route = route, adaptiveLayoutMode = adaptiveLayoutMode)
        }

        if (adaptiveLayoutMode == LibraryAdaptiveLayoutMode.ListDetail) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier.fillMaxSize()
                            .recordRhythHausBackdrop(rootBackdrop),
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(0.42f),
                    ) {
                        LibraryHomeContent(
                            title = snapshot.title,
                            subtitle = snapshot.subtitle,
                            tracks = snapshot.tracks,
                            browseMode = appState.browseMode,
                            folderPickerLauncher = folderPickerLauncher,
                            sourcePickerActionVisible =
                                sourcePickerActionVisible,
                            importMessage = importMessage,
                            scanProgress = scanProgress,
                            mutationsEnabled = coordinatorMutationsEnabled,
                            currentTrackId = playbackState.currentTrack?.id,
                            selectionModeActive =
                                trackSelectionState.pageKey ==
                                    TrackSelectionPageKey.HomeSongs &&
                                    trackSelectionState.selectedTrackIds
                                        .isNotEmpty(),
                            selectedTrackIds =
                                if (trackSelectionState.pageKey ==
                                    TrackSelectionPageKey.HomeSongs)
                                    trackSelectionState.selectedTrackIds
                                else emptySet(),
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
                                openDetailRoute(LibraryRoute.AlbumDetail(album))
                            },
                            onOpenArtist = { artist ->
                                openDetailRoute(
                                    LibraryRoute.ArtistDetail(artist))
                            },
                            onShowPlaylists = {
                                pushRoute(LibraryRoute.PlaylistHub)
                            },
                            onPlayTrack = { orderedTracks, selectedTrack ->
                                appState.setSelectedTrackId(selectedTrack.id)
                                selectTrackFromTracks(
                                    orderedTracks, selectedTrack)
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
                            bottomContentPadding = activeBottomBarClearance,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(0.58f),
                    ) {
                        when (val route = appState.navigation.current) {
                            is LibraryRoute.AlbumDetail,
                            is LibraryRoute.ArtistDetail,
                            is LibraryRoute.PlaylistDetail,
                            LibraryRoute.PlaylistHub,
                            -> {
                                RouteContent(
                                    entry = appState.navigation.currentEntry)
                            }

                            else -> AdaptiveDetailPlaceholder()
                        }
                    }
                }
                if (libraryRouteRendersAsActiveOverlay(
                    route = appState.navigation.current,
                    mode = adaptiveLayoutMode,
                )) {
                    RouteOverlays(route = appState.navigation.current)
                }
            }
        } else {
            if (predictiveBackProgress > 0f && previousEntry != null) {
                RouteContent(entry = previousEntry)
            }
            AnimatedContent(
                targetState = appState.navigation.currentEntry,
                transitionSpec = {
                    routeContentTransform(appState.lastNavigationTransition)
                },
                label = "LibraryRouteTransition",
                modifier =
                    Modifier.fillMaxSize()
                        .recordRhythHausBackdrop(rootBackdrop)
                        .offset(x = predictiveBackOffset.value.dp),
            ) { currentEntry ->
                RouteContent(entry = currentEntry)
            }
        }

        // Fixed bottom bar (outside AnimatedContent). It stays in composition
        // so
        // returning from Now Playing does not re-trigger the enter animation
        // when
        // the bar was already visible underneath the overlay.
        val bottomBarOffset by
            animateFloatAsState(
                targetValue =
                    if (bottomBarContent == LibraryBottomBarContent.Hidden) 1f
                    else 0f,
                animationSpec = tween(250),
                label = "BottomBarOffset",
            )
        val bottomBarPresentation =
            libraryBottomBarPresentation(
                content = bottomBarContent,
                measurement = bottomBarMeasurement,
                hiddenFraction = bottomBarOffset,
            )
        if (bottomBarContent != LibraryBottomBarContent.Hidden) {
            key(bottomBarContent) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .onSizeChanged {
                                bottomBarMeasurement =
                                    LibraryBottomBarMeasurement(
                                        bottomBarContent, it.height)
                            }
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y =
                                        nowPlayingBarOffsetPx(
                                            hiddenFraction = bottomBarOffset,
                                            measuredHeightPx =
                                                bottomBarPresentation
                                                    .clearancePx,
                                        ),
                                )
                            }
                            .alpha(bottomBarPresentation.alpha),
                ) {
                    LibraryShellBottomBar(
                        content = bottomBarContent,
                        presentation = bottomBarPresentation,
                        selectedTrack = selectedTrack,
                        playbackState = playbackState,
                        artworkLoader = { trackId ->
                            artworkLoader(trackId)?.bytes
                        },
                        onCancelSelection = {
                            dispatchTrackSelection(TrackSelectionAction.Cancel)
                        },
                        onAddToPlaylist = ::openSelectedTracksPicker,
                        onPlayPause = playbackController::togglePlayPause,
                        onExpand = {
                            if (selectedTrack != null) appState.showNowPlaying()
                        },
                        onSettings = { pushRoute(LibraryRoute.Settings) },
                        onSearch = { pushRoute(LibraryRoute.Search) },
                        expandProgress = expandProgress,
                        isExpanded = appState.showNowPlaying,
                        screenHeightPx = screenHeightPx,
                        backdrop = rootBackdrop,
                    )
                }
            }
        }

        // Now Playing expand overlay (outside AnimatedContent)
        NowPlayingExpandOverlay(
            track = selectedTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            currentLibraryTrack =
                libraryTracks.firstOrNull { it.id == selectedTrack?.id },
            isVisible = appState.showNowPlaying,
            expandProgress = expandProgress,
            onBack = requestLibraryBack,
            modifier = Modifier.fillMaxSize(),
        )

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
                                selectedPlaylistId = updated.selectedPlaylistId,
                                enteredName = updated.enteredName,
                            ),
                        ),
                    )
                },
                onDismiss = {
                    onPlaylistStateAction(PlaylistStateAction.ClosePicker)
                },
                onAppend = { playlistId, trackIds, onOutcome ->
                    onPlaylistMutation(
                        { append(playlistId, trackIds) },
                        { outcome ->
                            onOutcome(outcome)
                            if (outcome
                                is PlaylistStateAction.SnapshotConfirmed) {
                                onPlaylistStateAction(
                                    PlaylistStateAction.ClosePicker)
                                dispatchTrackSelection(
                                    TrackSelectionAction.Cancel)
                            }
                        },
                    )
                },
                onInlineCreate = { name, trackIds, onOutcome ->
                    onPlaylistMutation(
                        {
                            createWithEntries(name, trackIds)
                        },
                        { outcome ->
                            onOutcome(outcome)
                            if (outcome
                                is PlaylistStateAction.SnapshotConfirmed) {
                                onPlaylistStateAction(
                                    PlaylistStateAction.ClosePicker)
                                dispatchTrackSelection(
                                    TrackSelectionAction.Cancel)
                            }
                        },
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
                                    visibleTrackIds = updated.visibleTrackIds,
                                    selectedTrackIds = updated.selectedTrackIds,
                                ),
                            ),
                        )
                    },
                    onDismiss = {
                        onPlaylistStateAction(PlaylistStateAction.CloseBrowser)
                    },
                    onConfirm = { playlistId, trackIds, onOutcome ->
                        onPlaylistMutation(
                            { append(playlistId, trackIds) },
                            { outcome ->
                                onOutcome(outcome)
                                if (outcome
                                    is PlaylistStateAction.SnapshotConfirmed) {
                                    onPlaylistStateAction(
                                        PlaylistStateAction.CloseBrowser)
                                }
                            },
                        )
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
    onAddToPlaylist: () -> Unit,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    screenHeightPx: Float = 0f,
    backdrop: RhythHausBackdrop? = null,
) {
    when (content) {
        is LibraryBottomBarContent.Selection ->
            TrackSelectionBar(
                selectedCount = content.selectedCount,
                onCancel = onCancelSelection,
                onAddToPlaylist = onAddToPlaylist,
                interactive = presentation.isInteractive,
                modifier = Modifier.testTag(SelectionShellPlacementTestTag),
            )

        LibraryBottomBarContent.NowPlaying ->
            NowPlayingBar(
                modifier = Modifier.testTag(NowPlayingShellPlacementTestTag),
                track = selectedTrack,
                playbackState = playbackState,
                labels =
                    NowPlayingBarLabels(
                        play = stringResource(Res.string.play),
                        pause = stringResource(Res.string.pause),
                        search = stringResource(Res.string.search),
                        settings = stringResource(Res.string.settings),
                        albumArt = stringResource(Res.string.album_art),
                        currentTrackArtistAlbum =
                            selectedTrack?.let {
                                stringResource(
                                    Res.string.track_artist_album_format,
                                    it.artist,
                                    it.album,
                                )
                            } ?: "",
                    ),
                artworkLoader = artworkLoader,
                onPlayPause = onPlayPause,
                onExpand = onExpand,
                onSettings = onSettings,
                onSearch = onSearch,
                expandProgress = expandProgress,
                isExpanded = isExpanded,
                interactive = presentation.isInteractive,
                screenHeightPx = screenHeightPx,
                backdrop = backdrop,
            )

        LibraryBottomBarContent.Hidden -> Unit
    }
}

@Composable
private fun AdaptiveDetailPlaceholder() {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(HausColors.current.paper)
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                text = stringResource(Res.string.library),
                color = HausColors.current.ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            top.yukonga.miuix.kmp.basic.Text(
                text = stringResource(Res.string.adaptive_detail_placeholder),
                color = HausColors.current.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun NowPlayingExpandOverlay(
    track: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    isVisible: Boolean,
    expandProgress: Animatable<Float, AnimationVector1D>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureScope = rememberCoroutineScope()
    if (expandProgress.value > 0.001f && track != null) {
        val fraction = expandProgress.value
        Box(modifier = modifier) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .align(Alignment.BottomCenter)
                        .verticalSheetGesture(
                            expandProgress = expandProgress,
                            isActive = true,
                            scope = gestureScope,
                            direction =
                                com.eterocell.rhythhaus.ui
                                    .VerticalSheetGestureDirection
                                    .Downward,
                            onTerminal = nowPlayingSwipeCollapseAction(onBack),
                        ),
                shape =
                    RoundedCornerShape(
                        topStart = (24 * (1f - fraction).coerceAtLeast(0f)).dp,
                        topEnd = (24 * (1f - fraction).coerceAtLeast(0f)).dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp,
                    ),
                color = HausColors.current.paper,
            ) {
                NowPlayingScreen(
                    track = track,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    currentLibraryTrack = currentLibraryTrack,
                    onBack = onBack,
                )
            }
        }
    }
}

/**
 * Kept as a named seam so threshold-swipe and screen Back share the same
 * callback.
 */
internal fun nowPlayingSwipeCollapseAction(onBack: () -> Unit): () -> Unit =
    onBack

private const val NavigationAnimationMillis = 240
private const val NavigationSlideDistancePx = 90

private fun routeContentTransform(
    transition: LibraryNavigationTransition
): ContentTransform =
    when (transition) {
        LibraryNavigationTransition.Push ->
            routeSlideContentTransform(forward = true)

        LibraryNavigationTransition.Pop,
        LibraryNavigationTransition.Root,
        -> routeSlideContentTransform(forward = false)

        LibraryNavigationTransition.Replace -> routeFadeContentTransform()

        LibraryNavigationTransition.None -> routeFadeContentTransform()
    }

private fun routeSlideContentTransform(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (fadeIn(animationSpec = tween(NavigationAnimationMillis)) +
            slideInHorizontally(
                animationSpec = tween(NavigationAnimationMillis),
                initialOffsetX = { NavigationSlideDistancePx * direction },
            ))
        .togetherWith(
            fadeOut(animationSpec = tween(NavigationAnimationMillis)) +
                slideOutHorizontally(
                    animationSpec = tween(NavigationAnimationMillis),
                    targetOffsetX = { -NavigationSlideDistancePx * direction },
                ),
        )
}

private fun routeFadeContentTransform(): ContentTransform =
    fadeIn(
            animationSpec = tween(NavigationAnimationMillis),
        )
        .togetherWith(
            fadeOut(animationSpec = tween(NavigationAnimationMillis)),
        )
