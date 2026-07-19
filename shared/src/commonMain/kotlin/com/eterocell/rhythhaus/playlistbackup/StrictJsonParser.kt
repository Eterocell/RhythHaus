package com.eterocell.rhythhaus.playlistbackup

internal data class ParsedBackupDocument(
    val document: PlaylistBackupDocument,
    val canonicalPayloadEnd: Int,
)

internal class BackupParseException(val error: PlaylistBackupValidationError) : Exception()

internal class StrictJsonParser(private val text: String) {
    private var index = 0
    private var totalEntries = 0

    fun parse(): ParsedBackupDocument {
        skipWhitespace()
        expect('{')
        val seen = mutableSetOf<String>()
        var format: String? = null
        var version: Long? = null
        var exportedAt: Long? = null
        var playlists: List<PlaylistBackupPlaylist>? = null
        var checksum: String? = null
        var payloadEnd = -1
        var first = true
        while (true) {
            skipWhitespace()
            if (consume('}')) break
            val separatorIndex = if (!first) {
                val separator = index
                expect(',')
                separator
            } else {
                -1
            }
            first = false
            skipWhitespace()
            val key = string()
            if (!seen.add(key)) fail(PlaylistBackupValidationError.DUPLICATE_FIELD)
            skipWhitespace()
            expect(':')
            skipWhitespace()
            when (key) {
                "format" -> format = string()
                "version" -> version = integer()
                "exportedAtEpochMillis" -> exportedAt = integer()
                "playlists" -> playlists = playlistArray()
                "checksumCrc32" -> {
                    if (separatorIndex < 0) fail(PlaylistBackupValidationError.MALFORMED_JSON)
                    payloadEnd = separatorIndex
                    checksum = string()
                }
                else -> fail(PlaylistBackupValidationError.UNKNOWN_FIELD)
            }
        }
        skipWhitespace()
        if (index != text.length) fail(PlaylistBackupValidationError.TRAILING_CONTENT)
        if (format == null || version == null || exportedAt == null || playlists == null || checksum == null) {
            fail(PlaylistBackupValidationError.MISSING_FIELD)
        }
        if (format != PlaylistBackupCodec.FORMAT) fail(PlaylistBackupValidationError.UNSUPPORTED_FORMAT)
        if (version != PlaylistBackupCodec.VERSION.toLong()) fail(PlaylistBackupValidationError.UNSUPPORTED_VERSION)
        return ParsedBackupDocument(
            PlaylistBackupDocument(format, version.toInt(), exportedAt, playlists, checksum),
            payloadEnd,
        )
    }

    private fun playlistArray(): List<PlaylistBackupPlaylist> {
        expect('[')
        val result = mutableListOf<PlaylistBackupPlaylist>()
        skipWhitespace()
        if (consume(']')) return result
        while (true) {
            if (result.size == PlaylistBackupLimits.MAX_PLAYLISTS) fail(PlaylistBackupValidationError.PLAYLIST_LIMIT_EXCEEDED)
            result += playlist()
            skipWhitespace()
            if (consume(']')) return result
            expect(',')
            skipWhitespace()
        }
    }

    private fun playlist(): PlaylistBackupPlaylist {
        expect('{')
        val seen = mutableSetOf<String>()
        var name: String? = null
        var entries: List<PlaylistBackupEntry>? = null
        var first = true
        while (true) {
            skipWhitespace()
            if (consume('}')) break
            if (!first) expect(',')
            first = false
            skipWhitespace()
            val key = string()
            if (!seen.add(key)) fail(PlaylistBackupValidationError.DUPLICATE_FIELD)
            skipWhitespace()
            expect(':')
            skipWhitespace()
            when (key) {
                "name" -> name = boundedString()
                "entries" -> entries = entryArray()
                else -> fail(PlaylistBackupValidationError.UNKNOWN_FIELD)
            }
        }
        if (name == null || entries == null) fail(PlaylistBackupValidationError.MISSING_FIELD)
        if (name.isBlank()) fail(PlaylistBackupValidationError.BLANK_PLAYLIST_NAME)
        return PlaylistBackupPlaylist(name, entries)
    }

    private fun entryArray(): List<PlaylistBackupEntry> {
        expect('[')
        val result = mutableListOf<PlaylistBackupEntry>()
        skipWhitespace()
        if (consume(']')) return result
        while (true) {
            if (result.size == PlaylistBackupLimits.MAX_ENTRIES_PER_PLAYLIST) {
                fail(PlaylistBackupValidationError.PLAYLIST_ENTRY_LIMIT_EXCEEDED)
            }
            if (totalEntries == PlaylistBackupLimits.MAX_TOTAL_ENTRIES) {
                fail(PlaylistBackupValidationError.TOTAL_ENTRY_LIMIT_EXCEEDED)
            }
            result += entry()
            totalEntries++
            skipWhitespace()
            if (consume(']')) return result
            expect(',')
            skipWhitespace()
        }
    }

    private fun entry(): PlaylistBackupEntry {
        expect('{')
        val seen = mutableSetOf<String>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration: Long? = null
        var first = true
        while (true) {
            skipWhitespace()
            if (consume('}')) break
            if (!first) expect(',')
            first = false
            skipWhitespace()
            val key = string()
            if (!seen.add(key)) fail(PlaylistBackupValidationError.DUPLICATE_FIELD)
            skipWhitespace()
            expect(':')
            skipWhitespace()
            when (key) {
                "title" -> title = boundedString()
                "artist" -> artist = boundedString()
                "album" -> album = boundedString()
                "durationSeconds" -> duration = integer()
                else -> fail(PlaylistBackupValidationError.UNKNOWN_FIELD)
            }
        }
        if (title == null || artist == null || album == null || duration == null) fail(PlaylistBackupValidationError.MISSING_FIELD)
        if (duration !in 0..PlaylistBackupLimits.MAX_DURATION_SECONDS.toLong()) fail(PlaylistBackupValidationError.INVALID_DURATION)
        return PlaylistBackupEntry(title, artist, album, duration.toInt())
    }

    private fun boundedString(): String = string().also {
        var codePoints = 0
        var position = 0
        while (position < it.length) {
            val first = it[position].code
            position += if (first in 0xD800..0xDBFF) 2 else 1
            codePoints++
            if (codePoints > PlaylistBackupLimits.MAX_STRING_CODE_POINTS) {
                fail(PlaylistBackupValidationError.STRING_LIMIT_EXCEEDED)
            }
        }
    }

    private fun string(): String {
        expect('"')
        val result = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when {
                char == '"' -> return result.toString()
                char == '\\' -> escaped(result)
                char.code < 0x20 -> fail(PlaylistBackupValidationError.MALFORMED_JSON)
                char.isHighSurrogate() -> {
                    if (index >= text.length || !text[index].isLowSurrogate()) fail(PlaylistBackupValidationError.MALFORMED_JSON)
                    result.append(char).append(text[index++])
                }
                char.isLowSurrogate() -> fail(PlaylistBackupValidationError.MALFORMED_JSON)
                else -> result.append(char)
            }
        }
        fail(PlaylistBackupValidationError.MALFORMED_JSON)
    }

    private fun escaped(result: StringBuilder) {
        if (index >= text.length) fail(PlaylistBackupValidationError.MALFORMED_JSON)
        when (val escape = text[index++]) {
            '"', '\\', '/' -> result.append(escape)
            'b' -> result.append('\b')
            'f' -> result.append('\u000c')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            't' -> result.append('\t')
            'u' -> {
                val first = hexCodeUnit()
                when {
                    first in 0xD800..0xDBFF -> {
                        if (index + 2 > text.length || text[index] != '\\' || text[index + 1] != 'u') {
                            fail(PlaylistBackupValidationError.MALFORMED_JSON)
                        }
                        index += 2
                        val second = hexCodeUnit()
                        if (second !in 0xDC00..0xDFFF) fail(PlaylistBackupValidationError.MALFORMED_JSON)
                        result.append(first.toChar()).append(second.toChar())
                    }
                    first in 0xDC00..0xDFFF -> fail(PlaylistBackupValidationError.MALFORMED_JSON)
                    else -> result.append(first.toChar())
                }
            }
            else -> fail(PlaylistBackupValidationError.MALFORMED_JSON)
        }
    }

    private fun hexCodeUnit(): Int {
        if (index + 4 > text.length) fail(PlaylistBackupValidationError.MALFORMED_JSON)
        var value = 0
        repeat(4) {
            val digit = text[index++].digitToIntOrNull(16) ?: fail(PlaylistBackupValidationError.MALFORMED_JSON)
            value = value * 16 + digit
        }
        return value
    }

    private fun integer(): Long {
        val start = index
        if (consume('-')) {
            if (index >= text.length) fail(PlaylistBackupValidationError.INVALID_INTEGER)
        }
        if (index >= text.length || text[index] !in '0'..'9') fail(PlaylistBackupValidationError.INVALID_INTEGER)
        if (text[index] == '0' && index + 1 < text.length && text[index + 1] in '0'..'9') {
            fail(PlaylistBackupValidationError.INVALID_INTEGER)
        }
        while (index < text.length && text[index] in '0'..'9') index++
        if (index < text.length && text[index] !in " \t\r\n,]}") fail(PlaylistBackupValidationError.INVALID_INTEGER)
        return text.substring(start, index).toLongOrNull() ?: fail(PlaylistBackupValidationError.NUMERIC_OVERFLOW)
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index] in " \t\r\n") index++
    }

    private fun expect(expected: Char) {
        if (!consume(expected)) fail(PlaylistBackupValidationError.MALFORMED_JSON)
    }

    private fun consume(expected: Char): Boolean {
        if (index < text.length && text[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun fail(error: PlaylistBackupValidationError): Nothing = throw BackupParseException(error)
}
