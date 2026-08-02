package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformCapabilityBoundaryTest {
    @Test
    fun currentTimeMillisReturnsTheCurrentEpochMillis() {
        val before = currentSystemEpochMillis()
        val actual = currentTimeMillis()
        val after = currentSystemEpochMillis()

        assertTrue(actual in before..after)
    }

    @Test
    fun uuid4ReturnsUniqueRfc4122Version4Identifiers() {
        val identifiers = List(16) { uuid4() }
        val version4UuidPattern =
            Regex(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")

        assertTrue(identifiers.all(version4UuidPattern::matches))
        assertEquals(identifiers.size, identifiers.toSet().size)
    }

    private fun currentSystemEpochMillis(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds()
}
