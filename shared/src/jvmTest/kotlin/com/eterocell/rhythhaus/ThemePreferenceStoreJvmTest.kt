package com.eterocell.rhythhaus

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class ThemePreferenceStoreJvmTest {
    @Test
    fun themePreferenceStoreDefaultsToSystemAndPersistsSelectedMode() = runBlocking {
        val tempFile = File.createTempFile("rhythhaus-theme", ".preferences_pb").apply { delete() }
        try {
            val store = DataStoreThemePreferenceStore(
                PreferenceDataStoreFactory.createWithPath(
                    corruptionHandler = null,
                    migrations = emptyList(),
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    produceFile = { tempFile.toOkioPath() },
                ),
            )

            assertEquals(RhythHausThemeMode.System, store.selectedThemeMode.first())

            store.setSelectedThemeMode(RhythHausThemeMode.Dark)
            assertEquals(RhythHausThemeMode.Dark, store.selectedThemeMode.first())

            store.setSelectedThemeMode(RhythHausThemeMode.Light)
            assertEquals(RhythHausThemeMode.Light, store.selectedThemeMode.first())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun themePreferenceStoreFallsBackToSystemForInvalidPersistedValue() = runBlocking {
        val tempFile = File.createTempFile("rhythhaus-theme-invalid", ".preferences_pb").apply { delete() }
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                corruptionHandler = null,
                migrations = emptyList(),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { tempFile.toOkioPath() },
            )
            val store = DataStoreThemePreferenceStore(dataStore)

            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("theme_mode")] = "neon"
            }

            assertEquals(RhythHausThemeMode.System, store.selectedThemeMode.first())
        } finally {
            tempFile.delete()
        }
    }
}
