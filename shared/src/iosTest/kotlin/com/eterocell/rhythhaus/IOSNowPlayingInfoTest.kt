package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class IOSNowPlayingInfoTest {

    @Test
    fun nowPlayingInfoCarriesTrackMetadataForIOSControlCenter() {
        val track = PlayableTrack(
            id = "track-1",
            title = "Night Drive",
            artist = "Rhyth Haus",
            album = "Local Sessions",
            durationMillis = 181_000L,
            source = AudioSource.FilePath("/tmp/night-drive.wav"),
        )

        val nowPlayingInfo = buildIOSNowPlayingInfo(track, positionMillis = 42_000L, durationMillis = 181_000L)

        assertEquals("Night Drive", nowPlayingInfo["title"])
        assertEquals("Rhyth Haus", nowPlayingInfo["artist"])
        assertEquals("Local Sessions", nowPlayingInfo["albumTitle"])
        assertEquals(181.0, nowPlayingInfo["durationSeconds"])
        assertEquals(42.0, nowPlayingInfo["elapsedSeconds"])
    }
}
