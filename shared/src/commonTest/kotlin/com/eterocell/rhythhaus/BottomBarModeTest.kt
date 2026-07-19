package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.nowplaying.BottomBarMode
import com.eterocell.rhythhaus.nowplaying.bottomBarModeFor
import com.eterocell.rhythhaus.library.ui.LibraryBottomBarContent
import com.eterocell.rhythhaus.library.ui.LibraryRoute
import com.eterocell.rhythhaus.library.ui.TrackSelectionPageKey
import com.eterocell.rhythhaus.library.ui.TrackSelectionState
import com.eterocell.rhythhaus.library.ui.TrackSelectionBarSemantics
import com.eterocell.rhythhaus.library.ui.activeBottomBarClearancePx
import com.eterocell.rhythhaus.library.ui.activeBottomBarAlpha
import com.eterocell.rhythhaus.library.ui.LibraryBottomBarMeasurement
import com.eterocell.rhythhaus.library.ui.libraryBottomBarContent
import com.eterocell.rhythhaus.library.ui.trackSelectionBarSemantics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarModeTest {
    @Test
    fun emptyLibraryStillUsesBottomBarNavigationMode() {
        assertEquals(BottomBarMode.EmptyLibraryNavigation, bottomBarModeFor(track = null))
    }

    @Test
    fun selectionTakesPrecedenceOverNowPlayingInTheSingleBottomSlot() {
        assertEquals(
            LibraryBottomBarContent.Selection(selectedCount = 2),
            libraryBottomBarContent(
                route = LibraryRoute.Home,
                selectionState = TrackSelectionState(
                    pageKey = TrackSelectionPageKey.HomeSongs,
                    selectedTrackIds = setOf("track-a", "track-b"),
                ),
                isNowPlayingVisible = true,
            ),
        )
    }

    @Test
    fun visibleHomeSelectionStillOwnsTheSlotBesideWideDetailContent() {
        assertEquals(
            LibraryBottomBarContent.Selection(selectedCount = 1),
            libraryBottomBarContent(
                route = LibraryRoute.AlbumDetail("Night"),
                selectionState = TrackSelectionState(
                    pageKey = TrackSelectionPageKey.HomeSongs,
                    selectedTrackIds = setOf("track-a"),
                ),
                isNowPlayingVisible = true,
            ),
        )
    }

    @Test
    fun homeSelectionOwnsWidePlaylistHubSlotWhenNowPlayingIsVisible() {
        assertEquals(
            LibraryBottomBarContent.Selection(selectedCount = 1),
            libraryBottomBarContent(
                route = LibraryRoute.PlaylistHub,
                selectionState = TrackSelectionState(
                    pageKey = TrackSelectionPageKey.HomeSongs,
                    selectedTrackIds = setOf("track-a"),
                ),
                isNowPlayingVisible = true,
            ),
        )
    }

    @Test
    fun unsupportedRouteClearsTheBottomSlotSelectionContent() {
        assertEquals(
            LibraryBottomBarContent.Hidden,
            libraryBottomBarContent(
                route = LibraryRoute.Settings,
                selectionState = TrackSelectionState(
                    pageKey = TrackSelectionPageKey.HomeSongs,
                    selectedTrackIds = setOf("track-a"),
                ),
                isNowPlayingVisible = true,
            ),
        )
        assertEquals(
            LibraryBottomBarContent.NowPlaying,
            libraryBottomBarContent(
                route = LibraryRoute.PlaylistHub,
                selectionState = TrackSelectionState(
                    pageKey = TrackSelectionPageKey.Album("stale"),
                    selectedTrackIds = setOf("track-a"),
                ),
                isNowPlayingVisible = true,
            ),
        )
    }

    @Test
    fun activeBottomClearanceUsesTheMeasuredSlotAndHiddenContentUsesZero() {
        val selection = LibraryBottomBarContent.Selection(2)
        assertEquals(286, activeBottomBarClearancePx(selection, LibraryBottomBarMeasurement(selection, 286)))
        assertEquals(0, activeBottomBarClearancePx(LibraryBottomBarContent.Hidden, LibraryBottomBarMeasurement(selection, 286)))
        assertEquals(
            0,
            activeBottomBarClearancePx(LibraryBottomBarContent.NowPlaying, LibraryBottomBarMeasurement(selection, 286)),
        )
    }

    @Test
    fun unmeasuredOrStaleBottomContentRemainsInvisibleUntilItsOwnMeasurementArrives() {
        val selection = LibraryBottomBarContent.Selection(2)
        val staleNowPlaying = LibraryBottomBarMeasurement(LibraryBottomBarContent.NowPlaying, 286)

        assertEquals(0f, activeBottomBarAlpha(selection, null, hiddenFraction = 0f))
        assertEquals(0f, activeBottomBarAlpha(selection, staleNowPlaying, hiddenFraction = 0f))
        assertEquals(
            1f,
            activeBottomBarAlpha(selection, LibraryBottomBarMeasurement(selection, 286), hiddenFraction = 0f),
        )
    }

    @Test
    fun selectionBarSemanticsExposeLocalizedCountCancelAndAddLabels() {
        assertEquals(
            TrackSelectionBarSemantics(
                selectedCountDescription = "2 tracks selected",
                cancelDescription = "Cancel selection",
                addToPlaylistDescription = "Add selected tracks to playlist",
            ),
            trackSelectionBarSemantics(
                selectedCountDescription = "2 tracks selected",
                cancelDescription = "Cancel selection",
                addToPlaylistDescription = "Add selected tracks to playlist",
            ),
        )
    }
}
