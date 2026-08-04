package com.eterocell.rhythhaus.nowplaying

/** Selects the preserved compact or split Now Playing layout. */
public enum class NowPlayingAdaptiveLayoutMode {
    Compact,
    Split
}

/**
 * Returns the preserved Now Playing layout selection for the supplied bounds in
 * dp.
 */
public fun nowPlayingAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): NowPlayingAdaptiveLayoutMode =
    if (widthDp >= 840f ||
        (widthDp >= 600f && widthDp > 0f && heightDp / widthDp < 1.2f)) {
        NowPlayingAdaptiveLayoutMode.Split
    } else {
        NowPlayingAdaptiveLayoutMode.Compact
    }
