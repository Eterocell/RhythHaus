package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaylistBackupMatcherTest {
    @Test
    fun normalizationTrimsAndCollapsesUnicodeWhitespaceWithoutChangingPortableCharacters() {
        assertEquals(
            "héllo, ｗorld!",
            normalizePortableText("\u2003HÉLLO,\u00a0\tｗORLD!\u3000"),
        )
        assertEquals("i", normalizePortableText("I"))
        assertEquals("i\u0307", normalizePortableText("İ"))
    }

    @Test
    fun normalizationLowercasesSupplementaryUnicodeCodePoints() {
        assertEquals("\uD801\uDC28", normalizePortableText("\uD801\uDC00"))
    }

    @Test
    fun matcherMatchesSupplementaryUppercaseAndLowercaseForms() {
        val matcher = PlaylistBackupMatcher(
            listOf(track("deseret", title = "\uD801\uDC00", durationMillis = 100_000)),
        )

        assertEquals(
            PlaylistBackupMatch.Unique("deseret"),
            matcher.match(entry(title = "\uD801\uDC28")),
        )
    }

    @Test
    fun durationToleranceIsInclusiveAndUnknownDestinationDurationNeverMatches() {
        val matcher = PlaylistBackupMatcher(
            listOf(
                track("minus-two", durationMillis = 98_000),
                track("plus-two", durationMillis = 102_999),
                track("outside", durationMillis = 103_000),
                track("unknown", durationMillis = null),
            ),
        )

        val result = matcher.match(entry(durationSeconds = 100))

        assertEquals(listOf("minus-two", "plus-two"), assertIs<PlaylistBackupMatch.Ambiguous>(result).trackIds)
    }

    @Test
    fun matcherReturnsUniqueUnmatchedAndAmbiguousWithoutChoosingFirstCandidate() {
        val matcher = PlaylistBackupMatcher(
            listOf(
                track("unique", title = " One ", durationMillis = 100_000),
                track("duplicate-a", title = "Two", durationMillis = 100_000),
                track("duplicate-b", title = "TWO", durationMillis = 101_000),
                track("punctuation", title = "One!", durationMillis = 100_000),
            ),
        )

        assertEquals(PlaylistBackupMatch.Unique("unique"), matcher.match(entry(title = "one")))
        assertEquals(PlaylistBackupMatch.Unmatched, matcher.match(entry(title = "missing")))
        assertEquals(
            listOf("duplicate-a", "duplicate-b"),
            assertIs<PlaylistBackupMatch.Ambiguous>(matcher.match(entry(title = "two"))).trackIds,
        )
    }

    @Test
    fun matcherRequiresExactNormalizedArtistAndAlbum() {
        val matcher = PlaylistBackupMatcher(listOf(track("exact", durationMillis = 100_000)))

        assertEquals(PlaylistBackupMatch.Unmatched, matcher.match(entry(artist = "Other Artist")))
        assertEquals(PlaylistBackupMatch.Unmatched, matcher.match(entry(album = "Other Album")))
    }

    private fun entry(
        title: String = "Title",
        artist: String = "Artist",
        album: String = "Album",
        durationSeconds: Int = 100,
    ) = PlaylistBackupEntry(title, artist, album, durationSeconds)

    private fun track(
        id: String,
        title: String = "Title",
        durationMillis: Long?,
    ) = LibraryTrack(
        id = id,
        sourceId = "source",
        sourceLocalKey = id,
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = "$id.mp3",
        title = title,
        artist = "Artist",
        album = "Album",
        durationMillis = durationMillis,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
