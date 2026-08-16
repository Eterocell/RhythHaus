package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.androidSafSourceId
import com.eterocell.rhythhaus.library.emptyLibrarySourceMutationsAllowed
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.jvmFolderSourceId
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking

class LibrarySourceManagementTest {
    @Test
    fun coordinatorAcceptsCombinedLibraryAndPlaylistPublicationAtomically() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val admission =
                coordinator.admitMutation(LibraryOperationKind.Clear)
                    as LibraryOperationAdmission.Admitted
            val events = mutableListOf<String>()

            coordinator.publishIfCurrent(admission.token) {
                events += "library"
                events += "playlists"
            }

            assertEquals(listOf("library", "playlists"), events)
            coordinator.complete(admission.token)
        }

    @Test
    fun staleMutationPublicationCannotOverwriteLibraryOrPlaylistState() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val first =
                coordinator.admitMutation(LibraryOperationKind.RemoveSource)
                    as LibraryOperationAdmission.Admitted
            coordinator.complete(first.token)
            val current =
                coordinator.admitMutation(LibraryOperationKind.Clear)
                    as LibraryOperationAdmission.Admitted
            val published = mutableListOf<String>()

            coordinator.publishIfCurrent(first.token) {
                published += "library"
                published += "playlists"
            }

            assertEquals(emptyList(), published)
            coordinator.publishIfCurrent(current.token) {
                published += "library"
                published += "playlists"
            }
            assertEquals(listOf("library", "playlists"), published)
            coordinator.complete(current.token)
        }

    @Test
    fun staleTokenSuppressesCombinedMutationHelperPublicationAtOrchestratorBoundary() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("remove"))
                    upsertTrack(track("remove-track", "remove"))
                }
            val playlists =
                SourceManagementPlaylistRepository().apply { create("Saved") }
            val coordinator = AppLibraryOperationCoordinator {}
            val orchestrator =
                AppLibraryOrchestrator(coordinator, publishError = {})
            val stale =
                coordinator.admitMutation(LibraryOperationKind.RemoveSource)
                    as LibraryOperationAdmission.Admitted
            coordinator.complete(stale.token)
            val current =
                coordinator.admitMutation(LibraryOperationKind.Clear)
                    as LibraryOperationAdmission.Admitted
            var observedContent: LibraryContentState? = null
            var observedPlaylistAction: PlaylistStateAction? = null
            val observedErrors = mutableListOf<String>()

            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                platformAccess = FakePlatformSourceAccess(),
                reconciler =
                    PlaybackSessionReconciler {
                        PlaybackSessionReconcileResult.Applied
                    },
                ioDispatcher = Dispatchers.Default,
                playlistStateOwner =
                    PlaylistStateOwner(playlists, Dispatchers.Default),
                publish = { publication ->
                    assertTrue(publication.content != null)
                    assertTrue(
                        publication.playlists
                            is PlaylistStateAction.SnapshotConfirmed)
                    orchestrator.publishIfCurrent(stale.token) {
                        observedContent = publication.content
                        observedPlaylistAction = publication.playlists
                        publication.errorMessage?.let(observedErrors::add)
                    }
                },
            )

            assertEquals(null, observedContent)
            assertEquals(null, observedPlaylistAction)
            assertEquals(emptyList(), observedErrors)
            coordinator.complete(current.token)
        }

    @Test
    fun rejectedMissingTrackRemovalPreservesAppStateAndPublishesOnlyFeedback() =
        runBlocking {
            val seededContent =
                LibraryContentState(
                    sources = listOf(source("source")),
                    tracks = listOf(track("existing-track", "source")),
                )
            val seededPlaylistState =
                PlaylistState(
                    confirmedSnapshot = PlaylistSnapshot(),
                    readErrorMessage = "existing playlist read error",
                    hasConfirmedSnapshot = true,
                )
            val seededScanErrors =
                listOf(
                    com.eterocell.rhythhaus.library.ScanError(
                        id = "existing-error",
                        scanId = "scan",
                        sourceLocalKey = "missing.mp3",
                        displayPath = "/missing.mp3",
                        reason = "missing",
                        recoverable = true,
                        createdAtEpochMillis = 1L,
                    ),
                )
            val coordinator = AppLibraryOperationCoordinator {}
            val feedback = mutableListOf<String>()
            val orchestrator =
                AppLibraryOrchestrator(coordinator, feedback::add)
            var libraryContent = seededContent
            var playlistState = seededPlaylistState
            var scanErrors = seededScanErrors

            orchestrator.launch(LibraryOperationKind.RemoveMissingTracks) {
                token ->
                removeMissingTracksInBackground(
                    sourceId = "source",
                    latestScanId = "scan",
                    repository =
                        FailingMutationRepository(
                            removeMissingTracksResult =
                                RemoveMissingTracksResult.Rejected(
                                    com.eterocell.rhythhaus.library
                                        .RemoveMissingTracksRejectionReason
                                        .StaleCompletedScan,
                                ),
                        ),
                    platformAccess = FakePlatformSourceAccess(),
                    reconciler =
                        PlaybackSessionReconciler {
                            PlaybackSessionReconcileResult.Applied
                        },
                    ioDispatcher = Dispatchers.Default,
                    playlistStateOwner = testPlaylistStateOwner(),
                    publish = { publication ->
                        orchestrator.publishIfCurrent(token) {
                            publication.content?.let { libraryContent = it }
                            scanErrors =
                                resolveMutationScanErrors(
                                    scanErrors, publication)
                            publication.playlists?.let { action ->
                                playlistState =
                                    reducePlaylistState(
                                        playlistState,
                                        action.requireSuccessfulPublication(),
                                    )
                            }
                            publication.errorMessage?.let(feedback::add)
                        }
                    },
                )
            }

            assertEquals(seededContent, libraryContent)
            assertEquals(seededPlaylistState, playlistState)
            assertEquals(seededScanErrors, scanErrors)
            assertEquals(
                listOf("Unable to remove missing tracks: StaleCompletedScan"),
                feedback,
            )
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun mutationHelperPlaylistReadFailurePublishesOrchestratorErrorOnceAndReturnsIdle() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("source"))
                }
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launch(LibraryOperationKind.RemoveSource) { token ->
                removeSourceInBackground(
                    sourceId = "source",
                    repository = repository,
                    platformAccess = FakePlatformSourceAccess(),
                    reconciler =
                        PlaybackSessionReconciler {
                            PlaybackSessionReconcileResult.Applied
                        },
                    ioDispatcher = Dispatchers.Default,
                    playlistStateOwner = failingPlaylistReadHarness().owner,
                    publish = { publication ->
                        orchestrator.publishIfCurrent(token) {
                            publication.playlists
                                ?.requireSuccessfulPublication()
                        }
                    },
                )
            }

            assertEquals(listOf("playlist_load_failed"), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun pickerActionIsVisibleForFirstSourceRegardlessOfAdditionalSourceCapability() {
        assertTrue(
            sourcePickerActionVisible(
                supportsAdditionalSources = true, sourceCount = 0))
        assertTrue(
            sourcePickerActionVisible(
                supportsAdditionalSources = false, sourceCount = 0))
    }

    @Test
    fun pickerActionRemainsVisibleForExistingSourcesWhenAdditionalSourcesAreSupported() {
        assertTrue(
            sourcePickerActionVisible(
                supportsAdditionalSources = true, sourceCount = 1))
    }

    @Test
    fun pickerActionIsHiddenForExistingSourceWhenAdditionalSourcesAreUnsupported() {
        assertFalse(
            sourcePickerActionVisible(
                supportsAdditionalSources = false, sourceCount = 1))
    }

    @Test
    fun sourceMutationsAreAllowedOnlyWhenProgressAndJobAreInactive() {
        assertTrue(
            sourceMutationsAllowed(
                isProgressActive = false, isJobActive = false))
        assertFalse(
            sourceMutationsAllowed(
                isProgressActive = true, isJobActive = false))
        assertFalse(
            sourceMutationsAllowed(
                isProgressActive = false, isJobActive = true))
        assertFalse(
            sourceMutationsAllowed(isProgressActive = true, isJobActive = true))
    }

    @Test
    fun emptyLibraryMutationGateBlocksJobOnlyScanStartupWindow() {
        assertFalse(
            emptyLibrarySourceMutationsAllowed(
                isProgressActive = false, isJobActive = true))
    }

    @Test
    fun pickedSourceReusesPersistedIdentityAndCreationTimeWhenHandleMatches() {
        val persisted =
            source("legacy-id")
                .copy(
                    handle = "content://provider/tree/Aa",
                    createdAtEpochMillis = 42L,
                )
        val picked =
            persisted.copy(
                id = "android-saf-new-id",
                displayName = "Renamed folder",
                createdAtEpochMillis = 99L,
            )

        assertEquals(
            picked.copy(id = "legacy-id", createdAtEpochMillis = 42L),
            normalizePickedSource(picked, listOf(persisted)),
        )
    }

    @Test
    fun pickedSourceKeepsNewIdentityWhenHandleDoesNotMatch() {
        val picked =
            source("new-id")
                .copy(
                    handle = "content://provider/tree/new",
                    createdAtEpochMillis = 99L)

        assertEquals(
            picked,
            normalizePickedSource(picked, listOf(source("existing-id"))))
    }

    @Test
    fun androidSafIdentityDistinguishesKnownJavaHashCollisionUris() {
        val first = "content://provider/tree/Aa"
        val second = "content://provider/tree/BB"
        assertEquals(first.hashCode(), second.hashCode())

        assertTrue(androidSafSourceId(first) != androidSafSourceId(second))
    }

    @Test
    fun jvmFolderIdentityDistinguishesCanonicalPathsWithLegacyHashCollision() {
        val first = "/music/Aa"
        val second = "/music/BB"
        assertEquals(first.hashCode(), second.hashCode())

        assertTrue(jvmFolderSourceId(first) != jvmFolderSourceId(second))
    }

    @Test
    fun repickedJvmFolderKeepsPersistedLegacyIdentityForExactHandle() {
        val canonicalPath = "/music/Aa"
        val persisted =
            source("jvm-folder-legacy-hash")
                .copy(
                    handle = canonicalPath,
                    createdAtEpochMillis = 42L,
                )
        val picked =
            persisted.copy(
                id = jvmFolderSourceId(canonicalPath),
                createdAtEpochMillis = 99L,
            )

        assertEquals(
            picked.copy(
                id = persisted.id,
                createdAtEpochMillis = persisted.createdAtEpochMillis),
            normalizePickedSource(picked, listOf(persisted)),
        )
    }

    @Test
    fun sourceMutationsFollowTerminalProgressWhenNoJobIsActive() {
        assertTrue(
            sourceMutationsAllowed(
                isProgressActive = false, isJobActive = false))
        assertFalse(
            sourceMutationsAllowed(
                isProgressActive = scanProgress(ScanStatus.Scanning).isActive,
                isJobActive = false))
        assertFalse(
            sourceMutationsAllowed(
                isProgressActive = scanProgress(ScanStatus.Cancelling).isActive,
                isJobActive = false))
        assertTrue(
            sourceMutationsAllowed(
                isProgressActive = scanProgress(ScanStatus.Completed).isActive,
                isJobActive = false))
    }

    @Test
    fun libraryStateRefreshDecoratesPersistedSourcesWithCurrentPlatformAccess() {
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(source("available"))
                upsertSource(source("lost"))
            }
        val platformAccess = FakePlatformSourceAccess(lostSourceId = "lost")

        val state = loadLibraryContent(repository, platformAccess)

        assertEquals(
            listOf(
                LibrarySourceAccessStatus.Available,
                LibrarySourceAccessStatus.LostAccess),
            state.sources.map { it.accessStatus },
        )
    }

    @Test
    fun sourceRemovalRefreshesBothSourcesAndTracks() = runBlocking {
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(source("remove"))
                upsertSource(source("keep"))
                upsertTrack(track("remove-track", "remove"))
                upsertTrack(track("keep-track", "keep"))
            }
        var refreshedState: LibraryContentState? = null

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            reconciler =
                PlaybackSessionReconciler {
                    PlaybackSessionReconcileResult.Applied
                },
            ioDispatcher = Dispatchers.Default,
            playlistStateOwner = testPlaylistStateOwner(),
            publish =
                testLibraryMutationPublication(
                    updateLibrary = { refreshedState = it }),
        )

        assertEquals(listOf("keep"), refreshedState?.sources?.map { it.id })
        assertEquals(
            listOf("keep-track"), refreshedState?.tracks?.map { it.id })
    }

    @Test
    fun sourceRemovalReconcilesBeforePublishing() = runBlocking {
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(source("remove"))
                upsertSource(source("keep"))
                upsertTrack(track("keep-track", "keep"))
            }
        val events = mutableListOf<String>()

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            reconciler =
                PlaybackSessionReconciler {
                    events += "reconcile"
                    PlaybackSessionReconcileResult.Applied
                },
            ioDispatcher = Dispatchers.Default,
            playlistStateOwner = testPlaylistStateOwner(),
            publish =
                testLibraryMutationPublication(
                    updateLibrary = { events += "publish" }),
        )

        assertEquals(listOf("reconcile", "publish"), events)
    }

    @Test
    fun sourceRemovalRefreshesAppOwnedPlaylistSnapshotAfterCascadeAndReconciliation() =
        runBlocking {
            val events = mutableListOf<String>()
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("remove"))
                    upsertTrack(track("remove-track", "remove"))
                }
            val playlists =
                RecordingPlaylistRepository(events).apply {
                    val playlist = create("Saved")
                    append(playlist.id, listOf("remove-track"))
                    events.clear()
                }
            val playlistStateOwner =
                PlaylistStateOwner(playlists, Dispatchers.Default)
            var refreshedContent: LibraryContentState? = null
            var refreshed: PlaylistSnapshot? = null

            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                platformAccess = FakePlatformSourceAccess(),
                reconciler =
                    PlaybackSessionReconciler {
                        events += "reconcile"
                        PlaybackSessionReconcileResult.Applied
                    },
                ioDispatcher = Dispatchers.Default,
                playlistStateOwner = playlistStateOwner,
                publish =
                    testLibraryMutationPublication(
                        updateLibrary = { refreshedContent = it },
                        updatePlaylists = { action ->
                            refreshed =
                                (action
                                        as
                                        PlaylistStateAction.SnapshotConfirmed)
                                    .snapshot
                        }),
            )

            assertEquals(
                listOf("playlist-entry-1"), refreshed?.playlists?.map { it.id })
            assertEquals(emptyList(), refreshedContent?.sources)
            assertEquals(emptyList(), refreshedContent?.tracks)
            assertEquals(listOf("reconcile", "read_playlists"), events)
        }

    @Test
    fun clearLibraryAccessReleaseFailurePropagatesWithoutPublishing() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("source"))
                }
            var published = false

            assertFailsWith<IllegalStateException> {
                clearLibraryInBackground(
                    repository = repository,
                    platformAccess = ThrowingReleasePlatformSourceAccess,
                    reconciler =
                        PlaybackSessionReconciler {
                            PlaybackSessionReconcileResult.Applied
                        },
                    ioDispatcher = Dispatchers.Default,
                    playlistStateOwner = testPlaylistStateOwner(),
                    publish =
                        testLibraryMutationPublication(
                            updateLibrary = { published = true }),
                )
            }

            assertEquals(false, published)
        }

    @Test
    fun clearLibraryActiveOwnerCancellationPublishesEmptyContentAndRethrows() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("source"))
                    upsertTrack(track("track", "source"))
                }
            val platformAccess =
                FakePlatformSourceAccess(repository = repository)
            var published: LibraryContentState? = null
            var errorMessage: String? = null

            assertFailsWith<CancellationException> {
                clearLibraryInBackground(
                    repository = repository,
                    platformAccess = platformAccess,
                    reconciler =
                        PlaybackSessionReconciler {
                            throw CancellationException("clear cancelled")
                        },
                    ioDispatcher = Dispatchers.Default,
                    playlistStateOwner = testPlaylistStateOwner(),
                    publish =
                        testLibraryMutationPublication(
                            updateLibrary = { published = it },
                            updateError = { errorMessage = it }),
                )
            }

            assertEquals(emptyList(), published?.sources)
            assertEquals(emptyList(), published?.tracks)
            assertEquals("clear cancelled", errorMessage)
            assertEquals(
                listOf("source"), platformAccess.releasedSources.map { it.id })
            assertEquals(false, platformAccess.sourceWasPresentWhenReleased)
        }

    @Test
    fun clearLibraryGoneOwnerCancellationDoesNotPublishOrReportError() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(source("source"))
                }
            var published = false
            var errorReported = false

            assertFailsWith<CancellationException> {
                clearLibraryInBackground(
                    repository = repository,
                    platformAccess =
                        FakePlatformSourceAccess(repository = repository),
                    reconciler =
                        PlaybackSessionReconciler {
                            throw CancellationException("gone")
                        },
                    ioDispatcher = Dispatchers.Default,
                    playlistStateOwner = testPlaylistStateOwner(),
                    ownerIsActive = { false },
                    publish =
                        testLibraryMutationPublication(
                            updateLibrary = { published = true },
                            updateError = { errorReported = true }),
                )
            }

            assertEquals(false, published)
            assertEquals(false, errorReported)
        }

    private fun source(id: String) =
        LibrarySource(
            id = id,
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = id,
            handle = "/$id",
            createdAtEpochMillis = 1L,
        )

    private fun track(id: String, sourceId: String) =
        LibraryTrack(
            id = id,
            sourceId = sourceId,
            sourceLocalKey = "$id.mp3",
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = "$id.mp3",
            title = id,
            artist = "Artist",
            album = "Album",
            durationMillis = null,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private fun scanProgress(status: ScanStatus) =
        ScanProgress(
            session =
                ScanSession(
                    id = "scan",
                    sourceId = "source",
                    status = status,
                    startedAtEpochMillis = 1L,
                ),
        )
}

private fun testLibraryMutationPublication(
    updateLibrary: suspend (LibraryContentState) -> Unit = {},
    updatePlaylists: suspend (PlaylistStateAction) -> Unit = {},
    updateError: suspend (String) -> Unit = {},
): suspend (LibraryMutationPublication) -> Unit = { publication ->
    publication.content?.let { updateLibrary(it) }
    publication.playlists?.let { updatePlaylists(it) }
    publication.errorMessage?.let { updateError(it) }
}

private fun testPlaylistStateOwner() =
    PlaylistStateOwner(
        SourceManagementPlaylistRepository(), Dispatchers.Default)

private fun cancellingReconciler(events: MutableList<String>) =
    PlaybackSessionReconciler {
        events += "reconcile"
        val cancellation = CancellationException("cancelled")
        currentCoroutineContext().cancel(cancellation)
        throw cancellation
    }

private class FailingPlaylistReadHarness {
    private var reads = 0
    private val confirmedPlaylist =
        com.eterocell.rhythhaus.library.PlaylistSummary(
            id = "playlist-1",
            name = "Saved",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
    private val repository =
        object : SourceManagementPlaylistRepository() {
            override fun playlists():
                List<com.eterocell.rhythhaus.library.PlaylistSummary> {
                reads += 1
                error("playlist read failed")
            }
        }
    val owner = PlaylistStateOwner(repository, Dispatchers.Default)
    val initialState =
        PlaylistState(
            confirmedSnapshot =
                PlaylistSnapshot(playlists = listOf(confirmedPlaylist)),
            hasConfirmedSnapshot = true,
        )

    fun readCount(): Int = reads
}

private fun failingPlaylistReadHarness() = FailingPlaylistReadHarness()

private class RecordingPlaylistRepository(
    private val events: MutableList<String>,
) : com.eterocell.rhythhaus.library.PlaylistRepository {
    private val delegate = SourceManagementPlaylistRepository()

    override fun playlists():
        List<com.eterocell.rhythhaus.library.PlaylistSummary> {
        events += "read_playlists"
        return delegate.playlists()
    }

    override fun playlist(id: String) = delegate.playlist(id)

    override fun entries(playlistId: String) = delegate.entries(playlistId)

    override fun create(name: String) = delegate.create(name)

    override fun createWithEntries(name: String, trackIds: List<String>) =
        delegate.createWithEntries(name, trackIds)

    override fun importPlaylists(
        playlists: List<com.eterocell.rhythhaus.library.PlaylistImportMutation>
    ) = delegate.importPlaylists(playlists)

    override fun rename(id: String, name: String) = delegate.rename(id, name)

    override fun delete(id: String) = delegate.delete(id)

    override fun append(playlistId: String, trackIds: List<String>) =
        delegate.append(playlistId, trackIds)

    override fun removeEntry(entryId: String) = delegate.removeEntry(entryId)

    override fun reorder(playlistId: String, entryIds: List<String>) =
        delegate.reorder(playlistId, entryIds)
}

private open class SourceManagementPlaylistRepository :
    com.eterocell.rhythhaus.library.PlaylistRepository {
    private val playlists =
        linkedMapOf<String, com.eterocell.rhythhaus.library.PlaylistSummary>()
    private val entries =
        linkedMapOf<
            String,
            MutableList<com.eterocell.rhythhaus.library.PlaylistEntry>>()
    private var nextId = 1

    override fun playlists() = playlists.values.toList()

    override fun playlist(id: String) = playlists[id]

    override fun entries(playlistId: String) =
        entries[playlistId].orEmpty().toList()

    override fun create(name: String) = createWithEntries(name, emptyList())

    override fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): com.eterocell.rhythhaus.library.PlaylistSummary {
        val id = "playlist-entry-${nextId++}"
        val playlist =
            com.eterocell.rhythhaus.library.PlaylistSummary(id, name, 1L, 1L)
        playlists[id] = playlist
        entries[id] =
            trackIds
                .mapIndexed { index, trackId ->
                    com.eterocell.rhythhaus.library.PlaylistEntry(
                        "playlist-entry-${nextId++}", id, trackId, index, 1L)
                }
                .toMutableList()
        return playlist
    }

    override fun importPlaylists(
        playlists: List<com.eterocell.rhythhaus.library.PlaylistImportMutation>
    ) = playlists.map { createWithEntries(it.name, it.trackIds) }

    override fun rename(id: String, name: String) {
        playlists[id]?.let {
            playlists[id] = it.copy(name = name, updatedAtEpochMillis = 1L)
        }
    }

    override fun delete(id: String) {
        playlists.remove(id)
        entries.remove(id)
    }

    override fun append(playlistId: String, trackIds: List<String>) {
        val list = entries.getOrPut(playlistId) { mutableListOf() }
        trackIds.forEach { trackId ->
            list +=
                com.eterocell.rhythhaus.library.PlaylistEntry(
                    "playlist-entry-${nextId++}",
                    playlistId,
                    trackId,
                    list.size,
                    1L)
        }
    }

    override fun removeEntry(entryId: String) {
        entries.values.forEach { list ->
            list.removeAll { it.id == entryId }
            list.indices.forEach { index ->
                list[index] = list[index].copy(position = index)
            }
        }
    }

    override fun reorder(playlistId: String, entryIds: List<String>) {
        val list = entries[playlistId] ?: return
        val byId = list.associateBy { it.id }
        list.clear()
        entryIds.mapNotNull(byId::get).forEachIndexed { index, entry ->
            list += entry.copy(position = index)
        }
    }
}

private class FakePlatformSourceAccess(
    private val lostSourceId: String? = null,
    private val repository: com.eterocell.rhythhaus.library.LibraryRepository? =
        null,
) : PlatformSourceAccess {
    val releasedSources = mutableListOf<LibrarySource>()
    var sourceWasPresentWhenReleased = false
        private set

    override fun accessStatus(
        source: LibrarySource
    ): LibrarySourceAccessStatus =
        if (source.id == lostSourceId) LibrarySourceAccessStatus.LostAccess
        else LibrarySourceAccessStatus.Available

    override fun releaseAccess(source: LibrarySource) {
        sourceWasPresentWhenReleased =
            sourceWasPresentWhenReleased ||
                repository?.sources()?.any { it.id == source.id } == true
        releasedSources += source
    }

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private object ThrowingReleasePlatformSourceAccess : PlatformSourceAccess {
    override fun releaseAccess(source: LibrarySource): Unit =
        throw IllegalStateException("release failed")

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private class FailingMutationRepository(
    private val sources: List<LibrarySource> = emptyList(),
    private val removeMissingTracksResult: RemoveMissingTracksResult =
        RemoveMissingTracksResult.Removed(0),
) : com.eterocell.rhythhaus.library.LibraryRepository {
    override fun upsertSource(source: LibrarySource) = Unit

    override fun sources(): List<LibrarySource> = sources

    override fun upsertTrack(track: LibraryTrack) = error("unused")

    override fun tracks(): List<LibraryTrack> = emptyList()

    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        emptyList()

    override fun artworkForTrack(trackId: String) = null

    override fun insertScanSession(session: ScanSession) = Unit

    override fun updateScanSession(session: ScanSession) = Unit

    override fun insertScanError(
        error: com.eterocell.rhythhaus.library.ScanError
    ) = Unit

    override fun scanErrors(scanId: String) =
        emptyList<com.eterocell.rhythhaus.library.ScanError>()

    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String
    ) = removeMissingTracksResult

    override fun latestTerminalScanSession(): ScanSession? = null

    override fun removeSource(sourceId: String): Unit =
        throw IllegalStateException("remove failed")

    override fun clearAll(): Unit = throw IllegalStateException("clear failed")
}
