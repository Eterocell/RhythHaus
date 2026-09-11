package com.eterocell.rhythhaus.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.onboarding.generated.resources.Res
import rhythhaus.feature.onboarding.generated.resources.action_back
import rhythhaus.feature.onboarding.generated.resources.action_close
import rhythhaus.feature.onboarding.generated.resources.action_finish
import rhythhaus.feature.onboarding.generated.resources.action_next
import rhythhaus.feature.onboarding.generated.resources.action_retry
import rhythhaus.feature.onboarding.generated.resources.action_skip
import rhythhaus.feature.onboarding.generated.resources.body_add_music
import rhythhaus.feature.onboarding.generated.resources.body_formats
import rhythhaus.feature.onboarding.generated.resources.body_local_first
import rhythhaus.feature.onboarding.generated.resources.body_recovery
import rhythhaus.feature.onboarding.generated.resources.error_save
import rhythhaus.feature.onboarding.generated.resources.guidance_android
import rhythhaus.feature.onboarding.generated.resources.guidance_ios
import rhythhaus.feature.onboarding.generated.resources.guidance_macos
import rhythhaus.feature.onboarding.generated.resources.heading_add_music
import rhythhaus.feature.onboarding.generated.resources.heading_formats
import rhythhaus.feature.onboarding.generated.resources.heading_local_first
import rhythhaus.feature.onboarding.generated.resources.heading_recovery
import rhythhaus.feature.onboarding.generated.resources.onboarding_title
import rhythhaus.feature.onboarding.generated.resources.page_progress
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

/** Host-neutral platform wording selected by the composition root. */
public enum class OnboardingPlatformGuidance {
    Android,
    IOS,
    MacOS
}

internal enum class OnboardingPage {
    LocalFirst,
    AddMusic,
    ScanAndFormats,
    Recovery
}

internal val onboardingPages = OnboardingPage.entries

internal fun nextOnboardingPage(index: Int) =
    (index + 1).coerceAtMost(onboardingPages.lastIndex)

internal fun previousOnboardingPage(index: Int) = (index - 1).coerceAtLeast(0)

/** Stable semantics tag for the onboarding root. */
public const val OnboardingRootTestTag: String = "onboarding-root"
/** Stable semantics tag for the current page. */
public const val OnboardingPageTestTag: String = "onboarding-page"
/** Stable semantics tag for page progress. */
public const val OnboardingProgressTestTag: String = "onboarding-progress"
/** Stable semantics tag for completion errors. */
public const val OnboardingErrorTestTag: String = "onboarding-error"
/** Stable semantics tag for Back. */
public const val OnboardingBackTestTag: String = "onboarding-back"
/** Stable semantics tag for Next. */
public const val OnboardingNextTestTag: String = "onboarding-next"
/** Stable semantics tag for Skip. */
public const val OnboardingSkipTestTag: String = "onboarding-skip"
/** Stable semantics tag for Finish. */
public const val OnboardingFinishTestTag: String = "onboarding-finish"
/** Stable semantics tag for Close. */
public const val OnboardingCloseTestTag: String = "onboarding-close"
/** Stable semantics tag for retry. */
public const val OnboardingRetryTestTag: String = "onboarding-retry"
/** Stable semantics tag for rendered platform guidance. */
public const val OnboardingGuidanceTestTag: String = "onboarding-guidance"

/**
 * Renders one stateless onboarding page and its host-owned action callbacks.
 *
 * @param pageIndex current page payload
 * @param platform host platform guidance
 * @param saving whether completion is being persisted
 * @param completionError retryable save error, if any
 * @param reviewMode whether this is review rather than first-run mode
 * @param onBack handles Back
 * @param onNext handles Next
 * @param onSkip handles Skip
 * @param onFinish handles Finish
 * @param onClose handles Close
 * @param modifier root modifier
 */
@Composable
public fun OnboardingScreen(
    pageIndex: Int,
    platform: OnboardingPlatformGuidance,
    saving: Boolean,
    completionError: String?,
    reviewMode: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = pageIndex.coerceIn(0, onboardingPages.lastIndex)
    val title = stringResource(Res.string.onboarding_title)
    Surface(
        color = HausColors.current.paper,
        modifier = modifier.fillMaxSize().testTag(OnboardingRootTestTag)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().safeContentPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { RhythHausTopAppBar(title, onBack = onBack) }
                item {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag(OnboardingPageTestTag)) {
                            Text(
                                stringResource(
                                    Res.string.page_progress,
                                    page + 1,
                                    onboardingPages.size),
                                Modifier.testTag(OnboardingProgressTestTag))
                            Text(
                                pageHeading(page),
                                Modifier.padding(top = 16.dp))
                            Text(
                                pageBody(page, platform),
                                Modifier.padding(top = 10.dp))
                            if (onboardingPages[page] ==
                                OnboardingPage.AddMusic) {
                                val guidance = platformGuidance(platform)
                                Text(
                                    guidance,
                                    Modifier.testTag(OnboardingGuidanceTestTag)
                                        .semantics {
                                            contentDescription = guidance
                                        })
                            }
                        }
                }
                if (completionError != null) {
                    item {
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .testTag(OnboardingErrorTestTag)) {
                                Text(stringResource(Res.string.error_save))
                                if (completionError != null)
                                    Text(completionError)
                                Button(
                                    onClick = onFinish,
                                    enabled = !saving,
                                    modifier =
                                        Modifier.testTag(
                                            OnboardingRetryTestTag)) {
                                        Text(
                                            stringResource(
                                                Res.string.action_retry))
                                    }
                            }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (reviewMode) {
                            action(
                                onClose,
                                !saving,
                                OnboardingCloseTestTag,
                                stringResource(Res.string.action_close))
                            if (page > 0)
                                action(
                                    onBack,
                                    !saving,
                                    OnboardingBackTestTag,
                                    stringResource(Res.string.action_back))
                            if (page < onboardingPages.lastIndex)
                                action(
                                    onNext,
                                    !saving,
                                    OnboardingNextTestTag,
                                    stringResource(Res.string.action_next))
                        } else {
                            action(
                                onBack,
                                true,
                                OnboardingBackTestTag,
                                stringResource(Res.string.action_back))
                            if (page < onboardingPages.lastIndex)
                                action(
                                    onNext,
                                    !saving,
                                    OnboardingNextTestTag,
                                    stringResource(Res.string.action_next))
                            if (page == onboardingPages.lastIndex)
                                action(
                                    onFinish,
                                    !saving,
                                    OnboardingFinishTestTag,
                                    stringResource(Res.string.action_finish))
                            action(
                                onSkip,
                                !saving,
                                OnboardingSkipTestTag,
                                stringResource(Res.string.action_skip))
                        }
                    }
                }
            }
        }
}

@Composable
private fun action(
    onClick: () -> Unit,
    enabled: Boolean,
    tag: String,
    label: String
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.testTag(tag)
                .then(
                    if (!enabled) Modifier.semantics { disabled() }
                    else Modifier),
        cornerRadius = 12.dp,
        colors =
            ButtonDefaults.buttonColors(
                color = HausColors.current.ink,
                contentColor = HausColors.current.paper),
    ) {
        Text(label)
    }
}

@Composable
private fun pageHeading(page: Int): String =
    stringResource(
        when (onboardingPages[page]) {
            OnboardingPage.LocalFirst -> Res.string.heading_local_first
            OnboardingPage.AddMusic -> Res.string.heading_add_music
            OnboardingPage.ScanAndFormats -> Res.string.heading_formats
            OnboardingPage.Recovery -> Res.string.heading_recovery
        })

@Composable
private fun pageBody(page: Int, platform: OnboardingPlatformGuidance): String {
    val body =
        when (onboardingPages[page]) {
            OnboardingPage.LocalFirst -> Res.string.body_local_first
            OnboardingPage.AddMusic -> Res.string.body_add_music
            OnboardingPage.ScanAndFormats -> Res.string.body_formats
            OnboardingPage.Recovery -> Res.string.body_recovery
        }
    return stringResource(body)
}

@Composable
private fun platformGuidance(platform: OnboardingPlatformGuidance): String =
    when (platform) {
        OnboardingPlatformGuidance.Android ->
            stringResource(Res.string.guidance_android)
        OnboardingPlatformGuidance.IOS ->
            stringResource(Res.string.guidance_ios)
        OnboardingPlatformGuidance.MacOS ->
            stringResource(Res.string.guidance_macos)
    }
