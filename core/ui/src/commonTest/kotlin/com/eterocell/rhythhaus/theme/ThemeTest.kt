package com.eterocell.rhythhaus.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThemeTest {
    @Test
    fun themeModesSerializeToStableValues() {
        assertEquals("system", RhythHausThemeMode.System.serialized)
        assertEquals("light", RhythHausThemeMode.Light.serialized)
        assertEquals("dark", RhythHausThemeMode.Dark.serialized)
    }

    @Test
    fun missingOrInvalidSerializedValuesParseToSystem() {
        assertEquals(
            RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized(null))
        assertEquals(
            RhythHausThemeMode.System, RhythHausThemeMode.fromSerialized(""))
        assertEquals(
            RhythHausThemeMode.System,
            RhythHausThemeMode.fromSerialized("unknown"))
    }

    @Test
    fun validSerializedValuesParseToThemeModes() {
        assertEquals(
            RhythHausThemeMode.System,
            RhythHausThemeMode.fromSerialized("system"))
        assertEquals(
            RhythHausThemeMode.Light,
            RhythHausThemeMode.fromSerialized("light"))
        assertEquals(
            RhythHausThemeMode.Dark, RhythHausThemeMode.fromSerialized("dark"))
    }

    @Test
    fun systemPaletteResolvesFromSystemDarkPreference() {
        assertSame(
            LightHausPalette,
            resolveHausPalette(RhythHausThemeMode.System, systemIsDark = false))
        assertSame(
            DarkHausPalette,
            resolveHausPalette(RhythHausThemeMode.System, systemIsDark = true))
    }

    @Test
    fun explicitPaletteIgnoresSystemDarkPreference() {
        assertSame(
            LightHausPalette,
            resolveHausPalette(RhythHausThemeMode.Light, systemIsDark = true))
        assertSame(
            DarkHausPalette,
            resolveHausPalette(RhythHausThemeMode.Dark, systemIsDark = false))
    }

    @Test
    fun settingsOptionOrderIsSystemLightDark() {
        assertEquals(
            listOf(
                RhythHausThemeMode.System,
                RhythHausThemeMode.Light,
                RhythHausThemeMode.Dark),
            RhythHausThemeMode.settingsOptions)
    }

    @Test
    fun settingsOptionLabelsAndDescriptionsAreStable() {
        assertEquals("System", RhythHausThemeMode.System.displayLabel)
        assertEquals(
            "Follow system appearance",
            RhythHausThemeMode.System.displayDescription)
        assertEquals("Light", RhythHausThemeMode.Light.displayLabel)
        assertEquals(
            "Use light appearance", RhythHausThemeMode.Light.displayDescription)
        assertEquals("Dark", RhythHausThemeMode.Dark.displayLabel)
        assertEquals(
            "Use dark appearance", RhythHausThemeMode.Dark.displayDescription)
    }

    @Test
    fun activePalettesMeetReadableActionContrast() {
        listOf("light" to LightHausPalette, "dark" to DarkHausPalette)
            .forEach { (name, palette) ->
                assertContrastAtLeast(
                    "$name ink/paper", palette.ink, palette.paper, 4.5)
                assertContrastAtLeast(
                    "$name muted/paper", palette.muted, palette.paper, 4.5)
                assertContrastAtLeast(
                    "$name muted/panel", palette.muted, palette.panel, 4.5)
                assertContrastAtLeast(
                    "$name ink/panelStrong",
                    palette.ink,
                    palette.panelStrong,
                    4.5)
                assertContrastAtLeast(
                    "$name muted/navigation",
                    palette.muted,
                    palette.navigation,
                    4.5)
                assertContrastAtLeast(
                    "$name onPulse/pulse", palette.onPulse, palette.pulse, 4.5)
                assertContrastAtLeast(
                    "$name onDanger/danger",
                    palette.onDanger,
                    palette.danger,
                    4.5)
                assertContrastAtLeast(
                    "$name danger/dangerPanel",
                    palette.danger,
                    palette.dangerPanel,
                    4.5)
                assertContrastAtLeast(
                    "$name warning/paper", palette.warning, palette.paper, 4.5)
                assertContrastAtLeast(
                    "$name outline/panel", palette.outline, palette.panel, 3.0)
                assertContrastAtLeast(
                    "$name outline/paper", palette.outline, palette.paper, 3.0)
                assertContrastAtLeast(
                    "$name outline/panelStrong",
                    palette.outline,
                    palette.panelStrong,
                    3.0)
                assertContrastAtLeast(
                    "$name outline/navigation",
                    palette.outline,
                    palette.navigation,
                    3.0)
            }
    }
}

private fun assertContrastAtLeast(
    pairName: String,
    foreground: Color,
    background: Color,
    minimum: Double,
) {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    val contrast =
        (max(foregroundLuminance, backgroundLuminance) + 0.05) /
            (min(foregroundLuminance, backgroundLuminance) + 0.05)

    assertTrue(
        contrast >= minimum,
        "$pairName contrast was $contrast:1; expected at least $minimum:1",
    )
}

private fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double {
        val normalized = channel.toDouble()
        return if (normalized <= 0.04045) {
            normalized / 12.92
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4)
        }
    }

    return 0.2126 * linearize(color.red) +
        0.7152 * linearize(color.green) +
        0.0722 * linearize(color.blue)
}
