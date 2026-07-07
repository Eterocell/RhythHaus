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
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.taglib.TagLibReader
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
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onDismiss: () -> Unit,
    onShowClearLibrary: () -> Unit,
    onScrollPositionChanged: (LibraryScrollPosition) -> Unit,
) {
    if (route == LibraryRoute.ClearLibraryDialog) {
        AnimatedClearLibraryDialogRoute(
            onDismiss = onDismiss,
            onClearLibrary = {
                onClearLibrary()
                onDismiss()
            },
        )
    }

    if (route == LibraryRoute.Settings) {
        SettingsScreen(
            folderPickerLauncher = folderPickerLauncher,
            importMessage = importMessage,
            scanProgress = scanProgress,
            scanJob = scanJob,
            hasImportedTracks = snapshot.tracks.isNotEmpty(),
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            onClearLibrary = onShowClearLibrary,
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
    onPlayPauseFromTracks: (List<Track>, Track) -> Unit,
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
                    selectedTrack = selectedAlbumTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = onBack,
                    onTrackSelected = { /* selection only */ },
                    onPlayPause = { track -> onPlayPauseFromTracks(albumTracks, track) },
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
                    selectedTrack = selectedArtistTrack,
                    playbackState = playbackState,
                    playbackController = playbackController,
                    tagLibReader = tagLibReader,
                    libraryTracks = libraryTracks,
                    onBack = onBack,
                    onTrackSelected = { /* selection only */ },
                    onPlayPause = { track -> onPlayPauseFromTracks(artistTracks, track) },
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
        LibraryRoute.ClearLibraryDialog,
        -> {
            homeContent(onOpenDetailRoute)
        }
    }
}
