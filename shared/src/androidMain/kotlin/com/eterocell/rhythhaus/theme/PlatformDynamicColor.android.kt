package com.eterocell.rhythhaus.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

actual fun isPlatformDynamicColorAvailable(): Boolean =
    androidDynamicColorSupported(Build.VERSION.SDK_INT)

@Composable
actual fun platformDynamicColorRoles(isDark: Boolean): HausDynamicColorRoles? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(context, configuration, isDark) {
        resolveAndroidDynamicColorRoles(
            apiLevel = Build.VERSION.SDK_INT,
            isDark = isDark,
        ) { useDarkScheme ->
            context.dynamicColorScheme(useDarkScheme)
        }
    }
}

private fun Context.dynamicColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) {
        dynamicDarkColorScheme(this)
    } else {
        dynamicLightColorScheme(this)
    }

internal fun androidDynamicColorSupported(apiLevel: Int): Boolean =
    apiLevel >= Build.VERSION_CODES.S

internal fun resolveAndroidDynamicColorRoles(
    apiLevel: Int,
    isDark: Boolean,
    dynamicSchemeProvider: (isDark: Boolean) -> ColorScheme,
): HausDynamicColorRoles? {
    if (!androidDynamicColorSupported(apiLevel)) return null

    return try {
        androidDynamicSchemeToHaus(dynamicSchemeProvider(isDark))
    } catch (_: Exception) {
        null
    }
}

internal fun androidDynamicSchemeToHaus(
    scheme: ColorScheme
): HausDynamicColorRoles =
    HausDynamicColorRoles(
        background = scheme.background,
        surface = scheme.surface,
        raisedSurface = scheme.surfaceContainerHigh,
        navigationSurface = scheme.surfaceContainerLow,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        textInverse = scheme.inverseOnSurface,
        primaryAction = scheme.primary,
        onPrimaryAction = scheme.onPrimary,
        selectedSurface = scheme.primaryContainer,
        onSelectedSurface = scheme.onPrimaryContainer,
        divider = scheme.outlineVariant,
        essentialBoundary = scheme.outline,
        focus = scheme.primary,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
    )
