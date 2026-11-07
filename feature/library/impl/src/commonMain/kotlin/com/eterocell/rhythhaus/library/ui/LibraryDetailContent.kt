package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.album_detail_subtitle_format
import rhythhaus.feature.library.generated.resources.artist_detail_subtitle_format
import rhythhaus.feature.library.generated.resources.unknown_artist
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

/** A resolved drill-down row action. */
public sealed interface DrillDownAction {
    /** Requests playback of the ordered detail sequence at [track]. */
    public data class SelectTrack(
        /** The selected track to play. */
        public val track: Track,
    ) : DrillDownAction

    /** Retained no-op transport toggle; playback control stays Shared-owned. */
    public data object ToggleTransport : DrillDownAction
}

/**
 * Dispatches a resolved [DrillDownAction] to its playback callback.
 *
 * @param action the resolved row action.
 * @param onPlayTrack requests playback of the ordered tracks at the selected
 *   occurrence.
 * @param orderedTracks the ordered detail track sequence.
 */
public fun dispatchDrillDownAction(
    action: DrillDownAction,
    onPlayTrack: (List<Track>, Track) -> Unit,
    orderedTracks: List<Track>,
) {
    when (action) {
        is DrillDownAction.SelectTrack ->
            onPlayTrack(orderedTracks, action.track)
        DrillDownAction.ToggleTransport -> Unit
    }
}

/**
 * Renders an already-resolved album or artist detail destination using only raw
 * feature inputs and callbacks. It owns its artwork, list state, local
 * backdrop, and selected-row fallback; Shared owns route chrome, Back,
 * selection state, playback policy, and scroll storage.
 *
 * @param title the detail title (album or artist name).
 * @param summary raw detail counts used to compose the localized subtitle.
 * @param tracks the resolved ordered detail track sequence.
 * @param representativeArtworkTrack the representative track whose artwork is
 *   shown above the detail summary, or null.
 * @param currentTrackId the current playback track ID, or null.
 * @param selectionPage the Shared-owned selection page this detail renders.
 * @param selectionModeActive whether rows currently select rather than play.
 * @param selectedTrackIds immutable selected IDs effective for this page.
 * @param labels Shared-owned localized wording.
 * @param artworkLoader lazily resolves artwork bytes for a track ID.
 * @param onBack requests Shared route dismissal.
 * @param onPlayTrack requests playback of the ordered tracks at the selected
 *   occurrence.
 * @param onToggleSelection requests one toggle of the given track ID.
 * @param onStartSelection requests selection beginning with the given track ID.
 * @param onVisibleTrackIdsChanged receives the page's track IDs whenever the
 *   page track sequence changes.
 * @param onScrollPositionChanged receives first visible item index and pixel
 *   offset.
 * @param bottomContentPadding reserved trailing list space for Shared shell
 *   chrome.
 * @param favoriteTrackIds immutable authoritative favorite IDs.
 * @param onSetTrackFavorite requests a desired favorite state.
 * @param onEditTrackMetadata requests editing app-local metadata for a track.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
public fun DrillDownView(
    title: String,
    summary: LibraryDetailSummary,
    tracks: List<Track>,
    representativeArtworkTrack: Track?,
    currentTrackId: String?,
    selectionPage: LibrarySelectionPage,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onBack: () -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit,
    onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged:
        (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    onEditTrackMetadata: ((String) -> Unit)? = null,
) {
    val subtitle =
        when (summary) {
            is LibraryDetailSummary.Album ->
                stringResource(
                    Res.string.album_detail_subtitle_format,
                    summary.trackCount,
                    summary.artist ?: stringResource(Res.string.unknown_artist))
            is LibraryDetailSummary.Artist ->
                stringResource(
                    Res.string.artist_detail_subtitle_format,
                    summary.albumCount,
                    summary.trackCount)
        }
    var selectedTrackId by
        remember(tracks) {
            mutableStateOf(currentTrackId ?: tracks.firstOrNull()?.id)
        }
    LaunchedEffect(currentTrackId) {
        currentTrackId?.let { selectedTrackId = it }
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().leftEdgeSwipeBack(onBack).semantics {
                paneTitle = title
            },
    ) {
        val drillDownBackdrop = rememberRhythHausBackdrop()
        val listState = rememberLazyListState()
        val topBarArtworkState =
            rememberLazyTrackArtworkState(
                    trackId = representativeArtworkTrack?.id,
                    eagerArtworkBytes =
                        representativeArtworkTrack?.artworkBytes,
                    artworkLoader = artworkLoader,
                )
                .value
        val artworkBytes =
            (topBarArtworkState as? TrackArtworkLoadState.Available)?.bytes
        LaunchedEffect(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset) {
                onScrollPositionChanged(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset)
            }
        LaunchedEffect(tracks) {
            onVisibleTrackIdsChanged(tracks.map { it.id })
        }
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .recordRhythHausBackdrop(drillDownBackdrop),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HausColors.current.background) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                bottom = bottomContentPadding,
                            ),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item(key = "detail-artwork") {
                            BoxWithConstraints(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val artworkSize = maxWidth.coerceAtMost(360.dp)
                                Box(
                                    Modifier.size(artworkSize)
                                        .aspectRatio(1f)
                                        .clip(
                                            RoundedCornerShape(
                                                HausShapes.artwork),
                                        )
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    HausColors.current
                                                        .primaryAction,
                                                    HausColors.current
                                                        .textPrimary,
                                                ),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ArtworkImage(
                                        artworkBytes = artworkBytes,
                                        contentDescription =
                                            labels.albumArtwork,
                                        role = ArtworkImageRole.Hero,
                                        modifier = Modifier.fillMaxSize(),
                                        fallback = {
                                            Text(
                                                text =
                                                    title.take(2).uppercase(),
                                                color = Color.White,
                                                style =
                                                    HausTypography.pageTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 40.sp,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        item(key = "detail-summary") {
                            SectionLabel(title = subtitle, subtitle = null)
                        }
                        items(tracks, key = { it.id }) { track ->
                            DrillDownTrackRow(
                                track = track,
                                isNowPlaying = track.id == selectedTrackId,
                                selectionModeActive = selectionModeActive,
                                isSelected = track.id in selectedTrackIds,
                                onSelected = { selectedTrackId = track.id },
                                orderedTracks = tracks,
                                labels = labels,
                                artworkLoader = artworkLoader,
                                onPlayTrack = onPlayTrack,
                                onToggleSelection = onToggleSelection,
                                onStartSelection = onStartSelection,
                                favorite = track.id in favoriteTrackIds,
                                onSetFavorite = { favorite ->
                                    onSetTrackFavorite(track.id, favorite)
                                },
                                onEditMetadata =
                                    onEditTrackMetadata?.let { edit ->
                                        { edit(track.id) }
                                    },
                            )
                        }
                    }
                }
        }
        DrillDownScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun DrillDownTrackRow(
    track: Track,
    isNowPlaying: Boolean,
    selectionModeActive: Boolean,
    isSelected: Boolean,
    onSelected: () -> Unit,
    orderedTracks: List<Track>,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onPlayTrack: (List<Track>, Track) -> Unit,
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    favorite: Boolean,
    onSetFavorite: (Boolean) -> Unit,
    onEditMetadata: (() -> Unit)?,
) {
    TrackRow(
        track = track,
        isNowPlaying = isNowPlaying,
        selectionModeActive = selectionModeActive,
        isSelected = isSelected,
        labels = labels,
        artworkLoader = artworkLoader,
        onPlay = {
            onSelected()
            dispatchDrillDownAction(
                action = DrillDownAction.SelectTrack(track),
                onPlayTrack = onPlayTrack,
                orderedTracks = orderedTracks,
            )
        },
        onToggleSelection = { onToggleSelection(track.id) },
        onStartSelection = { onStartSelection(track.id) },
        favorite = favorite,
        onSetFavorite = onSetFavorite,
        onEditMetadata = onEditMetadata,
    )
}
