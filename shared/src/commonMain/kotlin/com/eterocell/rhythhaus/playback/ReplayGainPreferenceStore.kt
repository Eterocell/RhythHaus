package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.ReplayGainMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val ReplayGainModePreferenceKey =
    stringPreferencesKey("replaygain_mode")

/**
 * Durable listener preference; emitted values are never optimistic selections.
 */
public interface ReplayGainPreferenceStore {
    /**
     * Restored committed mode, with missing or unreadable state defaulting Off.
     */
    public val selectedMode: Flow<ReplayGainMode>

    /**
     * Persists the mode or throws without accepting the requested selection.
     */
    public suspend fun setSelectedMode(mode: ReplayGainMode)
}

internal class DataStoreReplayGainPreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : ReplayGainPreferenceStore {
    override val selectedMode: Flow<ReplayGainMode> =
        dataStore.data
            .catch { failure ->
                if (failure is CancellationException) throw failure
                emit(emptyPreferences())
            }
            .map { preferences ->
                // Preferences can contain a valid protobuf value of the wrong
                // type. Read it without a typed cast so that it also means Off.
                val serialized =
                    preferences
                        .asMap()
                        .entries
                        .firstOrNull {
                            it.key.name == ReplayGainModePreferenceKey.name
                        }
                        ?.value as? String
                ReplayGainMode.fromSerialized(serialized)
            }

    override suspend fun setSelectedMode(mode: ReplayGainMode) {
        dataStore.edit { preferences ->
            preferences[ReplayGainModePreferenceKey] = mode.serialized
        }
    }
}

/**
 * Uses the process-owned platform DataStore in its dedicated preference file.
 */
public expect fun createReplayGainPreferenceStore(): ReplayGainPreferenceStore
