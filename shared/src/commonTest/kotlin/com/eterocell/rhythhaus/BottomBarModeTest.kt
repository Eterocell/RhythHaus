package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarModeTest {
    @Test
    fun emptyLibraryStillUsesBottomBarNavigationMode() {
        assertEquals(BottomBarMode.EmptyLibraryNavigation, bottomBarModeFor(track = null))
    }
}
