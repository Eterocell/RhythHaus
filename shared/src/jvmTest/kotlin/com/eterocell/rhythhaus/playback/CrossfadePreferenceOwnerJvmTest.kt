package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eterocell.rhythhaus.CrossfadeDuration
import java.io.IOException
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class CrossfadePreferenceOwnerJvmTest {
    @Test
    fun failedWriteRetainsDurableDurationAndRetryAcceptsOnlySuccessfulCommit() =
        runBlocking {
            withPreferences { dataStore ->
                val failing = FailingWritesDataStore(dataStore)
                val store = DataStoreCrossfadePreferenceStore(failing)
                store.setDuration(CrossfadeDuration.Seconds3)
                val applied = mutableListOf<CrossfadeDuration>()
                val owner = CrossfadePreferenceOwner(store, applied::add)
                owner.restore()
                failing.failWrites = true

                owner.select(CrossfadeDuration.Seconds8)

                assertEquals(
                    CrossfadeDuration.Seconds3,
                    owner.state.value.selectedDuration)
                assertEquals(CrossfadeDuration.Seconds3, store.readDuration())
                assertEquals(listOf(CrossfadeDuration.Seconds3), applied)
                assertFalse(owner.state.value.isBusy)
                assertTrue(owner.state.value.saveFailed)

                failing.failWrites = false
                owner.select(CrossfadeDuration.Seconds8)
                assertEquals(
                    CrossfadeDuration.Seconds8,
                    owner.state.value.selectedDuration)
                assertEquals(CrossfadeDuration.Seconds8, store.readDuration())
                assertEquals(
                    listOf(
                        CrossfadeDuration.Seconds3, CrossfadeDuration.Seconds8),
                    applied)
                assertFalse(owner.state.value.saveFailed)
            }
        }

    @Test
    fun cancellationAfterRealCommitPublishesAcceptedDurationBeforePropagating() =
        runBlocking {
            withPreferences { dataStore ->
                val committed = CompletableDeferred<Unit>()
                val allowReturn = CompletableDeferred<Unit>()
                val delayed =
                    object : DataStore<Preferences> {
                        override val data: Flow<Preferences> = dataStore.data

                        override suspend fun updateData(
                            transform: suspend (Preferences) -> Preferences
                        ): Preferences {
                            val persisted = dataStore.updateData(transform)
                            committed.complete(Unit)
                            allowReturn.await()
                            return persisted
                        }
                    }
                val applied = mutableListOf<CrossfadeDuration>()
                val store = DataStoreCrossfadePreferenceStore(delayed)
                val owner = CrossfadePreferenceOwner(store, applied::add)
                owner.restore()
                val request =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(CrossfadeDuration.Seconds5)
                    }
                try {
                    committed.await()
                    assertEquals(
                        CrossfadeDuration.Seconds5,
                        DataStoreCrossfadePreferenceStore(dataStore)
                            .readDuration(),
                    )
                    assertEquals(
                        CrossfadeDuration.Off,
                        owner.state.value.selectedDuration)
                    assertTrue(owner.state.value.isBusy)
                    request.cancel()
                    allowReturn.complete(Unit)
                    assertFailsWith<CancellationException> { request.await() }
                    request.join()

                    assertEquals(
                        CrossfadeDuration.Seconds5,
                        owner.state.value.selectedDuration)
                    assertEquals(
                        CrossfadeDuration.Seconds5,
                        store.selectedDuration.value)
                    assertEquals(
                        listOf(
                            CrossfadeDuration.Off, CrossfadeDuration.Seconds5),
                        applied)
                    assertFalse(owner.state.value.isBusy)
                    assertFalse(owner.state.value.saveFailed)
                } finally {
                    allowReturn.complete(Unit)
                    request.cancelAndJoin()
                }
            }
        }

    @Test
    fun concurrentSelectionsAreSerializedAndNeverOptimisticallyPublished() =
        runBlocking {
            withPreferences { dataStore ->
                val controlled = ControlledWritesDataStore(dataStore)
                val store = DataStoreCrossfadePreferenceStore(controlled)
                val applied = mutableListOf<CrossfadeDuration>()
                val owner = CrossfadePreferenceOwner(store, applied::add)
                owner.restore()
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(CrossfadeDuration.Seconds3)
                    }
                controlled.entered.receive()
                val second =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(CrossfadeDuration.Seconds8)
                    }
                assertEquals(
                    CrossfadeDuration.Off, owner.state.value.selectedDuration)
                assertEquals(
                    CrossfadeDuration.Off, store.selectedDuration.value)
                assertTrue(owner.state.value.isBusy)
                assertTrue(controlled.entered.tryReceive().isFailure)

                controlled.permits.send(Unit)
                first.await()
                controlled.entered.receive()
                assertEquals(
                    CrossfadeDuration.Seconds3,
                    owner.state.value.selectedDuration)
                controlled.permits.send(Unit)
                second.await()

                assertEquals(
                    CrossfadeDuration.Seconds8,
                    owner.state.value.selectedDuration)
                assertEquals(CrossfadeDuration.Seconds8, store.readDuration())
                assertEquals(
                    listOf(
                        CrossfadeDuration.Off,
                        CrossfadeDuration.Seconds3,
                        CrossfadeDuration.Seconds8),
                    applied,
                )
                assertFalse(owner.state.value.isBusy)
            }
        }

    @Test
    fun cancellationBeforeAdmissionDoesNotPersistOrApplyQueuedDuration() =
        runBlocking {
            withPreferences { dataStore ->
                val controlled = ControlledWritesDataStore(dataStore)
                val store = DataStoreCrossfadePreferenceStore(controlled)
                val applied = mutableListOf<CrossfadeDuration>()
                val owner = CrossfadePreferenceOwner(store, applied::add)
                owner.restore()
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(CrossfadeDuration.Seconds3)
                    }
                controlled.entered.receive()
                val queued =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(CrossfadeDuration.Seconds8)
                    }
                queued.cancelAndJoin()
                controlled.permits.send(Unit)
                first.await()

                assertEquals(
                    CrossfadeDuration.Seconds3,
                    owner.state.value.selectedDuration)
                assertEquals(CrossfadeDuration.Seconds3, store.readDuration())
                assertEquals(
                    listOf(CrossfadeDuration.Off, CrossfadeDuration.Seconds3),
                    applied)
                assertTrue(controlled.entered.tryReceive().isFailure)
            }
        }

    @Test
    fun selectionBeforeStartupRestoreIsNotOverwrittenByLaterRestore() =
        runBlocking {
            withPreferences { dataStore ->
                val store = DataStoreCrossfadePreferenceStore(dataStore)
                store.setDuration(CrossfadeDuration.Seconds3)
                val applied = mutableListOf<CrossfadeDuration>()
                val owner = CrossfadePreferenceOwner(store, applied::add)
                owner.select(CrossfadeDuration.Seconds8)
                owner.restore()

                assertEquals(
                    CrossfadeDuration.Seconds8,
                    owner.state.value.selectedDuration)
                assertEquals(CrossfadeDuration.Seconds8, store.readDuration())
                assertEquals(
                    listOf(
                        CrossfadeDuration.Seconds3, CrossfadeDuration.Seconds8),
                    applied)
            }
        }

    private suspend fun withPreferences(
        block: suspend (DataStore<Preferences>) -> Unit
    ) {
        val root = Files.createTempDirectory("crossfade-owner").toFile()
        val job = SupervisorJob()
        try {
            block(
                PreferenceDataStoreFactory.createWithPath(
                    scope = CoroutineScope(Dispatchers.IO + job),
                    produceFile = {
                        root.resolve("crossfade.preferences_pb").toOkioPath()
                    },
                ),
            )
        } finally {
            job.cancelAndJoin()
            root.deleteRecursively()
        }
    }

    private class FailingWritesDataStore(
        private val delegate: DataStore<Preferences>
    ) : DataStore<Preferences> {
        var failWrites = false
        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            if (failWrites) throw IOException("controlled write failure")
            return delegate.updateData(transform)
        }
    }

    private class ControlledWritesDataStore(
        private val delegate: DataStore<Preferences>
    ) : DataStore<Preferences> {
        val entered = Channel<Unit>(Channel.UNLIMITED)
        val permits = Channel<Unit>(Channel.UNLIMITED)
        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            entered.send(Unit)
            permits.receive()
            return delegate.updateData(transform)
        }
    }
}
