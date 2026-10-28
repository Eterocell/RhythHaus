package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryDialogDangerColorJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearConfirmationRetainsDangerContrastForBothStatesAndPalettes() =
        withEnglishLocale {
            runComposeUiTest {
                for (palette in listOf(LightHausPalette, DarkHausPalette)) {
                    for (confirmEnabled in listOf(true, false)) {
                        setContent {
                            CompositionLocalProvider(
                                LocalHausColors provides palette) {
                                    AnimatedClearLibraryDialogRoute(
                                        sourceCount = 1,
                                        trackCount = 1,
                                        confirmEnabled = confirmEnabled,
                                        onDismiss = {},
                                        onClearLibrary = {},
                                    )
                                }
                        }
                        waitForIdle()

                        val action =
                            onNodeWithTag(
                                "settings-clear-confirm",
                                useUnmergedTree = true,
                            )
                        if (confirmEnabled) action.assertIsEnabled()
                        else action.assertIsNotEnabled()
                        val pixels = action.captureToImage().toPixelMap()
                        val colorCounts = mutableMapOf<Int, Int>()
                        for (y in 0 until pixels.height) {
                            for (x in 0 until pixels.width) {
                                val color = pixels[x, y].toArgb()
                                colorCounts[color] =
                                    (colorCounts[color] ?: 0) + 1
                            }
                        }
                        val dominantColor = colorCounts.maxBy { it.value }.key

                        assertEquals(
                            if (confirmEnabled) palette.danger.toArgb()
                            else palette.dangerPanel.toArgb(),
                            dominantColor,
                            "Clear confirmation should retain its danger surface",
                        )
                        val expectedText =
                            if (confirmEnabled) palette.onDanger
                            else palette.danger
                        assertTrue(
                            colorCounts.containsKey(expectedText.toArgb()),
                            "Clear confirmation should retain contrasting danger text",
                        )
                    }
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearAndRemoveMissingActionsMeetTargetMinimumAtLargeFontScale() =
        withEnglishLocale {
            runComposeUiTest {
                for (fontScale in listOf(1f, 3f)) {
                    val requiredHeight = if (fontScale >= 3f) 60.dp else 48.dp
                    setContent {
                        val currentDensity = LocalDensity.current
                        CompositionLocalProvider(
                            LocalDensity provides
                                Density(currentDensity.density, fontScale),
                        ) {
                            AnimatedClearLibraryDialogRoute(
                                sourceCount = 1,
                                trackCount = 2,
                                confirmEnabled = true,
                                onDismiss = {},
                                onClearLibrary = {},
                            )
                        }
                    }
                    waitForIdle()

                    onNodeWithTag(
                            "settings-clear-dismiss", useUnmergedTree = true)
                        .assertHeightIsAtLeast(requiredHeight)
                    onNodeWithTag(
                            "settings-clear-confirm", useUnmergedTree = true)
                        .assertHeightIsAtLeast(requiredHeight)
                    onNodeWithText("Cancel", useUnmergedTree = true)
                        .assertIsDisplayed()
                    onNodeWithText("Clear", useUnmergedTree = true)
                        .assertIsDisplayed()

                    setContent {
                        val currentDensity = LocalDensity.current
                        CompositionLocalProvider(
                            LocalDensity provides
                                Density(currentDensity.density, fontScale),
                        ) {
                            AnimatedRemoveMissingTracksDialogRoute(
                                sourceName = "Music",
                                trackCount = 2,
                                confirmEnabled = true,
                                onDismiss = {},
                                onRemoveMissingTracks = {},
                            )
                        }
                    }
                    waitForIdle()

                    onNodeWithTag(
                            "settings-remove-missing-dismiss",
                            useUnmergedTree = true)
                        .assertHeightIsAtLeast(requiredHeight)
                    onNodeWithTag(
                            "settings-remove-missing-confirm",
                            useUnmergedTree = true)
                        .assertHeightIsAtLeast(requiredHeight)
                    onNodeWithText("Cancel", useUnmergedTree = true)
                        .assertIsDisplayed()
                    onNodeWithText("Remove", useUnmergedTree = true)
                        .assertIsDisplayed()
                }
            }
        }
}

private inline fun withEnglishLocale(block: () -> Unit) {
    val originalLocale = Locale.getDefault()
    Locale.setDefault(Locale.ENGLISH)
    try {
        block()
    } finally {
        Locale.setDefault(originalLocale)
    }
}
