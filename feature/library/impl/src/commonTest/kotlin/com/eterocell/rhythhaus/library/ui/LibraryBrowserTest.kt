package com.eterocell.rhythhaus.library.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowserTest {
    @Test
    fun browseModesIncludeAlbumsArtistsAndSongsInOrder() {
        assertEquals(
            listOf(BrowseMode.Albums, BrowseMode.Artists, BrowseMode.Songs),
            BrowseMode.entries.toList())
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
}
