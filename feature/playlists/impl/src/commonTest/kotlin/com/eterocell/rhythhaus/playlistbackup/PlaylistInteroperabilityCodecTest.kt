package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PlaylistInteroperabilityCodecTest {
    private val entry =
        PlaylistInteroperabilityEntry(
            title = "Declared Title",
            artist = "Björk",
            album = "Début",
            durationSeconds = 245,
            path = "/untrusted/location/other-track.flac",
        )

    private val document =
        PlaylistInteroperabilityDocument(
            name = "Road Trip",
            entries = listOf(entry, entry),
            format = PlaylistInteroperabilityFormat.M3U8,
        )

    @Test
    fun m3uDecodingPreservesCrLfCommentsUnicodeOrderDuplicatesAndPathData() {
        val decoded =
            decode(
                PlaylistInteroperabilityFormat.M3U,
                listOf(
                        "#EXTM3U",
                        "# a comment between records",
                        "#PLAYLIST:旅行",
                        "#EXTINF:245,Declared Title",
                        "#EXTART:Björk",
                        "#EXTALB:Début",
                        "/untrusted/location/other-track.flac",
                        "#EXTINF:245,Declared Title",
                        "#EXTART:Björk",
                        "#EXTALB:Début",
                        "/untrusted/location/other-track.flac",
                    )
                    .joinToString(separator = "\r\n", postfix = "\r\n")
                    .encodeToByteArray(),
            )

        assertEquals(PlaylistInteroperabilityFormat.M3U, decoded.format)
        assertEquals("旅行", decoded.name)
        assertEquals(listOf(entry, entry), decoded.entries)
        assertEquals("Declared Title", decoded.entries.first().title)
        assertEquals(
            "/untrusted/location/other-track.flac",
            decoded.entries.first().path)
    }

    @Test
    fun m3u8EncodingUsesCanonicalUtf8ExtendedRecordsAndRoundTrips() {
        val encoded =
            PlaylistInteroperabilityCodec.encode(
                document,
                PlaylistInteroperabilityFormat.M3U8,
            )

        val expected =
            listOf(
                    "#EXTM3U",
                    "#PLAYLIST:Road Trip",
                    "#EXTINF:245,Declared Title",
                    "#EXTART:Björk",
                    "#EXTALB:Début",
                    "/untrusted/location/other-track.flac",
                    "#EXTINF:245,Declared Title",
                    "#EXTART:Björk",
                    "#EXTALB:Début",
                    "/untrusted/location/other-track.flac",
                )
                .joinToString(separator = "\n", postfix = "\n")
                .encodeToByteArray()

        assertContentEquals(expected, encoded)
        assertEquals(
            document, decode(PlaylistInteroperabilityFormat.M3U8, encoded))
    }

    @Test
    fun plsDecodingPreservesCommentsUnicodeOrderDuplicatesAndPathData() {
        val decoded =
            decode(
                PlaylistInteroperabilityFormat.PLS,
                listOf(
                        "; a comment before the playlist section",
                        "[playlist]",
                        "PlaylistName=旅行",
                        "File1=/untrusted/location/other-track.flac",
                        "Title1=Declared Title",
                        "Artist1=Björk",
                        "Album1=Début",
                        "Length1=245",
                        "File2=/untrusted/location/other-track.flac",
                        "Title2=Declared Title",
                        "Artist2=Björk",
                        "Album2=Début",
                        "Length2=245",
                        "NumberOfEntries=2",
                        "Version=2",
                    )
                    .joinToString(separator = "\r\n", postfix = "\r\n")
                    .encodeToByteArray(),
            )

        assertEquals(PlaylistInteroperabilityFormat.PLS, decoded.format)
        assertEquals("旅行", decoded.name)
        assertEquals(listOf(entry, entry), decoded.entries)
        assertEquals("Declared Title", decoded.entries.first().title)
        assertEquals(
            "/untrusted/location/other-track.flac",
            decoded.entries.first().path)
    }

    @Test
    fun plsEncodingUsesCanonicalIndexedRecordsAndRoundTrips() {
        val encoded =
            PlaylistInteroperabilityCodec.encode(
                document,
                PlaylistInteroperabilityFormat.PLS,
            )

        val expected =
            listOf(
                    "[playlist]",
                    "PlaylistName=Road Trip",
                    "File1=/untrusted/location/other-track.flac",
                    "Title1=Declared Title",
                    "Artist1=Björk",
                    "Album1=Début",
                    "Length1=245",
                    "File2=/untrusted/location/other-track.flac",
                    "Title2=Declared Title",
                    "Artist2=Björk",
                    "Album2=Début",
                    "Length2=245",
                    "NumberOfEntries=2",
                    "Version=2",
                )
                .joinToString(separator = "\n", postfix = "\n")
                .encodeToByteArray()

        assertContentEquals(expected, encoded)
        assertEquals(
            document.copy(format = PlaylistInteroperabilityFormat.PLS),
            decode(PlaylistInteroperabilityFormat.PLS, encoded),
        )
    }

    @Test
    fun malformedUtf8AndFormatSpecificGrammarReturnTypedFailures() {
        assertInvalid(
            bytes = byteArrayOf(0xc3.toByte(), 0x28),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.MALFORMED_UTF8,
        )
        assertInvalid(
            bytes =
                m3u(
                    "#EXTM3U",
                    "#EXTINF:245,Declared Title",
                ),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.MALFORMED_M3U,
        )
        assertInvalid(
            bytes =
                pls(
                    "[playlist]",
                    "File1=/track.flac",
                    "Title1=Title",
                    "Length1=245",
                    "NumberOfEntries=1",
                    "Version=3",
                ),
            format = PlaylistInteroperabilityFormat.PLS,
            expected =
                PlaylistInteroperabilityValidationError.UNSUPPORTED_VERSION,
        )
    }

    @Test
    fun integersAndDurationsAreBoundedBeforeCreatingEntries() {
        assertInvalid(
            bytes = m3u("#EXTM3U", "#EXTINF:01,Title", "/track.flac"),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.INVALID_INTEGER,
        )
        assertInvalid(
            bytes =
                m3u(
                    "#EXTM3U",
                    "#EXTINF:999999999999999999999,Title",
                    "/track.flac",
                ),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.NUMERIC_OVERFLOW,
        )
        assertInvalid(
            bytes =
                m3u(
                    "#EXTM3U",
                    "#EXTINF:${PlaylistBackupLimits.MAX_DURATION_SECONDS + 1},Title",
                    "/track.flac",
                ),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.INVALID_DURATION,
        )
        assertInvalid(
            bytes =
                pls(
                    "[playlist]",
                    "NumberOfEntries=${PlaylistBackupLimits.MAX_ENTRIES_PER_PLAYLIST + 1}",
                    "Version=2",
                ),
            format = PlaylistInteroperabilityFormat.PLS,
            expected =
                PlaylistInteroperabilityValidationError.ENTRY_LIMIT_EXCEEDED,
        )
    }

    @Test
    fun decoderEnforcesByteLineStringEntryAndFieldLimits() {
        assertInvalid(
            bytes = ByteArray(PlaylistBackupLimits.MAX_BYTES + 1),
            format = PlaylistInteroperabilityFormat.M3U,
            expected = PlaylistInteroperabilityValidationError.INPUT_TOO_LARGE,
        )
        assertInvalid(
            bytes =
                m3u(
                    "#EXTM3U",
                    "#" +
                        "x"
                            .repeat(
                                PlaylistInteroperabilityLimits
                                    .MAX_LINE_CODE_POINTS + 1),
                ),
            format = PlaylistInteroperabilityFormat.M3U,
            expected =
                PlaylistInteroperabilityValidationError.LINE_LIMIT_EXCEEDED,
        )
        assertInvalid(
            bytes =
                m3u(
                    "#EXTM3U",
                    "#EXTINF:1," +
                        "x"
                            .repeat(
                                PlaylistBackupLimits.MAX_STRING_CODE_POINTS +
                                    1),
                    "/track.flac",
                ),
            format = PlaylistInteroperabilityFormat.M3U,
            expected =
                PlaylistInteroperabilityValidationError.STRING_LIMIT_EXCEEDED,
        )
        assertInvalid(
            bytes = buildString {
                    append("#EXTM3U\n")
                    repeat(PlaylistBackupLimits.MAX_ENTRIES_PER_PLAYLIST + 1) {
                        append("#EXTINF:0,\n/path\n")
                    }
                }
                    .encodeToByteArray(),
            format = PlaylistInteroperabilityFormat.M3U,
            expected =
                PlaylistInteroperabilityValidationError.ENTRY_LIMIT_EXCEEDED,
        )
        assertInvalid(
            bytes = buildString {
                    append("[playlist]\n")
                    repeat(
                        PlaylistInteroperabilityLimits.MAX_PLS_ENTRY_FIELDS +
                            1) { index ->
                            append("File${index + 1}=/track.flac\n")
                        }
                }
                    .encodeToByteArray(),
            format = PlaylistInteroperabilityFormat.PLS,
            expected =
                PlaylistInteroperabilityValidationError.FIELD_LIMIT_EXCEEDED,
        )
    }

    @Test
    fun encodingRejectsUnrepresentableOrOutOfBoundsValues() {
        assertFailsWith<IllegalArgumentException> {
            PlaylistInteroperabilityCodec.encode(
                document.copy(entries = listOf(entry.copy(path = ""))),
                PlaylistInteroperabilityFormat.M3U,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlaylistInteroperabilityCodec.encode(
                document.copy(
                    entries = listOf(entry.copy(title = "line\nbreak"))),
                PlaylistInteroperabilityFormat.PLS,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlaylistInteroperabilityCodec.encode(
                document.copy(
                    entries =
                        listOf(
                            entry.copy(
                                durationSeconds =
                                    PlaylistBackupLimits.MAX_DURATION_SECONDS +
                                        1),
                        ),
                ),
                PlaylistInteroperabilityFormat.M3U8,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlaylistInteroperabilityCodec.encode(
                document.copy(
                    entries =
                        List(
                            PlaylistBackupLimits.MAX_ENTRIES_PER_PLAYLIST + 1,
                        ) {
                            entry
                        },
                ),
                PlaylistInteroperabilityFormat.PLS,
            )
        }
    }

    @Test
    fun encodingValidatesPathsForRequestedOutputFormat() {
        val commentPath = entry.copy(path = "#intro.mp3")

        assertFailsWith<IllegalArgumentException> {
            PlaylistInteroperabilityCodec.encode(
                document.copy(
                    format = PlaylistInteroperabilityFormat.PLS,
                    entries = listOf(commentPath),
                ),
                PlaylistInteroperabilityFormat.M3U,
            )
        }

        assertEquals(
            "#intro.mp3",
            decode(
                    PlaylistInteroperabilityFormat.PLS,
                    PlaylistInteroperabilityCodec.encode(
                        document.copy(entries = listOf(commentPath)),
                        PlaylistInteroperabilityFormat.PLS,
                    ),
                )
                .entries
                .single()
                .path,
        )
    }

    private fun decode(
        format: PlaylistInteroperabilityFormat,
        bytes: ByteArray,
    ): PlaylistInteroperabilityDocument =
        assertIs<PlaylistInteroperabilityDecodeResult.Success>(
                PlaylistInteroperabilityCodec.decode(bytes, format),
            )
            .document

    private fun m3u(vararg lines: String): ByteArray =
        lines.joinToString(separator = "\n", postfix = "\n").encodeToByteArray()

    private fun pls(vararg lines: String): ByteArray =
        lines.joinToString(separator = "\n", postfix = "\n").encodeToByteArray()

    private fun assertInvalid(
        bytes: ByteArray,
        format: PlaylistInteroperabilityFormat,
        expected: PlaylistInteroperabilityValidationError,
    ) {
        assertEquals(
            PlaylistInteroperabilityDecodeResult.Invalid(expected),
            PlaylistInteroperabilityCodec.decode(bytes, format),
        )
    }
}
