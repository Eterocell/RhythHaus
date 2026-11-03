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
    fun paletteIdsParseStableValuesAndUnknownValuesFallBackToMoss() {
        assertEquals("moss", HausPaletteId.Moss.serialized)
        assertEquals("bay", HausPaletteId.Bay.serialized)
        assertEquals("clay", HausPaletteId.Clay.serialized)
        assertEquals("iris", HausPaletteId.Iris.serialized)
        HausPaletteId.entries.forEach { paletteId ->
            assertEquals(
                paletteId,
                HausPaletteId.fromSerialized(paletteId.serialized),
            )
        }
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
    fun curatedSchemesMatchEveryApprovedTokenRole() {
        val expected =
            listOf(
                ExpectedPalette(
                    HausPaletteId.Moss,
                    false,
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
                ),
                ExpectedPalette(
                    HausPaletteId.Moss,
                    true,
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
                ExpectedPalette(
                    HausPaletteId.Bay,
                    false,
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
                ),
                ExpectedPalette(
                    HausPaletteId.Bay,
                    true,
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
                ExpectedPalette(
                    HausPaletteId.Clay,
                    false,
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
                ),
                ExpectedPalette(
                    HausPaletteId.Clay,
                    true,
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
                ExpectedPalette(
                    HausPaletteId.Iris,
                    false,
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
                ),
                ExpectedPalette(
                    HausPaletteId.Iris,
                    true,
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
            )

        expected.forEach { (paletteId, isDark, expectedPalette) ->
            assertEquals(
                expectedPalette,
                hausPalette(paletteId, isDark),
                "$paletteId/${if (isDark) "dark" else "light"} tokens",
            )
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

private data class ExpectedPalette(
    val paletteId: HausPaletteId,
    val isDark: Boolean,
    val palette: HausColorPalette,
)

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
