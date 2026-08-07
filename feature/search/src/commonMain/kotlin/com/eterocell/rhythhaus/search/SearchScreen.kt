package com.eterocell.rhythhaus.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import com.eterocell.rhythhaus.ui.hausCombinedClickable
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.search.generated.resources.Res
import rhythhaus.feature.search.generated.resources.search_no_tracks_match_format
import rhythhaus.feature.search.generated.resources.search_placeholder
import rhythhaus.feature.search.generated.resources.search_results_count_many
import rhythhaus.feature.search.generated.resources.search_results_count_one
import rhythhaus.feature.search.generated.resources.search_results_count_zero
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults

internal const val SearchResultCountTestTag = "search-result-count"
internal const val SearchNoMatchTestTag = "search-no-match"
internal const val SearchBottomSpacerTestTag = "search-bottom-spacer"
internal val SearchRenderedText =
    SemanticsPropertyKey<String>("SearchRenderedText")

/**
 * Shared-owned wording consumed by [SearchContent]. Value equality makes
 * unchanged labels stable across recomposition; callers provide
 * already-localized text.
 *
 * @property title Search route title.
 * @property clear Label for the query-clear action.
 * @property nowPlaying Accessibility state for the current result.
 */
public data class SearchSharedLabels(
    /** Search route title resolved by Shared. */
    public val title: String,
    /** Query-clear action label resolved by Shared. */
    public val clear: String,
    /** Current-result accessibility state resolved by Shared. */
    public val nowPlaying: String,
)

/**
 * Renders and locally controls Search over [libraryTracks], delegating
 * application policy through callbacks. It does not own navigation, playback,
 * shared selection state, scroll storage, or bottom-bar policy.
 *
 * @param libraryTracks Tracks searched in their supplied order.
 * @param currentTrackId Current playback track ID, or null when no track is
 *   current.
 * @param isPlaying Whether the current track is actively playing.
 * @param labels Shared-owned localized Search title, clear, and Now Playing
 *   labels.
 * @param selectTrackLabel Composably resolves the localized long-press/content
 *   description for a title using Shared's structured
 *   `stringResource(select_track_format, title)` when Search composes a row; no
 *   generated resource handle crosses the boundary.
 * @param selectionModeActive Whether Search rows currently select rather than
 *   play.
 * @param selectedTrackIds Immutable selected IDs effective for the Search page.
 * @param onStartSelection Requests Search selection beginning with the given
 *   track ID.
 * @param onToggleSelection Requests one toggle of the given Search track ID.
 * @param onVisibleTrackIdsChanged Receives filtered IDs whenever their sequence
 *   changes.
 * @param onScrollPositionChanged Receives first visible item index and pixel
 *   offset.
 * @param onPlayTrack Requests playback of ordered filtered results at the
 *   selected result.
 * @param onDismiss Requests Shared route dismissal.
 * @param playingIndicator Composes Shared-owned current-playing indication in a
 *   playing row.
 * @param bottomContentPadding Reserved trailing list space for Shared shell
 *   chrome.
 * @param modifier Modifier applied to the Search root.
 */
@Composable
public fun SearchContent(
    libraryTracks: List<LibraryTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    labels: SearchSharedLabels,
    selectTrackLabel: @Composable (String) -> String,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    onStartSelection: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged:
        (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    onPlayTrack:
        (
            orderedResults: List<LibraryTrack>,
            selectedTrack: LibraryTrack) -> Unit,
    onDismiss: () -> Unit,
    playingIndicator: @Composable () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val resultListState = rememberLazyListState()
    val filtered =
        remember(query, libraryTracks) {
            filterSearchTracks(libraryTracks, query)
        }
    val visibleTrackIds = filtered.map(LibraryTrack::id)
    LaunchedEffect(visibleTrackIds) {
        onVisibleTrackIdsChanged(visibleTrackIds)
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(
        resultListState.firstVisibleItemIndex,
        resultListState.firstVisibleItemScrollOffset) {
            onScrollPositionChanged(
                resultListState.firstVisibleItemIndex,
                resultListState.firstVisibleItemScrollOffset)
        }
    Box(
        modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .clickable(enabled = false, onClick = {})) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HausColors.current.paper) {
                    Column(
                        Modifier.fillMaxSize()
                            .safeContentPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            RhythHausTopAppBar(
                                title = labels.title, onBack = onDismiss)
                            Box(
                                Modifier.fillMaxWidth()
                                    .border(
                                        1.dp,
                                        HausColors.current.muted.copy(
                                            alpha = 0.3f),
                                        RoundedCornerShape(12.dp))) {
                                    TextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .focusRequester(focusRequester),
                                        insideMargin = DpSize(16.dp, 14.dp),
                                        colors =
                                            TextFieldDefaults.textFieldColors(
                                                backgroundColor =
                                                    HausColors.current.paper,
                                                labelColor =
                                                    HausColors.current.muted,
                                                borderColor =
                                                    Color.Transparent),
                                        cornerRadius = 12.dp,
                                        label =
                                            stringResource(
                                                Res.string.search_placeholder),
                                        useLabelAsPlaceholder = true,
                                        singleLine = true,
                                        textStyle =
                                            TextStyle(
                                                color = HausColors.current.ink,
                                                fontSize = 15.sp),
                                        cursorBrush =
                                            SolidColor(
                                                HausColors.current.pulse),
                                        trailingIcon =
                                            if (query.isNotEmpty()) {
                                                {
                                                    IconButton(
                                                        onClick = {
                                                            query = ""
                                                        },
                                                        backgroundColor =
                                                            Color.Transparent,
                                                        minWidth = 40.dp,
                                                        minHeight = 40.dp) {
                                                            Text(
                                                                labels.clear,
                                                                color =
                                                                    HausColors
                                                                        .current
                                                                        .pulse,
                                                                fontSize =
                                                                    12.sp,
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Black)
                                                        }
                                                }
                                            } else null)
                                }
                            if (query.isNotBlank()) {
                                val resultCount =
                                    searchResultCountLabel(filtered.size)
                                Text(
                                    resultCount,
                                    color = HausColors.current.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier =
                                        Modifier.testTag(
                                                SearchResultCountTestTag)
                                            .semantics {
                                                this[SearchRenderedText] =
                                                    resultCount
                                            })
                                if (filtered.isEmpty()) {
                                    val noMatch =
                                        stringResource(
                                            Res.string
                                                .search_no_tracks_match_format,
                                            query)
                                    Text(
                                        noMatch,
                                        color = HausColors.current.muted,
                                        fontSize = 15.sp,
                                        modifier =
                                            Modifier.padding(top = 24.dp)
                                                .testTag(SearchNoMatchTestTag)
                                                .semantics {
                                                    this[SearchRenderedText] =
                                                        noMatch
                                                })
                                } else
                                    LazyColumn(
                                        state = resultListState,
                                        verticalArrangement =
                                            Arrangement.spacedBy(4.dp)) {
                                            itemsIndexed(
                                                filtered,
                                                key = { occurrenceIndex, track
                                                    ->
                                                    searchOccurrenceKey(
                                                        occurrenceIndex,
                                                        track.id)
                                                }) { _, track ->
                                                    SearchResultRow(
                                                        track,
                                                        track.id ==
                                                            currentTrackId,
                                                        isPlaying,
                                                        labels.nowPlaying,
                                                        selectTrackLabel,
                                                        selectionModeActive,
                                                        track.id in
                                                            selectedTrackIds,
                                                        onPlay = {
                                                            onPlayTrack(
                                                                filtered, track)
                                                        },
                                                        onToggleSelection = {
                                                            onToggleSelection(
                                                                track.id)
                                                        },
                                                        onStartSelection = {
                                                            onStartSelection(
                                                                track.id)
                                                        },
                                                        playingIndicator)
                                                }
                                            item {
                                                Spacer(
                                                    Modifier.height(
                                                            bottomContentPadding)
                                                        .testTag(
                                                            SearchBottomSpacerTestTag))
                                            }
                                        }
                            }
                        }
                }
        }
}

internal fun filterSearchTracks(
    libraryTracks: List<LibraryTrack>,
    query: String
): List<LibraryTrack> =
    if (query.isBlank()) emptyList()
    else
        libraryTracks.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
                track.artist.orEmpty().contains(query, ignoreCase = true) ||
                track.album.orEmpty().contains(query, ignoreCase = true)
        }

private fun searchOccurrenceKey(index: Int, trackId: String): String =
    "$index\u0000$trackId"

@Composable
private fun searchResultCountLabel(count: Int): String =
    when (count) {
        0 -> stringResource(Res.string.search_results_count_zero)
        1 -> stringResource(Res.string.search_results_count_one)
        else -> stringResource(Res.string.search_results_count_many, count)
    }

@Composable
internal fun SearchResultRow(
    track: LibraryTrack,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    nowPlayingLabel: String,
    selectTrackLabel: @Composable (String) -> String,
    selectionModeActive: Boolean,
    isSelected: Boolean,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
    playingIndicator: @Composable () -> Unit
) {
    val selectionLabel = selectTrackLabel(track.title)
    fun click() {
        if (selectionModeActive) onToggleSelection() else onPlay()
    }
    Surface(
        modifier =
            Modifier.fillMaxWidth()
                .hausCombinedClickable(
                    onClick = ::click,
                    onLongClick = onStartSelection,
                    onLongClickLabel = selectionLabel)
                .semantics {
                    contentDescription = selectionLabel
                    if (selectionModeActive)
                        toggleableState =
                            if (isSelected) ToggleableState.On
                            else ToggleableState.Off
                    if (isNowPlaying) stateDescription = nowPlayingLabel
                },
        shape = RoundedCornerShape(12.dp),
        color =
            if (isNowPlaying) HausColors.current.panel
            else HausColors.current.paper) {
            Row(
                Modifier.padding(12.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                    if (selectionModeActive) {
                        Checkbox(
                            state =
                                if (isSelected) ToggleableState.On
                                else ToggleableState.Off,
                            onClick = onToggleSelection,
                            modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            color =
                                if (isNowPlaying && isPlaying)
                                    HausColors.current.pulse
                                else HausColors.current.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1)
                        Text(
                            listOfNotNull(track.artist, track.album)
                                .joinToString(" · "),
                            color = HausColors.current.muted,
                            fontSize = 12.sp,
                            maxLines = 1)
                    }
                    if (isNowPlaying && isPlaying) playingIndicator()
                }
        }
}
