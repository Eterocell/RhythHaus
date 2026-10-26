package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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
                    expectedLayout = AppNavigationLayout.Bottom,
                    expectedWidthPx = 390,
                    allowInteraction = true,
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
                            LibraryBottomBarContent.Selection(1),
                            286,
                            AppNavigationLayout.Bottom,
                            390,
                        ),
                    hiddenFraction = 0f,
                    expectedLayout = AppNavigationLayout.Bottom,
                    expectedWidthPx = 390,
                    allowInteraction = true,
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
                            LibraryBottomBarContent.NowPlaying,
                            286,
                            AppNavigationLayout.Bottom,
                            390,
                        ),
                    hiddenFraction = 0f,
                    expectedLayout = AppNavigationLayout.Bottom,
                    expectedWidthPx = 390,
                    allowInteraction = true,
                )
                .isInteractive,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistBrowserEmptySelectionCannotBeAppended() = runComposeUiTest {
        var confirmCount = 0
        setContent {
            val destination = PlaylistFeatureDestination("browser")
            PlaylistTrackBrowserOverlay(
                playlistName = "Saved",
                libraryTracks = listOf(libraryTrack("b"), libraryTrack("a")),
                state = PlaylistBrowserState(playlistId = "playlist-1"),
                destination = destination,
                appearanceSource =
                    rememberPlaylistFeatureAppearanceSource(destination),
                dismissalPublisher = NoopPlaylistFeatureDismissalPublisher,
                onStateChange = {},
                onDismiss = {},
                onConfirmed = {},
                onConfirm = { _, _, _ -> confirmCount += 1 },
            )
        }

        onNodeWithTag("playlist-browser-confirm").performClick()
        waitForIdle()
        assertEquals(0, confirmCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistBrowserSelectedConfirmationUsesVisibleOrder() =
        runComposeUiTest {
            var request: Pair<String, List<String>>? = null
            setContent {
                var state by remember {
                    mutableStateOf(
                        PlaylistBrowserState(playlistId = "playlist-1"))
                }
                val destination = PlaylistFeatureDestination("browser")
                PlaylistTrackBrowserOverlay(
                    playlistName = "Saved",
                    libraryTracks =
                        listOf(libraryTrack("b"), libraryTrack("a")),
                    state = state,
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = NoopPlaylistFeatureDismissalPublisher,
                    onStateChange = { state = it },
                    onDismiss = {},
                    onConfirmed = {},
                    onConfirm = { playlistId, trackIds, _ ->
                        request = playlistId to trackIds
                    },
                )
            }

            onNode(hasText("Song b")).performClick()
            onNode(hasText("Song a")).performClick()
            onNodeWithTag("playlist-browser-confirm").performClick()
            waitForIdle()
            assertEquals("playlist-1" to listOf("b", "a"), request)
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
                    onSelectAll = {},
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
                            LibraryBottomBarContent.NowPlaying,
                            286,
                            AppNavigationLayout.Bottom,
                            390,
                        ),
                    hiddenFraction = 0f,
                    expectedLayout = AppNavigationLayout.Bottom,
                    expectedWidthPx = 390,
                    allowInteraction = true,
                )
            var cancelCount = 0
            var addCount = 0
            setContent {
                TrackSelectionBar(
                    selectedCount = 2,
                    onCancel = { cancelCount += 1 },
                    onSelectAll = {},
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
                    measurement =
                        LibraryBottomBarMeasurement(
                            selection, 286, AppNavigationLayout.Bottom, 390),
                    hiddenFraction = 0f,
                    expectedLayout = AppNavigationLayout.Bottom,
                    expectedWidthPx = 390,
                    allowInteraction = true,
                )
            var cancelCount = 0
            var addCount = 0
            var selectAllCount = 0
            setContent {
                TrackSelectionBar(
                    selectedCount = 2,
                    onCancel = { cancelCount += 1 },
                    onSelectAll = { selectAllCount += 1 },
                    onAddToPlaylist = { addCount += 1 },
                    interactive = presentation.isInteractive,
                )
            }

            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(3)
            val actions =
                onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            for (index in 0 until 3) {
                actions[index]
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)
            }
            actions[0].performClick()
            actions[1].performClick()
            actions[2].performClick()
            waitForIdle()
            assertEquals(1, cancelCount)
            assertEquals(1, addCount)
            assertEquals(1, selectAllCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionClearanceSurvivesSelectedCountChanges() {
        val measurement =
            LibraryBottomBarMeasurement(
                LibraryBottomBarContent.Selection(1),
                286,
                AppNavigationLayout.Bottom,
                390,
            )

        assertEquals(
            286,
            activeBottomBarClearancePx(
                LibraryBottomBarContent.Selection(2),
                measurement,
                AppNavigationLayout.Bottom,
                390,
            ),
        )
        assertEquals(
            LibraryBottomBarContent.Selection(1).kind,
            LibraryBottomBarContent.Selection(2).kind,
        )
        assertEquals(
            0,
            activeBottomBarClearancePx(
                LibraryBottomBarContent.NowPlaying,
                measurement,
                AppNavigationLayout.Bottom,
                390,
            ),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerRemainsDismissibleWhenFirstSelectedTrackDisappears() =
        runComposeUiTest {
            var visible by mutableStateOf(true)
            var dismissCount = 0
            setContent {
                val destination = PlaylistFeatureDestination("picker")
                if (visible) {
                    AddToPlaylistPickerOverlay(
                        playlists = listOf(playlist()),
                        state =
                            PlaylistPickerState(
                                trackIds = listOf("missing", "track-b")),
                        destination = destination,
                        appearanceSource =
                            rememberPlaylistFeatureAppearanceSource(
                                destination),
                        dismissalPublisher =
                            NoopPlaylistFeatureDismissalPublisher,
                        onStateChange = {},
                        onDismiss = {
                            dismissCount += 1
                            visible = false
                        },
                        onConfirmed = {},
                        onAppend = { _, _, _ -> },
                        onInlineCreate = { _, _, _ -> },
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
            var appendRequest: Pair<String, List<String>>? = null
            setContent {
                var state by remember {
                    mutableStateOf(
                        PlaylistPickerState(
                            trackIds = listOf("missing", "track-b")))
                }
                val destination = PlaylistFeatureDestination("picker")
                AddToPlaylistPickerOverlay(
                    playlists = listOf(playlist()),
                    state = state,
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = NoopPlaylistFeatureDismissalPublisher,
                    onStateChange = { state = it },
                    onDismiss = {},
                    onConfirmed = {},
                    onAppend = { playlistId, trackIds, _ ->
                        appendRequest = playlistId to trackIds
                    },
                    onInlineCreate = { _, _, _ -> },
                )
            }

            onAllNodes(hasContentDescription("Saved"))[0].performClick()
            onNodeWithTag("playlist-picker-append").performClick()
            waitForIdle()
            assertEquals(
                "playlist-1" to listOf("missing", "track-b"),
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

    private object NoopPlaylistFeatureDismissalPublisher :
        PlaylistFeatureDismissalPublisher {
        override fun publish(
            dismissal: PlaylistFeatureDismissal?,
            dispatch:
                (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
        ): () -> Unit = {}
    }
}
