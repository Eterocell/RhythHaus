package com.eterocell.rhythhaus.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
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

public class AboutScreensJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun aboutRendersVersionLogoLibrariesAndSourceLink(): Unit =
        runComposeUiTest {
            var opened = ""
            var libraries = 0
            setContent {
                CompositionLocalProvider(
                    LocalUriHandler provides
                        object : UriHandler {
                            override fun openUri(uri: String): Unit {
                                opened = uri
                            }
                        }) {
                        SettingsAboutScreen({ libraries++ }, {})
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
            assertEquals(RhythHausSourceUrl, opened)
            assertEquals(1, libraries)
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
