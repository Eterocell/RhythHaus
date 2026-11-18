package com.eterocell.rhythhaus.nowplaying.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ScrubberTestTag = "MusicProgressScrubberSemantics"

public class MusicProgressScrubberSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun exposesLocalizedAdjustableNameValueAndRange(): Unit =
        withEnglishLocale {
            runComposeUiTest {
                setContent {
                    MusicProgressScrubber(
                        positionMillis = 10_000L,
                        durationMillis = 100_000L,
                        onSeek = {},
                        modifier = Modifier.testTag(ScrubberTestTag),
                    )
                }

                val config =
                    onNodeWithTag(ScrubberTestTag).fetchSemanticsNode().config
                assertEquals(
                    listOf("Playback position"),
                    config[SemanticsProperties.ContentDescription],
                )
                assertEquals(
                    "0:10 of 1:40",
                    config[SemanticsProperties.StateDescription],
                )
                assertEquals(
                    ProgressBarRangeInfo(current = 0.1f, range = 0f..1f),
                    config[SemanticsProperties.ProgressBarRangeInfo],
                )
                assertTrue(config.contains(SemanticsActions.SetProgress))
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun setProgressActionSeeksAtRequestedFractionAndClamps(): Unit =
        runComposeUiTest {
            val seeks = mutableListOf<Long>()
            setContent {
                MusicProgressScrubber(
                    positionMillis = 10_000L,
                    durationMillis = 100_000L,
                    onSeek = seeks::add,
                    modifier = Modifier.testTag(ScrubberTestTag),
                )
            }
            val scrubber = onNodeWithTag(ScrubberTestTag)

            scrubber.performSemanticsAction(SemanticsActions.SetProgress) {
                it(0.5f)
            }
            waitForIdle()
            assertEquals(listOf(50_000L), seeks)

            scrubber.performSemanticsAction(SemanticsActions.SetProgress) {
                it(1.5f)
            }
            waitForIdle()
            assertEquals(listOf(50_000L, 100_000L), seeks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun keyboardArrowsSeekFromFocusedPositionByOneStep(): Unit =
        runComposeUiTest {
            val seeks = mutableListOf<Long>()
            setContent {
                MusicProgressScrubber(
                    positionMillis = 10_000L,
                    durationMillis = 100_000L,
                    onSeek = seeks::add,
                    modifier = Modifier.testTag(ScrubberTestTag),
                )
            }
            val scrubber = onNodeWithTag(ScrubberTestTag)
            scrubber.performSemanticsAction(SemanticsActions.RequestFocus)
            scrubber.assertIsFocused()

            scrubber.performKeyInput {
                keyDown(Key.DirectionRight)
                keyUp(Key.DirectionRight)
            }
            waitForIdle()
            assertEquals(listOf(15_000L), seeks)

            scrubber.performKeyInput {
                keyDown(Key.DirectionLeft)
                keyUp(Key.DirectionLeft)
            }
            waitForIdle()
            assertEquals(listOf(15_000L, 5_000L), seeks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun keyboardArrowsDoNotSeekBeyondBounds(): Unit = runComposeUiTest {
        val seeks = mutableListOf<Long>()
        var position by mutableStateOf(0L)
        setContent {
            MusicProgressScrubber(
                positionMillis = position,
                durationMillis = 100_000L,
                onSeek = {
                    seeks += it
                    position = it
                },
                modifier = Modifier.testTag(ScrubberTestTag),
            )
        }
        val scrubber = onNodeWithTag(ScrubberTestTag)
        scrubber.performSemanticsAction(SemanticsActions.RequestFocus)
        scrubber.assertIsFocused()

        scrubber.performKeyInput {
            keyDown(Key.DirectionLeft)
            keyUp(Key.DirectionLeft)
        }
        waitForIdle()
        assertTrue(seeks.isEmpty())

        runOnIdle { position = 100_000L }
        scrubber.performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        waitForIdle()
        assertTrue(seeks.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun unknownDurationExposesNoAdjustableSeek(): Unit =
        runComposeUiTest {
            val seeks = mutableListOf<Long>()
            setContent {
                MusicProgressScrubber(
                    positionMillis = 0L,
                    durationMillis = 0L,
                    onSeek = seeks::add,
                    modifier = Modifier.testTag(ScrubberTestTag),
                )
            }
            val scrubber = onNodeWithTag(ScrubberTestTag)
            val config = scrubber.fetchSemanticsNode().config
            assertFalse(
                config.contains(SemanticsProperties.ProgressBarRangeInfo))
            assertFalse(config.contains(SemanticsActions.SetProgress))

            scrubber.performSemanticsAction(SemanticsActions.RequestFocus)
            scrubber.assertIsFocused()
            scrubber.performKeyInput {
                keyDown(Key.DirectionRight)
                keyUp(Key.DirectionRight)
            }
            scrubber.performTouchInput {
                down(Offset(width * 0.5f, center.y))
                up()
            }
            waitForIdle()
            assertTrue(seeks.isEmpty())
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun dragPreviewDefersSeekUntilRelease(): Unit = withEnglishLocale {
        runComposeUiTest {
            val seeks = mutableListOf<Long>()
            setContent {
                MusicProgressScrubber(
                    positionMillis = 0L,
                    durationMillis = 100_000L,
                    onSeek = seeks::add,
                    modifier = Modifier.testTag(ScrubberTestTag),
                )
            }
            val scrubber = onNodeWithTag(ScrubberTestTag)

            scrubber.performTouchInput {
                down(Offset(width * 0.2f, center.y))
                moveTo(Offset(width * 0.5f, center.y))
            }
            waitForIdle()
            assertTrue(seeks.isEmpty())
            assertEquals(
                "0:50 of 1:40",
                scrubber
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.StateDescription],
            )

            scrubber.performTouchInput { up() }
            waitForIdle()
            assertEquals(listOf(50_000L), seeks)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun interruptedDragDoesNotSeek(): Unit = runComposeUiTest {
        val seeks = mutableListOf<Long>()
        var scrubberVisible by mutableStateOf(true)
        setContent {
            if (scrubberVisible) {
                MusicProgressScrubber(
                    positionMillis = 0L,
                    durationMillis = 100_000L,
                    onSeek = seeks::add,
                    modifier = Modifier.testTag(ScrubberTestTag),
                )
            }
        }
        val scrubber = onNodeWithTag(ScrubberTestTag)

        scrubber.performTouchInput {
            down(Offset(width * 0.2f, center.y))
            moveTo(Offset(width * 0.6f, center.y))
        }
        waitForIdle()
        assertTrue(seeks.isEmpty())

        runOnIdle { scrubberVisible = false }
        waitForIdle()
        onNodeWithTag(ScrubberTestTag).assertDoesNotExist()
        assertTrue(seeks.isEmpty())
    }
}

private fun withEnglishLocale(block: () -> Unit) {
    val original = Locale.getDefault()
    try {
        Locale.setDefault(Locale.ENGLISH)
        block()
    } finally {
        Locale.setDefault(original)
    }
}
