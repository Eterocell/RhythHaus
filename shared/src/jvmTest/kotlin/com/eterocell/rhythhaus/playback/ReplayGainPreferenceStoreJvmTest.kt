package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.ReplayGainMode
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class ReplayGainPreferenceStoreJvmTest {
    @Test
    fun selectedModeSurvivesClosingAndReopeningRealFile() = runBlocking {
        val root = Files.createTempDirectory("replaygain-preferences").toFile()
        val file = root.resolve("replaygain.preferences_pb")
        try {
            val firstJob = SupervisorJob()
            try {
                val dataStore = preferences(file, firstJob)
                val first = DataStoreReplayGainPreferenceStore(dataStore)
                assertEquals(ReplayGainMode.Off, first.selectedMode.first())
                first.setSelectedMode(ReplayGainMode.Album)
                assertEquals(
                    "album",
                    dataStore.data
                        .first()[stringPreferencesKey("replaygain_mode")])
            } finally {
                firstJob.cancelAndJoin()
            }
            val secondJob = SupervisorJob()
            try {
                val reopened =
                    DataStoreReplayGainPreferenceStore(
                        preferences(file, secondJob))
                assertEquals(
                    ReplayGainMode.Album, reopened.selectedMode.first())
            } finally {
                secondJob.cancelAndJoin()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun readFailureDefaultsOffAndFailedWriteDoesNotPublishRequestedMode() =
        runBlocking {
            val dataStore =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flow {
                        throw IOException("unreadable")
                    }

                    override suspend fun updateData(
                        transform: suspend (Preferences) -> Preferences
                    ): Preferences = throw IOException("not writable")
                }
            val store = DataStoreReplayGainPreferenceStore(dataStore)
            assertEquals(ReplayGainMode.Off, store.selectedMode.first())
            assertFailsWith<IOException> {
                store.setSelectedMode(ReplayGainMode.Track)
            }
            assertEquals(ReplayGainMode.Off, store.selectedMode.first())
        }

    @Test
    fun invalidSerializedModeAndWrongPreferenceTypeDefaultOff() = runBlocking {
        withPreferences { dataStore, _ ->
            val store = DataStoreReplayGainPreferenceStore(dataStore)
            dataStore.edit {
                it[stringPreferencesKey("replaygain_mode")] = "loud"
            }
            assertEquals(ReplayGainMode.Off, store.selectedMode.first())
            dataStore.edit { it[intPreferencesKey("replaygain_mode")] = 1 }
            assertEquals(ReplayGainMode.Off, store.selectedMode.first())
            store.setSelectedMode(ReplayGainMode.Track)
            assertEquals(ReplayGainMode.Track, store.selectedMode.first())
        }
    }

    @Test
    fun corruptFileDefaultsOffAndAllowsDurableRecovery() = runBlocking {
        val root = Files.createTempDirectory("replaygain-corrupt").toFile()
        val file = root.resolve("replaygain.preferences_pb")
        file.writeBytes(byteArrayOf(0x0a, 0x7f))
        try {
            val firstJob = SupervisorJob()
            try {
                val store =
                    DataStoreReplayGainPreferenceStore(
                        preferences(file, firstJob))
                assertEquals(ReplayGainMode.Off, store.selectedMode.first())
                store.setSelectedMode(ReplayGainMode.Track)
            } finally {
                firstJob.cancelAndJoin()
            }
            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    DataStoreReplayGainPreferenceStore(
                        preferences(file, reopenedJob))
                assertEquals(
                    ReplayGainMode.Track, reopened.selectedMode.first())
            } finally {
                reopenedJob.cancelAndJoin()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun unreadableRealFileDefaultsOffWithoutPretendingWritesSucceeded() =
        runBlocking {
            val root =
                Files.createTempDirectory("replaygain-unreadable").toFile()
            val file =
                root.resolve("replaygain.preferences_pb").apply { mkdir() }
            val job = SupervisorJob()
            try {
                val store =
                    DataStoreReplayGainPreferenceStore(preferences(file, job))
                assertEquals(ReplayGainMode.Off, store.selectedMode.first())
                assertFailsWith<IOException> {
                    store.setSelectedMode(ReplayGainMode.Album)
                }
                assertEquals(ReplayGainMode.Off, store.selectedMode.first())
            } finally {
                job.cancelAndJoin()
                root.deleteRecursively()
            }
        }

    @Test
    fun readCancellationIsNotConvertedToAnAcceptedOffMode() = runBlocking {
        val dataStore =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow {
                    throw CancellationException("cancelled read")
                }

                override suspend fun updateData(
                    transform: suspend (Preferences) -> Preferences
                ): Preferences = error("No write expected")
            }
        assertFailsWith<CancellationException> {
            DataStoreReplayGainPreferenceStore(dataStore).selectedMode.first()
        }
        Unit
    }

    private fun preferences(
        file: File,
        job: kotlinx.coroutines.Job
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file.toOkioPath() },
        )

    private suspend fun withPreferences(
        block: suspend (DataStore<Preferences>, File) -> Unit
    ) {
        val root = Files.createTempDirectory("replaygain-preferences").toFile()
        val job = SupervisorJob()
        try {
            block(
                preferences(root.resolve("replaygain.preferences_pb"), job),
                root)
        } finally {
            job.cancelAndJoin()
            root.deleteRecursively()
        }
    }
}
