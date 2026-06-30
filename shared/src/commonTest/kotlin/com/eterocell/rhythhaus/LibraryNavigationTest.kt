package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryNavigationTest {
    @Test
    fun rootStackStartsAtHomeAndCannotPopPastHome() {
        val stack = LibraryNavigationStack()

        assertEquals(LibraryRoute.Home, stack.current)
        assertFalse(stack.canPop)
        assertEquals(stack, stack.pop())
    }

    @Test
    fun duplicateTopPushIsNoOp() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Search)

        assertEquals(listOf(LibraryRoute.Home, LibraryRoute.Search), stack.routes)
    }

    @Test
    fun searchOpenedFromAlbumReturnsToAlbum() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val stack = LibraryNavigationStack()
            .push(album)
            .push(LibraryRoute.Search)

        assertEquals(LibraryRoute.Search, stack.current)
        assertTrue(stack.canPop)
        assertEquals(album, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun nowPlayingOpenedFromArtistReturnsToArtist() {
        val artist = LibraryRoute.ArtistDetail("John Coltrane")
        val stack = LibraryNavigationStack()
            .push(artist)
            .push(LibraryRoute.NowPlaying)

        assertEquals(LibraryRoute.NowPlaying, stack.current)
        assertEquals(artist, stack.pop().current)
    }

    @Test
    fun clearDialogPopsBackToSettingsOrigin() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Settings)
            .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertEquals(LibraryRoute.Settings, stack.pop().current)
    }

    @Test
    fun pushingHomeReturnsToRoot() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("A"))
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Home)

        assertEquals(listOf(LibraryRoute.Home), stack.routes)
        assertEquals(LibraryRoute.Home, stack.current)
    }
}
