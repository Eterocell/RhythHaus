package com.eterocell.rhythhaus

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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

    @Test
    fun iosPlaybackEngineWorkDoesNotRunOnMainDispatcher() {
        assertNotEquals(
            Dispatchers.Main,
            playbackEngineDispatcher,
            "iOS playback load/configuration work should avoid Main because AVAudioSession.setCategory, setActive, and AVAudioPlayer.prepareToPlay can block the UI thread.",
        )
    }

    @Test
    fun iosTrackSwitchUsesSoftPlayerTeardown() {
        assertEquals(IOSTrackSwitchTeardown.SoftFade, iosTrackSwitchTeardown)
        assertEquals(0.05, IOS_TRACK_SWITCH_FADE_SECONDS)
        assertEquals(0.0f, IOS_TRACK_SWITCH_SILENT_VOLUME)
    }
}
