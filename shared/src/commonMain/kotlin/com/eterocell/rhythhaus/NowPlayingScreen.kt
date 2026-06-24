package com.eterocell.rhythhaus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Slider
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
fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(track.accent.start), Color(track.accent.end)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite,
    )
    val durationMillis = playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val positionMillis = playbackState.positionMillis.coerceIn(0L, durationMillis)
    val progressFraction = if (durationMillis > 0) positionMillis.toFloat() / durationMillis else 0f
    val statusText = playbackState.error?.message ?: statusLabel(playbackState.status)
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val isPlaying = playbackState.isPlaying

    Surface(modifier = modifier.fillMaxSize(), color = HausPaper) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HausInk)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "← LIBRARY",
                        color = HausPaper,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Large artwork
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                colors = CardDefaults.defaultColors(color = HausInk),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(brush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap,
                            contentDescription = "Album artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = track.title.take(3).uppercase(),
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Track info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = track.title,
                    color = HausInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} · ${track.album}",
                    color = HausMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.trackNumber != null) {
                    Text(
                        text = "Track ${track.trackNumber}",
                        color = HausMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status
            Text(
                text = statusText,
                color = HausMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Spacer(Modifier.height(12.dp))

            // Seek bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        playbackController.seekTo((durationMillis * fraction).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMillis(positionMillis),
                        color = HausMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatMillis(durationMillis),
                        color = HausMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Stop
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausPanel)
                        .clickable(onClick = playbackController::stop),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "■", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                // Play/Pause (large, highlighted)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HausPulse)
                        .clickable { playbackController.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Next track
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausPanel)
                        .clickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
                            nextTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⏭", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun statusLabel(status: PlaybackStatus): String = when (status) {
    PlaybackStatus.Idle -> "Ready"
    PlaybackStatus.Loading -> "Loading"
    PlaybackStatus.Buffering -> "Buffering"
    PlaybackStatus.Playing -> "Playing"
    PlaybackStatus.Paused -> "Paused"
    PlaybackStatus.Stopped -> "Stopped"
    PlaybackStatus.Error -> "Needs a local file"
}
