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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausBackdrop
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface

/**
 * Resolves the home list top content padding from the system bar inset.
 *
 * @param systemBarTopPadding the system bar top padding in density pixels.
 */
public fun libraryHomeTopContentPadding(systemBarTopPadding: Dp): Dp =
    systemBarTopPadding

/**
 * Renders the library home using only raw feature inputs and callbacks. It owns
 * internal album/artist grouping, its lazy list state, and the recorded
 * backdrop; Shared owns browse state, selection, playback policy, navigation,
 * and scroll storage.
 *
 * @param title the library header title.
 * @param subtitle the library header subtitle.
 * @param tracks the authoritative display/playback track sequence.
 * @param browseMode the current album/artist/song browse mode.
 * @param folderPickerLauncher launches the platform folder picker.
 * @param sourcePickerActionVisible whether the import source action is visible.
 * @param importMessage a transient import message, if any.
 * @param scanProgress the active scan progress, if any.
 * @param mutationsEnabled whether source mutations are currently allowed.
 * @param currentTrackId the current playback track ID, or null.
 * @param selectionModeActive whether songs currently select rather than play.
 * @param selectedTrackIds immutable selected IDs effective for the home page.
 * @param labels Shared-owned localized wording.
 * @param homeBackdrop the recorded root backdrop for liquid-glass chrome.
 * @param artworkLoader lazily resolves artwork bytes for a track ID.
 * @param onBrowseModeChange requests a browse-mode change.
 * @param onClearLibrary requests clearing the library.
 * @param onCancelScan requests cancellation of the active scan.
 * @param onOpenAlbum requests navigation to an album detail.
 * @param onOpenArtist requests navigation to an artist detail.
 * @param onShowPlaylists requests navigation to the playlists hub.
 * @param onPlayTrack requests playback of the ordered tracks at the selected
 *   occurrence.
 * @param onToggleSelection requests one toggle of the given track ID.
 * @param onStartSelection requests selection beginning with the given track ID.
 * @param onVisibleTrackIdsChanged receives the page's track IDs whenever the
 *   page track sequence changes.
 * @param onScrollPositionChanged receives first visible item index and pixel
 *   offset.
 * @param bottomContentPadding reserved trailing list space for Shared shell
 *   chrome.
 */
@Composable
public fun LibraryHomeContent(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    browseMode: BrowseMode,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    mutationsEnabled: Boolean,
    currentTrackId: String?,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    labels: LibrarySharedLabels,
    homeBackdrop: RhythHausBackdrop?,
    artworkLoader: suspend (String) -> ByteArray?,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onClearLibrary: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onShowPlaylists: () -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit,
    onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged:
        (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
) {
    val albums = remember(tracks) { groupTracksByAlbum(tracks) }
    val artists = remember(tracks) { groupTracksByArtist(tracks) }
    val homeListState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        val homeTopContentPadding =
            libraryHomeTopContentPadding(rememberSystemBarTopPadding())
        Box(
            modifier =
                Modifier.fillMaxSize().recordRhythHausBackdrop(homeBackdrop),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HausColors.current.paper) {
                    LazyColumn(
                        state = homeListState,
                        modifier =
                            Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        contentPadding =
                            PaddingValues(top = homeTopContentPadding),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item {
                            HeaderSection(title = title, subtitle = subtitle)
                        }
                        if (tracks.isEmpty() && sourcePickerActionVisible) {
                            item {
                                ImportAudioCard(
                                    folderPickerLauncher = folderPickerLauncher,
                                    importMessage = importMessage,
                                    hasImportedTracks = false,
                                    mutationsEnabled = mutationsEnabled,
                                    labels = labels,
                                    onClearLibrary = onClearLibrary,
                                )
                            }
                        }
                        if (tracks.isEmpty() &&
                            scanProgress?.isActive == true) {
                            item {
                                val sp = scanProgress
                                val ss = sp.session!!
                                ScanningCard(
                                    foldersVisited = ss.foldersVisited,
                                    filesVisited = ss.filesVisited,
                                    tracksAdded = ss.tracksAdded,
                                    latestItem = sp.latestItem,
                                    labels = labels,
                                    onCancel = onCancelScan,
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = onShowPlaylists,
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(48.dp)
                                        .semantics {
                                            contentDescription =
                                                labels.playlistsAccessibility
                                        },
                                cornerRadius = 16.dp,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        color = HausColors.current.panel,
                                        contentColor = HausColors.current.ink,
                                    ),
                            ) {
                                top.yukonga.miuix.kmp.basic.Text(
                                    text = labels.playlists,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        item {
                            SectionLabel(
                                title = labels.libraryQueue,
                                subtitle = null,
                            )
                        }
                        item {
                            BrowseModePicker(
                                browseMode = browseMode,
                                labels = labels,
                                onModeChange = onBrowseModeChange,
                            )
                        }
                        when (browseMode) {
                            BrowseMode.Albums -> {
                                item {
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxWidth()) {
                                            val columns =
                                                albumGridColumnsForWidth(
                                                    maxWidth.value)
                                            Column(
                                                verticalArrangement =
                                                    Arrangement.spacedBy(
                                                        10.dp)) {
                                                    albums
                                                        .chunked(columns)
                                                        .forEach { row ->
                                                            Row(
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth(),
                                                                horizontalArrangement =
                                                                    Arrangement
                                                                        .spacedBy(
                                                                            10
                                                                                .dp),
                                                            ) {
                                                                row.forEach {
                                                                    albumGroup
                                                                    ->
                                                                    AlbumCard(
                                                                        album =
                                                                            albumGroup,
                                                                        labels =
                                                                            labels,
                                                                        artworkLoader =
                                                                            artworkLoader,
                                                                        modifier =
                                                                            Modifier
                                                                                .weight(
                                                                                    1f),
                                                                        onClick = {
                                                                            onOpenAlbum(
                                                                                albumGroup
                                                                                    .album)
                                                                        },
                                                                    )
                                                                }
                                                                repeat(
                                                                    columns -
                                                                        row
                                                                            .size) {
                                                                        Spacer(
                                                                            Modifier
                                                                                .weight(
                                                                                    1f))
                                                                    }
                                                            }
                                                        }
                                                }
                                        }
                                }
                            }

                            BrowseMode.Artists -> {
                                items(artists, key = { it.artist }) {
                                    artistGroup ->
                                    ArtistRow(
                                        artist = artistGroup,
                                        labels = labels,
                                        artworkLoader = artworkLoader,
                                        onClick = {
                                            onOpenArtist(artistGroup.artist)
                                        },
                                    )
                                }
                            }

                            BrowseMode.Songs -> {
                                items(tracks, key = { it.id }) { track ->
                                    TrackRow(
                                        track = track,
                                        isNowPlaying =
                                            track.id == currentTrackId,
                                        selectionModeActive =
                                            selectionModeActive,
                                        isSelected =
                                            track.id in selectedTrackIds,
                                        labels = labels,
                                        artworkLoader = artworkLoader,
                                        onPlay = {
                                            onPlayTrack(tracks, track)
                                        },
                                        onToggleSelection = {
                                            onToggleSelection(track.id)
                                        },
                                        onStartSelection = {
                                            onStartSelection(track.id)
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
    LaunchedEffect(
        homeListState.firstVisibleItemIndex,
        homeListState.firstVisibleItemScrollOffset) {
            onScrollPositionChanged(
                homeListState.firstVisibleItemIndex,
                homeListState.firstVisibleItemScrollOffset)
        }
    LaunchedEffect(tracks) {
        onVisibleTrackIdsChanged(tracks.map { it.id })
    }
}
