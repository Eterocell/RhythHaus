package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource

interface LibraryRepository {
    fun upsertSource(source: LibrarySource)
    fun sources(): List<LibrarySource>
    fun upsertTrack(track: LibraryTrack): TrackUpsertResult
    fun tracks(): List<LibraryTrack>
    fun tracksForSource(sourceId: String): List<LibraryTrack>
    fun insertScanSession(session: ScanSession)
    fun updateScanSession(session: ScanSession)
    fun insertScanError(error: ScanError)
    fun scanErrors(scanId: String): List<ScanError>
    fun removeMissingTracks(sourceId: String, latestScanId: String): Int
}

enum class TrackUpsertResult { Added, Updated }

class InMemoryLibraryRepository : LibraryRepository {
    private val sources = linkedMapOf<String, LibrarySource>()
    private val tracks = linkedMapOf<String, LibraryTrack>()
    private val scanSessions = linkedMapOf<String, ScanSession>()
    private val scanErrors = mutableListOf<ScanError>()

    override fun upsertSource(source: LibrarySource) {
        sources[source.id] = source
    }

    override fun sources(): List<LibrarySource> = sources.values.toList()

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = tracks.values.firstOrNull {
            it.sourceId == track.sourceId && it.sourceLocalKey == track.sourceLocalKey
        }
        return if (existing == null) {
            tracks[track.id] = track
            TrackUpsertResult.Added
        } else {
            tracks[existing.id] = track.copy(
                id = existing.id,
                createdAtEpochMillis = existing.createdAtEpochMillis,
            )
            TrackUpsertResult.Updated
        }
    }

    override fun tracks(): List<LibraryTrack> = tracks.values.sortedWith(
        compareBy<LibraryTrack> { it.title.lowercase() }.thenBy { it.artist.lowercase() },
    )

    override fun tracksForSource(sourceId: String): List<LibraryTrack> = tracks().filter { it.sourceId == sourceId }

    override fun insertScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    override fun updateScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    override fun insertScanError(error: ScanError) {
        scanErrors += error
    }

    override fun scanErrors(scanId: String): List<ScanError> = scanErrors.filter { it.scanId == scanId }

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        val ids = tracks.values
            .filter { it.sourceId == sourceId && it.lastSeenScanId != latestScanId }
            .map { it.id }
        ids.forEach { tracks.remove(it) }
        return ids.size
    }
}
