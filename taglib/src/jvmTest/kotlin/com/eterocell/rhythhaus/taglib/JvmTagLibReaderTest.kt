package com.eterocell.rhythhaus.taglib

import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
        assertNull(found.metadata.replayGainTrackGain)
        assertNull(found.metadata.replayGainTrackPeak)
        assertNull(found.metadata.replayGainAlbumGain)
        assertNull(found.metadata.replayGainAlbumPeak)
    }

    @Test
    fun nativeReaderExtractsAllReplayGainTagsWithoutChangingMedia() {
        val fixture =
            createWavInfoFixture(
                "replaygain_track_gain" to "-6.00 dB",
                "REPLAYGAIN_TRACK_PEAK" to "1.2",
                "REPLAYGAIN_ALBUM_GAIN" to "-4.00 dB",
                "REPLAYGAIN_ALBUM_PEAK" to "1.1",
            )
        try {
            val before = fixture.sha256()

            val found =
                assertIs<TagReadResult.Found>(
                    createTagLibReader().readPath(fixture.absolutePath))

            assertEquals("-6.00 dB", found.metadata.replayGainTrackGain)
            assertEquals("1.2", found.metadata.replayGainTrackPeak)
            assertEquals("-4.00 dB", found.metadata.replayGainAlbumGain)
            assertEquals("1.1", found.metadata.replayGainAlbumPeak)
            assertEquals("Native Fixture Title", found.metadata.title)
            assertEquals("Native Fixture Artist", found.metadata.artist)
            assertEquals(1_000L, found.metadata.durationMillis)
            assertContentEquals(before, fixture.sha256())
        } finally {
            fixture.delete()
        }
    }

    @Test
    fun malformedReplayGainRemainsRawAndDoesNotInvalidateNativeMetadata() {
        val fixture =
            createWavInfoFixture(
                "REPLAYGAIN_TRACK_GAIN" to "-6 dB",
                "REPLAYGAIN_TRACK_PEAK" to "2",
                "REPLAYGAIN_ALBUM_GAIN" to "NaN dB",
                "REPLAYGAIN_ALBUM_PEAK" to "junk",
            )
        try {
            val before = fixture.sha256()

            val found =
                assertIs<TagReadResult.Found>(
                    createTagLibReader().readPath(fixture.absolutePath))

            assertEquals("-6 dB", found.metadata.replayGainTrackGain)
            assertEquals("2", found.metadata.replayGainTrackPeak)
            assertEquals("NaN dB", found.metadata.replayGainAlbumGain)
            assertEquals("junk", found.metadata.replayGainAlbumPeak)
            assertEquals("Native Fixture Title", found.metadata.title)
            assertEquals(8_000, found.metadata.sampleRate)
            assertContentEquals(before, fixture.sha256())
        } finally {
            fixture.delete()
        }
    }

    @Test
    fun nativeResultMapsFoundMetadata() {
        val result =
            NativeTagLibReadResult(
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
                    artworkMimeType = null,
                    artworkBytes = null,
                    replayGainTrackGain = "-6 dB",
                    replayGainTrackPeak = "2",
                    replayGainAlbumGain = "-4 dB",
                    replayGainAlbumPeak = "1.1",
                )
                .toTagReadResult()

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
        assertEquals("-6 dB", found.metadata.replayGainTrackGain)
        assertEquals("2", found.metadata.replayGainTrackPeak)
        assertEquals("-4 dB", found.metadata.replayGainAlbumGain)
        assertEquals("1.1", found.metadata.replayGainAlbumPeak)
    }

    @Test
    fun nativeResultMapsNonPositiveNumbersToNull() {
        val result =
            NativeTagLibReadResult(
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
                    artworkMimeType = null,
                    artworkBytes = null,
                )
                .toTagReadResult()

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
        val result =
            createTagLibReader()
                .readPath("/definitely/not/a/real/audio/file.mp3")

        assertTrue(
            result is TagReadResult.Unsupported ||
                result is TagReadResult.Failed,
            "Expected unsupported or failed native result for nonexistent path, got $result",
        )
    }

    private fun createWavInfoFixture(
        vararg replayGainTags: Pair<String, String>
    ): File {
        val sampleRate = 8_000
        val channels = 1
        val bitsPerSample = 8
        val durationSeconds = 1
        val dataSize =
            sampleRate * channels * (bitsPerSample / 8) * durationSeconds
        val infoChunk =
            listInfoChunk(
                "INAM" to "Native Fixture Title",
                "IART" to "Native Fixture Artist",
                "IPRD" to "Native Fixture Album",
                "IGNR" to "Test Genre",
            )
        val replayGainChunk = id3ReplayGainChunk(replayGainTags)
        val formatChunkSize = 16
        val riffSize =
            4 +
                (8 + formatChunkSize) +
                (8 + dataSize) +
                infoChunk.size +
                replayGainChunk.size
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
        wav.write(replayGainChunk)

        return File.createTempFile("rhythhaus-taglib-info-fixture", ".wav")
            .apply {
                writeBytes(wav.toByteArray())
                deleteOnExit()
            }
    }

    private fun id3ReplayGainChunk(
        entries: Array<out Pair<String, String>>
    ): ByteArray {
        if (entries.isEmpty()) return byteArrayOf()
        val frames = ByteArrayOutputStream()
        entries.forEach { (key, value) ->
            val text =
                byteArrayOf(0) +
                    key.encodeToByteArray() +
                    byteArrayOf(0) +
                    value.encodeToByteArray() +
                    byteArrayOf(0)
            frames.writeAscii("TXXX")
            frames.writeIntBe(text.size)
            frames.write(byteArrayOf(0, 0))
            frames.write(text)
        }
        val frameBytes = frames.toByteArray()
        val tag =
            ByteArrayOutputStream()
                .apply {
                    writeAscii("ID3")
                    write(byteArrayOf(3, 0, 0)) // ID3v2.3, no flags
                    write((frameBytes.size ushr 21) and 0x7f)
                    write((frameBytes.size ushr 14) and 0x7f)
                    write((frameBytes.size ushr 7) and 0x7f)
                    write(frameBytes.size and 0x7f)
                    write(frameBytes)
                }
                .toByteArray()
        return ByteArrayOutputStream()
            .apply {
                writeAscii("ID3 ")
                writeIntLe(tag.size)
                write(tag)
                if (tag.size % 2 != 0) write(0)
            }
            .toByteArray()
    }

    private fun File.sha256(): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(readBytes())

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
        return ByteArrayOutputStream()
            .apply {
                writeAscii("LIST")
                writeIntLe(payloadBytes.size)
                write(payloadBytes)
            }
            .toByteArray()
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

    private fun ByteArrayOutputStream.writeIntBe(value: Int) {
        write((value ushr 24) and 0xff)
        write((value ushr 16) and 0xff)
        write((value ushr 8) and 0xff)
        write(value and 0xff)
    }
}
