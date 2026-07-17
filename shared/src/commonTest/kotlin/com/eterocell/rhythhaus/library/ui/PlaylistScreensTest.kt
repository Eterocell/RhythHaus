package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationRejection
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarContentPadding
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class PlaylistScreensTest {
    @Test
    fun playlistNameDraftTrimsValidNamesAndRejectsBlankWithoutDiscardingText() {
        val valid = PlaylistNameDraft("  Road trip  ")
        val blank = PlaylistNameDraft("   ")

        assertEquals("Road trip", valid.confirmedName())
        assertNull(blank.confirmedName())
        assertEquals("   ", blank.enteredText)
    }

    @Test
    fun failedNameMutationRetainsEnteredTextAndConfirmedSnapshot() {
        val draft = PlaylistNameDraft("  同名歌单  ")
        val failed = draft.mutationFailed()

        assertEquals("  同名歌单  ", failed.enteredText)
        assertTrue(failed.showFailure)
    }

    @Test
    fun duplicateEntriesMoveAndRemoveByEntryId() {
        val ids = listOf("entry-a", "entry-b", "entry-c")

        assertEquals(listOf("entry-b", "entry-a", "entry-c"), movedPlaylistEntryIds(ids, "entry-b", -1))
        assertEquals(listOf("entry-a", "entry-c"), ids.filterNot { it == "entry-b" })
        assertEquals(ids, movedPlaylistEntryIds(ids, "missing", 1))
    }

    @Test
    fun accessibleMoveAvailabilityMatchesVisibleEntryPosition() {
        val ids = listOf("entry-a", "entry-b", "entry-c")

        assertEquals(PlaylistMoveAvailability(canMoveUp = false, canMoveDown = true), playlistMoveAvailability(ids, "entry-a"))
        assertEquals(PlaylistMoveAvailability(canMoveUp = true, canMoveDown = true), playlistMoveAvailability(ids, "entry-b"))
        assertEquals(PlaylistMoveAvailability(canMoveUp = true, canMoveDown = false), playlistMoveAvailability(ids, "entry-c"))
    }

    @Test
    fun savedPlaybackUsesExactVisibleEntryOrderAndSelectedOccurrence() {
        val track = playableTrack("track-a")
        val entries = listOf(
            entry("entry-first", track.id, 0),
            entry("entry-second", track.id, 1),
        )

        val request = savedPlaylistPlaybackRequest(
            visibleEntries = entries,
            tracksById = mapOf(track.id to track),
            selectedEntryId = "entry-second",
        )

        assertEquals(listOf("entry-first", "entry-second"), request?.occurrences?.map { it.id })
        assertEquals("entry-second", request?.selectedOccurrenceId)
    }

    @Test
    fun pickerConfirmationAppendsOneIndependentOccurrenceEveryTime() {
        val picker = AddToPlaylistPickerState(trackId = "track-a", selectedPlaylistId = "playlist-1")

        assertEquals(PlaylistAppendRequest("playlist-1", listOf("track-a")), picker.confirmedAppend())
        assertEquals(PlaylistAppendRequest("playlist-1", listOf("track-a")), picker.confirmedAppend())
    }

    @Test
    fun pickerInlineCreationTrimsNameAndRetainsInvalidText() {
        val valid = AddToPlaylistPickerState(trackId = "track-a", enteredName = "  New list ")
        val blank = AddToPlaylistPickerState(trackId = "track-a", enteredName = "   ")

        assertEquals(PlaylistInlineCreateRequest("New list", "track-a"), valid.confirmedInlineCreate())
        assertNull(blank.confirmedInlineCreate())
        assertEquals("   ", blank.enteredName)
    }

    @Test
    fun detailBrowserAppendsSelectedTracksInVisibleOrder() {
        val state = PlaylistTrackBrowserState(
            playlistId = "playlist-1",
            visibleTrackIds = listOf("b", "a", "c"),
            selectedTrackIds = setOf("a", "b"),
        )

        assertEquals(listOf("b", "a"), state.confirmedTrackIds())
        assertEquals(PlaylistAppendRequest("playlist-1", listOf("b", "a")), state.confirmedAppend())
    }

    @Test
    fun detailBrowserSelectionIsKeyedByTrackIdAndSurvivesFiltering() {
        val selected = PlaylistTrackBrowserState(
            playlistId = "playlist-1",
            visibleTrackIds = listOf("a", "b"),
        ).toggle("b")
        val filtered = selected.copy(visibleTrackIds = listOf("b"))

        assertTrue("b" in filtered.selectedTrackIds)
        assertFalse("a" in filtered.selectedTrackIds)
        assertEquals(listOf("b"), filtered.confirmedTrackIds())
    }

    @Test
    fun removingFinalEntryRetainsEmptyPlaylistDetailModel() {
        val model = playlistDetailModel(
            playlistId = "playlist-1",
            playlistName = "Keep me",
            entries = listOf(entry("only", "track-a", 0)),
            tracksById = mapOf("track-a" to playableTrack("track-a")),
        ).withoutEntry("only")

        assertEquals("playlist-1", model.playlistId)
        assertEquals("Keep me", model.playlistName)
        assertTrue(model.rows.isEmpty())
    }

    @Test
    fun rowOverflowOpensPickerForExactTrackId() {
        assertEquals(
            PlaylistStateAction.OpenPicker(PlaylistPickerState(trackId = "track-b")),
            openAddToPlaylistPickerAction("track-b"),
        )
    }

    @Test
    fun pickerInlineCreationPlansAtomicInitialEntriesWithoutCollapsingDuplicates() {
        val request = PlaylistInlineCreateRequest(name = "New", trackId = "track-a")

        assertEquals(
            PlaylistInlineMutationPlan(name = "New", trackIds = listOf("track-a")),
            request.mutationPlan(),
        )
    }

    @Test
    fun browserFilteringUsesAuthoritativeOrderAcrossTitleArtistAndAlbum() {
        val tracks = listOf(
            browserTrack("b", title = "Beta", artist = "二号", album = "Night"),
            browserTrack("a", title = "Alpha", artist = "One", album = "晨光"),
            browserTrack("c", title = "Gamma", artist = "Three", album = "Day"),
        )

        assertEquals(listOf("b"), filteredPlaylistTrackIds(tracks, "二号"))
        assertEquals(listOf("a"), filteredPlaylistTrackIds(tracks, "晨光"))
        assertEquals(listOf("b", "a", "c"), filteredPlaylistTrackIds(tracks, ""))
    }

    @Test
    fun createModalPresentationRetainsDraftAndShowsFailureAfterRevisionedOutcome() {
        val draft = PlaylistNameDraft("  同名歌单  ")
        val presentation = playlistNameModalPresentation(
            draft = draft,
            outcome = PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, revision = 8L),
        )

        assertTrue(presentation.isVisible)
        assertEquals("  同名歌单  ", presentation.enteredText)
        assertEquals(PlaylistModalNotice.MutationFailed, presentation.notice)
    }

    @Test
    fun pickerAndBrowserPresentMutationFailureWithoutLosingSelection() {
        val state = PlaylistState(
            mutationErrorMessage = PlaylistMutationFailedMessage,
            picker = PlaylistPickerState("track-a", selectedPlaylistId = "playlist-1", enteredName = "New"),
            browser = PlaylistBrowserState("playlist-1", query = "blue", selectedTrackIds = setOf("track-a")),
        )

        val picker = playlistPickerPresentation(state)
        val browser = playlistBrowserPresentation(state)

        assertEquals("playlist-1", picker?.selectedPlaylistId)
        assertEquals("New", picker?.enteredName)
        assertEquals(PlaylistModalNotice.MutationFailed, picker?.notice)
        assertEquals(setOf("track-a"), browser?.selectedTrackIds)
        assertEquals("blue", browser?.query)
        assertEquals(PlaylistModalNotice.MutationFailed, browser?.notice)
    }

    @Test
    fun retainedReadFailurePresentationKeepsHubContentAndExposesRetry() {
        val state = PlaylistState(
            confirmedSnapshot = PlaylistSnapshot(playlists = listOf(playlist("playlist-1"))),
            hasConfirmedSnapshot = true,
            readErrorMessage = PlaylistReadFailedMessage,
        )

        val presentation = playlistRoutePresentation(state)

        assertTrue(presentation.showConfirmedContent)
        assertEquals(PlaylistRoutePresentationNotice.ReadFailed, presentation.notice)
        assertTrue(presentation.showRetry)
    }

    @Test
    fun searchAddActionPresentationContainsTrackTitleAndDispatchesExactTrack() {
        val action = searchAddToPlaylistPresentation("track-a", "夜に駆ける")

        assertEquals("track-a", action.trackId)
        assertEquals("夜に駆ける", action.trackTitle)
        assertEquals(openAddToPlaylistPickerAction("track-a"), action.action)
    }

    @Test
    fun destructivePresentationDoesNotDispatchUntilConfirmation() {
        val pending = playlistDestructivePresentation(entryId = "entry-a")

        assertNull(pending.confirmedEntryId)
        assertEquals("entry-a", pending.confirm().confirmedEntryId)
        assertNull(pending.dismiss().confirmedEntryId)
    }

    @Test
    fun dragPresentationDispatchesOneCompleteOrderForDraggedOccurrenceAndTargetRow() {
        val session = PlaylistDragPresentation(
            entryIds = listOf("a", "b", "c", "d"),
            draggedEntryId = "d",
        ).target(index = 1)

        assertEquals(listOf("a", "d", "b", "c"), session.finalOrder())
        assertEquals(listOf("a", "b", "c", "d"), session.finalOrder())
    }

    @Test
    fun failedDeleteOutcomeRetainsConfirmationAndPlaylistSnapshot() {
        val playlist = playlist("playlist-1")
        val state = reducePlaylistState(
            PlaylistState(
                confirmedSnapshot = PlaylistSnapshot(playlists = listOf(playlist)),
                hasConfirmedSnapshot = true,
            ),
            PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, revision = 9L),
        )

        val decision = playlistMutationDecision(
            workflow = PlaylistMutationWorkflow.Delete,
            outcome = PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, revision = 9L),
        )

        assertEquals(PlaylistMutationDecision.RetainConfirmationWithFailure, decision)
        assertEquals(playlist, state.confirmedSnapshot.playlist(playlist.id))
    }

    @Test
    fun actualMutationWorkflowDecisionsCloseOrRetainEveryTaskFiveSurface() {
        val success = PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot(), revision = 10L)
        val failure = PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, revision = 10L)

        val modalWorkflows = listOf(
            PlaylistMutationWorkflow.Create,
            PlaylistMutationWorkflow.Rename,
            PlaylistMutationWorkflow.PickerAppend,
            PlaylistMutationWorkflow.PickerInlineCreate,
            PlaylistMutationWorkflow.BrowserAppend,
        )
        modalWorkflows.forEach { workflow ->
            assertEquals(PlaylistMutationDecision.CloseModal, playlistMutationDecision(workflow, success))
            assertEquals(PlaylistMutationDecision.RetainModalWithFailure, playlistMutationDecision(workflow, failure))
        }
        assertEquals(PlaylistMutationDecision.CloseConfirmationAndRoute, playlistMutationDecision(PlaylistMutationWorkflow.Delete, success))
        assertEquals(PlaylistMutationDecision.RetainConfirmationWithFailure, playlistMutationDecision(PlaylistMutationWorkflow.Delete, failure))
        listOf(PlaylistMutationWorkflow.Remove, PlaylistMutationWorkflow.Reorder).forEach { workflow ->
            assertEquals(PlaylistMutationDecision.KeepRoute, playlistMutationDecision(workflow, success))
            assertEquals(PlaylistMutationDecision.ShowRouteFailure, playlistMutationDecision(workflow, failure))
        }
    }

    @Test
    fun failedInlineCreateCallbackRetainsPickerRetryState() {
        val initial = PlaylistState(
            picker = PlaylistPickerState(
                trackId = "track-a",
                selectedPlaylistId = "playlist-existing",
                enteredName = "Retry list",
            ),
            hasConfirmedSnapshot = true,
            publicationRevision = 11L,
        )
        val outcome = PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage, revision = 12L)

        val reduced = reducePlaylistState(initial, outcome)
        val decision = playlistMutationDecision(PlaylistMutationWorkflow.PickerInlineCreate, outcome)

        assertEquals(PlaylistMutationDecision.RetainModalWithFailure, decision)
        assertEquals("track-a", reduced.picker?.trackId)
        assertEquals("playlist-existing", reduced.picker?.selectedPlaylistId)
        assertEquals("Retry list", reduced.picker?.enteredName)
        assertEquals(PlaylistModalNotice.MutationFailed, playlistPickerPresentation(reduced)?.notice)
    }

    @Test
    fun queuePresentationPinsCurrentAndNeverPromotesHistoryToUpcoming() {
        val history = queueOccurrence("history", "History")
        val current = queueOccurrence("current", "Current")
        val upcoming = queueOccurrence("upcoming", "Upcoming")

        val presentation = queueTabPresentation(
            PlaybackState(
                currentOccurrenceId = current.id,
                queue = listOf(history, current, upcoming),
            ),
        )

        assertFalse(presentation.isEmpty)
        assertEquals(listOf("current", "upcoming"), presentation.rows.map { it.occurrence.id })
        assertEquals(QueueRowRole.Current, presentation.rows.first().role)
        assertFalse(presentation.rows.first().canDrag)
        assertFalse(presentation.rows.first().canMoveUp)
        assertFalse(presentation.rows.first().canMoveDown)
        assertFalse(presentation.rows.first().canRemove)
        assertEquals(QueueRowRole.Upcoming, presentation.rows.last().role)
    }

    @Test
    fun queuePresentationHasDistinctEmptyStateAndExactNowPlayingInset() {
        val presentation = queueTabPresentation(PlaybackState())

        assertTrue(presentation.isEmpty)
        assertTrue(presentation.rows.isEmpty())
        assertEquals(NowPlayingBarContentPadding, presentation.bottomContentPadding)
    }

    @Test
    fun duplicateUpcomingRowsStayIndependentAndExposeCorrectMoveBoundaries() {
        val duplicateTrack = playableTrack("duplicate")
        val presentation = queueTabPresentation(
            PlaybackState(
                currentOccurrenceId = "current",
                queue = listOf(
                    queueOccurrence("current", "Current"),
                    QueueOccurrence("duplicate-a", duplicateTrack),
                    QueueOccurrence("duplicate-b", duplicateTrack),
                ),
            ),
        )

        val first = presentation.rows[1]
        val second = presentation.rows[2]
        assertEquals(listOf("duplicate-a", "duplicate-b"), presentation.upcomingOccurrenceIds)
        assertFalse(first.canMoveUp)
        assertTrue(first.canMoveDown)
        assertTrue(second.canMoveUp)
        assertFalse(second.canMoveDown)
        assertTrue(first.canDrag && first.canRemove)
        assertTrue(second.canDrag && second.canRemove)
        assertEquals(listOf("duplicate-b", "duplicate-a"), presentation.movedUpcomingIds("duplicate-b", -1))
    }

    @Test
    fun queueRowsDriveLocalizedRoleFreeStateAndNamedActions() {
        val presentation = queueTabPresentation(
            PlaybackState(
                currentOccurrenceId = "current",
                queue = listOf(
                    queueOccurrence("current", "当前曲目"),
                    queueOccurrence("upcoming", "夜に駆ける"),
                ),
            ),
        )

        val current = presentation.rows.first()
        val upcoming = presentation.rows.last()
        assertEquals(QueueRowRole.Current, current.role)
        assertNull(current.semanticRole)
        assertEquals(QueueRowState.Current, current.semanticState)
        assertEquals(emptySet(), current.availableActions)
        assertTrue(current.actionTrackTitle == null)
        assertEquals(QueueRowRole.Upcoming, upcoming.role)
        assertNull(upcoming.semanticRole)
        assertEquals(QueueRowState.Upcoming, upcoming.semanticState)
        assertEquals(
            setOf(QueueRowAction.Drag, QueueRowAction.Remove),
            upcoming.availableActions,
        )
        assertEquals("夜に駆ける", upcoming.actionTrackTitle)
    }

    @Test
    fun queueRowLayoutPreservesMetadataAndMinimumTargetsAtCompactAndWideWidths() {
        val compact = queueRowLayoutPolicy(availableWidth = 320.dp, isEditable = true)
        val wide = queueRowLayoutPolicy(availableWidth = 720.dp, isEditable = true)
        val current = queueRowLayoutPolicy(availableWidth = 320.dp, isEditable = false)

        assertEquals(QueueActionPlacement.SecondaryRow, compact.actionPlacement)
        assertTrue(compact.reservesMetadataWidth)
        assertEquals(44.dp, compact.minimumInteractiveTarget)
        assertEquals(QueueActionPlacement.Inline, wide.actionPlacement)
        assertTrue(wide.reservesMetadataWidth)
        assertEquals(44.dp, wide.minimumInteractiveTarget)
        assertEquals(QueueActionPlacement.None, current.actionPlacement)
        assertTrue(current.reservesMetadataWidth)
    }

    @Test
    fun queueDragTargetsNearestMeasuredUpcomingRowAtAndBeyondBoundaries() {
        val centers = mapOf(0 to 100f, 1 to 200f, 2 to 300f)

        assertEquals(0, playlistDragTargetIndex(pointerY = 20f, rowCentersByIndex = centers, fallbackIndex = 1))
        assertEquals(1, playlistDragTargetIndex(pointerY = 190f, rowCentersByIndex = centers, fallbackIndex = 0))
        assertEquals(2, playlistDragTargetIndex(pointerY = 450f, rowCentersByIndex = centers, fallbackIndex = 1))
        assertEquals(1, playlistDragTargetIndex(pointerY = 450f, rowCentersByIndex = emptyMap(), fallbackIndex = 1))
    }

    @Test
    fun queueDragAfterFinalOccurrenceRemovalCannotTargetStaleMeasuredIndex() {
        val measuredCenters = mapOf(
            "upcoming-a" to 100f,
            "upcoming-b" to 200f,
            "upcoming-c" to 300f,
        )
        val currentUpcomingIds = listOf("upcoming-a", "upcoming-b")

        assertEquals(
            1,
            queueDragTargetIndex(
                pointerY = 450f,
                rowCentersByOccurrenceId = measuredCenters,
                upcomingIds = currentUpcomingIds,
                fallbackOccurrenceId = "upcoming-a",
            ),
        )
    }

    @Test
    fun rejectedQueueCommandRefreshesFromStateFlowAndShowsQueueChangedNotice() = runBlocking {
        val initial = PlaybackState(
            currentOccurrenceId = "current",
            queue = listOf(queueOccurrence("current", "Current"), queueOccurrence("stale", "Stale")),
        )
        val refreshed = initial.copy(queue = listOf(initial.queue.first()))
        val state = MutableStateFlow(initial)

        val feedback = executeQueueMutation(state) {
            state.value = refreshed
            QueueMutationResult.Rejected(QueueMutationRejection.StaleOccurrence)
        }

        assertEquals(refreshed, feedback.refreshedState)
        assertTrue(feedback.showQueueChanged)
    }

    @Test
    fun appliedQueueCommandTargetsExactDuplicateOccurrenceWithoutChangedNotice() = runBlocking {
        val duplicateTrack = playableTrack("duplicate")
        val state = MutableStateFlow(
            PlaybackState(
                currentOccurrenceId = "current",
                queue = listOf(
                    queueOccurrence("current", "Current"),
                    QueueOccurrence("duplicate-a", duplicateTrack),
                    QueueOccurrence("duplicate-b", duplicateTrack),
                ),
            ),
        )
        var targetedOccurrenceId: String? = null

        val feedback = executeQueueMutation(state) {
            targetedOccurrenceId = "duplicate-b"
            state.value = state.value.copy(queue = listOf(state.value.queue[0], state.value.queue[2]))
            QueueMutationResult.Applied
        }

        assertEquals("duplicate-b", targetedOccurrenceId)
        assertEquals(listOf("current", "duplicate-b"), feedback.refreshedState.queue.map { it.id })
        assertFalse(feedback.showQueueChanged)
    }

    @Test
    fun clearUpcomingDispatchesOnlyAfterExplicitConfirmation() {
        val pending = queueClearConfirmationPresentation()

        assertFalse(pending.shouldDispatchClear)
        assertTrue(pending.confirm().shouldDispatchClear)
        assertFalse(pending.dismiss().shouldDispatchClear)
    }

    @Test
    fun queueMutationDispatcherWiresExactOccurrenceTargetAndClearCommands() = runBlocking {
        val state = MutableStateFlow(
            PlaybackState(
                currentOccurrenceId = "current",
                queue = listOf(queueOccurrence("current", "Current"), queueOccurrence("duplicate-b", "Duplicate")),
            ),
        )
        val calls = mutableListOf<String>()
        val dispatcher = QueueMutationDispatcher(
            state = state,
            reorderCommand = { occurrenceId, targetIndex ->
                calls += "reorder:$occurrenceId:$targetIndex"
                QueueMutationResult.Applied
            },
            removeCommand = { occurrenceId ->
                calls += "remove:$occurrenceId"
                QueueMutationResult.Applied
            },
            clearCommand = {
                calls += "clear"
                QueueMutationResult.Applied
            },
        )

        dispatcher.reorder("duplicate-b", 0)
        dispatcher.remove("duplicate-b")
        dispatcher.clear()

        assertEquals(listOf("reorder:duplicate-b:0", "remove:duplicate-b", "clear"), calls)
    }

    private fun entry(id: String, trackId: String, position: Int) = PlaylistEntry(
        id = id,
        playlistId = "playlist-1",
        trackId = trackId,
        position = position,
        createdAtEpochMillis = 1L,
    )

    private fun playableTrack(id: String) = PlayableTrack(
        id = id,
        source = AudioSource.FilePath("/$id.mp3"),
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMillis = 180_000L,
    )

    private fun queueOccurrence(id: String, title: String) = QueueOccurrence(
        id = id,
        track = playableTrack(id).copy(title = title),
    )

    private fun browserTrack(id: String, title: String, artist: String, album: String) =
        com.eterocell.rhythhaus.library.LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = title,
            title = title,
            artist = artist,
            album = album,
            durationMillis = 180_000L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun playlist(id: String) = Playlist(
        id = id,
        name = "Playlist $id",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}
