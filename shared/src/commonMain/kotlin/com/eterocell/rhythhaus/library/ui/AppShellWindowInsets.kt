package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayVisible

/**
 * Keeps the full-window player's interactive content inside Shared's current
 * safe rectangle.
 */
@Composable
internal fun NowPlayingSafeFrame(
    bounds: AppShellSafeBounds,
    content: @Composable () -> Unit
) {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val overlayOwnsIme = LocalHausWindowOverlayVisible.current
    Box(
        Modifier.fillMaxSize()
            .padding(
                start = if (ltr) bounds.leftInset else bounds.rightInset,
                top = bounds.topInset,
                end = if (ltr) bounds.rightInset else bounds.leftInset,
                bottom =
                    maxOf(
                        bounds.bottomInset,
                        if (overlayOwnsIme) 0.dp else bounds.imeBottomInset),
            )) {
            Box(Modifier.fillMaxSize().testTag("NowPlayingSafeFrame")) {
                content()
            }
        }
}

/**
 * Physical bars/cutouts deliberately exclude IME; the foreground owner consumes
 * IME separately.
 */
@Composable
internal fun rememberAppShellWindowBounds(
    widthDp: Float,
    heightDp: Float,
    physicalInsets: WindowInsets =
        WindowInsets.systemBars.union(WindowInsets.displayCutout),
    keyboardInsets: WindowInsets = WindowInsets.ime,
): AppShellSafeBounds {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val left =
        with(density) {
            physicalInsets.getLeft(density, direction).toDp().value
        }
    val top = with(density) { physicalInsets.getTop(density).toDp().value }
    val right =
        with(density) {
            physicalInsets.getRight(density, direction).toDp().value
        }
    val bottom =
        with(density) { physicalInsets.getBottom(density).toDp().value }
    val ime = with(density) { keyboardInsets.getBottom(density).toDp().value }
    return remember(widthDp, heightDp, left, top, right, bottom, ime) {
        appShellSafeBoundsFor(widthDp, heightDp, left, top, right, bottom, ime)
    }
}
