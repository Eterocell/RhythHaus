package com.eterocell.rhythhaus.ui

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val DefaultThreshold = 0.7f
private const val RubberBandFactor = 0.5f

fun Modifier.verticalSheetGesture(
    expandProgress: Animatable<Float, AnimationVector1D>,
    isActive: Boolean,
    scope: CoroutineScope,
    onSwipeExpand: () -> Unit,
    onSwipeCollapse: () -> Unit,
    threshold: Float = DefaultThreshold,
    referenceHeight: Float? = null,
): Modifier = pointerInput(isActive) {
    if (!isActive) return@pointerInput
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = {
            totalDrag = 0f
        },
        onVerticalDrag = { _, dragAmount ->
            scope.launch {
                totalDrag += dragAmount
                val screenHeight = referenceHeight ?: size.height.toFloat()
                if (screenHeight <= 0f) return@launch
                val current = expandProgress.value
                val delta = -(dragAmount / screenHeight)
                var target = current + delta
                if (target < 0f) {
                    target = current + delta * RubberBandFactor
                } else if (target > 1f) {
                    target = current + delta * RubberBandFactor
                }
                expandProgress.snapTo(target.coerceIn(-0.05f, 1.05f))
            }
        },
        onDragEnd = {
            scope.launch {
                val target = expandProgress.value
                if (target >= threshold) {
                    expandProgress.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                    onSwipeExpand()
                } else {
                    expandProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    onSwipeCollapse()
                }
            }
        },
        onDragCancel = {
            scope.launch {
                val current = expandProgress.value
                val target = if (current >= threshold) 1f else 0f
                expandProgress.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                if (target == 1f) onSwipeExpand() else onSwipeCollapse()
            }
        },
    )
}
