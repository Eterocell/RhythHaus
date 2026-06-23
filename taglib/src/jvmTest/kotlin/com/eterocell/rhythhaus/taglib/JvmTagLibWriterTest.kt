package com.eterocell.rhythhaus.taglib

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmTagLibWriterTest {
    @Test
    fun writerSetsBasicTagsOnWavFixture() {
        val fixture = createEmptyWavFixture()
        val path = fixture.absolutePath

        val writer = createTagLibWriter()
        val result = writer.writePath(
            path,
            WriteMeta(
                title = "Written Title",
                artist = "Written Artist",
                album = "Written Album",
                genre = "Electronic",
                comment = "Test comment",
                year = 2025,
                trackNumber = 5,
            ),
        )

        assertTrue(result is WriteResult.Success, "Expected Success, got $result")

        // Verify by reading back
        val reader = createTagLibReader()
        val readResult = reader.readPath(path)
        val found = readResult as? TagReadResult.Found
            ?: throw AssertionError("Expected TagReadResult.Found, got $readResult")

        assertEquals("Written Title", found.metadata.title)
        assertEquals("Written Artist", found.metadata.artist)
        assertEquals("Written Album", found.metadata.album)
        assertEquals("Electronic", found.metadata.genre)
        assertEquals("Test comment", found.metadata.comment)
        assertEquals(2025, found.metadata.year)
        assertEquals(5, found.metadata.trackNumber)
    }

    @Test
    fun writerReportsFailureForNonexistentPath() {
        val result = createTagLibWriter().writePath(
            "/nonexistent/path/audio.mp3",
            WriteMeta(title = "nope"),
        )
        assertTrue(result is WriteResult.Unsupported || result is WriteResult.Failed)
    }

    @Test
    fun writerSetsProperties() {
        val fixture = createEmptyWavFixture()
        val path = fixture.absolutePath

        val writer = createTagLibWriter()
        val result = writer.writePath(
            path,
            WriteMeta(
                title = "Props Test",
                properties = mapOf(
                    "COMPOSER" to "Test Composer",
                    "COPYRIGHT" to "2025 Test Label",
                    "BPM" to "128",
                ),
            ),
        )

        assertTrue(result is WriteResult.Success, "Expected Success, got $result")

        // Verify properties
        val reader = createTagLibReader()
        val props = reader.readProperties(path)
        assertTrue(props.isNotEmpty(), "Expected properties to be non-empty")
        assertEquals("Test Composer", props["COMPOSER"])
        assertEquals("2025 Test Label", props["COPYRIGHT"])
    }

    private fun createEmptyWavFixture(): File {
        val sampleRate = 8000
        val channels = 1
        val bitsPerSample = 8
        val durationSeconds = 1
        val dataSize = sampleRate * channels * (bitsPerSample / 8) * durationSeconds
        val formatChunkSize = 16
        val riffSize = 4 + (8 + formatChunkSize) + (8 + dataSize)
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

        return File.createTempFile("rhythhaus-taglib-write-fixture", ".wav").apply {
            writeBytes(wav.toByteArray())
            deleteOnExit()
        }
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
