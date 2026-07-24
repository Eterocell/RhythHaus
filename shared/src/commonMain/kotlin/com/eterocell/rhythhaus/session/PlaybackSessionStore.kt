package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlinx.coroutines.flow.first

private val QueueIdsPreferenceKey = stringPreferencesKey("queue_ids")
private val CurrentIdPreferenceKey = stringPreferencesKey("current_id")
private val QueueEntriesPreferenceKey = stringPreferencesKey("queue_entries")
private val CurrentOccurrencePreferenceKey = stringPreferencesKey("current_occurrence_id")
private val PositionPreferenceKey = longPreferencesKey("position_millis")
private val RepeatModePreferenceKey = stringPreferencesKey("repeat_mode")
private val ShuffleModePreferenceKey = stringPreferencesKey("shuffle_mode")

interface PlaybackSessionStore {
    suspend fun read(): PlaybackSessionSnapshot

    suspend fun save(snapshot: PlaybackSessionSnapshot)
}

class DataStorePlaybackSessionStore(
    private val dataStore: DataStore<Preferences>,
) : PlaybackSessionStore {
    override suspend fun read(): PlaybackSessionSnapshot = decodeSnapshot(dataStore.data.first())

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        val encodedQueue = PlaybackSessionCodec.encodeQueue(snapshot.queue)
        val encodedCurrent = snapshot.currentOccurrenceId?.let { currentId ->
            require(snapshot.queue.any { it.occurrenceId == currentId })
            PlaybackSessionCodec.encodeIds(listOf(currentId))
        }
        val positionMillis = snapshot.positionMillis.coerceAtLeast(0L)
        val repeatMode = snapshot.repeatMode.name
        val shuffleMode = snapshot.shuffleMode.name

        dataStore.edit { preferences ->
            preferences[QueueEntriesPreferenceKey] = encodedQueue
            preferences[CurrentOccurrencePreferenceKey] = encodedCurrent.orEmpty()
            preferences.remove(QueueIdsPreferenceKey)
            preferences.remove(CurrentIdPreferenceKey)
            preferences[PositionPreferenceKey] = positionMillis
            preferences[RepeatModePreferenceKey] = repeatMode
            preferences[ShuffleModePreferenceKey] = shuffleMode
        }
    }

    private fun decodeSnapshot(preferences: Preferences): PlaybackSessionSnapshot {
        val encodedQueue = preferences[QueueEntriesPreferenceKey]
        val encodedCurrent = preferences[CurrentOccurrencePreferenceKey]
        val positionMillis = preferences[PositionPreferenceKey] ?: 0L
        val repeatModeName = preferences[RepeatModePreferenceKey] ?: RepeatMode.StopAfterQueue.name
        val shuffleModeName = preferences[ShuffleModePreferenceKey] ?: ShuffleMode.Off.name

        val queue = if (encodedQueue != null) {
            PlaybackSessionCodec.decodeQueue(encodedQueue) ?: return PlaybackSessionSnapshot()
        } else {
            val legacyIds = PlaybackSessionCodec.decodeIds(preferences[QueueIdsPreferenceKey] ?: "")
                ?: return PlaybackSessionSnapshot()
            normalizeLegacyQueue(legacyIds)
        }
        val currentIds = PlaybackSessionCodec.decodeIds(encodedCurrent ?: preferences[CurrentIdPreferenceKey] ?: "")
            ?: return PlaybackSessionSnapshot()
        if (currentIds.size > 1) return PlaybackSessionSnapshot()
        val storedCurrent = currentIds.singleOrNull()
        val currentOccurrenceId = if (encodedQueue != null) {
            storedCurrent
        } else {
            storedCurrent?.let { current -> queue.firstOrNull { it.trackId == current }?.occurrenceId }
        }
        if (currentOccurrenceId != null && queue.none { it.occurrenceId == currentOccurrenceId }) return PlaybackSessionSnapshot()
        if (positionMillis < 0L) return PlaybackSessionSnapshot()
        val repeatMode = enumValueOrNull<RepeatMode>(repeatModeName) ?: return PlaybackSessionSnapshot()
        val shuffleMode = enumValueOrNull<ShuffleMode>(shuffleModeName) ?: return PlaybackSessionSnapshot()

        return PlaybackSessionSnapshot(
            queue = queue,
            currentOccurrenceId = currentOccurrenceId,
            positionMillis = positionMillis,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )
    }
}

expect fun createPlaybackSessionStore(): PlaybackSessionStore

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? = enumValues<T>().firstOrNull { value -> value.name == name }
