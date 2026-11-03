package com.eterocell.rhythhaus.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.resolveSharedHausTheme
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class ThemePreferenceStoreJvmTest {
    @Test
    fun emptyStoreDefaultsEveryAppearanceChoiceIndependently() = runBlocking {
        withStore("rhythhaus-theme-empty") { store ->
            assertEquals(
                ThemePreferenceState(
                    mode = RhythHausThemeMode.System,
                    paletteId = HausPaletteId.Moss,
                    dynamicColorEnabled = false,
                ),
                store.state.first(),
            )
        }
    }

    @Test
    fun legacyModeOnlyStoreDefaultsPaletteAndMonetIndependently() =
        runBlocking {
            val tempFile = newPreferencesFile("rhythhaus-theme-legacy")
            val scope = newPreferenceScope()
            try {
                val dataStore = newDataStore(tempFile, scope)
                dataStore.edit { preferences ->
                    preferences[ThemeModeKey] = "dark"
                }

                val store = DataStoreThemePreferenceStore(dataStore)

                assertEquals(
                    ThemePreferenceState(
                        mode = RhythHausThemeMode.Dark,
                        paletteId = HausPaletteId.Moss,
                        dynamicColorEnabled = false,
                    ),
                    store.state.first(),
                )
            } finally {
                scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
                tempFile.delete()
            }
        }

    @Test
    fun unknownModeAndPaletteFallBackWithoutChangingOtherPreferences() =
        runBlocking {
            val tempFile = newPreferencesFile("rhythhaus-theme-invalid")
            val scope = newPreferenceScope()
            try {
                val dataStore = newDataStore(tempFile, scope)
                dataStore.edit { preferences ->
                    preferences[ThemeModeKey] = "neon"
                    preferences[PaletteIdKey] = "future-palette"
                    preferences[DynamicColorEnabledKey] = true
                    preferences[UnrelatedPreferenceKey] = "keep"
                }

                val state =
                    DataStoreThemePreferenceStore(dataStore).state.first()

                assertEquals(RhythHausThemeMode.System, state.mode)
                assertEquals(HausPaletteId.Moss, state.paletteId)
                assertEquals(true, state.dynamicColorEnabled)
                val persisted = dataStore.data.first()
                assertEquals("neon", persisted[ThemeModeKey])
                assertEquals("future-palette", persisted[PaletteIdKey])
                assertEquals("keep", persisted[UnrelatedPreferenceKey])
            } finally {
                scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
                tempFile.delete()
            }
        }

    @Test
    fun allAppearanceChoicesRestoreAfterCreatingANewDataStore() = runBlocking {
        val tempFile = newPreferencesFile("rhythhaus-theme-restart")
        val firstScope = newPreferenceScope()
        try {
            val firstStore =
                DataStoreThemePreferenceStore(
                    newDataStore(tempFile, firstScope))
            firstStore.setMode(RhythHausThemeMode.Dark)
            firstStore.setPaletteId(HausPaletteId.Clay)
            firstStore.setDynamicColorEnabled(true)
            assertEquals(
                ThemePreferenceState(
                    RhythHausThemeMode.Dark,
                    HausPaletteId.Clay,
                    true,
                ),
                firstStore.state.first(),
            )
            firstScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()

            val secondScope = newPreferenceScope()
            try {
                val restored =
                    DataStoreThemePreferenceStore(
                        newDataStore(tempFile, secondScope))
                assertEquals(
                    ThemePreferenceState(
                        RhythHausThemeMode.Dark,
                        HausPaletteId.Clay,
                        true,
                    ),
                    restored.state.first(),
                )
            } finally {
                secondScope.coroutineContext[kotlinx.coroutines.Job]
                    ?.cancelAndJoin()
            }
        } finally {
            firstScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
            tempFile.delete()
        }
    }

    @Test
    fun selectingPaletteDisablesMonetAndPreservesMode() = runBlocking {
        withStore("rhythhaus-theme-palette") { store ->
            store.setMode(RhythHausThemeMode.Dark)
            store.setDynamicColorEnabled(true)

            store.setPaletteId(HausPaletteId.Iris)

            assertEquals(
                ThemePreferenceState(
                    RhythHausThemeMode.Dark,
                    HausPaletteId.Iris,
                    false,
                ),
                store.state.first(),
            )
        }
    }

    @Test
    fun modeAndMonetActionsPreservePaletteAndEachOther() = runBlocking {
        withStore("rhythhaus-theme-independent-actions") { store ->
            store.setPaletteId(HausPaletteId.Bay)
            store.setMode(RhythHausThemeMode.Light)
            store.setDynamicColorEnabled(true)

            assertEquals(
                ThemePreferenceState(
                    RhythHausThemeMode.Light,
                    HausPaletteId.Bay,
                    true,
                ),
                store.state.first(),
            )

            store.setMode(RhythHausThemeMode.System)

            assertEquals(
                ThemePreferenceState(
                    RhythHausThemeMode.System,
                    HausPaletteId.Bay,
                    true,
                ),
                store.state.first(),
            )
        }
    }

    @Test
    fun failedWriteKeepsPersistedStateAndTheActionCanBeRetried() = runBlocking {
        val tempFile = newPreferencesFile("rhythhaus-theme-write-failure")
        val scope = newPreferenceScope()
        try {
            val backingStore = newDataStore(tempFile, scope)
            val store =
                DataStoreThemePreferenceStore(FailOnceDataStore(backingStore))
            val before = store.state.first()

            assertFailsWith<IOException> {
                store.setMode(RhythHausThemeMode.Dark)
            }
            assertEquals(before, store.state.first())

            store.setMode(RhythHausThemeMode.Dark)

            assertEquals(RhythHausThemeMode.Dark, store.state.first().mode)
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
            tempFile.delete()
        }
    }

    @Test
    fun failedPaletteSaveKeepsResolvedAppearanceAndRetryCommitsTheSelection() =
        runBlocking {
            val tempFile = newPreferencesFile("rhythhaus-theme-owner-retry")
            val scope = newPreferenceScope()
            try {
                val dataStore = newDataStore(tempFile, scope)
                val persistedStore = DataStoreThemePreferenceStore(dataStore)
                persistedStore.setMode(RhythHausThemeMode.Dark)
                persistedStore.setPaletteId(HausPaletteId.Bay)
                persistedStore.setDynamicColorEnabled(true)
                val persistedBefore = persistedStore.state.first()
                val resolvedBefore =
                    resolveSharedHausTheme(
                        preferences = persistedBefore,
                        systemIsDark = false,
                        dynamicColorAvailable = true,
                        dynamicRoles = null,
                    )
                assertEquals(
                    HausThemeColorSource.Curated, resolvedBefore.source)
                assertEquals(
                    hausPalette(HausPaletteId.Bay, isDark = true),
                    resolvedBefore.colors,
                )

                val owner =
                    ThemePreferenceOwner(
                        DataStoreThemePreferenceStore(
                            FailOnceDataStore(dataStore),
                        ),
                    )
                owner.selectPalette(HausPaletteId.Iris)

                assertEquals(persistedBefore, persistedStore.state.first())
                assertEquals(true, owner.state.value.saveFailed)
                assertEquals(
                    resolvedBefore.colors,
                    resolveSharedHausTheme(
                            preferences = persistedStore.state.first(),
                            systemIsDark = false,
                            dynamicColorAvailable = true,
                            dynamicRoles = null,
                        )
                        .colors,
                )

                owner.retry()

                val persistedAfter = persistedStore.state.first()
                assertEquals(RhythHausThemeMode.Dark, persistedAfter.mode)
                assertEquals(HausPaletteId.Iris, persistedAfter.paletteId)
                assertEquals(false, persistedAfter.dynamicColorEnabled)
                assertEquals(false, owner.state.value.isBusy)
                assertEquals(false, owner.state.value.saveFailed)
                assertEquals(
                    hausPalette(HausPaletteId.Iris, isDark = true),
                    resolveSharedHausTheme(
                            preferences = persistedAfter,
                            systemIsDark = false,
                            dynamicColorAvailable = true,
                            dynamicRoles = null,
                        )
                        .colors,
                )
            } finally {
                scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
                tempFile.delete()
            }
        }

    private suspend fun withStore(
        fileName: String,
        block: suspend (DataStoreThemePreferenceStore) -> Unit,
    ) {
        val tempFile = newPreferencesFile(fileName)
        val scope = newPreferenceScope()
        try {
            block(DataStoreThemePreferenceStore(newDataStore(tempFile, scope)))
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
            tempFile.delete()
        }
    }
}

private val ThemeModeKey = stringPreferencesKey("theme_mode")
private val PaletteIdKey = stringPreferencesKey("palette_id")
private val DynamicColorEnabledKey =
    booleanPreferencesKey("dynamic_color_enabled")
private val UnrelatedPreferenceKey = stringPreferencesKey("unrelated")

private fun newPreferencesFile(name: String): File =
    File.createTempFile(name, ".preferences_pb").apply { delete() }

private fun newPreferenceScope() =
    CoroutineScope(Dispatchers.IO + SupervisorJob())

private fun newDataStore(
    file: File,
    scope: CoroutineScope
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        scope = scope,
        produceFile = { file.toOkioPath() },
    )

private class FailOnceDataStore(
    private val backing: DataStore<Preferences>,
) : DataStore<Preferences> by backing {
    private var failNextUpdate = true

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        if (failNextUpdate) {
            failNextUpdate = false
            throw IOException("Injected DataStore write failure")
        }
        return backing.updateData(transform)
    }
}
