package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HausMotionJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zeroHostDurationScaleEnablesReducedMotion() =
        runComposeUiTest(
            effectContext = TestMotionDurationScale(scaleFactor = 0f),
        ) {
            var reducedMotion: Boolean? = null
            setContent { reducedMotion = rememberHausReducedMotion() }

            runOnIdle { assertEquals(true, reducedMotion) }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun positiveHostDurationScaleKeepsStandardMotion() =
        runComposeUiTest(
            effectContext = TestMotionDurationScale(scaleFactor = 1f),
        ) {
            var reducedMotion: Boolean? = null
            setContent { reducedMotion = rememberHausReducedMotion() }

            runOnIdle { assertEquals(false, reducedMotion) }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reducedMotionUsesLatestPlatformProjectionWithoutRestart() {
        val durationScale = MutableTestMotionDurationScale(1f)
        runComposeUiTest(effectContext = durationScale) {
            var reducedMotion: Boolean? = null
            setContent { reducedMotion = rememberHausReducedMotion() }

            runOnIdle { assertEquals(false, reducedMotion) }

            runOnIdle { durationScale.scaleFactor = 0f }
            waitForIdle()
            runOnIdle { assertEquals(true, reducedMotion) }

            runOnIdle { durationScale.scaleFactor = 2f }
            waitForIdle()
            runOnIdle { assertEquals(false, reducedMotion) }
        }
    }
}

private class TestMotionDurationScale(
    override val scaleFactor: Float,
) : MotionDurationScale

private class MutableTestMotionDurationScale(
    initialScaleFactor: Float,
) : MotionDurationScale {
    private val state = mutableStateOf(initialScaleFactor)
    override var scaleFactor: Float
        get() = state.value
        set(value) {
            state.value = value
        }
}
