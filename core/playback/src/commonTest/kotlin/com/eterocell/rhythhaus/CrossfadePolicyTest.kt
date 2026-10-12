package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrossfadePolicyTest {
    @Test
    fun windowFitsBothTracksAndRemainingPlayback() {
        assertEquals(
            2000L, effectiveCrossfadeWindowMillis(5000, 20000, 4000, 5000))
        assertEquals(
            1000L, effectiveCrossfadeWindowMillis(5000, 20000, 4000, 1000))
        assertEquals(
            250L, effectiveCrossfadeWindowMillis(5000, 20000, 4000, 250))
        assertNull(effectiveCrossfadeWindowMillis(5000, 20000, 4000, 249))
    }

    @Test
    fun nonpositiveOrTooShortDurationsCannotOverlap() {
        assertNull(effectiveCrossfadeWindowMillis(0, 20000, 4000, 5000))
        assertNull(effectiveCrossfadeWindowMillis(5000, 0, 4000, 5000))
        assertNull(effectiveCrossfadeWindowMillis(5000, 20000, -1, 5000))
        assertNull(effectiveCrossfadeWindowMillis(5000, 20000, 499, 5000))
        assertNull(effectiveCrossfadeWindowMillis(5000, 20000, 4000, 0))
        assertEquals(
            Long.MAX_VALUE / 2,
            effectiveCrossfadeWindowMillis(
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
    }
}
