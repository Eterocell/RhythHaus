package com.eterocell.rhythhaus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.resolvePathForMetadata
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.taglib.TagMetadata as RawTagMetadata

@Composable
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
        color = HausPaper,
    ) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        .hausClickable(onClick = onBack)
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
                        .background(HausPanel)
                        .hausClickable {
                            val queue = playbackState.queue
                            val currentId = playbackState.currentTrack?.id
                            val currentIndex = queue.indexOfFirst { it.id == currentId }
                            val prevTrack = queue.getOrNull(currentIndex - 1) ?: queue.lastOrNull()
                            prevTrack?.let { playbackController.selectTrack(it.id, autoPlay = true) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⏮", color = HausInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                // Play/Pause (large, highlighted)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(HausPulse)
                        .hausClickable { playbackController.togglePlayPause() },
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
                        .hausClickable {
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

            // Developer: TagLib metadata for current track
            if (currentLibraryTrack != null) {
                Spacer(Modifier.height(18.dp))
                DeveloperTrackPanel(
                    track = currentLibraryTrack,
                    tagLibReader = tagLibReader,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeveloperTrackPanel(
    track: LibraryTrack,
    tagLibReader: TagLibReader,
) {
    var expanded by remember { mutableStateOf(false) }
    val devBgColor = HausInk.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(devBgColor)
            .hausClickable { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "DEV · TagLib",
                    color = HausInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = track.displayName,
                    color = HausMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (expanded) "▲" else "▼",
                color = HausPulse,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rawResult = remember(track.audioSource.stableKey) {
                    when (track.audioSource) {
                        is AudioSource.FilePath -> tagLibReader.readPath(resolvePathForMetadata(track.audioSource.path))
                        is AudioSource.Uri -> null
                    }
                }
                val properties = remember(track.audioSource.stableKey) {
                    when (track.audioSource) {
                        is AudioSource.FilePath -> tagLibReader.readProperties(resolvePathForMetadata(track.audioSource.path))
                        is AudioSource.Uri -> emptyMap()
                    }
                }

                when {
                    rawResult == null -> Text(
                        text = "URI source — TagLib requires a filesystem path",
                        color = HausPulse,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    rawResult is TagReadResult.Found -> {
                        rawTagLines(rawResult.metadata).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = label,
                                    color = HausMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = value,
                                    color = HausInk,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false),
                                )
                            }
                        }
                        if (properties.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "ALL PROPERTIES (${properties.size})",
                                color = HausInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                            )
                            properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = key,
                                        color = HausMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = value,
                                        color = HausInk,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rawTagLines(metadata: RawTagMetadata): List<Pair<String, String>> = listOf(
    "title" to (metadata.title ?: "—"),
    "artist" to (metadata.artist ?: "—"),
    "album" to (metadata.album ?: "—"),
    "albumArtist" to (metadata.albumArtist ?: "—"),
    "genre" to (metadata.genre ?: "—"),
    "comment" to (metadata.comment ?: "—"),
    "year" to (metadata.year?.toString() ?: "—"),
    "track" to trackDisplay(metadata.trackNumber, metadata.trackTotal),
    "disc" to trackDisplay(metadata.discNumber, metadata.discTotal),
    "duration" to (metadata.durationMillis?.let { formatMillis(it) } ?: "—"),
    "bitrate" to (metadata.bitrate?.let { "${it}kbps" } ?: "—"),
    "sampleRate" to (metadata.sampleRate?.let { "${it}Hz" } ?: "—"),
    "channels" to (metadata.channels?.toString() ?: "—"),
    "artwork" to artworkLabel(metadata.artwork),
)

private fun trackDisplay(number: Int?, total: Int?): String = when {
    number != null && total != null -> "$number/$total"
    number != null -> number.toString()
    else -> "—"
}

private fun artworkLabel(artwork: com.eterocell.rhythhaus.taglib.EmbeddedArtwork?): String {
    if (artwork == null) return "—"
    return "${artwork.mimeType ?: "unknown"} · ${artwork.bytes.size}B"
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
