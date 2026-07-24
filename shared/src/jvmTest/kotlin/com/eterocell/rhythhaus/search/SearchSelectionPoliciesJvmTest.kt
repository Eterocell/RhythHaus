package com.eterocell.rhythhaus.search

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ui.BrowseMode
import com.eterocell.rhythhaus.library.ui.TrackSelectionAction
import com.eterocell.rhythhaus.library.ui.TrackSelectionPageKey
import com.eterocell.rhythhaus.library.ui.dispatchHomeBrowseModeChange
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchSelectionPoliciesJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun normalClickPlaysOnlyOutsideSelection() = runComposeUiTest {
        var playCount = 0
        var toggleCount = 0
        setContent {
            SearchResultRow(
                track = testTrack(),
                isNowPlaying = false,
                isPlaying = false,
                selectionModeActive = false,
                isSelected = false,
                onPlay = { playCount += 1 },
                onToggleSelection = { toggleCount += 1 },
                onStartSelection = {},
            )
        }

        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .performClick()
        waitForIdle()

        assertEquals(1, playCount)
        assertEquals(0, toggleCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longClickStartsSelectionWithoutPlayback() = runComposeUiTest {
        var playCount = 0
        var startCount = 0
        setContent {
            SearchResultRow(
                track = testTrack(),
                isNowPlaying = false,
                isPlaying = false,
                selectionModeActive = false,
                isSelected = false,
                onPlay = { playCount += 1 },
                onToggleSelection = {},
                onStartSelection = { startCount += 1 },
            )
        }

        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .performSemanticsAction(SemanticsActions.OnLongClick)
        waitForIdle()

        assertEquals(0, playCount)
        assertEquals(1, startCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionModeRowAndCheckboxEachToggleExactlyOnceWithoutPlayback() =
        runComposeUiTest {
            var playCount = 0
            var toggleCount = 0
            setContent {
                SearchResultRow(
                    track = testTrack(),
                    isNowPlaying = false,
                    isPlaying = false,
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

            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role, Role.Checkbox) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.Off),
                )
                .performClick()
            waitForIdle()

            assertEquals(0, playCount)
            assertEquals(2, toggleCount)
        }

    @Test
    fun changingFilteredIdsDispatchesEachSearchReconciliation() {
        val actions = mutableListOf<TrackSelectionAction>()

        dispatchSearchVisibleTrackIds(listOf("track-a", "track-b")) {
            actions += it
        }
        dispatchSearchVisibleTrackIds(listOf("track-b")) { actions += it }

        assertEquals(
            listOf<TrackSelectionAction>(
                TrackSelectionAction.ReconcileVisible(
                    TrackSelectionPageKey.Search,
                    listOf("track-a", "track-b"),
                ),
                TrackSelectionAction.ReconcileVisible(
                    TrackSelectionPageKey.Search,
                    listOf("track-b"),
                ),
            ),
            actions,
        )
    }

    @Test
    fun leavingHomeSongsForAlbumsOrArtistsClearsSelectionExactlyOnce() {
        listOf(BrowseMode.Albums, BrowseMode.Artists).forEach { destination ->
            val actions = mutableListOf<TrackSelectionAction>()
            val modes = mutableListOf<BrowseMode>()

            dispatchHomeBrowseModeChange(
                currentMode = BrowseMode.Songs,
                nextMode = destination,
                onTrackSelectionAction = { actions += it },
                onBrowseModeChange = { modes += it },
            )

            assertEquals(
                listOf<TrackSelectionAction>(
                    TrackSelectionAction.RouteChanged(null)),
                actions)
            assertEquals(listOf(destination), modes)
        }
    }

    private fun testTrack() =
        LibraryTrack(
            id = "track-1",
            sourceId = "source-1",
            sourceLocalKey = "song.mp3",
            audioSource = AudioSource.FilePath("song.mp3"),
            displayName = "song.mp3",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationMillis = 180_000,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = "scan-1",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )
}
