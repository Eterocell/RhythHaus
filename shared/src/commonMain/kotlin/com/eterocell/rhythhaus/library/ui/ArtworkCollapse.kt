package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.ui.TrackArtworkLoadState

internal data class ArtworkCollapseSnapshot(
    val upperSliceHeightPx: Float,
    val lowerSliceHeightPx: Float,
    val lowerSliceImageOffsetPx: Float,
    val progress: Float,
)

internal enum class DrillDownScrollOwner { Artwork, Miuix }

internal enum class ArtworkHeaderItemPolicy { UpperAndStickyLower, StickyLowerOnly }

internal fun artworkHeaderItemPolicy(geometry: ArtworkCollapseGeometry): ArtworkHeaderItemPolicy =
    if (geometry.collapseRangePx > 0f) {
        ArtworkHeaderItemPolicy.UpperAndStickyLower
    } else {
        ArtworkHeaderItemPolicy.StickyLowerOnly
    }

internal data class DrillDownListSpacing(
    val horizontalPaddingDp: Float,
    val itemGapDp: Float,
    val artworkSliceGapDp: Float,
)

internal val ArtworkDrillDownListSpacing = DrillDownListSpacing(20f, 18f, 0f)

internal fun artworkChromeSolidAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

internal data class ArtworkSlicePlaneGeometry(
    val planeSide: Float,
    val viewportHeight: Float,
    val imageOffsetY: Float,
)

internal fun artworkSlicePlaneGeometry(
    expandedSize: Float,
    viewportHeight: Float,
    imageOffsetY: Float,
): ArtworkSlicePlaneGeometry = ArtworkSlicePlaneGeometry(
    planeSide = expandedSize,
    viewportHeight = viewportHeight,
    imageOffsetY = imageOffsetY,
)

internal data class ArtworkTitleAvailableWidth(
    val collapsedDp: Float,
    val expandedDp: Float,
)

internal fun artworkTitleAvailableWidth(
    containerWidthDp: Float,
    safeStartInsetDp: Float,
): ArtworkTitleAvailableWidth {
    val centeredSideReservationDp = safeStartInsetDp + 12f + 44f + 8f
    return ArtworkTitleAvailableWidth(
        collapsedDp = (containerWidthDp - centeredSideReservationDp * 2f).coerceAtLeast(0f),
        expandedDp = (containerWidthDp - ArtworkDrillDownListSpacing.horizontalPaddingDp * 2f).coerceAtLeast(0f),
    )
}

internal data class DrillDownArtwork(
    val representativeTrackId: String?,
    val state: TrackArtworkLoadState,
)

internal fun drillDownScrollOwner(artwork: DrillDownArtwork): DrillDownScrollOwner =
    if (artwork.state is TrackArtworkLoadState.Available) DrillDownScrollOwner.Artwork else DrillDownScrollOwner.Miuix

internal data class ArtworkCollapseGeometry(
    val expandedHeightPx: Float,
    val collapsedHeightPx: Float,
) {
    val collapseRangePx: Float
        get() = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(0f)

    fun snapshot(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): ArtworkCollapseSnapshot {
        val range = collapseRangePx
        if (range == 0f) return ArtworkCollapseSnapshot(0f, collapsedHeightPx, 0f, 1f)
        val consumed = if (firstVisibleItemIndex > 0) {
            range
        } else {
            firstVisibleItemScrollOffset.toFloat().coerceIn(0f, range)
        }
        return ArtworkCollapseSnapshot(
            upperSliceHeightPx = range,
            lowerSliceHeightPx = collapsedHeightPx,
            lowerSliceImageOffsetPx = -range,
            progress = consumed / range,
        )
    }
}
