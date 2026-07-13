package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackEngineListener
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.LoadedPlayback
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryPlaybackSelectionTest {
    @Test
    fun currentSelectionRestartsWithoutReplacingExistingQueue() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val existingQueue = tracks("existing-1", "existing-2", "existing-3")
        val visibleQueue = tracks("visible-1", "existing-2", "visible-3")
        controller.setQueue(existingQueue, selectedTrackId = "existing-2")
        engine.awaitLoad()
        engine.listener?.onPlaybackProgress(engine.generation, 750L, existingQueue[1].durationMillis)
        engine.clearEvents()

        selectLibraryTrackForPlayback(controller, visibleQueue, selectedTrackId = "existing-2")
        engine.awaitEvents(2)

        assertEquals(existingQueue, controller.state.value.queue)
        assertEquals("existing-2", controller.state.value.currentTrack?.id)
        assertEquals(0L, controller.state.value.positionMillis)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
    }

    @Test
    fun differentSelectionReplacesQueueWithVisibleOrderAndAutoplays() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(tracks("existing-1"), selectedTrackId = "existing-1")
        engine.awaitLoad()
        engine.clearEvents()
        val visibleQueue = tracks("visible-3", "visible-1", "visible-2")

        selectLibraryTrackForPlayback(controller, visibleQueue, selectedTrackId = "visible-1")
        engine.awaitEvents(2)

        assertEquals(visibleQueue, controller.state.value.queue)
        assertEquals("visible-1", controller.state.value.currentTrack?.id)
        assertEquals(PlaybackStatus.Playing, controller.state.value.status)
    }

    @Test
    fun differentSelectionPreservesRepeatAndShuffleModes() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(tracks("existing-1"), selectedTrackId = "existing-1")
        engine.awaitLoad()
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)
        engine.clearEvents()

        selectLibraryTrackForPlayback(controller, tracks("visible-1", "visible-2"), "visible-2")
        engine.awaitEvents(2)

        assertEquals(RepeatMode.RepeatOne, controller.state.value.repeatMode)
        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
    }

    @Test
    fun invalidSelectionDoesNotFallBackToFirstVisibleTrack() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val existingQueue = tracks("existing-1", "existing-2")
        controller.setQueue(existingQueue, selectedTrackId = "existing-2")
        engine.awaitLoad()
        engine.clearEvents()
        val initialState = controller.state.value

        selectLibraryTrackForPlayback(controller, tracks("visible-1", "visible-2"), "missing")

        assertEquals(initialState, controller.state.value)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    @Test
    fun emptyVisibleQueueIsNoOp() = runBlocking {
        val engine = RecordingPlaybackEngine()
        val controller = PlaybackController(engine)
        val existingQueue = tracks("existing-1")
        controller.setQueue(existingQueue, selectedTrackId = "existing-1")
        engine.awaitLoad()
        engine.clearEvents()
        val initialState = controller.state.value

        selectLibraryTrackForPlayback(controller, emptyList(), "existing-1")

        assertEquals(initialState, controller.state.value)
        assertEquals(emptyList(), engine.eventSnapshot())
    }

    private fun tracks(vararg ids: String): List<PlayableTrack> = ids.mapIndexed { index, id ->
        PlayableTrack(
            id = id,
            title = "Track $id",
            artist = "Test Artist",
            album = "Test Album",
            durationMillis = (index + 1) * 1_000L,
            source = AudioSource.FilePath("/tmp/$id.mp3"),
        )
    }

    private sealed interface EngineEvent {
        data class Load(val trackId: String) : EngineEvent
        data class Seek(val positionMillis: Long) : EngineEvent
        data object Play : EngineEvent
    }

    private class RecordingPlaybackEngine : PlatformPlaybackEngine {
        override var listener: PlaybackEngineListener? = null
        private val events = Channel<EngineEvent>(Channel.UNLIMITED)
        private var loadSignal = CompletableDeferred<Unit>()
        var generation: Long = 0L
            private set

        override suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback {
            this.generation = generation
            record(EngineEvent.Load(track.id))
            listener?.onPlaybackProgress(generation, 0L, track.durationMillis)
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            loadSignal.complete(Unit)
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun clear(generation: Long) {
            this.generation = generation
        }

        override fun setUserTransportEnabled(enabled: Boolean) = Unit

        suspend fun awaitLoad() = loadSignal.await()

        fun clearEvents() {
            while (events.tryReceive().isSuccess) {}
            loadSignal = CompletableDeferred()
        }

        fun eventSnapshot(): List<EngineEvent> = buildList {
            while (true) add(events.tryReceive().getOrNull() ?: break)
        }

        suspend fun awaitEvents(count: Int): List<EngineEvent> = withTimeout(5_000) {
            List(count) { events.receive() }
        }

        override fun play() {
            record(EngineEvent.Play)
            listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
        }

        override fun pause() = Unit

        override fun stop() = Unit

        override fun seekTo(positionMillis: Long) {
            record(EngineEvent.Seek(positionMillis))
            listener?.onPlaybackProgress(generation, positionMillis, null)
        }

        override fun release() = Unit

        private fun record(event: EngineEvent) {
            check(events.trySend(event).isSuccess)
        }
    }
}
