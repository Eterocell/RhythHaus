package com.eterocell.rhythhaus.ui

import com.eterocell.rhythhaus.library.TrackArtwork
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class ArtworkImageTest {
    @Test
    fun trackIdentityStartsLoadingWithoutClaimingArtworkAvailability() {
        assertEquals(
            TrackArtworkLoadState.Loading,
            initialTrackArtworkLoadState(
                trackId = "route-representative", eagerArtworkBytes = null),
        )
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            initialTrackArtworkLoadState(
                trackId = null, eagerArtworkBytes = null),
        )
    }

    @Test
    fun eagerAndLazyArtworkResolveToAvailableBytes() = runBlocking {
        val eagerBytes = byteArrayOf(1, 2)
        val lazyBytes = byteArrayOf(3, 4)

        val eager =
            initialTrackArtworkLoadState(
                trackId = "eager", eagerArtworkBytes = eagerBytes)
        val lazy =
            loadTrackArtworkState("lazy") {
                TrackArtwork(lazyBytes, "image/jpeg")
            }

        assertContentEquals(
            eagerBytes, (eager as TrackArtworkLoadState.Available).bytes)
        assertContentEquals(
            lazyBytes, (lazy as TrackArtworkLoadState.Available).bytes)
    }

    @Test
    fun absentOrFailedLazyArtworkResolvesUnavailable() = runBlocking {
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            loadTrackArtworkState("absent") { null })
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            loadTrackArtworkState("failed") { error("decode failed") },
        )
    }

    @Test
    fun cancellationRethrowsTheExactLoaderException() = runBlocking {
        val cancellation = CancellationException("cancelled")

        val thrown =
            assertFailsWith<CancellationException> {
                loadTrackArtworkState("cancelled") { throw cancellation }
            }

        assertSame(cancellation, thrown)
    }
}
