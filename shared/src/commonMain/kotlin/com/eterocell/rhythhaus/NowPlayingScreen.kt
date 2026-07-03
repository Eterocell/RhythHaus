package com.eterocell.rhythhaus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.taglib.TagLibReader
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
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
    val statusText = playbackState.error?.message ?: statusLabel(playbackState.status)
    val artworkBitmap = remember(track.artworkBytes) {
        track.artworkBytes?.decodeArtwork()
    }
    val isPlaying = playbackState.isPlaying

    Surface(
        modifier = modifier
            .fillMaxSize()
            .leftEdgeSwipeBack(onBack),
        color = HausColors.current.paper,
    ) {
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
                BackChip(onClick = onBack)
            }

            Spacer(Modifier.height(18.dp))

            // Large artwork
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 32.dp,
                colors = CardDefaults.defaultColors(color = HausColors.current.ink),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
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
                    color = HausColors.current.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} · ${track.album}",
                    color = HausColors.current.muted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.trackNumber != null) {
                    Text(
                        text = "Track ${track.trackNumber}",
                        color = HausColors.current.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status
            Text(
                text = statusText,
                color = HausColors.current.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Spacer(Modifier.height(12.dp))

            // Seek bar
            MusicProgressScrubber(
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                onSeek = playbackController::seekTo,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Previous track
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausColors.current.panel)
                        .hausClickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
                            prevTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = HausColors.current.ink,
                        modifier = Modifier.size(26.dp),
                    )
                }

                // Play/Pause (large, highlighted)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HausColors.current.pulse)
                        .hausClickable { playbackController.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Next track
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausColors.current.panel)
                        .hausClickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val nextTrack = queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
                            nextTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = HausColors.current.ink,
                        modifier = Modifier.size(26.dp),
                    )
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
