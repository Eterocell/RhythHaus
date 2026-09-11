package com.eterocell.rhythhaus.onboarding

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val OnboardingPreferenceFileName = "onboarding.preferences_pb"

internal class JvmOnboardingPreferenceStoreFactory(
    rootDirectory: File = defaultHomeDirectory(),
    produceFile: (() -> File)? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
 ) {
    val fileProducer = produceFile ?: { onboardingPreferenceFile(rootDirectory) }
    private val dataStore by lazy {
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = scope,
            produceFile = { fileProducer().toOkioPath() },
        )
    }
    private val store by lazy { DataStoreOnboardingPreferenceStore(dataStore) }

    fun createStore(): OnboardingPreferenceStore = store
}

internal fun createJvmOnboardingPreferenceStore(
    rootDirectory: File = defaultHomeDirectory(),
    produceFile: (() -> File)? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): OnboardingPreferenceStore =
    JvmOnboardingPreferenceStoreFactory(rootDirectory, produceFile, scope).createStore()

private val onboardingPreferenceStoreFactory = JvmOnboardingPreferenceStoreFactory()

public actual fun createOnboardingPreferenceStore(): OnboardingPreferenceStore =
    onboardingPreferenceStoreFactory.createStore()

private fun defaultHomeDirectory(): File = File(System.getProperty("user.home"))

private fun onboardingPreferenceFile(rootDirectory: File): File =
    File(rootDirectory, "Library/Application Support/RhythHaus/$OnboardingPreferenceFileName")
        .also { it.parentFile?.mkdirs() }
