package com.eterocell.rhythhaus.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AppOnboardingGate
import com.eterocell.rhythhaus.library.ui.OnboardingLaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow

class AppOnboardingLifecycleJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadingShowsOnlyGateWhileSiblingStartupContinues() = runComposeUiTest {
        val store = FakeOnboardingPreferenceStore(OnboardingEligibility.Loading)
        var startupRuns = 0

        setContent {
            LaunchedEffect(Unit) { startupRuns++ }
            TestOnboardingGate(store)
        }
        waitForIdle()

        assertEquals(1, startupRuns)
        onNodeWithTag(LoadingTag).assertExists()
        onAllNodesWithTag(FirstRunTag).assertCountEquals(0)
        onAllNodesWithTag(HomeTag).assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun requiredAndCompletedEligibilitySelectFirstRunAndHome() =
        runComposeUiTest {
            val store =
                FakeOnboardingPreferenceStore(OnboardingEligibility.Required)
            setContent { TestOnboardingGate(store) }

            onNodeWithTag(FirstRunTag).assertExists()
            onAllNodesWithTag(HomeTag).assertCountEquals(0)

            store.eligibility.value = OnboardingEligibility.Completed
            waitForIdle()

            onNodeWithTag(HomeTag).assertExists()
            onAllNodesWithTag(FirstRunTag).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun successfulWriteDoesNotExitBeforeCompletedEligibility() =
        runComposeUiTest {
            val store =
                FakeOnboardingPreferenceStore(OnboardingEligibility.Required)
            setContent { TestOnboardingGate(store) }

            onNodeWithTag(FirstRunTag).performClick()
            waitForIdle()

            assertEquals(1, store.completionCalls)
            onNodeWithTag(FirstRunTag).assertExists()
            onAllNodesWithTag(HomeTag).assertCountEquals(0)

            store.eligibility.value = OnboardingEligibility.Completed
            waitForIdle()
            onNodeWithTag(HomeTag).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedCompletionWriteKeepsFirstRunAndPublishesError() =
        runComposeUiTest {
            val store =
                FakeOnboardingPreferenceStore(
                    OnboardingEligibility.Required,
                    completionFailure = IllegalStateException("write failed"),
                )
            setContent { TestOnboardingGate(store) }

            onNodeWithTag(FirstRunTag).performClick()
            waitForIdle()

            assertEquals(1, store.completionCalls)
            onNodeWithTag(FirstRunTag).assertExists()
            onNodeWithTag(ErrorTag, useUnmergedTree = true).assertExists()
            onAllNodesWithTag(HomeTag).assertCountEquals(0)
        }

    private class FakeOnboardingPreferenceStore(
        initialEligibility: OnboardingEligibility,
        private val completionFailure: Throwable? = null,
    ) : OnboardingPreferenceStore {
        override val eligibility = MutableStateFlow(initialEligibility)
        var completionCalls = 0
            private set

        override suspend fun markCurrentVersionCompleted() {
            completionCalls++
            completionFailure?.let { throw it }
        }
    }

    companion object {
        private const val LoadingTag = "app-onboarding-loading"
        private const val FirstRunTag = "app-onboarding-first-run"
        private const val HomeTag = "app-onboarding-home"
        private const val ErrorTag = "app-onboarding-error"

        @androidx.compose.runtime.Composable
        private fun TestOnboardingGate(store: OnboardingPreferenceStore) {
            AppOnboardingGate(
                onboardingPreferenceStore = store,
                loadingContent = { Box(Modifier.testTag(LoadingTag)) },
            ) { state ->
                val tag =
                    if (state.initialOnboarding ==
                        OnboardingLaunchMode.FirstRun) {
                        FirstRunTag
                    } else {
                        HomeTag
                    }
                Box(
                    Modifier.size(48.dp).testTag(tag).clickable {
                        state.completeOnboarding()
                    },
                ) {
                    if (state.completionError != null) {
                        Box(Modifier.testTag(ErrorTag))
                    }
                }
            }
        }
    }
}
