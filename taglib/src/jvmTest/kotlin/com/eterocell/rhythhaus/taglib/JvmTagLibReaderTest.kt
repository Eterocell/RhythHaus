package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmTagLibReaderTest {
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
            year = 2026,
            track = 7,
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
            year = 0,
            track = 0,
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
}
