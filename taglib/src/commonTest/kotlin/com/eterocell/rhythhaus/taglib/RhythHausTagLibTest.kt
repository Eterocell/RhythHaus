package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhythHausTagLibTest {
    @Test
    fun emptyInputIsUnsupportedInsteadOfThrowing() {
        val result = RhythHausTagLib.read(ByteArray(0))

        assertTrue(result is TagReadResult.Unsupported)
        assertEquals("No supported tag header found", result.reason)
    }
}
