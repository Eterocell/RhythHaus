package com.eterocell.rhythhaus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import com.eterocell.rhythhaus.library.ui.OnboardingLaunchMode
import com.eterocell.rhythhaus.onboarding.OnboardingEligibility
import com.eterocell.rhythhaus.onboarding.OnboardingPreferenceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.playlist_loading
import top.yukonga.miuix.kmp.basic.Text

internal data class AppOnboardingPresentation(
    val initialOnboarding: OnboardingLaunchMode?,
    val saving: Boolean,
    val completionError: String?,
    val completeOnboarding: () -> Unit,
)

@Composable
internal fun AppOnboardingGate(
    onboardingPreferenceStore: OnboardingPreferenceStore,
    loadingContent: @Composable () -> Unit,
    content: @Composable (AppOnboardingPresentation) -> Unit,
) {
    val eligibility by
        onboardingPreferenceStore.eligibility.collectAsState(
            OnboardingEligibility.Loading)
    var saving by remember { mutableStateOf(false) }
    var completionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val completeOnboarding: () -> Unit = {
        if (!saving) {
            saving = true
            completionError = null
            scope.launch {
                try {
                    onboardingPreferenceStore.markCurrentVersionCompleted()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    completionError =
                        failure.message ?: "Unable to save onboarding"
                } finally {
                    saving = false
                }
            }
        }
    }
    if (eligibility == OnboardingEligibility.Loading) {
        loadingContent()
    } else {
        content(
            AppOnboardingPresentation(
                initialOnboarding =
                    OnboardingLaunchMode.FirstRun.takeIf {
                        eligibility == OnboardingEligibility.Required
                    },
                saving = saving,
                completionError = completionError,
                completeOnboarding = completeOnboarding,
            ),
        )
    }
}

@Composable
internal fun OnboardingLoadingSurface() {
    Box(
        modifier =
            androidx.compose.ui.Modifier.fillMaxSize()
                .testTag("onboarding-loading"),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.playlist_loading))
    }
}
