package com.eterocell.rhythhaus.session

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.eterocell.rhythhaus.library.LibraryDatabase
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val PlaybackSessionPreferenceFileName =
    "playback_session.preferences_pb"

internal class JvmPlaybackSessionStoreFactory(
    homeDirectory: File,
    private val scope: CoroutineScope,
) {
    /** Legacy Preferences file imported once into the database store. */
    private val preferenceFile: File =
        File(
                homeDirectory,
                "Library/Application Support/RhythHaus/$PlaybackSessionPreferenceFileName",
            )
            .also { file -> file.parentFile?.mkdirs() }

    private val legacyDataStore by lazy {
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = scope,
            produceFile = { preferenceFile.toOkioPath() },
        )
    }

    fun createStore(database: LibraryDatabase): PlaybackSessionStore =
        SqlDelightPlaybackSessionStore(
            database = database.database,
            legacySource =
                DataStorePlaybackSessionLegacySource(legacyDataStore),
        )
}

private val playbackSessionStoreFactory =
    JvmPlaybackSessionStoreFactory(
        homeDirectory = File(System.getProperty("user.home")),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )

actual fun createPlaybackSessionStore(
    database: LibraryDatabase
): PlaybackSessionStore = playbackSessionStoreFactory.createStore(database)
