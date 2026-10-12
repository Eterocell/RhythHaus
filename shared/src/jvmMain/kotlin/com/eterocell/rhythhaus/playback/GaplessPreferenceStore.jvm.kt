package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val GaplessPreferenceFileName = "gapless.preferences_pb"

private val gaplessPreferenceStore by lazy {
    DataStoreGaplessPreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { defaultGaplessPreferenceFile().toOkioPath() },
        ),
    )
}

public actual fun createGaplessPreferenceStore(): GaplessPreferenceStore =
    gaplessPreferenceStore

private fun defaultGaplessPreferenceFile(): File =
    File(
            System.getProperty("user.home"),
            "Library/Application Support/RhythHaus/$GaplessPreferenceFileName")
        .also { it.parentFile?.mkdirs() }
