package com.eterocell.rhythhaus.onboarding

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
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
                androidx.compose.ui.Modifier)
        }
        onNodeWithTag(OnboardingPageTestTag).assertIsDisplayed()
        onAllNodesWithTag("onboarding-page-1").assertCountEquals(0)
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
                androidx.compose.ui.Modifier)
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
                androidx.compose.ui.Modifier)
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
                androidx.compose.ui.Modifier)
        }
        onNodeWithTag(OnboardingFinishTestTag).assertIsNotEnabled()
        onNodeWithTag(OnboardingSkipTestTag).assertIsNotEnabled()
    }
}
