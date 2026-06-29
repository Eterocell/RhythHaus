package com.eterocell.rhythhaus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.eterocell.rhythhaus.library.resolvePathForMetadata
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.taglib.createTagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata as RawTagMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



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
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        when (result) {
            is PlatformFolderPickResult.Success -> {
                val source = result.source
                scanJob = scope.launch(Dispatchers.Default) {
                    var progress = ScanProgress(
                        session = ScanSession(id = "", sourceId = source.id, status = ScanStatus.Scanning, startedAtEpochMillis = 0L)
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

    RhythHausTheme {
        LibraryHomeScreen(
            snapshot = snapshot,
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = controller,
            folderPickerLauncher = folderPickerLauncher,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            showClearDialog = showClearDialog,
            onShowClearDialog = { showClearDialog = it },
            onClearLibrary = {
                repository.clearAll()
                libraryTracks = emptyList()
                showClearDialog = false
            },
            showSettings = showSettings,
            onShowSettings = { showSettings = it },
            showSearch = showSearch,
            onShowSearch = { showSearch = it },
        )
    }
}

@Composable
private fun RhythHausTheme(content: @Composable () -> Unit) {
    MiuixTheme(
        colors = lightColorScheme(
            primary = HausInk,
            onPrimary = HausPaper,
            secondary = HausPulse,
            onSecondary = HausPaper,
            background = HausPaper,
            onBackground = HausInk,
            surface = HausPanel,
            onSurface = HausInk,
            surfaceContainer = HausPanel,
            onSurfaceContainer = HausInk,
            secondaryVariant = HausPulse,
            onSecondaryVariant = HausPaper,
            disabledSecondaryVariant = HausPulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = HausPaper.copy(alpha = 0.28f),
        ),
        content = content,
    )
}

@Composable
fun LibraryHomeScreen(
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    showClearDialog: Boolean,
    onShowClearDialog: (Boolean) -> Unit,
    onClearLibrary: () -> Unit,
    showSettings: Boolean,
    onShowSettings: (Boolean) -> Unit,
    showSearch: Boolean,
    onShowSearch: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrackId by remember(snapshot.nowPlayingTrackId) { mutableStateOf(snapshot.nowPlayingTrackId) }
    val selectedTrack = snapshot.tracks.firstOrNull { it.id == selectedTrackId } ?: snapshot.tracks.firstOrNull()
    val playbackState by playbackController.state.collectAsState()
    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.id?.let { selectedTrackId = it }
    }
    var browseMode by remember { mutableStateOf(BrowseMode.Albums) }
    var selectedAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistGroup?>(null) }
    var showNowPlayingScreen by remember { mutableStateOf(false) }

    if (selectedAlbum != null) {
        val album = selectedAlbum!!
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
            onBack = { selectedAlbum = null },
            onTrackSelected = { /* selection only */ },
            onPlayPause = { track ->
                val playableTracks = albumTracks.map { it.toPlayableTrack() }
                if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                    playbackController.setQueue(playableTracks, track.id)
                }
                playbackController.togglePlayPause()
            },
            onShowSettings = { onShowSettings(true) },
            onShowSearch = { onShowSearch(true) },
        )
    } else if (selectedArtist != null) {
        val artist = selectedArtist!!
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
            onBack = { selectedArtist = null },
            onTrackSelected = { /* selection only */ },
            onPlayPause = { track ->
                val playableTracks = artistTracks.map { it.toPlayableTrack() }
                if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                    playbackController.setQueue(playableTracks, track.id)
                }
                playbackController.togglePlayPause()
            },
            onShowSettings = { onShowSettings(true) },
            onShowSearch = { onShowSearch(true) },
        )
    } else {
        if (showNowPlayingScreen && selectedTrack != null) {
            val currentLibTrack = libraryTracks.firstOrNull { it.id == selectedTrack.id }
            NowPlayingScreen(
                track = selectedTrack,
                playbackState = playbackState,
                playbackController = playbackController,
                tagLibReader = tagLibReader,
                currentLibraryTrack = currentLibTrack,
                onBack = { showNowPlayingScreen = false },
            )
        } else {
            Box(modifier = modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
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
                                subtitle = "${snapshot.tracks.size} tracks • ${formatDuration(snapshot.totalDurationSeconds)} total",
                            )
                        }
                        item {
                            BrowseModePicker(
                                browseMode = browseMode,
                                onModeChange = { browseMode = it },
                            )
                        }
                        if (browseMode == BrowseMode.Albums) {
                            val albums = groupTracksByAlbum(snapshot.tracks)
                            val albumRows = albums.chunked(2)
                            albumRows.forEach { row ->
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        row.forEach { albumGroup ->
                                            AlbumCard(
                                                album = albumGroup,
                                                modifier = Modifier.weight(1f),
                                                onClick = { selectedAlbum = albumGroup },
                                            )
                                        }
                                        if (row.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            val artists = groupTracksByArtist(snapshot.tracks)
                            items(artists, key = { it.artist }) { artistGroup ->
                                ArtistRow(
                                    artist = artistGroup,
                                    onClick = { selectedArtist = artistGroup },
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                if (selectedTrack != null) {
                    NowPlayingBar(
                        track = selectedTrack,
                        playbackState = playbackState,
                        onPlayPause = {
                            val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                            if (playbackState.currentTrack?.id != selectedTrack.id || playbackState.status == PlaybackStatus.Idle) {
                                playbackController.setQueue(playableTracks, selectedTrack.id)
                            }
                            playbackController.togglePlayPause()
                        },
                        onExpand = { showNowPlayingScreen = true },
                        onSettings = { onShowSettings(true) },
                        onSearch = { onShowSearch(true) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        Dialog(onDismissRequest = { onShowClearDialog(false) }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                cornerRadius = 24.dp,
                colors = CardDefaults.defaultColors(color = HausPanel),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Clear Library",
                        color = HausInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "This will remove all scanned tracks. Your music files are not deleted. Continue?",
                        color = HausMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { onShowClearDialog(false) },
                            modifier = Modifier.height(36.dp),
                            cornerRadius = 12.dp,
                            colors = ButtonDefaults.buttonColors(
                                color = HausMuted.copy(alpha = 0.15f),
                                contentColor = HausMuted,
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
                                color = HausPulse,
                                contentColor = HausPaper,
                            ),
                        ) {
                            Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsScreen(
            folderPickerLauncher = folderPickerLauncher,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            hasImportedTracks = snapshot.tracks.isNotEmpty(),
            onClearLibrary = { onShowClearDialog(true) },
            onDismiss = { onShowSettings(false) },
        )
    }

    if (showSearch) {
        SearchScreen(
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = playbackController,
            playbackState = playbackState,
            onDismiss = { onShowSearch(false) },
        )
    }
}

@Composable
private fun HeaderSection(snapshot: LibrarySnapshot) {
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
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HausInk)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "RHYTHHAUS",
                    color = HausPaper,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                )
            }
            Text(
                text = if (snapshot.tracks.isNotEmpty()) "${snapshot.tracks.size} tracks · ${formatDuration(snapshot.totalDurationSeconds)}" else "Local music player",
                color = HausMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = snapshot.title,
            color = HausInk,
            fontSize = 44.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.6).sp,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            text = snapshot.subtitle,
            color = HausMuted,
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
        colors = CardDefaults.defaultColors(color = HausPanel),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (hasImportedTracks) importCardTitleWithTracks else importCardTitle,
                color = HausInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = importMessage ?: importCardDescription,
                color = HausMuted,
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
                    color = HausInk,
                    contentColor = HausPaper,
                    disabledColor = HausMuted.copy(alpha = 0.28f),
                    disabledContentColor = HausMuted,
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
                        color = HausPulse.copy(alpha = 0.15f),
                        contentColor = HausPulse,
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
        colors = CardDefaults.defaultColors(color = HausInk),
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
                        contentColor = HausInk,
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
private fun DeveloperPanel(
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = HausPanelStrong),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .semantics { contentDescription = if (expanded) "Collapse developer panel" else "Expand developer panel" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DEV · TagLib metadata",
                        color = HausInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        text = "${libraryTracks.size} track(s) parsed natively",
                        color = HausMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    text = if (expanded) "Hide" else "Show",
                    color = HausPulse,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (libraryTracks.isEmpty()) {
                        Text(
                            text = "Import local audio to inspect all TagLib metadata fields plus the full property map (composer, copyright, BPM, ISRC, custom tags, and more).",
                            color = HausMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics { contentDescription = "Developer panel empty state" },
                        )
                    } else {
                        libraryTracks.forEach { track ->
                            DeveloperMetadataRow(track, tagLibReader)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperMetadataRow(track: LibraryTrack, tagLibReader: TagLibReader) {
    val rawResult = remember(track.audioSource.stableKey) {
        when (track.audioSource) {
            is AudioSource.FilePath -> tagLibReader.readPath(resolvePathForMetadata(track.audioSource.path))
            is AudioSource.Uri -> null
        }
    }
    val properties = remember(track.audioSource.stableKey) {
        when (track.audioSource) {
            is AudioSource.FilePath -> tagLibReader.readProperties(resolvePathForMetadata(track.audioSource.path))
            is AudioSource.Uri -> emptyMap()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HausPaper)
            .border(1.dp, HausLine, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = track.displayName,
            color = HausInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "source: ${track.audioSource.devLabel}",
            color = HausMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (rawResult == null) {
            Text(
                text = "URI source — TagLib requires a filesystem path",
                color = HausPulse,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            when (rawResult) {
                is TagReadResult.Found -> {
                    rawTagLines(rawResult.metadata).forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                color = HausMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = value,
                                color = HausInk,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f, fill = false),
                            )
                        }
                    }
                    if (properties.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ALL PROPERTIES (${properties.size})",
                            color = HausInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                        properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = key,
                                    color = HausMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = value,
                                    color = HausInk,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .weight(1f, fill = false),
                                )
                            }
                        }
                    }
                }

                is TagReadResult.Unsupported -> Text(
                    text = "native: unsupported — ${rawResult.reason}",
                    color = HausPulse,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                is TagReadResult.Failed -> Text(
                    text = "native: failed — ${rawResult.reason}",
                    color = HausPulse,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val AudioSource.devLabel: String
    get() = when (this) {
        is AudioSource.FilePath -> "file:$path"
        is AudioSource.Uri -> "uri:$value"
    }

private fun rawTagLines(metadata: RawTagMetadata): List<Pair<String, String>> = listOf(
    "title" to (metadata.title ?: "—"),
    "artist" to (metadata.artist ?: "—"),
    "album" to (metadata.album ?: "—"),
    "albumArtist" to (metadata.albumArtist ?: "—"),
    "genre" to (metadata.genre ?: "—"),
    "comment" to (metadata.comment ?: "—"),
    "year" to (metadata.year?.toString() ?: "—"),
    "track" to trackDisplay(metadata.trackNumber, metadata.trackTotal),
    "disc" to trackDisplay(metadata.discNumber, metadata.discTotal),
    "duration" to (metadata.durationMillis?.let { formatMillis(it) } ?: "—"),
    "bitrate" to (metadata.bitrate?.let { "${it}kbps" } ?: "—"),
    "sampleRate" to (metadata.sampleRate?.let { "${it}Hz" } ?: "—"),
    "channels" to (metadata.channels?.toString() ?: "—"),
    "artwork" to artworkLabel(metadata.artwork),
)

private fun trackDisplay(number: Int?, total: Int?): String = when {
    number != null && total != null -> "$number/$total"
    number != null -> number.toString()
    else -> "—"
}

private fun artworkLabel(artwork: com.eterocell.rhythhaus.taglib.EmbeddedArtwork?): String {
    if (artwork == null) return "—"
    return "${artwork.mimeType ?: "unknown"} · ${artwork.bytes.size}B"
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = HausInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = subtitle,
            color = HausMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TrackRow(track: Track, selected: Boolean, onClick: () -> Unit) {
    val selectionAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "track-row-selection",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) HausPanelStrong else HausPanel.copy(alpha = 0.54f))
            .border(1.dp, if (selected) HausInk else HausLine, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select track ${track.title}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlbumMark(track = track, selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = HausInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} · ${track.album}",
                color = HausMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(visible = selected) {
                Text(
                    text = "queued on shared UI ${(selectionAlpha * 100).toInt()}%",
                    color = HausPulse,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = formatDuration(track.durationSeconds),
            color = if (selected) HausInk else HausMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AlbumMark(track: Track, selected: Boolean) {
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
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
    onShowSettings: (Boolean) -> Unit = {},
    onShowSearch: (Boolean) -> Unit = {},
) {
    var selectedTrackId by remember { mutableStateOf(selectedTrack?.id) }
    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.id?.let { selectedTrackId = it }
    }
    var showNowPlayingScreen by remember { mutableStateOf(false) }
    val currentTrack = tracks.firstOrNull { it.id == selectedTrackId } ?: selectedTrack

    if (showNowPlayingScreen && currentTrack != null) {
        val currentLibTrack = libraryTracks.firstOrNull { it.id == currentTrack.id }
        NowPlayingScreen(
            track = currentTrack,
            playbackState = playbackState,
            playbackController = playbackController,
            tagLibReader = tagLibReader,
            currentLibraryTrack = currentLibTrack,
            onBack = { showNowPlayingScreen = false },
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .leftEdgeSwipeBack(onBack),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
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
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                    // Scroll indicator
                    val scrollFraction by remember(listState) {
                        derivedStateOf {
                            val total = listState.layoutInfo.totalItemsCount
                            if (total == 0) 0f else listState.firstVisibleItemIndex.toFloat() / total
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .width(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.15f)
                                .offset(y = (scrollFraction * 100).dp.coerceAtMost(200.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(HausMuted.copy(alpha = 0.3f)),
                        )
                    }
                }
            }

            if (currentTrack != null) {
                NowPlayingBar(
                    track = currentTrack,
                    playbackState = playbackState,
                    onPlayPause = { onPlayPause(currentTrack) },
                    onExpand = { showNowPlayingScreen = true },
                    onSettings = { onShowSettings(true) },
                    onSearch = { onShowSearch(true) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(HausInk)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "← BACK",
                    color = HausPaper,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                )
            }
            Text(
                text = subtitle,
                color = HausMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = title,
            color = HausInk,
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
                colors = if (isSelected) ButtonDefaults.buttonColors(
                    color = HausInk,
                    contentColor = HausPaper,
                ) else ButtonDefaults.buttonColors(
                    color = HausPanel,
                    contentColor = HausInk,
                ),
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
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Album ${album.album}" },
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = HausPanel),
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
                            listOf(HausInk, HausPulse),
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
                color = HausInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (album.artist != null) "${album.artist} · ${album.tracks.size} tracks" else "${album.tracks.size} tracks",
                color = HausMuted,
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
            .background(HausPanel.copy(alpha = 0.54f))
            .border(1.dp, HausLine, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
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
                        listOf(HausInk, HausPulse),
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
                color = HausInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${artist.albumCount} albums · ${artist.tracks.size} tracks",
                color = HausMuted,
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
        colors = CardDefaults.defaultColors(color = HausPanel),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Scanning…", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HausInk)
            Spacer(Modifier.height(6.dp))
            Text("$foldersVisited folders • $filesVisited files • $tracksAdded tracks", fontSize = 12.sp, color = HausInk.copy(alpha = 0.7f))
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 8.dp,
                colors = ButtonDefaults.buttonColors(color = HausInk, contentColor = HausPaper),
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}
