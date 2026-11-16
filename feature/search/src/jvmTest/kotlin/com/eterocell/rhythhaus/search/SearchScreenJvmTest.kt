package com.eterocell.rhythhaus.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
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
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchResultRendersEmbeddedArtwork() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            runComposeUiTest {
                val artworkColor = Color(0xFFFF3355)
                var playedTrackId: String? = null
                setContent {
                    CompositionLocalProvider(
                        LocalHausColors provides
                            hausPalette(HausPaletteId.Moss, isDark = false),
                    ) {
                        Box(Modifier.size(840.dp, 760.dp)) {
                            SearchContent(
                                libraryTracks =
                                    listOf(
                                        searchTrack(
                                            1,
                                            artworkBytes =
                                                solidColorPng(artworkColor),
                                        ),
                                    ),
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
                                onScrollPositionChanged = { _, _ -> },
                                onPlayTrack = { _, track ->
                                    playedTrackId = track.id
                                },
                                onDismiss = {},
                                playingIndicator = {},
                                bottomContentPadding = 0.dp,
                            )
                        }
                    }
                }
                waitForIdle()
                onNode(hasSetTextAction()).performTextInput("Song 1")
                waitForIdle()

                waitUntil(timeoutMillis = 5_000) {
                    val slots =
                        onAllNodes(
                                hasTestTag(SearchArtworkSlotTestTag),
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNodes()
                    slots.isNotEmpty() &&
                        onNodeWithTag(
                                SearchArtworkSlotTestTag,
                                useUnmergedTree = true,
                            )
                            .captureToImage()
                            .toPixelMap()
                            .let { pixels ->
                                pixels[pixels.width / 2, pixels.height / 2]
                                    .toArgb() == artworkColor.toArgb()
                            }
                }

                // Artwork is decorative and adds no competing row action.
                onNodeWithTag(
                        SearchArtworkSlotTestTag,
                        useUnmergedTree = true,
                    )
                    .assertHasNoClickAction()
                // The row keeps its existing title semantics and play action.
                onNode(hasContentDescription("Song 1")).performClick()
                assertEquals("song-1", playedTrackId)
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchResultArtworkFallsBackWhenBytesMissingOrInvalid() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(
                        LocalHausColors provides
                            hausPalette(HausPaletteId.Moss, isDark = false),
                    ) {
                        Box(Modifier.size(840.dp, 760.dp)) {
                            SearchContent(
                                libraryTracks =
                                    listOf(
                                        searchTrack(1, artworkBytes = null),
                                        searchTrack(
                                            2,
                                            artworkBytes =
                                                byteArrayOf(1, 2, 3, 4, 5),
                                        ),
                                    ),
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
                onNode(hasSetTextAction()).performTextInput("Song")
                waitForIdle()

                waitUntil(timeoutMillis = 5_000) {
                    onAllNodes(
                            hasTestTag(SearchNeutralArtworkTestTag),
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .size == 2
                }
                onAllNodes(
                        hasTestTag(SearchNeutralArtworkTestTag),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(2)
                for (index in 0..1) {
                    onAllNodes(
                            hasTestTag(SearchNeutralArtworkTestTag),
                            useUnmergedTree = true,
                        )[index]
                        .assertIsDisplayed()
                }
                // Neither fallback slot is an actionable row element.
                onAllNodes(
                        hasTestTag(SearchArtworkSlotTestTag) and
                            hasClickAction(),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(0)
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    /**
     * A solid-color PNG that Coil can decode, so rendered-pixel assertions are
     * deterministic instead of racing an asynchronous decode failure.
     */
    private fun solidColorPng(color: Color): ByteArray {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        val argb = color.toArgb()
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                image.setRGB(x, y, argb)
            }
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private fun searchTrack(
        index: Int,
        artworkBytes: ByteArray? = null,
    ): LibraryTrack {
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
            artworkBytes = artworkBytes,
        )
    }
}
