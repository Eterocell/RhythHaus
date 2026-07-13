package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun androidOldTokenCannotAcknowledgeSameTrackNewGeneration() {
        val tracker = Media3RequestTokenTracker()
        val first = tracker.begin(10L)
        val second = tracker.begin(11L)

        assertFalse(tracker.accepts(first, observedCurrentToken = first))
        assertTrue(tracker.accepts(second, observedCurrentToken = second))
    }

    @Test
    fun androidMediaItemCarriesUniqueGenerationRequestToken() {
        val track = PlayableTrack(
            id = "same-track",
            title = "Same Track",
            artist = "Rhyth Haus",
            album = null,
            durationMillis = 1_000L,
            source = AudioSource.Uri("content://media/external/audio/media/42"),
        )
        val tracker = Media3RequestTokenTracker()
        val first = tracker.begin(20L)
        val second = tracker.begin(21L)

        val firstMediaId = first.encode()
        val secondMediaId = second.encode()

        assertFalse(firstMediaId == secondMediaId)
        assertEquals(first, Media3RequestToken.decode(firstMediaId))
        assertEquals(second, Media3RequestToken.decode(secondMediaId))
    }
}
