package com.eterocell.rhythhaus.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.theme.LightHausPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class NowPlayingBarSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun unmeasuredNowPlayingBarExposesNoActions(): Unit =
        runComposeUiTest {
            setContent { bar(interactive = false) }
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun staleMeasuredNowPlayingBarExposesNoActionsAndDispatchesNoPointerOrGestureCallbacks():
        Unit = runComposeUiTest {
        var playPauseCallbacks = 0
        var expandCallbacks = 0
        setContent {
            bar(
                interactive = false,
                onPlayPause = { playPauseCallbacks += 1 },
                onExpand = { expandCallbacks += 1 },
            )
        }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertCountEquals(0)
        onNodeWithTag(NowPlayingBarPlayPauseTestTag).performTouchInput {
            down(center)
            up()
        }
        onAllNodesWithTag("NowPlayingBarSearch").assertCountEquals(0)
        onAllNodesWithTag("NowPlayingBarSettings").assertCountEquals(0)
        onNodeWithTag(NowPlayingBarRootTestTag).performTouchInput { swipeUp() }
        waitForIdle()

        assertEquals(0, playPauseCallbacks)
        assertEquals(0, expandCallbacks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun miniPlayerActionsRemainIndependentFromExpansion(): Unit =
        runComposeUiTest {
            var playPauseCallbacks = 0
            var skipNextCallbacks = 0
            var openQueueCallbacks = 0
            var expandCallbacks = 0
            setContent {
                bar(
                    interactive = true,
                    onPlayPause = { playPauseCallbacks += 1 },
                    onSkipNext = { skipNextCallbacks += 1 },
                    onOpenQueue = { openQueueCallbacks += 1 },
                    onExpand = { expandCallbacks += 1 },
                )
            }
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .assertCountEquals(4)
            onNodeWithTag(NowPlayingBarPlayPauseTestTag).performClick()
            onNodeWithTag(NowPlayingBarNextTestTag).performClick()
            onNodeWithTag(NowPlayingBarQueueTestTag).performClick()
            onNodeWithTag(NowPlayingBarRootTestTag).performClick()
            waitForIdle()
            assertEquals(1, playPauseCallbacks)
            assertEquals(1, skipNextCallbacks)
            assertEquals(1, openQueueCallbacks)
            assertEquals(1, expandCallbacks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun focusedMiniPlayerActionUsesInkContrastBoundary(): Unit =
        runComposeUiTest {
            setContent { bar(interactive = true) }

            val playPause = onNodeWithTag(NowPlayingBarPlayPauseTestTag)
            playPause.performSemanticsAction(SemanticsActions.RequestFocus)
            playPause.assertIsFocused()
            val pixels = playPause.captureToImage().toPixelMap()
            val centerY = pixels.height / 2
            assertEquals(
                LightHausPalette.outline.toArgb(),
                pixels[1, centerY].toArgb(),
                "Mini-player action should draw an outer focus boundary",
            )
            assertEquals(
                LightHausPalette.paper.toArgb(),
                pixels[3, centerY].toArgb(),
                "Mini-player action should contrast with its ink fill",
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun miniPlayerControlsMeetThe48DpMinimum(): Unit = runComposeUiTest {
        setContent { bar(interactive = true) }

        for (tag in
            listOf(
                NowPlayingBarPlayPauseTestTag,
                NowPlayingBarNextTestTag,
                NowPlayingBarQueueTestTag,
            )) {
            val target = onNodeWithTag(tag).fetchSemanticsNode()
            val minimumSizePx = 48f * target.layoutInfo.density.density
            assertTrue(
                target.boundsInRoot.width >= minimumSizePx &&
                    target.boundsInRoot.height >= minimumSizePx,
                "$tag target is ${target.boundsInRoot.width}x" +
                    "${target.boundsInRoot.height}px; minimum is " +
                    "$minimumSizePx px",
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun bar(
    interactive: Boolean,
    onPlayPause: () -> Unit = {},
    onExpand: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
): Unit =
    NowPlayingBar(
        track =
            Track(
                "id",
                "Song",
                "Artist",
                "Album",
                1,
                TrackAccent(0, 0),
                AudioSource.FilePath("song.mp3")),
        playbackState = PlaybackState(),
        labels =
            NowPlayingBarLabels(
                "Play",
                "Pause",
                "Art",
                "Artist · Album",
                "Next track",
                "Queue"),
        artworkLoader = { null },
        onPlayPause = onPlayPause,
        onExpand = onExpand,
        expandProgress = remember { Animatable(0f) },
        isExpanded = false,
        interactive = interactive,
        onSkipNext = onSkipNext,
        onOpenQueue = onOpenQueue,
    )
