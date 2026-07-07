package com.eterocell.rhythhaus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.max
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.album_accessibility_format
import rhythhaus.shared.generated.resources.album_art
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.album_detail_subtitle_format
import rhythhaus.shared.generated.resources.album_track_count_format
import rhythhaus.shared.generated.resources.artist_accessibility_format
import rhythhaus.shared.generated.resources.artist_album_tracks_format
import rhythhaus.shared.generated.resources.artist_artwork
import rhythhaus.shared.generated.resources.artist_detail_subtitle_format
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.clear_library_message
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.library
import rhythhaus.shared.generated.resources.library_queue
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.now_playing_label
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.pause_playback
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.play_selected_track
import rhythhaus.shared.generated.resources.playback_seek_position
import rhythhaus.shared.generated.resources.playback_status_buffering
import rhythhaus.shared.generated.resources.playback_status_error
import rhythhaus.shared.generated.resources.playback_status_format
import rhythhaus.shared.generated.resources.playback_status_loading
import rhythhaus.shared.generated.resources.playback_status_paused
import rhythhaus.shared.generated.resources.playback_status_playing
import rhythhaus.shared.generated.resources.playback_status_ready
import rhythhaus.shared.generated.resources.playback_status_stopped
import rhythhaus.shared.generated.resources.scan_progress_format
import rhythhaus.shared.generated.resources.scanning
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.stop
import rhythhaus.shared.generated.resources.stop_playback
import rhythhaus.shared.generated.resources.track_artist_album_format
import rhythhaus.shared.generated.resources.track_count_format
import rhythhaus.shared.generated.resources.unknown_artist
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
internal fun AnimatedClearLibraryDialogRoute(
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
                    text = stringResource(Res.string.clear_library),
                    color = HausColors.current.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.clear_library_message),
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
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.muted.copy(alpha = 0.15f),
                            contentColor = HausColors.current.muted,
                        ),
                    ) {
                        Text(stringResource(Res.string.cancel), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onClearLibrary,
                        modifier = Modifier.height(36.dp),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.pulse,
                            contentColor = HausColors.current.paper,
                        ),
                    ) {
                        Text(stringResource(Res.string.clear), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HeaderSection(snapshot: LibrarySnapshot) {
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
internal fun ImportAudioCard(
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
            val addMusicFolderContentDescription = stringResource(Res.string.add_music_folder)
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
                    .semantics { contentDescription = addMusicFolderContentDescription },
                cornerRadius = 16.dp,
                colors = ButtonDefaults.buttonColors(
                    color = HausColors.current.ink,
                    contentColor = HausColors.current.paper,
                    disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                    disabledContentColor = HausColors.current.muted,
                ),
            ) {
                Text(if (folderPickerLauncher.isAvailable) stringResource(Res.string.add_music_folder) else stringResource(Res.string.folder_picker_unavailable), fontWeight = FontWeight.Black)
            }
            if (hasImportedTracks) {
                Button(
                    onClick = onClearLibrary,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    cornerRadius = 12.dp,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        color = HausColors.current.pulse.copy(alpha = 0.15f),
                        contentColor = HausColors.current.pulse,
                    ),
                ) {
                    Text(stringResource(Res.string.clear_library), fontSize = 13.sp, fontWeight = FontWeight.Black)
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
    val statusText = playbackState.error?.message ?: playbackStatusLabel(playbackState.status)
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val playbackStatusDescription = stringResource(Res.string.playback_status_format, statusText)
    val playbackSeekDescription = stringResource(Res.string.playback_seek_position)
    val playPauseDescription = if (playbackState.isPlaying) {
        stringResource(Res.string.pause_playback)
    } else {
        stringResource(Res.string.play_selected_track)
    }
    val stopPlaybackDescription = stringResource(Res.string.stop_playback)

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
                    text = stringResource(Res.string.now_playing_label),
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
                    modifier = Modifier.semantics { contentDescription = playbackStatusDescription },
                )
            }

            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap,
                    contentDescription = stringResource(Res.string.album_artwork),
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
                    text = stringResource(Res.string.track_artist_album_format, track.artist, track.album),
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
                    modifier = Modifier.semantics { contentDescription = playbackSeekDescription },
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
                        .semantics { contentDescription = playPauseDescription },
                    cornerRadius = 18.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.White,
                        contentColor = HausColors.current.ink,
                    ),
                ) {
                    Text(if (playbackState.isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play), fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .width(96.dp)
                        .height(52.dp)
                        .semantics { contentDescription = stopPlaybackDescription },
                    cornerRadius = 18.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(Res.string.stop), fontWeight = FontWeight.Black)
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
internal fun SectionLabel(title: String, subtitle: String?) {
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
internal fun TrackRow(track: Track, selected: Boolean, onClick: () -> Unit) {
    val selectTrackContentDescription = stringResource(Res.string.select_track_format, track.title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) HausColors.current.panelStrong else HausColors.current.panel.copy(alpha = 0.54f))
            .border(1.dp, if (selected) HausColors.current.ink else HausColors.current.line, RoundedCornerShape(24.dp))
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = selectTrackContentDescription }
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
                text = stringResource(Res.string.track_artist_album_format, track.artist, track.album),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(visible = selected) {
                Text(
                    text = stringResource(Res.string.now_playing_badge),
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
                contentDescription = stringResource(Res.string.album_art),
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
internal fun DrillDownView(
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
    isNowPlayingBarVisible: Boolean = true,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
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
        val drillDownStatusBarHeight = rememberSystemBarTopPadding()
        val drillDownBackdrop = rememberRhythHausBackdrop()
        val listState = rememberLazyListState()
        val scrollChromeState by remember(listState) {
            derivedStateOf { nestedScrollChromeStateFor(listState.toLibraryScrollPosition()) }
        }
        LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            onScrollPositionChanged(listState.toLibraryScrollPosition())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordRhythHausBackdrop(drillDownBackdrop),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = drillDownStatusBarHeight),
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
            }
        }
        DrillDownScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        NestedScrollBlurChrome(
            state = scrollChromeState,
            title = title,
            backdrop = drillDownBackdrop,
            statusBarHeight = drillDownStatusBarHeight,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (currentTrack != null) {
            val barExpandProgress = remember { Animatable(0f) }
            AnimatedVisibility(
                visible = isNowPlayingBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                NowPlayingBar(
                    track = currentTrack,
                    playbackState = playbackState,
                    onPlayPause = { onPlayPause(currentTrack) },
                    onExpand = { onExpandNowPlaying(currentTrack) },
                    onSettings = onShowSettings,
                    onSearch = onShowSearch,
                    expandProgress = barExpandProgress,
                    isExpanded = false,
                    backdrop = drillDownBackdrop,
                )
            }
        }
    }
}

internal fun LazyListState.toLibraryScrollPosition(): LibraryScrollPosition = LibraryScrollPosition(
    firstVisibleItemIndex = firstVisibleItemIndex,
    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
)

private val NestedScrollChromeToolbarHeight = 56.dp

@Composable
internal fun rememberSystemBarTopPadding(): Dp {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val systemBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    return max(statusBarHeight.value, systemBarHeight.value).dp
}

@Composable
internal fun NestedScrollBlurChrome(
    state: NestedScrollChromeState,
    title: String,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    statusBarHeight: Dp = rememberSystemBarTopPadding(),
) {
    val progress = state.progress.coerceIn(0f, 1f)
    if (progress <= 0f) return
    val titleProgress = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)

    // The chrome still needs one known, fixed total height (status bar inset + toolbar) so the
    // glass surface is bounded to exactly that box instead of bleeding into the content below.
    val chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(chromeHeight)
            .zIndex(3f)
            .rhythHausLiquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(0.dp),
                fallbackColor = HausColors.current.panel.copy(alpha = RhythHausGlassSurfaceAlpha),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(NestedScrollChromeToolbarHeight),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 20.dp)
                    .alpha(titleProgress),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HausColors.current.pulse.copy(alpha = 0.72f * titleProgress)),
                )
                Text(
                    text = title,
                    color = HausColors.current.ink.copy(alpha = 0.86f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HausColors.current.line.copy(alpha = 0.42f * progress)),
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
internal fun BrowseModePicker(
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
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
internal fun AlbumCard(
    album: AlbumGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val albumContentDescription = stringResource(Res.string.album_accessibility_format, album.album)
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = albumContentDescription },
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
                        contentDescription = stringResource(Res.string.album_artwork),
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
                text = if (album.artist != null) stringResource(Res.string.artist_album_tracks_format, album.artist, album.tracks.size) else stringResource(Res.string.track_count_format, album.tracks.size),
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
internal fun ArtistRow(
    artist: ArtistGroup,
    onClick: () -> Unit,
) {
    val artistContentDescription = stringResource(Res.string.artist_accessibility_format, artist.artist)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HausColors.current.panel.copy(alpha = 0.54f))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(24.dp))
            .hausClickable(onClick = onClick)
            .semantics { contentDescription = artistContentDescription }
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
                    contentDescription = stringResource(Res.string.artist_artwork),
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
                text = stringResource(Res.string.album_track_count_format, artist.albumCount, artist.tracks.size),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun playbackStatusLabel(status: PlaybackStatus): String = when (status) {
    PlaybackStatus.Idle -> stringResource(Res.string.playback_status_ready)
    PlaybackStatus.Loading -> stringResource(Res.string.playback_status_loading)
    PlaybackStatus.Buffering -> stringResource(Res.string.playback_status_buffering)
    PlaybackStatus.Playing -> stringResource(Res.string.playback_status_playing)
    PlaybackStatus.Paused -> stringResource(Res.string.playback_status_paused)
    PlaybackStatus.Stopped -> stringResource(Res.string.playback_status_stopped)
    PlaybackStatus.Error -> stringResource(Res.string.playback_status_error)
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
            Text(stringResource(Res.string.scanning), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HausColors.current.ink)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.scan_progress_format, foldersVisited, filesVisited, tracksAdded), fontSize = 12.sp, color = HausColors.current.ink.copy(alpha = 0.7f))
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
                Text(stringResource(Res.string.cancel), fontSize = 12.sp)
            }
        }
    }
}
