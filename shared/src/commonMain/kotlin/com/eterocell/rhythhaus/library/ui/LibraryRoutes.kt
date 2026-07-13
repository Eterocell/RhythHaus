package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.taglib.TagLibReader
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_detail_subtitle_format
import rhythhaus.shared.generated.resources.artist_detail_subtitle_format
import rhythhaus.shared.generated.resources.unknown_artist
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.search.SearchScreen
import com.eterocell.rhythhaus.settings.SettingsScreen
import com.eterocell.rhythhaus.Track

@Composable
internal fun LibraryRouteOverlays(
    route: LibraryRoute,
    snapshot: LibrarySnapshot,
    libraryTracks: List<LibraryTrack>,
    tagLibReader: TagLibReader,
    playbackController: PlaybackController,
    playbackState: PlaybackState,
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    backdrop: LayerBackdrop?,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onCancelScan: () -> Unit,
    onDismiss: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
) {
    if (route == LibraryRoute.Settings) {
        SettingsScreen(
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            hasImportedTracks = snapshot.tracks.isNotEmpty(),
            currentThemeMode = currentThemeMode,
            clearLibraryDialogBackdrop = backdrop,
            onThemeModeSelected = onThemeModeSelected,
            onClearLibrary = onClearLibrary,
            onRescanSource = onRescanSource,
            onRemoveSource = onRemoveSource,
            onCancelScan = onCancelScan,
            onDismiss = onDismiss,
        )
    }

    if (route == LibraryRoute.Search) {
        SearchScreen(
            libraryTracks = libraryTracks,
            tagLibReader = tagLibReader,
            playbackController = playbackController,
            playbackState = playbackState,
            onDismiss = onDismiss,
            onScrollPositionChanged = onScrollPositionChanged,
        )
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
    selectedTrackId: String?,
    isNowPlayingBarVisible: Boolean,
    onBack: () -> Unit,
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
    onTrackClickFromTracks: (List<Track>, Track) -> Unit,
    onExpandNowPlaying: (Track) -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
    homeContent: @Composable ((LibraryRoute) -> Unit) -> Unit,
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
                    onExpandNowPlaying = onExpandNowPlaying,
                    onShowSettings = onShowSettings,
                    onShowSearch = onShowSearch,
                    isNowPlayingBarVisible = isNowPlayingBarVisible,
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
                    onExpandNowPlaying = onExpandNowPlaying,
                    onShowSettings = onShowSettings,
                    onShowSearch = onShowSearch,
                    isNowPlayingBarVisible = isNowPlayingBarVisible,
                    onScrollPositionChanged = onScrollPositionChanged,
                )
            }
        }

        LibraryRoute.NowPlaying -> {
            // Now Playing is shown as an overlay, not a navigation route
        }

        LibraryRoute.Home,
        LibraryRoute.Settings,
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
