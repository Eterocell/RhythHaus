package com.eterocell.rhythhaus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.taglib.TagLibReader

@Composable
fun SearchScreen(
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(query, libraryTracks) {
        if (query.isBlank()) emptyList()
        else libraryTracks.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
            track.artist.contains(query, ignoreCase = true) ||
            track.album.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HausInk)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "< Back",
                            color = HausPaper,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Search", color = HausInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

                // Search field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, HausMuted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .background(HausPaper)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "Track, artist, or album...",
                            color = HausMuted,
                            fontSize = 15.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = TextStyle(color = HausInk, fontSize = 15.sp),
                        cursorBrush = SolidColor(HausPulse),
                    )
                }

                // Results
                if (query.isNotBlank()) {
                    Text(
                        text = "${filtered.size} result${if (filtered.size != 1) "s" else ""}",
                        color = HausMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No tracks match \"$query\"",
                            color = HausMuted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        LazyColumn(
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
private fun SearchResultRow(
    track: LibraryTrack,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) HausPanel else HausPaper,
    ) {
        Row(
            modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isNowPlaying && isPlaying) HausPulse else HausInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(track.artist, track.album).joinToString(" · "),
                    color = HausMuted,
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
