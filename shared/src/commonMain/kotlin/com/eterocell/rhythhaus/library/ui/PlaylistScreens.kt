package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarContentPadding
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import com.eterocell.rhythhaus.ui.LazyTrackArtworkImage
import org.jetbrains.compose.resources.stringResource
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

data class PlaylistMoveAvailability(val canMoveUp: Boolean, val canMoveDown: Boolean)

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

data class PlaylistAppendRequest(val playlistId: String, val trackIds: List<String>)
data class PlaylistInlineCreateRequest(val name: String, val trackId: String)
data class PlaylistInlineMutationPlan(val name: String, val trackIds: List<String>)

fun PlaylistInlineCreateRequest.mutationPlan(): PlaylistInlineMutationPlan =
    PlaylistInlineMutationPlan(name, listOf(trackId))

fun openAddToPlaylistPickerAction(trackId: String): PlaylistStateAction =
    PlaylistStateAction.OpenPicker(PlaylistPickerState(trackId = trackId))

fun filteredPlaylistTrackIds(tracks: List<LibraryTrack>, query: String): List<String> = tracks
    .filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any {
            it.contains(query, ignoreCase = true)
        }
    }
    .map(LibraryTrack::id)

data class AddToPlaylistPickerState(
    val trackId: String,
    val selectedPlaylistId: String? = null,
    val enteredName: String = "",
) {
    fun confirmedAppend(): PlaylistAppendRequest? = selectedPlaylistId?.let { PlaylistAppendRequest(it, listOf(trackId)) }
    fun confirmedInlineCreate(): PlaylistInlineCreateRequest? = enteredName.trim().takeIf(String::isNotEmpty)?.let {
        PlaylistInlineCreateRequest(it, trackId)
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
    fun confirmedAppend(): PlaylistAppendRequest = PlaylistAppendRequest(playlistId, confirmedTrackIds())
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

@Composable
internal fun PlaylistHubScreen(
    state: PlaylistState,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSelectTab: (PlaylistTab) -> Unit,
    onCreate: (String, () -> Unit) -> Unit,
    onRetry: () -> Unit,
) {
    var createDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
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
        }
        item(key = "notice") { PlaylistNotice(state) }
        item(key = "spacer") { Spacer(Modifier.height(NowPlayingBarContentPadding)) }
    }
    createDraft?.let { draft ->
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_create),
            draft = draft,
            onDraftChange = { createDraft = PlaylistNameDraft(it) },
            onDismiss = { createDraft = null },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) createDraft = draft.mutationFailed() else {
                    onCreate(name) { createDraft = null }
                }
            },
        )
    }
}

@Composable
internal fun PlaylistDetailScreen(
    playlist: Playlist,
    entries: List<PlaylistEntry>,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistState,
    onBack: () -> Unit,
    onRename: (String, () -> Unit) -> Unit,
    onDelete: (() -> Unit) -> Unit,
    onOpenBrowser: () -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val tracksById = remember(libraryTracks) { libraryTracks.associate { it.id to it.toPlayableTrack() } }
    val model = playlistDetailModel(playlist.id, playlist.name, entries, tracksById)
    var renameDraft by remember { mutableStateOf<PlaylistNameDraft?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var removeConfirmation by remember { mutableStateOf<PlaylistDetailRow?>(null) }
    PlaylistScreenFrame(title = playlist.name, onBack = onBack) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactAction(stringResource(Res.string.playlist_add_tracks), Modifier.weight(1f), onOpenBrowser)
                CompactAction(stringResource(Res.string.playlist_rename), Modifier.weight(1f)) { renameDraft = PlaylistNameDraft(playlist.name) }
                CompactAction(stringResource(Res.string.playlist_delete), Modifier.weight(1f)) { deleteConfirmation = true }
            }
        }
        if (model.rows.isEmpty()) item(key = "empty") { EmptyPlaylistMessage(stringResource(Res.string.playlist_empty_detail)) }
        items(model.rows, key = { it.entry.id }) { row ->
            PlaylistEntryRow(
                row = row,
                availability = playlistMoveAvailability(entries.map(PlaylistEntry::id), row.entry.id),
                onClick = {
                    savedPlaylistPlaybackRequest(entries, tracksById, row.entry.id)?.let(onPlayEntry)
                },
                onMove = { offset -> onReorder(movedPlaylistEntryIds(entries.map(PlaylistEntry::id), row.entry.id, offset)) },
                onRemove = { removeConfirmation = row },
            )
        }
        item(key = "notice") { PlaylistNotice(state) }
        item(key = "spacer") { Spacer(Modifier.height(NowPlayingBarContentPadding)) }
    }
    renameDraft?.let { draft ->
        PlaylistNameDialog(
            title = stringResource(Res.string.playlist_rename),
            draft = draft,
            onDraftChange = { renameDraft = PlaylistNameDraft(it) },
            onDismiss = { renameDraft = null },
            onConfirm = {
                val name = draft.confirmedName()
                if (name == null) renameDraft = draft.mutationFailed() else {
                    onRename(name) { renameDraft = null }
                }
            },
        )
    }
    if (deleteConfirmation) ConfirmationDialog(
        title = stringResource(Res.string.playlist_delete),
        message = stringResource(Res.string.playlist_delete_confirmation_format, playlist.name),
        onDismiss = { deleteConfirmation = false },
        onConfirm = { onDelete { deleteConfirmation = false } },
    )
    removeConfirmation?.let { row ->
        ConfirmationDialog(
            title = stringResource(Res.string.playlist_remove_track_format, row.track.title),
            message = stringResource(Res.string.playlist_remove_track_format, row.track.title),
            onDismiss = { removeConfirmation = null },
            onConfirm = { removeConfirmation = null; onRemoveEntry(row.entry.id) },
        )
    }
}

@Composable
internal fun AddToPlaylistPicker(
    track: LibraryTrack,
    playlists: List<Playlist>,
    state: AddToPlaylistPickerState,
    onStateChange: (AddToPlaylistPickerState) -> Unit,
    onDismiss: () -> Unit,
    onAppend: (PlaylistAppendRequest) -> Unit,
    onInlineCreate: (PlaylistInlineCreateRequest) -> Unit,
) {
    ModalCard(onDismiss) {
        Text(stringResource(Res.string.playlist_add_to), color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(track.title, color = HausColors.current.muted, fontSize = 13.sp)
        Text(stringResource(Res.string.playlist_choose_existing), color = HausColors.current.ink, fontWeight = FontWeight.Bold)
        playlists.forEach { playlist ->
            val selected = state.selectedPlaylistId == playlist.id
            CompactAction(
                text = playlist.name,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = playlist.name },
            ) { onStateChange(state.copy(selectedPlaylistId = playlist.id)) }
            if (selected) {
                CompactAction(stringResource(Res.string.playlist_add_to), Modifier.fillMaxWidth()) {
                    state.confirmedAppend()?.let(onAppend)
                }
            }
        }
        Text(stringResource(Res.string.playlist_create_inline), color = HausColors.current.ink, fontWeight = FontWeight.Bold)
        PlaylistTextField(state.enteredName) { onStateChange(state.copy(enteredName = it)) }
        CompactAction(stringResource(Res.string.playlist_create), Modifier.fillMaxWidth()) {
            state.confirmedInlineCreate()?.let(onInlineCreate)
        }
    }
}

@Composable
internal fun PlaylistTrackBrowser(
    playlistName: String,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistTrackBrowserState,
    onStateChange: (PlaylistTrackBrowserState) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistAppendRequest) -> Unit,
) {
    val visibleIds = remember(state.query, libraryTracks) { filteredPlaylistTrackIds(libraryTracks, state.query) }
    val visible = remember(visibleIds, libraryTracks) {
        val byId = libraryTracks.associateBy(LibraryTrack::id)
        visibleIds.mapNotNull(byId::get)
    }
    val visibleState = state.copy(visibleTrackIds = visible.map(LibraryTrack::id))
    val selectedStateDescription = stringResource(Res.string.playlist_selected_state)
    ModalCard(onDismiss) {
        Text(playlistName, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
        CompactAction(stringResource(Res.string.playlist_confirm_add), Modifier.fillMaxWidth()) {
            val request = visibleState.confirmedAppend()
            if (request.trackIds.isNotEmpty()) onConfirm(request)
        }
    }
}

@Composable
private fun PlaylistScreenFrame(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, minWidth = 44.dp, minHeight = 44.dp, backgroundColor = Color.Transparent) { Text("‹", fontSize = 30.sp, color = HausColors.current.ink) }
                Text(title, color = HausColors.current.ink, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

@Composable
private fun PlaylistTabs(selected: PlaylistTab, onSelect: (PlaylistTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PlaylistTab.Saved to stringResource(Res.string.playlist_saved_tab), PlaylistTab.Queue to stringResource(Res.string.playlist_queue_tab)).forEach { (tab, label) ->
            Button(onClick = { onSelect(tab) }, modifier = Modifier.weight(1f).height(40.dp), cornerRadius = 20.dp, colors = ButtonDefaults.buttonColors(if (selected == tab) HausColors.current.ink else HausColors.current.panel, if (selected == tab) HausColors.current.paper else HausColors.current.ink)) { Text(label, fontWeight = FontWeight.Bold) }
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
private fun PlaylistEntryRow(row: PlaylistDetailRow, availability: PlaylistMoveAvailability, onClick: () -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    val moveUp = stringResource(Res.string.playlist_move_up_format, row.track.title)
    val moveDown = stringResource(Res.string.playlist_move_down_format, row.track.title)
    val drag = stringResource(Res.string.playlist_drag_format, row.track.title)
    val remove = stringResource(Res.string.playlist_remove_track_format, row.track.title)
    val entryState = stringResource(Res.string.playlist_entry_state)
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, HausColors.current.line, RoundedCornerShape(20.dp)).background(HausColors.current.panel.copy(alpha = .54f), RoundedCornerShape(20.dp)).hausClickable(onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "≡",
            modifier = Modifier
                .size(44.dp)
                .pointerInput(row.entry.id, availability) {
                    var dragDistance = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragDistance = 0f },
                        onDragEnd = {
                            when {
                                dragDistance < -20f && availability.canMoveUp -> onMove(-1)
                                dragDistance > 20f && availability.canMoveDown -> onMove(1)
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount.y
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
        Column(Modifier.weight(1f).semantics { stateDescription = entryState }) { Text(row.track.title, color = HausColors.current.ink, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(row.track.artist, color = HausColors.current.muted, fontSize = 12.sp, maxLines = 1) }
        IconButton(onClick = { onMove(-1) }, enabled = availability.canMoveUp, minWidth = 40.dp, minHeight = 40.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveUp }) { Text("↑", color = HausColors.current.ink) }
        IconButton(onClick = { onMove(1) }, enabled = availability.canMoveDown, minWidth = 40.dp, minHeight = 40.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = moveDown }) { Text("↓", color = HausColors.current.ink) }
        IconButton(onClick = onRemove, minWidth = 40.dp, minHeight = 40.dp, backgroundColor = Color.Transparent, modifier = Modifier.semantics { contentDescription = remove }) { Text("×", color = HausColors.current.pulse) }
    }
}

@Composable private fun CompactAction(text: String, modifier: Modifier, onClick: () -> Unit) { Button(onClick = onClick, modifier = modifier.height(44.dp), cornerRadius = 14.dp, insideMargin = PaddingValues(horizontal = 10.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(HausColors.current.panel, HausColors.current.ink)) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) } }
@Composable private fun EmptyPlaylistMessage(text: String) { Text(text, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), color = HausColors.current.muted, fontSize = 15.sp) }
@Composable private fun PlaylistNotice(state: PlaylistState) { if (state.mutationErrorMessage != null) Text(stringResource(Res.string.playlist_mutation_failed), color = HausColors.current.pulse, fontSize = 13.sp) }

@Composable
private fun PlaylistNameDialog(title: String, draft: PlaylistNameDraft, onDraftChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ModalCard(onDismiss) { Text(title, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black); PlaylistTextField(draft.enteredText) { onDraftChange(it) }; if (draft.showFailure) Text(stringResource(Res.string.playlist_create_name), color = HausColors.current.pulse, fontSize = 12.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CompactAction(stringResource(Res.string.cancel), Modifier.weight(1f), onDismiss); CompactAction(title, Modifier.weight(1f), onConfirm) } }
}

@Composable
private fun ConfirmationDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ModalCard(onDismiss) { Text(title, color = HausColors.current.ink, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(message, color = HausColors.current.muted, fontSize = 14.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CompactAction(stringResource(Res.string.cancel), Modifier.weight(1f), onDismiss); CompactAction(title, Modifier.weight(1f), onConfirm) } }
}

@Composable
private fun ModalCard(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(HausColors.current.ink.copy(alpha = .36f)).hausClickable(onDismiss).padding(24.dp), contentAlignment = Alignment.Center) { Card(modifier = Modifier.fillMaxWidth().hausClickable {}, cornerRadius = 24.dp, colors = CardDefaults.defaultColors(HausColors.current.panel)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
}

@Composable
private fun PlaylistTextField(value: String, label: String = stringResource(Res.string.playlist_create_name), onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = label, useLabelAsPlaceholder = true, singleLine = true, insideMargin = DpSize(14.dp, 12.dp), cornerRadius = 12.dp, textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp), cursorBrush = SolidColor(HausColors.current.pulse), colors = TextFieldDefaults.textFieldColors(backgroundColor = HausColors.current.paper, borderColor = HausColors.current.line, labelColor = HausColors.current.muted))
}
