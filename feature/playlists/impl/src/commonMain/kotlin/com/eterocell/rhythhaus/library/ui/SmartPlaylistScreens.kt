package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

@Composable
private fun SmartChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selection =
        stringResource(
            if (selected) Res.string.smart_selected
            else Res.string.smart_not_selected)
    CompactAction(
        label,
        modifier.fillMaxWidth().height(44.dp).semantics {
            role = Role.RadioButton
            stateDescription = selection
        },
        onClick)
}

internal data class SmartDraft(
    val appearance: PlaylistDismissalAppearance,
    val name: String = "",
    val rule: SmartPlaylistRule = SmartPlaylistRule.Favorites,
    val pending: Boolean = false,
    val failed: Boolean = false
)

@Composable
internal fun SmartRuleEditor(
    rule: SmartPlaylistRule,
    libraryTracks: List<LibraryTrack>,
    snapshot: PlaylistSnapshot,
    onRule: (SmartPlaylistRule) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.smart_rule))
        listOf(
                stringResource(Res.string.smart_favorites) to
                    SmartPlaylistRule.Favorites,
                stringResource(Res.string.smart_recent_played) to
                    SmartPlaylistRule.RecentlyPlayed(25),
                stringResource(Res.string.smart_recent_added) to
                    SmartPlaylistRule.RecentlyAdded(25),
                stringResource(Res.string.smart_artist) to
                    SmartPlaylistRule.Artist(
                        libraryTracks
                            .mapNotNull { it.artist }
                            .distinct()
                            .sorted()
                            .firstOrNull()
                            .orEmpty()),
                stringResource(Res.string.smart_album) to
                    libraryTracks
                        .firstOrNull()
                        ?.let { SmartPlaylistRule.Album(it.artist, it.album) }
                        .let { it ?: SmartPlaylistRule.Album("", "") },
                stringResource(Res.string.smart_saved) to
                    SmartPlaylistRule.SavedPlaylist(
                        snapshot.playlists.firstOrNull()?.id.orEmpty()),
            )
            .forEach { (label, candidate) ->
                SmartChoice(label, rule::class == candidate::class) {
                    onRule(candidate)
                }
            }
        when (rule) {
            is SmartPlaylistRule.RecentlyPlayed,
            is SmartPlaylistRule.RecentlyAdded -> {
                Text(stringResource(Res.string.smart_count))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 25, 50).forEach { count ->
                        SmartChoice(
                            count.toString(),
                            (rule as? SmartPlaylistRule.RecentlyPlayed)
                                ?.count == count ||
                                (rule as? SmartPlaylistRule.RecentlyAdded)
                                    ?.count == count,
                            Modifier.weight(1f)) {
                                onRule(
                                    if (rule
                                        is SmartPlaylistRule.RecentlyPlayed)
                                        SmartPlaylistRule.RecentlyPlayed(count)
                                    else SmartPlaylistRule.RecentlyAdded(count))
                            }
                    }
                }
            }
            is SmartPlaylistRule.Artist -> {
                (libraryTracks.mapNotNull { it.artist }.distinct() +
                        rule.artist)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .forEach { artist ->
                        SmartChoice(artist, rule.artist == artist) {
                            onRule(SmartPlaylistRule.Artist(artist))
                        }
                    }
            }
            is SmartPlaylistRule.Album -> {
                val available =
                    libraryTracks
                        .mapNotNull { track ->
                            val artist = track.artist
                            val album = track.album
                            artist to album
                        }
                        .distinct() + (rule.artist to rule.album)
                available
                    .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                    .distinct()
                    .sortedWith(compareBy({ it.first }, { it.second }))
                    .forEach { (artist, album) ->
                        SmartChoice(
                            "$artist — $album",
                            rule.artist == artist && rule.album == album) {
                                onRule(SmartPlaylistRule.Album(artist, album))
                            }
                    }
            }
            is SmartPlaylistRule.SavedPlaylist -> {
                (snapshot.playlists.map { it.id to it.name } +
                        (rule.playlistId to
                            (snapshot.playlist(rule.playlistId)?.name
                                ?: rule.playlistId)))
                    .distinctBy { it.first }
                    .filter { it.first.isNotBlank() }
                    .forEach { (id, name) ->
                        SmartChoice(name, rule.playlistId == id) {
                            onRule(SmartPlaylistRule.SavedPlaylist(id))
                        }
                    }
                if (snapshot.playlist(rule.playlistId) == null &&
                    rule.playlistId.isNotBlank())
                    Text(stringResource(Res.string.smart_missing_source))
            }
            else -> Unit
        }
    }
}

internal fun validSmartDraft(
    draft: SmartDraft,
    snapshot: PlaylistSnapshot,
    libraryTracks: List<LibraryTrack>,
    originalRule: SmartPlaylistRule? = null,
): Boolean =
    when (val rule = draft.rule) {
        SmartPlaylistRule.Favorites -> true
        is SmartPlaylistRule.RecentlyPlayed -> rule.count in setOf(10, 25, 50)
        is SmartPlaylistRule.RecentlyAdded -> rule.count in setOf(10, 25, 50)
        is SmartPlaylistRule.Artist ->
            (originalRule == rule && rule.artist.isNotBlank()) ||
                libraryTracks.any { it.artist == rule.artist }
        is SmartPlaylistRule.Album ->
            (originalRule == rule &&
                rule.artist.isNotBlank() &&
                rule.album.isNotBlank()) ||
                libraryTracks.any {
                    it.artist == rule.artist && it.album == rule.album
                }
        is SmartPlaylistRule.SavedPlaylist ->
            (originalRule == rule && rule.playlistId.isNotBlank()) ||
                snapshot.playlist(rule.playlistId) != null
    } && draft.name.isNotBlank()

@Composable
internal fun SmartRuleDialog(
    draft: SmartDraft,
    libraryTracks: List<LibraryTrack>,
    snapshot: PlaylistSnapshot,
    title: String,
    onChange: (SmartDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        body = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title)
                    val nameLabel =
                        stringResource(Res.string.playlist_create_name)
                    TextField(
                        value = draft.name,
                        onValueChange = {
                            onChange(draft.copy(name = it, failed = false))
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag("smart-playlist-name")
                                .semantics { contentDescription = nameLabel },
                        label = nameLabel,
                        useLabelAsPlaceholder = true,
                        singleLine = true)
                    SmartRuleEditor(draft.rule, libraryTracks, snapshot) {
                        onChange(draft.copy(rule = it, failed = false))
                    }
                    if (draft.failed)
                        Text(
                            stringResource(
                                Res.string.playlist_modal_mutation_failed),
                            color = HausColors.current.pulse)
                }
        },
        actions = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactAction(
                        stringResource(Res.string.playlist_cancel),
                        Modifier.weight(1f),
                        onDismiss)
                    CompactAction(
                        stringResource(Res.string.smart_save),
                        Modifier.weight(1f),
                        onConfirm)
                }
        })
}

/**
 * Renders live smart-rule members; playback snapshots the currently visible
 * occurrences.
 */
@Composable
public fun SmartPlaylistDetailScreen(
    playlist: SmartPlaylistSummary,
    projection: SmartPlaylistProjection,
    playableTracksById: Map<String, PlayableTrack>,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    mutationFailedLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUpdate:
        (String, SmartPlaylistRule, (PlaylistStateAction) -> Unit) -> Unit,
    onDelete: ((PlaylistStateAction) -> Unit) -> Unit,
    onDeleteConfirmed: (PlaylistSnapshot) -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    var draft by remember(playlist.id) { mutableStateOf<SmartDraft?>(null) }
    var deleting by
        remember(playlist.id) {
            mutableStateOf<PlaylistDismissalAppearance?>(null)
        }
    var deletePending by remember(playlist.id) { mutableStateOf(false) }
    var deleteFailed by remember(playlist.id) { mutableStateOf(false) }
    val appearance = draft?.appearance ?: deleting
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        appearance?.let { PlaylistFeatureDismissal.Modal(destination, it) }) {
            target ->
            if (draft?.appearance == target.appearance) draft = null
            if (deleting == target.appearance) deleting = null
        }
    val visible =
        projection.rows.mapNotNull { row ->
            playableTracksById[row.trackId]?.let {
                QueueOccurrence(row.occurrenceId, it)
            }
        }
    PlaylistScreenFrame(
        title = playlist.name,
        onBack = onBack,
        beforeList = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactAction(
                        stringResource(Res.string.smart_edit),
                        Modifier.weight(1f).height(44.dp)) {
                            draft =
                                SmartDraft(
                                    appearanceSource.next("smart-edit"),
                                    playlist.name,
                                    playlist.rule)
                        }
                    CompactAction(
                        stringResource(Res.string.smart_delete),
                        Modifier.weight(1f).height(44.dp)) {
                            deleting = appearanceSource.next("smart-delete")
                            deletePending = false
                            deleteFailed = false
                        }
                }
        }) {
            if (projection.sourceMissing)
                item { Text(stringResource(Res.string.smart_missing_source)) }
            if (visible.isEmpty())
                item {
                    EmptyPlaylistMessage(stringResource(Res.string.smart_empty))
                }
            items(visible, key = QueueOccurrence::id) { row ->
                CompactAction(
                    row.track.title,
                    Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag("smart-playlist-row-${row.id}")
                        .semantics {
                            contentDescription =
                                "${row.track.title}, ${row.track.artist.orEmpty()}, ${row.track.album.orEmpty()}"
                        }) {
                        onPlayEntry(
                            SavedPlaylistPlaybackRequest(visible, row.id))
                    }
            }
            if (state.readErrorMessage != null)
                item { ReadFailureNotice(onRetry) }
            item { PlaylistNotice(state, mutationFailedLabel) }
            item {
                androidx.compose.foundation.layout.Spacer(
                    Modifier.height(bottomContentPadding))
            }
        }
    draft?.let { current ->
        SmartRuleDialog(
            current,
            libraryTracks,
            state.confirmedSnapshot,
            stringResource(Res.string.smart_edit),
            { updated ->
                if (draft?.appearance == current.appearance &&
                    draft?.pending == false)
                    draft = updated
            },
            { if (draft?.appearance == current.appearance) draft = null },
            {
                if (draft?.appearance == current.appearance &&
                    draft?.pending == false) {
                    if (!validSmartDraft(
                        current,
                        state.confirmedSnapshot,
                        libraryTracks,
                        playlist.rule))
                        draft = current.copy(failed = true)
                    else {
                        draft = current.copy(pending = true)
                        onUpdate(current.name.trim(), current.rule) { result ->
                            if (draft?.appearance == current.appearance)
                                draft =
                                    if (result
                                        is
                                        PlaylistStateAction.SnapshotConfirmed)
                                        null
                                    else current.copy(failed = true)
                        }
                    }
                }
            })
    }
    deleting?.let { current ->
        HausDialog(
            title = stringResource(Res.string.smart_delete),
            onDismiss = { if (deleting == current) deleting = null },
            body = {
                Column {
                    Text(stringResource(Res.string.smart_delete_confirm))
                    if (deleteFailed)
                        Text(
                            stringResource(
                                Res.string.playlist_modal_mutation_failed))
                }
            },
            actions = {
                Row {
                    CompactAction(
                        stringResource(Res.string.playlist_cancel),
                        Modifier.weight(1f),
                        { if (deleting == current) deleting = null })
                    CompactAction(
                        stringResource(Res.string.smart_delete),
                        Modifier.weight(1f),
                        {
                            if (deleting == current && !deletePending) {
                                deletePending = true
                                onDelete { outcome ->
                                    if (deleting == current) {
                                        deletePending = false
                                        if (outcome is
                                            PlaylistStateAction.SnapshotConfirmed &&
                                            outcome.snapshot.smartPlaylist(
                                                playlist.id) == null) {
                                            deleting = null
                                            onDeleteConfirmed(outcome.snapshot)
                                        } else deleteFailed = true
                                    }
                                }
                            }
                        })
                }
            })
    }
}
