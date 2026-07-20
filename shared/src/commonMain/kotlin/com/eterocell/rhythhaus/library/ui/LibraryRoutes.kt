package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.selectOccurrenceForPlayback
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.taglib.TagLibReader
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_detail_subtitle_format
import rhythhaus.shared.generated.resources.artist_detail_subtitle_format
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.playlist_load_failed
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.playlist_retry
import rhythhaus.shared.generated.resources.playlist_changed
import rhythhaus.shared.generated.resources.playlist_mutation_failed
import rhythhaus.shared.generated.resources.unknown_artist
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.search.SearchScreen
import com.eterocell.rhythhaus.settings.OpenSourceLibrariesScreen
import com.eterocell.rhythhaus.settings.SettingsAboutScreen
import com.eterocell.rhythhaus.settings.SettingsScreen
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.theme.HausColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import kotlinx.coroutines.flow.StateFlow
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState

internal class QueueMutationDispatcher(
    private val state: StateFlow<PlaybackState>,
    private val reorderCommand: suspend (String, Int) -> QueueMutationResult,
    private val removeCommand: suspend (String) -> QueueMutationResult,
    private val clearCommand: suspend () -> QueueMutationResult,
) {
    suspend fun reorder(occurrenceId: String, targetIndex: Int): QueueMutationFeedback =
        executeQueueMutation(state) { reorderCommand(occurrenceId, targetIndex) }

    suspend fun remove(occurrenceId: String): QueueMutationFeedback =
        executeQueueMutation(state) { removeCommand(occurrenceId) }

    suspend fun clear(): QueueMutationFeedback = executeQueueMutation(state, clearCommand)
}

@Composable
internal fun LibraryRouteOverlays(
    route: LibraryRoute,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    playlistRepository: PlaylistRepository,
    playlistState: PlaylistState,
    playlistBackupState: PlaylistBackupUiState,
    backupDocumentAvailable: Boolean,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation: PlaylistMutationLauncher,
    onExportPlaylists: () -> Unit,
    onOpenPlaylistBackup: () -> Unit,
    onConfirmPlaylistBackup: () -> Unit,
    onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onCancelScan: () -> Unit,
    onShowSettingsAbout: () -> Unit,
    onShowOpenSourceLibraries: () -> Unit,
    onDismiss: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    when (route) {
        LibraryRoute.Settings -> SettingsScreen(
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            hasImportedTracks = snapshot.tracks.isNotEmpty(),
            currentThemeMode = currentThemeMode,
            playlistBackupState = playlistBackupState,
            backupDocumentAvailable = backupDocumentAvailable,
            onExportPlaylists = onExportPlaylists,
            onOpenPlaylistBackup = onOpenPlaylistBackup,
            onConfirmPlaylistBackup = onConfirmPlaylistBackup,
            onPlaylistBackupAction = onPlaylistBackupAction,
            onThemeModeSelected = onThemeModeSelected,
            onClearLibrary = onClearLibrary,
            onRescanSource = onRescanSource,
            onRemoveSource = onRemoveSource,
            onCancelScan = onCancelScan,
            onAboutClick = onShowSettingsAbout,
            onDismiss = onDismiss,
        )

        LibraryRoute.Search -> SearchScreen(
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = playbackController,
            playbackState = playbackState,
            onDismiss = onDismiss,
            onScrollPositionChanged = onScrollPositionChanged,
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = onTrackSelectionAction,
            bottomContentPadding = bottomContentPadding,
        )

        LibraryRoute.SettingsAbout -> SettingsAboutScreen(
            onOpenLibraries = onShowOpenSourceLibraries,
            onDismiss = onDismiss,
        )
        LibraryRoute.OpenSourceLibraries -> OpenSourceLibrariesScreen(
            onDismiss = onDismiss,
        )
        LibraryRoute.Home,
        is LibraryRoute.AlbumDetail,
        is LibraryRoute.ArtistDetail,
        LibraryRoute.NowPlaying,
        LibraryRoute.ClearLibraryDialog,
        LibraryRoute.PlaylistHub,
        is LibraryRoute.PlaylistDetail,
        -> Unit
    }
}

@Composable
internal fun LibraryRouteContent(
    route: LibraryRoute,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    playlistRepository: PlaylistRepository,
    playlistState: PlaylistState,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation: PlaylistMutationLauncher,
    onRecoverStalePlaylistDetail: (String) -> Unit,
    selectedTrackId: String?,
    isNowPlayingBarVisible: Boolean,
    onBack: () -> Unit,
    registerPlaylistEditMode: (Any, () -> Unit) -> () -> Unit = { _, _ -> {} },
    registerPlaylistModalDismiss: ((() -> Unit)?) -> () -> Unit = { {} },
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
    onTrackClickFromTracks: (List<Track>, Track) -> Unit,
    onExpandNowPlaying: (Track) -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    homeContent: @Composable ((LibraryRoute) -> Unit) -> Unit,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    when (route) {
        is LibraryRoute.AlbumDetail -> {
            val album = albums.firstOrNull { it.album == route.album }
            if (album == null) {
                LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                val albumTracks = album.tracks
                val selectedAlbumTrackId by remember(album.album) { mutableStateOf(albumTracks.firstOrNull()?.id) }
                val selectedAlbumTrack = albumTracks.firstOrNull { it.id == selectedAlbumTrackId } ?: albumTracks.firstOrNull()
                DrillDownView(
                    title = album.album,
                    subtitle = stringResource(Res.string.album_detail_subtitle_format, albumTracks.size, album.artist ?: stringResource(Res.string.unknown_artist)),
                    tracks = albumTracks,
                    topBarArtworkTrack = albumTracks.firstOrNull(),
                    selectedTrack = selectedAlbumTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = onBack,
                    onTrackClick = { track ->
                        onTrackSelected(track.id)
                        onTrackClickFromTracks(albumTracks, track)
                    },
                    onPlayPause = playbackController::togglePlayPause,
                    selectionPageKey = TrackSelectionPageKey.Album(album.album),
                    trackSelectionState = trackSelectionState,
                    onTrackSelectionAction = onTrackSelectionAction,
                    bottomContentPadding = bottomContentPadding,
                    onScrollPositionChanged = onScrollPositionChanged,
                )
            }
        }

        is LibraryRoute.ArtistDetail -> {
            val artist = artists.firstOrNull { it.artist == route.artist }
            if (artist == null) {
                LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                val artistTracks = artist.tracks
                val selectedArtistTrackId by remember(artist.artist) { mutableStateOf(artistTracks.firstOrNull()?.id) }
                val selectedArtistTrack = artistTracks.firstOrNull { it.id == selectedArtistTrackId } ?: artistTracks.firstOrNull()
                DrillDownView(
                    title = artist.artist,
                    subtitle = stringResource(Res.string.artist_detail_subtitle_format, artist.albumCount, artistTracks.size),
                    tracks = artistTracks,
                    topBarArtworkTrack = artistTracks.firstOrNull(),
                    selectedTrack = selectedArtistTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = onBack,
                    onTrackClick = { track ->
                        onTrackSelected(track.id)
                        onTrackClickFromTracks(artistTracks, track)
                    },
                    onPlayPause = playbackController::togglePlayPause,
                    selectionPageKey = TrackSelectionPageKey.Artist(artist.artist),
                    trackSelectionState = trackSelectionState,
                    onTrackSelectionAction = onTrackSelectionAction,
                    bottomContentPadding = bottomContentPadding,
                    onScrollPositionChanged = onScrollPositionChanged,
                )
            }
        }

        LibraryRoute.NowPlaying -> {
            // Now Playing is shown as an overlay, not a navigation route
        }

        LibraryRoute.PlaylistHub -> {
            val queueMutations = QueueMutationDispatcher(
                state = playbackController.state,
                reorderCommand = playbackController::reorderUpcoming,
                removeCommand = playbackController::removeUpcoming,
                clearCommand = playbackController::clearUpcoming,
            )
            PlaylistHubScreen(
                state = playlistState,
                playbackState = playbackState,
                onBack = onBack,
                onRetry = onRefreshPlaylists,
                onOpenPlaylist = { onOpenDetailRoute(LibraryRoute.PlaylistDetail(it)) },
                onSelectTab = { onPlaylistStateAction(PlaylistStateAction.SelectTab(it)) },
                onCreate = { name, onSuccess ->
                    onPlaylistMutation({ create(name) }, onSuccess)
                },
                onReorderUpcoming = queueMutations::reorder,
                onRemoveUpcoming = queueMutations::remove,
                onClearUpcoming = queueMutations::clear,
                bottomContentPadding = bottomContentPadding,
            )
        }

        is LibraryRoute.PlaylistDetail -> {
            when (val resolution = playlistDetailResolution(route.playlistId, playlistState)) {
                PlaylistDetailResolution.AwaitConfirmation -> PlaylistRoutePlaceholder(
                    title = stringResource(Res.string.playlists),
                    state = playlistState,
                    onBack = onBack,
                    onRetry = onRefreshPlaylists,
                )
                is PlaylistDetailResolution.Show -> PlaylistDetailScreen(
                    playlist = resolution.playlist,
                    entries = playlistState.confirmedSnapshot.entries(resolution.playlist.id),
                    libraryTracks = libraryTracks,
                    state = playlistState,
                    onBack = onBack,
                    onRetry = onRefreshPlaylists,
                    onRename = { name, onSuccess ->
                        onPlaylistMutation({ rename(resolution.playlist.id, name) }, onSuccess)
                    },
                    onDelete = { onOutcome ->
                        onPlaylistMutation(
                            { delete(resolution.playlist.id) },
                            { outcome ->
                                onOutcome(outcome)
                                if (playlistMutationDecision(PlaylistMutationWorkflow.Delete, outcome) == PlaylistMutationDecision.CloseConfirmationAndRoute) {
                                    onBack()
                                }
                            },
                        )
                    },
                    onOpenBrowser = {
                        onPlaylistStateAction(
                            PlaylistStateAction.OpenBrowser(
                                PlaylistBrowserState(playlistId = resolution.playlist.id),
                            ),
                        )
                    },
                    onPlayEntry = { request ->
                        selectOccurrenceForPlayback(
                            playbackController,
                            request.occurrences,
                            request.selectedOccurrenceId,
                        )
                    },
                    onRemoveEntry = { entryId ->
                        onPlaylistMutation({ removeEntry(entryId) }) { outcome ->
                            playlistMutationDecision(PlaylistMutationWorkflow.Remove, outcome)
                        }
                    },
                    onReorder = { entryIds ->
                        onPlaylistMutation({ reorder(resolution.playlist.id, entryIds) }) { outcome ->
                            playlistMutationDecision(PlaylistMutationWorkflow.Reorder, outcome)
                        }
                    },
                    bottomContentPadding = bottomContentPadding,
                    registerPlaylistEditMode = registerPlaylistEditMode,
                    registerPlaylistModalDismiss = registerPlaylistModalDismiss,
                )
                is PlaylistDetailResolution.ReturnToHub -> LaunchedEffect(route) {
                    onRecoverStalePlaylistDetail(resolution.message)
                }
            }
        }

        LibraryRoute.Home,
        LibraryRoute.Settings,
        LibraryRoute.SettingsAbout,
        LibraryRoute.OpenSourceLibraries,
        LibraryRoute.Search,
        -> {
            homeContent(onOpenDetailRoute)
        }

        LibraryRoute.ClearLibraryDialog -> {
            LaunchedEffect(route) { onBack() }
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlaylistRoutePlaceholder(
    title: String,
    state: PlaylistState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val scrollBehavior = rememberMiuixTopAppBarScrollBehavior()
    val retryLabel = stringResource(Res.string.playlist_retry)
    Box(modifier = Modifier.fillMaxSize()) {
        DrillDownMiuixScrollChrome(
            scrollBehavior = scrollBehavior,
            title = title,
            onBack = onBack,
            backdrop = null,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                state.isLoading -> Text(
                    text = stringResource(Res.string.playlist_loading),
                    color = HausColors.current.muted,
                )
                state.readErrorMessage != null -> {
                    Text(
                        text = stringResource(Res.string.playlist_load_failed),
                        color = HausColors.current.muted,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(48.dp)
                            .semantics { contentDescription = retryLabel },
                        cornerRadius = 16.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.ink,
                            contentColor = HausColors.current.paper,
                        ),
                    ) {
                        Text(retryLabel)
                    }
                }
            }
            when (playlistRouteNotice(state)) {
                PlaylistRouteNotice.PlaylistChanged -> Text(
                    text = stringResource(Res.string.playlist_changed),
                    color = HausColors.current.muted,
                    modifier = Modifier.padding(top = 12.dp),
                )
                PlaylistRouteNotice.MutationFailed -> Text(
                    text = stringResource(Res.string.playlist_mutation_failed),
                    color = HausColors.current.muted,
                    modifier = Modifier.padding(top = 12.dp),
                )
                null -> Unit
            }
        }
    }
}
