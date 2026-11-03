package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.hausPalette
import kotlin.test.Test
import kotlin.test.assertTrue

private val lightHausPalette = hausPalette(HausPaletteId.Moss, isDark = false)

class AppNavigationChromeJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusedPrimaryDestinationShowsThemeOutline() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalHausColors provides lightHausPalette) {
                    AppNavigationChrome(
                        layout = AppNavigationLayout.Bottom,
                        selectedDestination = null,
                        activeBrowseMode = null,
                        onNavigate = {},
                        onBrowseModeChange = {},
                        onSettings = {},
                    )
                }
        }

        val destination = onNodeWithTag(AppNavigationSearchTestTag)
        destination.performSemanticsAction(SemanticsActions.RequestFocus)
        destination.assertIsFocused()
        val pixels = destination.captureToImage().toPixelMap()
        val outline = lightHausPalette.focus.toArgb()
        assertTrue(
            (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x ->
                    pixels[x, y].toArgb() == outline
                }
            },
            "Focused primary navigation action should show the theme outline",
        )
    }
}
