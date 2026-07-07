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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.ui.BackChip
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.library.LibraryTrack
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

@Composable
fun SearchScreen(
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
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
                // Title bar with back
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackChip(onClick = onDismiss)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(Res.string.search), color = HausColors.current.ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

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
                                    onClick = {
                                        val playable = libraryTracks.map { it.toPlayableTrack() }
                                        playbackController.setQueue(playable, track.id)
                                        playbackController.play()
                                        onDismiss()
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
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().hausClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) HausColors.current.panel else HausColors.current.paper,
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
