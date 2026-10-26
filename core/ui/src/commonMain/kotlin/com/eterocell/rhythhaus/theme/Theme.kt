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
    /** Foreground color for controls displayed on the pulse color. */
    public val onPulse: Color,
    /** Surface color for secondary navigation and chrome. */
    public val navigation: Color,
    /** Color for essential interaction and focus boundaries. */
    public val outline: Color,
    /** Foreground color for destructive actions. */
    public val danger: Color,
    /** Surface color for destructive-action containers. */
    public val dangerPanel: Color,
    /** Foreground color for controls displayed on the danger color. */
    public val onDanger: Color,
    /** Foreground color for warning states. */
    public val warning: Color,
)

/** Palette for light appearance. */
public val LightHausPalette: HausColorPalette =
    HausColorPalette(
        ink = Color(0xFF253129),
        paper = Color(0xFFF9FAF7),
        muted = Color(0xFF65715F),
        line = Color(0xFFDEE3D8),
        panel = Color(0xFFFFFFFF),
        panelStrong = Color(0xFFEAF0E5),
        pulse = Color(0xFF3C6148),
        onPulse = Color(0xFFFFFFFF),
        navigation = Color(0xFFF2F4EE),
        outline = Color(0xFF6C7A66),
        danger = Color(0xFFB13B32),
        dangerPanel = Color(0xFFFBEAE7),
        onDanger = Color(0xFFFFFFFF),
        warning = Color(0xFF805D13),
    )

/** Palette for dark appearance. */
public val DarkHausPalette: HausColorPalette =
    HausColorPalette(
        ink = Color(0xFFE5EADD),
        paper = Color(0xFF171E1A),
        muted = Color(0xFFA5B09F),
        line = Color(0xFF394434),
        panel = Color(0xFF202923),
        panelStrong = Color(0xFF303E2F),
        pulse = Color(0xFFBFD0AA),
        onPulse = Color(0xFF182217),
        navigation = Color(0xFF1C251E),
        outline = Color(0xFF7E8E74),
        danger = Color(0xFFFFABA2),
        dangerPanel = Color(0xFF462824),
        onDanger = Color(0xFF321A18),
        warning = Color(0xFFE7C976),
    )

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
