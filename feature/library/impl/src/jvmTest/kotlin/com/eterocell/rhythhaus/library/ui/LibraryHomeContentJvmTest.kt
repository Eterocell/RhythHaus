package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackMetadataEditorData
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.ui.LocalHausDesktopDensity
import com.eterocell.rhythhaus.ui.songArtistAlbumSubtitle
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryHomeContentJvmTest {
    private var previousLocale: Locale? = null

    @BeforeTest
    fun setEnglishLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        previousLocale?.let(Locale::setDefault)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun songsApplyQueryProjectionsAndReportPrimitiveCallbacks() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            val scrolls = mutableListOf<Pair<Int, Int>>()
            val plays = mutableListOf<Pair<List<Track>, Track>>()
            var browseQuery by mutableStateOf(LibraryBrowseQuery())
            val expectedTracks =
                listOf(
                    tracks()[1],
                    tracks()[2],
                    tracks()[0],
                    tracks()[3],
                )
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = browseQuery,
                        sourceOptions = emptyList(),
                        sourceIdByTrackId =
                            mapOf(
                                "t-1" to "source-a",
                                "t-2" to "source-a",
                                "t-3" to "source-b",
                                "t-4" to "source-a",
                            ),
                        modifiedAtByTrackId =
                            mapOf(
                                "t-1" to 20L,
                                "t-2" to 10L,
                                "t-3" to 30L,
                                "t-4" to null,
                            ),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            assertEquals(listOf(0, 0), scrolls.first().toList())
            assertEquals(
                expectedTracks.map(Track::id),
                visibleReports.last(),
            )
            onAllNodes(hasText("Now playing")).assertCountEquals(1)

            browseQuery =
                LibraryBrowseQuery(
                    sort = LibrarySort.Modified,
                    direction = LibrarySortDirection.Descending,
                    sourceId = "source-a",
                )
            waitForIdle()
            val queryProjectedTracks =
                listOf(
                    tracks()[0],
                    tracks()[1],
                    tracks()[3],
                )
            assertEquals(
                queryProjectedTracks.map(Track::id),
                visibleReports.last(),
            )

            onAllNodes(hasContentDescription("Two"))[0].performClick()
            waitForIdle()
            assertEquals(queryProjectedTracks, plays.last().first)
            assertEquals(tracks()[0].id, plays.last().second.id)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun namedRowMenuStartsSelectionWithoutPlayback() = runComposeUiTest {
        val selectionStarts = mutableListOf<String>()
        var playRequests = 0
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    tracks = tracks(),
                    browseMode = BrowseMode.Songs,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> playRequests++ },
                    onToggleSelection = {},
                    onStartSelection = { selectionStarts += it },
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }

        onNode(hasContentDescription("More actions for One")).performClick()
        onNodeWithTag("track-row-select").performClick()
        waitForIdle()

        assertEquals(listOf("t-2"), selectionStarts)
        assertEquals(0, playRequests)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playableRowsNameTheirTrackAndKeepSelectAsALongPressLabel() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            val row =
                onNode(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ContentDescription,
                            listOf("One"),
                        ),
                    )
                    .fetchSemanticsNode()
            assertEquals(
                "Select One",
                row.config.getOrNull(SemanticsActions.OnLongClick)?.label,
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowDensityUsesRootProjectionWhenHomeContentIsNarrow() =
        runComposeUiTest {
            var desktopDensity by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(
                    LocalHausDesktopDensity provides desktopDensity,
                ) {
                    Box(Modifier.size(672.dp, 900.dp)) {
                        LibraryHomeContent(
                            tracks = tracks().take(1),
                            browseMode = BrowseMode.Songs,
                            playHistory = emptyMap(),
                            createdAtByTrackId = emptyMap(),
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
                            onPlayTrack = { _, _ -> },
                            onToggleSelection = {},
                            onStartSelection = {},
                            onVisibleTrackIdsChanged = {},
                            onScrollPositionChanged = { _, _ -> },
                            bottomContentPadding = 0.dp,
                            favoriteTrackIds = emptySet(),
                            artworkTrackIds = emptySet(),
                            onSetTrackFavorite = { _, _ -> },
                            browseQuery = LibraryBrowseQuery(),
                            sourceOptions = emptyList(),
                            sourceIdByTrackId = emptyMap(),
                            modifiedAtByTrackId = emptyMap(),
                            onBrowseSortChange = {},
                            onBrowseSortDirectionChange = {},
                            onBrowseFavoriteOnlyChange = {},
                            onBrowseArtworkOnlyChange = {},
                            onBrowseSourceIdChange = {},
                        )
                    }
                }
            }
            waitForIdle()

            val desktopRowHeight =
                onNode(hasContentDescription("Two"))
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .height
            desktopDensity = false
            waitForIdle()
            val compactRowHeight =
                onNode(hasContentDescription("Two"))
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .height

            assertTrue(
                desktopRowHeight < compactRowHeight,
                "desktop root density must control rows in a 672dp Home pane; " +
                    "heights were $desktopRowHeight and $compactRowHeight px",
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queryAndModeChangesPublishAllProjectedHomeIdsWithoutPlaybackRequests() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            var playRequests = 0
            var browseMode by mutableStateOf(BrowseMode.Songs)
            var browseQuery by mutableStateOf(LibraryBrowseQuery())
            setContent {
                // The controls alone exceed this viewport, so any IDs reported
                // here must come from the full projection rather than laid-out
                // rows.
                Box(Modifier.size(420.dp, 160.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = browseMode,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> playRequests++ },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { ids ->
                            visibleReports += ids
                        },
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = setOf("t-1", "t-4"),
                        artworkTrackIds = setOf("t-1"),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = browseQuery,
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            assertEquals(
                listOf("t-2", "t-3", "t-1", "t-4"), visibleReports.last())

            browseQuery =
                LibraryBrowseQuery(direction = LibrarySortDirection.Descending)
            waitForIdle()
            assertEquals(
                listOf("t-4", "t-1", "t-3", "t-2"), visibleReports.last())

            // Routine rows contain no artwork bytes; persisted artwork
            // presence still keeps the artwork-bearing track visible.
            browseQuery = LibraryBrowseQuery(artworkOnly = true)
            waitForIdle()
            assertEquals(listOf("t-1"), visibleReports.last())

            browseQuery =
                LibraryBrowseQuery(direction = LibrarySortDirection.Descending)
            waitForIdle()

            browseMode = BrowseMode.Favorites
            waitForIdle()
            assertEquals(listOf("t-4", "t-1"), visibleReports.last())
            assertEquals(0, playRequests)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun favoriteToggleRequestsDesiredStateAndRendersProjectionState() =
        runComposeUiTest {
            var favoriteTrackIds by mutableStateOf(emptySet<String>())
            val requests = mutableListOf<Pair<String, Boolean>>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = tracks().take(1),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = favoriteTrackIds,
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { id, favorite ->
                            requests += id to favorite
                            favoriteTrackIds =
                                if (favorite) favoriteTrackIds + id
                                else favoriteTrackIds - id
                        },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            onNode(
                    hasContentDescription("Add Two to favorites") and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.Off,
                        ),
                )
                .performClick()
            waitForIdle()

            assertEquals(listOf("t-1" to true), requests)
            onNode(
                    hasContentDescription("Remove Two from favorites") and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.On,
                        ),
                )
                .assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun favoriteControlDoesNotBlockTrackRowGestures() = runComposeUiTest {
        var favoriteTrackIds by mutableStateOf(emptySet<String>())
        val plays = mutableListOf<Pair<List<Track>, Track>>()
        val selectionStarts = mutableListOf<String>()
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    tracks = tracks().take(1),
                    browseMode = BrowseMode.Songs,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { ordered, selected ->
                        plays += ordered to selected
                    },
                    onToggleSelection = {},
                    onStartSelection = { selectionStarts += it },
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = favoriteTrackIds,
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { id, favorite ->
                        favoriteTrackIds =
                            if (favorite) favoriteTrackIds + id
                            else favoriteTrackIds - id
                    },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        onNode(hasContentDescription("Add Two to favorites")).performClick()
        waitForIdle()
        assertEquals(emptyList(), plays)
        assertEquals(emptyList(), selectionStarts)

        onNode(hasContentDescription("Two")).performClick()
        waitForIdle()
        assertEquals(listOf("t-1"), plays.single().first.map(Track::id))
        assertEquals("t-1", plays.single().second.id)

        onNode(hasContentDescription("Two")).performTouchInput {
            longClick()
        }
        waitForIdle()
        assertEquals(listOf("t-1"), selectionStarts)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowActionsMeetMobileMinimumTouchTargets() = runComposeUiTest {
        setContent {
            Box(Modifier.size(420.dp, 100.dp)) {
                TrackRow(
                    track = track("t-1", "Two", "Album B", "Artist", 1, 1),
                    isNowPlaying = false,
                    selectionModeActive = false,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = {},
                    onToggleSelection = {},
                    onStartSelection = {},
                    favorite = false,
                    onSetFavorite = {},
                    onEditMetadata = {},
                )
            }
        }
        waitForIdle()

        onNodeWithTag("track-row-more-actions")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        onNode(hasContentDescription("Add Two to favorites"))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun favoriteControlIsAbsentDuringSelectionMode() = runComposeUiTest {
        var selectionModeActive by mutableStateOf(false)
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    tracks = tracks().take(1),
                    browseMode = BrowseMode.Songs,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
                    folderPickerLauncher = StubPicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = selectionModeActive,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = {},
                    onClearLibrary = {},
                    onCancelScan = {},
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = setOf("t-1"),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()
        onNode(hasContentDescription("Remove Two from favorites"))
            .assertIsDisplayed()

        selectionModeActive = true
        waitForIdle()
        onAllNodes(hasContentDescription("Add Two to favorites"))
            .assertCountEquals(0)
        onAllNodes(hasContentDescription("Remove Two from favorites"))
            .assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun favoritesRenderOnlyMembersAndPlayTheVisibleQueueAtCompactWidth() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            val plays = mutableListOf<Pair<List<Track>, Track>>()
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = BrowseMode.Favorites,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
                        folderPickerLauncher = StubPicker,
                        sourcePickerActionVisible = true,
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
                        onPlayTrack = { ordered, selected ->
                            plays += ordered to selected
                        },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { visibleReports += it },
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = setOf("t-2", "t-3"),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            assertEquals(listOf("t-2", "t-3"), visibleReports.last())
            onAllNodes(hasText("One")).assertCountEquals(1)
            onAllNodes(hasText("Solo")).assertCountEquals(1)
            onAllNodes(hasText("Two")).assertCountEquals(0)

            onAllNodes(hasContentDescription("One"))[0].performClick()
            waitForIdle()
            assertEquals(
                listOf("t-2", "t-3"), plays.single().first.map { it.id })
            assertEquals("t-2", plays.single().second.id)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun recentModeTapQueuesOnlyDisplayedOrderedTracks() = runComposeUiTest {
        val plays = mutableListOf<Pair<List<Track>, Track>>()
        val history =
            mapOf(
                "t-1" to TrackPlayHistory("t-1", 1L, 100L),
                "t-2" to TrackPlayHistory("t-2", 1L, 300L),
                "t-3" to TrackPlayHistory("t-3", 1L, 200L),
            )
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    tracks = tracks(),
                    browseMode = BrowseMode.RecentlyPlayed,
                    playHistory = history,
                    createdAtByTrackId =
                        tracks().associate { it.id to it.id.length.toLong() },
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
                    onPlayTrack = { ordered, selected ->
                        plays += ordered to selected
                    },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()
        onNode(hasContentDescription("One")).performClick()
        waitForIdle()
        assertEquals(
            listOf("t-2", "t-3", "t-1"), plays.single().first.map { it.id })
        assertEquals("t-2", plays.single().second.id)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyRecentlyPlayedShowsLocalizedMessageWithoutImportAction() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = BrowseMode.RecentlyPlayed,
                        playHistory = emptyMap(),
                        createdAtByTrackId = tracks().associate { it.id to 1L },
                        folderPickerLauncher = AvailableStubPicker,
                        sourcePickerActionVisible = true,
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()
            onNode(hasText("No recently played tracks yet."))
                .assertIsDisplayed()
            onAllNodes(hasText("Add music folder")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyFavoritesShowDedicatedStateWithoutImportAtWideWidth() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            setContent {
                Box(Modifier.size(1000.dp, 700.dp)) {
                    LibraryHomeContent(
                        tracks = tracks(),
                        browseMode = BrowseMode.Favorites,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
                        folderPickerLauncher = StubPicker,
                        sourcePickerActionVisible = true,
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { visibleReports += it },
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            assertEquals(emptyList(), visibleReports.last())
            onAllNodes(hasText("No favorite tracks yet.")).assertCountEquals(1)
            onAllNodes(hasText("Add music folder")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun homeScrollReportsPrimitivePositions() = runComposeUiTest {
        val scrolls = mutableListOf<Pair<Int, Int>>()
        setContent {
            Box(Modifier.size(420.dp, 300.dp)) {
                LibraryHomeContent(
                    tracks = twelveTracks(),
                    browseMode = BrowseMode.Songs,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { index, offset ->
                        scrolls += index to offset
                    },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        onNode(hasScrollToIndexAction()).performScrollToIndex(12)
        waitForIdle()
        assertTrue(scrolls.any { it.first > 0 || it.second > 0 })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun albumsGroupByExactNameSortedCaseInsensitively() = runComposeUiTest {
        var browseMode by mutableStateOf(BrowseMode.Albums)
        setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                LibraryHomeContent(
                    tracks = tracks(),
                    browseMode = browseMode,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Album B")).assertCountEquals(1)
        onAllNodes(hasText("album a")).assertCountEquals(1)
        onAllNodes(hasText("Artist · 3 tracks")).assertCountEquals(1)
        onAllNodes(hasText("Soloist · 1 tracks")).assertCountEquals(1)
        onNode(hasText("album a")).assertIsDisplayed()
        onNodeWithTag("library-track-count").assertIsDisplayed()
        onAllNodes(hasText("Library", substring = false)).assertCountEquals(0)
        onNode(hasText("4 tracks", substring = false)).assertIsDisplayed()

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
                    tracks = tracks(),
                    browseMode = browseMode,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        onAllNodes(hasContentDescription("Artist Artist")).assertCountEquals(1)
        onAllNodes(hasContentDescription("Artist Soloist")).assertCountEquals(1)
        onAllNodes(hasText("1 albums · 3 tracks")).assertCountEquals(1)
        onAllNodes(hasText("1 albums · 1 tracks")).assertCountEquals(1)

        onAllNodes(hasContentDescription("Artist Soloist"))[0].performClick()
        waitForIdle()
        assertEquals(listOf("Soloist"), openedArtists)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactHomeExposesAllBrowseModesWithinTwoActions() = runComposeUiTest {
        var browseMode by mutableStateOf(BrowseMode.Songs)
        setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                LibraryHomeContent(
                    tracks = tracks(),
                    browseMode = browseMode,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        val primaryModes =
            listOf(
                "Albums" to BrowseMode.Albums,
                "Artists" to BrowseMode.Artists,
                "Songs" to BrowseMode.Songs,
            )
        primaryModes.forEach { (label, mode) ->
            val option = onAllNodes(hasText(label))
            option.assertCountEquals(1)
            option[0].assertIsDisplayed().performClick()
            waitForIdle()
            assertEquals(mode, browseMode)
        }

        onNodeWithTag("library-secondary-browse-menu")
            .assert(hasContentDescription("More library views"))
        val secondaryModes =
            listOf(
                "Favorites" to BrowseMode.Favorites,
                "Recently played" to BrowseMode.RecentlyPlayed,
                "Recently added" to BrowseMode.RecentlyAdded,
            )
        secondaryModes.forEach { (label, mode) ->
            onNodeWithTag("library-secondary-browse-menu").performClick()
            waitForIdle()
            val option = onAllNodes(hasText(label))
            option.assertCountEquals(1)
            option[0].performClick()
            waitForIdle()
            assertEquals(mode, browseMode)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyLibraryShowsImportCardAndActiveScanWithCancel() =
        runComposeUiTest {
            var cancellations = 0
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        tracks = emptyList(),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Add music folder")).assertCountEquals(2)
            onNode(hasText("Scanning…")).assertIsDisplayed()
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
                    tracks = listOf(managerTrack()),
                    browseMode = BrowseMode.Songs,
                    playHistory = emptyMap(),
                    createdAtByTrackId = emptyMap(),
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
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                    favoriteTrackIds = emptySet(),
                    artworkTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    browseQuery = LibraryBrowseQuery(),
                    sourceOptions = emptyList(),
                    sourceIdByTrackId = emptyMap(),
                    modifiedAtByTrackId = emptyMap(),
                    onBrowseSortChange = {},
                    onBrowseSortDirectionChange = {},
                    onBrowseFavoriteOnlyChange = {},
                    onBrowseArtworkOnlyChange = {},
                    onBrowseSourceIdChange = {},
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
                        tracks = emptyList(),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Add music folder")).assertCountEquals(2)
            onAllNodes(hasText("Scan complete")).assertCountEquals(0)
            onAllNodes(hasText("Remove missing files")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun songsReconcilePayloadKeepsEveryTrackAfterScrolling() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            setContent {
                Box(Modifier.size(420.dp, 300.dp)) {
                    LibraryHomeContent(
                        tracks = twelveTracks(),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
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
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { visibleReports += it },
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(),
                        sourceOptions = emptyList(),
                        sourceIdByTrackId = emptyMap(),
                        modifiedAtByTrackId = emptyMap(),
                        onBrowseSortChange = {},
                        onBrowseSortDirectionChange = {},
                        onBrowseFavoriteOnlyChange = {},
                        onBrowseArtworkOnlyChange = {},
                        onBrowseSourceIdChange = {},
                    )
                }
            }
            waitForIdle()

            onNode(hasScrollToIndexAction()).performScrollToIndex(10)
            waitForIdle()
            assertEquals(
                listOf(
                    "t-1",
                    "t-10",
                    "t-11",
                    "t-12",
                    "t-2",
                    "t-3",
                    "t-4",
                    "t-5",
                    "t-6",
                    "t-7",
                    "t-8",
                    "t-9",
                ),
                visibleReports.last(),
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun visibleBrowseAndFilteredEmptyStatesReflowWithoutLosingMode() =
        runComposeUiTest {
            val longTitle =
                "A deliberately long local track title that remains accessible after reflow"
            val adaptiveTracks =
                List(12) { index ->
                    track(
                        id = "adaptive-${index + 1}",
                        title =
                            if (index == 11) longTitle
                            else "Track ${index + 1}",
                        album = "Album ${index % 4}",
                        artist = "Artist ${index % 5}",
                        disc = 1,
                        number = index + 1,
                    )
                }
            val favoriteIds = setOf(adaptiveTracks.first().id)
            val artworkIds = setOf(adaptiveTracks[1].id)
            val history =
                adaptiveTracks
                    .mapIndexed { index, track ->
                        track.id to
                            TrackPlayHistory(track.id, 1L, index.toLong())
                    }
                    .toMap()
            val createdAt =
                adaptiveTracks
                    .mapIndexed { index, track ->
                        track.id to index.toLong()
                    }
                    .toMap()
            var width by mutableStateOf(360.dp)
            var pageMargin by mutableStateOf(16.dp)
            var fontScale by mutableStateOf(1f)
            var browseMode by mutableStateOf(BrowseMode.Albums)
            var browseQuery by mutableStateOf(LibraryBrowseQuery())
            val visibleReports = mutableListOf<List<String>>()
            val scrollPositions = mutableListOf<Pair<Int, Int>>()

            setContent {
                val systemDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides
                        Density(systemDensity.density, fontScale),
                    LocalHausDesktopDensity provides (width >= 720.dp),
                ) {
                    Box(
                        Modifier.size(width, 760.dp)
                            .padding(horizontal = pageMargin),
                    ) {
                        LibraryHomeContent(
                            tracks = adaptiveTracks,
                            browseMode = browseMode,
                            playHistory = history,
                            createdAtByTrackId = createdAt,
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
                            onPlayTrack = { _, _ -> },
                            onToggleSelection = {},
                            onStartSelection = {},
                            onVisibleTrackIdsChanged = {
                                visibleReports += it
                            },
                            onScrollPositionChanged = { index, offset ->
                                scrollPositions += index to offset
                            },
                            bottomContentPadding = 0.dp,
                            favoriteTrackIds = favoriteIds,
                            artworkTrackIds = artworkIds,
                            onSetTrackFavorite = { _, _ -> },
                            browseQuery = browseQuery,
                            sourceOptions = emptyList(),
                            sourceIdByTrackId =
                                adaptiveTracks.associate { it.id to "source" },
                            modifiedAtByTrackId =
                                adaptiveTracks.associate { it.id to 1L },
                            onBrowseSortChange = {},
                            onBrowseSortDirectionChange = {},
                            onBrowseFavoriteOnlyChange = {
                                browseQuery =
                                    browseQuery.copy(favoriteOnly = it)
                            },
                            onBrowseArtworkOnlyChange = {
                                browseQuery = browseQuery.copy(artworkOnly = it)
                            },
                            onBrowseSourceIdChange = {
                                browseQuery = browseQuery.copy(sourceId = it)
                            },
                        )
                    }
                }
            }

            onNodeWithContentDescription("Album Album 0").assertIsDisplayed()
            onNodeWithTag("library-track-count").assertIsDisplayed()
            onAllNodes(
                    hasText("AL", substring = false),
                    useUnmergedTree = true,
                )
                .assertCountEquals(4)
            onNodeWithContentDescription("Artists").performClick()
            waitForIdle()
            assertEquals(BrowseMode.Artists, browseMode)
            onNodeWithContentDescription("Songs").performClick()
            waitForIdle()
            assertEquals(BrowseMode.Songs, browseMode)

            fun selectCompactMode(label: String, mode: BrowseMode) {
                onNodeWithTag("library-secondary-browse-menu").performClick()
                onNodeWithText(label, substring = false).performClick()
                waitForIdle()
                assertEquals(mode, browseMode)
            }

            selectCompactMode("Favorites", BrowseMode.Favorites)
            selectCompactMode("Recently played", BrowseMode.RecentlyPlayed)
            selectCompactMode("Recently added", BrowseMode.RecentlyAdded)
            val visibleBeforeResize = visibleReports.last()

            width = 840.dp
            pageMargin = 24.dp
            fontScale = 2f
            waitForIdle()
            assertEquals(BrowseMode.RecentlyAdded, browseMode)
            assertEquals(visibleBeforeResize, visibleReports.last())
            onNodeWithTag("library-browse-mode-row").assertExists()
            onNodeWithContentDescription("Recently played")
                .assertWidthIsAtLeast(176.dp)
            onNodeWithTag("library-browse-mode-row").performTouchInput {
                swipe(
                    center + Offset(center.x * 0.6f, 0f),
                    center - Offset(center.x * 0.6f, 0f),
                )
            }
            onNodeWithContentDescription("Recently played").performClick()
            waitForIdle()
            assertEquals(BrowseMode.RecentlyPlayed, browseMode)
            onNodeWithContentDescription("Recently added").performClick()
            waitForIdle()
            assertEquals(BrowseMode.RecentlyAdded, browseMode)
            onNodeWithTag("library-browse-mode-row").performTouchInput {
                swipe(
                    center - Offset(center.x * 0.6f, 0f),
                    center + Offset(center.x * 0.6f, 0f),
                )
            }
            onNodeWithContentDescription("Songs").performClick()
            waitForIdle()
            assertEquals(BrowseMode.Songs, browseMode)
            browseQuery =
                LibraryBrowseQuery(favoriteOnly = true, artworkOnly = true)
            waitForIdle()
            onNodeWithText("No tracks match the current filters.")
                .assertIsDisplayed()
            assertEquals(emptyList(), visibleReports.last())

            width = 1_200.dp
            pageMargin = 32.dp
            fontScale = 1f
            waitForIdle()
            assertEquals(BrowseMode.Songs, browseMode)
            assertTrue(browseQuery.favoriteOnly && browseQuery.artworkOnly)
            onNodeWithText("No tracks match the current filters.")
                .assertIsDisplayed()
            onNodeWithTag("library-track-count")
                .assertLeftPositionInRootIsEqualTo(32.dp)
            onAllNodes(hasText("Library", substring = false))
                .assertCountEquals(0)

            browseQuery = LibraryBrowseQuery()
            waitForIdle()
            onNodeWithText(longTitle).performScrollTo().assertIsDisplayed()
            width = 840.dp
            pageMargin = 24.dp
            fontScale = 2f
            waitForIdle()
            onNodeWithText(longTitle).performScrollTo().assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionAndMetadataDraftStayReachableAt200Percent() =
        runComposeUiTest {
            val selectedTrack =
                track(
                    id = "metadata-track",
                    title = "A long selection title that remains accessible",
                    album = "Album",
                    artist = "Artist",
                    disc = 1,
                    number = 1,
                )
            val metadata =
                TrackMetadataEditorData(
                    scannedTrack =
                        LibraryTrack(
                            id = selectedTrack.id,
                            sourceId = "source",
                            sourceLocalKey = "metadata-track.mp3",
                            audioSource =
                                AudioSource.FilePath(
                                    "/music/metadata-track.mp3"),
                            displayName = "metadata-track.mp3",
                            title = selectedTrack.title,
                            artist = selectedTrack.artist,
                            album = selectedTrack.album,
                            durationMillis = 100_000L,
                            sizeBytes = null,
                            modifiedAtEpochMillis = null,
                            lastSeenScanId = "scan",
                            createdAtEpochMillis = 1L,
                            updatedAtEpochMillis = 1L,
                        ),
                    overrides = TrackMetadataOverride(),
                )
            var editorOpen by mutableStateOf(false)
            var draft by mutableStateOf(TrackMetadataDraft())
            var failed by mutableStateOf(false)
            val saveRequests = mutableListOf<TrackMetadataOverride>()

            setContent {
                val systemDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(systemDensity.density, 2f),
                ) {
                    Box(
                        Modifier.size(390.dp, 760.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        LibraryHomeContent(
                            tracks = listOf(selectedTrack),
                            browseMode = BrowseMode.Songs,
                            playHistory = emptyMap(),
                            createdAtByTrackId = emptyMap(),
                            folderPickerLauncher = StubPicker,
                            sourcePickerActionVisible = false,
                            importMessage = null,
                            scanProgress = null,
                            mutationsEnabled = true,
                            currentTrackId = null,
                            selectionModeActive = true,
                            selectedTrackIds = setOf(selectedTrack.id),
                            labels = labels(),
                            homeBackdrop = null,
                            artworkLoader = { null },
                            onBrowseModeChange = {},
                            onClearLibrary = {},
                            onCancelScan = {},
                            onOpenAlbum = {},
                            onOpenArtist = {},
                            onPlayTrack = { _, _ -> },
                            onToggleSelection = {},
                            onStartSelection = {},
                            onVisibleTrackIdsChanged = {},
                            onScrollPositionChanged = { _, _ -> },
                            bottomContentPadding = 0.dp,
                            favoriteTrackIds = emptySet(),
                            artworkTrackIds = emptySet(),
                            onSetTrackFavorite = { _, _ -> },
                            browseQuery = LibraryBrowseQuery(),
                            sourceOptions = emptyList(),
                            sourceIdByTrackId =
                                mapOf(selectedTrack.id to "source"),
                            modifiedAtByTrackId = mapOf(selectedTrack.id to 1L),
                            onBrowseSortChange = {},
                            onBrowseSortDirectionChange = {},
                            onBrowseFavoriteOnlyChange = {},
                            onBrowseArtworkOnlyChange = {},
                            onBrowseSourceIdChange = {},
                            onEditTrackMetadata = { trackId ->
                                assertEquals(selectedTrack.id, trackId)
                                failed = false
                                editorOpen = true
                            },
                        )
                        if (editorOpen) {
                            TrackMetadataEditDialog(
                                data = metadata,
                                draft = draft,
                                saving = false,
                                failed = failed,
                                onDraft = { draft = it },
                                onDismiss = { editorOpen = false },
                                onSave = {
                                    saveRequests += it
                                    failed = true
                                },
                            )
                        }
                    }
                }
            }

            onNode(
                    hasContentDescription(selectedTrack.title) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ToggleableState,
                            ToggleableState.On,
                        ),
                )
                .assertIsDisplayed()
            onNodeWithTag("track-row-more-actions")
                .performScrollTo()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            onNodeWithTag("track-row-edit-metadata").performClick()
            onNodeWithTag("metadata-title")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performTextInput("Draft title")
            onNodeWithContentDescription("Title Restore")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            onNodeWithTag("metadata-restore-all")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            assertEquals(TrackMetadataDraft(), draft)
            onNodeWithTag("metadata-title").performTextInput("Draft title")
            onNodeWithTag("metadata-save")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            onNodeWithText("Cancel")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            assertEquals("Draft title", draft.title)
            assertTrue(saveRequests.isEmpty())

            onNodeWithTag("track-row-more-actions")
                .performScrollTo()
                .performClick()
            onNodeWithTag("track-row-edit-metadata").performClick()
            onNodeWithTag("metadata-save")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            onNodeWithText("Could not save changes.", substring = true)
                .performScrollTo()
                .assertIsDisplayed()
            assertEquals("Draft title", draft.title)
            assertEquals(
                listOf(TrackMetadataOverride(title = "Draft title")),
                saveRequests,
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun simplifiedChineseFilteredEmptyStateRemainsReachable() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runComposeUiTest {
                var browseMode by mutableStateOf(BrowseMode.Songs)
                setContent {
                    Box(Modifier.size(390.dp, 760.dp)) {
                        LibraryHomeContent(
                            tracks = tracks(),
                            browseMode = browseMode,
                            playHistory = emptyMap(),
                            createdAtByTrackId = emptyMap(),
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
                            onPlayTrack = { _, _ -> },
                            onToggleSelection = {},
                            onStartSelection = {},
                            onVisibleTrackIdsChanged = {},
                            onScrollPositionChanged = { _, _ -> },
                            bottomContentPadding = 0.dp,
                            favoriteTrackIds = emptySet(),
                            artworkTrackIds = emptySet(),
                            onSetTrackFavorite = { _, _ -> },
                            browseQuery =
                                LibraryBrowseQuery(favoriteOnly = true),
                            sourceOptions = emptyList(),
                            sourceIdByTrackId = emptyMap(),
                            modifiedAtByTrackId = emptyMap(),
                            onBrowseSortChange = {},
                            onBrowseSortDirectionChange = {},
                            onBrowseFavoriteOnlyChange = {},
                            onBrowseArtworkOnlyChange = {},
                            onBrowseSourceIdChange = {},
                        )
                    }
                }
                onNodeWithText("没有符合当前筛选条件的曲目。").assertIsDisplayed()
                onNodeWithTag("library-secondary-browse-menu").performClick()
                onNodeWithText("最近添加").performClick()
                waitForIdle()
                assertEquals(BrowseMode.RecentlyAdded, browseMode)
                onNodeWithText("没有符合当前筛选条件的曲目。").assertIsDisplayed()
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blankArtistAndAlbumDoNotRenderASeparator() = runComposeUiTest {
        val track = tracks().first().copy(artist = "", album = "")
        setContent {
            Box(Modifier.size(420.dp, 100.dp)) {
                TrackRow(
                    track = track,
                    isNowPlaying = false,
                    selectionModeActive = false,
                    isSelected = false,
                    labels = labels(),
                    artworkLoader = { null },
                    onPlay = {},
                    onToggleSelection = {},
                    onStartSelection = {},
                    favorite = false,
                    onSetFavorite = {},
                )
            }
        }

        onAllNodes(hasText(" · ", substring = false)).assertCountEquals(0)
    }

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
            albumArt = "Album art",
            albumArtwork = "Album artwork",
            nowPlayingBadge = "Now playing",
            selectTrack = { title -> "Select $title" },
            trackArtistAlbum = { artist, album ->
                songArtistAlbumSubtitle(artist, album)
            },
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
