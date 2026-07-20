package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistBackPolicyJvmTest {
    @Test
    fun staleEditDisposerCannotClearNewOwner() {
        val policy = PlaylistBackRegistrationPolicy()
        var firstClears = 0
        var secondClears = 0
        val first = policy.register(Any()) { firstClears++ }
        val owner = Any()
        val second = policy.register(owner) { secondClears++ }
        first()
        assertEquals(LibraryBackDecision.ExitPlaylistEditMode, policy.decision())
        policy.requestBack()
        assertEquals(0, firstClears)
        assertEquals(1, secondClears)
        second()
        assertEquals(LibraryBackDecision.None, policy.decision())
    }
}

private class PlaylistBackRegistrationPolicy {
    private var owner: Any? = null
    private var clear: (() -> Unit)? = null

    fun register(nextOwner: Any, nextClear: () -> Unit): () -> Unit {
        owner = nextOwner
        clear = nextClear
        return { if (owner === nextOwner && clear === nextClear) { owner = null; clear = null } }
    }

    fun decision(): LibraryBackDecision = libraryBackDecision(
        isPlaylistEditModeActive = clear != null,
        selectionState = TrackSelectionState(),
        isNowPlayingExpanded = false,
        canPopRoute = false,
    )

    fun requestBack() {
        if (decision() == LibraryBackDecision.ExitPlaylistEditMode) clear?.invoke()
    }
}
