package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest

class PlaylistBackPolicyJvmTest {
    @Test
    fun staleEditDisposerCannotClearNewOwner() {
        val policy = PlaylistBackRegistrationState()
        var firstClears = 0
        var secondClears = 0
        val first = policy.registerEdit(Any()) { firstClears++ }
        val owner = Any()
        val second = policy.registerEdit(owner) { secondClears++ }
        first()
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, policy.decision())
        policy.requestBack()
        assertEquals(0, firstClears)
        assertEquals(1, secondClears)
        second()
        assertEquals(LibraryBackDecision.None, policy.decision())
    }

    @Test
    fun staleModalDisposerCannotClearNewOwner() {
        val policy = PlaylistBackRegistrationState()
        var firstDismisses = 0
        var secondDismisses = 0
        val first = policy.registerModal(Any()) { firstDismisses++ }
        val second = policy.registerModal(Any()) { secondDismisses++ }
        first()
        assertEquals(LibraryBackDecision.DismissPlaylistModal, policy.decision())
        policy.requestBack()
        assertEquals(0, firstDismisses)
        assertEquals(1, secondDismisses)
        second()
        assertEquals(LibraryBackDecision.None, policy.decision())
    }

    @Test
    fun shippedDeleteCompletionFactoryClearsSelectionBeforePopping() {
        val events = mutableListOf<String>()
        directPlaylistDeleteCompletion(
            clearSelection = { events += "selection" },
            popRoute = { events += "pop" },
        ).invoke()
        assertEquals(listOf("selection", "pop"), events)
    }

    @Test
    fun registrationStateChangesAreObservableAtTheProductionBoundary() {
        val policy = PlaylistBackRegistrationState()
        val editOwner = Any()
        val modalOwner = Any()
        assertFalse(policy.hasEditRegistration)
        assertFalse(policy.hasModalRegistration)

        val clearEdit = policy.registerEdit(editOwner) {}
        assertTrue(policy.hasEditRegistration)
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, policy.decision())

        val dismissModal = policy.registerModal(modalOwner) {}
        assertTrue(policy.hasModalRegistration)
        assertEquals(LibraryBackDecision.DismissPlaylistModal, policy.decision())

        dismissModal()
        assertFalse(policy.hasModalRegistration)
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, policy.decision())
        clearEdit()
        assertFalse(policy.hasEditRegistration)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun composeObserverRecomposesWhenProductionBackRegistrationChanges() = runComposeUiTest {
        val policy = PlaylistBackRegistrationState()
        var observedDecision by mutableStateOf<LibraryBackDecision?>(null)
        var recompositions = 0
        setContent {
            recompositions++
            observedDecision = policy.decision()
        }
        waitForIdle()
        assertEquals(LibraryBackDecision.None, observedDecision)
        val dispose = policy.registerEdit(Any()) {}
        waitForIdle()
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, observedDecision)
        val afterRegister = recompositions
        dispose()
        waitForIdle()
        assertEquals(LibraryBackDecision.None, observedDecision)
        assertTrue(recompositions > afterRegister)
    }

    @Test
    fun productionBackDispatcherAdvancesPolicyAndUsesDirectPopOnlyAtRoute() {
        val policy = PlaylistBackRegistrationState()
        val events = mutableListOf<String>()
        var selection by mutableStateOf(TrackSelectionState())
        var nowPlaying by mutableStateOf(false)
        val dispatcher = PlaylistBackDispatchController(
            registration = policy,
            selectionState = { selection },
            isNowPlayingExpanded = { nowPlaying },
            canPopRoute = { true },
            cancelSelection = { events += "selection"; selection = TrackSelectionState() },
            hideNowPlaying = { events += "now-playing"; nowPlaying = false },
            directPopRoute = { events += "clear"; events += "pop" },
        )
        val editDispose = policy.registerEdit(Any()) { events += "edit"; editDisposeHolder?.invoke() }
        editDisposeHolder = editDispose
        dispatcher.dispatch()
        selection = TrackSelectionState(TrackSelectionPageKey.HomeSongs, setOf("track"))
        dispatcher.dispatch()
        nowPlaying = true
        dispatcher.dispatch()
        dispatcher.dispatch()
        assertEquals(listOf("edit", "selection", "now-playing", "clear", "pop"), events)
    }

    @Test
    fun systemBackCallbackUsesTheSameProductionDispatchAndDirectPopPrimitive() {
        val events = mutableListOf<String>()
        val policy = PlaylistBackRegistrationState()
        val controller = PlaylistBackDispatchController(
            registration = policy,
            selectionState = { TrackSelectionState() },
            isNowPlayingExpanded = { false },
            canPopRoute = { true },
            cancelSelection = {},
            hideNowPlaying = {},
            directPopRoute = { events += "clear"; events += "pop" },
        )
        controller.onSystemBackCompleted()
        assertEquals(listOf("clear", "pop"), events)
    }

    @Test
    fun productionNavigationBackCallbackUsesPredictivePopOnlyForPopRoute() {
        val events = mutableListOf<String>()
        var progress: Float? = null
        val callback = libraryBackCompletionCallback(
            decision = { LibraryBackDecision.PopRoute },
            transitionProgress = { 0.75f },
            setCompletionProgress = { progress = it },
            clearSelection = { events += "clear" },
            dispatchOrdinaryBack = { events += "ordinary" },
            navigationPop = { events += "navigation-pop"; LibraryNavigationStack() },
            completePredictivePop = { events += "complete" },
        )

        callback()

        assertEquals(listOf("clear", "navigation-pop", "complete"), events)
        assertEquals(null, progress)
    }

    @Test
    fun productionNavigationBackCallbackDoesNotPredictivelyPopModalOrEdit() {
        val events = mutableListOf<String>()
        var decision = LibraryBackDecision.DismissPlaylistModal
        val callback = libraryBackCompletionCallback(
            decision = { decision },
            transitionProgress = { error("not predictive") },
            setCompletionProgress = { error("not predictive") },
            clearSelection = { events += "clear" },
            dispatchOrdinaryBack = { events += "ordinary" },
            navigationPop = { error("not predictive") },
            completePredictivePop = { error("not predictive") },
        )

        callback()
        decision = LibraryBackDecision.ExitPlaylistEditMode
        callback()

        assertEquals(listOf("ordinary", "ordinary"), events)
    }

    private var editDisposeHolder: (() -> Unit)? = null
}
