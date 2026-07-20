package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun deleteCompletionPopsOnlyAfterModalIsDismissed() {
        val events = mutableListOf<String>()
        var modalOpen = true
        val completeDelete = playlistDeleteCompletion(
            isModalOpen = { modalOpen },
            dismissModal = { modalOpen = false; events += "dismiss" },
            clearSelection = { events += "selection" },
            popRoute = { events += "pop" },
        )
        completeDelete()
        assertEquals(listOf("dismiss", "selection", "pop"), events)
        assertEquals(false, modalOpen)
    }
}
