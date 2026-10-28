package com.eterocell.rhythhaus.library.ui

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.HausButton as Button
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.HausIconButton as IconButton
import com.eterocell.rhythhaus.ui.HausSongRow
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.songArtistAlbumSubtitle
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.*
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
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
                listOf(
                        track.title,
                        track.artist.orEmpty(),
                        track.album.orEmpty())
                    .any {
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
    desktopDensity: Boolean,
): PlaylistDetailRowLayoutPolicy =
    PlaylistDetailRowLayoutPolicy(
        actionPlacement =
            if (isEditable && availableWidth < 520.dp) {
                PlaylistDetailActionPlacement.SecondaryRow
            } else {
                PlaylistDetailActionPlacement.Inline
            },
        minimumInteractiveTarget = if (desktopDensity) 44.dp else 48.dp,
    )

/** Renders the feature-owned playlist hub while Shared controls navigation. */
@Composable
public fun PlaylistHubScreen(
    state: PlaylistState,
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
    onCreate: (String, (PlaylistStateAction) -> Unit) -> Unit,
    libraryTracks: List<LibraryTrack>,
    onOpenSmartPlaylist: (String) -> Unit,
    onCreateSmartPlaylist:
        (String, SmartPlaylistRule, (PlaylistStateAction) -> Unit) -> Unit,
    onRetry: () -> Unit,
    documentActions: @Composable () -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    var createDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var createTypeAppearance by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    var smartDraft by remember { mutableStateOf<SmartDraft?>(null) }
    var createOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var createPending by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    val routePresentation = playlistRoutePresentation(state)
    PlaylistScreenFrame(title = playlistsLabel, onBack = onBack) {
        if (state.isLoading && !state.hasConfirmedSnapshot) {
            item(key = "loading") {
                EmptyPlaylistMessage(loadingLabel)
            }
        } else if (state.readErrorMessage != null &&
            !state.hasConfirmedSnapshot) {
            item(key = "read-error") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyPlaylistMessage(loadFailedLabel)
                    CompactAction(
                        retryLabel, Modifier.fillMaxWidth(), onClick = onRetry)
                }
            }
        } else {
            item(key = "create") {
                Button(
                    onClick = {
                        createTypeAppearance =
                            appearanceSource.next("create-type")
                    },
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
            item(key = "static-heading") {
                Text(stringResource(Res.string.playlist_static_section))
            }
            if (state.confirmedSnapshot.playlists.isEmpty()) {
                item(key = "static-empty") {
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
            item(key = "smart-heading") {
                Text(stringResource(Res.string.smart_rules))
            }
            if (state.confirmedSnapshot.smartPlaylists.isEmpty()) {
                item(key = "smart-empty") {
                    EmptyPlaylistMessage(
                        stringResource(Res.string.smart_hub_empty))
                }
            } else {
                items(
                    state.confirmedSnapshot.smartPlaylists,
                    key = { "smart-${it.id}" }) { smart ->
                        CompactAction(
                            smart.name, Modifier.fillMaxWidth().height(48.dp)) {
                                onOpenSmartPlaylist(smart.id)
                            }
                    }
            }
        }
        item(key = "document-actions") {
            documentActions()
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
    val currentCreateTypeAppearance = createTypeAppearance
    val smartAppearance = smartDraft?.appearance
    PublishFeatureDismissal(
        destination = destination,
        publisher = dismissalPublisher,
        dismissal =
            smartDraft?.let {
                PlaylistFeatureDismissal.Modal(
                    destination, checkNotNull(smartAppearance))
            }
                ?: createDraft?.let {
                    PlaylistFeatureDismissal.Modal(
                        destination, checkNotNull(createAppearance))
                }
                ?: currentCreateTypeAppearance?.let {
                    PlaylistFeatureDismissal.Modal(destination, it)
                },
    ) { dismissal ->
        when (dismissal.appearance) {
            smartAppearance -> smartDraft = null
            createAppearance -> {
                createDraft = null
                createOutcome = null
                createPending = null
            }
            currentCreateTypeAppearance -> createTypeAppearance = null
            else -> Unit
        }
    }
    currentCreateTypeAppearance?.let { appearance ->
        PlaylistCreateTypeDialog(
            onStatic = {
                if (createTypeAppearance == appearance) {
                    createTypeAppearance = null
                    createDraft = PlaylistNameDraft()
                    createPending = null
                }
            },
            onSmart = {
                if (createTypeAppearance == appearance) {
                    createTypeAppearance = null
                    smartDraft =
                        SmartDraft(appearanceSource.next("smart-create"))
                }
            },
            onDismiss = {
                if (createTypeAppearance == appearance)
                    createTypeAppearance = null
            },
        )
    }
    smartDraft?.let { current ->
        SmartRuleDialog(
            current,
            libraryTracks,
            state.confirmedSnapshot,
            stringResource(Res.string.smart_create),
            { next ->
                if (smartDraft?.appearance == current.appearance &&
                    smartDraft?.pending == false)
                    smartDraft = next
            },
            {
                if (smartDraft?.appearance == current.appearance)
                    smartDraft = null
            },
            {
                if (smartDraft?.appearance == current.appearance &&
                    smartDraft?.pending == false) {
                    if (!validSmartDraft(
                        current, state.confirmedSnapshot, libraryTracks))
                        smartDraft = current.copy(failed = true)
                    else {
                        smartDraft = current.copy(pending = true)
                        onCreateSmartPlaylist(
                            current.name.trim(), current.rule) { outcome ->
                                if (smartDraft?.appearance ==
                                    current.appearance)
                                    smartDraft =
                                        if (outcome
                                            is
                                            PlaylistStateAction.SnapshotConfirmed)
                                            null
                                        else current.copy(failed = true)
                            }
                    }
                }
            })
    }
    createDraft?.let { draft ->
        val modalPresentation =
            playlistNameModalPresentation(draft, createOutcome)
        val isPending = createPending == createAppearance
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_create),
            draft = draft,
            notice = modalPresentation.notice,
            isPending = isPending,
            onDraftChange = {
                if (!isPending) {
                    createDraft = PlaylistNameDraft(it)
                    createOutcome = null
                }
            },
            onDismiss = {
                createDraft = null
                createOutcome = null
                createPending = null
            },
            onConfirm = {
                if (createPending == null) {
                    val name = draft.confirmedName()
                    if (name == null) {
                        createDraft = draft.mutationFailed()
                    } else {
                        val appearance = createAppearance
                        if (appearance != null) {
                            createPending = appearance
                            onCreate(name) { outcome ->
                                if (createPending == appearance) {
                                    createPending = null
                                    createOutcome = outcome
                                    if (playlistMutationDecision(
                                        PlaylistMutationWorkflow.Create,
                                        outcome) ==
                                        PlaylistMutationDecision.CloseModal) {
                                        createDraft = null
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun PlaylistRowArtwork(
    trackId: String,
    title: String,
    artworkBytes: ByteArray?,
    artworkLoader: suspend (String) -> ByteArray?,
) {
    PlaylistLazyArtworkImage(
        trackId = trackId,
        eagerArtworkBytes = artworkBytes,
        contentDescription = title,
        role = ArtworkImageRole.Thumbnail,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        artworkLoader = artworkLoader,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                title.firstOrNull()?.uppercase() ?: "♪",
                color = HausColors.current.ink,
                fontWeight = FontWeight.Black)
        }
    }
}

/** Renders one playlist detail route while Shared controls navigation. */
@Composable
public fun PlaylistDetailScreen(
    playlist: PlaylistSummary,
    entries: List<PlaylistEntry>,
    playableTracksById: Map<String, PlayableTrack>,
    artworkLoader: suspend (String) -> ByteArray? = { null },
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
    val desktopDensity = LocalHausDesktopDensity.current
    val tracksById = playableTracksById
    val model =
        playlistDetailModel(playlist.id, playlist.name, entries, tracksById)
    var renameDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var renameOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var renamePending by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var deleteOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var deletePending by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
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
                    renamePending = null
                })

            deleteConfirmation -> ({
                    deleteConfirmation = false
                    deleteOutcome = null
                    deletePending = null
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
                            onClick = onOpenBrowser)
                        CompactAction(
                            stringResource(Res.string.playlist_rename),
                            Modifier.weight(1f)) {
                                renameDraft = PlaylistNameDraft(playlist.name)
                                renamePending = null
                            }
                        CompactAction(
                            stringResource(Res.string.playlist_delete),
                            Modifier.weight(1f)) {
                                deleteConfirmation = true
                                deleteOutcome = null
                                deletePending = null
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
                artworkLoader = artworkLoader,
                desktopDensity = desktopDensity,
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
        val isPending = renamePending == renameAppearance
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            draft = draft,
            notice = modalPresentation.notice,
            isPending = isPending,
            onDraftChange = {
                if (!isPending) {
                    renameDraft = PlaylistNameDraft(it)
                    renameOutcome = null
                }
            },
            onDismiss = {
                renameDraft = null
                renameOutcome = null
                renamePending = null
            },
            onConfirm = {
                if (renamePending == null) {
                    val name = draft.confirmedName()
                    if (name == null) {
                        renameDraft = draft.mutationFailed()
                    } else {
                        val appearance = renameAppearance
                        if (appearance != null) {
                            renamePending = appearance
                            onRename(name) { outcome ->
                                if (renamePending == appearance) {
                                    renamePending = null
                                    renameOutcome = outcome
                                    if (playlistMutationDecision(
                                        PlaylistMutationWorkflow.Rename,
                                        outcome) ==
                                        PlaylistMutationDecision.CloseModal) {
                                        renameDraft = null
                                    }
                                }
                            }
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
            isPending = deletePending == deleteAppearance,
            onDismiss = {
                deleteConfirmation = false
                deleteOutcome = null
                deletePending = null
            },
            onConfirm = {
                if (deletePending == null) {
                    val appearance = deleteAppearance
                    if (appearance != null) {
                        deletePending = appearance
                        onDelete { outcome ->
                            if (deletePending == appearance) {
                                deletePending = null
                                deleteOutcome = outcome
                                if (playlistMutationDecision(
                                    PlaylistMutationWorkflow.Delete, outcome) ==
                                    PlaylistMutationDecision
                                        .CloseConfirmationAndRoute) {
                                    deleteConfirmation = false
                                    deleteOutcome = null
                                    onDeleteConfirmed(
                                        (outcome
                                                as
                                                PlaylistStateAction.SnapshotConfirmed)
                                            .snapshot)
                                }
                            }
                        }
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
                if (removeConfirmation == row) {
                    destructivePresentation = destructivePresentation?.dismiss()
                    removeConfirmation = null
                }
            },
            onConfirm = {
                if (removeConfirmation == row &&
                    destructivePresentation?.confirmedEntryId == null) {
                    destructivePresentation = destructivePresentation?.confirm()
                    removeConfirmation = null
                    destructivePresentation
                        ?.confirmedEntryId
                        ?.let(onRemoveEntry)
                }
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
    isPending: Boolean = false,
) {
    val currentPlaylists = rememberUpdatedState(playlists)
    val currentState = rememberUpdatedState(state)
    val currentOnStateChange = rememberUpdatedState(onStateChange)
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val currentOnAppend = rememberUpdatedState(onAppend)
    val currentOnInlineCreate = rememberUpdatedState(onInlineCreate)
    val currentNotice = rememberUpdatedState(notice)
    val currentIsPending = rememberUpdatedState(isPending)
    val title = stringResource(Res.string.playlist_add_to)
    HausDialog(
        title = title,
        onDismiss = { currentOnDismiss.value() },
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
                ModalFailureNotice(currentNotice.value)
                Text(
                    stringResource(Res.string.playlist_choose_existing),
                    color = HausColors.current.ink,
                    fontWeight = FontWeight.Bold)
                currentPlaylists.value.forEach { playlist ->
                    CompactAction(
                        text = playlist.name,
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentDescription = playlist.name
                            },
                        enabled = !currentIsPending.value,
                    ) {
                        currentOnStateChange.value(
                            currentState.value.copy(
                                selectedPlaylistId = playlist.id))
                    }
                }
                Text(
                    stringResource(Res.string.playlist_create_inline),
                    color = HausColors.current.ink,
                    fontWeight = FontWeight.Bold)
                PlaylistTextField(
                    currentState.value.enteredName,
                    modifier = Modifier.testTag("playlist-picker-name-field"),
                    enabled = !currentIsPending.value) {
                        currentOnStateChange.value(
                            currentState.value.copy(enteredName = it))
                    }
            }
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (currentState.value.selectedPlaylistId != null) {
                    CompactAction(
                        title,
                        Modifier.fillMaxWidth()
                            .testTag("playlist-picker-append"),
                        enabled = !currentIsPending.value) {
                            currentState.value
                                .confirmedAppend()
                                ?.let(currentOnAppend.value)
                        }
                }
                CompactAction(
                    stringResource(Res.string.playlist_create),
                    Modifier.fillMaxWidth().testTag("playlist-picker-create"),
                    enabled = !currentIsPending.value) {
                        currentState.value
                            .confirmedInlineCreate()
                            ?.let(currentOnInlineCreate.value)
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
    isPending: Boolean = false,
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
    val currentVisibleState = rememberUpdatedState(visibleState)
    val currentOnStateChange = rememberUpdatedState(onStateChange)
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val currentOnConfirm = rememberUpdatedState(onConfirm)
    val currentNotice = rememberUpdatedState(notice)
    val currentIsPending = rememberUpdatedState(isPending)
    HausDialog(
        title = playlistName,
        onDismiss = { currentOnDismiss.value() },
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
                ModalFailureNotice(currentNotice.value)
                PlaylistTextField(
                    currentVisibleState.value.query,
                    stringResource(Res.string.playlist_track_browser_search),
                    Modifier.testTag("playlist-browser-search"),
                    enabled = !currentIsPending.value) {
                        currentOnStateChange.value(
                            currentVisibleState.value.copy(query = it))
                    }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(visible, key = { it.id }) { track ->
                            val selected =
                                track.id in
                                    currentVisibleState.value.selectedTrackIds
                            val artist = track.artist.takeIf { it.isNotBlank() }
                            val album = track.album.takeIf { it.isNotBlank() }
                            val description =
                                when {
                                    artist != null && album != null ->
                                        "${track.title}, $artist, $album"
                                    artist != null -> "${track.title}, $artist"
                                    album != null -> "${track.title}, $album"
                                    else -> track.title
                                }
                            val toggle = {
                                currentOnStateChange.value(
                                    currentVisibleState.value.toggle(track.id))
                            }
                            HausSongRow(
                                title = track.title,
                                subtitle =
                                    songArtistAlbumSubtitle(artist, album),
                                selectionMode = true,
                                selected = selected,
                                playing = false,
                                interactive = !currentIsPending.value,
                                onActivate = toggle,
                                onSelect = toggle,
                                selectLabel = description,
                                rowContentDescription = description,
                                playingLabel = null,
                                artwork = {
                                    PlaylistRowArtwork(
                                        track.id,
                                        track.title,
                                        track.artworkBytes,
                                        { null })
                                },
                                indicator = {},
                                trailing = {},
                            )
                        }
                    }
            }
        },
        actions = {
            CompactAction(
                stringResource(Res.string.playlist_confirm_add),
                Modifier.fillMaxWidth().testTag("playlist-browser-confirm"),
                enabled = !currentIsPending.value) {
                    currentVisibleState.value
                        .confirmedAppend()
                        ?.let(currentOnConfirm.value)
                }
        },
    )
}

/**
 * Feature-owned playlist picker route; Shared supplies navigation and mutation
 * callbacks. [onConfirmed] runs only when the current appearance's mutation is
 * confirmed by a snapshot.
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
    onConfirmed: () -> Unit,
    onAppend: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
    onInlineCreate:
        (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
) {
    val appearance = rememberFeatureAppearance("picker", appearanceSource)
    var failureNotice by remember { mutableStateOf<PlaylistModalNotice?>(null) }
    var pendingAppearance by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    val dismissAndReset = {
        pendingAppearance = null
        failureNotice = null
        onDismiss()
    }
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance),
    ) {
        dismissAndReset()
        PlaylistFeatureDismissalDispatch.Started
    }
    AddToPlaylistPicker(
        playlists = playlists,
        state =
            AddToPlaylistPickerState(
                state.trackIds, state.selectedPlaylistId, state.enteredName),
        onStateChange = { next ->
            if (pendingAppearance == null) {
                onStateChange(
                    PlaylistPickerState(
                        next.trackIds,
                        next.selectedPlaylistId,
                        next.enteredName))
            }
        },
        onDismiss = dismissAndReset,
        onAppend = { request ->
            if (pendingAppearance == null) {
                failureNotice = null
                pendingAppearance = appearance
                onAppend(request.playlistId, request.trackIds) { outcome ->
                    if (pendingAppearance == appearance) {
                        pendingAppearance = null
                        if (outcome is PlaylistStateAction.SnapshotConfirmed)
                            onConfirmed()
                        else failureNotice = PlaylistModalNotice.MutationFailed
                    }
                }
            }
        },
        onInlineCreate = { request ->
            if (pendingAppearance == null) {
                failureNotice = null
                pendingAppearance = appearance
                onInlineCreate(request.name, request.trackIds) { outcome ->
                    if (pendingAppearance == appearance) {
                        pendingAppearance = null
                        if (outcome is PlaylistStateAction.SnapshotConfirmed)
                            onConfirmed()
                        else failureNotice = PlaylistModalNotice.MutationFailed
                    }
                }
            }
        },
        notice = failureNotice,
        isPending = pendingAppearance != null,
    )
}

/**
 * Feature-owned track browser route; Shared supplies navigation and mutation
 * callbacks. [onConfirmed] runs only when the current appearance's mutation is
 * confirmed by a snapshot.
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
    onConfirmed: () -> Unit,
    onConfirm: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
) {
    val appearance = rememberFeatureAppearance("browser", appearanceSource)
    var failureNotice by remember { mutableStateOf<PlaylistModalNotice?>(null) }
    var pendingAppearance by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    val dismissAndReset = {
        pendingAppearance = null
        failureNotice = null
        onDismiss()
    }
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance),
    ) {
        dismissAndReset()
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
            if (pendingAppearance == null) {
                onStateChange(
                    PlaylistBrowserState(
                        next.playlistId,
                        next.query,
                        next.visibleTrackIds,
                        next.selectedTrackIds))
            }
        },
        onDismiss = dismissAndReset,
        onConfirm = { request ->
            if (pendingAppearance == null) {
                failureNotice = null
                pendingAppearance = appearance
                onConfirm(request.playlistId, request.trackIds) { outcome ->
                    if (pendingAppearance == appearance) {
                        pendingAppearance = null
                        if (outcome is PlaylistStateAction.SnapshotConfirmed)
                            onConfirmed()
                        else failureNotice = PlaylistModalNotice.MutationFailed
                    }
                }
            }
        },
        notice = failureNotice,
        isPending = pendingAppearance != null,
    )
}

@Composable
internal fun PlaylistScreenFrame(
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
    artworkLoader: suspend (String) -> ByteArray?,
    desktopDensity: Boolean,
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
    val displayArtist = row.track.artist.takeIf { it.isNotBlank() }
    val displayAlbum = row.track.album?.takeIf { it.isNotBlank() }
    val rowSubtitle = songArtistAlbumSubtitle(displayArtist, displayAlbum)
    val rowDescription =
        when {
            displayArtist != null && displayAlbum != null ->
                "${row.track.title}, $displayArtist, $displayAlbum, $duration"
            displayArtist != null ->
                "${row.track.title}, $displayArtist, $duration"
            displayAlbum != null ->
                "${row.track.title}, $displayAlbum, $duration"
            else -> "${row.track.title}, $duration"
        }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val layoutPolicy =
            playlistDetailRowLayoutPolicy(
                maxWidth,
                mode == PlaylistDetailRowMode.Edit,
                desktopDensity,
            )
        val editing = mode == PlaylistDetailRowMode.Edit
        Column(
            Modifier.fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    rowCenters[rowIndex] =
                        coordinates.positionInRoot().y +
                            coordinates.size.height / 2f
                }
                .testTag("playlist-entry-metadata-${row.entry.id}")
                .semantics { stateDescription = entryState },
        ) {
            HausSongRow(
                title = row.track.title,
                subtitle = rowSubtitle,
                selectionMode = false,
                selected = false,
                playing = false,
                interactive = true,
                onActivate = onClick,
                onSelect = onLongClick,
                selectLabel = rowDescription,
                rowContentDescription = rowDescription,
                playingLabel = null,
                artwork = {
                    PlaylistRowArtwork(
                        trackId = row.track.id,
                        title = row.track.title,
                        artworkBytes = row.track.artworkBytes,
                        artworkLoader = artworkLoader,
                    )
                },
                indicator = {},
                trailing = {
                    Text(
                        duration,
                        color = HausColors.current.muted,
                        fontSize = 12.sp)
                    if (editing &&
                        layoutPolicy.actionPlacement ==
                            PlaylistDetailActionPlacement.Inline)
                        PlaylistEntryMutationActions(
                            availability,
                            moveUp,
                            moveDown,
                            remove,
                            onMove,
                            onRemove,
                            targetSize = layoutPolicy.minimumInteractiveTarget)
                    if (editing)
                        PlaylistEntryDragHandle(
                            row,
                            rowIndex,
                            entryIds,
                            rowCenters,
                            drag,
                            onDragOrder,
                            targetSize = layoutPolicy.minimumInteractiveTarget)
                },
            )
            if (editing &&
                layoutPolicy.actionPlacement ==
                    PlaylistDetailActionPlacement.SecondaryRow) {
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
                            onRemove,
                            targetSize = layoutPolicy.minimumInteractiveTarget)
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
    onDragOrder: (List<String>) -> Unit,
    targetSize: androidx.compose.ui.unit.Dp,
) {
    Text(
        "≡",
        modifier =
            Modifier.size(targetSize)
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
    onRemove: () -> Unit,
    targetSize: androidx.compose.ui.unit.Dp,
) {
    IconButton(
        onClick = { onMove(-1) },
        enabled = availability.canMoveUp,
        minWidth = targetSize,
        minHeight = targetSize,
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
        minWidth = targetSize,
        minHeight = targetSize,
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
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier = Modifier.semantics { contentDescription = remove }) {
            Text("×", color = HausColors.current.danger)
        }
}

@Composable
internal fun CompactAction(
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.heightIn(min = 44.dp).semantics {
                contentDescription = text
            },
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        colors =
            ButtonDefaults.buttonColors(
                color = HausColors.current.panel,
                contentColor = HausColors.current.ink,
            ),
    ) {
        Text(
            text = text,
            modifier = Modifier.semantics { contentDescription = text },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EmptyPlaylistMessage(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        color = HausColors.current.muted,
        fontSize = 15.sp)
}

@Composable
internal fun PlaylistNotice(state: PlaylistState, mutationFailedLabel: String) {
    if (state.mutationErrorMessage != null)
        Text(
            mutationFailedLabel,
            color = HausColors.current.danger,
            fontSize = 13.sp)
}

@Composable
internal fun PlaylistNameDialog(
    title: String,
    draft: PlaylistNameDraft,
    notice: PlaylistModalNotice?,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = title,
    isPending: Boolean = false,
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
                PlaylistTextField(
                    draft.enteredText,
                    modifier = Modifier.testTag("playlist-name-field"),
                    enabled = !isPending) {
                        onDraftChange(it)
                    }
                if (draft.showFailure)
                    Text(
                        stringResource(Res.string.playlist_create_name),
                        color = HausColors.current.danger,
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
                    onClick = onDismiss)
                CompactAction(
                    confirmLabel,
                    Modifier.weight(1f).testTag("playlist-name-confirm"),
                    enabled = !isPending,
                    onClick = onConfirm)
            }
        },
    )
}

@Composable
private fun PlaylistCreateTypeDialog(
    onStatic: () -> Unit,
    onSmart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(Res.string.playlist_create)
    val cancel = stringResource(Res.string.playlist_cancel)
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        dismissLabel = cancel,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(Res.string.playlist_create_type_prompt),
                    color = HausColors.current.muted,
                    fontSize = 14.sp,
                )
                CompactAction(
                    stringResource(Res.string.playlist_type_static),
                    Modifier.fillMaxWidth(),
                    onClick = onStatic,
                )
                CompactAction(
                    stringResource(Res.string.playlist_type_smart),
                    Modifier.fillMaxWidth(),
                    onClick = onSmart,
                )
            }
        },
        actions = {
            CompactAction(cancel, Modifier.fillMaxWidth(), onClick = onDismiss)
        },
    )
}

@Composable
private fun ModalFailureNotice(notice: PlaylistModalNotice?) {
    if (notice == PlaylistModalNotice.MutationFailed) {
        Text(
            stringResource(Res.string.playlist_modal_mutation_failed),
            color = HausColors.current.danger,
            fontSize = 13.sp)
    }
}

@Composable
internal fun ReadFailureNotice(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.playlist_retained_load_failed),
            color = HausColors.current.muted,
            fontSize = 13.sp)
        CompactAction(
            stringResource(Res.string.playlist_retained_retry),
            Modifier.fillMaxWidth(),
            onClick = onRetry)
    }
}

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    notice: PlaylistModalNotice? = null,
    isPending: Boolean = false,
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
                    onClick = onDismiss)
                CompactAction(
                    title,
                    Modifier.weight(1f).testTag("playlist-confirm"),
                    enabled = !isPending,
                    onClick = onConfirm)
            }
        },
    )
}

@Composable
private fun PlaylistTextField(
    value: String,
    label: String = stringResource(Res.string.playlist_create_name),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label,
        useLabelAsPlaceholder = true,
        enabled = enabled,
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
