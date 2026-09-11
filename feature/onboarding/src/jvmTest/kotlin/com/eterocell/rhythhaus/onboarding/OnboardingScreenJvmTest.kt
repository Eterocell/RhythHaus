package com.eterocell.rhythhaus.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingScreenJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inactivePagesExposeNoSemantics() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                0,
                OnboardingPlatformGuidance.Android,
                false,
                null,
                false,
                {},
                {},
                {},
                {},
                {},
                Modifier)
        }
        onNodeWithTag(OnboardingPageTestTag).assertIsDisplayed()
        onAllNodesWithText("Scan and organize").assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun firstRunActionsDispatchExactlyOnce() = runComposeUiTest {
        var next = 0
        var skip = 0
        setContent {
            OnboardingScreen(
                0,
                OnboardingPlatformGuidance.Android,
                false,
                null,
                false,
                {},
                { next++ },
                { skip++ },
                {},
                {},
                Modifier)
        }
        onNodeWithTag(OnboardingNextTestTag).performClick()
        onNodeWithTag(OnboardingSkipTestTag).performClick()
        assertEquals(1, next)
        assertEquals(1, skip)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reviewShowsCloseWithoutSkipOrFinish() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                0,
                OnboardingPlatformGuidance.Android,
                false,
                null,
                true,
                {},
                {},
                {},
                {},
                {},
                Modifier)
        }
        onNodeWithTag(OnboardingCloseTestTag).assertIsDisplayed()
        onAllNodesWithTag(OnboardingSkipTestTag).assertCountEquals(0)
        onAllNodesWithTag(OnboardingFinishTestTag).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun savingDisablesCompletionActions() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                3,
                OnboardingPlatformGuidance.Android,
                true,
                null,
                false,
                {},
                {},
                {},
                {},
                {},
                Modifier)
        }
        onNodeWithTag(OnboardingFinishTestTag).assertIsNotEnabled()
        onNodeWithTag(OnboardingSkipTestTag).assertIsNotEnabled()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun completionErrorUsesLocalizedFeatureCopy() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                3,
                OnboardingPlatformGuidance.Android,
                false,
                "detail",
                false,
                {},
                {},
                {},
                {},
                {},
                Modifier)
        }
        onNodeWithTag(OnboardingErrorTestTag).assertIsDisplayed()
        onNodeWithTag(OnboardingRetryTestTag).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun androidIosAndMacGuidanceAreDistinct() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runComposeUiTest {
                var currentPlatform by
                    mutableStateOf(OnboardingPlatformGuidance.Android)
                setContent {
                    OnboardingScreen(
                        1,
                        currentPlatform,
                        false,
                        null,
                        false,
                        {},
                        {},
                        {},
                        {},
                        {},
                        Modifier)
                }
                for (platform in OnboardingPlatformGuidance.entries) {
                    currentPlatform = platform
                    waitForIdle()
                    val text =
                        when (platform) {
                            OnboardingPlatformGuidance.Android ->
                                "Android folder access is granted through the system folder picker."
                            OnboardingPlatformGuidance.IOS ->
                                "iOS copies files into Documents/RhythHaus, or scans files already there in place."
                            OnboardingPlatformGuidance.MacOS ->
                                "macOS keeps a reference to the folder you choose; files remain where they are."
                        }
                    onNodeWithTag(
                            OnboardingGuidanceTestTag, useUnmergedTree = true)
                        .assertExists()
                        .assertTextContains(text, substring = true)
                    OnboardingPlatformGuidance.entries
                        .filter { it != platform }
                        .forEach { other ->
                            val otherText =
                                when (other) {
                                    OnboardingPlatformGuidance.Android ->
                                        "Android folder access is granted through the system folder picker."
                                    OnboardingPlatformGuidance.IOS ->
                                        "iOS copies files into Documents/RhythHaus, or scans files already there in place."
                                    OnboardingPlatformGuidance.MacOS ->
                                        "macOS keeps a reference to the folder you choose; files remain where they are."
                                }
                            onAllNodesWithText(otherText, substring = true)
                                .assertCountEquals(0)
                        }
                }
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun chineseLocaleRendersChineseHeadingAndActions() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
            runComposeUiTest {
                setContent {
                    OnboardingScreen(
                        0,
                        OnboardingPlatformGuidance.Android,
                        false,
                        null,
                        false,
                        {},
                        {},
                        {},
                        {},
                        {},
                        Modifier)
                }
                onAllNodesWithText("音乐始终保存在本地").assertCountEquals(1)
                onAllNodesWithText("下一步").assertCountEquals(1)
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun actionsRemainReachableAt600By400Dp() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                3,
                OnboardingPlatformGuidance.Android,
                false,
                null,
                false,
                {},
                {},
                {},
                {},
                {},
                Modifier.size(600.dp, 400.dp))
        }
        onNodeWithTag(OnboardingFinishTestTag).assertIsDisplayed()
        onNodeWithTag(OnboardingSkipTestTag).assertIsDisplayed()
    }
}
