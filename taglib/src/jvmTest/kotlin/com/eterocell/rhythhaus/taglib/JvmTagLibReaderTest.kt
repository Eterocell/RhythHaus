package com.eterocell.rhythhaus.taglib

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmTagLibReaderTest {
    @Test
    fun nativeReaderReturnsFoundForGeneratedWavInfoFixture() {
        val fixture = createWavInfoFixture()

        val result = createTagLibReader().readPath(fixture.absolutePath)

        val found = assertIs<TagReadResult.Found>(result)
        assertEquals("Native Fixture Title", found.metadata.title)
        assertEquals("Native Fixture Artist", found.metadata.artist)
        assertEquals("Native Fixture Album", found.metadata.album)
        assertEquals("Test Genre", found.metadata.genre)
        assertEquals(1_000L, found.metadata.durationMillis)
        assertEquals(8_000, found.metadata.sampleRate)
        assertEquals(1, found.metadata.channels)
    }

    @Test
    fun nativeResultMapsFoundMetadata() {
        val result = NativeTagLibReadResult(
            status = 0,
            errorMessage = null,
            title = "Maple Garden",
            artist = "RhythHaus",
            album = "House Sessions",
            albumArtist = "Various Artists",
            genre = "Electronic",
            comment = null,
            year = 2026,
            track = 7,
            trackTotal = 0,
            discNumber = 0,
            discTotal = 0,
            durationSeconds = 123,
            bitrate = 320,
            sampleRate = 44_100,
            channels = 2,
        ).toTagReadResult()

        val found = assertIs<TagReadResult.Found>(result)
        assertEquals("Maple Garden", found.metadata.title)
        assertEquals("RhythHaus", found.metadata.artist)
        assertEquals("House Sessions", found.metadata.album)
        assertEquals("Various Artists", found.metadata.albumArtist)
        assertEquals("Electronic", found.metadata.genre)
        assertEquals(2026, found.metadata.year)
        assertEquals(7, found.metadata.trackNumber)
        assertEquals(123_000L, found.metadata.durationMillis)
        assertEquals(320, found.metadata.bitrate)
        assertEquals(44_100, found.metadata.sampleRate)
        assertEquals(2, found.metadata.channels)
    }

    @Test
    fun nativeResultMapsNonPositiveNumbersToNull() {
        val result = NativeTagLibReadResult(
            status = 0,
            errorMessage = null,
            title = null,
            artist = null,
            album = null,
            albumArtist = null,
            genre = null,
            comment = null,
            year = 0,
            track = 0,
            trackTotal = 0,
            discNumber = 0,
            discTotal = 0,
            durationSeconds = 0,
            bitrate = 0,
            sampleRate = 0,
            channels = 0,
        ).toTagReadResult()

        val found = assertIs<TagReadResult.Found>(result)
        assertEquals(null, found.metadata.year)
        assertEquals(null, found.metadata.trackNumber)
        assertEquals(null, found.metadata.durationMillis)
        assertEquals(null, found.metadata.bitrate)
        assertEquals(null, found.metadata.sampleRate)
        assertEquals(null, found.metadata.channels)
    }

    @Test
    fun jvmReaderDoesNotParseInKotlinAndGracefullyReportsUnavailableOrUnsupportedNativeShim() {
        val result = createTagLibReader().readPath("/definitely/not/a/real/audio/file.mp3")

        assertTrue(
            result is TagReadResult.Unsupported || result is TagReadResult.Failed,
            "Expected unsupported or failed native result for nonexistent path, got $result",
        )
    }

    private fun createWavInfoFixture(): File {
        val sampleRate = 8_000
        val channels = 1
        val bitsPerSample = 8
        val durationSeconds = 1
        val dataSize = sampleRate * channels * (bitsPerSample / 8) * durationSeconds
        val infoChunk = listInfoChunk(
            "INAM" to "Native Fixture Title",
            "IART" to "Native Fixture Artist",
            "IPRD" to "Native Fixture Album",
            "IGNR" to "Test Genre",
        )
        val formatChunkSize = 16
        val riffSize = 4 + (8 + formatChunkSize) + (8 + dataSize) + infoChunk.size
        val wav = ByteArrayOutputStream()

        wav.writeAscii("RIFF")
        wav.writeIntLe(riffSize)
        wav.writeAscii("WAVE")
        wav.writeAscii("fmt ")
        wav.writeIntLe(formatChunkSize)
        wav.writeShortLe(1) // PCM
        wav.writeShortLe(channels)
        wav.writeIntLe(sampleRate)
        wav.writeIntLe(sampleRate * channels * (bitsPerSample / 8))
        wav.writeShortLe(channels * (bitsPerSample / 8))
        wav.writeShortLe(bitsPerSample)
        wav.writeAscii("data")
        wav.writeIntLe(dataSize)
        wav.write(ByteArray(dataSize) { 0x80.toByte() })
        wav.write(infoChunk)

        return File.createTempFile("rhythhaus-taglib-info-fixture", ".wav").apply {
            writeBytes(wav.toByteArray())
            deleteOnExit()
        }
    }

    private fun listInfoChunk(vararg entries: Pair<String, String>): ByteArray {
        val payload = ByteArrayOutputStream()
        payload.writeAscii("INFO")
        entries.forEach { (id, value) ->
            val text = value.encodeToByteArray() + byteArrayOf(0)
            payload.writeAscii(id)
            payload.writeIntLe(text.size)
            payload.write(text)
            if (text.size % 2 != 0) {
                payload.write(0)
            }
        }

        val payloadBytes = payload.toByteArray()
        return ByteArrayOutputStream().apply {
            writeAscii("LIST")
            writeIntLe(payloadBytes.size)
            write(payloadBytes)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        write(value.encodeToByteArray())
    }

    private fun ByteArrayOutputStream.writeShortLe(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
    }

    private fun ByteArrayOutputStream.writeIntLe(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
        write((value ushr 16) and 0xff)
        write((value ushr 24) and 0xff)
    }
}
