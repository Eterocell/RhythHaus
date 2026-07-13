package com.eterocell.rhythhaus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidPlaybackMediaSessionTest {

    @Test
    fun mediaItemCarriesTrackMetadataForAndroidSystemControls() {
        val track = PlayableTrack(
            id = "track-1",
            title = "Night Drive",
            artist = "Rhyth Haus",
            album = "Local Sessions",
            durationMillis = 181_000L,
            source = AudioSource.Uri("content://media/external/audio/media/42"),
        )

        val mediaMetadata = buildAndroidPlaybackMediaMetadata(track)

        assertEquals("Night Drive", mediaMetadata.title.toString())
        assertEquals("Rhyth Haus", mediaMetadata.artist.toString())
        assertEquals("Local Sessions", mediaMetadata.albumTitle.toString())
    }

    @Test
    fun androidOldTokenCannotAcknowledgeSameTrackNewGeneration() {
        val tracker = Media3RequestTokenTracker()
        val first = tracker.begin(10L)
        val second = tracker.begin(11L)

        assertFalse(tracker.accepts(first, observedCurrentToken = first))
        assertTrue(tracker.accepts(second, observedCurrentToken = second))
    }

    @Test
    fun androidMediaItemCarriesUniqueGenerationRequestToken() {
        val track = PlayableTrack(
            id = "same-track",
            title = "Same Track",
            artist = "Rhyth Haus",
            album = null,
            durationMillis = 1_000L,
            source = AudioSource.Uri("content://media/external/audio/media/42"),
        )
        val tracker = Media3RequestTokenTracker()
        val first = tracker.begin(20L)
        val second = tracker.begin(21L)

        val firstMediaId = first.encode()
        val secondMediaId = second.encode()

        assertFalse(firstMediaId == secondMediaId)
        assertEquals(first, Media3RequestToken.decode(firstMediaId))
        assertEquals(second, Media3RequestToken.decode(secondMediaId))
    }

    @Test
    fun connectionFailureSettlesActiveLoadAndAllowsReplacement() = runBlocking {
        val requests = AndroidPlaybackRequestState()
        val first = requests.begin(30L)

        requests.failActive(IllegalStateException("connection failed"))

        assertFailsWith<IllegalStateException> { first.result.await() }
        val second = requests.begin(31L)
        requests.ready(second.token, durationMillis = 2_000L)
        assertEquals(LoadedPlayback(31L, 2_000L), withTimeout(1_000) { second.result.await() })
    }

    @Test
    fun matchingTokenPlayerErrorSettlesLoadAndLaterLoadCanBecomeReady() = runBlocking {
        val requests = AndroidPlaybackRequestState()
        val first = requests.begin(40L)

        assertTrue(requests.fail(first.token, first.token, IllegalArgumentException("decoder")))
        assertFailsWith<IllegalArgumentException> { first.result.await() }

        val second = requests.begin(41L)
        requests.ready(second.token, durationMillis = 3_000L)
        assertEquals(LoadedPlayback(41L, 3_000L), second.result.await())
    }

    @Test
    fun cancellingOldLoadCannotInvalidateOrCompleteReplacement() = runBlocking {
        val requests = AndroidPlaybackRequestState()
        val first = requests.begin(50L)
        val second = requests.begin(51L)

        requests.cancelIfOwner(first.token, CancellationException("superseded"))
        assertFalse(requests.ready(first.token, durationMillis = 1_000L))
        assertTrue(requests.ready(second.token, durationMillis = 2_000L))
        assertEquals(LoadedPlayback(51L, 2_000L), second.result.await())
        assertNull(requests.activeTokenForTest())
    }

    @Test
    fun releaseInvalidatesTokenAndCancelsOutstandingLoad() = runBlocking {
        val requests = AndroidPlaybackRequestState()
        val request = requests.begin(60L)

        requests.release()

        assertFailsWith<CancellationException> { request.result.await() }
        assertFalse(requests.ready(request.token, durationMillis = 1_000L))
        assertNull(requests.activeTokenForTest())
    }

    @Test
    fun controllerOperationsAreDispatchedThroughProductionExecutorSeam() {
        val executor = RecordingAndroidControllerExecutor()
        val calls = mutableListOf<String>()
        val operations = AndroidControllerOperations(executor)

        operations.dispatch { calls += "load" }
        operations.dispatch { calls += "clear" }

        assertEquals(emptyList(), calls)
        assertEquals(2, executor.pendingCount)
        executor.runAll()
        assertEquals(listOf("load", "clear"), calls)
    }

    private class RecordingAndroidControllerExecutor : AndroidControllerExecutor {
        private val pending = ArrayDeque<() -> Unit>()
        val pendingCount: Int get() = pending.size

        override fun execute(action: () -> Unit) {
            pending += action
        }

        fun runAll() {
            while (pending.isNotEmpty()) pending.removeFirst().invoke()
        }
    }
}
