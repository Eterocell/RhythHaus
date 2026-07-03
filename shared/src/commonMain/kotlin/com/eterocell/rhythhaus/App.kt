package com.eterocell.rhythhaus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformAudioScanner
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.createLibraryDatabase
import com.eterocell.rhythhaus.library.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.createTagLibReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme


@Composable
@Preview
fun App() {
    val controller = remember { PlaybackController() }
    val metadataReader = remember { AudioMetadataReader() }
    val tagLibReader = remember { createTagLibReader() }
    val libraryDb = remember { createLibraryDatabase() }
    val repository = remember { SqlDelightLibraryRepository(libraryDb) }
    val platformAccess = remember { createPlatformSourceAccess() }
    val scanner = remember {
        LibraryScanner(
            repository = repository,
            platformScanner = platformAccess as PlatformAudioScanner,
            metadataReader = metadataReader,
            now = { currentTimeMillis() },
            idFactory = { _ -> uuid4() },
        )
    }
    var libraryTracks by remember { mutableStateOf(repository.tracks()) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val themePreferenceStore = remember { createThemePreferenceStore() }
    val selectedThemeMode by themePreferenceStore.selectedThemeMode.collectAsState(RhythHausThemeMode.System)
    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        when (result) {
            is PlatformFolderPickResult.Success -> {
                val source = result.source
                scanJob = scope.launch(Dispatchers.Default) {
                    var progress = ScanProgress(
                        session = ScanSession(id = "", sourceId = source.id, status = ScanStatus.Scanning, startedAtEpochMillis = 0L),
                    )
                    withContext(Dispatchers.Main) { scanProgress = progress }

                    val session = scanner.scan(source) { scanJob?.isActive != true }

                    withContext(Dispatchers.Main) {
                        scanProgress = ScanProgress(session = session)
                        importMessage = "Scan complete: ${session.tracksAdded} new, ${session.tracksUpdated} updated"
                        libraryTracks = repository.tracks()
                    }
                }
            }

            is PlatformFolderPickResult.Unavailable -> importMessage = result.message

            is PlatformFolderPickResult.Failure -> importMessage = result.cause?.let { "${result.message}: $it" } ?: result.message
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    val snapshot = remember(libraryTracks) { librarySnapshot(libraryTracks) }

    RhythHausTheme(selectedThemeMode = selectedThemeMode) {
        LibraryHomeScreen(
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = controller,
            folderPickerLauncher = folderPickerLauncher,
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
                repository.clearAll()
                libraryTracks = emptyList()
            },
        )
    }
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

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
fun LibraryHomeScreen(
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrackId by remember(snapshot.nowPlayingTrackId) { mutableStateOf(snapshot.nowPlayingTrackId) }
    val selectedTrack = snapshot.tracks.firstOrNull { it.id == selectedTrackId } ?: snapshot.tracks.firstOrNull()
    val playbackState by playbackController.state.collectAsState()
    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.id?.let { selectedTrackId = it }
    }
    var browseMode by remember { mutableStateOf(BrowseMode.Albums) }
    var navigation by remember { mutableStateOf(LibraryNavigationStack()) }
    var lastNavigationTransition by remember { mutableStateOf(LibraryNavigationTransition.None) }
    fun updateNavigation(next: LibraryNavigationStack) {
        lastNavigationTransition = classifyNavigationTransition(navigation, next)
        navigation = next
    }
    fun pushRoute(route: LibraryRoute) {
        updateNavigation(navigation.push(route))
    }
    fun popRoute() {
        updateNavigation(navigation.pop())
    }
    val expandProgress = remember { Animatable(0f) }
    LaunchedEffect(navigation.current == LibraryRoute.NowPlaying) {
        val target = if (navigation.current == LibraryRoute.NowPlaying) 1f else 0f
        expandProgress.animateTo(target, tween(300))
    }
    var backGestureProgressAtCompletion by remember { mutableStateOf<Float?>(null) }
    val navState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navState,
        isBackEnabled = navigation.canPop,
        onBackCancelled = { },
        onBackCompleted = {
            backGestureProgressAtCompletion = when (val ts = navState.transitionState) {
                is NavigationEventTransitionState.InProgress -> ts.latestEvent.progress
                else -> null
            }
            // Skip the AnimatedContent slide when predictive back drove the pop
            val next = navigation.pop()
            backGestureProgressAtCompletion = null
            lastNavigationTransition = LibraryNavigationTransition.None
            navigation = next
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

    Box(modifier = modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = navigation.current,
        transitionSpec = {
            if (targetState == LibraryRoute.NowPlaying) {
                // Keep exiting content visible while the expand overlay grows on top
                EnterTransition.None togetherWith fadeOut(tween(300))
            } else if (initialState == LibraryRoute.NowPlaying) {
                // Show entering content quickly while the collapse overlay shrinks
                fadeIn(tween(50)) togetherWith ExitTransition.None
            } else {
                routeContentTransform(lastNavigationTransition)
            }
        },
        label = "LibraryRouteTransition",
        modifier = Modifier
            .fillMaxSize()
            .offset(x = predictiveBackOffset.value.dp),
    ) { currentRoute ->
        when (val route = currentRoute) {
        is LibraryRoute.AlbumDetail -> {
            val album = albums.firstOrNull { it.album == route.album }
            if (album == null) {
                LaunchedEffect(route) { popRoute() }
                Box(modifier = modifier.fillMaxSize())
            } else {
                val albumTracks = album.tracks
                val selectedAlbumTrackId by remember(album.album) { mutableStateOf(albumTracks.firstOrNull()?.id) }
                val selectedAlbumTrack = albumTracks.firstOrNull { it.id == selectedAlbumTrackId } ?: albumTracks.firstOrNull()
                DrillDownView(
                    title = album.album,
                    subtitle = "${albumTracks.size} tracks · ${album.artist ?: "Unknown artist"}",
                    tracks = albumTracks,
                    selectedTrack = selectedAlbumTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = ::popRoute,
                    onTrackSelected = { /* selection only */ },
                    onPlayPause = { track ->
                        val playableTracks = albumTracks.map { it.toPlayableTrack() }
                        if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                            playbackController.setQueue(playableTracks, track.id)
                        }
                        playbackController.togglePlayPause()
                    },
                    onExpandNowPlaying = { track ->
                        selectedTrackId = track.id
                        pushRoute(LibraryRoute.NowPlaying)
                    },
                    onShowSettings = { pushRoute(LibraryRoute.Settings) },
                    onShowSearch = { pushRoute(LibraryRoute.Search) },
                )
            }
        }

        is LibraryRoute.ArtistDetail -> {
            val artist = artists.firstOrNull { it.artist == route.artist }
            if (artist == null) {
                LaunchedEffect(route) { popRoute() }
                Box(modifier = modifier.fillMaxSize())
            } else {
                val artistTracks = artist.tracks
                val selectedArtistTrackId by remember(artist.artist) { mutableStateOf(artistTracks.firstOrNull()?.id) }
                val selectedArtistTrack = artistTracks.firstOrNull { it.id == selectedArtistTrackId } ?: artistTracks.firstOrNull()
                DrillDownView(
                    title = artist.artist,
                    subtitle = "${artist.albumCount} albums · ${artistTracks.size} tracks",
                    tracks = artistTracks,
                    selectedTrack = selectedArtistTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = ::popRoute,
                    onTrackSelected = { /* selection only */ },
                    onPlayPause = { track ->
                        val playableTracks = artistTracks.map { it.toPlayableTrack() }
                        if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                            playbackController.setQueue(playableTracks, track.id)
                        }
                        playbackController.togglePlayPause()
                    },
                    onExpandNowPlaying = { track ->
                        selectedTrackId = track.id
                        pushRoute(LibraryRoute.NowPlaying)
                    },
                    onShowSettings = { pushRoute(LibraryRoute.Settings) },
                    onShowSearch = { pushRoute(LibraryRoute.Search) },
                )
            }
        }

        LibraryRoute.NowPlaying -> {
            Box(modifier = Modifier.fillMaxSize())
        }

        LibraryRoute.Home,
        LibraryRoute.Settings,
        LibraryRoute.Search,
        LibraryRoute.ClearLibraryDialog,
        -> {
            Box(modifier = modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
                    LazyColumn(
                        modifier = Modifier
                            .safeContentPadding()
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item {
                            HeaderSection(snapshot)
                        }
                        if (snapshot.tracks.isEmpty()) {
                            item {
                                ImportAudioCard(
                                    folderPickerLauncher = folderPickerLauncher,
                                    importMessage = importMessage,
                                    hasImportedTracks = false,
                                    onClearLibrary = onClearLibrary,
                                )
                            }
                        }
                        if (scanProgress?.isActive == true) {
                            item {
                                val sp = scanProgress
                                val ss = sp.session!!
                                ScanningCard(
                                    foldersVisited = ss.foldersVisited,
                                    filesVisited = ss.filesVisited,
                                    tracksAdded = ss.tracksAdded,
                                    onCancel = { scanJob?.cancel() },
                                )
                            }
                        }
                        item {
                            SectionLabel(
                                title = "Library queue",
                                subtitle = null,
                            )
                        }
                        item {
                            BrowseModePicker(
                                browseMode = browseMode,
                                onModeChange = { browseMode = it },
                            )
                        }
                        when (browseMode) {
                            BrowseMode.Albums -> {
                                item {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val columns = albumGridColumnsForWidth(maxWidth.value)
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            albums.chunked(columns).forEach { row ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                ) {
                                                    row.forEach { albumGroup ->
                                                        AlbumCard(
                                                            album = albumGroup,
                                                            modifier = Modifier.weight(1f),
                                                            onClick = { pushRoute(LibraryRoute.AlbumDetail(albumGroup.album)) },
                                                        )
                                                    }
                                                    repeat(columns - row.size) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            BrowseMode.Artists -> {
                                items(artists, key = { it.artist }) { artistGroup ->
                                    ArtistRow(
                                        artist = artistGroup,
                                        onClick = { pushRoute(LibraryRoute.ArtistDetail(artistGroup.artist)) },
                                    )
                                }
                            }

                            BrowseMode.Songs -> {
                                items(snapshot.tracks, key = { it.id }) { track ->
                                    TrackRow(
                                        track = track,
                                        selected = track.id == selectedTrackId,
                                        onClick = {
                                            selectedTrackId = track.id
                                            val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                                            if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                                                playbackController.setQueue(playableTracks, track.id)
                                            }
                                            playbackController.togglePlayPause()
                                        },
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(NowPlayingBarContentPadding)) }
                    }
                }
            }

            if (route == LibraryRoute.ClearLibraryDialog) {
                AnimatedClearLibraryDialogRoute(
                    onDismiss = ::popRoute,
                    onClearLibrary = {
                        onClearLibrary()
                        popRoute()
                    },
                )
            }

            if (route == LibraryRoute.Settings) {
                SettingsScreen(
                    folderPickerLauncher = folderPickerLauncher,
                    importMessage = importMessage,
                    scanProgress = scanProgress,
                    scanJob = scanJob,
                    hasImportedTracks = snapshot.tracks.isNotEmpty(),
                    currentThemeMode = currentThemeMode,
                    onThemeModeSelected = onThemeModeSelected,
                    onClearLibrary = { pushRoute(LibraryRoute.ClearLibraryDialog) },
                    onDismiss = ::popRoute,
                )
            }

            if (route == LibraryRoute.Search) {
                SearchScreen(
                    libraryTracks = libraryTracks,
                    tagLibReader = tagLibReader,
                    playbackController = playbackController,
                    playbackState = playbackState,
                    onDismiss = ::popRoute,
                )
            }
        }
    }
    }

    // Fixed bottom bar (outside AnimatedContent)
    NowPlayingBar(
        track = selectedTrack,
        playbackState = playbackState,
        onPlayPause = {
            selectedTrack?.let { track ->
                val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                    playbackController.setQueue(playableTracks, track.id)
                }
                playbackController.togglePlayPause()
            }
        },
        onExpand = { if (selectedTrack != null) pushRoute(LibraryRoute.NowPlaying) },
        onSettings = { pushRoute(LibraryRoute.Settings) },
        onSearch = { pushRoute(LibraryRoute.Search) },
        expandProgress = expandProgress,
        isExpanded = navigation.current == LibraryRoute.NowPlaying,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    // Now Playing expand overlay (outside AnimatedContent)
    NowPlayingExpandOverlay(
        track = selectedTrack,
        playbackState = playbackState,
        playbackController = playbackController,
        tagLibReader = tagLibReader,
        currentLibraryTrack = libraryTracks.firstOrNull { it.id == selectedTrack?.id },
        isVisible = navigation.current == LibraryRoute.NowPlaying,
        expandProgress = expandProgress,
        onBack = ::popRoute,
        modifier = Modifier.fillMaxSize(),
    )
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
                        scope = rememberCoroutineScope(),
                        onSwipeExpand = {},
                        onSwipeCollapse = onBack,
                    ),
                shape = RoundedCornerShape(
                    topStart = (24 * (1f - fraction)).dp,
                    topEnd = (24 * (1f - fraction)).dp,
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
                    modifier = Modifier.alpha(fraction),
                )
            }
        }
    }
}

@Composable
private fun AnimatedClearLibraryDialogRoute(
    onDismiss: () -> Unit,
    onClearLibrary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HausColors.current.ink.copy(alpha = 0.36f))
            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onTap = { }) },
            cornerRadius = 24.dp,
            colors = CardDefaults.defaultColors(color = HausColors.current.panel),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Clear Library",
                    color = HausColors.current.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "This will remove all scanned tracks. Your music files are not deleted. Continue?",
                    color = HausColors.current.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp),
                        cornerRadius = 12.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.muted.copy(alpha = 0.15f),
                            contentColor = HausColors.current.muted,
                        ),
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onClearLibrary,
                        modifier = Modifier.height(36.dp),
                        cornerRadius = 12.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.pulse,
                            contentColor = HausColors.current.paper,
                        ),
                    ) {
                        Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
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

@Composable
private fun HeaderSection(snapshot: LibrarySnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = snapshot.title,
            color = HausColors.current.ink,
            fontSize = 44.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.6).sp,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            text = snapshot.subtitle,
            color = HausColors.current.muted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ImportAudioCard(
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    hasImportedTracks: Boolean,
    onClearLibrary: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (hasImportedTracks) importCardTitleWithTracks else importCardTitle,
                color = HausColors.current.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = importMessage ?: importCardDescription,
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Button(
                onClick = folderPickerLauncher::launch,
                enabled = folderPickerLauncher.isAvailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Add music folder" },
                cornerRadius = 16.dp,
                colors = ButtonDefaults.buttonColors(
                    color = HausColors.current.ink,
                    contentColor = HausColors.current.paper,
                    disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                    disabledContentColor = HausColors.current.muted,
                ),
            ) {
                Text(if (folderPickerLauncher.isAvailable) "Add music folder" else "Folder picker not available yet", fontWeight = FontWeight.Black)
            }
            if (hasImportedTracks) {
                Button(
                    onClick = onClearLibrary,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    cornerRadius = 12.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = HausColors.current.pulse.copy(alpha = 0.15f),
                        contentColor = HausColors.current.pulse,
                    ),
                ) {
                    Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    track: Track,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekFraction: (Float) -> Unit,
) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(track.accent.start), Color(track.accent.end)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
    val durationMillis = playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val positionMillis = playbackState.positionMillis.coerceIn(0L, durationMillis)
    val statusText = playbackState.error?.message ?: playbackState.status.label
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.ink),
    ) {
        Column(
            modifier = Modifier
                .background(brush)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = "Playback status: $statusText" },
                )
            }

            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap,
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Spacer(Modifier.height(4.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 29.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} · ${track.album}",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            EqualizerStrip(active = playbackState.isPlaying)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Slider(
                    value = playbackState.progressFraction,
                    onValueChange = onSeekFraction,
                    modifier = Modifier.semantics { contentDescription = "Playback seek position" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMillis(positionMillis), color = Color.White.copy(alpha = 0.84f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatMillis(durationMillis), color = Color.White.copy(alpha = 0.84f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .semantics { contentDescription = if (playbackState.isPlaying) "Pause playback" else "Play selected track" },
                    cornerRadius = 18.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.White,
                        contentColor = HausColors.current.ink,
                    ),
                ) {
                    Text(if (playbackState.isPlaying) "Pause" else "Play", fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .width(96.dp)
                        .height(52.dp)
                        .semantics { contentDescription = "Stop playback" },
                    cornerRadius = 18.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Stop", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun EqualizerStrip(active: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        val bars = 22
        val gap = size.width / (bars * 1.55f)
        val stroke = gap * 0.58f
        repeat(bars) { index ->
            val normalized = if (active) ((index * 37) % 11 + 4) / 15f else 0.34f
            val barHeight = size.height * normalized
            val x = gap + index * gap * 1.55f
            drawLine(
                color = Color.White.copy(alpha = 0.32f + normalized * 0.48f),
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = HausColors.current.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = HausColors.current.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrackRow(track: Track, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) HausColors.current.panelStrong else HausColors.current.panel.copy(alpha = 0.54f))
            .border(1.dp, if (selected) HausColors.current.ink else HausColors.current.line, RoundedCornerShape(24.dp))
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = "Select track ${track.title}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlbumMark(track = track, selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = HausColors.current.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} · ${track.album}",
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(visible = selected) {
                Text(
                    text = "Now playing",
                    color = HausColors.current.pulse,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = formatDuration(track.durationSeconds),
            color = if (selected) HausColors.current.ink else HausColors.current.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AlbumMark(track: Track, selected: Boolean) {
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtworkThumbnailCached()
    }
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(track.accent.start), Color(track.accent.end)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
            )
        } else if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap,
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = track.title.firstOrNull()?.uppercase() ?: "♪",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DrillDownView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    selectedTrack: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    libraryTracks: List<LibraryTrack>,
    onBack: () -> Unit,
    onTrackSelected: (String) -> Unit,
    onPlayPause: (Track) -> Unit,
    onExpandNowPlaying: (Track) -> Unit,
    onShowSettings: () -> Unit = {},
    onShowSearch: () -> Unit = {},
) {
    var selectedTrackId by remember { mutableStateOf(selectedTrack?.id) }
    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.id?.let { selectedTrackId = it }
    }
    val currentTrack = tracks.firstOrNull { it.id == selectedTrackId } ?: selectedTrack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .leftEdgeSwipeBack(onBack),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .safeContentPadding()
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item { DrillDownHeader(title = title, subtitle = subtitle, onBack = onBack) }
                    item { SectionLabel(title = title, subtitle = subtitle) }
                    items(tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            selected = track.id == selectedTrackId,
                            onClick = {
                                selectedTrackId = track.id
                                onTrackSelected(track.id)
                                onPlayPause(track)
                            },
                        )
                    }
                    item { Spacer(Modifier.height(NowPlayingBarContentPadding)) }
                }
                DrillDownScrollbar(
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        if (currentTrack != null) {
            val barExpandProgress = remember { Animatable(0f) }
            NowPlayingBar(
                track = currentTrack,
                playbackState = playbackState,
                onPlayPause = { onPlayPause(currentTrack) },
                onExpand = { onExpandNowPlaying(currentTrack) },
                onSettings = onShowSettings,
                onSearch = onShowSearch,
                expandProgress = barExpandProgress,
                isExpanded = false,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun DrillDownScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollFraction by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val maxFirstVisibleIndex = (total - visible).coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex).coerceIn(0f, 1f)
        }
    }

    fun scrollTo(yPosition: Float, trackHeightPx: Float) {
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        if (total <= visible || trackHeightPx <= 0f) return

        val maxFirstVisibleIndex = (total - visible).coerceAtLeast(0)
        val targetFraction = (yPosition / trackHeightPx).coerceIn(0f, 1f)
        val targetIndex = (targetFraction * maxFirstVisibleIndex).toInt().coerceIn(0, maxFirstVisibleIndex)
        coroutineScope.launch {
            // Must be an immediate (non-animated) scroll: animateScrollToItem takes ~300ms
            // and gets cancelled/restarted on every drag-move event, so the list perpetually
            // chases a stale animation and only catches up once the drag ends.
            listState.scrollToItem(targetIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(24.dp)
            .pointerInput(listState) {
                detectTapGestures { offset ->
                    scrollTo(offset.y, size.height.toFloat())
                }
            }
            .pointerInput(listState) {
                detectVerticalDragGestures { change, _ ->
                    scrollTo(change.position.y, size.height.toFloat())
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbHeight = maxHeight * 0.15f
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .width(6.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(HausColors.current.muted.copy(alpha = 0.42f)),
        )
    }
}

@Composable
private fun DrillDownHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BackChip(onClick = onBack)
            Text(
                text = subtitle,
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = title,
            color = HausColors.current.ink,
            fontSize = 44.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.6).sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun BrowseModePicker(
    browseMode: BrowseMode,
    onModeChange: (BrowseMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseMode.entries.forEach { mode ->
            val isSelected = browseMode == mode
            Button(
                onClick = { onModeChange(mode) },
                modifier = Modifier.weight(1f).height(40.dp),
                cornerRadius = 20.dp,
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.ink,
                        contentColor = HausColors.current.paper,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.panel,
                        contentColor = HausColors.current.ink,
                    )
                },
            ) {
                Text(mode.name, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = "Album ${album.album}" },
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val albumArtwork = remember(album.tracks) {
                album.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtworkCached() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(HausColors.current.ink, HausColors.current.pulse),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (albumArtwork != null) {
                    Image(
                        bitmap = albumArtwork,
                        contentDescription = "Album artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = album.album.take(2).uppercase(),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(
                text = album.album,
                color = HausColors.current.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (album.artist != null) "${album.artist} · ${album.tracks.size} tracks" else "${album.tracks.size} tracks",
                color = HausColors.current.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtistRow(
    artist: ArtistGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HausColors.current.panel.copy(alpha = 0.54f))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(24.dp))
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = "Artist ${artist.artist}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val artistArtwork = remember(artist.tracks) {
            artist.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtworkCached() }
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(HausColors.current.ink, HausColors.current.pulse),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (artistArtwork != null) {
                Image(
                    bitmap = artistArtwork,
                    contentDescription = "Artist artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = artist.artist.firstOrNull()?.uppercase() ?: "♪",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = artist.artist,
                color = HausColors.current.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${artist.albumCount} albums · ${artist.tracks.size} tracks",
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val PlaybackStatus.label: String
    get() = when (this) {
        PlaybackStatus.Idle -> "Ready"
        PlaybackStatus.Loading -> "Loading"
        PlaybackStatus.Buffering -> "Buffering"
        PlaybackStatus.Playing -> "Playing"
        PlaybackStatus.Paused -> "Paused"
        PlaybackStatus.Stopped -> "Stopped"
        PlaybackStatus.Error -> "Needs a local file"
    }

@Composable
internal fun ScanningCard(
    foldersVisited: Int,
    filesVisited: Int,
    tracksAdded: Int,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        cornerRadius = 12.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Scanning…", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HausColors.current.ink)
            Spacer(Modifier.height(6.dp))
            Text("$foldersVisited folders • $filesVisited files • $tracksAdded tracks", fontSize = 12.sp, color = HausColors.current.ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 8.dp,
                colors = ButtonDefaults.buttonColors(color = HausColors.current.ink, contentColor = HausColors.current.paper),
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}
