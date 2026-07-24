package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackUpsertResult
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
            updateLibrary = { content -> clearedContent = content },
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
    ): Sequence<com.eterocell.rhythhaus.library.PlatformScanEvent> =
        emptySequence()
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
        latestScanId: String
    ): Int = 0

    override fun removeSource(sourceId: String) = Unit

    override fun clearAll() {
        clearThreadName = Thread.currentThread().name
    }
}
