package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode

data class PlaybackSessionSnapshot(
    val queue: List<SessionQueueEntry> = emptyList(),
    val currentOccurrenceId: String? = null,
    val positionMillis: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
) {
    constructor(
        queueIds: List<String>,
        currentTrackId: String?,
        positionMillis: Long = 0L,
        repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
        shuffleMode: ShuffleMode = ShuffleMode.Off,
        @Suppress("UNUSED_PARAMETER") legacyTrackIds: Boolean = true,
    ) : this(
        queue = normalizeLegacyQueue(queueIds),
        currentOccurrenceId = currentTrackId?.let { current ->
            queueIds.indexOf(current).takeIf { it >= 0 }?.let(::legacyOccurrenceId)
        },
        positionMillis = positionMillis,
        repeatMode = repeatMode,
        shuffleMode = shuffleMode,
    )

    val queueIds: List<String> get() = queue.map { it.trackId }
    val currentTrackId: String? get() = queue.firstOrNull { it.occurrenceId == currentOccurrenceId }?.trackId
}

data class SessionQueueEntry(
    val occurrenceId: String,
    val trackId: String,
)

object PlaybackSessionCodec {
    const val maxIds = 10_000
    const val maxIdCharacters = 4_096
    const val maxIdUtf8Bytes = 16_384
    const val maxEncodedUtf8Bytes = 1_048_576

    fun encodeSnapshot(snapshot: PlaybackSessionSnapshot): String {
        require(hasValidOccurrences(snapshot.queue))
        require(
            snapshot.currentOccurrenceId == null ||
                snapshot.queue.any { it.occurrenceId == snapshot.currentOccurrenceId },
        )
        return encodeIds(
            listOf(snapshot.currentOccurrenceId.orEmpty()) + snapshot.queue.flatMap { listOf(it.occurrenceId, it.trackId) },
            requireUnique = false,
            allowEmptyFirst = true,
            maxFrames = maxIds * 2 + 1,
        )
    }

    fun decodeSnapshot(encoded: String): PlaybackSessionSnapshot? {
        val values = decodeIds(
            encoded,
            requireUnique = false,
            allowEmptyFirst = true,
            maxFrames = maxIds * 2 + 1,
        ) ?: return null
        if (values.isEmpty() || (values.size - 1) % 2 != 0) return null
        val queue = values.drop(1).chunked(2).map { SessionQueueEntry(it[0], it[1]) }
        if (!hasValidOccurrences(queue)) return null
        val currentOccurrenceId = values.first().ifEmpty { null }
        if (currentOccurrenceId != null && queue.none { it.occurrenceId == currentOccurrenceId }) return null
        return PlaybackSessionSnapshot(queue = queue, currentOccurrenceId = currentOccurrenceId)
    }

    fun encodeQueue(queue: List<SessionQueueEntry>): String {
        require(hasValidOccurrences(queue))
        return encodeIds(
            queue.flatMap { listOf(it.occurrenceId, it.trackId) },
            requireUnique = false,
            maxFrames = maxIds * 2,
        )
    }

    fun decodeQueue(encoded: String): List<SessionQueueEntry>? {
        val values = decodeIds(encoded, requireUnique = false, maxFrames = maxIds * 2) ?: return null
        if (values.size % 2 != 0) return null
        return values.chunked(2).map { SessionQueueEntry(it[0], it[1]) }.takeIf(::hasValidOccurrences)
    }

    fun encodeIds(ids: List<String>): String = encodeIds(ids, requireUnique = true)

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
                require((id.isNotEmpty() || allowEmptyFirst && index == 0) && id.length <= maxIdCharacters)
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

    fun decodeIds(encoded: String): List<String>? = decodeIds(encoded, requireUnique = true)

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
                    (length == maxIdCharacters / 10 && digit > maxIdCharacters % 10)
                ) {
                    return null
                }
                length = length * 10 + digit
                index++
            }
            if (index == lengthStart || index >= encoded.length || encoded[index] != ':' || length == 0 && !(allowEmptyFirst && ids.isEmpty())) return null
            index++
            if (length > encoded.length - index) return null

            val id = encoded.substring(index, index + length)
            if (id.isEmpty() && !(allowEmptyFirst && ids.isEmpty()) || id.hasUnpairedSurrogate() || id.encodeToByteArray().size > maxIdUtf8Bytes) return null
            if (requireUnique && !seen.add(id)) return null
            ids += id
            index += length
        }
        return ids
    }
}

sealed interface PlaybackCheckpoint {
    val snapshot: PlaybackSessionSnapshot
    val revision: Long?

    data class Immediate(
        override val snapshot: PlaybackSessionSnapshot,
        override val revision: Long? = null,
    ) : PlaybackCheckpoint

    data class PlayingProgress(
        val key: ProgressCheckpointKey,
        override val snapshot: PlaybackSessionSnapshot,
        override val revision: Long? = null,
    ) : PlaybackCheckpoint
}

data class ProgressCheckpointKey(
    val generation: Long,
    val currentOccurrenceId: String,
    val secondBucket: Long,
)

internal fun normalizeLegacyQueue(trackIds: List<String>): List<SessionQueueEntry> = trackIds.mapIndexed { index, trackId -> SessionQueueEntry(legacyOccurrenceId(index), trackId) }

private fun legacyOccurrenceId(index: Int): String = "legacy-$index"

private fun hasValidOccurrences(queue: List<SessionQueueEntry>): Boolean {
    if (queue.size > PlaybackSessionCodec.maxIds) return false
    val occurrenceIds = HashSet<String>(queue.size)
    return queue.all { entry ->
        entry.occurrenceId.isNotEmpty() && entry.trackId.isNotEmpty() && occurrenceIds.add(entry.occurrenceId)
    }
}

private fun String.hasUnpairedSurrogate(): Boolean {
    var index = 0
    while (index < length) {
        val character = this[index]
        if (character.isHighSurrogate()) {
            if (index + 1 >= length || !this[index + 1].isLowSurrogate()) return true
            index += 2
        } else if (character.isLowSurrogate()) {
            return true
        } else {
            index++
        }
    }
    return false
}
