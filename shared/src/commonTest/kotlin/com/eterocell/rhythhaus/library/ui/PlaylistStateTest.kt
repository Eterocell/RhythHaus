package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistStateTest {
    @Test
    fun pickerStateRejectsEmptyAndBlankTrackIds() {
        assertFailsWith<IllegalArgumentException> { PlaylistPickerState(emptyList()) }
        assertFailsWith<IllegalArgumentException> { PlaylistPickerState(listOf("track-a", " ")) }
    }

    @Test
    fun unresolvedPlaylistDetailReturnsToHubWithRecoverableMessage() {
        val next = playlistDetailResolution(
            playlistId = "missing",
            state = PlaylistState(hasConfirmedSnapshot = true),
        )

        assertEquals(PlaylistDetailResolution.ReturnToHub("playlist_changed"), next)
    }

    @Test
    fun unresolvedPlaylistDetailWaitsForAuthoritativeConfirmation() {
        assertEquals(
            PlaylistDetailResolution.AwaitConfirmation,
            playlistDetailResolution(
                playlistId = "missing",
                state = PlaylistState(isLoading = true),
            ),
        )
        assertEquals(
            PlaylistDetailResolution.AwaitConfirmation,
            playlistDetailResolution(
                playlistId = "missing",
                state = PlaylistState(readErrorMessage = "read_failed"),
            ),
        )
        assertEquals(
            PlaylistDetailResolution.AwaitConfirmation,
            playlistDetailResolution(
                playlistId = "missing",
                state = PlaylistState(
                    isLoading = true,
                    hasConfirmedSnapshot = true,
                ),
            ),
        )
    }

    @Test
    fun resolvedPlaylistDetailStaysOnKeyedRoute() {
        val playlist = playlist("playlist-1")

        assertEquals(
            PlaylistDetailResolution.Show(playlist),
            playlistDetailResolution(
                playlistId = playlist.id,
                state = PlaylistState(
                    confirmedSnapshot = PlaylistSnapshot(playlists = listOf(playlist)),
                    hasConfirmedSnapshot = true,
                ),
            ),
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
        val picker = PlaylistPickerState(trackIds = listOf("track-2", "track-1"), enteredName = "Road trip")

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

    @Test
    fun staleRefreshPublicationCannotOverwriteNewerMutationPublication() = runBlocking {
        val repository = InMemoryPlaylistRepository(
            now = { 1L },
            idFactory = { "playlist-1" },
        )
        val owner = PlaylistStateOwner(repository, Dispatchers.Default)
        val oldReadReady = CompletableDeferred<Unit>()
        val releaseOldRead = CompletableDeferred<Unit>()
        var state = PlaylistState()

        val refresh = async {
            val publication = owner.refresh()
            oldReadReady.complete(Unit)
            releaseOldRead.await()
            state = reducePlaylistState(state, publication)
        }
        oldReadReady.await()

        state = reducePlaylistState(
            state,
            owner.mutate { create("Newer") },
        )
        releaseOldRead.complete(Unit)
        refresh.await()

        assertEquals(listOf("playlist-1"), state.confirmedSnapshot.playlists.map { it.id })
        assertEquals("Newer", state.confirmedSnapshot.playlists.single().name)
    }

    @Test
    fun staleRefreshFailureCannotOverwriteNewerSuccessOrClearNewerLoading() = runBlocking {
        var failReads = true
        val delegate = InMemoryPlaylistRepository(now = { 1L }, idFactory = { "playlist-1" })
        val repository = object : com.eterocell.rhythhaus.library.PlaylistRepository by delegate {
            override fun playlists(): List<Playlist> = if (failReads) error("old read failed") else delegate.playlists()
        }
        val owner = PlaylistStateOwner(repository, Dispatchers.Default)
        val oldFailure = owner.refresh(PlaylistReadFailedMessage)
        failReads = false
        var state = reducePlaylistState(
            PlaylistState(),
            owner.mutate(PlaylistMutationFailedMessage) { create("Newer") },
        )
        state = reducePlaylistState(state, PlaylistStateAction.LoadStarted)

        state = reducePlaylistState(state, oldFailure)

        assertTrue(state.isLoading)
        assertNull(state.readErrorMessage)
        assertEquals("Newer", state.confirmedSnapshot.playlists.single().name)
    }

    @Test
    fun staleMutationFailureCannotAddNoticeAfterNewerSuccess() = runBlocking {
        var failMutation = true
        val delegate = InMemoryPlaylistRepository(now = { 1L }, idFactory = { "playlist-1" })
        val repository = object : com.eterocell.rhythhaus.library.PlaylistRepository by delegate {
            override fun create(name: String): Playlist = if (failMutation) error("old mutation failed") else delegate.create(name)
        }
        val owner = PlaylistStateOwner(repository, Dispatchers.Default)
        val oldFailure = owner.mutate(PlaylistMutationFailedMessage) { create("Old") }
        failMutation = false
        var state = reducePlaylistState(
            PlaylistState(),
            owner.mutate(PlaylistMutationFailedMessage) { create("Newer") },
        )

        state = reducePlaylistState(state, oldFailure)

        assertNull(state.mutationErrorMessage)
        assertNull(playlistRouteNotice(state))
        assertEquals("Newer", state.confirmedSnapshot.playlists.single().name)
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
