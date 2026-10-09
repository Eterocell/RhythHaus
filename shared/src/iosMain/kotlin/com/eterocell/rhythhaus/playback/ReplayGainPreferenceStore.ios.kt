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

private const val ReplayGainPreferenceFileName = "replaygain.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val replayGainDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        produceFile = { replayGainPreferencePath().toPath() },
    )
}

public actual fun createReplayGainPreferenceStore(): ReplayGainPreferenceStore =
    DataStoreReplayGainPreferenceStore(replayGainDataStore)

@OptIn(ExperimentalForeignApi::class)
private fun replayGainPreferencePath(): String {
    val fileManager = NSFileManager.defaultManager
    val applicationSupport =
        fileManager
            .URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
            ?.path ?: error("Could not resolve application support directory")
    val folder = "$applicationSupport/$ApplicationSupportFolderName"
    fileManager.createDirectoryAtPath(
        folder,
        withIntermediateDirectories = true,
        attributes = null,
        error = null)
    return "$folder/$ReplayGainPreferenceFileName"
}
