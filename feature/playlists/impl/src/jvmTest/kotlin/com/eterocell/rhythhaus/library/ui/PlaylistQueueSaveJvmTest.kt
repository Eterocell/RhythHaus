package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.ui.HausWindowOverlayHost
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistQueueSaveJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun wideQueueSaveDialogBlocksControlsOutsideSidebar() = runComposeUiTest {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            var outsideClicks = 0
            setContent {
                HausWindowOverlayHost(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.align(Alignment.CenterStart)
                                .size(160.dp)
                                .clickable { outsideClicks++ }
                                .testTag("outside-player-control"),
                        )
                        Box(
                            Modifier.align(Alignment.CenterEnd)
                                .width(280.dp)
                                .fillMaxSize()
                                .testTag("queue-sidebar"),
                        ) {
                            QueueContentTestHost(
                                playbackState =
                                    playbackState(
                                        "current",
                                        "current" to "a",
                                        "upcoming" to "b",
                                    ),
                                destinationId = "wide-queue-modal",
                                onSaveQueueAsPlaylist = { _, _, _ -> },
                            )
                        }
                    }
                }
            }

            val outsideControlBounds =
                onNodeWithTag("outside-player-control")
                    .fetchSemanticsNode()
                    .boundsInRoot
            val sidebarWidth =
                onNodeWithTag("queue-sidebar")
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .width
            onNode(hasText("Save queue as playlist")).performClick()
            onNode(hasText("Create playlist")).assertExists()
            val saveDialogWidth =
                onNode(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.PaneTitle,
                            "Save queue as playlist",
                        ),
                    )
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .width
            assertTrue(saveDialogWidth > sidebarWidth)
            onNodeWithTag("outside-player-control").assertDoesNotExist()
            onRoot().performTouchInput {
                click(outsideControlBounds.center)
            }

            assertEquals(0, outsideClicks)
            onNodeWithText("Create playlist").assertExists()
            onNodeWithText("Cancel").performClick()

            onNodeWithText("Clear upcoming").performClick()
            val clearDialog =
                onAllNodes(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.PaneTitle,
                        "Clear upcoming tracks",
                    ),
                )
            clearDialog.assertCountEquals(1)
            val clearDialogWidth =
                clearDialog[0].fetchSemanticsNode().boundsInRoot.width
            assertTrue(clearDialogWidth > sidebarWidth)
            onRoot().performTouchInput {
                click(outsideControlBounds.center)
            }
            assertEquals(0, outsideClicks)
            clearDialog.assertCountEquals(1)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun queueDragTargetUsesNearestOccurrenceAndFallsBackToDraggedOccurrence() {
        val upcomingIds = listOf("up-1", "up-2", "up-3")
        val rowCenters = mapOf("up-1" to 80f, "up-2" to 170f, "up-3" to 280f)

        assertEquals(
            2,
            queueDragTargetIndex(
                pointerY = 265f,
                rowCenters = rowCenters,
                upcomingIds = upcomingIds,
                fallbackOccurrenceId = "up-1",
            ),
        )
        assertEquals(
            1,
            queueDragTargetIndex(
                pointerY = 400f,
                rowCenters = emptyMap(),
                upcomingIds = upcomingIds,
                fallbackOccurrenceId = "up-2",
            ),
        )
    }

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
                    QueueContentTestHost(
                        playbackState =
                            playbackState("current", "current" to "a"),
                        destinationId = "async-queue-save",
                        onSaveQueueAsPlaylist = { _, _, done ->
                            attempts++
                            completions += done
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
            QueueContentTestHost(
                playbackState = playbackState(null, "next" to "a"),
                destinationId = "empty-queue",
                onSaveQueueAsPlaylist = { _, _, _ -> attempts++ },
            )
        }
        onAllNodes(hasText("Save queue as playlist")).assertCountEquals(0)
        onAllNodes(hasText("a")).assertCountEquals(1)
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
                    QueueContentTestHost(
                        playbackState = playback,
                        destinationId = "queue-retry",
                        onSaveQueueAsPlaylist = { name, ids, done ->
                            attempts++
                            request = name to ids
                            done(PlaylistStateAction.MutationFailed("failed"))
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
                        QueueContentTestHost(
                            playbackState = playback,
                            destinationId = "queue-save",
                            onSaveQueueAsPlaylist = { name, ids, done ->
                                request = name to ids
                                done(
                                    PlaylistStateAction.SnapshotConfirmed(
                                        PlaylistSnapshot()))
                            },
                        )
                    }
                }
                onAllNodes(hasText("old")).assertCountEquals(1)
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun narrowQueueCanMoveUpcomingRowsWithoutDragging() = runComposeUiTest {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            var playback by
                mutableStateOf(
                    playbackState(
                        "current",
                        "current" to "a",
                        "next-b" to "b",
                        "next-c" to "c",
                    ),
                )
            val moves = mutableListOf<Pair<String, Int>>()
            setContent {
                Box(Modifier.size(390.dp, 600.dp)) {
                    QueueContentTestHost(
                        playbackState = playback,
                        destinationId = "queue-move",
                        onSaveQueueAsPlaylist = { _, _, _ -> },
                        onReorderUpcoming = { occurrenceId, targetIndex ->
                            moves += occurrenceId to targetIndex
                            val currentIndex =
                                playback.queue.indexOfFirst {
                                    it.id == playback.currentOccurrenceId
                                }
                            val fromIndex =
                                playback.queue.indexOfFirst {
                                    it.id == occurrenceId
                                }
                            val absoluteTargetIndex =
                                (currentIndex + 1 + targetIndex).coerceIn(
                                    currentIndex + 1,
                                    playback.queue.lastIndex,
                                )
                            val nextQueue = playback.queue.toMutableList()
                            val moved = nextQueue.removeAt(fromIndex)
                            nextQueue.add(absoluteTargetIndex, moved)
                            val refreshed = playback.copy(queue = nextQueue)
                            playback = refreshed
                            QueueMutationFeedback(refreshed, false)
                        },
                    )
                }
            }

            onNode(hasContentDescription("Move b down in queue"))
                .performScrollTo()
                .performClick()
            waitForIdle()
            assertEquals(listOf("next-b" to 1), moves)
            assertTrue(
                onNode(hasText("c")).fetchSemanticsNode().boundsInRoot.top <
                    onNode(hasText("b")).fetchSemanticsNode().boundsInRoot.top,
            )

            onNode(hasContentDescription("Move b up in queue"))
                .performScrollTo()
                .performClick()
            waitForIdle()
            assertEquals(listOf("next-b" to 1, "next-b" to 0), moves)
            assertTrue(
                onNode(hasText("b")).fetchSemanticsNode().boundsInRoot.top <
                    onNode(hasText("c")).fetchSemanticsNode().boundsInRoot.top,
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun narrowQueueCanRemoveOneDuplicateTrackOccurrenceWithoutDragging() =
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                var playback by
                    mutableStateOf(
                        playbackState(
                            "current",
                            "current" to "a",
                            "first-a" to "a",
                            "second-a" to "a",
                        ),
                    )
                val removed = mutableListOf<String>()
                setContent {
                    Box(Modifier.size(390.dp, 600.dp)) {
                        QueueContentTestHost(
                            playbackState = playback,
                            destinationId = "queue-remove",
                            onSaveQueueAsPlaylist = { _, _, _ -> },
                            onRemoveUpcoming = { occurrenceId ->
                                removed += occurrenceId
                                val refreshed =
                                    playback.copy(
                                        queue =
                                            playback.queue.filterNot {
                                                it.id == occurrenceId
                                            },
                                    )
                                playback = refreshed
                                QueueMutationFeedback(refreshed, false)
                            },
                        )
                    }
                }

                val removeActions =
                    onAllNodes(hasContentDescription("Remove a from queue"))
                removeActions.assertCountEquals(2)
                removeActions[1].performScrollTo().performClick()
                waitForIdle()

                assertEquals(listOf("second-a"), removed)
                onAllNodes(hasText("a")).assertCountEquals(2)
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

    @Composable
    private fun QueueContentTestHost(
        playbackState: PlaybackState,
        destinationId: String,
        onSaveQueueAsPlaylist:
            (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
        onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback =
            { _, _ ->
                QueueMutationFeedback(playbackState, false)
            },
        onRemoveUpcoming: suspend (String) -> QueueMutationFeedback = {
            QueueMutationFeedback(playbackState, false)
        },
    ) {
        val destination = PlaylistFeatureDestination(destinationId)
        PlaybackQueueContent(
            playbackState = playbackState,
            destination = destination,
            appearanceSource =
                rememberPlaylistFeatureAppearanceSource(destination),
            dismissalPublisher = noDismissalPublisher(),
            onClose = {},
            modifier = Modifier.fillMaxSize().testTag("queue-content"),
            onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
            onReorderUpcoming = onReorderUpcoming,
            onRemoveUpcoming = onRemoveUpcoming,
            onClearUpcoming = {
                QueueMutationFeedback(playbackState, false)
            },
        )
    }

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
