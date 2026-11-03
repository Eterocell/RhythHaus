package com.eterocell.rhythhaus.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.eterocell.rhythhaus.theme.HausPaletteId.Bay
import com.eterocell.rhythhaus.theme.RhythHausThemeMode.Dark
import com.eterocell.rhythhaus.theme.RhythHausThemeMode.Light
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidDynamicColorProviderTest {
    @Test
    fun androidDynamicColorsAreUnavailableBelowApi31WithoutReadingTheProvider() {
        for (apiLevel in listOf(29, 30)) {
            var providerCalls = 0

            assertFalse(androidDynamicColorSupported(apiLevel))
            assertNull(
                resolveAndroidDynamicColorRoles(apiLevel, isDark = false) {
                    providerCalls += 1
                    platformScheme()
                },
            )
            assertEquals(0, providerCalls)
        }

        assertTrue(androidDynamicColorSupported(31))
    }

    @Test
    fun androidDynamicSchemeMapsOnlyTheApprovedHausRoles() {
        val scheme = platformScheme()

        val roles = androidDynamicSchemeToHaus(scheme)

        assertEquals(Color(0xFF000001), roles.background)
        assertEquals(Color(0xFF000002), roles.surface)
        assertEquals(Color(0xFF000003), roles.raisedSurface)
        assertEquals(Color(0xFF000004), roles.navigationSurface)
        assertEquals(Color(0xFF000005), roles.textPrimary)
        assertEquals(Color(0xFF000006), roles.textSecondary)
        assertEquals(Color(0xFF000007), roles.textInverse)
        assertEquals(Color(0xFF000008), roles.primaryAction)
        assertEquals(Color(0xFF000009), roles.onPrimaryAction)
        assertEquals(Color(0xFF000010), roles.selectedSurface)
        assertEquals(Color(0xFF000011), roles.onSelectedSurface)
        assertEquals(Color(0xFF000012), roles.divider)
        assertEquals(Color(0xFF000013), roles.essentialBoundary)
        assertEquals(Color(0xFF000008), roles.focus)
        assertEquals(Color(0xFF000014), roles.error)
        assertEquals(Color(0xFF000015), roles.onError)
        assertEquals(Color(0xFF000016), roles.errorContainer)
        assertEquals(Color(0xFF000017), roles.onErrorContainer)
    }

    @Test
    fun androidDynamicProviderUsesTheResolvedLightOrDarkScheme() {
        val lightScheme = platformScheme(primary = Color(0xFF101010))
        val darkScheme = platformScheme(primary = Color(0xFF202020))
        val requestedModes = mutableListOf<Boolean>()

        val lightRoles =
            resolveAndroidDynamicColorRoles(apiLevel = 31, isDark = false) {
                isDark ->
                requestedModes += isDark
                lightScheme
            }
        val darkRoles =
            resolveAndroidDynamicColorRoles(apiLevel = 31, isDark = true) {
                isDark ->
                requestedModes += isDark
                darkScheme
            }

        assertEquals(listOf(false, true), requestedModes)
        assertEquals(Color(0xFF101010), lightRoles?.primaryAction)
        assertEquals(Color(0xFF202020), darkRoles?.primaryAction)
    }

    @Test
    fun providerFailureResolvesTheSelectedCuratedScheme() {
        val dynamicRoles =
            resolveAndroidDynamicColorRoles(apiLevel = 31, isDark = true) {
                error("platform dynamic scheme unavailable")
            }

        val resolved =
            resolveHausTheme(
                mode = Dark,
                paletteId = Bay,
                systemIsDark = false,
                dynamicColorRequested = true,
                dynamicRoles = dynamicRoles,
            )
        val curated = hausPalette(Bay, isDark = true)

        assertEquals(HausThemeColorSource.Curated, resolved.source)
        assertEquals(curated, resolved.colors)
        assertTrue(resolved.isDark)
    }

    @Test
    fun dynamicProjectionKeepsWarningSuccessAndScrimRolesCurated() {
        val dynamicRoles = androidDynamicSchemeToHaus(platformScheme())
        val curated = hausPalette(Bay, isDark = false)
        val resolved =
            resolveHausTheme(
                mode = Light,
                paletteId = Bay,
                systemIsDark = false,
                dynamicColorRequested = true,
                dynamicRoles = dynamicRoles,
            )

        assertEquals(HausThemeColorSource.AndroidDynamic, resolved.source)
        assertEquals(curated.warning, resolved.colors.warning)
        assertEquals(curated.onWarning, resolved.colors.onWarning)
        assertEquals(curated.warningContainer, resolved.colors.warningContainer)
        assertEquals(
            curated.onWarningContainer, resolved.colors.onWarningContainer)
        assertEquals(curated.success, resolved.colors.success)
        assertEquals(curated.onSuccess, resolved.colors.onSuccess)
        assertEquals(curated.successContainer, resolved.colors.successContainer)
        assertEquals(
            curated.onSuccessContainer, resolved.colors.onSuccessContainer)
        assertEquals(curated.disabledSurface, resolved.colors.disabledSurface)
        assertEquals(curated.disabledContent, resolved.colors.disabledContent)
        assertEquals(curated.scrimColor, resolved.colors.scrimColor)
        assertEquals(curated.scrimAlpha, resolved.colors.scrimAlpha)
    }
}

private fun platformScheme(
    primary: Color = Color(0xFF000008),
): ColorScheme =
    lightColorScheme(
        background = Color(0xFF000001),
        surface = Color(0xFF000002),
        surfaceContainerHigh = Color(0xFF000003),
        surfaceContainerLow = Color(0xFF000004),
        onSurface = Color(0xFF000005),
        onSurfaceVariant = Color(0xFF000006),
        inverseOnSurface = Color(0xFF000007),
        primary = primary,
        onPrimary = Color(0xFF000009),
        primaryContainer = Color(0xFF000010),
        onPrimaryContainer = Color(0xFF000011),
        outlineVariant = Color(0xFF000012),
        outline = Color(0xFF000013),
        error = Color(0xFF000014),
        onError = Color(0xFF000015),
        errorContainer = Color(0xFF000016),
        onErrorContainer = Color(0xFF000017),
    )
