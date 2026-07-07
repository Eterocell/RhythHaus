package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.nowplaying.BottomBarMode
import com.eterocell.rhythhaus.nowplaying.bottomBarModeFor
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarModeTest {
    @Test
    fun emptyLibraryStillUsesBottomBarNavigationMode() {
        assertEquals(BottomBarMode.EmptyLibraryNavigation, bottomBarModeFor(track = null))
    }
}
