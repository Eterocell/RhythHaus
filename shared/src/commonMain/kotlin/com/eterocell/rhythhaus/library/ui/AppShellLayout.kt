package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausMetrics
import com.eterocell.rhythhaus.ui.HausIconButton as IconButton
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.settings

internal const val AppSettingsTopBarTestTag = "AppSettingsTopBar"
internal const val AppShellHomePaneTestTag = "AppShellHomePane"
internal const val AppShellDetailPaneTestTag = "AppShellDetailPane"
internal const val AppShellContentRegionTestTag = "AppShellContentRegion"

internal enum class AppNavigationLayout(
    val navigationWidthDp: Float,
    val pageMarginDp: Float,
) {
    Bottom(navigationWidthDp = 0f, pageMarginDp = 16f),
    Rail(navigationWidthDp = 80f, pageMarginDp = 24f),
    Sidebar(navigationWidthDp = 208f, pageMarginDp = 32f),
}

internal fun appNavigationLayoutFor(widthDp: Float): AppNavigationLayout =
    when {
        widthDp < 720f -> AppNavigationLayout.Bottom
        widthDp < 1_000f -> AppNavigationLayout.Rail
        else -> AppNavigationLayout.Sidebar
    }

private data class AppNavigationMeasurement(
    val layout: AppNavigationLayout,
    val widthPx: Int,
    val heightPx: Int,
)

/**
 * Places the single route body, primary navigation, and measured bottom dock.
 * Route-specific state stays with its caller; this layout owns chrome and
 * reserves its measured bottom clearance outside the route viewport.
 */
@Composable
internal fun AppShellLayout(
    windowWidthDp: Float,
    navigationVisible: Boolean,
    selectedDestination: LibraryPrimaryDestination?,
    activeBrowseMode: BrowseMode?,
    onNavigate: (LibraryPrimaryDestination) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onSettings: () -> Unit,
    showSettingsAction: Boolean,
    contentHandlesSafeBottomInset: Boolean = false,
    bottomBarContent: LibraryBottomBarContent,
    content: @Composable () -> Unit,
    bottomBar:
        @Composable
        (
            presentation: LibraryBottomBarPresentation,
            minimumHeight: Dp,
        ) -> Unit,
) {
    val layout = appNavigationLayoutFor(windowWidthDp)
    val navigationWidthDp =
        if (navigationVisible) layout.navigationWidthDp else 0f
    val pageMarginDp = if (navigationVisible) layout.pageMarginDp else 0f
    val mainRegionWidthDp =
        (windowWidthDp - navigationWidthDp).coerceAtLeast(0f)
    val barSlotWidthDp =
        minOf(
            mainRegionWidthDp,
            AppShellContentMaxWidthDp + pageMarginDp * 2f,
        )
    val density = LocalDensity.current
    val windowWidthPx = with(density) { windowWidthDp.dp.roundToPx() }
    val barSlotWidthPx = with(density) { barSlotWidthDp.dp.roundToPx() }
    var navigationMeasurement by
        remember(layout, windowWidthPx) {
            mutableStateOf<AppNavigationMeasurement?>(null)
        }
    var bottomBarMeasurement by
        remember(bottomBarContent.kind, layout, barSlotWidthPx) {
            mutableStateOf<LibraryBottomBarMeasurement?>(null)
        }
    val safeBottomInsetPx =
        maxOf(
            WindowInsets.navigationBars.getBottom(density),
            WindowInsets.ime.getBottom(density),
        )
    val safeBottomInset = with(density) { safeBottomInsetPx.toDp() }
    val safeTopInset =
        with(density) { WindowInsets.safeDrawing.getTop(density).toDp() }
    val measuredNavigationHeightPx =
        navigationMeasurement
            ?.takeIf {
                navigationVisible &&
                    layout == AppNavigationLayout.Bottom &&
                    it.layout == layout &&
                    it.widthPx == windowWidthPx
            }
            ?.heightPx
            ?.coerceAtLeast(0) ?: 0
    val hiddenFraction by
        animateFloatAsState(
            targetValue =
                if (bottomBarContent == LibraryBottomBarContent.Hidden) 1f
                else 0f,
            animationSpec = tween(200),
            label = "BottomBarOffset",
        )
    val bottomBarPresentation =
        libraryBottomBarPresentation(
            content = bottomBarContent,
            measurement = bottomBarMeasurement,
            expectedLayout = layout,
            expectedWidthPx = barSlotWidthPx,
            hiddenFraction = hiddenFraction,
            allowInteraction = navigationVisible,
        )
    val contentSafeBottomInsetPx =
        if (contentHandlesSafeBottomInset) 0 else safeBottomInsetPx
    val contentBottomInset =
        with(density) {
            (bottomBarPresentation.clearancePx +
                    measuredNavigationHeightPx +
                    contentSafeBottomInsetPx)
                .toDp()
        }
    val nowPlayingBarOffsetPx =
        nowPlayingBarOffsetPx(
            hiddenFraction = hiddenFraction,
            measuredHeightPx = bottomBarPresentation.clearancePx,
        )
    val bottomNavigationHeightPx =
        if (layout == AppNavigationLayout.Bottom && navigationVisible)
            measuredNavigationHeightPx
        else 0
    val minimumNowPlayingBarHeight =
        if (layout == AppNavigationLayout.Bottom)
            HausMetrics.compactMiniPlayerMinHeight
        else HausMetrics.desktopMiniPlayerMinHeight
    val frameWidth = barSlotWidthDp.dp
    val pageMargin = pageMarginDp.dp

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (layout != AppNavigationLayout.Bottom && navigationVisible) {
                AppNavigationChrome(
                    layout = layout,
                    selectedDestination = selectedDestination,
                    activeBrowseMode = activeBrowseMode,
                    onNavigate = onNavigate,
                    onBrowseModeChange = onBrowseModeChange,
                    onSettings = onSettings,
                    modifier =
                        Modifier.fillMaxHeight().onSizeChanged {
                            navigationMeasurement =
                                AppNavigationMeasurement(
                                    layout = layout,
                                    widthPx = it.width,
                                    heightPx = it.height,
                                )
                        },
                )
            }
            CompositionLocalProvider(
                LocalHausDesktopDensity provides
                    (layout != AppNavigationLayout.Bottom),
            ) {
                AppShellMainRegion(
                    modifier =
                        if (layout == AppNavigationLayout.Bottom)
                            Modifier.fillMaxSize()
                        else Modifier.weight(1f).fillMaxHeight(),
                    frameWidth = frameWidth,
                    pageMargin = pageMargin,
                    content = content,
                    contentBottomInset = contentBottomInset,
                    bottomBarContent = bottomBarContent,
                    showSettingsAction =
                        layout == AppNavigationLayout.Bottom &&
                            navigationVisible &&
                            showSettingsAction,
                    safeTopInset = safeTopInset,
                    onSettings = onSettings,
                    bottomBarPresentation = bottomBarPresentation,
                    bottomNavigationHeightPx = bottomNavigationHeightPx,
                    safeBottomInsetPx = safeBottomInsetPx,
                    barOffsetPx = nowPlayingBarOffsetPx,
                    minimumNowPlayingBarHeight = minimumNowPlayingBarHeight,
                    onBottomBarMeasured = { size ->
                        bottomBarMeasurement =
                            LibraryBottomBarMeasurement(
                                content = bottomBarContent,
                                heightPx = size.height,
                                layout = layout,
                                widthPx = size.width,
                            )
                    },
                    bottomBar = bottomBar,
                )
            }
        }
        if (layout == AppNavigationLayout.Bottom && navigationVisible) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = safeBottomInset)) {
                    AppNavigationChrome(
                        layout = layout,
                        selectedDestination = selectedDestination,
                        activeBrowseMode = activeBrowseMode,
                        onNavigate = onNavigate,
                        onBrowseModeChange = onBrowseModeChange,
                        onSettings = onSettings,
                        modifier =
                            Modifier.fillMaxWidth().onSizeChanged {
                                navigationMeasurement =
                                    AppNavigationMeasurement(
                                        layout = layout,
                                        widthPx = it.width,
                                        heightPx = it.height,
                                    )
                            },
                    )
                }
        }
    }
}

@Composable
private fun AppShellMainRegion(
    modifier: Modifier,
    frameWidth: Dp,
    pageMargin: Dp,
    content: @Composable () -> Unit,
    contentBottomInset: Dp,
    bottomBarContent: LibraryBottomBarContent,
    showSettingsAction: Boolean,
    safeTopInset: Dp,
    onSettings: () -> Unit,
    bottomBarPresentation: LibraryBottomBarPresentation,
    bottomNavigationHeightPx: Int,
    safeBottomInsetPx: Int,
    barOffsetPx: Int,
    minimumNowPlayingBarHeight: Dp,
    onBottomBarMeasured: (IntSize) -> Unit,
    bottomBar:
        @Composable
        (
            presentation: LibraryBottomBarPresentation,
            minimumHeight: Dp,
        ) -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.fillMaxHeight()
                .widthIn(max = frameWidth)
                .fillMaxWidth()
                .padding(horizontal = pageMargin)) {
                Box(
                    Modifier.fillMaxSize()
                        .padding(bottom = contentBottomInset)) {
                        Box(
                            Modifier.fillMaxSize()
                                .testTag(AppShellContentRegionTestTag)) {
                                content()
                            }
                    }
                if (showSettingsAction) {
                    IconButton(
                        onClick = onSettings,
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .padding(top = safeTopInset)
                                .size(48.dp)
                                .testTag(AppSettingsTopBarTestTag),
                        backgroundColor = Color.Transparent,
                        minWidth = 48.dp,
                        minHeight = 48.dp,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription =
                                stringResource(Res.string.settings),
                            tint = HausColors.current.ink,
                        )
                    }
                }
            }
        if (bottomBarContent != LibraryBottomBarContent.Hidden) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .widthIn(max = frameWidth)
                    .fillMaxWidth()
                    .onSizeChanged(onBottomBarMeasured)
                    .offset {
                        IntOffset(
                            x = 0,
                            y =
                                -bottomNavigationHeightPx - safeBottomInsetPx +
                                    barOffsetPx,
                        )
                    }
                    .alpha(bottomBarPresentation.alpha)
                    .then(
                        if (bottomBarContent ==
                            LibraryBottomBarContent.NowPlaying)
                            Modifier.testTag(NowPlayingShellPlacementTestTag)
                        else Modifier,
                    )) {
                    bottomBar(bottomBarPresentation, minimumNowPlayingBarHeight)
                }
        }
    }
}
