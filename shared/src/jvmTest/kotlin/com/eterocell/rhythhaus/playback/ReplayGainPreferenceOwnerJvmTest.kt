package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.ReplayGainMode
import java.io.IOException
import java.nio.file.Files
import kotlin.math.abs
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath

class ReplayGainPreferenceOwnerJvmTest {
    @Test
    fun failedWriteRetainsDurableTrackModeAndActualControllerAttenuation() =
        runBlocking {
            withPreferences { dataStore ->
                val failing = FailingWritesDataStore(dataStore)
                val store = DataStoreReplayGainPreferenceStore(failing)
                store.setSelectedMode(ReplayGainMode.Track)
                val engine = ObservedEngine()
                val controller = PlaybackController(engine)
                val applied = mutableListOf<ReplayGainMode>()
                val owner =
                    ReplayGainPreferenceOwner(store) { mode ->
                        applied += mode
                        controller.setReplayGainMode(mode)
                    }
                try {
                    controller.setQueue(listOf(taggedTrack()), "tagged")
                    owner.restore()
                    engine.awaitGain(0.5011872f)
                    failing.failWrites = true

                    owner.select(ReplayGainMode.Album)

                    assertEquals(
                        ReplayGainMode.Track, owner.state.value.selectedMode)
                    assertFalse(owner.state.value.isBusy)
                    assertTrue(owner.state.value.saveFailed)
                    assertEquals(
                        ReplayGainMode.Track, store.selectedMode.first())
                    assertEquals(listOf(ReplayGainMode.Track), applied)
                    assertEquals(0.5011872f, engine.currentGain, 0.000001f)

                    failing.failWrites = false
                    owner.select(ReplayGainMode.Album)
                    engine.awaitGain(0.25118864f)
                    assertFalse(owner.state.value.saveFailed)
                    assertEquals(
                        ReplayGainMode.Album, owner.state.value.selectedMode)
                    assertEquals(
                        ReplayGainMode.Album, store.selectedMode.first())
                } finally {
                    controller.release()
                }
            }
        }

    @Test
    fun concurrentSelectionsRemainUnpublishedUntilEachSerializedWriteCommits() =
        runBlocking {
            withPreferences { dataStore ->
                val controlled = ControlledWritesDataStore(dataStore)
                val store = DataStoreReplayGainPreferenceStore(controlled)
                val applied = mutableListOf<ReplayGainMode>()
                val owner = ReplayGainPreferenceOwner(store, applied::add)
                owner.restore()
                val first =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(ReplayGainMode.Track)
                    }
                controlled.entered.receive()
                val second =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        owner.select(ReplayGainMode.Album)
                    }
                assertEquals(ReplayGainMode.Off, owner.state.value.selectedMode)
                assertTrue(owner.state.value.isBusy)
                assertEquals(ReplayGainMode.Off, store.selectedMode.first())
                assertEquals(listOf(ReplayGainMode.Off), applied)
                assertTrue(controlled.entered.tryReceive().isFailure)

                controlled.permits.send(Unit)
                first.await()
                controlled.entered.receive()
                assertEquals(
                    ReplayGainMode.Track, owner.state.value.selectedMode)
                assertEquals(ReplayGainMode.Track, store.selectedMode.first())
                assertEquals(
                    listOf(ReplayGainMode.Off, ReplayGainMode.Track), applied)
                controlled.permits.send(Unit)
                second.await()

                assertEquals(
                    ReplayGainMode.Album, owner.state.value.selectedMode)
                assertEquals(ReplayGainMode.Album, store.selectedMode.first())
                assertEquals(
                    listOf(
                        ReplayGainMode.Off,
                        ReplayGainMode.Track,
                        ReplayGainMode.Album),
                    applied)
                assertFalse(owner.state.value.isBusy)
            }
        }

    @Test
    fun cancellationAfterRealDurableCommitPublishesAndAppliesBeforeItPropagates() =
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
                val engine = ObservedEngine()
                val controller = PlaybackController(engine)
                val owner =
                    ReplayGainPreferenceOwner(
                        DataStoreReplayGainPreferenceStore(delayed),
                        controller::setReplayGainMode)
                try {
                    controller.setQueue(listOf(taggedTrack()), "tagged")
                    owner.restore()
                    val request =
                        async(start = CoroutineStart.UNDISPATCHED) {
                            owner.select(ReplayGainMode.Album)
                        }
                    committed.await()
                    assertEquals(
                        ReplayGainMode.Album,
                        DataStoreReplayGainPreferenceStore(dataStore)
                            .selectedMode
                            .first())
                    assertEquals(
                        ReplayGainMode.Off, owner.state.value.selectedMode)
                    request.cancel()
                    allowReturn.complete(Unit)

                    assertFailsWith<CancellationException> { request.await() }
                    request.join()
                    engine.awaitGain(0.25118864f)
                    assertEquals(
                        ReplayGainMode.Album, owner.state.value.selectedMode)
                    assertFalse(owner.state.value.isBusy)
                    assertFalse(owner.state.value.saveFailed)
                    assertEquals(0.25118864f, engine.currentGain, 0.000001f)
                } finally {
                    allowReturn.complete(Unit)
                    controller.release()
                }
            }
        }

    @Test
    fun cancellingAQueuedSelectionDoesNotPersistOrPublishIt() = runBlocking {
        withPreferences { dataStore ->
            val controlled = ControlledWritesDataStore(dataStore)
            val store = DataStoreReplayGainPreferenceStore(controlled)
            val applied = mutableListOf<ReplayGainMode>()
            val owner = ReplayGainPreferenceOwner(store, applied::add)
            owner.restore()
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    owner.select(ReplayGainMode.Track)
                }
            controlled.entered.receive()
            val queued =
                async(start = CoroutineStart.UNDISPATCHED) {
                    owner.select(ReplayGainMode.Album)
                }
            queued.cancelAndJoin()
            controlled.permits.send(Unit)
            first.await()

            assertEquals(ReplayGainMode.Track, store.selectedMode.first())
            assertEquals(ReplayGainMode.Track, owner.state.value.selectedMode)
            assertEquals(
                listOf(ReplayGainMode.Off, ReplayGainMode.Track), applied)
            assertTrue(controlled.entered.tryReceive().isFailure)
            assertFalse(owner.state.value.isBusy)
        }
    }

    @Test
    fun selectionBeforeStartupRestoreCannotBeOverwrittenByALaterRestore() =
        runBlocking {
            withPreferences { dataStore ->
                val store = DataStoreReplayGainPreferenceStore(dataStore)
                store.setSelectedMode(ReplayGainMode.Track)
                val applied = mutableListOf<ReplayGainMode>()
                val owner = ReplayGainPreferenceOwner(store, applied::add)
                owner.select(ReplayGainMode.Album)
                owner.restore()

                assertEquals(
                    ReplayGainMode.Album, owner.state.value.selectedMode)
                assertEquals(ReplayGainMode.Album, store.selectedMode.first())
                assertEquals(
                    listOf(ReplayGainMode.Track, ReplayGainMode.Album), applied)
            }
        }

    private fun taggedTrack(): PlayableTrack =
        PlayableTrack(
            id = "tagged",
            title = "Tagged track",
            artist = "Artist",
            album = "Album",
            durationMillis = 30_000,
            source = AudioSource.FilePath("/tmp/replaygain-owner.mp3"),
            replayGainTrackGainDb = -6.0,
            replayGainAlbumGainDb = -12.0,
        )

    private suspend fun withPreferences(
        block: suspend (DataStore<Preferences>) -> Unit
    ) {
        val root = Files.createTempDirectory("replaygain-owner").toFile()
        val job = SupervisorJob()
        try {
            block(
                PreferenceDataStoreFactory.createWithPath(
                    scope = CoroutineScope(Dispatchers.IO + job),
                    produceFile = {
                        root.resolve("replaygain.preferences_pb").toOkioPath()
                    },
                ))
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

    private class ObservedEngine(
        private val delegate: FakePlaybackEngine = FakePlaybackEngine()
    ) : PlatformPlaybackEngine by delegate {
        private val gains = Channel<Float>(Channel.UNLIMITED)
        val currentGain: Float
            get() = delegate.playbackGain

        override fun setPlaybackGain(gain: Float) {
            delegate.setPlaybackGain(gain)
            gains.trySend(gain).getOrThrow()
        }

        suspend fun awaitGain(expected: Float) {
            withTimeout(5_000) {
                while (abs(gains.receive() - expected) > 0.000001f) {}
            }
        }
    }
}
