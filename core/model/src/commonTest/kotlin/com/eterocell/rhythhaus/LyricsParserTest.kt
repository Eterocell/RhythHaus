package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsParserTest {
    @Test
    fun parsesMultipleTimestampsOffsetsAndPlainLinesStably() {
        val parsed =
            parseLyrics(
                "[offset:-1500]\n[00:02.50][00:01.250]line\n[bad:00.00]plain\n[00:01.25]tie",
            )

        assertEquals(
            listOf(
                LyricLine(0L, "line"),
                LyricLine(0L, "tie"),
                LyricLine(1_000L, "line"),
                LyricLine(null, "[bad:00.00]plain"),
            ),
            parsed,
        )
    }

    @Test
    fun offsetArithmeticSaturatesWithoutWrappingTimestamp() {
        assertEquals(
            Long.MAX_VALUE,
            parseLyrics("[offset:9223372036854775807]\n[00:01.00]line")
                .single()
                .timestampMillis,
        )
        assertEquals(
            0L,
            parseLyrics("[offset:-9223372036854775808]\n[00:01.00]line")
                .single()
                .timestampMillis,
        )
    }

    @Test
    fun parsesCentisecondsAndPreservesUntimedDocument() {
        assertEquals(
            listOf(LyricLine(12_340L, "hello"), LyricLine(null, "tail")),
            parseLyrics("[00:12.34]hello\ntail"),
        )
    }
}
