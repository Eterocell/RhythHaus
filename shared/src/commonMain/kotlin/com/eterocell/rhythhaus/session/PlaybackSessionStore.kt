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
        val encodedQueue = PlaybackSessionCodec.encodeIds(snapshot.queueIds)
        val encodedCurrent = snapshot.currentTrackId?.let { currentId ->
            require(currentId in snapshot.queueIds)
            PlaybackSessionCodec.encodeIds(listOf(currentId))
        }
        val positionMillis = snapshot.positionMillis.coerceAtLeast(0L)
        val repeatMode = snapshot.repeatMode.name
        val shuffleMode = snapshot.shuffleMode.name

        dataStore.edit { preferences ->
            preferences[QueueIdsPreferenceKey] = encodedQueue
            preferences[CurrentIdPreferenceKey] = encodedCurrent.orEmpty()
            preferences[PositionPreferenceKey] = positionMillis
            preferences[RepeatModePreferenceKey] = repeatMode
            preferences[ShuffleModePreferenceKey] = shuffleMode
        }
    }

    private fun decodeSnapshot(preferences: Preferences): PlaybackSessionSnapshot {
        val encodedQueue = preferences[QueueIdsPreferenceKey] ?: ""
        val encodedCurrent = preferences[CurrentIdPreferenceKey] ?: ""
        val positionMillis = preferences[PositionPreferenceKey] ?: 0L
        val repeatModeName = preferences[RepeatModePreferenceKey] ?: RepeatMode.StopAfterQueue.name
        val shuffleModeName = preferences[ShuffleModePreferenceKey] ?: ShuffleMode.Off.name

        val queueIds = PlaybackSessionCodec.decodeIds(encodedQueue) ?: return PlaybackSessionSnapshot()
        val currentIds = PlaybackSessionCodec.decodeIds(encodedCurrent) ?: return PlaybackSessionSnapshot()
        if (currentIds.size > 1) return PlaybackSessionSnapshot()
        val currentTrackId = currentIds.singleOrNull()
        if (currentTrackId != null && currentTrackId !in queueIds) return PlaybackSessionSnapshot()
        if (positionMillis < 0L) return PlaybackSessionSnapshot()
        val repeatMode = enumValueOrNull<RepeatMode>(repeatModeName) ?: return PlaybackSessionSnapshot()
        val shuffleMode = enumValueOrNull<ShuffleMode>(shuffleModeName) ?: return PlaybackSessionSnapshot()

        return PlaybackSessionSnapshot(
            queueIds = queueIds,
            currentTrackId = currentTrackId,
            positionMillis = positionMillis,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
        )
    }
}

expect fun createPlaybackSessionStore(): PlaybackSessionStore

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    enumValues<T>().firstOrNull { value -> value.name == name }
