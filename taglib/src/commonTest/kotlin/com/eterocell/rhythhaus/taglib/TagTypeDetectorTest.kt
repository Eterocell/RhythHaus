package com.eterocell.rhythhaus.taglib

import com.eterocell.rhythhaus.taglib.internal.TagTypeDetector
import kotlin.test.Test
import kotlin.test.assertEquals

class TagTypeDetectorTest {
    @Test
    fun detectsId3v23FromHeader() {
        val bytes = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0, 0, 0, 0, 0)

        assertEquals(TagFormat.ID3V23, TagTypeDetector.detect(bytes))
    }

    @Test
    fun detectsId3v24FromHeader() {
        val bytes = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 0)

        assertEquals(TagFormat.ID3V24, TagTypeDetector.detect(bytes))
    }

    @Test
    fun detectsId3v1FromTail() {
        val bytes = ByteArray(256)
        bytes[128] = 'T'.code.toByte()
        bytes[129] = 'A'.code.toByte()
        bytes[130] = 'G'.code.toByte()

        assertEquals(TagFormat.ID3V1, TagTypeDetector.detect(bytes))
    }

    @Test
    fun returnsNullForUnsupportedBytes() {
        assertEquals(null, TagTypeDetector.detect(byteArrayOf(1, 2, 3, 4)))
    }
}
