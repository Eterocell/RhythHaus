package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackSelectionSemanticsJvmTest {
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
                    onPlay = { playCount += 1 },
                    onToggleSelection = {},
                    onStartSelection = { selectionStartCount += 1 },
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
                onPlay = {},
                onToggleSelection = {},
                onStartSelection = {},
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
                    onPlay = { playCount += 1 },
                    onToggleSelection = { toggleCount += 1 },
                    onStartSelection = {},
                )
            }

            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role,
                        Role.Checkbox,
                    ) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.Off),
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
                    onPlay = { playCount += 1 },
                    onToggleSelection = { toggleCount += 1 },
                    onStartSelection = {},
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
}
