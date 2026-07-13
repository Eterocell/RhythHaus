package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.emptyLibrarySourceMutationsAllowed
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.library
import rhythhaus.shared.generated.resources.library_queue
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarContentPadding
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.toPlayableTrack

@Composable
internal fun LibraryHomeContent(
    snapshot: LibrarySnapshot,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    browseMode: BrowseMode,
    homeListState: LazyListState,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    selectedTrackId: String?,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    homeBackdrop: LayerBackdrop?,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onClearLibrary: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onTrackSelected: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val homeStatusBarHeight = rememberSystemBarTopPadding()
        val homeScrollChromeState by remember(homeListState) {
            derivedStateOf { nestedScrollChromeStateFor(homeListState.toLibraryScrollPosition()) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordRhythHausBackdrop(homeBackdrop),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
                LazyColumn(
                    state = homeListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = homeStatusBarHeight),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        HeaderSection(snapshot)
                    }
                    if (snapshot.tracks.isEmpty() && sourcePickerActionVisible) {
                        item {
                            ImportAudioCard(
                                folderPickerLauncher = folderPickerLauncher,
                                importMessage = importMessage,
                                hasImportedTracks = false,
                                mutationsEnabled = emptyLibrarySourceMutationsAllowed(
                                    isProgressActive = scanProgress?.isActive == true,
                                    isJobActive = scanJob?.isActive == true,
                                ),
                                onClearLibrary = onClearLibrary,
                            )
                        }
                    }
                    if (scanProgress?.isActive == true) {
                        item {
                            val sp = scanProgress
                            val ss = sp.session!!
                            ScanningCard(
                                foldersVisited = ss.foldersVisited,
                                filesVisited = ss.filesVisited,
                                tracksAdded = ss.tracksAdded,
                                latestItem = sp.latestItem,
                                onCancel = onCancelScan,
                            )
                        }
                    }
                    item {
                        SectionLabel(
                            title = stringResource(Res.string.library_queue),
                            subtitle = null,
                        )
                    }
                    item {
                        BrowseModePicker(
                            browseMode = browseMode,
                            onModeChange = onBrowseModeChange,
                        )
                    }
                    when (browseMode) {
                        BrowseMode.Albums -> {
                            item {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val columns = albumGridColumnsForWidth(maxWidth.value)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        albums.chunked(columns).forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                row.forEach { albumGroup ->
                                                    AlbumCard(
                                                        album = albumGroup,
                                                        modifier = Modifier.weight(1f),
                                                        onClick = { onOpenDetailRoute(LibraryRoute.AlbumDetail(albumGroup.album)) },
                                                    )
                                                }
                                                repeat(columns - row.size) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        BrowseMode.Artists -> {
                            items(artists, key = { it.artist }) { artistGroup ->
                                ArtistRow(
                                    artist = artistGroup,
                                    onClick = { onOpenDetailRoute(LibraryRoute.ArtistDetail(artistGroup.artist)) },
                                )
                            }
                        }

                        BrowseMode.Songs -> {
                            items(snapshot.tracks, key = { it.id }) { track ->
                                TrackRow(
                                    track = track,
                                    selected = track.id == selectedTrackId,
                                    onClick = {
                                        onTrackSelected(track.id)
                                        val playableTracks = snapshot.tracks.map { it.toPlayableTrack() }
                                        if (playbackState.currentTrack?.id != track.id || playbackState.status == PlaybackStatus.Idle) {
                                            playbackController.setQueue(playableTracks, track.id)
                                        }
                                        playbackController.togglePlayPause()
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(NowPlayingBarContentPadding)) }
                }
            }
        }
        NestedScrollBlurChrome(
            state = homeScrollChromeState,
            title = stringResource(Res.string.library),
            backdrop = homeBackdrop,
            statusBarHeight = homeStatusBarHeight,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
