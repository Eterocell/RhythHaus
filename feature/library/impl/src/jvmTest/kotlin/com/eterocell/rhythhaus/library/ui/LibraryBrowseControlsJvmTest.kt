package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryBrowseControlsJvmTest {
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
    fun controlsExposeEverySortChoiceAndDirection() = runComposeUiTest {
        val sortChanges = mutableListOf<LibrarySort>()
        val directionChanges = mutableListOf<LibrarySortDirection>()
        setContent {
            Box(Modifier.size(1_200.dp, 1_200.dp)) {
                LibraryBrowseControls(
                    query = LibraryBrowseQuery(),
                    sourceOptions =
                        listOf(
                            LibraryBrowseSourceOption(
                                id = "source-a",
                                displayName = "Library A",
                            ),
                        ),
                    onSortChange = sortChanges::add,
                    onSortDirectionChange = directionChanges::add,
                    onFavoriteOnlyChange = {},
                    onArtworkOnlyChange = {},
                    onSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        listOf(
                "Title",
                "Artist",
                "Album",
                "Added",
                "Modified",
                "Play count",
                "Favorite",
            )
            .forEach { label ->
                onNode(hasText(label, substring = false)).assertIsDisplayed()
            }
        onNode(
                hasText("Ascending", substring = false) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Selected,
                        true,
                    ),
            )
            .assertIsDisplayed()
        onNode(
                hasText("Descending", substring = false) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Selected,
                        false,
                    ),
            )
            .assertIsDisplayed()

        onNode(hasText("Artist", substring = false)).performClick()
        onNode(hasText("Descending", substring = false)).performClick()
        waitForIdle()
        assertEquals(listOf(LibrarySort.Artist), sortChanges)
        assertEquals(
            listOf(LibrarySortDirection.Descending),
            directionChanges,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filterTogglesExposeCheckedStateAndCanBeCleared() = runComposeUiTest {
        val favoriteChanges = mutableListOf<Boolean>()
        val artworkChanges = mutableListOf<Boolean>()
        var query by mutableStateOf(LibraryBrowseQuery())
        setContent {
            Box(Modifier.size(1_200.dp, 1_200.dp)) {
                LibraryBrowseControls(
                    query = query,
                    sourceOptions =
                        listOf(
                            LibraryBrowseSourceOption(
                                id = "source-a",
                                displayName = "Library A",
                            ),
                        ),
                    onSortChange = {},
                    onSortDirectionChange = {},
                    onFavoriteOnlyChange = { enabled ->
                        favoriteChanges += enabled
                        query = query.copy(favoriteOnly = enabled)
                    },
                    onArtworkOnlyChange = { enabled ->
                        artworkChanges += enabled
                        query = query.copy(artworkOnly = enabled)
                    },
                    onSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        val favoriteOff =
            hasText("Favorites only", substring = false) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off,
                ) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Not selected",
                )
        val artworkOff =
            hasText("Artwork only", substring = false) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off,
                ) and
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Not selected",
                )
        onNode(favoriteOff).performClick()
        onNode(artworkOff).performClick()
        waitForIdle()

        onNode(
                hasText("Favorites only", substring = false) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.On,
                    ),
            )
            .assertIsDisplayed()
        onNode(
                hasText("Artwork only", substring = false) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.On,
                    ),
            )
            .assertIsDisplayed()

        onNode(hasText("Favorites only", substring = false)).performClick()
        onNode(hasText("Artwork only", substring = false)).performClick()
        waitForIdle()
        assertEquals(listOf(true, false), favoriteChanges)
        assertEquals(listOf(true, false), artworkChanges)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sourceChoicesUseDisplayNamesAndClearStaleSelections() =
        runComposeUiTest {
            val sourceChanges = mutableListOf<String?>()
            var query by
                mutableStateOf(LibraryBrowseQuery(sourceId = "source-a"))
            setContent {
                Box(Modifier.size(1_200.dp, 1_200.dp)) {
                    LibraryBrowseControls(
                        query = query,
                        sourceOptions =
                            listOf(
                                LibraryBrowseSourceOption(
                                    id = "source-a",
                                    displayName = "Listening room",
                                ),
                                LibraryBrowseSourceOption(
                                    id = "source-b",
                                    displayName = "Archive",
                                ),
                            ),
                        onSortChange = {},
                        onSortDirectionChange = {},
                        onFavoriteOnlyChange = {},
                        onArtworkOnlyChange = {},
                        onSourceIdChange = { sourceId ->
                            sourceChanges += sourceId
                            query = query.copy(sourceId = sourceId)
                        },
                    )
                }
            }
            waitForIdle()

            onNode(
                    hasText("Listening room", substring = false) and
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Selected,
                            true,
                        ),
                )
                .assertIsDisplayed()
            onAllNodes(hasText("source-a", substring = false))
                .assertCountEquals(0)
            onNode(hasText("Archive", substring = false)).performClick()
            onNode(hasText("All sources", substring = false)).performClick()
            waitForIdle()
            assertEquals(listOf("source-b", null), sourceChanges)

            val staleSourceChanges = mutableListOf<String?>()
            var staleQuery by
                mutableStateOf(LibraryBrowseQuery(sourceId = "removed"))
            setContent {
                Box(Modifier.size(1_200.dp, 260.dp)) {
                    LibraryBrowseControls(
                        query = staleQuery,
                        sourceOptions = emptyList(),
                        onSortChange = {},
                        onSortDirectionChange = {},
                        onFavoriteOnlyChange = {},
                        onArtworkOnlyChange = {},
                        onSourceIdChange = { sourceId ->
                            staleSourceChanges += sourceId
                            staleQuery = staleQuery.copy(sourceId = sourceId)
                        },
                    )
                }
            }
            waitForIdle()
            onNode(hasText("All sources", substring = false)).performClick()
            waitForIdle()
            assertEquals(listOf<String?>(null), staleSourceChanges)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun controlsProvideFortyEightDpMinimumTouchTargets() = runComposeUiTest {
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryBrowseControls(
                    query = LibraryBrowseQuery(),
                    sourceOptions =
                        listOf(
                            LibraryBrowseSourceOption(
                                id = "source-a",
                                displayName = "Long-form listening archive",
                            ),
                        ),
                    onSortChange = {},
                    onSortDirectionChange = {},
                    onFavoriteOnlyChange = {},
                    onArtworkOnlyChange = {},
                    onSourceIdChange = {},
                )
            }
        }
        waitForIdle()

        val favoriteControl =
            onNode(hasContentDescription("Favorites only")).fetchSemanticsNode()
        val minimumHeight =
            48.dp.value * favoriteControl.layoutInfo.density.density
        assertTrue(
            favoriteControl.boundsInRoot.height >= minimumHeight,
            "favorites-only target must be at least 48dp tall",
        )
        onNode(hasText("Long-form listening archive", substring = false))
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun groupedModesHideFlatBrowseControls() = runComposeUiTest {
        listOf(BrowseMode.Albums, BrowseMode.Artists).forEach { browseMode ->
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        title = "Library",
                        subtitle = "",
                        tracks = listOf(testTrack()),
                        browseMode = browseMode,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
                        folderPickerLauncher = AvailablePicker,
                        sourcePickerActionVisible = false,
                        importMessage = null,
                        scanProgress = null,
                        mutationsEnabled = true,
                        currentTrackId = null,
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = testLabels(),
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
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery =
                            LibraryBrowseQuery(
                                favoriteOnly = true,
                                sourceId = "source-a",
                            ),
                        sourceIdByTrackId = mapOf("track" to "source-a"),
                        sourceOptions =
                            listOf(
                                LibraryBrowseSourceOption(
                                    id = "source-a",
                                    displayName = "Listening room",
                                ),
                            ),
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

            onAllNodes(hasText("Sort and filter", substring = false))
                .assertCountEquals(0)
            onAllNodes(hasText("Favorites only", substring = false))
                .assertCountEquals(0)
            onAllNodes(hasText("Artwork only", substring = false))
                .assertCountEquals(0)
            onAllNodes(hasText("All sources", substring = false))
                .assertCountEquals(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filteredEmptyHomeIsPassiveEvenWhenImportWouldOtherwiseBeShown() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    LibraryHomeContent(
                        title = "Library",
                        subtitle = "",
                        tracks = listOf(testTrack()),
                        browseMode = BrowseMode.Songs,
                        playHistory = emptyMap(),
                        createdAtByTrackId = emptyMap(),
                        folderPickerLauncher = AvailablePicker,
                        sourcePickerActionVisible = true,
                        importMessage = null,
                        scanProgress = null,
                        mutationsEnabled = true,
                        currentTrackId = null,
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = testLabels(),
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
                        favoriteTrackIds = emptySet(),
                        artworkTrackIds = emptySet(),
                        onSetTrackFavorite = { _, _ -> },
                        browseQuery = LibraryBrowseQuery(favoriteOnly = true),
                        sourceIdByTrackId = mapOf("track" to "source-a"),
                        sourceOptions =
                            listOf(
                                LibraryBrowseSourceOption(
                                    id = "source-a",
                                    displayName = "Listening room",
                                ),
                            ),
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
                    hasText(
                        "No tracks match the current filters.",
                        substring = false))
                .assertExists()
            onAllNodes(hasText("Add music folder", substring = false))
                .assertCountEquals(0)
            onAllNodes(hasText("Clear library", substring = false))
                .assertCountEquals(0)
        }

    @Test
    fun controlResourcesHaveEnglishAndSimplifiedChineseCopy() {
        val root = repositoryRoot()
        val english = resourceCatalog(root, "values/strings.xml")
        val chinese = resourceCatalog(root, "values-zh/strings.xml")
        val keys =
            listOf(
                "browse_controls_heading",
                "browse_sort_title",
                "browse_sort_artist",
                "browse_sort_album",
                "browse_sort_added",
                "browse_sort_modified",
                "browse_sort_play_count",
                "browse_sort_favorite",
                "browse_sort_ascending",
                "browse_sort_descending",
                "browse_filter_favorite",
                "browse_filter_artwork",
                "browse_filter_all_sources",
                "filtered_empty",
            )
        keys.forEach { key ->
            val en = english[key]
            val zh = chinese[key]
            assertTrue(en != null && en.isNotBlank(), "missing EN $key")
            assertTrue(zh != null && zh.isNotBlank(), "missing ZH $key")
            assertTrue(en != zh, "$key must be localized")
        }
        assertEquals(
            "Sort and filter", english.getValue("browse_controls_heading"))
        assertEquals("排序和筛选", chinese.getValue("browse_controls_heading"))
        assertEquals(
            "No tracks match the current filters.",
            english.getValue("filtered_empty"),
        )
        assertEquals("没有符合当前筛选条件的曲目。", chinese.getValue("filtered_empty"))
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (directory.parentFile != null &&
            !File(directory, "settings.gradle.kts").isFile) {
            directory = directory.parentFile
        }
        return directory
    }

    private fun resourceCatalog(
        root: File,
        localeDirectory: String
    ): Map<String, String> {
        val file =
            File(
                root,
                "feature/library/impl/src/commonMain/composeResources/$localeDirectory",
            )
        val pattern = Regex("""<string\s+name=\"([^\"]+)\">([^<]*)</string>""")
        return pattern.findAll(file.readText()).associate {
            it.groupValues[1] to it.groupValues[2]
        }
    }

    private fun testTrack(): Track =
        Track(
            id = "track",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 100,
            accent = TrackAccent(0xFF111111, 0xFF222222),
            source = AudioSource.FilePath("track.mp3"),
        )

    @Composable
    private fun testLabels(): LibrarySharedLabels =
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

    private object AvailablePicker : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = true
        override val supportsAdditionalSources: Boolean = true

        override fun launch() = Unit
    }
}
