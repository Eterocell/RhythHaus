package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.library.PlaylistSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistScreensTest {
    @Test
    fun createModalPresentationRetainsDraftAndShowsFailureAfterRevisionedOutcome() {
        val presentation =
            playlistNameModalPresentation(
                PlaylistNameDraft("  Name  "),
                PlaylistStateAction.MutationFailed(
                    PlaylistMutationFailedMessage, 8))
        assertTrue(presentation.isVisible)
        assertEquals("  Name  ", presentation.enteredText)
        assertEquals(PlaylistModalNotice.MutationFailed, presentation.notice)
    }

    @Test
    fun failedDeleteOutcomeRetainsConfirmationAndPlaylistSnapshot() {
        val playlist = PlaylistSummary("playlist-1", "Name", 0, 1)
        val state =
            reducePlaylistState(
                PlaylistState(
                    confirmedSnapshot =
                        PlaylistSnapshot(playlists = listOf(playlist)),
                    hasConfirmedSnapshot = true),
                PlaylistStateAction.MutationFailed(
                    PlaylistMutationFailedMessage, 9))
        assertEquals(
            PlaylistMutationDecision.RetainConfirmationWithFailure,
            playlistMutationDecision(
                PlaylistMutationWorkflow.Delete,
                PlaylistStateAction.MutationFailed(
                    PlaylistMutationFailedMessage, 9)))
        assertEquals(playlist, state.confirmedSnapshot.playlist(playlist.id))
    }
}
