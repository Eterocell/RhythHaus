package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistInteroperabilityServiceTest {
    @Test
    fun importPlanUsesExistingMatcherAndReportsUnmatchedEntries() {
        val track = track("one", "Song", 10_000)
        val document =
            PlaylistInteroperabilityDocument(
                name = "External",
                format = PlaylistInteroperabilityFormat.M3U,
                entries =
                    listOf(
                        PlaylistInteroperabilityEntry(
                            "Song", "Artist", "Album", 10, "/one.mp3"),
                        PlaylistInteroperabilityEntry(
                            "Missing", "Artist", "Album", 10, "/missing.mp3"),
                    ),
            )

        val plan = planPlaylistInteroperabilityImport(document, listOf(track))

        assertEquals("External", plan.name)
        assertEquals(listOf("one"), plan.trackIds)
        assertEquals(1, plan.unmatched)
        assertEquals(0, plan.ambiguous)
    }

    @Test
    fun formatDetectionAcceptsOnlyValidSupportedDocuments() {
        val bytes =
            PlaylistInteroperabilityCodec.encode(
                PlaylistInteroperabilityDocument(
                    name = "Road trip",
                    format = PlaylistInteroperabilityFormat.PLS,
                    entries =
                        listOf(
                            PlaylistInteroperabilityEntry(
                                "Song", "Artist", "Album", 10, "/one.mp3"),
                        ),
                ),
            )

        assertEquals(
            PlaylistInteroperabilityFormat.PLS,
            detectPlaylistInteroperabilityFormat(bytes))
        assertEquals(
            null,
            detectPlaylistInteroperabilityFormat(
                "not a playlist".encodeToByteArray()))
    }

    private fun track(id: String, title: String, durationMillis: Long) =
        LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = "$title.mp3",
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = durationMillis,
            sizeBytes = 1,
            modifiedAtEpochMillis = 1,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )
}
