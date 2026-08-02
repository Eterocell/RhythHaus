package com.eterocell.rhythhaus.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

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
