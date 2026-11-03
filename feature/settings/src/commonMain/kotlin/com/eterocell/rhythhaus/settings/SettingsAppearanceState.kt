package com.eterocell.rhythhaus.settings

import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.HausThemeColorSource
import com.eterocell.rhythhaus.theme.RhythHausThemeMode

/**
 * Immutable persisted-and-resolved appearance supplied by Shared to Settings.
 */
public data class SettingsAppearanceState(
    /** User-requested appearance mode. */
    public val themeMode: RhythHausThemeMode,
    /** Persisted curated palette, retained while Monet is requested. */
    public val paletteId: HausPaletteId,
    /** Effective light/dark appearance after resolving system mode. */
    public val isDark: Boolean,
    /** Whether this host can provide Android Monet colors. */
    public val dynamicColorAvailable: Boolean,
    /** Persisted Monet request, independent from palette selection. */
    public val dynamicColorEnabled: Boolean,
    /** Origin of the color roles currently applied by the app. */
    public val colorSource: HausThemeColorSource,
    /** Whether a preference write is in flight. */
    public val isSaving: Boolean,
    /** Whether the most recent preference write can be retried. */
    public val saveFailed: Boolean,
)
