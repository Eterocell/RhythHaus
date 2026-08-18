package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PlaylistBackPolicyJvmTest {
    private var previousLocale: Locale? = null

    @BeforeTest
    fun setEnglishLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        previousLocale?.let { Locale.setDefault(it) }
    }

    @Test
    fun coreNavigationEventHandlerLatchesStartBeforeImmediateCompletionAndNeverRetargets() {
        val state = playlistState()
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        var firstDispatches = 0
        var replacementDispatches = 0
        val first =
            publish(state, "a") {
                firstDispatches++
                PlaylistFeatureDismissalDispatch.Started
            }
        val handler =
            LibraryNavigationEventBackHandler(dispatcher) { state.beginBack() }
        input.backStarted(NavigationEvent())
        assertEquals(
            "a",
            assertIs<LibraryBackTarget.FeatureModal>(
                    state.pendingBackSession!!.target)
                .id
                .instanceToken)
        first()
        val replacement =
            publish(state, "b") {
                replacementDispatches++
                PlaylistFeatureDismissalDispatch.Started
            }
        input.backCompleted()
        assertEquals(0, firstDispatches)
        assertEquals(0, replacementDispatches)
        assertNull(state.pendingBackSession)
        assertEquals(
            LibraryRoute.PlaylistDetail("playlist-1"), state.navigation.current)

        input.backStarted(NavigationEvent())
        input.backCompleted()
        assertEquals(1, replacementDispatches)
        replacement()
        assertNull(state.pendingBackSession)

        val cancelled = publish(state, "cancelled")
        input.backStarted(NavigationEvent())
        assertEquals(
            "cancelled",
            assertIs<LibraryBackTarget.FeatureModal>(
                    state.pendingBackSession!!.target)
                .id
                .instanceToken)
        input.backCancelled()
        assertNull(state.pendingBackSession)
        cancelled()
        handler.dispose()
        dispatcher.removeInput(input)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistHubRoutePublishesCreateToTheActiveDestinationAndRejectsStalePublishers() =
        runComposeUiTest {
            val state = LibraryAppState(null)
            state.pushRoute(LibraryRoute.PlaylistHub)
            val destination = state.activeDestinationId
            val dispatcher = NavigationEventDispatcher()
            val input = DirectNavigationEventInput()
            dispatcher.addInput(input)
            val handler =
                LibraryNavigationEventBackHandler(dispatcher) {
                    state.beginBack()
                }
            val playbackController = PlaybackController(FakePlaybackEngine())
            setContent {
                val appearanceSource =
                    rememberPlaylistFeatureAppearanceSource(
                        PlaylistFeatureDestination(destination.instanceToken))
                LibraryRouteContent(
                    route = LibraryRoute.PlaylistHub,
                    tracks = emptyList(),
                    snapshot =
                        LibrarySnapshot(
                            "Library", "", emptyList<Track>(), null),
                    libraryTracks = emptyList(),
                    playbackController = playbackController,
                    playbackState = PlaybackState(),
                    playlistRepository = EmptyPlaylistRepository,
                    playlistState =
                        PlaylistState(
                            confirmedSnapshot = PlaylistSnapshot(),
                            hasConfirmedSnapshot = true),
                    onPlaylistStateAction = {},
                    onRefreshPlaylists = {},
                    onPlaylistMutation = { _, _ -> },
                    onRecoverStalePlaylistDetail = {},
                    selectedTrackId = null,
                    isNowPlayingBarVisible = false,
                    onBack = {},
                    destinationId = destination,
                    playlistAppearanceSource = appearanceSource,
                    registerBackSurface = state::registerBackSurface,
                    onOpenDetailRoute = {},
                    onTrackSelected = {},
                    onTrackClickFromTracks = { _, _ -> },
                    onExpandNowPlaying = {},
                    onShowSettings = {},
                    onShowSearch = {},
                    onScrollPositionChanged = {},
                    artworkLoader = { null },
                    homeContent = { _ -> },
                )
            }
            waitForIdle()
            assertIs<LibraryBackTarget.Route>(
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
                    .target)
            state.pendingBackSession!!.reject()

            onNode(hasText("Create playlist")).performClick()
            waitForIdle()
            val first =
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
            val firstTarget =
                assertIs<LibraryBackTarget.FeatureModal>(first.target)
            assertEquals(destination, firstTarget.id.destinationId)
            assertEquals(
                "create", firstTarget.id.instanceToken.substringBefore('-'))
            first.reject()

            val staleDestination =
                LibraryDestinationId(LibraryRoute.PlaylistHub, "outgoing")
            featureDismissalPublisher(
                    staleDestination, state::registerBackSurface)
                .publish(
                    PlaylistFeatureDismissal.Modal(
                        PlaylistFeatureDestination("outgoing"),
                        PlaylistDismissalAppearance("stale")),
                ) {
                    error("outgoing publisher must not dispatch")
                }
            val retained =
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
            assertEquals(firstTarget, retained.target)
            retained.reject()

            input.backStarted(NavigationEvent())
            input.backCompleted()
            waitForIdle()
            assertNull(state.pendingBackSession)
            assertEquals(LibraryRoute.PlaylistHub, state.navigation.current)
            assertIs<LibraryBackTarget.Route>(
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
                    .target)
            state.pendingBackSession!!.reject()

            onNode(hasText("Create playlist")).performClick()
            waitForIdle()
            val reopened =
                assertIs<LibraryBackBeginResult.Started>(state.beginBack())
                    .session
            val reopenedTarget =
                assertIs<LibraryBackTarget.FeatureModal>(reopened.target)
            assertNotEquals(
                firstTarget.id.instanceToken, reopenedTarget.id.instanceToken)
            reopened.reject()
            handler.dispose()
            dispatcher.removeInput(input)
            playbackController.release()
        }

    @Test
    fun presentedDestinationPublishesEditAndAFeatureModalPrecedesIt() {
        val state = playlistState()
        val edit = publish(state, "edit", PlaylistFeatureDismissal::Edit)
        val modal = publish(state, "modal", PlaylistFeatureDismissal::Modal)
        val session =
            assertIs<LibraryBackBeginResult.Started>(state.beginBack()).session
        assertIs<LibraryBackTarget.FeatureModal>(session.target)
        session.reject()
        modal()
        val restoredEdit =
            publish(state, "edit", PlaylistFeatureDismissal::Edit)
        val editSession =
            assertIs<LibraryBackBeginResult.Started>(state.beginBack()).session
        assertIs<LibraryBackTarget.FeatureEdit>(editSession.target)
        editSession.reject()
        restoredEdit()
        edit()
    }

    @Test
    fun inactiveHiddenAndOutgoingPortsAreRejectedAndStaleDisposersAreSafe() {
        val state = playlistState()
        val oldDestination = state.activeDestinationId
        state.replaceTopRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val active = state.activeDestinationId
        val stale =
            featureDismissalPublisher(
                    oldDestination, state::registerBackSurface)
                .publish(
                    PlaylistFeatureDismissal.Modal(
                        PlaylistFeatureDestination("old"),
                        PlaylistDismissalAppearance("old"))) {
                        PlaylistFeatureDismissalDispatch.Started
                    }
        val first = publish(state, "first")
        val replacement = publish(state, "replacement")
        first()
        assertEquals("replacement", featureTarget(state).id.instanceToken)
        stale()
        assertEquals(active, featureTarget(state).id.destinationId)
        replacement()
        assertIs<LibraryBackBeginResult.Started>(state.beginBack())
            .session
            .reject()
    }

    @Test
    fun featureCallbackReturnKeepsTheExactSessionPendingUntilItsPortDisappears() {
        val state = playlistState()
        val dispose = publish(state, "modal")
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}))
        assertEquals(
            LibraryBackAdapterResult.Suppressed,
            performLibraryBack(state, null, {}))
        dispose()
        assertNull(state.pendingBackSession)
    }

    @Test
    fun explicitFeatureRejectionImmediatelyReleasesSuppressionWithoutRouteFallThrough() {
        val state = playlistState()
        publish(state, "rejecting") {
            PlaylistFeatureDismissalDispatch.Rejected
        }
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}))
        assertNull(state.pendingBackSession)
        assertEquals(
            LibraryRoute.PlaylistDetail("playlist-1"), state.navigation.current)
    }

    @Test
    fun predictiveCancellationReleasesTheLatchedFeatureTarget() {
        val state = playlistState()
        publish(state, "modal")
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler =
            LibraryNavigationEventBackHandler(dispatcher) { state.beginBack() }
        input.backStarted(NavigationEvent())
        input.backCancelled()
        assertNull(state.pendingBackSession)
        handler.dispose()
        dispatcher.removeInput(input)
    }

    @Test
    fun nonPredictiveSettlementWaitsForAuthoritativeInactivityWithoutCallbackReturn() {
        val state = playlistState()
        val dispose = publish(state, "modal")
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null, {}))
        assertIs<LibraryBackSession>(state.pendingBackSession)
        dispose()
        assertNull(state.pendingBackSession)
    }

    private fun playlistState(): LibraryAppState =
        LibraryAppState(null).also {
            it.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        }

    private fun publish(
        state: LibraryAppState,
        appearance: String,
        kind:
            (
                PlaylistFeatureDestination,
                PlaylistDismissalAppearance) -> PlaylistFeatureDismissal =
            PlaylistFeatureDismissal::Modal,
        dispatch: () -> PlaylistFeatureDismissalDispatch = {
            PlaylistFeatureDismissalDispatch.Started
        },
    ): () -> Unit {
        val destination = state.activeDestinationId
        return featureDismissalPublisher(
                destination, state::registerBackSurface)
            .publish(
                kind(
                    PlaylistFeatureDestination(destination.instanceToken),
                    PlaylistDismissalAppearance(appearance)),
            ) {
                dispatch()
            }
    }

    private fun featureTarget(state: LibraryAppState): LibraryBackTarget =
        assertIs<LibraryBackBeginResult.Started>(state.beginBack())
            .session
            .target
            .also {
                state.pendingBackSession!!.reject()
            }

    private object EmptyPlaylistRepository : PlaylistRepository {
        override fun playlists(): List<PlaylistSummary> = emptyList()

        override fun playlist(id: String): PlaylistSummary? = null

        override fun entries(playlistId: String): List<PlaylistEntry> =
            emptyList()

        override fun create(name: String): PlaylistSummary = error("not used")

        override fun createWithEntries(
            name: String,
            trackIds: List<String>
        ): PlaylistSummary = error("not used")

        override fun importPlaylists(
            playlists: List<PlaylistImportMutation>
        ): List<PlaylistSummary> = error("not used")

        override fun rename(id: String, name: String) = error("not used")

        override fun delete(id: String) = error("not used")

        override fun append(playlistId: String, trackIds: List<String>) =
            error("not used")

        override fun removeEntry(entryId: String) = error("not used")

        override fun reorder(playlistId: String, entryIds: List<String>) =
            error("not used")
    }
}
