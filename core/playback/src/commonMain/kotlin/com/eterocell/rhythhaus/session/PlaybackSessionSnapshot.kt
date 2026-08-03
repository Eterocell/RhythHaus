package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode

/** Persistable queue selection and transport settings. */
public data class PlaybackSessionSnapshot(
    /** Ordered persisted queue occurrences. */
    public val queue: List<SessionQueueEntry> = emptyList(),
    /** Selected queue occurrence, when any. */
    public val currentOccurrenceId: String? = null,
    /** Non-negative playback position for the selected occurrence. */
    public val positionMillis: Long = 0L,
    /** Completion behavior restored with this session. */
    public val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    /** Queue traversal behavior restored with this session. */
    public val shuffleMode: ShuffleMode = ShuffleMode.Off,
) {
    /**
     * Converts the legacy track-ID queue into distinct persisted occurrences.
     *
     * @param queueIds Legacy ordered track IDs.
     * @param currentTrackId Legacy selected track ID.
     * @param positionMillis Legacy position for the selected track.
     * @param repeatMode Legacy completion behavior.
     * @param shuffleMode Legacy queue traversal behavior.
     * @param legacyTrackIds Retained source-compatibility marker for legacy
     *   callers.
     */
    public constructor(
        queueIds: List<String>,
        currentTrackId: String?,
        positionMillis: Long = 0L,
        repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
        shuffleMode: ShuffleMode = ShuffleMode.Off,
        @Suppress("UNUSED_PARAMETER") legacyTrackIds: Boolean = true,
    ) : this(
        queue = normalizeLegacyQueue(queueIds),
        currentOccurrenceId =
            currentTrackId?.let { current ->
                queueIds
                    .indexOf(current)
                    .takeIf { it >= 0 }
                    ?.let(::legacyOccurrenceId)
            },
        positionMillis = positionMillis,
        repeatMode = repeatMode,
        shuffleMode = shuffleMode,
    )

    /** Track IDs in queue order for legacy callers. */
    public val queueIds: List<String>
        get() = queue.map { it.trackId }

    /** Selected track ID resolved from [currentOccurrenceId]. */
    public val currentTrackId: String?
        get() =
            queue
                .firstOrNull { it.occurrenceId == currentOccurrenceId }
                ?.trackId
}

/** Identifies one persisted occurrence and the track it references. */
public data class SessionQueueEntry(
    /** Stable persisted occurrence identifier. */
    public val occurrenceId: String,
    /** Identifier of the occurrence's track. */
    public val trackId: String,
)

/** Encodes and validates persisted playback-session values. */
public object PlaybackSessionCodec {
    /** Maximum supported IDs. */
    public const val maxIds: Int = 10_000
    /** Maximum ID characters. */
    public const val maxIdCharacters: Int = 4_096
    /** Maximum UTF-8 bytes per ID. */
    public const val maxIdUtf8Bytes: Int = 16_384
    /** Maximum encoded UTF-8 bytes. */
    public const val maxEncodedUtf8Bytes: Int = 1_048_576

    /** Encodes a validated session snapshot. */
    public fun encodeSnapshot(snapshot: PlaybackSessionSnapshot): String {
        require(hasValidOccurrences(snapshot.queue))
        require(
            snapshot.currentOccurrenceId == null ||
                snapshot.queue.any {
                    it.occurrenceId == snapshot.currentOccurrenceId
                },
        )
        return encodeIds(
            listOf(snapshot.currentOccurrenceId.orEmpty()) +
                snapshot.queue.flatMap { listOf(it.occurrenceId, it.trackId) },
            requireUnique = false,
            allowEmptyFirst = true,
            maxFrames = maxIds * 2 + 1,
        )
    }

    /** Decodes a valid session snapshot or returns null for invalid input. */
    public fun decodeSnapshot(encoded: String): PlaybackSessionSnapshot? {
        val values =
            decodeIds(
                encoded,
                requireUnique = false,
                allowEmptyFirst = true,
                maxFrames = maxIds * 2 + 1,
            ) ?: return null
        if (values.isEmpty() || (values.size - 1) % 2 != 0) return null
        val queue =
            values.drop(1).chunked(2).map { SessionQueueEntry(it[0], it[1]) }
        if (!hasValidOccurrences(queue)) return null
        val currentOccurrenceId = values.first().ifEmpty { null }
        if (currentOccurrenceId != null &&
            queue.none { it.occurrenceId == currentOccurrenceId })
            return null
        return PlaybackSessionSnapshot(
            queue = queue, currentOccurrenceId = currentOccurrenceId)
    }

    /** Encodes a validated persisted occurrence queue. */
    public fun encodeQueue(queue: List<SessionQueueEntry>): String {
        require(hasValidOccurrences(queue))
        return encodeIds(
            queue.flatMap { listOf(it.occurrenceId, it.trackId) },
            requireUnique = false,
            maxFrames = maxIds * 2,
        )
    }

    /** Decodes a valid persisted occurrence queue or returns null. */
    public fun decodeQueue(encoded: String): List<SessionQueueEntry>? {
        val values =
            decodeIds(encoded, requireUnique = false, maxFrames = maxIds * 2)
                ?: return null
        if (values.size % 2 != 0) return null
        return values
            .chunked(2)
            .map { SessionQueueEntry(it[0], it[1]) }
            .takeIf(::hasValidOccurrences)
    }

    /** Encodes distinct non-empty identifiers. */
    public fun encodeIds(ids: List<String>): String =
        encodeIds(ids, requireUnique = true)

    private fun encodeIds(
        ids: List<String>,
        requireUnique: Boolean,
        allowEmptyFirst: Boolean = false,
        maxFrames: Int = maxIds,
    ): String {
        require(ids.size <= maxFrames)
        val seen = HashSet<String>(ids.size)
        if (requireUnique) ids.forEach { require(seen.add(it)) }

        return buildString {
            var encodedSize = 0
            ids.forEachIndexed { index, id ->
                require(
                    (id.isNotEmpty() || allowEmptyFirst && index == 0) &&
                        id.length <= maxIdCharacters)
                require(!id.hasUnpairedSurrogate())
                val idUtf8Size = id.encodeToByteArray().size
                require(idUtf8Size <= maxIdUtf8Bytes)
                val frameSize = id.length.toString().length + 1L + idUtf8Size
                require(frameSize <= maxEncodedUtf8Bytes.toLong() - encodedSize)
                encodedSize += frameSize.toInt()
                append(id.length).append(':').append(id)
            }
        }
    }

    /** Decodes distinct non-empty identifiers or returns null. */
    public fun decodeIds(encoded: String): List<String>? =
        decodeIds(encoded, requireUnique = true)

    private fun decodeIds(
        encoded: String,
        requireUnique: Boolean,
        allowEmptyFirst: Boolean = false,
        maxFrames: Int = maxIds,
    ): List<String>? {
        if (encoded.isEmpty()) return emptyList()
        if (encoded.encodeToByteArray().size > maxEncodedUtf8Bytes) return null

        val ids = ArrayList<String>()
        val seen = HashSet<String>()
        var index = 0
        while (index < encoded.length) {
            if (ids.size == maxFrames) return null

            val lengthStart = index
            var length = 0
            while (index < encoded.length && encoded[index].isDigit()) {
                val digit = encoded[index] - '0'
                if (length > maxIdCharacters / 10 ||
                    (length == maxIdCharacters / 10 &&
                        digit > maxIdCharacters % 10)) {
                    return null
                }
                length = length * 10 + digit
                index++
            }
            if (index == lengthStart ||
                index >= encoded.length ||
                encoded[index] != ':' ||
                length == 0 && !(allowEmptyFirst && ids.isEmpty()))
                return null
            index++
            if (length > encoded.length - index) return null

            val id = encoded.substring(index, index + length)
            if (id.isEmpty() && !(allowEmptyFirst && ids.isEmpty()) ||
                id.hasUnpairedSurrogate() ||
                id.encodeToByteArray().size > maxIdUtf8Bytes)
                return null
            if (requireUnique && !seen.add(id)) return null
            ids += id
            index += length
        }
        return ids
    }
}

/** A persisted playback checkpoint. */
public sealed interface PlaybackCheckpoint {
    /** Session snapshot carried by every checkpoint. */
    public val snapshot: PlaybackSessionSnapshot
    /** Persisted revision associated with [snapshot], when available. */
    public val revision: Long?

    /** A checkpoint taken immediately. */
    public data class Immediate(
        /** Captured session. */
        public override val snapshot: PlaybackSessionSnapshot,
        /** Persisted revision. */
        public override val revision: Long? = null,
    ) : PlaybackCheckpoint

    /** A checkpoint taken while playing. */
    public data class PlayingProgress(
        /** Checkpoint identity. */
        public val key: ProgressCheckpointKey,
        /** Captured session. */
        public override val snapshot: PlaybackSessionSnapshot,
        /** Persisted revision. */
        public override val revision: Long? = null,
    ) : PlaybackCheckpoint
}

/** Identifies a progress checkpoint within one active playback generation. */
public data class ProgressCheckpointKey(
    /** Engine generation that produced the progress checkpoint. */
    public val generation: Long,
    /** Occurrence selected when the checkpoint was emitted. */
    public val currentOccurrenceId: String,
    /** Whole-second position bucket used to coalesce progress updates. */
    public val secondBucket: Long,
)

/** Converts legacy track IDs into session queue entries. */
internal fun normalizeLegacyQueue(
    trackIds: List<String>
): List<SessionQueueEntry> = trackIds.mapIndexed { index, trackId ->
    SessionQueueEntry(legacyOccurrenceId(index), trackId)
}

private fun legacyOccurrenceId(index: Int): String = "legacy-$index"

private fun hasValidOccurrences(queue: List<SessionQueueEntry>): Boolean {
    if (queue.size > PlaybackSessionCodec.maxIds) return false
    val occurrenceIds = HashSet<String>(queue.size)
    return queue.all { entry ->
        entry.occurrenceId.isNotEmpty() &&
            entry.trackId.isNotEmpty() &&
            occurrenceIds.add(entry.occurrenceId)
    }
}

private fun String.hasUnpairedSurrogate(): Boolean {
    var index = 0
    while (index < length) {
        val character = this[index]
        if (character.isHighSurrogate()) {
            if (index + 1 >= length || !this[index + 1].isLowSurrogate())
                return true
            index += 2
        } else if (character.isLowSurrogate()) {
            return true
        } else {
            index++
        }
    }
    return false
}
