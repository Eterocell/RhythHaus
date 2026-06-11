package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Id3v23ParserTest {
    @Test
    fun readsId3v23TextFrames() {
        val frames = buildFrames(
            frame("TIT2", textPayload("Breathe")),
            frame("TPE1", textPayload("The Prodigy")),
            frame("TALB", textPayload("The Fat of the Land")),
            frame("TCON", textPayload("Electronic")),
            frame("TRCK", textPayload("3/10")),
            frame("TPOS", textPayload("1/2")),
            frame("TYER", textPayload("1997")),
        )
        val bytes = id3v2(version = 3, frames = frames)

        val result = RhythHausTagLib.read(bytes)

        assertTrue(result is TagReadResult.Found)
        assertEquals(TagFormat.ID3V23, result.format)
        assertEquals("Breathe", result.metadata.title)
        assertEquals("The Prodigy", result.metadata.artist)
        assertEquals("The Fat of the Land", result.metadata.album)
        assertEquals("Electronic", result.metadata.genre)
        assertEquals(3, result.metadata.trackNumber)
        assertEquals(10, result.metadata.trackTotal)
        assertEquals(1, result.metadata.discNumber)
        assertEquals(2, result.metadata.discTotal)
        assertEquals(1997, result.metadata.year)
    }

    private fun textPayload(value: String): ByteArray = byteArrayOf(0) + value.encodeToByteArray()

    private fun frame(id: String, payload: ByteArray): ByteArray =
        id.encodeToByteArray() + intBytes(payload.size) + byteArrayOf(0, 0) + payload

    private fun buildFrames(vararg frames: ByteArray): ByteArray = frames.fold(ByteArray(0)) { acc, next -> acc + next }

    private fun id3v2(version: Int, frames: ByteArray): ByteArray =
        byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) + syncSafe(frames.size) + frames

    private fun intBytes(value: Int): ByteArray = byteArrayOf(
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )
}
