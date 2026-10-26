package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import kotlin.test.Test
import kotlin.test.assertTrue

class HausClickableJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusedClickableDrawsTheActivePaletteOutline() = runComposeUiTest {
        val focusRequester = FocusRequester()
        var palette by mutableStateOf(LightHausPalette)
        setContent {
            CompositionLocalProvider(LocalHausColors provides palette) {
                Box(
                    Modifier.testTag("focusable-action")
                        .size(48.dp)
                        .background(palette.panel)
                        .focusRequester(focusRequester)
                        .hausClickable {},
                )
            }
        }

        runOnIdle { focusRequester.requestFocus() }
        val focusedAction = onNodeWithTag("focusable-action")
        focusedAction.assertIsFocused()
        assertContainsFocusColor(focusedAction, LightHausPalette.outline)

        runOnIdle { palette = DarkHausPalette }
        waitForIdle()
        assertContainsFocusColor(focusedAction, DarkHausPalette.outline)
    }

    private fun assertContainsFocusColor(
        node: SemanticsNodeInteraction,
        expected: Color,
    ) {
        val pixels = node.captureToImage().toPixelMap()
        val expectedArgb = expected.toArgb()
        assertTrue(
            (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x ->
                    pixels[x, y].toArgb() == expectedArgb
                }
            },
            "Focused clickable should draw the palette outline",
        )
    }
}
