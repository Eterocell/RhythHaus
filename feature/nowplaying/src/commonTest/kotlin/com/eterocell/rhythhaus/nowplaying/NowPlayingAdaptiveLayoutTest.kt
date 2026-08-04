package com.eterocell.rhythhaus.nowplaying

import kotlin.test.Test
import kotlin.test.assertEquals

public class NowPlayingAdaptiveLayoutTest {
    @Test
    public fun nowPlayingAdaptiveLayoutUsesCompactForPhonePortrait(): Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(390f, 844f))

    @Test
    public fun nowPlayingAdaptiveLayoutUsesCompactForNarrowPortraitTablet():
        Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(700f, 1000f))

    @Test
    public fun nowPlayingAdaptiveLayoutUsesSplitForWideTablet(): Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(840f, 1180f))

    @Test
    public fun nowPlayingAdaptiveLayoutUsesSplitForLandscapeMediumWidth():
        Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(700f, 500f))

    @Test
    public fun nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth(): Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(1200f, 800f))
}
