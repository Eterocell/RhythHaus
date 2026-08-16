package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.playlistsImplementationModule
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PlaylistLifecycleIntegrationJvmTest {
    @Test
    fun sourceRemovalCascadesBeforePlaylistPublicationAndReconcilesResolvedQueue() =
        runBlocking {
            openHarness().use { harness ->
                harness.seedSourceAndTrack("remove-source", "remove-track")
                harness.seedSourceAndTrack("keep-source", "keep-track")
                val playlist = harness.playlists.create("Saved")
                harness.playlists.append(
                    playlist.id, listOf("remove-track", "keep-track"))
                val savedEntries = harness.playlists.entries(playlist.id)
                harness.controller.setOccurrenceQueue(
                    harness.occurrences(savedEntries), savedEntries.first().id)

                harness.playlists.removeEntry(savedEntries.last().id)
                assertEquals(
                    savedEntries.map(PlaylistEntry::id),
                    harness.controller.state.value.queue.map(
                        QueueOccurrence::id))

                val events = mutableListOf<String>()
                var publishedPlaylists: PlaylistSnapshot? = null
                removeSourceInBackground(
                    sourceId = "remove-source",
                    repository = harness.library,
                    playlistStateOwner = harness.playlistStateOwner,
                    platformAccess = NoOpPlatformSourceAccess,
                    reconciler =
                        harness.reconciler(events) {
                            assertEquals(
                                emptyList(),
                                harness.playlists.entries(playlist.id))
                        },
                    ioDispatcher = Dispatchers.Default,
                    publish =
                        testLibraryMutationPublication(
                            onPublication = { events += "read_playlists" },
                            onContent = { events += "library" },
                            onPlaylists = { action ->
                                events += "playlists"
                                publishedPlaylists =
                                    (action
                                            as
                                            PlaylistStateAction.SnapshotConfirmed)
                                        .snapshot
                            }),
                )

                assertEquals(
                    listOf(
                        "reconcile", "read_playlists", "library", "playlists"),
                    events)
                assertNotNull(harness.playlists.playlist(playlist.id))
                assertEquals(
                    emptyList(),
                    publishedPlaylists?.entriesByPlaylistId?.get(playlist.id))
                assertEquals(
                    listOf(savedEntries.last().id),
                    harness.controller.state.value.queue.map(
                        QueueOccurrence::id))
                assertEquals(
                    listOf("keep-track"),
                    harness.controller.state.value.queue.map { it.track.id })
            }
        }

    @Test
    fun clearLibraryCascadesBeforePlaylistPublicationAndReconcilesResolvedQueue() =
        runBlocking {
            openHarness().use { harness ->
                harness.seedSourceAndTrack("source", "track")
                val playlist = harness.playlists.create("Saved")
                harness.playlists.append(playlist.id, listOf("track", "track"))
                val savedEntries = harness.playlists.entries(playlist.id)
                harness.controller.setOccurrenceQueue(
                    harness.occurrences(savedEntries), savedEntries.first().id)

                harness.playlists.reorder(
                    playlist.id, savedEntries.map(PlaylistEntry::id).reversed())
                assertEquals(
                    savedEntries.map(PlaylistEntry::id),
                    harness.controller.state.value.queue.map(
                        QueueOccurrence::id))

                val events = mutableListOf<String>()
                var publishedPlaylists: PlaylistSnapshot? = null
                clearLibraryInBackground(
                    repository = harness.library,
                    playlistStateOwner = harness.playlistStateOwner,
                    platformAccess = NoOpPlatformSourceAccess,
                    reconciler =
                        harness.reconciler(events) {
                            assertEquals(
                                emptyList(),
                                harness.playlists.entries(playlist.id))
                        },
                    ioDispatcher = Dispatchers.Default,
                    publish =
                        testLibraryMutationPublication(
                            onPublication = { events += "read_playlists" },
                            onContent = { events += "library" },
                            onPlaylists = { action ->
                                events += "playlists"
                                publishedPlaylists =
                                    (action
                                            as
                                            PlaylistStateAction.SnapshotConfirmed)
                                        .snapshot
                            }),
                )

                assertEquals(
                    listOf(
                        "reconcile", "read_playlists", "library", "playlists"),
                    events)
                assertNotNull(harness.playlists.playlist(playlist.id))
                assertEquals(
                    emptyList(),
                    publishedPlaylists?.entriesByPlaylistId?.get(playlist.id))
                assertEquals(emptyList(), harness.controller.state.value.queue)
            }
        }

    @Test
    fun removeMissingTracksRefreshesPlaylistAndReconcilesResolvedQueue() =
        runBlocking {
            openHarness().use { harness ->
                harness.seedSourceAndTrack("source", "missing-track")
                harness.seedSourceAndTrack("source", "kept-track")
                val playlist = harness.playlists.create("Saved")
                harness.playlists.append(
                    playlist.id, listOf("missing-track", "kept-track"))
                val savedEntries = harness.playlists.entries(playlist.id)
                harness.controller.setOccurrenceQueue(
                    harness.occurrences(savedEntries), savedEntries.first().id)
                harness.library.insertScanSession(
                    ScanSession(
                        id = "completed-scan",
                        sourceId = "source",
                        status =
                            com.eterocell.rhythhaus.library.ScanStatus
                                .Completed,
                        startedAtEpochMillis = 1L,
                        completedAtEpochMillis = 2L,
                    ),
                )
                harness.library.upsertTrack(
                    LibraryTrack(
                        id = "kept-track",
                        sourceId = "source",
                        sourceLocalKey = "kept-track.mp3",
                        audioSource =
                            AudioSource.FilePath("/source/kept-track.mp3"),
                        displayName = "kept-track.mp3",
                        title = "kept-track",
                        artist = "Artist",
                        album = "Album",
                        durationMillis = null,
                        sizeBytes = null,
                        modifiedAtEpochMillis = null,
                        lastSeenScanId = "completed-scan",
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                    ),
                )
                val events = mutableListOf<String>()
                var publishedPlaylists: PlaylistSnapshot? = null

                removeMissingTracksInBackground(
                    sourceId = "source",
                    latestScanId = "completed-scan",
                    repository = harness.library,
                    playlistStateOwner = harness.playlistStateOwner,
                    platformAccess = NoOpPlatformSourceAccess,
                    reconciler =
                        harness.reconciler(events) {
                            assertEquals(
                                listOf("kept-track"),
                                harness.playlists
                                    .entries(playlist.id)
                                    .map(PlaylistEntry::trackId),
                            )
                        },
                    ioDispatcher = Dispatchers.Default,
                    publish =
                        testLibraryMutationPublication(
                            onPublication = { events += "read_playlists" },
                            onContent = { events += "library" },
                            onPlaylists = { action ->
                                events += "playlists"
                                publishedPlaylists =
                                    (action
                                            as
                                            PlaylistStateAction.SnapshotConfirmed)
                                        .snapshot
                            },
                        ),
                )

                assertEquals(
                    listOf(
                        "reconcile", "read_playlists", "library", "playlists"),
                    events,
                )
                assertEquals(
                    listOf("kept-track"),
                    publishedPlaylists
                        ?.entriesByPlaylistId
                        ?.get(playlist.id)
                        ?.map(PlaylistEntry::trackId),
                )
                assertEquals(
                    listOf("kept-track"),
                    harness.controller.state.value.queue.map { it.track.id },
                )
            }
        }
}

private fun testLibraryMutationPublication(
    onPublication: suspend () -> Unit = {},
    onContent: suspend (LibraryContentState) -> Unit = {},
    onPlaylists: suspend (PlaylistStateAction) -> Unit = {},
): suspend (LibraryMutationPublication) -> Unit = { publication ->
    onPublication()
    publication.content?.let { onContent(it) }
    publication.playlists?.let { onPlaylists(it) }
}

private class PlaylistLifecycleHarness(
    val database: LibraryDatabase,
    val playlists: PlaylistRepository,
) : AutoCloseable {
    val library = SqlDelightLibraryRepository(database)
    val playlistStateOwner = PlaylistStateOwner(playlists, Dispatchers.Default)
    val controller = PlaybackController(FakePlaybackEngine())

    fun seedSourceAndTrack(sourceId: String, trackId: String) {
        library.upsertSource(
            LibrarySource(
                id = sourceId,
                platformKind = LibraryPlatformKind.JvmFolder,
                displayName = sourceId,
                handle = "/$sourceId",
                createdAtEpochMillis = 1L,
            ),
        )
        library.upsertTrack(
            LibraryTrack(
                id = trackId,
                sourceId = sourceId,
                sourceLocalKey = "$trackId.mp3",
                audioSource = AudioSource.FilePath("/$sourceId/$trackId.mp3"),
                displayName = "$trackId.mp3",
                title = trackId,
                artist = "Artist",
                album = "Album",
                durationMillis = null,
                sizeBytes = null,
                modifiedAtEpochMillis = null,
                lastSeenScanId = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
    }

    fun occurrences(entries: List<PlaylistEntry>): List<QueueOccurrence> {
        val tracksById = library.tracks().associateBy(LibraryTrack::id)
        return entries.map { entry ->
            QueueOccurrence(
                entry.id,
                requireNotNull(tracksById[entry.trackId]).toPlayableTrack())
        }
    }

    fun reconciler(events: MutableList<String>, afterCascade: () -> Unit) =
        PlaybackSessionReconciler { tracks ->
            events += "reconcile"
            afterCascade()
            controller.reconcileSession(tracks.map { it.toPlayableTrack() })
            PlaybackSessionReconcileResult.Applied
        }

    override fun close() {
        controller.release()
        stopKoin()
        database.driver.close()
    }
}

private object NoOpPlatformSourceAccess :
    com.eterocell.rhythhaus.library.PlatformSourceAccess {
    override fun scan(
        source: LibrarySource
    ): Sequence<com.eterocell.rhythhaus.library.impl.PlatformScanEvent> =
        emptySequence()
}

private fun openHarness(): PlaylistLifecycleHarness {
    val databaseFile: File =
        Files.createTempFile("rhythhaus-playlist-lifecycle", ".db")
            .toFile()
            .apply {
                delete()
                deleteOnExit()
            }
    val database = LibraryDatabase(databaseFile)
    val application = startKoin {
        modules(
            module { single<LibraryDatabase> { database } },
            playlistsImplementationModule(),
        )
    }
    return PlaylistLifecycleHarness(database, application.koin.get())
}
