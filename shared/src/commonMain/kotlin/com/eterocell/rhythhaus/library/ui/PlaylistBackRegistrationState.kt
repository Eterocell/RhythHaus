package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class PlaylistBackRegistrationState {
    private var editOwner by mutableStateOf<Any?>(null)
    private var editClear by mutableStateOf<(() -> Unit)?>(null)
    private var modalOwner by mutableStateOf<Any?>(null)
    private var modalDismiss by mutableStateOf<(() -> Unit)?>(null)

    val hasEditRegistration: Boolean get() = editClear != null
    val hasModalRegistration: Boolean get() = modalDismiss != null

    fun registerEdit(owner: Any, clear: () -> Unit): () -> Unit {
        editOwner = owner
        editClear = clear
        return { if (editOwner === owner && editClear === clear) { editOwner = null; editClear = null } }
    }

    fun registerModal(owner: Any, dismiss: () -> Unit): () -> Unit {
        modalOwner = owner
        modalDismiss = dismiss
        return { if (modalOwner === owner && modalDismiss === dismiss) { modalOwner = null; modalDismiss = null } }
    }

    fun decision(selectionState: TrackSelectionState = TrackSelectionState(), isNowPlayingExpanded: Boolean = false, canPopRoute: Boolean = false): LibraryBackDecision =
        libraryBackDecision(modalDismiss != null, editClear != null, selectionState, isNowPlayingExpanded, canPopRoute)

    fun requestBack(
        selectionState: TrackSelectionState = TrackSelectionState(),
        isNowPlayingExpanded: Boolean = false,
        canPopRoute: Boolean = false,
    ) {
        when (decision(selectionState, isNowPlayingExpanded, canPopRoute)) {
            LibraryBackDecision.DismissPlaylistModal -> modalDismiss?.invoke()
            LibraryBackDecision.ExitPlaylistEditMode -> editClear?.invoke()
            else -> Unit
        }
    }
}

internal class PlaylistBackDispatchController(
    private val registration: PlaylistBackRegistrationState,
    private val selectionState: () -> TrackSelectionState,
    private val isNowPlayingExpanded: () -> Boolean,
    private val canPopRoute: () -> Boolean,
    private val cancelSelection: () -> Unit,
    private val hideNowPlaying: () -> Unit,
    private val directPopRoute: () -> Unit,
) {
    fun decision(): LibraryBackDecision = registration.decision(selectionState(), isNowPlayingExpanded(), canPopRoute())

    fun dispatch(popRouteOverride: (() -> Unit)? = null) {
        when (val decision = decision()) {
            LibraryBackDecision.DismissPlaylistModal,
            LibraryBackDecision.ExitPlaylistEditMode,
            -> registration.requestBack(selectionState(), isNowPlayingExpanded(), canPopRoute())
            LibraryBackDecision.CancelSelection -> cancelSelection()
            LibraryBackDecision.HideNowPlaying -> hideNowPlaying()
            LibraryBackDecision.PopRoute -> (popRouteOverride ?: directPopRoute).invoke()
            LibraryBackDecision.None -> Unit
        }
    }

    fun onSystemBackCompleted() = dispatch()
}

internal fun libraryBackCompletionCallback(
    decision: () -> LibraryBackDecision,
    transitionProgress: () -> Float?,
    setCompletionProgress: (Float?) -> Unit,
    clearSelection: () -> Unit,
    dispatchOrdinaryBack: () -> Unit,
    navigationPop: () -> LibraryNavigationStack,
    completePredictivePop: (LibraryNavigationStack) -> Unit,
): () -> Unit = {
    if (decision() == LibraryBackDecision.PopRoute) {
        setCompletionProgress(transitionProgress())
        clearSelection()
        val next = navigationPop()
        completePredictivePop(next)
        setCompletionProgress(null)
    } else {
        dispatchOrdinaryBack()
    }
}

internal fun directPlaylistDeleteCompletion(
    clearSelection: () -> Unit,
    popRoute: () -> Unit,
): () -> Unit = {
    clearSelection()
    popRoute()
}
