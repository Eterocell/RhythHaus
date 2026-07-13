package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource

internal const val ARTWORK_CHUNK_SIZE_BYTES = 256 * 1024

internal fun artworkChunkCount(byteLength: Long): Int {
    if (byteLength < 0L || byteLength > Int.MAX_VALUE.toLong()) return 0
    if (byteLength == 0L) return 0
    return ((byteLength - 1L) / ARTWORK_CHUNK_SIZE_BYTES + 1L).toInt()
}

class SqlDelightLibraryRepository(
    private val libraryDatabase: LibraryDatabase,
) : LibraryRepository {
    private val database = libraryDatabase.database

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

    override fun sources(): List<LibrarySource> = database.librarySourceQueries.selectAllSources { id, platformKind, displayName, handle, createdAtEpochMillis, lastScanAtEpochMillis, accessStatus ->
        LibrarySource(
            id = id,
            platformKind = LibraryPlatformKind.valueOf(platformKind),
            displayName = displayName,
            handle = handle,
            createdAtEpochMillis = createdAtEpochMillis,
            lastScanAtEpochMillis = lastScanAtEpochMillis,
            accessStatus = LibrarySourceAccessStatus.valueOf(accessStatus),
        )
    }.executeAsList()

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = database.libraryTrackQueries.selectTrackBySourceKey(
            sourceId = track.sourceId,
            sourceLocalKey = track.sourceLocalKey,
            mapper = { id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, durationMillis, sizeBytes, modifiedAtEpochMillis, lastSeenScanId, createdAtEpochMillis, updatedAtEpochMillis, trackNumber, discNumber, artworkBytes, artworkMimeType ->
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
        ).executeAsOneOrNull()

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

    override fun tracks(): List<LibraryTrack> = database.libraryTrackQueries.selectAllTracks { id, sourceId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, durationMillis, sizeBytes, modifiedAtEpochMillis, lastSeenScanId, createdAtEpochMillis, updatedAtEpochMillis, trackNumber, discNumber, artworkBytes, artworkMimeType ->
        LibraryTrack(
            id = id,
            sourceId = sourceId,
            sourceLocalKey = sourceLocalKey,
            audioSource = audioSourceFrom(audioSourceKind, audioSourceValue),
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
    }.executeAsList()

    override fun tracksForSource(sourceId: String): List<LibraryTrack> = database.libraryTrackQueries.selectTracksForSource(sourceId) { id, srcId, sourceLocalKey, audioSourceKind, audioSourceValue, displayName, title, artist, album, durationMillis, sizeBytes, modifiedAtEpochMillis, lastSeenScanId, createdAtEpochMillis, updatedAtEpochMillis, trackNumber, discNumber, artworkBytes, artworkMimeType ->
        LibraryTrack(
            id = id,
            sourceId = srcId,
            sourceLocalKey = sourceLocalKey,
            audioSource = audioSourceFrom(audioSourceKind, audioSourceValue),
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
    }.executeAsList()

    override fun artworkForTrack(trackId: String): TrackArtwork? {
        val metadata = database.libraryTrackQueries
            .selectArtworkMetadataForTrack(trackId)
            .executeAsOneOrNull() ?: return null
        val byteLength = metadata.artworkByteLength ?: return null
        if (byteLength < 0L || byteLength > Int.MAX_VALUE.toLong()) return null
        if (byteLength == 0L) return TrackArtwork(bytes = ByteArray(0), mimeType = metadata.artworkMimeType)

        val artworkBytes = ByteArray(byteLength.toInt())
        repeat(artworkChunkCount(byteLength)) { chunkIndex ->
            val destinationOffset = chunkIndex * ARTWORK_CHUNK_SIZE_BYTES
            val requestedLength = minOf(ARTWORK_CHUNK_SIZE_BYTES, artworkBytes.size - destinationOffset)
            val chunk = database.libraryTrackQueries
                .selectArtworkChunkForTrack(
                    startPosition = (destinationOffset.toLong() + 1L).toString(),
                    chunkLength = requestedLength.toString(),
                    id = trackId,
                )
                .executeAsOneOrNull()?.artworkChunk ?: return null
            if (chunk.size != requestedLength) return null
            chunk.copyInto(
                destination = artworkBytes,
                destinationOffset = destinationOffset,
            )
        }
        return TrackArtwork(bytes = artworkBytes, mimeType = metadata.artworkMimeType)
    }

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

    override fun scanErrors(scanId: String): List<ScanError> = database.scanSessionQueries.selectScanErrorsForScan(scanId) { id, scanId_, sourceLocalKey, displayPath, reason, recoverable, createdAtEpochMillis ->
        ScanError(
            id = id,
            scanId = scanId_,
            sourceLocalKey = sourceLocalKey,
            displayPath = displayPath,
            reason = reason,
            recoverable = recoverable != 0L,
            createdAtEpochMillis = createdAtEpochMillis,
        )
    }.executeAsList()

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        val result = database.libraryTrackQueries.removeMissingTracks(sourceId, latestScanId)
        return result.value.toInt()
    }

    override fun removeSource(sourceId: String) {
        database.transaction {
            database.scanErrorQueries.removeErrorsForSource(sourceId)
            database.scanSessionQueries.removeSessionsForSource(sourceId)
            database.libraryTrackQueries.removeTracksForSource(sourceId)
            database.librarySourceQueries.removeSource(sourceId)
        }
    }

    override fun clearAll() {
        database.transaction {
            database.scanErrorQueries.clearAllErrors()
            database.scanSessionQueries.clearAllSessions()
            database.libraryTrackQueries.clearAllTracks()
            database.librarySourceQueries.clearAllSources()
        }
    }
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
    get() = when (this) {
        is AudioSource.FilePath -> "FilePath"
        is AudioSource.Uri -> "Uri"
        is AudioSource.FileDescriptor -> "FileDescriptor"
    }

private val AudioSource.stableValue: String
    get() = when (this) {
        is AudioSource.FilePath -> path
        is AudioSource.Uri -> value
        is AudioSource.FileDescriptor -> stableKey
    }

private fun audioSourceFrom(kind: String, value: String): AudioSource = when (kind) {
    "FilePath" -> AudioSource.FilePath(value)
    "Uri" -> AudioSource.Uri(value)
    "FileDescriptor" -> AudioSource.Uri(value)
    else -> AudioSource.Uri(value)
}
