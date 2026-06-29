package com.eterocell.rhythhaus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
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

@Composable
fun NowPlayingBar(
    track: Track,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(track.accent.start), Color(track.accent.end)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite,
    )
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val progressFraction = playbackState.progressFraction
    val isPlaying = playbackState.isPlaying

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .safeContentPadding()
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        color = HausPanel,
    ) {
        Column {
            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(HausLine),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(HausPulse),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                listOf(Color(track.accent.start), Color(track.accent.end)),
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
                            text = track.title.firstOrNull()?.uppercase() ?: "♪",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = HausInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist} · ${track.album}",
                        color = HausMuted,
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
                        .background(HausInk)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        color = HausPaper,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Search button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSearch),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🔍", fontSize = 16.sp)
                }
                // Settings button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⚙️", fontSize = 16.sp)
                }
            }
        }
    }
}
