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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.taglib.TagLibReader
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.adaptive_detail_placeholder
import rhythhaus.shared.generated.resources.library
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.nowplaying.NowPlayingBar
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarHeightPx
import com.eterocell.rhythhaus.nowplaying.NowPlayingScreen
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.ui.verticalSheetGesture

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
fun LibraryHomeScreen(
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by playbackController.state.collectAsState()
    val appState = rememberLibraryAppState(snapshot = snapshot)
    LaunchedEffect(playbackState.currentTrack?.id) {
        appState.syncSelectedTrackWithPlayback(playbackState.currentTrack?.id)
    }
    val selectedTrack = snapshot.tracks.firstOrNull { it.id == appState.selectedTrackId } ?: snapshot.tracks.firstOrNull()
    val homeListState = rememberLazyListState()
    val expandProgress = remember { Animatable(0f) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(appState.showNowPlaying) {
        val target = if (appState.showNowPlaying) 1f else 0f
        expandProgress.animateTo(target, tween(300))
    }
    var backGestureProgressAtCompletion by remember { mutableStateOf<Float?>(null) }
    val navState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navState,
        isBackEnabled = appState.showNowPlaying || appState.navigation.canPop,
        onBackCancelled = { },
        onBackCompleted = {
            if (appState.showNowPlaying) {
                appState.hideNowPlaying()
            } else {
                backGestureProgressAtCompletion = when (val ts = navState.transitionState) {
                    is NavigationEventTransitionState.InProgress -> ts.latestEvent.progress
                    else -> null
                }
                // Skip the AnimatedContent slide when predictive back drove the pop
                val next = appState.navigation.pop()
                backGestureProgressAtCompletion = null
                appState.completePredictivePop(next)
            }
        },
    )
    val albums = remember(snapshot.tracks) { groupTracksByAlbum(snapshot.tracks) }
    val artists = remember(snapshot.tracks) { groupTracksByArtist(snapshot.tracks) }

    val predictiveBackProgress = when (val ts = navState.transitionState) {
        is NavigationEventTransitionState.InProgress -> {
            if (ts.direction == NavigationEventTransitionState.TRANSITIONING_BACK) ts.latestEvent.progress
            else 0f
        }
        else -> 0f
    }
    val predictiveBackOffset = remember { Animatable(0f) }
    LaunchedEffect(predictiveBackProgress) {
        if (predictiveBackProgress > 0f) {
            predictiveBackOffset.snapTo(40 * predictiveBackProgress)
        } else {
            predictiveBackOffset.animateTo(0f, tween(150))
        }
    }

    val previousRoute = if (appState.navigation.routes.size >= 2) appState.navigation.routes[appState.navigation.routes.size - 2] else null

    LaunchedEffect(homeListState.firstVisibleItemIndex, homeListState.firstVisibleItemScrollOffset) {
        appState.updateNowPlayingBarVisibilityForScroll(homeListState.toLibraryScrollPosition())
    }
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
    fun RouteOverlays(route: LibraryRoute, backdrop: LayerBackdrop?) {
        LibraryRouteOverlays(
            route = route,
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = playbackController,
            playbackState = playbackState,
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            currentThemeMode = currentThemeMode,
            backdrop = backdrop,
            onThemeModeSelected = onThemeModeSelected,
            onClearLibrary = onClearLibrary,
            onRescanSource = onRescanSource,
            onRemoveSource = onRemoveSource,
            onCancelScan = onCancelScan,
            onDismiss = appState::popRoute,
            onScrollPositionChanged = appState::updateNowPlayingBarVisibilityForScroll,
        )
    }

    @Composable
    fun RouteContent(route: LibraryRoute) {
        LibraryRouteContent(
            route = route,
            albums = albums,
            artists = artists,
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = playbackController,
            playbackState = playbackState,
            selectedTrackId = appState.selectedTrackId,
            isNowPlayingBarVisible = appState.isNowPlayingBarVisible,
            onBack = appState::popRoute,
            onOpenDetailRoute = appState::pushRoute,
            onTrackSelected = appState::setSelectedTrackId,
            onTrackClickFromTracks = ::selectTrackFromTracks,
            onExpandNowPlaying = ::expandNowPlaying,
            onShowSettings = { appState.pushRoute(LibraryRoute.Settings) },
            onShowSearch = { appState.pushRoute(LibraryRoute.Search) },
            onScrollPositionChanged = appState::updateNowPlayingBarVisibilityForScroll,
            homeContent = { onOpenDetailRoute ->
                LibraryHomeContent(
                    snapshot = snapshot,
                    albums = albums,
                    artists = artists,
                    browseMode = appState.browseMode,
                    homeListState = homeListState,
                    folderPickerLauncher = folderPickerLauncher,
                    sourcePickerActionVisible = sourcePickerActionVisible,
                    importMessage = importMessage,
                    scanProgress = scanProgress,
                    scanJob = scanJob,
                    selectedTrackId = appState.selectedTrackId,
                    playbackController = playbackController,
                    homeBackdrop = rememberRhythHausBackdrop(),
                    onBrowseModeChange = appState::setBrowseMode,
                    onClearLibrary = onClearLibrary,
                    onCancelScan = onCancelScan,
                    onOpenDetailRoute = onOpenDetailRoute,
                    onTrackSelected = appState::setSelectedTrackId,
                )
                RouteOverlays(route = route, backdrop = null)
            },
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .onSizeChanged { screenHeightPx = it.height.toFloat() },
    ) {
        val rootBackdrop = rememberRhythHausBackdrop()
        val adaptiveLayoutMode = libraryAdaptiveLayoutModeFor(
            widthDp = maxWidth.value,
            heightDp = maxHeight.value,
        )
        fun openDetailRoute(route: LibraryRoute) {
            appState.openDetailRoute(route = route, adaptiveLayoutMode = adaptiveLayoutMode)
        }

        if (adaptiveLayoutMode == LibraryAdaptiveLayoutMode.ListDetail) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .recordRhythHausBackdrop(rootBackdrop),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.42f),
                    ) {
                        LibraryHomeContent(
                            snapshot = snapshot,
                            albums = albums,
                            artists = artists,
                            browseMode = appState.browseMode,
                            homeListState = homeListState,
                            folderPickerLauncher = folderPickerLauncher,
                            sourcePickerActionVisible = sourcePickerActionVisible,
                            importMessage = importMessage,
                            scanProgress = scanProgress,
                            scanJob = scanJob,
                            selectedTrackId = appState.selectedTrackId,
                            playbackController = playbackController,
                            homeBackdrop = rememberRhythHausBackdrop(),
                            onBrowseModeChange = appState::setBrowseMode,
                            onClearLibrary = onClearLibrary,
                            onCancelScan = onCancelScan,
                            onOpenDetailRoute = ::openDetailRoute,
                            onTrackSelected = appState::setSelectedTrackId,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.58f),
                    ) {
                        when (val route = appState.navigation.current) {
                            is LibraryRoute.AlbumDetail, is LibraryRoute.ArtistDetail -> {
                                RouteContent(route = route)
                            }

                            else -> AdaptiveDetailPlaceholder()
                        }
                    }
                }
                RouteOverlays(route = appState.navigation.current, backdrop = rootBackdrop)
            }
        } else {
            if (predictiveBackProgress > 0f && previousRoute != null) {
                RouteContent(route = previousRoute)
            }
            AnimatedContent(
                targetState = appState.navigation.current,
                transitionSpec = {
                    routeContentTransform(appState.lastNavigationTransition)
                },
                label = "LibraryRouteTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .recordRhythHausBackdrop(rootBackdrop)
                    .offset(x = predictiveBackOffset.value.dp),
            ) { currentRoute ->
                RouteContent(route = currentRoute)
            }
        }

        // Fixed bottom bar (outside AnimatedContent). It stays in composition so
        // returning from Now Playing does not re-trigger the enter animation when
        // the bar was already visible underneath the overlay.
        val bottomBarOffset by animateFloatAsState(
            targetValue = if (appState.isNowPlayingBarVisible) 0f else 1f,
            animationSpec = tween(250),
            label = "BottomBarOffset",
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (bottomBarOffset * NowPlayingBarHeightPx).dp)
                .alpha(1f - bottomBarOffset),
        ) {
            NowPlayingBar(
                track = selectedTrack,
                playbackState = playbackState,
                onPlayPause = {
                    playbackController.togglePlayPause()
                },
                onExpand = { if (selectedTrack != null) appState.showNowPlaying() },
                onSettings = { appState.pushRoute(LibraryRoute.Settings) },
                onSearch = { appState.pushRoute(LibraryRoute.Search) },
                expandProgress = expandProgress,
                isExpanded = appState.showNowPlaying,
                screenHeightPx = screenHeightPx,
                backdrop = rootBackdrop,
            )
        }

        // Now Playing expand overlay (outside AnimatedContent)
        NowPlayingExpandOverlay(
            track = selectedTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            currentLibraryTrack = libraryTracks.firstOrNull { it.id == selectedTrack?.id },
            isVisible = appState.showNowPlaying,
            expandProgress = expandProgress,
            onBack = { appState.hideNowPlaying() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AdaptiveDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .align(Alignment.BottomCenter)
                    .verticalSheetGesture(
                        expandProgress = expandProgress,
                        isActive = true,
                        scope = gestureScope,
                        onSwipeExpand = {},
                        onSwipeCollapse = onBack,
                    ),
                shape = RoundedCornerShape(
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

private const val NavigationAnimationMillis = 240
private const val NavigationSlideDistancePx = 90

private fun routeContentTransform(transition: LibraryNavigationTransition): ContentTransform = when (transition) {
    LibraryNavigationTransition.Push -> routeSlideContentTransform(forward = true)
    LibraryNavigationTransition.Pop,
    LibraryNavigationTransition.Root,
    -> routeSlideContentTransform(forward = false)
    LibraryNavigationTransition.Replace -> routeFadeContentTransform()
    LibraryNavigationTransition.None -> routeFadeContentTransform()
}

private fun routeSlideContentTransform(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (
        fadeIn(animationSpec = tween(NavigationAnimationMillis)) +
            slideInHorizontally(
                animationSpec = tween(NavigationAnimationMillis),
                initialOffsetX = { NavigationSlideDistancePx * direction },
            )
        ).togetherWith(
        fadeOut(animationSpec = tween(NavigationAnimationMillis)) +
            slideOutHorizontally(
                animationSpec = tween(NavigationAnimationMillis),
                targetOffsetX = { -NavigationSlideDistancePx * direction },
            ),
    )
}

private fun routeFadeContentTransform(): ContentTransform = fadeIn(
    animationSpec = tween(NavigationAnimationMillis),
).togetherWith(
    fadeOut(animationSpec = tween(NavigationAnimationMillis)),
)
