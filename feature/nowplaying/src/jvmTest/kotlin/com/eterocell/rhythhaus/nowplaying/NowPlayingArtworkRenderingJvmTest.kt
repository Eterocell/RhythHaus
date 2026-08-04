package com.eterocell.rhythhaus.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job

public class NowPlayingArtworkRenderingJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barEagerArtworkWinsWithoutCallingLoaderAndRendersLabel(): Unit =
        runComposeUiTest {
            var loaderCalls = 0
            setContent {
                NowPlayingBar(
                    track = track("eager", validPng()),
                    playbackState = PlaybackState(),
                    labels = barLabels(),
                    artworkLoader = {
                        loaderCalls += 1
                        validPng()
                    },
                    onPlayPause = {},
                    onExpand = {},
                    onSettings = {},
                    onSearch = {},
                    expandProgress = Animatable(0f),
                    isExpanded = false,
                )
            }
            waitForIdle()
            onNodeWithContentDescription("Album art").assertExists()
            assertEquals(0, loaderCalls)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barLazySuccessUsesCurrentTrackIdAndReplacesFallback(): Unit =
        runComposeUiTest {
            var requestedId: String? = null
            val currentTrack = mutableStateOf(track("lazy", null))
            setContent {
                NowPlayingBar(
                    track = currentTrack.value,
                    playbackState = PlaybackState(),
                    labels = barLabels(),
                    artworkLoader = { id ->
                        requestedId = id
                        validPng()
                    },
                    onPlayPause = {},
                    onExpand = {},
                    onSettings = {},
                    onSearch = {},
                    expandProgress = Animatable(0f),
                    isExpanded = false,
                )
            }
            onNodeWithTag(NowPlayingBarRootTestTag).assertExists()
            waitForIdle()
            onNodeWithContentDescription("Album art").assertExists()
            assertEquals("lazy", requestedId)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barNullAndOrdinaryFailureRemainFallbackAndResetByTrackAndLoader():
        Unit = runComposeUiTest {
        var loaderCalls = 0
        val currentTrack = mutableStateOf(track("null", null))
        val loader =
            mutableStateOf<suspend (String) -> ByteArray?>({ _ -> null })
        setContent {
            NowPlayingBar(
                track = currentTrack.value,
                playbackState = PlaybackState(),
                labels = barLabels(),
                artworkLoader = loader.value,
                onPlayPause = {},
                onExpand = {},
                onSettings = {},
                onSearch = {},
                expandProgress = Animatable(0f),
                isExpanded = false,
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        loader.value = {
            loaderCalls += 1
            throw IllegalStateException("ordinary failure")
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        assertEquals(1, loaderCalls)
        onNodeWithTag(NowPlayingBarRootTestTag).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barStaleLoaderCompletionCannotOverwriteNewTrackArtwork(): Unit =
        runComposeUiTest {
            val firstLoadStarted = CompletableDeferred<Unit>()
            val firstResult = CompletableDeferred<ByteArray?>()
            val currentTrack = mutableStateOf(track("first", null))
            val loader =
                mutableStateOf<suspend (String) -> ByteArray?>({
                    firstLoadStarted.complete(Unit)
                    firstResult.await()
                })
            setContent {
                NowPlayingBar(
                    track = currentTrack.value,
                    playbackState = PlaybackState(),
                    labels = barLabels(),
                    artworkLoader = loader.value,
                    onPlayPause = {},
                    onExpand = {},
                    onSettings = {},
                    onSearch = {},
                    expandProgress = Animatable(0f),
                    isExpanded = false,
                )
            }
            waitUntil { firstLoadStarted.isCompleted }
            currentTrack.value = track("second", validPng())
            waitForIdle()
            firstResult.complete(null)
            waitForIdle()
            onNodeWithContentDescription("Album art").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedEagerArtworkAndFallbackAreObservable(): Unit =
        runComposeUiTest {
            val controller = PlaybackController(TestPlaybackEngine())
            val currentTrack = mutableStateOf(track("expanded", validPng()))
            var loaderCalls = 0
            setContent {
                Box(Modifier.size(390.dp, 844.dp)) {
                    NowPlayingContent(
                        track = currentTrack.value,
                        playbackState = PlaybackState(),
                        playbackController = controller,
                        labels =
                            NowPlayingScreenLabels(
                                "Play", "Pause", "Album art", "Artist - Album"),
                        artworkLoader = {
                            loaderCalls += 1
                            null
                        },
                        onBack = {},
                    )
                }
            }
            waitForIdle()
            onNodeWithContentDescription("Album art").assertExists()
            assertEquals(0, loaderCalls)
            currentTrack.value = track("fallback", null)
            waitForIdle()
            onAllNodes(hasText("TIT"))[0].assertExists()
            assertEquals(1, loaderCalls)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedLazySuccessAndLoaderResetReplaceRenderedArtworkSynchronously():
        Unit = runComposeUiTest {
        val controller = PlaybackController(TestPlaybackEngine())
        val currentTrack = mutableStateOf(track("expanded-lazy", null))
        var firstLoaderCalls = 0
        val secondLoadStarted = CompletableDeferred<Unit>()
        val secondResult = CompletableDeferred<ByteArray?>()
        val loader =
            mutableStateOf<suspend (String) -> ByteArray?>({ id ->
                firstLoaderCalls += 1
                assertEquals("expanded-lazy", id)
                validPng()
            })
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = currentTrack.value,
                    playbackState = PlaybackState(),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = loader.value,
                    onBack = {},
                )
            }
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
        assertEquals(1, firstLoaderCalls)
        loader.value = {
            secondLoadStarted.complete(Unit)
            secondResult.await()
        }
        waitUntil { secondLoadStarted.isCompleted }
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        secondResult.complete(validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barTrackAndEagerArtworkChangesClearOldArtworkBeforeReplacementCompletes():
        Unit = runComposeUiTest {
        val replacementStarted = CompletableDeferred<Unit>()
        val replacement = CompletableDeferred<ByteArray?>()
        val currentTrack = mutableStateOf(track("first", validPng()))
        setContent {
            NowPlayingBar(
                track = currentTrack.value,
                playbackState = PlaybackState(),
                labels = barLabels(),
                artworkLoader = { id ->
                    assertEquals("second", id)
                    replacementStarted.complete(Unit)
                    replacement.await()
                },
                onPlayPause = {},
                onExpand = {},
                onSettings = {},
                onSearch = {},
                expandProgress = Animatable(0f),
                isExpanded = false,
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
        currentTrack.value = track("second", null)
        waitUntil { replacementStarted.isCompleted }
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        replacement.complete(validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()

        currentTrack.value = track("third", byteArrayOf(0))
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedNullFailureTrackResetAndStaleCompletionStayOnCurrentArtwork():
        Unit = runComposeUiTest {
        val controller = PlaybackController(TestPlaybackEngine())
        val firstStarted = CompletableDeferred<Unit>()
        val firstResult = CompletableDeferred<ByteArray?>()
        val currentTrack = mutableStateOf(track("first", null))
        val loader =
            mutableStateOf<suspend (String) -> ByteArray?>({ id ->
                assertEquals("first", id)
                firstStarted.complete(Unit)
                firstResult.await()
            })
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = currentTrack.value,
                    playbackState = PlaybackState(),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = loader.value,
                    onBack = {},
                )
            }
        }
        waitUntil { firstStarted.isCompleted }
        currentTrack.value = track("second", null)
        loader.value = { id ->
            assertEquals("second", id)
            throw IllegalStateException("ordinary failure")
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        firstResult.complete(validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()

        currentTrack.value = track("third", validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
        currentTrack.value = track("fourth", null)
        loader.value = { id ->
            assertEquals("fourth", id)
            null
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedEagerArtworkRemovalClearsRenderedArtworkSynchronously():
        Unit = runComposeUiTest {
        val controller = PlaybackController(TestPlaybackEngine())
        val currentTrack = mutableStateOf(track("same", validPng()))
        val replacementStarted = CompletableDeferred<Unit>()
        val replacement = CompletableDeferred<ByteArray?>()
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = currentTrack.value,
                    playbackState = PlaybackState(),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = {
                        replacementStarted.complete(Unit)
                        replacement.await()
                    },
                    onBack = {},
                )
            }
        }
        waitForIdle()
        currentTrack.value = track("same", null)
        waitUntil { replacementStarted.isCompleted }
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        replacement.complete(validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barLoaderAndSameTrackEagerChangesResetArtworkSynchronously():
        Unit = runComposeUiTest {
        val currentTrack = mutableStateOf(track("same", null))
        val replacementStarted = CompletableDeferred<Unit>()
        val replacement = CompletableDeferred<ByteArray?>()
        val loader =
            mutableStateOf<suspend (String) -> ByteArray?>({ validPng() })
        setContent {
            NowPlayingBar(
                track = currentTrack.value,
                playbackState = PlaybackState(),
                labels = barLabels(),
                artworkLoader = loader.value,
                onPlayPause = {},
                onExpand = {},
                onSettings = {},
                onSearch = {},
                expandProgress = Animatable(0f),
                isExpanded = false,
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()
        loader.value = {
            replacementStarted.complete(Unit)
            replacement.await()
        }
        waitUntil { replacementStarted.isCompleted }
        onNodeWithContentDescription("Album art").assertDoesNotExist()
        replacement.complete(validPng())
        waitForIdle()
        onNodeWithContentDescription("Album art").assertExists()

        currentTrack.value = track("same", byteArrayOf(0))
        onNodeWithContentDescription("Album art").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun barCancellationCompletesThePrivateArtworkEffectWithTheExactSentinel():
        Unit = runComposeUiTest {
        val sentinel = CancellationException("bar artwork sentinel")
        val completionObserved = AtomicBoolean(false)
        val completionCause = AtomicReference<Throwable?>(null)
        setContent {
            NowPlayingBar(
                track = track("bar-cancel", null),
                playbackState = PlaybackState(),
                labels = barLabels(),
                artworkLoader = {
                    currentCoroutineContext().job.invokeOnCompletion { cause ->
                        completionCause.set(cause)
                        completionObserved.set(true)
                    }
                    throw sentinel
                },
                onPlayPause = {},
                onExpand = {},
                onSettings = {},
                onSearch = {},
                expandProgress = Animatable(0f),
                isExpanded = false,
            )
        }
        waitUntil { completionObserved.get() }
        assertSame(sentinel, completionCause.get())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedCancellationCompletesThePrivateArtworkEffectWithTheExactSentinel():
        Unit = runComposeUiTest {
        val sentinel = CancellationException("expanded artwork sentinel")
        val completionObserved = AtomicBoolean(false)
        val completionCause = AtomicReference<Throwable?>(null)
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = track("expanded-cancel", null),
                    playbackState = PlaybackState(),
                    playbackController =
                        PlaybackController(TestPlaybackEngine()),
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = {
                        currentCoroutineContext().job.invokeOnCompletion { cause
                            ->
                            completionCause.set(cause)
                            completionObserved.set(true)
                        }
                        throw sentinel
                    },
                    onBack = {},
                )
            }
        }
        waitUntil { completionObserved.get() }
        assertSame(sentinel, completionCause.get())
    }

    private fun barLabels() =
        NowPlayingBarLabels(
            "Play",
            "Pause",
            "Search",
            "Settings",
            "Album art",
            "Artist - Album")

    private fun track(id: String, artworkBytes: ByteArray?): Track =
        Track(
            id = id,
            title = "Title $id",
            artist = "Artist",
            album = "Album",
            durationSeconds = 100,
            accent = TrackAccent(0xFF123456, 0xFF654321),
            source = AudioSource.FilePath("$id.mp3"),
            artworkBytes = artworkBytes,
        )

    private fun validPng(): ByteArray =
        byteArrayOf(
            137.toByte(),
            80,
            78,
            71,
            13,
            10,
            26,
            10,
            0,
            0,
            0,
            13,
            73,
            72,
            68,
            82,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            1,
            8,
            6,
            0,
            0,
            0,
            31,
            21,
            196.toByte(),
            137.toByte(),
            0,
            0,
            0,
            13,
            73,
            68,
            65,
            84,
            8,
            215.toByte(),
            99,
            248.toByte(),
            207.toByte(),
            192.toByte(),
            240.toByte(),
            31,
            0,
            5,
            0,
            1,
            255.toByte(),
            169.toByte(),
            38,
            33,
            164.toByte(),
            0,
            0,
            0,
            0,
            73,
            69,
            78,
            68,
            174.toByte(),
            66,
            96,
            130.toByte(),
        )

    private class TestPlaybackEngine :
        com.eterocell.rhythhaus.PlatformPlaybackEngine {
        override var listener: com.eterocell.rhythhaus.PlaybackEngineListener? =
            null

        override suspend fun loadPaused(
            track: com.eterocell.rhythhaus.PlayableTrack,
            generation: Long,
        ) =
            com.eterocell.rhythhaus.LoadedPlayback(
                generation, track.durationMillis)

        override fun clear(generation: Long): Unit = Unit

        override fun setUserTransportEnabled(enabled: Boolean): Unit = Unit

        override fun play(): Unit = Unit

        override fun pause(): Unit = Unit

        override fun stop(): Unit = Unit

        override fun seekTo(positionMillis: Long): Unit = Unit

        override fun release(): Unit = Unit
    }
}
