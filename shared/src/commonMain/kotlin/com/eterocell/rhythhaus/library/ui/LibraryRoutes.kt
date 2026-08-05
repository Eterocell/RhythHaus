package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueMutationResult
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.selectOccurrenceForPlayback
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.search.SearchScreen
import com.eterocell.rhythhaus.settings.OpenSourceLibrariesScreen
import com.eterocell.rhythhaus.settings.SettingsAboutScreen
import com.eterocell.rhythhaus.settings.SettingsScreen
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_detail_subtitle_format
import rhythhaus.shared.generated.resources.artist_detail_subtitle_format
import rhythhaus.shared.generated.resources.playlist_changed
import rhythhaus.shared.generated.resources.playlist_load_failed
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.playlist_mutation_failed
import rhythhaus.shared.generated.resources.playlist_retry
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.unknown_artist
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

internal class QueueMutationDispatcher(
    private val state: StateFlow<PlaybackState>,
    private val reorderCommand: suspend (String, Int) -> QueueMutationResult,
    private val removeCommand: suspend (String) -> QueueMutationResult,
    private val clearCommand: suspend () -> QueueMutationResult,
) {
    suspend fun reorder(
        occurrenceId: String,
        targetIndex: Int
    ): QueueMutationFeedback = mutation {
        reorderCommand(occurrenceId, targetIndex)
    }

    suspend fun remove(occurrenceId: String): QueueMutationFeedback = mutation {
        removeCommand(occurrenceId)
    }

    suspend fun clear(): QueueMutationFeedback = mutation(clearCommand)

    private suspend fun mutation(
        command: suspend () -> QueueMutationResult
    ): QueueMutationFeedback {
        val result = command()
        return QueueMutationFeedback(
            refreshedState = state.value,
            showQueueChanged = result is QueueMutationResult.Rejected,
        )
    }
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
    destinationId: LibraryDestinationId,
    playlistAppearanceSource: PlaylistFeatureAppearanceSource,
    registerBackSurface: (LibraryBackSurfacePort) -> () -> Unit,
    onPlaylistStateAction: (PlaylistStateAction) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
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
        LibraryRoute.Settings ->
            SettingsScreen(
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
                destination =
                    PlaylistFeatureDestination(destinationId.instanceToken),
                appearanceSource = playlistAppearanceSource,
                dismissalPublisher =
                    featureDismissalPublisher(
                        destinationId, registerBackSurface),
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

        LibraryRoute.Search ->
            SearchScreen(
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

        LibraryRoute.SettingsAbout ->
            SettingsAboutScreen(
                onOpenLibraries = onShowOpenSourceLibraries,
                onDismiss = onDismiss,
            )

        LibraryRoute.OpenSourceLibraries ->
            OpenSourceLibrariesScreen(
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
    onPlaylistMutation:
        (PlaylistRepository.() -> Unit, (PlaylistStateAction) -> Unit) -> Unit,
    onRecoverStalePlaylistDetail: (String) -> Unit,
    onDisplayedPlaylistDeleteConfirmed: (PlaylistSnapshot) -> Unit = {},
    selectedTrackId: String?,
    isNowPlayingBarVisible: Boolean,
    onBack: () -> Unit,
    destinationId: LibraryDestinationId? = null,
    playlistAppearanceSource: PlaylistFeatureAppearanceSource,
    registerBackSurface: (LibraryBackSurfacePort) -> () -> Unit = { {} },
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
    val playlistDestinationId =
        destinationId ?: LibraryDestinationId(route, "unpresented")
    val playlistDestination =
        PlaylistFeatureDestination(playlistDestinationId.instanceToken)
    val playlistDismissalPublisher =
        featureDismissalPublisher(playlistDestinationId, registerBackSurface)
    when (route) {
        is LibraryRoute.AlbumDetail -> {
            val album = albums.firstOrNull { it.album == route.album }
            if (album == null) {
                LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                val albumTracks = album.tracks
                val selectedAlbumTrackId by
                    remember(album.album) {
                        mutableStateOf(albumTracks.firstOrNull()?.id)
                    }
                val selectedAlbumTrack =
                    albumTracks.firstOrNull { it.id == selectedAlbumTrackId }
                        ?: albumTracks.firstOrNull()
                DrillDownView(
                    title = album.album,
                    subtitle =
                        stringResource(
                            Res.string.album_detail_subtitle_format,
                            albumTracks.size,
                            album.artist
                                ?: stringResource(Res.string.unknown_artist)),
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
                val selectedArtistTrackId by
                    remember(artist.artist) {
                        mutableStateOf(artistTracks.firstOrNull()?.id)
                    }
                val selectedArtistTrack =
                    artistTracks.firstOrNull { it.id == selectedArtistTrackId }
                        ?: artistTracks.firstOrNull()
                DrillDownView(
                    title = artist.artist,
                    subtitle =
                        stringResource(
                            Res.string.artist_detail_subtitle_format,
                            artist.albumCount,
                            artistTracks.size),
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
                    selectionPageKey =
                        TrackSelectionPageKey.Artist(artist.artist),
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
            val queueMutations =
                QueueMutationDispatcher(
                    state = playbackController.state,
                    reorderCommand = playbackController::reorderUpcoming,
                    removeCommand = playbackController::removeUpcoming,
                    clearCommand = playbackController::clearUpcoming,
                )
            PlaylistHubScreen(
                state = playlistState,
                playbackState = playbackState,
                destination = playlistDestination,
                appearanceSource = playlistAppearanceSource,
                dismissalPublisher = playlistDismissalPublisher,
                playlistsLabel = stringResource(Res.string.playlists),
                loadingLabel = stringResource(Res.string.playlist_loading),
                loadFailedLabel =
                    stringResource(Res.string.playlist_load_failed),
                retryLabel = stringResource(Res.string.playlist_retry),
                mutationFailedLabel =
                    stringResource(Res.string.playlist_mutation_failed),
                onBack = onBack,
                onRetry = onRefreshPlaylists,
                onOpenPlaylist = {
                    onOpenDetailRoute(LibraryRoute.PlaylistDetail(it))
                },
                onSelectTab = {
                    onPlaylistStateAction(PlaylistStateAction.SelectTab(it))
                },
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
            when (val resolution =
                sharedPlaylistDetailResolution(
                    route.playlistId, playlistState)) {
                SharedPlaylistDetailResolution.AwaitConfirmation ->
                    PlaylistRoutePlaceholder(
                        title = stringResource(Res.string.playlists),
                        state = playlistState,
                        onBack = onBack,
                        onRetry = onRefreshPlaylists,
                    )

                is SharedPlaylistDetailResolution.Show ->
                    PlaylistDetailRouteContent(
                        playlist = resolution.playlist,
                        entries =
                            playlistState.confirmedSnapshot.entries(
                                resolution.playlist.id),
                        libraryTracks = libraryTracks,
                        state = playlistState,
                        destination = playlistDestination,
                        appearanceSource = playlistAppearanceSource,
                        dismissalPublisher = playlistDismissalPublisher,
                        mutationFailedLabel =
                            stringResource(Res.string.playlist_mutation_failed),
                        onBack = onBack,
                        onRetry = onRefreshPlaylists,
                        onRename = { name, onSuccess ->
                            onPlaylistMutation(
                                { rename(resolution.playlist.id, name) },
                                onSuccess)
                        },
                        onDeleteMutation = { onOutcome ->
                            onPlaylistMutation(
                                { delete(resolution.playlist.id) },
                                { outcome ->
                                    onOutcome(outcome)
                                },
                            )
                        },
                        onDisplayedPlaylistDeleteConfirmed =
                            onDisplayedPlaylistDeleteConfirmed,
                        onOpenBrowser = {
                            onPlaylistStateAction(
                                PlaylistStateAction.OpenBrowser(
                                    PlaylistBrowserState(
                                        playlistId = resolution.playlist.id),
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
                            onPlaylistMutation({ removeEntry(entryId) }) {}
                        },
                        onReorder = { entryIds ->
                            onPlaylistMutation({
                                reorder(resolution.playlist.id, entryIds)
                            }) {}
                        },
                        bottomContentPadding = bottomContentPadding,
                        onScrollPositionChanged = onScrollPositionChanged,
                    )

                is SharedPlaylistDetailResolution.ReturnToHub ->
                    PlaylistDetailRouteResolutionEffect(
                        route = route,
                        state = playlistState,
                        onRecoverStalePlaylistDetail =
                            onRecoverStalePlaylistDetail,
                    )
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

/** The actual stale-route recovery seam used by [LibraryRouteContent]. */
@Composable
internal fun PlaylistDetailRouteResolutionEffect(
    route: LibraryRoute.PlaylistDetail,
    state: PlaylistState,
    onRecoverStalePlaylistDetail: (String) -> Unit,
) {
    val resolution = sharedPlaylistDetailResolution(route.playlistId, state)
    if (resolution is SharedPlaylistDetailResolution.ReturnToHub) {
        LaunchedEffect(route, state.publicationRevision) {
            onRecoverStalePlaylistDetail(resolution.message)
        }
    }
}

/**
 * Production seam for the displayed playlist-detail route. A confirmed deletion
 * is routed only through [onDisplayedPlaylistDeleteConfirmed]; stale-detail
 * recovery remains a separate route resolution path in [LibraryRouteContent].
 */
@Composable
internal fun PlaylistDetailRouteContent(
    playlist: PlaylistSummary,
    entries: List<PlaylistEntry>,
    libraryTracks: List<LibraryTrack>,
    state: PlaylistState,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    mutationFailedLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRename: (String, (PlaylistStateAction) -> Unit) -> Unit,
    onDeleteMutation: ((PlaylistStateAction) -> Unit) -> Unit,
    onDisplayedPlaylistDeleteConfirmed: (PlaylistSnapshot) -> Unit,
    onOpenBrowser: () -> Unit,
    onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit = {},
) {
    PlaylistDetailScreen(
        playlist = playlist,
        entries = entries,
        libraryTracks = libraryTracks,
        state = state,
        destination = destination,
        appearanceSource = appearanceSource,
        dismissalPublisher = dismissalPublisher,
        mutationFailedLabel = mutationFailedLabel,
        onBack = onBack,
        onRetry = onRetry,
        onRename = onRename,
        onDelete = onDeleteMutation,
        onDeleteConfirmed = onDisplayedPlaylistDeleteConfirmed,
        onOpenBrowser = onOpenBrowser,
        onPlayEntry = onPlayEntry,
        onRemoveEntry = onRemoveEntry,
        onReorder = onReorder,
        bottomContentPadding = bottomContentPadding,
        onScrollPositionChanged = { index, offset ->
            onScrollPositionChanged(LibraryScrollPosition(index, offset))
        },
    )
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                state.isLoading ->
                    Text(
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
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(48.dp)
                                .semantics { contentDescription = retryLabel },
                        cornerRadius = 16.dp,
                        colors =
                            ButtonDefaults.buttonColors(
                                color = HausColors.current.ink,
                                contentColor = HausColors.current.paper,
                            ),
                    ) {
                        Text(retryLabel)
                    }
                }
            }
            when (sharedPlaylistRouteNotice(state)) {
                SharedPlaylistRouteNotice.PlaylistChanged ->
                    Text(
                        text = stringResource(Res.string.playlist_changed),
                        color = HausColors.current.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                SharedPlaylistRouteNotice.MutationFailed ->
                    Text(
                        text =
                            stringResource(Res.string.playlist_mutation_failed),
                        color = HausColors.current.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )

                null -> Unit
            }
        }
    }
}

private enum class SharedPlaylistRouteNotice {
    PlaylistChanged,
    MutationFailed
}

private fun sharedPlaylistRouteNotice(
    state: PlaylistState
): SharedPlaylistRouteNotice? =
    when {
        state.recoverableMessage != null ->
            SharedPlaylistRouteNotice.PlaylistChanged
        state.mutationErrorMessage != null ->
            SharedPlaylistRouteNotice.MutationFailed
        else -> null
    }

private sealed interface SharedPlaylistDetailResolution {
    data object AwaitConfirmation : SharedPlaylistDetailResolution

    data class Show(val playlist: PlaylistSummary) :
        SharedPlaylistDetailResolution

    data class ReturnToHub(val message: String) : SharedPlaylistDetailResolution
}

private fun sharedPlaylistDetailResolution(
    playlistId: String,
    state: PlaylistState,
): SharedPlaylistDetailResolution {
    val playlist = state.confirmedSnapshot.playlist(playlistId)
    return when {
        playlist != null -> SharedPlaylistDetailResolution.Show(playlist)
        state.isLoading || !state.hasConfirmedSnapshot ->
            SharedPlaylistDetailResolution.AwaitConfirmation
        else -> SharedPlaylistDetailResolution.ReturnToHub("playlist_changed")
    }
}
