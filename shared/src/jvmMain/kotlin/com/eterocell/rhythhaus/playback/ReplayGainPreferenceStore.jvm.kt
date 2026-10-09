package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val ReplayGainPreferenceFileName = "replaygain.preferences_pb"

private val replayGainDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { defaultReplayGainPreferenceFile().toOkioPath() },
    )
}

public actual fun createReplayGainPreferenceStore(): ReplayGainPreferenceStore =
    DataStoreReplayGainPreferenceStore(replayGainDataStore)

private fun defaultReplayGainPreferenceFile(): File =
    File(
            System.getProperty("user.home"),
            "Library/Application Support/RhythHaus/$ReplayGainPreferenceFileName",
        )
        .also { file -> file.parentFile?.mkdirs() }
