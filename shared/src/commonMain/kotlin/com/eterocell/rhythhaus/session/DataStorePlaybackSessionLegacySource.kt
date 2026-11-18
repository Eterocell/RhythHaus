package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlinx.coroutines.flow.first

private val QueueIdsPreferenceKey = stringPreferencesKey("queue_ids")
private val CurrentIdPreferenceKey = stringPreferencesKey("current_id")
private val QueueEntriesPreferenceKey = stringPreferencesKey("queue_entries")
private val CurrentOccurrencePreferenceKey =
    stringPreferencesKey("current_occurrence_id")
private val PositionPreferenceKey = longPreferencesKey("position_millis")
private val RepeatModePreferenceKey = stringPreferencesKey("repeat_mode")
private val ShuffleModePreferenceKey = stringPreferencesKey("shuffle_mode")

/**
 * Read-only view of the playback session persisted in Preferences before the
 * database migration.
 *
 * Nothing is written back: the database store owns persistence now, and the
 * legacy file is only consulted while its one-time import has not been
 * committed.
 */
internal class DataStorePlaybackSessionLegacySource(
    private val dataStore: DataStore<Preferences>,
) : PlaybackSessionLegacySnapshotSource {
    override suspend fun read(): PlaybackSessionSnapshot =
        decodeSnapshot(dataStore.data.first())

    private fun decodeSnapshot(
        preferences: Preferences
    ): PlaybackSessionSnapshot {
        val encodedQueue = preferences[QueueEntriesPreferenceKey]
        val encodedCurrent = preferences[CurrentOccurrencePreferenceKey]
        val positionMillis = preferences[PositionPreferenceKey] ?: 0L
        val repeatModeName =
            preferences[RepeatModePreferenceKey]
                ?: RepeatMode.StopAfterQueue.name
        val shuffleModeName =
            preferences[ShuffleModePreferenceKey] ?: ShuffleMode.Off.name

        val queue =
            if (encodedQueue != null) {
                PlaybackSessionCodec.decodeQueue(encodedQueue)
                    ?: return PlaybackSessionSnapshot()
            } else {
                val legacyIds =
                    PlaybackSessionCodec.decodeIds(
                        preferences[QueueIdsPreferenceKey] ?: "")
                        ?: return PlaybackSessionSnapshot()
                legacyQueueEntries(legacyIds)
            }
        val currentIds =
            PlaybackSessionCodec.decodeIds(
                encodedCurrent ?: preferences[CurrentIdPreferenceKey] ?: "")
                ?: return PlaybackSessionSnapshot()
        if (currentIds.size > 1) return PlaybackSessionSnapshot()
        val storedCurrent = currentIds.singleOrNull()
        val currentOccurrenceId =
            if (encodedQueue != null) {
                storedCurrent
            } else {
                storedCurrent?.let { current ->
                    queue.firstOrNull { it.trackId == current }?.occurrenceId
                }
            }
        if (currentOccurrenceId != null &&
            queue.none { it.occurrenceId == currentOccurrenceId })
            return PlaybackSessionSnapshot()
        if (positionMillis < 0L) return PlaybackSessionSnapshot()
        val repeatMode =
            RepeatMode.entries.firstOrNull { it.name == repeatModeName }
                ?: return PlaybackSessionSnapshot()
        val shuffleMode =
            ShuffleMode.entries.firstOrNull { it.name == shuffleModeName }
                ?: return PlaybackSessionSnapshot()

        return PlaybackSessionSnapshot(
            queue = queue,
            currentOccurrenceId = currentOccurrenceId,
            positionMillis = positionMillis,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )
    }
}

private fun legacyQueueEntries(
    trackIds: List<String>
): List<SessionQueueEntry> = trackIds.mapIndexed { index, trackId ->
    SessionQueueEntry("legacy-$index", trackId)
}
