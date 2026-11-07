package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausSpacing
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res as CoreUiRes
import rhythhaus.core.ui.generated.resources.back
import rhythhaus.feature.settings.generated.resources.Res as SettingsRes
import rhythhaus.feature.settings.generated.resources.about_open_source_libraries

public class AboutScreensJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun aboutRendersVersionLogoLibrariesAndSourceLink(): Unit =
        runComposeUiTest {
            var opened = ""
            var libraries = 0
            var librariesTitle = ""
            var expectedLibrariesTitle = ""
            setContent {
                expectedLibrariesTitle =
                    stringResource(
                        SettingsRes.string.about_open_source_libraries)
                CompositionLocalProvider(
                    LocalUriHandler provides
                        object : UriHandler {
                            override fun openUri(uri: String): Unit {
                                opened = uri
                            }
                        }) {
                        SettingsAboutScreen(
                            { title ->
                                libraries++
                                librariesTitle = title
                            },
                            {})
                    }
            }
            onNodeWithText("RhythHaus").assertExists()
            onNodeWithText(RhythHausBuildInfo.versionName, substring = true)
                .assertExists()
            onNodeWithTag(AboutSourceTestTag, useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            onNodeWithTag(AboutLibrariesTestTag, useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            onNodeWithTag("about-logo", useUnmergedTree = true).assertExists()
            val appIcon =
                onNodeWithTag("about-app-icon", useUnmergedTree = true)
                    .fetchSemanticsNode()
            val expectedIconSize =
                with(appIcon.layoutInfo.density) {
                    AboutScreenLayoutPolicy.logoImageSize.toPx()
                }
            assertEquals(expectedIconSize, appIcon.boundsInRoot.width, 1f)
            assertEquals(expectedIconSize, appIcon.boundsInRoot.height, 1f)
            assertEquals(RhythHausSourceUrl, opened)
            assertEquals(1, libraries)
            assertEquals(expectedLibrariesTitle, librariesTitle)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun hiddenRouteHeaderStartsAboutContentAtDocumentedPagePadding():
        Unit = runComposeUiTest {
        val topContentPadding = 24.dp
        var backLabel = ""
        setContent {
            backLabel = stringResource(CoreUiRes.string.back)
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String): Unit = Unit
                    }) {
                    Box(Modifier.size(600.dp, 1_200.dp)) {
                        SettingsAboutScreen(
                            onOpenLibraries = {},
                            onDismiss = {},
                            showRouteHeader = false,
                            topContentPadding = topContentPadding,
                        )
                    }
                }
        }
        val logo =
            onNodeWithTag("about-logo", useUnmergedTree = true)
                .fetchSemanticsNode()
        // The identity block carries its own `HausSpacing.xl` top padding, so
        // the rendered top is the page padding plus that inset. A retained
        // empty header item would add `itemSpacing` on top of it.
        val expectedTop =
            with(logo.layoutInfo.density) {
                (topContentPadding +
                        CompactSettingsLayoutPolicy.verticalPagePadding +
                        HausSpacing.xl +
                        (88.dp - AboutScreenLayoutPolicy.logoImageSize) / 2)
                    .toPx()
            }
        assertEquals(
            expectedTop,
            logo.boundsInRoot.top,
            absoluteTolerance = 1f,
            message =
                "the About content must begin at the documented page " +
                    "padding without an empty header list item",
        )
        onAllNodes(
                hasClickAction() and
                    hasAnyDescendant(hasContentDescription(backLabel)),
                useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun longLicenseContentRemainsScrollableAt200Percent(): Unit =
        runComposeUiTest {
            val libraries =
                (1..24).joinToString(",") { index ->
                    """{"uniqueId":"test:license-$index","artifactVersion":"1","name":"License entry $index","description":"Library metadata $index","licenses":["MIT"]}"""
                }
            val catalog =
                """{"libraries":[$libraries],"licenses":{"MIT":{"name":"MIT License","url":"https://example.test/mit","content":"${"Long license text. ".repeat(80)}","spdxId":"MIT","hash":"MIT"}}}"""
            val libraryListState = LazyListState()
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f, 2f),
                ) {
                    Box(Modifier.size(390.dp, 640.dp)) {
                        OpenSourceLibrariesContent(
                            readCatalogJson = { catalog },
                            onDismiss = {},
                            libraryListState = libraryListState,
                        )
                    }
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                runCatching { onNodeWithTag(AboutLoadedTestTag).assertExists() }
                    .isSuccess
            }
            val loadedBounds =
                onNodeWithTag(AboutLoadedTestTag)
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertTrue(loadedBounds.height > 300f)
            onNodeWithTag(AboutLoadedTestTag).assertIsDisplayed()
            assertEquals(0, libraryListState.firstVisibleItemIndex)
            onNodeWithTag(AboutLibrariesListTestTag).performScrollToIndex(23)
            waitForIdle()
            assertTrue(libraryListState.firstVisibleItemIndex > 0)
            assertTrue(
                libraryListState.layoutInfo.visibleItemsInfo.lastOrNull()?.let {
                    it.index == 23
                } == true,
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun hiddenRouteHeaderKeepsLicensesFlushWithThePagePadding(): Unit =
        runComposeUiTest {
            var showRouteHeader by mutableStateOf(false)
            val libraryListState = LazyListState()
            val loadCatalog: suspend () -> String = { catalogJson }
            setContent {
                Box(Modifier.size(390.dp, 640.dp)) {
                    OpenSourceLibrariesContent(
                        readCatalogJson = loadCatalog,
                        onDismiss = {},
                        showRouteHeader = showRouteHeader,
                        libraryListState = libraryListState,
                    )
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                runCatching { onNodeWithTag(AboutLoadedTestTag).assertExists() }
                    .isSuccess
            }
            val density =
                onNodeWithTag(AboutLibrariesListTestTag)
                    .fetchSemanticsNode()
                    .layoutInfo
                    .density
            val headerSpacing =
                with(density) {
                    CompactSettingsLayoutPolicy.itemSpacing.roundToPx()
                }
            runOnIdle {
                assertEquals(
                    0, libraryListState.layoutInfo.beforeContentPadding)
            }
            runOnIdle { showRouteHeader = true }
            waitForIdle()
            runOnIdle {
                assertEquals(
                    headerSpacing,
                    libraryListState.layoutInfo.beforeContentPadding,
                )
            }
        }

    @Test
    public fun catalogLoadsOnlyWhenNonEmpty(): Unit = runBlocking {
        val loaded = loadAboutLibraries({ catalogJson })
        assertIs<AboutLibrariesLoadState.Loaded>(loaded)
        assertEquals(1, loaded.libraries.libraries.size)
    }

    @Test
    public fun malformedAndEmptyCatalogsFail(): Unit = runBlocking {
        assertIs<AboutLibrariesLoadState.Failed>(
            loadAboutLibraries({ "not json" }))
        assertIs<AboutLibrariesLoadState.Failed>(loadAboutLibraries({ "" }))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun retryImmediatelyShowsLoadingAndUsesCurrentLoader(): Unit =
        runComposeUiTest {
            val gate = CompletableDeferred<String>()
            var invocations = 0
            val loader: suspend () -> String = {
                if (++invocations == 1) "" else gate.await()
            }
            setContent { OpenSourceLibrariesScreen(loader, {}) }
            waitUntil {
                runCatching { onNodeWithTag(AboutRetryTestTag).assertExists() }
                    .isSuccess
            }
            assertEquals(1, invocations)
            onNodeWithTag(AboutRetryTestTag).performClick()
            onNodeWithTag(AboutLoadingTestTag).assertExists()
            gate.complete(catalogJson)
            waitUntil {
                runCatching { onNodeWithTag(AboutLoadedTestTag).assertExists() }
                    .isSuccess
            }
            assertEquals(2, invocations)
            onNodeWithTag(AboutLoadedTestTag).assertExists()
        }

    @Test
    public fun readCancellationIsRethrownIdentically(): Unit = runBlocking {
        val cancellation = CancellationException("read")
        val thrown =
            assertFailsWith<CancellationException> {
                loadAboutLibraries(readJson = { throw cancellation })
            }
        assertSame(cancellation, thrown)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun suppliedDispatcherRunsLoadAndCancellationDoesNotPublishState():
        Unit = runComposeUiTest {
        runBlocking {
            val dispatcher = RecordingDispatcher()
            val loaded =
                loadAboutLibraries(
                    readJson = { catalogJson },
                    parseJson = { json ->
                        assertEquals(catalogJson, json)
                        com.mikepenz.aboutlibraries.Libs.Builder()
                            .withJson(json)
                            .build()
                    },
                    dispatcher = dispatcher)
            assertIs<AboutLibrariesLoadState.Loaded>(loaded)
            assertEquals(1, dispatcher.dispatches)

            val rejection =
                assertFailsWith<CancellationException> {
                    loadAboutLibraries(
                        readJson = { catalogJson },
                        dispatcher = RejectingDispatcher())
                }
            assertEquals("dispatcher rejected", rejection.message)
        }
        val oldLoaderStarted = CompletableDeferred<Unit>()
        var loader: suspend () -> String by
            mutableStateOf({
                oldLoaderStarted.complete(Unit)
                CompletableDeferred<String>().await()
            })
        setContent { OpenSourceLibrariesScreen(loader, {}) }
        waitUntil { oldLoaderStarted.isCompleted }
        runOnIdle {
            loader = { catalogJson }
        }
        waitUntil {
            runCatching { onNodeWithTag(AboutLoadedTestTag).assertExists() }
                .isSuccess
        }
        onNodeWithTag(AboutRetryTestTag).assertDoesNotExist()
    }

    @Test
    public fun parseCancellationIsRethrownIdentically(): Unit = runBlocking {
        val cancellation = CancellationException("parse")
        val thrown =
            assertFailsWith<CancellationException> {
                loadAboutLibraries({ catalogJson }, { throw cancellation })
            }
        assertSame(cancellation, thrown)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun loaderReplacementUsesNewestLoader(): Unit = runComposeUiTest {
        val oldLoaderStarted = CompletableDeferred<Unit>()
        var loader: suspend () -> String by
            mutableStateOf({
                oldLoaderStarted.complete(Unit)
                CompletableDeferred<String>().await()
            })
        setContent { OpenSourceLibrariesScreen(loader, {}) }
        waitUntil { oldLoaderStarted.isCompleted }
        runOnIdle { loader = { replacementCatalogJson } }
        waitUntil {
            runCatching { onNodeWithTag(AboutLoadedTestTag).assertExists() }
                .isSuccess
        }
        onNodeWithTag(AboutLoadedTestTag).assertExists()
        onNodeWithText("Replacement Catalog").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun cancellationResistantStaleLoaderCannotOverwriteNewerResult():
        Unit = runComposeUiTest {
        val readerA: suspend () -> String = { catalogJson }
        val readerB: suspend () -> String = { replacementCatalogJson }
        val loadedA = runBlocking { loadAboutLibraries(readerA) }
        val loadedB = runBlocking { loadAboutLibraries(readerB) }
        val staleLoaderStarted = CompletableDeferred<Unit>()
        val staleComparisonObserved = CompletableDeferred<Unit>()
        val staleGate = CompletableDeferred<Unit>()
        val comparisons =
            mutableListOf<Pair<AboutLibrariesLoadState, Boolean>>()
        var reader: suspend () -> String by mutableStateOf(readerA)
        var wholeLoader: AboutLibrariesLoader by
            mutableStateOf({
                staleLoaderStarted.complete(Unit)
                try {
                    staleGate.await()
                    loadAboutLibraries(readerA)
                } catch (_: CancellationException) {
                    withContext(NonCancellable) {
                        staleGate.await()
                        loadAboutLibraries(readerA)
                    }
                }
            })
        setContent {
            OpenSourceLibrariesContent(
                readCatalogJson = reader,
                onDismiss = {},
                loadLibraries = wholeLoader,
                onLoadCompared = { result, isCurrent ->
                    comparisons += result to isCurrent
                    if (result == loadedA && !isCurrent) {
                        staleComparisonObserved.complete(Unit)
                    }
                })
        }
        waitUntil { staleLoaderStarted.isCompleted }
        runOnIdle {
            reader = readerB
            wholeLoader = { loadAboutLibraries(readerB) }
        }
        waitUntil {
            runCatching { onNodeWithText("Replacement Catalog").assertExists() }
                .isSuccess
        }
        assertTrue(loadedB to true in comparisons)
        staleGate.complete(Unit)
        waitUntil { staleComparisonObserved.isCompleted }
        assertTrue(loadedA to false in comparisons)
        onNodeWithText("Replacement Catalog").assertExists()
        onNodeWithText("Catalog").assertDoesNotExist()
        onNodeWithTag(AboutRetryTestTag).assertDoesNotExist()
        onNodeWithText("Could not load open source libraries")
            .assertDoesNotExist()
    }
}

private class RecordingDispatcher : CoroutineDispatcher() {
    var dispatches: Int = 0

    override fun dispatch(context: CoroutineContext, block: Runnable): Unit {
        dispatches++
        Dispatchers.Default.dispatch(context, block)
    }
}

private class RejectingDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable): Nothing =
        throw CancellationException("dispatcher rejected")
}

private const val catalogJson: String =
    """{"libraries":[{"uniqueId":"test:catalog","artifactVersion":"1","name":"Catalog","licenses":["MIT"]}],"licenses":{"MIT":{"name":"MIT License","url":"https://example.test/mit","content":"MIT","spdxId":"MIT","hash":"MIT"}}}"""

private const val replacementCatalogJson: String =
    """{"libraries":[{"uniqueId":"test:replacement","artifactVersion":"1","name":"Replacement Catalog","licenses":["MIT"]}],"licenses":{"MIT":{"name":"MIT License","url":"https://example.test/mit","content":"MIT","spdxId":"MIT","hash":"MIT"}}}"""
