package com.eterocell.rhythhaus.carplay

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class CarPlayFacadeTest {
    @Test
    fun categoryLabelsUseLocalizedValues() = runBlocking {
        val fixture = Fixture()
        fixture.publish(labels = localizedLabels)

        assertEquals(
            listOf("歌曲", "专辑", "艺术家", "播放列表"),
            fixture.snapshot().categories.map { it.title },
        )

        fixture.close()
    }

    @Test
    fun catalogRevisionStaysStableUntilTheAuthoritativeCatalogChanges() =
        runBlocking {
            val fixture = Fixture()
            fixture.addTracks(track("one"))
            fixture.publish(libraryRevision = 1L)
            val first = fixture.snapshot().revision

            fixture.publish(libraryRevision = 1L)
            assertEquals(first, fixture.snapshot().revision)

            fixture.publish(libraryRevision = 2L)
            assertTrue(fixture.snapshot().revision > first)

            fixture.close()
        }

    @Test
    fun snapshotDistinguishesLoadingEmptyAndPopulatedLibraries() = runBlocking {
        val fixture = Fixture()

        fixture.publish(isReady = false)
        assertEquals(CarPlayLibraryStatus.Loading, fixture.snapshot().libraryStatus)

        fixture.publish(isReady = true)
        assertEquals(CarPlayLibraryStatus.Empty, fixture.snapshot().libraryStatus)

        fixture.addTracks(track("one"))
        fixture.publish(isReady = true, libraryRevision = 1L)
        val populated = fixture.snapshot()
        assertEquals(CarPlayLibraryStatus.Ready, populated.libraryStatus)
        assertEquals(listOf("Track one"), populated.categories
            .single { it.id == CarPlayCategoryId.Songs }
            .entries.map { it.title })

        fixture.close()
    }

    @Test
    fun albumAndArtistCategoriesGroupVisibleTracksInStableOrder() = runBlocking {
        val fixture = Fixture()
        fixture.addTracks(
            track("one").copy(artist = "Band B", album = "Alpha"),
            track("two").copy(artist = "Band A", album = "Beta"),
            track("three").copy(artist = "Band B", album = "Alpha"),
        )
        fixture.publish(libraryRevision = 1L)
        val snapshot = fixture.snapshot()
        val albums = snapshot.categories
            .single { it.id == CarPlayCategoryId.Albums }
            .entries
        val artists = snapshot.categories
            .single { it.id == CarPlayCategoryId.Artists }
            .entries

        assertEquals(listOf("Beta", "Alpha"), albums.map { it.title })
        assertEquals(listOf("Band A", "Band B"), artists.map { it.title })
        val alphaTracks = albums.single { it.title == "Alpha" }.children
        assertEquals(
            listOf("Track one", "Track three"),
            alphaTracks.map { it.title },
        )
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.select(alphaTracks.last().id, snapshot.revision),
        )
        assertEquals(
            listOf("one", "three"),
            fixture.controller.state.value.queue.map { it.track.id },
        )

        fixture.close()
    }

    @Test
    fun playlistSelectionPreservesVisibleOrderAndDuplicateOccurrences() =
        runBlocking {
            val fixture = Fixture()
            fixture.addTracks(track("first"), track("second"))
            val playlist = PlaylistSummary("saved", "Saved", 1L, 1L)
            val playlistSnapshot =
                PlaylistSnapshot(
                    playlists = listOf(playlist),
                    entriesByPlaylistId =
                        mapOf(
                            playlist.id to
                                listOf(
                                    PlaylistEntry("occurrence-1", "saved", "second", 0, 1L),
                                    PlaylistEntry("occurrence-2", "saved", "first", 1, 1L),
                                    PlaylistEntry("occurrence-3", "saved", "first", 2, 1L),
                                ),
                        ),
                )
            fixture.publish(
                libraryRevision = 1L,
                playlistSnapshot = playlistSnapshot,
            )
            val snapshot = fixture.snapshot()
            val playlistEntry = snapshot.categories
                .single { it.id == CarPlayCategoryId.Playlists }
                .entries.single()
            val visibleEntries = playlistEntry.children

            assertEquals(
                listOf("Track second", "Track first", "Track first"),
                visibleEntries.map { it.title },
            )
            assertEquals(3, visibleEntries.map { it.id }.distinct().size)

            assertEquals(
                CarPlayCommandResult.Applied,
                fixture.select(visibleEntries.last().id, snapshot.revision),
            )
            assertEquals(
                visibleEntries.map { it.id },
                fixture.controller.state.value.queue.map { it.id },
            )
            assertEquals(
                listOf("second", "first", "first"),
                fixture.controller.state.value.queue.map { it.track.id },
            )
            assertEquals(
                visibleEntries.last().id,
                fixture.controller.state.value.currentOccurrenceId,
            )

            fixture.close()
        }

    @Test
    fun staleLibraryEntryIsRejectedWithoutChangingPlayback() = runBlocking {
        val fixture = Fixture()
        fixture.addTracks(track("current", sourceId = "current-source"))
        fixture.addTracks(track("removed", sourceId = "removed-source"))
        fixture.controller.setQueue(
            listOf(track("current", sourceId = "current-source").toPlayable()),
            "current",
        )
        fixture.awaitTrack("current", PlaybackStatus.Paused)
        fixture.publish(libraryRevision = 1L)
        val snapshot = fixture.snapshot()
        val staleEntry = snapshot.categories
            .single { it.id == CarPlayCategoryId.Songs }
            .entries.single { it.title == "Track removed" }
        fixture.repository.removeSource("removed-source")
        val playbackBefore = fixture.controller.state.value

        assertEquals(
            CarPlayCommandResult.StaleEntry,
            fixture.select(staleEntry.id, snapshot.revision),
        )
        assertEquals(playbackBefore, fixture.controller.state.value)

        fixture.close()
    }

    @Test
    fun availableSongSelectionExcludesUnavailableSourcesFromItsQueue() =
        runBlocking {
            val fixture = Fixture()
            fixture.addTracks(
                track("available", sourceId = "available-source"),
                track("unavailable", sourceId = "unavailable-source"),
            )
            fixture.repository.upsertSource(
                LibrarySource(
                    id = "unavailable-source",
                    platformKind = LibraryPlatformKind.JvmFolder,
                    displayName = "Unavailable",
                    handle = "/unavailable-source",
                    createdAtEpochMillis = 1L,
                    accessStatus =
                        com.eterocell.rhythhaus.library.LibrarySourceAccessStatus.LostAccess,
                ),
            )
            fixture.publish(libraryRevision = 1L)
            val snapshot = fixture.snapshot()
            val songs = snapshot.categories
                .single { it.id == CarPlayCategoryId.Songs }
                .entries
            val availableEntry = songs.single { it.title == "Track available" }
            val unavailableEntry = songs.single { it.title == "Track unavailable" }

            assertTrue(availableEntry.isPlayable)
            assertFalse(unavailableEntry.isPlayable)
            assertEquals(
                CarPlayCommandResult.Applied,
                fixture.select(availableEntry.id, snapshot.revision),
            )
            assertEquals(
                listOf("available"),
                fixture.controller.state.value.queue.map { it.track.id },
            )
            val playbackBeforeUnavailableSelection = fixture.controller.state.value
            assertEquals(
                CarPlayCommandResult.Unavailable,
                fixture.select(unavailableEntry.id, snapshot.revision),
            )
            assertEquals(
                playbackBeforeUnavailableSelection.queue,
                fixture.controller.state.value.queue,
            )
            assertEquals(
                playbackBeforeUnavailableSelection.currentOccurrenceId,
                fixture.controller.state.value.currentOccurrenceId,
            )

            fixture.close()
        }

    @Test
    fun playbackProjectionPublishesCurrentTrackAndPosition() = runBlocking {
        val fixture = Fixture()
        val playable = track("one").toPlayable().copy(album = null)
        fixture.controller.setQueue(listOf(playable), "one")
        fixture.awaitTrack("one", PlaybackStatus.Paused)

        val loaded = fixture.awaitSnapshot {
            it.playback.currentTrack?.id == "one" &&
                it.playback.status == CarPlayPlaybackStatus.Paused
        }
        assertNull(loaded.playback.currentTrack?.album)
        assertEquals(3_000L, loaded.playback.durationMillis)

        fixture.controller.seekTo(1_250L)
        val positioned = fixture.awaitSnapshot {
            it.playback.currentTrack?.id == "one" &&
                it.playback.positionMillis == 1_250L
        }
        assertEquals(1_250L, positioned.playback.positionMillis)
        assertEquals(loaded.revision, positioned.revision)

        fixture.close()
    }

    @Test
    fun playbackArtworkBytesCannotMutatePublishedSnapshot() = runBlocking {
        val fixture = Fixture()
        val expectedArtwork = byteArrayOf(1, 2, 3)
        val playable =
            track("one").toPlayable().copy(artworkBytes = expectedArtwork)
        fixture.controller.setQueue(listOf(playable), "one")
        fixture.awaitTrack("one", PlaybackStatus.Paused)
        val snapshot = fixture.awaitSnapshot {
            it.playback.currentTrack?.id == "one" &&
                it.playback.currentTrack?.artworkBytes != null
        }

        val exposedArtwork =
            requireNotNull(snapshot.playback.currentTrack?.artworkBytes)
        exposedArtwork[0] = 99

        assertContentEquals(
            expectedArtwork,
            snapshot.playback.currentTrack?.artworkBytes,
        )

        fixture.close()
    }

    @Test
    fun catalogRevisionRejectsEntryFromAnOlderSnapshot() = runBlocking {
        val fixture = Fixture()
        fixture.addTracks(track("one"))
        fixture.publish(libraryRevision = 1L)
        val oldSnapshot = fixture.snapshot()
        val oldEntryId = oldSnapshot.categories
            .single { it.id == CarPlayCategoryId.Songs }
            .entries.single().id

        fixture.publish(libraryRevision = 2L)
        val playbackBefore = fixture.controller.state.value
        assertEquals(
            CarPlayCommandResult.StaleSnapshot,
            fixture.select(oldEntryId, oldSnapshot.revision),
        )
        assertEquals(playbackBefore, fixture.controller.state.value)

        fixture.close()
    }

    @Test
    fun transportCommandsRouteThroughTheSharedPlaybackController() = runBlocking {
        val fixture = Fixture()
        val tracks = listOf(track("first"), track("second"))
        fixture.addTracks(*tracks.toTypedArray())
        fixture.publish(libraryRevision = 1L)
        fixture.controller.setQueue(tracks.map { it.toPlayable() }, "first")
        fixture.awaitTrack("first", PlaybackStatus.Paused)

        var snapshot = fixture.snapshot()
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.transport(snapshot, "first") { facade, revision, current, reply ->
                facade.play(revision, current, reply)
            },
        )
        fixture.awaitTrack("first", PlaybackStatus.Playing)

        snapshot = fixture.snapshot()
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.transport(snapshot, "first") { facade, revision, current, reply ->
                facade.pause(revision, current, reply)
            },
        )
        fixture.awaitTrack("first", PlaybackStatus.Paused)

        snapshot = fixture.snapshot()
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.transport(snapshot, "first") { facade, revision, current, reply ->
                facade.skipToNext(revision, current, reply)
            },
        )
        fixture.awaitTrack("second", PlaybackStatus.Playing)

        snapshot = fixture.snapshot()
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.transport(snapshot, "second") { facade, revision, current, reply ->
                facade.seekTo(1_250L, revision, current, reply)
            },
        )
        withTimeout(5_000) {
            fixture.controller.state.first { it.positionMillis == 1_250L }
        }

        snapshot = fixture.snapshot()
        assertEquals(
            CarPlayCommandResult.Applied,
            fixture.transport(snapshot, "second") { facade, revision, current, reply ->
                facade.skipToPrevious(revision, current, reply)
            },
        )
        fixture.awaitTrack("first", PlaybackStatus.Playing)

        fixture.close()
    }

    @Test
    fun pauseIsRejectedWhileThePlaybackSessionBlocksCommands() = runBlocking {
        val fixture = Fixture()
        val track = track("one")
        fixture.addTracks(track)
        fixture.publish(libraryRevision = 1L)
        fixture.controller.setQueue(listOf(track.toPlayable()), "one")
        fixture.awaitTrack("one", PlaybackStatus.Paused)
        fixture.controller.play()
        fixture.awaitTrack("one", PlaybackStatus.Playing)
        fixture.playbackCommandsAllowed = false
        fixture.controller.setCommandsEnabled(false)
        val snapshot = fixture.snapshot()
        val playbackBefore = fixture.controller.state.value

        assertEquals(
            CarPlayCommandResult.Unavailable,
            fixture.transport(snapshot, "one") { facade, revision, current, reply ->
                facade.pause(revision, current, reply)
            },
        )
        val playbackAfter = fixture.controller.state.value
        assertEquals(playbackBefore.queue, playbackAfter.queue)
        assertEquals(playbackBefore.currentOccurrenceId, playbackAfter.currentOccurrenceId)
        assertEquals(PlaybackStatus.Playing, playbackAfter.status)

        fixture.close()
    }

    @Test
    fun nextIsUnavailableWhenTheSharedQueueHasNoOtherOccurrence() = runBlocking {
        val fixture = Fixture()
        val track = track("one")
        fixture.addTracks(track)
        fixture.publish(libraryRevision = 1L)
        fixture.controller.setQueue(listOf(track.toPlayable()), "one")
        fixture.awaitTrack("one", PlaybackStatus.Paused)
        val snapshot = fixture.awaitSnapshot {
            it.playback.currentTrack?.id == "one" &&
                it.playback.status == CarPlayPlaybackStatus.Paused
        }
        assertFalse(snapshot.playback.canSkipNext)
        val playbackBefore = fixture.controller.state.value

        assertEquals(
            CarPlayCommandResult.Unavailable,
            fixture.transport(snapshot, "one") { facade, revision, current, reply ->
                facade.skipToNext(revision, current, reply)
            },
        )
        assertEquals(playbackBefore.queue, fixture.controller.state.value.queue)
        assertEquals(
            playbackBefore.currentOccurrenceId,
            fixture.controller.state.value.currentOccurrenceId,
        )

        fixture.close()
    }

    @Test
    fun staleTransportIdentityIsRejectedWithoutChangingPlayback() = runBlocking {
        val fixture = Fixture()
        val tracks = listOf(track("first"), track("second"))
        fixture.addTracks(*tracks.toTypedArray())
        fixture.publish(libraryRevision = 1L)
        fixture.controller.setQueue(tracks.map { it.toPlayable() }, "first")
        fixture.awaitTrack("first", PlaybackStatus.Paused)
        val snapshot = fixture.snapshot()
        fixture.controller.selectTrack("second")
        fixture.awaitTrack("second", PlaybackStatus.Paused)
        val playbackBefore = fixture.controller.state.value

        assertEquals(
            CarPlayCommandResult.StalePlayback,
            fixture.transport(snapshot, "first") { facade, revision, current, reply ->
                facade.play(revision, current, reply)
            },
        )
        assertEquals(playbackBefore, fixture.controller.state.value)

        fixture.close()
    }

    private class Fixture {
        val repository = InMemoryLibraryRepository()
        val controller = PlaybackController(FakePlaybackEngine())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var playbackCommandsAllowed = true
        val facade =
            CarPlayFacade(
                libraryRepository = repository,
                playbackController = controller,
                processScope = scope,
                canDispatchPlaybackCommands = { playbackCommandsAllowed },
            )

        suspend fun addTracks(vararg tracks: LibraryTrack) {
            tracks.map { it.sourceId }.distinct().forEach { sourceId ->
                if (repository.sources().none { it.id == sourceId }) {
                    repository.upsertSource(
                        LibrarySource(
                            sourceId,
                            LibraryPlatformKind.JvmFolder,
                            sourceId,
                            "/$sourceId",
                            1L,
                        ),
                    )
                }
            }
            tracks.forEach(repository::upsertTrack)
        }

        suspend fun publish(
            libraryRevision: Long = 0L,
            isReady: Boolean = true,
            playlistSnapshot: PlaylistSnapshot = PlaylistSnapshot(),
            labels: CarPlayCategoryLabels = englishLabels,
        ) {
            facade.publishCatalog(
                libraryRevision = libraryRevision,
                isReady = isReady,
                tracks = repository.tracks(),
                favoriteTrackIds = repository.favoriteTrackIds(),
                playHistory = repository.playHistory(),
                playlists = playlistSnapshot,
                labels = labels,
            )
        }

        fun snapshot(): CarPlaySnapshot {
            var result: CarPlaySnapshot? = null
            facade.readSnapshot { result = it }
            return requireNotNull(result)
        }

        suspend fun awaitSnapshot(
            predicate: (CarPlaySnapshot) -> Boolean,
        ): CarPlaySnapshot {
            val result = CompletableDeferred<CarPlaySnapshot>()
            val observerId = facade.observeSnapshots { snapshot ->
                if (predicate(snapshot)) result.complete(snapshot)
            }
            try {
                return withTimeout(5_000) { result.await() }
            } finally {
                facade.removeSnapshotObserver(observerId)
            }
        }

        suspend fun select(entryId: String, revision: Long): CarPlayCommandResult {
            val result = CompletableDeferred<CarPlayCommandResult>()
            facade.selectEntry(entryId, revision, result::complete)
            return withTimeout(5_000) { result.await() }
        }

        suspend fun transport(
            snapshot: CarPlaySnapshot,
            expectedTrackId: String,
            issue: (
                CarPlayFacade,
                Long,
                String,
                (CarPlayCommandResult) -> Unit,
            ) -> Unit,
        ): CarPlayCommandResult {
            val result = CompletableDeferred<CarPlayCommandResult>()
            issue(facade, snapshot.revision, expectedTrackId, result::complete)
            return withTimeout(5_000) { result.await() }
        }

        suspend fun awaitTrack(trackId: String, status: PlaybackStatus) {
            withTimeout(5_000) {
                controller.state.first {
                    it.currentTrack?.id == trackId && it.status == status
                }
            }
        }

        fun close() {
            scope.cancel()
        }
    }

    private fun track(
        id: String,
        sourceId: String = "source",
    ): LibraryTrack =
        LibraryTrack(
            id = id,
            sourceId = sourceId,
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = "Track $id",
            title = "Track $id",
            artist = "Artist",
            album = "Album",
            durationMillis = 3_000L,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun LibraryTrack.toPlayable() =
        com.eterocell.rhythhaus.PlayableTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMillis = durationMillis,
            source = audioSource,
        )

    private companion object {
        val englishLabels =
            CarPlayCategoryLabels("Songs", "Albums", "Artists", "Playlists")
        val localizedLabels =
            CarPlayCategoryLabels("歌曲", "专辑", "艺术家", "播放列表")
    }
}
