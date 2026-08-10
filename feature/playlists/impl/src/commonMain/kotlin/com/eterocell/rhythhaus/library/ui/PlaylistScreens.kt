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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.hausCombinedClickable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.*
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults

/**
 * Shared-owned route identity supplied to a playlist feature presentation;
 * value equality uses [value].
 */
public data class PlaylistFeatureDestination(val value: String)

/**
 * Stable identity for one visible dismissal presentation; value equality uses
 * [value].
 */
public data class PlaylistDismissalAppearance(val value: String)

/**
 * A foreground feature dismissal target with a Shared route and stable
 * presentation identity.
 */
public sealed interface PlaylistFeatureDismissal {
    /** Shared route receiving this target. */
    public val destination: PlaylistFeatureDestination
    /** Stable representation identity for this target. */
    public val appearance: PlaylistDismissalAppearance

    /**
     * A modal presentation that Shared may dismiss only while this exact
     * appearance is active.
     */
    public data class Modal(
        override val destination: PlaylistFeatureDestination,
        override val appearance: PlaylistDismissalAppearance
    ) : PlaylistFeatureDismissal

    /**
     * An edit presentation that Shared may dismiss only while this exact
     * appearance is active.
     */
    public data class Edit(
        override val destination: PlaylistFeatureDestination,
        override val appearance: PlaylistDismissalAppearance
    ) : PlaylistFeatureDismissal
}

/**
 * Result of asking Shared to dispatch a feature dismissal for the registered
 * target.
 */
public enum class PlaylistFeatureDismissalDispatch {
    Started,
    Rejected
}

/**
 * Registers one current feature dismissal with Shared and returns its
 * exact-registration disposer.
 *
 * Shared remains the navigation authority: the feature publishes a target and
 * handles only a [PlaylistFeatureDismissalDispatch.Started] callback for that
 * same target.
 */
public interface PlaylistFeatureDismissalPublisher {
    /** Publishes [dismissal] and dispatches only the registered identity. */
    public fun publish(
        dismissal: PlaylistFeatureDismissal?,
        dispatch: (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch
    ): () -> Unit
}

/**
 * Allocates stable dismissal identities for one Shared-owned destination
 * lifetime.
 *
 * Shared creates one source per active destination. Tokens are monotonic and
 * checked before `Long.MAX_VALUE` overflow; an appearance remains stable while
 * visible and a re-presentation receives a new identity. The source survives
 * recomposition and temporary overlay absence, and is discarded only when its
 * destination ends.
 */
public class PlaylistFeatureAppearanceSource
internal constructor(
    private val destination: PlaylistFeatureDestination,
) {
    private var nextToken = 0L

    internal fun next(identity: String): PlaylistDismissalAppearance {
        check(nextToken != Long.MAX_VALUE) {
            "Playlist dismissal appearance token overflow"
        }
        return PlaylistDismissalAppearance(
            "$identity-${destination.value}-${nextToken++}")
    }
}

/**
 * Remembers the one appearance source Shared retains for an active
 * [destination].
 *
 * Shared must retain this source for the destination lifetime and pass it to
 * feature route entries and overlays; those composables never allocate fallback
 * sources.
 */
@Composable
public fun rememberPlaylistFeatureAppearanceSource(
    destination: PlaylistFeatureDestination,
): PlaylistFeatureAppearanceSource =
    remember(destination) { PlaylistFeatureAppearanceSource(destination) }

@Composable
internal fun rememberFeatureAppearance(
    identity: String,
    source: PlaylistFeatureAppearanceSource,
): PlaylistDismissalAppearance =
    remember(identity, source) { source.next(identity) }

@Composable
internal fun PublishFeatureDismissal(
    destination: PlaylistFeatureDestination,
    publisher: PlaylistFeatureDismissalPublisher,
    dismissal: PlaylistFeatureDismissal?,
    onStarted: (PlaylistFeatureDismissal) -> Unit
) {
    DisposableEffect(destination, dismissal?.appearance) {
        var disposed = false
        val dispose =
            publisher.publish(dismissal) { target ->
                if (!disposed && target == dismissal) {
                    onStarted(target)
                    PlaylistFeatureDismissalDispatch.Started
                } else PlaylistFeatureDismissalDispatch.Rejected
            }
        onDispose {
            disposed = true
            dispose()
        }
    }
}

internal data class PlaylistNameDraft(
    val enteredText: String = "",
    val showFailure: Boolean = false,
) {
    fun confirmedName(): String? = enteredText.trim().takeIf(String::isNotEmpty)

    fun mutationFailed(): PlaylistNameDraft = copy(showFailure = true)
}

internal enum class PlaylistModalNotice {
    MutationFailed
}

internal enum class PlaylistMutationWorkflow {
    Create,
    Rename,
    Delete,
    PickerAppend,
    PickerInlineCreate,
    BrowserAppend,
    Remove,
    Reorder,
}

internal enum class PlaylistMutationDecision {
    CloseModal,
    RetainModalWithFailure,
    CloseConfirmationAndRoute,
    RetainConfirmationWithFailure,
    KeepRoute,
    ShowRouteFailure,
}

internal fun playlistMutationDecision(
    workflow: PlaylistMutationWorkflow,
    outcome: PlaylistStateAction,
): PlaylistMutationDecision =
    when (workflow) {
        PlaylistMutationWorkflow.Delete ->
            if (outcome is PlaylistStateAction.SnapshotConfirmed) {
                PlaylistMutationDecision.CloseConfirmationAndRoute
            } else {
                PlaylistMutationDecision.RetainConfirmationWithFailure
            }

        PlaylistMutationWorkflow.Remove,
        PlaylistMutationWorkflow.Reorder,
        ->
            if (outcome is PlaylistStateAction.SnapshotConfirmed) {
                PlaylistMutationDecision.KeepRoute
            } else {
                PlaylistMutationDecision.ShowRouteFailure
            }

        else ->
            if (outcome is PlaylistStateAction.SnapshotConfirmed) {
                PlaylistMutationDecision.CloseModal
            } else {
                PlaylistMutationDecision.RetainModalWithFailure
            }
    }

internal data class PlaylistNameModalPresentation(
    val enteredText: String,
    val notice: PlaylistModalNotice? = null,
) {
    val isVisible: Boolean = true
}

internal fun playlistNameModalPresentation(
    draft: PlaylistNameDraft,
    outcome: PlaylistStateAction? = null,
): PlaylistNameModalPresentation =
    PlaylistNameModalPresentation(
        enteredText = draft.enteredText,
        notice =
            if (outcome is PlaylistStateAction.MutationFailed)
                PlaylistModalNotice.MutationFailed
            else null,
    )

internal data class PlaylistPickerPresentation(
    val selectedPlaylistId: String?,
    val enteredName: String,
    val notice: PlaylistModalNotice?,
)

internal fun playlistPickerPresentation(
    state: PlaylistState
): PlaylistPickerPresentation? =
    state.picker?.let {
        PlaylistPickerPresentation(
            selectedPlaylistId = it.selectedPlaylistId,
            enteredName = it.enteredName,
            notice =
                if (state.mutationErrorMessage != null)
                    PlaylistModalNotice.MutationFailed
                else null,
        )
    }

internal data class PlaylistBrowserPresentation(
    val query: String,
    val selectedTrackIds: Set<String>,
    val notice: PlaylistModalNotice?,
)

internal fun playlistBrowserPresentation(
    state: PlaylistState
): PlaylistBrowserPresentation? =
    state.browser?.let {
        PlaylistBrowserPresentation(
            query = it.query,
            selectedTrackIds = it.selectedTrackIds,
            notice =
                if (state.mutationErrorMessage != null)
                    PlaylistModalNotice.MutationFailed
                else null,
        )
    }

internal enum class PlaylistRoutePresentationNotice {
    ReadFailed
}

internal data class PlaylistRoutePresentation(
    val showConfirmedContent: Boolean,
    val notice: PlaylistRoutePresentationNotice?,
    val showRetry: Boolean,
)

internal fun playlistRoutePresentation(
    state: PlaylistState
): PlaylistRoutePresentation =
    PlaylistRoutePresentation(
        showConfirmedContent = state.hasConfirmedSnapshot,
        notice =
            if (state.readErrorMessage != null)
                PlaylistRoutePresentationNotice.ReadFailed
            else null,
        showRetry = state.readErrorMessage != null,
    )

internal data class SearchAddToPlaylistPresentation(
    val trackId: String,
    val trackTitle: String,
    val action: PlaylistStateAction,
)

internal fun searchAddToPlaylistPresentation(
    trackId: String,
    trackTitle: String
) =
    SearchAddToPlaylistPresentation(
        trackId = trackId,
        trackTitle = trackTitle,
        action = openAddToPlaylistPickerAction(trackId),
    )

internal data class PlaylistDestructivePresentation(
    val entryId: String,
    val confirmedEntryId: String? = null,
) {
    fun confirm() = copy(confirmedEntryId = entryId)

    fun dismiss() = copy(confirmedEntryId = null)
}

internal fun playlistDestructivePresentation(entryId: String) =
    PlaylistDestructivePresentation(entryId)

internal class PlaylistDragPresentation(
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
        return entryIds.toMutableList().apply {
            add(targetIndex, removeAt(sourceIndex))
        }
    }
}

internal fun playlistDragTargetIndex(
    pointerY: Float,
    rowCentersByIndex: Map<Int, Float>,
    fallbackIndex: Int,
    rowCount: Int? = null,
): Int =
    (rowCentersByIndex
            .minByOrNull { (_, centerY) -> kotlin.math.abs(centerY - pointerY) }
            ?.key ?: fallbackIndex)
        .let { target ->
            rowCount?.let { target.coerceIn(0, (it - 1).coerceAtLeast(0)) }
                ?: target
        }

internal fun queueDragTargetIndex(
    pointerY: Float,
    rowCentersByOccurrenceId: Map<String, Float>,
    upcomingIds: List<String>,
    fallbackOccurrenceId: String,
): Int {
    if (upcomingIds.isEmpty()) return 0
    val fallbackIndex =
        upcomingIds.indexOf(fallbackOccurrenceId).takeIf { it >= 0 } ?: 0
    val targetOccurrenceId = upcomingIds.minByOrNull { occurrenceId ->
        rowCentersByOccurrenceId[occurrenceId]?.let { centerY ->
            kotlin.math.abs(centerY - pointerY)
        } ?: Float.POSITIVE_INFINITY
    }
    val targetIndex =
        targetOccurrenceId
            ?.takeIf(rowCentersByOccurrenceId::containsKey)
            ?.let(upcomingIds::indexOf)
            ?.takeIf { it >= 0 } ?: fallbackIndex
    return targetIndex.coerceIn(upcomingIds.indices)
}

internal data class PlaylistMoveAvailability(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

internal enum class PlaylistDetailRowMode {
    Default,
    Edit
}

internal enum class PlaylistDetailRowAction {
    MoveUp,
    MoveDown,
    Remove
}

internal fun playlistDetailRowActions(
    mode: PlaylistDetailRowMode,
    availability: PlaylistMoveAvailability,
): Set<PlaylistDetailRowAction> =
    if (mode == PlaylistDetailRowMode.Default) {
        emptySet()
    } else {
        buildSet {
            if (availability.canMoveUp) add(PlaylistDetailRowAction.MoveUp)
            if (availability.canMoveDown) add(PlaylistDetailRowAction.MoveDown)
            add(PlaylistDetailRowAction.Remove)
        }
    }

internal fun playlistMoveAvailability(
    ids: List<String>,
    entryId: String
): PlaylistMoveAvailability {
    val index = ids.indexOf(entryId)
    return PlaylistMoveAvailability(
        index > 0, index >= 0 && index < ids.lastIndex)
}

internal fun movedPlaylistEntryIds(
    ids: List<String>,
    entryId: String,
    offset: Int
): List<String> {
    val from = ids.indexOf(entryId)
    val to = from + offset
    if (from < 0 || to !in ids.indices || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * Immutable playback request from a saved playlist occurrence; value equality
 * covers both fields.
 */
public data class SavedPlaylistPlaybackRequest(
    val occurrences: List<QueueOccurrence>,
    val selectedOccurrenceId: String,
)

internal fun savedPlaylistPlaybackRequest(
    visibleEntries: List<PlaylistEntry>,
    tracksById: Map<String, PlayableTrack>,
    selectedEntryId: String,
): SavedPlaylistPlaybackRequest? {
    val occurrences = savedPlaylistOccurrences(visibleEntries, tracksById)
    if (occurrences.none { it.id == selectedEntryId }) return null
    return SavedPlaylistPlaybackRequest(occurrences, selectedEntryId)
}

internal data class PlaylistAppendRequest(
    val playlistId: String,
    val trackIds: List<String>
) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }
}

internal data class PlaylistInlineCreateRequest(
    val name: String,
    val trackIds: List<String>
) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }
}

internal data class PlaylistInlineMutationPlan(
    val name: String,
    val trackIds: List<String>
)

internal fun PlaylistInlineCreateRequest.mutationPlan():
    PlaylistInlineMutationPlan = PlaylistInlineMutationPlan(name, trackIds)

internal fun openAddToPlaylistPickerAction(
    trackId: String
): PlaylistStateAction =
    PlaylistStateAction.OpenPicker(
        PlaylistPickerState(trackIds = listOf(trackId)))

internal fun openAddToPlaylistPickerAction(
    trackIds: List<String>
): PlaylistStateAction =
    PlaylistStateAction.OpenPicker(PlaylistPickerState(trackIds = trackIds))

internal fun filteredPlaylistTrackIds(
    tracks: List<LibraryTrack>,
    query: String
): List<String> =
    tracks
        .filter { track ->
            query.isBlank() ||
                listOf(track.title, track.artist.orEmpty(), track.album.orEmpty()).any {
                    it.contains(query, ignoreCase = true)
                }
        }
        .map(LibraryTrack::id)

internal data class AddToPlaylistPickerState(
    val trackIds: List<String>,
    val selectedPlaylistId: String? = null,
    val enteredName: String = "",
) {
    init {
        require(trackIds.isNotEmpty() && trackIds.all(String::isNotBlank))
    }

    fun confirmedAppend(): PlaylistAppendRequest? = selectedPlaylistId?.let {
        PlaylistAppendRequest(it, trackIds)
    }

    fun confirmedInlineCreate(): PlaylistInlineCreateRequest? =
        enteredName.trim().takeIf(String::isNotEmpty)?.let {
            PlaylistInlineCreateRequest(it, trackIds)
        }
}

internal data class PlaylistTrackBrowserState(
    val playlistId: String,
    val query: String = "",
    val visibleTrackIds: List<String> = emptyList(),
    val selectedTrackIds: Set<String> = emptySet(),
) {
    fun toggle(trackId: String): PlaylistTrackBrowserState =
        copy(
            selectedTrackIds =
                if (trackId in selectedTrackIds) selectedTrackIds - trackId
                else selectedTrackIds + trackId,
        )

    fun confirmedTrackIds(): List<String> =
        visibleTrackIds.filter(selectedTrackIds::contains)

    fun confirmedAppend(): PlaylistAppendRequest? {
        val trackIds = confirmedTrackIds()
        return trackIds.takeIf(List<String>::isNotEmpty)?.let {
            PlaylistAppendRequest(playlistId, it)
        }
    }
}

internal data class PlaylistDetailRow(
    val entry: PlaylistEntry,
    val track: PlayableTrack
)

internal data class PlaylistDetailModel(
    val playlistId: String,
    val playlistName: String,
    val rows: List<PlaylistDetailRow>
) {
    fun withoutEntry(entryId: String): PlaylistDetailModel =
        copy(rows = rows.filterNot { it.entry.id == entryId })
}

internal fun playlistDetailModel(
    playlistId: String,
    playlistName: String,
    entries: List<PlaylistEntry>,
    tracksById: Map<String, PlayableTrack>,
): PlaylistDetailModel =
    PlaylistDetailModel(
        playlistId,
        playlistName,
        entries.mapNotNull { entry ->
            tracksById[entry.trackId]?.let { PlaylistDetailRow(entry, it) }
        },
    )

internal enum class PlaylistDetailActionPlacement {
    Inline,
    SecondaryRow
}

internal data class PlaylistDetailRowLayoutPolicy(
    val actionPlacement: PlaylistDetailActionPlacement,
    val minimumInteractiveTarget: androidx.compose.ui.unit.Dp = 44.dp,
)

internal fun playlistDetailRowLayoutPolicy(
    availableWidth: androidx.compose.ui.unit.Dp,
    isEditable: Boolean,
): PlaylistDetailRowLayoutPolicy =
    PlaylistDetailRowLayoutPolicy(
        actionPlacement =
            if (isEditable && availableWidth < 520.dp) {
                PlaylistDetailActionPlacement.SecondaryRow
            } else {
                PlaylistDetailActionPlacement.Inline
            },
    )

internal enum class QueueRowRole {
    Current,
    Upcoming
}

internal enum class QueueRowState {
    Current,
    Upcoming
}

internal enum class QueueRowAction {
    Drag,
    MoveUp,
    MoveDown,
    Remove
}

internal enum class QueueActionPlacement {
    None,
    Inline,
    SecondaryRow
}

internal data class QueueRowLayoutPolicy(
    val actionPlacement: QueueActionPlacement,
    val reservesMetadataWidth: Boolean = true,
    val minimumInteractiveTarget: androidx.compose.ui.unit.Dp = 44.dp,
)

internal fun queueRowLayoutPolicy(
    availableWidth: androidx.compose.ui.unit.Dp,
    isEditable: Boolean,
): QueueRowLayoutPolicy =
    QueueRowLayoutPolicy(
        actionPlacement =
            when {
                !isEditable -> QueueActionPlacement.None
                availableWidth < 520.dp -> QueueActionPlacement.SecondaryRow
                else -> QueueActionPlacement.Inline
            },
    )

internal data class QueueRowPresentation(
    val occurrence: QueueOccurrence,
    val role: QueueRowRole,
    val canDrag: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val canRemove: Boolean,
    val semanticRole: Role? = null,
    val semanticState: QueueRowState =
        if (role == QueueRowRole.Current) QueueRowState.Current
        else QueueRowState.Upcoming,
    val actionTrackTitle: String? =
        occurrence.track.title.takeIf { role == QueueRowRole.Upcoming },
) {
    val availableActions: Set<QueueRowAction> = buildSet {
        if (canDrag) add(QueueRowAction.Drag)
        if (canMoveUp) add(QueueRowAction.MoveUp)
        if (canMoveDown) add(QueueRowAction.MoveDown)
        if (canRemove) add(QueueRowAction.Remove)
    }
}

internal data class QueueTabPresentation(
    val rows: List<QueueRowPresentation>,
) {
    val isEmpty: Boolean
        get() = rows.isEmpty()

    val upcomingOccurrenceIds: List<String>
        get() =
            rows
                .filter { it.role == QueueRowRole.Upcoming }
                .map { it.occurrence.id }

    fun movedUpcomingIds(occurrenceId: String, offset: Int): List<String> =
        movedPlaylistEntryIds(upcomingOccurrenceIds, occurrenceId, offset)
}

internal fun queueTabPresentation(state: PlaybackState): QueueTabPresentation {
    val currentIndex =
        state.queue.indexOfFirst { it.id == state.currentOccurrenceId }
    if (currentIndex < 0) return QueueTabPresentation(emptyList())
    val current = state.queue[currentIndex]
    val upcoming = state.queue.drop(currentIndex + 1)
    return QueueTabPresentation(
        rows =
            buildList {
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

/**
 * Immutable queue mutation outcome; value equality covers the refreshed
 * playback state and notice flag.
 */
public data class QueueMutationFeedback(
    val refreshedState: PlaybackState,
    val showQueueChanged: Boolean,
)

internal suspend fun executeQueueMutation(
    state: StateFlow<PlaybackState>,
    command: suspend () -> QueueMutationResult,
): QueueMutationFeedback {
    val result = command()
    return QueueMutationFeedback(
        refreshedState = state.value,
        showQueueChanged = result is QueueMutationResult.Rejected,
    )
}

internal data class QueueClearConfirmationPresentation(
    val shouldDispatchClear: Boolean = false
) {
    fun confirm() = copy(shouldDispatchClear = true)

    fun dismiss() = copy(shouldDispatchClear = false)
}

internal fun queueClearConfirmationPresentation() =
    QueueClearConfirmationPresentation()

/** Renders the feature-owned playlist hub while Shared controls navigation. */
@Composable
public fun PlaylistHubScreen(
    state: PlaylistState,
    playbackState: PlaybackState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    playlistsLabel: String,
    loadingLabel: String,
    loadFailedLabel: String,
    retryLabel: String,
    mutationFailedLabel: String,
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
    var queueClearConfirmation by remember {
        mutableStateOf<QueueClearConfirmationPresentation?>(null)
    }
    val routePresentation = playlistRoutePresentation(state)
    PlaylistScreenFrame(title = playlistsLabel, onBack = onBack) {
        item(key = "tabs") { PlaylistTabs(state.selectedTab, onSelectTab) }
        if (state.isLoading && !state.hasConfirmedSnapshot) {
            item(key = "loading") {
                EmptyPlaylistMessage(loadingLabel)
            }
        } else if (state.readErrorMessage != null &&
            !state.hasConfirmedSnapshot) {
            item(key = "read-error") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyPlaylistMessage(loadFailedLabel)
                    CompactAction(retryLabel, Modifier.fillMaxWidth(), onRetry)
                }
            }
        } else if (state.selectedTab == PlaylistTab.Saved) {
            item(key = "create") {
                Button(
                    onClick = { createDraft = PlaylistNameDraft() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    cornerRadius = 16.dp,
                    colors =
                        ButtonDefaults.buttonColors(
                            HausColors.current.ink, HausColors.current.paper),
                ) {
                    Text(
                        stringResource(Res.string.playlist_create),
                        fontWeight = FontWeight.Black)
                }
            }
            if (state.confirmedSnapshot.playlists.isEmpty()) {
                item(key = "empty") {
                    EmptyPlaylistMessage(
                        stringResource(Res.string.playlist_empty_saved))
                }
            } else {
                items(
                    state.confirmedSnapshot.playlists,
                    key = PlaylistSummary::id) { playlist ->
                        PlaylistHubRow(
                            playlist = playlist,
                            entryCount =
                                state.confirmedSnapshot
                                    .entries(playlist.id)
                                    .size,
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
                clearConfirmation = queueClearConfirmation,
                onClearConfirmationChange = { queueClearConfirmation = it },
            )
        }
        if (routePresentation.notice ==
            PlaylistRoutePresentationNotice.ReadFailed &&
            state.hasConfirmedSnapshot) {
            item(key = "retained-read-error") {
                ReadFailureNotice(onRetry)
            }
        }
        item(key = "notice") { PlaylistNotice(state, mutationFailedLabel) }
        item(key = "spacer") {
            Spacer(Modifier.height(bottomContentPadding))
        }
    }
    val createAppearance = createDraft?.let {
        rememberFeatureAppearance("create", appearanceSource)
    }
    val queueClearAppearance = queueClearConfirmation?.let {
        rememberFeatureAppearance("queue", appearanceSource)
    }
    PublishFeatureDismissal(
        destination = destination,
        publisher = dismissalPublisher,
        dismissal =
            createDraft?.let {
                PlaylistFeatureDismissal.Modal(
                    destination, checkNotNull(createAppearance))
            }
                ?: queueClearConfirmation?.let {
                    PlaylistFeatureDismissal.Modal(
                        destination, checkNotNull(queueClearAppearance))
                },
    ) { dismissal ->
        when (dismissal.appearance) {
            createAppearance -> {
                createDraft = null
                createOutcome = null
            }
            queueClearAppearance -> queueClearConfirmation = null
            else -> Unit
        }
    }
    createDraft?.let { draft ->
        val modalPresentation =
            playlistNameModalPresentation(draft, createOutcome)
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_create),
            draft = draft,
            notice = modalPresentation.notice,
            onDraftChange = {
                createDraft = PlaylistNameDraft(it)
                createOutcome = null
            },
            onDismiss = {
                createDraft = null
                createOutcome = null
            },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) {
                    createDraft = draft.mutationFailed()
                } else {
                    onCreate(name) { outcome ->
                        createOutcome = outcome
                        if (playlistMutationDecision(
                            PlaylistMutationWorkflow.Create, outcome) ==
                            PlaylistMutationDecision.CloseModal) {
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
    clearConfirmation: QueueClearConfirmationPresentation?,
    onClearConfirmationChange: (QueueClearConfirmationPresentation?) -> Unit,
) {
    item(key = "queue-content") {
        QueueTabScreen(
            playbackState = playbackState,
            onReorderUpcoming = onReorderUpcoming,
            onRemoveUpcoming = onRemoveUpcoming,
            onClearUpcoming = onClearUpcoming,
            clearConfirmation = clearConfirmation,
            onClearConfirmationChange = onClearConfirmationChange,
        )
    }
}

@Composable
internal fun QueueTabScreen(
    playbackState: PlaybackState,
    onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback,
    onRemoveUpcoming: suspend (String) -> QueueMutationFeedback,
    onClearUpcoming: suspend () -> QueueMutationFeedback,
    clearConfirmation: QueueClearConfirmationPresentation?,
    onClearConfirmationChange: (QueueClearConfirmationPresentation?) -> Unit,
) {
    var confirmedState by remember { mutableStateOf(playbackState) }
    var showQueueChanged by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rowCenters = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(playbackState) { confirmedState = playbackState }
    val presentation = queueTabPresentation(confirmedState)
    LaunchedEffect(presentation.upcomingOccurrenceIds) {
        rowCenters.clear()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (presentation.isEmpty) {
            EmptyPlaylistMessage(
                stringResource(Res.string.playlist_empty_queue))
        } else {
            QueueSectionLabel(stringResource(Res.string.queue_current))
            QueueOccurrenceRow(row = presentation.rows.first())
            val upcomingRows = presentation.rows.drop(1)
            if (upcomingRows.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QueueSectionLabel(
                        stringResource(Res.string.queue_upcoming),
                        Modifier.weight(1f))
                    CompactAction(
                        stringResource(Res.string.queue_clear_upcoming),
                        Modifier) {
                            onClearConfirmationChange(
                                queueClearConfirmationPresentation())
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
                                    val feedback =
                                        onReorderUpcoming(
                                            row.occurrence.id, index + offset)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                            onDragTarget = { targetIndex ->
                                scope.launch {
                                    val feedback =
                                        onReorderUpcoming(
                                            row.occurrence.id, targetIndex)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    val feedback =
                                        onRemoveUpcoming(row.occurrence.id)
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                        )
                    }
                }
            }
        }
        if (showQueueChanged)
            Text(
                stringResource(Res.string.queue_changed),
                color = HausColors.current.pulse,
                fontSize = 13.sp)
    }

    clearConfirmation?.let { confirmation ->
        ConfirmationDialog(
            title = stringResource(Res.string.queue_clear_confirm),
            message = stringResource(Res.string.queue_clear_confirmation),
            onDismiss = {
                onClearConfirmationChange(confirmation.dismiss())
                onClearConfirmationChange(null)
            },
            onConfirm = {
                onClearConfirmationChange(confirmation.confirm())
                if (confirmation.confirm().shouldDispatchClear) {
                    scope.launch {
                        val feedback = onClearUpcoming()
                        confirmedState = feedback.refreshedState
                        showQueueChanged = feedback.showQueueChanged
                    }
                }
                onClearConfirmationChange(null)
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
    val rowState =
        stringResource(
            when (row.semanticState) {
                QueueRowState.Current -> Res.string.queue_current_state
                QueueRowState.Upcoming -> Res.string.queue_upcoming_state
            },
        )
    val actionTrackTitle = row.actionTrackTitle ?: row.occurrence.track.title
    val moveUp =
        stringResource(Res.string.queue_move_up_format, actionTrackTitle)
    val moveDown =
        stringResource(Res.string.queue_move_down_format, actionTrackTitle)
    val drag = stringResource(Res.string.queue_drag_format, actionTrackTitle)
    val remove =
        stringResource(Res.string.queue_remove_format, actionTrackTitle)
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
                .then(
                    if (upcomingIndex >= 0) {
                        Modifier.onGloballyPositioned { coordinates ->
                            rowCenters[row.occurrence.id] =
                                coordinates.positionInRoot().y +
                                    coordinates.size.height / 2f
                        }
                    } else {
                        Modifier
                    },
                )
                .border(
                    1.dp,
                    if (isCurrent) HausColors.current.pulse
                    else HausColors.current.line,
                    shape)
                .background(
                    if (isCurrent) HausColors.current.panelStrong
                    else HausColors.current.panel.copy(alpha = .54f),
                    shape)
                .semantics {
                    row.semanticRole?.let { role = it }
                    contentDescription = row.occurrence.track.title
                    stateDescription = rowState
                }
                .padding(12.dp),
    ) {
        val layoutPolicy =
            queueRowLayoutPolicy(maxWidth, row.availableActions.isNotEmpty())
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
                if (layoutPolicy.actionPlacement ==
                    QueueActionPlacement.Inline) {
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
            if (layoutPolicy.actionPlacement ==
                QueueActionPlacement.SecondaryRow) {
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
        modifier =
            Modifier.size(targetSize)
                .pointerInput(
                    row.occurrence.id, upcomingIds, rowCenters.toMap()) {
                        var pointerY = rowCenters[row.occurrence.id] ?: 0f
                        var targetIndex = upcomingIndex
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                pointerY = rowCenters[row.occurrence.id] ?: 0f
                                targetIndex = upcomingIndex
                            },
                            onDragEnd = {
                                if (targetIndex != upcomingIndex)
                                    onDragTarget(targetIndex)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                pointerY += amount.y
                                targetIndex =
                                    queueDragTargetIndex(
                                        pointerY = pointerY,
                                        rowCentersByOccurrenceId = rowCenters,
                                        upcomingIds = upcomingIds,
                                        fallbackOccurrenceId =
                                            row.occurrence.id,
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
private fun androidx.compose.foundation.layout.RowScope.QueueTrackMetadata(
    occurrence: QueueOccurrence
) {
    ArtworkImage(
        artworkBytes = occurrence.track.artworkBytes,
        contentDescription = occurrence.track.title,
        role = ArtworkImageRole.Thumbnail,
        modifier =
            Modifier.size(48.dp)
                .background(
                    HausColors.current.panelStrong, RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                occurrence.track.title.firstOrNull()?.uppercase() ?: "♪",
                color = HausColors.current.ink,
                fontWeight = FontWeight.Black)
        }
    }
    Column(Modifier.weight(1f)) {
        Text(
            occurrence.track.title,
            color = HausColors.current.ink,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Text(
            occurrence.track.artist,
            color = HausColors.current.muted,
            fontSize = 12.sp,
            maxLines = 1)
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
    IconButton(
        onClick = { onMove(-1) },
        enabled = QueueRowAction.MoveUp in row.availableActions,
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics { contentDescription = moveUpDescription }) {
            Text("↑", color = HausColors.current.ink)
        }
    IconButton(
        onClick = { onMove(1) },
        enabled = QueueRowAction.MoveDown in row.availableActions,
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics { contentDescription = moveDownDescription }) {
            Text("↓", color = HausColors.current.ink)
        }
    IconButton(
        onClick = onRemove,
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics { contentDescription = removeDescription }) {
            Text("×", color = HausColors.current.pulse)
        }
}

/** Renders one playlist detail route while Shared controls navigation. */
@Composable
public fun PlaylistDetailScreen(
    playlist: PlaylistSummary,
    entries: List<PlaylistEntry>,
    playableTracksById: Map<String, PlayableTrack>,
    state: PlaylistState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    mutationFailedLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRename: (String, (PlaylistStateAction) -> Unit) -> Unit,
    onDelete: ((PlaylistStateAction) -> Unit) -> Unit,
    onDeleteConfirmed: (PlaylistSnapshot) -> Unit,
    onOpenBrowser: () -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    initialEditMode: Boolean = false,
) {
    val tracksById = playableTracksById
    val model =
        playlistDetailModel(playlist.id, playlist.name, entries, tracksById)
    var renameDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var renameOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var deleteOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var removeConfirmation by remember {
        mutableStateOf<PlaylistDetailRow?>(null)
    }
    var destructivePresentation by remember {
        mutableStateOf<PlaylistDestructivePresentation?>(null)
    }
    var editMode by
        remember(playlist.id) {
            mutableStateOf(initialEditMode)
        }
    val rowCenters = remember { mutableStateMapOf<Int, Float>() }
    val routePresentation = playlistRoutePresentation(state)
    val modalDismiss: (() -> Unit)? =
        when {
            renameDraft != null -> ({
                    renameDraft = null
                    renameOutcome = null
                })

            deleteConfirmation -> ({
                    deleteConfirmation = false
                    deleteOutcome = null
                })

            removeConfirmation != null -> ({
                    destructivePresentation = destructivePresentation?.dismiss()
                    removeConfirmation = null
                })

            else -> null
        }
    val editAppearance =
        if (editMode) rememberFeatureAppearance("edit", appearanceSource)
        else null
    val renameAppearance = renameDraft?.let {
        rememberFeatureAppearance("rename", appearanceSource)
    }
    val deleteAppearance =
        if (deleteConfirmation)
            rememberFeatureAppearance("delete", appearanceSource)
        else null
    val removeAppearance = removeConfirmation?.let {
        rememberFeatureAppearance("remove", appearanceSource)
    }
    val modalAppearance =
        when {
            renameDraft != null -> renameAppearance
            deleteConfirmation -> deleteAppearance
            removeConfirmation != null -> removeAppearance
            else -> null
        }
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        when {
            modalDismiss != null ->
                PlaylistFeatureDismissal.Modal(
                    destination, checkNotNull(modalAppearance))
            editMode ->
                PlaylistFeatureDismissal.Edit(
                    destination, checkNotNull(editAppearance))
            else -> null
        },
    ) { dismissal ->
        when (dismissal.appearance) {
            modalAppearance -> modalDismiss?.invoke()
            editAppearance -> editMode = false
            else -> Unit
        }
    }
    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset) {
            onScrollPositionChanged(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }
    PlaylistScreenFrame(
        title = playlist.name,
        onBack = onBack,
        editMode = editMode,
        onOutsideEditTap = { editMode = false },
        listState = listState,
        beforeList = {
            if (editMode) {
                val exitEditing =
                    stringResource(Res.string.playlist_exit_editing)
                CompactAction(
                    exitEditing, Modifier.fillMaxWidth().height(44.dp)) {
                        editMode = false
                    }
            } else {
                Row(
                    Modifier.fillMaxWidth().height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactAction(
                            stringResource(Res.string.playlist_add_tracks),
                            Modifier.weight(1f),
                            onOpenBrowser)
                        CompactAction(
                            stringResource(Res.string.playlist_rename),
                            Modifier.weight(1f)) {
                                renameDraft = PlaylistNameDraft(playlist.name)
                            }
                        CompactAction(
                            stringResource(Res.string.playlist_delete),
                            Modifier.weight(1f)) {
                                deleteConfirmation = true
                                deleteOutcome = null
                            }
                    }
            }
        },
    ) {
        if (model.rows.isEmpty())
            item(key = "empty") {
                EmptyPlaylistMessage(
                    stringResource(Res.string.playlist_empty_detail))
            }
        items(model.rows, key = { it.entry.id }) { row ->
            val rowIndex = entries.indexOfFirst { it.id == row.entry.id }
            PlaylistEntryRow(
                row = row,
                rowIndex = rowIndex,
                entryIds = entries.map(PlaylistEntry::id),
                rowCenters = rowCenters,
                availability =
                    playlistMoveAvailability(
                        entries.map(PlaylistEntry::id), row.entry.id),
                mode =
                    if (editMode) PlaylistDetailRowMode.Edit
                    else PlaylistDetailRowMode.Default,
                onClick = {
                    if (!editMode)
                        savedPlaylistPlaybackRequest(
                                entries, tracksById, row.entry.id)
                            ?.let(onPlayEntry)
                },
                onLongClick = { editMode = true },
                onMove = { offset ->
                    onReorder(
                        movedPlaylistEntryIds(
                            entries.map(PlaylistEntry::id),
                            row.entry.id,
                            offset))
                },
                onDragOrder = onReorder,
                onRemove = {
                    removeConfirmation = row
                    destructivePresentation =
                        playlistDestructivePresentation(row.entry.id)
                },
            )
        }
        if (routePresentation.notice ==
            PlaylistRoutePresentationNotice.ReadFailed) {
            item(key = "retained-read-error") { ReadFailureNotice(onRetry) }
        }
        item(key = "notice") { PlaylistNotice(state, mutationFailedLabel) }
        item(key = "spacer") {
            Spacer(
                Modifier.height(bottomContentPadding)
                    .testTag("playlist-bottom-clearance"))
        }
    }
    renameDraft?.let { draft ->
        val modalPresentation =
            playlistNameModalPresentation(draft, renameOutcome)
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            draft = draft,
            notice = modalPresentation.notice,
            onDraftChange = {
                renameDraft = PlaylistNameDraft(it)
                renameOutcome = null
            },
            onDismiss = {
                renameDraft = null
                renameOutcome = null
            },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) {
                    renameDraft = draft.mutationFailed()
                } else {
                    onRename(name) { outcome ->
                        renameOutcome = outcome
                        if (playlistMutationDecision(
                            PlaylistMutationWorkflow.Rename, outcome) ==
                            PlaylistMutationDecision.CloseModal) {
                            renameDraft = null
                        }
                    }
                }
            },
        )
    }
    if (deleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.playlist_delete),
            message =
                stringResource(
                    Res.string.playlist_delete_confirmation_format,
                    playlist.name),
            notice =
                if (deleteOutcome is PlaylistStateAction.MutationFailed)
                    PlaylistModalNotice.MutationFailed
                else null,
            onDismiss = {
                deleteConfirmation = false
                deleteOutcome = null
            },
            onConfirm = {
                onDelete { outcome ->
                    deleteOutcome = outcome
                    if (playlistMutationDecision(
                        PlaylistMutationWorkflow.Delete, outcome) ==
                        PlaylistMutationDecision.CloseConfirmationAndRoute) {
                        deleteConfirmation = false
                        deleteOutcome = null
                        onDeleteConfirmed(
                            (outcome as PlaylistStateAction.SnapshotConfirmed)
                                .snapshot)
                    }
                }
            },
        )
    }
    removeConfirmation?.let { row ->
        ConfirmationDialog(
            title =
                stringResource(
                    Res.string.playlist_remove_track_format, row.track.title),
            message =
                stringResource(
                    Res.string.playlist_remove_track_format, row.track.title),
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
    playlists: List<PlaylistSummary>,
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
                Text(
                    title,
                    color = HausColors.current.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black)
                ModalFailureNotice(notice)
                Text(
                    stringResource(Res.string.playlist_choose_existing),
                    color = HausColors.current.ink,
                    fontWeight = FontWeight.Bold)
                playlists.forEach { playlist ->
                    CompactAction(
                        text = playlist.name,
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentDescription = playlist.name
                            },
                    ) {
                        onStateChange(
                            state.copy(selectedPlaylistId = playlist.id))
                    }
                }
                Text(
                    stringResource(Res.string.playlist_create_inline),
                    color = HausColors.current.ink,
                    fontWeight = FontWeight.Bold)
                PlaylistTextField(state.enteredName) {
                    onStateChange(state.copy(enteredName = it))
                }
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
                CompactAction(
                    stringResource(Res.string.playlist_create),
                    Modifier.fillMaxWidth()) {
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
    val visibleIds =
        remember(state.query, libraryTracks) {
            filteredPlaylistTrackIds(libraryTracks, state.query)
        }
    val visible =
        remember(visibleIds, libraryTracks) {
            val byId = libraryTracks.associateBy(LibraryTrack::id)
            visibleIds.mapNotNull(byId::get)
        }
    val visibleState =
        state.copy(visibleTrackIds = visible.map(LibraryTrack::id))
    val selectedStateDescription =
        stringResource(Res.string.playlist_selected_state)
    HausDialog(
        title = playlistName,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    playlistName,
                    color = HausColors.current.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black)
                ModalFailureNotice(notice)
                PlaylistTextField(
                    state.query,
                    stringResource(Res.string.playlist_track_browser_search)) {
                        onStateChange(state.copy(query = it))
                    }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(visible, key = { it.id }) { track ->
                            val selected = track.id in state.selectedTrackIds
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .background(
                                            if (selected)
                                                HausColors.current.panelStrong
                                            else HausColors.current.panel,
                                            RoundedCornerShape(16.dp))
                                        .hausClickable {
                                            onStateChange(
                                                visibleState.toggle(track.id))
                                        }
                                        .semantics {
                                            contentDescription = track.title
                                            if (selected)
                                                stateDescription =
                                                    selectedStateDescription
                                        }
                                        .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        color = HausColors.current.ink,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(
                                        track.artist,
                                        color = HausColors.current.muted,
                                        fontSize = 12.sp,
                                        maxLines = 1)
                                }
                                Text(
                                    if (selected) "✓" else "+",
                                    color = HausColors.current.pulse,
                                    fontSize = 18.sp)
                            }
                        }
                    }
            }
        },
        actions = {
            CompactAction(
                stringResource(Res.string.playlist_confirm_add),
                Modifier.fillMaxWidth()) {
                    visibleState.confirmedAppend()?.let(onConfirm)
                }
        },
    )
}

/**
 * Feature-owned playlist picker route; Shared supplies navigation and mutation
 * callbacks.
 */
@Composable
public fun AddToPlaylistPickerOverlay(
    playlists: List<PlaylistSummary>,
    state: PlaylistPickerState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    onStateChange: (PlaylistPickerState) -> Unit,
    onDismiss: () -> Unit,
    onAppend: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
    onInlineCreate:
        (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
) {
    val appearance = rememberFeatureAppearance("picker", appearanceSource)
    var failureNotice by remember { mutableStateOf<PlaylistModalNotice?>(null) }
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance),
    ) {
        onDismiss()
        PlaylistFeatureDismissalDispatch.Started
    }
    AddToPlaylistPicker(
        playlists = playlists,
        state =
            AddToPlaylistPickerState(
                state.trackIds, state.selectedPlaylistId, state.enteredName),
        onStateChange = { next ->
            onStateChange(
                PlaylistPickerState(
                    next.trackIds, next.selectedPlaylistId, next.enteredName))
        },
        onDismiss = onDismiss,
        onAppend = { request ->
            onAppend(request.playlistId, request.trackIds) { outcome ->
                if (outcome !is PlaylistStateAction.SnapshotConfirmed)
                    failureNotice = PlaylistModalNotice.MutationFailed
            }
        },
        onInlineCreate = { request ->
            onInlineCreate(request.name, request.trackIds) { outcome ->
                if (outcome !is PlaylistStateAction.SnapshotConfirmed)
                    failureNotice = PlaylistModalNotice.MutationFailed
            }
        },
        notice = failureNotice,
    )
}

/**
 * Feature-owned track browser route; Shared supplies navigation and mutation
 * callbacks.
 */
@Composable
public fun PlaylistTrackBrowserOverlay(
    playlistName: String,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistBrowserState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    onStateChange: (PlaylistBrowserState) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
) {
    val appearance = rememberFeatureAppearance("browser", appearanceSource)
    var failureNotice by remember { mutableStateOf<PlaylistModalNotice?>(null) }
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance),
    ) {
        onDismiss()
        PlaylistFeatureDismissalDispatch.Started
    }
    PlaylistTrackBrowser(
        playlistName = playlistName,
        libraryTracks = libraryTracks,
        state =
            PlaylistTrackBrowserState(
                state.playlistId,
                state.query,
                state.visibleTrackIds,
                state.selectedTrackIds),
        onStateChange = { next ->
            onStateChange(
                PlaylistBrowserState(
                    next.playlistId,
                    next.query,
                    next.visibleTrackIds,
                    next.selectedTrackIds))
        },
        onDismiss = onDismiss,
        onConfirm = { request ->
            onConfirm(request.playlistId, request.trackIds) { outcome ->
                if (outcome !is PlaylistStateAction.SnapshotConfirmed)
                    failureNotice = PlaylistModalNotice.MutationFailed
            }
        },
        notice = failureNotice,
    )
}

@Composable
private fun PlaylistScreenFrame(
    title: String,
    onBack: () -> Unit,
    beforeList: (@Composable () -> Unit)? = null,
    editMode: Boolean = false,
    onOutsideEditTap: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val topPadding =
        playlistSystemBarTopPadding() +
            PlaylistScreenLayoutPolicy.additionalTopPadding
    Surface(
        modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(
                            horizontal =
                                PlaylistScreenLayoutPolicy.horizontalPadding)
                        .padding(top = topPadding),
                verticalArrangement =
                    Arrangement.spacedBy(
                        PlaylistScreenLayoutPolicy.itemSpacing),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onBack,
                            minWidth = 44.dp,
                            minHeight = 44.dp,
                            backgroundColor = Color.Transparent,
                            modifier = Modifier.testTag("playlist-back"),
                        ) {
                            Text(
                                "‹",
                                fontSize = 30.sp,
                                color = HausColors.current.ink)
                        }
                        Box(
                            Modifier.weight(1f)
                                .height(44.dp)
                                .testTag("playlist-toolbar-title")
                                .then(
                                    if (editMode) {
                                        Modifier.hausClickable(onOutsideEditTap)
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                title,
                                color = HausColors.current.ink,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                if (beforeList != null) {
                    Box(
                        Modifier.fillMaxWidth()
                            .testTag("playlist-action-header")) {
                            beforeList()
                        }
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            PlaylistScreenLayoutPolicy.itemSpacing),
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .testTag("playlist-list-viewport"),
                    content = content,
                )
            }
        }
}

@Composable
private fun PlaylistTabs(
    selected: PlaylistTab,
    onSelect: (PlaylistTab) -> Unit
) {
    val palette = HausColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
                PlaylistTab.Saved to
                    stringResource(Res.string.playlist_saved_tab),
                PlaylistTab.Queue to
                    stringResource(Res.string.playlist_queue_tab))
            .forEach { (tab, label) ->
                val presentation = playlistTabPresentation(tab, palette)
                val isSelected = selected == tab
                Button(
                    onClick = { onSelect(tab) },
                    modifier =
                        Modifier.weight(1f)
                            .height(presentation.compactControlHeight),
                    cornerRadius = 20.dp,
                    insideMargin =
                        PaddingValues(
                            horizontal = 10.dp,
                            vertical = presentation.insideVerticalMargin),
                    colors =
                        ButtonDefaults.buttonColors(
                            color =
                                if (isSelected)
                                    presentation.selectedContainerColor
                                else presentation.unselectedContainerColor,
                            contentColor =
                                if (isSelected)
                                    presentation.selectedContentColor
                                else presentation.unselectedContentColor,
                        ),
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        lineHeight = presentation.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
    }
}

@Composable
private fun PlaylistHubRow(
    playlist: PlaylistSummary,
    entryCount: Int,
    onClick: () -> Unit
) {
    val label =
        stringResource(
            Res.string.playlist_row_accessibility_format,
            playlist.name,
            entryCount)
    Card(
        modifier =
            Modifier.clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .hausClickable(onClick)
                .semantics { contentDescription = label },
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(HausColors.current.panel)) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.name,
                            color = HausColors.current.ink,
                            fontWeight = FontWeight.Black)
                        Text(
                            entryCount.toString(),
                            color = HausColors.current.muted,
                            fontSize = 12.sp)
                    }
                    Text(
                        "›", color = HausColors.current.pulse, fontSize = 24.sp)
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
    onLongClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDragOrder: (List<String>) -> Unit,
    onRemove: () -> Unit,
) {
    val moveUp =
        stringResource(Res.string.playlist_move_up_format, row.track.title)
    val moveDown =
        stringResource(Res.string.playlist_move_down_format, row.track.title)
    val drag = stringResource(Res.string.playlist_drag_format, row.track.title)
    val remove =
        stringResource(Res.string.playlist_remove_track_format, row.track.title)
    val entryState = stringResource(Res.string.playlist_entry_state)
    val duration =
        playlistFormatDuration(
            ((row.track.durationMillis ?: 0L) / 1_000L).toInt())
    val rowDescription =
        "${row.track.title}, ${row.track.artist}, ${row.track.album}, $duration"
    BoxWithConstraints(
        Modifier.clip(RoundedCornerShape(20.dp))
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                rowCenters[rowIndex] =
                    coordinates.positionInRoot().y +
                        coordinates.size.height / 2f
            }
            .border(1.dp, HausColors.current.line, RoundedCornerShape(20.dp))
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(20.dp))
            .hausCombinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = rowDescription)
            .semantics { contentDescription = rowDescription }
            .padding(12.dp),
    ) {
        val layoutPolicy =
            playlistDetailRowLayoutPolicy(
                maxWidth, mode == PlaylistDetailRowMode.Edit)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == PlaylistDetailRowMode.Edit)
                        PlaylistEntryDragHandle(
                            row,
                            rowIndex,
                            entryIds,
                            rowCenters,
                            drag,
                            onDragOrder)
                    ArtworkImage(
                        artworkBytes = row.track.artworkBytes,
                        contentDescription = row.track.title,
                        role = ArtworkImageRole.Thumbnail,
                        modifier =
                            Modifier.size(48.dp)
                                .background(
                                    HausColors.current.panelStrong,
                                    RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center) {
                                    Text(
                                        row.track.title
                                            .firstOrNull()
                                            ?.uppercase() ?: "♪",
                                        color = HausColors.current.ink,
                                        fontWeight = FontWeight.Black)
                                }
                        }
                    Column(
                        Modifier.weight(1f)
                            .testTag("playlist-entry-metadata-${row.entry.id}")
                            .semantics { stateDescription = entryState }) {
                            Text(
                                row.track.title,
                                color = HausColors.current.ink,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(
                                "${row.track.artist} · ${row.track.album}",
                                color = HausColors.current.muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    Text(
                        duration,
                        color = HausColors.current.muted,
                        fontSize = 12.sp)
                    if (layoutPolicy.actionPlacement ==
                        PlaylistDetailActionPlacement.Inline &&
                        mode == PlaylistDetailRowMode.Edit)
                        PlaylistEntryMutationActions(
                            availability,
                            moveUp,
                            moveDown,
                            remove,
                            onMove,
                            onRemove)
                }
            if (layoutPolicy.actionPlacement ==
                PlaylistDetailActionPlacement.SecondaryRow &&
                mode == PlaylistDetailRowMode.Edit) {
                Row(
                    Modifier.fillMaxWidth()
                        .testTag("playlist-entry-action-rail-${row.entry.id}"),
                    horizontalArrangement = Arrangement.End) {
                        PlaylistEntryMutationActions(
                            availability,
                            moveUp,
                            moveDown,
                            remove,
                            onMove,
                            onRemove)
                    }
            }
        }
    }
}

private fun playlistFormatDuration(totalSeconds: Int): String {
    val safeSeconds = maxOf(0, totalSeconds)
    return "${safeSeconds / 60}:${(safeSeconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun playlistSystemBarTopPadding(): Dp {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val system = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    return maxOf(status.value, system.value).dp
}

@Composable
private fun PlaylistEntryDragHandle(
    row: PlaylistDetailRow,
    rowIndex: Int,
    entryIds: List<String>,
    rowCenters: MutableMap<Int, Float>,
    drag: String,
    onDragOrder: (List<String>) -> Unit
) {
    Text(
        "≡",
        modifier =
            Modifier.size(44.dp)
                .pointerInput(row.entry.id, entryIds, rowCenters.toMap()) {
                    var pointerY = rowCenters[rowIndex] ?: 0f
                    var dragPresentation =
                        PlaylistDragPresentation(entryIds, row.entry.id)
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            pointerY = rowCenters[rowIndex] ?: 0f
                            dragPresentation =
                                PlaylistDragPresentation(entryIds, row.entry.id)
                        },
                        onDragEnd = {
                            dragPresentation
                                .finalOrder()
                                .takeIf { it != entryIds }
                                ?.let(onDragOrder)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            pointerY += amount.y
                            dragPresentation.target(
                                playlistDragTargetIndex(
                                    pointerY, rowCenters, rowIndex))
                        })
                }
                .semantics { contentDescription = drag },
        color = HausColors.current.muted,
        fontSize = 24.sp,
    )
}

@Composable
private fun PlaylistEntryMutationActions(
    availability: PlaylistMoveAvailability,
    moveUp: String,
    moveDown: String,
    remove: String,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit
) {
    IconButton(
        onClick = { onMove(-1) },
        enabled = availability.canMoveUp,
        minWidth = 44.dp,
        minHeight = 44.dp,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics {
                contentDescription = moveUp
                if (!availability.canMoveUp) disabled()
            },
    ) {
        Text("↑", color = HausColors.current.ink)
    }
    IconButton(
        onClick = { onMove(1) },
        enabled = availability.canMoveDown,
        minWidth = 44.dp,
        minHeight = 44.dp,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics {
                contentDescription = moveDown
                if (!availability.canMoveDown) disabled()
            },
    ) {
        Text("↓", color = HausColors.current.ink)
    }
    IconButton(
        onClick = onRemove,
        minWidth = 44.dp,
        minHeight = 44.dp,
        backgroundColor = Color.Transparent,
        modifier = Modifier.semantics { contentDescription = remove }) {
            Text("×", color = HausColors.current.pulse)
        }
}

@Composable
private fun CompactAction(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val presentation =
        playlistTabPresentation(PlaylistTab.Saved, HausColors.current)
    Button(
        onClick = onClick,
        modifier =
            modifier.height(presentation.compactControlHeight).semantics {
                contentDescription = text
            },
        cornerRadius = 14.dp,
        insideMargin =
            PaddingValues(
                horizontal = 10.dp,
                vertical = presentation.insideVerticalMargin),
        colors =
            ButtonDefaults.buttonColors(
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

@Composable
private fun EmptyPlaylistMessage(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        color = HausColors.current.muted,
        fontSize = 15.sp)
}

@Composable
private fun PlaylistNotice(state: PlaylistState, mutationFailedLabel: String) {
    if (state.mutationErrorMessage != null)
        Text(
            mutationFailedLabel,
            color = HausColors.current.pulse,
            fontSize = 13.sp)
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    draft: PlaylistNameDraft,
    notice: PlaylistModalNotice?,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    title,
                    color = HausColors.current.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black)
                PlaylistTextField(draft.enteredText) { onDraftChange(it) }
                if (draft.showFailure)
                    Text(
                        stringResource(Res.string.playlist_create_name),
                        color = HausColors.current.pulse,
                        fontSize = 12.sp)
                ModalFailureNotice(notice)
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactAction(
                    stringResource(Res.string.playlist_cancel),
                    Modifier.weight(1f),
                    onDismiss)
                CompactAction(title, Modifier.weight(1f), onConfirm)
            }
        },
    )
}

@Composable
private fun ModalFailureNotice(notice: PlaylistModalNotice?) {
    if (notice == PlaylistModalNotice.MutationFailed) {
        Text(
            stringResource(Res.string.playlist_modal_mutation_failed),
            color = HausColors.current.pulse,
            fontSize = 13.sp)
    }
}

@Composable
private fun ReadFailureNotice(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.playlist_retained_load_failed),
            color = HausColors.current.muted,
            fontSize = 13.sp)
        CompactAction(
            stringResource(Res.string.playlist_retained_retry),
            Modifier.fillMaxWidth(),
            onRetry)
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    notice: PlaylistModalNotice? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    title,
                    color = HausColors.current.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black)
                Text(
                    message, color = HausColors.current.muted, fontSize = 14.sp)
                ModalFailureNotice(notice)
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactAction(
                    stringResource(Res.string.playlist_cancel),
                    Modifier.weight(1f),
                    onDismiss)
                CompactAction(title, Modifier.weight(1f), onConfirm)
            }
        },
    )
}

@Composable
private fun PlaylistTextField(
    value: String,
    label: String = stringResource(Res.string.playlist_create_name),
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = label,
        useLabelAsPlaceholder = true,
        singleLine = true,
        insideMargin = DpSize(14.dp, 12.dp),
        cornerRadius = 12.dp,
        textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp),
        cursorBrush = SolidColor(HausColors.current.pulse),
        colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = HausColors.current.paper,
                borderColor = HausColors.current.line,
                labelColor = HausColors.current.muted))
}

// Library extraction
