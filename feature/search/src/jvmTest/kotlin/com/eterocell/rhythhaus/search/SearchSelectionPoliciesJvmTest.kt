package com.eterocell.rhythhaus.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchSelectionPoliciesJvmTest {
    private var previousLocale: Locale? = null

    /**
     * The result-count and no-match texts are resource-backed and asserted in
     * Simplified Chinese; pin the default locale so the test does not depend on
     * the JVM's ambient locale.
     */
    @BeforeTest
    fun setSimplifiedChineseLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
    }

    @AfterTest
    fun restoreLocale() {
        previousLocale?.let { Locale.setDefault(it) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun normalClickPlaysOnlyOutsideSelection() = runComposeUiTest {
        var plays = 0
        var toggles = 0
        setContent {
            content(
                tracks = listOf(track()),
                onPlay = { _, _ -> plays++ },
                onToggle = { toggles++ })
        }
        enterQuery("Song")
        onNode(hasContentDescription("Select Song")).performClick()
        assertEquals(1, plays)
        assertEquals(0, toggles)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longClickStartsSelectionWithoutPlayback() = runComposeUiTest {
        var starts = 0
        var plays = 0
        setContent {
            content(
                tracks = listOf(track()),
                onPlay = { _, _ -> plays++ },
                onStart = { starts++ })
        }
        enterQuery("Song")
        onNode(hasContentDescription("Select Song"))
            .performSemanticsAction(SemanticsActions.OnLongClick)
        assertEquals(1, starts)
        assertEquals(0, plays)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionModeRowAndCheckboxEachToggleExactlyOnceWithoutPlayback() =
        runComposeUiTest {
            var toggles = 0
            var plays = 0
            setContent {
                content(
                    tracks = listOf(track()),
                    selection = true,
                    onPlay = { _, _ -> plays++ },
                    onToggle = { toggles++ })
            }
            enterQuery("Song")
            onNode(hasContentDescription("Select Song")).performClick()
            assertEquals(1, toggles)
            assertEquals(0, plays)
            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role, Role.Checkbox) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.Off))
                .performClick()
            assertEquals(2, toggles)
            assertEquals(0, plays)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blankQueryHasNoResults() = runComposeUiTest {
        setContent { content(tracks = listOf(track())) }
        onAllNodes(hasText("Song")).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changingFilteredIdsDispatchesEachSearchReconciliation() =
        runComposeUiTest {
            val visible = mutableListOf<List<String>>()
            var tracks by mutableStateOf(listOf(track("one"), track("two")))
            setContent {
                content(tracks = tracks, onVisible = { visible += it })
            }
            enterQuery("Song")
            tracks = listOf(track("two"))
            waitForIdle()
            assertEquals(
                listOf(listOf("one", "two"), listOf("two")),
                visible.takeLast(2))
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filtersTitleArtistAndAlbumIgnoringCase() = runComposeUiTest {
        setContent {
            content(
                tracks =
                    listOf(
                        track("title", "Alpha"),
                        track("artist", "Song", "ALPHA"),
                        track("album", "Song", "Artist", "alpha")))
        }
        enterQuery("aLpHa")
        val visible = mutableListOf<List<String>>()
        setContent {
            content(
                tracks =
                    listOf(
                        track("title", "Alpha"),
                        track("artist", "Song", "ALPHA"),
                        track("album", "Song", "Artist", "alpha")),
                onVisible = { visible += it })
        }
        enterQuery("aLpHa")
        assertEquals(listOf("title", "artist", "album"), visible.last())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun resultCountsAndNoMatchTextUseFeatureResources() = runComposeUiTest {
        setContent {
            content(
                tracks =
                    listOf(
                        track("one", "Song"),
                        track("two", "Pair A"),
                        track("three", "Pair B")))
        }
        enterQuery("Song")
        onNodeWithTag(SearchResultCountTestTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SearchRenderedText, "1 个结果"))
        replaceQuery("Pair")
        onNodeWithTag(SearchResultCountTestTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SearchRenderedText, "2 个结果"))
        replaceQuery("missing")
        onNodeWithTag(SearchResultCountTestTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SearchRenderedText, "0 个结果"))
        onNodeWithTag(SearchNoMatchTestTag, useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SearchRenderedText, "没有匹配“missing”的曲目"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun requestsFocusOnce() = runComposeUiTest {
        var unrelated by mutableStateOf(0)
        lateinit var focusManager: FocusManager
        setContent {
            focusManager = LocalFocusManager.current
            unrelated
            content(tracks = listOf(track()))
        }
        val editable =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
        assertTrue(
            editable
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Focused) == true)
        runOnIdle { focusManager.clearFocus(force = true) }
        waitForIdle()
        assertTrue(
            editable
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Focused) != true)
        unrelated++
        waitForIdle()
        assertTrue(
            editable
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Focused) != true)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearResetsQuery() = runComposeUiTest {
        setContent { content(tracks = listOf(track())) }
        enterQuery("Song")
        onAllNodes(hasText("Clear")).assertCountEquals(1)[0].performClick()
        onAllNodes(hasText("result", substring = true), useUnmergedTree = true)
            .assertCountEquals(0)
        onAllNodes(hasText("Song")).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reportsPrimitiveScrollAndBottomPadding() = runComposeUiTest {
        val positions = mutableListOf<Pair<Int, Int>>()
        setContent {
            content(
                tracks = List(40) { track("$it", "Song $it") },
                bottomPadding = 96,
                onScroll = { index, offset -> positions += index to offset })
        }
        enterQuery("Song")
        onNode(hasScrollToIndexAction()).performScrollToIndex(20)
        onNode(hasScrollToIndexAction()).performTouchInput {
            swipe(center, center - Offset(0f, 20f))
        }
        waitForIdle()
        assertTrue(
            positions.any { (index, offset) -> index == 20 && offset > 0 })
        onNode(hasScrollToIndexAction()).performScrollToIndex(40)
        waitForIdle()
        onNodeWithTag(SearchBottomSpacerTestTag, useUnmergedTree = true)
            .assertExists()
            .performScrollTo()
        waitForIdle()
        val spacerNode =
            onNodeWithTag(SearchBottomSpacerTestTag, useUnmergedTree = true)
                .fetchSemanticsNode()
        val expectedSpacerHeight =
            96.dp.value * spacerNode.layoutInfo.density.density
        assertEquals(
            expectedSpacerHeight,
            spacerNode.layoutInfo.coordinates.size.height.toFloat(),
            0.5f)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun currentIndicatorAndNowPlayingSemanticsAreScoped() = runComposeUiTest {
        var playing by mutableStateOf(true)
        var currentId by mutableStateOf("current")
        setContent {
            content(
                tracks =
                    listOf(
                        track("current", "Current"), track("other", "Other")),
                currentId = currentId,
                playing = playing,
                indicator = { Box(Modifier.testTag("test-playing-indicator")) })
        }
        enterQuery("r")
        onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription, "Now playing"))
            .assertExists()
        onNodeWithTag("test-playing-indicator", useUnmergedTree = true)
            .assertExists()
        playing = false
        waitForIdle()
        onNodeWithTag("test-playing-indicator", useUnmergedTree = true)
            .assertDoesNotExist()
        playing = true
        currentId = "not-a-search-result"
        waitForIdle()
        onNodeWithTag("test-playing-indicator", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionAndVisibleSequenceUseProductionContent() = runComposeUiTest {
        val visible = mutableListOf<List<String>>()
        var toggles = 0
        setContent {
            content(
                tracks = listOf(track("one"), track("two")),
                selection = true,
                selectedIds = setOf("one"),
                onToggle = { toggles++ },
                onVisible = { visible += it })
        }
        enterQuery("Song")
        onNode(
                hasContentDescription("Select Song") and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.On))
            .performClick()
        assertEquals(1, toggles)
        assertEquals(listOf("one", "two"), visible.last())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyMetadataIsRetained() = runComposeUiTest {
        setContent { content(tracks = listOf(track("empty", "Empty", "", ""))) }
        enterQuery("Empty")
        onNode(hasContentDescription("Select Empty")).fetchSemanticsNode()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun duplicateOccurrencesRenderAndActivateDistinctlyAcrossUnrelatedRecomposition() =
        runComposeUiTest {
            val played = mutableListOf<Pair<List<String>, String>>()
            val visible = mutableListOf<List<String>>()
            var unrelated by mutableStateOf(0)
            setContent {
                unrelated
                content(
                    tracks =
                        listOf(
                            track("same", "Duplicate A"),
                            track("same", "Duplicate B")),
                    onPlay = { results, selected ->
                        played +=
                            results.map(LibraryTrack::title) to selected.title
                    },
                    onVisible = { visible += it })
            }
            enterQuery("Duplicate")
            onAllNodes(hasText("Duplicate A"))
                .assertCountEquals(1)[0]
                .performClick()
            onAllNodes(hasText("Duplicate B"))
                .assertCountEquals(1)[0]
                .performClick()
            val before = visible.size
            unrelated++
            waitForIdle()
            assertEquals(
                listOf(
                    listOf("Duplicate A", "Duplicate B") to "Duplicate A",
                    listOf("Duplicate A", "Duplicate B") to "Duplicate B"),
                played)
            assertEquals(listOf("same", "same"), visible.last())
            assertEquals(before, visible.size)
        }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.enterQuery(
        value: String
    ) {
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .performTextInput(value)
        waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.replaceQuery(
        value: String
    ) {
        onAllNodes(hasText("Clear")).assertCountEquals(1)[0].performClick()
        enterQuery(value)
    }

    @Composable
    private fun content(
        tracks: List<LibraryTrack>,
        currentId: String? = null,
        playing: Boolean = false,
        selection: Boolean = false,
        selectedIds: Set<String> = emptySet(),
        bottomPadding: Int = 0,
        onPlay: (List<LibraryTrack>, LibraryTrack) -> Unit = { _, _ -> },
        onToggle: () -> Unit = {},
        onStart: () -> Unit = {},
        onVisible: (List<String>) -> Unit = {},
        onScroll: (Int, Int) -> Unit = { _, _ -> },
        indicator: @Composable () -> Unit = {},
    ) =
        SearchContent(
            libraryTracks = tracks,
            currentTrackId = currentId,
            isPlaying = playing,
            labels = SearchSharedLabels("Search", "Clear", "Now playing"),
            selectTrackLabel = { "Select $it" },
            selectionModeActive = selection,
            selectedTrackIds = selectedIds,
            onStartSelection = { onStart() },
            onToggleSelection = { onToggle() },
            onVisibleTrackIdsChanged = onVisible,
            onScrollPositionChanged = onScroll,
            onPlayTrack = onPlay,
            onDismiss = {},
            playingIndicator = indicator,
            bottomContentPadding = bottomPadding.dp,
        )

    private fun track(
        id: String = "track",
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album"
    ) =
        LibraryTrack(
            id,
            "source",
            id,
            AudioSource.FilePath(id),
            id,
            title,
            artist,
            album,
            1,
            null,
            null,
            "scan",
            1,
            1)
}
