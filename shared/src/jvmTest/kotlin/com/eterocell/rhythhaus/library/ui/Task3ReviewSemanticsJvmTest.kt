package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class Task3ReviewSemanticsJvmTest {
    @Test
    fun unmeasuredNowPlayingPresentationIsNotInteractive() {
        assertEquals(
            false,
            libraryBottomBarPresentation(
                    content = LibraryBottomBarContent.NowPlaying,
                    measurement = null,
                    hiddenFraction = 0f,
                )
                .isInteractive,
        )
    }

    @Test
    fun staleMeasuredNowPlayingPresentationIsNotInteractive() {
        assertEquals(
            false,
            libraryBottomBarPresentation(
                    content = LibraryBottomBarContent.NowPlaying,
                    measurement =
                        LibraryBottomBarMeasurement(
                            LibraryBottomBarContent.Selection(1), 286),
                    hiddenFraction = 0f,
                )
                .isInteractive,
        )
    }

    @Test
    fun matchingMeasuredNowPlayingPresentationIsInteractive() {
        assertEquals(
            true,
            libraryBottomBarPresentation(
                    content = LibraryBottomBarContent.NowPlaying,
                    measurement =
                        LibraryBottomBarMeasurement(
                            LibraryBottomBarContent.NowPlaying, 286),
                    hiddenFraction = 0f,
                )
                .isInteractive,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistBrowserEmptyConfirmationDoesNotThrowOrInvokeOnConfirm() =
        runComposeUiTest {
            var confirmCount = 0
            setContent {
                PlaylistTrackBrowser(
                    playlistName = "Saved",
                    libraryTracks =
                        listOf(libraryTrack("b"), libraryTrack("a")),
                    state =
                        PlaylistTrackBrowserState(playlistId = "playlist-1"),
                    onStateChange = {},
                    onDismiss = {},
                    onConfirm = { confirmCount += 1 },
                )
            }

            val buttons =
                onAllNodes(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role, Role.Button))
            buttons.assertCountEquals(1)
            buttons[0].performClick()
            waitForIdle()
            assertEquals(0, confirmCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistBrowserSelectedConfirmationUsesVisibleOrder() =
        runComposeUiTest {
            var request: PlaylistAppendRequest? = null
            setContent {
                var state by remember {
                    mutableStateOf(
                        PlaylistTrackBrowserState(playlistId = "playlist-1"))
                }
                PlaylistTrackBrowser(
                    playlistName = "Saved",
                    libraryTracks =
                        listOf(libraryTrack("b"), libraryTrack("a")),
                    state = state,
                    onStateChange = { state = it },
                    onDismiss = {},
                    onConfirm = { request = it },
                )
            }

            onNode(hasContentDescription("Song b")).performClick()
            onNode(hasContentDescription("Song a")).performClick()
            onAllNodes(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role, Role.Button))[0]
                .performClick()
            waitForIdle()
            assertEquals(
                PlaylistAppendRequest("playlist-1", listOf("b", "a")), request)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unmeasuredSelectionBarExposesNoActionsAndCannotDispatchClicks() =
        runComposeUiTest {
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

            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(0)
            assertEquals(0, cancelCount)
            assertEquals(0, addCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun staleMeasuredSelectionBarExposesNoActionsAndCannotDispatchClicks() =
        runComposeUiTest {
            val selection = LibraryBottomBarContent.Selection(2)
            val presentation =
                libraryBottomBarPresentation(
                    content = selection,
                    measurement =
                        LibraryBottomBarMeasurement(
                            LibraryBottomBarContent.NowPlaying, 286),
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

            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(0)
            assertEquals(0, cancelCount)
            assertEquals(0, addCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun matchingMeasuredSelectionBarExposesOnlyExpectedActions() =
        runComposeUiTest {
            val selection = LibraryBottomBarContent.Selection(2)
            val presentation =
                libraryBottomBarPresentation(
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

            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(2)
            val actions =
                onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            actions[0].performClick()
            actions[1].performClick()
            waitForIdle()
            assertEquals(1, cancelCount)
            assertEquals(1, addCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerRemainsDismissibleWhenFirstSelectedTrackDisappears() =
        runComposeUiTest {
            var visible by mutableStateOf(true)
            var dismissCount = 0
            setContent {
                if (visible) {
                    AddToPlaylistPicker(
                        playlists = listOf(playlist()),
                        state =
                            AddToPlaylistPickerState(
                                trackIds = listOf("missing", "track-b")),
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

            val dismissMatcher =
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
            onAllNodes(dismissMatcher).assertCountEquals(1)
            onAllNodes(dismissMatcher)[0].performSemanticsAction(
                SemanticsActions.Dismiss)
            waitForIdle()
            assertEquals(1, dismissCount)
            onAllNodes(dismissMatcher).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerUsesRetainedOrderedIdsWhenFirstSelectedTrackDisappears() =
        runComposeUiTest {
            var appendRequest: PlaylistAppendRequest? = null
            setContent {
                var state by remember {
                    mutableStateOf(
                        AddToPlaylistPickerState(
                            trackIds = listOf("missing", "track-b")))
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
            val buttons =
                onAllNodes(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role, Role.Button))
            buttons.assertCountEquals(3)
            buttons[1].performClick()
            waitForIdle()
            assertEquals(
                PlaylistAppendRequest(
                    "playlist-1", listOf("missing", "track-b")),
                appendRequest,
            )
        }

    private fun playlist() =
        PlaylistSummary(
            id = "playlist-1",
            name = "Saved",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun track() =
        Track(
            id = "track-1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
            source = AudioSource.FilePath("song.mp3"),
        )

    private fun libraryTrack(id: String) =
        LibraryTrack(
            id = id,
            sourceId = "source-1",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("$id.mp3"),
            displayName = "Song $id",
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            durationMillis = 180_000L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = "scan-1",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
}
