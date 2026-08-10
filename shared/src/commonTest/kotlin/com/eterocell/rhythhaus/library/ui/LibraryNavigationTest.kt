package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.BrowseMode
import com.eterocell.rhythhaus.library.ui.DrillDownAction
import com.eterocell.rhythhaus.library.ui.dispatchDrillDownAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryNavigationTest {
    @Test
    fun pushingTheCurrentRouteIsANoOpAndPreservesItsEntryIdentity() {
        val stack =
            LibraryNavigationStack().push(LibraryRoute.AlbumDetail("Night"))

        assertEquals(stack, stack.push(LibraryRoute.AlbumDetail("Night")))
        assertEquals(stack.currentEntry, stack.push(stack.current).currentEntry)
    }

    @Test
    fun mismatchedSelectionCapabilityCannotOwnBackForTheActivePage() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.AlbumDetail("Night"))
        val destination = state.activeDestinationId
        val mismatched =
            LibraryBackSelectionPort(
                destination,
                LibraryBackTarget.PageSelection(
                    LibraryBackTargetId(destination, "wrong-page"),
                    TrackSelectionPageKey.Search,
                ),
                cancel = {},
            )

        state.publishSelectionPort(mismatched)

        assertIs<LibraryBackTarget.Route>(
            assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                .session
                .target,
        )
    }

    @Test
    fun displayedPlaylistDeletionInvalidatesOnlyItsExactActiveDestination() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val deletedEntry = state.navigation.currentEntry
        val destination = state.activeDestinationId
        val modal =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destination, "delete-confirmation"),
            )
        var modalDispatches = 0
        var selectionCancels = 0
        state.publishSelectionPort(
            LibraryBackSelectionPort(
                destination,
                LibraryBackTarget.PageSelection(
                    LibraryBackTargetId(destination, "detail-selection"),
                    TrackSelectionPageKey.HomeSongs,
                ),
                cancel = { selectionCancels += 1 },
            ),
        )
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, modal) {
                modalDispatches += 1
                LibraryBackFeatureRequestResult.Started
            },
        )
        state.showNowPlaying()
        state.setSelectedTrackId("playing-track")
        val pending =
            assertIs<LibraryBackBeginResult.Started>(state.beginBack()).session

        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(), "playlist-1", deletedEntry)

        assertEquals(LibraryRoute.PlaylistHub, state.navigation.current)
        assertEquals(
            LibraryNavigationTransition.Replace, state.lastNavigationTransition)
        assertTrue(state.showNowPlaying)
        assertEquals("playing-track", state.selectedTrackId)
        assertNull(state.pendingBackSession)
        pending.complete()
        assertEquals(0, modalDispatches)
        assertEquals(0, selectionCancels)
        assertIs<LibraryBackTarget.NowPlaying>(
            assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                .session
                .target,
        )
    }

    @Test
    fun displayedPlaylistDeletionPopsToItsOriginalPlaylistHubPredecessor() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistHub)
        val originalHubEntry = state.navigation.currentEntry
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val detailEntry = state.navigation.currentEntry

        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(),
            "playlist-1",
            detailEntry,
        )

        assertEquals(
            listOf(LibraryRoute.Home, LibraryRoute.PlaylistHub),
            state.navigation.routes,
        )
        assertEquals(originalHubEntry, state.navigation.currentEntry)
        assertEquals(
            LibraryNavigationTransition.Pop, state.lastNavigationTransition)
    }

    @Test
    fun staleDisplayedPlaylistDeletionCannotInvalidateReplacementOrAnotherPlaylist() {
        val state = LibraryAppState(initialSelectedTrackId = "playing-track")
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-a"))
        val originalEntry = state.navigation.currentEntry

        state.replaceTopRoute(LibraryRoute.PlaylistDetail("playlist-a"))
        val replacementEntry = state.navigation.currentEntry
        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(), "playlist-a", originalEntry)

        assertEquals(replacementEntry, state.navigation.currentEntry)
        assertEquals(
            LibraryRoute.PlaylistDetail("playlist-a"), state.navigation.current)

        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(), "playlist-b", replacementEntry)
        assertEquals(replacementEntry, state.navigation.currentEntry)

        state.replaceTopRoute(LibraryRoute.PlaylistDetail("playlist-b"))
        val otherPlaylistEntry = state.navigation.currentEntry
        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(), "playlist-a", originalEntry)

        assertEquals(otherPlaylistEntry, state.navigation.currentEntry)
        assertEquals("playing-track", state.selectedTrackId)
    }

    @Test
    fun nonDeletionConfirmationLeavesDisplayedPlaylistAndFeatureSessionUntouched() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-a"))
        val entry = state.navigation.currentEntry
        val destination = state.activeDestinationId
        val target =
            LibraryBackTarget.FeatureEdit(
                LibraryBackTargetId(destination, "edit"))
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, target) {
                LibraryBackFeatureRequestResult.Started
            },
        )
        val pending =
            assertIs<LibraryBackBeginResult.Started>(state.beginBack()).session

        state.completeDisplayedPlaylistDeletion(
            PlaylistSnapshot(
                playlists =
                    listOf(PlaylistSummary("playlist-a", "Saved", 1L, 1L))),
            "playlist-a",
            entry,
        )

        assertEquals(entry, state.navigation.currentEntry)
        assertEquals(pending, state.pendingBackSession)
    }

    @Test
    fun predictiveLifecycleLatchesUnhandledAndSystemCompletionWithoutGestureBeginsOnce() {
        val lifecycle = LibraryBackGestureLifecycle()
        var begins = 0
        assertEquals(
            LibraryBackBeginResult.Unhandled,
            lifecycle.beginPredictive {
                begins++
                LibraryBackBeginResult.Unhandled
            })
        assertEquals(
            LibraryBackBeginResult.Unhandled,
            lifecycle.beginPredictive {
                begins++
                LibraryBackBeginResult.Started(error("must not begin twice"))
            })
        lifecycle.completeSystemBack {
            begins++
            LibraryBackBeginResult.Unhandled
        }
        assertEquals(1, begins)

        var ordinaryBegins = 0
        lifecycle.completeSystemBack {
            ordinaryBegins++
            LibraryBackBeginResult.Unhandled
        }
        assertEquals(1, ordinaryBegins)
    }

    @Test
    fun predictiveLifecycleKeepsSuppressedAndStartedResultsLatchedUntilTheirOwnEnd() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.AlbumDetail("Night"))
        val lifecycle = LibraryBackGestureLifecycle()
        var begins = 0

        val started = lifecycle.beginPredictive {
            begins++
            state.beginBack()
        }
        lifecycle.beginPredictive {
            begins++
            LibraryBackBeginResult.Unhandled
        }
        lifecycle.completeSystemBack {
            begins++
            LibraryBackBeginResult.Unhandled
        }

        assertEquals(1, begins)
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertIs<LibraryBackBeginResult.Started>(started)

        val suppressedLifecycle = LibraryBackGestureLifecycle()
        val pendingState = LibraryAppState(initialSelectedTrackId = null)
        pendingState.pushRoute(LibraryRoute.AlbumDetail("Night"))
        val pending =
            assertIs<LibraryBackBeginResult.Started>(pendingState.beginBack())
                .session
        assertEquals(
            LibraryBackBeginResult.Suppressed,
            suppressedLifecycle.beginPredictive { pendingState.beginBack() },
        )
        suppressedLifecycle.completeSystemBack {
            error("latched suppressed result must not re-begin")
        }
        assertEquals(pending, pendingState.pendingBackSession)
        pending.cancel()
    }

    @Test
    fun nowPlayingThresholdSwipeUsesTheExactSharedBackCallback() {
        var calls = 0
        val back: () -> Unit = { calls += 1 }

        nowPlayingSwipeCollapseAction(back)()

        assertEquals(1, calls)
    }

    @Test
    fun navigationEntriesGiveEqualRouteReplacementANewIdentityAndPopRestoresPredecessor() {
        val first =
            LibraryNavigationStack()
                .push(LibraryRoute.PlaylistDetail("playlist-a"))
        val firstEntry = first.currentEntry
        val replacement =
            first.replaceTop(LibraryRoute.PlaylistDetail("playlist-a"))

        assertNotEquals(
            firstEntry.destinationId, replacement.currentEntry.destinationId)
        assertEquals(first.entries.first(), replacement.pop().currentEntry)
    }

    @Test
    fun drillDownRowDispatchesOnlyTrackSelectionWithSelectedTrack() {
        val selectedTrack = testTrack(id = "selected")
        val selectedTracks = mutableListOf<Track>()

        dispatchDrillDownAction(
            action = DrillDownAction.SelectTrack(selectedTrack),
            onPlayTrack = { _, track -> selectedTracks.add(track) },
            orderedTracks = listOf(selectedTrack),
        )

        assertEquals(listOf(selectedTrack), selectedTracks)
    }

    @Test
    fun drillDownTransportToggleDispatchesNoPlayback() {
        val selectedTracks = mutableListOf<Track>()

        dispatchDrillDownAction(
            action = DrillDownAction.ToggleTransport,
            onPlayTrack = { _, track -> selectedTracks.add(track) },
            orderedTracks = emptyList(),
        )

        assertTrue(selectedTracks.isEmpty())
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
    fun equalTopPushIsANoOp() {
        val stack =
            LibraryNavigationStack()
                .push(LibraryRoute.Search)
                .push(LibraryRoute.Search)

        assertEquals(
            listOf(LibraryRoute.Home, LibraryRoute.Search), stack.routes)
    }

    @Test
    fun searchOpenedFromAlbumReturnsToAlbum() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val stack =
            LibraryNavigationStack().push(album).push(LibraryRoute.Search)

        assertEquals(LibraryRoute.Search, stack.current)
        assertTrue(stack.canPop)
        assertEquals(album, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun nowPlayingOpenedFromArtistReturnsToArtist() {
        val artist = LibraryRoute.ArtistDetail("John Coltrane")
        val stack =
            LibraryNavigationStack().push(artist).push(LibraryRoute.NowPlaying)

        assertEquals(LibraryRoute.NowPlaying, stack.current)
        assertEquals(artist, stack.pop().current)
    }

    @Test
    fun clearDialogPopsBackToSettingsOrigin() {
        val stack =
            LibraryNavigationStack()
                .push(LibraryRoute.Settings)
                .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertEquals(LibraryRoute.Settings, stack.pop().current)
    }

    @Test
    fun settingsAboutAndLibrariesPopBackThroughSettingsToHome() {
        val stack =
            LibraryNavigationStack()
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
        val stack =
            LibraryNavigationStack()
                .push(LibraryRoute.PlaylistHub)
                .push(LibraryRoute.PlaylistDetail("playlist-1"))

        assertEquals(LibraryRoute.PlaylistDetail("playlist-1"), stack.current)
        assertEquals(LibraryRoute.PlaylistHub, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun playlistRoutesPreserveNowPlayingBarPolicy() {
        assertTrue(routePermitsNowPlayingBar(LibraryRoute.PlaylistHub))
        assertTrue(
            routePermitsNowPlayingBar(
                LibraryRoute.PlaylistDetail("playlist-1")))
    }

    @Test
    fun playlistRoutesUseContentOwnershipInsteadOfSettingsOverlayOwnership() {
        LibraryAdaptiveLayoutMode.entries.forEach { mode ->
            assertFalse(
                libraryRouteRendersAsActiveOverlay(
                    LibraryRoute.PlaylistHub, mode))
            assertFalse(
                libraryRouteRendersAsActiveOverlay(
                    LibraryRoute.PlaylistDetail("playlist-1"), mode))
        }
    }

    @Test
    fun settingsAboutRoutesUseActiveOverlayOwnershipInCompactAndWideLayouts() {
        val routes =
            listOf(
                LibraryRoute.SettingsAbout,
                LibraryRoute.OpenSourceLibraries,
            )

        LibraryAdaptiveLayoutMode.entries.forEach { mode ->
            routes.forEach { route ->
                assertTrue(
                    libraryRouteRendersAsActiveOverlay(
                        route = route, mode = mode))
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
        val stack =
            LibraryNavigationStack()
                .push(LibraryRoute.Settings)
                .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertFalse(
            routeRequiresInWindowContentAnimation(
                LibraryRoute.ClearLibraryDialog))
        assertFalse(
            routeRequiresInWindowContentAnimation(LibraryRoute.Settings))
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
    fun pushingHomeReturnsToRoot() {
        val stack =
            LibraryNavigationStack()
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

        assertEquals(
            LibraryNavigationTransition.Push,
            classifyNavigationTransition(from, to))
    }

    @Test
    fun poppingNestedRouteClassifiesAsPop() {
        val from =
            LibraryNavigationStack()
                .push(LibraryRoute.AlbumDetail("Blue Train"))
                .push(LibraryRoute.Search)
        val to = from.pop()

        assertEquals(
            LibraryNavigationTransition.Pop,
            classifyNavigationTransition(from, to))
    }

    @Test
    fun pushingHomeClassifiesAsRoot() {
        val from =
            LibraryNavigationStack()
                .push(LibraryRoute.AlbumDetail("Blue Train"))
                .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Home)

        assertEquals(
            LibraryNavigationTransition.Root,
            classifyNavigationTransition(from, to))
    }

    @Test
    fun replacingTopRouteClassifiesAsReplace() {
        val from = LibraryNavigationStack().push(LibraryRoute.Search)
        val to = from.replaceTop(LibraryRoute.Settings)

        assertEquals(
            LibraryNavigationTransition.Replace,
            classifyNavigationTransition(from, to))
    }

    @Test
    fun equalTopPushClassifiesAsNoTransition() {
        val from = LibraryNavigationStack().push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Search)

        assertEquals(
            LibraryNavigationTransition.None,
            classifyNavigationTransition(from, to))
    }

    @Test
    fun libraryScrollDownWithinSameItemHidesNowPlayingBar() {
        val previous =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)
        val current =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)

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
        val previous =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)

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
        val previous =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)
        val current =
            LibraryScrollPosition(
                firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)

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
        val previous =
            LibraryScrollPosition(
                firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)
        val current =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120)

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
        val previous =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30)
        val current =
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 31)

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
        val pushed =
            applyNavigationAction(
                LibraryNavigationStack(), LibraryNavigationAction.Push(album))
        assertEquals(listOf(LibraryRoute.Home, album), pushed.routes)

        val replaced =
            applyNavigationAction(
                pushed,
                LibraryNavigationAction.ReplaceTop(
                    LibraryRoute.ArtistDetail("Alice")))
        assertEquals(
            listOf(LibraryRoute.Home, LibraryRoute.ArtistDetail("Alice")),
            replaced.routes)

        assertEquals(
            LibraryRoute.Home,
            applyNavigationAction(replaced, LibraryNavigationAction.Pop)
                .current)
        assertEquals(
            listOf(LibraryRoute.Home),
            applyNavigationAction(replaced, LibraryNavigationAction.PopToRoot)
                .routes)
    }

    @Test
    fun navigationActionTransitionMatchesStackChange() {
        val from =
            LibraryNavigationStack()
                .push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(
            LibraryNavigationTransition.Push,
            transitionForNavigationAction(
                from, LibraryNavigationAction.Push(LibraryRoute.Search)),
        )
        assertEquals(
            LibraryNavigationTransition.Pop,
            transitionForNavigationAction(from, LibraryNavigationAction.Pop),
        )
        assertEquals(
            LibraryNavigationTransition.Replace,
            transitionForNavigationAction(
                from,
                LibraryNavigationAction.ReplaceTop(
                    LibraryRoute.ArtistDetail("Alice"))),
        )
        assertEquals(
            LibraryNavigationTransition.Root,
            transitionForNavigationAction(
                from, LibraryNavigationAction.PopToRoot),
        )
        assertEquals(
            LibraryNavigationTransition.None,
            transitionForNavigationAction(
                LibraryNavigationStack(), LibraryNavigationAction.Pop),
        )
        assertEquals(
            LibraryNavigationTransition.None,
            transitionForNavigationAction(
                LibraryNavigationStack(), LibraryNavigationAction.PopToRoot),
        )
    }

    @Test
    fun playbackTrackSelectionOverridesOnlyWhenPlaybackHasTrack() {
        assertEquals(
            "playing", selectedTrackIdForPlaybackChange("selected", "playing"))
        assertEquals(
            "selected", selectedTrackIdForPlaybackChange("selected", null))
        assertEquals(null, selectedTrackIdForPlaybackChange(null, null))
    }

    @Test
    fun bottomBarVisibilityStateStoresPreviousScrollPosition() {
        val initial =
            LibraryBottomBarVisibilityState(
                visible = true, previousScrollPosition = null)
        val first =
            updateBottomBarVisibilityForScroll(
                state = initial,
                current =
                    LibraryScrollPosition(
                        firstVisibleItemIndex = 0,
                        firstVisibleItemScrollOffset = 10),
            )
        assertTrue(first.visible)
        assertEquals(
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10),
            first.previousScrollPosition)

        val second =
            updateBottomBarVisibilityForScroll(
                state = first,
                current =
                    LibraryScrollPosition(
                        firstVisibleItemIndex = 0,
                        firstVisibleItemScrollOffset = 30),
            )
        assertFalse(second.visible)
        assertEquals(
            LibraryScrollPosition(
                firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 30),
            second.previousScrollPosition)
    }

    @Test
    fun libraryAppStateNavigationActionsRecordTransitions() {
        val state = LibraryAppState(initialSelectedTrackId = null)

        state.pushRoute(LibraryRoute.AlbumDetail("A"))
        assertEquals(LibraryRoute.AlbumDetail("A"), state.navigation.current)
        assertEquals(
            LibraryNavigationTransition.Push, state.lastNavigationTransition)

        state.replaceTopRoute(LibraryRoute.ArtistDetail("B"))
        assertEquals(LibraryRoute.ArtistDetail("B"), state.navigation.current)
        assertEquals(
            LibraryNavigationTransition.Replace, state.lastNavigationTransition)

        state.popRoute()
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertEquals(
            LibraryNavigationTransition.Pop, state.lastNavigationTransition)
    }

    @Test
    fun stalePlaylistDetailRecoveryReplacesDetailWithHub() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistHub)
        state.pushRoute(LibraryRoute.PlaylistDetail("missing"))

        var message: String? = null
        state.recoverStalePlaylistDetail("playlist_changed") { message = it }

        assertEquals(LibraryRoute.PlaylistHub, state.navigation.current)
        assertEquals(
            LibraryNavigationTransition.Replace, state.lastNavigationTransition)
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

        state.updateNowPlayingBarVisibilityForScroll(
            LibraryScrollPosition(0, 10))
        assertTrue(state.isNowPlayingBarVisible)
        state.updateNowPlayingBarVisibilityForScroll(
            LibraryScrollPosition(0, 30))
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
        val permittedRoutes =
            listOf(
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
            assertTrue(
                routePermitsNowPlayingBar(route),
                "Expected $route to permit the bar")
        }
    }

    @Test
    fun routeEligibilityCombinesWithExistingVisibility() {
        assertTrue(
            shouldShowNowPlayingBar(
                LibraryRoute.Home, existingVisibility = true))
        assertFalse(
            shouldShowNowPlayingBar(
                LibraryRoute.Home, existingVisibility = false))
        assertFalse(
            shouldShowNowPlayingBar(
                LibraryRoute.Settings, existingVisibility = true))
        assertFalse(
            shouldShowNowPlayingBar(
                LibraryRoute.SettingsAbout, existingVisibility = true))
    }

    @Test
    fun eligiblePageKeyMatchesOnlyTheCurrentSupportedSurface() {
        assertEquals(
            TrackSelectionPageKey.HomeSongs,
            trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.Songs),
        )
        assertEquals(
            null,
            trackSelectionPageKeyFor(LibraryRoute.Home, BrowseMode.Albums))
        assertEquals(
            TrackSelectionPageKey.Album("Night"),
            trackSelectionPageKeyFor(
                LibraryRoute.AlbumDetail("Night"), BrowseMode.Songs),
        )
        assertEquals(
            TrackSelectionPageKey.Search,
            trackSelectionPageKeyFor(LibraryRoute.Search, BrowseMode.Songs))
        assertEquals(
            null,
            trackSelectionPageKeyFor(
                LibraryRoute.PlaylistHub, BrowseMode.Songs))
    }

    @Test
    fun nowPlayingBarOffsetPxIsZeroWhenFullyShown() {
        assertEquals(
            0,
            nowPlayingBarOffsetPx(hiddenFraction = 0f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxMatchesMeasuredHeightWhenFullyHidden() {
        // Regression guard: measured height can exceed the old 156px estimate
        // (e.g. after platform navigation-bar insets), so the offset must move
        // the entire measured wrapper off-screen, not a fixed fallback amount.
        assertEquals(
            312,
            nowPlayingBarOffsetPx(hiddenFraction = 1f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxScalesLinearlyWithFraction() {
        assertEquals(
            78,
            nowPlayingBarOffsetPx(
                hiddenFraction = 0.25f, measuredHeightPx = 312))
    }

    @Test
    fun nowPlayingBarOffsetPxCoercesFractionOutsideUnitRange() {
        assertEquals(
            0,
            nowPlayingBarOffsetPx(
                hiddenFraction = -0.5f, measuredHeightPx = 312))
        assertEquals(
            312,
            nowPlayingBarOffsetPx(
                hiddenFraction = 1.5f, measuredHeightPx = 312))
    }

    @Test
    fun backResolverUsesOneExactTargetInModalEditSelectionNowPlayingRouteOrder() {
        val route = LibraryRoute.AlbumDetail("Night")
        val destination =
            LibraryDestinationId(route, instanceToken = "destination-a")
        val modalId =
            LibraryBackTargetId(destination, instanceToken = "modal-a")
        val editId = LibraryBackTargetId(destination, instanceToken = "edit-a")
        val selectionTarget =
            LibraryBackTarget.PageSelection(
                LibraryBackTargetId(destination, "selection-a"),
                TrackSelectionPageKey.Album("Night"),
            )
        val input =
            LibraryBackResolutionInput(
                activeDestinationId = destination,
                backSurfacePorts =
                    listOf(
                        LibraryBackSurfacePort(
                            destinationId = destination,
                            foremostFeatureTarget =
                                LibraryBackTarget.FeatureModal(modalId),
                        ),
                    ),
                browseMode = BrowseMode.Songs,
                isNowPlayingExpanded = true,
                navigation = LibraryNavigationStack().push(route),
                selectionPort =
                    LibraryBackSelectionPort(destination, selectionTarget, {}),
            )

        assertEquals(
            LibraryBackTarget.FeatureModal(modalId),
            assertIs<LibraryBackResolution.Started>(resolveLibraryBack(input))
                .target,
        )
        assertEquals(
            LibraryBackTarget.FeatureEdit(editId),
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        input.copy(
                            backSurfacePorts =
                                listOf(
                                    LibraryBackSurfacePort(
                                        destination,
                                        LibraryBackTarget.FeatureEdit(editId),
                                    ),
                                ),
                        ),
                    ))
                .target,
        )
        assertIs<LibraryBackTarget.PageSelection>(
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        input.copy(backSurfacePorts = emptyList())))
                .target,
        )
        assertIs<LibraryBackTarget.NowPlaying>(
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        input.copy(
                            backSurfacePorts = emptyList(),
                            selectionPort = null,
                        ),
                    ))
                .target,
        )
        val routeTarget =
            assertIs<LibraryBackTarget.Route>(
                assertIs<LibraryBackResolution.Started>(
                        resolveLibraryBack(
                            input.copy(
                                backSurfacePorts = emptyList(),
                                selectionPort = null,
                                isNowPlayingExpanded = false,
                            ),
                        ))
                    .target,
            )
        assertEquals(
            LibraryRoute.Home, routeTarget.routePreview.nextNavigation.current)
        assertEquals(
            LibraryBackResolution.Unhandled,
            resolveLibraryBack(
                input.copy(
                    backSurfacePorts = emptyList(),
                    selectionPort = null,
                    isNowPlayingExpanded = false,
                    navigation = LibraryNavigationStack(),
                ),
            ),
        )
    }

    @Test
    fun backResolverAcceptsOnlyActivePortAndActivePageSelection() {
        val activeDestination =
            LibraryDestinationId(LibraryRoute.AlbumDetail("Night"), "active")
        val inactiveDestination =
            LibraryDestinationId(LibraryRoute.AlbumDetail("Night"), "outgoing")
        val inactiveModal =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(inactiveDestination, "modal"))
        val input =
            LibraryBackResolutionInput(
                activeDestinationId = activeDestination,
                backSurfacePorts =
                    listOf(
                        LibraryBackSurfacePort(
                            inactiveDestination, inactiveModal),
                    ),
                browseMode = BrowseMode.Songs,
                isNowPlayingExpanded = true,
                navigation =
                    LibraryNavigationStack().push(activeDestination.route),
            )

        assertIs<LibraryBackTarget.NowPlaying>(
            assertIs<LibraryBackResolution.Started>(resolveLibraryBack(input))
                .target,
        )

        val selectedForemostTarget =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(activeDestination, "feature-chosen-modal"))
        assertEquals(
            selectedForemostTarget,
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        input.copy(
                            backSurfacePorts =
                                listOf(
                                    LibraryBackSurfacePort(
                                        activeDestination,
                                        selectedForemostTarget,
                                    ),
                                ),
                        ),
                    ))
                .target,
        )
    }

    @Test
    fun sameShapedReplacementUsesNewDestinationAndTargetIdentities() {
        val route = LibraryRoute.PlaylistDetail("playlist-1")
        val destinationA = LibraryDestinationId(route, "destination-a")
        val destinationB = LibraryDestinationId(route, "destination-b")
        val modalA =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationA, "modal-a"))
        val modalB =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationB, "modal-b"))

        val targetA =
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        LibraryBackResolutionInput(
                            activeDestinationId = destinationA,
                            backSurfacePorts =
                                listOf(
                                    LibraryBackSurfacePort(
                                        destinationA, modalA)),
                            browseMode = BrowseMode.Songs,
                            isNowPlayingExpanded = false,
                            navigation = LibraryNavigationStack().push(route),
                        ),
                    ))
                .target
        val targetB =
            assertIs<LibraryBackResolution.Started>(
                    resolveLibraryBack(
                        LibraryBackResolutionInput(
                            activeDestinationId = destinationB,
                            backSurfacePorts =
                                listOf(
                                    LibraryBackSurfacePort(
                                        destinationB, modalB)),
                            browseMode = BrowseMode.Songs,
                            isNowPlayingExpanded = false,
                            navigation = LibraryNavigationStack().push(route),
                        ),
                    ))
                .target

        assertNotEquals(destinationA, destinationB)
        assertNotEquals(targetA, targetB)
        assertNotEquals(targetA.id, targetB.id)
        assertEquals(destinationA, targetA.id.destinationId)
        assertEquals(destinationB, targetB.id.destinationId)
    }

    /* Superseded manual-activation session tests were replaced by navigation-entry tests below. */
    @Test
    fun backSessionLatchesDispatchesOnceAndCallbackReturnDoesNotSettle() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.AlbumDetail("Night"))
        val destination = state.activeDestinationId
        val target =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destination, "modal-a"))
        var selectionCancels = 0
        val selection =
            LibraryBackSelectionPort(
                destination,
                LibraryBackTarget.PageSelection(
                    LibraryBackTargetId(destination, "selection"),
                    TrackSelectionPageKey.Album("Night"),
                ),
            ) {
                selectionCancels += 1
            }
        var dispatches = 0
        state.showNowPlaying()
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, target) { dispatched ->
                assertEquals(target, dispatched)
                dispatches += 1
                LibraryBackFeatureRequestResult.Started
            },
        )

        val session =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(selection),
                )
                .session
        assertEquals(target, session.target)
        assertEquals(LibraryBackBeginResult.Suppressed, state.beginBack())

        session.complete()
        session.complete()

        assertEquals(1, dispatches)
        assertEquals(0, selectionCancels)
        assertTrue(state.showNowPlaying)
        assertEquals(session, state.pendingBackSession)
        assertEquals(LibraryBackBeginResult.Suppressed, state.beginBack())
    }

    @Test
    fun authoritativeExactSettlementAndRejectionNeverRetargetOrFallThrough() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destinationA = state.activeDestinationId
        val modalA =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationA, "modal-a"))
        val modalB =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationA, "modal-b"))
        var modalADispatches = 0
        var modalBDispatches = 0
        state.registerBackSurface(
            LibraryBackSurfacePort(destinationA, modalA) {
                modalADispatches += 1
                LibraryBackFeatureRequestResult.Started
            })

        val oldSession =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        state.registerBackSurface(
            LibraryBackSurfacePort(destinationA, modalB) {
                modalBDispatches += 1
                LibraryBackFeatureRequestResult.Started
            })
        state.reconcileBackSession()

        assertNull(state.pendingBackSession)
        oldSession.complete()
        assertEquals(0, modalADispatches)
        assertEquals(0, modalBDispatches)

        val rejected =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        rejected.reject()
        assertNull(state.pendingBackSession)
        assertEquals(
            modalB,
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
                .target)

        state.popRoute()
        assertNull(state.pendingBackSession)
    }

    @Test
    fun activeDestinationPortPublicationRejectsInactiveAndStaleDisposal() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destinationA = state.activeDestinationId
        val targetA =
            LibraryBackTarget.FeatureEdit(
                LibraryBackTargetId(destinationA, "edit-a"))
        val targetA2 =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationA, "modal-a2"))
        val disposeA =
            state.registerBackSurface(
                LibraryBackSurfacePort(destinationA, targetA) {
                    LibraryBackFeatureRequestResult.Started
                })
        val disposeA2 =
            state.registerBackSurface(
                LibraryBackSurfacePort(destinationA, targetA2) {
                    LibraryBackFeatureRequestResult.Started
                })
        state.replaceTopRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destinationB = state.activeDestinationId
        val targetB =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destinationB, "modal-b"))
        state.registerBackSurface(
            LibraryBackSurfacePort(destinationB, targetB) {
                LibraryBackFeatureRequestResult.Started
            })
        disposeA()

        assertEquals(
            targetB,
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
                .target)
        state.pendingBackSession!!.cancel()
        disposeA2()
        assertEquals(
            targetB,
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
                .target)
    }

    @Test
    fun routeSessionCancellationAndInvalidCompletionDoNotNavigateOrFallThrough() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        val route = LibraryRoute.AlbumDetail("Night")
        state.pushRoute(route)

        val cancelled =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        cancelled.cancel()
        assertEquals(route, state.navigation.current)

        val invalid =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        state.replaceTopRoute(route)
        invalid.complete()
        assertEquals(route, state.navigation.current)
        assertNull(state.pendingBackSession)
    }

    @Test
    fun newlyPublishedHigherPriorityTargetCannotStealValidPendingSession() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        val route = LibraryRoute.AlbumDetail("Night")
        val destination = LibraryDestinationId(route, "a")
        val modal =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destination, "modal"))
        var modalDispatches = 0
        state.pushRoute(destination.route)
        state.showNowPlaying()

        val session =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        assertIs<LibraryBackTarget.NowPlaying>(session.target)
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, modal) {
                modalDispatches += 1
                LibraryBackFeatureRequestResult.Started
            })
        state.reconcileBackSession()

        assertEquals(session, state.pendingBackSession)
        session.complete()
        assertFalse(state.showNowPlaying)
        assertEquals(0, modalDispatches)
    }

    @Test
    fun ordinaryAndSystemBackAdaptersBeginAndCompleteTheSameTarget() {
        listOf(
                AdapterBackCase.Modal,
                AdapterBackCase.Edit,
                AdapterBackCase.Selection,
                AdapterBackCase.NowPlaying,
                AdapterBackCase.Route,
            )
            .forEach { case ->
                val ordinary =
                    adapterBackOutcome(case, useSystemAdapter = false)
                val system = adapterBackOutcome(case, useSystemAdapter = true)

                assertEquals(
                    ordinary, system, "Expected matching result for $case")
                assertEquals(LibraryBackAdapterResult.Handled, ordinary.result)
                assertEquals(1, ordinary.targetExecutions)
                assertEquals(0, ordinary.unhandledDefaults)
            }
    }

    @Test
    fun backAdaptersSuppressPendingSessionsAndDelegateOnlyRootToTheirOwnDefault() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        var dispatches = 0
        var ordinaryDefault = 0
        var systemDefault = 0
        state.pushRoute(LibraryRoute.AlbumDetail("Night"))
        val destination = state.activeDestinationId
        val target =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destination, "modal"))
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, target) {
                dispatches += 1
                LibraryBackFeatureRequestResult.Started
            })

        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null) { ordinaryDefault += 1 },
        )
        assertEquals(1, dispatches)
        assertEquals(
            LibraryBackAdapterResult.Suppressed,
            performLibraryBack(state, null) { systemDefault += 1 },
        )
        assertEquals(1, dispatches)
        assertEquals(0, ordinaryDefault)
        assertEquals(0, systemDefault)

        state.popToRoot()
        assertEquals(
            LibraryBackAdapterResult.Unhandled,
            performLibraryBack(state, null) { ordinaryDefault += 1 },
        )
        assertEquals(
            LibraryBackAdapterResult.Unhandled,
            performLibraryBack(state, null) { systemDefault += 1 },
        )
        assertEquals(1, ordinaryDefault)
        assertEquals(1, systemDefault)
    }

    @Test
    fun predictiveSessionLatchesPreviewAndCompletionWithoutReresolution() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        val route = LibraryRoute.AlbumDetail("Night")

        state.pushRoute(route)

        val session =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        val preview = assertIs<LibraryRoutePreview>(session.routePreview)
        state.showNowPlaying()

        assertEquals(LibraryRoute.Home, preview.nextNavigation.current)
        session.complete()
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertTrue(state.showNowPlaying)
        assertNull(state.pendingBackSession)
    }

    @Test
    fun predictiveCancelCompletionRejectionAndReplacementRespectTheLatchedSession() {
        val state = LibraryAppState(initialSelectedTrackId = null)
        val route = LibraryRoute.AlbumDetail("Night")
        state.pushRoute(route)

        val cancelled =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        cancelled.cancel()
        assertEquals(route, state.navigation.current)
        assertNull(state.pendingBackSession)

        val valid =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        valid.complete()
        valid.complete()
        assertEquals(LibraryRoute.Home, state.navigation.current)
        assertNull(state.pendingBackSession)

        valid.reject()
        assertNull(state.pendingBackSession)
        state.pushRoute(route)
        val invalid =
            assertIs<LibraryBackBeginResult.Started>(
                    state.beginBack(),
                )
                .session
        state.replaceTopRoute(route)
        invalid.complete()
        assertEquals(route, state.navigation.current)
        assertNull(state.pendingBackSession)
    }
}

private enum class AdapterBackCase {
    Modal,
    Edit,
    Selection,
    NowPlaying,
    Route,
}

private data class AdapterBackOutcome(
    val result: LibraryBackAdapterResult,
    val targetExecutions: Int,
    val unhandledDefaults: Int,
)

private fun adapterBackOutcome(
    case: AdapterBackCase,
    useSystemAdapter: Boolean,
): AdapterBackOutcome {
    val state = LibraryAppState(initialSelectedTrackId = null)
    val route = LibraryRoute.AlbumDetail("Night")
    var targetExecutions = 0
    var unhandledDefaults = 0
    state.pushRoute(route)
    val destination = state.activeDestinationId
    when (case) {
        AdapterBackCase.Modal ->
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(destination, "modal")),
                ) {
                    targetExecutions += 1
                    LibraryBackFeatureRequestResult.Started
                },
            )

        AdapterBackCase.Edit ->
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureEdit(
                        LibraryBackTargetId(destination, "edit")),
                ) {
                    targetExecutions += 1
                    LibraryBackFeatureRequestResult.Started
                },
            )

        AdapterBackCase.Selection -> Unit
        AdapterBackCase.NowPlaying -> state.showNowPlaying()
        AdapterBackCase.Route -> Unit
    }
    val selection =
        LibraryBackSelectionPort(
            destination,
            LibraryBackTarget.PageSelection(
                LibraryBackTargetId(destination, "selection"),
                TrackSelectionPageKey.Album("Night"),
            ),
        ) {
            targetExecutions += 1
        }
    val adapter =
        if (useSystemAdapter) ::systemAdapterBack else ::ordinaryAdapterBack
    val result =
        adapter(
            state,
            if (case == AdapterBackCase.Selection) selection else null,
            { unhandledDefaults += 1 },
        )
    if (case == AdapterBackCase.NowPlaying && !state.showNowPlaying) {
        targetExecutions += 1
    }
    if (case == AdapterBackCase.Route &&
        state.navigation.current == LibraryRoute.Home) {
        targetExecutions += 1
    }
    return AdapterBackOutcome(result, targetExecutions, unhandledDefaults)
}

private fun ordinaryAdapterBack(
    state: LibraryAppState,
    selectionPort: LibraryBackSelectionPort?,
    onUnhandled: () -> Unit,
): LibraryBackAdapterResult =
    performLibraryBack(state, selectionPort, onUnhandled)

private fun systemAdapterBack(
    state: LibraryAppState,
    selectionPort: LibraryBackSelectionPort?,
    onUnhandled: () -> Unit,
): LibraryBackAdapterResult =
    performLibraryBack(state, selectionPort, onUnhandled)

private fun testTrack(id: String): Track =
    Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationSeconds = 180,
        accent = TrackAccent(start = 0xFF111111, end = 0xFF222222),
        source = AudioSource.FilePath("audio/$id.mp3"),
    )
