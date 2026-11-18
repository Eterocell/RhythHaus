package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.library.RhythHausDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Database-backed [PlaybackSessionStore].
 *
 * Structural saves rewrite the whole ordered occurrence queue inside one
 * transaction, so duplicate track IDs and queue lengths beyond the legacy
 * Preferences limits are persisted as-is.
 *
 * The legacy Preferences session is imported once: a caller that interrupts
 * while the import transaction is running leaves both the session and its
 * import marker untouched, so the next call imports again.
 */
internal class SqlDelightPlaybackSessionStore(
    private val database: RhythHausDatabase,
    private val legacySource: PlaybackSessionLegacySnapshotSource,
) : PlaybackSessionStore {
    // Serializes session writes so a position update can never interleave with
    // a structural save that rewrites the same rows.
    private val lock = Mutex()
    // Single process store; update this cache only after the transaction
    // commits.
    private var committedQueue: List<SessionQueueEntry>? = null
    private var importComplete = false

    override suspend fun read(): PlaybackSessionSnapshot = lock.withLock {
        importLegacySessionOnce()
        persistedSnapshot().also { committedQueue = it.queue }
    }

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        lock.withLock {
            importLegacySessionOnce()
            val replaceQueue = committedQueue != snapshot.queue
            if (replaceQueue) requireValidSnapshot(snapshot)
            else
                require(
                    snapshot.currentOccurrenceId == null ||
                        snapshot.queue.any {
                            it.occurrenceId == snapshot.currentOccurrenceId
                        })
            database.transaction {
                writeSnapshot(snapshot, replaceQueue)
            }
            committedQueue = snapshot.queue
        }
    }

    override suspend fun savePosition(
        currentOccurrenceId: String,
        positionMillis: Long,
    ) {
        val sanitizedPositionMillis = positionMillis.coerceAtLeast(0L)
        lock.withLock {
            importLegacySessionOnce()
            database.playbackSessionQueries.updatePositionForOccurrence(
                sanitizedPositionMillis,
                currentOccurrenceId,
            )
        }
    }

    private suspend fun importLegacySessionOnce() {
        if (importComplete) return
        val meta =
            database.playbackSessionQueries
                .selectSessionMeta()
                .executeAsOneOrNull()
        if (meta == null || meta.legacyImportCompleted == 0L) {
            val legacy = legacySource.read()
            database.transaction {
                writeSnapshot(legacy, replaceQueue = true)
                database.playbackSessionQueries.markLegacyImportComplete()
            }
            committedQueue = legacy.queue
        }
        importComplete = true
    }

    private fun writeSnapshot(
        snapshot: PlaybackSessionSnapshot,
        replaceQueue: Boolean
    ) {
        if (replaceQueue) {
            database.playbackSessionQueries.deleteQueueEntries()
            snapshot.queue.forEachIndexed { index, entry ->
                database.playbackSessionQueries.insertQueueEntry(
                    entry.occurrenceId,
                    index.toLong(),
                    entry.trackId,
                )
            }
        }
        database.playbackSessionQueries.upsertSessionMeta(
            snapshot.currentOccurrenceId,
            snapshot.positionMillis.coerceAtLeast(0L),
            snapshot.repeatMode.name,
            snapshot.shuffleMode.name,
        )
    }

    private fun persistedSnapshot(): PlaybackSessionSnapshot =
        database.transactionWithResult {
            val meta =
                database.playbackSessionQueries
                    .selectSessionMeta()
                    .executeAsOneOrNull()
                    ?: return@transactionWithResult PlaybackSessionSnapshot()
            val queue =
                database.playbackSessionQueries
                    .selectQueueEntries(::SessionQueueEntry)
                    .executeAsList()
            PlaybackSessionSnapshot(
                queue = queue,
                currentOccurrenceId =
                    meta.currentOccurrenceId?.takeIf { current ->
                        queue.any { it.occurrenceId == current }
                    },
                positionMillis = meta.positionMillis.coerceAtLeast(0L),
                repeatMode =
                    RepeatMode.entries.firstOrNull {
                        it.name == meta.repeatMode
                    } ?: RepeatMode.StopAfterQueue,
                shuffleMode =
                    ShuffleMode.entries.firstOrNull {
                        it.name == meta.shuffleMode
                    } ?: ShuffleMode.Off,
            )
        }

    private fun requireValidSnapshot(snapshot: PlaybackSessionSnapshot) {
        val occurrenceIds = HashSet<String>(snapshot.queue.size)
        snapshot.queue.forEach { entry ->
            require(entry.occurrenceId.isNotEmpty()) {
                "Playback session occurrence id must not be empty"
            }
            require(entry.trackId.isNotEmpty()) {
                "Playback session track id must not be empty"
            }
            require(occurrenceIds.add(entry.occurrenceId)) {
                "Playback session occurrence ids must be unique"
            }
        }
        val currentOccurrenceId = snapshot.currentOccurrenceId ?: return
        require(currentOccurrenceId in occurrenceIds) {
            "Playback session current occurrence must be queued"
        }
    }
}
