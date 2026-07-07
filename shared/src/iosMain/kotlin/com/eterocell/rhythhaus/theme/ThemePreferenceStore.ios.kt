package com.eterocell.rhythhaus.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val ThemePreferenceFileName = "theme.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val themeDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        produceFile = { themePreferencePath().toPath() },
    )
}

actual fun createThemePreferenceStore(): ThemePreferenceStore = DataStoreThemePreferenceStore(themeDataStore)

@OptIn(ExperimentalForeignApi::class)
private fun themePreferencePath(): String {
    val fileManager = NSFileManager.defaultManager
    val applicationSupport = fileManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("Could not resolve application support directory")
    val folder = "$applicationSupport/$ApplicationSupportFolderName"
    fileManager.createDirectoryAtPath(folder, withIntermediateDirectories = true, attributes = null, error = null)
    return "$folder/$ThemePreferenceFileName"
}

@Composable
actual fun systemPrefersDarkTheme(): Boolean = isSystemInDarkTheme()
