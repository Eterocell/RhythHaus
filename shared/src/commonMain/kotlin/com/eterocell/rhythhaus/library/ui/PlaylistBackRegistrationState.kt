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

    fun requestBack() {
        when (decision()) {
            LibraryBackDecision.DismissPlaylistModal -> modalDismiss?.invoke()
            LibraryBackDecision.ExitPlaylistEditMode -> editClear?.invoke()
            else -> Unit
        }
    }
}

internal fun playlistDeleteCompletion(
    isModalOpen: () -> Boolean,
    dismissModal: () -> Unit,
    clearSelection: () -> Unit,
    popRoute: () -> Unit,
): () -> Unit = {
    if (isModalOpen()) dismissModal()
    clearSelection()
    popRoute()
}
