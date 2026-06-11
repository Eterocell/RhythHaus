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

class SqlDelightLibraryRepository(
    private val database: RhythHausDatabase,
) : LibraryRepository {
    private val queries = database.rhythHausDatabaseQueries

    override fun upsertSource(source: LibrarySource) {
        queries.upsertSource(
            id = source.id,
            platform_kind = source.platformKind.name,
            display_name = source.displayName,
            handle = source.handle,
            created_at_epoch_millis = source.createdAtEpochMillis,
            last_scan_at_epoch_millis = source.lastScanAtEpochMillis,
            access_status = source.accessStatus.name,
        )
    }

    override fun sources(): List<LibrarySource> = queries.selectSources().executeAsList().map { row ->
        LibrarySource(
            id = row.id,
            platformKind = LibraryPlatformKind.valueOf(row.platform_kind),
            displayName = row.display_name,
            handle = row.handle,
            createdAtEpochMillis = row.created_at_epoch_millis,
            lastScanAtEpochMillis = row.last_scan_at_epoch_millis,
            accessStatus = LibrarySourceAccessStatus.valueOf(row.access_status),
        )
    }

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult {
        val existing = queries.selectTrackBySourceKey(track.sourceId, track.sourceLocalKey).executeAsOneOrNull()
        return if (existing == null) {
            queries.insertTrack(
                id = track.id,
                source_id = track.sourceId,
                source_local_key = track.sourceLocalKey,
                audio_source_kind = track.audioSource.kindName,
                audio_source_value = track.audioSource.stableValue,
                display_name = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_millis = track.durationMillis,
                size_bytes = track.sizeBytes,
                modified_at_epoch_millis = track.modifiedAtEpochMillis,
                last_seen_scan_id = track.lastSeenScanId,
                created_at_epoch_millis = track.createdAtEpochMillis,
                updated_at_epoch_millis = track.updatedAtEpochMillis,
            )
            TrackUpsertResult.Added
        } else {
            queries.updateTrack(
                audio_source_kind = track.audioSource.kindName,
                audio_source_value = track.audioSource.stableValue,
                display_name = track.displayName,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration_millis = track.durationMillis,
                size_bytes = track.sizeBytes,
                modified_at_epoch_millis = track.modifiedAtEpochMillis,
                last_seen_scan_id = track.lastSeenScanId,
                updated_at_epoch_millis = track.updatedAtEpochMillis,
                id = existing.id,
            )
            TrackUpsertResult.Updated
        }
    }

    override fun tracks(): List<LibraryTrack> = queries.selectTracks().executeAsList().map { it.toLibraryTrack() }

    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        queries.selectTracksForSource(sourceId).executeAsList().map { it.toLibraryTrack() }

    override fun insertScanSession(session: ScanSession) = upsertScanSession(session, insert = true)

    override fun updateScanSession(session: ScanSession) = upsertScanSession(session, insert = false)

    override fun insertScanError(error: ScanError) {
        queries.insertScanError(
            id = error.id,
            scan_id = error.scanId,
            source_local_key = error.sourceLocalKey,
            display_path = error.displayPath,
            reason = error.reason,
            recoverable = if (error.recoverable) 1L else 0L,
            created_at_epoch_millis = error.createdAtEpochMillis,
        )
    }

    override fun scanErrors(scanId: String): List<ScanError> = queries.selectScanErrors(scanId).executeAsList().map { row ->
        ScanError(
            id = row.id,
            scanId = row.scan_id,
            sourceLocalKey = row.source_local_key,
            displayPath = row.display_path,
            reason = row.reason,
            recoverable = row.recoverable != 0L,
            createdAtEpochMillis = row.created_at_epoch_millis,
        )
    }

    override fun removeMissingTracks(sourceId: String, latestScanId: String): Int {
        val missing = queries.selectMissingTracks(sourceId, latestScanId).executeAsList().size
        queries.removeMissingTracks(sourceId, latestScanId)
        return missing
    }

    private fun upsertScanSession(session: ScanSession, insert: Boolean) {
        if (insert) {
            queries.insertScanSession(
                id = session.id,
                source_id = session.sourceId,
                status = session.status.name,
                started_at_epoch_millis = session.startedAtEpochMillis,
                completed_at_epoch_millis = session.completedAtEpochMillis,
                folders_visited = session.foldersVisited.toLong(),
                files_visited = session.filesVisited.toLong(),
                tracks_added = session.tracksAdded.toLong(),
                tracks_updated = session.tracksUpdated.toLong(),
                files_skipped = session.filesSkipped.toLong(),
                terminal_message = session.terminalMessage,
            )
        } else {
            queries.updateScanSession(
                status = session.status.name,
                completed_at_epoch_millis = session.completedAtEpochMillis,
                folders_visited = session.foldersVisited.toLong(),
                files_visited = session.filesVisited.toLong(),
                tracks_added = session.tracksAdded.toLong(),
                tracks_updated = session.tracksUpdated.toLong(),
                files_skipped = session.filesSkipped.toLong(),
                terminal_message = session.terminalMessage,
                id = session.id,
            )
        }
    }
}

private val AudioSource.kindName: String
    get() = when (this) {
        is AudioSource.FilePath -> "FilePath"
        is AudioSource.Uri -> "Uri"
    }

private val AudioSource.stableValue: String
    get() = when (this) {
        is AudioSource.FilePath -> path
        is AudioSource.Uri -> value
    }

private fun audioSourceFrom(kind: String, value: String): AudioSource = when (kind) {
    "FilePath" -> AudioSource.FilePath(value)
    "Uri" -> AudioSource.Uri(value)
    else -> AudioSource.Uri(value)
}

private fun Library_track.toLibraryTrack(): LibraryTrack = LibraryTrack(
    id = id,
    sourceId = source_id,
    sourceLocalKey = source_local_key,
    audioSource = audioSourceFrom(audio_source_kind, audio_source_value),
    displayName = display_name,
    title = title,
    artist = artist,
    album = album,
    durationMillis = duration_millis,
    sizeBytes = size_bytes,
    modifiedAtEpochMillis = modified_at_epoch_millis,
    lastSeenScanId = last_seen_scan_id,
    createdAtEpochMillis = created_at_epoch_millis,
    updatedAtEpochMillis = updated_at_epoch_millis,
)
