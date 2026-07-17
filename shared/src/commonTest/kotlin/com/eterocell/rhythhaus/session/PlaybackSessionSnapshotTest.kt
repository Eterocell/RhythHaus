package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlaybackSessionSnapshotTest {
    @Test
    fun sessionRoundTripKeepsDuplicateTrackOccurrencesInOrder() {
        val snapshot = PlaybackSessionSnapshot(
            queue = listOf(
                SessionQueueEntry("entry-1", "track-a"),
                SessionQueueEntry("entry-2", "track-a"),
            ),
            currentOccurrenceId = "entry-2",
        )

        assertEquals(snapshot, PlaybackSessionCodec.decodeSnapshot(PlaybackSessionCodec.encodeSnapshot(snapshot)))
    }

    @Test
    fun sessionCodecRejectsDuplicateOccurrenceIdsButAllowsDuplicateTrackIds() {
        val duplicateTracks = listOf(
            SessionQueueEntry("entry-1", "track-a"),
            SessionQueueEntry("entry-2", "track-a"),
        )
        val duplicateOccurrences = listOf(
            SessionQueueEntry("entry-1", "track-a"),
            SessionQueueEntry("entry-1", "track-b"),
        )

        assertEquals(duplicateTracks, PlaybackSessionCodec.decodeQueue(PlaybackSessionCodec.encodeQueue(duplicateTracks)))
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeQueue(duplicateOccurrences) }
    }

    @Test
    fun snapshotCodecRejectsDuplicateOccurrencesAndCurrentOutsideQueue() {
        val duplicateOccurrences = PlaybackSessionSnapshot(
            queue = listOf(
                SessionQueueEntry("entry-1", "track-a"),
                SessionQueueEntry("entry-1", "track-b"),
            ),
            currentOccurrenceId = "entry-1",
        )
        val missingCurrent = PlaybackSessionSnapshot(
            queue = listOf(SessionQueueEntry("entry-1", "track-a")),
            currentOccurrenceId = "missing",
        )

        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeSnapshot(duplicateOccurrences) }
        assertFailsWith<IllegalArgumentException> { PlaybackSessionCodec.encodeSnapshot(missingCurrent) }
    }

    @Test
    fun sessionQueueCodecRetainsTheExistingMaximumQueueCount() {
        val exactMaximum = List(PlaybackSessionCodec.maxIds) { index ->
            SessionQueueEntry("o$index", "t$index")
        }

        assertEquals(exactMaximum, PlaybackSessionCodec.decodeQueue(PlaybackSessionCodec.encodeQueue(exactMaximum)))
        assertFailsWith<IllegalArgumentException> {
            PlaybackSessionCodec.encodeQueue(exactMaximum + SessionQueueEntry("overflow", "overflow"))
        }
    }

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
            overCountEncoded() to "over-count",
            "${PlaybackSessionCodec.maxIdCharacters + 1}:${"a".repeat(PlaybackSessionCodec.maxIdCharacters + 1)}" to "over-character",
            framedEncoded(List(256) { index -> "id-$index" + "x".repeat(PlaybackSessionCodec.maxIdCharacters - "id-$index".length) }) to "over-total-size",
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

        val largestReachableUtf8Id = "\u0800".repeat(PlaybackSessionCodec.maxIdCharacters)
        assertEquals(PlaybackSessionCodec.maxIdCharacters, largestReachableUtf8Id.length)
        assertEquals(12_288, largestReachableUtf8Id.encodeToByteArray().size)
        assertEquals(listOf(largestReachableUtf8Id), PlaybackSessionCodec.decodeIds(PlaybackSessionCodec.encodeIds(listOf(largestReachableUtf8Id))))
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
        val key = ProgressCheckpointKey(generation = 4L, currentOccurrenceId = snapshot.currentOccurrenceId!!, secondBucket = 1L)

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

    private fun overCountEncoded(): String = framedEncoded(List(PlaybackSessionCodec.maxIds + 1) { it.toString() })

    private fun framedEncoded(ids: List<String>): String = buildString {
        ids.forEach { id -> append(id.length).append(':').append(id) }
    }
}
