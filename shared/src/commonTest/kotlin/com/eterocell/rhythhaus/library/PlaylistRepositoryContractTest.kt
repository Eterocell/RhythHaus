package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlaylistRepositoryContractTest {
    private fun repository(): InMemoryPlaylistRepository {
        val ids =
            sequenceOf("playlist-1", "entry-1", "entry-2", "entry-3").iterator()
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
        assertEquals(
            listOf("track-a", "track-a"), entries.map(PlaylistEntry::trackId))
        assertNotEquals(entries[0].id, entries[1].id)
        assertEquals("Road trip", repository.playlist(playlist.id)?.name)
    }

    @Test
    fun blankNamesAreRejectedWithoutChangingConfirmedState() {
        val repository = repository()
        val playlist = repository.create("Road trip")

        assertFailsWith<IllegalArgumentException> {
            repository.rename(playlist.id, "   ")
        }

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

        assertEquals(
            listOf("track-c", "track-a"),
            repository.entries(playlist.id).map(PlaylistEntry::trackId))
        assertEquals(
            listOf(0, 1),
            repository.entries(playlist.id).map(PlaylistEntry::position))
    }

    @Test
    fun invalidReorderLeavesTheConfirmedOrderUnchanged() {
        val repository = repository()
        val playlist = repository.create("Road trip")
        repository.append(playlist.id, listOf("track-a", "track-b"))
        val before = repository.entries(playlist.id)

        assertFailsWith<IllegalArgumentException> {
            repository.reorder(
                playlist.id, listOf(before.first().id, "missing-entry"))
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

    @Test
    fun createWithEntriesPreservesDuplicateEntriesAndNonuniqueNames() {
        val ids =
            sequenceOf(
                    "playlist-1", "entry-1", "entry-2", "playlist-2", "entry-3")
                .iterator()
        val repository =
            InMemoryPlaylistRepository(now = { 100L }, idFactory = ids::next)

        val first =
            repository.createWithEntries(" Same ", listOf("track-a", "track-a"))
        val second = repository.createWithEntries("Same", listOf("track-b"))

        assertEquals(
            listOf("Same", "Same"), repository.playlists().map(Playlist::name))
        assertEquals(
            listOf("track-a", "track-a"),
            repository.entries(first.id).map(PlaylistEntry::trackId))
        assertEquals(
            listOf(0, 1),
            repository.entries(first.id).map(PlaylistEntry::position))
        assertEquals(
            listOf("track-b"),
            repository.entries(second.id).map(PlaylistEntry::trackId))
    }

    @Test
    fun createWithEntriesRollsBackPlaylistWhenEntryCreationFails() {
        var calls = 0
        val repository =
            InMemoryPlaylistRepository(
                now = { 100L },
                idFactory = {
                    calls += 1
                    if (calls == 2) error("entry id failure") else "playlist-1"
                },
            )

        assertFailsWith<IllegalStateException> {
            repository.createWithEntries("Transient", listOf("track-a"))
        }

        assertTrue(repository.playlists().isEmpty())
    }

    @Test
    fun importPlaylistsReturnsEmptyWithoutCallingFactoriesOrMutatingState() {
        val repository =
            InMemoryPlaylistRepository(
                now = { error("time must not be requested") },
                idFactory = { error("id must not be requested") },
            )

        assertEquals(emptyList(), repository.importPlaylists(emptyList()))
        assertEquals(emptyList(), repository.playlists())
    }

    @Test
    fun importPlaylistsValidatesEveryMutationBeforeCallingFactoriesOrPublishing() {
        var factoryCalls = 0
        val repository =
            InMemoryPlaylistRepository(
                now = {
                    factoryCalls += 1
                    100L
                },
                idFactory = {
                    factoryCalls += 1
                    "unexpected-id"
                },
            )

        val invalidRequests =
            listOf(
                listOf(PlaylistImportMutation("   ", listOf("track-a"))),
                listOf(PlaylistImportMutation("Empty", emptyList())),
                listOf(
                    PlaylistImportMutation("Valid", listOf("track-a")),
                    PlaylistImportMutation("Invalid", listOf("track-b", "   ")),
                ),
            )

        invalidRequests.forEach { request ->
            assertFailsWith<IllegalArgumentException> {
                repository.importPlaylists(request)
            }
        }
        assertEquals(0, factoryCalls)
        assertEquals(emptyList(), repository.playlists())
    }

    @Test
    fun importPlaylistsPreservesRequestTrackAndDuplicateOrder() {
        val ids =
            sequenceOf(
                    "playlist-1",
                    "entry-1",
                    "entry-2",
                    "entry-3",
                    "playlist-2",
                    "entry-4",
                )
                .iterator()
        var timestamp = 100L
        val repository =
            InMemoryPlaylistRepository(
                now = { timestamp++ },
                idFactory = ids::next,
            )

        val imported =
            repository.importPlaylists(
                listOf(
                    PlaylistImportMutation(
                        " First ", listOf("track-b", "track-a", "track-b")),
                    PlaylistImportMutation("Second", listOf("track-c")),
                ),
            )

        assertEquals(
            listOf("playlist-1", "playlist-2"), imported.map(Playlist::id))
        assertEquals(listOf("First", "Second"), imported.map(Playlist::name))
        assertEquals(
            listOf("playlist-1", "playlist-2"),
            repository.playlists().map(Playlist::id))
        assertEquals(
            listOf("track-b", "track-a", "track-b"),
            repository.entries(imported[0].id).map(PlaylistEntry::trackId))
        assertEquals(
            listOf(0, 1, 2),
            repository.entries(imported[0].id).map(PlaylistEntry::position))
        assertEquals(
            3,
            repository
                .entries(imported[0].id)
                .map(PlaylistEntry::id)
                .toSet()
                .size)
        assertEquals(
            listOf("track-c"),
            repository.entries(imported[1].id).map(PlaylistEntry::trackId))
    }

    @Test
    fun importPlaylistsPublishesNothingWhenSecondPlaylistEntryStagingFailsAndRetryCreatesAllOnce() {
        var failSecondPlaylistEntry = true
        var idCall = 0
        val repository =
            InMemoryPlaylistRepository(
                now = { 100L },
                idFactory = {
                    idCall += 1
                    if (failSecondPlaylistEntry && idCall == 7)
                        error("second playlist entry id failure")
                    "generated-$idCall"
                },
            )
        val existing =
            repository.createWithEntries("Existing", listOf("track-existing"))
        val existingPlaylists = repository.playlists()
        val existingEntries = repository.entries(existing.id)
        val request =
            listOf(
                PlaylistImportMutation("First", listOf("track-a", "track-a")),
                PlaylistImportMutation("Second", listOf("track-b")),
            )

        assertFailsWith<IllegalStateException> {
            repository.importPlaylists(request)
        }

        assertEquals(existingPlaylists, repository.playlists())
        assertEquals(existingEntries, repository.entries(existing.id))

        failSecondPlaylistEntry = false
        val imported = repository.importPlaylists(request)

        assertEquals(listOf("First", "Second"), imported.map(Playlist::name))
        assertEquals(
            setOf("Existing", "First", "Second"),
            repository.playlists().map(Playlist::name).toSet())
        assertEquals(3, repository.playlists().size)
        assertEquals(
            listOf("track-a", "track-a"),
            repository.entries(imported[0].id).map(PlaylistEntry::trackId))
        assertEquals(
            listOf("track-b"),
            repository.entries(imported[1].id).map(PlaylistEntry::trackId))
    }
}
