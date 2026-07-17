package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PlaylistRepositoryContractTest {
    private fun repository(): InMemoryPlaylistRepository {
        val ids = sequenceOf("playlist-1", "entry-1", "entry-2", "entry-3").iterator()
        return InMemoryPlaylistRepository(
            now = { 100L },
            idFactory = ids::next,
        )
    }

    @Test
    fun duplicateEntriesRemainIndependentInTheInMemoryContract() {
        val repository = repository()
        val playlist = repository.create(" Road trip ")

        repository.append(playlist.id, listOf("track-a", "track-a"))

        val entries = repository.entries(playlist.id)
        assertEquals(listOf("track-a", "track-a"), entries.map(PlaylistEntry::trackId))
        assertNotEquals(entries[0].id, entries[1].id)
        assertEquals("Road trip", repository.playlist(playlist.id)?.name)
    }

    @Test
    fun blankNamesAreRejectedWithoutChangingConfirmedState() {
        val repository = repository()
        val playlist = repository.create("Road trip")

        assertFailsWith<IllegalArgumentException> { repository.rename(playlist.id, "   ") }

        assertEquals("Road trip", repository.playlist(playlist.id)?.name)
    }

    @Test
    fun removeAndReorderKeepAContiguousSequence() {
        val repository = repository()
        val playlist = repository.create("Road trip")
        repository.append(playlist.id, listOf("track-a", "track-b", "track-c"))
        val entries = repository.entries(playlist.id)

        repository.removeEntry(entries[1].id)
        repository.reorder(playlist.id, listOf(entries[2].id, entries[0].id))

        assertEquals(listOf("track-c", "track-a"), repository.entries(playlist.id).map(PlaylistEntry::trackId))
        assertEquals(listOf(0, 1), repository.entries(playlist.id).map(PlaylistEntry::position))
    }

    @Test
    fun invalidReorderLeavesTheConfirmedOrderUnchanged() {
        val repository = repository()
        val playlist = repository.create("Road trip")
        repository.append(playlist.id, listOf("track-a", "track-b"))
        val before = repository.entries(playlist.id)

        assertFailsWith<IllegalArgumentException> {
            repository.reorder(playlist.id, listOf(before.first().id, "missing-entry"))
        }

        assertEquals(before, repository.entries(playlist.id))
    }

    @Test
    fun removingTheFinalEntryRetainsAnEmptyPlaylist() {
        val repository = repository()
        val playlist = repository.create("Road trip")
        repository.append(playlist.id, listOf("track-a"))

        repository.removeEntry(repository.entries(playlist.id).single().id)

        assertNotNull(repository.playlist(playlist.id))
        assertEquals(emptyList(), repository.entries(playlist.id))
    }
}
