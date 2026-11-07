package com.eterocell.rhythhaus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val DefaultThreshold = 0.7f
private const val RubberBandFactor = 0.5f

/**
 * Restricts the stateless drag mechanism to one locally owned terminal
 * direction.
 */
public enum class VerticalSheetGestureDirection {
    Upward,
    Downward
}

/**
 * Mutates [expandProgress] and invokes [onTerminal] only at [direction]'s
 * terminal state.
 *
 * @param preferDownwardDragOverScroll claims downward drags in the Initial
 *   pointer pass, before a parent scrollable can consume them. Use only for
 *   non-interactive child surfaces that own this dismissal gesture.
 */
public fun Modifier.verticalSheetGesture(
    expandProgress: Animatable<Float, AnimationVector1D>,
    isActive: Boolean,
    scope: CoroutineScope,
    direction: VerticalSheetGestureDirection,
    onTerminal: () -> Unit,
    threshold: Float = DefaultThreshold,
    referenceHeight: Float? = null,
    preferDownwardDragOverScroll: Boolean = false,
): Modifier =
    pointerInput(
        isActive,
        direction,
        threshold,
        referenceHeight,
        preferDownwardDragOverScroll,
    ) {
        if (!isActive) return@pointerInput
        var totalDrag = 0f
        var gestureStarted = false
        val onDragStart = {
            gestureStarted = true
            totalDrag = 0f
        }
        val onVerticalDrag: (PointerInputChange, Float) -> Unit =
            { _, dragAmount ->
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
            }
        val onDragEnd: () -> Unit = {
            gestureStarted = false
            scope.launch {
                val target = expandProgress.value
                val reachesTerminal =
                    when (direction) {
                        VerticalSheetGestureDirection.Upward ->
                            target >= threshold
                        VerticalSheetGestureDirection.Downward ->
                            target < threshold
                    }
                if (reachesTerminal) {
                    onTerminal()
                    expandProgress.animateTo(
                        if (direction == VerticalSheetGestureDirection.Upward)
                            1f
                        else 0f,
                        spring(stiffness = Spring.StiffnessMediumLow),
                    )
                } else {
                    expandProgress.animateTo(
                        if (direction == VerticalSheetGestureDirection.Upward)
                            0f
                        else 1f,
                        spring(stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }
        }
        val onDragCancel: () -> Unit = {
            gestureStarted = false
            scope.launch {
                val current = expandProgress.value
                val reachesTerminal =
                    when (direction) {
                        VerticalSheetGestureDirection.Upward ->
                            current >= threshold
                        VerticalSheetGestureDirection.Downward ->
                            current < threshold
                    }
                val target =
                    if (reachesTerminal) {
                        if (direction == VerticalSheetGestureDirection.Upward)
                            1f
                        else 0f
                    } else {
                        if (direction == VerticalSheetGestureDirection.Upward)
                            0f
                        else 1f
                    }
                expandProgress.animateTo(
                    target,
                    spring(stiffness = Spring.StiffnessMediumLow),
                )
                if (reachesTerminal) onTerminal()
            }
        }
        try {
            if (preferDownwardDragOverScroll &&
                direction == VerticalSheetGestureDirection.Downward) {
                detectDownwardDragBeforeScroll(
                    onDragStart = onDragStart,
                    onVerticalDrag = onVerticalDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            } else {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onVerticalDrag = onVerticalDrag,
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            }
        } finally {
            if (gestureStarted) onDragCancel()
        }
    }

private suspend fun PointerInputScope.detectDownwardDragBeforeScroll(
    onDragStart: () -> Unit,
    onVerticalDrag: (PointerInputChange, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val mouseTouchSlop = 0.125.dp.toPx()
    awaitEachGesture {
        var dragStarted = false
        try {
            val down =
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
            val touchSlop =
                if (down.type == PointerType.Mouse) mouseTouchSlop
                else viewConfiguration.touchSlop
            var totalX = 0f
            var totalY = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    if (dragStarted) {
                        dragStarted = false
                        onDragCancel()
                    }
                    return@awaitEachGesture
                }
                if (!change.pressed) {
                    if (dragStarted) {
                        dragStarted = false
                        if (event.changes.any { it.pressed }) onDragCancel()
                        else onDragEnd()
                    }
                    return@awaitEachGesture
                }

                val movement = change.positionChangeIgnoreConsumed()
                totalX += movement.x
                totalY += movement.y
                if (!dragStarted) {
                    if (abs(totalX) >= touchSlop || abs(totalY) >= touchSlop) {
                        if (totalY > 0f && abs(totalY) > abs(totalX)) {
                            dragStarted = true
                            change.consume()
                            onDragStart()
                            onVerticalDrag(change, totalY - touchSlop)
                        } else {
                            return@awaitEachGesture
                        }
                    }
                } else {
                    change.consume()
                    onVerticalDrag(change, movement.y)
                }
            }
        } catch (cancellation: CancellationException) {
            if (dragStarted) {
                dragStarted = false
                onDragCancel()
            }
            throw cancellation
        } finally {
            if (dragStarted) {
                dragStarted = false
                onDragCancel()
            }
        }
    }
}
