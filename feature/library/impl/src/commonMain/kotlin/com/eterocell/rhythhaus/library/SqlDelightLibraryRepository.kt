package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource

internal const val ARTWORK_CHUNK_SIZE_BYTES = 256 * 1024

/**
 * Returns the number of artwork chunks for the given byte length.
 *
 * @param byteLength the total artwork byte length.
 */
internal fun artworkChunkCount(byteLength: Long): Int {
    if (byteLength < 0L || byteLength > Int.MAX_VALUE.toLong()) return 0
    if (byteLength == 0L) return 0
    return ((byteLength - 1L) / ARTWORK_CHUNK_SIZE_BYTES + 1L).toInt()
}

/** SQLDelight-backed [LibraryRepository] persisting library records. */
class SqlDelightLibraryRepository(
    private val libraryDatabase: LibraryDatabase,
) : LibraryRepository {
    private val database = libraryDatabase.database

    /**
     * Stores or updates the given source.
     *
     * @param source the source to store.
     */
    override fun upsertSource(source: LibrarySource) {
        database.librarySourceQueries.upsertSource(
            id = source.id,
            platformKind = source.platformKind.name,
            displayName = source.displayName,
            handle = source.handle,
            createdAtEpochMillis = source.createdAtEpochMillis,
            lastScanAtEpochMillis = source.lastScanAtEpochMillis,
            accessStatus = source.accessStatus.name,
        )
    }

    /** Returns all stored sources. */
    override fun sources(): List<LibrarySource> =
        database.librarySourceQueries
            .selectAllSources {
                id,
                platformKind,
                displayName,
                handle,
                createdAtEpochMillis,
                lastScanAtEpochMillis,
                accessStatus ->
                LibrarySource(
                    id = id,
                    platformKind = LibraryPlatformKind.valueOf(platformKind),
                    displayName = displayName,
                    handle = handle,
                    createdAtEpochMillis = createdAtEpochMillis,
                    lastScanAtEpochMillis = lastScanAtEpochMillis,
                    accessStatus =
                        LibrarySourceAccessStatus.valueOf(accessStatus),
                )
            }
            .executeAsList()

    /**
     * Stores or updates the given track.
     *
     * @param track the track to store.
     */
    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing =
            database.libraryTrackQueries
                .selectTrackBySourceKey(
                    sourceId = track.sourceId,
                    sourceLocalKey = track.sourceLocalKey,
                    mapper = {
                        id,
                        sourceId,
                        sourceLocalKey,
                        audioSourceKind,
                        audioSourceValue,
                        displayName,
                        title,
                        artist,
                        album,
                        durationMillis,
                        sizeBytes,
                        modifiedAtEpochMillis,
                        lastSeenScanId,
                        createdAtEpochMillis,
                        updatedAtEpochMillis,
                        trackNumber,
                        discNumber,
                        artworkBytes,
                        artworkMimeType ->
                        DomainTrackRow(
                            id = id,
                            sourceId = sourceId,
                            sourceLocalKey = sourceLocalKey,
                            audioSourceKind = audioSourceKind,
                            audioSourceValue = audioSourceValue,
                            displayName = displayName,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMillis = durationMillis,
                            sizeBytes = sizeBytes,
                            modifiedAtEpochMillis = modifiedAtEpochMillis,
                            lastSeenScanId = lastSeenScanId,
                            createdAtEpochMillis = createdAtEpochMillis,
                            updatedAtEpochMillis = updatedAtEpochMillis,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            artworkBytes = artworkBytes,
                            artworkMimeType = artworkMimeType,
                        )
                    },
                )
                .executeAsOneOrNull()

        return if (existing == null) {
            val audioSource = track.audioSource
            database.libraryTrackQueries.upsertTrack(
                id = track.id,
                sourceId = track.sourceId,
                sourceLocalKey = track.sourceLocalKey,
                audioSourceKind = audioSource.kindName,
                audioSourceValue = audioSource.stableValue,
                displayName = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMillis = track.durationMillis,
                sizeBytes = track.sizeBytes,
                modifiedAtEpochMillis = track.modifiedAtEpochMillis,
                lastSeenScanId = track.lastSeenScanId,
                createdAtEpochMillis = track.createdAtEpochMillis,
                updatedAtEpochMillis = track.updatedAtEpochMillis,
                trackNumber = track.trackNumber?.toLong(),
                discNumber = track.discNumber?.toLong(),
                artworkBytes = track.artworkBytes,
                artworkMimeType = track.artworkMimeType,
            )
            TrackUpsertResult.Added
        } else {
            val audioSource = track.audioSource
            database.libraryTrackQueries.upsertTrack(
                id = existing.id,
                sourceId = existing.sourceId,
                sourceLocalKey = existing.sourceLocalKey,
                audioSourceKind = audioSource.kindName,
                audioSourceValue = audioSource.stableValue,
                displayName = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMillis = track.durationMillis,
                sizeBytes = track.sizeBytes,
                modifiedAtEpochMillis = track.modifiedAtEpochMillis,
                lastSeenScanId = track.lastSeenScanId,
                createdAtEpochMillis = existing.createdAtEpochMillis,
                updatedAtEpochMillis = track.updatedAtEpochMillis,
                trackNumber = track.trackNumber?.toLong(),
                discNumber = track.discNumber?.toLong(),
                artworkBytes = track.artworkBytes,
                artworkMimeType = track.artworkMimeType,
            )
            TrackUpsertResult.Updated
        }
    }

    /** Returns all tracks. */
    override fun tracks(): List<LibraryTrack> =
        database.libraryTrackQueries
            .selectAllTracks {
                id,
                sourceId,
                sourceLocalKey,
                audioSourceKind,
                audioSourceValue,
                displayName,
                title,
                artist,
                album,
                durationMillis,
                sizeBytes,
                modifiedAtEpochMillis,
                lastSeenScanId,
                createdAtEpochMillis,
                updatedAtEpochMillis,
                trackNumber,
                discNumber,
                artworkBytes,
                artworkMimeType ->
                LibraryTrack(
                    id = id,
                    sourceId = sourceId,
                    sourceLocalKey = sourceLocalKey,
                    audioSource =
                        audioSourceFrom(audioSourceKind, audioSourceValue),
                    displayName = displayName,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMillis = durationMillis,
                    sizeBytes = sizeBytes,
                    modifiedAtEpochMillis = modifiedAtEpochMillis,
                    lastSeenScanId = lastSeenScanId,
                    createdAtEpochMillis = createdAtEpochMillis,
                    updatedAtEpochMillis = updatedAtEpochMillis,
                    trackNumber = trackNumber?.toInt(),
                    discNumber = discNumber?.toInt(),
                    artworkBytes = artworkBytes,
                    artworkMimeType = artworkMimeType,
                )
            }
            .executeAsList()

    /**
     * Returns tracks belonging to the given source.
     *
     * @param sourceId the owning source identifier.
     */
    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        database.libraryTrackQueries
            .selectTracksForSource(sourceId) {
                id,
                srcId,
                sourceLocalKey,
                audioSourceKind,
                audioSourceValue,
                displayName,
                title,
                artist,
                album,
                durationMillis,
                sizeBytes,
                modifiedAtEpochMillis,
                lastSeenScanId,
                createdAtEpochMillis,
                updatedAtEpochMillis,
                trackNumber,
                discNumber,
                artworkBytes,
                artworkMimeType ->
                LibraryTrack(
                    id = id,
                    sourceId = srcId,
                    sourceLocalKey = sourceLocalKey,
                    audioSource =
                        audioSourceFrom(audioSourceKind, audioSourceValue),
                    displayName = displayName,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMillis = durationMillis,
                    sizeBytes = sizeBytes,
                    modifiedAtEpochMillis = modifiedAtEpochMillis,
                    lastSeenScanId = lastSeenScanId,
                    createdAtEpochMillis = createdAtEpochMillis,
                    updatedAtEpochMillis = updatedAtEpochMillis,
                    trackNumber = trackNumber?.toInt(),
                    discNumber = discNumber?.toInt(),
                    artworkBytes = artworkBytes,
                    artworkMimeType = artworkMimeType,
                )
            }
            .executeAsList()

    /**
     * Returns artwork for the given track, if stored.
     *
     * @param trackId the track identifier.
     */
    override fun artworkForTrack(trackId: String): TrackArtwork? {
        val metadata =
            database.libraryTrackQueries
                .selectArtworkMetadataForTrack(trackId)
                .executeAsOneOrNull() ?: return null
        val byteLength = metadata.artworkByteLength ?: return null
        if (byteLength < 0L || byteLength > Int.MAX_VALUE.toLong()) return null
        if (byteLength == 0L)
            return TrackArtwork(
                bytes = ByteArray(0), mimeType = metadata.artworkMimeType)

        val artworkBytes = ByteArray(byteLength.toInt())
        repeat(artworkChunkCount(byteLength)) { chunkIndex ->
            val destinationOffset = chunkIndex * ARTWORK_CHUNK_SIZE_BYTES
            val requestedLength =
                minOf(
                    ARTWORK_CHUNK_SIZE_BYTES,
                    artworkBytes.size - destinationOffset)
            val chunk =
                database.libraryTrackQueries
                    .selectArtworkChunkForTrack(
                        startPosition =
                            (destinationOffset.toLong() + 1L).toString(),
                        chunkLength = requestedLength.toString(),
                        id = trackId,
                    )
                    .executeAsOneOrNull()
                    ?.artworkChunk ?: return null
            if (chunk.size != requestedLength) return null
            chunk.copyInto(
                destination = artworkBytes,
                destinationOffset = destinationOffset,
            )
        }
        return TrackArtwork(
            bytes = artworkBytes, mimeType = metadata.artworkMimeType)
    }

    /**
     * Stores a scan session.
     *
     * @param session the session to store.
     */
    override fun insertScanSession(session: ScanSession) {
        database.scanSessionQueries.insertScanSession(
            id = session.id,
            sourceId = session.sourceId,
            status = session.status.name,
            startedAtEpochMillis = session.startedAtEpochMillis,
            completedAtEpochMillis = session.completedAtEpochMillis,
            foldersVisited = session.foldersVisited.toLong(),
            filesVisited = session.filesVisited.toLong(),
            tracksAdded = session.tracksAdded.toLong(),
            tracksUpdated = session.tracksUpdated.toLong(),
            filesSkipped = session.filesSkipped.toLong(),
            terminalMessage = session.terminalMessage,
        )
    }

    /**
     * Updates a stored scan session.
     *
     * @param session the session to update.
     */
    override fun updateScanSession(session: ScanSession) {
        database.scanSessionQueries.updateScanSession(
            status = session.status.name,
            completedAtEpochMillis = session.completedAtEpochMillis,
            foldersVisited = session.foldersVisited.toLong(),
            filesVisited = session.filesVisited.toLong(),
            tracksAdded = session.tracksAdded.toLong(),
            tracksUpdated = session.tracksUpdated.toLong(),
            filesSkipped = session.filesSkipped.toLong(),
            terminalMessage = session.terminalMessage,
            id = session.id,
        )
    }

    /**
     * Stores a scan error.
     *
     * @param error the error to store.
     */
    override fun insertScanError(error: ScanError) {
        database.scanErrorQueries.insertScanError(
            id = error.id,
            scanId = error.scanId,
            sourceLocalKey = error.sourceLocalKey,
            displayPath = error.displayPath,
            reason = error.reason,
            recoverable = if (error.recoverable) 1L else 0L,
            createdAtEpochMillis = error.createdAtEpochMillis,
        )
    }

    /**
     * Returns errors recorded for the given scan.
     *
     * @param scanId the scan identifier.
     */
    override fun scanErrors(scanId: String): List<ScanError> =
        database.scanSessionQueries
            .selectScanErrorsForScan(scanId) {
                id,
                scanId_,
                sourceLocalKey,
                displayPath,
                reason,
                recoverable,
                createdAtEpochMillis ->
                ScanError(
                    id = id,
                    scanId = scanId_,
                    sourceLocalKey = sourceLocalKey,
                    displayPath = displayPath,
                    reason = reason,
                    recoverable = recoverable != 0L,
                    createdAtEpochMillis = createdAtEpochMillis,
                )
            }
            .executeAsList()

    /**
     * Removes tracks not observed by the requested completed scan when that
     * scan is the source's latest completed scan.
     *
     * The requested scan is the authority for deciding which tracks were
     * observed. A discriminated [RemoveMissingTracksResult.Rejected] result
     * identifies why the request was not accepted instead of mutating data.
     *
     * @param sourceId the owning source identifier.
     * @param requestedScanId the completed scan requested as the source of
     *   truth for observed tracks.
     */
    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String,
    ): RemoveMissingTracksResult {
        var outcome: RemoveMissingTracksResult =
            RemoveMissingTracksResult.Rejected(
                RemoveMissingTracksRejectionReason.UnknownScan)
        database.transaction {
            if (database.librarySourceQueries
                .selectAllSources()
                .executeAsList()
                .none { it.id == sourceId }) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason.UnknownSource)
                return@transaction
            }
            val requested =
                database.scanSessionQueries
                    .selectScanSessionById(requestedScanId)
                    .executeAsOneOrNull()
            if (requested == null) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason.UnknownScan)
                return@transaction
            }
            if (requested.sourceId != sourceId) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason.ForeignSource)
                return@transaction
            }
            if (requested.status != ScanStatus.Completed.name) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason.NotCompleted)
                return@transaction
            }
            if (requested.completedAtEpochMillis == null) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason
                            .MissingCompletionTimestamp)
                return@transaction
            }
            val latest =
                database.scanSessionQueries
                    .selectLatestCompletedScanForSource(sourceId)
                    .executeAsOneOrNull()
            check(latest != null) {
                "Completed request must be returned by latest completed query"
            }
            if (latest.id != requestedScanId) {
                outcome =
                    RemoveMissingTracksResult.Rejected(
                        RemoveMissingTracksRejectionReason.StaleCompletedScan)
                return@transaction
            }
            outcome =
                RemoveMissingTracksResult.Removed(
                    database.libraryTrackQueries
                        .removeMissingTracks(sourceId, requestedScanId)
                        .value
                        .toIntChecked())
        }
        return outcome
    }

    /**
     * Returns only the latest terminal session; callers load errors with
     * [scanErrors].
     */
    override fun latestTerminalScanSession(): ScanSession? =
        database.scanSessionQueries
            .selectLatestTerminalScanSession {
                id,
                sourceId,
                status,
                startedAtEpochMillis,
                completedAtEpochMillis,
                foldersVisited,
                filesVisited,
                tracksAdded,
                tracksUpdated,
                filesSkipped,
                terminalMessage ->
                ScanSession(
                    id = id,
                    sourceId = sourceId,
                    status = ScanStatus.valueOf(status),
                    startedAtEpochMillis = startedAtEpochMillis,
                    completedAtEpochMillis = completedAtEpochMillis,
                    foldersVisited = foldersVisited.toInt(),
                    filesVisited = filesVisited.toInt(),
                    tracksAdded = tracksAdded.toInt(),
                    tracksUpdated = tracksUpdated.toInt(),
                    filesSkipped = filesSkipped.toInt(),
                    terminalMessage = terminalMessage,
                )
            }
            .executeAsOneOrNull()

    /**
     * Removes a source and all of its owned records.
     *
     * @param sourceId the source identifier to remove.
     */
    override fun removeSource(sourceId: String) {
        database.transaction {
            database.scanErrorQueries.removeErrorsForSource(sourceId)
            database.scanSessionQueries.removeSessionsForSource(sourceId)
            database.libraryTrackQueries.removeTracksForSource(sourceId)
            database.librarySourceQueries.removeSource(sourceId)
        }
    }

    /** Clears all stored library records. */
    override fun clearAll() {
        database.transaction {
            database.scanErrorQueries.clearAllErrors()
            database.scanSessionQueries.clearAllSessions()
            database.libraryTrackQueries.clearAllTracks()
            database.librarySourceQueries.clearAllSources()
        }
    }
}

private fun Long.toIntChecked(): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
    return toInt()
}

// --- Internal helpers ---

private data class DomainTrackRow(
    val id: String,
    val sourceId: String,
    val sourceLocalKey: String,
    val audioSourceKind: String,
    val audioSourceValue: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long?,
    val sizeBytes: Long?,
    val modifiedAtEpochMillis: Long?,
    val lastSeenScanId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val trackNumber: Long?,
    val discNumber: Long?,
    val artworkBytes: ByteArray?,
    val artworkMimeType: String?,
)

private val AudioSource.kindName: String
    get() =
        when (this) {
            is AudioSource.FilePath -> "FilePath"
            is AudioSource.Uri -> "Uri"
            is AudioSource.FileDescriptor -> "FileDescriptor"
        }

private val AudioSource.stableValue: String
    get() =
        when (this) {
            is AudioSource.FilePath -> path
            is AudioSource.Uri -> value
            is AudioSource.FileDescriptor -> stableKey
        }

private fun audioSourceFrom(kind: String, value: String): AudioSource =
    when (kind) {
        "FilePath" -> AudioSource.FilePath(value)
        "Uri" -> AudioSource.Uri(value)
        "FileDescriptor" -> AudioSource.Uri(value)
        else -> AudioSource.Uri(value)
    }
