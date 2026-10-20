package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryDialogDangerColorJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearConfirmationUsesDangerForegroundInBothPalettes() =
        withEnglishLocale {
            runComposeUiTest {
                for (palette in listOf(LightHausPalette, DarkHausPalette)) {
                    setContent {
                        CompositionLocalProvider(
                            LocalHausColors provides palette) {
                                AnimatedClearLibraryDialogRoute(
                                    onDismiss = {},
                                    onClearLibrary = {},
                                )
                            }
                    }
                    waitForIdle()

                    val pixels =
                        onNodeWithText("Clear", useUnmergedTree = false)
                            .captureToImage()
                            .toPixelMap()
                    val colorCounts = mutableMapOf<Int, Int>()
                    for (y in 0 until pixels.height) {
                        for (x in 0 until pixels.width) {
                            val color = pixels[x, y].toArgb()
                            colorCounts[color] = (colorCounts[color] ?: 0) + 1
                        }
                    }
                    val dominantColor = colorCounts.maxBy { it.value }.key

                    assertEquals(
                        palette.danger.toArgb(),
                        dominantColor,
                        "Clear confirmation should use the danger role",
                    )
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
