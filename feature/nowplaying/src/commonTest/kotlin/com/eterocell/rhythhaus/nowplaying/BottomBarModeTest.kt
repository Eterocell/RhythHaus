package com.eterocell.rhythhaus.nowplaying

import kotlin.test.Test
import kotlin.test.assertEquals

public class BottomBarModeTest {
    @Test
    public fun emptyLibraryStillUsesBottomBarNavigationMode(): Unit =
        assertEquals(
            BottomBarMode.EmptyLibraryNavigation, bottomBarModeFor(null))
}
