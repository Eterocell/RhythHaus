package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.album_detail_subtitle_format
import rhythhaus.feature.library.generated.resources.artist_detail_subtitle_format
import rhythhaus.feature.library.generated.resources.unknown_artist
import top.yukonga.miuix.kmp.basic.Surface

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

internal fun shouldApplyDrillDownOverscroll(
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Boolean = canScrollForward || canScrollBackward

/**
 * Renders an already-resolved album or artist detail destination using only raw
 * feature inputs and callbacks. It owns the artwork-collapse chrome, list
 * state, local backdrop, and selected-row fallback; Shared owns route
 * resolution, Back, selection state, playback policy, and scroll storage.
 *
 * @param title the detail title (album or artist name).
 * @param summary raw detail counts used to compose the localized subtitle.
 * @param tracks the resolved ordered detail track sequence.
 * @param topBarArtworkTrack the representative track whose artwork drives the
 *   expanding chrome, or null.
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
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
public fun DrillDownView(
    title: String,
    summary: LibraryDetailSummary,
    tracks: List<Track>,
    topBarArtworkTrack: Track?,
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
        modifier = Modifier.fillMaxSize().leftEdgeSwipeBack(onBack),
    ) {
        val drillDownStatusBarHeight = rememberSystemBarTopPadding()
        val drillDownBackdrop = rememberRhythHausBackdrop()
        val listState = rememberLazyListState()
        val shouldApplyOverscroll by
            remember(listState) {
                derivedStateOf {
                    shouldApplyDrillDownOverscroll(
                        canScrollForward = listState.canScrollForward,
                        canScrollBackward = listState.canScrollBackward,
                    )
                }
            }
        val overscrollEffect =
            if (shouldApplyOverscroll) rememberOverscrollEffect() else null
        val miuixScrollBehavior = rememberMiuixTopAppBarScrollBehavior {
            shouldApplyDrillDownOverscroll(
                canScrollForward = listState.canScrollForward,
                canScrollBackward = listState.canScrollBackward,
            )
        }
        val topBarArtworkState =
            rememberLazyTrackArtworkState(
                    trackId = topBarArtworkTrack?.id,
                    eagerArtworkBytes = topBarArtworkTrack?.artworkBytes,
                    artworkLoader = artworkLoader,
                )
                .value
        val drillDownArtwork =
            DrillDownArtwork(
                representativeTrackId = topBarArtworkTrack?.id,
                state = topBarArtworkState,
            )
        val scrollOwner = drillDownScrollOwner(drillDownArtwork)
        val artworkBytes =
            (topBarArtworkState as? TrackArtworkLoadState.Available)?.bytes
        val hasTopBarArtwork = artworkBytes != null
        val collapsedChromeHeight =
            drillDownStatusBarHeight + NestedScrollChromeToolbarHeight
        val density = LocalDensity.current
        val artworkGeometry =
            ArtworkCollapseGeometry(
                expandedHeightPx = with(density) { maxWidth.toPx() },
                collapsedHeightPx =
                    with(density) { collapsedChromeHeight.toPx() },
            )
        val artworkSnapshot by
            remember(listState, artworkGeometry) {
                derivedStateOf {
                    artworkGeometry.snapshot(
                        firstVisibleItemIndex = listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset =
                            listState.firstVisibleItemScrollOffset,
                    )
                }
            }
        val expandedArtworkHeight = maxWidth
        val upperSliceHeight =
            with(density) { artworkSnapshot.upperSliceHeightPx.toDp() }
        val lowerSliceHeight =
            with(density) { artworkSnapshot.lowerSliceHeightPx.toDp() }
        val lowerSliceImageOffset =
            with(density) { artworkSnapshot.lowerSliceImageOffsetPx.toDp() }
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
                color = HausColors.current.paper) {
                    LazyColumn(
                        state = listState,
                        overscrollEffect = overscrollEffect,
                        modifier =
                            Modifier.fillMaxSize()
                                .then(
                                    if (hasTopBarArtwork) {
                                        Modifier
                                    } else {
                                        Modifier.nestedScroll(
                                                miuixScrollBehavior
                                                    .nestedScrollConnection)
                                            .padding(horizontal = 20.dp)
                                    },
                                ),
                        contentPadding =
                            if (hasTopBarArtwork) {
                                PaddingValues()
                            } else {
                                PaddingValues(
                                    top =
                                        drillDownStatusBarHeight +
                                            DrillDownMiuixScrollContentTopPadding)
                            },
                        verticalArrangement =
                            if (hasTopBarArtwork) {
                                Arrangement.Top
                            } else {
                                Arrangement.spacedBy(18.dp)
                            },
                    ) {
                        if (hasTopBarArtwork) {
                            if (artworkHeaderItemPolicy(artworkGeometry) ==
                                ArtworkHeaderItemPolicy.UpperAndStickyLower) {
                                item(key = "artwork-upper") {
                                    DrillDownArtworkUpperSlice(
                                        artworkBytes =
                                            requireNotNull(artworkBytes),
                                        expandedHeight = expandedArtworkHeight,
                                        upperSliceHeight = upperSliceHeight,
                                        albumArtworkLabel = labels.albumArtwork,
                                    )
                                }
                            }
                            stickyHeader(key = "artwork-lower") {
                                DrillDownArtworkStickySlice(
                                    title = title,
                                    artworkBytes = requireNotNull(artworkBytes),
                                    expandedHeight = expandedArtworkHeight,
                                    collapsedHeight = lowerSliceHeight,
                                    imageOffsetY = lowerSliceImageOffset,
                                    progress = artworkSnapshot.progress,
                                    albumArtworkLabel = labels.albumArtwork,
                                )
                            }
                            item(key = "section") {
                                DrillDownListItem {
                                    SectionLabel(
                                        title = title, subtitle = subtitle)
                                }
                            }
                            items(tracks, key = { it.id }) { track ->
                                DrillDownListItem {
                                    DrillDownTrackRow(
                                        track = track,
                                        isNowPlaying =
                                            track.id == selectedTrackId,
                                        selectionModeActive =
                                            selectionModeActive,
                                        isSelected =
                                            track.id in selectedTrackIds,
                                        onSelected = {
                                            selectedTrackId = track.id
                                        },
                                        orderedTracks = tracks,
                                        labels = labels,
                                        artworkLoader = artworkLoader,
                                        onPlayTrack = onPlayTrack,
                                        onToggleSelection = onToggleSelection,
                                        onStartSelection = onStartSelection,
                                    )
                                }
                            }
                            item(key = "now-playing-spacer") {
                                Spacer(Modifier.height(bottomContentPadding))
                            }
                        } else {
                            item {
                                SectionLabel(title = title, subtitle = subtitle)
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
                                )
                            }
                            item {
                                Spacer(Modifier.height(bottomContentPadding))
                            }
                        }
                    }
                }
        }
        DrillDownScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        if (scrollOwner == DrillDownScrollOwner.Artwork) {
            DrillDownArtworkBackButton(
                progress = artworkSnapshot.progress,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
        } else {
            DrillDownMiuixScrollChrome(
                scrollBehavior = miuixScrollBehavior,
                title = title,
                onBack = onBack,
                backdrop = drillDownBackdrop,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun DrillDownListItem(
    bottomGap: Dp = ArtworkDrillDownListSpacing.itemGapDp.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal =
                        ArtworkDrillDownListSpacing.horizontalPaddingDp.dp)
                .padding(bottom = bottomGap),
    ) {
        content()
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
    )
}
