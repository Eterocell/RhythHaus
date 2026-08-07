package com.eterocell.rhythhaus.search

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFilterTest {
    @Test
    fun blankAndWhitespaceQueriesHaveNoResults() {
        assertEquals(emptyList(), filterSearchTracks(listOf(track()), ""))
        assertEquals(emptyList(), filterSearchTracks(listOf(track()), "  "))
    }

    @Test
    fun caseInsensitiveTitleArtistAndAlbumFilteringPreservesInputOrder() =
        assertEquals(
            listOf("title", "artist", "album"),
            filterSearchTracks(
                    listOf(
                        track("title", title = "Alpha"),
                        track("artist", artist = "ALPHA"),
                        track("album", album = "alpha"),
                    ),
                    "aLpHa",
                )
                .map(LibraryTrack::id),
        )

    @Test
    fun duplicateIdsAndEmptyMetadataArePreserved() =
        assertEquals(
            listOf("same", "same"),
            filterSearchTracks(
                    listOf(
                        track("same", title = "match", artist = "", album = ""),
                        track("same", title = "MATCH", artist = "", album = ""),
                    ),
                    "match",
                )
                .map(LibraryTrack::id),
        )

    private fun track(
        id: String = "one",
        title: String = "title",
        artist: String = "artist",
        album: String = "album",
    ) =
        LibraryTrack(
            id,
            "source",
            id,
            AudioSource.FilePath(id),
            id,
            title,
            artist,
            album,
            1,
            null,
            null,
            "scan",
            1,
            1,
        )
}
