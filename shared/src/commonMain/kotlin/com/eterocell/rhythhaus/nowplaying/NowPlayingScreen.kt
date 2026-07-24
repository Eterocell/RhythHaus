package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ui.NowPlayingAdaptiveLayoutMode
import com.eterocell.rhythhaus.library.ui.nowPlayingAdaptiveLayoutModeFor
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.LazyTrackArtworkImage
import com.eterocell.rhythhaus.ui.MusicProgressScrubber
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.next_track
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.playback_status_buffering
import rhythhaus.shared.generated.resources.playback_status_error
import rhythhaus.shared.generated.resources.playback_status_loading
import rhythhaus.shared.generated.resources.playback_status_paused
import rhythhaus.shared.generated.resources.playback_status_playing
import rhythhaus.shared.generated.resources.playback_status_ready
import rhythhaus.shared.generated.resources.playback_status_stopped
import rhythhaus.shared.generated.resources.previous_track
import rhythhaus.shared.generated.resources.repeat_mode_repeat_one
import rhythhaus.shared.generated.resources.repeat_mode_repeat_playlist
import rhythhaus.shared.generated.resources.repeat_mode_stop_after_current
import rhythhaus.shared.generated.resources.repeat_mode_stop_after_queue
import rhythhaus.shared.generated.resources.shuffle_off
import rhythhaus.shared.generated.resources.shuffle_on
import rhythhaus.shared.generated.resources.track_artist_album_format
import rhythhaus.shared.generated.resources.track_number_format
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

private data class NowPlayingUiState(
    val durationMillis: Long,
    val positionMillis: Long,
    val statusText: String,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatContentDescription: String,
    val shuffleContentDescription: String,
)

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
    val brush =
        Brush.linearGradient(
            colors = listOf(Color(track.accent.start), Color(track.accent.end)),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Infinite,
        )
    val durationMillis =
        playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val positionMillis =
        playbackState.positionMillis.coerceIn(0L, durationMillis)
    val statusText =
        playbackState.error?.message ?: statusLabel(playbackState.status)
    val isPlaying = playbackState.isPlaying
    val shuffleEnabled = playbackState.shuffleMode == ShuffleMode.On
    val repeatContentDescription =
        when (playbackState.repeatMode) {
            RepeatMode.StopAfterQueue ->
                stringResource(Res.string.repeat_mode_stop_after_queue)
            RepeatMode.RepeatPlaylist ->
                stringResource(Res.string.repeat_mode_repeat_playlist)
            RepeatMode.RepeatOne ->
                stringResource(Res.string.repeat_mode_repeat_one)
            RepeatMode.StopAfterCurrent ->
                stringResource(Res.string.repeat_mode_stop_after_current)
        }
    val shuffleContentDescription =
        if (shuffleEnabled) {
            stringResource(Res.string.shuffle_on)
        } else {
            stringResource(Res.string.shuffle_off)
        }
    val uiState =
        NowPlayingUiState(
            durationMillis = durationMillis,
            positionMillis = positionMillis,
            statusText = statusText,
            isPlaying = isPlaying,
            shuffleEnabled = shuffleEnabled,
            repeatContentDescription = repeatContentDescription,
            shuffleContentDescription = shuffleContentDescription,
        )

    Surface(
        modifier = modifier.fillMaxSize().leftEdgeSwipeBack(onBack),
        color = HausColors.current.paper,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            when (nowPlayingAdaptiveLayoutModeFor(
                widthDp = maxWidth.value, heightDp = maxHeight.value)) {
                NowPlayingAdaptiveLayoutMode.Compact ->
                    CompactNowPlayingLayout(
                        track = track,
                        playbackState = playbackState,
                        playbackController = playbackController,
                        uiState = uiState,
                        brush = brush,
                    )

                NowPlayingAdaptiveLayoutMode.Split ->
                    WideNowPlayingLayout(
                        track = track,
                        playbackState = playbackState,
                        playbackController = playbackController,
                        uiState = uiState,
                        brush = brush,
                    )
            }
        }
    }
}

@Composable
private fun NowPlayingArtworkPane(
    track: Track,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        cornerRadius = 32.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.ink),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth().aspectRatio(1f).background(brush),
            contentAlignment = Alignment.Center,
        ) {
            LazyTrackArtworkImage(
                trackId = track.id,
                eagerArtworkBytes = track.artworkBytes,
                contentDescription = stringResource(Res.string.album_artwork),
                role = ArtworkImageRole.Hero,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                Text(
                    text = track.title.take(3).uppercase(),
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingControlsPane(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
                text =
                    stringResource(
                        Res.string.track_artist_album_format,
                        track.artist,
                        track.album),
                color = HausColors.current.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.trackNumber != null) {
                Text(
                    text =
                        stringResource(
                            Res.string.track_number_format, track.trackNumber),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = uiState.statusText,
            color = HausColors.current.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )

        Spacer(Modifier.height(12.dp))

        MusicProgressScrubber(
            positionMillis = uiState.positionMillis,
            durationMillis = uiState.durationMillis,
            onSeek = playbackController::seekTo,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackModeButton(
                selected = uiState.shuffleEnabled,
                contentDescription = uiState.shuffleContentDescription,
                onClick = playbackController::toggleShuffleMode,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    tint =
                        if (uiState.shuffleEnabled) Color.White
                        else HausColors.current.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            PlaybackModeButton(
                selected =
                    playbackState.repeatMode == RepeatMode.RepeatPlaylist ||
                        playbackState.repeatMode == RepeatMode.RepeatOne,
                contentDescription = uiState.repeatContentDescription,
                onClick = playbackController::cycleRepeatMode,
            ) {
                val repeatIcon =
                    when (playbackState.repeatMode) {
                        RepeatMode.RepeatOne -> Icons.Filled.RepeatOne
                        RepeatMode.StopAfterCurrent -> Icons.Filled.Filter1
                        else -> Icons.Filled.Repeat
                    }
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = null,
                    tint =
                        if (playbackState.repeatMode ==
                            RepeatMode.RepeatPlaylist ||
                            playbackState.repeatMode == RepeatMode.RepeatOne)
                            Color.White
                        else HausColors.current.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausColors.current.panel)
                        .hausClickable { playbackController.skipToPrevious() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription =
                        stringResource(Res.string.previous_track),
                    tint = HausColors.current.ink,
                    modifier = Modifier.size(26.dp),
                )
            }

            Box(
                modifier =
                    Modifier.size(64.dp)
                        .clip(CircleShape)
                        .background(HausColors.current.pulse)
                        .hausClickable { playbackController.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        if (uiState.isPlaying) Icons.Filled.Pause
                        else Icons.Filled.PlayArrow,
                    contentDescription =
                        if (uiState.isPlaying) stringResource(Res.string.pause)
                        else stringResource(Res.string.play),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            Box(
                modifier =
                    Modifier.size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HausColors.current.panel)
                        .hausClickable { playbackController.skipToNext() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(Res.string.next_track),
                    tint = HausColors.current.ink,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CompactNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        NowPlayingArtworkPane(
            track = track,
            brush = brush,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        NowPlayingControlsPane(
            track = track,
            playbackState = playbackState,
            playbackController = playbackController,
            uiState = uiState,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WideNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    uiState: NowPlayingUiState,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().weight(0.48f),
            contentAlignment = Alignment.Center,
        ) {
            NowPlayingArtworkPane(
                track = track,
                brush = brush,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        Box(
            modifier = Modifier.fillMaxHeight().weight(0.52f),
            contentAlignment = Alignment.Center,
        ) {
            NowPlayingControlsPane(
                track = track,
                playbackState = playbackState,
                playbackController = playbackController,
                uiState = uiState,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlaybackModeButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier.size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) HausColors.current.pulse
                    else HausColors.current.panel)
                .hausClickable(onClick)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun statusLabel(status: PlaybackStatus): String =
    when (status) {
        PlaybackStatus.Idle -> stringResource(Res.string.playback_status_ready)
        PlaybackStatus.Loading ->
            stringResource(Res.string.playback_status_loading)
        PlaybackStatus.Buffering ->
            stringResource(Res.string.playback_status_buffering)
        PlaybackStatus.Playing ->
            stringResource(Res.string.playback_status_playing)
        PlaybackStatus.Paused ->
            stringResource(Res.string.playback_status_paused)
        PlaybackStatus.Stopped ->
            stringResource(Res.string.playback_status_stopped)
        PlaybackStatus.Error -> stringResource(Res.string.playback_status_error)
    }
