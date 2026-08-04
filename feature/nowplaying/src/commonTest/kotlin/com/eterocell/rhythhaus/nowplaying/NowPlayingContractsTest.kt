package com.eterocell.rhythhaus.nowplaying

import kotlin.test.Test
import kotlin.test.assertEquals

public class NowPlayingContractsTest {
    @Test
    public fun screenLabelsPreserveAllInjectedValues(): Unit {
        val labels =
            NowPlayingScreenLabels("play", "pause", "artwork", "artist - album")
        assertEquals("play", labels.play)
        assertEquals("pause", labels.pause)
        assertEquals("artwork", labels.albumArtwork)
        assertEquals("artist - album", labels.currentTrackArtistAlbum)
    }

    @Test
    public fun barLabelsPreserveAllInjectedValues(): Unit {
        val labels =
            NowPlayingBarLabels(
                "play", "pause", "search", "settings", "art", "artist - album")
        assertEquals("play", labels.play)
        assertEquals("pause", labels.pause)
        assertEquals("search", labels.search)
        assertEquals("settings", labels.settings)
        assertEquals("art", labels.albumArt)
        assertEquals("artist - album", labels.currentTrackArtistAlbum)
    }

    @Test
    public fun emptyBarUsesNavigationModeRegardlessOfInertTrackLabels(): Unit {
        val labels =
            NowPlayingBarLabels(
                "play", "pause", "search", "settings", "art", "inert")
        assertEquals(
            BottomBarMode.EmptyLibraryNavigation, bottomBarModeFor(null))
        assertEquals("inert", labels.currentTrackArtistAlbum)
    }

    @Test
    public fun adaptiveLayoutContractPreservesCompactAndSplitBounds(): Unit {
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Compact,
            nowPlayingAdaptiveLayoutModeFor(390f, 844f))
        assertEquals(
            NowPlayingAdaptiveLayoutMode.Split,
            nowPlayingAdaptiveLayoutModeFor(840f, 1180f))
    }
}
