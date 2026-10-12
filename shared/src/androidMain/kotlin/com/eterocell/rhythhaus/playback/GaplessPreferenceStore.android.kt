package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val GaplessPreferenceFileName = "gapless.preferences_pb"

private val gaplessPreferenceStore by lazy {
    val file = File(LibraryDatabaseContext.applicationContext.filesDir, GaplessPreferenceFileName)
    DataStoreGaplessPreferenceStore(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { file.toOkioPath() },
        ),
    )
}

public actual fun createGaplessPreferenceStore(): GaplessPreferenceStore = gaplessPreferenceStore
