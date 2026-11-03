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

/**
 * Resolves this mode to dark appearance, consulting the system when requested.
 */
public fun RhythHausThemeMode.resolveIsDark(systemIsDark: Boolean): Boolean =
    when (this) {
        RhythHausThemeMode.System -> systemIsDark
        RhythHausThemeMode.Light -> false
        RhythHausThemeMode.Dark -> true
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

/** Stable persisted identity of a curated RhythHaus color system. */
public enum class HausPaletteId(
    /** Stable value stored independently from the selected appearance mode. */
    public val serialized: String,
    internal val lightColors: HausColorPalette,
    internal val darkColors: HausColorPalette,
) {
    Moss(
        serialized = "moss",
        lightColors =
            HausColorPalette(
                background = Color(0xFFF4F7F2),
                surface = Color(0xFFFFFFFF),
                raisedSurface = Color(0xFFEAF1E8),
                navigationSurface = Color(0xFFE8F0E6),
                textPrimary = Color(0xFF1E2720),
                textSecondary = Color(0xFF4C5A4E),
                textInverse = Color(0xFFF8FBF8),
                primaryAction = Color(0xFF315D3E),
                onPrimaryAction = Color(0xFFFFFFFF),
                selectedSurface = Color(0xFFDDEADE),
                onSelectedSurface = Color(0xFF1C3222),
                divider = Color(0xFF758176),
                essentialBoundary = Color(0xFF758176),
                focus = Color(0xFF315D3E),
                disabledSurface = Color(0xFFE7EAE6),
                disabledContent = Color(0xFF58625A),
                error = Color(0xFFA32835),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFCE9E8),
                onErrorContainer = Color(0xFF64161E),
                warning = Color(0xFF795700),
                onWarning = Color(0xFFFFFFFF),
                warningContainer = Color(0xFFFFF1C5),
                onWarningContainer = Color(0xFF4E3B00),
                success = Color(0xFF23613A),
                onSuccess = Color(0xFFFFFFFF),
                successContainer = Color(0xFFE4F1E5),
                onSuccessContainer = Color(0xFF173E25),
                scrimColor = Color(0xFF07100A),
                scrimAlpha = 0.54f,
            ),
        darkColors =
            HausColorPalette(
                background = Color(0xFF151B16),
                surface = Color(0xFF1D251E),
                raisedSurface = Color(0xFF263128),
                navigationSurface = Color(0xFF202B22),
                textPrimary = Color(0xFFE9EFE9),
                textSecondary = Color(0xFFB9C4B9),
                textInverse = Color(0xFF202820),
                primaryAction = Color(0xFF98D9A4),
                onPrimaryAction = Color(0xFF0C2817),
                selectedSurface = Color(0xFF304437),
                onSelectedSurface = Color(0xFFEFF7EF),
                divider = Color(0xFF77857A),
                essentialBoundary = Color(0xFF77857A),
                focus = Color(0xFF98D9A4),
                disabledSurface = Color(0xFF303932),
                disabledContent = Color(0xFFADB8AE),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF641219),
                errorContainer = Color(0xFF67272B),
                onErrorContainer = Color(0xFFFFE1DD),
                warning = Color(0xFFFFE18E),
                onWarning = Color(0xFF352800),
                warningContainer = Color(0xFF544000),
                onWarningContainer = Color(0xFFFFEAB8),
                success = Color(0xFFA5E2A7),
                onSuccess = Color(0xFF12391D),
                successContainer = Color(0xFF204B2A),
                onSuccessContainer = Color(0xFFD4F4D0),
                scrimColor = Color(0xFF000000),
                scrimAlpha = 0.64f,
            ),
    ),
    Bay(
        serialized = "bay",
        lightColors =
            HausColorPalette(
                background = Color(0xFFF2F7FA),
                surface = Color(0xFFFFFFFF),
                raisedSurface = Color(0xFFE9F2F7),
                navigationSurface = Color(0xFFE6F0F5),
                textPrimary = Color(0xFF1C252B),
                textSecondary = Color(0xFF465761),
                textInverse = Color(0xFFF7FBFD),
                primaryAction = Color(0xFF285D83),
                onPrimaryAction = Color(0xFFFFFFFF),
                selectedSurface = Color(0xFFDCEBF4),
                onSelectedSurface = Color(0xFF162F40),
                divider = Color(0xFF71818A),
                essentialBoundary = Color(0xFF71818A),
                focus = Color(0xFF285D83),
                disabledSurface = Color(0xFFE5EAEC),
                disabledContent = Color(0xFF56636A),
                error = Color(0xFFA32835),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFCE9E8),
                onErrorContainer = Color(0xFF64161E),
                warning = Color(0xFF795700),
                onWarning = Color(0xFFFFFFFF),
                warningContainer = Color(0xFFFFF1C5),
                onWarningContainer = Color(0xFF4E3B00),
                success = Color(0xFF23613A),
                onSuccess = Color(0xFFFFFFFF),
                successContainer = Color(0xFFE4F1E5),
                onSuccessContainer = Color(0xFF173E25),
                scrimColor = Color(0xFF07100A),
                scrimAlpha = 0.54f,
            ),
        darkColors =
            HausColorPalette(
                background = Color(0xFF121A20),
                surface = Color(0xFF1B252E),
                raisedSurface = Color(0xFF25333E),
                navigationSurface = Color(0xFF1D2B34),
                textPrimary = Color(0xFFE7EDF4),
                textSecondary = Color(0xFFB7C2CD),
                textInverse = Color(0xFF20272D),
                primaryAction = Color(0xFF98D1FF),
                onPrimaryAction = Color(0xFF102A3D),
                selectedSurface = Color(0xFF2B4558),
                onSelectedSurface = Color(0xFFEDF6FD),
                divider = Color(0xFF778895),
                essentialBoundary = Color(0xFF778895),
                focus = Color(0xFF98D1FF),
                disabledSurface = Color(0xFF303A42),
                disabledContent = Color(0xFFAEBBC5),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF641219),
                errorContainer = Color(0xFF67272B),
                onErrorContainer = Color(0xFFFFE1DD),
                warning = Color(0xFFFFE18E),
                onWarning = Color(0xFF352800),
                warningContainer = Color(0xFF544000),
                onWarningContainer = Color(0xFFFFEAB8),
                success = Color(0xFFA5E2A7),
                onSuccess = Color(0xFF12391D),
                successContainer = Color(0xFF204B2A),
                onSuccessContainer = Color(0xFFD4F4D0),
                scrimColor = Color(0xFF000000),
                scrimAlpha = 0.64f,
            ),
    ),
    Clay(
        serialized = "clay",
        lightColors =
            HausColorPalette(
                background = Color(0xFFFBF6F0),
                surface = Color(0xFFFFFFFF),
                raisedSurface = Color(0xFFF5ECE3),
                navigationSurface = Color(0xFFF2E9E0),
                textPrimary = Color(0xFF29221D),
                textSecondary = Color(0xFF5B4D43),
                textInverse = Color(0xFFFFFAF5),
                primaryAction = Color(0xFF89502C),
                onPrimaryAction = Color(0xFFFFFFFF),
                selectedSurface = Color(0xFFF2E2D4),
                onSelectedSurface = Color(0xFF382417),
                divider = Color(0xFF877A6F),
                essentialBoundary = Color(0xFF877A6F),
                focus = Color(0xFF89502C),
                disabledSurface = Color(0xFFECE7E2),
                disabledContent = Color(0xFF665A51),
                error = Color(0xFFA32835),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFCE9E8),
                onErrorContainer = Color(0xFF64161E),
                warning = Color(0xFF795700),
                onWarning = Color(0xFFFFFFFF),
                warningContainer = Color(0xFFFFF1C5),
                onWarningContainer = Color(0xFF4E3B00),
                success = Color(0xFF23613A),
                onSuccess = Color(0xFFFFFFFF),
                successContainer = Color(0xFFE4F1E5),
                onSuccessContainer = Color(0xFF173E25),
                scrimColor = Color(0xFF140C06),
                scrimAlpha = 0.54f,
            ),
        darkColors =
            HausColorPalette(
                background = Color(0xFF1D1714),
                surface = Color(0xFF2A211C),
                raisedSurface = Color(0xFF3A2D24),
                navigationSurface = Color(0xFF30251F),
                textPrimary = Color(0xFFF2E9E2),
                textSecondary = Color(0xFFC6B8AC),
                textInverse = Color(0xFF2B231F),
                primaryAction = Color(0xFFFFD0AB),
                onPrimaryAction = Color(0xFF36200F),
                selectedSurface = Color(0xFF4C3527),
                onSelectedSurface = Color(0xFFFFF3E8),
                divider = Color(0xFF93857A),
                essentialBoundary = Color(0xFF93857A),
                focus = Color(0xFFFFD0AB),
                disabledSurface = Color(0xFF3D332C),
                disabledContent = Color(0xFFC3B5A9),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF641219),
                errorContainer = Color(0xFF67272B),
                onErrorContainer = Color(0xFFFFE1DD),
                warning = Color(0xFFFFE18E),
                onWarning = Color(0xFF352800),
                warningContainer = Color(0xFF544000),
                onWarningContainer = Color(0xFFFFEAB8),
                success = Color(0xFFA5E2A7),
                onSuccess = Color(0xFF12391D),
                successContainer = Color(0xFF204B2A),
                onSuccessContainer = Color(0xFFD4F4D0),
                scrimColor = Color(0xFF000000),
                scrimAlpha = 0.64f,
            ),
    ),
    Iris(
        serialized = "iris",
        lightColors =
            HausColorPalette(
                background = Color(0xFFF7F5FB),
                surface = Color(0xFFFFFFFF),
                raisedSurface = Color(0xFFF0EBF7),
                navigationSurface = Color(0xFFEEE9F5),
                textPrimary = Color(0xFF25222C),
                textSecondary = Color(0xFF514B5E),
                textInverse = Color(0xFFFAF8FF),
                primaryAction = Color(0xFF635091),
                onPrimaryAction = Color(0xFFFFFFFF),
                selectedSurface = Color(0xFFE9E3F3),
                onSelectedSurface = Color(0xFF2E2442),
                divider = Color(0xFF7F778A),
                essentialBoundary = Color(0xFF7F778A),
                focus = Color(0xFF635091),
                disabledSurface = Color(0xFFEAE7EE),
                disabledContent = Color(0xFF605A68),
                error = Color(0xFFA32835),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFCE9E8),
                onErrorContainer = Color(0xFF64161E),
                warning = Color(0xFF795700),
                onWarning = Color(0xFFFFFFFF),
                warningContainer = Color(0xFFFFF1C5),
                onWarningContainer = Color(0xFF4E3B00),
                success = Color(0xFF23613A),
                onSuccess = Color(0xFFFFFFFF),
                successContainer = Color(0xFFE4F1E5),
                onSuccessContainer = Color(0xFF173E25),
                scrimColor = Color(0xFF0D0714),
                scrimAlpha = 0.54f,
            ),
        darkColors =
            HausColorPalette(
                background = Color(0xFF191720),
                surface = Color(0xFF242130),
                raisedSurface = Color(0xFF322D40),
                navigationSurface = Color(0xFF292537),
                textPrimary = Color(0xFFEEEAF6),
                textSecondary = Color(0xFFC4BDD2),
                textInverse = Color(0xFF28242F),
                primaryAction = Color(0xFFD5C2FF),
                onPrimaryAction = Color(0xFF2D2050),
                selectedSurface = Color(0xFF3F3651),
                onSelectedSurface = Color(0xFFF5F0FF),
                divider = Color(0xFF898198),
                essentialBoundary = Color(0xFF898198),
                focus = Color(0xFFD5C2FF),
                disabledSurface = Color(0xFF383440),
                disabledContent = Color(0xFFBDB5CB),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF641219),
                errorContainer = Color(0xFF67272B),
                onErrorContainer = Color(0xFFFFE1DD),
                warning = Color(0xFFFFE18E),
                onWarning = Color(0xFF352800),
                warningContainer = Color(0xFF544000),
                onWarningContainer = Color(0xFFFFEAB8),
                success = Color(0xFFA5E2A7),
                onSuccess = Color(0xFF12391D),
                successContainer = Color(0xFF204B2A),
                onSuccessContainer = Color(0xFFD4F4D0),
                scrimColor = Color(0xFF000000),
                scrimAlpha = 0.64f,
            ),
    ),
    ;

    /** Palette serialization and settings helpers. */
    public companion object {
        /** Curated palette options in their stable settings order. */
        public val settingsOptions: List<HausPaletteId> =
            listOf(Moss, Bay, Clay, Iris)

        /** Parses a persisted palette identity, falling back to Moss. */
        public fun fromSerialized(value: String?): HausPaletteId =
            when (value) {
                Moss.serialized -> Moss
                Bay.serialized -> Bay
                Clay.serialized -> Clay
                Iris.serialized -> Iris
                else -> Moss
            }
    }
}

/** Complete semantic color roles for one curated palette and appearance. */
public data class HausColorPalette(
    /** App background color. */
    public val background: Color,
    /** Standard surface color. */
    public val surface: Color,
    /** Emphasized surface color for raised containers. */
    public val raisedSurface: Color,
    /** Surface color for navigation chrome. */
    public val navigationSurface: Color,
    /** Primary text and icon color. */
    public val textPrimary: Color,
    /** Secondary text and icon color. */
    public val textSecondary: Color,
    /** Content color used on the inverse surface. */
    public val textInverse: Color,
    /** Primary action and accent color. */
    public val primaryAction: Color,
    /** Content color for primary actions. */
    public val onPrimaryAction: Color,
    /** Surface color for selected or active items. */
    public val selectedSurface: Color,
    /** Content color on selected surfaces. */
    public val onSelectedSurface: Color,
    /** Subtle divider color. */
    public val divider: Color,
    /** Essential interaction boundary and outline color. */
    public val essentialBoundary: Color,
    /** Focus-indicator color for keyboard and accessibility focus. */
    public val focus: Color,
    /** Surface color for disabled controls. */
    public val disabledSurface: Color,
    /** Content color for disabled controls. */
    public val disabledContent: Color,
    /** Error text and destructive-action color. */
    public val error: Color,
    /** Content color on error-colored areas. */
    public val onError: Color,
    /** Container color for error and destructive surfaces. */
    public val errorContainer: Color,
    /** Content color on error containers. */
    public val onErrorContainer: Color,
    /** Warning text and icon color. */
    public val warning: Color,
    /** Content color on warning-colored areas. */
    public val onWarning: Color,
    /** Container color for warning surfaces. */
    public val warningContainer: Color,
    /** Content color on warning containers. */
    public val onWarningContainer: Color,
    /** Success text and icon color. */
    public val success: Color,
    /** Content color on success-colored areas. */
    public val onSuccess: Color,
    /** Container color for success surfaces. */
    public val successContainer: Color,
    /** Content color on success containers. */
    public val onSuccessContainer: Color,
    /** Scrim color for modal overlays. */
    public val scrimColor: Color,
    /** Alpha applied to [scrimColor] when composing modal overlays. */
    public val scrimAlpha: Float,
)

/**
 * Dynamic roles supplied by a supported platform, independent of Android APIs.
 */
public data class HausDynamicColorRoles(
    /** Platform background color. */
    public val background: Color,
    /** Platform standard surface color. */
    public val surface: Color,
    /** Platform emphasized surface color for raised containers. */
    public val raisedSurface: Color,
    /** Platform surface color for navigation chrome. */
    public val navigationSurface: Color,
    /** Platform primary text color. */
    public val textPrimary: Color,
    /** Platform secondary text color. */
    public val textSecondary: Color,
    /** Platform content color used on the inverse surface. */
    public val textInverse: Color,
    /** Platform primary action color. */
    public val primaryAction: Color,
    /** Platform content color for primary actions. */
    public val onPrimaryAction: Color,
    /** Platform surface color for selected items. */
    public val selectedSurface: Color,
    /** Platform content color on selected surfaces. */
    public val onSelectedSurface: Color,
    /** Platform divider color. */
    public val divider: Color,
    /** Platform essential boundary color. */
    public val essentialBoundary: Color,
    /** Platform focus-indicator color. */
    public val focus: Color,
    /** Platform error color. */
    public val error: Color,
    /** Platform content color on error-colored areas. */
    public val onError: Color,
    /** Platform error container color. */
    public val errorContainer: Color,
    /** Platform content color on error containers. */
    public val onErrorContainer: Color,
)

/**
 * Origin of the effective color roles after resolving requested preferences.
 */
public enum class HausThemeColorSource {
    Curated,
    AndroidDynamic,
}

/**
 * One immutable projection of requested appearance and resolved color roles.
 */
public data class ResolvedHausTheme(
    /** Curated palette that supplied the fallback roles and stable identity. */
    public val paletteId: HausPaletteId,
    /** Appearance mode requested by the user. */
    public val requestedMode: RhythHausThemeMode,
    /** Appearance resolved once for this projection. */
    public val isDark: Boolean,
    /** Origin of the effective color roles. */
    public val source: HausThemeColorSource,
    /** Complete effective color roles. */
    public val colors: HausColorPalette,
)

/**
 * Returns the stable curated palette object for one palette and resolved mode.
 */
public fun hausPalette(
    paletteId: HausPaletteId,
    isDark: Boolean,
): HausColorPalette =
    if (isDark) paletteId.darkColors else paletteId.lightColors

/**
 * Resolves appearance once, optionally combining platform dynamic and curated
 * roles.
 */
public fun resolveHausTheme(
    mode: RhythHausThemeMode,
    paletteId: HausPaletteId,
    systemIsDark: Boolean,
    dynamicColorRequested: Boolean = false,
    dynamicRoles: HausDynamicColorRoles? = null,
): ResolvedHausTheme {
    val isDark = mode.resolveIsDark(systemIsDark)
    val curated = hausPalette(paletteId, isDark)
    val dynamic = dynamicRoles.takeIf { dynamicColorRequested }
    val colors =
        dynamic?.let { roles ->
            curated.copy(
                background = roles.background,
                surface = roles.surface,
                raisedSurface = roles.raisedSurface,
                navigationSurface = roles.navigationSurface,
                textPrimary = roles.textPrimary,
                textSecondary = roles.textSecondary,
                textInverse = roles.textInverse,
                primaryAction = roles.primaryAction,
                onPrimaryAction = roles.onPrimaryAction,
                selectedSurface = roles.selectedSurface,
                onSelectedSurface = roles.onSelectedSurface,
                divider = roles.divider,
                essentialBoundary = roles.essentialBoundary,
                focus = roles.focus,
                error = roles.error,
                onError = roles.onError,
                errorContainer = roles.errorContainer,
                onErrorContainer = roles.onErrorContainer,
            )
        } ?: curated

    return ResolvedHausTheme(
        paletteId = paletteId,
        requestedMode = mode,
        isDark = isDark,
        source =
            if (dynamic == null) HausThemeColorSource.Curated
            else HausThemeColorSource.AndroidDynamic,
        colors = colors,
    )
}
