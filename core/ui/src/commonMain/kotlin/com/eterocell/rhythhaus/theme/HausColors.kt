package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Composition-local palette consumed by RhythHaus UI primitives. */
public val LocalHausColors: ProvidableCompositionLocal<HausColorPalette> =
    staticCompositionLocalOf {
        hausPalette(HausPaletteId.Moss, isDark = false)
    }

/** Provides the current RhythHaus palette to composable UI. */
public object HausColors {
    /** Current composition-local palette. */
    public val current: HausColorPalette
        @Composable @ReadOnlyComposable get() = LocalHausColors.current
}
