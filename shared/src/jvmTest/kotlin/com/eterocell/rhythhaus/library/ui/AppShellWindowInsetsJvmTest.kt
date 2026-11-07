package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayVisible
import kotlin.test.Test
import kotlin.test.assertEquals

class AppShellWindowInsetsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fullScreenPlayerConsumesEveryPhysicalEdgeAndOnlyItsOwnIme() =
        runComposeUiTest {
            val overlayVisible = mutableStateOf(false)
            val ime = mutableStateOf(0.dp)
            setContent {
                Box(Modifier.size(390.dp, 768.dp)) {
                    CompositionLocalProvider(
                        LocalHausWindowOverlayVisible provides
                            overlayVisible.value) {
                            val bounds =
                                rememberAppShellWindowBounds(
                                    390f,
                                    768f,
                                    physicalInsets =
                                        WindowInsets(
                                            left = 12.dp,
                                            top = 62.dp,
                                            right = 18.dp,
                                            bottom = 34.dp),
                                    keyboardInsets =
                                        WindowInsets(bottom = ime.value),
                                )
                            NowPlayingSafeFrame(bounds) {}
                        }
                }
            }
            val frame = onNodeWithTag("NowPlayingSafeFrame")
            val before = frame.fetchSemanticsNode().boundsInRoot
            assertEquals(12f, before.left)
            assertEquals(62f, before.top)
            assertEquals(372f, before.right)
            assertEquals(734f, before.bottom)
            ime.value = 240.dp
            waitForIdle()
            assertEquals(528f, frame.fetchSemanticsNode().boundsInRoot.bottom)
            overlayVisible.value = true
            waitForIdle()
            assertEquals(before, frame.fetchSemanticsNode().boundsInRoot)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun windowSnapshotKeepsCoveredRouteOutsideSystemBarsButIgnoresOverlayIme() =
        runComposeUiTest {
            val ime = mutableStateOf(0.dp)
            setContent {
                Box(Modifier.size(390.dp, 768.dp)) {
                    CompositionLocalProvider(
                        LocalHausWindowOverlayVisible provides true) {
                            val bounds =
                                rememberAppShellWindowBounds(
                                    widthDp = 390f,
                                    heightDp = 768f,
                                    physicalInsets =
                                        WindowInsets(
                                            left = 12.dp,
                                            top = 24.dp,
                                            right = 18.dp,
                                            bottom = 34.dp),
                                    keyboardInsets =
                                        WindowInsets(bottom = ime.value),
                                )
                            AppShellLayout(
                                safeBounds = bounds,
                                navigationVisible = false,
                                selectedDestination = null,
                                activeBrowseMode = null,
                                onNavigate = {},
                                onBrowseModeChange = {},
                                onSettings = {},
                                showSettingsAction = false,
                                bottomBarContent =
                                    LibraryBottomBarContent.Hidden,
                                content = {},
                                bottomBar = { _, _ -> },
                            )
                        }
                }
            }
            val content = onNodeWithTag(AppShellContentRegionTestTag)
            val before = content.fetchSemanticsNode().boundsInRoot
            assertEquals(12f, before.left)
            assertEquals(372f, before.right)
            assertEquals(734f, before.bottom)
            ime.value = 240.dp
            waitForIdle()
            assertEquals(before, content.fetchSemanticsNode().boundsInRoot)
        }
}
