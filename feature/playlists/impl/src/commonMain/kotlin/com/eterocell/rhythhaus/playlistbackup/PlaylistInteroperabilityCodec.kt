package com.eterocell.rhythhaus.playlistbackup

internal const val DEFAULT_INTEROPERABILITY_PLAYLIST_NAME = "Imported playlist"

/** Supported interoperable playlist document formats. */
public enum class PlaylistInteroperabilityFormat {
    M3U,
    M3U8,
    PLS,
}

internal data class PlaylistInteroperabilityDocument(
    val entries: List<PlaylistInteroperabilityEntry>,
    val format: PlaylistInteroperabilityFormat,
    val name: String = DEFAULT_INTEROPERABILITY_PLAYLIST_NAME,
)

internal data class PlaylistInteroperabilityEntry(
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val path: String,
)

internal sealed interface PlaylistInteroperabilityDecodeResult {
    data class Success(val document: PlaylistInteroperabilityDocument) :
        PlaylistInteroperabilityDecodeResult

    data class Invalid(val error: PlaylistInteroperabilityValidationError) :
        PlaylistInteroperabilityDecodeResult
}

internal object PlaylistInteroperabilityCodec {
    internal fun encode(
        document: PlaylistInteroperabilityDocument,
        format: PlaylistInteroperabilityFormat = document.format,
    ): ByteArray {
        validateDocument(document)
        val writer = BoundedUtf8Writer()
        when (format) {
            PlaylistInteroperabilityFormat.M3U,
            PlaylistInteroperabilityFormat.M3U8,
            -> writeM3u(document, writer)

            PlaylistInteroperabilityFormat.PLS -> writePls(document, writer)
        }
        return writer.toByteArray()
    }

    internal fun decode(
        bytes: ByteArray,
        format: PlaylistInteroperabilityFormat,
    ): PlaylistInteroperabilityDecodeResult {
        if (bytes.size > PlaylistBackupLimits.MAX_BYTES) {
            return invalid(
                PlaylistInteroperabilityValidationError.INPUT_TOO_LARGE)
        }
        val text =
            decodeUtf8Strict(bytes)
                ?: return invalid(
                    PlaylistInteroperabilityValidationError.MALFORMED_UTF8)
        return try {
            PlaylistInteroperabilityDecodeResult.Success(
                when (format) {
                    PlaylistInteroperabilityFormat.M3U,
                    PlaylistInteroperabilityFormat.M3U8,
                    -> parseM3u(text, format)

                    PlaylistInteroperabilityFormat.PLS -> parsePls(text)
                },
            )
        } catch (exception: InteroperabilityParseException) {
            invalid(exception.error)
        }
    }

    private fun writeM3u(
        document: PlaylistInteroperabilityDocument,
        writer: BoundedUtf8Writer,
    ) {
        writer.append("#EXTM3U")
        writer.newLine()
        writer.append("#PLAYLIST:")
        writer.append(document.name)
        writer.newLine()
        document.entries.forEach { entry ->
            writer.append("#EXTINF:")
            writer.append(entry.durationSeconds)
            writer.append(',')
            writer.append(entry.title)
            writer.newLine()
            writer.append("#EXTART:")
            writer.append(entry.artist)
            writer.newLine()
            writer.append("#EXTALB:")
            writer.append(entry.album)
            writer.newLine()
            writer.append(entry.path)
            writer.newLine()
        }
    }

    private fun writePls(
        document: PlaylistInteroperabilityDocument,
        writer: BoundedUtf8Writer,
    ) {
        writer.append("[playlist]")
        writer.newLine()
        writer.append("PlaylistName=")
        writer.append(document.name)
        writer.newLine()
        document.entries.forEachIndexed { entryIndex, entry ->
            val index = entryIndex + 1
            writer.append("File")
            writer.append(index)
            writer.append('=')
            writer.append(entry.path)
            writer.newLine()
            writer.append("Title")
            writer.append(index)
            writer.append('=')
            writer.append(entry.title)
            writer.newLine()
            writer.append("Artist")
            writer.append(index)
            writer.append('=')
            writer.append(entry.artist)
            writer.newLine()
            writer.append("Album")
            writer.append(index)
            writer.append('=')
            writer.append(entry.album)
            writer.newLine()
            writer.append("Length")
            writer.append(index)
            writer.append('=')
            writer.append(entry.durationSeconds)
            writer.newLine()
        }
        writer.append("NumberOfEntries=")
        writer.append(document.entries.size)
        writer.newLine()
        writer.append("Version=2")
        writer.newLine()
    }

    private fun parseM3u(
        text: String,
        format: PlaylistInteroperabilityFormat,
    ): PlaylistInteroperabilityDocument {
        var firstLine = true
        var headerSeen = false
        var name: String? = null
        var pending: M3uEntry? = null
        val entries = mutableListOf<PlaylistInteroperabilityEntry>()

        forEachDocumentLine(
            text,
            PlaylistInteroperabilityValidationError.MALFORMED_M3U,
        ) { rawLine ->
            val line =
                if (firstLine && rawLine.firstOrNull() == '\uFEFF') {
                    rawLine.substring(1)
                } else {
                    rawLine
                }
            firstLine = false
            if (line.isEmpty()) return@forEachDocumentLine
            if (!headerSeen) {
                if (line != "#EXTM3U") {
                    fail(PlaylistInteroperabilityValidationError.MALFORMED_M3U)
                }
                headerSeen = true
                return@forEachDocumentLine
            }
            when {
                line == "#EXTM3U" || line.startsWith("#EXTM3U") ->
                    fail(PlaylistInteroperabilityValidationError.MALFORMED_M3U)

                line.startsWith("#PLAYLIST") -> {
                    val playlistName = m3uTagValue(line, "#PLAYLIST")
                    if (name != null ||
                        pending != null ||
                        entries.isNotEmpty()) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .MALFORMED_M3U)
                    }
                    validateFieldString(
                        playlistName,
                        PlaylistInteroperabilityValidationError.MALFORMED_M3U,
                    )
                    if (playlistName.isBlank()) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .BLANK_PLAYLIST_NAME)
                    }
                    name = playlistName
                }

                line.startsWith("#EXTINF") -> {
                    val extendedInfo = m3uTagValue(line, "#EXTINF")
                    if (pending != null) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .MALFORMED_M3U)
                    }
                    val separator = extendedInfo.indexOf(',')
                    if (separator < 0) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .MALFORMED_M3U)
                    }
                    val duration =
                        parseDuration(extendedInfo.substring(0, separator))
                    val title = extendedInfo.substring(separator + 1)
                    validateFieldString(
                        title,
                        PlaylistInteroperabilityValidationError.MALFORMED_M3U,
                    )
                    pending = M3uEntry(duration, title)
                }

                line.startsWith("#EXTART") -> {
                    val artist = m3uTagValue(line, "#EXTART")
                    val entry =
                        pending
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MALFORMED_M3U)
                    if (entry.artist != null) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .DUPLICATE_FIELD)
                    }
                    validateFieldString(
                        artist,
                        PlaylistInteroperabilityValidationError.MALFORMED_M3U,
                    )
                    entry.artist = artist
                }

                line.startsWith("#EXTALB") -> {
                    val album = m3uTagValue(line, "#EXTALB")
                    val entry =
                        pending
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MALFORMED_M3U)
                    if (entry.album != null) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .DUPLICATE_FIELD)
                    }
                    validateFieldString(
                        album,
                        PlaylistInteroperabilityValidationError.MALFORMED_M3U,
                    )
                    entry.album = album
                }

                line.startsWith('#') -> Unit

                else -> {
                    val entry =
                        pending
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MALFORMED_M3U)
                    validatePath(
                        line,
                        PlaylistInteroperabilityValidationError.MALFORMED_M3U,
                    )
                    if (entries.size ==
                        PlaylistBackupLimits.MAX_TOTAL_ENTRIES) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .ENTRY_LIMIT_EXCEEDED)
                    }
                    entries +=
                        PlaylistInteroperabilityEntry(
                            title = entry.title,
                            artist = entry.artist.orEmpty(),
                            album = entry.album.orEmpty(),
                            durationSeconds = entry.durationSeconds,
                            path = line,
                        )
                    pending = null
                }
            }
        }
        if (!headerSeen || pending != null) {
            fail(PlaylistInteroperabilityValidationError.MALFORMED_M3U)
        }
        return PlaylistInteroperabilityDocument(
            entries = entries,
            format = format,
            name = name ?: DEFAULT_INTEROPERABILITY_PLAYLIST_NAME,
        )
    }

    private fun parsePls(text: String): PlaylistInteroperabilityDocument {
        var firstLine = true
        var sectionSeen = false
        var name: String? = null
        var numberOfEntries: Int? = null
        var versionSeen = false
        val fieldsByIndex = mutableMapOf<Long, PlsEntryFields>()
        val fieldCounts = PlsFieldCounts()

        forEachDocumentLine(
            text,
            PlaylistInteroperabilityValidationError.MALFORMED_PLS,
        ) { rawLine ->
            val line =
                if (firstLine && rawLine.firstOrNull() == '\uFEFF') {
                    rawLine.substring(1)
                } else {
                    rawLine
                }
            firstLine = false
            if (line.isEmpty() ||
                line.startsWith(';') ||
                line.startsWith('#')) {
                return@forEachDocumentLine
            }
            if (!sectionSeen) {
                if (!line.equals("[playlist]", ignoreCase = true)) {
                    fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
                }
                sectionSeen = true
                return@forEachDocumentLine
            }
            if (line.startsWith('[')) {
                fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
            }
            val separator = line.indexOf('=')
            if (separator <= 0) {
                fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
            }
            val key = line.substring(0, separator)
            if (key != key.trim()) {
                fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
            }
            val value = line.substring(separator + 1)
            when (key.lowercase()) {
                "playlistname" -> {
                    if (name != null) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .DUPLICATE_FIELD)
                    }
                    validateFieldString(
                        value,
                        PlaylistInteroperabilityValidationError.MALFORMED_PLS,
                    )
                    if (value.isBlank()) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .BLANK_PLAYLIST_NAME)
                    }
                    name = value
                }

                "numberofentries" -> {
                    if (numberOfEntries != null) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .DUPLICATE_FIELD)
                    }
                    val parsed = parsePlainInteger(value)
                    if (parsed < 0L) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .INVALID_INTEGER)
                    }
                    if (parsed >
                        PlaylistBackupLimits.MAX_TOTAL_ENTRIES.toLong()) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .ENTRY_LIMIT_EXCEEDED)
                    }
                    numberOfEntries = parsed.toInt()
                }

                "version" -> {
                    if (versionSeen) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .DUPLICATE_FIELD)
                    }
                    versionSeen = true
                    if (parsePlainInteger(value) != 2L) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .UNSUPPORTED_VERSION)
                    }
                }

                else ->
                    parsePlsEntryField(key, value, fieldsByIndex, fieldCounts)
            }
        }

        if (!sectionSeen || numberOfEntries == null || !versionSeen) {
            fail(PlaylistInteroperabilityValidationError.MISSING_FIELD)
        }
        val entryCount =
            numberOfEntries
                ?: fail(PlaylistInteroperabilityValidationError.MISSING_FIELD)
        if (fieldsByIndex.keys.any { it > entryCount.toLong() }) {
            fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
        }
        val entries = ArrayList<PlaylistInteroperabilityEntry>(entryCount)
        repeat(entryCount) { entryOffset ->
            val fields =
                fieldsByIndex[(entryOffset + 1).toLong()]
                    ?: fail(
                        PlaylistInteroperabilityValidationError.MISSING_FIELD)
            entries +=
                PlaylistInteroperabilityEntry(
                    title =
                        fields.title
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MISSING_FIELD),
                    artist = fields.artist.orEmpty(),
                    album = fields.album.orEmpty(),
                    durationSeconds =
                        fields.durationSeconds
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MISSING_FIELD),
                    path =
                        fields.path
                            ?: fail(
                                PlaylistInteroperabilityValidationError
                                    .MISSING_FIELD),
                )
        }
        return PlaylistInteroperabilityDocument(
            entries = entries,
            format = PlaylistInteroperabilityFormat.PLS,
            name = name ?: DEFAULT_INTEROPERABILITY_PLAYLIST_NAME,
        )
    }

    private fun parsePlsEntryField(
        key: String,
        value: String,
        fieldsByIndex: MutableMap<Long, PlsEntryFields>,
        fieldCounts: PlsFieldCounts,
    ) {
        val normalized = key.lowercase()
        val field =
            when {
                normalized.startsWith("file") -> PlsField.FILE
                normalized.startsWith("title") -> PlsField.TITLE
                normalized.startsWith("artist") -> PlsField.ARTIST
                normalized.startsWith("album") -> PlsField.ALBUM
                normalized.startsWith("length") -> PlsField.LENGTH
                else ->
                    fail(PlaylistInteroperabilityValidationError.MALFORMED_PLS)
            }
        val prefixLength =
            when (field) {
                PlsField.FILE -> "file".length
                PlsField.TITLE -> "title".length
                PlsField.ARTIST -> "artist".length
                PlsField.ALBUM -> "album".length
                PlsField.LENGTH -> "length".length
            }
        val index = parsePlsIndex(key.substring(prefixLength))
        val existing = fieldsByIndex[index]
        if (existing?.has(field) == true) {
            fail(PlaylistInteroperabilityValidationError.DUPLICATE_FIELD)
        }
        when (field) {
            PlsField.FILE ->
                validatePath(
                    value,
                    PlaylistInteroperabilityValidationError.MALFORMED_PLS,
                    rejectCommentMarker = false,
                )

            PlsField.TITLE,
            PlsField.ARTIST,
            PlsField.ALBUM,
            ->
                validateFieldString(
                    value,
                    PlaylistInteroperabilityValidationError.MALFORMED_PLS,
                )

            PlsField.LENGTH -> parseDuration(value)
        }
        fieldCounts.increment(field)
        val fields =
            existing
                ?: run {
                    if (fieldsByIndex.size ==
                        PlaylistBackupLimits.MAX_TOTAL_ENTRIES) {
                        fail(
                            PlaylistInteroperabilityValidationError
                                .ENTRY_LIMIT_EXCEEDED)
                    }
                    PlsEntryFields().also { fieldsByIndex[index] = it }
                }
        when (field) {
            PlsField.FILE -> fields.path = value
            PlsField.TITLE -> fields.title = value
            PlsField.ARTIST -> fields.artist = value
            PlsField.ALBUM -> fields.album = value
            PlsField.LENGTH -> fields.durationSeconds = parseDuration(value)
        }
    }

    private fun m3uTagValue(line: String, tag: String): String {
        val prefix = "$tag:"
        if (!line.startsWith(prefix)) {
            fail(PlaylistInteroperabilityValidationError.MALFORMED_M3U)
        }
        return line.substring(prefix.length)
    }

    private fun parsePlsIndex(value: String): Long {
        val index = parsePlainInteger(value)
        if (index <= 0L) {
            fail(PlaylistInteroperabilityValidationError.INVALID_INTEGER)
        }
        return index
    }

    private fun parseDuration(value: String): Int {
        val duration = parsePlainInteger(value)
        if (duration !in
            0L..PlaylistBackupLimits.MAX_DURATION_SECONDS.toLong()) {
            fail(PlaylistInteroperabilityValidationError.INVALID_DURATION)
        }
        return duration.toInt()
    }

    private fun parsePlainInteger(value: String): Long {
        if (value.isEmpty()) {
            fail(PlaylistInteroperabilityValidationError.INVALID_INTEGER)
        }
        val numberStart = if (value.first() == '-') 1 else 0
        if (numberStart == value.length ||
            (value.length - numberStart > 1 && value[numberStart] == '0') ||
            value.substring(numberStart).any { it !in '0'..'9' }) {
            fail(PlaylistInteroperabilityValidationError.INVALID_INTEGER)
        }
        return value.toLongOrNull()
            ?: fail(PlaylistInteroperabilityValidationError.NUMERIC_OVERFLOW)
    }

    private fun validateDocument(document: PlaylistInteroperabilityDocument) {
        requireValidName(document.name)
        require(
            document.entries.size <= PlaylistBackupLimits.MAX_TOTAL_ENTRIES) {
                "Playlist interoperability document exceeds the entry limit"
            }
        document.entries.forEach { entry ->
            requireValidField(entry.title)
            requireValidField(entry.artist)
            requireValidField(entry.album)
            requireValidPath(
                entry.path,
                rejectCommentMarker =
                    document.format != PlaylistInteroperabilityFormat.PLS,
            )
            require(
                entry.durationSeconds in
                    0..PlaylistBackupLimits.MAX_DURATION_SECONDS) {
                    "Playlist interoperability duration is out of bounds"
                }
        }
    }

    private fun requireValidName(value: String) {
        require(!value.isBlank()) { "Playlist interoperability name is blank" }
        requireValidField(value)
    }

    private fun requireValidPath(value: String, rejectCommentMarker: Boolean) {
        require(!value.isBlank()) { "Playlist interoperability path is blank" }
        require(!rejectCommentMarker || !value.startsWith('#')) {
            "Playlist interoperability M3U path cannot begin with a comment marker"
        }
        requireValidField(value)
    }

    private fun requireValidField(value: String) {
        require(validFieldString(value)) {
            "Playlist interoperability field is not representable"
        }
    }

    private fun validatePath(
        value: String,
        malformedError: PlaylistInteroperabilityValidationError,
        rejectCommentMarker: Boolean = true,
    ) {
        validateFieldString(value, malformedError)
        if (value.isBlank() || (rejectCommentMarker && value.startsWith('#'))) {
            fail(malformedError)
        }
    }

    private fun validateFieldString(
        value: String,
        malformedError: PlaylistInteroperabilityValidationError,
    ) {
        val codePoints = codePointCount(value)
        if (codePoints == null ||
            codePoints > PlaylistBackupLimits.MAX_STRING_CODE_POINTS) {
            fail(PlaylistInteroperabilityValidationError.STRING_LIMIT_EXCEEDED)
        }
        if (value.any { character ->
            character == '\r' ||
                character == '\n' ||
                (character.code < 0x20 && character != '\t')
        }) {
            fail(malformedError)
        }
    }

    private fun validFieldString(value: String): Boolean {
        val codePoints = codePointCount(value) ?: return false
        return codePoints <= PlaylistBackupLimits.MAX_STRING_CODE_POINTS &&
            value.none { character ->
                character == '\r' ||
                    character == '\n' ||
                    (character.code < 0x20 && character != '\t')
            }
    }

    private fun codePointCount(value: String): Int? {
        var index = 0
        var count = 0
        while (index < value.length) {
            val character = value[index]
            if (character.isHighSurrogate()) {
                if (index + 1 >= value.length ||
                    !value[index + 1].isLowSurrogate()) {
                    return null
                }
                index += 2
            } else {
                if (character.isLowSurrogate()) return null
                index++
            }
            count++
        }
        return count
    }

    private fun forEachDocumentLine(
        text: String,
        malformedLineError: PlaylistInteroperabilityValidationError,
        consume: (String) -> Unit,
    ) {
        var lineStart = 0
        var index = 0
        while (index < text.length) {
            when (text[index]) {
                '\n' -> {
                    consumeLine(text, lineStart, index, consume)
                    lineStart = index + 1
                }

                '\r' -> {
                    if (index + 1 >= text.length || text[index + 1] != '\n') {
                        fail(malformedLineError)
                    }
                    consumeLine(text, lineStart, index, consume)
                    index++
                    lineStart = index + 1
                }
            }
            index++
        }
        if (lineStart < text.length) {
            consumeLine(text, lineStart, text.length, consume)
        }
    }

    private fun consumeLine(
        text: String,
        start: Int,
        end: Int,
        consume: (String) -> Unit,
    ) {
        val line = text.substring(start, end)
        val codePoints = codePointCount(line)
        if (codePoints == null ||
            codePoints > PlaylistInteroperabilityLimits.MAX_LINE_CODE_POINTS) {
            fail(PlaylistInteroperabilityValidationError.LINE_LIMIT_EXCEEDED)
        }
        consume(line)
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
                        first == 0xed && secondByte >= 0xa0) {
                        return null
                    }
                    result.append(
                        ((first and 0x0f) shl 12 or (second shl 6) or third)
                            .toChar(),
                    )
                    index += 3
                }

                first in 0xf0..0xf4 -> {
                    if (index + 3 >= bytes.size) return null
                    val secondByte = bytes[index + 1].toInt() and 0xff
                    val second = continuation(bytes[index + 1]) ?: return null
                    val third = continuation(bytes[index + 2]) ?: return null
                    val fourth = continuation(bytes[index + 3]) ?: return null
                    if (first == 0xf0 && secondByte < 0x90 ||
                        first == 0xf4 && secondByte >= 0x90) {
                        return null
                    }
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

    private fun invalid(error: PlaylistInteroperabilityValidationError) =
        PlaylistInteroperabilityDecodeResult.Invalid(error)

    private fun fail(error: PlaylistInteroperabilityValidationError): Nothing =
        throw InteroperabilityParseException(error)

    private enum class PlsField {
        FILE,
        TITLE,
        ARTIST,
        ALBUM,
        LENGTH,
    }

    private class M3uEntry(
        val durationSeconds: Int,
        val title: String,
        var artist: String? = null,
        var album: String? = null,
    )

    private class PlsEntryFields {
        var path: String? = null
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationSeconds: Int? = null

        fun has(field: PlsField): Boolean =
            when (field) {
                PlsField.FILE -> path != null
                PlsField.TITLE -> title != null
                PlsField.ARTIST -> artist != null
                PlsField.ALBUM -> album != null
                PlsField.LENGTH -> durationSeconds != null
            }
    }

    private class PlsFieldCounts {
        private var files = 0
        private var titles = 0
        private var artists = 0
        private var albums = 0
        private var lengths = 0

        fun increment(field: PlsField) {
            when (field) {
                PlsField.FILE -> files = increment(files)
                PlsField.TITLE -> titles = increment(titles)
                PlsField.ARTIST -> artists = increment(artists)
                PlsField.ALBUM -> albums = increment(albums)
                PlsField.LENGTH -> lengths = increment(lengths)
            }
        }

        private fun increment(count: Int): Int {
            if (count == PlaylistInteroperabilityLimits.MAX_PLS_ENTRY_FIELDS) {
                fail(
                    PlaylistInteroperabilityValidationError
                        .FIELD_LIMIT_EXCEEDED)
            }
            return count + 1
        }
    }

    private class BoundedUtf8Writer {
        private val builder = StringBuilder()
        private var utf8Bytes = 0

        fun append(value: String) {
            var index = 0
            while (index < value.length) {
                val character = value[index]
                val byteCount =
                    when {
                        character.code <= 0x7f -> 1
                        character.code <= 0x7ff -> 2
                        character.isHighSurrogate() -> {
                            require(
                                index + 1 < value.length &&
                                    value[index + 1].isLowSurrogate(),
                            )
                            appendCharacter(character, 4)
                            index++
                            appendCharacter(value[index], 0)
                            index++
                            continue
                        }

                        character.isLowSurrogate() ->
                            throw IllegalArgumentException("Unpaired surrogate")

                        else -> 3
                    }
                appendCharacter(character, byteCount)
                index++
            }
        }

        fun append(value: Int) = append(value.toString())

        fun append(value: Char) {
            require(!value.isHighSurrogate() && !value.isLowSurrogate())
            appendCharacter(value, if (value.code <= 0x7f) 1 else 3)
        }

        fun newLine() = append('\n')

        fun toByteArray(): ByteArray = builder.toString().encodeToByteArray()

        private fun appendCharacter(character: Char, byteCount: Int) {
            if (byteCount > 0) {
                require(
                    utf8Bytes <= PlaylistBackupLimits.MAX_BYTES - byteCount) {
                        "Encoded playlist interoperability document exceeds 4 MiB"
                    }
                utf8Bytes += byteCount
            }
            builder.append(character)
        }
    }

    private class InteroperabilityParseException(
        val error: PlaylistInteroperabilityValidationError,
    ) : Exception()
}
