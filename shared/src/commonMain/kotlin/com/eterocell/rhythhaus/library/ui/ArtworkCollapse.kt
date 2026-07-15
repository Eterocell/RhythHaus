package com.eterocell.rhythhaus.library.ui

internal data class ArtworkCollapseSnapshot(
    val offsetPx: Float,
    val headerHeightPx: Float,
    val progress: Float,
)

internal data class ArtworkCollapseConsumption(
    val offsetPx: Float,
    val consumedY: Float,
)

internal data class ArtworkCollapseGeometry(
    val expandedHeightPx: Float,
    val collapsedHeightPx: Float,
) {
    val collapseRangePx: Float
        get() = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(0f)

    fun snapshot(offsetPx: Float): ArtworkCollapseSnapshot {
        if (collapseRangePx == 0f) {
            return ArtworkCollapseSnapshot(offsetPx = 0f, headerHeightPx = collapsedHeightPx, progress = 1f)
        }
        val clampedOffset = offsetPx.coerceIn(0f, collapseRangePx)
        return ArtworkCollapseSnapshot(
            offsetPx = clampedOffset,
            headerHeightPx = expandedHeightPx - clampedOffset,
            progress = clampedOffset / collapseRangePx,
        )
    }

    fun consumeUpward(offsetPx: Float, availableY: Float): ArtworkCollapseConsumption {
        val before = snapshot(offsetPx).offsetPx
        if (availableY >= 0f || collapseRangePx == 0f) return ArtworkCollapseConsumption(before, 0f)
        val after = (before - availableY).coerceAtMost(collapseRangePx)
        val consumedY = if (after == before) 0f else -(after - before)
        return ArtworkCollapseConsumption(offsetPx = after, consumedY = consumedY)
    }

    fun consumeDownward(offsetPx: Float, availableY: Float): ArtworkCollapseConsumption {
        val before = snapshot(offsetPx).offsetPx
        if (availableY <= 0f || collapseRangePx == 0f) return ArtworkCollapseConsumption(before, 0f)
        val after = (before - availableY).coerceAtLeast(0f)
        return ArtworkCollapseConsumption(offsetPx = after, consumedY = before - after)
    }
}
