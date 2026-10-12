package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val GaplessPreferenceFileName = "gapless.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val gaplessPreferenceStore by lazy {
    DataStoreGaplessPreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            produceFile = { gaplessPreferencePath().toPath() },
        ),
    )
}

public actual fun createGaplessPreferenceStore(): GaplessPreferenceStore =
    gaplessPreferenceStore

@OptIn(ExperimentalForeignApi::class)
private fun gaplessPreferencePath(): String {
    val fileManager = NSFileManager.defaultManager
    val root =
        fileManager
            .URLForDirectory(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                null,
                true,
                null)
            ?.path ?: error("Could not resolve application support directory")
    val folder = "$root/$ApplicationSupportFolderName"
    fileManager.createDirectoryAtPath(
        folder,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return "$folder/$GaplessPreferenceFileName"
}
