package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ThemeTest {
    @Test
    fun themeModesSerializeToStableValues() {
        assertEquals("system", RhythHausThemeMode.System.serialized)
        assertEquals("light", RhythHausThemeMode.Light.serialized)
        assertEquals("dark", RhythHausThemeMode.Dark.serialized)
    }

    @Test
    fun missingOrInvalidSerializedValuesParseToSystem() {
        assertEquals(RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized(null))
        assertEquals(RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized(""))
        assertEquals(RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized("unknown"))
    }

    @Test
    fun validSerializedValuesParseToThemeModes() {
        assertEquals(RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized("system"))
        assertEquals(RhythHausThemeMode.Light, RhythHausThemeMode.fromSerialized("light"))
        assertEquals(RhythHausThemeMode.Dark, RhythHausThemeMode.fromSerialized("dark"))
    }

    @Test
    fun systemPaletteResolvesFromSystemDarkPreference() {
        assertSame(LightHausPalette, resolveHausPalette(RhythHausThemeMode.System, systemIsDark = false))
        assertSame(DarkHausPalette, resolveHausPalette(RhythHausThemeMode.System, systemIsDark = true))
    }

    @Test
    fun explicitPaletteIgnoresSystemDarkPreference() {
        assertSame(LightHausPalette, resolveHausPalette(RhythHausThemeMode.Light, systemIsDark = true))
        assertSame(DarkHausPalette, resolveHausPalette(RhythHausThemeMode.Dark, systemIsDark = false))
    }

    @Test
    fun settingsOptionOrderIsSystemLightDark() {
        assertEquals(
            listOf(RhythHausThemeMode.System, RhythHausThemeMode.Light, RhythHausThemeMode.Dark),
            RhythHausThemeMode.settingsOptions,
        )
    }
}
