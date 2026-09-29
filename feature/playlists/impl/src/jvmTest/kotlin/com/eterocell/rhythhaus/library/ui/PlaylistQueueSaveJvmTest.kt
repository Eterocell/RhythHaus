package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueOccurrence
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaylistQueueSaveJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pendingConfirmationDoesNotDuplicateAndOldCompletionCannotDismissReopenedDraft() =
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                var attempts = 0
                setContent {
                    PlaylistHubScreen(
                        state =
                            PlaylistState(
                                selectedTab = PlaylistTab.Queue,
                                hasConfirmedSnapshot = true),
                        playbackState =
                            playbackState("current", "current" to "a"),
                        destination =
                            PlaylistFeatureDestination("async-queue-save"),
                        appearanceSource =
                            rememberPlaylistFeatureAppearanceSource(
                                PlaylistFeatureDestination("async-queue-save")),
                        dismissalPublisher = noDismissalPublisher(),
                        playlistsLabel = "Playlists",
                        loadingLabel = "Loading",
                        loadFailedLabel = "Failed",
                        retryLabel = "Retry",
                        mutationFailedLabel = "Failed",
                        libraryTracks = emptyList(),
                        onBack = {},
                        onOpenPlaylist = {},
                        onOpenSmartPlaylist = {
                            error("Unexpected smart route")
                        },
                        onCreateSmartPlaylist = { _, _, _ ->
                            error("Unexpected smart creation")
                        },
                        onSelectTab = {},
                        onCreate = { _, _ -> },
                        onSaveQueueAsPlaylist = { _, _, done ->
                            attempts++
                            completions += done
                        },
                        onRetry = {},
                        onReorderUpcoming = { _, _ ->
                            QueueMutationFeedback(playbackState(null), false)
                        },
                        onRemoveUpcoming = {
                            QueueMutationFeedback(playbackState(null), false)
                        },
                        onClearUpcoming = {
                            QueueMutationFeedback(playbackState(null), false)
                        },
                    )
                }
                onAllNodes(hasText("Save queue as playlist"))[0].performClick()
                onAllNodes(hasText("Playlist name"))[0].performTextInput(
                    "First")
                onAllNodes(hasText("Create playlist"))[0].performClick()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                assertEquals(1, attempts)

                onAllNodes(hasText("Cancel"))[0].performClick()
                onAllNodes(hasText("Save queue as playlist"))[0].performClick()
                onAllNodes(hasText("Playlist name"))[0].performTextInput(
                    "Second")
                onAllNodes(hasText("Create playlist"))[0].assertExists()
                completions[0](
                    PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
                waitForIdle()
                onAllNodes(hasText("Create playlist"))[0].assertExists()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                assertEquals(2, attempts)
                completions[1](PlaylistStateAction.MutationFailed("failed"))
                waitForIdle()
                onAllNodes(hasText("Could not save playlist changes"))[0]
                    .assertExists()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                assertEquals(3, attempts)
            } finally {
                Locale.setDefault(originalLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unselectedQueueDoesNotOfferSaveAction() = runComposeUiTest {
        var attempts = 0
        setContent {
            PlaylistHubScreen(
                state =
                    PlaylistState(
                        selectedTab = PlaylistTab.Queue,
                        hasConfirmedSnapshot = true),
                playbackState = playbackState(null, "next" to "a"),
                destination = PlaylistFeatureDestination("empty-queue"),
                appearanceSource =
                    rememberPlaylistFeatureAppearanceSource(
                        PlaylistFeatureDestination("empty-queue")),
                dismissalPublisher = noDismissalPublisher(),
                playlistsLabel = "Playlists",
                loadingLabel = "Loading",
                loadFailedLabel = "Failed",
                retryLabel = "Retry",
                mutationFailedLabel = "Failed",
                libraryTracks = emptyList(),
                onBack = {},
                onOpenPlaylist = {},
                onOpenSmartPlaylist = { error("Unexpected smart route") },
                onCreateSmartPlaylist = { _, _, _ ->
                    error("Unexpected smart creation")
                },
                onSelectTab = {},
                onCreate = { _, _ -> },
                onSaveQueueAsPlaylist = { _, _, _ -> attempts++ },
                onRetry = {},
                onReorderUpcoming = { _, _ ->
                    QueueMutationFeedback(playbackState(null), false)
                },
                onRemoveUpcoming = {
                    QueueMutationFeedback(playbackState(null), false)
                },
                onClearUpcoming = {
                    QueueMutationFeedback(playbackState(null), false)
                },
            )
        }
        onAllNodes(hasText("Save queue as playlist")).assertCountEquals(0)
        assertEquals(0, attempts)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedSaveRetainsFrozenDraftForRetryAndCancelWritesNothing() =
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                var playback by
                    mutableStateOf(
                        playbackState(
                            "current", "current" to "a", "next" to "b"))
                var request: Pair<String, List<String>>? = null
                var attempts = 0
                setContent {
                    PlaylistHubScreen(
                        state =
                            PlaylistState(
                                selectedTab = PlaylistTab.Queue,
                                hasConfirmedSnapshot = true),
                        playbackState = playback,
                        destination = PlaylistFeatureDestination("queue-retry"),
                        appearanceSource =
                            rememberPlaylistFeatureAppearanceSource(
                                PlaylistFeatureDestination("queue-retry")),
                        dismissalPublisher = noDismissalPublisher(),
                        playlistsLabel = "Playlists",
                        loadingLabel = "Loading",
                        loadFailedLabel = "Failed",
                        retryLabel = "Retry",
                        mutationFailedLabel = "Save failed",
                        libraryTracks = emptyList(),
                        onBack = {},
                        onOpenPlaylist = {},
                        onOpenSmartPlaylist = {
                            error("Unexpected smart route")
                        },
                        onCreateSmartPlaylist = { _, _, _ ->
                            error("Unexpected smart creation")
                        },
                        onSelectTab = {},
                        onCreate = { _, _ -> },
                        onSaveQueueAsPlaylist = { name, ids, done ->
                            attempts++
                            request = name to ids
                            done(PlaylistStateAction.MutationFailed("failed"))
                        },
                        onRetry = {},
                        onReorderUpcoming = { _, _ ->
                            QueueMutationFeedback(playback, false)
                        },
                        onRemoveUpcoming = {
                            QueueMutationFeedback(playback, false)
                        },
                        onClearUpcoming = {
                            QueueMutationFeedback(playback, false)
                        },
                    )
                }
                onAllNodes(hasText("Save queue as playlist"))[0].performClick()
                onAllNodes(hasText("Cancel"))[0].performClick()
                assertNull(request)
                onAllNodes(hasText("Save queue as playlist"))[0].performClick()
                onAllNodes(hasText("Playlist name"))[0].performTextInput(
                    "Keep this")
                playback = playbackState("other", "other" to "c")
                waitForIdle()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                onAllNodes(hasText("Could not save playlist changes"))[0]
                    .assertExists()
                onAllNodes(hasText("Create playlist"))[0].performClick()
                assertEquals(2, attempts)
                assertEquals("Keep this" to listOf("a", "b"), request)
            } finally {
                Locale.setDefault(originalLocale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun savesFrozenVisibleOccurrencesWithDuplicatesAfterQueueChanges() =
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                var playback by
                    mutableStateOf(
                        playbackState(
                            "current",
                            "past" to "old",
                            "current" to "a",
                            "next" to "b",
                            "again" to "a",
                        ),
                    )
                var request: Pair<String, List<String>>? = null
                setContent {
                    Box(Modifier.size(600.dp, 400.dp)) {
                        PlaylistHubScreen(
                            state =
                                PlaylistState(
                                    selectedTab = PlaylistTab.Queue,
                                    hasConfirmedSnapshot = true),
                            playbackState = playback,
                            destination =
                                PlaylistFeatureDestination("queue-save"),
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    PlaylistFeatureDestination("queue-save")),
                            dismissalPublisher = noDismissalPublisher(),
                            playlistsLabel = "Playlists",
                            loadingLabel = "Loading",
                            loadFailedLabel = "Failed",
                            retryLabel = "Retry",
                            mutationFailedLabel = "Save failed",
                            libraryTracks = emptyList(),
                            onBack = {},
                            onOpenPlaylist = {},
                            onOpenSmartPlaylist = {
                                error("Unexpected smart route")
                            },
                            onCreateSmartPlaylist = { _, _, _ ->
                                error("Unexpected smart creation")
                            },
                            onSelectTab = {},
                            onCreate = { _, _ -> },
                            onSaveQueueAsPlaylist = { name, ids, done ->
                                request = name to ids
                                done(
                                    PlaylistStateAction.SnapshotConfirmed(
                                        PlaylistSnapshot()))
                            },
                            onRetry = {},
                            onReorderUpcoming = { _, _ ->
                                QueueMutationFeedback(playback, false)
                            },
                            onRemoveUpcoming = {
                                QueueMutationFeedback(playback, false)
                            },
                            onClearUpcoming = {
                                QueueMutationFeedback(playback, false)
                            },
                        )
                    }
                }
                onAllNodes(hasText("Save queue as playlist"))[0].performClick()
                playback = playbackState("replacement", "replacement" to "new")
                waitForIdle()
                onAllNodes(hasText("Playlist name"))[0].performTextInput(
                    " Road trip ")
                onAllNodes(hasText("Create playlist"))[0].performClick()
                assertEquals("Road trip" to listOf("a", "b", "a"), request)
                assertEquals("replacement", playback.currentOccurrenceId)
                onAllNodes(hasText("Playlist name")).assertCountEquals(0)
            } finally {
                Locale.setDefault(originalLocale)
            }
        }

    private fun playbackState(
        current: String?,
        vararg occurrences: Pair<String, String>
    ) =
        PlaybackState(
            currentOccurrenceId = current,
            queue =
                occurrences.map { (id, track) ->
                    QueueOccurrence(
                        id,
                        PlayableTrack(
                            track,
                            track,
                            "artist",
                            "album",
                            1000,
                            AudioSource.FilePath("/$track")),
                    )
                },
        )

    private fun noDismissalPublisher() =
        object : PlaylistFeatureDismissalPublisher {
            override fun publish(
                dismissal: PlaylistFeatureDismissal?,
                dispatch:
                    (
                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
            ): () -> Unit = {}
        }
}
