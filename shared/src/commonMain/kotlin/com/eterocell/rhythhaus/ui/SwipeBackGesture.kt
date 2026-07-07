package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val SwipeBackEdgeWidthPx = 56f
private const val SwipeBackTriggerDistancePx = 96f

fun Modifier.leftEdgeSwipeBack(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    var startedAtLeftEdge = false
    var accumulatedDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { offset ->
            startedAtLeftEdge = offset.x <= SwipeBackEdgeWidthPx
            accumulatedDrag = 0f
        },
        onHorizontalDrag = { _, dragAmount ->
            if (startedAtLeftEdge) {
                accumulatedDrag = (accumulatedDrag + dragAmount).coerceAtLeast(0f)
            }
        },
        onDragEnd = {
            if (startedAtLeftEdge && accumulatedDrag >= SwipeBackTriggerDistancePx) {
                onBack()
            }
            startedAtLeftEdge = false
            accumulatedDrag = 0f
        },
        onDragCancel = {
            startedAtLeftEdge = false
            accumulatedDrag = 0f
        },
    )
}
