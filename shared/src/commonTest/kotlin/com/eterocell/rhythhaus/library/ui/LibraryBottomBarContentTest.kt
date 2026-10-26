package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBottomBarContentTest {
    @Test
    fun selectionTakesPrecedenceOverPlaybackInTheSingleDockSlot() {
        assertEquals(
            LibraryBottomBarContent.Selection(selectedCount = 2),
            libraryBottomBarContent(
                route = LibraryRoute.Home,
                selectionState =
                    TrackSelectionState(
                        pageKey = TrackSelectionPageKey.HomeSongs,
                        selectedTrackIds = setOf("track-a", "track-b"),
                    ),
                isNowPlayingVisible = true,
                hasSelectedTrack = true,
            ),
        )
    }

    @Test
    fun playbackDockRequiresAResolvedCurrentTrack() {
        assertEquals(
            LibraryBottomBarContent.Hidden,
            libraryBottomBarContent(
                route = LibraryRoute.Home,
                selectionState = TrackSelectionState(),
                isNowPlayingVisible = true,
                hasSelectedTrack = false,
            ),
        )
        assertEquals(
            LibraryBottomBarContent.NowPlaying,
            libraryBottomBarContent(
                route = LibraryRoute.Home,
                selectionState = TrackSelectionState(),
                isNowPlayingVisible = true,
                hasSelectedTrack = true,
            ),
        )
    }

    @Test
    fun settingsAndStaleSelectionDoNotReplaceTheCurrentPlaybackDock() {
        assertEquals(
            LibraryBottomBarContent.Hidden,
            libraryBottomBarContent(
                route = LibraryRoute.Settings,
                selectionState =
                    TrackSelectionState(
                        pageKey = TrackSelectionPageKey.HomeSongs,
                        selectedTrackIds = setOf("track-a"),
                    ),
                isNowPlayingVisible = true,
                hasSelectedTrack = true,
            ),
        )
        assertEquals(
            LibraryBottomBarContent.NowPlaying,
            libraryBottomBarContent(
                route = LibraryRoute.PlaylistHub,
                selectionState =
                    TrackSelectionState(
                        pageKey = TrackSelectionPageKey.Album("stale"),
                        selectedTrackIds = setOf("track-a"),
                    ),
                isNowPlayingVisible = true,
                hasSelectedTrack = true,
            ),
        )
    }

    @Test
    fun dockClearanceRequiresMatchingContentLayoutAndWidth() {
        val selection = LibraryBottomBarContent.Selection(2)
        val measured =
            LibraryBottomBarMeasurement(
                content = selection,
                heightPx = 286,
                layout = AppNavigationLayout.Bottom,
                widthPx = 390,
            )

        assertEquals(
            286,
            activeBottomBarClearancePx(
                selection,
                measured,
                AppNavigationLayout.Bottom,
                390,
            ),
        )
        assertEquals(
            0,
            activeBottomBarClearancePx(
                LibraryBottomBarContent.Hidden,
                measured,
                AppNavigationLayout.Bottom,
                390,
            ),
        )
        assertEquals(
            0,
            activeBottomBarClearancePx(
                LibraryBottomBarContent.NowPlaying,
                measured,
                AppNavigationLayout.Bottom,
                390,
            ),
        )
        assertEquals(
            0,
            activeBottomBarClearancePx(
                selection,
                measured.copy(layout = AppNavigationLayout.Rail),
                AppNavigationLayout.Bottom,
                390,
            ),
        )
        assertEquals(
            0,
            activeBottomBarClearancePx(
                selection,
                measured.copy(widthPx = 391),
                AppNavigationLayout.Bottom,
                390,
            ),
        )
    }

    @Test
    fun dockRequiresCurrentMeasurementAndVisibleInteractiveHost() {
        val selection = LibraryBottomBarContent.Selection(2)
        val measured =
            LibraryBottomBarMeasurement(
                content = selection,
                heightPx = 286,
                layout = AppNavigationLayout.Bottom,
                widthPx = 390,
            )

        assertEquals(
            0f,
            activeBottomBarAlpha(
                selection,
                null,
                AppNavigationLayout.Bottom,
                390,
                hiddenFraction = 0f,
            ),
        )
        val unmeasured =
            libraryBottomBarPresentation(
                selection,
                null,
                AppNavigationLayout.Bottom,
                390,
                hiddenFraction = 0f,
                allowInteraction = true,
            )
        assertEquals(0, unmeasured.clearancePx)
        assertEquals(0f, unmeasured.alpha)
        assertEquals(false, unmeasured.isInteractive)
        assertEquals(
            0f,
            activeBottomBarAlpha(
                selection,
                measured,
                AppNavigationLayout.Bottom,
                390,
                hiddenFraction = 1f,
            ),
        )
        assertEquals(
            true,
            libraryBottomBarPresentation(
                    selection,
                    measured,
                    AppNavigationLayout.Bottom,
                    390,
                    hiddenFraction = 0f,
                    allowInteraction = true,
                )
                .isInteractive,
        )
        assertEquals(
            false,
            libraryBottomBarPresentation(
                    selection,
                    measured,
                    AppNavigationLayout.Bottom,
                    390,
                    hiddenFraction = 0f,
                    allowInteraction = false,
                )
                .isInteractive,
        )
    }
}
