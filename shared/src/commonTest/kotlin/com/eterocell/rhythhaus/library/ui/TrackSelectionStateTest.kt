package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackSelectionStateTest {
    private val home = TrackSelectionPageKey.HomeSongs
    private val album = TrackSelectionPageKey.Album("Night")

    @Test
    fun startEntersPageAndSelectsTrack() {
        assertEquals(
            TrackSelectionState(home, setOf("track-a")),
            reduceTrackSelection(
                TrackSelectionState(),
                TrackSelectionAction.Start(home, "track-a"),
            ),
        )
    }

    @Test
    fun selectIsIdempotentAndStalePagesAreIgnored() {
        val selected =
            reduceTrackSelection(
                TrackSelectionState(home, setOf("track-a")),
                TrackSelectionAction.Select(home, "track-a"),
            )

        assertEquals(setOf("track-a"), selected.selectedTrackIds)
        assertEquals(
            selected,
            reduceTrackSelection(
                selected, TrackSelectionAction.Select(album, "track-b")),
        )
    }

    @Test
    fun toggleAddsAndRemovesTrack() {
        val selected =
            reduceTrackSelection(
                TrackSelectionState(home),
                TrackSelectionAction.Toggle(home, "track-a"),
            )

        assertEquals(TrackSelectionState(home, setOf("track-a")), selected)
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(
                selected, TrackSelectionAction.Toggle(home, "track-a")),
        )
    }

    @Test
    fun finalDeselectionExitsSelectionMode() {
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(
                    TrackSelectionState(home, setOf("track-a")),
                    TrackSelectionAction.Select(home, "track-a"),
                )
                .let {
                    reduceTrackSelection(
                        it, TrackSelectionAction.Toggle(home, "track-a"))
                },
        )
    }

    @Test
    fun cancelAndCompletedExitSelectionMode() {
        val active = TrackSelectionState(home, setOf("track-a"))

        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(active, TrackSelectionAction.Cancel))
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(active, TrackSelectionAction.Completed))
    }

    @Test
    fun routeChangeClearsSelectionForAnyDestination() {
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(
                TrackSelectionState(home, setOf("track-a")),
                TrackSelectionAction.RouteChanged(album),
            ),
        )
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(
                TrackSelectionState(home, setOf("track-a")),
                TrackSelectionAction.RouteChanged(null)))
    }

    @Test
    fun searchReconciliationDropsTracksNoLongerVisible() {
        assertEquals(
            TrackSelectionState(TrackSelectionPageKey.Search, setOf("track-b")),
            reduceTrackSelection(
                TrackSelectionState(
                    TrackSelectionPageKey.Search, setOf("track-a", "track-b")),
                TrackSelectionAction.ReconcileVisible(
                    TrackSelectionPageKey.Search, listOf("track-b", "track-c")),
            ),
        )
    }

    @Test
    fun reconciliationRemovesMalformedBlankIdsAndNormalizesBlankOnlyState() {
        val malformed = TrackSelectionState(home, setOf("track-a", ""))

        assertEquals(
            TrackSelectionState(home, setOf("track-a")),
            reduceTrackSelection(
                malformed,
                TrackSelectionAction.ReconcileVisible(
                    home, listOf("track-a", "")),
            ),
        )
        assertEquals(
            TrackSelectionState(),
            reduceTrackSelection(
                TrackSelectionState(home, setOf("")),
                TrackSelectionAction.ReconcileVisible(home, listOf("")),
            ),
        )
    }

    @Test
    fun stalePageToggleAndReconciliationAreNoOps() {
        val state = TrackSelectionState(home, setOf("track-a"))

        assertEquals(
            state,
            reduceTrackSelection(
                state, TrackSelectionAction.Toggle(album, "track-b")))
        assertEquals(
            state,
            reduceTrackSelection(
                state,
                TrackSelectionAction.ReconcileVisible(
                    album, listOf("track-b"))),
        )
    }

    @Test
    fun blankIdsAreRejected() {
        val state = TrackSelectionState(home, setOf("track-a"))
        val actions =
            listOf(
                TrackSelectionAction.Start(home, ""),
                TrackSelectionAction.Select(home, ""),
                TrackSelectionAction.Toggle(home, ""),
                TrackSelectionAction.ReconcileVisible(
                    home, listOf("track-a", "")),
            )

        actions
            .fold(state) { current, action ->
                reduceTrackSelection(current, action)
            }
            .also { assertEquals(state, it) }
    }

    @Test
    fun orderedSelectionUsesVisibleOrderOnly() {
        val state =
            TrackSelectionState(
                home, setOf("track-a", "track-b", "track-hidden"))

        assertEquals(
            listOf("track-b", "track-a"),
            orderedSelectedTrackIds(state, home, listOf("track-b", "track-a")),
        )
    }

    @Test
    fun orderedSelectionIgnoresDuplicateVisibleIds() {
        val state = TrackSelectionState(home, setOf("track-a", "track-b"))

        assertEquals(
            listOf("track-b", "track-a"),
            orderedSelectedTrackIds(
                state,
                home,
                listOf("track-b", "track-b", "track-a", "track-a")),
        )
    }

    @Test
    fun orderedSelectionWithMismatchedPageIsEmpty() {
        assertEquals(
            emptyList(),
            orderedSelectedTrackIds(
                TrackSelectionState(home, setOf("track-a")),
                album,
                listOf("track-a"),
            ),
        )
    }
}
