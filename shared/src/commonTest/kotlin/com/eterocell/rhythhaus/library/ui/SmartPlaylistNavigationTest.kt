package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartPlaylistNavigationTest {
    @Test
    fun outgoingSmartDetailCannotRecoverOverNewRoute() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistHub)
        state.pushRoute(LibraryRoute.SmartPlaylistDetail("smart"))
        val outgoing = state.navigation.currentEntry
        var selectionClears = 0
        val messages = mutableListOf<PlaylistStateAction>()
        val orchestrator =
            PlaylistDetailRouteOrchestrator(
                state, { selectionClears++ }, messages::add)
        state.replaceTopRoute(LibraryRoute.Settings)

        orchestrator.recoverStalePlaylistDetail(outgoing, "No longer available")

        assertEquals(LibraryRoute.Settings, state.navigation.current)
        assertEquals(0, selectionClears)
        assertEquals(emptyList(), messages)
    }

    @Test
    fun deletionOnlyInvalidatesExactConfirmedSmartDestination() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistHub)
        state.pushRoute(LibraryRoute.SmartPlaylistDetail("smart"))
        val origin = state.navigation.currentEntry
        val retained =
            PlaylistSnapshot(
                smartPlaylists =
                    listOf(
                        SmartPlaylistSummary(
                            "smart",
                            "Smart",
                            SmartPlaylistRule.Favorites,
                            1,
                            1)))
        state.completeDisplayedSmartPlaylistDeletion(retained, "smart", origin)
        assertEquals(
            LibraryRoute.SmartPlaylistDetail("smart"), state.navigation.current)
        state.replaceTopRoute(LibraryRoute.SmartPlaylistDetail("smart"))
        state.completeDisplayedSmartPlaylistDeletion(
            PlaylistSnapshot(), "smart", origin)
        assertEquals(
            LibraryRoute.SmartPlaylistDetail("smart"), state.navigation.current)
        state.completeDisplayedSmartPlaylistDeletion(
            PlaylistSnapshot(), "smart", state.navigation.currentEntry)
        assertEquals(LibraryRoute.PlaylistHub, state.navigation.current)
    }

    @Test
    fun smartRouteRetainsNowPlayingSurfaceAndPopsToHub() {
        val stack =
            LibraryNavigationStack()
                .push(LibraryRoute.PlaylistHub)
                .push(LibraryRoute.SmartPlaylistDetail("smart"))
        assertEquals(true, routePermitsNowPlayingBar(stack.current))
        assertEquals(LibraryRoute.PlaylistHub, stack.pop().current)
    }
}
