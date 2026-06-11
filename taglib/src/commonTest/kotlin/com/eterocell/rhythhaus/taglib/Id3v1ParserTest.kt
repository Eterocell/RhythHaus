package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Id3v1ParserTest {
    @Test
    fun readsBasicId3v1FieldsFromTail() {
        val bytes = ByteArray(256)
        val tagStart = bytes.size - 128
        writeAscii(bytes, tagStart, "TAG", 3)
        writeAscii(bytes, tagStart + 3, "Midnight City", 30)
        writeAscii(bytes, tagStart + 33, "M83", 30)
        writeAscii(bytes, tagStart + 63, "Hurry Up", 30)
        writeAscii(bytes, tagStart + 93, "2011", 4)
        writeAscii(bytes, tagStart + 97, "single", 30)
        bytes[tagStart + 127] = 17

        val result = RhythHausTagLib.read(bytes)

        assertTrue(result is TagReadResult.Found)
        assertEquals(TagFormat.ID3V1, result.format)
        assertEquals("Midnight City", result.metadata.title)
        assertEquals("M83", result.metadata.artist)
        assertEquals("Hurry Up", result.metadata.album)
        assertEquals(2011, result.metadata.year)
    }

    private fun writeAscii(bytes: ByteArray, start: Int, value: String, width: Int) {
        val encoded = value.encodeToByteArray()
        encoded.copyInto(bytes, start, 0, minOf(encoded.size, width))
    }
}
