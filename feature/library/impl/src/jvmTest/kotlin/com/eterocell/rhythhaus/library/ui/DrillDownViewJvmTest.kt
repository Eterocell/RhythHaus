package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrillDownViewJvmTest {
    init {
        Locale.setDefault(Locale.ENGLISH)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersSummaryProjectionAndCurrentTrackHighlight() = runComposeUiTest {
        val scrolls = mutableListOf<Pair<Int, Int>>()
        val visibleReports = mutableListOf<List<String>>()
        setContent {
            Box(Modifier.size(420.dp, 520.dp)) {
                DrillDownView(
                    title = "Album B",
                    summary =
                        LibraryDetailSummary.Album(
                            trackCount = 3, artist = "Artist"),
                    tracks = tracks(),
                    topBarArtworkTrack = null,
                    currentTrackId = "t-2",
                    selectionPage = LibrarySelectionPage.Album("Album B"),
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    artworkLoader = { null },
                    onBack = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = { visibleReports += it },
                    onScrollPositionChanged = { index, offset ->
                        scrolls += index to offset
                    },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("3 tracks · Artist")).assertCountEquals(1)
        onAllNodes(hasText("Now playing")).assertCountEquals(1)
        assertEquals(tracks().map(Track::id), visibleReports.last())
        assertEquals(listOf(0, 0), scrolls.first().toList())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unknownAlbumArtistResolvesToLocalizedUnknownArtist() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(420.dp, 520.dp)) {
                    DrillDownView(
                        title = "Album B",
                        summary =
                            LibraryDetailSummary.Album(
                                trackCount = 2, artist = null),
                        tracks = tracks().take(2),
                        topBarArtworkTrack = null,
                        currentTrackId = null,
                        selectionPage = LibrarySelectionPage.Album("Album B"),
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        artworkLoader = { null },
                        onBack = {},
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

            onAllNodes(hasText("2 tracks · Unknown artist"))
                .assertCountEquals(1)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowDispatchesOrderedPlaybackAndLongPressStartsSelection() =
        runComposeUiTest {
            val plays = mutableListOf<Pair<List<Track>, Track>>()
            val selectionStarts = mutableListOf<String>()
            setContent {
                Box(Modifier.size(420.dp, 520.dp)) {
                    DrillDownView(
                        title = "Album B",
                        summary =
                            LibraryDetailSummary.Album(
                                trackCount = 3, artist = "Artist"),
                        tracks = tracks(),
                        topBarArtworkTrack = null,
                        currentTrackId = null,
                        selectionPage = LibrarySelectionPage.Album("Album B"),
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        artworkLoader = { null },
                        onBack = {},
                        onPlayTrack = { ordered, selected ->
                            plays += ordered to selected
                        },
                        onToggleSelection = {},
                        onStartSelection = { selectionStarts += it },
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Select Two"))[0].performClick()
            waitForIdle()
            assertEquals(tracks(), plays.last().first)
            assertEquals("t-1", plays.last().second.id)

            onAllNodes(hasContentDescription("Select Two"))[0]
                .performTouchInput { longClick() }
            waitForIdle()
            assertEquals(listOf("t-1"), selectionStarts)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionModeRoutesClicksToToggleSelectionCallbacks() =
        runComposeUiTest {
            var selectionModeActive by mutableStateOf(false)
            val toggles = mutableListOf<String>()
            setContent {
                Box(Modifier.size(420.dp, 520.dp)) {
                    DrillDownView(
                        title = "Album B",
                        summary =
                            LibraryDetailSummary.Album(
                                trackCount = 3, artist = "Artist"),
                        tracks = tracks(),
                        topBarArtworkTrack = null,
                        currentTrackId = null,
                        selectionPage = LibrarySelectionPage.Album("Album B"),
                        selectionModeActive = selectionModeActive,
                        selectedTrackIds = setOf("t-2"),
                        labels = labels(),
                        artworkLoader = { null },
                        onBack = {},
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = { toggles += it },
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = {},
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()
            selectionModeActive = true
            waitForIdle()

            onAllNodes(hasContentDescription("Select Two"))[0].performClick()
            waitForIdle()
            assertEquals(listOf("t-1"), toggles)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backButtonAndScrollReportsReachShared() = runComposeUiTest {
        var backCount = 0
        val scrolls = mutableListOf<Pair<Int, Int>>()
        setContent {
            Box(Modifier.size(420.dp, 280.dp)) {
                DrillDownView(
                    title = "Album B",
                    summary =
                        LibraryDetailSummary.Album(
                            trackCount = 12, artist = "Artist"),
                    tracks = twelveTracks(),
                    topBarArtworkTrack = null,
                    currentTrackId = null,
                    selectionPage = LibrarySelectionPage.Album("Album B"),
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    artworkLoader = { null },
                    onBack = { backCount += 1 },
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

        onAllNodes(hasContentDescription("Back"))[0].performSemanticsAction(
            SemanticsActions.OnClick)
        waitForIdle()
        assertEquals(1, backCount)

        onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        waitForIdle()
        assertTrue(scrolls.any { it.first >= 8 })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun albumReconcilePayloadKeepsEveryPageTrackAfterScrolling() =
        runComposeUiTest {
            val visibleReports = mutableListOf<List<String>>()
            setContent {
                Box(Modifier.size(420.dp, 520.dp)) {
                    DrillDownView(
                        title = "Album B",
                        summary =
                            LibraryDetailSummary.Album(
                                trackCount = 40, artist = "Artist"),
                        tracks = fortyTracks(),
                        topBarArtworkTrack = null,
                        currentTrackId = null,
                        selectionPage = LibrarySelectionPage.Album("Album B"),
                        selectionModeActive = false,
                        selectedTrackIds = emptySet(),
                        labels = labels(),
                        artworkLoader = { null },
                        onBack = {},
                        onPlayTrack = { _, _ -> },
                        onToggleSelection = {},
                        onStartSelection = {},
                        onVisibleTrackIdsChanged = { visibleReports += it },
                        onScrollPositionChanged = { _, _ -> },
                        bottomContentPadding = 0.dp,
                    )
                }
            }
            waitForIdle()

            onNode(hasScrollToIndexAction()).performScrollToIndex(35)
            waitForIdle()
            assertEquals(fortyTracks().map(Track::id), visibleReports.last())
        }

    private fun fortyTracks(): List<Track> =
        List(40) { index ->
            track(
                "t-${index + 1}",
                "Track ${index + 1}",
                "Album B",
                "Artist",
                number = index + 1)
        }

    private fun twelveTracks(): List<Track> =
        List(12) { index ->
            track(
                "t-${index + 1}",
                "Track ${index + 1}",
                "Album B",
                "Artist",
                number = index + 1)
        }

    private fun tracks(): List<Track> =
        listOf(
            track("t-1", "Two", "Album B", "Artist", number = 2),
            track("t-2", "One", "Album B", "Artist", number = 1),
            track("t-3", "Solo", "Album B", "Soloist", number = 3),
        )

    private fun track(
        id: String,
        title: String,
        album: String,
        artist: String,
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
            discNumber = 1,
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
}
