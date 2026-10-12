package com.eterocell.rhythhaus.playback

import com.eterocell.rhythhaus.CrossfadeDuration
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackProcessLifecycle
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ReplayGainMode
import com.eterocell.rhythhaus.di.rhythHausModule
import com.eterocell.rhythhaus.session.PlaybackSessionSnapshot
import com.eterocell.rhythhaus.session.PlaybackSessionStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ReplayGainProcessLifecycleJvmTest {
    @Test
    fun recreatedConsumerObservesAdmittedWriteThroughOneAuthority() =
        runBlocking {
            val preferences = DelayedPreferences()
            val scope = CoroutineScope(coroutineContext + SupervisorJob())
            val application = koinApplication {
                modules(
                    rhythHausModule(),
                    module {
                        single<CoroutineScope> { scope }
                        single<PlatformPlaybackEngine> { FakePlaybackEngine() }
                        single<ReplayGainPreferenceStore> { preferences }
                        single<CrossfadePreferenceStore> {
                            InMemoryCrossfadePreferences()
                        }
                    })
            }
            try {
                val first = application.koin.get<ReplayGainPreferenceOwner>()
                first.restore()
                val writing =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        first.select(ReplayGainMode.Album)
                    }
                preferences.writeStarted.await()
                val recreated =
                    application.koin.get<ReplayGainPreferenceOwner>()
                val restoring = async { recreated.restore() }
                yield()
                preferences.allowWrite.complete(Unit)
                writing.await()
                restoring.await()
                assertEquals(
                    ReplayGainMode.Album, recreated.state.value.selectedMode)
                assertEquals(ReplayGainMode.Album, preferences.mode.value)
            } finally {
                application.koin.get<PlaybackController>().release()
                scope.cancel()
                application.close()
            }
        }

    @Test
    fun startupWaitsForNormalizationBeforeSessionAndTransportAdmission() =
        runBlocking {
            val preferences = DelayedPreferences(blockRead = true)
            val scope = CoroutineScope(coroutineContext + SupervisorJob())
            var sessionReads = 0
            val application = koinApplication {
                modules(
                    rhythHausModule(),
                    module {
                        single<CoroutineScope> { scope }
                        single<PlatformPlaybackEngine> { FakePlaybackEngine() }
                        single<ReplayGainPreferenceStore> { preferences }
                        single<CrossfadePreferenceStore> {
                            InMemoryCrossfadePreferences()
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
                    })
            }
            try {
                val controller = application.koin.get<PlaybackController>()
                val startup = async {
                    application.koin
                        .get<PlaybackProcessLifecycle>()
                        .restoreOnce(emptyList())
                }
                yield()
                yield()
                assertEquals(0, sessionReads)
                controller.setRepeatMode(RepeatMode.RepeatOne)
                assertEquals(
                    RepeatMode.StopAfterQueue,
                    controller.state.value.repeatMode)
                preferences.allowRead.complete(Unit)
                startup.await()
                assertEquals(
                    ReplayGainMode.Track,
                    application.koin
                        .get<ReplayGainPreferenceOwner>()
                        .state
                        .value
                        .selectedMode)
                assertEquals(1, sessionReads)
                controller.setRepeatMode(RepeatMode.RepeatOne)
                assertEquals(
                    RepeatMode.RepeatOne, controller.state.value.repeatMode)
                assertTrue(startup.isCompleted)
            } finally {
                preferences.allowRead.complete(Unit)
                application.koin.get<PlaybackController>().release()
                scope.cancel()
                application.close()
            }
        }

    private class InMemoryCrossfadePreferences : CrossfadePreferenceStore {
        override val selectedDuration = MutableStateFlow(CrossfadeDuration.Off)

        override suspend fun readDuration(): CrossfadeDuration =
            selectedDuration.value

        override suspend fun setDuration(duration: CrossfadeDuration) {
            selectedDuration.value = duration
        }
    }

    private class DelayedPreferences(private val blockRead: Boolean = false) :
        ReplayGainPreferenceStore {
        val mode = MutableStateFlow(ReplayGainMode.Track)
        val allowRead = CompletableDeferred<Unit>()
        val writeStarted = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        override val selectedMode: Flow<ReplayGainMode> = flow {
            if (blockRead) allowRead.await()
            emit(mode.value)
        }

        override suspend fun setSelectedMode(mode: ReplayGainMode) {
            writeStarted.complete(Unit)
            allowWrite.await()
            this.mode.value = mode
        }
    }
}
