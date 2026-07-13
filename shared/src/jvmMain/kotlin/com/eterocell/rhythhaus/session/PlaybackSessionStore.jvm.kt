package com.eterocell.rhythhaus.session

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

private const val PlaybackSessionPreferenceFileName = "playback_session.preferences_pb"

internal class JvmPlaybackSessionStoreFactory(
    homeDirectory: File,
    private val scope: CoroutineScope,
) {
    val preferenceFile: File = File(
        homeDirectory,
        "Library/Application Support/RhythHaus/$PlaybackSessionPreferenceFileName",
    ).also { file -> file.parentFile?.mkdirs() }

    val dataStore by lazy {
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = emptyList(),
            scope = scope,
            produceFile = { preferenceFile.toOkioPath() },
        )
    }

    fun createStore(): PlaybackSessionStore = DataStorePlaybackSessionStore(dataStore)
}

private val playbackSessionStoreFactory = JvmPlaybackSessionStoreFactory(
    homeDirectory = File(System.getProperty("user.home")),
    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
)

actual fun createPlaybackSessionStore(): PlaybackSessionStore =
    playbackSessionStoreFactory.createStore()
