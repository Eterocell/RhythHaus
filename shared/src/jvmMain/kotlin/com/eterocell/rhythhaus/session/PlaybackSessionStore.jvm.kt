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

private val playbackSessionDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { defaultPlaybackSessionPreferenceFile().toOkioPath() },
    )
}

actual fun createPlaybackSessionStore(): PlaybackSessionStore =
    DataStorePlaybackSessionStore(playbackSessionDataStore)

private fun defaultPlaybackSessionPreferenceFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/$PlaybackSessionPreferenceFileName",
).also { file -> file.parentFile?.mkdirs() }
