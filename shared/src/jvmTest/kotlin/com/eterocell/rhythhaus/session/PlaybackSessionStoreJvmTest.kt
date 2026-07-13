package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toOkioPath

class PlaybackSessionStoreJvmTest {
    @Test
    fun defaultsToEmptySnapshotAndPersistsEveryField() = runBlocking {
        withStore { store, _ ->
            assertEquals(PlaybackSessionSnapshot(), store.read())

            val snapshot = PlaybackSessionSnapshot(
                queueIds = listOf("one", "two:2", "🎧"),
                currentTrackId = "two:2",
                positionMillis = 12_345L,
                repeatMode = RepeatMode.RepeatPlaylist,
                shuffleMode = ShuffleMode.On,
            )
            store.save(snapshot)

            assertEquals(snapshot, store.read())
        }
    }

    @Test
    fun negativePositionIsPersistedAsZero() = runBlocking {
        withStore { store, _ ->
            store.save(
                PlaybackSessionSnapshot(
                    queueIds = listOf("one"),
                    currentTrackId = "one",
                    positionMillis = -1L,
                ),
            )

            assertEquals(0L, store.read().positionMillis)
        }
    }

    @Test
    fun nonEmptyQueueWithNoCurrentTrackRoundTripsAllOtherFields() = runBlocking {
        withStore { store, _ ->
            val snapshot = PlaybackSessionSnapshot(
                queueIds = listOf("one", "two"),
                currentTrackId = null,
                positionMillis = 8_765L,
                repeatMode = RepeatMode.StopAfterCurrent,
                shuffleMode = ShuffleMode.On,
            )

            store.save(snapshot)

            assertEquals(snapshot, store.read())
        }
    }

    @Test
    fun invalidSaveKeepsPriorDurableSnapshot() = runBlocking {
        withStore { store, _ ->
            val valid = PlaybackSessionSnapshot(queueIds = listOf("one"), currentTrackId = "one")
            store.save(valid)

            assertFailsWith<IllegalArgumentException> {
                store.save(valid.copy(queueIds = List(PlaybackSessionCodec.maxIds + 1) { "$it" }))
            }

            assertEquals(valid, store.read())
        }
    }

    @Test
    fun invalidCurrentIdSaveKeepsPriorDurableSnapshot() = runBlocking {
        withStore { store, _ ->
            val valid = PlaybackSessionSnapshot(queueIds = listOf("one"), currentTrackId = "one")
            store.save(valid)

            assertFailsWith<IllegalArgumentException> {
                store.save(valid.copy(currentTrackId = "missing"))
            }

            assertEquals(valid, store.read())
        }
    }

    @Test
    fun malformedQueueFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[QueueIdsKey] = "2:a" }
        }
    }

    @Test
    fun truncatedCurrentEncodingFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[CurrentIdKey] = "2:a" }
        }
    }

    @Test
    fun twoIdCurrentEncodingFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[CurrentIdKey] = PlaybackSessionCodec.encodeIds(listOf("one", "two")) }
        }
    }

    @Test
    fun currentIdMissingFromQueueFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[CurrentIdKey] = PlaybackSessionCodec.encodeIds(listOf("missing")) }
        }
    }

    @Test
    fun invalidPositionFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[PositionKey] = -1L }
        }
    }

    @Test
    fun invalidRepeatModeFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[RepeatModeKey] = "Forever" }
        }
    }

    @Test
    fun invalidShuffleModeFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[ShuffleModeKey] = "Sometimes" }
        }
    }

    @Test
    fun corruptPreferencesFileRecoversToDefaultAndRemainsUsable() = runBlocking {
        val tempFile = newPreferencesFile("rhythhaus-playback-corrupt")
        tempFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x7f))
        withDataStore(
            file = tempFile,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        ) { dataStore ->
            val store = DataStorePlaybackSessionStore(dataStore)
            assertEquals(PlaybackSessionSnapshot(), store.read())

            val recovered = PlaybackSessionSnapshot(
                queueIds = listOf("recovered"),
                currentTrackId = "recovered",
                positionMillis = 99L,
                repeatMode = RepeatMode.RepeatOne,
                shuffleMode = ShuffleMode.On,
            )
            store.save(recovered)

            assertEquals(recovered, store.read())
        }
    }

    @Test
    fun playbackStorageDoesNotReadOrOverwriteThemePreferences() = runBlocking {
        val themeFile = newPreferencesFile("rhythhaus-theme-isolation")
        val playbackFile = newPreferencesFile("rhythhaus-playback-isolation")
        withDataStores(themeFile, playbackFile) { themeDataStore, playbackDataStore ->
            themeDataStore.edit { preferences ->
                preferences[ThemeModeKey] = "dark"
                preferences[QueueIdsKey] = PlaybackSessionCodec.encodeIds(listOf("theme-file-track"))
            }
            val playbackStore = DataStorePlaybackSessionStore(playbackDataStore)

            assertEquals(PlaybackSessionSnapshot(), playbackStore.read())

            playbackStore.save(
                PlaybackSessionSnapshot(
                    queueIds = listOf("playback-track"),
                    currentTrackId = "playback-track",
                ),
            )

            val themePreferences = themeDataStore.data.first()
            assertEquals("dark", themePreferences[ThemeModeKey])
            assertEquals(
                PlaybackSessionCodec.encodeIds(listOf("theme-file-track")),
                themePreferences[QueueIdsKey],
            )
        }
    }

    @Test
    fun jvmFactoryUsesExactFileNameAndOneLazyDataStoreInstance() = runBlocking {
        val homeDirectory = Files.createTempDirectory("rhythhaus-playback-home").toFile()
        val job = SupervisorJob()
        try {
            val factory = JvmPlaybackSessionStoreFactory(
                homeDirectory = homeDirectory,
                scope = CoroutineScope(Dispatchers.IO + job),
            )

            assertEquals(
                File(homeDirectory, "Library/Application Support/RhythHaus/playback_session.preferences_pb"),
                factory.preferenceFile,
            )
            assertSame(factory.dataStore, factory.dataStore)
            assertEquals(PlaybackSessionSnapshot(), factory.createStore().read())
        } finally {
            job.cancelAndJoin()
            homeDirectory.deleteRecursively()
        }
    }

    private suspend fun assertMalformedFieldFallsBack(
        corrupt: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val valid = PlaybackSessionSnapshot(
            queueIds = listOf("one", "two"),
            currentTrackId = "two",
            positionMillis = 42L,
            repeatMode = RepeatMode.RepeatOne,
            shuffleMode = ShuffleMode.On,
        )

        withStore { store, dataStore ->
            store.save(valid)
            corrupt(dataStore)

            assertEquals(PlaybackSessionSnapshot(), store.read())
        }
    }

    private suspend fun withStore(
        block: suspend (DataStorePlaybackSessionStore, DataStore<Preferences>) -> Unit,
    ) {
        val tempFile = newPreferencesFile("rhythhaus-playback-session")
        withDataStore(tempFile) { dataStore ->
            block(DataStorePlaybackSessionStore(dataStore), dataStore)
        }
    }

    private suspend fun withDataStore(
        file: File,
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        block: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val job = SupervisorJob()
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                corruptionHandler = corruptionHandler,
                migrations = emptyList(),
                scope = CoroutineScope(Dispatchers.IO + job),
                produceFile = { file.toOkioPath() },
            )
            block(dataStore)
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }

    private suspend fun withDataStores(
        firstFile: File,
        secondFile: File,
        block: suspend (DataStore<Preferences>, DataStore<Preferences>) -> Unit,
    ) {
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = testPreferencesDataStore(firstFile, firstJob)
            val second = testPreferencesDataStore(secondFile, secondJob)
            block(first, second)
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            firstFile.delete()
            secondFile.delete()
        }
    }

    private fun testPreferencesDataStore(file: File, job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file.toOkioPath() },
        )

    private fun newPreferencesFile(prefix: String): File =
        File.createTempFile(prefix, ".preferences_pb").apply { delete() }

    private companion object {
        val QueueIdsKey = stringPreferencesKey("queue_ids")
        val CurrentIdKey = stringPreferencesKey("current_id")
        val PositionKey = longPreferencesKey("position_millis")
        val RepeatModeKey = stringPreferencesKey("repeat_mode")
        val ShuffleModeKey = stringPreferencesKey("shuffle_mode")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
    }
}
