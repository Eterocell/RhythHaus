package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackUpsertResult
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppDispatcherJvmTest {
    @Test
    fun clearLibraryRunsRepositoryWorkOnProvidedDispatcher() = runBlocking {
        val repository = ThreadCapturingRepository()
        val callerThread = Thread.currentThread().name
        var clearedContent: LibraryContentState? = null

        clearLibraryInBackground(
            repository = repository,
            platformAccess = TestPlatformSourceAccess,
            reconciler =
                PlaybackSessionReconciler {
                    PlaybackSessionReconcileResult.Applied
                },
            ioDispatcher = Dispatchers.Default,
            playlistStateOwner =
                PlaylistStateOwner(
                    TestPlaylistRepository(), Dispatchers.Default),
            publish =
                testLibraryMutationPublication(
                    onContent = { content -> clearedContent = content }),
        )

        assertEquals(emptyList(), clearedContent?.tracks)
        assertEquals(emptyList(), clearedContent?.sources)
        val clearThread = repository.clearThreadName
        check(clearThread != null)
        check(clearThread != callerThread) {
            "clearAll ran on caller thread $callerThread"
        }
    }
}

private object TestPlatformSourceAccess :
    com.eterocell.rhythhaus.library.PlatformSourceAccess {
    override fun scan(
        source: LibrarySource
    ): Sequence<com.eterocell.rhythhaus.library.impl.PlatformScanEvent> =
        emptySequence()
}

private fun testLibraryMutationPublication(
    onContent: suspend (LibraryContentState) -> Unit = {},
): suspend (LibraryMutationPublication) -> Unit = { publication ->
    publication.content?.let { onContent(it) }
}

private class TestPlaylistRepository :
    com.eterocell.rhythhaus.library.PlaylistRepository {
    override fun playlists() =
        emptyList<com.eterocell.rhythhaus.library.PlaylistSummary>()

    override fun playlist(id: String) = null

    override fun entries(playlistId: String) =
        emptyList<com.eterocell.rhythhaus.library.PlaylistEntry>()

    override fun create(name: String) = error("Not used by this test")

    override fun createWithEntries(name: String, trackIds: List<String>) =
        error("Not used by this test")

    override fun importPlaylists(
        playlists: List<com.eterocell.rhythhaus.library.PlaylistImportMutation>
    ) = error("Not used by this test")

    override fun rename(id: String, name: String) =
        error("Not used by this test")

    override fun delete(id: String) = error("Not used by this test")

    override fun append(playlistId: String, trackIds: List<String>) =
        error("Not used by this test")

    override fun removeEntry(entryId: String) = error("Not used by this test")

    override fun reorder(playlistId: String, entryIds: List<String>) =
        error("Not used by this test")
}

private class ThreadCapturingRepository : LibraryRepository {
    var clearThreadName: String? = null
        private set

    override fun upsertSource(source: LibrarySource) = Unit

    override fun sources(): List<LibrarySource> = emptyList()

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult =
        TrackUpsertResult.Added

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

    override fun removeSource(sourceId: String) = Unit

    override fun clearAll() {
        clearThreadName = Thread.currentThread().name
    }
}
