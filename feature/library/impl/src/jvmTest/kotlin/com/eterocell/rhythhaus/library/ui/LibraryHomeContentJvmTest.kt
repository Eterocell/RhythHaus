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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanError
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
                        sources = emptyList(),
                        importMessage = null,
                        scanProgress = null,
                        scanErrors = emptyList(),
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
                        onRescanSource = {},
                        onRemoveSource = {},
                        onRemoveMissingTracks = { _, _ -> },
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
                    sources = emptyList(),
                    importMessage = null,
                    scanProgress = null,
                    scanErrors = emptyList(),
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
                    onRescanSource = {},
                    onRemoveSource = {},
                    onRemoveMissingTracks = { _, _ -> },
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
                    sources = emptyList(),
                    importMessage = null,
                    scanProgress = null,
                    scanErrors = emptyList(),
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
                    onRescanSource = {},
                    onRemoveSource = {},
                    onRemoveMissingTracks = { _, _ -> },
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
                    sources = emptyList(),
                    importMessage = null,
                    scanProgress = null,
                    scanErrors = emptyList(),
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
                    onRescanSource = {},
                    onRemoveSource = {},
                    onRemoveMissingTracks = { _, _ -> },
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
                    sources = emptyList(),
                    importMessage = null,
                    scanProgress = null,
                    scanErrors = emptyList(),
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
                    onRescanSource = {},
                    onRemoveSource = {},
                    onRemoveMissingTracks = { _, _ -> },
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
    fun emptyManagerAndActiveScanExposeEmptyAndCancelStates() =
        runComposeUiTest {
            var cancellations = 0
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = emptyList(),
                        progress = ScanProgress(session(ScanStatus.Scanning)),
                        onCancelScan = { cancellations++ },
                    )
                }
            }
            waitForIdle()

            onNode(hasText("Add a music folder to start your local library."))
                .assertExists()
            onNode(hasText("Scanning…")).assertExists()
            onNode(hasText("Cancel")).performClick()
            waitForIdle()

            assertEquals(1, cancellations)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completedScanExposesReportRescanAndRemoveMissingCallbacks() =
        runComposeUiTest {
            val source = source()
            val session = session(ScanStatus.Completed)
            val rescanned = mutableListOf<LibrarySource>()
            val missingRemoved =
                mutableListOf<Pair<LibrarySource, ScanSession>>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session),
                        errors = listOf(scanError(session)),
                        onRescan = { rescanned += it },
                        onRemoveMissing = { selectedSource, selectedSession ->
                            missingRemoved += selectedSource to selectedSession
                        },
                    )
                }
            }
            waitForIdle()

            onNode(hasText("Scan complete")).assertExists()
            onNode(hasText("View scan report")).performClick()
            onNode(hasText("broken.mp3: Unsupported file")).assertExists()
            onAllNodes(hasText("Rescan"))[0].performClick()
            onNode(hasText("Remove missing files")).performClick()
            waitForIdle()

            assertEquals(listOf(source), rescanned)
            assertEquals(listOf(source to session), missingRemoved)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanReportRemainsExpandedAfterManagerLazyItemIsRecreated() =
        runComposeUiTest {
            val source = source()
            val session = session(ScanStatus.Completed)
            setContent {
                Box(Modifier.size(800.dp, 600.dp)) {
                    managerContent(
                        sources = listOf(source),
                        tracks = manyAlbumTracks(),
                        browseMode = BrowseMode.Albums,
                        progress = ScanProgress(session),
                        errors = listOf(scanError(session)),
                    )
                }
            }
            waitForIdle()

            onNode(hasText("View scan report")).performClick()
            onNode(hasText("broken.mp3: Unsupported file")).assertExists()
            onNode(hasScrollToIndexAction()).performTouchInput {
                repeat(20) { swipeUp() }
            }
            waitForIdle()
            onAllNodes(hasText("Hide scan report")).assertCountEquals(0)
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            waitForIdle()

            onNode(hasText("Hide scan report")).assertExists()
            onNode(hasText("broken.mp3: Unsupported file")).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanReportCollapsesWhenDisplayedSessionChanges() = runComposeUiTest {
        val source = source()
        var displayedSession by mutableStateOf(session(ScanStatus.Completed))
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                managerContent(
                    sources = listOf(source),
                    progress = ScanProgress(displayedSession),
                    errors = listOf(scanError(displayedSession)),
                )
            }
        }
        waitForIdle()

        onNode(hasText("View scan report")).performClick()
        onNode(hasText("Hide scan report")).assertExists()
        displayedSession = session(ScanStatus.Completed, id = "next-scan")
        waitForIdle()

        onNode(hasText("View scan report")).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completedScanManagerStateAndActionsMatchAtCompactAndWideContentWidths() =
        runComposeUiTest {
            val source = source()
            val session = session(ScanStatus.Completed)
            val compactActions = mutableListOf<String>()
            val wideActions = mutableListOf<String>()

            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session),
                        onRescan = { compactActions += "rescan:${it.id}" },
                        onRemoveMissing = { selectedSource, selectedSession ->
                            compactActions +=
                                "remove-missing:${selectedSource.id}:${selectedSession.id}"
                        },
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Rescan"))[0].performClick()
            onNode(hasText("Remove missing files")).performClick()
            waitForIdle()

            setContent {
                Box(Modifier.size(1200.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session),
                        onRescan = { wideActions += "rescan:${it.id}" },
                        onRemoveMissing = { selectedSource, selectedSession ->
                            wideActions +=
                                "remove-missing:${selectedSource.id}:${selectedSession.id}"
                        },
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Rescan"))[0].performClick()
            onNode(hasText("Remove missing files")).performClick()
            waitForIdle()

            assertEquals(compactActions, wideActions)
            assertEquals(
                listOf("rescan:source", "remove-missing:source:scan"),
                compactActions,
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun addSourceActionIsVisibleAndLaunchesTheAvailablePicker() =
        runComposeUiTest {
            AvailableStubPicker.launches = 0
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source()),
                        tracks = listOf(managerTrack()),
                        folderPickerLauncher = AvailableStubPicker,
                        sourcePickerActionVisible = true,
                    )
                }
            }
            waitForIdle()

            onNode(hasText("Add music folder")).performClick()
            waitForIdle()

            assertEquals(1, AvailableStubPicker.launches)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noResultPickerLeavesCallerOwnedManagerStateUnchangedAfterAddSourceAction() =
        runComposeUiTest {
            val initialSources = listOf(source())
            var managerSources by mutableStateOf(initialSources)
            var pickerResultCallbacks = 0
            val picker = NoResultStubPicker {
                pickerResultCallbacks++
                managerSources = managerSources + source()
            }
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = managerSources,
                        tracks = listOf(managerTrack()),
                        folderPickerLauncher = picker,
                        sourcePickerActionVisible = true,
                    )
                }
            }
            waitForIdle()

            onNode(hasText("Add music folder")).performClick()
            waitForIdle()

            assertEquals(1, picker.launches)
            assertEquals(0, pickerResultCallbacks)
            assertEquals(initialSources, managerSources)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun coordinatorDisabledStateDisablesCompletedScanMutationControls() =
        runComposeUiTest {
            val source = source()
            val session = session(ScanStatus.Completed)
            val rescanned = mutableListOf<LibrarySource>()
            val missingRemoved =
                mutableListOf<Pair<LibrarySource, ScanSession>>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session),
                        mutationsEnabled = false,
                        onRescan = { rescanned += it },
                        onRemoveMissing = { selectedSource, selectedSession ->
                            missingRemoved += selectedSource to selectedSession
                        },
                    )
                }
            }
            waitForIdle()

            onNode(hasText("View scan report")).performClick()
            onAllNodes(hasText("Rescan"))[0].assertIsNotEnabled()
            onNode(hasText("Remove missing files")).assertIsNotEnabled()
            waitForIdle()

            assertTrue(rescanned.isEmpty())
            assertTrue(missingRemoved.isEmpty())
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun restoredTerminalStatesRenderThroughProductionContentAndRetryWhenRequired() =
        runComposeUiTest {
            val source = source()
            var status by mutableStateOf(ScanStatus.Completed)
            val rescanned = mutableListOf<LibrarySource>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session(status)),
                        onRescan = { rescanned += it },
                    )
                }
            }
            waitForIdle()

            onNode(hasText("Scan complete")).assertExists()
            status = ScanStatus.Failed
            waitForIdle()
            onNode(hasText("Scan failed")).assertExists()
            onNode(hasText("Retry scan")).performClick()
            status = ScanStatus.Cancelled
            waitForIdle()
            onNode(hasText("Scan cancelled")).assertExists()
            onNode(hasText("Retry scan")).performClick()

            assertEquals(listOf(source, source), rescanned)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun managerActionsMatchAtCompactAndWideContentWidths() = runComposeUiTest {
        val source = source()
        val completedSession = session(ScanStatus.Completed)
        val compactActions = mutableListOf<String>()
        val wideActions = mutableListOf<String>()
        AvailableStubPicker.launches = 0

        listOf(420.dp to compactActions, 1200.dp to wideActions).forEach {
            (width, actions) ->
            setContent {
                Box(Modifier.size(width, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        tracks = listOf(managerTrack()),
                        progress = ScanProgress(session(ScanStatus.Scanning)),
                        folderPickerLauncher = AvailableStubPicker,
                        sourcePickerActionVisible = true,
                        onCancelScan = { actions += "cancel" },
                    )
                }
            }
            waitForIdle()
            onNode(hasText("Cancel")).performClick()
            onNode(hasText("Add music folder")).performClick()

            setContent {
                Box(Modifier.size(width, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session(ScanStatus.Cancelled)),
                        errors = listOf(scanError(completedSession)),
                        onRescan = { actions += "retry-cancelled:${it.id}" },
                    )
                }
            }
            waitForIdle()
            onNode(hasText("Retry scan")).performClick()
            onNode(hasText("View scan report")).performClick()
            onNode(hasText("broken.mp3: Unsupported file")).assertExists()

            setContent {
                Box(Modifier.size(width, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(session(ScanStatus.Failed)),
                        onRescan = { actions += "retry-failed:${it.id}" },
                    )
                }
            }
            waitForIdle()
            onNode(hasText("Retry scan")).performClick()

            setContent {
                Box(Modifier.size(width, 900.dp)) {
                    managerContent(
                        sources =
                            listOf(
                                source(
                                    accessStatus =
                                        LibrarySourceAccessStatus.LostAccess)),
                        folderPickerLauncher = AvailableStubPicker,
                    )
                }
            }
            waitForIdle()
            onNode(hasText("Choose folder again")).performClick()

            setContent {
                Box(Modifier.size(width, 900.dp)) {
                    managerContent(
                        sources = listOf(source),
                        progress = ScanProgress(completedSession),
                        onRescan = { actions += "rescan:${it.id}" },
                        onRemoveMissing = { selectedSource, selectedSession ->
                            actions +=
                                "remove-missing:${selectedSource.id}:${selectedSession.id}"
                        },
                    )
                }
            }
            waitForIdle()
            onAllNodes(hasText("Rescan"))[1].performClick()
            onNode(hasText("Remove missing files")).performClick()
            waitForIdle()
        }

        assertEquals(compactActions, wideActions)
        assertEquals(
            listOf(
                "cancel",
                "retry-cancelled:source",
                "retry-failed:source",
                "rescan:source",
                "remove-missing:source:scan",
            ),
            compactActions,
        )
        assertEquals(4, AvailableStubPicker.launches)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lostAccessSourceOffersRecoveryAndRemovalCallbacks() = runComposeUiTest {
        val source = source(accessStatus = LibrarySourceAccessStatus.LostAccess)
        val removed = mutableListOf<LibrarySource>()
        AvailableStubPicker.launches = 0
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                managerContent(
                    sources = listOf(source),
                    folderPickerLauncher = AvailableStubPicker,
                    onRemove = { removed += it },
                )
            }
        }
        waitForIdle()

        onNode(hasText("Access lost")).assertExists()
        onNode(hasText("Choose folder again")).performClick()
        onNode(hasText("Remove folder")).performClick()
        waitForIdle()

        assertEquals(1, AvailableStubPicker.launches)
        assertEquals(listOf(source), removed)
    }

    @Composable
    private fun managerContent(
        sources: List<LibrarySource>,
        tracks: List<Track> = emptyList(),
        browseMode: BrowseMode = BrowseMode.Songs,
        progress: ScanProgress? = null,
        errors: List<ScanError> = emptyList(),
        folderPickerLauncher: PlatformFolderPickerLauncher = StubPicker,
        sourcePickerActionVisible: Boolean = false,
        mutationsEnabled: Boolean = true,
        onRescan: (LibrarySource) -> Unit = {},
        onRemove: (LibrarySource) -> Unit = {},
        onRemoveMissing: (LibrarySource, ScanSession) -> Unit = { _, _ -> },
        onCancelScan: () -> Unit = {},
    ) {
        LibraryHomeContent(
            title = "Library",
            subtitle = "",
            tracks = tracks,
            browseMode = browseMode,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = sourcePickerActionVisible,
            sources = sources,
            importMessage = null,
            scanProgress = progress,
            scanErrors = errors,
            mutationsEnabled = mutationsEnabled,
            currentTrackId = null,
            selectionModeActive = false,
            selectedTrackIds = emptySet(),
            labels = labels(),
            homeBackdrop = null,
            artworkLoader = { null },
            onBrowseModeChange = {},
            onClearLibrary = {},
            onCancelScan = onCancelScan,
            onRescanSource = onRescan,
            onRemoveSource = onRemove,
            onRemoveMissingTracks = onRemoveMissing,
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

    private fun source(
        accessStatus: LibrarySourceAccessStatus =
            LibrarySourceAccessStatus.Available,
    ): LibrarySource =
        LibrarySource(
            id = "source",
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = "Music",
            handle = "/music",
            createdAtEpochMillis = 1L,
            accessStatus = accessStatus,
        )

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

    private fun scanError(session: ScanSession): ScanError =
        ScanError(
            id = "error",
            scanId = session.id,
            sourceLocalKey = "broken.mp3",
            displayPath = "broken.mp3",
            reason = "Unsupported file",
            recoverable = true,
            createdAtEpochMillis = 1L,
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

    private fun manyAlbumTracks(): List<Track> =
        List(80) { index ->
            track(
                id = "scroll-${index + 1}",
                title = "Scroll track ${index + 1}",
                album = "Scroll album ${index + 1}",
                artist = "Scroll artist",
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

    private class NoResultStubPicker(
        private val onResult: () -> Unit,
    ) : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = true
        override val supportsAdditionalSources: Boolean = true
        var launches: Int = 0

        override fun launch() {
            launches++
        }
    }
}
