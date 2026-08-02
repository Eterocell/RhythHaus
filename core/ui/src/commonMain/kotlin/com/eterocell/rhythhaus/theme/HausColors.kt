package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Composition-local palette consumed by RhythHaus UI primitives. */
public val LocalHausColors: ProvidableCompositionLocal<HausColorPalette> =
    staticCompositionLocalOf {
        LightHausPalette
    }

/** Provides the current RhythHaus palette to composable UI. */
public object HausColors {
    /** Current composition-local palette. */
    public val current: HausColorPalette
        @Composable @ReadOnlyComposable get() = LocalHausColors.current
}

/** Current ink color. */
public val HausInk: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.ink
/** Current paper color. */
public val HausPaper: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.paper
/** Current muted color. */
public val HausMuted: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.muted
/** Current line color. */
public val HausLine: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.line
/** Current panel color. */
public val HausPanel: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.panel
/** Current strong-panel color. */
public val HausPanelStrong: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.panelStrong
/** Current pulse color. */
public val HausPulse: Color
    @Composable @ReadOnlyComposable get() = HausColors.current.pulse
