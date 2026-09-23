package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.TrackPlayHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowserTest {
    @Test
    fun browseModesIncludeFavoritesAfterTheStandardModes() {
        assertEquals(
            listOf(
                "Albums",
                "Artists",
                "Songs",
                "Favorites",
                "RecentlyPlayed",
                "RecentlyAdded",
            ),
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
    fun recentlyPlayedUsesLastPlayedDescendingThenTitleArtist() {
        val tracks =
            listOf(
                testTrack("same-z", "Same", "Zed"),
                testTrack("old", "Old", "Artist"),
                testTrack("same-a", "Same", "Ada"),
                testTrack("new", "New", "Artist"),
            )
        val history =
            mapOf(
                "same-z" to TrackPlayHistory("same-z", 1L, 200L),
                "old" to TrackPlayHistory("old", 4L, 100L),
                "same-a" to TrackPlayHistory("same-a", 2L, 200L),
                "new" to TrackPlayHistory("new", 1L, 300L),
            )

        assertEquals(
            listOf("new", "same-a", "same-z", "old"),
            visibleTracksForBrowseMode(
                tracks,
                BrowseMode.RecentlyPlayed,
                emptySet(),
                history,
                mapOf("new" to 1L, "same-a" to 2L, "same-z" to 3L, "old" to 4L),
            ).map { it.id },
        )
    }

    @Test
    fun recentlyAddedUsesCreatedAtDescendingThenTitleArtist() {
        val tracks =
            listOf(
                testTrack("same-z", "Same", "Zed"),
                testTrack("old", "Old", "Artist"),
                testTrack("same-a", "Same", "Ada"),
                testTrack("new", "New", "Artist"),
            )

        assertEquals(
            listOf("same-a", "same-z", "new", "old"),
            visibleTracksForBrowseMode(
                tracks,
                BrowseMode.RecentlyAdded,
                emptySet(),
                emptyMap(),
                mapOf("same-z" to 200L, "old" to 100L, "same-a" to 200L, "new" to 150L),
            ).map { it.id },
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

    private fun testTrack(id: String, title: String, artist: String = "Artist") =
        Track(
            id = id,
            title = title,
            artist = artist,
            album = "Album",
            durationSeconds = 1,
            accent = TrackAccent(0, 0),
            source = AudioSource.FilePath("/$id.mp3"),
        )
}
