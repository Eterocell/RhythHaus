package com.eterocell.rhythhaus.ui

import com.eterocell.rhythhaus.library.TrackArtwork
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArtworkImageTest {
    @Test
    fun artworkMemoryCacheKeyIncludesRoleAndSize() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val thumbnailKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Thumbnail)
        val heroKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Hero)

        assertNotEquals(thumbnailKey, heroKey)
        assertTrue(thumbnailKey.contains("thumbnail"))
        assertTrue(heroKey.contains("hero"))
        assertTrue(thumbnailKey.endsWith(":4"))
    }

    @Test
    fun trackIdentityStartsLoadingWithoutClaimingArtworkAvailability() {
        assertEquals(
            TrackArtworkLoadState.Loading,
            initialTrackArtworkLoadState(trackId = "route-representative", eagerArtworkBytes = null),
        )
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            initialTrackArtworkLoadState(trackId = null, eagerArtworkBytes = null),
        )
    }

    @Test
    fun eagerAndLazyArtworkResolveToAvailableBytes() = runBlocking {
        val eagerBytes = byteArrayOf(1, 2)
        val lazyBytes = byteArrayOf(3, 4)

        val eager = initialTrackArtworkLoadState(trackId = "eager", eagerArtworkBytes = eagerBytes)
        val lazy = loadTrackArtworkState("lazy") { TrackArtwork(lazyBytes, "image/jpeg") }

        assertContentEquals(eagerBytes, (eager as TrackArtworkLoadState.Available).bytes)
        assertContentEquals(lazyBytes, (lazy as TrackArtworkLoadState.Available).bytes)
    }

    @Test
    fun absentOrFailedLazyArtworkResolvesUnavailable() = runBlocking {
        assertEquals(TrackArtworkLoadState.Unavailable, loadTrackArtworkState("absent") { null })
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            loadTrackArtworkState("failed") { error("decode failed") },
        )
    }
}
