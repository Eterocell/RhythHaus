package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlaybackSessionSnapshotTest {
    @Test
    fun codecRoundTripsDelimitersAndEmoji() {
        val ids = listOf("a:b", "line\n2", "🎧")

        assertEquals(ids, PlaybackSessionCodec.decodeIds(PlaybackSessionCodec.encodeIds(ids)))
    }

    @Test
    fun codecRejectsInvalidForms() {
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("")) }
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("same", "same")) }
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(listOf("\uD800")) }
        assertNull(PlaybackSessionCodec.decodeIds("2:ab!"))
        assertNull(PlaybackSessionCodec.decodeIds("2:a"))
    }

    @Test
    fun codecEnforcesBoundsAndAllOrNullDecoding() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeIds(List(PlaybackSessionCodec.maxIds + 1) { it.toString() })
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeIds(listOf("x".repeat(PlaybackSessionCodec.maxIdCharacters + 1)))
        }
        assertNull(PlaybackSessionCodec.decodeIds("1:a1:"))
        assertNull(PlaybackSessionCodec.decodeIds("0:"))
        assertNull(PlaybackSessionCodec.decodeIds("1:a1:a"))
    }

    @Test
    fun checkpointModelsExposeCompleteSnapshotsAndProgressKey() {
        val snapshot = PlaybackSessionSnapshot(
            queueIds = listOf("one"),
            currentTrackId = "one",
            positionMillis = 1_000L,
            repeatMode = RepeatMode.RepeatPlaylist,
            shuffleMode = ShuffleMode.On,
        )
        val key = ProgressCheckpointKey(generation = 4L, currentTrackId = "one", secondBucket = 1L)

        assertEquals(snapshot, PlaybackCheckpoint.Immediate(snapshot).snapshot)
        assertEquals(key, PlaybackCheckpoint.PlayingProgress(key, snapshot).key)
        assertEquals(snapshot, PlaybackCheckpoint.PlayingProgress(key, snapshot).snapshot)
    }
}
