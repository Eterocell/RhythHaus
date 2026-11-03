package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val ThemeModePreferenceKey = stringPreferencesKey("theme_mode")
private val PaletteIdPreferenceKey = stringPreferencesKey("palette_id")
private val DynamicColorEnabledPreferenceKey =
    booleanPreferencesKey("dynamic_color_enabled")

/**
 * Immutable user-selected appearance settings persisted by
 * [ThemePreferenceStore].
 */
data class ThemePreferenceState(
    /** Requested system, light, or dark appearance mode. */
    val mode: RhythHausThemeMode,
    /** Selected curated palette, retained while dynamic colors are enabled. */
    val paletteId: HausPaletteId,
    /** Whether Android dynamic colors are requested. */
    val dynamicColorEnabled: Boolean,
)

/** Persists the user's independent appearance choices. */
interface ThemePreferenceStore {
    /** Emits the latest persisted mode, palette, and dynamic-color request. */
    val state: Flow<ThemePreferenceState>

    /**
     * Persists the requested appearance mode without changing other choices.
     */
    suspend fun setMode(mode: RhythHausThemeMode)

    /** Persists the curated palette and disables dynamic colors atomically. */
    suspend fun setPaletteId(paletteId: HausPaletteId)

    /** Persists whether dynamic colors are requested. */
    suspend fun setDynamicColorEnabled(enabled: Boolean)
}

/** DataStore-backed implementation of the appearance preference contract. */
class DataStoreThemePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceStore {
    override val state: Flow<ThemePreferenceState> =
        dataStore.data.map { preferences ->
            ThemePreferenceState(
                mode =
                    RhythHausThemeMode.fromSerialized(
                        preferences[ThemeModePreferenceKey]),
                paletteId =
                    HausPaletteId.fromSerialized(
                        preferences[PaletteIdPreferenceKey]),
                dynamicColorEnabled =
                    preferences[DynamicColorEnabledPreferenceKey] ?: false,
            )
        }

    override suspend fun setMode(mode: RhythHausThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModePreferenceKey] = mode.serialized
        }
    }

    override suspend fun setPaletteId(paletteId: HausPaletteId) {
        dataStore.edit { preferences ->
            preferences[PaletteIdPreferenceKey] = paletteId.serialized
            preferences[DynamicColorEnabledPreferenceKey] = false
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DynamicColorEnabledPreferenceKey] = enabled
        }
    }
}

expect fun createThemePreferenceStore(): ThemePreferenceStore

@Composable expect fun systemPrefersDarkTheme(): Boolean
