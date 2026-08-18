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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.hausCombinedClickable
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.album_accessibility_format
import rhythhaus.feature.library.generated.resources.album_track_count_format
import rhythhaus.feature.library.generated.resources.artist_accessibility_format
import rhythhaus.feature.library.generated.resources.artist_album_tracks_format
import rhythhaus.feature.library.generated.resources.artist_artwork
import rhythhaus.feature.library.generated.resources.browse_mode_albums
import rhythhaus.feature.library.generated.resources.browse_mode_artists
import rhythhaus.feature.library.generated.resources.browse_mode_songs
import rhythhaus.feature.library.generated.resources.hide_scan_report
import rhythhaus.feature.library.generated.resources.import_card_description
import rhythhaus.feature.library.generated.resources.import_card_title
import rhythhaus.feature.library.generated.resources.import_card_title_with_tracks
import rhythhaus.feature.library.generated.resources.remove_missing
import rhythhaus.feature.library.generated.resources.rescan
import rhythhaus.feature.library.generated.resources.retry_scan
import rhythhaus.feature.library.generated.resources.scan_cancelled
import rhythhaus.feature.library.generated.resources.scan_completed
import rhythhaus.feature.library.generated.resources.scan_failed
import rhythhaus.feature.library.generated.resources.scan_progress_format
import rhythhaus.feature.library.generated.resources.scan_report_empty
import rhythhaus.feature.library.generated.resources.scan_report_error_format
import rhythhaus.feature.library.generated.resources.scan_summary_format
import rhythhaus.feature.library.generated.resources.scanning
import rhythhaus.feature.library.generated.resources.track_count_format
import rhythhaus.feature.library.generated.resources.view_scan_report
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

/** A track-row input gesture. */
public enum class TrackRowGesture {
    /** A single click. */
    Click,
    /** A long click. */
    LongClick
}

/** The activation a [TrackRow] resolves for one gesture. */
public enum class TrackRowActivation {
    /** Requests playback of the row's track. */
    Play,
    /** Requests toggling the row's selection. */
    ToggleSelection,
    /** Requests beginning selection with the row's track. */
    StartSelection
}

/**
 * Resolves the activation for a track-row gesture.
 *
 * @param selectionModeActive whether rows currently select rather than play.
 * @param gesture the row gesture being activated.
 */
public fun trackRowActivation(
    selectionModeActive: Boolean,
    gesture: TrackRowGesture,
): TrackRowActivation =
    when {
        selectionModeActive -> TrackRowActivation.ToggleSelection
        gesture == TrackRowGesture.LongClick ->
            TrackRowActivation.StartSelection
        else -> TrackRowActivation.Play
    }

@Composable
internal fun HeaderSection(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = HausColors.current.ink,
            fontSize = 44.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.6).sp,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            text = subtitle,
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
    labels: LibrarySharedLabels,
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
                text =
                    if (hasImportedTracks)
                        stringResource(Res.string.import_card_title_with_tracks)
                    else stringResource(Res.string.import_card_title),
                color = HausColors.current.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text =
                    importMessage
                        ?: stringResource(Res.string.import_card_description),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Button(
                onClick = folderPickerLauncher::launch,
                enabled = folderPickerLauncher.isAvailable && mutationsEnabled,
                modifier =
                    Modifier.fillMaxWidth().height(48.dp).semantics {
                        contentDescription = labels.addMusicFolder
                    },
                cornerRadius = 16.dp,
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.ink,
                        contentColor = HausColors.current.paper,
                        disabledColor =
                            HausColors.current.muted.copy(alpha = 0.28f),
                        disabledContentColor = HausColors.current.muted,
                    ),
            ) {
                Text(
                    if (folderPickerLauncher.isAvailable) labels.addMusicFolder
                    else labels.folderPickerUnavailable,
                    fontWeight = FontWeight.Black)
            }
            if (hasImportedTracks) {
                Button(
                    onClick = onClearLibrary,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    cornerRadius = 12.dp,
                    insideMargin =
                        PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            color =
                                HausColors.current.pulse.copy(alpha = 0.15f),
                            contentColor = HausColors.current.pulse,
                        ),
                ) {
                    Text(
                        labels.clearLibrary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * Renders a decorative equalizer strip for the now-playing state.
 *
 * @param active whether the strip renders in its active animated state.
 */
@Composable
public fun EqualizerStrip(active: Boolean) {
    Canvas(
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        val bars = 22
        val gap = size.width / (bars * 1.55f)
        val stroke = gap * 0.58f
        repeat(bars) { index ->
            val normalized =
                if (active) ((index * 37) % 11 + 4) / 15f else 0.34f
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

/**
 * Renders one track row with album artwork, duration, and selection or playback
 * activation.
 *
 * @param track the track to render.
 * @param isNowPlaying whether this row represents the current playback track.
 * @param selectionModeActive whether the row currently selects rather than
 *   plays.
 * @param isSelected whether this row is selected.
 * @param labels Shared-owned localized wording.
 * @param artworkLoader lazily resolves artwork bytes for a track ID.
 * @param onPlay requests playback of this row's track.
 * @param onToggleSelection requests toggling this row's selection.
 * @param onStartSelection requests beginning selection with this row's track.
 */
@Composable
public fun TrackRow(
    track: Track,
    isNowPlaying: Boolean,
    selectionModeActive: Boolean,
    isSelected: Boolean,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
) {
    val selectTrackContentDescription = labels.selectTrack(track.title)
    val nowPlayingDescription = labels.nowPlayingBadge
    fun activate(gesture: TrackRowGesture) {
        when (trackRowActivation(selectionModeActive, gesture)) {
            TrackRowActivation.Play -> onPlay()
            TrackRowActivation.ToggleSelection -> onToggleSelection()
            TrackRowActivation.StartSelection -> onStartSelection()
        }
    }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isNowPlaying || isSelected)
                        HausColors.current.panelStrong
                    else HausColors.current.panel.copy(alpha = 0.54f))
                .border(
                    1.dp,
                    if (isSelected) HausColors.current.ink
                    else HausColors.current.line,
                    RoundedCornerShape(24.dp))
                .hausCombinedClickable(
                    onClick = { activate(TrackRowGesture.Click) },
                    onLongClick = { activate(TrackRowGesture.LongClick) },
                    onLongClickLabel = selectTrackContentDescription,
                )
                .semantics {
                    contentDescription = selectTrackContentDescription
                    if (selectionModeActive)
                        toggleableState =
                            if (isSelected) ToggleableState.On
                            else ToggleableState.Off
                    if (isNowPlaying) stateDescription = nowPlayingDescription
                }
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selectionModeActive) {
            Checkbox(
                state =
                    if (isSelected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggleSelection,
                modifier = Modifier.size(44.dp),
            )
        }
        AlbumMark(
            track = track,
            selected = isNowPlaying,
            labels = labels,
            artworkLoader = artworkLoader,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = track.title,
                    color = HausColors.current.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = labels.trackArtistAlbum(track.artist, track.album),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(visible = isNowPlaying) {
                    Text(
                        text = labels.nowPlayingBadge,
                        color = HausColors.current.pulse,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        Text(
            text = formatDuration(track.durationSeconds),
            color =
                if (isNowPlaying || isSelected) HausColors.current.ink
                else HausColors.current.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun AlbumMark(
    track: Track,
    selected: Boolean,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
) {
    val albumArtContentDescription = labels.albumArt
    Box(
        modifier =
            Modifier.size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(track.accent.start), Color(track.accent.end)),
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
            artworkLoader = artworkLoader,
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
                modifier =
                    Modifier.size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
            )
        }
    }
}

@Composable
internal fun BrowseModePicker(
    browseMode: BrowseMode,
    labels: LibrarySharedLabels,
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
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                colors =
                    if (isSelected) {
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
                Text(
                    stringResource(mode.displayLabelResource()),
                    fontSize = 14.sp,
                    fontWeight =
                        if (isSelected) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BrowseMode.displayLabelResource() =
    when (this) {
        BrowseMode.Albums -> Res.string.browse_mode_albums
        BrowseMode.Artists -> Res.string.browse_mode_artists
        BrowseMode.Songs -> Res.string.browse_mode_songs
    }

@Composable
internal fun AlbumCard(
    album: AlbumGroup,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val albumContentDescription =
        stringResource(Res.string.album_accessibility_format, album.album)
    Card(
        modifier =
            modifier
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
            val albumArtworkTrack =
                remember(album.tracks) { album.tracks.firstOrNull() }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    HausColors.current.ink,
                                    HausColors.current.pulse),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                LazyTrackArtworkImage(
                    trackId = albumArtworkTrack?.id,
                    eagerArtworkBytes = albumArtworkTrack?.artworkBytes,
                    contentDescription = labels.albumArtwork,
                    role = ArtworkImageRole.Card,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    artworkLoader = artworkLoader,
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
                text =
                    if (album.artist != null)
                        stringResource(
                            Res.string.artist_album_tracks_format,
                            album.artist,
                            album.tracks.size)
                    else
                        stringResource(
                            Res.string.track_count_format, album.tracks.size),
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
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onClick: () -> Unit,
) {
    val artistContentDescription =
        stringResource(Res.string.artist_accessibility_format, artist.artist)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HausColors.current.panel.copy(alpha = 0.54f))
                .border(
                    1.dp, HausColors.current.line, RoundedCornerShape(24.dp))
                .hausClickable(onClick = onClick)
                .semantics { contentDescription = artistContentDescription }
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val artistArtworkTrack =
            remember(artist.tracks) { artist.tracks.firstOrNull() }
        Box(
            modifier =
                Modifier.size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                HausColors.current.ink,
                                HausColors.current.pulse),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            LazyTrackArtworkImage(
                trackId = artistArtworkTrack?.id,
                eagerArtworkBytes = artistArtworkTrack?.artworkBytes,
                contentDescription = stringResource(Res.string.artist_artwork),
                role = ArtworkImageRole.Card,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                artworkLoader = artworkLoader,
            ) {
                Text(
                    text = artist.artist.firstOrNull()?.uppercase() ?: "♪",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = artist.artist,
                    color = HausColors.current.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.album_track_count_format,
                            artist.albumCount,
                            artist.tracks.size),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

/**
 * Renders the active scan progress card with a cancel action.
 *
 * @param foldersVisited the number of folders visited so far.
 * @param filesVisited the number of files visited so far.
 * @param tracksAdded the number of tracks added so far.
 * @param latestItem the most recently processed display item, if any.
 * @param labels Shared-owned localized scan wording.
 * @param onCancel requests cancellation of the active scan.
 */
@Composable
public fun ScanningCard(
    foldersVisited: Int,
    filesVisited: Int,
    tracksAdded: Int,
    latestItem: String?,
    labels: LibrarySharedLabels,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        cornerRadius = 12.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(Res.string.scanning),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HausColors.current.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    Res.string.scan_progress_format,
                    foldersVisited,
                    filesVisited,
                    tracksAdded),
                fontSize = 12.sp,
                color = HausColors.current.ink.copy(alpha = 0.7f))
            if (!latestItem.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    latestItem,
                    fontSize = 11.sp,
                    color = HausColors.current.ink.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
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
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.ink,
                        contentColor = HausColors.current.paper),
            ) {
                Text(labels.cancel, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Renders the terminal scan-outcome panel with summary, rescan/retry,
 * report toggle, and remove-missing actions.
 *
 * @param session the terminal scan session to display.
 * @param source the source the session scanned, if still configured.
 * @param errors file errors recorded for the displayed scan session.
 * @param reportVisible whether the error report is expanded.
 * @param mutationsEnabled whether scan mutations are currently allowed.
 * @param onToggleReport requests toggling the report expansion.
 * @param onRescanSource requests scanning the source again.
 * @param onRemoveMissingTracks requests removing tracks not seen by the
 *   completed scan.
 */
@Composable
public fun ScanOutcomePanel(
    session: ScanSession,
    source: LibrarySource?,
    errors: List<ScanError>,
    reportVisible: Boolean,
    mutationsEnabled: Boolean,
    onToggleReport: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
) {
    val title =
        stringResource(
            when (session.status) {
                ScanStatus.Completed -> Res.string.scan_completed
                ScanStatus.Cancelled -> Res.string.scan_cancelled
                ScanStatus.Failed -> Res.string.scan_failed
                else -> Res.string.scan_completed
            },
        )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            color = HausColors.current.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black)
        Text(
            stringResource(
                Res.string.scan_summary_format,
                session.foldersVisited,
                session.filesVisited,
                session.tracksAdded,
                session.tracksUpdated,
                session.filesSkipped),
            color = HausColors.current.muted,
            fontSize = 12.sp)
        session.terminalMessage?.takeIf(String::isNotBlank)?.let {
            Text(it, color = HausColors.current.pulse, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            source?.let {
                Button(
                    onClick = { onRescanSource(it) },
                    enabled = mutationsEnabled,
                    modifier = Modifier.weight(1f).height(38.dp),
                    cornerRadius = 8.dp) {
                        Text(
                            stringResource(
                                if (session.status == ScanStatus.Completed)
                                    Res.string.rescan
                                else Res.string.retry_scan,
                            ),
                            fontSize = 12.sp,
                        )
                    }
            }
            Button(
                onClick = onToggleReport,
                modifier = Modifier.weight(1f).height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(
                            if (reportVisible) Res.string.hide_scan_report
                            else Res.string.view_scan_report),
                        fontSize = 12.sp)
                }
        }
        if (session.status == ScanStatus.Completed && source != null) {
            Button(
                onClick = { onRemoveMissingTracks(source, session) },
                enabled = mutationsEnabled,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(Res.string.remove_missing),
                        fontSize = 12.sp)
                }
        }
        if (reportVisible) {
            if (errors.isEmpty())
                Text(
                    stringResource(Res.string.scan_report_empty),
                    color = HausColors.current.muted,
                    fontSize = 12.sp)
            errors.forEach { error ->
                Text(
                    stringResource(
                        Res.string.scan_report_error_format,
                        error.displayPath,
                        error.reason),
                    color = HausColors.current.muted,
                    fontSize = 12.sp)
            }
        }
    }
}
