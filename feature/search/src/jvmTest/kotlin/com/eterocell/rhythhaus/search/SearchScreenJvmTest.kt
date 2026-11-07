package com.eterocell.rhythhaus.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.hausPalette
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchScreenJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchStateSurvivesResizeAndThemeChange() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            runComposeUiTest {
                var width by mutableStateOf(390.dp)
                var pageMargin by mutableStateOf(16.dp)
                var fontScale by mutableStateOf(1f)
                var colors by
                    mutableStateOf(hausPalette(HausPaletteId.Moss, false))
                val scrollPositions = mutableListOf<Pair<Int, Int>>()
                val tracks = List(32) { index -> searchTrack(index) }

                setContent {
                    val systemDensity = LocalDensity.current
                    CompositionLocalProvider(
                        LocalHausColors provides colors,
                        LocalDensity provides
                            Density(systemDensity.density, fontScale),
                    ) {
                        Box(
                            Modifier.size(width, 840.dp)
                                .padding(horizontal = pageMargin),
                        ) {
                            SearchContent(
                                libraryTracks = tracks,
                                currentTrackId = null,
                                isPlaying = false,
                                labels =
                                    SearchSharedLabels(
                                        clear = "Clear",
                                        nowPlaying = "Now playing",
                                    ),
                                selectTrackLabel = { "Select $it" },
                                selectionModeActive = false,
                                selectedTrackIds = emptySet(),
                                onStartSelection = {},
                                onToggleSelection = {},
                                onVisibleTrackIdsChanged = {},
                                onScrollPositionChanged = { index, offset ->
                                    scrollPositions += index to offset
                                },
                                onPlayTrack = { _, _ -> },
                                onDismiss = {},
                                playingIndicator = {},
                                bottomContentPadding = 0.dp,
                            )
                        }
                    }
                }
                waitForIdle()

                onAllNodes(hasContentDescription("Back")).assertCountEquals(0)
                val queryField = onNode(hasSetTextAction())
                queryField.assertLeftPositionInRootIsEqualTo(16.dp)
                queryField.assertTopPositionInRootIsEqualTo(16.dp)
                queryField.performTextInput("Song")
                waitForIdle()
                queryField.assertTextContains("Song")
                onNode(hasScrollToIndexAction()).performScrollToIndex(20)
                waitForIdle()
                assertTrue(scrollPositions.last().first >= 20)

                width = 840.dp
                pageMargin = 24.dp
                fontScale = 2f
                colors = hausPalette(HausPaletteId.Moss, isDark = true)
                waitForIdle()
                queryField.assertLeftPositionInRootIsEqualTo(24.dp)
                queryField.assertTopPositionInRootIsEqualTo(16.dp)
                queryField.assertTextContains("Song")
                assertTrue(scrollPositions.last().first >= 20)
                onNode(hasText("Clear", substring = false))
                    .assertHeightIsAtLeast(48.dp)

                width = 1_200.dp
                pageMargin = 32.dp
                colors = hausPalette(HausPaletteId.Moss, isDark = false)
                waitForIdle()
                queryField.assertLeftPositionInRootIsEqualTo(32.dp)
                queryField.assertTopPositionInRootIsEqualTo(16.dp)
                queryField.assertTextContains("Song")
                assertTrue(scrollPositions.last().first >= 20)

                onNode(hasText("Clear", substring = false)).performClick()
                queryField.performTextInput("No matching local track")
                waitForIdle()
                onNodeWithTag(
                        SearchNoMatchTestTag,
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun simplifiedChineseNoMatchRemainsVisibleAt200Percent() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(
                        LocalHausColors provides
                            hausPalette(HausPaletteId.Moss, isDark = false),
                        LocalDensity provides
                            Density(LocalDensity.current.density, 2f),
                    ) {
                        Box(Modifier.size(840.dp, 760.dp)) {
                            SearchContent(
                                libraryTracks = listOf(searchTrack(1)),
                                currentTrackId = null,
                                isPlaying = false,
                                labels =
                                    SearchSharedLabels(
                                        clear = "清除",
                                        nowPlaying = "正在播放",
                                    ),
                                selectTrackLabel = { "选择 $it" },
                                selectionModeActive = false,
                                selectedTrackIds = emptySet(),
                                onStartSelection = {},
                                onToggleSelection = {},
                                onVisibleTrackIdsChanged = {},
                                onScrollPositionChanged = { _, _ -> },
                                onPlayTrack = { _, _ -> },
                                onDismiss = {},
                                playingIndicator = {},
                                bottomContentPadding = 0.dp,
                            )
                        }
                    }
                }
                waitForIdle()
                onNode(hasSetTextAction()).performTextInput("无匹配曲目")
                waitForIdle()

                onNodeWithTag(
                        SearchNoMatchTestTag,
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                onNodeWithTag(
                        SearchResultCountTestTag,
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun searchTrack(index: Int): LibraryTrack {
        val id = "song-$index"
        return LibraryTrack(
            id,
            "source",
            id,
            AudioSource.FilePath(id),
            id,
            "Song $index",
            "Artist",
            "Album",
            180_000L,
            null,
            null,
            "scan",
            index.toLong(),
            index.toLong(),
        )
    }
}
