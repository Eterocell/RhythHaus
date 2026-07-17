package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistStateTest {
    @Test
    fun unresolvedPlaylistDetailReturnsToHubWithRecoverableMessage() {
        val next = playlistDetailResolution(playlistId = "missing", resolvedPlaylist = null)

        assertEquals(PlaylistDetailResolution.ReturnToHub("playlist_changed"), next)
    }

    @Test
    fun resolvedPlaylistDetailStaysOnKeyedRoute() {
        val playlist = playlist("playlist-1")

        assertEquals(
            PlaylistDetailResolution.Show(playlist),
            playlistDetailResolution(playlistId = playlist.id, resolvedPlaylist = playlist),
        )
    }

    @Test
    fun confirmedSnapshotClearsLoadingAndReadFailure() {
        val snapshot = PlaylistSnapshot(playlists = listOf(playlist("playlist-1")))

        val state = reducePlaylistState(
            PlaylistState(isLoading = true, readErrorMessage = "old"),
            PlaylistStateAction.SnapshotConfirmed(snapshot),
        )

        assertEquals(snapshot, state.confirmedSnapshot)
        assertFalse(state.isLoading)
        assertNull(state.readErrorMessage)
    }

    @Test
    fun retryLoadingRetainsConfirmedSnapshotAndClearsReadError() {
        val snapshot = PlaylistSnapshot(playlists = listOf(playlist("playlist-1")))

        val state = reducePlaylistState(
            PlaylistState(
                confirmedSnapshot = snapshot,
                readErrorMessage = "read_failed",
                recoverableMessage = "playlist_changed",
            ),
            PlaylistStateAction.LoadStarted,
        )

        assertEquals(snapshot, state.confirmedSnapshot)
        assertTrue(state.isLoading)
        assertNull(state.readErrorMessage)
        assertEquals("playlist_changed", state.recoverableMessage)
    }

    @Test
    fun confirmedSnapshotClearsStaleRecoverableAndMutationMessages() {
        val state = reducePlaylistState(
            PlaylistState(
                mutationErrorMessage = "save_failed",
                recoverableMessage = "playlist_changed",
            ),
            PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()),
        )

        assertNull(state.mutationErrorMessage)
        assertNull(state.recoverableMessage)
    }

    @Test
    fun routeNoticePrioritizesRecoverableThenMutationMessage() {
        assertEquals(
            PlaylistRouteNotice.PlaylistChanged,
            playlistRouteNotice(PlaylistState(recoverableMessage = "playlist_changed", mutationErrorMessage = "save_failed")),
        )
        assertEquals(
            PlaylistRouteNotice.MutationFailed,
            playlistRouteNotice(PlaylistState(mutationErrorMessage = "save_failed")),
        )
        assertNull(playlistRouteNotice(PlaylistState()))
    }

    @Test
    fun retryableReadFailureRetainsLastConfirmedSnapshot() {
        val snapshot = PlaylistSnapshot(playlists = listOf(playlist("playlist-1")))

        val state = reducePlaylistState(
            PlaylistState(confirmedSnapshot = snapshot, isLoading = true),
            PlaylistStateAction.ReadFailed("read_failed"),
        )

        assertEquals(snapshot, state.confirmedSnapshot)
        assertFalse(state.isLoading)
        assertEquals("read_failed", state.readErrorMessage)
    }

    @Test
    fun recoverableMutationFailureRetainsConfirmedSnapshotAndPickerState() {
        val snapshot = PlaylistSnapshot(playlists = listOf(playlist("playlist-1")))
        val picker = PlaylistPickerState(trackId = "track-1", enteredName = "Road trip")

        val state = reducePlaylistState(
            PlaylistState(confirmedSnapshot = snapshot, picker = picker),
            PlaylistStateAction.MutationFailed("save_failed"),
        )

        assertEquals(snapshot, state.confirmedSnapshot)
        assertEquals(picker, state.picker)
        assertEquals("save_failed", state.mutationErrorMessage)
    }

    @Test
    fun tabAndBrowserSelectionArePureStateTransitions() {
        val browser = PlaylistBrowserState(
            playlistId = "playlist-1",
            query = "blue",
            visibleTrackIds = listOf("track-b", "track-a"),
            selectedTrackIds = setOf("track-a"),
        )
        val queueSelected = reducePlaylistState(PlaylistState(), PlaylistStateAction.SelectTab(PlaylistTab.Queue))
        val browserOpened = reducePlaylistState(queueSelected, PlaylistStateAction.OpenBrowser(browser))

        assertEquals(PlaylistTab.Queue, browserOpened.selectedTab)
        assertEquals(browser, browserOpened.browser)
        assertTrue(browserOpened.browser?.selectedTrackIds?.contains("track-a") == true)
    }

    @Test
    fun savedPlaybackUsesEntryIdsAndExactVisibleOrderIncludingDuplicates() {
        val duplicate = playableTrack("track-a")
        val entries = listOf(
            entry(id = "entry-2", trackId = duplicate.id, position = 0),
            entry(id = "entry-1", trackId = duplicate.id, position = 1),
        )

        val occurrences = savedPlaylistOccurrences(entries, mapOf(duplicate.id to duplicate))

        assertEquals(listOf("entry-2", "entry-1"), occurrences.map { it.id })
        assertEquals(listOf("track-a", "track-a"), occurrences.map { it.track.id })
    }

    @Test
    fun savedRowKeysRemainIndependentForDuplicateTracks() {
        val entries = listOf(
            entry(id = "entry-1", trackId = "track-a", position = 0),
            entry(id = "entry-2", trackId = "track-a", position = 1),
        )

        assertEquals(listOf("entry-1", "entry-2"), savedPlaylistRowKeys(entries))
    }

    @Test
    fun confirmedWriteRefreshesSnapshotOnlyAfterRepositoryMutationCompletes() {
        val events = mutableListOf<String>()
        val repository = object : com.eterocell.rhythhaus.library.PlaylistRepository by InMemoryPlaylistRepository() {
            private val delegate = InMemoryPlaylistRepository(
                now = { 1L },
                idFactory = { "playlist-1" },
            )

            override fun playlists(): List<Playlist> {
                events += "read"
                return delegate.playlists()
            }

            override fun create(name: String): Playlist {
                events += "write"
                return delegate.create(name)
            }

            override fun entries(playlistId: String) = delegate.entries(playlistId)
        }

        val snapshot = mutatePlaylistAndRefresh(repository) { create("Saved") }

        assertEquals(listOf("write", "read"), events)
        assertEquals(listOf("playlist-1"), snapshot.playlists.map { it.id })
    }

    private fun playlist(id: String) = Playlist(
        id = id,
        name = "Playlist $id",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun entry(id: String, trackId: String, position: Int) = PlaylistEntry(
        id = id,
        playlistId = "playlist-1",
        trackId = trackId,
        position = position,
        createdAtEpochMillis = 1L,
    )

    private fun playableTrack(id: String) = PlayableTrack(
        id = id,
        source = AudioSource.FilePath("/$id.mp3"),
        title = id,
        artist = "Artist",
        album = "Album",
        durationMillis = 180_000L,
    )
}
