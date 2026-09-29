package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.SleepTimerMode
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

public class SleepTimerPanelSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun timerSetupDoesNotDisplacePrimaryTransport(): Unit =
        withLocale(Locale.ENGLISH) {
            listOf(390.dp to 844.dp, 600.dp to 400.dp).forEach { (width, height)
                ->
                runComposeUiTest {
                    val controller = sleepTimerController()
                    mountSleepTimer(
                        controller = controller, width = width, height = height)
                    onNodeWithTag(NowPlayingPlayPauseTestTag)
                        .assertIsDisplayed()
                    onNodeWithTag(NowPlayingPreviousTestTag).assertIsDisplayed()
                    onNodeWithTag(NowPlayingNextTestTag).assertIsDisplayed()
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun idleTimerControlsArmWithSelectedFadeAndCancelWithoutLeakingActiveState():
        Unit =
        withLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val controller = sleepTimerController()
                mountSleepTimer(controller)

                onNodeWithTag(NowPlayingSleepTimerActiveSummaryTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(NowPlayingSleepTimerCancelTestTag)
                    .assertDoesNotExist()
                onNode(
                        hasContentDescription("10-second fade") and
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.ToggleableState,
                                ToggleableState.Off,
                            ),
                    )
                    .performClick()
                onNode(
                        hasContentDescription("10-second fade") and
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.ToggleableState,
                                ToggleableState.On,
                            ),
                    )
                    .assertIsDisplayed()
                onNodeWithTag(NowPlayingSleepTimer15MinutesTestTag)
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value.mode ==
                        SleepTimerMode.Timed
                }
                waitForIdle()

                assertEquals(
                    SleepTimerMode.Timed,
                    controller.sleepTimerState.value.mode,
                )
                assertEquals(true, controller.sleepTimerState.value.fadeEnabled)
                assertEquals(
                    true,
                    controller.sleepTimerState.value.remainingMillis?.let {
                        it in 1L..900_000L
                    } == true,
                )
                onNode(
                        hasContentDescription(
                            "Stops in 15 minutes; 10-second fade on",
                        ),
                    )
                    .assertIsDisplayed()
                onNode(
                        hasContentDescription("10-second fade") and
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.ToggleableState,
                                ToggleableState.On,
                            ),
                    )
                    .assertHasClickAction()
                    .performClick()
                assertEquals(true, controller.sleepTimerState.value.fadeEnabled)
                onNode(
                        hasContentDescription(
                            "Stops in 15 minutes; 10-second fade on",
                        ),
                    )
                    .assertIsDisplayed()
                onNodeWithTag(NowPlayingSleepTimer3TracksTestTag)
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value.remainingTracks == 3
                }
                assertEquals(
                    false, controller.sleepTimerState.value.fadeEnabled)
                onNodeWithTag(NowPlayingSleepTimerCancelTestTag).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value == SleepTimerState()
                }
                waitForIdle()

                assertEquals(
                    SleepTimerState(), controller.sleepTimerState.value)
                onNodeWithTag(NowPlayingSleepTimerActiveSummaryTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(NowPlayingSleepTimerCancelTestTag)
                    .assertDoesNotExist()
                controller.release()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun everyTimerChoiceIsReachableInCompactAndShortSplitLayouts():
        Unit =
        withLocale(Locale.ENGLISH) {
            listOf(
                    TimerViewport(
                        width = 390.dp,
                        height = 400.dp,
                        layoutTag = NowPlayingCompactLayoutTestTag,
                    ),
                    TimerViewport(
                        width = 600.dp,
                        height = 400.dp,
                        layoutTag = NowPlayingSplitLayoutTestTag,
                    ),
                )
                .forEach { viewport ->
                    runComposeUiTest {
                        val controller = sleepTimerController()
                        mountSleepTimer(
                            controller = controller,
                            width = viewport.width,
                            height = viewport.height,
                        )
                        onNodeWithTag(viewport.layoutTag).assertIsDisplayed()

                        listOf(
                                NowPlayingSleepTimer15MinutesTestTag to
                                    15 * 60_000L,
                                NowPlayingSleepTimer30MinutesTestTag to
                                    30 * 60_000L,
                                NowPlayingSleepTimer45MinutesTestTag to
                                    45 * 60_000L,
                                NowPlayingSleepTimer60MinutesTestTag to
                                    60 * 60_000L,
                            )
                            .forEach { (tag, expectedMillis) ->
                                onNodeWithTag(tag)
                                    .performScrollTo()
                                    .assertHasClickAction()
                                    .performClick()
                                waitUntil(timeoutMillis = 5_000) {
                                    controller.sleepTimerState.value
                                        .remainingMillis
                                        ?.let {
                                            it in
                                                (expectedMillis -
                                                    5_000L)..expectedMillis
                                        } == true
                                }
                                assertEquals(
                                    SleepTimerMode.Timed,
                                    controller.sleepTimerState.value.mode,
                                )
                                assertEquals(
                                    true,
                                    controller.sleepTimerState.value
                                        .remainingMillis
                                        ?.let {
                                            it in
                                                (expectedMillis -
                                                    5_000L)..expectedMillis
                                        } == true,
                                )
                                onNodeWithTag(NowPlayingSleepTimerCancelTestTag)
                                    .performScrollTo()
                                    .performClick()
                                waitUntil(timeoutMillis = 5_000) {
                                    controller.sleepTimerState.value ==
                                        SleepTimerState()
                                }
                            }

                        listOf(
                                NowPlayingSleepTimerCurrentTrackTestTag to 1,
                                NowPlayingSleepTimer3TracksTestTag to 3,
                                NowPlayingSleepTimer5TracksTestTag to 5,
                            )
                            .forEach { (tag, expectedTracks) ->
                                onNodeWithTag(tag)
                                    .performScrollTo()
                                    .assertHasClickAction()
                                    .performClick()
                                waitUntil(timeoutMillis = 5_000) {
                                    controller.sleepTimerState.value
                                        .remainingTracks == expectedTracks
                                }
                                assertEquals(
                                    SleepTimerMode.TrackCount,
                                    controller.sleepTimerState.value.mode,
                                )
                                assertEquals(
                                    expectedTracks,
                                    controller.sleepTimerState.value
                                        .remainingTracks,
                                )
                                onNodeWithTag(NowPlayingSleepTimerCancelTestTag)
                                    .performScrollTo()
                                    .performClick()
                                waitUntil(timeoutMillis = 5_000) {
                                    controller.sleepTimerState.value ==
                                        SleepTimerState()
                                }
                            }
                        controller.release()
                    }
                }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun activeTimerSummaryLocalizesModeRemainingAmountAndFadeSetting():
        Unit {
        withLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val controller = sleepTimerController()
                mountSleepTimer(
                    controller = controller,
                    timerState =
                        SleepTimerState(
                            mode = SleepTimerMode.Timed,
                            remainingMillis = 61_000L,
                            fadeEnabled = true,
                        ),
                )

                onNode(
                        hasContentDescription(
                            "Stops in 2 minutes; 10-second fade on",
                        ),
                    )
                    .assertIsDisplayed()
                controller.release()
            }
        }
        withLocale(Locale.SIMPLIFIED_CHINESE) {
            runComposeUiTest {
                val controller = sleepTimerController()
                mountSleepTimer(
                    controller = controller,
                    timerState =
                        SleepTimerState(
                            mode = SleepTimerMode.TrackCount,
                            remainingTracks = 3,
                            fadeEnabled = false,
                        ),
                )

                onNode(hasContentDescription("将在剩余 3 首歌曲后停止；10 秒淡出：关闭"))
                    .assertIsDisplayed()
                listOf(
                        "15 分钟",
                        "30 分钟",
                        "45 分钟",
                        "60 分钟",
                        "当前歌曲",
                        "3 首歌曲",
                        "5 首歌曲",
                        "取消定时器",
                    )
                    .forEach { label ->
                        onNode(hasContentDescription(label))
                            .assertHasClickAction()
                    }
                onNode(hasContentDescription("10 秒淡出")).assertHasClickAction()
                controller.release()
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun androidx.compose.ui.test.ComposeUiTest.mountSleepTimer(
    controller: PlaybackController,
    width: Dp = 390.dp,
    height: Dp = 844.dp,
    timerState: SleepTimerState? = null,
): Unit {
    setContent {
        val playbackState by controller.state.collectAsState()
        val controllerTimerState by controller.sleepTimerState.collectAsState()
        Box(Modifier.size(width, height)) {
            NowPlayingContent(
                track = sleepTimerTrack(),
                playbackState = playbackState,
                playbackController = controller,
                labels = sleepTimerLabels,
                artworkLoader = { null },
                onBack = {},
                sleepTimerState = timerState ?: controllerTimerState,
                onArmSleepTimer = controller::armSleepTimer,
                onArmSleepTimerAfterCompletions =
                    controller::armSleepTimerAfterCompletions,
                onCancelSleepTimer = controller::cancelSleepTimer,
            )
        }
    }
    waitForIdle()
    waitUntil(timeoutMillis = 5_000) {
        controller.state.value.currentTrack?.id == "sleep-timer-track" &&
            controller.state.value.status == PlaybackStatus.Paused
    }
}

private fun sleepTimerController(): PlaybackController =
    PlaybackController(FakePlaybackEngine()).also { controller ->
        val track = sleepTimerTrack()
        controller.setQueue(
            listOf(
                PlayableTrack(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMillis = track.durationSeconds * 1_000L,
                    source = track.source,
                ),
            ),
            selectedTrackId = track.id,
        )
    }

private data class TimerViewport(
    val width: Dp,
    val height: Dp,
    val layoutTag: String,
)

private fun sleepTimerTrack(): Track =
    Track(
        id = "sleep-timer-track",
        title = "Sleep timer track",
        artist = "Artist",
        album = "Album",
        durationSeconds = 180,
        accent = TrackAccent(0xFF123456, 0xFF654321),
        source = AudioSource.FilePath("sleep-timer.mp3"),
    )

private val sleepTimerLabels =
    NowPlayingScreenLabels(
        play = "Play",
        pause = "Pause",
        albumArtwork = "Album art",
        currentTrackArtistAlbum = "Artist - Album",
    )

private fun withLocale(locale: Locale, block: () -> Unit): Unit {
    val previousLocale = Locale.getDefault()
    try {
        Locale.setDefault(locale)
        block()
    } finally {
        Locale.setDefault(previousLocale)
    }
}
