package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryApiContractTest {
    @Test
    fun repositoryExposesTheCompleteStableContract() {
        val repository = RecordingLibraryRepository()
        val source = source()
        val track = track()
        val session = ScanSession("scan", source.id, ScanStatus.Scanning, 1L)
        val error =
            ScanError(
                "error",
                session.id,
                "key",
                "/Music/Track.mp3",
                "read",
                true,
                2L)

        repository.upsertSource(source)
        repository.upsertTrack(track)
        repository.insertScanSession(session)
        repository.updateScanSession(
            session.copy(status = ScanStatus.Completed))
        repository.insertScanError(error)

        assertEquals(listOf(source), repository.sources())
        assertEquals(listOf(track), repository.tracks())
        assertEquals(listOf(track), repository.tracksForSource(source.id))
        assertEquals(
            TrackArtwork(byteArrayOf(1), "image/jpeg"),
            repository.artworkForTrack(track.id))
        assertEquals(listOf(error), repository.scanErrors(session.id))
        assertEquals(
            RemoveMissingTracksResult.Removed(1),
            repository.removeMissingTracks(source.id, session.id))
        assertNull(repository.latestTerminalScanSession())
        repository.removeSource(source.id)
        repository.clearAll()
        assertTrue(
            repository.calls.containsAll(LibraryRepositoryMethod.entries))
    }

    @Test
    fun removeMissingResultIsDiscriminatedAndTerminalQueryIsExposed() {
        val rejected =
            RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.UnknownScan)

        assertEquals(
            RemoveMissingTracksRejectionReason.UnknownScan, rejected.reason)
        assertEquals(
            RemoveMissingTracksResult.Removed(0),
            RemoveMissingTracksResult.Removed(0))
    }

    private fun source() =
        LibrarySource(
            "source", LibraryPlatformKind.JvmFolder, "Music", "/Music", 1L)

    private fun track() =
        LibraryTrack(
            id = "track",
            sourceId = "source",
            sourceLocalKey = "Track.mp3",
            audioSource = AudioSource.FilePath("/Music/Track.mp3"),
            displayName = "Track.mp3",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 1L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
            artworkBytes = byteArrayOf(1),
            artworkMimeType = "image/jpeg",
        )
}

private enum class LibraryRepositoryMethod {
    UpsertSource,
    Sources,
    UpsertTrack,
    Tracks,
    TracksForSource,
    ArtworkForTrack,
    InsertScanSession,
    UpdateScanSession,
    InsertScanError,
    ScanErrors,
    RemoveMissingTracks,
    LatestTerminalScanSession,
    RemoveSource,
    ClearAll,
}

private class RecordingLibraryRepository : LibraryRepository {
    val calls = mutableSetOf<LibraryRepositoryMethod>()
    private lateinit var source: LibrarySource
    private lateinit var track: LibraryTrack
    private lateinit var error: ScanError

    override fun upsertSource(source: LibrarySource) {
        calls += LibraryRepositoryMethod.UpsertSource
        this.source = source
    }

    override fun sources(): List<LibrarySource> {
        calls += LibraryRepositoryMethod.Sources
        return listOf(source)
    }

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        calls += LibraryRepositoryMethod.UpsertTrack
        this.track = track
        return TrackUpsertResult.Added
    }

    override fun tracks(): List<LibraryTrack> {
        calls += LibraryRepositoryMethod.Tracks
        return listOf(track)
    }

    override fun tracksForSource(sourceId: String): List<LibraryTrack> {
        calls += LibraryRepositoryMethod.TracksForSource
        return listOf(track)
    }

    override fun artworkForTrack(trackId: String): TrackArtwork? {
        calls += LibraryRepositoryMethod.ArtworkForTrack
        return TrackArtwork(byteArrayOf(1), "image/jpeg")
    }

    override fun insertScanSession(session: ScanSession) {
        calls += LibraryRepositoryMethod.InsertScanSession
    }

    override fun updateScanSession(session: ScanSession) {
        calls += LibraryRepositoryMethod.UpdateScanSession
    }

    override fun insertScanError(error: ScanError) {
        calls += LibraryRepositoryMethod.InsertScanError
        this.error = error
    }

    override fun scanErrors(scanId: String): List<ScanError> {
        calls += LibraryRepositoryMethod.ScanErrors
        return listOf(error)
    }

    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String
    ): RemoveMissingTracksResult {
        calls += LibraryRepositoryMethod.RemoveMissingTracks
        return RemoveMissingTracksResult.Removed(1)
    }

    override fun latestTerminalScanSession(): ScanSession? {
        calls += LibraryRepositoryMethod.LatestTerminalScanSession
        return null
    }

    override fun removeSource(sourceId: String) {
        calls += LibraryRepositoryMethod.RemoveSource
    }

    override fun clearAll() {
        calls += LibraryRepositoryMethod.ClearAll
    }
}
