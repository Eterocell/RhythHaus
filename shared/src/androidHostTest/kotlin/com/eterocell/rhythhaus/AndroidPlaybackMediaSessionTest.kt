package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidPlaybackMediaSessionTest {

    @Test
    fun mediaItemCarriesTrackMetadataForAndroidSystemControls() {
        val track = PlayableTrack(
            id = "track-1",
            title = "Night Drive",
            artist = "Rhyth Haus",
            album = "Local Sessions",
            durationMillis = 181_000L,
            source = AudioSource.Uri("content://media/external/audio/media/42"),
        )

        val mediaMetadata = buildAndroidPlaybackMediaMetadata(track)

        assertEquals("Night Drive", mediaMetadata.title.toString())
        assertEquals("Rhyth Haus", mediaMetadata.artist.toString())
        assertEquals("Local Sessions", mediaMetadata.albumTitle.toString())
    }
}
