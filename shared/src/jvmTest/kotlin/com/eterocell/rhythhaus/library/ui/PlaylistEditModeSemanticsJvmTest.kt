package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistEditModeSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun defaultPlaylistRowMatchesTrackContentAndHidesMutationActions() = runComposeUiTest {
        var playCount = 0
        var selectedOccurrence: String? = null
        setContent {
            PlaylistDetailScreen(
                playlist = playlist("playlist-1", "Saved"),
                entries = listOf(entry("entry-a", "track-a", 0)),
                libraryTracks = listOf(libraryTrack("track-a", title = "Song A", artist = "Artist A", album = "Album A")),
                state = PlaylistState(),
                onBack = {}, onRetry = {}, onRename = { _, _ -> }, onDelete = {}, onOpenBrowser = {},
                onPlayEntry = { playCount++; selectedOccurrence = it.selectedOccurrenceId },
                onRemoveEntry = {}, onReorder = {}, bottomContentPadding = 0.dp,
            )
        }

        onNode(hasContentDescription("Song A, Artist A, Album A, 3:12"), useUnmergedTree = true).assertExists().performClick()
        assertEquals(1, playCount)
        assertEquals("entry-a", selectedOccurrence)
        onNode(hasContentDescription("Move up Song A")).assertDoesNotExist()
        onNode(hasContentDescription("Move down Song A")).assertDoesNotExist()
        onNode(hasContentDescription("Remove Song A")).assertDoesNotExist()
    }

    private fun playlist(id: String, name: String) = Playlist(id, name, 1L, 1L)

    private fun entry(id: String, trackId: String, position: Int) = PlaylistEntry(id, "playlist-1", trackId, position, 1L)

    private fun libraryTrack(id: String, title: String, artist: String, album: String) = LibraryTrack(
        id = id,
        sourceId = "source-1",
        sourceLocalKey = "$id.mp3",
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = title,
        title = title,
        artist = artist,
        album = album,
        durationMillis = 192_000L,
        sizeBytes = 1L,
        modifiedAtEpochMillis = 1L,
        lastSeenScanId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}
