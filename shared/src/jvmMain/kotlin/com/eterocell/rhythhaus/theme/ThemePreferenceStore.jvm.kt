package com.eterocell.rhythhaus.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath
import java.io.File

private const val ThemePreferenceFileName = "theme.preferences_pb"

private val themeDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { defaultThemePreferenceFile().toOkioPath() },
    )
}

actual fun createThemePreferenceStore(): ThemePreferenceStore = DataStoreThemePreferenceStore(themeDataStore)

private fun defaultThemePreferenceFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/$ThemePreferenceFileName",
).also { file -> file.parentFile?.mkdirs() }

@Composable
actual fun systemPrefersDarkTheme(): Boolean = isSystemInDarkTheme()
