package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackUpsertResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppScanCancellationTest {
    @Test
    fun requestScanCancellationMarksActiveScanAsCancellingImmediately() {
        val progress = ScanProgress(
            session = ScanSession(
                id = "scan-1",
                sourceId = "source-1",
                status = ScanStatus.Scanning,
                startedAtEpochMillis = 1L,
                filesVisited = 42,
            ),
        )

        val result = progress.requestScanCancellation()

        assertEquals(ScanStatus.Cancelling, result?.session?.status)
        assertEquals(42, result?.session?.filesVisited)
    }

    @Test
    fun requestScanCancellationLeavesTerminalScanUntouched() {
        val progress = ScanProgress(
            session = ScanSession(
                id = "scan-1",
                sourceId = "source-1",
                status = ScanStatus.Completed,
                startedAtEpochMillis = 1L,
            ),
        )

        val result = progress.requestScanCancellation()

        assertSame(progress, result)
    }

    @Test
    fun clearLibraryRunsRepositoryWorkOnProvidedDispatcher() = runBlocking {
        val repository = ThreadCapturingRepository()
        val callerThread = Thread.currentThread().name

        var clearedTracks: List<LibraryTrack>? = null

        clearLibraryInBackground(
            repository = repository,
            ioDispatcher = Dispatchers.Default,
            updateTracks = { tracks -> clearedTracks = tracks },
        )

        assertEquals(emptyList(), clearedTracks)
        val clearThread = repository.clearThreadName
        check(clearThread != null)
        check(clearThread != callerThread) {
            "clearAll ran on caller thread $callerThread"
        }
    }
}

private class ThreadCapturingRepository : LibraryRepository {
    var clearThreadName: String? = null
        private set

    override fun upsertSource(source: LibrarySource) = Unit
    override fun sources(): List<LibrarySource> = emptyList()
    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult = TrackUpsertResult.Added
    override fun tracks(): List<LibraryTrack> = emptyList()
    override fun tracksForSource(sourceId: String): List<LibraryTrack> = emptyList()
    override fun artworkForTrack(trackId: String): TrackArtwork? = null
    override fun insertScanSession(session: ScanSession) = Unit
    override fun updateScanSession(session: ScanSession) = Unit
    override fun insertScanError(error: ScanError) = Unit
    override fun scanErrors(scanId: String): List<ScanError> = emptyList()
    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int = 0
    override fun removeSource(sourceId: String) = Unit

    override fun clearAll() {
        clearThreadName = Thread.currentThread().name
    }
}
