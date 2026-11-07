package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.hausPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val lightHeaderPalette = hausPalette(HausPaletteId.Moss, isDark = false)

class AppShellHeaderJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedRouteHeaderRetainsHeightAcrossRoutesAtSameLayout() =
        runComposeUiTest {
            val longTitle = "Recently Added — 最近添加的音乐与专辑"
            val model =
                mutableStateOf(
                    AppShellRouteHeader(
                        title = "Library",
                        onBack = null,
                        actions = emptyList(),
                    ),
                )
            setContent {
                CompositionLocalProvider(
                    LocalHausColors provides lightHeaderPalette,
                ) {
                    AppShellHeader(
                        model = model.value,
                        layout = AppNavigationLayout.Bottom,
                        safeTopInset = 8.dp,
                        modifier =
                            Modifier.width(320.dp).testTag("SharedRouteHeader"),
                    )
                }
            }

            val header = onNodeWithTag("SharedRouteHeader")
            val initialHeight = header.fetchSemanticsNode().boundsInRoot.height
            model.value =
                AppShellRouteHeader(
                    title = longTitle,
                    onBack = {},
                    actions =
                        (1..5).map { index ->
                            AppShellHeaderAction(
                                label = "Action $index",
                                icon = Icons.Filled.Settings,
                                enabled = true,
                                onClick = {},
                            )
                        },
                )
            waitForIdle()

            assertEquals(
                initialHeight,
                header.fetchSemanticsNode().boundsInRoot.height,
                "Changing title and excess actions must not resize a route header",
            )
            onNodeWithContentDescription(longTitle).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedHeaderPreservesFullAccessibleTitleAndOverflowActions() =
        runComposeUiTest {
            val longTitle = "Recently Added — 最近添加的音乐与专辑"
            val selectedActions = mutableListOf<String>()
            val actions =
                (1..5).map { index ->
                    AppShellHeaderAction(
                        label = "Action $index",
                        icon = Icons.Filled.Settings,
                        enabled = true,
                        onClick = { selectedActions += "Action $index" },
                    )
                }
            setContent {
                CompositionLocalProvider(
                    LocalHausColors provides lightHeaderPalette,
                ) {
                    AppShellHeader(
                        model =
                            AppShellRouteHeader(
                                title = longTitle,
                                onBack = {},
                                actions = actions,
                            ),
                        layout = AppNavigationLayout.Bottom,
                        safeTopInset = 0.dp,
                        modifier =
                            Modifier.width(320.dp).testTag("SharedRouteHeader"),
                    )
                }
            }

            onNodeWithContentDescription(longTitle).assertIsDisplayed()
            val overflow = onNodeWithTag("AppShellHeaderOverflow")
            overflow.assertIsDisplayed()
            assertTrue(
                overflow
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
                    .isNotEmpty(),
                "Overflow control must have an accessible name",
            )
            val menuName =
                overflow
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
                    .single()
            overflow.performClick()
            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.PaneTitle,
                        menuName,
                    ),
                )
                .assertExists()

            val fourth = onNodeWithText("Action 4").fetchSemanticsNode()
            val fifth = onNodeWithText("Action 5").fetchSemanticsNode()
            assertTrue(
                fourth.boundsInRoot.top < fifth.boundsInRoot.top,
                "Overflow menu must preserve route action order",
            )
            onNodeWithText("Action 4").performClick()
            assertEquals(listOf("Action 4"), selectedActions)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun headerTitleAtTwofoldFontScaleIsNotVerticallyClipped() =
        runComposeUiTest {
            val fontScale = mutableStateOf(1f)
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalHausColors provides lightHeaderPalette,
                    LocalDensity provides
                        Density(
                            density = density.density,
                            fontScale = fontScale.value),
                ) {
                    AppShellHeader(
                        model =
                            AppShellRouteHeader(
                                title = "Songs",
                                onBack = {},
                                actions = emptyList(),
                            ),
                        layout = AppNavigationLayout.Bottom,
                        safeTopInset = 0.dp,
                        modifier =
                            Modifier.width(320.dp).testTag("SharedRouteHeader"),
                    )
                }
            }

            val defaultHeaderHeight =
                onNodeWithTag("SharedRouteHeader")
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .height
            fontScale.value = 2f
            waitForIdle()
            assertTrue(
                onNodeWithTag("SharedRouteHeader")
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .height > defaultHeaderHeight,
                "Header must grow for twofold system text scale rather than constrain the title to its default height",
            )
            onNodeWithTag("AppShellHeaderBack").assertHeightIsEqualTo(48.dp)

            val textLayouts = mutableListOf<TextLayoutResult>()
            onNodeWithTag("AppShellHeaderTitle").performSemanticsAction(
                SemanticsActions.GetTextLayoutResult) {
                    it(textLayouts)
                }
            val titleLayout = textLayouts.single()
            val headerBounds =
                onNodeWithTag("SharedRouteHeader")
                    .fetchSemanticsNode()
                    .boundsInRoot
            val titleBounds =
                onNodeWithTag("AppShellHeaderTitle")
                    .fetchSemanticsNode()
                    .boundsInRoot

            assertFalse(
                titleLayout.didOverflowHeight,
                "Twofold font scale must not clip the route title vertically",
            )
            assertTrue(
                titleLayout.getLineBottom(0) - titleLayout.getLineTop(0) <=
                    titleBounds.height,
                "The full title line box must fit inside its measured height",
            )
            assertTrue(
                titleBounds.top >= headerBounds.top &&
                    titleBounds.bottom <= headerBounds.bottom,
                "Scaled title must stay inside the header bounds",
            )
        }
}
