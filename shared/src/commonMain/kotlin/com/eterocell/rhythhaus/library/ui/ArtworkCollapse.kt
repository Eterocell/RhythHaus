package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.eterocell.rhythhaus.ui.TrackArtworkLoadState

internal data class ArtworkCollapseSnapshot(
    val offsetPx: Float,
    val headerHeightPx: Float,
    val progress: Float,
)

internal data class ArtworkCollapseConsumption(
    val offsetPx: Float,
    val consumedY: Float,
)

internal enum class DrillDownScrollOwner { Artwork, Miuix }

internal data class DrillDownArtwork(
    val representativeTrackId: String?,
    val state: TrackArtworkLoadState,
)

internal fun drillDownScrollOwner(artwork: DrillDownArtwork): DrillDownScrollOwner =
    if (artwork.state is TrackArtworkLoadState.Available) DrillDownScrollOwner.Artwork else DrillDownScrollOwner.Miuix

internal fun artworkListTopPaddingPx(snapshot: ArtworkCollapseSnapshot): Float = snapshot.headerHeightPx

internal fun artworkChromeHeightPx(snapshot: ArtworkCollapseSnapshot): Float = snapshot.headerHeightPx

internal class ArtworkCollapseState internal constructor(
    private val offsetState: MutableState<Float>,
    private val geometryState: State<ArtworkCollapseGeometry>,
) {
    val snapshot: ArtworkCollapseSnapshot
        get() = geometryState.value.snapshot(offsetState.value)

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val result = geometryState.value.consumeUpward(offsetState.value, available.y)
            offsetState.value = result.offsetPx
            return Offset(0f, result.consumedY)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val result = geometryState.value.consumeDownward(offsetState.value, available.y)
            offsetState.value = result.offsetPx
            return Offset(0f, result.consumedY)
        }
    }
}

@Composable
internal fun rememberArtworkCollapseState(
    geometry: ArtworkCollapseGeometry,
): ArtworkCollapseState {
    val offsetState = remember { mutableStateOf(0f) }
    val geometryState = rememberUpdatedState(geometry)
    val state = remember { ArtworkCollapseState(offsetState, geometryState) }
    val clampedOffset = geometry.snapshot(offsetState.value).offsetPx
    SideEffect {
        if (offsetState.value != clampedOffset) offsetState.value = clampedOffset
    }
    return state
}

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
