package com.eterocell.rhythhaus.systemui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSystemBarAppearanceJvmTest {
    @Test
    fun iconContrastUsesTheVisibleEdgeSurfaceColor() {
        assertTrue(systemBarUsesLightIcons(Color(0xFF171717)))
        assertFalse(systemBarUsesLightIcons(Color(0xFFF4F4F4)))
    }
}
