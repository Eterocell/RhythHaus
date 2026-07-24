package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val ThemeModePreferenceKey = stringPreferencesKey("theme_mode")

interface ThemePreferenceStore {
    val selectedThemeMode: Flow<RhythHausThemeMode>

    suspend fun setSelectedThemeMode(mode: RhythHausThemeMode)
}

class DataStoreThemePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceStore {
    override val selectedThemeMode: Flow<RhythHausThemeMode> =
        dataStore.data.map { preferences ->
            RhythHausThemeMode.fromSerialized(
                preferences[ThemeModePreferenceKey])
        }

    override suspend fun setSelectedThemeMode(mode: RhythHausThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModePreferenceKey] = mode.serialized
        }
    }
}

expect fun createThemePreferenceStore(): ThemePreferenceStore

@Composable expect fun systemPrefersDarkTheme(): Boolean
