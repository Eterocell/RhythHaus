package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.CrossfadeDuration
import com.eterocell.rhythhaus.CrossfadePlaybackEngine
import com.eterocell.rhythhaus.LoadedPlayback
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackEngineListener
import com.eterocell.rhythhaus.PlaybackProcessLifecycle
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ReplayGainMode
import com.eterocell.rhythhaus.di.rhythHausModule
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.PlaybackSessionStore
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class CrossfadeProcessLifecycleJvmTest {
    @Test
    fun recreatedConsumerKeepsCancelledDurableWriteAndControllerOverlapThroughOneOwner() =
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
                            val result = dataStore.updateData(transform)
                            committed.complete(Unit)
                            allowReturn.await()
                            return result
                        }
                    }
                val store = DataStoreCrossfadePreferenceStore(delayed)
                val scope = CoroutineScope(coroutineContext + SupervisorJob())
                val engine = OverlapEngine()
                val application = koinApplication {
                    modules(
                        rhythHausModule(),
                        module {
                            single<CoroutineScope> { scope }
                            single<PlatformPlaybackEngine> { engine }
                            single<ReplayGainPreferenceStore> {
                                normalization()
                            }
                            single<CrossfadePreferenceStore> { store }
                            single<PlaybackSessionStore> { EmptySessionStore }
                        },
                    )
                }
                try {
                    application.koin
                        .get<PlaybackProcessLifecycle>()
                        .restoreOnce(emptyList())
                    val controller = application.koin.get<PlaybackController>()
                    val first = application.koin.get<CrossfadePreferenceOwner>()
                    val request =
                        async(start = CoroutineStart.UNDISPATCHED) {
                            first.select(CrossfadeDuration.Seconds5)
                        }
                    withTimeout(5_000) { committed.await() }
                    val recreated =
                        application.koin.get<CrossfadePreferenceOwner>()
                    assertSame(first, recreated)
                    assertEquals(
                        1,
                        application.koin
                            .getAll<CrossfadePreferenceOwner>()
                            .size)
                    assertSame(
                        store, application.koin.get<CrossfadePreferenceStore>())
                    assertEquals(
                        CrossfadeDuration.Off,
                        recreated.state.value.selectedDuration)
                    assertTrue(recreated.state.value.isBusy)
                    val restoring =
                        async(start = CoroutineStart.UNDISPATCHED) {
                            recreated.restore()
                        }
                    request.cancel()
                    allowReturn.complete(Unit)
                    assertFailsWith<CancellationException> { request.await() }
                    request.join()
                    restoring.await()

                    assertEquals(
                        CrossfadeDuration.Seconds5,
                        recreated.state.value.selectedDuration)
                    assertEquals(
                        CrossfadeDuration.Seconds5, store.readDuration())
                    assertFalse(recreated.state.value.isBusy)
                    assertFalse(recreated.state.value.saveFailed)
                    assertControllerOverlaps(controller, engine)
                } finally {
                    allowReturn.complete(Unit)
                    application.koin.get<PlaybackController>().release()
                    scope.cancel()
                    application.close()
                }
            }
        }

    @Test
    fun delayedFileReadOccursAfterNormalizationAndBeforeSessionOrTransportAdmission() =
        runBlocking {
            withPreferences { dataStore ->
                DataStoreCrossfadePreferenceStore(dataStore)
                    .setDuration(CrossfadeDuration.Seconds5)
                val normalizationStarted = CompletableDeferred<Unit>()
                val allowNormalization = CompletableDeferred<Unit>()
                val crossfadeStarted = CompletableDeferred<Unit>()
                val allowCrossfade = CompletableDeferred<Unit>()
                val delayed =
                    object : DataStore<Preferences> {
                        override val data: Flow<Preferences> = flow {
                            crossfadeStarted.complete(Unit)
                            allowCrossfade.await()
                            emit(dataStore.data.first())
                        }

                        override suspend fun updateData(
                            transform: suspend (Preferences) -> Preferences
                        ): Preferences = dataStore.updateData(transform)
                    }
                var sessionReads = 0
                val scope = CoroutineScope(coroutineContext + SupervisorJob())
                val engine = OverlapEngine()
                val application = koinApplication {
                    modules(
                        rhythHausModule(),
                        module {
                            single<CoroutineScope> { scope }
                            single<PlatformPlaybackEngine> { engine }
                            single<ReplayGainPreferenceStore> {
                                object : ReplayGainPreferenceStore {
                                    override val selectedMode:
                                        Flow<ReplayGainMode> =
                                        flow {
                                            normalizationStarted.complete(Unit)
                                            allowNormalization.await()
                                            emit(ReplayGainMode.Track)
                                        }

                                    override suspend fun setSelectedMode(
                                        mode: ReplayGainMode
                                    ) = Unit
                                }
                            }
                            single<CrossfadePreferenceStore> {
                                DataStoreCrossfadePreferenceStore(delayed)
                            }
                            single<PlaybackSessionStore> {
                                object : PlaybackSessionStore {
                                    override suspend fun read():
                                        PlaybackSessionSnapshot {
                                        sessionReads++
                                        return PlaybackSessionSnapshot()
                                    }

                                    override suspend fun save(
                                        snapshot: PlaybackSessionSnapshot
                                    ) = Unit
                                }
                            }
                        },
                    )
                }
                try {
                    val controller = application.koin.get<PlaybackController>()
                    val startup = async {
                        application.koin
                            .get<PlaybackProcessLifecycle>()
                            .restoreOnce(emptyList())
                    }
                    withTimeout(5_000) { normalizationStarted.await() }
                    assertFalse(crossfadeStarted.isCompleted)
                    assertEquals(0, sessionReads)
                    assertTransportNotAdmitted(controller, engine)

                    allowNormalization.complete(Unit)
                    withTimeout(5_000) { crossfadeStarted.await() }
                    assertEquals(
                        ReplayGainMode.Track,
                        application.koin
                            .get<ReplayGainPreferenceOwner>()
                            .state
                            .value
                            .selectedMode)
                    assertEquals(0, sessionReads)
                    assertTrue(
                        application.koin
                            .get<CrossfadePreferenceOwner>()
                            .state
                            .value
                            .isBusy)
                    assertTransportNotAdmitted(controller, engine)

                    allowCrossfade.complete(Unit)
                    startup.await()
                    assertEquals(1, sessionReads)
                    assertTrue(engine.transportEnabled)
                    assertEquals(
                        CrossfadeDuration.Seconds5,
                        application.koin
                            .get<CrossfadePreferenceOwner>()
                            .state
                            .value
                            .selectedDuration)
                    assertControllerOverlaps(controller, engine)
                } finally {
                    allowNormalization.complete(Unit)
                    allowCrossfade.complete(Unit)
                    application.koin.get<PlaybackController>().release()
                    scope.cancel()
                    application.close()
                }
            }
        }

    private fun assertTransportNotAdmitted(
        controller: PlaybackController,
        engine: OverlapEngine
    ) {
        assertFalse(engine.transportEnabled)
        controller.setOccurrenceQueue(
            listOf(QueueOccurrence("premature-occurrence", track("premature"))),
            autoPlay = true,
        )
        controller.play()
        engine.requestRemotePlay()
        controller.setRepeatMode(RepeatMode.RepeatOne)
        assertNull(controller.state.value.currentTrack)
        assertEquals(PlaybackStatus.Idle, controller.state.value.status)
        assertEquals(
            RepeatMode.StopAfterQueue, controller.state.value.repeatMode)
        assertFalse(engine.outgoingPlaying)
        assertFalse(engine.incomingPlaying)
    }

    private suspend fun assertControllerOverlaps(
        controller: PlaybackController,
        engine: OverlapEngine
    ) {
        controller.setOccurrenceQueue(
            listOf(
                QueueOccurrence("outgoing-occurrence", track("outgoing")),
                QueueOccurrence("incoming-occurrence", track("incoming"))),
            autoPlay = true,
        )
        withTimeout(5_000) {
            controller.state.first { it.status == PlaybackStatus.Playing }
        }
        engine.advanceOutgoing(16_000)
        withTimeout(5_000) { engine.incomingStarted.await() }
        withTimeout(5_000) {
            controller.state.first { it.currentTrack?.id == "incoming" }
        }
        assertTrue(engine.outgoingPlaying)
        assertTrue(engine.incomingPlaying)
        assertEquals("incoming", controller.state.value.currentTrack?.id)
    }

    private fun track(id: String): PlayableTrack =
        PlayableTrack(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            durationMillis = 20_000,
            source = AudioSource.FilePath("/tmp/crossfade-$id.wav"),
        )

    private fun normalization(): ReplayGainPreferenceStore =
        object : ReplayGainPreferenceStore {
            override val selectedMode: Flow<ReplayGainMode> = flow {
                emit(ReplayGainMode.Off)
            }

            override suspend fun setSelectedMode(mode: ReplayGainMode) = Unit
        }

    private suspend fun withPreferences(
        block: suspend (DataStore<Preferences>) -> Unit
    ) {
        val root = Files.createTempDirectory("crossfade-process").toFile()
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

    private object EmptySessionStore : PlaybackSessionStore {
        override suspend fun read(): PlaybackSessionSnapshot =
            PlaybackSessionSnapshot()

        override suspend fun save(snapshot: PlaybackSessionSnapshot) = Unit
    }

    private class OverlapEngine : CrossfadePlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        var transportEnabled = true
        var outgoingPlaying = false
        var incomingPlaying = false
        val incomingStarted = CompletableDeferred<Unit>()
        private var generation = 0L
        private var preparedGeneration = 0L
        private var duration: Long? = null

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            this.generation = generation
            duration = track.durationMillis
            listener?.onPlaybackProgress(generation, 0, duration)
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            return LoadedPlayback(generation, duration)
        }

        override suspend fun prepareIncoming(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            preparedGeneration = generation
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun startIncoming(playbackRunId: Long) {
            generation = preparedGeneration
            incomingPlaying = true
            listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
            listener?.onPlaybackProgress(generation, 0, duration)
            incomingStarted.complete(Unit)
        }

        override fun play(playbackRunId: Long) {
            outgoingPlaying = true
            listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
        }

        fun advanceOutgoing(positionMillis: Long) {
            listener?.onPlaybackProgress(generation, positionMillis, duration)
        }

        fun requestRemotePlay() {
            listener?.onPlayRequested(generation)
        }

        override fun setUserTransportEnabled(enabled: Boolean) {
            transportEnabled = enabled
        }

        override fun setPlaybackGain(gain: Float) {
            require(gain.isFinite() && gain in 0f..1f)
        }

        override fun setCrossfadeGains(
            outgoingGain: Float,
            incomingGain: Float
        ) {
            require(outgoingGain.isFinite() && outgoingGain in 0f..1f)
            require(incomingGain.isFinite() && incomingGain in 0f..1f)
        }

        override fun finishCrossfade() {
            outgoingPlaying = false
        }

        override fun cancelPreparedIncoming() {
            preparedGeneration = 0
        }

        override fun pause() {
            outgoingPlaying = false
            incomingPlaying = false
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
        }

        override fun stop() {
            outgoingPlaying = false
            incomingPlaying = false
        }

        override fun seekTo(positionMillis: Long) {
            outgoingPlaying = false
            listener?.onPlaybackProgress(generation, positionMillis, duration)
        }

        override fun clear(generation: Long) {
            stop()
            this.generation = generation
            preparedGeneration = 0
        }

        override fun release() {
            stop()
        }
    }
}
