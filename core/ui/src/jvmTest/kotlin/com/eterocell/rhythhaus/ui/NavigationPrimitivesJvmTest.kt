package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationPrimitivesJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backChipUsesExistingLabelDescriptionAndClickContract() =
        withEnglishLocale {
            runComposeUiTest {
                var clicks = 0
                setContent { BackChip(onClick = { clicks += 1 }) }
                onNodeWithText("‹ Back")
                    .assertExists()
                    .assertTextEquals("‹ Back")
                onNodeWithContentDescription("Back")
                    .assertWidthIsAtLeast(44.dp)
                    .assertHeightIsAtLeast(44.dp)
                    .performClick()
                assertEquals(1, clicks)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun topAppBarShowsClickableBackOnlyWhenCallbackExists() =
        withEnglishLocale {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    RhythHausTopAppBar(
                        title = "Title", onBack = { clicks += 1 })
                }
                onNodeWithText("Title").assertExists()
                onNodeWithContentDescription("Back")
                    .assertWidthIsAtLeast(44.dp)
                    .assertHeightIsAtLeast(44.dp)
                    .performClick()
                assertEquals(1, clicks)
                setContent {
                    RhythHausTopAppBar(title = "Title", onBack = null)
                }
                onNodeWithContentDescription("Back").assertDoesNotExist()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun topAppBarBackButtonShowsFocusOutline() = withEnglishLocale {
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalHausColors provides LightHausPalette) {
                        RhythHausTopAppBar(title = "Title", onBack = {})
                    }
            }

            val back = onNodeWithContentDescription("Back")
            back.performSemanticsAction(SemanticsActions.RequestFocus)
            back.assertIsFocused()
            val pixels = back.captureToImage().toPixelMap()
            val outline = LightHausPalette.outline.toArgb()
            assertTrue(
                (0 until pixels.height).any { y ->
                    (0 until pixels.width).any { x ->
                        pixels[x, y].toArgb() == outline
                    }
                },
                "Focused top-bar Back action should show the theme outline",
            )
        }
    }

    private fun withEnglishLocale(block: () -> Unit): Unit {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
