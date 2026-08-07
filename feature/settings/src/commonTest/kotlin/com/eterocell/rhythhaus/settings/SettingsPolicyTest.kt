package com.eterocell.rhythhaus.settings

import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

public class SettingsPolicyTest {
    @Test
    public fun compactSettingsLayoutPolicyHasApprovedValues() {
        assertEquals(
            16, CompactSettingsLayoutPolicy.horizontalPagePadding.value.toInt())
        assertEquals(
            8, CompactSettingsLayoutPolicy.verticalPagePadding.value.toInt())
        assertEquals(12, CompactSettingsLayoutPolicy.itemSpacing.value.toInt())
        assertEquals(
            8, CompactSettingsLayoutPolicy.bottomContentPadding.value.toInt())
    }

    @Test
    public fun sourceLabelsDeriveFromSettingsSourceItem() {
        assertEquals(
            SettingsSourcePresentation(
                displayNameFallbackRequired = false,
                access = SettingsSourceAccess.Available,
                scan = SettingsSourceScan.NeverScanned),
            SettingsSourceItem("opaque-one", "One", true, false).presentation())
        assertEquals(
            SettingsSourcePresentation(
                displayNameFallbackRequired = false,
                access = SettingsSourceAccess.Lost,
                scan = SettingsSourceScan.HasBeenScanned),
            SettingsSourceItem("opaque-two", "Two", false, true).presentation())
        assertEquals(
            SettingsSourcePresentation(
                displayNameFallbackRequired = true,
                access = SettingsSourceAccess.Available,
                scan = SettingsSourceScan.HasBeenScanned),
            SettingsSourceItem("opaque-three", "", true, true).presentation())
    }

    @Test
    public fun themeOptionsUseSystemLightDarkOrder() {
        assertEquals(
            listOf(
                RhythHausThemeMode.System,
                RhythHausThemeMode.Light,
                RhythHausThemeMode.Dark),
            RhythHausThemeMode.settingsOptions)
    }
}
