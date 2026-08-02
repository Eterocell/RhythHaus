package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistApiContractTest {
    @Test
    fun publicValuesAndRepositoryExposeTheStableSummaryBoundary() {
        val summary = PlaylistSummary("playlist", "Saved", 1L, 2L)
        val entry = PlaylistEntry("entry", summary.id, "track", 0, 3L)
        val mutation = PlaylistImportMutation("Imported", listOf("track"))
        val repository = RecordingPlaylistRepository(summary, entry)

        assertEquals(listOf(summary), repository.playlists())
        assertEquals(summary, repository.playlist(summary.id))
        assertEquals(listOf(entry), repository.entries(summary.id))
        assertEquals(summary, repository.create("Saved"))
        assertEquals(
            summary, repository.createWithEntries("Saved", listOf("track")))
        assertEquals(
            listOf(summary), repository.importPlaylists(listOf(mutation)))
        repository.rename(summary.id, "Renamed")
        repository.delete(summary.id)
        repository.append(summary.id, listOf("track"))
        repository.removeEntry(entry.id)
        repository.reorder(summary.id, listOf(entry.id))

        assertTrue(
            repository.calls.containsAll(PlaylistRepositoryMethod.entries))
    }
}

private enum class PlaylistRepositoryMethod {
    Playlists,
    Playlist,
    Entries,
    Create,
    CreateWithEntries,
    ImportPlaylists,
    Rename,
    Delete,
    Append,
    RemoveEntry,
    Reorder,
}

private class RecordingPlaylistRepository(
    private val summary: PlaylistSummary,
    private val entry: PlaylistEntry,
) : PlaylistRepository {
    val calls = mutableSetOf<PlaylistRepositoryMethod>()

    override fun playlists(): List<PlaylistSummary> {
        calls += PlaylistRepositoryMethod.Playlists
        return listOf(summary)
    }

    override fun playlist(id: String): PlaylistSummary? {
        calls += PlaylistRepositoryMethod.Playlist
        return summary
    }

    override fun entries(playlistId: String): List<PlaylistEntry> {
        calls += PlaylistRepositoryMethod.Entries
        return listOf(entry)
    }

    override fun create(name: String): PlaylistSummary {
        calls += PlaylistRepositoryMethod.Create
        return summary
    }

    override fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): PlaylistSummary {
        calls += PlaylistRepositoryMethod.CreateWithEntries
        return summary
    }

    override fun importPlaylists(
        playlists: List<PlaylistImportMutation>
    ): List<PlaylistSummary> {
        calls += PlaylistRepositoryMethod.ImportPlaylists
        return listOf(summary)
    }

    override fun rename(id: String, name: String) {
        calls += PlaylistRepositoryMethod.Rename
    }

    override fun delete(id: String) {
        calls += PlaylistRepositoryMethod.Delete
    }

    override fun append(playlistId: String, trackIds: List<String>) {
        calls += PlaylistRepositoryMethod.Append
    }

    override fun removeEntry(entryId: String) {
        calls += PlaylistRepositoryMethod.RemoveEntry
    }

    override fun reorder(playlistId: String, entryIds: List<String>) {
        calls += PlaylistRepositoryMethod.Reorder
    }
}
