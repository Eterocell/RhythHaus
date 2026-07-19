package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.album_accessibility_format
import rhythhaus.shared.generated.resources.album_art
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.album_track_count_format
import rhythhaus.shared.generated.resources.artist_accessibility_format
import rhythhaus.shared.generated.resources.artist_album_tracks_format
import rhythhaus.shared.generated.resources.artist_artwork
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.scan_progress_format
import rhythhaus.shared.generated.resources.scanning
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.track_artist_album_format
import rhythhaus.shared.generated.resources.track_count_format
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.LazyTrackArtworkImage
import com.eterocell.rhythhaus.formatDuration
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.hausCombinedClickable
import com.eterocell.rhythhaus.importCardDescription
import com.eterocell.rhythhaus.importCardTitle
import com.eterocell.rhythhaus.importCardTitleWithTracks
import rhythhaus.shared.generated.resources.browse_mode_albums
import rhythhaus.shared.generated.resources.browse_mode_artists
import rhythhaus.shared.generated.resources.browse_mode_songs
import top.yukonga.miuix.kmp.basic.Checkbox

enum class TrackRowGesture { Click, LongClick }

enum class TrackRowActivation { Play, ToggleSelection, StartSelection }

fun trackRowActivation(
    selectionModeActive: Boolean,
    gesture: TrackRowGesture,
): TrackRowActivation = when {
    selectionModeActive -> TrackRowActivation.ToggleSelection
    gesture == TrackRowGesture.LongClick -> TrackRowActivation.StartSelection
    else -> TrackRowActivation.Play
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
    mutationsEnabled: Boolean,
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
                text = if (hasImportedTracks) importCardTitleWithTracks() else importCardTitle(),
                color = HausColors.current.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            val addMusicFolderContentDescription = stringResource(Res.string.add_music_folder)
            Text(
                text = importMessage ?: importCardDescription(),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Button(
                onClick = folderPickerLauncher::launch,
                enabled = folderPickerLauncher.isAvailable && mutationsEnabled,
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
internal fun TrackRow(
    track: Track,
    isNowPlaying: Boolean,
    selectionModeActive: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
) {
    val selectTrackContentDescription = stringResource(Res.string.select_track_format, track.title)
    val nowPlayingDescription = stringResource(Res.string.now_playing_badge)
    fun activate(gesture: TrackRowGesture) {
        when (trackRowActivation(selectionModeActive, gesture)) {
            TrackRowActivation.Play -> onPlay()
            TrackRowActivation.ToggleSelection -> onToggleSelection()
            TrackRowActivation.StartSelection -> onStartSelection()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isNowPlaying || isSelected) HausColors.current.panelStrong else HausColors.current.panel.copy(alpha = 0.54f))
            .border(1.dp, if (isSelected) HausColors.current.ink else HausColors.current.line, RoundedCornerShape(24.dp))
            .hausCombinedClickable(
                onClick = { activate(TrackRowGesture.Click) },
                onLongClick = { activate(TrackRowGesture.LongClick) },
                onLongClickLabel = selectTrackContentDescription,
            )
            .semantics {
                contentDescription = selectTrackContentDescription
                if (selectionModeActive) toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                if (isNowPlaying) stateDescription = nowPlayingDescription
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selectionModeActive) {
            Checkbox(
                state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggleSelection,
                modifier = Modifier.size(44.dp),
            )
        }
        AlbumMark(track = track, selected = isNowPlaying)
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
            AnimatedVisibility(visible = isNowPlaying) {
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
            color = if (isNowPlaying || isSelected) HausColors.current.ink else HausColors.current.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AlbumMark(track: Track, selected: Boolean) {
    val albumArtContentDescription = stringResource(Res.string.album_art)
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
        LazyTrackArtworkImage(
            trackId = track.id,
            eagerArtworkBytes = track.artworkBytes,
            contentDescription = albumArtContentDescription,
            role = ArtworkImageRole.Thumbnail,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        ) {
            Text(
                text = track.title.firstOrNull()?.uppercase() ?: "♪",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
            )
        }
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
                Text(stringResource(mode.displayLabelResource()), fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BrowseMode.displayLabelResource() = when (this) {
    BrowseMode.Albums -> Res.string.browse_mode_albums
    BrowseMode.Artists -> Res.string.browse_mode_artists
    BrowseMode.Songs -> Res.string.browse_mode_songs
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
            val albumArtworkTrack = remember(album.tracks) { album.tracks.firstOrNull() }
            val albumArtworkContentDescription = stringResource(Res.string.album_artwork)
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
                LazyTrackArtworkImage(
                    trackId = albumArtworkTrack?.id,
                    eagerArtworkBytes = albumArtworkTrack?.artworkBytes,
                    contentDescription = albumArtworkContentDescription,
                    role = ArtworkImageRole.Card,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) {
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
        val artistArtworkTrack = remember(artist.tracks) { artist.tracks.firstOrNull() }
        val artistArtworkContentDescription = stringResource(Res.string.artist_artwork)
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
            LazyTrackArtworkImage(
                trackId = artistArtworkTrack?.id,
                eagerArtworkBytes = artistArtworkTrack?.artworkBytes,
                contentDescription = artistArtworkContentDescription,
                role = ArtworkImageRole.Card,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
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
internal fun ScanningCard(
    foldersVisited: Int,
    filesVisited: Int,
    tracksAdded: Int,
    latestItem: String?,
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
            if (!latestItem.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(latestItem, fontSize = 11.sp, color = HausColors.current.ink.copy(alpha = 0.56f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
