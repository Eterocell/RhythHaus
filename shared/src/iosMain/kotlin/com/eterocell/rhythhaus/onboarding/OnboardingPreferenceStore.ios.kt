package com.eterocell.rhythhaus.onboarding

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

private const val OnboardingPreferenceFileName = "onboarding.preferences_pb"
private const val ApplicationSupportFolderName = "RhythHaus"

private val onboardingDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        produceFile = { onboardingPreferencePath().toPath() },
    )
}

public actual fun createOnboardingPreferenceStore(): OnboardingPreferenceStore =
    DataStoreOnboardingPreferenceStore(onboardingDataStore)

@OptIn(ExperimentalForeignApi::class)
private fun onboardingPreferencePath(): String {
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
    return "$folder/$OnboardingPreferenceFileName"
}
