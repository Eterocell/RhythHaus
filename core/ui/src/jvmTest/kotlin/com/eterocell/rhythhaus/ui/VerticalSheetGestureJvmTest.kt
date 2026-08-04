package com.eterocell.rhythhaus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises the public pointer-input modifier, including its settled animation
 * state.
 */
public class VerticalSheetGestureJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun upwardTerminalGestureSettlesAtOneAndCallsOnce(): Unit =
        runComposeUiTest {
            var callbacks = 0
            lateinit var progress: Animatable<Float, *>
            setContent {
                GestureSurface(
                    0f,
                    VerticalSheetGestureDirection.Upward,
                    onProgress = { progress = it },
                ) {
                    callbacks += 1
                }
            }
            onNodeWithTag("gesture").performTouchInput { swipeUp() }
            waitForIdle()
            assertEquals(1, callbacks)
            assertEquals(1f, progress.value)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun downwardTerminalGestureSettlesAtZeroAndCallsOnce(): Unit =
        runComposeUiTest {
            var callbacks = 0
            lateinit var progress: Animatable<Float, *>
            setContent {
                GestureSurface(
                    1f,
                    VerticalSheetGestureDirection.Downward,
                    onProgress = { progress = it }) {
                        callbacks += 1
                    }
            }
            onNodeWithTag("gesture").performTouchInput { swipeDown() }
            waitForIdle()
            assertEquals(1, callbacks)
            assertEquals(0f, progress.value)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun upwardOppositeGestureBeyondSlopSettlesAtZeroWithoutCallback():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                0.2f,
                VerticalSheetGestureDirection.Upward,
                onProgress = { progress = it }) {
                    callbacks += 1
                }
        }
        onNodeWithTag("gesture").performTouchInput { swipeDown() }
        waitForIdle()
        assertEquals(0, callbacks)
        assertEquals(0f, progress.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun downwardOppositeGestureBeyondSlopSettlesAtOneWithoutCallback():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                0.8f,
                VerticalSheetGestureDirection.Downward,
                onProgress = { progress = it }) {
                    callbacks += 1
                }
        }
        onNodeWithTag("gesture").performTouchInput { swipeUp() }
        waitForIdle()
        assertEquals(0, callbacks)
        assertEquals(1f, progress.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun inactiveGestureLeavesProgressAndCallbackCountUnchanged(): Unit =
        runComposeUiTest {
            var callbacks = 0
            lateinit var progress: Animatable<Float, *>
            setContent {
                GestureSurface(
                    .42f,
                    VerticalSheetGestureDirection.Upward,
                    active = false,
                    onProgress = { progress = it }) {
                        callbacks += 1
                    }
            }
            onNodeWithTag("gesture").performTouchInput { swipeUp() }
            waitForIdle()
            assertEquals(0, callbacks)
            assertEquals(.42f, progress.value)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun upwardTerminalCancellationSettlesAtOneAndCallsOnce(): Unit =
        runComposeUiTest {
            var callbacks = 0
            lateinit var progress: Animatable<Float, *>
            setContent {
                GestureSurface(
                    0f,
                    VerticalSheetGestureDirection.Upward,
                    cancellationInterceptor = true,
                    onProgress = { progress = it },
                ) {
                    callbacks += 1
                }
            }
            onNodeWithTag("gesture").performTouchInput {
                down(center)
                moveBy(Offset(0f, -20f))
                moveBy(Offset(0f, -35f))
                moveBy(Offset(0f, -35f))
            }
            waitForIdle()
            waitForIdle()
            assertEquals(1, callbacks)
            assertEquals(1f, progress.value)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun downwardNonTerminalCancellationSettlesAtOneWithoutCallback():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                0.8f,
                VerticalSheetGestureDirection.Downward,
                cancellationInterceptor = true,
                onProgress = { progress = it },
            ) {
                callbacks += 1
            }
        }
        onNodeWithTag("gesture").performTouchInput {
            down(center)
            moveBy(Offset(0f, -20f))
            moveBy(Offset(0f, -35f))
            moveBy(Offset(0f, -35f))
        }
        waitForIdle()
        waitForIdle()
        assertEquals(0, callbacks)
        assertEquals(1f, progress.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun upwardExactThresholdIsTerminalAfterRecognizedPointerDrag():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                .3f,
                VerticalSheetGestureDirection.Upward,
                referenceHeight = Float.MAX_VALUE,
                onProgress = { progress = it },
            ) {
                callbacks += 1
            }
        }
        onNodeWithTag("gesture").performTouchInput { swipeDown() }
        waitForIdle()
        assertEquals(1, callbacks)
        assertEquals(1f, progress.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun downwardExactThresholdIsNotTerminalAfterRecognizedPointerDrag():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                .3f,
                VerticalSheetGestureDirection.Downward,
                referenceHeight = Float.MAX_VALUE,
                onProgress = { progress = it },
            ) {
                callbacks += 1
            }
        }
        onNodeWithTag("gesture").performTouchInput { swipeDown() }
        waitForIdle()
        assertEquals(0, callbacks)
        assertEquals(1f, progress.value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun referenceHeightChangesTheSameDragOutcome(): Unit =
        runComposeUiTest {
            var shortCallbacks = 0
            lateinit var shortProgress: Animatable<Float, *>
            setContent {
                GestureSurface(
                    0f,
                    VerticalSheetGestureDirection.Upward,
                    referenceHeight = 50f,
                    onProgress = { shortProgress = it }) {
                        shortCallbacks += 1
                    }
            }
            onNodeWithTag("gesture").performTouchInput {
                down(center)
                moveBy(Offset(0f, -35f))
                up()
            }
            waitForIdle()
            assertEquals(1, shortCallbacks)
            assertEquals(1f, shortProgress.value)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun measuredHeightFallbackKeepsTheSameDragBelowTerminalThreshold():
        Unit = runComposeUiTest {
        var callbacks = 0
        lateinit var progress: Animatable<Float, *>
        setContent {
            GestureSurface(
                0f,
                VerticalSheetGestureDirection.Upward,
                referenceHeight = null,
                onProgress = { progress = it }) {
                    callbacks += 1
                }
        }
        onNodeWithTag("gesture").performTouchInput {
            down(center)
            moveBy(Offset(0f, -35f))
            up()
        }
        waitForIdle()
        assertEquals(0, callbacks)
        assertEquals(0f, progress.value)
    }
}

@Composable
private fun GestureSurface(
    initialProgress: Float,
    direction: VerticalSheetGestureDirection,
    active: Boolean = true,
    referenceHeight: Float? = 100f,
    cancellationInterceptor: Boolean = false,
    onProgress: (Animatable<Float, *>) -> Unit,
    onTerminal: () -> Unit,
): Unit {
    val progress = remember { Animatable(initialProgress) }
    onProgress(progress)
    val scope = rememberCoroutineScope()
    Box(
        Modifier.size(100.dp)
            .verticalSheetGesture(
                expandProgress = progress,
                isActive = active,
                scope = scope,
                direction = direction,
                onTerminal = onTerminal,
                threshold = .3f,
                referenceHeight = referenceHeight,
            ),
    ) {
        Box(
            Modifier.size(100.dp)
                .testTag("gesture")
                .then(
                    if (cancellationInterceptor) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                var movementEvents = 0
                                while (true) {
                                    val event =
                                        awaitPointerEvent(PointerEventPass.Main)
                                    event.changes
                                        .filter {
                                            it.pressed &&
                                                it.positionChange() !=
                                                    Offset.Zero
                                        }
                                        .onEach { movementEvents += 1 }
                                        .filter { movementEvents >= 3 }
                                        .forEach { it.consume() }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}
