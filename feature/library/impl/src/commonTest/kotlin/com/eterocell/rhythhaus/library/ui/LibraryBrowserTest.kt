package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowserTest {
    @Test
    fun browseModesIncludeFavoritesAfterTheStandardModes() {
        assertEquals(
            listOf("Albums", "Artists", "Songs", "Favorites"),
            BrowseMode.entries.map { it.name },
        )
    }

    @Test
    fun favoritesPreserveStandardOrderAndExcludeUnfavoritedTracks() {
        val tracks =
            listOf(
                testTrack("first", "First"),
                testTrack("second", "Second"),
                testTrack("third", "Third"),
            )

        assertEquals(
            listOf("first", "third"),
            visibleTracksForBrowseMode(
                    tracks,
                    BrowseMode.Favorites,
                    setOf("third", "first"),
                )
                .map { it.id },
        )
    }

    @Test
    fun nonFavoritesModesKeepTheAuthoritativeTrackSequence() {
        val tracks = listOf(testTrack("one", "One"))
        assertEquals(
            tracks,
            visibleTracksForBrowseMode(tracks, BrowseMode.Songs, emptySet()),
        )
    }

    @Test
    fun albumGridUsesTwoColumnsBelowTabletWidth() {
        assertEquals(2, albumGridColumnsForWidth(0f))
        assertEquals(2, albumGridColumnsForWidth(559f))
    }

    @Test
    fun albumGridUsesThreeColumnsForTabletWidth() {
        assertEquals(3, albumGridColumnsForWidth(560f))
        assertEquals(3, albumGridColumnsForWidth(899f))
    }

    @Test
    fun albumGridUsesFourColumnsForDesktopWidth() {
        assertEquals(4, albumGridColumnsForWidth(900f))
        assertEquals(4, albumGridColumnsForWidth(1400f))
    }

    private fun testTrack(id: String, title: String) =
        Track(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            durationSeconds = 1,
            accent = TrackAccent(0, 0),
            source = AudioSource.FilePath("/$id.mp3"),
        )
}
