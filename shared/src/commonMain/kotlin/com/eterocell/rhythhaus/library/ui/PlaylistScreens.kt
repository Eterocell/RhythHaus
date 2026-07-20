package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.formatDuration
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.LazyTrackArtworkImage
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.playlist_add_to
import rhythhaus.shared.generated.resources.playlist_add_tracks
import rhythhaus.shared.generated.resources.playlist_choose_existing
import rhythhaus.shared.generated.resources.playlist_confirm_add
import rhythhaus.shared.generated.resources.playlist_create
import rhythhaus.shared.generated.resources.playlist_create_inline
import rhythhaus.shared.generated.resources.playlist_create_name
import rhythhaus.shared.generated.resources.playlist_delete
import rhythhaus.shared.generated.resources.playlist_delete_confirmation_format
import rhythhaus.shared.generated.resources.playlist_drag_format
import rhythhaus.shared.generated.resources.playlist_empty_detail
import rhythhaus.shared.generated.resources.playlist_empty_queue
import rhythhaus.shared.generated.resources.playlist_empty_saved
import rhythhaus.shared.generated.resources.playlist_entry_state
import rhythhaus.shared.generated.resources.playlist_move_down_format
import rhythhaus.shared.generated.resources.playlist_move_up_format
import rhythhaus.shared.generated.resources.playlist_mutation_failed
import rhythhaus.shared.generated.resources.playlist_load_failed
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.playlist_queue_tab
import rhythhaus.shared.generated.resources.playlist_remove_track_format
import rhythhaus.shared.generated.resources.playlist_rename
import rhythhaus.shared.generated.resources.playlist_row_accessibility_format
import rhythhaus.shared.generated.resources.playlist_retry
import rhythhaus.shared.generated.resources.playlist_saved_tab
import rhythhaus.shared.generated.resources.playlist_selected_state
import rhythhaus.shared.generated.resources.playlist_track_browser_search
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.queue_changed
import rhythhaus.shared.generated.resources.queue_clear_confirmation
import rhythhaus.shared.generated.resources.queue_clear_confirm
import rhythhaus.shared.generated.resources.queue_clear_upcoming
import rhythhaus.shared.generated.resources.queue_current
import rhythhaus.shared.generated.resources.queue_current_state
import rhythhaus.shared.generated.resources.queue_drag_format
import rhythhaus.shared.generated.resources.queue_move_down_format
import rhythhaus.shared.generated.resources.queue_move_up_format
import rhythhaus.shared.generated.resources.queue_remove_format
import rhythhaus.shared.generated.resources.queue_upcoming
import rhythhaus.shared.generated.resources.queue_upcoming_state
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults

data class PlaylistNameDraft(
    val enteredText: String = "",
    val showFailure: Boolean = false,
) {
    fun confirmedName(): String? = enteredText.trim().takeIf(String::isNotEmpty)
    fun mutationFailed(): PlaylistNameDraft = copy(showFailure = true)
}

enum class PlaylistModalNotice { MutationFailed }

enum class PlaylistMutationWorkflow {
    Create,
    Rename,
    Delete,
    PickerAppend,
    PickerInlineCreate,
    BrowserAppend,
    Remove,
    Reorder,
}

enum class PlaylistMutationDecision {
    CloseModal,
    RetainModalWithFailure,
    CloseConfirmationAndRoute,
    RetainConfirmationWithFailure,
    KeepRoute,
    ShowRouteFailure,
}

fun playlistMutationDecision(
    workflow: PlaylistMutationWorkflow,
    outcome: PlaylistStateAction,
): PlaylistMutationDecision = when (workflow) {
    PlaylistMutationWorkflow.Delete -> if (outcome is PlaylistStateAction.SnapshotConfirmed) {
        PlaylistMutationDecision.CloseConfirmationAndRoute
    } else {
        PlaylistMutationDecision.RetainConfirmationWithFailure
    }
    PlaylistMutationWorkflow.Remove,
    PlaylistMutationWorkflow.Reorder,
    -> if (outcome is PlaylistStateAction.SnapshotConfirmed) {
        PlaylistMutationDecision.KeepRoute
    } else {
        PlaylistMutationDecision.ShowRouteFailure
    }
    else -> if (outcome is PlaylistStateAction.SnapshotConfirmed) {
        PlaylistMutationDecision.CloseModal
    } else {
        PlaylistMutationDecision.RetainModalWithFailure
    }
}

fun trackSelectionActionAfterPickerOutcome(outcome: PlaylistStateAction?): TrackSelectionAction? =
    if (outcome is PlaylistStateAction.SnapshotConfirmed) TrackSelectionAction.Completed else null

data class PlaylistNameModalPresentation(
    val enteredText: String,
    val notice: PlaylistModalNotice? = null,
) {
    val isVisible: Boolean = true
}

fun playlistNameModalPresentation(
    draft: PlaylistNameDraft,
    outcome: PlaylistStateAction? = null,
): PlaylistNameModalPresentation = PlaylistNameModalPresentation(
    enteredText = draft.enteredText,
    notice = if (outcome is PlaylistStateAction.MutationFailed) PlaylistModalNotice.MutationFailed else null,
)

data class PlaylistPickerPresentation(
    val selectedPlaylistId: String?,
    val enteredName: String,
    val notice: PlaylistModalNotice?,
)

fun playlistPickerPresentation(state: PlaylistState): PlaylistPickerPresentation? = state.picker?.let {
    PlaylistPickerPresentation(
        selectedPlaylistId = it.selectedPlaylistId,
        enteredName = it.enteredName,
        notice = if (state.mutationErrorMessage != null) PlaylistModalNotice.MutationFailed else null,
    )
}

data class PlaylistBrowserPresentation(
    val query: String,
    val selectedTrackIds: Set<String>,
    val notice: PlaylistModalNotice?,
)

fun playlistBrowserPresentation(state: PlaylistState): PlaylistBrowserPresentation? = state.browser?.let {
    PlaylistBrowserPresentation(
        query = it.query,
        selectedTrackIds = it.selectedTrackIds,
        notice = if (state.mutationErrorMessage != null) PlaylistModalNotice.MutationFailed else null,
    )
}

enum class PlaylistRoutePresentationNotice { ReadFailed }

data class PlaylistRoutePresentation(
    val showConfirmedContent: Boolean,
    val notice: PlaylistRoutePresentationNotice?,
    val showRetry: Boolean,
)

fun playlistRoutePresentation(state: PlaylistState): PlaylistRoutePresentation = PlaylistRoutePresentation(
    showConfirmedContent = state.hasConfirmedSnapshot,
    notice = if (state.readErrorMessage != null) PlaylistRoutePresentationNotice.ReadFailed else null,
    showRetry = state.readErrorMessage != null,
)

data class SearchAddToPlaylistPresentation(
    val trackId: String,
    val trackTitle: String,
    val action: PlaylistStateAction,
)

fun searchAddToPlaylistPresentation(trackId: String, trackTitle: String) = SearchAddToPlaylistPresentation(
    trackId = trackId,
    trackTitle = trackTitle,
    action = openAddToPlaylistPickerAction(trackId),
)

data class PlaylistDestructivePresentation(
    val entryId: String,
    val confirmedEntryId: String? = null,
) {
    fun confirm() = copy(confirmedEntryId = entryId)
    fun dismiss() = copy(confirmedEntryId = null)
}

fun playlistDestructivePresentation(entryId: String) = PlaylistDestructivePresentation(entryId)

class PlaylistDragPresentation(
    private val entryIds: List<String>,
    private val draggedEntryId: String,
) {
    private var targetIndex = entryIds.indexOf(draggedEntryId)
    private var consumed = false

    fun target(index: Int): PlaylistDragPresentation = apply {
        targetIndex = index.coerceIn(entryIds.indices)
    }

    fun finalOrder(): List<String> {
        if (consumed) return entryIds
        consumed = true
        val sourceIndex = entryIds.indexOf(draggedEntryId)
        if (sourceIndex < 0 || sourceIndex == targetIndex) return entryIds
        return entryIds.toMutableList().apply { add(targetIndex, removeAt(sourceIndex)) }
    }
}

fun playlistDragTargetIndex(
    pointerY: Float,
    rowCentersByIndex: Map<Int, Float>,
    fallbackIndex: Int,
    rowCount: Int? = null,
): Int = (rowCentersByIndex.minByOrNull { (_, centerY) -> kotlin.math.abs(centerY - pointerY) }?.key ?: fallbackIndex)
    .let { target -> rowCount?.let { target.coerceIn(0, (it - 1).coerceAtLeast(0)) } ?: target }

fun queueDragTargetIndex(
    pointerY: Float,
    rowCentersByOccurrenceId: Map<String, Float>,
    upcomingIds: List<String>,
    fallbackOccurrenceId: String,
): Int {
    if (upcomingIds.isEmpty()) return 0
    val fallbackIndex = upcomingIds.indexOf(fallbackOccurrenceId).takeIf { it >= 0 } ?: 0
    val targetOccurrenceId = upcomingIds.minByOrNull { occurrenceId ->
        rowCentersByOccurrenceId[occurrenceId]?.let { centerY -> kotlin.math.abs(centerY - pointerY) }
            ?: Float.POSITIVE_INFINITY
    }
    val targetIndex = targetOccurrenceId
        ?.takeIf(rowCentersByOccurrenceId::containsKey)
        ?.let(upcomingIds::indexOf)
        ?.takeIf { it >= 0 }
        ?: fallbackIndex
    return targetIndex.coerceIn(upcomingIds.indices)
}

data class PlaylistMoveAvailability(val canMoveUp: Boolean, val canMoveDown: Boolean)

enum class PlaylistDetailRowMode { Default, Edit }
enum class PlaylistDetailRowAction { MoveUp, MoveDown, Remove }

fun playlistDetailRowActions(
    mode: PlaylistDetailRowMode,
    availability: PlaylistMoveAvailability,
): Set<PlaylistDetailRowAction> = if (mode == PlaylistDetailRowMode.Default) emptySet() else buildSet {
    if (availability.canMoveUp) add(PlaylistDetailRowAction.MoveUp)
    if (availability.canMoveDown) add(PlaylistDetailRowAction.MoveDown)
    add(PlaylistDetailRowAction.Remove)
}

fun playlistMoveAvailability(ids: List<String>, entryId: String): PlaylistMoveAvailability {
    val index = ids.indexOf(entryId)
    return PlaylistMoveAvailability(index > 0, index >= 0 && index < ids.lastIndex)
}

fun movedPlaylistEntryIds(ids: List<String>, entryId: String, offset: Int): List<String> {
    val from = ids.indexOf(entryId)
    val to = from + offset
    if (from < 0 || to !in ids.indices || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

data class SavedPlaylistPlaybackRequest(
    val occurrences: List<QueueOccurrence>,
    val selectedOccurrenceId: String,
)

fun savedPlaylistPlaybackRequest(
    visibleEntries: List<PlaylistEntry>,
    tracksById: Map<String, PlayableTrack>,
    selectedEntryId: String,
): SavedPlaylistPlaybackRequest? {
    val occurrences = savedPlaylistOccurrences(visibleEntries, tracksById)
    if (occurrences.none { it.id == selectedEntryId }) return null
    return SavedPlaylistPlaybackRequest(occurrences, selectedEntryId)
}

data class PlaylistAppendRequest(val playlistId: String, val trackIds: List<String>) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }
}

data class PlaylistInlineCreateRequest(val name: String, val trackIds: List<String>) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }
}
data class PlaylistInlineMutationPlan(val name: String, val trackIds: List<String>)

fun PlaylistInlineCreateRequest.mutationPlan(): PlaylistInlineMutationPlan =
    PlaylistInlineMutationPlan(name, trackIds)

fun openAddToPlaylistPickerAction(trackId: String): PlaylistStateAction =
    PlaylistStateAction.OpenPicker(PlaylistPickerState(trackIds = listOf(trackId)))

fun openAddToPlaylistPickerAction(trackIds: List<String>): PlaylistStateAction =
    PlaylistStateAction.OpenPicker(PlaylistPickerState(trackIds = trackIds))

fun filteredPlaylistTrackIds(tracks: List<LibraryTrack>, query: String): List<String> = tracks
    .filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any {
            it.contains(query, ignoreCase = true)
        }
    }
    .map(LibraryTrack::id)

data class AddToPlaylistPickerState(
    val trackIds: List<String>,
    val selectedPlaylistId: String? = null,
    val enteredName: String = "",
) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }

    fun confirmedAppend(): PlaylistAppendRequest? = selectedPlaylistId?.let { PlaylistAppendRequest(it, trackIds) }
    fun confirmedInlineCreate(): PlaylistInlineCreateRequest? = enteredName.trim().takeIf(String::isNotEmpty)?.let {
        PlaylistInlineCreateRequest(it, trackIds)
    }
}

data class PlaylistTrackBrowserState(
    val playlistId: String,
    val query: String = "",
    val visibleTrackIds: List<String> = emptyList(),
    val selectedTrackIds: Set<String> = emptySet(),
) {
    fun toggle(trackId: String): PlaylistTrackBrowserState = copy(
        selectedTrackIds = if (trackId in selectedTrackIds) selectedTrackIds - trackId else selectedTrackIds + trackId,
    )

    fun confirmedTrackIds(): List<String> = visibleTrackIds.filter(selectedTrackIds::contains)
    fun confirmedAppend(): PlaylistAppendRequest? {
        val trackIds = confirmedTrackIds()
        return trackIds.takeIf(List<String>::isNotEmpty)?.let { PlaylistAppendRequest(playlistId, it) }
    }
}

data class PlaylistDetailRow(val entry: PlaylistEntry, val track: PlayableTrack)
data class PlaylistDetailModel(val playlistId: String, val playlistName: String, val rows: List<PlaylistDetailRow>) {
    fun withoutEntry(entryId: String): PlaylistDetailModel = copy(rows = rows.filterNot { it.entry.id == entryId })
}

fun playlistDetailModel(
    playlistId: String,
    playlistName: String,
    entries: List<PlaylistEntry>,
    tracksById: Map<String, PlayableTrack>,
): PlaylistDetailModel = PlaylistDetailModel(
    playlistId,
    playlistName,
    entries.mapNotNull { entry -> tracksById[entry.trackId]?.let { PlaylistDetailRow(entry, it) } },
)

enum class QueueRowRole { Current, Upcoming }
enum class QueueRowState { Current, Upcoming }
enum class QueueRowAction { Drag, MoveUp, MoveDown, Remove }
enum class QueueActionPlacement { None, Inline, SecondaryRow }

data class QueueRowLayoutPolicy(
    val actionPlacement: QueueActionPlacement,
    val reservesMetadataWidth: Boolean = true,
    val minimumInteractiveTarget: androidx.compose.ui.unit.Dp = 44.dp,
)

fun queueRowLayoutPolicy(
    availableWidth: androidx.compose.ui.unit.Dp,
    isEditable: Boolean,
): QueueRowLayoutPolicy = QueueRowLayoutPolicy(
    actionPlacement = when {
        !isEditable -> QueueActionPlacement.None
        availableWidth < 520.dp -> QueueActionPlacement.SecondaryRow
        else -> QueueActionPlacement.Inline
    },
)

data class QueueRowPresentation(
    val occurrence: QueueOccurrence,
    val role: QueueRowRole,
    val canDrag: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val canRemove: Boolean,
    val semanticRole: Role? = null,
    val semanticState: QueueRowState = if (role == QueueRowRole.Current) QueueRowState.Current else QueueRowState.Upcoming,
    val actionTrackTitle: String? = occurrence.track.title.takeIf { role == QueueRowRole.Upcoming },
) {
    val availableActions: Set<QueueRowAction> = buildSet {
        if (canDrag) add(QueueRowAction.Drag)
        if (canMoveUp) add(QueueRowAction.MoveUp)
        if (canMoveDown) add(QueueRowAction.MoveDown)
        if (canRemove) add(QueueRowAction.Remove)
    }
}

data class QueueTabPresentation(
    val rows: List<QueueRowPresentation>,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
    val upcomingOccurrenceIds: List<String> get() = rows.filter { it.role == QueueRowRole.Upcoming }.map { it.occurrence.id }

    fun movedUpcomingIds(occurrenceId: String, offset: Int): List<String> =
        movedPlaylistEntryIds(upcomingOccurrenceIds, occurrenceId, offset)
}

fun queueTabPresentation(state: PlaybackState): QueueTabPresentation {
    val currentIndex = state.queue.indexOfFirst { it.id == state.currentOccurrenceId }
    if (currentIndex < 0) return QueueTabPresentation(emptyList())
    val current = state.queue[currentIndex]
    val upcoming = state.queue.drop(currentIndex + 1)
    return QueueTabPresentation(
        rows = buildList {
            add(
                QueueRowPresentation(
                    occurrence = current,
                    role = QueueRowRole.Current,
                    canDrag = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    canRemove = false,
                ),
            )
            upcoming.forEachIndexed { index, occurrence ->
                add(
                    QueueRowPresentation(
                        occurrence = occurrence,
                        role = QueueRowRole.Upcoming,
                        canDrag = true,
                        canMoveUp = index > 0,
                        canMoveDown = index < upcoming.lastIndex,
                        canRemove = true,
                    ),
                )
            }
        },
    )
}

data class QueueMutationFeedback(
    val refreshedState: PlaybackState,
    val showQueueChanged: Boolean,
)

suspend fun executeQueueMutation(
    state: StateFlow<PlaybackState>,
    command: suspend () -> QueueMutationResult,
): QueueMutationFeedback {
    val result = command()
    return QueueMutationFeedback(
        refreshedState = state.value,
        showQueueChanged = result is QueueMutationResult.Rejected,
    )
}

data class QueueClearConfirmationPresentation(val shouldDispatchClear: Boolean = false) {
    fun confirm() = copy(shouldDispatchClear = true)
    fun dismiss() = copy(shouldDispatchClear = false)
}

fun queueClearConfirmationPresentation() = QueueClearConfirmationPresentation()

@Composable
internal fun PlaylistHubScreen(
    state: PlaylistState,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSelectTab: (PlaylistTab) -> Unit,
    onCreate: (String, (PlaylistStateAction) -> Unit) -> Unit,
    onRetry: () -> Unit,
    onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback,
    onRemoveUpcoming: suspend (String) -> QueueMutationFeedback,
    onClearUpcoming: suspend () -> QueueMutationFeedback,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    var createDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var createOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    val routePresentation = playlistRoutePresentation(state)
    PlaylistScreenFrame(title = stringResource(Res.string.playlists), onBack = onBack) {
        item(key = "tabs") { PlaylistTabs(state.selectedTab, onSelectTab) }
        if (state.isLoading && !state.hasConfirmedSnapshot) {
            item(key = "loading") { EmptyPlaylistMessage(stringResource(Res.string.playlist_loading)) }
        } else if (state.readErrorMessage != null && !state.hasConfirmedSnapshot) {
            item(key = "read-error") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyPlaylistMessage(stringResource(Res.string.playlist_load_failed))
                    CompactAction(stringResource(Res.string.playlist_retry), Modifier.fillMaxWidth(), onRetry)
                }
            }
        } else if (state.selectedTab == PlaylistTab.Saved) {
            item(key = "create") {
                Button(
                    onClick = { createDraft = PlaylistNameDraft() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    cornerRadius = 16.dp,
                    colors = ButtonDefaults.buttonColors(HausColors.current.ink, HausColors.current.paper),
                ) { Text(stringResource(Res.string.playlist_create), fontWeight = FontWeight.Black) }
            }
            if (state.confirmedSnapshot.playlists.isEmpty()) {
                item(key = "empty") { EmptyPlaylistMessage(stringResource(Res.string.playlist_empty_saved)) }
            } else {
                items(state.confirmedSnapshot.playlists, key = Playlist::id) { playlist ->
                    PlaylistHubRow(
                        playlist = playlist,
                        entryCount = state.confirmedSnapshot.entries(playlist.id).size,
                        onClick = { onOpenPlaylist(playlist.id) },
                    )
                }
            }
        } else {
            queueTabItems(
                playbackState = playbackState,
                onReorderUpcoming = onReorderUpcoming,
                onRemoveUpcoming = onRemoveUpcoming,
                onClearUpcoming = onClearUpcoming,
            )
        }
        if (routePresentation.notice == PlaylistRoutePresentationNotice.ReadFailed && state.hasConfirmedSnapshot) {
            item(key = "retained-read-error") {
                ReadFailureNotice(onRetry)
            }
        }
        item(key = "notice") { PlaylistNotice(state) }
        item(key = "spacer") { Spacer(Modifier.height(bottomContentPadding)) }
    }
    createDraft?.let { draft ->
        val modalPresentation = playlistNameModalPresentation(draft, createOutcome)
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_create),
            draft = draft,
            notice = modalPresentation.notice,
            onDraftChange = { createDraft = PlaylistNameDraft(it); createOutcome = null },
            onDismiss = { createDraft = null; createOutcome = null },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) createDraft = draft.mutationFailed() else {
                    onCreate(name) { outcome ->
                        createOutcome = outcome
                        if (playlistMutationDecision(PlaylistMutationWorkflow.Create, outcome) == PlaylistMutationDecision.CloseModal) {
                            createDraft = null
                        }
                    }
                }
            },
        )
    }
}

private fun LazyListScope.queueTabItems(
    playbackState: PlaybackState,
    onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback,
    onRemoveUpcoming: suspend (String) -> QueueMutationFeedback,
    onClearUpcoming: suspend () -> QueueMutationFeedback,
) {
    item(key = "queue-content") {
        QueueTabScreen(
            playbackState = playbackState,
            onReorderUpcoming = onReorderUpcoming,
            onRemoveUpcoming = onRemoveUpcoming,
            onClearUpcoming = onClearUpcoming,
        )
    }
}

@Composable
internal fun QueueTabScreen(
    playbackState: PlaybackState,
    onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback,
    onRemoveUpcoming: suspend (String) -> QueueMutationFeedback,
    onClearUpcoming: suspend () -> QueueMutationFeedback,
) {
    var confirmedState by remember { mutableStateOf(playbackState) }
    var showQueueChanged by remember { mutableStateOf(false) }
    var clearConfirmation by remember { mutableStateOf<QueueClearConfirmationPresentation?>(null) }
    val scope = rememberCoroutineScope()
    val rowCenters = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(playbackState) { confirmedState = playbackState }
    val presentation = queueTabPresentation(confirmedState)
    LaunchedEffect(presentation.upcomingOccurrenceIds) {
        rowCenters.clear()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (presentation.isEmpty) {
            EmptyPlaylistMessage(stringResource(Res.string.playlist_empty_queue))
        } else {
            QueueSectionLabel(stringResource(Res.string.queue_current))
            QueueOccurrenceRow(row = presentation.rows.first())
            val upcomingRows = presentation.rows.drop(1)
            if (upcomingRows.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QueueSectionLabel(stringResource(Res.string.queue_upcoming), Modifier.weight(1f))
                    CompactAction(stringResource(Res.string.queue_clear_upcoming), Modifier) {
                        clearConfirmation = queueClearConfirmationPresentation()
                    }
                }
                upcomingRows.forEachIndexed { index, row ->
                    key(row.occurrence.id) {
                        QueueOccurrenceRow(
                            row = row,
                            upcomingIndex = index,
                            upcomingIds = presentation.upcomingOccurrenceIds,
                            rowCenters = rowCenters,
                            onMove = { offset ->
                                scope.launch {
                                    val feedback = onReorderUpcoming(row.occurrence.id, index + offset)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                            onDragTarget = { targetIndex ->
                                scope.launch {
                                    val feedback = onReorderUpcoming(row.occurrence.id, targetIndex)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    val feedback = onRemoveUpcoming(row.occurrence.id)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                        )
                    }
                }
            }
        }
        if (showQueueChanged) Text(stringResource(Res.string.queue_changed), color = HausColors.current.pulse, fontSize = 13.sp)
    }

    clearConfirmation?.let { confirmation ->
        ConfirmationDialog(
            title = stringResource(Res.string.queue_clear_confirm),
            message = stringResource(Res.string.queue_clear_confirmation),
            onDismiss = { clearConfirmation = confirmation.dismiss(); clearConfirmation = null },
            onConfirm = {
                clearConfirmation = confirmation.confirm()
                if (clearConfirmation?.shouldDispatchClear == true) {
                    scope.launch {
                        val feedback = onClearUpcoming()
                        confirmedState = feedback.refreshedState
                        showQueueChanged = feedback.showQueueChanged
                    }
                }
                clearConfirmation = null
            },
        )
    }
}

@Composable
private fun QueueSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 4.dp),
        color = HausColors.current.ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun QueueOccurrenceRow(
    row: QueueRowPresentation,
    upcomingIndex: Int = -1,
    upcomingIds: List<String> = emptyList(),
    rowCenters: MutableMap<String, Float> = mutableMapOf(),
    onMove: (Int) -> Unit = {},
    onDragTarget: (Int) -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val isCurrent = row.role == QueueRowRole.Current
    val rowState = stringResource(
        when (row.semanticState) {
            QueueRowState.Current -> Res.string.queue_current_state
            QueueRowState.Upcoming -> Res.string.queue_upcoming_state
        },
    )
    val actionTrackTitle = row.actionTrackTitle ?: row.occurrence.track.title
    val moveUp = stringResource(Res.string.queue_move_up_format, actionTrackTitle)
    val moveDown = stringResource(Res.string.queue_move_down_format, actionTrackTitle)
    val drag = stringResource(Res.string.queue_drag_format, actionTrackTitle)
    val remove = stringResource(Res.string.queue_remove_format, actionTrackTitle)
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (upcomingIndex >= 0) {
                    Modifier.onGloballyPositioned { coordinates ->
                        rowCenters[row.occurrence.id] = coordinates.positionInRoot().y + coordinates.size.height / 2f
                    }
                } else {
                    Modifier
                },
            )
            .border(1.dp, if (isCurrent) HausColors.current.pulse else HausColors.current.line, shape)
            .background(if (isCurrent) HausColors.current.panelStrong else HausColors.current.panel.copy(alpha = .54f), shape)
            .semantics {
                row.semanticRole?.let { role = it }
                contentDescription = row.occurrence.track.title
                stateDescription = rowState
            }
            .padding(12.dp),
    ) {
        val layoutPolicy = queueRowLayoutPolicy(maxWidth, row.availableActions.isNotEmpty())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (QueueRowAction.Drag in row.availableActions) {
                    QueueDragHandle(
                        row = row,
                        upcomingIndex = upcomingIndex,
                        upcomingIds = upcomingIds,
                        rowCenters = rowCenters,
                        targetSize = layoutPolicy.minimumInteractiveTarget,
                        contentDescription = drag,
                        onDragTarget = onDragTarget,
                    )
                }
                QueueTrackMetadata(row.occurrence)
                if (layoutPolicy.actionPlacement == QueueActionPlacement.Inline) {
                    QueueMutationActions(
                        row = row,
                        targetSize = layoutPolicy.minimumInteractiveTarget,
                        moveUpDescription = moveUp,
                        moveDownDescription = moveDown,
                        removeDescription = remove,
                        onMove = onMove,
                        onRemove = onRemove,
                    )
                }
            }
            if (layoutPolicy.actionPlacement == QueueActionPlacement.SecondaryRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    QueueMutationActions(
                        row = row,
                        targetSize = layoutPolicy.minimumInteractiveTarget,
                        moveUpDescription = moveUp,
                        moveDownDescription = moveDown,
                        removeDescription = remove,
                        onMove = onMove,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueDragHandle(
    row: QueueRowPresentation,
    upcomingIndex: Int,
    upcomingIds: List<String>,
    rowCenters: MutableMap<String, Float>,
    targetSize: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    onDragTarget: (Int) -> Unit,
) {
    Text(
        "≡",
        modifier = Modifier
            .size(targetSize)
            .pointerInput(row.occurrence.id, upcomingIds, rowCenters.toMap()) {
                var pointerY = rowCenters[row.occurrence.id] ?: 0f
                var targetIndex = upcomingIndex
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        pointerY = rowCenters[row.occurrence.id] ?: 0f
                        targetIndex = upcomingIndex
                    },
                    onDragEnd = {
                        if (targetIndex != upcomingIndex) onDragTarget(targetIndex)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        pointerY += amount.y
                        targetIndex = queueDragTargetIndex(
                            pointerY = pointerY,
                            rowCentersByOccurrenceId = rowCenters,
                            upcomingIds = upcomingIds,
                            fallbackOccurrenceId = row.occurrence.id,
                        )
                    },
                )
            }
            .semantics { this.contentDescription = contentDescription },
        color = HausColors.current.muted,
        fontSize = 24.sp,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.QueueTrackMetadata(occurrence: QueueOccurrence) {
    LazyTrackArtworkImage(
        trackId = occurrence.track.id,
        eagerArtworkBytes = occurrence.track.artworkBytes,
        contentDescription = occurrence.track.title,
        role = ArtworkImageRole.Thumbnail,
        modifier = Modifier.size(48.dp).background(HausColors.current.panelStrong, RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(occurrence.track.title.firstOrNull()?.uppercase() ?: "♪", color = HausColors.current.ink, fontWeight = FontWeight.Black)
        }
    }
    Column(Modifier.weight(1f)) {
        Text(occurrence.track.title, color = HausColors.current.ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(occurrence.track.artist, color = HausColors.current.muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun QueueMutationActions(
    row: QueueRowPresentation,
    targetSize: androidx.compose.ui.unit.Dp,
    moveUpDescription: String,
    moveDownDescription: String,
    removeDescription: String,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    if (QueueRowAction.Remove !in row.availableActions) return
    IconButton(onClick = { onMove(-1) }, enabled = QueueRowAction.MoveUp in row.availableActions, minWidth = targetSize, minHeight = targetSize, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveUpDescription }) { Text("↑", color = HausColors.current.ink) }
    IconButton(onClick = { onMove(1) }, enabled = QueueRowAction.MoveDown in row.availableActions, minWidth = targetSize, minHeight = targetSize, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveDownDescription }) { Text("↓", color = HausColors.current.ink) }
    IconButton(onClick = onRemove, minWidth = targetSize, minHeight = targetSize, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = removeDescription }) { Text("×", color = HausColors.current.pulse) }
}

@Composable
internal fun PlaylistDetailScreen(
    playlist: Playlist,
    entries: List<PlaylistEntry>,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRename: (String, (PlaylistStateAction) -> Unit) -> Unit,
    onDelete: ((PlaylistStateAction) -> Unit) -> Unit,
    onDeleteCompleted: () -> Unit = {},
    onOpenBrowser: () -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    rowMode: PlaylistDetailRowMode = PlaylistDetailRowMode.Default,
    registerPlaylistEditMode: (Any, () -> Unit) -> () -> Unit = { _, _ -> {} },
    registerPlaylistModalDismiss: (Any, (() -> Unit)?) -> () -> Unit = { _, _ -> {} },
) {
    val tracksById = remember(libraryTracks) { libraryTracks.associate { it.id to it.toPlayableTrack() } }
    val model = playlistDetailModel(playlist.id, playlist.name, entries, tracksById)
    var renameDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var renameOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var deleteOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var removeConfirmation by remember { mutableStateOf<PlaylistDetailRow?>(null) }
    var destructivePresentation by remember { mutableStateOf<PlaylistDestructivePresentation?>(null) }
    var editMode by remember { mutableStateOf(rowMode == PlaylistDetailRowMode.Edit) }
    var deleteRoutePending by remember { mutableStateOf(false) }
    val rowCenters = remember { mutableStateMapOf<Int, Float>() }
    val routePresentation = playlistRoutePresentation(state)
    val editOwner = remember(playlist.id) { Any() }
    val modalDismiss: (() -> Unit)? = when {
        renameDraft != null -> ({ renameDraft = null; renameOutcome = null })
        deleteConfirmation -> ({ deleteConfirmation = false; deleteOutcome = null })
        removeConfirmation != null -> ({ destructivePresentation = destructivePresentation?.dismiss(); removeConfirmation = null })
        else -> null
    }
    val currentEditClear = rememberUpdatedState<() -> Unit> { editMode = false }
    DisposableEffect(editMode, editOwner) {
        val unregister = if (editMode) {
            registerPlaylistEditMode(editOwner) { currentEditClear.value() }
        } else null
        onDispose { unregister?.invoke() }
    }
    val modalOwner = remember(playlist.id) { Any() }
    val currentModalDismiss = rememberUpdatedState(modalDismiss)
    DisposableEffect(modalDismiss != null, modalOwner) {
        val unregister = if (modalDismiss != null) {
            registerPlaylistModalDismiss(modalOwner) { currentModalDismiss.value?.invoke() }
        } else null
        onDispose { unregister?.invoke() }
    }
    LaunchedEffect(deleteRoutePending, deleteConfirmation) {
        if (deleteRoutePending && !deleteConfirmation) {
            deleteRoutePending = false
            onDeleteCompleted()
        }
    }
    PlaylistScreenFrame(title = playlist.name, onBack = onBack) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactAction(stringResource(Res.string.playlist_add_tracks), Modifier.weight(1f), onOpenBrowser)
                CompactAction(stringResource(Res.string.playlist_rename), Modifier.weight(1f)) { renameDraft = PlaylistNameDraft(playlist.name) }
                CompactAction(stringResource(Res.string.playlist_delete), Modifier.weight(1f)) {
                    deleteConfirmation = true
                    deleteOutcome = null
                }
            }
        }
        if (model.rows.isEmpty()) item(key = "empty") { EmptyPlaylistMessage(stringResource(Res.string.playlist_empty_detail)) }
        items(model.rows, key = { it.entry.id }) { row ->
            val rowIndex = entries.indexOfFirst { it.id == row.entry.id }
            PlaylistEntryRow(
                row = row,
                rowIndex = rowIndex,
                entryIds = entries.map(PlaylistEntry::id),
                rowCenters = rowCenters,
                availability = playlistMoveAvailability(entries.map(PlaylistEntry::id), row.entry.id),
                mode = if (editMode) PlaylistDetailRowMode.Edit else PlaylistDetailRowMode.Default,
                onClick = {
                    savedPlaylistPlaybackRequest(entries, tracksById, row.entry.id)?.let(onPlayEntry)
                },
                onMove = { offset -> onReorder(movedPlaylistEntryIds(entries.map(PlaylistEntry::id), row.entry.id, offset)) },
                onDragOrder = onReorder,
                onRemove = {
                    removeConfirmation = row
                    destructivePresentation = playlistDestructivePresentation(row.entry.id)
                },
            )
        }
        if (routePresentation.notice == PlaylistRoutePresentationNotice.ReadFailed) {
            item(key = "retained-read-error") { ReadFailureNotice(onRetry) }
        }
        item(key = "notice") { PlaylistNotice(state) }
        item(key = "spacer") { Spacer(Modifier.height(bottomContentPadding)) }
    }
    renameDraft?.let { draft ->
        val modalPresentation = playlistNameModalPresentation(draft, renameOutcome)
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            draft = draft,
            notice = modalPresentation.notice,
            onDraftChange = { renameDraft = PlaylistNameDraft(it); renameOutcome = null },
            onDismiss = { renameDraft = null; renameOutcome = null },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) renameDraft = draft.mutationFailed() else {
                    onRename(name) { outcome ->
                        renameOutcome = outcome
                        if (playlistMutationDecision(PlaylistMutationWorkflow.Rename, outcome) == PlaylistMutationDecision.CloseModal) {
                            renameDraft = null
                        }
                    }
                }
            },
        )
    }
    if (deleteConfirmation) ConfirmationDialog(
        title = stringResource(Res.string.playlist_delete),
        message = stringResource(Res.string.playlist_delete_confirmation_format, playlist.name),
        notice = if (deleteOutcome is PlaylistStateAction.MutationFailed) PlaylistModalNotice.MutationFailed else null,
        onDismiss = { deleteConfirmation = false; deleteOutcome = null },
        onConfirm = {
            onDelete { outcome ->
                deleteOutcome = outcome
                if (playlistMutationDecision(PlaylistMutationWorkflow.Delete, outcome) == PlaylistMutationDecision.CloseConfirmationAndRoute) {
                    deleteConfirmation = false
                    deleteRoutePending = true
                }
            }
        },
    )
    removeConfirmation?.let { row ->
        ConfirmationDialog(
            title = stringResource(Res.string.playlist_remove_track_format, row.track.title),
            message = stringResource(Res.string.playlist_remove_track_format, row.track.title),
            onDismiss = {
                destructivePresentation = destructivePresentation?.dismiss()
                removeConfirmation = null
            },
            onConfirm = {
                destructivePresentation = destructivePresentation?.confirm()
                destructivePresentation?.confirmedEntryId?.let(onRemoveEntry)
                removeConfirmation = null
            },
        )
    }
}

@Composable
internal fun AddToPlaylistPicker(
    playlists: List<Playlist>,
    state: AddToPlaylistPickerState,
    onStateChange: (AddToPlaylistPickerState) -> Unit,
    onDismiss: () -> Unit,
    onAppend: (PlaylistAppendRequest) -> Unit,
    onInlineCreate: (PlaylistInlineCreateRequest) -> Unit,
    notice: PlaylistModalNotice? = null,
) {
    val title = stringResource(Res.string.playlist_add_to)
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                ModalFailureNotice(notice)
                Text(stringResource(Res.string.playlist_choose_existing), color = HausColors.current.ink, fontWeight = FontWeight.Bold)
                playlists.forEach { playlist ->
                    CompactAction(
                        text = playlist.name,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = playlist.name },
                    ) { onStateChange(state.copy(selectedPlaylistId = playlist.id)) }
                }
                Text(stringResource(Res.string.playlist_create_inline), color = HausColors.current.ink, fontWeight = FontWeight.Bold)
                PlaylistTextField(state.enteredName) { onStateChange(state.copy(enteredName = it)) }
            }
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.selectedPlaylistId != null) {
                    CompactAction(title, Modifier.fillMaxWidth()) {
                        state.confirmedAppend()?.let(onAppend)
                    }
                }
                CompactAction(stringResource(Res.string.playlist_create), Modifier.fillMaxWidth()) {
                    state.confirmedInlineCreate()?.let(onInlineCreate)
                }
            }
        },
    )
}

@Composable
internal fun PlaylistTrackBrowser(
    playlistName: String,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistTrackBrowserState,
    onStateChange: (PlaylistTrackBrowserState) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistAppendRequest) -> Unit,
    notice: PlaylistModalNotice? = null,
) {
    val visibleIds = remember(state.query, libraryTracks) { filteredPlaylistTrackIds(libraryTracks, state.query) }
    val visible = remember(visibleIds, libraryTracks) {
        val byId = libraryTracks.associateBy(LibraryTrack::id)
        visibleIds.mapNotNull(byId::get)
    }
    val visibleState = state.copy(visibleTrackIds = visible.map(LibraryTrack::id))
    val selectedStateDescription = stringResource(Res.string.playlist_selected_state)
    HausDialog(
        title = playlistName,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(playlistName, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                ModalFailureNotice(notice)
                PlaylistTextField(state.query, stringResource(Res.string.playlist_track_browser_search)) {
                    onStateChange(state.copy(query = it))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(visible, key = { it.id }) { track ->
                        val selected = track.id in state.selectedTrackIds
                        Row(
                            modifier = Modifier.fillMaxWidth().background(if (selected) HausColors.current.panelStrong else HausColors.current.panel, RoundedCornerShape(16.dp))
                                .hausClickable { onStateChange(visibleState.toggle(track.id)) }
                                .semantics {
                                    contentDescription = track.title
                                    if (selected) stateDescription = selectedStateDescription
                                }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, color = HausColors.current.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = HausColors.current.muted, fontSize = 12.sp, maxLines = 1)
                            }
                            Text(if (selected) "✓" else "+", color = HausColors.current.pulse, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        actions = {
            CompactAction(stringResource(Res.string.playlist_confirm_add), Modifier.fillMaxWidth()) {
                visibleState.confirmedAppend()?.let(onConfirm)
            }
        },
    )
}

@Composable
private fun PlaylistScreenFrame(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val topPadding = rememberSystemBarTopPadding() + PlaylistScreenLayoutPolicy.additionalTopPadding
    Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PlaylistScreenLayoutPolicy.horizontalPadding)
                .padding(top = topPadding),
            verticalArrangement = Arrangement.spacedBy(PlaylistScreenLayoutPolicy.itemSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, minWidth = 44.dp, minHeight = 44.dp, backgroundColor = Color.Transparent) { Text("‹", fontSize = 30.sp, color = HausColors.current.ink) }
                Text(title, color = HausColors.current.ink, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(PlaylistScreenLayoutPolicy.itemSpacing),
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

@Composable
private fun PlaylistTabs(selected: PlaylistTab, onSelect: (PlaylistTab) -> Unit) {
    val palette = HausColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PlaylistTab.Saved to stringResource(Res.string.playlist_saved_tab), PlaylistTab.Queue to stringResource(Res.string.playlist_queue_tab)).forEach { (tab, label) ->
            val presentation = playlistTabPresentation(tab, palette)
            val isSelected = selected == tab
            Button(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f).height(presentation.compactControlHeight),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(horizontal = 10.dp, vertical = presentation.insideVerticalMargin),
                colors = ButtonDefaults.buttonColors(
                    color = if (isSelected) presentation.selectedContainerColor else presentation.unselectedContainerColor,
                    contentColor = if (isSelected) presentation.selectedContentColor else presentation.unselectedContentColor,
                ),
            ) {
                Text(label, fontWeight = FontWeight.Bold, lineHeight = presentation.lineHeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaylistHubRow(playlist: Playlist, entryCount: Int, onClick: () -> Unit) {
    val label = stringResource(Res.string.playlist_row_accessibility_format, playlist.name, entryCount)
    Card(modifier = Modifier.fillMaxWidth().hausClickable(onClick).semantics { contentDescription = label }, cornerRadius = 20.dp, colors = CardDefaults.defaultColors(HausColors.current.panel)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(playlist.name, color = HausColors.current.ink, fontWeight = FontWeight.Black); Text(entryCount.toString(), color = HausColors.current.muted, fontSize = 12.sp) }
            Text("›", color = HausColors.current.pulse, fontSize = 24.sp)
        }
    }
}

@Composable
private fun PlaylistEntryRow(
    row: PlaylistDetailRow,
    rowIndex: Int,
    entryIds: List<String>,
    rowCenters: MutableMap<Int, Float>,
    availability: PlaylistMoveAvailability,
    mode: PlaylistDetailRowMode,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDragOrder: (List<String>) -> Unit,
    onRemove: () -> Unit,
) {
    val moveUp = stringResource(Res.string.playlist_move_up_format, row.track.title)
    val moveDown = stringResource(Res.string.playlist_move_down_format, row.track.title)
    val drag = stringResource(Res.string.playlist_drag_format, row.track.title)
    val remove = stringResource(Res.string.playlist_remove_track_format, row.track.title)
    val entryState = stringResource(Res.string.playlist_entry_state)
    val duration = formatDuration(((row.track.durationMillis ?: 0L) / 1_000L).toInt())
    val rowDescription = "${row.track.title}, ${row.track.artist}, ${row.track.album}, $duration"
    Row(modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates -> rowCenters[rowIndex] = coordinates.positionInRoot().y + coordinates.size.height / 2f }.border(1.dp, HausColors.current.line, RoundedCornerShape(20.dp)).background(HausColors.current.panel.copy(alpha = .54f), RoundedCornerShape(20.dp)).hausClickable(onClick).semantics { contentDescription = rowDescription }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mode == PlaylistDetailRowMode.Edit) Text(
            "≡",
            modifier = Modifier
                .size(44.dp)
                .pointerInput(row.entry.id, entryIds, rowCenters.toMap()) {
                    var pointerY = rowCenters[rowIndex] ?: 0f
                    var dragPresentation = PlaylistDragPresentation(entryIds, row.entry.id)
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            pointerY = rowCenters[rowIndex] ?: 0f
                            dragPresentation = PlaylistDragPresentation(entryIds, row.entry.id)
                        },
                        onDragEnd = {
                            val finalOrder = dragPresentation.finalOrder()
                            if (finalOrder != entryIds) onDragOrder(finalOrder)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pointerY += amount.y
                            dragPresentation.target(
                                playlistDragTargetIndex(
                                    pointerY = pointerY,
                                    rowCentersByIndex = rowCenters,
                                    fallbackIndex = rowIndex,
                                ),
                            )
                        },
                    )
                }
                .semantics { contentDescription = drag },
            color = HausColors.current.muted,
            fontSize = 24.sp,
        )
        LazyTrackArtworkImage(
            trackId = row.track.id,
            eagerArtworkBytes = row.track.artworkBytes,
            contentDescription = row.track.title,
            role = ArtworkImageRole.Thumbnail,
            modifier = Modifier.size(48.dp).background(HausColors.current.panelStrong, RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(row.track.title.firstOrNull()?.uppercase() ?: "♪", color = HausColors.current.ink, fontWeight = FontWeight.Black)
            }
        }
        Column(Modifier.weight(1f).semantics { stateDescription = entryState }) {
            Text(row.track.title, color = HausColors.current.ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${row.track.artist} · ${row.track.album}", color = HausColors.current.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(duration, color = HausColors.current.muted, fontSize = 12.sp)
        if (mode == PlaylistDetailRowMode.Edit) {
            IconButton(onClick = { onMove(-1) }, enabled = availability.canMoveUp, minWidth = 44.dp, minHeight = 44.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveUp }) { Text("↑", color = HausColors.current.ink) }
            IconButton(onClick = { onMove(1) }, enabled = availability.canMoveDown, minWidth = 44.dp, minHeight = 44.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveDown }) { Text("↓", color = HausColors.current.ink) }
            IconButton(onClick = onRemove, minWidth = 44.dp, minHeight = 44.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = remove }) { Text("×", color = HausColors.current.pulse) }
        }
    }
}

@Composable
private fun CompactAction(text: String, modifier: Modifier, onClick: () -> Unit) {
    val presentation = playlistTabPresentation(PlaylistTab.Saved, HausColors.current)
    Button(
        onClick = onClick,
        modifier = modifier.height(presentation.compactControlHeight).semantics { contentDescription = text },
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = presentation.insideVerticalMargin),
        colors = ButtonDefaults.buttonColors(
            color = presentation.unselectedContainerColor,
            contentColor = presentation.unselectedContentColor,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.semantics { contentDescription = text },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = presentation.lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
@Composable private fun EmptyPlaylistMessage(text: String) { Text(text, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), color = HausColors.current.muted, fontSize = 15.sp) }
@Composable private fun PlaylistNotice(state: PlaylistState) { if (state.mutationErrorMessage != null) Text(stringResource(Res.string.playlist_mutation_failed), color = HausColors.current.pulse, fontSize = 13.sp) }

@Composable
private fun PlaylistNameDialog(title: String, draft: PlaylistNameDraft, notice: PlaylistModalNotice?, onDraftChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                PlaylistTextField(draft.enteredText) { onDraftChange(it) }
                if (draft.showFailure) Text(stringResource(Res.string.playlist_create_name), color = HausColors.current.pulse, fontSize = 12.sp)
                ModalFailureNotice(notice)
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactAction(stringResource(Res.string.cancel), Modifier.weight(1f), onDismiss)
                CompactAction(title, Modifier.weight(1f), onConfirm)
            }
        },
    )
}

@Composable
private fun ModalFailureNotice(notice: PlaylistModalNotice?) {
    if (notice == PlaylistModalNotice.MutationFailed) {
        Text(stringResource(Res.string.playlist_mutation_failed), color = HausColors.current.pulse, fontSize = 13.sp)
    }
}

@Composable
private fun ReadFailureNotice(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.playlist_load_failed), color = HausColors.current.muted, fontSize = 13.sp)
        CompactAction(stringResource(Res.string.playlist_retry), Modifier.fillMaxWidth(), onRetry)
    }
}

@Composable
private fun ConfirmationDialog(title: String, message: String, notice: PlaylistModalNotice? = null, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(message, color = HausColors.current.muted, fontSize = 14.sp)
                ModalFailureNotice(notice)
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactAction(stringResource(Res.string.cancel), Modifier.weight(1f), onDismiss)
                CompactAction(title, Modifier.weight(1f), onConfirm)
            }
        },
    )
}

@Composable
private fun PlaylistTextField(value: String, label: String = stringResource(Res.string.playlist_create_name), onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = label, useLabelAsPlaceholder = true, singleLine = true, insideMargin = DpSize(14.dp, 12.dp), cornerRadius = 12.dp, textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp), cursorBrush = SolidColor(HausColors.current.pulse), colors = TextFieldDefaults.textFieldColors(backgroundColor = HausColors.current.paper, borderColor = HausColors.current.line, labelColor = HausColors.current.muted))
}
