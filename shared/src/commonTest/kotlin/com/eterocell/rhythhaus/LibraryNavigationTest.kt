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
    fun clearDialogRouteRequiresInWindowContentAnimation() {
        assertTrue(routeRequiresInWindowContentAnimation(LibraryRoute.ClearLibraryDialog))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Settings))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Search))
        assertFalse(routeRequiresInWindowContentAnimation(LibraryRoute.Home))
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

    @Test
    fun pushingNestedRouteClassifiesAsPush() {
        val from = LibraryNavigationStack()
        val to = from.push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(LibraryNavigationTransition.Push, classifyNavigationTransition(from, to))
    }

    @Test
    fun poppingNestedRouteClassifiesAsPop() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.pop()

        assertEquals(LibraryNavigationTransition.Pop, classifyNavigationTransition(from, to))
    }

    @Test
    fun pushingHomeClassifiesAsRoot() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Home)

        assertEquals(LibraryNavigationTransition.Root, classifyNavigationTransition(from, to))
    }

    @Test
    fun replacingTopRouteClassifiesAsReplace() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.replaceTop(LibraryRoute.Settings)

        assertEquals(LibraryNavigationTransition.Replace, classifyNavigationTransition(from, to))
    }

    @Test
    fun duplicateTopPushClassifiesAsNone() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Search)

        assertEquals(LibraryNavigationTransition.None, classifyNavigationTransition(from, to))
    }
}
