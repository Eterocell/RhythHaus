package com.eterocell.rhythhaus.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.VerticalSheetGestureDirection
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.verticalSheetGesture
import top.yukonga.miuix.kmp.basic.Text

internal const val NowPlayingBarRootTestTag = "NowPlayingBarRoot"
internal const val NowPlayingBarPlayPauseTestTag = "NowPlayingBarPlayPause"
internal const val NowPlayingBarNextTestTag = "NowPlayingBarNext"
internal const val NowPlayingBarQueueTestTag = "NowPlayingBarQueue"

/** Immutable shared-resolved labels used by [NowPlayingBar]. */
public data class NowPlayingBarLabels(
    /** Localized play action. */
    public val play: String,
    /** Localized pause action. */
    public val pause: String,
    /** Localized album-art description. */
    public val albumArt: String,
    /** Localized current track subtitle. */
    public val currentTrackArtistAlbum: String,
    /** Localized skip-to-next action. */
    public val nextTrack: String,
    /** Localized queue action. */
    public val queue: String,
)

/** Renders a playback-only mini-player for the current track. */
@Composable
public fun NowPlayingBar(
    track: Track,
    playbackState: PlaybackState,
    labels: NowPlayingBarLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onExpand: () -> Unit,
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    interactive: Boolean = true,
    screenHeightPx: Float = 0f,
    modifier: Modifier = Modifier,
): Unit {
    val barShape: Shape = RoundedCornerShape(20.dp)
    val barModifier =
        modifier
            .fillMaxWidth()
            .testTag(NowPlayingBarRootTestTag)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clip(barShape)
            .background(HausColors.current.panel)
            .then(
                if (interactive) Modifier.hausClickable(onExpand) else Modifier)
            .then(
                if (interactive)
                    upwardOnlyExpansionGesture(
                        expandProgress, !isExpanded, onExpand, screenHeightPx)
                else Modifier)

    Box(barModifier) {
        Column {
            Box(
                Modifier.fillMaxWidth()
                    .height(3.dp)
                    .background(HausColors.current.line)) {
                    Box(
                        Modifier.fillMaxWidth(playbackState.progressFraction)
                            .fillMaxHeight()
                            .background(HausColors.current.pulse))
                }
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Artwork(track, labels.albumArt, artworkLoader)
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            color = HausColors.current.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            labels.currentTrackArtistAlbum,
                            color = HausColors.current.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    MiniPlayerActionButton(
                        label = labels.nextTrack,
                        icon = Icons.Filled.SkipNext,
                        testTag = NowPlayingBarNextTestTag,
                        interactive = interactive,
                        onClick = onSkipNext,
                    )
                    MiniPlayerActionButton(
                        label = labels.queue,
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        testTag = NowPlayingBarQueueTestTag,
                        interactive = interactive,
                        onClick = onOpenQueue,
                    )
                    Box(
                        Modifier.size(48.dp)
                            .testTag(NowPlayingBarPlayPauseTestTag)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HausColors.current.ink)
                            .then(
                                if (interactive)
                                    Modifier.hausClickable(
                                        onClick = onPlayPause,
                                        focusColor = HausColors.current.paper,
                                        outerColor = HausColors.current.outline,
                                    )
                                else Modifier),
                        contentAlignment = Alignment.Center) {
                            val playing = playbackState.isPlaying
                            Icon(
                                if (playing) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                                if (playing) labels.pause else labels.play,
                                tint = HausColors.current.paper,
                                modifier = Modifier.size(20.dp))
                        }
                }
        }
    }
}

@Composable
private fun MiniPlayerActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    interactive: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (interactive) Modifier.hausClickable(onClick) else Modifier)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HausColors.current.ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun Artwork(
    track: Track,
    contentDescription: String,
    artworkLoader: suspend (String) -> ByteArray?,
) {
    Box(
        Modifier.size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(track.accent.start), Color(track.accent.end)))),
        contentAlignment = Alignment.Center) {
            val artwork =
                rememberNowPlayingArtwork(
                    track.id, track.artworkBytes, artworkLoader)
            ArtworkImage(
                artwork,
                contentDescription,
                ArtworkImageRole.Thumbnail,
                Modifier.fillMaxSize(),
                ContentScale.Crop) {
                    Text(
                        track.title.firstOrNull()?.uppercase() ?: "♪",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp)
                }
        }
}

@Composable
private fun upwardOnlyExpansionGesture(
    progress: Animatable<Float, AnimationVector1D>,
    active: Boolean,
    onExpand: () -> Unit,
    screenHeightPx: Float
): Modifier =
    Modifier.verticalSheetGesture(
        progress,
        active,
        rememberCoroutineScope(),
        VerticalSheetGestureDirection.Upward,
        onExpand,
        0.3f,
        screenHeightPx)
