package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.hausPalette
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayVisible
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val lightHausPalette = hausPalette(HausPaletteId.Moss, isDark = false)

class AppNavigationChromeJvmTest {
    @Test
    fun navigationAndHeaderReflowAtApprovedBreakpoints() {
        assertEquals(
            AppNavigationLayout.Bottom, appNavigationLayoutFor(719.99f))
        assertEquals(AppNavigationLayout.Rail, appNavigationLayoutFor(720f))
        assertEquals(AppNavigationLayout.Rail, appNavigationLayoutFor(999.99f))
        assertEquals(
            AppNavigationLayout.Sidebar, appNavigationLayoutFor(1_000f))
    }

    @Test
    fun safeInsetsAndImeRemainDistinctInSharedWindowGeometry() {
        val bounds =
            appShellSafeBoundsFor(
                windowWidthDp = 800f,
                windowHeightDp = 768f,
                safeLeftInsetDp = 48f,
                safeTopInsetDp = 24f,
                safeRightInsetDp = 48f,
                safeBottomInsetDp = 34f,
                imeBottomInsetDp = 200f,
            )

        assertEquals(704f, bounds.widthDp)
        assertEquals(710f, bounds.heightDp)
        assertEquals(
            AppNavigationLayout.Bottom, appNavigationLayoutFor(bounds.widthDp))
        assertEquals(48.dp, bounds.leftInset)
        assertEquals(24.dp, bounds.topInset)
        assertEquals(34.dp, bounds.bottomInset)
        assertEquals(200.dp, bounds.imeBottomInset)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeHeaderTextScaleKeepsContentTopAtHeaderBottom() = runComposeUiTest {
        val title = mutableStateOf("Library")
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalHausColors provides lightHausPalette,
                LocalDensity provides
                    Density(density = density.density, fontScale = 2f),
            ) {
                Box(Modifier.size(width = 800.dp, height = 768.dp)) {
                    AppShellLayout(
                        safeBounds =
                            appShellSafeBoundsFor(
                                windowWidthDp = 800f,
                                windowHeightDp = 768f,
                                safeLeftInsetDp = 48f,
                                safeTopInsetDp = 24f,
                                safeRightInsetDp = 48f,
                                safeBottomInsetDp = 34f,
                                imeBottomInsetDp = 0f,
                            ),
                        navigationVisible = false,
                        selectedDestination = null,
                        activeBrowseMode = null,
                        onNavigate = {},
                        onBrowseModeChange = {},
                        onSettings = {},
                        showSettingsAction = false,
                        routeHeader =
                            AppShellRouteHeader(
                                title = title.value,
                                onBack = null,
                                actions = emptyList(),
                            ),
                        bottomBarContent = LibraryBottomBarContent.Hidden,
                        content = { Box(Modifier.fillMaxSize()) },
                        bottomBar = { _, _ -> },
                    )
                }
            }
        }

        val assertContentStartsAtHeaderBottom = {
            val headerBounds =
                onNodeWithTag("AppShellHeaderRoot")
                    .fetchSemanticsNode()
                    .boundsInRoot
            val contentBounds =
                onNodeWithTag(AppShellContentRegionTestTag)
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertEquals(
                headerBounds.bottom,
                contentBounds.top,
                0.5f,
                "Content region must start exactly at the header bottom",
            )
        }

        val initialHeaderHeight =
            onNodeWithTag("AppShellHeaderRoot")
                .fetchSemanticsNode()
                .boundsInRoot
                .height
        assertContentStartsAtHeaderBottom()

        title.value = "Recently Added — 最近添加的音乐与专辑"
        waitForIdle()

        assertEquals(
            initialHeaderHeight,
            onNodeWithTag("AppShellHeaderRoot")
                .fetchSemanticsNode()
                .boundsInRoot
                .height)
        assertContentStartsAtHeaderBottom()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedHeaderAndContentStayInsideResolvedSafeRectangle() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(width = 800.dp, height = 768.dp)) {
                    CompositionLocalProvider(
                        LocalHausColors provides lightHausPalette,
                    ) {
                        AppShellLayout(
                            safeBounds =
                                appShellSafeBoundsFor(
                                    windowWidthDp = 800f,
                                    windowHeightDp = 768f,
                                    safeLeftInsetDp = 48f,
                                    safeTopInsetDp = 24f,
                                    safeRightInsetDp = 48f,
                                    safeBottomInsetDp = 34f,
                                    imeBottomInsetDp = 0f,
                                ),
                            navigationVisible = false,
                            selectedDestination = null,
                            activeBrowseMode = null,
                            onNavigate = {},
                            onBrowseModeChange = {},
                            onSettings = {},
                            showSettingsAction = false,
                            routeHeader =
                                AppShellRouteHeader(
                                    title = "Library",
                                    onBack = null,
                                    actions = emptyList(),
                                ),
                            bottomBarContent = LibraryBottomBarContent.Hidden,
                            content = { Box(Modifier.fillMaxSize()) },
                            bottomBar = { _, _ -> },
                        )
                    }
                }
            }

            val safeFrame =
                onNodeWithTag(AppShellSafeFrameTestTag)
                    .fetchSemanticsNode()
                    .boundsInRoot
            val headerBounds =
                onNodeWithTag("AppShellHeaderRoot")
                    .fetchSemanticsNode()
                    .boundsInRoot
            val contentBounds =
                onNodeWithTag(AppShellContentRegionTestTag)
                    .fetchSemanticsNode()
                    .boundsInRoot

            onNodeWithTag(AppShellSafeFrameTestTag)
                .assertLeftPositionInRootIsEqualTo(48.dp)
                .assertWidthIsEqualTo(704.dp)
            assertTrue(headerBounds.left >= safeFrame.left)
            assertTrue(headerBounds.right <= safeFrame.right)
            assertTrue(contentBounds.left >= safeFrame.left)
            assertTrue(contentBounds.right <= safeFrame.right)
            onNodeWithTag("AppShellHeaderRoot")
                .assertTopPositionInRootIsEqualTo(0.dp)
                .assertHeightIsEqualTo(80.dp)
            onNodeWithTag(AppShellContentRegionTestTag)
                .assertTopPositionInRootIsEqualTo(80.dp)
                .assertHeightIsEqualTo(654.dp)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fullWindowRouteStaysWithinSafeBottomAndImeAfterResize() =
        runComposeUiTest {
            val windowHeight = mutableStateOf(768.dp)
            val safeBounds =
                mutableStateOf(
                    appShellSafeBoundsFor(
                        windowWidthDp = 800f,
                        windowHeightDp = 768f,
                        safeLeftInsetDp = 0f,
                        safeTopInsetDp = 24f,
                        safeRightInsetDp = 0f,
                        safeBottomInsetDp = 34f,
                        imeBottomInsetDp = 0f,
                    ),
                )

            setContent {
                Box(
                    Modifier.size(
                        width = 800.dp, height = windowHeight.value)) {
                        CompositionLocalProvider(
                            LocalHausColors provides lightHausPalette,
                        ) {
                            AppShellLayout(
                                safeBounds = safeBounds.value,
                                navigationVisible = false,
                                selectedDestination = null,
                                activeBrowseMode = null,
                                onNavigate = {},
                                onBrowseModeChange = {},
                                onSettings = {},
                                showSettingsAction = false,
                                bottomBarContent =
                                    LibraryBottomBarContent.Hidden,
                                content = { Box(Modifier.fillMaxSize()) },
                                bottomBar = { _, _ -> },
                            )
                        }
                    }
            }

            val content = onNodeWithTag(AppShellContentRegionTestTag)
            assertEquals(734f, content.fetchSemanticsNode().boundsInRoot.bottom)

            safeBounds.value =
                appShellSafeBoundsFor(
                    windowWidthDp = 800f,
                    windowHeightDp = 768f,
                    safeLeftInsetDp = 0f,
                    safeTopInsetDp = 24f,
                    safeRightInsetDp = 0f,
                    safeBottomInsetDp = 34f,
                    imeBottomInsetDp = 200f,
                )
            waitForIdle()
            assertEquals(568f, content.fetchSemanticsNode().boundsInRoot.bottom)

            windowHeight.value = 640.dp
            safeBounds.value =
                appShellSafeBoundsFor(
                    windowWidthDp = 800f,
                    windowHeightDp = 640f,
                    safeLeftInsetDp = 0f,
                    safeTopInsetDp = 24f,
                    safeRightInsetDp = 0f,
                    safeBottomInsetDp = 34f,
                    imeBottomInsetDp = 200f,
                )
            waitForIdle()
            assertEquals(440f, content.fetchSemanticsNode().boundsInRoot.bottom)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun imeOwnershipRestoresMeasuredChromeAndClearanceAcrossResize() =
        runComposeUiTest {
            val windowWidth = mutableStateOf(390.dp)
            val windowHeight = mutableStateOf(768.dp)
            val safeBottom = mutableStateOf(34f)
            val imeBottom = mutableStateOf(0f)
            val overlayVisible = mutableStateOf(false)
            val safeBounds = {
                appShellSafeBoundsFor(
                    windowWidthDp = windowWidth.value.value,
                    windowHeightDp = windowHeight.value.value,
                    safeLeftInsetDp = 0f,
                    safeTopInsetDp = 24f,
                    safeRightInsetDp = 0f,
                    safeBottomInsetDp = safeBottom.value,
                    imeBottomInsetDp = imeBottom.value,
                )
            }

            setContent {
                Box(
                    Modifier.size(
                        width = windowWidth.value,
                        height = windowHeight.value)) {
                        CompositionLocalProvider(
                            LocalHausColors provides lightHausPalette,
                            LocalHausWindowOverlayVisible provides
                                overlayVisible.value,
                        ) {
                            AppShellLayout(
                                safeBounds = safeBounds(),
                                navigationVisible = true,
                                selectedDestination =
                                    LibraryPrimaryDestination.Library,
                                activeBrowseMode = BrowseMode.Songs,
                                onNavigate = {},
                                onBrowseModeChange = {},
                                onSettings = {},
                                showSettingsAction = true,
                                bottomBarContent =
                                    LibraryBottomBarContent.NowPlaying,
                                content = { Box(Modifier.fillMaxSize()) },
                                bottomBar = { _, _ ->
                                    Box(Modifier.fillMaxWidth().height(100.dp))
                                },
                            )
                        }
                    }
            }

            val content = onNodeWithTag(AppShellContentRegionTestTag)
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 570f
            }

            imeBottom.value = 200f
            overlayVisible.value = true
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 570f
            }

            overlayVisible.value = false
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 568f
            }
            imeBottom.value = 0f
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 570f
            }

            windowWidth.value = 1_000.dp
            windowHeight.value = 640.dp
            safeBottom.value = 34f
            waitForIdle()
            assertEquals(506f, content.fetchSemanticsNode().boundsInRoot.bottom)

            imeBottom.value = 200f
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 440f
            }
            imeBottom.value = 0f
            waitUntil(timeoutMillis = 5_000) {
                content.fetchSemanticsNode().boundsInRoot.bottom == 506f
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun imeHidesCompactNavigationAndPlaybackChromeWhileRouteEditorOwnsIt() =
        runComposeUiTest {
            val imeBottom = mutableStateOf(0f)

            setContent {
                Box(Modifier.size(width = 390.dp, height = 768.dp)) {
                    CompositionLocalProvider(
                        LocalHausColors provides lightHausPalette,
                    ) {
                        AppShellLayout(
                            safeBounds =
                                appShellSafeBoundsFor(
                                    windowWidthDp = 390f,
                                    windowHeightDp = 768f,
                                    safeLeftInsetDp = 0f,
                                    safeTopInsetDp = 24f,
                                    safeRightInsetDp = 0f,
                                    safeBottomInsetDp = 34f,
                                    imeBottomInsetDp = imeBottom.value,
                                ),
                            navigationVisible = true,
                            selectedDestination =
                                LibraryPrimaryDestination.Library,
                            activeBrowseMode = BrowseMode.Songs,
                            onNavigate = {},
                            onBrowseModeChange = {},
                            onSettings = {},
                            showSettingsAction = true,
                            bottomBarContent =
                                LibraryBottomBarContent.NowPlaying,
                            content = { Box(Modifier.fillMaxSize()) },
                            bottomBar = { _, _ ->
                                Box(
                                    Modifier.fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("ime-test-dock"),
                                )
                            },
                        )
                    }
                }
            }

            onNodeWithTag(AppNavigationSearchTestTag).assertExists()
            onNodeWithTag("ime-test-dock").assertExists()

            imeBottom.value = 200f
            waitForIdle()

            onNodeWithTag(AppNavigationSearchTestTag).assertDoesNotExist()
            onNodeWithTag("ime-test-dock").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusedOverlayConsumesImeWithoutResizingCoveredRoute() =
        runComposeUiTest {
            val imeBottom = mutableStateOf(0f)

            setContent {
                Box(Modifier.size(width = 390.dp, height = 768.dp)) {
                    CompositionLocalProvider(
                        LocalHausColors provides lightHausPalette,
                        LocalHausWindowOverlayVisible provides true,
                    ) {
                        AppShellLayout(
                            safeBounds =
                                appShellSafeBoundsFor(
                                    windowWidthDp = 390f,
                                    windowHeightDp = 768f,
                                    safeLeftInsetDp = 0f,
                                    safeTopInsetDp = 24f,
                                    safeRightInsetDp = 0f,
                                    safeBottomInsetDp = 34f,
                                    imeBottomInsetDp = imeBottom.value,
                                ),
                            navigationVisible = false,
                            selectedDestination = null,
                            activeBrowseMode = null,
                            onNavigate = {},
                            onBrowseModeChange = {},
                            onSettings = {},
                            showSettingsAction = false,
                            bottomBarContent = LibraryBottomBarContent.Hidden,
                            content = { Box(Modifier.fillMaxSize()) },
                            bottomBar = { _, _ -> },
                        )
                    }
                }
            }

            val content = onNodeWithTag(AppShellContentRegionTestTag)
            assertEquals(734f, content.fetchSemanticsNode().boundsInRoot.bottom)

            imeBottom.value = 200f
            waitForIdle()

            assertEquals(734f, content.fetchSemanticsNode().boundsInRoot.bottom)
        }

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
