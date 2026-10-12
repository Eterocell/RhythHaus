package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eterocell.rhythhaus.CrossfadeDuration
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class CrossfadePreferenceStoreJvmTest {
    @Test
    fun acceptedDurationSurvivesClosingAndReopeningRealFile() = runBlocking {
        val root = Files.createTempDirectory("crossfade-preferences").toFile()
        val file = root.resolve("crossfade.preferences_pb")
        try {
            val firstJob = SupervisorJob()
            try {
                val dataStore = preferences(file, firstJob)
                val first = DataStoreCrossfadePreferenceStore(dataStore)
                assertEquals(CrossfadeDuration.Off, first.readDuration())
                first.setDuration(CrossfadeDuration.Seconds5)
                assertEquals(
                    CrossfadeDuration.Seconds5, first.selectedDuration.value)
                assertEquals(
                    "5",
                    dataStore.data
                        .first()[stringPreferencesKey("crossfade_duration")])
            } finally {
                firstJob.cancelAndJoin()
            }
            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    DataStoreCrossfadePreferenceStore(
                        preferences(file, reopenedJob))
                assertEquals(
                    CrossfadeDuration.Seconds5, reopened.readDuration())
                assertEquals(
                    CrossfadeDuration.Seconds5, reopened.selectedDuration.value)
            } finally {
                reopenedJob.cancelAndJoin()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun corruptFileDefaultsOffAndAllowsDurableRecovery() = runBlocking {
        val root = Files.createTempDirectory("crossfade-corrupt").toFile()
        val file = root.resolve("crossfade.preferences_pb")
        file.writeBytes(byteArrayOf(0x0a, 0x7f))
        try {
            val firstJob = SupervisorJob()
            try {
                val store =
                    DataStoreCrossfadePreferenceStore(
                        preferences(file, firstJob))
                assertEquals(CrossfadeDuration.Off, store.readDuration())
                store.setDuration(CrossfadeDuration.Seconds8)
            } finally {
                firstJob.cancelAndJoin()
            }
            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    DataStoreCrossfadePreferenceStore(
                        preferences(file, reopenedJob))
                assertEquals(
                    CrossfadeDuration.Seconds8, reopened.readDuration())
            } finally {
                reopenedJob.cancelAndJoin()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun invalidSerializedDurationAndWrongPreferenceTypeDefaultOff() =
        runBlocking {
            val root = Files.createTempDirectory("crossfade-invalid").toFile()
            val job = SupervisorJob()
            try {
                val dataStore =
                    preferences(root.resolve("crossfade.preferences_pb"), job)
                val store = DataStoreCrossfadePreferenceStore(dataStore)
                dataStore.edit {
                    it[stringPreferencesKey("crossfade_duration")] = "4"
                }
                assertEquals(CrossfadeDuration.Off, store.readDuration())
                dataStore.edit {
                    it[intPreferencesKey("crossfade_duration")] = 5
                }
                assertEquals(CrossfadeDuration.Off, store.readDuration())
                store.setDuration(CrossfadeDuration.Seconds3)
                assertEquals(CrossfadeDuration.Seconds3, store.readDuration())
            } finally {
                job.cancelAndJoin()
                root.deleteRecursively()
            }
        }

    @Test
    fun unreadableRealFileDefaultsOffWithoutAcceptingFailedWrites() =
        runBlocking {
            val root =
                Files.createTempDirectory("crossfade-unreadable").toFile()
            val file =
                root.resolve("crossfade.preferences_pb").apply { mkdir() }
            val job = SupervisorJob()
            try {
                val store =
                    DataStoreCrossfadePreferenceStore(preferences(file, job))
                assertEquals(CrossfadeDuration.Off, store.readDuration())
                assertFailsWith<IOException> {
                    store.setDuration(CrossfadeDuration.Seconds5)
                }
                assertEquals(
                    CrossfadeDuration.Off, store.selectedDuration.value)
            } finally {
                job.cancelAndJoin()
                root.deleteRecursively()
            }
        }

    @Test
    fun readCancellationDoesNotBecomeAnAcceptedOffDuration() = runBlocking {
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
            DataStoreCrossfadePreferenceStore(dataStore).readDuration()
        }
        Unit
    }

    private fun preferences(file: File, job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file.toOkioPath() },
        )
}
