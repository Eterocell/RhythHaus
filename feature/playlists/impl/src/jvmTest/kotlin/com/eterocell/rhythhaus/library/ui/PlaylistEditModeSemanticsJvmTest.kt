package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistEditModeSemanticsJvmTest {
    @Test
    fun failedDeleteRetainsTheDisplayedDetailAndItsConfirmation() {
        val failure =
            PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, 7)
        assertEquals(
            PlaylistMutationDecision.RetainConfirmationWithFailure,
            playlistMutationDecision(PlaylistMutationWorkflow.Delete, failure))
    }
}
