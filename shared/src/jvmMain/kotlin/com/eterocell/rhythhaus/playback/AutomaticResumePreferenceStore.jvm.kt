package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val AutomaticResumePreferenceFileName =
    "automatic_resume.preferences_pb"

private val automaticResumePreferenceStore by lazy {
    val file = defaultAutomaticResumePreferenceFile()
    DataStoreAutomaticResumePreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { file.toOkioPath() },
        ),
    )
}

public actual fun createAutomaticResumePreferenceStore():
    AutomaticResumePreferenceStore = automaticResumePreferenceStore

private fun defaultAutomaticResumePreferenceFile(): File =
    File(
            System.getProperty("user.home"),
            "Library/Application Support/RhythHaus/$AutomaticResumePreferenceFileName",
        )
        .also { file -> file.parentFile?.mkdirs() }
