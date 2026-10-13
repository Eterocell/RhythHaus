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

private const val AutomaticResumePreferenceFileName =
    "automatic_resume.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val automaticResumePreferenceStore by lazy {
    DataStoreAutomaticResumePreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            produceFile = { automaticResumePreferencePath().toPath() },
        ),
    )
}

public actual fun createAutomaticResumePreferenceStore():
    AutomaticResumePreferenceStore = automaticResumePreferenceStore

@OptIn(ExperimentalForeignApi::class)
private fun automaticResumePreferencePath(): String {
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
        error = null,
    )
    return "$folder/$AutomaticResumePreferenceFileName"
}
