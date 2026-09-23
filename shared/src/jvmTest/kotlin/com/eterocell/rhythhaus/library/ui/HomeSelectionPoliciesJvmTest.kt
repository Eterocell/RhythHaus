package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSelectionPoliciesJvmTest {
    @Test
    fun leavingAFlatHomeSurfaceForAGroupedModeClearsSelectionExactlyOnce() {
        listOf(
            BrowseMode.Songs,
            BrowseMode.Favorites,
            BrowseMode.RecentlyPlayed,
            BrowseMode.RecentlyAdded,
        ).forEach { source ->
            listOf(BrowseMode.Albums, BrowseMode.Artists).forEach { destination ->
                val actions = mutableListOf<TrackSelectionAction>()
                val browseModes = mutableListOf<BrowseMode>()
                dispatchHomeBrowseModeChange(
                    source,
                    destination,
                    { actions += it },
                    { browseModes += it },
                )
                assertEquals(
                    listOf<TrackSelectionAction>(TrackSelectionAction.RouteChanged(null)),
                    actions,
                )
                assertEquals(listOf(destination), browseModes)
            }
        }
    }

    @Test
    fun movingBetweenRecentFlatHomeSurfacesKeepsSelection() {
        listOf(
            BrowseMode.Songs to BrowseMode.RecentlyPlayed,
            BrowseMode.RecentlyPlayed to BrowseMode.RecentlyAdded,
            BrowseMode.RecentlyAdded to BrowseMode.Favorites,
            BrowseMode.Favorites to BrowseMode.RecentlyPlayed,
        ).forEach { (source, destination) ->
            val actions = mutableListOf<TrackSelectionAction>()
            dispatchHomeBrowseModeChange(source, destination, actions::add) {}
            assertEquals(emptyList(), actions)
        }
    }

    @Test
    fun recentModesUseTheFlatHomeSelectionPageAndBackPolicy() {
        assertEquals(
            TrackSelectionPageKey.HomeSongs,
            trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.RecentlyPlayed),
        )
        assertEquals(
            TrackSelectionPageKey.HomeSongs,
            trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.RecentlyAdded),
        )
    }

    @Test
    fun movingBetweenFlatHomeSurfacesKeepsSelection() {
        listOf(
                BrowseMode.Songs to BrowseMode.Favorites,
                BrowseMode.Favorites to BrowseMode.Songs,
            )
            .forEach { (source, destination) ->
                val actions = mutableListOf<TrackSelectionAction>()
                dispatchHomeBrowseModeChange(source, destination, actions::add) {}
                assertEquals(emptyList(), actions)
            }
    }
}
