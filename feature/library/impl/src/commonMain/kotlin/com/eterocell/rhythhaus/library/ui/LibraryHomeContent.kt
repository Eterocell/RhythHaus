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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausBackdrop
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.hide_scan_report
import rhythhaus.feature.library.generated.resources.library_empty
import rhythhaus.feature.library.generated.resources.library_sources
import rhythhaus.feature.library.generated.resources.recover_source
import rhythhaus.feature.library.generated.resources.remove_missing
import rhythhaus.feature.library.generated.resources.remove_source
import rhythhaus.feature.library.generated.resources.rescan
import rhythhaus.feature.library.generated.resources.retry_scan
import rhythhaus.feature.library.generated.resources.scan_cancelled
import rhythhaus.feature.library.generated.resources.scan_completed
import rhythhaus.feature.library.generated.resources.scan_failed
import rhythhaus.feature.library.generated.resources.scan_report_empty
import rhythhaus.feature.library.generated.resources.scan_report_error_format
import rhythhaus.feature.library.generated.resources.scan_summary_format
import rhythhaus.feature.library.generated.resources.source_access_available
import rhythhaus.feature.library.generated.resources.source_access_lost
import rhythhaus.feature.library.generated.resources.view_scan_report
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

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
 * @param sources configured local library sources.
 * @param importMessage a transient import message, if any.
 * @param scanProgress the active scan progress, if any.
 * @param scanErrors errors recorded for the displayed scan session.
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
 * @param onRescanSource requests scanning a configured source again.
 * @param onRemoveSource requests removing a configured source.
 * @param onRemoveMissingTracks requests removing tracks not seen by a completed
 *   scan.
 * @param onOpenAlbum requests navigation to an album detail.
 * @param onOpenArtist requests navigation to an artist detail.
 * @param onShowPlaylists requests navigation to the playlists hub.
 * @param onPlayTrack requests playback of the ordered tracks at the selected
 *   occurrence.
 * @param onToggleSelection requests one toggle of the given track ID.
 * @param onStartSelection requests selection beginning with the given track ID.
 * @param onVisibleTrackIdsChanged receives rendered track IDs whenever their
 *   visible sequence changes.
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
    sources: List<LibrarySource>,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanErrors: List<ScanError>,
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
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
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
    var reportVisible by
        remember(scanProgress?.session?.id) { mutableStateOf(false) }
    val trackIdSet = remember(tracks) { tracks.mapTo(mutableSetOf()) { it.id } }
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
                        item {
                            LibraryManagerCard(
                                sources = sources,
                                folderPickerLauncher = folderPickerLauncher,
                                sourcePickerActionVisible =
                                    sourcePickerActionVisible,
                                labels = labels,
                                mutationsEnabled = mutationsEnabled,
                                scanProgress = scanProgress,
                                scanErrors = scanErrors,
                                reportVisible = reportVisible,
                                onCancelScan = onCancelScan,
                                onToggleReport = {
                                    reportVisible = !reportVisible
                                },
                                onRecoverSource = folderPickerLauncher::launch,
                                onRescanSource = onRescanSource,
                                onRemoveSource = onRemoveSource,
                                onRemoveMissingTracks = onRemoveMissingTracks,
                            )
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
    LaunchedEffect(homeListState) {
        snapshotFlow {
            homeListState.layoutInfo.visibleItemsInfo.mapNotNull {
                it.key as? String
            }
        }
            .collect { visibleKeys ->
                onVisibleTrackIdsChanged(
                    visibleKeys.filter { it in trackIdSet })
            }
    }
}

@Composable
private fun LibraryManagerCard(
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    labels: LibrarySharedLabels,
    mutationsEnabled: Boolean,
    scanProgress: ScanProgress?,
    scanErrors: List<ScanError>,
    reportVisible: Boolean,
    onCancelScan: () -> Unit,
    onToggleReport: () -> Unit,
    onRecoverSource: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
) {
    val session = scanProgress?.session
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel(stringResource(Res.string.library_sources), null)
        if (sources.isEmpty()) {
            Text(
                stringResource(Res.string.library_empty),
                color = HausColors.current.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        sources.forEach { source ->
            SourceManagerRow(
                source = source,
                mutationsEnabled = mutationsEnabled,
                recoveryAvailable = folderPickerLauncher.isAvailable,
                onRecoverSource = onRecoverSource,
                onRescanSource = onRescanSource,
                onRemoveSource = onRemoveSource,
            )
        }
        if (sourcePickerActionVisible && sources.isNotEmpty()) {
            Button(
                onClick = folderPickerLauncher::launch,
                enabled = folderPickerLauncher.isAvailable && mutationsEnabled,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                cornerRadius = 10.dp,
            ) {
                Text(labels.addMusicFolder, fontSize = 13.sp)
            }
        }
        if (session != null && !scanProgress.isActive) {
            ScanOutcomePanel(
                session = session,
                source = sources.firstOrNull { it.id == session.sourceId },
                errors = scanErrors,
                reportVisible = reportVisible,
                mutationsEnabled = mutationsEnabled,
                onToggleReport = onToggleReport,
                onRescanSource = onRescanSource,
                onRemoveMissingTracks = onRemoveMissingTracks,
            )
        }
    }
}

@Composable
private fun SourceManagerRow(
    source: LibrarySource,
    mutationsEnabled: Boolean,
    recoveryAvailable: Boolean,
    onRecoverSource: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
) {
    val access =
        stringResource(
            if (source.accessStatus == LibrarySourceAccessStatus.Available)
                Res.string.source_access_available
            else Res.string.source_access_lost,
        )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            source.displayName,
            color = HausColors.current.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black)
        Text(access, color = HausColors.current.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val lostAccess =
                source.accessStatus == LibrarySourceAccessStatus.LostAccess
            Button(
                onClick = {
                    if (lostAccess) onRecoverSource()
                    else onRescanSource(source)
                },
                enabled =
                    mutationsEnabled && (!lostAccess || recoveryAvailable),
                modifier = Modifier.weight(1f).height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(
                            if (lostAccess) Res.string.recover_source
                            else Res.string.rescan),
                        fontSize = 12.sp)
                }
            Button(
                onClick = { onRemoveSource(source) },
                enabled = mutationsEnabled,
                modifier = Modifier.weight(1f).height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(Res.string.remove_source),
                        fontSize = 12.sp)
                }
        }
    }
}

@Composable
private fun ScanOutcomePanel(
    session: ScanSession,
    source: LibrarySource?,
    errors: List<ScanError>,
    reportVisible: Boolean,
    mutationsEnabled: Boolean,
    onToggleReport: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
) {
    val title =
        stringResource(
            when (session.status) {
                ScanStatus.Completed -> Res.string.scan_completed
                ScanStatus.Cancelled -> Res.string.scan_cancelled
                ScanStatus.Failed -> Res.string.scan_failed
                else -> Res.string.scan_completed
            },
        )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            color = HausColors.current.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black)
        Text(
            stringResource(
                Res.string.scan_summary_format,
                session.foldersVisited,
                session.filesVisited,
                session.tracksAdded,
                session.tracksUpdated,
                session.filesSkipped),
            color = HausColors.current.muted,
            fontSize = 12.sp)
        session.terminalMessage?.takeIf(String::isNotBlank)?.let {
            Text(it, color = HausColors.current.pulse, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            source?.let {
                Button(
                    onClick = { onRescanSource(it) },
                    enabled = mutationsEnabled,
                    modifier = Modifier.weight(1f).height(38.dp),
                    cornerRadius = 8.dp) {
                        Text(
                            stringResource(
                                if (session.status == ScanStatus.Completed)
                                    Res.string.rescan
                                else Res.string.retry_scan,
                            ),
                            fontSize = 12.sp,
                        )
                    }
            }
            Button(
                onClick = onToggleReport,
                modifier = Modifier.weight(1f).height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(
                            if (reportVisible) Res.string.hide_scan_report
                            else Res.string.view_scan_report),
                        fontSize = 12.sp)
                }
        }
        if (session.status == ScanStatus.Completed && source != null) {
            Button(
                onClick = { onRemoveMissingTracks(source, session) },
                enabled = mutationsEnabled,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                cornerRadius = 8.dp) {
                    Text(
                        stringResource(Res.string.remove_missing),
                        fontSize = 12.sp)
                }
        }
        if (reportVisible) {
            if (errors.isEmpty())
                Text(
                    stringResource(Res.string.scan_report_empty),
                    color = HausColors.current.muted,
                    fontSize = 12.sp)
            errors.forEach { error ->
                Text(
                    stringResource(
                        Res.string.scan_report_error_format,
                        error.displayPath,
                        error.reason),
                    color = HausColors.current.muted,
                    fontSize = 12.sp)
            }
        }
    }
}
