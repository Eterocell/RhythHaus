package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
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
import com.eterocell.rhythhaus.theme.HausColorPalette
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.HausButton as Button
import com.eterocell.rhythhaus.ui.HausSongRow
import com.eterocell.rhythhaus.ui.hausClickable
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.album_accessibility_format
import rhythhaus.feature.library.generated.resources.album_track_count_format
import rhythhaus.feature.library.generated.resources.artist_accessibility_format
import rhythhaus.feature.library.generated.resources.artist_album_tracks_format
import rhythhaus.feature.library.generated.resources.artist_artwork
import rhythhaus.feature.library.generated.resources.browse_controls_direction_value_format
import rhythhaus.feature.library.generated.resources.browse_controls_filters
import rhythhaus.feature.library.generated.resources.browse_controls_heading
import rhythhaus.feature.library.generated.resources.browse_controls_order
import rhythhaus.feature.library.generated.resources.browse_controls_sort
import rhythhaus.feature.library.generated.resources.browse_controls_sort_value_format
import rhythhaus.feature.library.generated.resources.browse_controls_source
import rhythhaus.feature.library.generated.resources.browse_controls_source_value_format
import rhythhaus.feature.library.generated.resources.browse_filter_all_sources
import rhythhaus.feature.library.generated.resources.browse_filter_artwork
import rhythhaus.feature.library.generated.resources.browse_filter_clear_all
import rhythhaus.feature.library.generated.resources.browse_filter_favorite
import rhythhaus.feature.library.generated.resources.browse_filter_remove_format
import rhythhaus.feature.library.generated.resources.browse_filter_source_unavailable
import rhythhaus.feature.library.generated.resources.browse_mode_albums
import rhythhaus.feature.library.generated.resources.browse_mode_artists
import rhythhaus.feature.library.generated.resources.browse_mode_favorites
import rhythhaus.feature.library.generated.resources.browse_mode_more
import rhythhaus.feature.library.generated.resources.browse_mode_more_views
import rhythhaus.feature.library.generated.resources.browse_mode_recently_added
import rhythhaus.feature.library.generated.resources.browse_mode_recently_played
import rhythhaus.feature.library.generated.resources.browse_mode_songs
import rhythhaus.feature.library.generated.resources.browse_selected_state
import rhythhaus.feature.library.generated.resources.browse_sort_added
import rhythhaus.feature.library.generated.resources.browse_sort_album
import rhythhaus.feature.library.generated.resources.browse_sort_artist
import rhythhaus.feature.library.generated.resources.browse_sort_ascending
import rhythhaus.feature.library.generated.resources.browse_sort_descending
import rhythhaus.feature.library.generated.resources.browse_sort_favorite
import rhythhaus.feature.library.generated.resources.browse_sort_modified
import rhythhaus.feature.library.generated.resources.browse_sort_play_count
import rhythhaus.feature.library.generated.resources.browse_sort_title
import rhythhaus.feature.library.generated.resources.browse_source_format
import rhythhaus.feature.library.generated.resources.browse_unselected_state
import rhythhaus.feature.library.generated.resources.hide_scan_report
import rhythhaus.feature.library.generated.resources.import_card_description
import rhythhaus.feature.library.generated.resources.import_card_title
import rhythhaus.feature.library.generated.resources.import_card_title_with_tracks
import rhythhaus.feature.library.generated.resources.latest_completed_scan_format
import rhythhaus.feature.library.generated.resources.metadata_edit_action
import rhythhaus.feature.library.generated.resources.remove_missing
import rhythhaus.feature.library.generated.resources.rescan
import rhythhaus.feature.library.generated.resources.retry_scan
import rhythhaus.feature.library.generated.resources.scan_cancelled
import rhythhaus.feature.library.generated.resources.scan_changes_details
import rhythhaus.feature.library.generated.resources.scan_changes_format
import rhythhaus.feature.library.generated.resources.scan_completed
import rhythhaus.feature.library.generated.resources.scan_failed
import rhythhaus.feature.library.generated.resources.scan_progress_format
import rhythhaus.feature.library.generated.resources.scan_report_empty
import rhythhaus.feature.library.generated.resources.scan_report_error_format
import rhythhaus.feature.library.generated.resources.scan_summary_format
import rhythhaus.feature.library.generated.resources.scanning
import rhythhaus.feature.library.generated.resources.track_count_format
import rhythhaus.feature.library.generated.resources.track_more_actions_format
import rhythhaus.feature.library.generated.resources.view_scan_report
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

/** A track-row input gesture. */
public enum class TrackRowGesture {
    /** A single click. */
    Click,
    /** A long click. */
    LongClick
}

private val compactPrimaryBrowseModes =
    listOf(BrowseMode.Songs, BrowseMode.Albums, BrowseMode.Artists)

private val compactSecondaryBrowseModes =
    listOf(
        BrowseMode.Favorites,
        BrowseMode.RecentlyPlayed,
        BrowseMode.RecentlyAdded,
    )

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

internal fun trackRowMetadataColor(
    palette: HausColorPalette,
    isNowPlaying: Boolean,
    isSelected: Boolean,
): Color = if (isNowPlaying || isSelected) palette.ink else palette.muted

@Composable
internal fun HeaderSection(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = HausColors.current.ink,
            style = HausTypography.pageTitle,
        )
        Text(
            text = subtitle,
            color = HausColors.current.muted,
            style = HausTypography.body,
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
        cornerRadius = HausShapes.group,
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
                style = HausTypography.section,
            )
            Text(
                text =
                    importMessage
                        ?: stringResource(Res.string.import_card_description),
                color = HausColors.current.muted,
                style = HausTypography.secondary,
            )
            Button(
                onClick = folderPickerLauncher::launch,
                enabled = folderPickerLauncher.isAvailable && mutationsEnabled,
                modifier =
                    Modifier.fillMaxWidth().height(48.dp).semantics {
                        contentDescription = labels.addMusicFolder
                    },
                cornerRadius = HausShapes.control,
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
                    style = HausTypography.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (hasImportedTracks) {
                Button(
                    onClick = onClearLibrary,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    cornerRadius = HausShapes.control,
                    insideMargin =
                        PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            color = HausColors.current.dangerPanel,
                            contentColor = HausColors.current.danger,
                        ),
                ) {
                    Text(
                        labels.clearLibrary,
                        style = HausTypography.secondary,
                        fontWeight = FontWeight.Medium,
                    )
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
            style = HausTypography.section,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = HausColors.current.muted,
                style = HausTypography.label,
                fontWeight = FontWeight.SemiBold,
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
 * @param labels Localized labels and semantic descriptions for the row.
 * @param artworkLoader lazily resolves artwork bytes for a track ID.
 * @param onPlay requests playback of this row's track.
 * @param onToggleSelection requests toggling this row's selection.
 * @param onStartSelection requests beginning selection with this row.
 * @param favorite whether this track is currently favorited.
 * @param onSetFavorite requests the desired favorite state.
 * @param onEditMetadata requests editing app-local metadata for this track.
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
    favorite: Boolean,
    onSetFavorite: (Boolean) -> Unit,
    onEditMetadata: (() -> Unit)? = null,
) {
    val selectTrackContentDescription = labels.selectTrack(track.title)
    var moreActionsExpanded by remember(track.id) { mutableStateOf(false) }
    fun activate(gesture: TrackRowGesture) {
        when (trackRowActivation(selectionModeActive, gesture)) {
            TrackRowActivation.Play -> onPlay()
            TrackRowActivation.ToggleSelection -> onToggleSelection()
            TrackRowActivation.StartSelection -> onStartSelection()
        }
    }
    val favoriteDescription =
        if (favorite) labels.removeFavorite(track.title)
        else labels.addFavorite(track.title)
    HausSongRow(
        title = track.title,
        subtitle = labels.trackArtistAlbum(track.artist, track.album),
        selectionMode = selectionModeActive,
        selected = isSelected,
        playing = isNowPlaying,
        interactive = true,
        onActivate = { activate(TrackRowGesture.Click) },
        onSelect = { activate(TrackRowGesture.LongClick) },
        selectLabel = selectTrackContentDescription,
        playingLabel = labels.nowPlayingBadge,
        artwork = {
            AlbumMark(
                track = track,
                selected = isNowPlaying,
                labels = labels,
                artworkLoader = artworkLoader,
                modifier = Modifier.fillMaxSize(),
            )
        },
        indicator = {
            Text(
                text = labels.nowPlayingBadge,
                color = HausColors.current.pulse,
                style = HausTypography.label,
            )
        },
        trailing = {
            if (!selectionModeActive || onEditMetadata != null) {
                val moreLabel =
                    stringResource(
                        Res.string.track_more_actions_format,
                        track.title,
                    )
                Box {
                    Button(
                        onClick = { moreActionsExpanded = true },
                        modifier =
                            Modifier.size(48.dp)
                                .testTag("track-row-more-actions")
                                .semantics {
                                    contentDescription = moreLabel
                                },
                        cornerRadius = HausShapes.control,
                        insideMargin = PaddingValues(0.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                color = HausColors.current.panel,
                                contentColor = HausColors.current.ink,
                            ),
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = moreActionsExpanded,
                        onDismissRequest = { moreActionsExpanded = false },
                    ) {
                        if (!selectionModeActive) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        selectTrackContentDescription,
                                        color = HausColors.current.ink,
                                    )
                                },
                                onClick = {
                                    moreActionsExpanded = false
                                    onStartSelection()
                                },
                                modifier = Modifier.testTag("track-row-select"),
                            )
                        }
                        onEditMetadata?.let { editMetadata ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            Res.string.metadata_edit_action,
                                            track.title,
                                        ),
                                        color = HausColors.current.ink,
                                    )
                                },
                                onClick = {
                                    moreActionsExpanded = false
                                    editMetadata()
                                },
                                modifier =
                                    Modifier.testTag("track-row-edit-metadata"),
                            )
                        }
                    }
                }
            }
            if (!selectionModeActive) {
                Button(
                    onClick = { onSetFavorite(!favorite) },
                    modifier =
                        Modifier.size(48.dp).semantics {
                            contentDescription = favoriteDescription
                            toggleableState =
                                if (favorite) ToggleableState.On
                                else ToggleableState.Off
                        },
                    cornerRadius = HausShapes.control,
                    insideMargin = PaddingValues(0.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            color = HausColors.current.panel,
                            contentColor = HausColors.current.ink,
                        ),
                ) {
                    Text(if (favorite) "♥" else "♡", fontSize = 20.sp)
                }
            }
            Text(
                text = formatDuration(track.durationSeconds),
                color = HausColors.current.muted,
                style = HausTypography.label,
            )
        },
    )
}

@Composable
private fun AlbumMark(
    track: Track,
    selected: Boolean,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val albumArtContentDescription = labels.albumArt
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(HausShapes.artwork))
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
    compact: Boolean,
    onModeChange: (BrowseMode) -> Unit,
) {
    if (compact) {
        var moreExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HausSpacing.xs),
        ) {
            compactPrimaryBrowseModes.forEach { mode ->
                BrowseModeButton(
                    mode = mode,
                    isSelected = browseMode == mode,
                    compact = true,
                    modifier = Modifier.weight(1f),
                    onModeChange = onModeChange,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                val selectedMode = browseMode in compactSecondaryBrowseModes
                val moreLabel =
                    if (selectedMode)
                        stringResource(browseMode.displayLabelResource())
                    else stringResource(Res.string.browse_mode_more)
                val moreViewsDescription =
                    stringResource(Res.string.browse_mode_more_views)
                Button(
                    onClick = { moreExpanded = true },
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("library-secondary-browse-menu")
                            .semantics {
                                contentDescription = moreViewsDescription
                                selected = selectedMode
                                if (selectedMode) {
                                    stateDescription = moreLabel
                                }
                            },
                    cornerRadius = HausShapes.control,
                    insideMargin =
                        PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors =
                        if (selectedMode) {
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
                        text = moreLabel,
                        style = HausTypography.label,
                        fontWeight =
                            if (selectedMode) FontWeight.SemiBold
                            else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false },
                ) {
                    compactSecondaryBrowseModes.forEach { mode ->
                        val label = stringResource(mode.displayLabelResource())
                        val stateDescription =
                            stringResource(
                                if (browseMode == mode)
                                    Res.string.browse_selected_state
                                else Res.string.browse_unselected_state,
                            )
                        DropdownMenuItem(
                            text = {
                                Text(label, color = HausColors.current.ink)
                            },
                            onClick = {
                                moreExpanded = false
                                onModeChange(mode)
                            },
                            modifier =
                                Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = label
                                    selected = browseMode == mode
                                    this.stateDescription = stateDescription
                                },
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HausSpacing.xs),
        ) {
            BrowseMode.entries.forEach { mode ->
                BrowseModeButton(
                    mode = mode,
                    isSelected = browseMode == mode,
                    compact = false,
                    modifier = Modifier.weight(1f),
                    onModeChange = onModeChange,
                )
            }
        }
    }
}

/**
 * Renders the ephemeral Home sorting and filtering controls.
 *
 * Each control row scrolls horizontally so the complete choice set remains
 * reachable at compact widths without forcing the Home list to measure an
 * unbounded row. Choice buttons expose selected semantics, while filter buttons
 * expose checked semantics through [ToggleableState].
 */
@Composable
internal fun LibraryBrowseControls(
    query: LibraryBrowseQuery,
    browseMode: BrowseMode,
    sourceOptions: List<LibraryBrowseSourceOption>,
    onSortChange: (LibrarySort) -> Unit,
    onSortDirectionChange: (LibrarySortDirection) -> Unit,
    onFavoriteOnlyChange: (Boolean) -> Unit,
    onArtworkOnlyChange: (Boolean) -> Unit,
    onSourceIdChange: (String?) -> Unit,
) {
    val sortIsActive =
        query.sortExplicit ||
            browseMode != BrowseMode.RecentlyPlayed &&
                browseMode != BrowseMode.RecentlyAdded
    var sortExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    val sortLabel =
        if (sortIsActive) stringResource(query.sort.labelResource())
        else stringResource(Res.string.browse_controls_sort)
    val directionLabel =
        if (sortIsActive) stringResource(query.direction.labelResource())
        else stringResource(Res.string.browse_controls_order)
    val sourceLabel =
        if (query.sourceId == null) {
            stringResource(Res.string.browse_filter_all_sources)
        } else {
            sourceOptions.firstOrNull { it.id == query.sourceId }?.displayName
                ?: stringResource(Res.string.browse_filter_source_unavailable)
        }
    val activeFilterCount =
        (if (query.favoriteOnly) 1 else 0) +
            (if (query.artworkOnly) 1 else 0) +
            (if (query.sourceId != null) 1 else 0)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HausSpacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.browse_controls_heading),
            color = HausColors.current.ink,
            style = HausTypography.section,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HausSpacing.xs),
        ) {
            BrowseControlMenuButton(
                label =
                    if (sortIsActive)
                        stringResource(
                            Res.string.browse_controls_sort_value_format,
                            sortLabel,
                        )
                    else sortLabel,
                contentDescription =
                    stringResource(
                        Res.string.browse_controls_sort_value_format,
                        sortLabel,
                    ),
                testTag = "browse-controls-sort",
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                LibrarySort.entries.forEach { sort ->
                    val itemLabel = stringResource(sort.labelResource())
                    BrowseControlMenuItem(
                        label = itemLabel,
                        selected = sortIsActive && query.sort == sort,
                        testTag = "browse-sort-${sort.name.lowercase()}",
                        onClick = {
                            sortExpanded = false
                            onSortChange(sort)
                        },
                    )
                }
            }
            BrowseControlMenuButton(
                label = directionLabel,
                contentDescription =
                    stringResource(
                        Res.string.browse_controls_direction_value_format,
                        directionLabel,
                    ),
                testTag = "browse-controls-direction",
                expanded = directionExpanded,
                onExpandedChange = { directionExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                LibrarySortDirection.entries.forEach { direction ->
                    val itemLabel = stringResource(direction.labelResource())
                    BrowseControlMenuItem(
                        label = itemLabel,
                        selected = sortIsActive && query.direction == direction,
                        testTag =
                            "browse-direction-${direction.name.lowercase()}",
                        onClick = {
                            directionExpanded = false
                            onSortDirectionChange(direction)
                        },
                    )
                }
            }
            BrowseControlMenuButton(
                label = stringResource(Res.string.browse_controls_filters),
                contentDescription =
                    stringResource(Res.string.browse_controls_filters),
                testTag = "browse-controls-filters",
                expanded = filtersExpanded,
                onExpandedChange = { filtersExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                BrowseControlToggleItem(
                    label = stringResource(Res.string.browse_filter_favorite),
                    checked = query.favoriteOnly,
                    testTag = "browse-filter-favorite",
                    onClick = { onFavoriteOnlyChange(!query.favoriteOnly) },
                )
                BrowseControlToggleItem(
                    label = stringResource(Res.string.browse_filter_artwork),
                    checked = query.artworkOnly,
                    testTag = "browse-filter-artwork",
                    onClick = { onArtworkOnlyChange(!query.artworkOnly) },
                )
            }
            BrowseControlMenuButton(
                label = stringResource(Res.string.browse_controls_source),
                contentDescription =
                    stringResource(
                        Res.string.browse_controls_source_value_format,
                        sourceLabel,
                    ),
                testTag = "browse-controls-source",
                expanded = sourceExpanded,
                onExpandedChange = { sourceExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                val allSourcesLabel =
                    stringResource(Res.string.browse_filter_all_sources)
                BrowseControlMenuItem(
                    label = allSourcesLabel,
                    selected = query.sourceId == null,
                    testTag = "browse-source-all",
                    onClick = {
                        sourceExpanded = false
                        onSourceIdChange(null)
                    },
                )
                sourceOptions.forEach { sourceOption ->
                    val sourceDescription =
                        stringResource(
                            Res.string.browse_source_format,
                            sourceOption.displayName,
                        )
                    BrowseControlMenuItem(
                        label = sourceOption.displayName,
                        contentDescription = sourceDescription,
                        selected = query.sourceId == sourceOption.id,
                        testTag = "browse-source-${sourceOption.id}",
                        onClick = {
                            sourceExpanded = false
                            onSourceIdChange(sourceOption.id)
                        },
                    )
                }
            }
        }
        if (activeFilterCount > 0) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(HausSpacing.xs),
            ) {
                if (query.favoriteOnly) {
                    ActiveBrowseFilter(
                        label =
                            stringResource(Res.string.browse_filter_favorite),
                        testTag = "browse-filter-remove-favorite",
                        onRemove = { onFavoriteOnlyChange(false) },
                    )
                }
                if (query.artworkOnly) {
                    ActiveBrowseFilter(
                        label =
                            stringResource(Res.string.browse_filter_artwork),
                        testTag = "browse-filter-remove-artwork",
                        onRemove = { onArtworkOnlyChange(false) },
                    )
                }
                if (query.sourceId != null) {
                    ActiveBrowseFilter(
                        label = sourceLabel,
                        testTag = "browse-filter-remove-source",
                        onRemove = { onSourceIdChange(null) },
                    )
                }
            }
            Button(
                onClick = {
                    onFavoriteOnlyChange(false)
                    onArtworkOnlyChange(false)
                    onSourceIdChange(null)
                },
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .testTag("browse-filter-clear-all"),
                cornerRadius = HausShapes.control,
                insideMargin = PaddingValues(horizontal = HausSpacing.md),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.panel,
                        contentColor = HausColors.current.ink,
                    ),
            ) {
                Text(
                    text = stringResource(Res.string.browse_filter_clear_all),
                    style = HausTypography.label,
                )
            }
        }
    }
}

@Composable
private fun BrowseControlMenuButton(
    label: String,
    contentDescription: String = label,
    testTag: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Button(
            onClick = { onExpandedChange(!expanded) },
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(testTag)
                    .semantics { this.contentDescription = contentDescription },
            cornerRadius = HausShapes.control,
            insideMargin = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    color = HausColors.current.panel,
                    contentColor = HausColors.current.ink,
                ),
        ) {
            Text(
                text = label,
                style = HausTypography.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content,
        )
    }
}

@Composable
private fun BrowseControlMenuItem(
    label: String,
    selected: Boolean,
    testTag: String,
    contentDescription: String = label,
    onClick: () -> Unit,
) {
    val stateDescription =
        stringResource(
            if (selected) Res.string.browse_selected_state
            else Res.string.browse_unselected_state,
        )
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = HausColors.current.ink,
                style = HausTypography.secondary,
                maxLines = 2,
            )
        },
        onClick = onClick,
        modifier =
            Modifier.heightIn(min = 48.dp).testTag(testTag).semantics {
                this.contentDescription = contentDescription
                this.selected = selected
                this.stateDescription = stateDescription
            },
    )
}

@Composable
private fun BrowseControlToggleItem(
    label: String,
    checked: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val stateDescription =
        stringResource(
            if (checked) Res.string.browse_selected_state
            else Res.string.browse_unselected_state,
        )
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = HausColors.current.ink,
                style = HausTypography.secondary,
            )
        },
        onClick = onClick,
        modifier =
            Modifier.heightIn(min = 48.dp).testTag(testTag).semantics {
                contentDescription = label
                toggleableState =
                    if (checked) ToggleableState.On else ToggleableState.Off
                this.stateDescription = stateDescription
            },
    )
}

@Composable
private fun ActiveBrowseFilter(
    label: String,
    testTag: String,
    onRemove: () -> Unit,
) {
    val removeDescription =
        stringResource(Res.string.browse_filter_remove_format, label)
    Button(
        onClick = onRemove,
        modifier =
            Modifier.heightIn(min = 48.dp).testTag(testTag).semantics {
                contentDescription = removeDescription
            },
        cornerRadius = HausShapes.control,
        insideMargin = PaddingValues(horizontal = HausSpacing.md),
        colors =
            ButtonDefaults.buttonColors(
                color = HausColors.current.panel,
                contentColor = HausColors.current.ink,
            ),
    ) {
        Text("$label ×", style = HausTypography.label, maxLines = 1)
    }
}

private fun LibrarySort.labelResource() =
    when (this) {
        LibrarySort.Title -> Res.string.browse_sort_title
        LibrarySort.Artist -> Res.string.browse_sort_artist
        LibrarySort.Album -> Res.string.browse_sort_album
        LibrarySort.Added -> Res.string.browse_sort_added
        LibrarySort.Modified -> Res.string.browse_sort_modified
        LibrarySort.PlayCount -> Res.string.browse_sort_play_count
        LibrarySort.Favorite -> Res.string.browse_sort_favorite
    }

private fun LibrarySortDirection.labelResource() =
    when (this) {
        LibrarySortDirection.Ascending -> Res.string.browse_sort_ascending
        LibrarySortDirection.Descending -> Res.string.browse_sort_descending
    }

@Composable
private fun BrowseModeButton(
    mode: BrowseMode,
    isSelected: Boolean,
    compact: Boolean,
    modifier: Modifier,
    onModeChange: (BrowseMode) -> Unit,
) {
    val label = stringResource(mode.displayLabelResource())
    Button(
        onClick = { onModeChange(mode) },
        modifier =
            modifier.heightIn(min = 48.dp).semantics {
                contentDescription = label
                selected = isSelected
            },
        cornerRadius = HausShapes.control,
        insideMargin =
            PaddingValues(
                horizontal = if (compact) 0.dp else 12.dp,
                vertical = 10.dp,
            ),
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
            label,
            style = HausTypography.label,
            fontWeight =
                if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun BrowseMode.displayLabelResource() =
    when (this) {
        BrowseMode.Albums -> Res.string.browse_mode_albums
        BrowseMode.Artists -> Res.string.browse_mode_artists
        BrowseMode.Songs -> Res.string.browse_mode_songs
        BrowseMode.Favorites -> Res.string.browse_mode_favorites
        BrowseMode.RecentlyPlayed -> Res.string.browse_mode_recently_played
        BrowseMode.RecentlyAdded -> Res.string.browse_mode_recently_added
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
                .clip(RoundedCornerShape(HausShapes.group))
                .hausClickable(onClick = onClick)
                .semantics { contentDescription = albumContentDescription },
        cornerRadius = HausShapes.group,
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
                        .clip(RoundedCornerShape(HausShapes.artwork))
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
                style = HausTypography.title,
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
                style = HausTypography.label,
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
                .clip(RoundedCornerShape(HausShapes.group))
                .background(HausColors.current.panel.copy(alpha = 0.54f))
                .border(
                    1.dp,
                    HausColors.current.line,
                    RoundedCornerShape(HausShapes.group),
                )
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
                    style = HausTypography.title,
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
                    style = HausTypography.secondary,
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
        cornerRadius = HausShapes.group,
        colors = CardDefaults.defaultColors(color = HausColors.current.panel),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(Res.string.scanning),
                style = HausTypography.secondary,
                fontWeight = FontWeight.SemiBold,
                color = HausColors.current.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    Res.string.scan_progress_format,
                    foldersVisited,
                    filesVisited,
                    tracksAdded),
                style = HausTypography.label,
                color = HausColors.current.ink.copy(alpha = 0.7f))
            if (!latestItem.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    latestItem,
                    style = HausTypography.label,
                    color = HausColors.current.ink.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            ScanActionButton(
                labels.cancel,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Renders the terminal scan-outcome panel with summary, rescan/retry, report
 * toggle, and remove-missing actions.
 *
 * @param session the terminal scan session to display.
 * @param completedSummarySession the separate completed session whose summary
 *   is retained after a failed or cancelled terminal report.
 * @param completedSummarySource the configured source for the retained
 *   completed session, if it still exists.
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
    completedSummarySession: ScanSession?,
    completedSummarySource: LibrarySource?,
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
            style = HausTypography.title,
        )
        Text(
            stringResource(
                Res.string.scan_summary_format,
                session.foldersVisited,
                session.filesVisited,
                session.tracksAdded,
                session.tracksUpdated,
                session.filesSkipped,
            ),
            color = HausColors.current.muted,
            style = HausTypography.label,
        )
        val summarySession =
            if (session.status == ScanStatus.Completed) session
            else completedSummarySession
        summarySession?.changeSummary?.let { summary ->
            if (summarySession.id != session.id) {
                Text(
                    stringResource(
                        Res.string.latest_completed_scan_format,
                        completedSummarySource?.displayName
                            ?: summarySession.sourceId,
                        summarySession.id,
                    ),
                    color = HausColors.current.muted,
                    style = HausTypography.label,
                )
            }
            Text(
                stringResource(
                    Res.string.scan_changes_format,
                    summary.addedCount,
                    summary.modifiedCount,
                    summary.unchangedCount,
                    summary.missingCount,
                ),
                color = HausColors.current.muted,
                style = HausTypography.label,
                modifier = Modifier.semantics { heading() },
            )
            val details =
                summary.addedDetails +
                    summary.modifiedDetails +
                    summary.missingDetails
            if (details.isNotEmpty()) {
                Text(
                    stringResource(Res.string.scan_changes_details),
                    color = HausColors.current.muted,
                    style = HausTypography.label,
                )
                details.take(100).forEach { detail ->
                    Text(
                        detail,
                        color = HausColors.current.muted,
                        style = HausTypography.label,
                    )
                }
            }
        }
        session.terminalMessage?.takeIf(String::isNotBlank)?.let {
            Text(
                it,
                color =
                    if (session.status == ScanStatus.Failed) {
                        HausColors.current.danger
                    } else {
                        HausColors.current.muted
                    },
                style = HausTypography.label,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            source?.let {
                ScanActionButton(
                    stringResource(
                        if (session.status == ScanStatus.Completed)
                            Res.string.rescan
                        else Res.string.retry_scan,
                    ),
                    onClick = { onRescanSource(it) },
                    modifier = Modifier.weight(1f),
                    enabled = mutationsEnabled,
                )
            }
            ScanActionButton(
                stringResource(
                    if (reportVisible) Res.string.hide_scan_report
                    else Res.string.view_scan_report,
                ),
                onClick = onToggleReport,
                modifier = Modifier.weight(1f),
            )
        }
        if (session.status == ScanStatus.Completed && source != null) {
            ScanActionButton(
                stringResource(Res.string.remove_missing),
                onClick = { onRemoveMissingTracks(source, session) },
                modifier = Modifier.fillMaxWidth(),
                enabled = mutationsEnabled,
                destructive = true,
            )
        }
        if (reportVisible) {
            if (errors.isEmpty()) {
                Text(
                    stringResource(Res.string.scan_report_empty),
                    color = HausColors.current.muted,
                    style = HausTypography.label,
                )
            }
            errors.forEach { error ->
                Text(
                    stringResource(
                        Res.string.scan_report_error_format,
                        error.displayPath,
                        error.reason,
                    ),
                    color = HausColors.current.muted,
                    style = HausTypography.label,
                )
            }
        }
    }
}

/**
 * Settings-page action button: 48dp tall with the shared control shape. Primary
 * uses ink/paper; destructive uses a pulse tint; disabled uses the shared muted
 * state.
 */
@Composable
private fun ScanActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        cornerRadius = HausShapes.control,
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (destructive) {
                        HausColors.current.dangerPanel
                    } else {
                        HausColors.current.ink
                    },
                contentColor =
                    if (destructive) HausColors.current.danger
                    else HausColors.current.paper,
                disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                disabledContentColor = HausColors.current.muted),
    ) {
        Text(
            label,
            style = HausTypography.secondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
