package com.eterocell.rhythhaus.playlistbackup

object PlaylistBackupCodec {
    const val FORMAT = "rhythhaus-playlist-backup"
    const val VERSION = 1

    fun encode(payload: PlaylistBackupPayload): ByteArray {
        validatePayload(payload)
        val canonicalPayload = canonicalPayload(payload)
        val complete =
            canonicalPayload.dropLast(1) +
                ",\"checksumCrc32\":\"${Crc32.hex(canonicalPayload.encodeToByteArray())}\"}"
        return complete.encodeToByteArray().also {
            require(it.size <= PlaylistBackupLimits.MAX_BYTES) {
                "Encoded playlist backup exceeds 4 MiB"
            }
        }
    }

    fun decode(bytes: ByteArray): PlaylistBackupDecodeResult {
        if (bytes.size > PlaylistBackupLimits.MAX_BYTES)
            return invalid(PlaylistBackupValidationError.INPUT_TOO_LARGE)
        val text =
            decodeUtf8Strict(bytes)
                ?: return invalid(PlaylistBackupValidationError.MALFORMED_UTF8)
        return try {
            val parsed = StrictJsonParser(text).parse()
            val checksum = parsed.document.checksumCrc32
            if (checksum.length != 8 ||
                checksum.any { it !in '0'..'9' && it !in 'a'..'f' }) {
                invalid(PlaylistBackupValidationError.INVALID_CHECKSUM)
            } else {
                val payloadBytes =
                    text
                        .substring(0, parsed.canonicalPayloadEnd)
                        .plus("}")
                        .encodeToByteArray()
                if (Crc32.hex(payloadBytes) != checksum) {
                    invalid(PlaylistBackupValidationError.INVALID_CHECKSUM)
                } else {
                    val canonical =
                        completeDocument(
                            PlaylistBackupPayload(
                                parsed.document.exportedAtEpochMillis,
                                parsed.document.playlists),
                            checksum,
                        )
                    if (text != canonical) {
                        invalid(
                            PlaylistBackupValidationError.NON_CANONICAL_JSON)
                    } else {
                        PlaylistBackupDecodeResult.Success(parsed.document)
                    }
                }
            }
        } catch (exception: BackupParseException) {
            invalid(exception.error)
        }
    }

    private fun canonicalPayload(payload: PlaylistBackupPayload): String =
        BoundedJsonWriter()
            .apply {
                append("{\"format\":\"").append(FORMAT)
                append("\",\"version\":").append(VERSION)
                append(",\"exportedAtEpochMillis\":")
                    .append(payload.exportedAtEpochMillis)
                append(",\"playlists\":[")
                payload.playlists.forEachIndexed { playlistIndex, playlist ->
                    if (playlistIndex > 0) append(',')
                    append("{\"name\":")
                    appendJsonString(playlist.name)
                    append(",\"entries\":[")
                    playlist.entries.forEachIndexed { entryIndex, entry ->
                        if (entryIndex > 0) append(',')
                        append("{\"title\":")
                        appendJsonString(entry.title)
                        append(",\"artist\":")
                        appendJsonString(entry.artist)
                        append(",\"album\":")
                        appendJsonString(entry.album)
                        append(",\"durationSeconds\":")
                            .append(entry.durationSeconds)
                            .append('}')
                    }
                    append("]}")
                }
                append("]}")
            }
            .toString()

    private fun completeDocument(
        payload: PlaylistBackupPayload,
        checksum: String
    ): String =
        canonicalPayload(payload).dropLast(1) +
            ",\"checksumCrc32\":\"$checksum\"}"

    private fun BoundedJsonWriter.appendJsonString(value: String) {
        append('"')
        var index = 0
        while (index < value.length) {
            val char = value[index]
            when (char) {
                '"' -> append("\\\"")

                '\\' -> append("\\\\")

                '\b' -> append("\\b")

                '\u000c' -> append("\\f")

                '\n' -> append("\\n")

                '\r' -> append("\\r")

                '\t' -> append("\\t")

                else ->
                    when {
                        char.code < 0x20 ->
                            append("\\u")
                                .append(char.code.toString(16).padStart(4, '0'))

                        char.isHighSurrogate() -> {
                            append(value.substring(index, index + 2))
                            index++
                        }

                        else -> append(char)
                    }
            }
            index++
        }
        append('"')
    }

    private fun validatePayload(payload: PlaylistBackupPayload) {
        require(payload.playlists.size <= PlaylistBackupLimits.MAX_PLAYLISTS)
        var totalEntries = 0
        payload.playlists.forEach { playlist ->
            require(!playlist.name.isBlank())
            require(validString(playlist.name))
            require(
                playlist.entries.size <=
                    PlaylistBackupLimits.MAX_ENTRIES_PER_PLAYLIST)
            totalEntries += playlist.entries.size
            require(totalEntries <= PlaylistBackupLimits.MAX_TOTAL_ENTRIES)
            playlist.entries.forEach { entry ->
                require(
                    validString(entry.title) &&
                        validString(entry.artist) &&
                        validString(entry.album))
                require(
                    entry.durationSeconds in
                        0..PlaylistBackupLimits.MAX_DURATION_SECONDS)
            }
        }
    }

    private fun validString(value: String): Boolean {
        var count = 0
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char.isHighSurrogate()) {
                if (index + 1 >= value.length ||
                    !value[index + 1].isLowSurrogate())
                    return false
                index += 2
            } else {
                if (char.isLowSurrogate()) return false
                index++
            }
            count++
            if (count > PlaylistBackupLimits.MAX_STRING_CODE_POINTS)
                return false
        }
        return true
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String? {
        val result = StringBuilder(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xff
            when {
                first <= 0x7f -> {
                    result.append(first.toChar())
                    index++
                }

                first in 0xc2..0xdf -> {
                    if (index + 1 >= bytes.size) return null
                    val second = continuation(bytes[index + 1]) ?: return null
                    result.append(((first and 0x1f) shl 6 or second).toChar())
                    index += 2
                }

                first in 0xe0..0xef -> {
                    if (index + 2 >= bytes.size) return null
                    val secondByte = bytes[index + 1].toInt() and 0xff
                    val second = continuation(bytes[index + 1]) ?: return null
                    val third = continuation(bytes[index + 2]) ?: return null
                    if (first == 0xe0 && secondByte < 0xa0 ||
                        first == 0xed && secondByte >= 0xa0)
                        return null
                    result.append(
                        ((first and 0x0f) shl 12 or (second shl 6) or third)
                            .toChar())
                    index += 3
                }

                first in 0xf0..0xf4 -> {
                    if (index + 3 >= bytes.size) return null
                    val secondByte = bytes[index + 1].toInt() and 0xff
                    val second = continuation(bytes[index + 1]) ?: return null
                    val third = continuation(bytes[index + 2]) ?: return null
                    val fourth = continuation(bytes[index + 3]) ?: return null
                    if (first == 0xf0 && secondByte < 0x90 ||
                        first == 0xf4 && secondByte >= 0x90)
                        return null
                    val codePoint =
                        (first and 0x07) shl
                            18 or
                            (second shl 12) or
                            (third shl 6) or
                            fourth
                    val adjusted = codePoint - 0x10000
                    result.append((0xd800 + (adjusted shr 10)).toChar())
                    result.append((0xdc00 + (adjusted and 0x3ff)).toChar())
                    index += 4
                }

                else -> return null
            }
        }
        return result.toString()
    }

    private fun continuation(byte: Byte): Int? {
        val value = byte.toInt() and 0xff
        return if (value in 0x80..0xbf) value and 0x3f else null
    }

    private fun invalid(error: PlaylistBackupValidationError) =
        PlaylistBackupDecodeResult.Invalid(error)

    private class BoundedJsonWriter {
        private val builder = StringBuilder()
        private var utf8Bytes = 0

        fun append(value: String): BoundedJsonWriter {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                val byteCount =
                    when {
                        char.code <= 0x7f -> 1

                        char.code <= 0x7ff -> 2

                        char.isHighSurrogate() -> {
                            require(
                                index + 1 < value.length &&
                                    value[index + 1].isLowSurrogate())
                            index++
                            4
                        }

                        else -> 3
                    }
                require(
                    utf8Bytes <= PlaylistBackupLimits.MAX_BYTES - byteCount) {
                        "Encoded playlist backup exceeds 4 MiB"
                    }
                utf8Bytes += byteCount
                builder.append(char)
                if (byteCount == 4) builder.append(value[index])
                index++
            }
            return this
        }

        fun append(value: Char): BoundedJsonWriter = append(value.toString())

        fun append(value: Int): BoundedJsonWriter = append(value.toString())

        fun append(value: Long): BoundedJsonWriter = append(value.toString())

        override fun toString(): String = builder.toString()
    }
}
