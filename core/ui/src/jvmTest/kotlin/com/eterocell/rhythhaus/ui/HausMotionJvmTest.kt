package com.eterocell.rhythhaus.ui

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
}

private class TestMotionDurationScale(
    override val scaleFactor: Float,
) : MotionDurationScale
