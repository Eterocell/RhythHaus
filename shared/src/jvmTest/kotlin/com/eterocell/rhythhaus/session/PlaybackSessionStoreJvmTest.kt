package com.eterocell.rhythhaus.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.sqldelight.db.SqlDriver
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.RhythHausDatabase
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class PlaybackSessionStoreJvmTest {
    @Test
    fun savesTenThousandOneDuplicateTrackOccurrencesBeyondLegacyLimits() =
        runBlocking {
            // One past PlaybackSessionCodec.maxIds, all referencing one track.
            val queue =
                (0 until PlaybackSessionCodec.maxIds + 1).map { index ->
                    SessionQueueEntry(
                        occurrenceId = "occurrence-$index",
                        trackId = "track-1",
                    )
                }
            val snapshot =
                PlaybackSessionSnapshot(
                    queue = queue,
                    currentOccurrenceId = "occurrence-10000",
                    positionMillis = 42L,
                    repeatMode = RepeatMode.RepeatPlaylist,
                    shuffleMode = ShuffleMode.On,
                )
            withDatabase { database, _ ->
                val store = newStore(database)
                store.save(snapshot)

                val restored = store.read()
                assertEquals(queue, restored.queue)
                assertEquals("occurrence-10000", restored.currentOccurrenceId)
                assertEquals("track-1", restored.currentTrackId)
                assertEquals(42L, restored.positionMillis)
                assertEquals(RepeatMode.RepeatPlaylist, restored.repeatMode)
                assertEquals(ShuffleMode.On, restored.shuffleMode)
            }
        }

    @Test
    fun restoresOccurrenceNearQueueEndAfterRestartThenPersistsSmallQueue() =
        runBlocking {
            val queue =
                (0 until PlaybackSessionCodec.maxIds + 1).map { index ->
                    SessionQueueEntry("occurrence-$index", "track-1")
                }
            val legacySource = FakeLegacySource()
            withDatabase { database, reopen ->
                newStore(database, legacySource)
                    .save(
                        PlaybackSessionSnapshot(
                            queue = queue,
                            currentOccurrenceId = "occurrence-10000",
                            positionMillis = 9_000L,
                        ),
                    )

                val reopened = newStore(reopen(), legacySource)
                val restored = reopened.read()
                assertEquals(queue, restored.queue)
                assertEquals("occurrence-10000", restored.currentOccurrenceId)
                assertEquals(9_000L, restored.positionMillis)
                val controller = PlaybackController(FakePlaybackEngine())
                try {
                    controller.restoreSession(
                        restored,
                        listOf(
                            PlayableTrack(
                                "track-1",
                                "Repeated",
                                "Artist",
                                null,
                                15_000L,
                                AudioSource.FilePath("track.flac"))))
                    assertEquals(
                        PlaybackStatus.Paused, controller.state.value.status)
                    assertEquals(restored, controller.sessionSnapshot())
                } finally {
                    controller.release()
                }
                // The import marker survives the restart.
                assertEquals(1, legacySource.reads)

                val smallQueue =
                    listOf(
                        SessionQueueEntry("small-0", "track-2"),
                        SessionQueueEntry("small-1", "track-3"),
                    )
                reopened.save(
                    PlaybackSessionSnapshot(
                        queue = smallQueue,
                        currentOccurrenceId = "small-1",
                        positionMillis = 5L,
                    ),
                )

                val afterSmallSave = newStore(reopen(), legacySource).read()
                assertEquals(smallQueue, afterSmallSave.queue)
                assertEquals("small-1", afterSmallSave.currentOccurrenceId)
                assertEquals("track-3", afterSmallSave.currentTrackId)
                assertEquals(5L, afterSmallSave.positionMillis)
            }
        }

    @Test
    fun importsLegacySessionExactlyOnceAcrossReadsAndRestarts() = runBlocking {
        val legacy =
            PlaybackSessionSnapshot(
                queue =
                    listOf(
                        SessionQueueEntry("legacy-0", "track-a"),
                        SessionQueueEntry("legacy-1", "track-b"),
                    ),
                currentOccurrenceId = "legacy-1",
                positionMillis = 500L,
                repeatMode = RepeatMode.RepeatOne,
                shuffleMode = ShuffleMode.On,
            )
        val legacySource = FakeLegacySource(legacy)
        withDatabase { database, reopen ->
            val store = newStore(database, legacySource)
            assertEquals(legacy, store.read())
            assertEquals(1, legacySource.reads)
            assertEquals(legacy, store.read())
            assertEquals(1, legacySource.reads)

            assertEquals(legacy, newStore(reopen(), legacySource).read())
            assertEquals(1, legacySource.reads)
        }
    }

    @Test
    fun importsEmptyLegacySessionOnceAndNeverReimports() = runBlocking {
        val legacySource = FakeLegacySource()
        withDatabase { database, reopen ->
            val store = newStore(database, legacySource)
            assertEquals(PlaybackSessionSnapshot(), store.read())
            assertEquals(1, legacySource.reads)

            val saved =
                PlaybackSessionSnapshot(
                    queue =
                        listOf(SessionQueueEntry("occurrence-0", "track-1")),
                    currentOccurrenceId = "occurrence-0",
                )
            store.save(saved)
            assertEquals(saved, store.read())
            assertEquals(1, legacySource.reads)

            assertEquals(saved, newStore(reopen(), legacySource).read())
            assertEquals(1, legacySource.reads)
        }
    }

    @Test
    fun retriesImportWhenInterruptedBeforeMarkerCommit() = runBlocking {
        val legacy =
            PlaybackSessionSnapshot(
                queue = listOf(SessionQueueEntry("legacy-0", "track-a")),
                currentOccurrenceId = "legacy-0",
                positionMillis = 77L,
            )
        val legacySource = FakeLegacySource(legacy)
        lateinit var driver: SqlDriver
        withDatabase(onOpen = { driver = it }) { database, reopen ->
            driver.execute(
                null,
                """
                CREATE TRIGGER interrupt_import BEFORE UPDATE OF legacyImportCompleted ON playback_session_meta
                WHEN NEW.legacyImportCompleted = 1
                BEGIN SELECT RAISE(ABORT, 'interrupt import'); END
                """
                    .trimIndent(),
                0)
            kotlin.test.assertFails { newStore(database, legacySource).read() }
            assertEquals(
                emptyList(),
                database.playbackSessionQueries
                    .selectQueueEntries()
                    .executeAsList())
            assertEquals(
                null,
                database.playbackSessionQueries
                    .selectSessionMeta()
                    .executeAsOneOrNull())
            driver.execute(null, "DROP TRIGGER interrupt_import", 0)
            val store = newStore(reopen(), legacySource)
            assertEquals(legacy, store.read())
            assertEquals(legacy, store.read())
            assertEquals(2, legacySource.reads)
        }
    }

    @Test
    fun savePositionUpdatesOnlyMatchingCurrentOccurrence() = runBlocking {
        val queue =
            listOf(
                SessionQueueEntry("occurrence-0", "track-1"),
                SessionQueueEntry("occurrence-1", "track-2"),
            )
        val snapshot =
            PlaybackSessionSnapshot(
                queue = queue,
                currentOccurrenceId = "occurrence-0",
                positionMillis = 10L,
                repeatMode = RepeatMode.RepeatOne,
                shuffleMode = ShuffleMode.On,
            )
        withDatabase { database, _ ->
            val store = newStore(database)
            store.save(snapshot)

            store.savePosition("occurrence-0", 1_234L)
            assertEquals(snapshot.copy(positionMillis = 1_234L), store.read())

            // A stale occurrence never moves the persisted position.
            store.savePosition("occurrence-1", 9_999L)
            assertEquals(snapshot.copy(positionMillis = 1_234L), store.read())

            store.savePosition("occurrence-0", -5L)
            assertEquals(snapshot.copy(positionMillis = 0L), store.read())

            // A structural save wins over a position update for the
            // occurrence that is no longer current.
            store.save(snapshot.copy(currentOccurrenceId = "occurrence-1"))
            store.savePosition("occurrence-0", 123L)
            val afterStructuralSave = store.read()
            assertEquals(
                "occurrence-1", afterStructuralSave.currentOccurrenceId)
            assertEquals(queue, afterStructuralSave.queue)
            assertEquals(
                snapshot.positionMillis, afterStructuralSave.positionMillis)
        }
    }

    @Test
    fun positionAndModeChangesDoNotReplaceOccurrenceRows() = runBlocking {
        lateinit var driver: SqlDriver
        withDatabase(onOpen = { driver = it }) { database, _ ->
            val store = newStore(database)
            val saved =
                PlaybackSessionSnapshot(
                    queue = listOf(SessionQueueEntry("one", "track")),
                    currentOccurrenceId = "one",
                )
            store.save(saved)
            driver.execute(
                null,
                """
                CREATE TRIGGER reject_queue_rewrite BEFORE DELETE ON playback_session_queue
                BEGIN SELECT RAISE(ABORT, 'unchanged queue rewritten'); END
                """
                    .trimIndent(),
                0)
            store.save(saved.copy(repeatMode = RepeatMode.RepeatOne))
            for (second in 1L..60L) store.savePosition("one", second * 1_000L)
            assertEquals(
                saved.copy(
                    repeatMode = RepeatMode.RepeatOne,
                    positionMillis = 60_000L),
                store.read())
        }
    }

    @Test
    fun rejectedStructuralSaveLeavesPersistedSessionUntouched() = runBlocking {
        val saved =
            PlaybackSessionSnapshot(
                queue =
                    listOf(
                        SessionQueueEntry("occurrence-0", "track-1"),
                        SessionQueueEntry("occurrence-1", "track-2"),
                    ),
                currentOccurrenceId = "occurrence-1",
                positionMillis = 8L,
            )
        withDatabase { database, _ ->
            val store = newStore(database)
            store.save(saved)

            assertFailsWith<IllegalArgumentException> {
                store.save(
                    saved.copy(
                        queue =
                            listOf(
                                SessionQueueEntry("duplicate", "track-1"),
                                SessionQueueEntry("duplicate", "track-2"),
                            ),
                    ))
            }
            assertFailsWith<IllegalArgumentException> {
                store.save(saved.copy(currentOccurrenceId = "not-queued"))
            }

            assertEquals(saved, store.read())
            assertEquals(
                2,
                database.playbackSessionQueries
                    .selectQueueEntries()
                    .executeAsList()
                    .size,
            )
        }
    }

    @Test
    fun importsOccurrencePreferencesWithoutCollapsingRepeatedTrackIds() =
        runBlocking {
            val entries =
                listOf(
                    SessionQueueEntry("first", "track"),
                    SessionQueueEntry("second", "track"))
            withDataStore(
                newPreferencesFile("rhythhaus-session-occurrences")) {
                    preferences ->
                    preferences.edit {
                        it[stringPreferencesKey("queue_entries")] =
                            PlaybackSessionCodec.encodeQueue(entries)
                        it[stringPreferencesKey("current_occurrence_id")] =
                            PlaybackSessionCodec.encodeIds(listOf("second"))
                        it[PositionKey] = 1234L
                    }
                    withDatabase { database, _ ->
                        val imported =
                            SqlDelightPlaybackSessionStore(
                                    database,
                                    DataStorePlaybackSessionLegacySource(
                                        preferences))
                                .read()
                        assertEquals(
                            PlaybackSessionSnapshot(
                                queue = entries,
                                currentOccurrenceId = "second",
                                positionMillis = 1234L),
                            imported)
                    }
                }
        }

    @Test
    fun importsLegacyPreferencesSessionWithNormalizedOccurrences() =
        runBlocking {
            val file = newPreferencesFile("rhythhaus-playback-legacy")
            withDataStore(file) { dataStore ->
                dataStore.edit { preferences ->
                    preferences[QueueIdsKey] =
                        PlaybackSessionCodec.encodeIds(
                            listOf("track-a", "track-b", "track-c"))
                    preferences[CurrentIdKey] =
                        PlaybackSessionCodec.encodeIds(listOf("track-b"))
                    preferences[PositionKey] = 321L
                    preferences[RepeatModeKey] = RepeatMode.RepeatOne.name
                    preferences[ShuffleModeKey] = ShuffleMode.On.name
                }

                withDatabase { database, _ ->
                    val store =
                        SqlDelightPlaybackSessionStore(
                            database = database,
                            legacySource =
                                DataStorePlaybackSessionLegacySource(dataStore),
                        )

                    val imported = store.read()
                    assertEquals(
                        listOf(
                            SessionQueueEntry("legacy-0", "track-a"),
                            SessionQueueEntry("legacy-1", "track-b"),
                            SessionQueueEntry("legacy-2", "track-c"),
                        ),
                        imported.queue,
                    )
                    assertEquals("legacy-1", imported.currentOccurrenceId)
                    assertEquals("track-b", imported.currentTrackId)
                    assertEquals(321L, imported.positionMillis)
                    assertEquals(RepeatMode.RepeatOne, imported.repeatMode)
                    assertEquals(ShuffleMode.On, imported.shuffleMode)
                }
            }
        }

    private class FakeLegacySource(
        private val snapshot: PlaybackSessionSnapshot =
            PlaybackSessionSnapshot(),
    ) : PlaybackSessionLegacySnapshotSource {
        var reads: Int = 0
            private set

        override suspend fun read(): PlaybackSessionSnapshot {
            reads++
            return snapshot
        }
    }

    private fun newStore(
        database: RhythHausDatabase,
        legacySource: PlaybackSessionLegacySnapshotSource = FakeLegacySource(),
    ): PlaybackSessionStore =
        SqlDelightPlaybackSessionStore(
            database = database,
            legacySource = legacySource,
        )

    private suspend fun withDatabase(
        onOpen: (SqlDriver) -> Unit = {},
        block: suspend (RhythHausDatabase, () -> RhythHausDatabase) -> Unit,
    ) {
        val file =
            Files.createTempFile("rhythhaus-playback-session", ".db")
                .toFile()
                .also(File::delete)
        var libraryDatabase = LibraryDatabase(file)
        try {
            onOpen(libraryDatabase.driver)
            block(libraryDatabase.database) {
                libraryDatabase.driver.close()
                libraryDatabase = LibraryDatabase(file)
                libraryDatabase.database
            }
        } finally {
            libraryDatabase.driver.close()
            file.delete()
        }
    }

    private suspend fun withDataStore(
        file: File,
        block: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val job: Job = SupervisorJob()
        try {
            val dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    corruptionHandler = null,
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

    private fun newPreferencesFile(prefix: String): File =
        File.createTempFile(prefix, ".preferences_pb").apply { delete() }

    private companion object {
        val QueueIdsKey = stringPreferencesKey("queue_ids")
        val CurrentIdKey = stringPreferencesKey("current_id")
        val PositionKey = longPreferencesKey("position_millis")
        val RepeatModeKey = stringPreferencesKey("repeat_mode")
        val ShuffleModeKey = stringPreferencesKey("shuffle_mode")
    }
}
