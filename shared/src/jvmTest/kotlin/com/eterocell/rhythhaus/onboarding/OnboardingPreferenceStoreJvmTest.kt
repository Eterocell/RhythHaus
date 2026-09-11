package com.eterocell.rhythhaus.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class OnboardingPreferenceStoreJvmTest {
    @Test
    fun missingAndOlderVersionsRequireOnboarding() = runBlocking {
        withStore { store, dataStore ->
            assertEquals(
                OnboardingEligibility.Required,
                store.eligibility.first { it != OnboardingEligibility.Loading })
            dataStore.edit {
                it[intPreferencesKey("completed_schema_version")] = 0
            }
            assertEquals(
                OnboardingEligibility.Required,
                store.eligibility.first { it != OnboardingEligibility.Loading })
        }
    }

    @Test
    fun currentAndNewerVersionsAreCompleted() = runBlocking {
        withStore { store, dataStore ->
            dataStore.edit {
                it[intPreferencesKey("completed_schema_version")] = 1
            }
            assertEquals(
                OnboardingEligibility.Completed,
                store.eligibility.first { it != OnboardingEligibility.Loading })
            dataStore.edit {
                it[intPreferencesKey("completed_schema_version")] = 99
            }
            assertEquals(
                OnboardingEligibility.Completed,
                store.eligibility.first { it != OnboardingEligibility.Loading })
        }
    }

    @Test
    fun markCompletedWritesSchemaOneIdempotently() = runBlocking {
        withStore { store, dataStore ->
            store.markCurrentVersionCompleted()
            assertEquals(
                1,
                dataStore.data
                    .first()[intPreferencesKey("completed_schema_version")])
            store.markCurrentVersionCompleted()
            assertEquals(
                1,
                dataStore.data
                    .first()[intPreferencesKey("completed_schema_version")])
        }
    }

    @Test
    fun corruptFileRecoversToRequiredAndRemainsWritable() = runBlocking {
        val root = Files.createTempDirectory("onboarding-corrupt").toFile()
        val file =
            File(
                    root,
                    "Library/Application Support/RhythHaus/onboarding.preferences_pb")
                .apply {
                    parentFile?.mkdirs()
                    writeText("not preferences")
                }
        try {
            val store =
                createJvmOnboardingPreferenceStore(
                    rootDirectory = root,
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                )
            assertEquals(
                OnboardingEligibility.Required,
                store.eligibility.first { it != OnboardingEligibility.Loading })
            store.markCurrentVersionCompleted()
            assertEquals(
                OnboardingEligibility.Completed,
                store.eligibility.first { it != OnboardingEligibility.Loading })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun readFailureEmitsRequiredInsteadOfCancellingCollector() = runBlocking {
        val failing =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow {
                    throw IllegalStateException("read")
                }

                override suspend fun updateData(
                    transform: suspend (t: Preferences) -> Preferences
                ): Preferences = error("unused")
            }
        assertEquals(
            OnboardingEligibility.Required,
            DataStoreOnboardingPreferenceStore(failing).eligibility.first {
                it != OnboardingEligibility.Loading
            })
    }

    @Test
    fun cancellationStillPropagates() {
        runBlocking {
            val cancelling =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flow {
                        throw CancellationException("cancel")
                    }

                    override suspend fun updateData(
                        transform: suspend (t: Preferences) -> Preferences
                    ): Preferences = error("unused")
                }
            assertFailsWith<CancellationException> {
                DataStoreOnboardingPreferenceStore(cancelling)
                    .eligibility
                    .first { it != OnboardingEligibility.Loading }
            }
        }
    }

    @Test
    fun onboardingStorageDoesNotReadOrOverwriteThemeOrPlaybackKeys() =
        runBlocking {
            val root = Files.createTempDirectory("onboarding-home").toFile()
            val applicationSupport =
                File(root, "Library/Application Support/RhythHaus")
            val themeFile =
                File(applicationSupport, "theme.preferences_pb").apply {
                    parentFile?.mkdirs()
                    writeBytes(byteArrayOf(1, 2, 3, 4))
                }
            val playbackFile =
                File(applicationSupport, "playback_session.preferences_pb")
                    .apply { writeBytes(byteArrayOf(9, 8, 7, 6)) }
            val themeBytes = themeFile.readBytes()
            val playbackBytes = playbackFile.readBytes()
            try {
                val store =
                    createJvmOnboardingPreferenceStore(
                        rootDirectory = root,
                        scope =
                            CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    )
                assertEquals(
                    OnboardingEligibility.Required,
                    store.eligibility.first {
                        it != OnboardingEligibility.Loading
                    })
                store.markCurrentVersionCompleted()
                assertEquals(
                    OnboardingEligibility.Completed,
                    store.eligibility.first {
                        it != OnboardingEligibility.Loading
                    })
                assertEquals(
                    themeBytes.toList(), themeFile.readBytes().toList())
                assertEquals(
                    playbackBytes.toList(), playbackFile.readBytes().toList())
                val expectedOnboardingFile =
                    File(
                        root,
                        "Library/Application Support/RhythHaus/onboarding.preferences_pb")
                assertTrue(expectedOnboardingFile.isFile)
                assertTrue(expectedOnboardingFile.length() > 0)
            } finally {
                root.deleteRecursively()
            }
        }

    @Test
    fun publicFactoryReusesOneStoreInstance() {
        assertSame(
            createOnboardingPreferenceStore(),
            createOnboardingPreferenceStore())
    }

    private suspend fun withStore(
        block:
            suspend (OnboardingPreferenceStore, DataStore<Preferences>) -> Unit
    ) {
        val file =
            File.createTempFile("onboarding", ".preferences_pb").apply {
                delete()
            }
        try {
            val dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    corruptionHandler = null,
                    migrations = emptyList(),
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    produceFile = { file.toOkioPath() })
            block(DataStoreOnboardingPreferenceStore(dataStore), dataStore)
        } finally {
            file.delete()
        }
    }
}
