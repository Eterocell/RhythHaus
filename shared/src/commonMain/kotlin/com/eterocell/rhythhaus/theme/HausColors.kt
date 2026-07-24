package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalHausColors = staticCompositionLocalOf { LightHausPalette }

object HausColors {
    val current: HausColorPalette
        @Composable @ReadOnlyComposable get() = LocalHausColors.current
}

val HausInk: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.ink

val HausPaper: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.paper

val HausMuted: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.muted

val HausLine: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.line

val HausPanel: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.panel

val HausPanelStrong: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.panelStrong

val HausPulse: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.pulse
