package com.eterocell.rhythhaus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath
import java.io.File

private const val ThemePreferenceFileName = "theme.preferences_pb"

actual fun createThemePreferenceStore(): ThemePreferenceStore {
    val file = File(LibraryDatabaseContext.applicationContext.filesDir, ThemePreferenceFileName)
    return DataStoreThemePreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { file.toOkioPath() },
        ),
    )
}

@Composable
actual fun systemPrefersDarkTheme(): Boolean = isSystemInDarkTheme()
