package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.library.Playlist
import kotlin.test.Test
import kotlin.test.assertEquals

class Task3ReviewSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unmeasuredSelectionBarExposesNoActionsAndCannotDispatchClicks() = runComposeUiTest {
        var cancelCount = 0
        var addCount = 0
        setContent {
            TrackSelectionBar(
                selectedCount = 2,
                onCancel = { cancelCount += 1 },
                onAddToPlaylist = { addCount += 1 },
                interactive = false,
            )
        }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertCountEquals(0)
        assertEquals(0, cancelCount)
        assertEquals(0, addCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun staleMeasuredSelectionBarExposesNoActionsAndCannotDispatchClicks() = runComposeUiTest {
        val selection = LibraryBottomBarContent.Selection(2)
        val presentation = libraryBottomBarPresentation(
            content = selection,
            measurement = LibraryBottomBarMeasurement(LibraryBottomBarContent.NowPlaying, 286),
            hiddenFraction = 0f,
        )
        var cancelCount = 0
        var addCount = 0
        setContent {
            TrackSelectionBar(
                selectedCount = 2,
                onCancel = { cancelCount += 1 },
                onAddToPlaylist = { addCount += 1 },
                interactive = presentation.isInteractive,
            )
        }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertCountEquals(0)
        assertEquals(0, cancelCount)
        assertEquals(0, addCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun matchingMeasuredSelectionBarExposesOnlyExpectedActions() = runComposeUiTest {
        val selection = LibraryBottomBarContent.Selection(2)
        val presentation = libraryBottomBarPresentation(
            content = selection,
            measurement = LibraryBottomBarMeasurement(selection, 286),
            hiddenFraction = 0f,
        )
        var cancelCount = 0
        var addCount = 0
        setContent {
            TrackSelectionBar(
                selectedCount = 2,
                onCancel = { cancelCount += 1 },
                onAddToPlaylist = { addCount += 1 },
                interactive = presentation.isInteractive,
            )
        }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertCountEquals(2)
        val actions = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        actions[0].performClick()
        actions[1].performClick()
        waitForIdle()
        assertEquals(1, cancelCount)
        assertEquals(1, addCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerRemainsDismissibleWhenFirstSelectedTrackDisappears() = runComposeUiTest {
        var visible by mutableStateOf(true)
        var dismissCount = 0
        setContent {
            if (visible) {
                AddToPlaylistPicker(
                    playlists = listOf(playlist()),
                    state = AddToPlaylistPickerState(trackIds = listOf("missing", "track-b")),
                    onStateChange = {},
                    onDismiss = {
                        dismissCount += 1
                        visible = false
                    },
                    onAppend = {},
                    onInlineCreate = {},
                )
            }
        }

        val dismissMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        onAllNodes(dismissMatcher).assertCountEquals(1)
        onAllNodes(dismissMatcher)[0].performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertEquals(1, dismissCount)
        onAllNodes(dismissMatcher).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerUsesRetainedOrderedIdsWhenFirstSelectedTrackDisappears() = runComposeUiTest {
        var appendRequest: PlaylistAppendRequest? = null
        setContent {
            var state by remember {
                mutableStateOf(AddToPlaylistPickerState(trackIds = listOf("missing", "track-b")))
            }
            AddToPlaylistPicker(
                playlists = listOf(playlist()),
                state = state,
                onStateChange = { state = it },
                onDismiss = {},
                onAppend = { appendRequest = it },
                onInlineCreate = {},
            )
        }

        onAllNodes(hasContentDescription("Saved"))[0].performClick()
        val buttons = onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        buttons.assertCountEquals(3)
        buttons[1].performClick()
        waitForIdle()
        assertEquals(
            PlaylistAppendRequest("playlist-1", listOf("missing", "track-b")),
            appendRequest,
        )
    }

    private fun playlist() = Playlist(
        id = "playlist-1",
        name = "Saved",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

}
