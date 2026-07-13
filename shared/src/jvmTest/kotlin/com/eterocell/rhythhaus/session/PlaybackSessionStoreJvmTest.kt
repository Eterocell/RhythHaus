package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
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
    fun anyMalformedPersistedFieldFallsBackToFullDefaultSnapshot() = runBlocking {
        val valid = PlaybackSessionSnapshot(
            queueIds = listOf("one", "two"),
            currentTrackId = "two",
            positionMillis = 42L,
            repeatMode = RepeatMode.RepeatOne,
            shuffleMode = ShuffleMode.On,
        )
        val corruptions: List<suspend (DataStore<Preferences>) -> Unit> = listOf(
            { dataStore -> dataStore.edit { it[QueueIdsKey] = "2:a" } },
            { dataStore -> dataStore.edit { it[CurrentIdKey] = "7:missing" } },
            { dataStore -> dataStore.edit { it[PositionKey] = -1L } },
            { dataStore -> dataStore.edit { it[RepeatModeKey] = "Forever" } },
            { dataStore -> dataStore.edit { it[ShuffleModeKey] = "Sometimes" } },
        )

        corruptions.forEach { corrupt ->
            withStore { store, dataStore ->
                store.save(valid)
                corrupt(dataStore)

                assertEquals(PlaybackSessionSnapshot(), store.read())
            }
        }
    }

    private suspend fun withStore(
        block: suspend (DataStorePlaybackSessionStore, DataStore<Preferences>) -> Unit,
    ) {
        val tempFile = File.createTempFile("rhythhaus-playback-session", ".preferences_pb").apply { delete() }
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                corruptionHandler = null,
                migrations = emptyList(),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { tempFile.toOkioPath() },
            )
            block(DataStorePlaybackSessionStore(dataStore), dataStore)
        } finally {
            tempFile.delete()
        }
    }

    private companion object {
        val QueueIdsKey = stringPreferencesKey("queue_ids")
        val CurrentIdKey = stringPreferencesKey("current_id")
        val PositionKey = longPreferencesKey("position_millis")
        val RepeatModeKey = stringPreferencesKey("repeat_mode")
        val ShuffleModeKey = stringPreferencesKey("shuffle_mode")
    }
}
