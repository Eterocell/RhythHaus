package com.eterocell.rhythhaus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

internal val NowPlayingBarContentPadding = 144.dp

enum class BottomBarMode {
    TrackLoaded,
    EmptyLibraryNavigation,
}

fun bottomBarModeFor(track: Track?): BottomBarMode = if (track == null) BottomBarMode.EmptyLibraryNavigation else BottomBarMode.TrackLoaded

@Composable
fun NowPlayingBar(
    track: Track?,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = bottomBarModeFor(track)
    val accent = track?.accent ?: TrackAccent(start = 0xFF111827, end = 0xFF776F66)
    val artworkBitmap = remember(track?.artworkBytes) {
        track?.artworkBytes?.decodeArtwork()
    }
    val progressFraction = if (track == null) 0f else playbackState.progressFraction
    val isPlaying = track != null && playbackState.isPlaying
    val title = track?.title ?: "RhythHaus"
    val subtitle = track?.let { "${it.artist} · ${it.album}" } ?: "Add music in Settings to start listening"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .hausClickable(onClick = { if (mode == BottomBarMode.TrackLoaded) onExpand() }),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        color = HausColors.current.panel,
    ) {
        Column {
            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(HausColors.current.line),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(HausColors.current.pulse),
                )
            }

            // Row 1: Artwork | Track info | Play/pause
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Artwork / fallback
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(accent.start), Color(accent.end)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = track?.title?.firstOrNull()?.uppercase() ?: "♪",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = HausColors.current.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        color = HausColors.current.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Play/pause button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HausColors.current.ink)
                        .hausClickable(onClick = { if (mode == BottomBarMode.TrackLoaded) onPlayPause() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (track == null) {
                            "♪"
                        } else if (isPlaying) {
                            "⏸"
                        } else {
                            "▶"
                        },
                        color = HausColors.current.paper,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Row 2: Search & Settings buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .hausClickable(onClick = onSearch),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "🔍", fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .hausClickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "⚙️", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
