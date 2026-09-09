package com.eterocell.rhythhaus

import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class AndroidPlaybackMediaSessionTest {

    @Test
    fun mediaItemCarriesTrackMetadataForAndroidSystemControls() {
        val artwork = byteArrayOf(1, 2, 3)
        val track =
            PlayableTrack(
                id = "track-1",
                title = "Night Drive",
                artist = "Rhyth Haus",
                album = "Local Sessions",
                durationMillis = 181_000L,
                source =
                    AudioSource.Uri("content://media/external/audio/media/42"),
                artworkBytes = artwork,
            )

        val mediaMetadata = buildAndroidPlaybackMediaMetadata(track)

        assertEquals("Night Drive", mediaMetadata.title.toString())
        assertEquals("Rhyth Haus", mediaMetadata.artist.toString())
        assertEquals("Local Sessions", mediaMetadata.albumTitle.toString())
        assertTrue(artwork.contentEquals(mediaMetadata.artworkData))
        assertEquals(
            MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            mediaMetadata.artworkDataType,
        )
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
        val track =
            PlayableTrack(
                id = "same-track",
                title = "Same Track",
                artist = "Rhyth Haus",
                album = null,
                durationMillis = 1_000L,
                source =
                    AudioSource.Uri("content://media/external/audio/media/42"),
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
        assertEquals(
            LoadedPlayback(31L, 2_000L),
            withTimeout(1_000) { second.result.await() })
    }

    @Test
    fun matchingTokenPlayerErrorSettlesLoadAndLaterLoadCanBecomeReady() =
        runBlocking {
            val requests = AndroidPlaybackRequestState()
            val first = requests.begin(40L)

            assertTrue(
                requests.fail(
                    first.token,
                    first.token,
                    IllegalArgumentException("decoder")))
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
    fun readySourceRetainsObservableGenerationForLaterCallbacks() =
        runBlocking {
            val requests = AndroidPlaybackRequestState()
            val request = requests.begin(70L)

            assertTrue(requests.ready(request.token, durationMillis = 1_234L))
            assertEquals(LoadedPlayback(70L, 1_234L), request.result.await())

            assertEquals(70L, requests.observableGeneration(request.token))
            assertEquals(70L, requests.observableGeneration(request.token))
            assertEquals(70L, requests.observableGeneration(request.token))
            assertEquals(70L, requests.observableGeneration(request.token))
        }

    @Test
    fun replacementInvalidatesReadySourceAndRejectsLateCallbacks() {
        val requests = AndroidPlaybackRequestState()
        val first = requests.begin(80L)
        assertTrue(requests.ready(first.token, durationMillis = 2_000L))

        val second = requests.begin(81L)

        assertNull(requests.observableGeneration(first.token))
        assertEquals(81L, requests.observableGeneration(second.token))
    }

    @Test
    fun progressCaptureCannotBeRelabelledAcrossSourceReplacement() {
        val requests = AndroidPlaybackRequestState()
        val first = requests.begin(90L)
        assertTrue(requests.ready(first.token, durationMillis = 3_000L))
        val captured = requireNotNull(requests.captureObservable(first.token))

        val second = requests.begin(91L)

        assertFalse(requests.revalidate(captured, first.token))
        assertFalse(requests.revalidate(captured, second.token))
        assertEquals(90L, captured.generation)
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

    @Test
    fun androidFileNotFoundMapsToMissingFile() {
        val mapped =
            androidPlaybackError(
                errorCode = PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                message = "source file missing",
            )

        assertEquals(PlaybackFailureKind.MissingFile, mapped.kind)
        assertEquals("Android could not play this audio file.", mapped.message)
        assertEquals("source file missing", mapped.cause)
    }

    @Test
    fun androidPermissionFailureMapsToAccessLost() {
        val errorCode = PlaybackException.ERROR_CODE_IO_NO_PERMISSION

        assertEquals(
            PlaybackFailureKind.AccessLost, androidFailureKind(errorCode))
        assertEquals(
            PlaybackFailureKind.AccessLost,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "no permission to read source",
                )
                .kind,
        )
    }

    @Test
    fun androidUnsupportedContainerMapsToUnsupportedFormat() {
        val errorCode =
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED

        assertEquals(
            PlaybackFailureKind.UnsupportedFormat,
            androidFailureKind(errorCode))
    }

    @Test
    fun androidDecoderFailureMapsToDecoderFailure() {
        val errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED
        val decoderErrorCodes =
            listOf(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                PlaybackException
                    .ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            )

        decoderErrorCodes.forEach { code ->
            assertEquals(
                PlaybackFailureKind.DecoderFailure,
                androidFailureKind(code),
            )
        }
        assertEquals(
            PlaybackFailureKind.DecoderFailure,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "decoder could not decode samples",
                )
                .kind,
        )
    }

    @Test
    fun androidOpaquePlayerErrorRemainsUnknown() {
        val errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED

        assertEquals(PlaybackFailureKind.Unknown, androidFailureKind(errorCode))
        assertEquals(
            PlaybackFailureKind.Unknown,
            androidPlaybackError(
                    errorCode = errorCode,
                    message = "unidentified failure",
                )
                .kind,
        )
    }

    @Test
    fun androidAbsentLocalSourceFileMapsToMissingFile() {
        val absent = Files.createTempFile("rhythhaus-absent-android", ".wav")
        Files.deleteIfExists(absent)
        try {
            val mapped =
                androidPlaybackError(
                    errorCode = PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    message = "opaque io failure",
                    sourceFile = absent.toFile(),
                )

            assertEquals(PlaybackFailureKind.MissingFile, mapped.kind)
        } finally {
            Files.deleteIfExists(absent)
        }
    }

    @Test
    fun androidReadySourceKeepsLocalFileEvidenceForOpaquePlayerError() {
        val absent = Files.createTempFile("rhythhaus-evidence-android", ".wav")
        Files.deleteIfExists(absent)
        try {
            val requests = AndroidPlaybackRequestState()
            val request = requests.begin(95L, absent.toFile())
            assertTrue(requests.ready(request.token, durationMillis = 2_000L))

            val evidence = requests.localFileFor(request.token)
            assertNotNull(evidence)
            assertEquals(
                PlaybackFailureKind.MissingFile,
                androidPlaybackError(
                        errorCode = PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                        message = "opaque io failure while playing",
                        sourceFile = evidence,
                    )
                    .kind,
            )
        } finally {
            Files.deleteIfExists(absent)
        }
    }

    @Test
    fun releaseIsSerializedAndSuppressesQueuedAndPostReleaseControllerWork() {
        val executor = RecordingAndroidControllerExecutor()
        val operations = AndroidControllerOperations(executor)
        val lifecycle = AndroidControllerLifecycle(operations)
        val calls = mutableListOf<String>()
        val releaseQueued = CountDownLatch(1)

        lifecycle.dispatchControllerWork { calls += "queued-before-release" }
        val releaseThread =
            thread(start = true) {
                lifecycle.release {
                    calls += "release-controller"
                    calls += "release-future"
                }
                releaseQueued.countDown()
            }
        assertTrue(releaseQueued.await(1, TimeUnit.SECONDS))
        releaseThread.join()
        lifecycle.dispatchControllerWork { calls += "queued-after-release" }

        assertEquals(emptyList(), calls)
        executor.runAll()

        assertEquals(listOf("release-controller", "release-future"), calls)
        assertTrue(lifecycle.isDisposedForTest())
        assertEquals(0, executor.pendingCount)
    }

    private class RecordingAndroidControllerExecutor :
        AndroidControllerExecutor {
        private val pending = ArrayDeque<() -> Unit>()
        val pendingCount: Int
            get() = pending.size

        override fun execute(action: () -> Unit) {
            pending += action
        }

        fun runAll() {
            while (pending.isNotEmpty()) pending.removeFirst().invoke()
        }
    }
}
