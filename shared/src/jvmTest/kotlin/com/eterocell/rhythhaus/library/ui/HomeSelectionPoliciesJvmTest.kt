package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSelectionPoliciesJvmTest {
    @Test
    fun leavingAFlatHomeSurfaceForAGroupedModeClearsSelectionExactlyOnce() {
        listOf(BrowseMode.Songs, BrowseMode.Favorites).forEach { source ->
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
                    listOf<TrackSelectionAction>(
                        TrackSelectionAction.RouteChanged(null)),
                    actions,
                )
                assertEquals(listOf(destination), browseModes)
            }
        }
    }

    @Test
    fun movingBetweenFlatHomeSurfacesKeepsSelection() {
        listOf(
            BrowseMode.Songs to BrowseMode.Favorites,
            BrowseMode.Favorites to BrowseMode.Songs,
        ).forEach { (source, destination) ->
            val actions = mutableListOf<TrackSelectionAction>()
            dispatchHomeBrowseModeChange(source, destination, actions::add) {}
            assertEquals(emptyList(), actions)
        }
    }
}
