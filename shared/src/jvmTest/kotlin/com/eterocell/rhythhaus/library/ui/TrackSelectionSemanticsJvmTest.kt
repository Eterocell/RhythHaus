package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackSelectionSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusingSelectionActionShowsContrastingOutline() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalHausColors provides LightHausPalette) {
                    TrackSelectionBar(
                        selectedCount = 1,
                        onCancel = {},
                        onSelectAll = {},
                        onAddToPlaylist = {},
                    )
                }
        }

        val addAction =
            onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role, Role.Button),
            )[2]
        val unfocusedPixels = addAction.captureToImage().toPixelMap()
        val edgeY = unfocusedPixels.height / 2
        assertTrue(
            unfocusedPixels[0, edgeY] != LightHausPalette.onPulse,
            "Unfocused selection action should not show the focus outline",
        )
        addAction.performSemanticsAction(SemanticsActions.RequestFocus)
        addAction.assertIsFocused()
        waitForIdle()
        val focusedPixels = addAction.captureToImage().toPixelMap()
        assertEquals(
            LightHausPalette.outline,
            focusedPixels[0, focusedPixels.height / 2],
            "Focused pulse action should draw the outer focus boundary at its edge",
        )
        assertEquals(
            LightHausPalette.onPulse,
            focusedPixels[3, focusedPixels.height / 2],
            "Focused pulse action should contrast with its pulse surface",
        )
    }

    @Test
    fun focusBoundaryPalettePairsMeetThreeToOneInBothAppearances() {
        listOf(LightHausPalette, DarkHausPalette).forEach { palette ->
            listOf(
                    palette.paper,
                    palette.panel,
                    palette.panelStrong,
                    palette.navigation,
                )
                .forEach { surface ->
                    assertTrue(
                        contrastRatio(palette.outline, surface) >= 3.0,
                        "Focus outline must contrast with its outer surface",
                    )
                }
            assertTrue(
                contrastRatio(palette.paper, palette.ink) >= 3.0,
                "Selected setting focus band must contrast with its ink surface",
            )
            assertTrue(
                contrastRatio(palette.onPulse, palette.pulse) >= 3.0,
                "Pulse-action focus band must contrast with its fill",
            )
            assertTrue(
                contrastRatio(palette.paper, palette.pulse) >= 3.0,
                "Sleep-timer focus band must contrast with its pulse fill",
            )
            assertTrue(
                contrastRatio(palette.onDanger, palette.danger) >= 3.0,
                "Destructive-action focus band must contrast with its fill",
            )
            assertTrue(
                contrastRatio(palette.danger, palette.dangerPanel) >= 3.0,
                "Destructive-panel focus band must contrast with its fill",
            )
        }
    }

    @Test
    fun activationDispatchesPlaybackOnlyForClickOutsideSelection() {
        assertEquals(
            TrackRowActivation.Play,
            trackRowActivation(
                selectionModeActive = false, gesture = TrackRowGesture.Click),
        )
        assertEquals(
            TrackRowActivation.StartSelection,
            trackRowActivation(
                selectionModeActive = false,
                gesture = TrackRowGesture.LongClick),
        )
        assertEquals(
            TrackRowActivation.ToggleSelection,
            trackRowActivation(
                selectionModeActive = true, gesture = TrackRowGesture.Click),
        )
        assertEquals(
            TrackRowActivation.ToggleSelection,
            trackRowActivation(
                selectionModeActive = true,
                gesture = TrackRowGesture.LongClick),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowExposesAccessibleLongClickSelectionActionOutsideSelectionMode() =
        runComposeUiTest {
            var playCount = 0
            var selectionStartCount = 0
            setContent {
                TrackRow(
                    track = testTrack(),
                    isNowPlaying = false,
                    selectionModeActive = false,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = { playCount += 1 },
                    onToggleSelection = {},
                    onStartSelection = { selectionStartCount += 1 },
                    favorite = false,
                    onSetFavorite = {},
                    onEditMetadata = {},
                )
            }

            val longClickMatcher =
                SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)
            onNode(longClickMatcher)
                .assertExists()
                .assert(
                    SemanticsMatcher("Long click has a selection label") { node
                        ->
                        !node.config
                            .getOrNull(SemanticsActions.OnLongClick)
                            ?.label
                            .isNullOrBlank()
                    },
                )
                .performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()

            assertEquals(0, playCount)
            assertEquals(1, selectionStartCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectedAndNowPlayingSemanticsCoexist() = runComposeUiTest {
        setContent {
            TrackRow(
                track = testTrack(),
                isNowPlaying = true,
                selectionModeActive = true,
                isSelected = true,
                labels = labels(),
                artworkLoader = { null },
                onPlay = {},
                onToggleSelection = {},
                onStartSelection = {},
                favorite = false,
                onSetFavorite = {},
                onEditMetadata = {},
            )
        }

        onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.On,
                ) and
                    SemanticsMatcher.keyIsDefined(
                        SemanticsProperties.StateDescription),
            )
            .assertIsOn()
            .assert(
                SemanticsMatcher("Now playing state remains available") { node
                    ->
                    !node.config
                        .getOrNull(SemanticsProperties.StateDescription)
                        .isNullOrBlank()
                },
            )
        onNodeWithTag("track-row-more-actions").performClick()
        onNodeWithTag("track-row-select").assertDoesNotExist()
        onNodeWithTag("track-row-edit-metadata").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun namedMoreMenuOpensMetadataWithoutActivatingPlayback() =
        runComposeUiTest {
            var playCount = 0
            var editCount = 0
            setContent {
                TrackRow(
                    track = testTrack(),
                    isNowPlaying = false,
                    selectionModeActive = false,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = { playCount++ },
                    onToggleSelection = {},
                    onStartSelection = {},
                    favorite = false,
                    onSetFavorite = {},
                    onEditMetadata = { editCount++ },
                )
            }

            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.Off,
                    ),
                )
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            onNodeWithTag("track-row-more-actions")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .assert(
                    SemanticsMatcher("More actions are named") { node ->
                        !node.config
                            .getOrNull(SemanticsProperties.ContentDescription)
                            .isNullOrEmpty()
                    })
                .performClick()
            onNodeWithTag("track-row-edit-metadata").performClick()
            waitForIdle()

            assertEquals(1, editCount)
            assertEquals(0, playCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun checkboxClickTogglesSelectionExactlyOnceWithoutPlayback() =
        runComposeUiTest {
            var playCount = 0
            var toggleCount = 0
            setContent {
                TrackRow(
                    track = testTrack(),
                    isNowPlaying = false,
                    selectionModeActive = true,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = { playCount += 1 },
                    onToggleSelection = { toggleCount += 1 },
                    onStartSelection = {},
                    favorite = false,
                    onSetFavorite = {},
                    onEditMetadata = {},
                )
            }

            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role,
                        Role.Checkbox,
                    ) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.Off) and
                        hasAnyAncestor(hasContentDescription("Song")),
                    useUnmergedTree = true,
                )
                .performClick()
            waitForIdle()

            assertEquals(0, playCount)
            assertEquals(1, toggleCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowClickInSelectionModeTogglesExactlyOnceWithoutPlayback() =
        runComposeUiTest {
            var playCount = 0
            var toggleCount = 0
            setContent {
                TrackRow(
                    track = testTrack(),
                    isNowPlaying = false,
                    selectionModeActive = true,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = { playCount += 1 },
                    onToggleSelection = { toggleCount += 1 },
                    onStartSelection = {},
                    favorite = false,
                    onSetFavorite = {},
                    onEditMetadata = {},
                )
            }

            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
                .performClick()
            waitForIdle()

            assertEquals(0, playCount)
            assertEquals(1, toggleCount)
        }

    private fun testTrack() =
        Track(
            id = "track-1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
            source = AudioSource.FilePath("song.mp3"),
        )

    private fun labels(): LibrarySharedLabels =
        LibrarySharedLabels(
            addMusicFolder = "Add music folder",
            folderPickerUnavailable = "Folder picker unavailable",
            clearLibrary = "Clear library",
            cancel = "Cancel",
            libraryTitle = "Library",
            albumArt = "Album art",
            albumArtwork = "Album artwork",
            nowPlayingBadge = "Now playing",
            selectTrack = { title -> "Select $title" },
            trackArtistAlbum = { artist, album -> "$artist · $album" },
        )
}

private fun contrastRatio(first: Color, second: Color): Double {
    fun linearChannel(value: Float): Double {
        val channel = value.toDouble()
        return if (channel <= 0.04045) channel / 12.92
        else ((channel + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(color: Color): Double =
        0.2126 * linearChannel(color.red) +
            0.7152 * linearChannel(color.green) +
            0.0722 * linearChannel(color.blue)

    val firstLuminance = luminance(first)
    val secondLuminance = luminance(second)
    return (maxOf(firstLuminance, secondLuminance) + 0.05) /
        (minOf(firstLuminance, secondLuminance) + 0.05)
}
