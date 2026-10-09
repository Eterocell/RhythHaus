package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayGainTest {
    private val track =
        PlayableTrack(
            "tagged",
            "Title",
            "Artist",
            "Album",
            1000L,
            AudioSource.FilePath("/track.wav"))

    @Test
    fun serializedModesRestoreOnlyKnownLowercaseValues() {
        assertEquals("off", ReplayGainMode.Off.serialized)
        assertEquals("track", ReplayGainMode.Track.serialized)
        assertEquals("album", ReplayGainMode.Album.serialized)
        assertEquals(
            ReplayGainMode.Track, ReplayGainMode.fromSerialized("track"))
        assertEquals(
            ReplayGainMode.Album, ReplayGainMode.fromSerialized("album"))
        for (value in listOf(null, "", "TRACK", "unknown", "off")) {
            assertEquals(
                ReplayGainMode.Off, ReplayGainMode.fromSerialized(value))
        }
        assertEquals(1f, replayGainFactor(null, ReplayGainMode.Track))
    }

    @Test
    fun trackModeAttenuatesAndPeakLimitsWithoutAmplifying() {
        assertEquals(
            0.5011872f,
            replayGainFactor(
                track.copy(replayGainTrackGainDb = -6.0), ReplayGainMode.Track),
            0.000001f)
        assertEquals(
            0.5f,
            replayGainFactor(
                track.copy(
                    replayGainTrackGainDb = 6.0, replayGainTrackPeak = 2.0),
                ReplayGainMode.Track))
        assertEquals(
            1f,
            replayGainFactor(
                track.copy(replayGainTrackPeak = 2.0), ReplayGainMode.Track))
        assertEquals(
            1f,
            replayGainFactor(
                track.copy(replayGainTrackGainDb = -6.0), ReplayGainMode.Off))
    }

    @Test
    fun albumFallbackKeepsGainAndPeakFromTheSameScope() {
        val tagged =
            track.copy(replayGainTrackGainDb = -6.0, replayGainTrackPeak = 4.0)
        assertEquals(0.25f, replayGainFactor(tagged, ReplayGainMode.Album))
        assertEquals(
            1f,
            replayGainFactor(
                tagged.copy(replayGainAlbumGainDb = 0.0), ReplayGainMode.Album))
        assertEquals(
            0.25f,
            replayGainFactor(
                tagged.copy(replayGainAlbumGainDb = Double.NaN),
                ReplayGainMode.Album))
        assertEquals(
            0.25f,
            replayGainFactor(
                tagged.copy(replayGainAlbumPeak = 8.0), ReplayGainMode.Album))
        assertEquals(
            0.5f,
            replayGainFactor(
                tagged.copy(
                    replayGainAlbumGainDb = 6.0, replayGainAlbumPeak = 2.0),
                ReplayGainMode.Album))
    }

    @Test
    fun invalidGainLeavesUnityAndInvalidPeakDoesNotEraseValidGain() {
        for (db in
            listOf(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY)) {
            assertEquals(
                1f,
                replayGainFactor(
                    track.copy(
                        replayGainTrackGainDb = db, replayGainTrackPeak = 4.0),
                    ReplayGainMode.Track))
        }
        for (peak in
            listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(
                0.5011872f,
                replayGainFactor(
                    track.copy(
                        replayGainTrackGainDb = -6.0,
                        replayGainTrackPeak = peak),
                    ReplayGainMode.Track),
                0.000001f)
        }
        assertEquals(
            0f,
            replayGainFactor(
                track.copy(replayGainTrackGainDb = -Double.MAX_VALUE),
                ReplayGainMode.Track))
        assertEquals(
            1f,
            replayGainFactor(
                track.copy(replayGainTrackGainDb = Double.MAX_VALUE),
                ReplayGainMode.Track))
        assertEquals(
            0f,
            replayGainFactor(
                track.copy(
                    replayGainTrackGainDb = 0.0,
                    replayGainTrackPeak = Double.MAX_VALUE),
                ReplayGainMode.Track))
    }

    @Test
    fun numericExtremesNeverProduceInvalidNativeGain() {
        for (db in
            listOf(
                -Double.MAX_VALUE,
                Double.MAX_VALUE,
                Double.NaN,
                Double.POSITIVE_INFINITY)) {
            for (peak in
                listOf(
                    Double.MIN_VALUE,
                    Double.MAX_VALUE,
                    Double.NaN,
                    0.0,
                    -1.0)) {
                val gain =
                    replayGainFactor(
                        track.copy(
                            replayGainTrackGainDb = db,
                            replayGainTrackPeak = peak),
                        ReplayGainMode.Track)
                assertTrue(gain.isFinite() && gain in 0f..1f)
            }
        }
    }
}
