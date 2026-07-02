package com.eterocell.rhythhaus

import kotlinx.cinterop.ExperimentalForeignApi
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the iOS Now Playing info dictionary construction.
 *
 * Verifies that [buildIOSNowPlayingDictionary] always provides either a valid
 * `MPMediaItemPropertyPlaybackDuration` or `MPNowPlayingInfoPropertyIsLiveStream = true`,
 * so iOS never renders a greyed-out slider due to a missing duration key.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSNowPlayingDiagnosticTest {

    @Test
    fun diagnostic_durationNullSetsIsLiveStream() {
        // When durationMillis is null, the dictionary MUST set IsLiveStream = true
        // instead of omitting the duration key entirely. This prevents iOS from
        // greying out the progress slider.
        val track = PlayableTrack(
            id = "null-dur",
            title = "No Duration",
            artist = "Test",
            album = null,
            durationMillis = null,
            source = AudioSource.FilePath("/nonexistent"),
        )

        val dict = buildIOSNowPlayingDictionary(
            track = track,
            positionMillis = 0L,
            durationMillis = null,
            playbackRate = 0.0,
            existingArtwork = null,
        )

        println("[NP-DBG-TEST] dict with null duration keys: ${dict.keys.joinToString()}")
        assertNull(dict[MPMediaItemPropertyPlaybackDuration], "Duration key must be ABSENT when durationMillis is null")
        val isLive = dict[MPNowPlayingInfoPropertyIsLiveStream]
        assertNotNull(isLive, "IsLiveStream key MUST be present when duration is null")
        assertEquals(true, isLive, "IsLiveStream must be true when duration is null")
        assertNotNull(dict[MPMediaItemPropertyTitle], "Title should still be present")
        assertNotNull(dict[MPNowPlayingInfoPropertyPlaybackRate], "Rate should still be present")
    }

    @Test
    fun diagnostic_durationPresentSetsPlaybackDurationAndNotLiveStream() {
        // When durationMillis is non-null, the dictionary MUST set PlaybackDuration
        // AND IsLiveStream = false so the slider renders normally.
        val track = PlayableTrack(
            id = "with-dur",
            title = "With Duration",
            artist = "Test",
            album = null,
            durationMillis = 181_000L,
            source = AudioSource.FilePath("/nonexistent"),
        )

        val dict = buildIOSNowPlayingDictionary(
            track = track,
            positionMillis = 5_000L,
            durationMillis = 181_000L,
            playbackRate = 1.0,
            existingArtwork = null,
        )

        println("[NP-DBG-TEST] dict with 181000ms duration keys: ${dict.keys.joinToString()}")
        val dur = dict[MPMediaItemPropertyPlaybackDuration]
        assertNotNull(dur, "Duration key must be present when durationMillis is non-null")
        val durDouble = (dur as Number).toDouble()
        assertEquals(181.0, durDouble, "Duration should be 181.0 seconds")
        val isLive = dict[MPNowPlayingInfoPropertyIsLiveStream]
        assertNotNull(isLive, "IsLiveStream key should be present (set to false)")
        assertEquals(false, isLive, "IsLiveStream must be false when duration is known")
    }
}
