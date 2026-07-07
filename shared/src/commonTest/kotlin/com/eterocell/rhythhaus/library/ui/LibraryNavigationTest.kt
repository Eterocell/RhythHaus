package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryNavigationTest {
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
    fun clearDialogRouteRequiresInWindowContentAnimation() {
        assertTrue(routeRequiresInWindowContentAnimation(LibraryRoute.ClearLibraryDialog))
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
    fun nestedScrollChromeIsInactiveAtTopOfList() {
        val state = nestedScrollChromeStateFor(
            position = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0),
        )

        assertEquals(0f, state.progress)
        assertEquals(0f, state.headerOffsetPx)
    }

    @Test
    fun nestedScrollChromeProgressesWithinFirstItem() {
        val state = nestedScrollChromeStateFor(
            position = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 80),
        )

        assertEquals(0.8333333f, state.progress, absoluteTolerance = 0.0001f)
        assertEquals(0f, state.headerOffsetPx, absoluteTolerance = 0.0001f)
    }

    @Test
    fun nestedScrollChromeIsFullyActiveAfterFirstItem() {
        val state = nestedScrollChromeStateFor(
            position = LibraryScrollPosition(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0),
        )

        assertEquals(1f, state.progress, absoluteTolerance = 0.0001f)
        assertEquals(0f, state.headerOffsetPx, absoluteTolerance = 0.0001f)
    }

    @Test
    fun nestedScrollChromeCanStillUseExplicitHeaderOffsetWhenRequested() {
        val state = nestedScrollChromeStateFor(
            position = LibraryScrollPosition(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 80),
            activationDistancePx = 160f,
            maxHeaderOffsetPx = 18f,
        )

        assertEquals(0.5f, state.progress)
        assertEquals(-9f, state.headerOffsetPx)
    }
}
