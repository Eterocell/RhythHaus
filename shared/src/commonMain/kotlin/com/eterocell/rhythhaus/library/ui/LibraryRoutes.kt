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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlayableTrack
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
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.library.selectOccurrenceForPlayback
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsHost
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsLabels
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.search.SearchContent
import com.eterocell.rhythhaus.search.SearchSharedLabels
import com.eterocell.rhythhaus.settings.OpenSourceLibrariesScreen
import com.eterocell.rhythhaus.settings.SettingsAboutScreen
import com.eterocell.rhythhaus.settings.SettingsScreen
import com.eterocell.rhythhaus.settings.SettingsSharedLabels
import com.eterocell.rhythhaus.settings.SettingsSourceItem
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.close
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.now_playing_badge
import rhythhaus.shared.generated.resources.playlist_changed
import rhythhaus.shared.generated.resources.playlist_load_failed
import rhythhaus.shared.generated.resources.playlist_loading
import rhythhaus.shared.generated.resources.playlist_mutation_failed
import rhythhaus.shared.generated.resources.playlist_retry
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.remove
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.select_track_format
import rhythhaus.shared.generated.resources.settings
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
        LibraryRoute.Settings -> {
            val mutationsEnabled =
                sourceMutationsAllowed(
                    isProgressActive = scanProgress?.isActive == true,
                    isJobActive = scanJob?.isActive == true,
                )
            var showClearLibraryDialog by
                remember(destinationId, playlistAppearanceSource) {
                    mutableStateOf(false)
                }
            SettingsScreen(
                labels =
                    SettingsSharedLabels(
                        title = stringResource(Res.string.settings),
                        addMusicFolder =
                            stringResource(Res.string.add_music_folder),
                        folderPickerUnavailable =
                            stringResource(
                                Res.string.folder_picker_unavailable),
                        clearLibrary = stringResource(Res.string.clear_library),
                        cancel = stringResource(Res.string.cancel),
                        remove = stringResource(Res.string.remove),
                    ),
                currentThemeMode = currentThemeMode,
                sources =
                    sources.map {
                        SettingsSourceItem(
                            id = it.id,
                            displayName = it.displayName,
                            accessAvailable =
                                it.accessStatus ==
                                    com.eterocell.rhythhaus.library
                                        .LibrarySourceAccessStatus
                                        .Available,
                            hasBeenScanned = it.lastScanAtEpochMillis != null,
                        )
                    },
                sourcePickerActionVisible = sourcePickerActionVisible,
                sourcePickerAvailable = folderPickerLauncher.isAvailable,
                importMessage = importMessage,
                mutationsEnabled = mutationsEnabled,
                hasImportedTracks = snapshot.tracks.isNotEmpty(),
                playlistBackupContent = {
                    PlaylistBackupSettingsHost(
                        state = playlistBackupState,
                        launcherAvailable = backupDocumentAvailable,
                        destination =
                            PlaylistFeatureDestination(
                                destinationId.instanceToken),
                        appearanceSource = playlistAppearanceSource,
                        dismissalPublisher =
                            featureDismissalPublisher(
                                destinationId, registerBackSurface),
                        labels =
                            PlaylistBackupSettingsLabels(
                                cancel = stringResource(Res.string.cancel),
                                close = stringResource(Res.string.close),
                            ),
                        onExport = onExportPlaylists,
                        onOpen = onOpenPlaylistBackup,
                        onAction = onPlaylistBackupAction,
                        onDismissPreview = {
                            onPlaylistBackupAction(
                                PlaylistBackupUiAction.DismissPreview)
                        },
                        onConfirmPreview = onConfirmPlaylistBackup,
                        onDismissResult = {
                            onPlaylistBackupAction(
                                PlaylistBackupUiAction.DismissResult)
                        },
                    )
                },
                activeScanContent =
                    if (scanProgress?.isActive == true) {
                        {
                            val session = scanProgress.session!!
                            ScanningCard(
                                foldersVisited = session.foldersVisited,
                                filesVisited = session.filesVisited,
                                tracksAdded = session.tracksAdded,
                                latestItem = scanProgress.latestItem,
                                labels = librarySharedLabels(),
                                onCancel = onCancelScan,
                            )
                        }
                    } else {
                        null
                    },
                clearLibraryDialog =
                    if (showClearLibraryDialog) {
                        {
                            AnimatedClearLibraryDialogRoute(
                                onDismiss = { showClearLibraryDialog = false },
                                onClearLibrary = {
                                    if (sourceMutationsAllowed(
                                        scanProgress?.isActive == true,
                                        scanJob?.isActive == true,
                                    )) {
                                        onClearLibrary()
                                    }
                                    showClearLibraryDialog = false
                                },
                            )
                        }
                    } else {
                        null
                    },
                onThemeModeSelected = onThemeModeSelected,
                onAddMusicFolder = folderPickerLauncher::launch,
                onRescanSource = { id ->
                    sources
                        .firstOrNull { it.id == id }
                        ?.let { source ->
                            if (sourceMutationsAllowed(
                                scanProgress?.isActive == true,
                                scanJob?.isActive == true,
                            )) {
                                onRescanSource(source)
                            }
                        }
                },
                onRemoveSource = { id ->
                    sources
                        .firstOrNull { it.id == id }
                        ?.let { source ->
                            if (sourceMutationsAllowed(
                                scanProgress?.isActive == true,
                                scanJob?.isActive == true,
                            )) {
                                onRemoveSource(source)
                            }
                        }
                },
                onRequestClearLibrary = {
                    if (mutationsEnabled && snapshot.tracks.isNotEmpty()) {
                        showClearLibraryDialog = true
                    }
                },
                onAboutClick = onShowSettingsAbout,
                onDismiss = {
                    showClearLibraryDialog = false
                    onDismiss()
                },
            )
        }

        LibraryRoute.Search ->
            SearchContent(
                libraryTracks = libraryTracks,
                currentTrackId = playbackState.currentTrack?.id,
                isPlaying = playbackState.isPlaying,
                labels =
                    SearchSharedLabels(
                        stringResource(Res.string.search),
                        stringResource(Res.string.clear),
                        stringResource(Res.string.now_playing_badge)),
                selectTrackLabel = { title ->
                    stringResource(Res.string.select_track_format, title)
                },
                selectionModeActive =
                    trackSelectionState.pageKey ==
                        TrackSelectionPageKey.Search &&
                        trackSelectionState.selectedTrackIds.isNotEmpty(),
                selectedTrackIds =
                    if (trackSelectionState.pageKey ==
                        TrackSelectionPageKey.Search)
                        trackSelectionState.selectedTrackIds
                    else emptySet(),
                onStartSelection = { id ->
                    onTrackSelectionAction(
                        TrackSelectionAction.Start(
                            TrackSelectionPageKey.Search, id))
                },
                onToggleSelection = { id ->
                    onTrackSelectionAction(
                        TrackSelectionAction.Toggle(
                            TrackSelectionPageKey.Search, id))
                },
                onVisibleTrackIdsChanged = { ids ->
                    onTrackSelectionAction(
                        TrackSelectionAction.ReconcileVisible(
                            TrackSelectionPageKey.Search, ids))
                },
                onScrollPositionChanged = { index, offset ->
                    onScrollPositionChanged(
                        LibraryScrollPosition(index, offset))
                },
                onPlayTrack = { orderedResults, selectedTrack ->
                    playSearchTrack(
                        playbackController,
                        orderedResults,
                        selectedTrack,
                        onDismiss)
                },
                onDismiss = onDismiss,
                playingIndicator = {
                    Box(Modifier.testTag("shared-search-equalizer")) {
                        EqualizerStrip(active = true)
                    }
                },
                bottomContentPadding = bottomContentPadding,
            )

        LibraryRoute.SettingsAbout ->
            SettingsAboutScreen(
                onOpenLibraries = onShowOpenSourceLibraries,
                onDismiss = onDismiss,
            )

        LibraryRoute.OpenSourceLibraries ->
            OpenSourceLibrariesScreen(
                readCatalogJson = {
                    Res.readBytes("files/aboutlibraries.json").decodeToString()
                },
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

internal fun playSearchTrack(
    playbackController: PlaybackController,
    orderedResults: List<LibraryTrack>,
    selectedTrack: LibraryTrack,
    onDismiss: () -> Unit,
) {
    selectLibraryTrackForPlayback(
        playbackController = playbackController,
        visibleQueue = orderedResults.map { it.toPlayableTrack() },
        selectedTrackId = selectedTrack.id,
    )
    onDismiss()
}

/**
 * Resolves the ordered album track sequence for a detail route, matching the
 * feature's internal grouping order (disc, track, lowercase title).
 */
private fun albumDetailTracks(tracks: List<Track>, album: String): List<Track> =
    tracks
        .filter { it.album == album }
        .sortedWith(
            compareBy<Track> { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
                .thenBy { it.title.lowercase() })

/**
 * Resolves the ordered artist track sequence for a detail route, matching the
 * feature's internal grouping order (disc, track, lowercase title).
 */
private fun artistDetailTracks(
    tracks: List<Track>,
    artist: String
): List<Track> =
    tracks
        .filter { it.artist == artist }
        .sortedWith(
            compareBy<Track> { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
                .thenBy { it.title.lowercase() })

@Composable
internal fun LibraryRouteContent(
    route: LibraryRoute,
    tracks: List<Track>,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
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
    artworkLoader: suspend (String) -> ByteArray?,
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
    val playableTracksById =
        remember(libraryTracks) {
            libraryTracks.associate { it.id to it.toPlayableTrack() }
        }
    when (route) {
        is LibraryRoute.AlbumDetail -> {
            val albumTracks = albumDetailTracks(tracks, route.album)
            if (albumTracks.isEmpty()) {
                LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                DrillDownView(
                    title = route.album,
                    summary =
                        LibraryDetailSummary.Album(
                            trackCount = albumTracks.size,
                            artist = albumTracks.firstOrNull()?.artist,
                        ),
                    tracks = albumTracks,
                    topBarArtworkTrack = albumTracks.firstOrNull(),
                    currentTrackId = playbackState.currentTrack?.id,
                    selectionPage = LibrarySelectionPage.Album(route.album),
                    selectionModeActive =
                        trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Album(route.album) &&
                            trackSelectionState.selectedTrackIds.isNotEmpty(),
                    selectedTrackIds =
                        if (trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Album(route.album))
                            trackSelectionState.selectedTrackIds
                        else emptySet(),
                    labels = librarySharedLabels(),
                    artworkLoader = artworkLoader,
                    onBack = onBack,
                    onPlayTrack = { orderedTracks, selectedTrack ->
                        onTrackSelected(selectedTrack.id)
                        onTrackClickFromTracks(orderedTracks, selectedTrack)
                    },
                    onToggleSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Toggle(
                                TrackSelectionPageKey.Album(route.album),
                                trackId,
                            ),
                        )
                    },
                    onStartSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Start(
                                TrackSelectionPageKey.Album(route.album),
                                trackId,
                            ),
                        )
                    },
                    onVisibleTrackIdsChanged = { ids ->
                        onTrackSelectionAction(
                            TrackSelectionAction.ReconcileVisible(
                                TrackSelectionPageKey.Album(route.album), ids))
                    },
                    onScrollPositionChanged = { index, offset ->
                        onScrollPositionChanged(
                            LibraryScrollPosition(index, offset))
                    },
                    bottomContentPadding = bottomContentPadding,
                )
            }
        }

        is LibraryRoute.ArtistDetail -> {
            val artistTracks = artistDetailTracks(tracks, route.artist)
            if (artistTracks.isEmpty()) {
                LaunchedEffect(route) { onBack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                DrillDownView(
                    title = route.artist,
                    summary =
                        LibraryDetailSummary.Artist(
                            albumCount =
                                artistTracks.map { it.album }.distinct().size,
                            trackCount = artistTracks.size,
                        ),
                    tracks = artistTracks,
                    topBarArtworkTrack = artistTracks.firstOrNull(),
                    currentTrackId = playbackState.currentTrack?.id,
                    selectionPage = LibrarySelectionPage.Artist(route.artist),
                    selectionModeActive =
                        trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Artist(route.artist) &&
                            trackSelectionState.selectedTrackIds.isNotEmpty(),
                    selectedTrackIds =
                        if (trackSelectionState.pageKey ==
                            TrackSelectionPageKey.Artist(route.artist))
                            trackSelectionState.selectedTrackIds
                        else emptySet(),
                    labels = librarySharedLabels(),
                    artworkLoader = artworkLoader,
                    onBack = onBack,
                    onPlayTrack = { orderedTracks, selectedTrack ->
                        onTrackSelected(selectedTrack.id)
                        onTrackClickFromTracks(orderedTracks, selectedTrack)
                    },
                    onToggleSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Toggle(
                                TrackSelectionPageKey.Artist(route.artist),
                                trackId,
                            ),
                        )
                    },
                    onStartSelection = { trackId ->
                        onTrackSelectionAction(
                            TrackSelectionAction.Start(
                                TrackSelectionPageKey.Artist(route.artist),
                                trackId,
                            ),
                        )
                    },
                    onVisibleTrackIdsChanged = { ids ->
                        onTrackSelectionAction(
                            TrackSelectionAction.ReconcileVisible(
                                TrackSelectionPageKey.Artist(route.artist),
                                ids,
                            ),
                        )
                    },
                    onScrollPositionChanged = { index, offset ->
                        onScrollPositionChanged(
                            LibraryScrollPosition(index, offset))
                    },
                    bottomContentPadding = bottomContentPadding,
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
                        playableTracksById = playableTracksById,
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
    playableTracksById: Map<String, PlayableTrack>,
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
        playableTracksById = playableTracksById,
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
            modifier = Modifier,
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
