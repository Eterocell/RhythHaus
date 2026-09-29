package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartPlaylistProjectionTest {
    private val tracks =
        listOf(
            track("z", "alpha", "Other", "Record", 30),
            track("b", "Beta", "Artist", "Record", 20),
            track("a", "ALPHA", "Artist", "Record", 20),
            track("c", "Gamma", "Artist", "Another", 10),
        )

    @Test
    fun favoriteMembershipChangesWithCurrentAuthorityAndOrdersTitleThenId() {
        assertEquals(
            listOf("a", "z", "b"),
            rows(
                SmartPlaylistRule.Favorites,
                favorites = setOf("z", "a", "b", "missing")))
        assertEquals(
            listOf("b"),
            rows(SmartPlaylistRule.Favorites, favorites = setOf("b")))
    }

    @Test
    fun artistAndAlbumAreExactAndAlbumIsScopedToArtist() {
        assertEquals(
            listOf("a", "b", "c"), rows(SmartPlaylistRule.Artist("Artist")))
        assertEquals(
            listOf("a", "b"), rows(SmartPlaylistRule.Album("Artist", "Record")))
        assertEquals(emptyList(), rows(SmartPlaylistRule.Artist("artist")))
    }

    @Test
    fun recentlyPlayedExcludesUnrecordedAndRemovedTracksAndTiesById() {
        val history =
            mapOf(
                "a" to TrackPlayHistory("a", 1, 20),
                "b" to TrackPlayHistory("b", 9, 20),
                "c" to TrackPlayHistory("c", 1, 30),
                "missing" to TrackPlayHistory("missing", 1, 50))
        assertEquals(
            listOf("c", "a", "b"),
            rows(SmartPlaylistRule.RecentlyPlayed(10), history = history))
        assertEquals(
            listOf("a", "c", "b"),
            rows(
                SmartPlaylistRule.RecentlyPlayed(10),
                history = history + ("a" to TrackPlayHistory("a", 2, 40))))
    }

    @Test
    fun newestCountsApplyAfterOrderingAndUpdateWithScan() {
        val many =
            (1..60).map {
                track(
                    it.toString().padStart(2, '0'),
                    "Title",
                    "Artist",
                    "Album",
                    it.toLong())
            }
        for (count in listOf(10, 25, 50)) {
            val expected =
                (60 downTo 61 - count).map { it.toString().padStart(2, '0') }
            assertEquals(
                expected,
                projectSmartPlaylist(
                        summary(SmartPlaylistRule.RecentlyAdded(count)),
                        many,
                        emptySet(),
                        emptyMap(),
                        PlaylistSnapshot())
                    .rows
                    .map { it.trackId })
        }
        assertEquals(
            listOf("z", "a", "b", "c"),
            rows(SmartPlaylistRule.RecentlyAdded(10)))
    }

    @Test
    fun savedSourceKeepsDuplicateOccurrencesAndStableIdsAcrossReorder() {
        val source = PlaylistSummary("saved", "Queue", 1, 1)
        val entries =
            listOf(
                entry("one", "a", 0),
                entry("two", "b", 1),
                entry("three", "a", 2),
                entry("gone", "missing", 3))
        val definition = summary(SmartPlaylistRule.SavedPlaylist(source.id))
        val first =
            projectSmartPlaylist(
                definition,
                tracks,
                emptySet(),
                emptyMap(),
                PlaylistSnapshot(listOf(source), mapOf(source.id to entries)))
        assertFalse(first.sourceMissing)
        assertEquals(listOf("a", "b", "a"), first.rows.map { it.trackId })
        assertEquals(3, first.rows.map { it.occurrenceId }.toSet().size)
        val next =
            projectSmartPlaylist(
                definition,
                tracks,
                emptySet(),
                emptyMap(),
                PlaylistSnapshot(
                    listOf(source), mapOf(source.id to entries.reversed())))
        assertEquals(first.rows.reversed(), next.rows)
        val removed =
            projectSmartPlaylist(
                definition,
                tracks.filterNot { it.id == "a" },
                emptySet(),
                emptyMap(),
                PlaylistSnapshot(listOf(source), mapOf(source.id to entries)))
        assertEquals(listOf("b"), removed.rows.map { it.trackId })
    }

    @Test
    fun missingSourceIsExplicitAndDoesNotInventEntries() {
        val projection =
            projectSmartPlaylist(
                summary(SmartPlaylistRule.SavedPlaylist("saved")),
                tracks,
                emptySet(),
                emptyMap(),
                PlaylistSnapshot())
        assertTrue(projection.sourceMissing)
        assertEquals(emptyList(), projection.rows)
    }

    private fun rows(
        rule: SmartPlaylistRule,
        favorites: Set<String> = emptySet(),
        history: Map<String, TrackPlayHistory> = emptyMap()
    ) =
        projectSmartPlaylist(
                summary(rule), tracks, favorites, history, PlaylistSnapshot())
            .rows
            .map { it.trackId }

    private fun summary(rule: SmartPlaylistRule) =
        SmartPlaylistSummary("smart", "Smart", rule, 1, 1)

    private fun entry(id: String, trackId: String, position: Int) =
        PlaylistEntry(id, "saved", trackId, position, 1)

    private fun track(
        id: String,
        title: String,
        artist: String,
        album: String,
        created: Long
    ) =
        LibraryTrack(
            id,
            "source",
            id,
            AudioSource.FilePath("/$id.wav"),
            "$id.wav",
            title,
            artist,
            album,
            1000,
            100,
            null,
            null,
            created,
            created)
}
