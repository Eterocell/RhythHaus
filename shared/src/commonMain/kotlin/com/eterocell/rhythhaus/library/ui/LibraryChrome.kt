package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.RhythHausGlassSurfaceAlpha
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
import kotlin.math.max
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.back
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

internal fun LazyListState.toLibraryScrollPosition(): LibraryScrollPosition = LibraryScrollPosition(
    firstVisibleItemIndex = firstVisibleItemIndex,
    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
)

internal val NestedScrollChromeToolbarHeight = 56.dp
internal val DrillDownMiuixScrollContentTopPadding = 128.dp

@Composable
internal fun rememberSystemBarTopPadding(): Dp {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val systemBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    return max(statusBarHeight.value, systemBarHeight.value).dp
}

@Composable
private fun rememberSafeDrawingStartPadding(): Dp = WindowInsets.safeDrawing
    .asPaddingValues()
    .calculateStartPadding(LocalLayoutDirection.current)

@Composable
internal fun rememberMiuixTopAppBarScrollBehavior(): ScrollBehavior = MiuixScrollBehavior()

@Composable
internal fun DrillDownMiuixScrollChrome(
    scrollBehavior: ScrollBehavior,
    title: String,
    topBarArtworkBytes: ByteArray? = null,
    artworkCollapseSnapshot: ArtworkCollapseSnapshot? = null,
    onBack: () -> Unit,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val artworkSnapshot = artworkCollapseSnapshot
    val hasArtwork = artworkSnapshot != null
    val artworkBytes = if (hasArtwork) requireNotNull(topBarArtworkBytes) else null
    val artworkProgress = artworkSnapshot?.progress ?: 0f
    val collapsedFraction = if (hasArtwork) {
        artworkProgress
    } else {
        scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    }
    val collapsedTitleAlpha = (collapsedFraction * 3f).coerceIn(0f, 1f)
    val largeTitleAlpha = (1f - collapsedFraction * 2f).coerceIn(0f, 1f)
    val solidBackgroundAlpha by animateFloatAsState(
        targetValue = if (collapsedFraction <= 0f) 0f else collapsedFraction,
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(3f),
    ) {
        val collapsedChromeHeight = rememberSystemBarTopPadding() + NestedScrollChromeToolbarHeight
        val navigationStartPadding = rememberSafeDrawingStartPadding() + 12.dp
        val chromeHeight = artworkSnapshot
            ?.let { with(LocalDensity.current) { artworkChromeHeightPx(it).toDp() } }
            ?: collapsedChromeHeight

        Box(
            modifier = if (hasArtwork) {
                Modifier
                    .fillMaxWidth()
                    .height(chromeHeight)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(chromeHeight)
                    .rhythHausLiquidGlass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(0.dp),
                        fallbackColor = HausColors.current.panel.copy(alpha = RhythHausGlassSurfaceAlpha),
                    )
            },
        ) {
            if (hasArtwork) {
                ArtworkImage(
                    artworkBytes = artworkBytes,
                    contentDescription = stringResource(Res.string.album_artwork),
                    role = ArtworkImageRole.Hero,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    fallback = {},
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.34f),
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.48f),
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(HausColors.current.paper.copy(alpha = 0.92f * solidBackgroundAlpha)),
                )
            }
        TopAppBar(
            title = if (hasArtwork) "" else title,
            largeTitle = if (hasArtwork) "" else title,
            subtitle = "",
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            titleColor = HausColors.current.ink.copy(alpha = 0.90f),
            largeTitleColor = HausColors.current.ink,
            defaultWindowInsetsPadding = false,
            titlePadding = 20.dp,
            navigationIconPadding = navigationStartPadding,
            actionIconPadding = 0.dp,
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    backgroundColor = if (hasArtwork) {
                        HausColors.current.paper.copy(alpha = 0.78f * solidBackgroundAlpha)
                    } else {
                        Color.Transparent
                    },
                    minWidth = 44.dp,
                    minHeight = 44.dp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = HausColors.current.ink,
                    )
                }
            },
            bottomContent = {
                if (!hasArtwork) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HausColors.current.line.copy(alpha = 0.42f * scrollBehavior.state.collapsedFraction)),
                    )
                }
            },
        )
            if (hasArtwork) {
                TitleChip(
                    title = title,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = rememberSystemBarTopPadding() + 10.dp)
                        .alpha(collapsedTitleAlpha),
                )
                TitleChip(
                    title = title,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                        .alpha(largeTitleAlpha),
                )
            }
        }
    }
}

@Composable
private fun TitleChip(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        fontWeight = FontWeight.Black,
        color = HausColors.current.ink,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(HausColors.current.paper.copy(alpha = 0.82f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
internal fun DrillDownScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollFraction by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            val maxFirstVisibleIndex = (total - visible).coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex).coerceIn(0f, 1f)
        }
    }

    fun scrollTo(yPosition: Float, trackHeightPx: Float) {
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        if (total <= visible || trackHeightPx <= 0f) return

        val maxFirstVisibleIndex = (total - visible).coerceAtLeast(0)
        val targetFraction = (yPosition / trackHeightPx).coerceIn(0f, 1f)
        val targetIndex = (targetFraction * maxFirstVisibleIndex).toInt().coerceIn(0, maxFirstVisibleIndex)
        coroutineScope.launch {
            // Must be an immediate (non-animated) scroll: animateScrollToItem takes ~300ms
            // and gets cancelled/restarted on every drag-move event, so the list perpetually
            // chases a stale animation and only catches up once the drag ends.
            listState.scrollToItem(targetIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(24.dp)
            .pointerInput(listState) {
                detectTapGestures { offset ->
                    scrollTo(offset.y, size.height.toFloat())
                }
            }
            .pointerInput(listState) {
                detectVerticalDragGestures { change, _ ->
                    scrollTo(change.position.y, size.height.toFloat())
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbHeight = maxHeight * 0.15f
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .width(6.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(HausColors.current.muted.copy(alpha = 0.42f)),
        )
    }
}
