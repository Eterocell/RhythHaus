package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackUpsertResult
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers

class AppMutationCancellationTest {
    @Test
    fun removeSourceCancellationSurvivesReadFailedPlaylistCleanup() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.RemoveSource,
            playlistStateOwner = failingPlaylistStateOwner(),
            operation = removeSourceOperation(),
            expectedPublicationCalls = 0,
            cleanupPublication = { publication ->
                publication.playlists?.requireSuccessfulPublication()
            },
        )

    @Test
    fun removeSourceCancellationSurvivesThrowingPublicationCallback() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.RemoveSource,
            playlistStateOwner = successfulPlaylistStateOwner(),
            operation = removeSourceOperation(),
            expectedPublicationCalls = 1,
            cleanupPublication = { error("publication callback failed") },
        )

    @Test
    fun clearLibraryCancellationSurvivesReadFailedPlaylistCleanup() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.Clear,
            playlistStateOwner = failingPlaylistStateOwner(),
            operation = clearLibraryOperation(),
            expectedPublicationCalls = 0,
            cleanupPublication = { publication ->
                publication.playlists?.requireSuccessfulPublication()
            },
        )

    @Test
    fun clearLibraryCancellationSurvivesThrowingPublicationCallback() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.Clear,
            playlistStateOwner = successfulPlaylistStateOwner(),
            operation = clearLibraryOperation(),
            expectedPublicationCalls = 1,
            cleanupPublication = { error("publication callback failed") },
        )

    @Test
    fun removeMissingTracksCancellationSurvivesReadFailedPlaylistCleanup() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.RemoveMissingTracks,
            playlistStateOwner = failingPlaylistStateOwner(),
            operation = removeMissingTracksOperation(),
            expectedPublicationCalls = 0,
            cleanupPublication = { publication ->
                publication.playlists?.requireSuccessfulPublication()
            },
        )

    @Test
    fun removeMissingTracksCancellationSurvivesThrowingPublicationCallback() =
        assertCleanupFailureDoesNotReplaceCancellation(
            kind = LibraryOperationKind.RemoveMissingTracks,
            playlistStateOwner = successfulPlaylistStateOwner(),
            operation = removeMissingTracksOperation(),
            expectedPublicationCalls = 1,
            cleanupPublication = { error("publication callback failed") },
        )
}

private typealias MutationOperation =
    suspend (
        PlaybackSessionReconciler,
        PlaylistStateOwner,
        suspend (LibraryMutationPublication) -> Unit,
    ) -> Unit

private fun assertCleanupFailureDoesNotReplaceCancellation(
    kind: LibraryOperationKind,
    playlistStateOwner: PlaylistStateOwner,
    operation: MutationOperation,
    expectedPublicationCalls: Int,
    cleanupPublication: suspend (LibraryMutationPublication) -> Unit,
) {
    kotlinx.coroutines.runBlocking {
        val coordinator = AppLibraryOperationCoordinator {}
        val userFacingErrors = mutableListOf<String>()
        val orchestrator =
            AppLibraryOrchestrator(coordinator, userFacingErrors::add)
        val originalCancellation =
            CancellationException("reconciliation cancelled")
        var publicationCalls = 0

        val thrown =
            assertFailsWith<CancellationException> {
                orchestrator.launch(kind) { token ->
                    operation(
                        PlaybackSessionReconciler {
                            throw originalCancellation
                        },
                        playlistStateOwner,
                    ) { publication ->
                        orchestrator.publishIfCurrent(token) {
                            publicationCalls++
                            cleanupPublication(publication)
                        }
                    }
                }
            }

        assertSame(originalCancellation, thrown)
        assertEquals(expectedPublicationCalls, publicationCalls)
        assertEquals(emptyList(), userFacingErrors)
        assertEquals(LibraryOperationState.Idle, coordinator.state.value)
    }
}

private fun removeSourceOperation(): MutationOperation =
    { reconciler, playlistStateOwner, publish ->
        removeSourceInBackground(
            sourceId = "source",
            repository = MutationLibraryRepository(),
            platformAccess = MutationPlatformSourceAccess,
            reconciler = reconciler,
            ioDispatcher = Dispatchers.Default,
            ownerIsActive = { true },
            playlistStateOwner = playlistStateOwner,
            publish = publish,
        )
    }

private fun clearLibraryOperation(): MutationOperation =
    { reconciler, playlistStateOwner, publish ->
        clearLibraryInBackground(
            repository = MutationLibraryRepository(),
            platformAccess = MutationPlatformSourceAccess,
            reconciler = reconciler,
            ioDispatcher = Dispatchers.Default,
            ownerIsActive = { true },
            playlistStateOwner = playlistStateOwner,
            publish = publish,
        )
    }

private fun removeMissingTracksOperation(): MutationOperation =
    { reconciler, playlistStateOwner, publish ->
        removeMissingTracksInBackground(
            sourceId = "source",
            latestScanId = "scan",
            repository = MutationLibraryRepository(),
            platformAccess = MutationPlatformSourceAccess,
            reconciler = reconciler,
            ioDispatcher = Dispatchers.Default,
            ownerIsActive = { true },
            playlistStateOwner = playlistStateOwner,
            publish = publish,
        )
    }

private fun failingPlaylistStateOwner() =
    PlaylistStateOwner(FailingMutationPlaylistRepository, Dispatchers.Default)

private fun successfulPlaylistStateOwner() =
    PlaylistStateOwner(EmptyMutationPlaylistRepository, Dispatchers.Default)

private class MutationLibraryRepository : LibraryRepository {
    private var sources = listOf(mutationSource())

    override fun upsertSource(source: LibrarySource) = Unit

    override fun sources(): List<LibrarySource> = sources

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult =
        error("unused")

    override fun tracks(): List<LibraryTrack> = emptyList()

    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        emptyList()

    override fun artworkForTrack(trackId: String): TrackArtwork? = null

    override fun insertScanSession(session: ScanSession) = Unit

    override fun updateScanSession(session: ScanSession) = Unit

    override fun insertScanError(error: ScanError) = Unit

    override fun scanErrors(scanId: String): List<ScanError> = emptyList()

    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String,
    ): RemoveMissingTracksResult = RemoveMissingTracksResult.Removed(0)

    override fun latestTerminalScanSession(): ScanSession? = null

    override fun removeSource(sourceId: String) {
        sources = emptyList()
    }

    override fun clearAll() {
        sources = emptyList()
    }
}

private object MutationPlatformSourceAccess : PlatformSourceAccess {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private object FailingMutationPlaylistRepository :
    PlaylistRepository by EmptyMutationPlaylistRepository {
    override fun playlists(): List<PlaylistSummary> =
        error("playlist read failed")
}

private object EmptyMutationPlaylistRepository : PlaylistRepository {
    override fun playlists(): List<PlaylistSummary> = emptyList()

    override fun playlist(id: String): PlaylistSummary? = null

    override fun entries(playlistId: String): List<PlaylistEntry> = emptyList()

    override fun create(name: String): PlaylistSummary = error("unused")

    override fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): PlaylistSummary = error("unused")

    override fun importPlaylists(
        playlists: List<PlaylistImportMutation>
    ): List<PlaylistSummary> = error("unused")

    override fun rename(id: String, name: String) = error("unused")

    override fun delete(id: String) = error("unused")

    override fun append(playlistId: String, trackIds: List<String>) =
        error("unused")

    override fun removeEntry(entryId: String) = error("unused")

    override fun reorder(playlistId: String, entryIds: List<String>) =
        error("unused")
}

private fun mutationSource() =
    LibrarySource(
        id = "source",
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = "Music",
        handle = "/music",
        createdAtEpochMillis = 1L,
    )
