package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Id3v24ParserTest {
    @Test
    fun readsId3v24DateFrame() {
        val frames = frame("TIT2", textPayload("Idioteque")) + frame("TDRC", textPayload("2000-10-02"))
        val bytes = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0) + syncSafe(frames.size) + frames

        val result = RhythHausTagLib.read(bytes)

        assertTrue(result is TagReadResult.Found)
        assertEquals(TagFormat.ID3V24, result.format)
        assertEquals("Idioteque", result.metadata.title)
        assertEquals(2000, result.metadata.year)
    }

    private fun textPayload(value: String): ByteArray = byteArrayOf(0) + value.encodeToByteArray()

    private fun frame(id: String, payload: ByteArray): ByteArray =
        id.encodeToByteArray() + syncSafe(payload.size) + byteArrayOf(0, 0) + payload

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )
}
