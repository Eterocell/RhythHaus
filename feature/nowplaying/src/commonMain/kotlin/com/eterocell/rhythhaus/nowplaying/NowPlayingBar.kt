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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.RhythHausBackdrop
import com.eterocell.rhythhaus.ui.RhythHausGlassSurfaceAlpha
import com.eterocell.rhythhaus.ui.VerticalSheetGestureDirection
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
import com.eterocell.rhythhaus.ui.verticalSheetGesture
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.nowplaying.generated.resources.Res
import rhythhaus.feature.nowplaying.generated.resources.mini_player_empty_subtitle
import top.yukonga.miuix.kmp.basic.Text

internal val NowPlayingBarContentPadding = 144.dp
internal const val NowPlayingBarRootTestTag = "NowPlayingBarRoot"
internal const val NowPlayingBarPlayPauseTestTag = "NowPlayingBarPlayPause"
internal const val NowPlayingBarSearchTestTag = "NowPlayingBarSearch"
internal const val NowPlayingBarSettingsTestTag = "NowPlayingBarSettings"

/** Selects the mini-player behavior for a loaded track or an empty library. */
public enum class BottomBarMode {
    TrackLoaded,
    EmptyLibraryNavigation
}

/** Returns [BottomBarMode.EmptyLibraryNavigation] only when [track] is null. */
public fun bottomBarModeFor(track: Track?): BottomBarMode =
    if (track == null) BottomBarMode.EmptyLibraryNavigation
    else BottomBarMode.TrackLoaded

/** Immutable shared-resolved labels used by [NowPlayingBar]. */
public data class NowPlayingBarLabels(
    /** Localized play action. */
    public val play: String,
    /** Localized pause action. */
    public val pause: String,
    /** Localized search action. */
    public val search: String,
    /** Localized settings action. */
    public val settings: String,
    /** Localized album-art description. */
    public val albumArt: String,
    /** Localized current track subtitle. */
    public val currentTrackArtistAlbum: String,
)

/**
 * Renders the shell-composed mini-player and emits upward expansion only
 * through [onExpand].
 */
@Composable
public fun NowPlayingBar(
    track: Track?,
    playbackState: PlaybackState,
    labels: NowPlayingBarLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    interactive: Boolean = true,
    screenHeightPx: Float = 0f,
    backdrop: RhythHausBackdrop? = null,
    modifier: Modifier = Modifier,
): Unit {
    val mode = bottomBarModeFor(track)
    val accent =
        track?.accent ?: TrackAccent(start = 0xFF111827, end = 0xFF776F66)
    val barShape: Shape = RoundedCornerShape(20.dp)
    val barModifier =
        modifier
            .fillMaxWidth()
            .testTag(NowPlayingBarRootTestTag)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clip(barShape)
            .then(
                if (backdrop != null)
                    Modifier.rhythHausLiquidGlass(
                        backdrop,
                        barShape,
                        HausColors.current.panel.copy(
                            alpha = RhythHausGlassSurfaceAlpha))
                else Modifier.background(HausColors.current.panel))
            .then(
                if (interactive)
                    Modifier.hausClickable {
                        if (mode == BottomBarMode.TrackLoaded) onExpand()
                    }
                else Modifier)
            .then(
                if (interactive)
                    upwardOnlyExpansionGesture(
                        expandProgress,
                        !isExpanded && mode == BottomBarMode.TrackLoaded,
                        onExpand,
                        screenHeightPx)
                else Modifier)

    Box(barModifier) {
        Column {
            Box(
                Modifier.fillMaxWidth()
                    .height(3.dp)
                    .background(HausColors.current.line)) {
                    Box(
                        Modifier.fillMaxWidth(
                                if (track == null) 0f
                                else playbackState.progressFraction)
                            .fillMaxHeight()
                            .background(HausColors.current.pulse))
                }
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Artwork(track, labels.albumArt, artworkLoader, accent)
                    Column(Modifier.weight(1f)) {
                        Text(
                            track?.title ?: "RhythHaus",
                            color = HausColors.current.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            track?.let { labels.currentTrackArtistAlbum }
                                ?: stringResource(
                                    Res.string.mini_player_empty_subtitle),
                            color = HausColors.current.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.size(36.dp)
                            .testTag(NowPlayingBarPlayPauseTestTag)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HausColors.current.ink)
                            .then(
                                if (interactive)
                                    Modifier.hausClickable {
                                        if (mode == BottomBarMode.TrackLoaded)
                                            onPlayPause()
                                    }
                                else Modifier),
                        contentAlignment = Alignment.Center) {
                            val playing =
                                track != null && playbackState.isPlaying
                            Icon(
                                if (playing) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                                if (playing) labels.pause else labels.play,
                                tint = HausColors.current.paper,
                                modifier = Modifier.size(20.dp))
                        }
                }
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BarAction(
                            NowPlayingBarSearchTestTag,
                            Icons.Filled.Search,
                            labels.search,
                            interactive,
                            onSearch)
                        BarAction(
                            NowPlayingBarSettingsTestTag,
                            Icons.Filled.Settings,
                            labels.settings,
                            interactive,
                            onSettings)
                    }
                }
        }
    }
}

@Composable
private fun Artwork(
    track: Track?,
    contentDescription: String,
    artworkLoader: suspend (String) -> ByteArray?,
    accent: TrackAccent
) {
    Box(
        Modifier.size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(accent.start), Color(accent.end)))),
        contentAlignment = Alignment.Center) {
            val artwork = track?.let {
                rememberNowPlayingArtwork(it.id, it.artworkBytes, artworkLoader)
            }
            ArtworkImage(
                artwork,
                contentDescription,
                ArtworkImageRole.Thumbnail,
                Modifier.fillMaxSize(),
                ContentScale.Crop) {
                    Text(
                        track?.title?.firstOrNull()?.uppercase() ?: "♪",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp)
                }
        }
}

@Composable
private fun BarAction(
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    interactive: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(44.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (interactive) Modifier.hausClickable(onClick) else Modifier),
        contentAlignment = Alignment.Center) {
            Icon(
                icon,
                description,
                tint = HausColors.current.ink,
                modifier = Modifier.size(18.dp))
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
