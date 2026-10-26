package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HausSongRowJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionRowExposesOneCheckboxAndTogglesOnce() = runComposeUiTest {
        var activations = 0
        setContent {
            HausSongRow(
                title = "Track",
                subtitle = "Artist",
                selectionMode = true,
                selected = false,
                playing = false,
                interactive = true,
                onActivate = { activations += 1 },
                onSelect = {},
                selectLabel = "Select Track",
                playingLabel = null,
                desktopDensity = false,
                artwork = {},
                indicator = {},
                trailing = {},
            )
        }

        val checkbox =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Checkbox,
            )
        onAllNodes(checkbox).assertCountEquals(1)
        onNode(checkbox).performClick()
        waitForIdle()

        assertEquals(1, activations)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusedSelectionCheckboxShowsThemeOutline() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalHausColors provides LightHausPalette) {
                    HausSongRow(
                        title = "Track",
                        subtitle = "Artist",
                        selectionMode = true,
                        selected = false,
                        playing = false,
                        interactive = true,
                        onActivate = {},
                        onSelect = {},
                        selectLabel = "Select Track",
                        playingLabel = null,
                        desktopDensity = false,
                        artwork = {},
                        indicator = {},
                        trailing = {},
                        modifier = Modifier.testTag("selection-row"),
                    )
                }
        }

        val checkbox =
            onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox,
                ),
            )
        checkbox.performSemanticsAction(SemanticsActions.RequestFocus)
        checkbox.assertIsFocused()
        val pixels =
            onNodeWithTag("selection-row").captureToImage().toPixelMap()
        val outline = LightHausPalette.outline.toArgb()
        assertTrue(
            (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x ->
                    pixels[x, y].toArgb() == outline
                }
            },
            "Focused selection checkbox should show the theme outline",
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowGrowsToKeepLongTitleReadableAt200PercentTextScale() =
        runComposeUiTest {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, 2f),
                ) {
                    Box(Modifier.width(240.dp)) {
                        HausSongRow(
                            title =
                                "A long title that needs multiple lines to remain readable",
                            subtitle = "",
                            selectionMode = false,
                            selected = false,
                            playing = false,
                            interactive = false,
                            onActivate = {},
                            onSelect = null,
                            selectLabel = null,
                            playingLabel = null,
                            desktopDensity = false,
                            artwork = {},
                            indicator = {},
                            trailing = {},
                            modifier = Modifier.testTag("haus-song-row"),
                        )
                    }
                }
            }

            onNodeWithTag("haus-song-row").assertHeightIsAtLeast(80.dp)
        }

    @Test
    fun songArtistAlbumSubtitleOmitsBlankMetadata() {
        assertEquals(
            "Artist · Album", songArtistAlbumSubtitle("Artist", "Album"))
        assertEquals("Artist", songArtistAlbumSubtitle("Artist", ""))
        assertEquals("Album", songArtistAlbumSubtitle("", "Album"))
        assertEquals("Album", songArtistAlbumSubtitle(null, "Album"))
        assertEquals("Artist", songArtistAlbumSubtitle("Artist", "\t"))
        assertEquals("", songArtistAlbumSubtitle(" ", null))
    }
}
