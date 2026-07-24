package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class PlaybackSessionStoreJvmTest {
    @Test
    fun legacyMaximumLengthTrackRestoresAndNormalizesWithoutFailedSafe() =
        runBlocking {
            val trackId = "x".repeat(PlaybackSessionCodec.maxIdCharacters)
            withStore { store, dataStore ->
                dataStore.edit { preferences ->
                    preferences[QueueIdsKey] =
                        PlaybackSessionCodec.encodeIds(listOf(trackId))
                    preferences[CurrentIdKey] =
                        PlaybackSessionCodec.encodeIds(listOf(trackId))
                }
                val processJob = SupervisorJob()
                val controller = PlaybackController(FakePlaybackEngine())
                val coordinator =
                    PlaybackSessionCoordinator(
                        controller = controller,
                        store = store,
                        processScope =
                            CoroutineScope(Dispatchers.Default + processJob),
                    )
                try {
                    coordinator.restoreOnce(listOf(track(trackId)))

                    assertEquals(
                        PlaybackSessionPhase.Ready, coordinator.phase.value)
                    val normalized = store.read()
                    assertEquals(trackId, normalized.queue.single().trackId)
                    assertEquals(
                        normalized.queue.single().occurrenceId,
                        normalized.currentOccurrenceId)
                    assertEquals(null, dataStore.data.first()[QueueIdsKey])
                    assertEquals(null, dataStore.data.first()[CurrentIdKey])
                } finally {
                    processJob.cancelAndJoin()
                    controller.release()
                }
            }
        }

    @Test
    fun genericMaximumLengthTrackCheckpointPersistsWithoutFailedSafe() =
        runBlocking {
            val trackId = "x".repeat(PlaybackSessionCodec.maxIdCharacters)
            withStore { store, _ ->
                val processJob = SupervisorJob()
                val controller = PlaybackController(FakePlaybackEngine())
                val coordinator =
                    PlaybackSessionCoordinator(
                        controller = controller,
                        store = store,
                        processScope =
                            CoroutineScope(Dispatchers.Default + processJob),
                    )
                try {
                    coordinator.restoreOnce(emptyList())
                    controller.setQueue(
                        listOf(track(trackId)), selectedTrackId = trackId)
                    coordinator.flush()

                    assertEquals(
                        PlaybackSessionPhase.Ready, coordinator.phase.value)
                    val persisted = store.read()
                    assertEquals(trackId, persisted.queue.single().trackId)
                    assertEquals(
                        persisted.queue.single().occurrenceId,
                        persisted.currentOccurrenceId)
                    assertEquals(
                        true,
                        persisted.queue.single().occurrenceId.length <=
                            PlaybackSessionCodec.maxIdCharacters)
                } finally {
                    processJob.cancelAndJoin()
                    controller.release()
                }
            }
        }

    @Test
    fun newStoreRoundTripKeepsOrderedOccurrenceTrackPairsAndUsesOneQueueValue() =
        runBlocking {
            withStore { store, dataStore ->
                val snapshot =
                    PlaybackSessionSnapshot(
                        queue =
                            listOf(
                                SessionQueueEntry("entry-1", "track-a"),
                                SessionQueueEntry("entry-2", "track-a"),
                            ),
                        currentOccurrenceId = "entry-2",
                    )

                store.save(snapshot)

                assertEquals(snapshot, store.read())
                val preferences = dataStore.data.first()
                assertEquals(
                    PlaybackSessionCodec.encodeQueue(snapshot.queue),
                    preferences[QueueEntriesKey])
                assertEquals(null, preferences[QueueIdsKey])
                assertEquals(null, preferences[CurrentIdKey])
            }
        }

    @Test
    fun legacyStoreReadNormalizesDeterministicOccurrencesAndNextSaveRemovesLegacyKeys() =
        runBlocking {
            withStore { store, dataStore ->
                dataStore.edit { preferences ->
                    preferences[QueueIdsKey] =
                        PlaybackSessionCodec.encodeIds(
                            listOf("track-a", "track-b"))
                    preferences[CurrentIdKey] =
                        PlaybackSessionCodec.encodeIds(listOf("track-b"))
                }

                val normalized = store.read()

                assertEquals(
                    listOf(
                        SessionQueueEntry("legacy-0", "track-a"),
                        SessionQueueEntry("legacy-1", "track-b"),
                    ),
                    normalized.queue,
                )
                assertEquals("legacy-1", normalized.currentOccurrenceId)
                store.save(normalized)
                val preferences = dataStore.data.first()
                assertEquals(null, preferences[QueueIdsKey])
                assertEquals(null, preferences[CurrentIdKey])
                assertEquals(
                    PlaybackSessionCodec.encodeQueue(normalized.queue),
                    preferences[QueueEntriesKey])
            }
        }

    @Test
    fun defaultsToEmptySnapshotAndPersistsEveryField() = runBlocking {
        withStore { store, _ ->
            assertEquals(PlaybackSessionSnapshot(), store.read())

            val snapshot =
                PlaybackSessionSnapshot(
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
    fun nonEmptyQueueWithNoCurrentTrackRoundTripsAllOtherFields() =
        runBlocking {
            withStore { store, _ ->
                val snapshot =
                    PlaybackSessionSnapshot(
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
            val valid =
                PlaybackSessionSnapshot(
                    queueIds = listOf("one"), currentTrackId = "one")
            store.save(valid)

            assertFailsWith<IllegalArgumentException> {
                store.save(
                    valid.copy(
                        queue =
                            List(PlaybackSessionCodec.maxIds + 1) { index ->
                                SessionQueueEntry(
                                    "entry-$index", "track-$index")
                            },
                    ),
                )
            }

            assertEquals(valid, store.read())
        }
    }

    @Test
    fun invalidCurrentIdSaveKeepsPriorDurableSnapshot() = runBlocking {
        withStore { store, _ ->
            val valid =
                PlaybackSessionSnapshot(
                    queueIds = listOf("one"), currentTrackId = "one")
            store.save(valid)

            assertFailsWith<IllegalArgumentException> {
                store.save(valid.copy(currentOccurrenceId = "missing"))
            }

            assertEquals(valid, store.read())
        }
    }

    @Test
    fun malformedQueueFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[QueueEntriesKey] = "2:a" }
        }
    }

    @Test
    fun truncatedCurrentEncodingFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit { it[CurrentOccurrenceIdKey] = "2:a" }
        }
    }

    @Test
    fun twoIdCurrentEncodingFallsBackToFullDefaultSnapshot() = runBlocking {
        assertMalformedFieldFallsBack { dataStore ->
            dataStore.edit {
                it[CurrentOccurrenceIdKey] =
                    PlaybackSessionCodec.encodeIds(listOf("one", "two"))
            }
        }
    }

    @Test
    fun currentIdMissingFromQueueFallsBackToFullDefaultSnapshot() =
        runBlocking {
            assertMalformedFieldFallsBack { dataStore ->
                dataStore.edit {
                    it[CurrentOccurrenceIdKey] =
                        PlaybackSessionCodec.encodeIds(listOf("missing"))
                }
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
    fun corruptPreferencesFileRecoversToDefaultAndRemainsUsable() =
        runBlocking {
            val tempFile = newPreferencesFile("rhythhaus-playback-corrupt")
            tempFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x7f))
            withDataStore(
                file = tempFile,
                corruptionHandler =
                    ReplaceFileCorruptionHandler { emptyPreferences() },
            ) { dataStore ->
                val store = DataStorePlaybackSessionStore(dataStore)
                assertEquals(PlaybackSessionSnapshot(), store.read())

                val recovered =
                    PlaybackSessionSnapshot(
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
        withDataStores(themeFile, playbackFile) {
            themeDataStore,
            playbackDataStore ->
            themeDataStore.edit { preferences ->
                preferences[ThemeModeKey] = "dark"
                preferences[QueueIdsKey] =
                    PlaybackSessionCodec.encodeIds(listOf("theme-file-track"))
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
        val homeDirectory =
            Files.createTempDirectory("rhythhaus-playback-home").toFile()
        val job = SupervisorJob()
        try {
            val factory =
                JvmPlaybackSessionStoreFactory(
                    homeDirectory = homeDirectory,
                    scope = CoroutineScope(Dispatchers.IO + job),
                )

            assertEquals(
                File(
                    homeDirectory,
                    "Library/Application Support/RhythHaus/playback_session.preferences_pb"),
                factory.preferenceFile,
            )
            assertSame(factory.dataStore, factory.dataStore)
            assertEquals(
                PlaybackSessionSnapshot(), factory.createStore().read())
        } finally {
            job.cancelAndJoin()
            homeDirectory.deleteRecursively()
        }
    }

    private suspend fun assertMalformedFieldFallsBack(
        corrupt: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val valid =
            PlaybackSessionSnapshot(
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
        block:
            suspend (
                DataStorePlaybackSessionStore, DataStore<Preferences>) -> Unit,
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
            val dataStore =
                PreferenceDataStoreFactory.createWithPath(
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

    private fun testPreferencesDataStore(
        file: File,
        job: Job
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file.toOkioPath() },
        )

    private fun newPreferencesFile(prefix: String): File =
        File.createTempFile(prefix, ".preferences_pb").apply { delete() }

    private fun track(id: String): PlayableTrack =
        PlayableTrack(
            id = id,
            title = "Boundary track",
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000L,
            source = AudioSource.FilePath("/boundary.mp3"),
        )

    private companion object {
        val QueueEntriesKey = stringPreferencesKey("queue_entries")
        val CurrentOccurrenceIdKey =
            stringPreferencesKey("current_occurrence_id")
        val QueueIdsKey = stringPreferencesKey("queue_ids")
        val CurrentIdKey = stringPreferencesKey("current_id")
        val PositionKey = longPreferencesKey("position_millis")
        val RepeatModeKey = stringPreferencesKey("repeat_mode")
        val ShuffleModeKey = stringPreferencesKey("shuffle_mode")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
    }
}
