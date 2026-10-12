package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.CrossfadeDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val CrossfadeDurationPreferenceKey =
    stringPreferencesKey("crossfade_duration")

/** Durable listener preference, with no optimistic requested selections. */
public interface CrossfadePreferenceStore {
    /**
     * Last read or successfully committed duration. Initially Off until
     * [readDuration] completes; startup must await that durable read.
     */
    public val selectedDuration: StateFlow<CrossfadeDuration>

    /**
     * Reads committed state, defaulting missing or unreadable preferences Off.
     * Unlike the initial [selectedDuration] value, completion establishes that
     * restoration has settled.
     */
    public suspend fun readDuration(): CrossfadeDuration

    /** Persists [duration] or throws without accepting the requested choice. */
    public suspend fun setDuration(duration: CrossfadeDuration)
}

internal class DataStoreCrossfadePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : CrossfadePreferenceStore {
    private val gate = Mutex()
    private val mutableDuration = MutableStateFlow(CrossfadeDuration.Off)

    override val selectedDuration: StateFlow<CrossfadeDuration> =
        mutableDuration.asStateFlow()

    override suspend fun readDuration(): CrossfadeDuration = gate.withLock {
        val duration =
            try {
                val serialized =
                    dataStore.data
                        .first()
                        .asMap()
                        .entries
                        .firstOrNull {
                            it.key.name == CrossfadeDurationPreferenceKey.name
                        }
                        ?.value as? String
                when (serialized) {
                    "3" -> CrossfadeDuration.Seconds3
                    "5" -> CrossfadeDuration.Seconds5
                    "8" -> CrossfadeDuration.Seconds8
                    else -> CrossfadeDuration.Off
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                CrossfadeDuration.Off
            }
        mutableDuration.value = duration
        duration
    }

    override suspend fun setDuration(duration: CrossfadeDuration) {
        gate.withLock {
            dataStore.edit { preferences ->
                preferences[CrossfadeDurationPreferenceKey] =
                    when (duration) {
                        CrossfadeDuration.Off -> "off"
                        CrossfadeDuration.Seconds3 -> "3"
                        CrossfadeDuration.Seconds5 -> "5"
                        CrossfadeDuration.Seconds8 -> "8"
                    }
            }
            mutableDuration.value = duration
        }
    }
}

/** Uses the process-owned platform store in its dedicated preference file. */
public expect fun createCrossfadePreferenceStore(): CrossfadePreferenceStore
