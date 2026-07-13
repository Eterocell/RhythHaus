package com.eterocell.rhythhaus.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenTest {
    @Test
    fun compactSettingsLayoutPolicyUsesApprovedSpacing() {
        assertEquals(16.dp, CompactSettingsLayoutPolicy.horizontalPagePadding)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.verticalPagePadding)
        assertEquals(12.dp, CompactSettingsLayoutPolicy.itemSpacing)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.bottomContentPadding)
    }
}
