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
        require(sources.containsKey(track.sourceId)) {
            "Unknown source: ${track.sourceId}"
        }
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
        require(sources.containsKey(session.sourceId)) {
            "Unknown source: ${session.sourceId}"
        }
        scanSessions[session.id] = session
    }

    /**
     * Updates a stored scan session.
     *
     * @param session the session to update.
     */
    override fun updateScanSession(session: ScanSession) {
        val existing = scanSessions[session.id] ?: return
        scanSessions[session.id] =
            existing.copy(
                status = session.status,
                completedAtEpochMillis = session.completedAtEpochMillis,
                foldersVisited = session.foldersVisited,
                filesVisited = session.filesVisited,
                tracksAdded = session.tracksAdded,
                tracksUpdated = session.tracksUpdated,
                filesSkipped = session.filesSkipped,
                terminalMessage = session.terminalMessage,
            )
    }

    /**
     * Stores a scan error.
     *
     * @param error the error to store.
     */
    override fun insertScanError(error: ScanError) {
        require(scanSessions.containsKey(error.scanId)) {
            "Unknown scan: ${error.scanId}"
        }
        scanErrors += error
    }

    /**
     * Returns errors recorded for the given scan.
     *
     * @param scanId the scan identifier.
     */
    override fun scanErrors(scanId: String): List<ScanError> =
        scanErrors
            .filter {
                it.scanId == scanId
            }
            .sortedWith(
                compareBy<ScanError> { it.createdAtEpochMillis }
                    .thenBy { it.id })

    /**
     * Removes tracks not observed by the latest scan of a source.
     *
     * @param sourceId the owning source identifier.
     * @param latestScanId the latest scan identifier for the source.
     */
    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String,
    ): RemoveMissingTracksResult {
        if (!sources.containsKey(sourceId)) {
            return RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.UnknownSource)
        }
        val requested =
            scanSessions[requestedScanId]
                ?: return RemoveMissingTracksResult.Rejected(
                    RemoveMissingTracksRejectionReason.UnknownScan)
        if (requested.sourceId != sourceId) {
            return RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.ForeignSource)
        }
        if (requested.status != ScanStatus.Completed) {
            return RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.NotCompleted)
        }
        if (requested.completedAtEpochMillis == null) {
            return RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.MissingCompletionTimestamp)
        }
        val latest =
            scanSessions.values
                .filter {
                    it.sourceId == sourceId &&
                        it.status == ScanStatus.Completed &&
                        it.completedAtEpochMillis != null
                }
                .maxWithOrNull(scanSessionAuthorityComparator)!!
        if (latest.id != requestedScanId) {
            return RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.StaleCompletedScan)
        }
        val ids =
            tracks.values
                .filter {
                    it.sourceId == sourceId &&
                        it.lastSeenScanId != requestedScanId
                }
                .map { it.id }
        ids.forEach { tracks.remove(it) }
        return RemoveMissingTracksResult.Removed(ids.size)
    }

    override fun latestTerminalScanSession(): ScanSession? =
        scanSessions.values
            .filter {
                it.status == ScanStatus.Completed ||
                    it.status == ScanStatus.Cancelled ||
                    it.status == ScanStatus.Failed
            }
            .maxWithOrNull(scanSessionTerminalComparator)

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

private val scanSessionAuthorityComparator =
    compareBy<ScanSession> {
            it.completedAtEpochMillis ?: Long.MIN_VALUE
        }
        .thenBy { it.startedAtEpochMillis }
        .thenBy { it.id }

private val scanSessionTerminalComparator =
    compareBy<ScanSession> {
            it.completedAtEpochMillis ?: it.startedAtEpochMillis
        }
        .thenBy { it.startedAtEpochMillis }
        .thenBy { it.id }

private fun LibraryTrack.withoutArtwork(): LibraryTrack =
    copy(
        artworkBytes = null,
        artworkMimeType = null,
    )
