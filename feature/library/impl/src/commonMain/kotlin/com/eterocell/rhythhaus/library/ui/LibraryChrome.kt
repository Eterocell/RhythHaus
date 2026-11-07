package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import kotlinx.coroutines.launch

@Composable
internal fun DrillDownScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollFraction by
        remember(listState) {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val total = layoutInfo.totalItemsCount
                val visible = layoutInfo.visibleItemsInfo.size
                val maxFirstVisibleIndex = (total - visible).coerceAtLeast(1)
                (listState.firstVisibleItemIndex.toFloat() /
                        maxFirstVisibleIndex)
                    .coerceIn(0f, 1f)
            }
        }

    fun scrollTo(yPosition: Float, trackHeightPx: Float) {
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo.size
        if (total <= visible || trackHeightPx <= 0f) return

        val maxFirstVisibleIndex = (total - visible).coerceAtLeast(0)
        val targetFraction = (yPosition / trackHeightPx).coerceIn(0f, 1f)
        val targetIndex =
            (targetFraction * maxFirstVisibleIndex)
                .toInt()
                .coerceIn(0, maxFirstVisibleIndex)
        coroutineScope.launch {
            // Must be an immediate (non-animated) scroll: animateScrollToItem
            // takes ~300ms
            // and gets cancelled/restarted on every drag-move event, so the
            // list perpetually
            // chases a stale animation and only catches up once the drag ends.
            listState.scrollToItem(index = targetIndex, scrollOffset = 0)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .width(24.dp)
                .pointerInput(listState) {
                    detectTapGestures { offset ->
                        scrollTo(offset.y, size.height.toFloat())
                    }
                }
                .pointerInput(listState) {
                    detectVerticalDragGestures { change, _ ->
                        scrollTo(change.position.y, size.height.toFloat())
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        val thumbHeight = maxHeight * 0.15f
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = thumbOffset)
                    .width(6.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        HausColors.current.textSecondary.copy(alpha = 0.42f)),
        )
    }
}
