package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class PlaylistTab {
    Saved,
    Queue,
}

data class PlaylistSnapshot(
    val playlists: List<Playlist> = emptyList(),
    val entriesByPlaylistId: Map<String, List<PlaylistEntry>> = emptyMap(),
) {
    fun playlist(id: String): Playlist? = playlists.firstOrNull { it.id == id }
    fun entries(id: String): List<PlaylistEntry> = entriesByPlaylistId[id].orEmpty()
}

data class PlaylistPickerState(
    val trackId: String,
    val selectedPlaylistId: String? = null,
    val enteredName: String = "",
)

data class PlaylistBrowserState(
    val playlistId: String,
    val query: String = "",
    val visibleTrackIds: List<String> = emptyList(),
    val selectedTrackIds: Set<String> = emptySet(),
)

data class PlaylistState(
    val selectedTab: PlaylistTab = PlaylistTab.Saved,
    val confirmedSnapshot: PlaylistSnapshot = PlaylistSnapshot(),
    val isLoading: Boolean = false,
    val readErrorMessage: String? = null,
    val mutationErrorMessage: String? = null,
    val recoverableMessage: String? = null,
    val picker: PlaylistPickerState? = null,
    val browser: PlaylistBrowserState? = null,
)

sealed interface PlaylistStateAction {
    data object LoadStarted : PlaylistStateAction
    data class SnapshotConfirmed(val snapshot: PlaylistSnapshot) : PlaylistStateAction
    data class ReadFailed(val message: String) : PlaylistStateAction
    data class MutationFailed(val message: String) : PlaylistStateAction
    data class SelectTab(val tab: PlaylistTab) : PlaylistStateAction
    data class ShowRecoverableMessage(val message: String) : PlaylistStateAction
    data class OpenPicker(val picker: PlaylistPickerState) : PlaylistStateAction
    data object ClosePicker : PlaylistStateAction
    data class OpenBrowser(val browser: PlaylistBrowserState) : PlaylistStateAction
    data object CloseBrowser : PlaylistStateAction
    data object ClearMessages : PlaylistStateAction
}

fun reducePlaylistState(state: PlaylistState, action: PlaylistStateAction): PlaylistState = when (action) {
    PlaylistStateAction.LoadStarted -> state.copy(isLoading = true, readErrorMessage = null)
    is PlaylistStateAction.SnapshotConfirmed -> state.copy(
        confirmedSnapshot = action.snapshot,
        isLoading = false,
        readErrorMessage = null,
        mutationErrorMessage = null,
        recoverableMessage = null,
    )
    is PlaylistStateAction.ReadFailed -> state.copy(isLoading = false, readErrorMessage = action.message)
    is PlaylistStateAction.MutationFailed -> state.copy(mutationErrorMessage = action.message)
    is PlaylistStateAction.SelectTab -> state.copy(selectedTab = action.tab)
    is PlaylistStateAction.ShowRecoverableMessage -> state.copy(recoverableMessage = action.message)
    is PlaylistStateAction.OpenPicker -> state.copy(picker = action.picker)
    PlaylistStateAction.ClosePicker -> state.copy(picker = null)
    is PlaylistStateAction.OpenBrowser -> state.copy(browser = action.browser)
    PlaylistStateAction.CloseBrowser -> state.copy(browser = null)
    PlaylistStateAction.ClearMessages -> state.copy(
        readErrorMessage = null,
        mutationErrorMessage = null,
        recoverableMessage = null,
    )
}

enum class PlaylistRouteNotice {
    PlaylistChanged,
    MutationFailed,
}

fun playlistRouteNotice(state: PlaylistState): PlaylistRouteNotice? = when {
    state.recoverableMessage != null -> PlaylistRouteNotice.PlaylistChanged
    state.mutationErrorMessage != null -> PlaylistRouteNotice.MutationFailed
    else -> null
}

sealed interface PlaylistDetailResolution {
    data class Show(val playlist: Playlist) : PlaylistDetailResolution
    data class ReturnToHub(val message: String) : PlaylistDetailResolution
}

fun playlistDetailResolution(
    playlistId: String,
    resolvedPlaylist: Playlist?,
): PlaylistDetailResolution = if (resolvedPlaylist?.id == playlistId) {
    PlaylistDetailResolution.Show(resolvedPlaylist)
} else {
    PlaylistDetailResolution.ReturnToHub(PlaylistChangedMessage)
}

fun loadPlaylistSnapshot(repository: PlaylistRepository): PlaylistSnapshot {
    val playlists = repository.playlists()
    return PlaylistSnapshot(
        playlists = playlists,
        entriesByPlaylistId = playlists.associate { it.id to repository.entries(it.id) },
    )
}

fun mutatePlaylistAndRefresh(
    repository: PlaylistRepository,
    mutation: PlaylistRepository.() -> Unit,
): PlaylistSnapshot {
    repository.mutation()
    return loadPlaylistSnapshot(repository)
}

class PlaylistStateOwner(
    private val repository: PlaylistRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    suspend fun refresh(): PlaylistSnapshot = mutex.withLock {
        withContext(dispatcher) { loadPlaylistSnapshot(repository) }
    }

    suspend fun mutate(mutation: PlaylistRepository.() -> Unit): PlaylistSnapshot = mutex.withLock {
        withContext(dispatcher) { mutatePlaylistAndRefresh(repository, mutation) }
    }
}

fun savedPlaylistOccurrences(
    visibleEntries: List<PlaylistEntry>,
    tracksById: Map<String, PlayableTrack>,
): List<QueueOccurrence> = visibleEntries.mapNotNull { entry ->
    tracksById[entry.trackId]?.let { track -> QueueOccurrence(id = entry.id, track = track) }
}

fun savedPlaylistRowKeys(visibleEntries: List<PlaylistEntry>): List<String> = visibleEntries.map(PlaylistEntry::id)

const val PlaylistChangedMessage = "playlist_changed"
