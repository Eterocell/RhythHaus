package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public enum class GaplessPlaybackMode { Off, On }

private val GaplessPreferenceKey = stringPreferencesKey("gapless_playback")

public interface GaplessPreferenceStore {
    public val selectedMode: StateFlow<GaplessPlaybackMode>
    public suspend fun readMode(): GaplessPlaybackMode
    public suspend fun setMode(mode: GaplessPlaybackMode)
}

internal class DataStoreGaplessPreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : GaplessPreferenceStore {
    private val gate = Mutex()
    private val mutableMode = MutableStateFlow(GaplessPlaybackMode.Off)
    override val selectedMode: StateFlow<GaplessPlaybackMode> = mutableMode.asStateFlow()

    override suspend fun readMode(): GaplessPlaybackMode = gate.withLock {
        val mode = try {
            if (dataStore.data.first().asMap()[GaplessPreferenceKey] == "on")
                GaplessPlaybackMode.On else GaplessPlaybackMode.Off
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            GaplessPlaybackMode.Off
        }
        mutableMode.value = mode
        mode
    }

    override suspend fun setMode(mode: GaplessPlaybackMode) {
        gate.withLock {
            dataStore.edit { it[GaplessPreferenceKey] = if (mode == GaplessPlaybackMode.On) "on" else "off" }
            mutableMode.value = mode
        }
    }
}

public expect fun createGaplessPreferenceStore(): GaplessPreferenceStore
