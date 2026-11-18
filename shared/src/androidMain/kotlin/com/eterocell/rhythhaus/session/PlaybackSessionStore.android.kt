package com.eterocell.rhythhaus.session

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val PlaybackSessionPreferenceFileName =
    "playback_session.preferences_pb"

private val legacyPlaybackSessionDataStore by lazy {
    val file =
        File(
            LibraryDatabaseContext.applicationContext.filesDir,
            PlaybackSessionPreferenceFileName)
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { file.toOkioPath() },
    )
}

actual fun createPlaybackSessionStore(
    database: LibraryDatabase
): PlaybackSessionStore =
    SqlDelightPlaybackSessionStore(
        database = database.database,
        legacySource =
            DataStorePlaybackSessionLegacySource(
                legacyPlaybackSessionDataStore),
    )
