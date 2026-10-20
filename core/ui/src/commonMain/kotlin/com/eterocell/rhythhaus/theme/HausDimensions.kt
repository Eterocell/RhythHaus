package com.eterocell.rhythhaus.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Standard spacing steps for shared Studio surfaces. */
public object HausSpacing {
    /** Extra-small spacing. */
    public val xs: Dp = 4.dp

    /** Small spacing. */
    public val sm: Dp = 8.dp

    /** Medium spacing. */
    public val md: Dp = 12.dp

    /** Large spacing. */
    public val lg: Dp = 16.dp

    /** Extra-large spacing. */
    public val xl: Dp = 24.dp

    /** Double-extra-large spacing. */
    public val xxl: Dp = 32.dp

    /** Section spacing. */
    public val section: Dp = 48.dp
}

/** Corner radii for common Studio surface families. */
public object HausShapes {
    /** Artwork corner radius. */
    public val artwork: Dp = 8.dp

    /** Control corner radius. */
    public val control: Dp = 10.dp

    /** Grouped-surface corner radius. */
    public val group: Dp = 12.dp

    /** Dialog and overlay corner radius. */
    public val overlay: Dp = 20.dp
}

/** Minimum and adaptive dimensions for shared Studio components. */
public object HausMetrics {
    /** Compact song-row minimum height. */
    public val compactSongRowMinHeight: Dp = 64.dp

    /** Compact artwork minimum size. */
    public val compactArtworkMinSize: Dp = 48.dp

    /** Desktop song-row minimum height. */
    public val desktopSongRowMinHeight: Dp = 56.dp

    /** Desktop artwork minimum size. */
    public val desktopArtworkMinSize: Dp = 40.dp

    /** Compact mini-player minimum height. */
    public val compactMiniPlayerMinHeight: Dp = 64.dp

    /** Desktop mini-player minimum height. */
    public val desktopMiniPlayerMinHeight: Dp = 80.dp
}
