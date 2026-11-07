package com.eterocell.rhythhaus.library.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausMetrics
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.HausIconButton as IconButton
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayVisible
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.settings

internal const val AppSettingsTopBarTestTag = "AppSettingsTopBar"
internal const val AppShellHomePaneTestTag = "AppShellHomePane"
internal const val AppShellDetailPaneTestTag = "AppShellDetailPane"
internal const val AppShellContentRegionTestTag = "AppShellContentRegion"
internal const val AppShellSafeFrameTestTag = "AppShellSafeFrame"

internal data class AppShellSafeBounds(
    val widthDp: Float,
    val heightDp: Float,
    val leftInset: Dp,
    val topInset: Dp,
    val rightInset: Dp,
    val bottomInset: Dp,
    val imeBottomInset: Dp,
)

internal fun appShellSafeBoundsFor(
    windowWidthDp: Float,
    windowHeightDp: Float,
    safeLeftInsetDp: Float,
    safeTopInsetDp: Float,
    safeRightInsetDp: Float,
    safeBottomInsetDp: Float,
    imeBottomInsetDp: Float,
): AppShellSafeBounds {
    val leftInset = safeLeftInsetDp.coerceAtLeast(0f)
    val topInset = safeTopInsetDp.coerceAtLeast(0f)
    val rightInset = safeRightInsetDp.coerceAtLeast(0f)
    val bottomInset = safeBottomInsetDp.coerceAtLeast(0f)
    val imeBottomInset = imeBottomInsetDp.coerceAtLeast(0f)
    return AppShellSafeBounds(
        widthDp = (windowWidthDp - leftInset - rightInset).coerceAtLeast(0f),
        heightDp = (windowHeightDp - topInset - bottomInset).coerceAtLeast(0f),
        leftInset = leftInset.dp,
        topInset = topInset.dp,
        rightInset = rightInset.dp,
        bottomInset = bottomInset.dp,
        imeBottomInset = imeBottomInset.dp,
    )
}

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

/** Vertical breathing room above and below the single-line route title. */
private val AppShellHeaderTitlePadding = 4.dp

/**
 * Canonical route-title style; the header renders it and its height reserves
 * it.
 */
internal fun appShellHeaderTitleStyle(layout: AppNavigationLayout): TextStyle =
    if (layout == AppNavigationLayout.Bottom) HausTypography.compactTitle
    else HausTypography.pageTitle

/**
 * Route header height for [layout], including text-scale growth of the
 * canonical title. The header and the content region reserved beneath it both
 * read this one value, so scaled text can never desynchronise chrome from the
 * route body.
 */
@Composable
internal fun appShellHeaderHeight(layout: AppNavigationLayout): Dp {
    val density = LocalDensity.current
    val minimumHeight =
        if (layout == AppNavigationLayout.Bottom) 56.dp else 64.dp
    val titleHeight =
        with(density) { appShellHeaderTitleStyle(layout).lineHeight.toDp() } +
            AppShellHeaderTitlePadding * 2
    return maxOf(minimumHeight, titleHeight)
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
    safeBounds: AppShellSafeBounds,
    navigationVisible: Boolean,
    selectedDestination: LibraryPrimaryDestination?,
    activeBrowseMode: BrowseMode?,
    onNavigate: (LibraryPrimaryDestination) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onSettings: () -> Unit,
    showSettingsAction: Boolean,
    routeHeader: AppShellRouteHeader? = null,
    bottomBarContent: LibraryBottomBarContent,
    content: @Composable () -> Unit,
    bottomBar:
        @Composable
        (
            presentation: LibraryBottomBarPresentation,
            minimumHeight: Dp,
        ) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeWindowWidthDp = safeBounds.widthDp
    val layout = appNavigationLayoutFor(safeWindowWidthDp)
    val compactSettingsAction =
        if (showSettingsAction && layout == AppNavigationLayout.Bottom) {
            AppShellHeaderAction(
                label = stringResource(Res.string.settings),
                icon = Icons.Filled.Settings,
                enabled = true,
                onClick = onSettings,
                testTag = AppSettingsTopBarTestTag,
            )
        } else null
    val effectiveRouteHeader = routeHeader?.let { header ->
        if (compactSettingsAction == null) header
        else
            header.copy(
                actions = listOf(compactSettingsAction) + header.actions)
    }
    val overlayOwnsIme = LocalHausWindowOverlayVisible.current
    val routeEditorOwnsIme = !overlayOwnsIme && safeBounds.imeBottomInset > 0.dp
    val navigationChromeVisible =
        navigationVisible &&
            !(routeEditorOwnsIme && layout == AppNavigationLayout.Bottom)
    val activeBottomBarContent =
        if (routeEditorOwnsIme) LibraryBottomBarContent.Hidden
        else bottomBarContent
    val safeStartInset =
        if (layoutDirection == LayoutDirection.Ltr) safeBounds.leftInset
        else safeBounds.rightInset
    val safeEndInset =
        if (layoutDirection == LayoutDirection.Ltr) safeBounds.rightInset
        else safeBounds.leftInset
    val navigationWidthDp =
        if (navigationVisible) layout.navigationWidthDp else 0f
    val pageMarginDp =
        when {
            routeHeader != null ->
                when (layout) {
                    AppNavigationLayout.Bottom -> 16f
                    AppNavigationLayout.Rail -> 24f
                    AppNavigationLayout.Sidebar -> 32f
                }
            navigationVisible -> layout.pageMarginDp
            else -> 0f
        }
    val mainRegionWidthDp =
        (safeWindowWidthDp - navigationWidthDp).coerceAtLeast(0f)
    val barSlotWidthDp =
        minOf(
            mainRegionWidthDp,
            AppShellContentMaxWidthDp + pageMarginDp * 2f,
        )
    val windowWidthPx = with(density) { safeWindowWidthDp.dp.roundToPx() }
    val barSlotWidthPx = with(density) { barSlotWidthDp.dp.roundToPx() }
    var navigationMeasurement by
        remember(layout, windowWidthPx) {
            mutableStateOf<AppNavigationMeasurement?>(null)
        }
    var bottomBarMeasurement by
        remember(activeBottomBarContent.kind, layout, barSlotWidthPx) {
            mutableStateOf<LibraryBottomBarMeasurement?>(null)
        }
    val occupiedBottomInset =
        maxOf(
            safeBounds.bottomInset,
            if (overlayOwnsIme) 0.dp else safeBounds.imeBottomInset,
        )
    val occupiedBottomInsetPx =
        with(density) { occupiedBottomInset.roundToPx() }
    val safeTopInset = safeBounds.topInset
    val measuredNavigationHeightPx =
        navigationMeasurement
            ?.takeIf {
                navigationChromeVisible &&
                    layout == AppNavigationLayout.Bottom &&
                    it.layout == layout &&
                    it.widthPx == windowWidthPx
            }
            ?.heightPx
            ?.coerceAtLeast(0) ?: 0
    val hiddenFraction by
        animateFloatAsState(
            targetValue =
                if (activeBottomBarContent == LibraryBottomBarContent.Hidden) 1f
                else 0f,
            animationSpec = tween(200),
            label = "BottomBarOffset",
        )
    val bottomBarPresentation =
        libraryBottomBarPresentation(
            content = activeBottomBarContent,
            measurement = bottomBarMeasurement,
            expectedLayout = layout,
            expectedWidthPx = barSlotWidthPx,
            hiddenFraction = hiddenFraction,
            allowInteraction = navigationChromeVisible,
        )
    val contentBottomInset =
        with(density) {
            (bottomBarPresentation.clearancePx +
                    measuredNavigationHeightPx +
                    occupiedBottomInsetPx)
                .toDp()
        }
    val nowPlayingBarOffsetPx =
        nowPlayingBarOffsetPx(
            hiddenFraction = hiddenFraction,
            measuredHeightPx = bottomBarPresentation.clearancePx,
        )
    val bottomNavigationHeightPx =
        if (layout == AppNavigationLayout.Bottom && navigationChromeVisible)
            measuredNavigationHeightPx
        else 0
    val minimumNowPlayingBarHeight =
        if (layout == AppNavigationLayout.Bottom)
            HausMetrics.compactMiniPlayerMinHeight
        else HausMetrics.desktopMiniPlayerMinHeight
    val frameWidth = barSlotWidthDp.dp
    val pageMargin = pageMarginDp.dp

    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize()
                .padding(start = safeStartInset, end = safeEndInset)
                .testTag(AppShellSafeFrameTestTag),
        ) {
            if (layout != AppNavigationLayout.Bottom && navigationVisible) {
                AppNavigationChrome(
                    layout = layout,
                    selectedDestination = selectedDestination,
                    activeBrowseMode = activeBrowseMode,
                    onNavigate = onNavigate,
                    onBrowseModeChange = onBrowseModeChange,
                    onSettings = onSettings,
                    modifier =
                        Modifier.fillMaxHeight()
                            .padding(top = safeTopInset)
                            .onSizeChanged {
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
                    layout = layout,
                    content = content,
                    contentBottomInset = contentBottomInset,
                    bottomBarContent = activeBottomBarContent,
                    showSettingsAction =
                        layout == AppNavigationLayout.Bottom &&
                            navigationChromeVisible &&
                            showSettingsAction &&
                            effectiveRouteHeader == null,
                    safeTopInset = safeTopInset,
                    routeHeader = effectiveRouteHeader,
                    onSettings = onSettings,
                    bottomBarPresentation = bottomBarPresentation,
                    bottomNavigationHeightPx = bottomNavigationHeightPx,
                    safeBottomInsetPx = occupiedBottomInsetPx,
                    barOffsetPx = nowPlayingBarOffsetPx,
                    minimumNowPlayingBarHeight = minimumNowPlayingBarHeight,
                    onBottomBarMeasured = { size ->
                        bottomBarMeasurement =
                            LibraryBottomBarMeasurement(
                                content = activeBottomBarContent,
                                heightPx = size.height,
                                layout = layout,
                                widthPx = size.width,
                            )
                    },
                    bottomBar = bottomBar,
                )
            }
        }
        if (layout == AppNavigationLayout.Bottom && navigationChromeVisible) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(
                        start = safeStartInset,
                        end = safeEndInset,
                        bottom = occupiedBottomInset,
                    )) {
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
    layout: AppNavigationLayout,
    content: @Composable () -> Unit,
    contentBottomInset: Dp,
    bottomBarContent: LibraryBottomBarContent,
    showSettingsAction: Boolean,
    safeTopInset: Dp,
    routeHeader: AppShellRouteHeader?,
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
                        .padding(
                            top =
                                routeHeader?.let {
                                    appShellHeaderHeight(layout) + safeTopInset
                                } ?: 0.dp,
                            bottom = contentBottomInset,
                        )) {
                        Box(
                            Modifier.fillMaxSize()
                                .testTag(AppShellContentRegionTestTag)) {
                                content()
                            }
                    }
                routeHeader?.let { header ->
                    AppShellHeader(
                        model = header,
                        layout = layout,
                        safeTopInset = safeTopInset,
                        modifier =
                            Modifier.align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .testTag("AppShellHeaderRoot"),
                    )
                }
                if (showSettingsAction) {
                    IconButton(
                        onClick = onSettings,
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .padding(
                                    top = safeTopInset,
                                )
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
                            tint = HausColors.current.textPrimary,
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
