package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RhythHausTagLibTest {
    @Test
    fun readPathDelegatesToReader() {
        val expected = TagReadResult.Found(
            TagMetadata(
                title = "Maple Garden",
                artist = "RhythHaus",
                durationMillis = 123_000,
                bitrate = 320,
                sampleRate = 44_100,
                channels = 2,
            ),
        )
        val reader = RecordingReader(expected)

        val result = RhythHausTagLib.readPath("/music/maple-garden.flac", reader)

        assertSame(expected, result)
        assertEquals(listOf("/music/maple-garden.flac"), reader.paths)
    }

    @Test
    fun unsupportedPathResultIsPublicModel() {
        val result: TagReadResult = TagReadResult.Unsupported("Native TagLib reader is not linked yet")

        assertTrue(result is TagReadResult.Unsupported)
        assertEquals("Native TagLib reader is not linked yet", result.reason)
    }

    private class RecordingReader(
        private val result: TagReadResult,
    ) : TagLibReader {
        val paths = mutableListOf<String>()

        override fun readPath(path: String): TagReadResult {
            paths += path
            return result
        }

        override fun readProperties(path: String): Map<String, String> = emptyMap()
    }
}
