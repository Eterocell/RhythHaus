package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import com.eterocell.rhythhaus.nowplaying.ui.MusicProgressScrubber
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.nowplaying.generated.resources.Res
import rhythhaus.feature.nowplaying.generated.resources.next_track
import rhythhaus.feature.nowplaying.generated.resources.playback_status_buffering
import rhythhaus.feature.nowplaying.generated.resources.playback_status_error
import rhythhaus.feature.nowplaying.generated.resources.playback_status_loading
import rhythhaus.feature.nowplaying.generated.resources.playback_status_paused
import rhythhaus.feature.nowplaying.generated.resources.playback_status_playing
import rhythhaus.feature.nowplaying.generated.resources.playback_status_ready
import rhythhaus.feature.nowplaying.generated.resources.playback_status_stopped
import rhythhaus.feature.nowplaying.generated.resources.previous_track
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_repeat_one
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_repeat_playlist
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_stop_after_current
import rhythhaus.feature.nowplaying.generated.resources.repeat_mode_stop_after_queue
import rhythhaus.feature.nowplaying.generated.resources.shuffle_off
import rhythhaus.feature.nowplaying.generated.resources.shuffle_on
import rhythhaus.feature.nowplaying.generated.resources.track_number_format
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

internal const val NowPlayingPreviousTestTag = "NowPlayingPrevious"
internal const val NowPlayingPlayPauseTestTag = "NowPlayingPlayPause"
internal const val NowPlayingNextTestTag = "NowPlayingNext"
internal const val NowPlayingShuffleTestTag = "NowPlayingShuffle"
internal const val NowPlayingRepeatTestTag = "NowPlayingRepeat"
internal const val NowPlayingContentRootTestTag = "NowPlayingContentRoot"
internal const val NowPlayingCompactLayoutTestTag = "NowPlayingCompactLayout"
internal const val NowPlayingSplitLayoutTestTag = "NowPlayingSplitLayout"
internal const val NowPlayingTrackNumberTestTag = "NowPlayingTrackNumber"
internal const val NowPlayingStatusTestTag = "NowPlayingStatus"
internal const val NowPlayingProgressTestTag = "NowPlayingProgress"
internal const val NowPlayingTitleTestTag = "NowPlayingTitle"
internal const val NowPlayingSubtitleTestTag = "NowPlayingSubtitle"

/** Immutable shared-resolved labels used by [NowPlayingContent]. */
public data class NowPlayingScreenLabels(
    /** Localized play action. */
    public val play: String,
    /** Localized pause action. */
    public val pause: String,
    /** Localized artwork description. */
    public val albumArtwork: String,
    /** Localized current track subtitle. */
    public val currentTrackArtistAlbum: String,
)

private data class NowPlayingUiState(
    val durationMillis: Long,
    val positionMillis: Long,
    val statusText: String,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatContentDescription: String,
    val shuffleContentDescription: String,
)

/**
 * Renders expanded Now Playing and sends the generic left-edge callback to
 * [onBack].
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
public fun NowPlayingContent(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val brush =
        Brush.linearGradient(
            listOf(Color(track.accent.start), Color(track.accent.end)))
    val durationMillis =
        playbackState.durationMillis ?: track.durationSeconds * 1_000L
    val uiState =
        NowPlayingUiState(
            durationMillis = durationMillis,
            positionMillis =
                playbackState.positionMillis.coerceIn(0L, durationMillis),
            statusText =
                playbackState.error?.message
                    ?: statusLabel(playbackState.status),
            isPlaying = playbackState.isPlaying,
            shuffleEnabled = playbackState.shuffleMode == ShuffleMode.On,
            repeatContentDescription = repeatLabel(playbackState.repeatMode),
            shuffleContentDescription =
                if (playbackState.shuffleMode == ShuffleMode.On)
                    stringResource(Res.string.shuffle_on)
                else stringResource(Res.string.shuffle_off),
        )
    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(NowPlayingContentRootTestTag)
                .leftEdgeSwipeBack(onBack),
        color = HausColors.current.paper) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                when (nowPlayingAdaptiveLayoutModeFor(
                    maxWidth.value, maxHeight.value)) {
                    NowPlayingAdaptiveLayoutMode.Compact ->
                        CompactNowPlayingLayout(
                            track,
                            playbackState,
                            playbackController,
                            labels,
                            artworkLoader,
                            uiState,
                            brush)
                    NowPlayingAdaptiveLayoutMode.Split ->
                        WideNowPlayingLayout(
                            track,
                            playbackState,
                            playbackController,
                            labels,
                            artworkLoader,
                            uiState,
                            brush)
                }
            }
        }
}

@Composable
private fun NowPlayingArtworkPane(
    track: Track,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        cornerRadius = 32.dp,
        colors = CardDefaults.defaultColors(color = HausColors.current.ink)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).background(brush),
                contentAlignment = Alignment.Center) {
                    val artwork =
                        rememberNowPlayingArtwork(
                            track.id, track.artworkBytes, artworkLoader)
                    ArtworkImage(
                        artwork,
                        labels.albumArtwork,
                        ArtworkImageRole.Hero,
                        Modifier.fillMaxSize(),
                        ContentScale.Crop) {
                            Text(
                                track.title.take(3).uppercase(),
                                color = Color.White.copy(alpha = 0.48f),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black)
                        }
                }
        }
}

@Composable
private fun NowPlayingControlsPane(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    uiState: NowPlayingUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                track.title,
                modifier = Modifier.testTag(NowPlayingTitleTestTag),
                color = HausColors.current.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                labels.currentTrackArtistAlbum,
                modifier = Modifier.testTag(NowPlayingSubtitleTestTag),
                color = HausColors.current.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            track.trackNumber?.let {
                Text(
                    stringResource(Res.string.track_number_format, it),
                    modifier = Modifier.testTag(NowPlayingTrackNumberTestTag),
                    color = HausColors.current.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.statusText,
            modifier = Modifier.testTag(NowPlayingStatusTestTag),
            color = HausColors.current.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1)
        Spacer(Modifier.height(12.dp))
        MusicProgressScrubber(
            uiState.positionMillis,
            uiState.durationMillis,
            playbackController::seekTo,
            Modifier.fillMaxWidth().testTag(NowPlayingProgressTestTag))
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
                PlaybackModeButton(
                    uiState.shuffleEnabled,
                    uiState.shuffleContentDescription,
                    playbackController::toggleShuffleMode,
                    Modifier.testTag(NowPlayingShuffleTestTag)) {
                        Icon(
                            Icons.Filled.Shuffle,
                            null,
                            tint =
                                if (uiState.shuffleEnabled) Color.White
                                else HausColors.current.ink,
                            modifier = Modifier.size(22.dp))
                    }
                Spacer(Modifier.width(12.dp))
                val repeatSelected =
                    playbackState.repeatMode == RepeatMode.RepeatPlaylist ||
                        playbackState.repeatMode == RepeatMode.RepeatOne
                PlaybackModeButton(
                    repeatSelected,
                    uiState.repeatContentDescription,
                    playbackController::cycleRepeatMode,
                    Modifier.testTag(NowPlayingRepeatTestTag)) {
                        val icon =
                            when (playbackState.repeatMode) {
                                RepeatMode.RepeatOne -> Icons.Filled.RepeatOne
                                RepeatMode.StopAfterCurrent ->
                                    Icons.Filled.Filter1
                                else -> Icons.Filled.Repeat
                            }
                        Icon(
                            icon,
                            null,
                            tint =
                                if (repeatSelected) Color.White
                                else HausColors.current.ink,
                            modifier = Modifier.size(22.dp))
                    }
            }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    48.dp,
                    RoundedCornerShape(14.dp),
                    HausColors.current.panel,
                    Icons.Filled.SkipPrevious,
                    stringResource(Res.string.previous_track),
                    playbackController::skipToPrevious,
                    testTag = NowPlayingPreviousTestTag)
                TransportButton(
                    64.dp,
                    CircleShape,
                    HausColors.current.pulse,
                    if (uiState.isPlaying) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    if (uiState.isPlaying) labels.pause else labels.play,
                    playbackController::togglePlayPause,
                    Color.White,
                    34.dp,
                    testTag = NowPlayingPlayPauseTestTag)
                TransportButton(
                    48.dp,
                    RoundedCornerShape(14.dp),
                    HausColors.current.panel,
                    Icons.Filled.SkipNext,
                    stringResource(Res.string.next_track),
                    playbackController::skipToNext,
                    testTag = NowPlayingNextTestTag)
            }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TransportButton(
    size: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = HausColors.current.ink,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
    testTag: String? = null,
) {
    Box(
        Modifier.size(size)
            .clip(shape)
            .background(color)
            .then(testTag?.let(Modifier::testTag) ?: Modifier)
            .hausClickable(onClick),
        contentAlignment = Alignment.Center) {
            Icon(
                icon,
                description,
                tint = tint,
                modifier = Modifier.size(iconSize))
        }
}

@Composable
private fun CompactNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    uiState: NowPlayingUiState,
    brush: Brush
) {
    Column(
        Modifier.testTag(NowPlayingCompactLayoutTestTag)
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            NowPlayingArtworkPane(
                track, labels, artworkLoader, brush, Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            NowPlayingControlsPane(
                track,
                playbackState,
                playbackController,
                labels,
                uiState,
                Modifier.fillMaxWidth())
        }
}

@Composable
private fun WideNowPlayingLayout(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    labels: NowPlayingScreenLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    uiState: NowPlayingUiState,
    brush: Brush
) {
    Row(
        Modifier.testTag(NowPlayingSplitLayoutTestTag)
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().weight(0.48f),
                contentAlignment = Alignment.Center) {
                    NowPlayingArtworkPane(
                        track,
                        labels,
                        artworkLoader,
                        brush,
                        Modifier.fillMaxWidth().aspectRatio(1f))
                }
            Box(
                Modifier.fillMaxHeight().weight(0.52f),
                contentAlignment = Alignment.Center) {
                    NowPlayingControlsPane(
                        track,
                        playbackState,
                        playbackController,
                        labels,
                        uiState,
                        Modifier.fillMaxWidth())
                }
        }
}

@Composable
private fun PlaybackModeButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) HausColors.current.pulse
                else HausColors.current.panel)
            .hausClickable(onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content)
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

@Composable
private fun repeatLabel(mode: RepeatMode): String =
    when (mode) {
        RepeatMode.StopAfterQueue ->
            stringResource(Res.string.repeat_mode_stop_after_queue)
        RepeatMode.RepeatPlaylist ->
            stringResource(Res.string.repeat_mode_repeat_playlist)
        RepeatMode.RepeatOne ->
            stringResource(Res.string.repeat_mode_repeat_one)
        RepeatMode.StopAfterCurrent ->
            stringResource(Res.string.repeat_mode_stop_after_current)
    }
