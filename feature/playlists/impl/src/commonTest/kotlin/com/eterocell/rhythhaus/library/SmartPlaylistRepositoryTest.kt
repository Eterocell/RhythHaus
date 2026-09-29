package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SmartPlaylistRepositoryTest {
    @Test
    fun rulesRemainIndependentOfStaticEntriesAndSurviveSourceDeletion() {
        val repository = InMemoryPlaylistRepository()
        val source =
            repository.createWithEntries("Source", listOf("a", "b", "a"))
        val smart =
            repository.createSmartPlaylist(
                "Derived", SmartPlaylistRule.SavedPlaylist(source.id))
        repository.delete(source.id)
        assertEquals(listOf(smart), repository.smartPlaylists())
        repository.updateSmartPlaylist(
            smart.id, "Repaired", SmartPlaylistRule.Favorites)
        assertEquals(
            SmartPlaylistRule.Favorites,
            repository.smartPlaylists().single().rule)
        repository.deleteSmartPlaylist(smart.id)
        assertEquals(emptyList(), repository.smartPlaylists())
    }

    @Test
    fun invalidMutationsLeavePreviouslySavedRuleUnchanged() {
        val repository = InMemoryPlaylistRepository()
        val saved =
            repository.createSmartPlaylist("Saved", SmartPlaylistRule.Favorites)
        assertFailsWith<IllegalArgumentException> {
            repository.createSmartPlaylist(
                "bad", SmartPlaylistRule.RecentlyPlayed(12))
        }
        assertFailsWith<IllegalArgumentException> {
            repository.updateSmartPlaylist(
                saved.id, "", SmartPlaylistRule.Favorites)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.createSmartPlaylist(
                "bad", SmartPlaylistRule.SavedPlaylist("missing"))
        }
        assertEquals(listOf(saved), repository.smartPlaylists())
    }
}
