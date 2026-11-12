package com.eterocell.rhythhaus

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme as darkMaterial3ColorScheme
import androidx.compose.material3.lightColorScheme as lightMaterial3ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.systemui.AppSystemBarAppearance
import com.eterocell.rhythhaus.theme.HausColorPalette
import com.eterocell.rhythhaus.theme.HausDynamicColorRoles
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.ResolvedHausTheme
import com.eterocell.rhythhaus.theme.ThemePreferenceState
import com.eterocell.rhythhaus.theme.resolveHausTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

internal fun resolveSharedHausTheme(
    preferences: ThemePreferenceState,
    systemIsDark: Boolean,
    dynamicColorAvailable: Boolean,
    dynamicRoles: HausDynamicColorRoles?,
): ResolvedHausTheme =
    resolveHausTheme(
        mode = preferences.mode,
        paletteId = preferences.paletteId,
        systemIsDark = systemIsDark,
        dynamicColorRequested =
            preferences.dynamicColorEnabled && dynamicColorAvailable,
        dynamicRoles = dynamicRoles,
    )

@Composable
internal fun RhythHausTheme(
    resolved: ResolvedHausTheme,
    content: @Composable () -> Unit,
) {
    val colors = resolved.colors
    val miuixScheme =
        remember(colors, resolved.isDark) {
            hausMiuixColorScheme(colors, resolved.isDark)
        }
    val material3Scheme =
        remember(colors, resolved.isDark) {
            hausMaterial3ColorScheme(colors, resolved.isDark)
        }

    MaterialTheme(colorScheme = material3Scheme) {
        MiuixTheme(colors = miuixScheme) {
            CompositionLocalProvider(LocalHausColors provides colors) {
                AppSystemBarAppearance(
                    statusBarSurface = colors.background,
                    navigationBarSurface = colors.background,
                )
                content()
            }
        }
    }
}

/**
 * Maps the effective Haus roles into the Miuix scheme for one resolved mode.
 */
internal fun hausMiuixColorScheme(
    colors: HausColorPalette,
    isDark: Boolean,
) =
    if (isDark) {
        darkColorScheme(
            primary = colors.primaryAction,
            onPrimary = colors.onPrimaryAction,
            secondary = colors.primaryAction,
            onSecondary = colors.onPrimaryAction,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceContainer = colors.raisedSurface,
            onSurfaceContainer = colors.textPrimary,
            secondaryVariant = colors.primaryAction,
            onSecondaryVariant = colors.onPrimaryAction,
            disabledSecondaryVariant = colors.disabledSurface,
            disabledOnSecondaryVariant = colors.disabledContent,
        )
    } else {
        lightColorScheme(
            primary = colors.primaryAction,
            onPrimary = colors.onPrimaryAction,
            secondary = colors.primaryAction,
            onSecondary = colors.onPrimaryAction,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceContainer = colors.raisedSurface,
            onSurfaceContainer = colors.textPrimary,
            secondaryVariant = colors.primaryAction,
            onSecondaryVariant = colors.onPrimaryAction,
            disabledSecondaryVariant = colors.disabledSurface,
            disabledOnSecondaryVariant = colors.disabledContent,
        )
    }

/** Maps the canonical Haus roles into Material3 for one resolved mode. */
internal fun hausMaterial3ColorScheme(
    colors: HausColorPalette,
    isDark: Boolean,
) =
    if (isDark) {
        darkMaterial3ColorScheme(
            primary = colors.primaryAction,
            onPrimary = colors.onPrimaryAction,
            primaryContainer = colors.selectedSurface,
            onPrimaryContainer = colors.onSelectedSurface,
            inversePrimary = colors.primaryAction,
            secondary = colors.primaryAction,
            onSecondary = colors.onPrimaryAction,
            secondaryContainer = colors.raisedSurface,
            onSecondaryContainer = colors.textPrimary,
            tertiary = colors.primaryAction,
            onTertiary = colors.onPrimaryAction,
            tertiaryContainer = colors.selectedSurface,
            onTertiaryContainer = colors.onSelectedSurface,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.raisedSurface,
            onSurfaceVariant = colors.textSecondary,
            surfaceTint = colors.primaryAction,
            inverseSurface = colors.textPrimary,
            inverseOnSurface = colors.textInverse,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            outline = colors.essentialBoundary,
            outlineVariant = colors.divider,
            scrim = colors.scrimColor,
            surfaceBright = colors.surface,
            surfaceDim = colors.background,
            surfaceContainer = colors.raisedSurface,
            surfaceContainerHigh = colors.raisedSurface,
            surfaceContainerHighest = colors.raisedSurface,
            surfaceContainerLow = colors.surface,
            surfaceContainerLowest = colors.background,
        )
    } else {
        lightMaterial3ColorScheme(
            primary = colors.primaryAction,
            onPrimary = colors.onPrimaryAction,
            primaryContainer = colors.selectedSurface,
            onPrimaryContainer = colors.onSelectedSurface,
            inversePrimary = colors.primaryAction,
            secondary = colors.primaryAction,
            onSecondary = colors.onPrimaryAction,
            secondaryContainer = colors.raisedSurface,
            onSecondaryContainer = colors.textPrimary,
            tertiary = colors.primaryAction,
            onTertiary = colors.onPrimaryAction,
            tertiaryContainer = colors.selectedSurface,
            onTertiaryContainer = colors.onSelectedSurface,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.raisedSurface,
            onSurfaceVariant = colors.textSecondary,
            surfaceTint = colors.primaryAction,
            inverseSurface = colors.textPrimary,
            inverseOnSurface = colors.textInverse,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            outline = colors.essentialBoundary,
            outlineVariant = colors.divider,
            scrim = colors.scrimColor,
            surfaceBright = colors.surface,
            surfaceDim = colors.background,
            surfaceContainer = colors.raisedSurface,
            surfaceContainerHigh = colors.raisedSurface,
            surfaceContainerHighest = colors.raisedSurface,
            surfaceContainerLow = colors.surface,
            surfaceContainerLowest = colors.background,
        )
    }
