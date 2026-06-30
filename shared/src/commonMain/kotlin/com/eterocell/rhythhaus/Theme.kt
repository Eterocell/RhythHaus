package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.Color

enum class RhythHausThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        val settingsOptions: List<RhythHausThemeMode> = listOf(System, Light, Dark)

        fun fromSerialized(value: String?): RhythHausThemeMode = when (value) {
            System.serialized -> System
            Light.serialized -> Light
            Dark.serialized -> Dark
            else -> System
        }
    }
}

val RhythHausThemeMode.serialized: String
    get() = when (this) {
        RhythHausThemeMode.System -> "system"
        RhythHausThemeMode.Light -> "light"
        RhythHausThemeMode.Dark -> "dark"
    }

val RhythHausThemeMode.displayLabel: String
    get() = when (this) {
        RhythHausThemeMode.System -> "System"
        RhythHausThemeMode.Light -> "Light"
        RhythHausThemeMode.Dark -> "Dark"
    }

val RhythHausThemeMode.displayDescription: String
    get() = when (this) {
        RhythHausThemeMode.System -> "Follow system appearance"
        RhythHausThemeMode.Light -> "Use light appearance"
        RhythHausThemeMode.Dark -> "Use dark appearance"
    }

data class HausColorPalette(
    val ink: Color,
    val paper: Color,
    val muted: Color,
    val line: Color,
    val panel: Color,
    val panelStrong: Color,
    val pulse: Color,
)

val LightHausPalette = HausColorPalette(
    ink = Color(0xFF111018),
    paper = Color(0xFFFFFAF1),
    muted = Color(0xFF776F66),
    line = Color(0x1A111018),
    panel = Color(0xFFF5EBDD),
    panelStrong = Color(0xFFE9D8C2),
    pulse = Color(0xFFFF5E3A),
)

val DarkHausPalette = HausColorPalette(
    ink = Color(0xFFF7EFE4),
    paper = Color(0xFF0F1117),
    muted = Color(0xFFB7AFA6),
    line = Color(0x33F7EFE4),
    panel = Color(0xFF1A1D26),
    panelStrong = Color(0xFF252A36),
    pulse = Color(0xFFFF7A52),
)

fun resolveHausPalette(mode: RhythHausThemeMode, systemIsDark: Boolean): HausColorPalette = when (mode) {
    RhythHausThemeMode.System -> if (systemIsDark) DarkHausPalette else LightHausPalette
    RhythHausThemeMode.Light -> LightHausPalette
    RhythHausThemeMode.Dark -> DarkHausPalette
}
