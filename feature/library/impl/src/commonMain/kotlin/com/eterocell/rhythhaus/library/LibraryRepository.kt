package com.eterocell.rhythhaus.library

/** In-memory [LibraryRepository] used for tests and non-persistent flows. */
class InMemoryLibraryRepository : LibraryRepository {
    private val sources = linkedMapOf<String, LibrarySource>()
    private val tracks = linkedMapOf<String, LibraryTrack>()
    private val scanSessions = linkedMapOf<String, ScanSession>()
    private val scanErrors = mutableListOf<ScanError>()

    /**
     * Stores or updates the given source.
     *
     * @param source the source to store.
     */
    override fun upsertSource(source: LibrarySource) {
        sources[source.id] = source
    }

    /** Returns all stored sources. */
    override fun sources(): List<LibrarySource> = sources.values.toList()

    /**
     * Stores or updates the given track.
     *
     * @param track the track to store.
     */
    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing =
            tracks.values.firstOrNull {
                it.sourceId == track.sourceId &&
                    it.sourceLocalKey == track.sourceLocalKey
            }
        return if (existing == null) {
            tracks[track.id] = track
            TrackUpsertResult.Added
        } else {
            tracks[existing.id] =
                track.copy(
                    id = existing.id,
                    createdAtEpochMillis = existing.createdAtEpochMillis,
                )
            TrackUpsertResult.Updated
        }
    }

    /** Returns all tracks sorted by title, then artist. */
    override fun tracks(): List<LibraryTrack> =
        tracks.values
            .sortedWith(
                compareBy<LibraryTrack> { it.title.lowercase() }
                    .thenBy { it.artist.lowercase() },
            )
            .map { it.withoutArtwork() }

    /**
     * Returns tracks belonging to the given source.
     *
     * @param sourceId the owning source identifier.
     */
    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        tracks().filter { it.sourceId == sourceId }

    /**
     * Returns artwork for the given track, if stored.
     *
     * @param trackId the track identifier.
     */
    override fun artworkForTrack(trackId: String): TrackArtwork? {
        val track = tracks[trackId] ?: return null
        val bytes = track.artworkBytes ?: return null
        return TrackArtwork(bytes = bytes, mimeType = track.artworkMimeType)
    }

    /**
     * Stores a scan session.
     *
     * @param session the session to store.
     */
    override fun insertScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    /**
     * Updates a stored scan session.
     *
     * @param session the session to update.
     */
    override fun updateScanSession(session: ScanSession) {
        scanSessions[session.id] = session
    }

    /**
     * Stores a scan error.
     *
     * @param error the error to store.
     */
    override fun insertScanError(error: ScanError) {
        scanErrors += error
    }

    /**
     * Returns errors recorded for the given scan.
     *
     * @param scanId the scan identifier.
     */
    override fun scanErrors(scanId: String): List<ScanError> =
        scanErrors.filter {
            it.scanId == scanId
        }

    /**
     * Removes tracks not observed by the latest scan of a source.
     *
     * @param sourceId the owning source identifier.
     * @param latestScanId the latest scan identifier for the source.
     */
    override fun removeMissingTracks(
        sourceId: String,
        latestScanId: String
    ): Int {
        val ids =
            tracks.values
                .filter {
                    it.sourceId == sourceId && it.lastSeenScanId != latestScanId
                }
                .map { it.id }
        ids.forEach { tracks.remove(it) }
        return ids.size
    }

    /**
     * Removes a source and all of its owned records.
     *
     * @param sourceId the source identifier to remove.
     */
    override fun removeSource(sourceId: String) {
        val scanIds =
            scanSessions.values
                .filter { it.sourceId == sourceId }
                .mapTo(mutableSetOf()) { it.id }
        scanErrors.removeAll { it.scanId in scanIds }
        scanIds.forEach { scanSessions.remove(it) }
        tracks.entries.removeAll { it.value.sourceId == sourceId }
        sources.remove(sourceId)
    }

    /** Clears all stored library records. */
    override fun clearAll() {
        sources.clear()
        tracks.clear()
        scanSessions.clear()
        scanErrors.clear()
    }
}

private fun LibraryTrack.withoutArtwork(): LibraryTrack =
    copy(
        artworkBytes = null,
        artworkMimeType = null,
    )
