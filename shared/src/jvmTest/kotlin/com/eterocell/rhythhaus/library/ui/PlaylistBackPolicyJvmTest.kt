package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import com.eterocell.rhythhaus.library.PlaylistSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlaylistBackPolicyJvmTest {
    @Test
    fun coreNavigationEventHandlerLatchesStartBeforeImmediateCompletionAndNeverRetargets() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        var begins = 0
        var firstDispatches = 0
        var replacementDispatches = 0
        val handler =
            LibraryNavigationEventBackHandler(dispatcher) {
                begins++
                state.beginBack()
            }
        val destination = state.activeDestinationId

        val disposeFirst =
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(destination, "first")),
                ) {
                    firstDispatches++
                    LibraryBackFeatureRequestResult.Started
                },
            )
        input.backStarted(NavigationEvent())
        input.backCompleted()

        assertEquals(1, begins)
        assertEquals(1, firstDispatches)
        assertEquals(0, replacementDispatches)
        disposeFirst()
        state.reconcileBackSession()

        val disposeReplacement =
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(destination, "replacement")),
                ) {
                    replacementDispatches++
                    LibraryBackFeatureRequestResult.Started
                },
            )
        // Direct input completion without an explicit predictive start is one
        // ordinary begin/complete.
        input.backCompleted()
        assertEquals(2, begins)
        assertEquals(1, replacementDispatches)
        disposeReplacement()
        state.reconcileBackSession()

        var cancelledDispatches = 0
        val disposeCancelled =
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(destination, "cancelled")),
                ) {
                    cancelledDispatches++
                    LibraryBackFeatureRequestResult.Started
                },
            )
        input.backStarted(NavigationEvent())
        input.backCancelled()
        assertEquals(3, begins)
        assertEquals(0, cancelledDispatches)
        assertNull(state.pendingBackSession)
        disposeCancelled()

        var secondGestureDispatches = 0
        state.registerBackSurface(
            LibraryBackSurfacePort(
                destination,
                LibraryBackTarget.FeatureModal(
                    LibraryBackTargetId(destination, "second")),
            ) {
                secondGestureDispatches++
                LibraryBackFeatureRequestResult.Started
            },
        )
        input.backStarted(NavigationEvent())
        input.backCompleted()
        assertEquals(4, begins)
        assertEquals(1, secondGestureDispatches)
        handler.dispose()
        dispatcher.removeInput(input)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun presentedDestinationPublishesEditAndAFeatureModalPrecedesIt() =
        runComposeUiTest {
            val state = LibraryAppState(null)
            state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
            val destination = state.activeDestinationId
            setContent {
                PlaylistDetailScreen(
                    playlist = PlaylistSummary("playlist-1", "Saved", 1L, 1L),
                    entries = emptyList(),
                    libraryTracks = emptyList(),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                    destinationId = destination,
                    registerBackSurface = state::registerBackSurface,
                )
            }
            waitForIdle()
            val edit =
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
            assertIs<LibraryBackTarget.FeatureEdit>(edit.target)
            edit.reject()
        }

    @Test
    fun inactiveHiddenAndOutgoingPortsAreRejectedAndStaleDisposersAreSafe() {
        val state = LibraryAppState(null)
        val route = LibraryRoute.PlaylistDetail("playlist-1")
        state.pushRoute(route)
        state.replaceTopRoute(route)
        val b = state.activeDestinationId
        state.replaceTopRoute(route)
        val activeA = state.activeDestinationId
        val first =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(activeA, "first"))
        val replacement =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(activeA, "replacement"))
        val disposeFirst =
            state.registerBackSurface(LibraryBackSurfacePort(activeA, first))
        state.registerBackSurface(LibraryBackSurfacePort(activeA, replacement))
        state.registerBackSurface(
            LibraryBackSurfacePort(
                b,
                LibraryBackTarget.FeatureModal(
                    LibraryBackTargetId(b, "hidden"))))
        disposeFirst()
        assertEquals(
            replacement,
            assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                .session
                .target)
        state.pendingBackSession!!.cancel()
        state.popToRoot()
        assertEquals(LibraryBackBeginResult.Unhandled, state.beginBack())
    }

    @Test
    fun featureCallbackReturnKeepsTheExactSessionPendingUntilItsPortDisappears() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destination = state.activeDestinationId
        var dispatched = 0
        val dispose =
            state.registerBackSurface(
                LibraryBackSurfacePort(
                    destination,
                    LibraryBackTarget.FeatureModal(
                        LibraryBackTargetId(destination, "modal"))) {
                        dispatched++
                        LibraryBackFeatureRequestResult.Started
                    },
            )
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}))
        assertEquals(1, dispatched)
        assertEquals(
            LibraryBackAdapterResult.Suppressed,
            performLibraryBack(state, null, {}))
        dispose()
        state.reconcileBackSession()
        assertEquals(null, state.pendingBackSession)
    }

    @Test
    fun ordinaryAndSystemAdaptersUseTheSameStateProtocol() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destination = state.activeDestinationId
        var dismisses = 0
        state.registerBackSurface(
            LibraryBackSurfacePort(
                destination,
                LibraryBackTarget.FeatureModal(
                    LibraryBackTargetId(destination, "modal"))) {
                    dismisses++
                    LibraryBackFeatureRequestResult.Started
                },
        )
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}))
        assertEquals(
            LibraryBackAdapterResult.Suppressed,
            performLibraryBack(state, null, {}))
        assertEquals(1, dismisses)
    }

    @Test
    fun explicitFeatureRejectionImmediatelyReleasesSuppressionWithoutRouteFallThrough() {
        val state = LibraryAppState(null)
        state.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val destination = state.activeDestinationId
        val target =
            LibraryBackTarget.FeatureModal(
                LibraryBackTargetId(destination, "rejecting-modal"))
        state.registerBackSurface(
            LibraryBackSurfacePort(destination, target) {
                LibraryBackFeatureRequestResult.Rejected
            },
        )

        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}),
        )
        assertNull(state.pendingBackSession)
        assertEquals(
            LibraryRoute.PlaylistDetail("playlist-1"), state.navigation.current)
    }
}
