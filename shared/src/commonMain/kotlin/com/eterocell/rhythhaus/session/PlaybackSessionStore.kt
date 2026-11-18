package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.library.LibraryDatabase

/** Reads the playback session persisted by the legacy Preferences store. */
interface PlaybackSessionLegacySnapshotSource {
    /**
     * Reads the legacy session, returning an empty snapshot when nothing was
     * ever persisted or the stored value cannot be decoded.
     */
    suspend fun read(): PlaybackSessionSnapshot
}

interface PlaybackSessionStore {
    suspend fun read(): PlaybackSessionSnapshot

    suspend fun save(snapshot: PlaybackSessionSnapshot)

    /**
     * Persists only the playback position of the currently selected occurrence.
     *
     * The update is applied only while [currentOccurrenceId] is still the
     * persisted current occurrence; a stale occurrence is ignored so a
     * structural save that already moved on is never partially overwritten.
     * Negative positions are stored as zero.
     */
    suspend fun savePosition(
        currentOccurrenceId: String,
        positionMillis: Long,
    )
}

/**
 * Creates the playback session store backed by the shared [LibraryDatabase].
 *
 * The platform implementation also supplies the legacy Preferences source used
 * for the one-time import of sessions written before the database migration.
 */
expect fun createPlaybackSessionStore(
    database: LibraryDatabase
): PlaybackSessionStore
