package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.ui.TrackArtworkLoadState
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtworkCollapseTest {
    private val geometry = ArtworkCollapseGeometry(
        expandedHeightPx = 320f,
        collapsedHeightPx = 80f,
    )

    @Test
    fun snapshotKeepsHeaderAndContentOnOneClampedGeometry() {
        assertEquals(ArtworkCollapseSnapshot(0f, 320f, 0f), geometry.snapshot(0f))
        assertEquals(ArtworkCollapseSnapshot(120f, 200f, 0.5f), geometry.snapshot(120f))
        assertEquals(ArtworkCollapseSnapshot(240f, 80f, 1f), geometry.snapshot(500f))
    }

    @Test
    fun upwardMovementIsConsumedOneForOneUntilCollapsed() {
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 50f, consumedY = -50f),
            geometry.consumeUpward(offsetPx = 0f, availableY = -50f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 240f, consumedY = -20f),
            geometry.consumeUpward(offsetPx = 220f, availableY = -80f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 240f, consumedY = 0f),
            geometry.consumeUpward(offsetPx = 240f, availableY = -20f),
        )
    }

    @Test
    fun downwardMovementExpandsSymmetrically() {
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 170f, consumedY = 50f),
            geometry.consumeDownward(offsetPx = 220f, availableY = 50f),
        )
        assertEquals(
            ArtworkCollapseConsumption(offsetPx = 0f, consumedY = 20f),
            geometry.consumeDownward(offsetPx = 20f, availableY = 80f),
        )
    }

    @Test
    fun invalidRangesRenderCollapsedAndConsumeNothing() {
        listOf(
            ArtworkCollapseGeometry(80f, 80f),
            ArtworkCollapseGeometry(60f, 80f),
        ).forEach { invalid ->
            assertEquals(ArtworkCollapseSnapshot(0f, 80f, 1f), invalid.snapshot(40f))
            assertEquals(ArtworkCollapseConsumption(0f, 0f), invalid.consumeUpward(0f, -30f))
            assertEquals(ArtworkCollapseConsumption(0f, 0f), invalid.consumeDownward(0f, 30f))
        }
    }

    @Test
    fun resizedGeometryClampsExistingOffsetImmediately() {
        val narrowed = ArtworkCollapseGeometry(expandedHeightPx = 200f, collapsedHeightPx = 80f)

        assertEquals(ArtworkCollapseSnapshot(120f, 80f, 1f), narrowed.snapshot(offsetPx = 220f))
    }

    @Test
    fun artworkPagesUseAppOwnedScrollWhileFallbackPagesUseMiuix() {
        assertEquals(
            DrillDownScrollOwner.Artwork,
            drillDownScrollOwner(
                DrillDownArtwork(
                    representativeTrackId = "track",
                    state = TrackArtworkLoadState.Available(byteArrayOf(1)),
                ),
            ),
        )
        assertEquals(
            DrillDownScrollOwner.Miuix,
            drillDownScrollOwner(
                DrillDownArtwork(
                    representativeTrackId = null,
                    state = TrackArtworkLoadState.Unavailable,
                ),
            ),
        )
    }

    @Test
    fun albumAndArtistRepresentativeIdentityDoesNotOwnScrollWithoutResolvedArtwork() {
        listOf(
            LibraryRoute.AlbumDetail("Album") to "album-first-track",
            LibraryRoute.ArtistDetail("Artist") to "artist-first-track",
        ).forEach { (_, representativeTrackId) ->
            assertEquals(
                DrillDownScrollOwner.Miuix,
                drillDownScrollOwner(
                    DrillDownArtwork(
                        representativeTrackId = representativeTrackId,
                        state = TrackArtworkLoadState.Loading,
                    ),
                ),
            )
            assertEquals(
                DrillDownScrollOwner.Miuix,
                drillDownScrollOwner(
                    DrillDownArtwork(
                        representativeTrackId = representativeTrackId,
                        state = TrackArtworkLoadState.Unavailable,
                    ),
                ),
            )
            assertEquals(
                DrillDownScrollOwner.Artwork,
                drillDownScrollOwner(
                    DrillDownArtwork(
                        representativeTrackId = representativeTrackId,
                        state = TrackArtworkLoadState.Available(byteArrayOf(1)),
                    ),
                ),
            )
        }
    }

    @Test
    fun listTopAndChromeHeightUseTheSameSnapshotValue() {
        val snapshot = geometry.snapshot(offsetPx = 75f)

        assertEquals(snapshot.headerHeightPx, artworkListTopPaddingPx(snapshot))
        assertEquals(snapshot.headerHeightPx, artworkChromeHeightPx(snapshot))
    }
}
