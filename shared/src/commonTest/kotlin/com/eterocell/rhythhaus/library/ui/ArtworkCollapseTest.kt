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
    fun listPositionDerivesExpandedPartialAndCollapsedProgress() {
        assertEquals(
            ArtworkCollapseSnapshot(240f, 80f, -240f, 0f),
            geometry.snapshot(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0),
        )
        assertEquals(
            ArtworkCollapseSnapshot(240f, 80f, -240f, 0.5f),
            geometry.snapshot(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 120),
        )
        assertEquals(
            ArtworkCollapseSnapshot(240f, 80f, -240f, 1f),
            geometry.snapshot(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0),
        )
    }

    @Test
    fun itemZeroOffsetZeroRestoresExpandedArtwork() {
        assertEquals(0f, geometry.snapshot(0, 0).progress)
    }

    @Test
    fun offsetsClampToTheArtworkRange() {
        assertEquals(0f, geometry.snapshot(0, -20).progress)
        assertEquals(1f, geometry.snapshot(0, 500).progress)
    }

    @Test
    fun zeroAndInvertedRangesUseOneCollapsedSlice() {
        listOf(
            ArtworkCollapseGeometry(80f, 80f),
            ArtworkCollapseGeometry(60f, 80f),
        ).forEach { invalid ->
            assertEquals(
                ArtworkCollapseSnapshot(0f, 80f, 0f, 1f),
                invalid.snapshot(0, 40),
            )
        }
    }

    @Test
    fun slicesFormOneSquareAndShareOneImagePlacement() {
        val snapshot = geometry.snapshot(0, 0)
        assertEquals(320f, snapshot.upperSliceHeightPx + snapshot.lowerSliceHeightPx)
        assertEquals(-snapshot.upperSliceHeightPx, snapshot.lowerSliceImageOffsetPx)
    }

    @Test
    fun resizedGeometryRecomputesFromCurrentListPosition() {
        val resized = ArtworkCollapseGeometry(200f, 80f)
        assertEquals(1f, resized.snapshot(0, 220).progress)
        assertEquals(120f, resized.snapshot(0, 220).upperSliceHeightPx)
    }

    @Test
    fun validRangeUsesUpperAndStickyLowerArtworkItems() {
        assertEquals(
            ArtworkHeaderItemPolicy.UpperAndStickyLower,
            artworkHeaderItemPolicy(geometry),
        )
    }

    @Test
    fun zeroRangeUsesOnlyStickyLowerArtworkItem() {
        assertEquals(
            ArtworkHeaderItemPolicy.StickyLowerOnly,
            artworkHeaderItemPolicy(ArtworkCollapseGeometry(80f, 80f)),
        )
    }

    @Test
    fun artworkListSpacingPreservesRowsWithoutSplittingArtworkSlices() {
        assertEquals(20f, ArtworkDrillDownListSpacing.horizontalPaddingDp)
        assertEquals(18f, ArtworkDrillDownListSpacing.itemGapDp)
        assertEquals(0f, ArtworkDrillDownListSpacing.artworkSliceGapDp)
    }

    @Test
    fun artworkChromeSolidAlphaClampsCollapseProgress() {
        assertEquals(0f, artworkChromeSolidAlpha(-0.2f))
        assertEquals(0f, artworkChromeSolidAlpha(0f))
        assertEquals(0.5f, artworkChromeSolidAlpha(0.5f))
        assertEquals(1f, artworkChromeSolidAlpha(1f))
        assertEquals(1f, artworkChromeSolidAlpha(1.4f))
    }

    @Test
    fun artworkTitleAvailableWidthReservesMarginsAndSafeBackRegion() {
        assertEquals(
            ArtworkTitleAvailableWidth(collapsedDp = 176f, expandedDp = 280f),
            artworkTitleAvailableWidth(containerWidthDp = 320f, safeStartInsetDp = 8f),
        )
        assertEquals(
            ArtworkTitleAvailableWidth(collapsedDp = 0f, expandedDp = 0f),
            artworkTitleAvailableWidth(containerWidthDp = 32f, safeStartInsetDp = 40f),
        )
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

}
