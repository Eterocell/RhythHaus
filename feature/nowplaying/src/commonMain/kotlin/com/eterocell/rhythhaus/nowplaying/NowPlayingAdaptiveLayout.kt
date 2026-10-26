package com.eterocell.rhythhaus.nowplaying

/** Selects a layout whose panes fit the measured safe content bounds. */
public enum class NowPlayingAdaptiveLayoutMode {
    Compact,
    Split
}

/**
 * Returns a split only when the two 320dp panes, 24dp gap, and 48dp of
 * horizontal padding fit the available safe width. The artwork also needs 320dp
 * of vertical space after 48dp of padding.
 */
public fun nowPlayingAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): NowPlayingAdaptiveLayoutMode =
    if (widthDp >= 712f && heightDp >= 368f) {
        NowPlayingAdaptiveLayoutMode.Split
    } else {
        NowPlayingAdaptiveLayoutMode.Compact
    }

/**
 * Whether a queue sidebar can sit beside the artwork and controls without
 * shrinking either pane below 320dp.
 */
internal fun nowPlayingQueueSidebarFor(widthDp: Float): Boolean =
    widthDp >= 1_016f
