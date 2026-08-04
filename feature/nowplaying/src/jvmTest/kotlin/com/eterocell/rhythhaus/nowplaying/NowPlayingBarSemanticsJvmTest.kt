package com.eterocell.rhythhaus.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import kotlin.test.Test
import kotlin.test.assertEquals

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
        var settingsCallbacks = 0
        var searchCallbacks = 0
        setContent {
            bar(
                interactive = false,
                onPlayPause = { playPauseCallbacks += 1 },
                onExpand = { expandCallbacks += 1 },
                onSettings = { settingsCallbacks += 1 },
                onSearch = { searchCallbacks += 1 },
            )
        }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertCountEquals(0)
        onNodeWithTag(NowPlayingBarPlayPauseTestTag).performTouchInput {
            down(center)
            up()
        }
        onNodeWithTag(NowPlayingBarSearchTestTag).performTouchInput {
            down(center)
            up()
        }
        onNodeWithTag(NowPlayingBarSettingsTestTag).performTouchInput {
            down(center)
            up()
        }
        onNodeWithTag(NowPlayingBarRootTestTag).performTouchInput { swipeUp() }
        waitForIdle()

        assertEquals(0, playPauseCallbacks)
        assertEquals(0, expandCallbacks)
        assertEquals(0, settingsCallbacks)
        assertEquals(0, searchCallbacks)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun matchingMeasuredNowPlayingBarDispatchesEachExactCallback():
        Unit = runComposeUiTest {
        var playPauseCallbacks = 0
        var expandCallbacks = 0
        var settingsCallbacks = 0
        var searchCallbacks = 0
        setContent {
            bar(
                interactive = true,
                onPlayPause = { playPauseCallbacks += 1 },
                onExpand = { expandCallbacks += 1 },
                onSettings = { settingsCallbacks += 1 },
                onSearch = { searchCallbacks += 1 },
            )
        }
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertCountEquals(4)
        onNodeWithTag(NowPlayingBarPlayPauseTestTag).performClick()
        onNodeWithTag(NowPlayingBarSearchTestTag).performClick()
        onNodeWithTag(NowPlayingBarSettingsTestTag).performClick()
        onNodeWithTag(NowPlayingBarRootTestTag).performClick()
        waitForIdle()
        assertEquals(1, playPauseCallbacks)
        assertEquals(1, expandCallbacks)
        assertEquals(1, settingsCallbacks)
        assertEquals(1, searchCallbacks)
    }
}

@androidx.compose.runtime.Composable
private fun bar(
    interactive: Boolean,
    onPlayPause: () -> Unit = {},
    onExpand: () -> Unit = {},
    onSettings: () -> Unit = {},
    onSearch: () -> Unit = {},
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
                "Play", "Pause", "Search", "Settings", "Art", "Artist · Album"),
        artworkLoader = { null },
        onPlayPause = onPlayPause,
        onExpand = onExpand,
        onSettings = onSettings,
        onSearch = onSearch,
        expandProgress = remember { Animatable(0f) },
        isExpanded = false,
        interactive = interactive,
    )
