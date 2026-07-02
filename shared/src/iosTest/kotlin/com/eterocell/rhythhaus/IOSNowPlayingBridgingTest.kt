package com.eterocell.rhythhaus

import kotlinx.cinterop.ExperimentalForeignApi
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that verify Kotlin/Native → Obj-C bridging of the nowPlayingInfo dictionary.
 *
 * The `buildIOSNowPlayingDictionary` function returns a `Map<Any?, Any?>` which is
 * assigned to `MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo` (an
 * `NSDictionary`). This test verifies that after the round-trip through Obj-C,
 * the values are still readable and have the correct types.
 *
 * If `Double` values don't bridge to `NSNumber` correctly, iOS would see a
 * non-nil dictionary but couldn't read duration/rate/elapsed — causing greyed
 * controls while play/pause (separate MPRemoteCommandCenter path) still works.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSNowPlayingBridgingTest {

    @Test
    fun bridging_nowPlayingInfoDoubleValuesSurviveObjCRoundTrip() {
        val track = PlayableTrack(
            id = "bridge-test",
            title = "Bridging Test",
            artist = "Test Artist",
            album = "Test Album",
            durationMillis = 181_000L,
            source = AudioSource.FilePath("/nonexistent"),
        )

        val dict = buildIOSNowPlayingDictionary(
            track = track,
            positionMillis = 42_000L,
            durationMillis = 181_000L,
            playbackRate = 1.0,
            existingArtwork = null,
        )

        // Assign to nowPlayingInfo (triggers Kotlin Map → NSDictionary bridging)
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = dict

        // Read back from Obj-C (triggers NSDictionary → Kotlin Map bridging)
        val readBack = MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo
        assertNotNull(readBack, "nowPlayingInfo must be non-nil after assignment")

        // Check String values
        val title = readBack!!.get(MPMediaItemPropertyTitle)
        println("[BRIDGE-TEST] title=$title type=${title?.let { it::class.simpleName }}")
        assertEquals("Bridging Test", title, "Title must survive round-trip")

        // Check Double values — THIS IS THE CRITICAL TEST
        val duration = readBack.get(MPMediaItemPropertyPlaybackDuration)
        println("[BRIDGE-TEST] duration=$duration type=${duration?.let { it::class.simpleName }}")
        assertNotNull(duration, "Duration must be present after round-trip")
        assertTrue(duration is Number, "Duration must be a Number after bridging — got ${duration::class.simpleName}")
        assertEquals(181.0, (duration as Number).toDouble(), "Duration value must survive round-trip")

        val elapsed = readBack.get(MPNowPlayingInfoPropertyElapsedPlaybackTime)
        println("[BRIDGE-TEST] elapsed=$elapsed type=${elapsed?.let { it::class.simpleName }}")
        assertNotNull(elapsed, "Elapsed must be present after round-trip")
        assertTrue(elapsed is Number, "Elapsed must be a Number after bridging — got ${elapsed::class.simpleName}")
        assertEquals(42.0, (elapsed as Number).toDouble(), "Elapsed value must survive round-trip")

        val rate = readBack.get(MPNowPlayingInfoPropertyPlaybackRate)
        println("[BRIDGE-TEST] rate=$rate type=${rate?.let { it::class.simpleName }}")
        assertNotNull(rate, "Rate must be present after round-trip")
        assertTrue(rate is Number, "Rate must be a Number after bridging — got ${rate::class.simpleName}")
        assertEquals(1.0, (rate as Number).toDouble(), "Rate value must survive round-trip")

        // IsLiveStream key should NOT be present when duration is known
        val isLive = readBack.get(MPNowPlayingInfoPropertyIsLiveStream)
        println("[BRIDGE-TEST] isLiveStream=$isLive (should be null when duration is known)")
        assertNull(isLive, "IsLiveStream must be ABSENT when duration is known")
    }

    @Test
    fun bridging_nullDurationSetsIsLiveStreamAndSurvivesRoundTrip() {
        val track = PlayableTrack(
            id = "bridge-null-dur",
            title = "Null Duration Bridge",
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

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = dict
        val readBack = MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo
        assertNotNull(readBack)

        // Duration should be absent
        assertNull(readBack!!.get(MPMediaItemPropertyPlaybackDuration), "Duration should be absent when null")

        // IsLiveStream should be present
        val isLive = readBack.get(MPNowPlayingInfoPropertyIsLiveStream)
        println("[BRIDGE-TEST] null-dur isLiveStream=$isLive type=${isLive?.let { it::class.simpleName }}")
        assertNotNull(isLive, "IsLiveStream must survive round-trip when duration is null")

        // Rate should still be present (0.0)
        val rate = readBack.get(MPNowPlayingInfoPropertyPlaybackRate)
        println("[BRIDGE-TEST] null-dur rate=$rate type=${rate?.let { it::class.simpleName }}")
        assertNotNull(rate, "Rate must survive round-trip even with null duration")
        assertTrue(rate is Number, "Rate must be a Number — got ${rate::class.simpleName}")
        assertEquals(0.0, (rate as Number).toDouble(), "Rate should be 0.0")
    }
}
