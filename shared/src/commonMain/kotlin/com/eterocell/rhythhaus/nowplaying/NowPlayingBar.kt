package com.eterocell.rhythhaus.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.LazyTrackArtworkImage
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
import com.eterocell.rhythhaus.ui.verticalSheetGesture
import com.eterocell.rhythhaus.ui.RhythHausGlassSurfaceAlpha
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_art
import rhythhaus.shared.generated.resources.mini_player_empty_subtitle
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.track_artist_album_format
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop

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
    expandProgress: Animatable<Float, AnimationVector1D>,
    isExpanded: Boolean,
    screenHeightPx: Float = 0f,
    backdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier,
) {
    val mode = bottomBarModeFor(track)
    val accent = track?.accent ?: TrackAccent(start = 0xFF111827, end = 0xFF776F66)
    val progressFraction = if (track == null) 0f else playbackState.progressFraction
    val isPlaying = track != null && playbackState.isPlaying
    val title = track?.title ?: "RhythHaus"
    val subtitle = track?.let { stringResource(Res.string.track_artist_album_format, it.artist, it.album) }
        ?: stringResource(Res.string.mini_player_empty_subtitle)

    val barShape: Shape = RoundedCornerShape(20.dp)
    val barModifier = modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        .clip(barShape)
        .then(
            if (backdrop != null) {
                Modifier.rhythHausLiquidGlass(
                    backdrop = backdrop,
                    shape = barShape,
                    fallbackColor = HausColors.current.panel.copy(alpha = RhythHausGlassSurfaceAlpha),
                )
            } else {
                Modifier.background(HausColors.current.panel)
            },
        )
        .hausClickable(onClick = { if (mode == BottomBarMode.TrackLoaded) onExpand() })
        .verticalSheetGesture(
            expandProgress = expandProgress,
            isActive = !isExpanded && mode == BottomBarMode.TrackLoaded,
            scope = rememberCoroutineScope(),
            onSwipeExpand = onExpand,
            onSwipeCollapse = {},
            threshold = 0.3f,
            referenceHeight = screenHeightPx,
        )

    Box(modifier = barModifier) {
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
                    LazyTrackArtworkImage(
                        trackId = track?.id,
                        eagerArtworkBytes = track?.artworkBytes,
                        contentDescription = stringResource(Res.string.album_art),
                        role = ArtworkImageRole.Thumbnail,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    ) {
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
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                        tint = HausColors.current.paper,
                        modifier = Modifier.size(20.dp),
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .hausClickable(onClick = onSearch),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(Res.string.search),
                            tint = HausColors.current.ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .hausClickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(Res.string.settings),
                            tint = HausColors.current.ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
