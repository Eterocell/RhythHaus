package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationRejection
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.PlaylistSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

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

    @Test
    fun rejectedQueueCommandRefreshesFromStateFlowAndShowsQueueChangedNotice() =
        runBlocking {
            val track =
                PlayableTrack(
                    "track",
                    "Track",
                    "Artist",
                    "Album",
                    1_000,
                    AudioSource.FilePath("/track"))
            val initial =
                PlaybackState(
                    currentOccurrenceId = "current",
                    queue =
                        listOf(
                            QueueOccurrence("current", track),
                            QueueOccurrence("stale", track)))
            val state = MutableStateFlow(initial)
            val refreshed = initial.copy(queue = listOf(initial.queue.first()))
            val feedback =
                executeQueueMutation(state) {
                    state.value = refreshed
                    QueueMutationResult.Rejected(
                        QueueMutationRejection.StaleOccurrence)
                }
            assertEquals(refreshed, feedback.refreshedState)
            assertTrue(feedback.showQueueChanged)
        }

    @Test
    fun clearUpcomingDispatchesOnlyAfterExplicitConfirmation() {
        val pending = queueClearConfirmationPresentation()
        assertFalse(pending.shouldDispatchClear)
        assertTrue(pending.confirm().shouldDispatchClear)
    }
}
