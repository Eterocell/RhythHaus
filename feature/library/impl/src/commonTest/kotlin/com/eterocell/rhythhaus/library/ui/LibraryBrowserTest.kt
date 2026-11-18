package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.TrackPlayHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowserTest {
    @Test
    fun detailTrackProjectionRetainsScannedReplayGainFacts() {
        val projected =
            LibraryTrack(
                    id = "track",
                    sourceId = "source",
                    sourceLocalKey = "song.flac",
                    audioSource = AudioSource.FilePath("/Music/song.flac"),
                    displayName = "song.flac",
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    durationMillis = 123_000L,
                    sizeBytes = null,
                    modifiedAtEpochMillis = null,
                    lastSeenScanId = "scan",
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1L,
                    replayGainTrackGainDb = -6.0,
                    replayGainTrackPeak = 1.2,
                    replayGainAlbumGainDb = -4.0,
                    replayGainAlbumPeak = 1.1,
                )
                .toUiTrack()
        assertEquals(
            listOf(-6.0, 1.2, -4.0, 1.1),
            listOf(
                projected.replayGainTrackGainDb,
                projected.replayGainTrackPeak,
                projected.replayGainAlbumGainDb,
                projected.replayGainAlbumPeak,
            ))
    }

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
    fun sameTitleReleasesGroupSeparatelyByAlbumArtist() {
        val groups =
            groupByAlbum(
                listOf(
                    libraryTrack(
                        id = "one",
                        title = "First",
                        artist = "Track Artist A",
                        album = "Shared Title",
                        albumArtist = "Zebra Records",
                    ),
                    libraryTrack(
                        id = "two",
                        title = "Second",
                        artist = "Track Artist B",
                        album = "Shared Title",
                        albumArtist = "Alpha Records",
                    ),
                ))

        assertEquals(2, groups.size)
        assertEquals(
            listOf("Alpha Records", "Zebra Records"),
            groups.map { it.albumArtist },
        )
        assertEquals(
            listOf("Track Artist B", "Track Artist A"),
            groups.map { it.tracks.single().artist },
        )
        assertEquals(
            listOf(listOf("Second"), listOf("First")),
            groups.map { group -> group.tracks.map { it.title } },
        )
    }

    @Test
    fun taggedCompilationGroupsTogetherWhileUntaggedTracksSplitByArtist() {
        val tagged =
            groupByAlbum(
                listOf(
                    libraryTrack(
                        id = "one",
                        title = "First",
                        artist = "Singer A",
                        album = "Hits",
                        albumArtist = "Various Artists",
                    ),
                    libraryTrack(
                        id = "two",
                        title = "Second",
                        artist = "Singer B",
                        album = "Hits",
                        albumArtist = "Various Artists",
                    ),
                ))
        val untagged =
            groupByAlbum(
                listOf(
                    libraryTrack(
                        id = "three",
                        title = "Third",
                        artist = "Singer C",
                        album = "Loose",
                    ),
                    libraryTrack(
                        id = "four",
                        title = "Fourth",
                        artist = "Singer D",
                        album = "Loose",
                    ),
                ))

        assertEquals(1, tagged.size)
        assertEquals("Various Artists", tagged.single().albumArtist)
        assertEquals(
            listOf("First", "Second"),
            tagged.single().tracks.map { it.title },
        )
        assertEquals(2, untagged.size)
        assertEquals(
            listOf("Singer C", "Singer D"),
            untagged.map { it.albumArtist },
        )
    }

    @Test
    fun albumGroupsOrderByTitleThenAlbumArtistAndKeepDiscTrackOrder() {
        val groups =
            groupByAlbum(
                listOf(
                    libraryTrack(
                        id = "b2",
                        title = "Beta Two",
                        artist = "Track Artist",
                        album = "Beta",
                        albumArtist = "Second",
                        discNumber = 2,
                        trackNumber = 1,
                    ),
                    libraryTrack(
                        id = "b1",
                        title = "Beta One",
                        artist = "Track Artist",
                        album = "Beta",
                        albumArtist = "Second",
                        discNumber = 1,
                        trackNumber = 2,
                    ),
                    libraryTrack(
                        id = "a2",
                        title = "Alpha Two",
                        artist = "Track Artist",
                        album = "Alpha",
                        albumArtist = "Zulu",
                    ),
                    libraryTrack(
                        id = "a1",
                        title = "Alpha One",
                        artist = "Track Artist",
                        album = "Alpha",
                        albumArtist = "Alpha",
                    ),
                ))

        assertEquals(
            listOf("Alpha" to "Alpha", "Alpha" to "Zulu", "Beta" to "Second"),
            groups.map { it.album to it.albumArtist },
        )
        assertEquals(
            listOf("Beta One", "Beta Two"),
            groups.last().tracks.map { it.title },
        )
    }

    @Test
    fun albumGridColumnsRespectCompactAndWideCardBoundaries() {
        assertEquals(16f, albumGridGapDp(compact = true))
        assertEquals(20f, albumGridGapDp(compact = false))
        assertEquals(2, albumGridColumnsForWidth(415f, compact = true))
        assertEquals(3, albumGridColumnsForWidth(416f, compact = true))
        assertEquals(4, albumGridColumnsForWidth(679f, compact = true))
        assertEquals(3, albumGridColumnsForWidth(679f, compact = false))
        assertEquals(3, albumGridColumnsForWidth(699f, compact = false))
        assertEquals(4, albumGridColumnsForWidth(700f, compact = false))
        assertEquals(4, albumGridColumnsForWidth(879f, compact = false))
        assertEquals(5, albumGridColumnsForWidth(880f, compact = false))
        assertEquals(4, albumGridColumnsForWidth(720f))
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
                    mapOf(
                        "new" to 1L,
                        "same-a" to 2L,
                        "same-z" to 3L,
                        "old" to 4L),
                )
                .map { it.id },
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
                    mapOf(
                        "same-z" to 200L,
                        "old" to 100L,
                        "same-a" to 200L,
                        "new" to 150L),
                )
                .map { it.id },
        )
    }

    private fun testTrack(
        id: String,
        title: String,
        artist: String = "Artist"
    ) =
        Track(
            id = id,
            title = title,
            artist = artist,
            album = "Album",
            durationSeconds = 1,
            accent = TrackAccent(0, 0),
            source = AudioSource.FilePath("/$id.mp3"),
        )

    private fun libraryTrack(
        id: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        discNumber: Int? = null,
        trackNumber: Int? = null,
    ): LibraryTrack =
        LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = "$id.mp3",
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = "$id.mp3",
            title = title,
            artist = artist,
            album = album,
            durationMillis = 1_000L,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
            trackNumber = trackNumber,
            discNumber = discNumber,
            albumArtist = albumArtist,
        )
}
