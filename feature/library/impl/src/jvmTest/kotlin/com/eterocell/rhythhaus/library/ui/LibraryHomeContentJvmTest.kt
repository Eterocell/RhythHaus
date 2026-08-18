package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryHomeContentJvmTest {
    init {
        Locale.setDefault(Locale.ENGLISH)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun songsRenderInOrderPreservingDuplicatesAndReportPrimitiveCallbacks() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            val scrolls = mutableListOf<Pair<Int, Int>>()
            val plays = mutableListOf<Pair<List<Track>, Track>>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        title = "Library",
                        subtitle = "",
                        tracks = tracks(),
                        browseMode = BrowseMode.Songs,
                        folderPickerLauncher = StubPicker,
                        sourcePickerActionVisible = false,
                        importMessage = null,
                        scanProgress = null,
                        mutationsEnabled = true,
                        currentTrackId = "t-2",
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        homeBackdrop = null,
                        artworkLoader = { null },
                        onBrowseModeChange = {},
                        onClearLibrary = {},
                        onCancelScan = {},
                        onOpenAlbum = {},
                        onOpenArtist = {},
                        onShowPlaylists = {},
                        onPlayTrack = { ordered, selected ->
                            plays += ordered to selected
                        },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { ids ->
                            visibleReports += ids
                        },
                        onScrollPositionChanged = { index, offset ->
                            scrolls += index to offset
                        },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()

            assertEquals(listOf(0, 0), scrolls.first().toList())
            assertEquals(
                tracks().map(Track::id),
                visibleReports.last(),
            )
            onAllNodes(hasText("Two")).assertCountEquals(2)
            onAllNodes(hasText("Now playing")).assertCountEquals(1)

            onAllNodes(hasContentDescription("Select Two"))[0].performClick()
            waitForIdle()
            assertEquals(tracks(), plays.last().first)
            assertEquals(tracks()[0].id, plays.last().second.id)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun homeScrollReportsPrimitivePositions() = runComposeUiTest {
        val scrolls = mutableListOf<Pair<Int, Int>>()
        setContent {
            Box(Modifier.size(420.dp, 300.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = twelveTracks(),
                    browseMode = BrowseMode.Songs,
                    folderPickerLauncher = StubPicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = {},
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { index, offset ->
                        scrolls += index to offset
                    },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onNode(hasScrollToIndexAction()).performScrollToIndex(12)
        waitForIdle()
        assertTrue(scrolls.any { it.first >= 12 })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun albumsGroupByExactNameSortedCaseInsensitively() = runComposeUiTest {
        var browseMode by mutableStateOf(BrowseMode.Albums)
        setContent {
            Box(Modifier.size(420.dp, 700.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = tracks(),
                    browseMode = browseMode,
                    folderPickerLauncher = StubPicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = { browseMode = it },
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Album B")).assertCountEquals(1)
        onAllNodes(hasText("album a")).assertCountEquals(1)
        onAllNodes(hasText("Artist · 3 tracks")).assertCountEquals(1)
        onAllNodes(hasText("Soloist · 1 tracks")).assertCountEquals(1)

        val albumA =
            onAllNodes(hasText("album a"))
                .fetchSemanticsNodes()
                .single()
                .boundsInRoot
                .left
        val albumB =
            onAllNodes(hasText("Album B"))
                .fetchSemanticsNodes()
                .single()
                .boundsInRoot
                .left
        assertTrue(albumA < albumB, "albums sort case-insensitively")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun artistsGroupByExactNameWithAlbumAndTrackCounts() = runComposeUiTest {
        var browseMode by mutableStateOf(BrowseMode.Artists)
        val openedArtists = mutableListOf<String>()
        setContent {
            Box(Modifier.size(420.dp, 700.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = tracks(),
                    browseMode = browseMode,
                    folderPickerLauncher = StubPicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = { browseMode = it },
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = { openedArtists += it },
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Artist")).assertCountEquals(1)
        onAllNodes(hasText("Soloist")).assertCountEquals(1)
        onAllNodes(hasText("1 albums · 3 tracks")).assertCountEquals(1)
        onAllNodes(hasText("1 albums · 1 tracks")).assertCountEquals(1)

        onAllNodes(hasText("Soloist"))[0].performClick()
        waitForIdle()
        assertEquals(listOf("Soloist"), openedArtists)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun browseModePickerDispatchesModeChange() = runComposeUiTest {
        val changed = mutableListOf<BrowseMode>()
        setContent {
            Box(Modifier.size(420.dp, 700.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = tracks(),
                    browseMode = BrowseMode.Songs,
                    folderPickerLauncher = StubPicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = { changed += it },
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Albums"))[0].performClick()
        waitForIdle()
        assertEquals(listOf(BrowseMode.Albums), changed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyLibraryShowsImportCardAndActiveScanWithCancel() =
        runComposeUiTest {
            var cancellations = 0
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        title = "Library",
                        subtitle = "",
                        tracks = emptyList(),
                        browseMode = BrowseMode.Songs,
                        folderPickerLauncher = AvailableStubPicker,
                        sourcePickerActionVisible = true,
                        importMessage = null,
                        scanProgress =
                            ScanProgress(session(ScanStatus.Scanning)),
                        mutationsEnabled = true,
                        currentTrackId = null,
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        homeBackdrop = null,
                        artworkLoader = { null },
                        onBrowseModeChange = {},
                        onClearLibrary = {},
                        onCancelScan = { cancellations++ },
                        onOpenAlbum = {},
                        onOpenArtist = {},
                        onShowPlaylists = {},
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Add music folder")).assertCountEquals(2)
            onNode(hasText("Scanning…")).assertExists()
            onNode(hasText("Cancel")).performClick()
            waitForIdle()

            assertEquals(1, cancellations)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun populatedLibraryHidesImportScanAndOutcomeUi() = runComposeUiTest {
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = listOf(managerTrack()),
                    browseMode = BrowseMode.Songs,
                    folderPickerLauncher = AvailableStubPicker,
                    sourcePickerActionVisible = true,
                    importMessage = null,
                    scanProgress = ScanProgress(session(ScanStatus.Scanning)),
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = {},
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Add music folder")).assertCountEquals(0)
        onAllNodes(hasText("Scanning…")).assertCountEquals(0)
        onAllNodes(hasText("Cancel")).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyLibraryWithTerminalSessionHidesOutcomeButShowsImport() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        title = "Library",
                        subtitle = "",
                        tracks = emptyList(),
                        browseMode = BrowseMode.Songs,
                        folderPickerLauncher = AvailableStubPicker,
                        sourcePickerActionVisible = true,
                        importMessage = null,
                        scanProgress =
                            ScanProgress(session(ScanStatus.Completed)),
                        mutationsEnabled = true,
                        currentTrackId = null,
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        homeBackdrop = null,
                        artworkLoader = { null },
                        onBrowseModeChange = {},
                        onClearLibrary = {},
                        onCancelScan = {},
                        onOpenAlbum = {},
                        onOpenArtist = {},
                        onShowPlaylists = {},
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Add music folder")).assertCountEquals(2)
            onAllNodes(hasText("Scan complete")).assertCountEquals(0)
            onAllNodes(hasText("Remove missing files")).assertCountEquals(0)
        }

    private fun session(status: ScanStatus, id: String = "scan"): ScanSession =
        ScanSession(
            id = id,
            sourceId = "source",
            status = status,
            startedAtEpochMillis = 1L,
            foldersVisited = 2,
            filesVisited = 4,
            tracksAdded = 2,
            tracksUpdated = 1,
            filesSkipped = 1,
            terminalMessage =
                if (status == ScanStatus.Failed) "Folder is unavailable"
                else null,
        )

    private fun twelveTracks(): List<Track> =
        List(12) { index ->
            track(
                id = "t-${index + 1}",
                title = "Track ${index + 1}",
                album = "Album B",
                artist = "Artist",
                disc = 1,
                number = index + 1,
            )
        }

    private fun managerTrack(): Track =
        track(
            id = "manager-track",
            title = "Manager track",
            album = "Manager album",
            artist = "Manager artist",
            disc = 1,
            number = 1,
        )

    private fun tracks(): List<Track> =
        listOf(
            track("t-1", "Two", "Album B", "Artist", disc = 1, number = 2),
            track("t-2", "One", "Album B", "Artist", disc = 1, number = 1),
            track("t-3", "Solo", "album a", "Soloist", disc = 1, number = 1),
            track("t-4", "Two", "Album B", "Artist", disc = 1, number = 3),
        )

    private fun track(
        id: String,
        title: String,
        album: String,
        artist: String,
        disc: Int,
        number: Int,
    ): Track =
        Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = 100,
            accent = TrackAccent(0xFF111111, 0xFF222222),
            source = AudioSource.FilePath("$id.mp3"),
            trackNumber = number,
            discNumber = disc,
        )

    @Composable
    private fun labels(): LibrarySharedLabels =
        LibrarySharedLabels(
            addMusicFolder = "Add music folder",
            folderPickerUnavailable = "Folder picker unavailable",
            clearLibrary = "Clear library",
            cancel = "Cancel",
            playlists = "Playlists",
            playlistsAccessibility = "Open playlists",
            libraryQueue = "Library queue",
            albumArt = "Album art",
            albumArtwork = "Album artwork",
            nowPlayingBadge = "Now playing",
            selectTrack = { title -> "Select $title" },
            trackArtistAlbum = { artist, album -> "$artist · $album" },
        )

    private object StubPicker : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = false
        override val supportsAdditionalSources: Boolean = false

        override fun launch() = Unit
    }

    private object AvailableStubPicker : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = true
        override val supportsAdditionalSources: Boolean = true
        var launches: Int = 0

        override fun launch() {
            launches++
        }
    }
}
