package com.eterocell.rhythhaus.nowplaying

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class NowPlayingFavoriteAvailabilityTest {
    @Test
    public fun requiresExactAuthoritativePlaybackIdentity(): Unit {
        assertTrue(nowPlayingFavoriteAvailable("track", "track"))
        assertFalse(nowPlayingFavoriteAvailable("library-track", "other-track"))
        assertFalse(nowPlayingFavoriteAvailable("library-track", null))
        assertFalse(nowPlayingFavoriteAvailable(null, "track"))
    }
}
