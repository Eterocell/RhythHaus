package com.eterocell.rhythhaus.systemui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
internal expect fun AppSystemBarAppearance(
    statusBarSurface: Color,
    navigationBarSurface: Color,
)

internal fun systemBarUsesLightIcons(surface: Color): Boolean =
    surface.luminance() < 0.5f
