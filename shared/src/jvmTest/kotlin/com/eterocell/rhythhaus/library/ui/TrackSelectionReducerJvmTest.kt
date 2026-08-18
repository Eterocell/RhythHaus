package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackSelectionReducerJvmTest {
    @Test
    fun selectAllReplacesSelectionWithPageTracksOnMatchingPage() {
        val state =
            reduceTrackSelection(
                TrackSelectionState(
                    TrackSelectionPageKey.Album("A"), setOf("t-1")),
                TrackSelectionAction.SelectAll(
                    TrackSelectionPageKey.Album("A"),
                    listOf("t-1", "t-2", "t-3"),
                ),
            )

        assertEquals(TrackSelectionPageKey.Album("A"), state.pageKey)
        assertEquals(setOf("t-1", "t-2", "t-3"), state.selectedTrackIds)
    }

    @Test
    fun selectAllIgnoresMismatchedPageKey() {
        val state =
            reduceTrackSelection(
                TrackSelectionState(
                    TrackSelectionPageKey.Album("A"), setOf("t-1")),
                TrackSelectionAction.SelectAll(
                    TrackSelectionPageKey.Album("B"),
                    listOf("x", "y"),
                ),
            )

        assertEquals(TrackSelectionPageKey.Album("A"), state.pageKey)
        assertEquals(setOf("t-1"), state.selectedTrackIds)
    }

    @Test
    fun selectAllFiltersBlankIdsAndStartsSelectionFromEmpty() {
        val state =
            reduceTrackSelection(
                TrackSelectionState(),
                TrackSelectionAction.SelectAll(
                    TrackSelectionPageKey.HomeSongs,
                    listOf("a", "", "b"),
                ),
            )

        assertEquals(TrackSelectionPageKey.HomeSongs, state.pageKey)
        assertEquals(setOf("a", "b"), state.selectedTrackIds)
    }

    @Test
    fun selectAllThenToggleRemovesSingleTrack() {
        val all =
            reduceTrackSelection(
                TrackSelectionState(
                    TrackSelectionPageKey.Album("A"), emptySet()),
                TrackSelectionAction.SelectAll(
                    TrackSelectionPageKey.Album("A"),
                    listOf("t-1", "t-2"),
                ),
            )
        val after =
            reduceTrackSelection(
                all,
                TrackSelectionAction.Toggle(
                    TrackSelectionPageKey.Album("A"), "t-1"))

        assertEquals(setOf("t-2"), after.selectedTrackIds)
    }

    @Test
    fun selectAllOnEmptyPageClearsSelection() {
        val state =
            reduceTrackSelection(
                TrackSelectionState(
                    TrackSelectionPageKey.Album("A"), setOf("t-1")),
                TrackSelectionAction.SelectAll(
                    TrackSelectionPageKey.Album("A"), emptyList()),
            )

        assertEquals(null, state.pageKey)
        assertEquals(emptySet(), state.selectedTrackIds)
    }
}
