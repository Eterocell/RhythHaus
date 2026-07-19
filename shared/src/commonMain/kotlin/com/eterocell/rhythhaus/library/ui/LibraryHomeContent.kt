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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.emptyLibrarySourceMutationsAllowed
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.library_queue
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.playlists_accessibility
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.library.selectLibraryTrackForPlayback
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.toPlayableTrack

internal fun libraryHomeTopContentPadding(systemBarTopPadding: Dp): Dp =
    systemBarTopPadding

internal fun dispatchHomeBrowseModeChange(
    currentMode: BrowseMode,
    nextMode: BrowseMode,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
) {
    if (currentMode == BrowseMode.Songs && nextMode != BrowseMode.Songs) {
        onTrackSelectionAction(TrackSelectionAction.RouteChanged(null))
    }
    onBrowseModeChange(nextMode)
}

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
    playbackController: PlaybackController,
    homeBackdrop: LayerBackdrop?,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onClearLibrary: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenDetailRoute: (LibraryRoute) -> Unit,
    onShowPlaylists: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onTrackSelected: (String) -> Unit,
    trackSelectionState: TrackSelectionState = TrackSelectionState(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    val selectionPageKey = TrackSelectionPageKey.HomeSongs
    val selectionModeActive = trackSelectionState.pageKey == selectionPageKey && trackSelectionState.selectedTrackIds.isNotEmpty()
    Box(modifier = Modifier.fillMaxSize()) {
        val homeTopContentPadding = libraryHomeTopContentPadding(rememberSystemBarTopPadding())
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
                    contentPadding = PaddingValues(top = homeTopContentPadding),
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
                        val playlistsDescription = stringResource(Res.string.playlists_accessibility)
                        Button(
                            onClick = onShowPlaylists,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .semantics { contentDescription = playlistsDescription },
                            cornerRadius = 16.dp,
                            colors = ButtonDefaults.buttonColors(
                                color = HausColors.current.panel,
                                contentColor = HausColors.current.ink,
                            ),
                        ) {
                            top.yukonga.miuix.kmp.basic.Text(
                                text = stringResource(Res.string.playlists),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
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
                            onModeChange = { nextMode ->
                                dispatchHomeBrowseModeChange(
                                    currentMode = browseMode,
                                    nextMode = nextMode,
                                    onTrackSelectionAction = onTrackSelectionAction,
                                    onBrowseModeChange = onBrowseModeChange,
                                )
                            },
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
                                    isNowPlaying = track.id == selectedTrackId,
                                    selectionModeActive = selectionModeActive,
                                    isSelected = track.id in trackSelectionState.selectedTrackIds,
                                    onPlay = {
                                        onTrackSelected(track.id)
                                        selectLibraryTrackForPlayback(
                                            playbackController = playbackController,
                                            visibleQueue = snapshot.tracks.map { it.toPlayableTrack() },
                                            selectedTrackId = track.id,
                                        )
                                    },
                                    onToggleSelection = {
                                        onTrackSelectionAction(TrackSelectionAction.Toggle(selectionPageKey, track.id))
                                    },
                                    onStartSelection = {
                                        onTrackSelectionAction(TrackSelectionAction.Start(selectionPageKey, track.id))
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(bottomContentPadding)) }
                }
            }
        }
    }
}
