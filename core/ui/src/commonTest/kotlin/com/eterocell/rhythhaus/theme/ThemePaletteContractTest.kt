package com.eterocell.rhythhaus.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemePaletteContractTest {
    @Test
    fun paletteIdsHaveStableValuesAndUnknownValuesFallBackToMoss() {
        assertEquals("moss", HausPaletteId.Moss.serialized)
        assertEquals("bay", HausPaletteId.Bay.serialized)
        assertEquals("clay", HausPaletteId.Clay.serialized)
        assertEquals("iris", HausPaletteId.Iris.serialized)
        assertEquals(HausPaletteId.Moss, HausPaletteId.fromSerialized(null))
        assertEquals(HausPaletteId.Moss, HausPaletteId.fromSerialized(""))
        assertEquals(
            HausPaletteId.Moss,
            HausPaletteId.fromSerialized("future-palette"),
        )
    }

    @Test
    fun allEightCuratedSchemesContainEveryOpaqueRoleAndScrimAlpha() {
        val schemes =
            HausPaletteId.entries.flatMap { paletteId ->
                listOf(
                    "$paletteId light" to
                        hausPalette(paletteId, isDark = false),
                    "$paletteId dark" to hausPalette(paletteId, isDark = true),
                )
            }

        assertEquals(8, schemes.size)
        schemes.forEach { (name, palette) ->
            val roles = palette.semanticColorRoles()
            assertEquals(29, roles.size, "$name must contain 29 color roles")
            roles.forEachIndexed { index, color ->
                assertTrue(
                    color.alpha == 1f, "$name role $index must be opaque")
            }
            assertTrue(palette.scrimAlpha in 0f..1f, "$name scrim alpha")
        }
    }

    @Test
    fun curatedSchemesUseTheApprovedPaletteAndModeTokens() {
        val expected =
            listOf(
                Triple(HausPaletteId.Moss, false, Color(0xFFF4F7F2)) to
                    (Color(0xFF315D3E) to 0.54f),
                Triple(HausPaletteId.Moss, true, Color(0xFF151B16)) to
                    (Color(0xFF98D9A4) to 0.64f),
                Triple(HausPaletteId.Bay, false, Color(0xFFF2F7FA)) to
                    (Color(0xFF285D83) to 0.54f),
                Triple(HausPaletteId.Bay, true, Color(0xFF121A20)) to
                    (Color(0xFF98D1FF) to 0.64f),
                Triple(HausPaletteId.Clay, false, Color(0xFFFBF6F0)) to
                    (Color(0xFF89502C) to 0.54f),
                Triple(HausPaletteId.Clay, true, Color(0xFF1D1714)) to
                    (Color(0xFFFFD0AB) to 0.64f),
                Triple(HausPaletteId.Iris, false, Color(0xFFF7F5FB)) to
                    (Color(0xFF635091) to 0.54f),
                Triple(HausPaletteId.Iris, true, Color(0xFF191720)) to
                    (Color(0xFFD5C2FF) to 0.64f),
            )

        expected.forEach { (scheme, expectedAction) ->
            val (paletteId, isDark, expectedBackground) = scheme
            val (expectedActionColor, expectedScrimAlpha) = expectedAction
            val palette = hausPalette(paletteId, isDark)

            assertEquals(
                expectedBackground,
                palette.background,
                "$paletteId/$isDark background")
            assertEquals(
                expectedActionColor,
                palette.primaryAction,
                "$paletteId/$isDark action")
            assertEquals(
                expectedScrimAlpha,
                palette.scrimAlpha,
                "$paletteId/$isDark scrim")
        }
    }

    @Test
    fun paletteSelectionDoesNotChangeRequestedMode() {
        val resolved =
            resolveHausTheme(
                mode = RhythHausThemeMode.Dark,
                paletteId = HausPaletteId.Bay,
                systemIsDark = false,
            )

        assertEquals(HausPaletteId.Bay, resolved.paletteId)
        assertEquals(RhythHausThemeMode.Dark, resolved.requestedMode)
        assertTrue(resolved.isDark)
        assertEquals(
            hausPalette(HausPaletteId.Bay, isDark = true), resolved.colors)
    }

    @Test
    fun systemModeResolvesDarknessOnceAndExplicitModesIgnoreTheSystem() {
        val systemLight =
            resolveHausTheme(
                RhythHausThemeMode.System,
                HausPaletteId.Clay,
                systemIsDark = false,
            )
        val systemDark =
            resolveHausTheme(
                RhythHausThemeMode.System,
                HausPaletteId.Clay,
                systemIsDark = true,
            )
        val explicitLight =
            resolveHausTheme(
                RhythHausThemeMode.Light,
                HausPaletteId.Clay,
                systemIsDark = true,
            )
        val explicitDark =
            resolveHausTheme(
                RhythHausThemeMode.Dark,
                HausPaletteId.Clay,
                systemIsDark = false,
            )

        assertEquals(false, systemLight.isDark)
        assertEquals(
            hausPalette(HausPaletteId.Clay, isDark = false), systemLight.colors)
        assertEquals(true, systemDark.isDark)
        assertEquals(
            hausPalette(HausPaletteId.Clay, isDark = true), systemDark.colors)
        assertEquals(false, explicitLight.isDark)
        assertEquals(true, explicitDark.isDark)
    }

    @Test
    fun dynamicRolesOverrideOnlyMappedRolesAndRetainCuratedStatusRoles() {
        val dynamic =
            HausDynamicColorRoles(
                background = Color(0xFF010101),
                surface = Color(0xFF020202),
                raisedSurface = Color(0xFF030303),
                navigationSurface = Color(0xFF040404),
                textPrimary = Color(0xFF050505),
                textSecondary = Color(0xFF060606),
                textInverse = Color(0xFF070707),
                primaryAction = Color(0xFF080808),
                onPrimaryAction = Color(0xFF090909),
                selectedSurface = Color(0xFF0A0A0A),
                onSelectedSurface = Color(0xFF0B0B0B),
                divider = Color(0xFF0C0C0C),
                essentialBoundary = Color(0xFF0D0D0D),
                focus = Color(0xFF0E0E0E),
                error = Color(0xFF0F0F0F),
                onError = Color(0xFF101010),
                errorContainer = Color(0xFF111111),
                onErrorContainer = Color(0xFF121212),
            )
        val curated = hausPalette(HausPaletteId.Iris, isDark = false)
        val resolved =
            resolveHausTheme(
                mode = RhythHausThemeMode.Light,
                paletteId = HausPaletteId.Iris,
                systemIsDark = true,
                dynamicColorRequested = true,
                dynamicRoles = dynamic,
            )

        assertEquals(HausThemeColorSource.AndroidDynamic, resolved.source)
        assertEquals(false, resolved.isDark)
        assertEquals(dynamic.background, resolved.colors.background)
        assertEquals(dynamic.surface, resolved.colors.surface)
        assertEquals(dynamic.raisedSurface, resolved.colors.raisedSurface)
        assertEquals(
            dynamic.navigationSurface, resolved.colors.navigationSurface)
        assertEquals(dynamic.textPrimary, resolved.colors.textPrimary)
        assertEquals(dynamic.textSecondary, resolved.colors.textSecondary)
        assertEquals(dynamic.textInverse, resolved.colors.textInverse)
        assertEquals(dynamic.primaryAction, resolved.colors.primaryAction)
        assertEquals(dynamic.onPrimaryAction, resolved.colors.onPrimaryAction)
        assertEquals(dynamic.selectedSurface, resolved.colors.selectedSurface)
        assertEquals(
            dynamic.onSelectedSurface, resolved.colors.onSelectedSurface)
        assertEquals(dynamic.divider, resolved.colors.divider)
        assertEquals(
            dynamic.essentialBoundary, resolved.colors.essentialBoundary)
        assertEquals(dynamic.focus, resolved.colors.focus)
        assertEquals(dynamic.error, resolved.colors.error)
        assertEquals(dynamic.onError, resolved.colors.onError)
        assertEquals(dynamic.errorContainer, resolved.colors.errorContainer)
        assertEquals(dynamic.onErrorContainer, resolved.colors.onErrorContainer)
        assertEquals(curated.warning, resolved.colors.warning)
        assertEquals(curated.onWarning, resolved.colors.onWarning)
        assertEquals(curated.warningContainer, resolved.colors.warningContainer)
        assertEquals(curated.success, resolved.colors.success)
        assertEquals(curated.onSuccess, resolved.colors.onSuccess)
        assertEquals(curated.successContainer, resolved.colors.successContainer)
        assertEquals(curated.scrimColor, resolved.colors.scrimColor)
        assertEquals(curated.scrimAlpha, resolved.colors.scrimAlpha)
    }

    @Test
    fun missingDynamicProviderUsesTheSelectedCuratedFallback() {
        val resolved =
            resolveHausTheme(
                mode = RhythHausThemeMode.System,
                paletteId = HausPaletteId.Iris,
                systemIsDark = true,
                dynamicColorRequested = true,
                dynamicRoles = null,
            )

        assertEquals(HausPaletteId.Iris, resolved.paletteId)
        assertEquals(HausThemeColorSource.Curated, resolved.source)
        assertTrue(resolved.isDark)
        assertEquals(
            hausPalette(HausPaletteId.Iris, isDark = true), resolved.colors)
    }

    @Test
    fun allCuratedSchemesMeetEveryTokenContrastRequirement() {
        val baseSurfaceNames =
            listOf(
                "background" to HausColorPalette::background,
                "surface" to HausColorPalette::surface,
                "raisedSurface" to HausColorPalette::raisedSurface,
                "navigationSurface" to HausColorPalette::navigationSurface,
            )

        HausPaletteId.entries.forEach { paletteId ->
            listOf(false, true).forEach { isDark ->
                val palette = hausPalette(paletteId, isDark)
                val name = "$paletteId/${if (isDark) "dark" else "light"}"

                baseSurfaceNames.forEach { (surfaceName, surface) ->
                    assertContrastAtLeast(
                        "$name textPrimary/$surfaceName",
                        palette.textPrimary,
                        surface(palette),
                        4.5,
                    )
                    assertContrastAtLeast(
                        "$name textSecondary/$surfaceName",
                        palette.textSecondary,
                        surface(palette),
                        4.5,
                    )
                    assertContrastAtLeast(
                        "$name focus/$surfaceName",
                        palette.focus,
                        surface(palette),
                        3.0,
                    )
                    assertContrastAtLeast(
                        "$name essentialBoundary/$surfaceName",
                        palette.essentialBoundary,
                        surface(palette),
                        3.0,
                    )
                    assertContrastAtLeast(
                        "$name error/$surfaceName",
                        palette.error,
                        surface(palette),
                        4.5,
                    )
                    assertContrastAtLeast(
                        "$name warning/$surfaceName",
                        palette.warning,
                        surface(palette),
                        4.5,
                    )
                    assertContrastAtLeast(
                        "$name success/$surfaceName",
                        palette.success,
                        surface(palette),
                        4.5,
                    )
                }

                assertContrastAtLeast(
                    "$name inverse surface",
                    palette.textInverse,
                    palette.textPrimary,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name action content",
                    palette.onPrimaryAction,
                    palette.primaryAction,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name selected content",
                    palette.onSelectedSurface,
                    palette.selectedSurface,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name disabled content",
                    palette.disabledContent,
                    palette.disabledSurface,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onError/error",
                    palette.onError,
                    palette.error,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onErrorContainer/errorContainer",
                    palette.onErrorContainer,
                    palette.errorContainer,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onWarning/warning",
                    palette.onWarning,
                    palette.warning,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onWarningContainer/warningContainer",
                    palette.onWarningContainer,
                    palette.warningContainer,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onSuccess/success",
                    palette.onSuccess,
                    palette.success,
                    4.5,
                )
                assertContrastAtLeast(
                    "$name onSuccessContainer/successContainer",
                    palette.onSuccessContainer,
                    palette.successContainer,
                    4.5,
                )
            }
        }
    }

    @Test
    fun scrimAlphaCompositesAgainstTheDeclaredSurface() {
        val palette = hausPalette(HausPaletteId.Moss, isDark = false)
        val composite =
            palette.scrimColor
                .copy(alpha = palette.scrimAlpha)
                .compositeOver(palette.surface)

        assertColorNear(Color(0xFF797E7B), composite)
    }
}

private fun HausColorPalette.semanticColorRoles(): List<Color> =
    listOf(
        background,
        surface,
        raisedSurface,
        navigationSurface,
        textPrimary,
        textSecondary,
        textInverse,
        primaryAction,
        onPrimaryAction,
        selectedSurface,
        onSelectedSurface,
        divider,
        essentialBoundary,
        focus,
        disabledSurface,
        disabledContent,
        error,
        onError,
        errorContainer,
        onErrorContainer,
        warning,
        onWarning,
        warningContainer,
        onWarningContainer,
        success,
        onSuccess,
        successContainer,
        onSuccessContainer,
        scrimColor,
    )

private fun assertContrastAtLeast(
    pairName: String,
    foreground: Color,
    background: Color,
    minimum: Double,
) {
    val contrast =
        (max(relativeLuminance(foreground), relativeLuminance(background)) +
            0.05) /
            (min(relativeLuminance(foreground), relativeLuminance(background)) +
                0.05)

    assertTrue(
        contrast >= minimum,
        "$pairName contrast was $contrast:1; expected at least $minimum:1",
    )
}

private fun assertColorNear(expected: Color, actual: Color) {
    val tolerance = 1f / 255f
    assertTrue(kotlin.math.abs(expected.red - actual.red) <= tolerance)
    assertTrue(kotlin.math.abs(expected.green - actual.green) <= tolerance)
    assertTrue(kotlin.math.abs(expected.blue - actual.blue) <= tolerance)
    assertTrue(kotlin.math.abs(expected.alpha - actual.alpha) <= tolerance)
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
