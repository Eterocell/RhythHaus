package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
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
        listOf(
            "x:a" to "non-decimal length",
            "1a" to "missing colon",
            "1:\uD800" to "unpaired surrogate",
            "10001:" to "over-count",
            "${PlaybackSessionCodec.maxIdCharacters + 1}:${"a".repeat(PlaybackSessionCodec.maxIdCharacters + 1)}" to "over-character",
            "8192:${"🎧".repeat(8192)}" to "over-UTF-8",
            "${PlaybackSessionCodec.maxEncodedUtf8Bytes}:" to "over-total-size",
            "2:ab!" to "trailing data",
            "2:a" to "truncated value",
        ).forEach { (encoded, reason) ->
            assertNull(PlaybackSessionCodec.decodeIds(encoded), reason)
        }
    }

    @Test
    fun codecEnforcesBoundsAndAllOrNullDecoding() {
        val exactMaxIds = List(PlaybackSessionCodec.maxIds) { it.toString() }
        assertEquals(exactMaxIds, PlaybackSessionCodec.decodeIds(PlaybackSessionCodec.encodeIds(exactMaxIds)))
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeIds(exactMaxIds + "too-many") }

        val exactMaxCharacters = "x".repeat(PlaybackSessionCodec.maxIdCharacters)
        assertEquals(listOf(exactMaxCharacters), PlaybackSessionCodec.decodeIds(PlaybackSessionCodec.encodeIds(listOf(exactMaxCharacters))))
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeIds(listOf(exactMaxCharacters + "x"))
        }

        val exactMaxEncodedBytes = exactEncodedSizeIds()
        assertEquals(
            PlaybackSessionCodec.maxEncodedUtf8Bytes,
            PlaybackSessionCodec.encodeIds(exactMaxEncodedBytes).encodeToByteArray().size,
        )
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeIds(exactMaxEncodedBytes + "overflow")
        }

        val largestUtf8IdWithinCharacterBound = "🎧".repeat(PlaybackSessionCodec.maxIdCharacters / 2)
        assertNotNull(PlaybackSessionCodec.encodeIds(listOf(largestUtf8IdWithinCharacterBound)))
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeIds(listOf("🎧".repeat(PlaybackSessionCodec.maxIdCharacters + 1)))
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

    private fun exactEncodedSizeIds(): List<String> {
        val base = List(255) { index ->
            index.toString() + "x".repeat(4_091 - index.toString().length)
        }
        val remaining = PlaybackSessionCodec.maxEncodedUtf8Bytes - PlaybackSessionCodec.encodeIds(base).encodeToByteArray().size
        var lastLength = remaining - 2
        while (lastLength.toString().length + 1 + lastLength != remaining) {
            lastLength--
        }
        return base + "z".repeat(lastLength)
    }
}
