package com.eterocell.rhythhaus.session

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

private const val PlaybackSessionPreferenceFileName =
    "playback_session.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val playbackSessionDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        produceFile = { playbackSessionPreferencePath().toPath() },
    )
}

actual fun createPlaybackSessionStore(): PlaybackSessionStore =
    DataStorePlaybackSessionStore(playbackSessionDataStore)

@OptIn(ExperimentalForeignApi::class)
private fun playbackSessionPreferencePath(): String {
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
    return "$folder/$PlaybackSessionPreferenceFileName"
}
