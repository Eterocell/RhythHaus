package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.BackChip
import com.eterocell.rhythhaus.ui.HausButton as Button
import com.eterocell.rhythhaus.ui.HausIconButton as IconButton
import com.eterocell.rhythhaus.ui.HausSongRow
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import com.eterocell.rhythhaus.ui.LocalHausWindowOverlayPublisher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.Res
import rhythhaus.feature.playlists.generated.resources.playlist_create
import rhythhaus.feature.playlists.generated.resources.playlist_empty_queue
import rhythhaus.feature.playlists.generated.resources.queue_back_to_player
import rhythhaus.feature.playlists.generated.resources.queue_changed
import rhythhaus.feature.playlists.generated.resources.queue_clear_confirm
import rhythhaus.feature.playlists.generated.resources.queue_clear_confirmation
import rhythhaus.feature.playlists.generated.resources.queue_clear_upcoming
import rhythhaus.feature.playlists.generated.resources.queue_current
import rhythhaus.feature.playlists.generated.resources.queue_current_state
import rhythhaus.feature.playlists.generated.resources.queue_drag_format
import rhythhaus.feature.playlists.generated.resources.queue_move_down_format
import rhythhaus.feature.playlists.generated.resources.queue_move_up_format
import rhythhaus.feature.playlists.generated.resources.queue_played
import rhythhaus.feature.playlists.generated.resources.queue_played_state
import rhythhaus.feature.playlists.generated.resources.queue_remove_format
import rhythhaus.feature.playlists.generated.resources.queue_save_as_playlist
import rhythhaus.feature.playlists.generated.resources.queue_title
import rhythhaus.feature.playlists.generated.resources.queue_upcoming
import rhythhaus.feature.playlists.generated.resources.queue_upcoming_state
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

internal enum class QueueRowRole {
    Played,
    Current,
    Upcoming,
}

internal enum class QueueRowState {
    Played,
    Current,
    Upcoming,
}

internal enum class QueueRowAction {
    Drag,
    MoveUp,
    MoveDown,
    Remove,
}

internal enum class QueueActionPlacement {
    None,
    Inline,
    SecondaryRow,
}

internal data class QueueRowLayoutPolicy(
    val actionPlacement: QueueActionPlacement,
    val reservesMetadataWidth: Boolean = true,
    val minimumInteractiveTarget: Dp = 44.dp,
)

internal fun queueRowLayoutPolicy(
    availableWidth: Dp,
    isEditable: Boolean,
    desktopDensity: Boolean,
): QueueRowLayoutPolicy =
    QueueRowLayoutPolicy(
        actionPlacement =
            when {
                !isEditable -> QueueActionPlacement.None
                availableWidth < 520.dp -> QueueActionPlacement.SecondaryRow
                else -> QueueActionPlacement.Inline
            },
        minimumInteractiveTarget = if (desktopDensity) 44.dp else 48.dp,
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

internal data class PlaybackQueuePresentation(
    val playedRows: List<QueueRowPresentation>,
    val currentRow: QueueRowPresentation?,
    val upcomingRows: List<QueueRowPresentation>,
) {
    val isEmpty: Boolean
        get() =
            playedRows.isEmpty() && currentRow == null && upcomingRows.isEmpty()

    val upcomingOccurrenceIds: List<String> = upcomingRows.map {
        it.occurrence.id
    }

    val saveableTrackIds: List<String> =
        currentRow?.let { current ->
            buildList {
                add(current.occurrence.track.id)
                upcomingRows.forEach { add(it.occurrence.track.id) }
            }
        } ?: emptyList()
}

internal fun playbackQueuePresentation(
    state: PlaybackState,
): PlaybackQueuePresentation {
    val currentIndex =
        state.queue.indexOfFirst { it.id == state.currentOccurrenceId }
    val playedRows = buildList {
        for (index in 0 until currentIndex.coerceAtLeast(0)) {
            add(
                QueueRowPresentation(
                    occurrence = state.queue[index],
                    role = QueueRowRole.Played,
                    canDrag = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    canRemove = false,
                    semanticState = QueueRowState.Played,
                ),
            )
        }
    }
    val currentRow =
        state.queue.getOrNull(currentIndex)?.let { occurrence ->
            QueueRowPresentation(
                occurrence = occurrence,
                role = QueueRowRole.Current,
                canDrag = false,
                canMoveUp = false,
                canMoveDown = false,
                canRemove = false,
            )
        }
    val upcomingStart = if (currentIndex >= 0) currentIndex + 1 else 0
    val upcomingCount = (state.queue.size - upcomingStart).coerceAtLeast(0)
    val upcomingRows = buildList {
        for (index in upcomingStart until state.queue.size) {
            val upcomingIndex = index - upcomingStart
            add(
                QueueRowPresentation(
                    occurrence = state.queue[index],
                    role = QueueRowRole.Upcoming,
                    canDrag = true,
                    canMoveUp = upcomingIndex > 0,
                    canMoveDown = upcomingIndex < upcomingCount - 1,
                    canRemove = true,
                ),
            )
        }
    }
    return PlaybackQueuePresentation(playedRows, currentRow, upcomingRows)
}

internal fun queueDragTargetIndex(
    pointerY: Float,
    rowCenters: Map<String, Float>,
    upcomingIds: List<String>,
    fallbackOccurrenceId: String,
): Int {
    val fallbackIndex =
        upcomingIds.indexOf(fallbackOccurrenceId).coerceAtLeast(0)
    var nearestIndex = fallbackIndex
    var nearestDistance = Float.POSITIVE_INFINITY
    for (index in upcomingIds.indices) {
        val center = rowCenters[upcomingIds[index]] ?: continue
        val distance = kotlin.math.abs(center - pointerY)
        if (distance < nearestDistance) {
            nearestIndex = index
            nearestDistance = distance
        }
    }
    return nearestIndex
}

/**
 * Immutable queue mutation outcome returned after the authoritative state
 * refresh.
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

private data class QueueSaveDraft(
    val name: PlaylistNameDraft,
    val trackIds: List<String>,
    val appearance: PlaylistDismissalAppearance,
    val pending: Boolean = false,
)

/**
 * Renders the one authoritative playback queue surface used by expanded Now
 * Playing. Queue mutations are supplied by Shared and retain their existing
 * serialized controller path.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
public fun PlaybackQueueContent(
    playbackState: PlaybackState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    onClose: () -> Unit,
    onSaveQueueAsPlaylist:
        (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit,
    onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback,
    onRemoveUpcoming: suspend (String) -> QueueMutationFeedback,
    onClearUpcoming: suspend () -> QueueMutationFeedback,
    artworkLoader: suspend (String) -> ByteArray? = { null },
    modifier: Modifier = Modifier,
) {
    val desktopDensity = LocalHausDesktopDensity.current
    var confirmedState by remember { mutableStateOf(playbackState) }
    var showQueueChanged by remember { mutableStateOf(false) }
    var saveDraft by remember { mutableStateOf<QueueSaveDraft?>(null) }
    var saveOutcome by remember { mutableStateOf<PlaylistStateAction?>(null) }
    var clearConfirmation by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    val scope = rememberCoroutineScope()
    val rowCenters = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(playbackState) { confirmedState = playbackState }
    val presentation =
        remember(confirmedState.currentOccurrenceId, confirmedState.queue) {
            playbackQueuePresentation(confirmedState)
        }
    LaunchedEffect(presentation.upcomingOccurrenceIds) { rowCenters.clear() }
    val backDescription = stringResource(Res.string.queue_back_to_player)
    val windowOverlayPublisher = LocalHausWindowOverlayPublisher.current
    val saveQueueFocusRequester = remember { FocusRequester() }
    val queueSurfaceAttached = remember { mutableStateOf(true) }
    DisposableEffect(queueSurfaceAttached) {
        onDispose { queueSurfaceAttached.value = false }
    }

    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackChip(
                onClick = onClose,
                modifier =
                    Modifier.semantics {
                        contentDescription = backDescription
                    },
            )
            Text(
                stringResource(Res.string.queue_title),
                modifier = Modifier.weight(1f),
                color = HausColors.current.ink,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                fontSize = 18.sp,
            )
        }
        LazyColumn(
            modifier =
                Modifier.fillMaxWidth().weight(1f).focusRestorer().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (presentation.saveableTrackIds.isNotEmpty()) {
                item(key = "queue-save") {
                    Button(
                        onClick = {
                            saveDraft =
                                QueueSaveDraft(
                                    name = PlaylistNameDraft(),
                                    trackIds = presentation.saveableTrackIds,
                                    appearance =
                                        appearanceSource.next("save-queue"),
                                )
                            saveOutcome = null
                        },
                        modifier =
                            Modifier.focusRequester(saveQueueFocusRequester)
                                .fillMaxWidth()
                                .height(48.dp),
                        cornerRadius = 16.dp,
                        colors =
                            ButtonDefaults.buttonColors(
                                HausColors.current.ink,
                                HausColors.current.paper,
                            ),
                    ) {
                        Text(stringResource(Res.string.queue_save_as_playlist))
                    }
                }
            }
            if (presentation.isEmpty) {
                item(key = "queue-empty") {
                    EmptyPlaylistMessage(
                        stringResource(Res.string.playlist_empty_queue),
                    )
                }
            } else {
                if (presentation.playedRows.isNotEmpty()) {
                    item(key = "queue-played-label") {
                        PlaybackQueueSectionLabel(
                            stringResource(Res.string.queue_played),
                        )
                    }
                    itemsIndexed(
                        items = presentation.playedRows,
                        key = { _, row -> row.occurrence.id },
                    ) { _, row ->
                        PlaybackQueueOccurrenceRow(
                            row = row,
                            artworkLoader = artworkLoader,
                            desktopDensity = desktopDensity,
                        )
                    }
                }
                presentation.currentRow?.let { current ->
                    item(key = "queue-current-label") {
                        PlaybackQueueSectionLabel(
                            stringResource(Res.string.queue_current),
                        )
                    }
                    item(key = current.occurrence.id) {
                        PlaybackQueueOccurrenceRow(
                            row = current,
                            artworkLoader = artworkLoader,
                            desktopDensity = desktopDensity,
                        )
                    }
                }
                if (presentation.upcomingRows.isNotEmpty()) {
                    item(key = "queue-upcoming-label") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlaybackQueueSectionLabel(
                                stringResource(Res.string.queue_upcoming),
                                Modifier.weight(1f),
                            )
                            CompactAction(
                                stringResource(Res.string.queue_clear_upcoming),
                                Modifier,
                            ) {
                                clearConfirmation =
                                    appearanceSource.next("queue")
                            }
                        }
                    }
                    itemsIndexed(
                        items = presentation.upcomingRows,
                        key = { _, row -> row.occurrence.id },
                    ) { index, row ->
                        PlaybackQueueOccurrenceRow(
                            row = row,
                            artworkLoader = artworkLoader,
                            desktopDensity = desktopDensity,
                            upcomingIndex = index,
                            upcomingIds = presentation.upcomingOccurrenceIds,
                            rowCenters = rowCenters,
                            onMove = { offset ->
                                scope.launch {
                                    val feedback =
                                        onReorderUpcoming(
                                            row.occurrence.id,
                                            index + offset,
                                        )
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            },
                            onDragTarget = { targetIndex ->
                                scope.launch {
                                    val feedback =
                                        onReorderUpcoming(
                                            row.occurrence.id,
                                            targetIndex,
                                        )
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
            if (showQueueChanged) {
                item(key = "queue-changed") {
                    Text(
                        stringResource(Res.string.queue_changed),
                        color = HausColors.current.pulse,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    val saveAppearance = saveDraft?.appearance
    val clearAppearance = clearConfirmation
    PublishFeatureDismissal(
        destination = destination,
        publisher = dismissalPublisher,
        dismissal =
            saveDraft?.let {
                PlaylistFeatureDismissal.Modal(
                    destination,
                    checkNotNull(saveAppearance),
                )
            }
                ?: clearConfirmation?.let {
                    PlaylistFeatureDismissal.Modal(
                        destination,
                        checkNotNull(clearAppearance),
                    )
                },
    ) { dismissal ->
        when (dismissal.appearance) {
            saveAppearance -> {
                saveDraft = null
                saveOutcome = null
            }
            clearAppearance -> clearConfirmation = null
            else -> Unit
        }
    }

    val modalContent: (@Composable () -> Unit)? =
        if (saveDraft == null && clearConfirmation == null) {
            null
        } else {
            @Composable {
                saveDraft?.let { save ->
                    PlaylistNameDialog(
                        title =
                            stringResource(Res.string.queue_save_as_playlist),
                        confirmLabel =
                            stringResource(Res.string.playlist_create),
                        draft = save.name,
                        notice =
                            playlistNameModalPresentation(
                                    save.name,
                                    saveOutcome,
                                )
                                .notice,
                        onDraftChange = {
                            if (saveDraft?.appearance == save.appearance &&
                                !save.pending) {
                                saveDraft =
                                    save.copy(name = PlaylistNameDraft(it))
                                saveOutcome = null
                            }
                        },
                        onDismiss = {
                            if (saveDraft?.appearance == save.appearance) {
                                saveDraft = null
                                saveOutcome = null
                            }
                        },
                        onConfirm = {
                            if (saveDraft?.appearance == save.appearance &&
                                !save.pending) {
                                val name = save.name.confirmedName()
                                if (name == null) {
                                    saveDraft =
                                        save.copy(
                                            name = save.name.mutationFailed(),
                                        )
                                } else {
                                    saveDraft = save.copy(pending = true)
                                    onSaveQueueAsPlaylist(
                                        name,
                                        save.trackIds,
                                    ) { outcome ->
                                        if (saveDraft?.appearance ==
                                            save.appearance) {
                                            saveOutcome = outcome
                                            saveDraft =
                                                if (outcome
                                                    is
                                                    PlaylistStateAction.SnapshotConfirmed) {
                                                    null
                                                } else {
                                                    save.copy(pending = false)
                                                }
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                clearConfirmation?.let { confirmation ->
                    ConfirmationDialog(
                        title = stringResource(Res.string.queue_clear_confirm),
                        message =
                            stringResource(Res.string.queue_clear_confirmation),
                        onDismiss = {
                            if (clearConfirmation == confirmation) {
                                clearConfirmation = null
                            }
                        },
                        onConfirm = {
                            if (clearConfirmation == confirmation) {
                                clearConfirmation = null
                                scope.launch {
                                    val feedback = onClearUpcoming()
                                    confirmedState = feedback.refreshedState
                                    showQueueChanged = feedback.showQueueChanged
                                }
                            }
                        },
                    )
                }
            }
        }
    val currentModalContent by rememberUpdatedState(modalContent)
    val hostedModalContent: @Composable () -> Unit = remember {
        @Composable {
            val content = currentModalContent
            if (content != null) {
                content()
            }
        }
    }
    if (windowOverlayPublisher == null) {
        modalContent?.invoke()
    } else {
        DisposableEffect(windowOverlayPublisher) {
            onDispose { windowOverlayPublisher.publish(null, null) }
        }
        SideEffect {
            val restoreFocus: (() -> Boolean)? =
                if (saveDraft != null &&
                    presentation.saveableTrackIds.isNotEmpty()) {
                    {
                        queueSurfaceAttached.value &&
                            saveQueueFocusRequester.requestFocus()
                    }
                } else {
                    null
                }
            windowOverlayPublisher.publish(
                if (modalContent == null) null else hostedModalContent,
                restoreInvokerFocus = restoreFocus,
            )
        }
    }
}

@Composable
private fun PlaybackQueueSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 4.dp),
        color = HausColors.current.ink,
        fontSize = 13.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
    )
}

@Composable
private fun PlaybackQueueOccurrenceRow(
    row: QueueRowPresentation,
    artworkLoader: suspend (String) -> ByteArray?,
    desktopDensity: Boolean,
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
                QueueRowState.Played -> Res.string.queue_played_state
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val layoutPolicy =
            queueRowLayoutPolicy(
                maxWidth, row.availableActions.isNotEmpty(), desktopDensity)
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (upcomingIndex >= 0) {
                            Modifier.onGloballyPositioned { coordinates ->
                                rowCenters[row.occurrence.id] =
                                    coordinates.positionInRoot().y +
                                        coordinates.size.height / 2f
                            }
                        } else Modifier,
                    )
                    .semantics {
                        row.semanticRole?.let { role = it }
                        if (!isCurrent) stateDescription = rowState
                    },
        ) {
            HausSongRow(
                title = row.occurrence.track.title,
                subtitle = row.occurrence.track.artist,
                selectionMode = false,
                selected = false,
                playing = isCurrent,
                interactive = false,
                onActivate = {},
                onSelect = null,
                selectLabel = null,
                playingLabel = rowState,
                desktopDensity = desktopDensity,
                artwork = {
                    PlaybackQueueArtwork(row.occurrence.track, artworkLoader)
                },
                indicator = {
                    Text(
                        "▶", color = HausColors.current.pulse, fontSize = 14.sp)
                },
                trailing = {
                    if (layoutPolicy.actionPlacement ==
                        QueueActionPlacement.Inline) {
                        PlaybackQueueMutationActions(
                            row,
                            layoutPolicy.minimumInteractiveTarget,
                            moveUp,
                            moveDown,
                            remove,
                            onMove,
                            onRemove,
                        )
                    }
                    if (QueueRowAction.Drag in row.availableActions) {
                        PlaybackQueueDragHandle(
                            row,
                            upcomingIndex,
                            upcomingIds,
                            rowCenters,
                            layoutPolicy.minimumInteractiveTarget,
                            drag,
                            onDragTarget,
                        )
                    }
                },
            )
            if (layoutPolicy.actionPlacement ==
                QueueActionPlacement.SecondaryRow) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    PlaybackQueueMutationActions(
                        row,
                        layoutPolicy.minimumInteractiveTarget,
                        moveUp,
                        moveDown,
                        remove,
                        onMove,
                        onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackQueueDragHandle(
    row: QueueRowPresentation,
    upcomingIndex: Int,
    upcomingIds: List<String>,
    rowCenters: MutableMap<String, Float>,
    targetSize: Dp,
    contentDescription: String,
    onDragTarget: (Int) -> Unit,
) {
    Text(
        "≡",
        modifier =
            Modifier.size(targetSize)
                .pointerInput(row.occurrence.id, upcomingIds) {
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
                                    pointerY,
                                    rowCenters,
                                    upcomingIds,
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
private fun PlaybackQueueArtwork(
    track: PlayableTrack,
    artworkLoader: suspend (String) -> ByteArray?,
) {
    PlaylistLazyArtworkImage(
        trackId = track.id,
        eagerArtworkBytes = track.artworkBytes,
        contentDescription = track.title,
        role = ArtworkImageRole.Thumbnail,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        artworkLoader = artworkLoader,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                track.title.firstOrNull()?.uppercase() ?: "♪",
                color = HausColors.current.ink,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PlaybackQueueMutationActions(
    row: QueueRowPresentation,
    targetSize: Dp,
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
            Modifier.semantics { contentDescription = moveUpDescription },
    ) {
        Text("↑", color = HausColors.current.ink)
    }
    IconButton(
        onClick = { onMove(1) },
        enabled = QueueRowAction.MoveDown in row.availableActions,
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics { contentDescription = moveDownDescription },
    ) {
        Text("↓", color = HausColors.current.ink)
    }
    IconButton(
        onClick = onRemove,
        minWidth = targetSize,
        minHeight = targetSize,
        backgroundColor = Color.Transparent,
        modifier =
            Modifier.semantics { contentDescription = removeDescription },
    ) {
        Text("×", color = HausColors.current.danger)
    }
}
