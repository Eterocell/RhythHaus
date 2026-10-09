package com.eterocell.rhythhaus.library.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayGainTagParserTest {
    @Test
    fun gainAcceptsTrimmedNumbersAndCaseInsensitiveDbSuffix() {
        val cases =
            listOf(
                " -6.00 dB " to -6.0,
                "+3 DB" to 3.0,
                "-4db" to -4.0,
                "0" to 0.0,
                " .5 " to 0.5,
                "2." to 2.0,
                "-1.25e2 dB" to -125.0,
                "1.7976931348623157e308" to Double.MAX_VALUE,
                "-1.7976931348623157e308 dB" to -Double.MAX_VALUE,
            )

        cases.forEach { (raw, expected) ->
            assertEquals(expected, parseReplayGainDb(raw), "gain=$raw")
        }
    }

    @Test
    fun gainRejectsMalformedAndNonFiniteValues() {
        val cases =
            listOf(
                null,
                "",
                "   ",
                "dB",
                "NaN",
                "NaN dB",
                "Infinity",
                "+Infinity dB",
                "-Infinity DB",
                "1.8e308",
                "junk",
                "-6 dB extra",
                "-6 dB dB",
                "-6,0",
                "- 6",
                "2f",
                "0x1.0p0",
            )

        cases.forEach { raw ->
            assertNull(parseReplayGainDb(raw), "gain=$raw")
        }
    }

    @Test
    fun peakAcceptsOnlyFinitePositiveNumbers() {
        val cases =
            listOf(
                "2" to 2.0,
                " 1.25 " to 1.25,
                "+.5" to 0.5,
                "1e-2" to 0.01,
                "4.9e-324" to Double.MIN_VALUE,
                "1.7976931348623157e308" to Double.MAX_VALUE,
            )

        cases.forEach { (raw, expected) ->
            assertEquals(expected, parseReplayGainPeak(raw), "peak=$raw")
        }
    }

    @Test
    fun peakRejectsNonPositiveMalformedAndUnitSuffixedValues() {
        val cases =
            listOf(
                null,
                "",
                "   ",
                "0",
                "-0.0",
                "-2",
                "1e-9999",
                "NaN",
                "Infinity",
                "1.8e308",
                "2 dB",
                "2DB",
                "2 peak",
                "2f",
                "0x1.0p0",
                "junk",
            )

        cases.forEach { raw ->
            assertNull(parseReplayGainPeak(raw), "peak=$raw")
        }
    }
}
