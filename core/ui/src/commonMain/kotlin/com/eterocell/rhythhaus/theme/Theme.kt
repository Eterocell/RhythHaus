package com.eterocell.rhythhaus.theme

import androidx.compose.ui.graphics.Color

/** User-selectable appearance mode for RhythHaus surfaces. */
public enum class RhythHausThemeMode {
    System,
    Light,
    Dark,
    ;

    /** Theme-mode serialization and settings helpers. */
    public companion object {
        /** Theme modes displayed by the settings UI in stable order. */
        public val settingsOptions: List<RhythHausThemeMode> =
            listOf(System, Light, Dark)

        /** Parses a persisted mode value, falling back to the system mode. */
        public fun fromSerialized(value: String?): RhythHausThemeMode =
            when (value) {
                System.serialized -> System
                Light.serialized -> Light
                Dark.serialized -> Dark
                else -> System
            }
    }
}

/** Stable persisted representation of this theme mode. */
public val RhythHausThemeMode.serialized: String
    get() =
        when (this) {
            RhythHausThemeMode.System -> "system"
            RhythHausThemeMode.Light -> "light"
            RhythHausThemeMode.Dark -> "dark"
        }

/** Human-readable theme mode label. */
public val RhythHausThemeMode.displayLabel: String
    get() =
        when (this) {
            RhythHausThemeMode.System -> "System"
            RhythHausThemeMode.Light -> "Light"
            RhythHausThemeMode.Dark -> "Dark"
        }

/** Human-readable explanation of this theme mode. */
public val RhythHausThemeMode.displayDescription: String
    get() =
        when (this) {
            RhythHausThemeMode.System -> "Follow system appearance"
            RhythHausThemeMode.Light -> "Use light appearance"
            RhythHausThemeMode.Dark -> "Use dark appearance"
        }

/** Palette used by reusable RhythHaus UI primitives. */
public data class HausColorPalette(
    /** Primary foreground color. */
    public val ink: Color,
    /** Primary background color. */
    public val paper: Color,
    /** Secondary foreground color. */
    public val muted: Color,
    /** Subtle divider color. */
    public val line: Color,
    /** Standard surface color. */
    public val panel: Color,
    /** Emphasized surface color. */
    public val panelStrong: Color,
    /** Accent color for active state. */
    public val pulse: Color,
)

/** Palette for light appearance. */
public val LightHausPalette: HausColorPalette =
    HausColorPalette(
        Color(0xFF111018),
        Color(0xFFFFFAF1),
        Color(0xFF776F66),
        Color(0x1A111018),
        Color(0xFFF5EBDD),
        Color(0xFFE9D8C2),
        Color(0xFFFF5E3A))

/** Palette for dark appearance. */
public val DarkHausPalette: HausColorPalette =
    HausColorPalette(
        Color(0xFFF7EFE4),
        Color(0xFF0F1117),
        Color(0xFFB7AFA6),
        Color(0x33F7EFE4),
        Color(0xFF1A1D26),
        Color(0xFF252A36),
        Color(0xFFFF7A52))

/** Resolves the active palette from the selected mode and system appearance. */
public fun resolveHausPalette(
    mode: RhythHausThemeMode,
    systemIsDark: Boolean
): HausColorPalette =
    when (mode) {
        RhythHausThemeMode.System ->
            if (systemIsDark) DarkHausPalette else LightHausPalette
        RhythHausThemeMode.Light -> LightHausPalette
        RhythHausThemeMode.Dark -> DarkHausPalette
    }
