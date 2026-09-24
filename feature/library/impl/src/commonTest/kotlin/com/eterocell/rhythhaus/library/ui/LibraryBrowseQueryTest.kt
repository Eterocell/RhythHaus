package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.TrackPlayHistory
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowseQueryTest {
    @Test
    fun defaultQueryUsesTitleAscendingWithoutFilters() {
        assertEquals(
            LibraryBrowseQuery(
                sort = LibrarySort.Title,
                direction = LibrarySortDirection.Ascending,
                favoriteOnly = false,
                artworkOnly = false,
                sourceId = null,
            ),
            LibraryBrowseQuery(),
        )
    }

    @Test
    fun everySortKeySupportsBothDirections() {
        val tracks =
            listOf(
                testTrack("z", "Zulu", "Beta", "Blue"),
                testTrack("x", "Alpha", "Gamma", "Red"),
                testTrack("y", "Bravo", "Alpha", "Green"),
            )
        val sourceIds = mapOf("z" to "source-2", "x" to "source-1", "y" to "source-1")
        val createdAt = mapOf("z" to 30L, "x" to 10L, "y" to 20L)
        val modifiedAt = mapOf("z" to 300L, "x" to 100L, "y" to 200L)
        val history =
            mapOf(
                "z" to TrackPlayHistory("z", 3L, 30L),
                "x" to TrackPlayHistory("x", 1L, 10L),
                "y" to TrackPlayHistory("y", 2L, 20L),
            )
        val favorites = setOf("z", "y")
        val expectedAscending =
            mapOf(
                LibrarySort.Title to listOf("x", "y", "z"),
                LibrarySort.Artist to listOf("y", "z", "x"),
                LibrarySort.Album to listOf("z", "y", "x"),
                LibrarySort.Added to listOf("x", "y", "z"),
                LibrarySort.Modified to listOf("x", "y", "z"),
                LibrarySort.PlayCount to listOf("x", "y", "z"),
                LibrarySort.Favorite to listOf("y", "z", "x"),
            )
        val expectedDescending =
            mapOf(
                LibrarySort.Title to listOf("z", "y", "x"),
                LibrarySort.Artist to listOf("x", "z", "y"),
                LibrarySort.Album to listOf("x", "y", "z"),
                LibrarySort.Added to listOf("z", "y", "x"),
                LibrarySort.Modified to listOf("z", "y", "x"),
                LibrarySort.PlayCount to listOf("z", "y", "x"),
                LibrarySort.Favorite to listOf("x", "z", "y"),
            )

        LibrarySort.entries.forEach { sort ->
            assertEquals(
                expectedAscending.getValue(sort),
                project(
                    tracks = tracks,
                    query = LibraryBrowseQuery(sort = sort),
                    favoriteTrackIds = favorites,
                    sourceIdByTrackId = sourceIds,
                    createdAtByTrackId = createdAt,
                    modifiedAtByTrackId = modifiedAt,
                    playHistory = history,
                ),
            )
            assertEquals(
                expectedDescending.getValue(sort),
                project(
                    tracks = tracks,
                    query =
                        LibraryBrowseQuery(
                            sort = sort,
                            direction = LibrarySortDirection.Descending,
                        ),
                    favoriteTrackIds = favorites,
                    sourceIdByTrackId = sourceIds,
                    createdAtByTrackId = createdAt,
                    modifiedAtByTrackId = modifiedAt,
                    playHistory = history,
                ),
            )
        }
    }

    @Test
    fun equalPrimaryValuesUseCaseInsensitiveTitleArtistAlbumAndIdTies() {
        val tracks =
            listOf(
                testTrack("b", "Same", "Same", "Same"),
                testTrack("a", "same", "same", "same"),
                testTrack("c", "Same", "Alpha", "Same"),
                testTrack("d", "Alpha", "Same", "Same"),
            )

        assertEquals(
            listOf("d", "c", "a", "b"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(sort = LibrarySort.Added),
                createdAtByTrackId = tracks.associate { it.id to 1L },
            ),
        )
        assertEquals(
            listOf("b", "a", "c", "d"),
            project(
                tracks = tracks,
                query =
                    LibraryBrowseQuery(
                        sort = LibrarySort.Added,
                        direction = LibrarySortDirection.Descending,
                    ),
                createdAtByTrackId = tracks.associate { it.id to 1L },
            ),
        )
    }

    @Test
    fun missingOptionalMetadataIsDeterministicAndReversesMissingPlacement() {
        val tracks =
            listOf(
                testTrack("missing", "Missing"),
                testTrack("known", "Known"),
                testTrack("zero", "Zero"),
            )

        assertEquals(
            listOf("zero", "known", "missing"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(sort = LibrarySort.Modified),
                modifiedAtByTrackId = mapOf("known" to 2L, "zero" to 0L),
            ),
        )
        assertEquals(
            listOf("missing", "known", "zero"),
            project(
                tracks = tracks,
                query =
                    LibraryBrowseQuery(
                        sort = LibrarySort.Modified,
                        direction = LibrarySortDirection.Descending,
                    ),
                modifiedAtByTrackId = mapOf("known" to 2L, "zero" to 0L),
            ),
        )
        assertEquals(
            listOf("zero", "known", "missing"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(sort = LibrarySort.PlayCount),
                playHistory =
                    mapOf("known" to TrackPlayHistory("known", 2L, 1L), "zero" to TrackPlayHistory("zero", 0L, 1L)),
            ),
        )
    }

    @Test
    fun filtersApplyBeforeSortingAndUnknownSourcesMatchNothing() {
        val tracks =
            listOf(
                testTrack("favorite-art", "A", artwork = byteArrayOf(1)),
                testTrack("favorite-empty", "B", artwork = byteArrayOf()),
                testTrack("plain-art", "C", artwork = byteArrayOf(2)),
                testTrack("plain", "D"),
            )
        val sourceIds =
            mapOf(
                "favorite-art" to "source-1",
                "favorite-empty" to "source-2",
                "plain-art" to "source-1",
                "plain" to "source-2",
            )
        val favorites = setOf("favorite-art", "favorite-empty")

        assertEquals(
            listOf("favorite-art", "favorite-empty"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(favoriteOnly = true),
                favoriteTrackIds = favorites,
                sourceIdByTrackId = sourceIds,
            ),
        )
        assertEquals(
            listOf("favorite-art", "plain-art"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(artworkOnly = true),
                favoriteTrackIds = favorites,
                sourceIdByTrackId = sourceIds,
            ),
        )
        assertEquals(
            listOf("favorite-art", "plain-art"),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(sourceId = "source-1"),
                favoriteTrackIds = favorites,
                sourceIdByTrackId = sourceIds,
            ),
        )
        assertEquals(
            emptyList(),
            project(
                tracks = tracks,
                query = LibraryBrowseQuery(sourceId = "missing-source"),
                sourceIdByTrackId = sourceIds,
            ),
        )
    }

    @Test
    fun recentAndFavoriteModesKeepMembershipBeforeExplicitSorting() {
        val tracks =
            listOf(
                testTrack("one", "One", "Zed"),
                testTrack("two", "Two", "Ada"),
                testTrack("three", "Three", "Moe"),
            )
        val history = mapOf("two" to TrackPlayHistory("two", 1L, 2L), "three" to TrackPlayHistory("three", 4L, 1L))

        assertEquals(
            listOf("two"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.Favorites,
                query = LibraryBrowseQuery(sort = LibrarySort.Title),
                favoriteTrackIds = setOf("two"),
                playHistory = history,
            ),
        )
        assertEquals(
            listOf("two", "three"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.RecentlyPlayed,
                query = LibraryBrowseQuery(),
                playHistory = history,
            ),
        )
        assertEquals(
            listOf("two", "three"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.RecentlyPlayed,
                query = LibraryBrowseQuery(sort = LibrarySort.PlayCount),
                favoriteTrackIds = setOf("one", "two"),
                playHistory = history,
            ),
        )
        assertEquals(
            listOf("two", "three"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.RecentlyPlayed,
                query = LibraryBrowseQuery(sort = LibrarySort.PlayCount),
                playHistory = history,
            ),
        )
        assertEquals(
            listOf("one", "two", "three"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.RecentlyAdded,
                query = LibraryBrowseQuery(sort = LibrarySort.Added),
                createdAtByTrackId = mapOf("one" to 1L, "two" to 2L, "three" to 3L),
                playHistory = history,
            ),
        )
        assertEquals(
            listOf("three", "two", "one"),
            project(
                tracks = tracks,
                browseMode = BrowseMode.RecentlyAdded,
                query = LibraryBrowseQuery(),
                createdAtByTrackId = mapOf("one" to 1L, "two" to 2L, "three" to 3L),
                playHistory = history,
            ),
        )
    }

    @Test
    fun groupedModesPassThroughWithoutApplyingQuery() {
        val tracks =
            listOf(
                testTrack("first", "First", artwork = null),
                testTrack("second", "Second", artwork = byteArrayOf(1)),
            )
        val query =
            LibraryBrowseQuery(
                sort = LibrarySort.Favorite,
                direction = LibrarySortDirection.Descending,
                favoriteOnly = true,
                artworkOnly = true,
                sourceId = "unknown",
            )

        assertEquals(
            tracks.map { it.id },
            project(
                tracks = tracks,
                browseMode = BrowseMode.Albums,
                query = query,
                favoriteTrackIds = setOf("second"),
            ),
        )
        assertEquals(
            tracks.map { it.id },
            project(
                tracks = tracks,
                browseMode = BrowseMode.Artists,
                query = query,
                favoriteTrackIds = setOf("second"),
            ),
        )
    }

    private fun project(
        tracks: List<Track>,
        browseMode: BrowseMode = BrowseMode.Songs,
        query: LibraryBrowseQuery = LibraryBrowseQuery(),
        favoriteTrackIds: Set<String> = emptySet(),
        sourceIdByTrackId: Map<String, String> = emptyMap(),
        createdAtByTrackId: Map<String, Long> = emptyMap(),
        modifiedAtByTrackId: Map<String, Long> = emptyMap(),
        playHistory: Map<String, TrackPlayHistory> = emptyMap(),
    ): List<String> =
        visibleTracksForBrowseQuery(
            tracks = tracks,
            browseMode = browseMode,
            query = query,
            favoriteTrackIds = favoriteTrackIds,
            sourceIdByTrackId = sourceIdByTrackId,
            createdAtByTrackId = createdAtByTrackId,
            modifiedAtByTrackId = modifiedAtByTrackId,
            playHistory = playHistory,
        ).map { it.id }

    private fun testTrack(
        id: String,
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        artwork: ByteArray? = null,
    ): Track =
        Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = 1,
            accent = TrackAccent(0, 0),
            source = AudioSource.FilePath("/$id.mp3"),
            artworkBytes = artwork,
        )
}
