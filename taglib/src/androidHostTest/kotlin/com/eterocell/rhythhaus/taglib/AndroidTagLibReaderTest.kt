package com.eterocell.rhythhaus.taglib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidTagLibReaderTest {
    @Test
    fun nativeDescriptorResultCarriesRawReplayGainAlongsideExistingMetadata() {
        val result =
            NativeTagLibReadResult(
                    status = 0,
                    errorMessage = null,
                    title = "Descriptor Title",
                    artist = "Descriptor Artist",
                    album = "Descriptor Album",
                    albumArtist = null,
                    genre = null,
                    comment = null,
                    year = 2026,
                    track = 3,
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
                    replayGainAlbumGain = "NaN dB",
                    replayGainAlbumPeak = "1.1",
                    lyrics = "[00:01.00]First line",
                )
                .toTagReadResult()

        val metadata = assertIs<TagReadResult.Found>(result).metadata
        assertEquals("Descriptor Title", metadata.title)
        assertEquals("Descriptor Artist", metadata.artist)
        assertEquals(123_000L, metadata.durationMillis)
        assertEquals(3, metadata.trackNumber)
        assertEquals("-6 dB", metadata.replayGainTrackGain)
        assertEquals("2", metadata.replayGainTrackPeak)
        assertEquals("NaN dB", metadata.replayGainAlbumGain)
        assertEquals("1.1", metadata.replayGainAlbumPeak)
        assertEquals("[00:01.00]First line", metadata.lyrics)
    }
}
