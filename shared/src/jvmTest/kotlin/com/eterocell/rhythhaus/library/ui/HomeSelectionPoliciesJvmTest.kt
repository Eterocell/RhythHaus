package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeSelectionPoliciesJvmTest {
    @Test
    fun leavingHomeSongsForAlbumsOrArtistsClearsSelectionExactlyOnce() {
        listOf(BrowseMode.Albums, BrowseMode.Artists).forEach { destination ->
            val actions = mutableListOf<TrackSelectionAction>()
            val browseModes = mutableListOf<BrowseMode>()
            dispatchHomeBrowseModeChange(
                BrowseMode.Songs,
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
