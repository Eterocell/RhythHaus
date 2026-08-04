package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.LoadedPlayback
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackEngineListener
import com.eterocell.rhythhaus.PlaybackError
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
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
