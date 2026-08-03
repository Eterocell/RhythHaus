package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

class IOSNowPlayingInfoTest {

    @Test
    fun nowPlayingInfoCarriesTrackMetadataForIOSControlCenter() {
        val track =
            PlayableTrack(
                id = "track-1",
                title = "Night Drive",
                artist = "Rhyth Haus",
                album = "Local Sessions",
                durationMillis = 181_000L,
                source = AudioSource.FilePath("/tmp/night-drive.wav"),
            )

        val nowPlayingInfo =
            buildIOSNowPlayingInfo(
                track, positionMillis = 42_000L, durationMillis = 181_000L)

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

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        configureIOSRemoteCommandAvailability(commandCenter)

        // Track-level transport controls must be enabled — these are the ones
        // this app can
        // actually service (play/pause/stop/scrub/previous track/next track).
        assertTrue(commandCenter.playCommand.enabled)
        assertTrue(commandCenter.pauseCommand.enabled)
        assertTrue(commandCenter.togglePlayPauseCommand.enabled)
        assertTrue(commandCenter.stopCommand.enabled)
        assertTrue(commandCenter.changePlaybackPositionCommand.enabled)
        assertTrue(commandCenter.previousTrackCommand.enabled)
        assertTrue(commandCenter.nextTrackCommand.enabled)

        // Skip-interval controls must be disabled — left enabled without a
        // handler, iOS renders
        // them (greyed out) on the lock screen INSTEAD OF previous/next track.
        assertFalse(commandCenter.skipForwardCommand.enabled)
        assertFalse(commandCenter.skipBackwardCommand.enabled)
        assertFalse(commandCenter.seekForwardCommand.enabled)
        assertFalse(commandCenter.seekBackwardCommand.enabled)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun nowPlayingDictionaryPreservesArtworkAndExplicitPausedRate() {
        val track =
            PlayableTrack(
                id = "track-artwork",
                title = "Artwork Track",
                artist = "Rhyth Haus",
                album = "Control Center",
                durationMillis = 120_000L,
                source = AudioSource.FilePath("/tmp/artwork-track.wav"),
            )
        val artworkSentinel = Any()

        val nowPlayingInfo =
            buildIOSNowPlayingDictionary(
                track = track,
                positionMillis = 5_000L,
                durationMillis = 120_000L,
                playbackRate = 0.0,
                existingArtwork = artworkSentinel,
            )

        assertEquals(
            artworkSentinel, nowPlayingInfo[MPMediaItemPropertyArtwork])
        assertEquals(0.0, nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate])
    }

    @Test
    fun iosRemoteTransportGateRejectsDisabledPlayAndSeekWithoutProviderAction() {
        val actions = mutableListOf<String>()
        val gate = IOSRemoteTransportGate()
        gate.setEnabled(false)

        assertEquals(
            MPRemoteCommandHandlerStatusCommandFailed,
            gate.play { actions += "play" })
        assertEquals(
            MPRemoteCommandHandlerStatusCommandFailed,
            gate.seek(2_000L) { actions += "seek:$it" })
        assertEquals(emptyList(), actions)

        gate.setEnabled(true)
        assertEquals(
            MPRemoteCommandHandlerStatusSuccess,
            gate.play { actions += "play" })
        assertEquals(
            MPRemoteCommandHandlerStatusSuccess,
            gate.seek(2_000L) { actions += "seek:$it" })
        assertEquals(listOf("play", "seek:2000"), actions)
    }
}
