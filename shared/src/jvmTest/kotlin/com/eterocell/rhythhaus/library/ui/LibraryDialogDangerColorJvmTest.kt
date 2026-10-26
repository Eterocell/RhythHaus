package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
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
