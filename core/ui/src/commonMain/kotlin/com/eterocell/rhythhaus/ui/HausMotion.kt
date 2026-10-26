package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale

/** Reports whether the host has disabled animation durations. */
@Composable
public fun rememberHausReducedMotion(): Boolean {
    val durationScale =
        rememberCoroutineScope().coroutineContext[MotionDurationScale.Key]
    return durationScale?.scaleFactor == 0f
}
