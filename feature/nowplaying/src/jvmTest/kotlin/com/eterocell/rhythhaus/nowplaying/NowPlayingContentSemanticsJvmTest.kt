package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.LoadedPlayback
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackEngineListener
import com.eterocell.rhythhaus.PlaybackError
import com.eterocell.rhythhaus.PlaybackFailureException
import com.eterocell.rhythhaus.PlaybackFailureKind
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals

public class NowPlayingContentSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedContentRendersErrorAndDispatchesModeAndTransportControls():
        Unit = runComposeUiTest {
        val engine = ImmediatePlaybackEngine()
        val controller = PlaybackController(engine)
        controller.setQueue(playableTracks(), selectedTrackId = "second")
        var backCallbacks = 0
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = displayTrack(),
                    playbackState =
                        PlaybackState(
                            positionMillis = 9_999L,
                            durationMillis = 1_000L,
                            status = PlaybackStatus.Error,
                            error = PlaybackError("Unavailable locally"),
                            repeatMode = RepeatMode.StopAfterQueue,
                            shuffleMode = ShuffleMode.Off,
                        ),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = { null },
                    onBack = { backCallbacks += 1 },
                )
            }
        }

        onAllNodes(hasText("Unavailable locally"))[0].assertExists()
        onAllNodes(hasText(displayTrack().title)).assertCountEquals(1)
        onAllNodes(hasText("Artist - Album")).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingContentRootTestTag))
            .assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingPreviousTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingPlayPauseTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingNextTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingShuffleTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingRepeatTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingProgressTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingTrackNumberTestTag))
            .assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingStatusTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingTitleTestTag)).assertCountEquals(1)
        onAllNodes(hasTestTag(NowPlayingSubtitleTestTag)).assertCountEquals(1)
        onNodeWithTag(NowPlayingPreviousTestTag).performClick()
        waitForIdle()
        val afterPrevious = controller.state.value.currentTrack?.id
        assertEquals(ShuffleMode.Off, controller.state.value.shuffleMode)
        assertEquals("first", afterPrevious)
        onNodeWithTag(NowPlayingPlayPauseTestTag).performClick()
        waitForIdle()
        val loadCallsAfterPlay = engine.loadCalls
        assertEquals(true, loadCallsAfterPlay > 0)
        onNodeWithTag(NowPlayingNextTestTag).performClick()
        waitForIdle()
        val afterNext = controller.state.value.currentTrack?.id
        assertEquals("second", afterNext)
        onNodeWithTag(NowPlayingShuffleTestTag).performClick()
        waitForIdle()
        onNodeWithTag(NowPlayingRepeatTestTag).performClick()
        waitForIdle()
        onNodeWithTag(NowPlayingProgressTestTag).performTouchInput {
            click(Offset(0f, center.y))
        }
        waitForIdle()
        onNodeWithTag(NowPlayingProgressTestTag).performTouchInput {
            click(Offset(width.toFloat(), center.y))
        }
        waitForIdle()
        onNodeWithTag(NowPlayingContentRootTestTag).performTouchInput {
            swipe(Offset(1f, center.y), Offset(120f, center.y))
        }
        waitForIdle()

        assertEquals(ShuffleMode.On, controller.state.value.shuffleMode)
        assertEquals(
            RepeatMode.RepeatPlaylist, controller.state.value.repeatMode)
        assertEquals("first", afterPrevious)
        assertEquals("second", afterNext)
        assertEquals("second", controller.state.value.currentTrack?.id)
        assertEquals(true, loadCallsAfterPlay > 0)
        assertEquals(listOf(0L, 1_000L), engine.seekPositions)
        assertEquals(1, backCallbacks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedContentUsesCompactAndSplitProductionLayoutBranches():
        Unit = runComposeUiTest {
        val controller = PlaybackController(ImmediatePlaybackEngine())
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = displayTrack(),
                    playbackState = PlaybackState(),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = { null },
                    onBack = {},
                )
            }
        }
        onNodeWithTag(NowPlayingCompactLayoutTestTag).assertExists()
        onNodeWithTag(NowPlayingSplitLayoutTestTag).assertDoesNotExist()
        setContent {
            Box(Modifier.size(1_200.dp, 800.dp)) {
                NowPlayingContent(
                    track = displayTrack(),
                    playbackState = PlaybackState(),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = { null },
                    onBack = {},
                )
            }
        }
        onNodeWithTag(NowPlayingSplitLayoutTestTag).assertExists()
        onNodeWithTag(NowPlayingCompactLayoutTestTag).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun expandedContentRendersNormalStatus(): Unit = runComposeUiTest {
        val controller = PlaybackController(ImmediatePlaybackEngine())
        setContent {
            Box(Modifier.size(390.dp, 844.dp)) {
                NowPlayingContent(
                    track = displayTrack(),
                    playbackState =
                        PlaybackState(status = PlaybackStatus.Playing),
                    playbackController = controller,
                    labels =
                        NowPlayingScreenLabels(
                            "Play", "Pause", "Album art", "Artist - Album"),
                    artworkLoader = { null },
                    onBack = {},
                )
            }
        }
        onAllNodes(hasTestTag(NowPlayingStatusTestTag)).assertCountEquals(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun errorStateRendersExactlyOneFailureRecoveryActionPerCommandWithLocalizedSummary():
        Unit = withEnglishLocale {
        runComposeUiTest {
            val engine = RecoveryPlaybackEngine()
            val controller = PlaybackController(engine)
            engine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.MissingFile,
                )
            controller.setQueue(playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        controller.state.collectAsState().value,
                    controller = controller,
                    width = 1200.dp,
                    height = 800.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onAllNodes(hasTestTag(NowPlayingRetryFailureTestTag))
                .assertCountEquals(1)
            onAllNodes(hasTestTag(NowPlayingSkipFailureTestTag))
                .assertCountEquals(1)
            onAllNodes(hasTestTag(NowPlayingRemoveFailureTestTag))
                .assertCountEquals(1)
            onAllNodes(hasText("Unavailable locally")).assertCountEquals(1)
            onAllNodes(hasText("This media file is missing")).assertCountEquals(1)
            onAllNodes(hasText("Retry")).assertCountEquals(1)
            onAllNodes(hasText("Skip")).assertCountEquals(1)
            onAllNodes(hasText("Remove from queue")).assertCountEquals(1)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun retryFailureRecoveryReloadsOnlyTheFailedOccurrenceAndClearsTheSection():
        Unit = withEnglishLocale {
        runComposeUiTest {
            val engine = RecoveryPlaybackEngine()
            val controller = PlaybackController(engine)
            engine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.DecoderFailure,
                )
            controller.setQueue(playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        controller.state.collectAsState().value,
                    controller = controller,
                    width = 1200.dp,
                    height = 800.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertExists()
            engine.failLoadWith = null
            onNodeWithTag(NowPlayingRetryFailureTestTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                engine.events() ==
                    listOf(
                        EngineEvent.Load("second"),
                        EngineEvent.Play,
                    )
            }
            waitForIdle()
            assertEquals(
                listOf(EngineEvent.Load("second"), EngineEvent.Play),
                engine.events(),
            )
            assertEquals("second", controller.state.value.currentTrack?.id)
            assertEquals(null, controller.state.value.error)
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun skipFailureRecoveryLoadsTheEffectiveSuccessorWithoutWrapping():
        Unit = withEnglishLocale {
        runComposeUiTest {
            val engine = RecoveryPlaybackEngine()
            val controller = PlaybackController(engine)
            engine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.UnsupportedFormat,
                )
            controller.setQueue(playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        controller.state.collectAsState().value,
                    controller = controller,
                    width = 1200.dp,
                    height = 800.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingSkipFailureTestTag).assertExists()
            engine.failLoadWith = null
            onNodeWithTag(NowPlayingSkipFailureTestTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                engine.events() ==
                    listOf(
                        EngineEvent.Load("third"),
                        EngineEvent.Play,
                    )
            }
            waitForIdle()
            assertEquals("third", controller.state.value.currentTrack?.id)
            assertEquals(
                listOf("first", "second", "third"),
                controller.state.value.queue.map { it.track.id },
            )
            assertEquals(null, controller.state.value.error)
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun removeFailureRecoveryRemovesOnlyTheFailedOccurrenceAndAutoplaysItsSuccessor():
        Unit = withEnglishLocale {
        runComposeUiTest {
            val engine = RecoveryPlaybackEngine()
            val controller = PlaybackController(engine)
            engine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.AccessLost,
                )
            controller.setQueue(playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        controller.state.collectAsState().value,
                    controller = controller,
                    width = 1200.dp,
                    height = 800.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertExists()
            engine.failLoadWith = null
            onNodeWithTag(NowPlayingRemoveFailureTestTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                engine.events() ==
                    listOf(
                        EngineEvent.Load("third"),
                        EngineEvent.Play,
                    )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.currentTrack?.id == "third"
            }
            waitForIdle()
            assertEquals(
                listOf("first", "third"),
                controller.state.value.queue.map { it.track.id },
            )
            assertEquals(null, controller.state.value.error)
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun finalOccurrenceFailureRecoveryRemovalClearsEngineAndKeepsRemainingQueueIdle():
        Unit = withEnglishLocale {
        runComposeUiTest {
            val engine = RecoveryPlaybackEngine()
            val controller = PlaybackController(engine)
            engine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.MissingFile,
                )
            controller.setQueue(playableTracks(), selectedTrackId = "third")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        controller.state.collectAsState().value,
                    controller = controller,
                    width = 1200.dp,
                    height = 800.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertExists()
            engine.failLoadWith = null
            onNodeWithTag(NowPlayingRemoveFailureTestTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                controller.state.value.status == PlaybackStatus.Idle &&
                    controller.state.value.currentTrack == null
            }
            waitForIdle()
            assertEquals(
                listOf("first", "second"),
                controller.state.value.queue.map { it.track.id },
            )
            assertEquals(emptyList(), engine.events())
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
            onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun pausedLoadingAndOccurrenceLessErrorStatesExposeNoFailureRecoveryNodes():
        Unit = runComposeUiTest {
        val controller = PlaybackController(ImmediatePlaybackEngine())
        val occurrence =
            QueueOccurrence("current", playableTracks()[0])
        val failedWithoutOccurrence =
            PlaybackState(
                status = PlaybackStatus.Error,
                error = PlaybackError("Unavailable locally"),
            )
        setContent {
            mountedRecoveryNowPlaying(
                track = displayTrack(),
                playbackState =
                    PlaybackState(
                        status = PlaybackStatus.Paused,
                        queue = listOf(occurrence),
                        currentOccurrenceId = occurrence.id,
                    ),
                controller = controller,
            )
        }
        waitForIdle()
        onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        setContent {
            mountedRecoveryNowPlaying(
                track = displayTrack(),
                playbackState =
                    PlaybackState(
                        status = PlaybackStatus.Loading,
                        queue = listOf(occurrence),
                        currentOccurrenceId = occurrence.id,
                    ),
                controller = controller,
            )
        }
        waitForIdle()
        onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
        setContent {
            mountedRecoveryNowPlaying(
                track = displayTrack(),
                playbackState = failedWithoutOccurrence,
                controller = controller,
            )
        }
        waitForIdle()
        onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingSkipFailureTestTag).assertDoesNotExist()
        onNodeWithTag(NowPlayingRemoveFailureTestTag).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun compactErrorStateKeepsRecoveryProgressAndTransportControlsReachable():
        Unit = withEnglishLocale {
        runComposeUiTest {
            // Production compact viewport (narrow 390dp phone, bounded 700dp
            // height): the error-only recovery section plus the existing
            // scrubber and transport row overflow a fixed non-scrollable pane,
            // so every control must stay reachable through real pointer paths.
            // Each scenario uses a fresh active-error controller.

            // Scenario 1: recovery actions are reachable and clickable; Retry
            // actually dispatches through a pointer click.
            val retryEngine = RecoveryPlaybackEngine()
            val retryController = PlaybackController(retryEngine)
            retryEngine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.MissingFile,
                )
            retryController.setQueue(playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        retryController.state.collectAsState().value,
                    controller = retryController,
                    height = 700.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                retryController.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingRetryFailureTestTag)
                .performScrollTo()
                .assertHasClickAction()
            onNodeWithTag(NowPlayingSkipFailureTestTag)
                .performScrollTo()
                .assertHasClickAction()
            onNodeWithTag(NowPlayingRemoveFailureTestTag)
                .performScrollTo()
                .assertHasClickAction()
            retryEngine.failLoadWith = null
            onNodeWithTag(NowPlayingRetryFailureTestTag)
                .performScrollTo()
                .performTouchInput { click(center) }
            waitUntil(timeoutMillis = 5_000) {
                retryEngine.events() ==
                    listOf(
                        EngineEvent.Load("second"),
                        EngineEvent.Play,
                    )
            }
            waitForIdle()
            assertEquals(
                listOf(EngineEvent.Load("second"), EngineEvent.Play),
                retryEngine.events(),
            )
            assertEquals("second", retryController.state.value.currentTrack?.id)
            onNodeWithTag(NowPlayingRetryFailureTestTag).assertDoesNotExist()

            // Scenario 2: the next-track transport control below the recovery
            // section stays reachable in the bounded compact viewport and
            // dispatches through a real pointer click while the error is
            // active.
            val nextEngine = RecoveryPlaybackEngine()
            val nextController = PlaybackController(nextEngine)
            nextEngine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.DecoderFailure,
                )
            nextController.setQueue(
                playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        nextController.state.collectAsState().value,
                    controller = nextController,
                    height = 700.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                nextController.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            nextEngine.failLoadWith = null
            onNodeWithTag(NowPlayingNextTestTag)
                .performScrollTo()
                .performTouchInput { click(center) }
            waitUntil(timeoutMillis = 5_000) {
                nextEngine.events() ==
                    listOf(
                        EngineEvent.Load("third"),
                        EngineEvent.Play,
                    )
            }
            waitForIdle()
            assertEquals(
                listOf(EngineEvent.Load("third"), EngineEvent.Play),
                nextEngine.events(),
            )
            assertEquals("third", nextController.state.value.currentTrack?.id)

            // Scenario 3: the progress scrubber stays reachable in the same
            // bounded compact viewport while the error is active and seeks
            // through a real pointer gesture.
            val scrubEngine = RecoveryPlaybackEngine()
            val scrubController = PlaybackController(scrubEngine)
            scrubEngine.failLoadWith =
                PlaybackError(
                    "Unavailable locally",
                    kind = PlaybackFailureKind.AccessLost,
                )
            scrubController.setQueue(
                playableTracks(), selectedTrackId = "second")
            setContent {
                mountedRecoveryNowPlaying(
                    track = displayTrack(),
                    playbackState =
                        scrubController.state.collectAsState().value,
                    controller = scrubController,
                    height = 700.dp,
                )
            }
            waitUntil(timeoutMillis = 5_000) {
                scrubController.state.value.status == PlaybackStatus.Error
            }
            waitForIdle()
            onNodeWithTag(NowPlayingProgressTestTag)
                .performScrollTo()
                .performTouchInput {
                    click(Offset(width.toFloat(), center.y))
                }
            waitUntil(timeoutMillis = 5_000) {
                scrubEngine.seekPositions.isNotEmpty()
            }
            assertEquals(1_000L, scrubEngine.seekPositions.last())
        }
    }

    private fun displayTrack(): Track =
        Track(
            id = "first",
            title =
                "A deliberately long title that remains bounded in the content pane",
            artist = "Artist",
            album = "Album",
            durationSeconds = 1,
            accent = TrackAccent(0xFF123456, 0xFF654321),
            source = AudioSource.FilePath("first.mp3"),
            trackNumber = 3,
        )

    private fun playableTracks(): List<PlayableTrack> =
        listOf("first", "second", "third").map { id ->
            PlayableTrack(
                id = id,
                title = id,
                artist = "Artist",
                album = "Album",
                durationMillis = 1_000L,
                source = AudioSource.FilePath("$id.mp3"),
            )
        }

    private class ImmediatePlaybackEngine : PlatformPlaybackEngine {
        var loadCalls: Int = 0
        val seekPositions: MutableList<Long> = mutableListOf()
        override var listener: PlaybackEngineListener? = null

        override suspend fun loadPaused(
            track: PlayableTrack,
            generation: Long
        ): LoadedPlayback {
            loadCalls += 1
            return LoadedPlayback(generation, track.durationMillis)
        }

        override fun clear(generation: Long): Unit = Unit

        override fun setUserTransportEnabled(enabled: Boolean): Unit = Unit

        override fun play(): Unit = Unit

        override fun pause(): Unit = Unit

        override fun stop(): Unit = Unit

        override fun seekTo(positionMillis: Long): Unit {
            seekPositions += positionMillis
        }

        override fun release(): Unit = Unit
    }
}

private sealed interface EngineEvent {
    data class Load(val trackId: String) : EngineEvent

    data object Play : EngineEvent
}

/**
 * Engine double that records successful loads and plays, and fails its next
 * load with a structured [PlaybackError] while [failLoadWith] is set. A failed
 * load never records an event, so recovery transitions are observed exactly.
 */
private class RecoveryPlaybackEngine : PlatformPlaybackEngine {
    var failLoadWith: PlaybackError? = null
    private val recordedEvents = CopyOnWriteArrayList<EngineEvent>()
    val seekPositions = CopyOnWriteArrayList<Long>()
    override var listener: PlaybackEngineListener? = null

    fun events(): List<EngineEvent> = recordedEvents

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        failLoadWith?.let { throw PlaybackFailureException(it) }
        recordedEvents += EngineEvent.Load(track.id)
        return LoadedPlayback(generation, track.durationMillis)
    }

    override fun clear(generation: Long): Unit = Unit

    override fun setUserTransportEnabled(enabled: Boolean): Unit = Unit

    override fun play(): Unit {
        recordedEvents += EngineEvent.Play
    }

    override fun pause(): Unit = Unit

    override fun stop(): Unit = Unit

    override fun seekTo(positionMillis: Long): Unit {
        seekPositions += positionMillis
    }

    override fun release(): Unit = Unit
}

private val recoveryLabels =
    NowPlayingScreenLabels(
        play = "Play",
        pause = "Pause",
        albumArtwork = "Album art",
        currentTrackArtistAlbum = "Artist - Album",
    )

@androidx.compose.runtime.Composable
private fun mountedRecoveryNowPlaying(
    track: Track,
    playbackState: PlaybackState,
    controller: PlaybackController,
    width: Dp = 390.dp,
    height: Dp = 844.dp,
): Unit {
    Box(Modifier.size(width, height)) {
        NowPlayingContent(
            track = track,
            playbackState = playbackState,
            playbackController = controller,
            labels = recoveryLabels,
            artworkLoader = { null },
            onBack = {},
        )
    }
}

private fun withEnglishLocale(block: () -> Unit): Unit {
    val original = Locale.getDefault()
    try {
        Locale.setDefault(Locale.ENGLISH)
        block()
    } finally {
        Locale.setDefault(original)
    }
}
