package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class AutomaticResumePreferenceStoreJvmTest {
    @Test
    fun defaultAndAcceptedPolicySurviveClosingAndReopeningFile() = runBlocking {
        withPreferenceFile { file ->
            val firstJob = SupervisorJob()
            try {
                val first =
                    DataStoreAutomaticResumePreferenceStore(
                        preferences(file, firstJob))
                assertEquals(
                    AutomaticResumePolicy.FollowSystemRecommendation,
                    first.readPolicy())
                first.setPolicy(AutomaticResumePolicy.Never)
                assertEquals(
                    AutomaticResumePolicy.Never, first.selectedPolicy.value)
            } finally {
                firstJob.cancelAndJoin()
            }

            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    DataStoreAutomaticResumePreferenceStore(
                        preferences(file, reopenedJob))
                assertEquals(AutomaticResumePolicy.Never, reopened.readPolicy())
            } finally {
                reopenedJob.cancelAndJoin()
            }
        }
    }

    @Test
    fun malformedAndWronglyTypedPolicyFallBackToSystemRecommendation() =
        runBlocking {
            withPreferenceFile { file ->
                val job = SupervisorJob()
                try {
                    val dataStore = preferences(file, job)
                    val store =
                        DataStoreAutomaticResumePreferenceStore(dataStore)
                    dataStore.edit {
                        it[stringPreferencesKey("automatic_resume_policy")] =
                            "sometimes"
                    }
                    assertEquals(
                        AutomaticResumePolicy.FollowSystemRecommendation,
                        store.readPolicy())
                    dataStore.edit {
                        it[intPreferencesKey("automatic_resume_policy")] = 1
                    }
                    assertEquals(
                        AutomaticResumePolicy.FollowSystemRecommendation,
                        store.readPolicy())
                } finally {
                    job.cancelAndJoin()
                }
            }
        }

    @Test
    fun ownerDoesNotPublishFailedWriteAndAcceptsDurableRetry() = runBlocking {
        val writes = mutableListOf<AutomaticResumePolicy>()
        var failWrites = false
        val store =
            object : AutomaticResumePreferenceStore {
                private var policy =
                    AutomaticResumePolicy.FollowSystemRecommendation
                override val selectedPolicy =
                    kotlinx.coroutines.flow.MutableStateFlow(policy)

                override suspend fun readPolicy(): AutomaticResumePolicy =
                    policy

                override suspend fun setPolicy(policy: AutomaticResumePolicy) {
                    if (failWrites) error("disk unavailable")
                    this.policy = policy
                    selectedPolicy.value = policy
                    writes += policy
                }
            }
        val applied = mutableListOf<AutomaticResumePolicy>()
        val owner = AutomaticResumePreferenceOwner(store, applied::add)
        owner.restore()
        failWrites = true
        owner.select(AutomaticResumePolicy.Never)
        assertEquals(
            AutomaticResumePolicy.FollowSystemRecommendation,
            owner.state.value.selectedPolicy)
        assertEquals(
            listOf(AutomaticResumePolicy.FollowSystemRecommendation), applied)
        assertTrue(owner.state.value.saveFailed)
        assertFalse(owner.state.value.isBusy)

        failWrites = false
        owner.select(AutomaticResumePolicy.Never)
        assertEquals(
            AutomaticResumePolicy.Never, owner.state.value.selectedPolicy)
        assertEquals(
            listOf(
                AutomaticResumePolicy.FollowSystemRecommendation,
                AutomaticResumePolicy.Never),
            applied)
        assertEquals(listOf(AutomaticResumePolicy.Never), writes)
        assertFalse(owner.state.value.saveFailed)
    }

    @Test
    fun cancellationAfterDurableCommitPublishesBeforePropagating() =
        runBlocking {
            withPreferenceFile { file ->
                val dataStore = preferences(file, SupervisorJob())
                val committed = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val delayed =
                    object : DataStore<Preferences> {
                        override val data: Flow<Preferences> = dataStore.data

                        override suspend fun updateData(
                            transform: suspend (Preferences) -> Preferences
                        ): Preferences {
                            val value = dataStore.updateData(transform)
                            committed.complete(Unit)
                            release.await()
                            return value
                        }
                    }
                val store = DataStoreAutomaticResumePreferenceStore(delayed)
                val applied = mutableListOf<AutomaticResumePolicy>()
                val owner = AutomaticResumePreferenceOwner(store, applied::add)
                try {
                    owner.restore()
                    val selection =
                        async(start = CoroutineStart.UNDISPATCHED) {
                            owner.select(AutomaticResumePolicy.Never)
                        }
                    committed.await()
                    assertEquals(
                        AutomaticResumePolicy.FollowSystemRecommendation,
                        owner.state.value.selectedPolicy)
                    selection.cancel()
                    release.complete(Unit)
                    assertFailsWith<CancellationException> { selection.await() }
                    assertEquals(
                        AutomaticResumePolicy.Never,
                        owner.state.value.selectedPolicy)
                    assertEquals(
                        AutomaticResumePolicy.Never, store.readPolicy())
                } finally {
                    release.complete(Unit)
                }
            }
        }

    private suspend fun withPreferenceFile(block: suspend (File) -> Unit) {
        val directory = Files.createTempDirectory("automatic-resume").toFile()
        try {
            block(directory.resolve("automatic_resume.preferences_pb"))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun preferences(file: File, job: kotlinx.coroutines.Job) =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler =
                ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file.toOkioPath() },
        )
}
