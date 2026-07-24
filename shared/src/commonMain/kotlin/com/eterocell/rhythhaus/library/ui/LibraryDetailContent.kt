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
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.TrackArtworkLoadState
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberLazyTrackArtworkState
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import top.yukonga.miuix.kmp.basic.Surface

internal sealed interface DrillDownAction {
    data class SelectTrack(val track: Track) : DrillDownAction

    data object ToggleTransport : DrillDownAction
}

internal fun dispatchDrillDownAction(
    action: DrillDownAction,
    onTrackClick: (Track) -> Unit,
    onPlayPause: () -> Unit,
) {
    when (action) {
        is DrillDownAction.SelectTrack -> onTrackClick(action.track)
        DrillDownAction.ToggleTransport -> onPlayPause()
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
internal fun DrillDownView(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    topBarArtworkTrack: Track? = null,
    selectedTrack: Track?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    libraryTracks: List<LibraryTrack>,
    onBack: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onPlayPause: () -> Unit,
    selectionPageKey: TrackSelectionPageKey,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
) {
    val selectionModeActive = trackSelectionState.pageKey == selectionPageKey && trackSelectionState.selectedTrackIds.isNotEmpty()
    var selectedTrackId by remember { mutableStateOf(selectedTrack?.id) }
    LaunchedEffect(playbackState.currentTrack?.id) {
        playbackState.currentTrack?.id?.let { selectedTrackId = it }
    }
    val currentTrack = tracks.firstOrNull { it.id == selectedTrackId } ?: selectedTrack

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .leftEdgeSwipeBack(onBack),
    ) {
        val drillDownStatusBarHeight = rememberSystemBarTopPadding()
        val drillDownBackdrop = rememberRhythHausBackdrop()
        val listState = rememberLazyListState()
        val miuixScrollBehavior = rememberMiuixTopAppBarScrollBehavior()
        val topBarArtworkState = rememberLazyTrackArtworkState(
            trackId = topBarArtworkTrack?.id,
            eagerArtworkBytes = topBarArtworkTrack?.artworkBytes,
        ).value
        val drillDownArtwork = DrillDownArtwork(
            representativeTrackId = topBarArtworkTrack?.id,
            state = topBarArtworkState,
        )
        val scrollOwner = drillDownScrollOwner(drillDownArtwork)
        val artworkBytes = (topBarArtworkState as? TrackArtworkLoadState.Available)?.bytes
        val hasTopBarArtwork = artworkBytes != null
        val collapsedChromeHeight = drillDownStatusBarHeight + NestedScrollChromeToolbarHeight
        val density = LocalDensity.current
        val artworkGeometry = ArtworkCollapseGeometry(
            expandedHeightPx = with(density) { maxWidth.toPx() },
            collapsedHeightPx = with(density) { collapsedChromeHeight.toPx() },
        )
        val artworkSnapshot by remember(listState, artworkGeometry) {
            derivedStateOf {
                artworkGeometry.snapshot(
                    firstVisibleItemIndex = listState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                )
            }
        }
        val expandedArtworkHeight = maxWidth
        val upperSliceHeight = with(density) { artworkSnapshot.upperSliceHeightPx.toDp() }
        val lowerSliceHeight = with(density) { artworkSnapshot.lowerSliceHeightPx.toDp() }
        val lowerSliceImageOffset = with(density) { artworkSnapshot.lowerSliceImageOffsetPx.toDp() }
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
                        .then(
                            if (hasTopBarArtwork) {
                                Modifier
                            } else {
                                Modifier
                                    .nestedScroll(miuixScrollBehavior.nestedScrollConnection)
                                    .padding(horizontal = 20.dp)
                            },
                        ),
                    contentPadding = if (hasTopBarArtwork) {
                        PaddingValues()
                    } else {
                        PaddingValues(top = drillDownStatusBarHeight + DrillDownMiuixScrollContentTopPadding)
                    },
                    verticalArrangement = if (hasTopBarArtwork) {
                        Arrangement.Top
                    } else {
                        Arrangement.spacedBy(18.dp)
                    },
                ) {
                    if (hasTopBarArtwork) {
                        if (artworkHeaderItemPolicy(artworkGeometry) == ArtworkHeaderItemPolicy.UpperAndStickyLower) {
                            item(key = "artwork-upper") {
                                DrillDownArtworkUpperSlice(
                                    artworkBytes = requireNotNull(artworkBytes),
                                    expandedHeight = expandedArtworkHeight,
                                    upperSliceHeight = upperSliceHeight,
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
                            )
                        }
                        item(key = "section") {
                            DrillDownListItem { SectionLabel(title = title, subtitle = subtitle) }
                        }
                        items(tracks, key = { it.id }) { track ->
                            DrillDownListItem {
                                DrillDownTrackRow(
                                    track = track,
                                    isNowPlaying = track.id == selectedTrackId,
                                    selectionModeActive = selectionModeActive,
                                    isSelected = track.id in trackSelectionState.selectedTrackIds,
                                    onSelected = { selectedTrackId = track.id },
                                    onTrackClick = onTrackClick,
                                    onPlayPause = onPlayPause,
                                    selectionPageKey = selectionPageKey,
                                    onTrackSelectionAction = onTrackSelectionAction,
                                )
                            }
                        }
                        item(key = "now-playing-spacer") {
                            Spacer(Modifier.height(bottomContentPadding))
                        }
                    } else {
                        item { SectionLabel(title = title, subtitle = subtitle) }
                        items(tracks, key = { it.id }) { track ->
                            DrillDownTrackRow(
                                track = track,
                                isNowPlaying = track.id == selectedTrackId,
                                selectionModeActive = selectionModeActive,
                                isSelected = track.id in trackSelectionState.selectedTrackIds,
                                onSelected = { selectedTrackId = track.id },
                                onTrackClick = onTrackClick,
                                onPlayPause = onPlayPause,
                                selectionPageKey = selectionPageKey,
                                onTrackSelectionAction = onTrackSelectionAction,
                            )
                        }
                        item { Spacer(Modifier.height(bottomContentPadding)) }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ArtworkDrillDownListSpacing.horizontalPaddingDp.dp)
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
    onTrackClick: (Track) -> Unit,
    onPlayPause: () -> Unit,
    selectionPageKey: TrackSelectionPageKey,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
) {
    TrackRow(
        track = track,
        isNowPlaying = isNowPlaying,
        selectionModeActive = selectionModeActive,
        isSelected = isSelected,
        onPlay = {
            onSelected()
            dispatchDrillDownAction(
                action = DrillDownAction.SelectTrack(track),
                onTrackClick = onTrackClick,
                onPlayPause = onPlayPause,
            )
        },
        onToggleSelection = {
            onTrackSelectionAction(TrackSelectionAction.Toggle(selectionPageKey, track.id))
        },
        onStartSelection = {
            onTrackSelectionAction(TrackSelectionAction.Start(selectionPageKey, track.id))
        },
    )
}
