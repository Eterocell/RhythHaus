package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PlaylistBackupCodecTest {
    private val duplicateEntry = PlaylistBackupEntry(
        title = "A \"quoted\" title\n",
        artist = "Bjork \uD83C\uDFB5",
        album = "tab\tand\\slash",
        durationSeconds = 245,
    )

    @Test
    fun crc32MatchesIeeeKnownVector() {
        assertEquals("cbf43926", Crc32.hex("123456789".encodeToByteArray()))
    }

    @Test
    fun encodeProducesExactCanonicalCompactUtf8Bytes() {
        val payload = PlaylistBackupPayload(
            exportedAtEpochMillis = 1_725_000_000_123,
            playlists = listOf(
                PlaylistBackupPlaylist("Mix", listOf(duplicateEntry, duplicateEntry)),
            ),
        )

        val payloadJson = "{\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":1725000000123," +
            "\"playlists\":[{\"name\":\"Mix\",\"entries\":[" +
            "{\"title\":\"A \\\"quoted\\\" title\\n\",\"artist\":\"Bjork \uD83C\uDFB5\",\"album\":\"tab\\tand\\\\slash\",\"durationSeconds\":245}," +
            "{\"title\":\"A \\\"quoted\\\" title\\n\",\"artist\":\"Bjork \uD83C\uDFB5\",\"album\":\"tab\\tand\\\\slash\",\"durationSeconds\":245}]}]}"
        val expected = payloadJson.dropLast(1) + ",\"checksumCrc32\":\"${Crc32.hex(payloadJson.encodeToByteArray())}\"}"

        assertContentEquals(expected.encodeToByteArray(), PlaylistBackupCodec.encode(payload))
    }

    @Test
    fun roundTripPreservesPlaylistOrderEntryOrderAndDuplicates() {
        val payload = PlaylistBackupPayload(
            exportedAtEpochMillis = Long.MAX_VALUE,
            playlists = listOf(
                PlaylistBackupPlaylist("Second", listOf(duplicateEntry, duplicateEntry)),
                PlaylistBackupPlaylist("First", emptyList()),
            ),
        )

        val document = assertIs<PlaylistBackupDecodeResult.Success>(
            PlaylistBackupCodec.decode(PlaylistBackupCodec.encode(payload)),
        ).document

        assertEquals("rhythhaus-playlist-backup", document.format)
        assertEquals(1, document.version)
        assertEquals(payload.exportedAtEpochMillis, document.exportedAtEpochMillis)
        assertEquals(payload.playlists, document.playlists)
        assertEquals(duplicateEntry, document.playlists.first().entries[1])
    }

    @Test
    fun malformedUtf8IsRejected() {
        assertInvalid(byteArrayOf(0xC3.toByte(), 0x28), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0x80.toByte()), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(byteArrayOf(0xC0.toByte(), 0xAF.toByte()), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(byteArrayOf(0xE2.toByte(), 0x82.toByte()), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(byteArrayOf(0x80.toByte()), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(byteArrayOf(0xE2.toByte(), 0x28, 0xA1.toByte()), PlaylistBackupValidationError.MALFORMED_UTF8)
        assertInvalid(
            byteArrayOf(0xF4.toByte(), 0x90.toByte(), 0x80.toByte(), 0x80.toByte()),
            PlaylistBackupValidationError.MALFORMED_UTF8,
        )
    }

    @Test
    fun malformedJsonAndUnpairedEscapedSurrogatesAreRejected() {
        assertInvalid("{".encodeToByteArray(), PlaylistBackupValidationError.MALFORMED_JSON)
        assertInvalid(document(entryTitle = "\\uD800"), PlaylistBackupValidationError.MALFORMED_JSON)
        assertInvalid(document(entryTitle = "\\uDC00"), PlaylistBackupValidationError.MALFORMED_JSON)
    }

    @Test
    fun duplicateFieldsAreRejectedAtEveryObjectLevel() {
        assertInvalid(
            checksummed("{\"format\":\"rhythhaus-playlist-backup\",\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[]}"),
            PlaylistBackupValidationError.DUPLICATE_FIELD,
        )
        assertInvalid(document(playlistPrefix = "\"name\":\"Again\","), PlaylistBackupValidationError.DUPLICATE_FIELD)
        assertInvalid(document(entryPrefix = "\"title\":\"Again\","), PlaylistBackupValidationError.DUPLICATE_FIELD)
    }

    @Test
    fun unknownFieldsAreRejectedAtEveryObjectLevel() {
        assertInvalid(document(rootPrefix = "\"extra\":0,"), PlaylistBackupValidationError.UNKNOWN_FIELD)
        assertInvalid(document(playlistPrefix = "\"extra\":0,"), PlaylistBackupValidationError.UNKNOWN_FIELD)
        assertInvalid(document(entryPrefix = "\"extra\":0,"), PlaylistBackupValidationError.UNKNOWN_FIELD)
    }

    @Test
    fun missingFieldsAreRejectedAtEveryObjectLevel() {
        assertInvalid(checksummed("{\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[]}"), PlaylistBackupValidationError.MISSING_FIELD)
        assertInvalid(
            checksummed("{\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[{\"entries\":[]}]}"),
            PlaylistBackupValidationError.MISSING_FIELD,
        )
        assertInvalid(
            checksummed("{\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[{\"name\":\"P\",\"entries\":[{\"artist\":\"A\",\"album\":\"B\",\"durationSeconds\":1}]}]}"),
            PlaylistBackupValidationError.MISSING_FIELD,
        )
    }

    @Test
    fun unsupportedFormatVersionAndBadChecksumAreTypedFailures() {
        assertInvalid(document(format = "other"), PlaylistBackupValidationError.UNSUPPORTED_FORMAT)
        assertInvalid(document(version = "2"), PlaylistBackupValidationError.UNSUPPORTED_VERSION)
        assertInvalid(document().decodeToString().replace(Regex("[0-9a-f]{8}(?=\"})"), "00000000").encodeToByteArray(), PlaylistBackupValidationError.INVALID_CHECKSUM)
        assertInvalid(document().decodeToString().replace(Regex("[0-9a-f]{8}(?=\"})"), "ABCDEF12").encodeToByteArray(), PlaylistBackupValidationError.INVALID_CHECKSUM)
        assertInvalid(withChecksum("1234567"), PlaylistBackupValidationError.INVALID_CHECKSUM)
        assertInvalid(withChecksum("gggggggg"), PlaylistBackupValidationError.INVALID_CHECKSUM)
        assertInvalid(withChecksum("1234567-"), PlaylistBackupValidationError.INVALID_CHECKSUM)
    }

    @Test
    fun numbersMustBePlainIntegersWithinLongRange() {
        listOf("1.0", "1e0", "+1", "01").forEach {
            assertInvalid(document(version = it), PlaylistBackupValidationError.INVALID_INTEGER)
        }
        assertInvalid(document(version = "١"), PlaylistBackupValidationError.INVALID_INTEGER)
        assertInvalid(document(version = "1١"), PlaylistBackupValidationError.INVALID_INTEGER)
        assertInvalid(document(exportedAt = "9223372036854775808"), PlaylistBackupValidationError.NUMERIC_OVERFLOW)
        assertInvalid(document(duration = "999999999999999999999"), PlaylistBackupValidationError.NUMERIC_OVERFLOW)
    }

    @Test
    fun trailingContentIsRejected() {
        assertInvalid(document() + " true".encodeToByteArray(), PlaylistBackupValidationError.TRAILING_CONTENT)
    }

    @Test
    fun nonCanonicalKeyOrderAndWhitespaceAreRejected() {
        assertInvalid(
            checksummed("{\"version\":1,\"format\":\"rhythhaus-playlist-backup\",\"exportedAtEpochMillis\":0,\"playlists\":[]}"),
            PlaylistBackupValidationError.NON_CANONICAL_JSON,
        )
        assertInvalid(
            checksummed("{\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[{\"entries\":[],\"name\":\"P\"}]}"),
            PlaylistBackupValidationError.NON_CANONICAL_JSON,
        )
        assertInvalid(
            checksummed("{\"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[{\"name\":\"P\",\"entries\":[{\"artist\":\"A\",\"title\":\"T\",\"album\":\"B\",\"durationSeconds\":1}]}]}"),
            PlaylistBackupValidationError.NON_CANONICAL_JSON,
        )
        assertInvalid(
            checksummed("{ \"format\":\"rhythhaus-playlist-backup\",\"version\":1,\"exportedAtEpochMillis\":0,\"playlists\":[]}"),
            PlaylistBackupValidationError.NON_CANONICAL_JSON,
        )
    }

    @Test
    fun alternateJsonEscapesAreRejectedThroughPublicDecode() {
        listOf(
            "T\\/X",
            "\\u0054",
            "\\u005A",
            "\\u0041",
            "\\u0061",
        ).forEach { encodedTitle ->
            assertInvalid(document(entryTitle = encodedTitle), PlaylistBackupValidationError.NON_CANONICAL_JSON)
        }
    }

    @Test
    fun nonAsciiUnicodeEscapeDigitsAreMalformedJsonThroughPublicDecode() {
        listOf(
            "\\u٠٠٤١",
            "\\u00４1",
            "\\u0٠4١",
        ).forEach { encodedTitle ->
            assertInvalid(document(entryTitle = encodedTitle), PlaylistBackupValidationError.MALFORMED_JSON)
        }
    }

    @Test
    fun blankPlaylistNamesAreRejected() {
        listOf("", " ", "\\t\\n").forEach {
            assertInvalid(document(playlistName = it), PlaylistBackupValidationError.BLANK_PLAYLIST_NAME)
        }
    }

    @Test
    fun durationBoundsAreInclusiveAndExactLimitPlusOneIsRejected() {
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(document(duration = "0")))
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(document(duration = "604800")))
        assertInvalid(document(duration = "-1"), PlaylistBackupValidationError.INVALID_DURATION)
        assertInvalid(document(duration = "604801"), PlaylistBackupValidationError.INVALID_DURATION)
    }

    @Test
    fun stringCodePointLimitHandlesSupplementaryCharactersAndRejectsPlusOne() {
        val exact = "\uD83C\uDFB5".repeat(1_024)
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(document(entryTitle = exact)))
        assertInvalid(document(entryTitle = exact + "x"), PlaylistBackupValidationError.STRING_LIMIT_EXCEEDED)
    }

    @Test
    fun playlistCountLimitAcceptsExactAndRejectsPlusOne() {
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(document(playlists = playlists(1_000))))
        assertInvalid(document(playlists = playlists(1_001)), PlaylistBackupValidationError.PLAYLIST_LIMIT_EXCEEDED)
    }

    @Test
    fun perPlaylistEntryLimitAcceptsExactAndRejectsPlusOne() {
        assertIs<PlaylistBackupDecodeResult.Success>(PlaylistBackupCodec.decode(document(entries = entries(10_000))))
        assertInvalid(document(entries = entries(10_001)), PlaylistBackupValidationError.PLAYLIST_ENTRY_LIMIT_EXCEEDED)
    }

    @Test
    fun totalEntryLimitAcceptsExactAndRejectsPlusOne() {
        val exact = checksummed(documentText(playlists = playlists(10, 10_000))).decodeToString()
        assertEquals(100_000, StrictJsonParser(exact).parse().document.playlists.sumOf { it.entries.size })
        assertEquals(
            PlaylistBackupValidationError.TOTAL_ENTRY_LIMIT_EXCEEDED,
            assertFailsWith<BackupParseException> {
                StrictJsonParser(checksummed(documentText(playlists = playlists(11, 9_091))).decodeToString()).parse()
            }.error,
        )
    }

    @Test
    fun byteLimitIsCheckedBeforeParsing() {
        val exact = ByteArray(4 * 1024 * 1024) { ' '.code.toByte() }
        assertInvalid(exact, PlaylistBackupValidationError.MALFORMED_JSON)
        assertInvalid(exact + 0, PlaylistBackupValidationError.INPUT_TOO_LARGE)
    }

    @Test
    fun encodeRejectsStructurallyValidPayloadBeyondByteLimit() {
        val maximumString = "x".repeat(1_024)
        val oversized = PlaylistBackupPayload(
            exportedAtEpochMillis = 0,
            playlists = listOf(
                PlaylistBackupPlaylist(
                    name = "P",
                    entries = List(1_400) {
                        PlaylistBackupEntry(maximumString, maximumString, maximumString, 1)
                    },
                ),
            ),
        )

        assertFailsWith<IllegalArgumentException> { PlaylistBackupCodec.encode(oversized) }
    }

    @Test
    fun encodeRejectsBlankNamesAndUnpairedKotlinSurrogates() {
        listOf("", " ", "\t\n").forEach { name ->
            assertEncodeInvalid(playlist(name = name))
        }
        listOf("\uD800", "\uDC00").forEach { malformed ->
            assertEncodeInvalid(playlist(name = malformed))
            assertEncodeInvalid(playlist(entry = entry(title = malformed)))
        }
    }

    @Test
    fun encodeEnforcesStringCodePointBounds() {
        val exact = "\uD83C\uDFB5".repeat(1_024)

        PlaylistBackupCodec.encode(PlaylistBackupPayload(0, listOf(playlist(name = exact))))
        PlaylistBackupCodec.encode(PlaylistBackupPayload(0, listOf(playlist(entry = entry(title = exact)))))
        assertEncodeInvalid(playlist(name = exact + "x"))
        assertEncodeInvalid(playlist(entry = entry(title = exact + "x")))
    }

    @Test
    fun encodeEnforcesPlaylistAndPerPlaylistEntryBounds() {
        PlaylistBackupCodec.encode(
            PlaylistBackupPayload(0, List(1_000) { PlaylistBackupPlaylist("P", emptyList()) }),
        )
        assertEncodeInvalid(List(1_001) { PlaylistBackupPlaylist("P", emptyList()) })

        val exactEntries = List(10_000) { entry(title = "") }
        PlaylistBackupCodec.encode(PlaylistBackupPayload(0, listOf(PlaylistBackupPlaylist("P", exactEntries))))
        assertEncodeInvalid(PlaylistBackupPlaylist("P", exactEntries + entry(title = "")))
    }

    @Test
    fun encodeEnforcesReachableTotalEntryBranches() {
        val tenThousand = List(10_000) { entry(title = "") }
        val exactTotal = List(10) { PlaylistBackupPlaylist("P", tenThousand) }
        assertFailsWith<IllegalArgumentException> {
            PlaylistBackupCodec.encode(PlaylistBackupPayload(0, exactTotal))
        }
        assertEncodeInvalid(exactTotal + PlaylistBackupPlaylist("P", listOf(entry(title = ""))))
    }

    @Test
    fun encodeEnforcesDurationBounds() {
        PlaylistBackupCodec.encode(PlaylistBackupPayload(0, listOf(playlist(entry = entry(duration = 0)))))
        PlaylistBackupCodec.encode(PlaylistBackupPayload(0, listOf(playlist(entry = entry(duration = 604_800)))))
        assertEncodeInvalid(playlist(entry = entry(duration = -1)))
        assertEncodeInvalid(playlist(entry = entry(duration = 604_801)))
    }

    private fun document(
        format: String = "rhythhaus-playlist-backup",
        version: String = "1",
        exportedAt: String = "0",
        playlists: String? = null,
        playlistName: String = "P",
        entries: String? = null,
        duration: String = "1",
        entryTitle: String = "T",
        rootPrefix: String = "",
        playlistPrefix: String = "",
        entryPrefix: String = "",
    ): ByteArray = checksummed(
        documentText(
            format = format,
            version = version,
            exportedAt = exportedAt,
            playlists = playlists,
            playlistName = playlistName,
            entries = entries,
            duration = duration,
            entryTitle = entryTitle,
            rootPrefix = rootPrefix,
            playlistPrefix = playlistPrefix,
            entryPrefix = entryPrefix,
        ),
    )

    private fun documentText(
        format: String = "rhythhaus-playlist-backup",
        version: String = "1",
        exportedAt: String = "0",
        playlists: String? = null,
        playlistName: String = "P",
        entries: String? = null,
        duration: String = "1",
        entryTitle: String = "T",
        rootPrefix: String = "",
        playlistPrefix: String = "",
        entryPrefix: String = "",
    ): String {
        val encodedEntries = entries ?: "[{${entryPrefix}\"title\":\"$entryTitle\",\"artist\":\"A\",\"album\":\"B\",\"durationSeconds\":$duration}]"
        val encodedPlaylists = playlists ?: "[{${playlistPrefix}\"name\":\"$playlistName\",\"entries\":$encodedEntries}]"
        return "{${rootPrefix}\"format\":\"$format\",\"version\":$version,\"exportedAtEpochMillis\":$exportedAt,\"playlists\":$encodedPlaylists}"
    }

    private fun entries(count: Int): String = List(count) {
        "{\"title\":\"T\",\"artist\":\"A\",\"album\":\"B\",\"durationSeconds\":1}"
    }.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun playlists(count: Int, entryCount: Int = 0): String {
        val encodedEntries = entries(entryCount)
        return List(count) { "{\"name\":\"P\",\"entries\":$encodedEntries}" }
            .joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    private fun checksummed(payloadJson: String): ByteArray {
        val checksum = Crc32.hex(payloadJson.encodeToByteArray())
        return (payloadJson.dropLast(1) + ",\"checksumCrc32\":\"$checksum\"}").encodeToByteArray()
    }

    private fun withChecksum(checksum: String): ByteArray {
        val payload = documentText()
        return (payload.dropLast(1) + ",\"checksumCrc32\":\"$checksum\"}").encodeToByteArray()
    }

    private fun entry(title: String = "T", duration: Int = 1): PlaylistBackupEntry = PlaylistBackupEntry(title, "A", "B", duration)

    private fun playlist(
        name: String = "P",
        entry: PlaylistBackupEntry = entry(),
    ): PlaylistBackupPlaylist = PlaylistBackupPlaylist(name, listOf(entry))

    private fun assertEncodeInvalid(playlist: PlaylistBackupPlaylist) {
        assertEncodeInvalid(listOf(playlist))
    }

    private fun assertEncodeInvalid(playlists: List<PlaylistBackupPlaylist>) {
        assertFailsWith<IllegalArgumentException> {
            PlaylistBackupCodec.encode(PlaylistBackupPayload(0, playlists))
        }
    }

    private fun assertInvalid(bytes: ByteArray, expected: PlaylistBackupValidationError) {
        assertEquals(
            PlaylistBackupDecodeResult.Invalid(expected),
            PlaylistBackupCodec.decode(bytes),
        )
    }
}
