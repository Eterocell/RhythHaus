package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode

data class PlaybackSessionSnapshot(
    val queueIds: List<String> = emptyList(),
    val currentTrackId: String? = null,
    val positionMillis: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.StopAfterQueue,
    val shuffleMode: ShuffleMode = ShuffleMode.Off,
)

object PlaybackSessionCodec {
    const val maxIds = 10_000
    const val maxIdCharacters = 4_096
    const val maxIdUtf8Bytes = 16_384
    const val maxEncodedUtf8Bytes = 1_048_576

    fun encodeIds(ids: List<String>): String {
        require(ids.size <= maxIds)
        require(ids.distinct().size == ids.size)

        return buildString {
            ids.forEach { id ->
                require(id.isNotEmpty() && id.length <= maxIdCharacters)
                require(!id.hasUnpairedSurrogate() && id.encodeToByteArray().size <= maxIdUtf8Bytes)
                append(id.length).append(':').append(id)
            }
        }.also { require(it.encodeToByteArray().size <= maxEncodedUtf8Bytes) }
    }

    fun decodeIds(encoded: String): List<String>? {
        if (encoded.isEmpty()) return emptyList()
        if (encoded.encodeToByteArray().size > maxEncodedUtf8Bytes) return null

        val ids = ArrayList<String>()
        var index = 0
        while (index < encoded.length) {
            if (ids.size == maxIds) return null

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
            if (index == lengthStart || index >= encoded.length || encoded[index] != ':' || length == 0) return null
            index++
            if (length > encoded.length - index) return null

            val id = encoded.substring(index, index + length)
            if (id.isEmpty() || id.hasUnpairedSurrogate() || id.encodeToByteArray().size > maxIdUtf8Bytes) return null
            if (ids.contains(id)) return null
            ids += id
            index += length
        }
        return ids
    }
}

sealed interface PlaybackCheckpoint {
    val snapshot: PlaybackSessionSnapshot

    data class Immediate(override val snapshot: PlaybackSessionSnapshot) : PlaybackCheckpoint

    data class PlayingProgress(
        val key: ProgressCheckpointKey,
        override val snapshot: PlaybackSessionSnapshot,
    ) : PlaybackCheckpoint
}

data class ProgressCheckpointKey(
    val generation: Long,
    val currentTrackId: String,
    val secondBucket: Long,
)

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
