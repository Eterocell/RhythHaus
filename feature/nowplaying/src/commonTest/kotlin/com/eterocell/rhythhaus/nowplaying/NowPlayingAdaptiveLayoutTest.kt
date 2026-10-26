package com.eterocell.rhythhaus.nowplaying

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    public fun nowPlayingAdaptiveLayoutKeepsSplitPanesAboveMinimumWidth():
        Unit {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(711f, 500f))
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(712f, 500f))
    }

    @Test
    public fun nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth(): Unit =
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(1200f, 800f))

    @Test
    public fun nowPlayingQueueSidebarRequiresItsFullMeasuredWidth(): Unit {
        assertFalse(nowPlayingQueueSidebarFor(1_015f))
        assertTrue(nowPlayingQueueSidebarFor(1_016f))
    }
}
