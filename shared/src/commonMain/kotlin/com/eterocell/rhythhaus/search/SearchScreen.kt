package com.eterocell.rhythhaus.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import com.eterocell.rhythhaus.ui.hausCombinedClickable
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.taglib.TagLibReader
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.search_no_tracks_match_format
import rhythhaus.shared.generated.resources.search_placeholder
import rhythhaus.shared.generated.resources.search_results_count_many
import rhythhaus.shared.generated.resources.search_results_count_one
import rhythhaus.shared.generated.resources.search_results_count_zero
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import com.eterocell.rhythhaus.library.ui.EqualizerStrip
import com.eterocell.rhythhaus.library.ui.LibraryScrollPosition
import com.eterocell.rhythhaus.library.ui.TrackRowActivation
import com.eterocell.rhythhaus.library.ui.TrackRowGesture
import com.eterocell.rhythhaus.library.ui.TrackSelectionAction
import com.eterocell.rhythhaus.library.ui.TrackSelectionPageKey
import com.eterocell.rhythhaus.library.ui.TrackSelectionState
import com.eterocell.rhythhaus.library.ui.trackRowActivation
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.select_track_format
import top.yukonga.miuix.kmp.basic.Checkbox

@Composable
fun SearchScreen(
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val resultListState = rememberLazyListState()

    val filtered = remember(query, libraryTracks) {
        if (query.isBlank()) {
            emptyList()
        } else {
            libraryTracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            }
        }
    }
    val selectionPageKey = TrackSelectionPageKey.Search
    val selectionModeActive = trackSelectionState.pageKey == selectionPageKey && trackSelectionState.selectedTrackIds.isNotEmpty()
    val visibleTrackIds = filtered.map(LibraryTrack::id)

    LaunchedEffect(visibleTrackIds) {
        onTrackSelectionAction(TrackSelectionAction.ReconcileVisible(selectionPageKey, visibleTrackIds))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(resultListState.firstVisibleItemIndex, resultListState.firstVisibleItemScrollOffset) {
        onScrollPositionChanged(
            LibraryScrollPosition(
                firstVisibleItemIndex = resultListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = resultListState.firstVisibleItemScrollOffset,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .clickable(enabled = false, onClick = {}),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RhythHausTopAppBar(
                    title = stringResource(Res.string.search),
                    onBack = onDismiss,
                )

                // Search field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = HausColors.current.muted.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        insideMargin = DpSize(16.dp, 14.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = HausColors.current.paper,
                            labelColor = HausColors.current.muted,
                            borderColor = Color.Transparent,
                        ),
                        cornerRadius = 12.dp,
                        label = stringResource(Res.string.search_placeholder),
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(HausColors.current.pulse),
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { query = "" },
                                    backgroundColor = Color.Transparent,
                                    minWidth = 40.dp,
                                    minHeight = 40.dp,
                                ) {
                                    Text(
                                        stringResource(Res.string.clear),
                                        color = HausColors.current.pulse,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }

                // Results
                if (query.isNotBlank()) {
                    Text(
                        text = searchResultCountLabel(filtered.size),
                        color = HausColors.current.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.search_no_tracks_match_format, query),
                            color = HausColors.current.muted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            state = resultListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filtered, key = { it.id }) { track ->
                                SearchResultRow(
                                    track = track,
                                    isNowPlaying = playbackState.currentTrack?.id == track.id,
                                    isPlaying = playbackState.isPlaying,
                                    selectionModeActive = selectionModeActive,
                                    isSelected = track.id in trackSelectionState.selectedTrackIds,
                                    onPlay = {
                                        selectLibraryTrackForPlayback(
                                            playbackController = playbackController,
                                            visibleQueue = filtered.map { it.toPlayableTrack() },
                                            selectedTrackId = track.id,
                                        )
                                        onDismiss()
                                    },
                                    onToggleSelection = {
                                        onTrackSelectionAction(TrackSelectionAction.Toggle(selectionPageKey, track.id))
                                    },
                                    onStartSelection = {
                                        onTrackSelectionAction(TrackSelectionAction.Start(selectionPageKey, track.id))
                                    },
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun searchResultCountLabel(count: Int): String = when (count) {
    0 -> stringResource(Res.string.search_results_count_zero)
    1 -> stringResource(Res.string.search_results_count_one)
    else -> stringResource(Res.string.search_results_count_many, count)
}

@Composable
private fun SearchResultRow(
    track: LibraryTrack,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    selectionModeActive: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
) {
    val selectTrackLabel = stringResource(Res.string.select_track_format, track.title)
    val nowPlayingDescription = stringResource(Res.string.now_playing_badge)
    fun activate(gesture: TrackRowGesture) {
        when (trackRowActivation(selectionModeActive, gesture)) {
            TrackRowActivation.Play -> onPlay()
            TrackRowActivation.ToggleSelection -> onToggleSelection()
            TrackRowActivation.StartSelection -> onStartSelection()
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hausCombinedClickable(
                onClick = { activate(TrackRowGesture.Click) },
                onLongClick = { activate(TrackRowGesture.LongClick) },
                onLongClickLabel = selectTrackLabel,
            )
            .semantics {
                contentDescription = selectTrackLabel
                if (selectionModeActive) toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
                if (isNowPlaying) stateDescription = nowPlayingDescription
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) HausColors.current.panel else HausColors.current.paper,
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionModeActive) {
                Checkbox(
                    state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggleSelection,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isNowPlaying && isPlaying) HausColors.current.pulse else HausColors.current.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(track.artist, track.album).joinToString(" · "),
                    color = HausColors.current.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            if (isNowPlaying && isPlaying) {
                EqualizerStrip(active = true)
            }
        }
    }
}
