package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.taglib.TagLibReader
import top.yukonga.miuix.kmp.basic.Surface
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.nowplaying.NowPlayingBar
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarContentPadding
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop

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
@OptIn(ExperimentalComposeUiApi::class)
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .leftEdgeSwipeBack(onBack),
    ) {
        val drillDownStatusBarHeight = rememberSystemBarTopPadding()
        val drillDownBackdrop = rememberRhythHausBackdrop()
        val listState = rememberLazyListState()
        val miuixScrollBehavior = rememberMiuixTopAppBarScrollBehavior()
        val drillDownTopPadding = if (topBarArtworkTrack != null) {
            maxWidth + 20.dp
        } else {
            drillDownStatusBarHeight + DrillDownMiuixScrollContentTopPadding
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
                        .nestedScroll(miuixScrollBehavior.nestedScrollConnection)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = drillDownTopPadding),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item { SectionLabel(title = title, subtitle = subtitle) }
                    items(tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            selected = track.id == selectedTrackId,
                            onClick = {
                                selectedTrackId = track.id
                                dispatchDrillDownAction(
                                    action = DrillDownAction.SelectTrack(track),
                                    onTrackClick = onTrackClick,
                                    onPlayPause = onPlayPause,
                                )
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
        DrillDownMiuixScrollChrome(
            scrollBehavior = miuixScrollBehavior,
            title = title,
            topBarArtworkTrackId = topBarArtworkTrack?.id,
            topBarArtworkBytes = topBarArtworkTrack?.artworkBytes,
            onBack = onBack,
            backdrop = drillDownBackdrop,
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
                    onPlayPause = {
                        dispatchDrillDownAction(
                            action = DrillDownAction.ToggleTransport,
                            onTrackClick = onTrackClick,
                            onPlayPause = onPlayPause,
                        )
                    },
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
