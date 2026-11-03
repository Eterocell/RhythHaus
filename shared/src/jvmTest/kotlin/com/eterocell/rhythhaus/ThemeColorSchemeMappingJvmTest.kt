package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.hausPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeColorSchemeMappingJvmTest {
    @Test
    fun miuixSchemeUsesCanonicalSurfaceAndDisabledRolesInBothAppearances() {
        listOf(false, true).forEach { isDark ->
            val colors = hausPalette(HausPaletteId.Moss, isDark)
            val scheme = hausMiuixColorScheme(colors, isDark)

            assertEquals(colors.primaryAction, scheme.primary)
            assertEquals(colors.onPrimaryAction, scheme.onPrimary)
            assertEquals(colors.primaryAction, scheme.secondary)
            assertEquals(colors.onPrimaryAction, scheme.onSecondary)
            assertEquals(colors.background, scheme.background)
            assertEquals(colors.textPrimary, scheme.onBackground)
            assertEquals(colors.surface, scheme.surface)
            assertEquals(colors.textPrimary, scheme.onSurface)
            assertEquals(colors.raisedSurface, scheme.surfaceContainer)
            assertEquals(colors.textPrimary, scheme.onSurfaceContainer)
            assertEquals(colors.primaryAction, scheme.secondaryVariant)
            assertEquals(colors.onPrimaryAction, scheme.onSecondaryVariant)
            assertEquals(
                colors.disabledSurface, scheme.disabledSecondaryVariant)
            assertEquals(
                colors.disabledContent, scheme.disabledOnSecondaryVariant)
        }
    }

    @Test
    fun material3SchemeUsesCanonicalHausRoleMappingInBothAppearances() {
        listOf(false, true).forEach { isDark ->
            val colors = hausPalette(HausPaletteId.Iris, isDark)
            val scheme = hausMaterial3ColorScheme(colors, isDark)

            assertEquals(colors.primaryAction, scheme.primary)
            assertEquals(colors.onPrimaryAction, scheme.onPrimary)
            assertEquals(colors.selectedSurface, scheme.primaryContainer)
            assertEquals(colors.onSelectedSurface, scheme.onPrimaryContainer)
            assertEquals(colors.primaryAction, scheme.secondary)
            assertEquals(colors.onPrimaryAction, scheme.onSecondary)
            assertEquals(colors.raisedSurface, scheme.secondaryContainer)
            assertEquals(colors.textPrimary, scheme.onSecondaryContainer)
            assertEquals(colors.primaryAction, scheme.tertiary)
            assertEquals(colors.onPrimaryAction, scheme.onTertiary)
            assertEquals(colors.selectedSurface, scheme.tertiaryContainer)
            assertEquals(colors.onSelectedSurface, scheme.onTertiaryContainer)
            assertEquals(colors.background, scheme.background)
            assertEquals(colors.textPrimary, scheme.onBackground)
            assertEquals(colors.surface, scheme.surface)
            assertEquals(colors.textPrimary, scheme.onSurface)
            assertEquals(colors.raisedSurface, scheme.surfaceVariant)
            assertEquals(colors.textSecondary, scheme.onSurfaceVariant)
            assertEquals(colors.primaryAction, scheme.surfaceTint)
            assertEquals(colors.background, scheme.surfaceDim)
            assertEquals(colors.surface, scheme.surfaceBright)
            assertEquals(colors.background, scheme.surfaceContainerLowest)
            assertEquals(colors.surface, scheme.surfaceContainerLow)
            assertEquals(colors.raisedSurface, scheme.surfaceContainer)
            assertEquals(colors.raisedSurface, scheme.surfaceContainerHigh)
            assertEquals(colors.raisedSurface, scheme.surfaceContainerHighest)
            assertEquals(colors.essentialBoundary, scheme.outline)
            assertEquals(colors.divider, scheme.outlineVariant)
            assertEquals(colors.scrimColor, scheme.scrim)
            assertEquals(colors.error, scheme.error)
            assertEquals(colors.onError, scheme.onError)
            assertEquals(colors.errorContainer, scheme.errorContainer)
            assertEquals(colors.onErrorContainer, scheme.onErrorContainer)
            assertEquals(colors.textPrimary, scheme.inverseSurface)
            assertEquals(colors.textInverse, scheme.inverseOnSurface)
            assertEquals(colors.primaryAction, scheme.inversePrimary)
        }
    }
}
