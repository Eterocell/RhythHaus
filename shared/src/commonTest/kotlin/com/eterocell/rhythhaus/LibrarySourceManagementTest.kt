package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.InMemoryPlaylistRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformScanEvent
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.androidSafSourceId
import com.eterocell.rhythhaus.library.emptyLibrarySourceMutationsAllowed
import com.eterocell.rhythhaus.library.jvmFolderSourceId
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.settings.SourceAccessLabel
import com.eterocell.rhythhaus.settings.SourceDialogName
import com.eterocell.rhythhaus.settings.SourceScanLabel
import com.eterocell.rhythhaus.settings.sourceDialogName
import com.eterocell.rhythhaus.settings.sourceManagementLabels
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibrarySourceManagementTest {
    @Test
    fun pickerActionIsVisibleForFirstSourceRegardlessOfAdditionalSourceCapability() {
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = true, sourceCount = 0))
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = false, sourceCount = 0))
    }

    @Test
    fun pickerActionRemainsVisibleForExistingSourcesWhenAdditionalSourcesAreSupported() {
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = true, sourceCount = 1))
    }

    @Test
    fun pickerActionIsHiddenForExistingSourceWhenAdditionalSourcesAreUnsupported() {
        assertFalse(sourcePickerActionVisible(supportsAdditionalSources = false, sourceCount = 1))
    }

    @Test
    fun sourceMutationsAreAllowedOnlyWhenProgressAndJobAreInactive() {
        assertTrue(sourceMutationsAllowed(isProgressActive = false, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = true, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = false, isJobActive = true))
        assertFalse(sourceMutationsAllowed(isProgressActive = true, isJobActive = true))
    }

    @Test
    fun emptyLibraryMutationGateBlocksJobOnlyScanStartupWindow() {
        assertFalse(emptyLibrarySourceMutationsAllowed(isProgressActive = false, isJobActive = true))
    }

    @Test
    fun pickedSourceReusesPersistedIdentityAndCreationTimeWhenHandleMatches() {
        val persisted = source("legacy-id").copy(
            handle = "content://provider/tree/Aa",
            createdAtEpochMillis = 42L,
        )
        val picked = persisted.copy(
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
        val picked = source("new-id").copy(handle = "content://provider/tree/new", createdAtEpochMillis = 99L)

        assertEquals(picked, normalizePickedSource(picked, listOf(source("existing-id"))))
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
        val persisted = source("jvm-folder-legacy-hash").copy(
            handle = canonicalPath,
            createdAtEpochMillis = 42L,
        )
        val picked = persisted.copy(
            id = jvmFolderSourceId(canonicalPath),
            createdAtEpochMillis = 99L,
        )

        assertEquals(
            picked.copy(id = persisted.id, createdAtEpochMillis = persisted.createdAtEpochMillis),
            normalizePickedSource(picked, listOf(persisted)),
        )
    }

    @Test
    fun sourceManagementLabelsMapAccessAndLastScanState() {
        assertEquals(
            SourceAccessLabel.Available to SourceScanLabel.NeverScanned,
            sourceManagementLabels(source("available")),
        )
        assertEquals(
            SourceAccessLabel.LostAccess to SourceScanLabel.LastScanned,
            sourceManagementLabels(
                source("lost").copy(
                    accessStatus = LibrarySourceAccessStatus.LostAccess,
                    lastScanAtEpochMillis = 2L,
                ),
            ),
        )
    }

    @Test
    fun sourceMutationsFollowTerminalProgressWhenNoJobIsActive() {
        assertTrue(sourceMutationsAllowed(isProgressActive = false, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Scanning).isActive, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Cancelling).isActive, isJobActive = false))
        assertTrue(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Completed).isActive, isJobActive = false))
    }

    @Test
    fun removalDialogBoundsVisualNameButPreservesFullAccessibilityName() {
        val fullName = "A".repeat(80)

        assertEquals(
            SourceDialogName(
                visual = "${"A".repeat(63)}…",
                accessibility = fullName,
            ),
            sourceDialogName(source("source").copy(displayName = fullName), unnamedLabel = "Unnamed folder"),
        )
    }

    @Test
    fun blankSourceDisplayNameUsesNeutralLabelWithoutExposingHandle() {
        val unnamed = source("source").copy(displayName = "", handle = "content://private/provider/tree/secret")

        assertEquals(
            SourceDialogName(visual = "Unnamed folder", accessibility = "Unnamed folder"),
            sourceDialogName(unnamed, unnamedLabel = "Unnamed folder"),
        )
    }

    @Test
    fun libraryStateRefreshDecoratesPersistedSourcesWithCurrentPlatformAccess() {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("available"))
            upsertSource(source("lost"))
        }
        val platformAccess = FakePlatformSourceAccess(lostSourceId = "lost")

        val state = loadLibraryContent(repository, platformAccess)

        assertEquals(
            listOf(LibrarySourceAccessStatus.Available, LibrarySourceAccessStatus.LostAccess),
            state.sources.map { it.accessStatus },
        )
    }

    @Test
    fun sourceRemovalRefreshesBothSourcesAndTracks() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
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
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { refreshedState = it },
        )

        assertEquals(listOf("keep"), refreshedState?.sources?.map { it.id })
        assertEquals(listOf("keep-track"), refreshedState?.tracks?.map { it.id })
    }

    @Test
    fun sourceRemovalReconcilesBeforePublishing() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
            upsertSource(source("keep"))
            upsertTrack(track("keep-track", "keep"))
        }
        val events = mutableListOf<String>()

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { events += "publish" },
        )

        assertEquals(listOf("reconcile", "publish"), events)
    }

    @Test
    fun sourceRemovalRefreshesAppOwnedPlaylistSnapshotAfterCascadeAndReconciliation() = runBlocking {
        val events = mutableListOf<String>()
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
            upsertTrack(track("remove-track", "remove"))
        }
        val playlists = RecordingPlaylistRepository(events).apply {
            val playlist = create("Saved")
            append(playlist.id, listOf("remove-track"))
            events.clear()
        }

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            loadPlaylists = { PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot(playlists)) },
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { events += "library" },
            updatePlaylists = { events += "playlists" },
        )

        assertEquals(listOf("reconcile", "read_playlists", "library", "playlists"), events)
    }

    @Test
    fun clearLibraryRefreshesAppOwnedPlaylistSnapshotAfterCascadeAndReconciliation() = runBlocking {
        val events = mutableListOf<String>()
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        val playlists = RecordingPlaylistRepository(events).apply {
            val playlist = create("Saved")
            append(playlist.id, listOf("track"))
            events.clear()
        }

        clearLibraryInBackground(
            repository = repository,
            loadPlaylists = { PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot(playlists)) },
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { events += "library" },
            updatePlaylists = { events += "playlists" },
        )

        assertEquals(listOf("reconcile", "read_playlists", "library", "playlists"), events)
    }

    @Test
    fun rescanRefreshesAppOwnedPlaylistSnapshotAfterReconciliation() = runBlocking {
        val events = mutableListOf<String>()
        val playlists = RecordingPlaylistRepository(events).apply {
            create("Saved")
            events.clear()
        }
        val content = LibraryContentState(sources = emptyList(), tracks = emptyList())
        val session = ScanSession(
            id = "scan",
            sourceId = "source",
            status = ScanStatus.Completed,
            startedAtEpochMillis = 1L,
        )

        publishScanContentAfterReconcile(
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            loadPlaylists = { PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot(playlists)) },
            content = content,
            session = session,
            ownerIsActive = { true },
            publish = { publication ->
                events += "publish"
                val playlistAction = publication.playlists as PlaylistStateAction.SnapshotConfirmed
                assertEquals(listOf("playlist-entry-1"), playlistAction.snapshot.playlists.map { it.id })
            },
        )

        assertEquals(listOf("reconcile", "read_playlists", "publish"), events)
    }

    @Test
    fun rescanPlaylistReadFailurePublishesTerminalScanAndRetainsConfirmedPlaylists() = runBlocking {
        val harness = failingPlaylistReadHarness()
        var state = harness.initialState
        var published: ScanPublicationState? = null

        publishScanContentAfterReconcile(
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            loadPlaylists = { harness.owner.refresh("playlist_load_failed") },
            content = LibraryContentState(emptyList(), emptyList()),
            session = ScanSession("scan", "source", ScanStatus.Completed, 1L),
            ownerIsActive = { true },
            publish = { publication ->
                published = publication
                publication.playlists?.let { state = reducePlaylistState(state, it) }
            },
        )

        assertEquals(ScanStatus.Completed, published?.progress?.session?.status)
        assertEquals("playlist-1", state.confirmedSnapshot.playlists.single().id)
        assertEquals("playlist_load_failed", state.readErrorMessage)
        assertEquals(1, harness.readCount())
    }

    @Test
    fun sourceRemovalPlaylistReadFailurePublishesLibraryAndRetainsConfirmedPlaylists() = runBlocking {
        val harness = failingPlaylistReadHarness()
        val repository = InMemoryLibraryRepository().apply { upsertSource(source("remove")) }
        var state = harness.initialState
        var libraryPublished = false

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            loadPlaylists = { harness.owner.refresh("playlist_load_failed") },
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { libraryPublished = true },
            updatePlaylists = { state = reducePlaylistState(state, it) },
        )

        assertTrue(libraryPublished)
        assertEquals("playlist-1", state.confirmedSnapshot.playlists.single().id)
        assertEquals("playlist_load_failed", state.readErrorMessage)
        assertEquals(1, harness.readCount())
    }

    @Test
    fun clearLibraryPlaylistReadFailurePublishesLibraryAndRetainsConfirmedPlaylists() = runBlocking {
        val harness = failingPlaylistReadHarness()
        val repository = InMemoryLibraryRepository().apply { upsertSource(source("source")) }
        var state = harness.initialState
        var libraryPublished = false

        clearLibraryInBackground(
            repository = repository,
            loadPlaylists = { harness.owner.refresh("playlist_load_failed") },
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { libraryPublished = true },
            updatePlaylists = { state = reducePlaylistState(state, it) },
        )

        assertTrue(libraryPublished)
        assertEquals("playlist-1", state.confirmedSnapshot.playlists.single().id)
        assertEquals("playlist_load_failed", state.readErrorMessage)
        assertEquals(1, harness.readCount())
    }

    @Test
    fun cancelledRescanCompletesAuthoritativePlaylistReadAndPublicationBeforeRethrow() = runBlocking {
        val events = mutableListOf<String>()
        val operation = async {
            publishScanContentAfterReconcile(
                reconciler = cancellingReconciler(events),
                loadPlaylists = {
                    yield()
                    events += "read_playlists"
                    PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.PlaylistSnapshot())
                },
                content = LibraryContentState(emptyList(), emptyList()),
                session = ScanSession("scan", "source", ScanStatus.Scanning, 1L),
                ownerIsActive = { true },
                publish = { events += "publish" },
            )
        }

        assertFailsWith<CancellationException> { operation.await() }
        assertEquals(listOf("reconcile", "read_playlists", "publish"), events)
    }

    @Test
    fun cancelledSourceRemovalCompletesAuthoritativePlaylistPublicationBeforeRethrow() = runBlocking {
        val events = mutableListOf<String>()
        val repository = InMemoryLibraryRepository().apply { upsertSource(source("remove")) }
        val operation = async {
            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                loadPlaylists = {
                    yield()
                    events += "read_playlists"
                    PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.PlaylistSnapshot())
                },
                platformAccess = FakePlatformSourceAccess(),
                reconciler = cancellingReconciler(events),
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { true },
                updateLibrary = { events += "library" },
                updatePlaylists = { events += "playlists" },
            )
        }

        assertFailsWith<CancellationException> { operation.await() }
        assertEquals(listOf("reconcile", "read_playlists", "library", "playlists"), events)
    }

    @Test
    fun cancelledClearLibraryCompletesAuthoritativePlaylistPublicationBeforeRethrow() = runBlocking {
        val events = mutableListOf<String>()
        val repository = InMemoryLibraryRepository().apply { upsertSource(source("source")) }
        val operation = async {
            clearLibraryInBackground(
                repository = repository,
                loadPlaylists = {
                    yield()
                    events += "read_playlists"
                    PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.PlaylistSnapshot())
                },
                platformAccess = FakePlatformSourceAccess(),
                reconciler = cancellingReconciler(events),
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { true },
                updateLibrary = { events += "library" },
                updatePlaylists = { events += "playlists" },
            )
        }

        assertFailsWith<CancellationException> { operation.await() }
        assertEquals(listOf("reconcile", "read_playlists", "library", "playlists"), events)
    }

    @Test
    fun sourceRemovalReleasesAccessOnlyAfterRepositoryDeletion() = runBlocking {
        val removedSource = source("remove")
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(removedSource)
        }
        val platformAccess = FakePlatformSourceAccess(repository = repository)

        removeSourceInBackground(
            sourceId = removedSource.id,
            repository = repository,
            platformAccess = platformAccess,
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = {},
        )

        assertEquals(listOf(removedSource), platformAccess.releasedSources)
        assertEquals(false, platformAccess.sourceWasPresentWhenReleased)
    }

    @Test
    fun sourceRemovalDoesNotReleaseAccessWhenRepositoryDeletionFails() = runBlocking {
        val platformAccess = FakePlatformSourceAccess()

        assertFailsWith<IllegalStateException> {
            removeSourceInBackground(
                sourceId = "remove",
                repository = FailingMutationRepository(),
                platformAccess = platformAccess,
                reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
                ioDispatcher = Dispatchers.Default,
                updateLibrary = {},
            )
        }

        assertEquals(emptyList(), platformAccess.releasedSources)
    }

    @Test
    fun sourceRemovalReconcileFailurePublishesAuthoritativeContentAndError() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
            upsertSource(source("keep"))
            upsertTrack(track("remove-track", "remove"))
            upsertTrack(track("keep-track", "keep"))
        }
        val platformAccess = FakePlatformSourceAccess(repository = repository)
        var published: LibraryContentState? = null
        var errorMessage: String? = null

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            platformAccess = platformAccess,
            reconciler = PlaybackSessionReconciler { throw IllegalStateException("remove reconcile failed") },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { published = it },
            updateError = { errorMessage = it },
        )

        assertEquals(listOf("keep"), published?.sources?.map { it.id })
        assertEquals(listOf("keep-track"), published?.tracks?.map { it.id })
        assertEquals("remove reconcile failed", errorMessage)
        assertEquals(listOf("remove"), platformAccess.releasedSources.map { it.id })
    }

    @Test
    fun sourceRemovalAccessReleaseFailurePropagatesWithoutPublishing() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
        }
        var published = false

        assertFailsWith<IllegalStateException> {
            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                platformAccess = ThrowingReleasePlatformSourceAccess,
                reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
                ioDispatcher = Dispatchers.Default,
                updateLibrary = { published = true },
            )
        }

        assertEquals(false, published)
    }

    @Test
    fun sourceRemovalActiveOwnerCancellationPublishesAuthoritativeContentAndRethrows() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
            upsertSource(source("keep"))
            upsertTrack(track("remove-track", "remove"))
            upsertTrack(track("keep-track", "keep"))
        }
        val platformAccess = FakePlatformSourceAccess(repository = repository)
        var published: LibraryContentState? = null
        var errorMessage: String? = null

        assertFailsWith<CancellationException> {
            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                platformAccess = platformAccess,
                reconciler = PlaybackSessionReconciler { throw CancellationException("remove cancelled") },
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { true },
                updateLibrary = { published = it },
                updateError = { errorMessage = it },
            )
        }

        assertEquals(listOf("keep"), published?.sources?.map { it.id })
        assertEquals(listOf("keep-track"), published?.tracks?.map { it.id })
        assertEquals("remove cancelled", errorMessage)
        assertEquals(listOf("remove"), platformAccess.releasedSources.map { it.id })
        assertEquals(false, platformAccess.sourceWasPresentWhenReleased)
    }

    @Test
    fun sourceRemovalGoneOwnerCancellationDoesNotPublishOrReportError() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
        }
        var published = false
        var errorReported = false

        assertFailsWith<CancellationException> {
            removeSourceInBackground(
                sourceId = "remove",
                repository = repository,
                platformAccess = FakePlatformSourceAccess(repository = repository),
                reconciler = PlaybackSessionReconciler { throw CancellationException("gone") },
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { false },
                updateLibrary = { published = true },
                updateError = { errorReported = true },
            )
        }

        assertEquals(false, published)
        assertEquals(false, errorReported)
    }

    @Test
    fun clearLibraryRefreshesBothSourcesAndTracks() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        var refreshedState: LibraryContentState? = null

        clearLibraryInBackground(
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { refreshedState = it },
        )

        assertEquals(emptyList(), refreshedState?.sources)
        assertEquals(emptyList(), refreshedState?.tracks)
    }

    @Test
    fun clearLibraryReconcilesBeforePublishingFailedSafeContent() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        val events = mutableListOf<String>()

        clearLibraryInBackground(
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.FailedSafeApplied
            },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { events += "publish" },
        )

        assertEquals(listOf("reconcile", "publish"), events)
    }

    @Test
    fun clearLibraryReleasesEverySnapshottedSourceAfterRepositoryClear() = runBlocking {
        val first = source("first")
        val second = source("second")
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(first)
            upsertSource(second)
        }
        val platformAccess = FakePlatformSourceAccess(repository = repository)

        clearLibraryInBackground(
            repository = repository,
            platformAccess = platformAccess,
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = {},
        )

        assertEquals(listOf(first, second), platformAccess.releasedSources)
        assertEquals(false, platformAccess.sourceWasPresentWhenReleased)
    }

    @Test
    fun clearLibraryDoesNotReleaseAccessWhenRepositoryClearFails() = runBlocking {
        val platformAccess = FakePlatformSourceAccess()

        assertFailsWith<IllegalStateException> {
            clearLibraryInBackground(
                repository = FailingMutationRepository(sources = listOf(source("first"))),
                platformAccess = platformAccess,
                reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
                ioDispatcher = Dispatchers.Default,
                updateLibrary = {},
            )
        }

        assertEquals(emptyList(), platformAccess.releasedSources)
    }

    @Test
    fun clearLibraryReconcileFailurePublishesEmptyContentAndError() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        var published: LibraryContentState? = null
        var errorMessage: String? = null

        clearLibraryInBackground(
            repository = repository,
            platformAccess = FakePlatformSourceAccess(repository = repository),
            reconciler = PlaybackSessionReconciler { throw IllegalStateException("clear reconcile failed") },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { published = it },
            updateError = { errorMessage = it },
        )

        assertEquals(emptyList(), published?.sources)
        assertEquals(emptyList(), published?.tracks)
        assertEquals("clear reconcile failed", errorMessage)
    }

    @Test
    fun clearLibraryReconcileFailureStillRefreshesAppOwnedPlaylistSnapshot() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        val playlists = RecordingPlaylistRepository(mutableListOf()).apply {
            create("Saved")
        }
        var refreshed: com.eterocell.rhythhaus.library.ui.PlaylistSnapshot? = null

        clearLibraryInBackground(
            repository = repository,
            loadPlaylists = { PlaylistStateAction.SnapshotConfirmed(com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot(playlists)) },
            platformAccess = FakePlatformSourceAccess(repository = repository),
            reconciler = PlaybackSessionReconciler { throw IllegalStateException("clear reconcile failed") },
            ioDispatcher = Dispatchers.Default,
            updateLibrary = {},
            updatePlaylists = { action ->
                refreshed = (action as PlaylistStateAction.SnapshotConfirmed).snapshot
            },
        )

        assertEquals(listOf("playlist-entry-1"), refreshed?.playlists?.map { it.id })
    }

    @Test
    fun clearLibraryAccessReleaseFailurePropagatesWithoutPublishing() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
        }
        var published = false

        assertFailsWith<IllegalStateException> {
            clearLibraryInBackground(
                repository = repository,
                platformAccess = ThrowingReleasePlatformSourceAccess,
                reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
                ioDispatcher = Dispatchers.Default,
                updateLibrary = { published = true },
            )
        }

        assertEquals(false, published)
    }

    @Test
    fun clearLibraryActiveOwnerCancellationPublishesEmptyContentAndRethrows() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        val platformAccess = FakePlatformSourceAccess(repository = repository)
        var published: LibraryContentState? = null
        var errorMessage: String? = null

        assertFailsWith<CancellationException> {
            clearLibraryInBackground(
                repository = repository,
                platformAccess = platformAccess,
                reconciler = PlaybackSessionReconciler { throw CancellationException("clear cancelled") },
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { true },
                updateLibrary = { published = it },
                updateError = { errorMessage = it },
            )
        }

        assertEquals(emptyList(), published?.sources)
        assertEquals(emptyList(), published?.tracks)
        assertEquals("clear cancelled", errorMessage)
        assertEquals(listOf("source"), platformAccess.releasedSources.map { it.id })
        assertEquals(false, platformAccess.sourceWasPresentWhenReleased)
    }

    @Test
    fun clearLibraryGoneOwnerCancellationDoesNotPublishOrReportError() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
        }
        var published = false
        var errorReported = false

        assertFailsWith<CancellationException> {
            clearLibraryInBackground(
                repository = repository,
                platformAccess = FakePlatformSourceAccess(repository = repository),
                reconciler = PlaybackSessionReconciler { throw CancellationException("gone") },
                ioDispatcher = Dispatchers.Default,
                ownerIsActive = { false },
                updateLibrary = { published = true },
                updateError = { errorReported = true },
            )
        }

        assertEquals(false, published)
        assertEquals(false, errorReported)
    }

    private fun source(id: String) = LibrarySource(
        id = id,
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = id,
        handle = "/$id",
        createdAtEpochMillis = 1L,
    )

    private fun track(id: String, sourceId: String) = LibraryTrack(
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

    private fun scanProgress(status: ScanStatus) = ScanProgress(
        session = ScanSession(
            id = "scan",
            sourceId = "source",
            status = status,
            startedAtEpochMillis = 1L,
        ),
    )
}

private fun cancellingReconciler(events: MutableList<String>) = PlaybackSessionReconciler {
    events += "reconcile"
    val cancellation = CancellationException("cancelled")
    currentCoroutineContext().cancel(cancellation)
    throw cancellation
}

private class FailingPlaylistReadHarness {
    private var reads = 0
    private val confirmedPlaylist = com.eterocell.rhythhaus.library.Playlist(
        id = "playlist-1",
        name = "Saved",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
    private val repository = object : com.eterocell.rhythhaus.library.PlaylistRepository by InMemoryPlaylistRepository() {
        override fun playlists(): List<com.eterocell.rhythhaus.library.Playlist> {
            reads += 1
            error("playlist read failed")
        }
    }
    val owner = PlaylistStateOwner(repository, Dispatchers.Default)
    val initialState = PlaylistState(
        confirmedSnapshot = PlaylistSnapshot(playlists = listOf(confirmedPlaylist)),
        hasConfirmedSnapshot = true,
    )
    fun readCount(): Int = reads
}

private fun failingPlaylistReadHarness() = FailingPlaylistReadHarness()

private class RecordingPlaylistRepository(
    private val events: MutableList<String>,
) : com.eterocell.rhythhaus.library.PlaylistRepository {
    private val delegate = InMemoryPlaylistRepository(
        now = { 1L },
        idFactory = generateSequence(1) { it + 1 }.map { "playlist-entry-$it" }.iterator()::next,
    )

    override fun playlists(): List<com.eterocell.rhythhaus.library.Playlist> {
        events += "read_playlists"
        return delegate.playlists()
    }

    override fun playlist(id: String) = delegate.playlist(id)
    override fun entries(playlistId: String) = delegate.entries(playlistId)
    override fun create(name: String) = delegate.create(name)
    override fun createWithEntries(name: String, trackIds: List<String>) = delegate.createWithEntries(name, trackIds)
    override fun rename(id: String, name: String) = delegate.rename(id, name)
    override fun delete(id: String) = delegate.delete(id)
    override fun append(playlistId: String, trackIds: List<String>) = delegate.append(playlistId, trackIds)
    override fun removeEntry(entryId: String) = delegate.removeEntry(entryId)
    override fun reorder(playlistId: String, entryIds: List<String>) = delegate.reorder(playlistId, entryIds)
}

private class FakePlatformSourceAccess(
    private val lostSourceId: String? = null,
    private val repository: com.eterocell.rhythhaus.library.LibraryRepository? = null,
) : PlatformSourceAccess {
    val releasedSources = mutableListOf<LibrarySource>()
    var sourceWasPresentWhenReleased = false
        private set

    override fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus =
        if (source.id == lostSourceId) LibrarySourceAccessStatus.LostAccess else LibrarySourceAccessStatus.Available

    override fun releaseAccess(source: LibrarySource) {
        sourceWasPresentWhenReleased = sourceWasPresentWhenReleased || repository?.sources()?.any { it.id == source.id } == true
        releasedSources += source
    }

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = emptySequence()
}

private object ThrowingReleasePlatformSourceAccess : PlatformSourceAccess {
    override fun releaseAccess(source: LibrarySource): Unit = throw IllegalStateException("release failed")
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = emptySequence()
}

private class FailingMutationRepository(
    private val sources: List<LibrarySource> = emptyList(),
) : com.eterocell.rhythhaus.library.LibraryRepository {
    override fun upsertSource(source: LibrarySource) = Unit
    override fun sources(): List<LibrarySource> = sources
    override fun upsertTrack(track: LibraryTrack) = error("unused")
    override fun tracks(): List<LibraryTrack> = emptyList()
    override fun tracksForSource(sourceId: String): List<LibraryTrack> = emptyList()
    override fun artworkForTrack(trackId: String) = null
    override fun insertScanSession(session: ScanSession) = Unit
    override fun updateScanSession(session: ScanSession) = Unit
    override fun insertScanError(error: com.eterocell.rhythhaus.library.ScanError) = Unit
    override fun scanErrors(scanId: String) = emptyList<com.eterocell.rhythhaus.library.ScanError>()
    override fun removeMissingTracks(sourceId: String, latestScanId: String) = 0
    override fun removeSource(sourceId: String): Unit = throw IllegalStateException("remove failed")
    override fun clearAll(): Unit = throw IllegalStateException("clear failed")
}
