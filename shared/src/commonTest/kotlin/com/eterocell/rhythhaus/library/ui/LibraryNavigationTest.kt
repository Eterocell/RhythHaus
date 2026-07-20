package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryNavigationTest {
    @Test
    fun drillDownRowDispatchesOnlyTrackSelectionWithSelectedTrack() {
        val selectedTrack = testTrack(id = "selected")
        val selectedTracks = mutableListOf<Track>()
        var transportToggleCount = 0

        dispatchDrillDownAction(
            action = DrillDownAction.SelectTrack(selectedTrack),
            onTrackClick = { selectedTracks.add(it) },
            onPlayPause = { transportToggleCount += 1 },
        )

        assertEquals(listOf(selectedTrack), selectedTracks)
        assertEquals(0, transportToggleCount)
    }

    @Test
    fun drillDownTransportDispatchesOnlyPlayPause() {
        val selectedTracks = mutableListOf<Track>()
        var transportToggleCount = 0

        dispatchDrillDownAction(
            action = DrillDownAction.ToggleTransport,
            onTrackClick = { selectedTracks.add(it) },
            onPlayPause = { transportToggleCount += 1 },
        )

        assertTrue(selectedTracks.isEmpty())
        assertEquals(1, transportToggleCount)
    }

    @Test
    fun libraryHomeTopContentPaddingPreservesSystemBarInset() {
        assertEquals(
            37.dp,
            libraryHomeTopContentPadding(systemBarTopPadding = 37.dp),
        )
    }

    @Test
    fun rootStackStartsAtHomeAndCannotPopPastHome() {
        val stack = LibraryNavigationStack()

        assertEquals(LibraryRoute.Home, stack.current)
        assertFalse(stack.canPop)
        assertEquals(stack, stack.pop())
    }

    @Test
    fun duplicateTopPushIsNoOp() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Search)

        assertEquals(listOf(LibraryRoute.Home, LibraryRoute.Search), stack.routes)
    }

    @Test
    fun searchOpenedFromAlbumReturnsToAlbum() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val stack = LibraryNavigationStack()
            .push(album)
            .push(LibraryRoute.Search)

        assertEquals(LibraryRoute.Search, stack.current)
        assertTrue(stack.canPop)
        assertEquals(album, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun nowPlayingOpenedFromArtistReturnsToArtist() {
        val artist = LibraryRoute.ArtistDetail("John Coltrane")
        val stack = LibraryNavigationStack()
            .push(artist)
            .push(LibraryRoute.NowPlaying)

        assertEquals(LibraryRoute.NowPlaying, stack.current)
        assertEquals(artist, stack.pop().current)
    }

    @Test
    fun clearDialogPopsBackToSettingsOrigin() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Settings)
            .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertEquals(LibraryRoute.Settings, stack.pop().current)
    }

    @Test
    fun settingsAboutAndLibrariesPopBackThroughSettingsToHome() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Settings)
            .push(LibraryRoute.SettingsAbout)
            .push(LibraryRoute.OpenSourceLibraries)

        assertEquals(LibraryRoute.OpenSourceLibraries, stack.current)
        assertEquals(LibraryRoute.SettingsAbout, stack.pop().current)
        assertEquals(LibraryRoute.Settings, stack.pop().pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().pop().current)
    }

    @Test
    fun playlistHubAndKeyedDetailUseTypedStackRoutes() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.PlaylistHub)
            .push(LibraryRoute.PlaylistDetail("playlist-1"))

        assertEquals(LibraryRoute.PlaylistDetail("playlist-1"), stack.current)
        assertEquals(LibraryRoute.PlaylistHub, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun playlistRoutesPreserveNowPlayingBarPolicy() {
        assertTrue(routePermitsNowPlayingBar(LibraryRoute.PlaylistHub))
        assertTrue(routePermitsNowPlayingBar(LibraryRoute.PlaylistDetail("playlist-1")))
    }

    @Test
    fun playlistRoutesUseContentOwnershipInsteadOfSettingsOverlayOwnership() {
        LibraryAdaptiveLayoutMode.entries.forEach { mode ->
            assertFalse(libraryRouteRendersAsActiveOverlay(LibraryRoute.PlaylistHub, mode))
            assertFalse(libraryRouteRendersAsActiveOverlay(LibraryRoute.PlaylistDetail("playlist-1"), mode))
        }
    }

    @Test
    fun settingsAboutRoutesUseActiveOverlayOwnershipInCompactAndWideLayouts() {
        val routes = listOf(
            LibraryRoute.SettingsAbout,
            LibraryRoute.OpenSourceLibraries,
        )

        LibraryAdaptiveLayoutMode.entries.forEach { mode ->
            routes.forEach { route ->
                assertTrue(libraryRouteRendersAsActiveOverlay(route = route, mode = mode))
            }
        }

        assertFalse(
            libraryRouteRendersAsActiveOverlay(
                route = LibraryRoute.AlbumDetail("Blue Train"),
                mode = LibraryAdaptiveLayoutMode.ListDetail,
            ),
        )
    }

    @Test
    fun clearDialogDoesNotUseRouteContentAnimation() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Settings)
            .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.ClearLibraryDialog))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Settings))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Search))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Home))
    }

    @Test
    fun adaptiveLayoutUsesCompactForPhonePortrait() {
        assertEquals(
            LibraryAdaptiveLayoutMode.Compact,
            libraryAdaptiveLayoutModeFor(widthDp = 390f, heightDp = 844f),
        )
    }

    @Test
    fun adaptiveLayoutUsesCompactForNarrowPortraitTablet() {
        assertEquals(
            LibraryAdaptiveLayoutMode.Compact,
            libraryAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 1000f),
        )
    }

    @Test
    fun adaptiveLayoutUsesListDetailForWideTablet() {
        assertEquals(
            LibraryAdaptiveLayoutMode.ListDetail,
            libraryAdaptiveLayoutModeFor(widthDp = 840f, heightDp = 1180f),
        )
    }

    @Test
    fun adaptiveLayoutUsesListDetailForLandscapeMediumWidth() {
        assertEquals(
            LibraryAdaptiveLayoutMode.ListDetail,
            libraryAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 500f),
        )
    }

    @Test
    fun adaptiveLayoutUsesListDetailForDesktopWidth() {
        assertEquals(
            LibraryAdaptiveLayoutMode.ListDetail,
            libraryAdaptiveLayoutModeFor(widthDp = 1200f, heightDp = 800f),
        )
    }

    @Test
    fun nowPlayingAdaptiveLayoutUsesCompactForPhonePortrait() {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(widthDp = 390f, heightDp = 844f),
        )
    }

    @Test
    fun nowPlayingAdaptiveLayoutUsesCompactForNarrowPortraitTablet() {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 1000f),
        )
    }

    @Test
    fun nowPlayingAdaptiveLayoutUsesSplitForWideTablet() {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(widthDp = 840f, heightDp = 1180f),
        )
    }

    @Test
    fun nowPlayingAdaptiveLayoutUsesSplitForLandscapeMediumWidth() {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 500f),
        )
    }

    @Test
    fun nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth() {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(widthDp = 1200f, heightDp = 800f),
        )
    }

    @Test
    fun pushingHomeReturnsToRoot() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("A"))
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Home)

        assertEquals(listOf(LibraryRoute.Home), stack.routes)
        assertEquals(LibraryRoute.Home, stack.current)
    }

    @Test
    fun pushingNestedRouteClassifiesAsPush() {
        val from = LibraryNavigationStack()
        val to = from.push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(LibraryNavigationTransition.Push, classifyNavigationTransition(from, to))
    }

    @Test
    fun poppingNestedRouteClassifiesAsPop() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.pop()

        assertEquals(LibraryNavigationTransition.Pop, classifyNavigationTransition(from, to))
    }

    @Test
    fun pushingHomeClassifiesAsRoot() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Home)

        assertEquals(LibraryNavigationTransition.Root, classifyNavigationTransition(from, to))
    }

    @Test
    fun replacingTopRouteClassifiesAsReplace() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.replaceTop(LibraryRoute.Settings)

        assertEquals(LibraryNavigationTransition.Replace, classifyNavigationTransition(from, to))
    }

    @Test
    fun duplicateTopPushClassifiesAsNone() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Search)

        assertEquals(LibraryNavigationTransition.None, classifyNavigationTransition(from, to))
    }

    @Test
    fun libraryScrollDownWithinSameItemHidesNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)

        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
            ),
        )
    }

    @Test
    fun libraryScrollUpWithinSameItemShowsNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun libraryScrollDownAcrossItemBoundaryHidesNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)

        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
            ),
        )
    }

    @Test
    fun libraryScrollUpAcrossItemBoundaryShowsNowPlayingBar() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun libraryScrollJitterKeepsCurrentNowPlayingBarVisibility() {
        val previous = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 31)

        assertTrue(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = true,
                jitterThresholdPx = 2,
            ),
        )
        assertFalse(
            decideNowPlayingBarVisibilityForLibraryScroll(
                previous = previous,
                current = current,
                currentlyVisible = false,
                jitterThresholdPx = 2,
            ),
        )
    }

    @Test
    fun wideDetailRouteReplacementOnlyAppliesBetweenDetailRoutesInListDetailMode() {
        assertTrue(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.ArtistDetail("B"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.Compact,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.ArtistDetail("B"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.Home,
                next = LibraryRoute.AlbumDetail("A"),
            ),
        )
        assertFalse(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.Search,
            ),
        )
        assertTrue(
            shouldReplaceWideDetailRoute(
                mode = LibraryAdaptiveLayoutMode.ListDetail,
                current = LibraryRoute.AlbumDetail("A"),
                next = LibraryRoute.PlaylistDetail("playlist-1"),
            ),
        )
    }

    @Test
    fun navigationActionsApplyExistingStackSemantics() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val pushed = applyNavigationAction(LibraryNavigationStack(), LibraryNavigationAction.Push(album))
        assertEquals(listOf(LibraryRoute.Home, album), pushed.routes)

        val replaced = applyNavigationAction(pushed, LibraryNavigationAction.ReplaceTop(LibraryRoute.ArtistDetail("Alice")))
        assertEquals(listOf(LibraryRoute.Home, LibraryRoute.ArtistDetail("Alice")), replaced.routes)

        assertEquals(LibraryRoute.Home, applyNavigationAction(replaced, LibraryNavigationAction.Pop).current)
        assertEquals(listOf(LibraryRoute.Home), applyNavigationAction(replaced, LibraryNavigationAction.PopToRoot).routes)
    }

    @Test
    fun navigationActionTransitionMatchesStackChange() {
        val from = LibraryNavigationStack().push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(
            LibraryNavigationTransition.Push,
            transitionForNavigationAction(from, LibraryNavigationAction.Push(LibraryRoute.Search)),
        )
        assertEquals(
            LibraryNavigationTransition.Pop,
            transitionForNavigationAction(from, LibraryNavigationAction.Pop),
        )
        assertEquals(
            LibraryNavigationTransition.Replace,
            transitionForNavigationAction(from, LibraryNavigationAction.ReplaceTop(LibraryRoute.ArtistDetail("Alice"))),
        )
        assertEquals(
            LibraryNavigationTransition.Root,
            transitionForNavigationAction(from, LibraryNavigationAction.PopToRoot),
        )
        assertEquals(
            LibraryNavigationTransition.None,
            transitionForNavigationAction(LibraryNavigationStack(), LibraryNavigationAction.Pop),
        )
        assertEquals(
            LibraryNavigationTransition.None,
            transitionForNavigationAction(LibraryNavigationStack(), LibraryNavigationAction.PopToRoot),
        )
    }

    @Test
    fun playbackTrackSelectionOverridesOnlyWhenPlaybackHasTrack() {
        assertEquals("playing", selectedTrackIdForPlaybackChange("selected", "playing"))
        assertEquals("selected", selectedTrackIdForPlaybackChange("selected", null))
        assertEquals(null, selectedTrackIdForPlaybackChange(null, null))
    }

    @Test
    fun bottomBarVisibilityStateStoresPreviousScrollPosition() {
        val initial = LibraryBottomBarVisibilityState(visible = true, previousScrollPosition = null)
        val first = updateBottomBarVisibilityForScroll(
            state = initial,
            current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10),
        )
        assertTrue(first.visible)
        assertEquals(LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10), first.previousScrollPosition)

        val second = updateBottomBarVisibilityForScroll(
            state = first,
            current = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30),
        )
        assertFalse(second.visible)
        assertEquals(LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30), second.previousScrollPosition)
    }

    @Test
    fun libraryAppStateNavigationActionsRecordTransitions() {
        val state = LibraryAppState(initialSelectedTrackId = null)

        state.pushRoute(LibraryRoute.AlbumDetail("A"))
        assertEquals(LibraryRoute.AlbumDetail("A"), state.navigation.current)
        assertEquals(LibraryNavigationTransition.Push, state.lastNavigationTransition)

        state.replaceTopRoute(LibraryRoute.ArtistDetail("B"))
        assertEquals(LibraryRoute.ArtistDetail("B"), state.navigation.current)
        assertEquals(LibraryNavigationTransition.Replace, state.lastNavigationTransition)

        state.popRoute()
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertEquals(LibraryNavigationTransition.Pop, state.lastNavigationTransition)
    }

    @Test
    fun stalePlaylistDetailRecoveryReplacesDetailWithHub() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistHub)
        state.pushRoute(LibraryRoute.PlaylistDetail("missing"))

        var message: String? = null
        state.recoverStalePlaylistDetail("playlist_changed") { message = it }

        assertEquals(LibraryRoute.PlaylistHub, state.navigation.current)
        assertEquals(LibraryNavigationTransition.Replace, state.lastNavigationTransition)
        assertEquals("playlist_changed", message)
    }

    @Test
    fun libraryAppStateTracksNowPlayingAndBottomBarVisibility() {
        val state = LibraryAppState(initialSelectedTrackId = "a")

        state.syncSelectedTrackWithPlayback("b")
        assertEquals("b", state.selectedTrackId)

        state.showNowPlaying()
        assertTrue(state.showNowPlaying)
        state.hideNowPlaying()
        assertFalse(state.showNowPlaying)

        state.updateNowPlayingBarVisibilityForScroll(LibraryScrollPosition(0, 10))
        assertTrue(state.isNowPlayingBarVisible)
        state.updateNowPlayingBarVisibilityForScroll(LibraryScrollPosition(0, 30))
        assertFalse(state.isNowPlayingBarVisible)
    }

    @Test
    fun settingsInformationRoutesSuppressNowPlayingBar() {
        assertFalse(routePermitsNowPlayingBar(LibraryRoute.Settings))
        assertFalse(routePermitsNowPlayingBar(LibraryRoute.SettingsAbout))
        assertFalse(routePermitsNowPlayingBar(LibraryRoute.OpenSourceLibraries))
    }

    @Test
    fun otherRoutesPermitNowPlayingBar() {
        val permittedRoutes = listOf(
            LibraryRoute.Home,
            LibraryRoute.AlbumDetail("Album"),
            LibraryRoute.ArtistDetail("Artist"),
            LibraryRoute.NowPlaying,
            LibraryRoute.Search,
            LibraryRoute.ClearLibraryDialog,
            LibraryRoute.PlaylistHub,
            LibraryRoute.PlaylistDetail("playlist-1"),
        )

        permittedRoutes.forEach { route ->
            assertTrue(routePermitsNowPlayingBar(route), "Expected $route to permit the bar")
        }
    }

    @Test
    fun routeEligibilityCombinesWithExistingVisibility() {
        assertTrue(shouldShowNowPlayingBar(LibraryRoute.Home, existingVisibility = true))
        assertFalse(shouldShowNowPlayingBar(LibraryRoute.Home, existingVisibility = false))
        assertFalse(shouldShowNowPlayingBar(LibraryRoute.Settings, existingVisibility = true))
        assertFalse(shouldShowNowPlayingBar(LibraryRoute.SettingsAbout, existingVisibility = true))
    }

    @Test
    fun backConsumesActiveSelectionBeforePoppingTheRoute() {
        assertEquals(
            LibraryBackDecision.CancelSelection,
            libraryBackDecision(
                selectionState = TrackSelectionState(
                    TrackSelectionPageKey.Album("Night"),
                    setOf("track-a"),
                ),
                isNowPlayingExpanded = false,
                canPopRoute = true,
            ),
        )
        assertEquals(
            LibraryBackDecision.PopRoute,
            libraryBackDecision(
                selectionState = TrackSelectionState(),
                isNowPlayingExpanded = false,
                canPopRoute = true,
            ),
        )
    }

    @Test
    fun backDecisionUsesPlaylistModalEditSelectionNowPlayingAndRoutePrecedence() {
        val selected = TrackSelectionState(
            TrackSelectionPageKey.Album("Night"),
            setOf("track-a"),
        )
        val emptySelection = TrackSelectionState()
        assertEquals(LibraryBackDecision.DismissPlaylistModal, libraryBackDecision(true, true, selected, true, true))
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, libraryBackDecision(false, true, selected, true, true))
        assertEquals(LibraryBackDecision.CancelSelection, libraryBackDecision(false, false, selected, true, true))
        assertEquals(LibraryBackDecision.HideNowPlaying, libraryBackDecision(false, false, emptySelection, true, true))
        assertEquals(LibraryBackDecision.PopRoute, libraryBackDecision(false, false, emptySelection, false, true))
        assertEquals(LibraryBackDecision.None, libraryBackDecision(false, false, emptySelection, false, false))
    }

    @Test
    fun eligiblePageKeyMatchesOnlyTheCurrentSupportedSurface() {
        assertEquals(
            TrackSelectionPageKey.HomeSongs,
            trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.Songs),
        )
        assertEquals(null, trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.Albums))
        assertEquals(
            TrackSelectionPageKey.Album("Night"),
            trackSelectionPageKeyFor(LibraryRoute.AlbumDetail("Night"), BrowseMode.Songs),
        )
        assertEquals(TrackSelectionPageKey.Search, trackSelectionPageKeyFor(LibraryRoute.Search, BrowseMode.Songs))
        assertEquals(null, trackSelectionPageKeyFor(LibraryRoute.PlaylistHub, BrowseMode.Songs))
    }

    @Test
    fun nowPlayingBarOffsetPxIsZeroWhenFullyShown() {
        assertEquals(0, nowPlayingBarOffsetPx(hiddenFraction = 0f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxMatchesMeasuredHeightWhenFullyHidden() {
        // Regression guard: measured height can exceed the old 156px estimate
        // (e.g. after platform navigation-bar insets), so the offset must move
        // the entire measured wrapper off-screen, not a fixed fallback amount.
        assertEquals(312, nowPlayingBarOffsetPx(hiddenFraction = 1f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxScalesLinearlyWithFraction() {
        assertEquals(78, nowPlayingBarOffsetPx(hiddenFraction = 0.25f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxCoercesFractionOutsideUnitRange() {
        assertEquals(0, nowPlayingBarOffsetPx(hiddenFraction = -0.5f, measuredHeightPx = 312))
        assertEquals(312, nowPlayingBarOffsetPx(hiddenFraction = 1.5f, measuredHeightPx = 312))
    }
}

private fun testTrack(id: String): Track = Track(
    id = id,
    title = "Title $id",
    artist = "Artist",
    album = "Album",
    durationSeconds = 180,
    accent = TrackAccent(start = 0xFF111111, end = 0xFF222222),
    source = AudioSource.FilePath("audio/$id.mp3"),
)
