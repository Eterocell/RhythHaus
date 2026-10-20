package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res as CoreUiRes
import rhythhaus.core.ui.generated.resources.back
import rhythhaus.feature.settings.generated.resources.Res as SettingsRes
import rhythhaus.feature.settings.generated.resources.notification_permission_body
import rhythhaus.feature.settings.generated.resources.notification_permission_request
import rhythhaus.feature.settings.generated.resources.notification_permission_settings
import rhythhaus.feature.settings.generated.resources.notification_permission_title
import rhythhaus.feature.settings.generated.resources.review_onboarding
import rhythhaus.feature.settings.generated.resources.source_access_available
import rhythhaus.feature.settings.generated.resources.source_access_lost
import rhythhaus.feature.settings.generated.resources.source_last_scanned
import rhythhaus.feature.settings.generated.resources.source_never_scanned
import rhythhaus.feature.settings.generated.resources.source_status_format
import rhythhaus.feature.settings.generated.resources.theme_dark_label
import rhythhaus.feature.settings.generated.resources.unnamed_folder

public class SettingsScreenSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun gaplessChoicesExposeAcceptedStateAndAreInertDuringConfirmation():
        Unit = runComposeUiTest {
        val selected = mutableListOf<SettingsGaplessMode>()
        var clearVisible by mutableStateOf(false)
        setContent {
            Box(Modifier.size(600.dp, 1_200.dp)) {
                content(
                    gaplessMode = SettingsGaplessMode.Off,
                    clearSlot =
                        if (clearVisible) ({ slot("confirmation") }) else null,
                    onGapless = { selected += it })
            }
        }
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsGaplessPrefix + "on"))
        onNodeWithTag(SettingsGaplessPrefix + "off").assertIsSelected()
        onNodeWithTag(SettingsGaplessPrefix + "on").assertIsNotSelected()
        runOnIdle { clearVisible = true }
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsGaplessPrefix + "on"))
        onNodeWithTag(SettingsGaplessPrefix + "on")
            .assertIsNotEnabled()
            .assertNoClickOrFocusSemantics()
        runOnIdle { clearVisible = false }
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsGaplessPrefix + "on"))
        onNodeWithTag(SettingsGaplessPrefix + "on").performClick()
        assertEquals(listOf(SettingsGaplessMode.On), selected)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun crossfadeChoicesAreInertDuringEitherConfirmation(): Unit =
        runComposeUiTest {
            var clearVisible by mutableStateOf(false)
            val requests = mutableListOf<SettingsCrossfadeDuration>()
            setContent {
                Box(Modifier.size(600.dp, 1_200.dp)) {
                    content(
                        sources = listOf(item()),
                        clearSlot =
                            if (clearVisible) ({ slot("clear-confirmation") })
                            else null,
                        onCrossfade = { requests += it })
                }
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsCrossfadePrefix + "5"))
            runOnIdle { clearVisible = true }
            onNodeWithTag(SettingsCrossfadePrefix + "5")
                .assertIsNotEnabled()
                .assertNoClickOrFocusSemantics()
            onNodeWithTag(SettingsCrossfadePrefix + "5", useUnmergedTree = true)
                .performTouchInput { click(center) }
            runOnIdle { clearVisible = false }
            onNodeWithTag(SettingsCrossfadePrefix + "5").assertHasClickAction()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsRemovePrefix + "one"))
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsCrossfadePrefix + "5"))
            onNodeWithTag(SettingsCrossfadePrefix + "5")
                .assertIsNotEnabled()
                .assertNoClickOrFocusSemantics()
            assertEquals(emptyList(), requests)
            onNodeWithTag(SettingsRemoveDismissTestTag, useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsCrossfadePrefix + "5").assertHasClickAction()
        }

    @Test
    public fun englishCrossfadeChoicesUseAcceptedSelectionAndRecoverableErrors():
        Unit =
        verifyCrossfadeSemantics(
            Locale.ENGLISH,
            "Crossfade",
            listOf("Off", "3 seconds", "5 seconds", "8 seconds"),
            "Overlaps eligible queue tracks by up to the selected duration. Short tracks use a shorter fade. Disabled with a sleep timer, Repeat One, Stop After Current, or unknown duration.",
            "Could not save crossfade. Your previous duration is still selected. Choose a duration again to retry.")

    @Test
    public fun chineseCrossfadeChoicesUseAcceptedSelectionAndRecoverableErrors():
        Unit =
        verifyCrossfadeSemantics(
            Locale.SIMPLIFIED_CHINESE,
            "交叉淡入淡出",
            listOf("关闭", "3 秒", "5 秒", "8 秒"),
            "符合条件的队列曲目按所选时长上限重叠播放。短曲目会缩短淡入淡出时长。启用睡眠定时器、单曲循环、播放当前曲目后停止或时长未知时不生效。",
            "无法保存交叉淡入淡出设置。仍使用上次选择的时长，请重新选择时长以重试。")

    @OptIn(ExperimentalTestApi::class)
    private fun verifyCrossfadeSemantics(
        locale: Locale,
        title: String,
        optionLabels: List<String>,
        explanation: String,
        failureMessage: String,
    ) {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            runComposeUiTest {
                var accepted by mutableStateOf(SettingsCrossfadeDuration.Off)
                var busy by mutableStateOf(false)
                var saveFailed by mutableStateOf(false)
                val requests = mutableListOf<SettingsCrossfadeDuration>()
                setContent {
                    Box(Modifier.size(600.dp, 1_200.dp)) {
                        content(
                            mutations = false,
                            crossfadeDuration = accepted,
                            crossfadeBusy = busy,
                            crossfadeError = saveFailed,
                            onCrossfade = { requests += it })
                    }
                }
                onNodeWithText(title, useUnmergedTree = true).assertExists()
                val tags = listOf("off", "3", "5", "8")
                tags.forEachIndexed { index, suffix ->
                    val tag = SettingsCrossfadePrefix + suffix
                    onNodeWithTag(SettingsListTestTag)
                        .performScrollToNode(hasTestTag(tag))
                    onNodeWithTag(tag)
                        .assertIsEnabled()
                        .assertHasClickAction()
                        .assert(
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.Role, Role.RadioButton))
                        .assert(hasText(optionLabels[index]))
                }
                onNodeWithTag(SettingsCrossfadePrefix + "off")
                    .assertIsSelected()
                onNodeWithTag(SettingsCrossfadePrefix + "5").performClick()
                assertEquals(
                    listOf(SettingsCrossfadeDuration.Seconds5), requests)
                onNodeWithTag(SettingsCrossfadePrefix + "off")
                    .assertIsSelected()
                onNodeWithTag(SettingsCrossfadePrefix + "5")
                    .assertIsNotSelected()
                runOnIdle { accepted = SettingsCrossfadeDuration.Seconds5 }
                onNodeWithTag(SettingsCrossfadePrefix + "5").assertIsSelected()

                runOnIdle { busy = true }
                tags.forEach { suffix ->
                    val tag = SettingsCrossfadePrefix + suffix
                    onNodeWithTag(SettingsListTestTag)
                        .performScrollToNode(hasTestTag(tag))
                    onNodeWithTag(tag)
                        .assertIsNotEnabled()
                        .assertNoClickOrFocusSemantics()
                    onNodeWithTag(tag, useUnmergedTree = true)
                        .assertHasNoClickAction()
                        .performTouchInput { click(center) }
                }
                assertEquals(
                    listOf(SettingsCrossfadeDuration.Seconds5), requests)
                onNodeWithTag(SettingsCrossfadeSavingTestTag)
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.LiveRegion,
                            LiveRegionMode.Polite))
                runOnIdle {
                    busy = false
                    saveFailed = true
                }
                onNodeWithTag(SettingsListTestTag)
                    .performScrollToNode(
                        hasTestTag(SettingsCrossfadeErrorTestTag))
                onNodeWithText(failureMessage, useUnmergedTree = true)
                    .assertExists()
                onNodeWithTag(SettingsCrossfadeErrorTestTag)
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.LiveRegion,
                            LiveRegionMode.Polite))
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Error, failureMessage))
                onNodeWithTag(SettingsCrossfadePrefix + "5").assertIsSelected()
                onNodeWithTag(SettingsCrossfadePrefix + "8").performClick()
                assertEquals(
                    listOf(
                        SettingsCrossfadeDuration.Seconds5,
                        SettingsCrossfadeDuration.Seconds8),
                    requests)
                runOnIdle {
                    accepted = SettingsCrossfadeDuration.Seconds8
                    saveFailed = false
                }
                onNodeWithTag(SettingsCrossfadePrefix + "8").assertIsSelected()
                onNodeWithTag(SettingsCrossfadeErrorTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(SettingsCrossfadeSavingTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(SettingsListTestTag)
                    .performScrollToNode(
                        hasTestTag(SettingsCrossfadeExplanationTestTag))
                onNodeWithText(explanation, useUnmergedTree = true)
                    .assertExists()
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun replayGainChoicesAreInertDuringEitherConfirmation(): Unit =
        runComposeUiTest {
            var clearVisible by mutableStateOf(false)
            val requests = mutableListOf<SettingsReplayGainMode>()
            setContent {
                Box(Modifier.size(600.dp, 1_200.dp)) {
                    content(
                        sources = listOf(item()),
                        clearSlot =
                            if (clearVisible) ({ slot("clear-confirmation") })
                            else null,
                        onReplayGain = { requests += it })
                }
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(
                    hasTestTag(SettingsReplayGainPrefix + "track"))
            runOnIdle { clearVisible = true }
            onNodeWithTag(SettingsReplayGainPrefix + "track")
                .assertNoClickOrFocusSemantics()
            runOnIdle { clearVisible = false }
            onNodeWithTag(SettingsReplayGainPrefix + "track")
                .assertHasClickAction()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsRemovePrefix + "one"))
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(
                    hasTestTag(SettingsReplayGainPrefix + "track"))
            onNodeWithTag(SettingsReplayGainPrefix + "track")
                .assertNoClickOrFocusSemantics()
            assertEquals(emptyList(), requests)
            onNodeWithTag(SettingsRemoveDismissTestTag, useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsReplayGainPrefix + "track")
                .assertHasClickAction()
        }

    @Test
    public fun englishReplayGainChoicesUseAcceptedSelectionAndRecoverableErrors():
        Unit =
        verifyReplayGainSemantics(
            Locale.ENGLISH,
            "ReplayGain normalization",
            listOf("Off", "Track", "Album"),
            "Uses existing ReplayGain tags to reduce volume only. Untagged tracks are unchanged, and positive gain is never amplified.",
            "Could not save normalization. Your previous mode is still selected. Choose a mode again to retry.")

    @Test
    public fun chineseReplayGainChoicesUseAcceptedSelectionAndRecoverableErrors():
        Unit =
        verifyReplayGainSemantics(
            Locale.SIMPLIFIED_CHINESE,
            "ReplayGain 音量均衡",
            listOf("关闭", "单曲", "专辑"),
            "仅根据已有 ReplayGain 标签降低音量。没有标签的曲目保持原音量，正增益不会被放大。",
            "无法保存音量均衡设置。仍使用上次选择的模式，请重新选择模式以重试。")

    @OptIn(ExperimentalTestApi::class)
    private fun verifyReplayGainSemantics(
        locale: Locale,
        title: String,
        optionLabels: List<String>,
        explanation: String,
        failureMessage: String
    ) {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            runComposeUiTest {
                var accepted by mutableStateOf(SettingsReplayGainMode.Off)
                var busy by mutableStateOf(false)
                var saveFailed by mutableStateOf(false)
                val requests = mutableListOf<SettingsReplayGainMode>()
                setContent {
                    Box(Modifier.size(600.dp, 900.dp)) {
                        content(
                            mutations = false,
                            replayGainMode = accepted,
                            replayGainBusy = busy,
                            replayGainError = saveFailed,
                            onReplayGain = { requests += it })
                    }
                }
                onNodeWithText(title, useUnmergedTree = true).assertExists()
                val modes =
                    listOf(
                        SettingsReplayGainMode.Off,
                        SettingsReplayGainMode.Track,
                        SettingsReplayGainMode.Album)
                modes.forEachIndexed { index, mode ->
                    val tag = SettingsReplayGainPrefix + mode.name.lowercase()
                    onNodeWithTag(SettingsListTestTag)
                        .performScrollToNode(hasTestTag(tag))
                    onNodeWithTag(tag)
                        .assertIsEnabled()
                        .assertHasClickAction()
                        .assert(
                            SemanticsMatcher("radio-button role") { node ->
                                node.config.getOrNull(
                                    SemanticsProperties.Role) ==
                                    Role.RadioButton
                            })
                        .assert(hasText(optionLabels[index]))
                }
                onNodeWithTag(SettingsReplayGainPrefix + "off")
                    .assertIsSelected()
                onNodeWithTag(SettingsReplayGainPrefix + "track").performClick()
                assertEquals(listOf(SettingsReplayGainMode.Track), requests)
                onNodeWithTag(SettingsReplayGainPrefix + "off")
                    .assertIsSelected()
                onNodeWithTag(SettingsReplayGainPrefix + "track")
                    .assertIsNotSelected()
                runOnIdle { accepted = SettingsReplayGainMode.Track }
                onNodeWithTag(SettingsReplayGainPrefix + "track")
                    .assertIsSelected()

                runOnIdle { busy = true }
                modes.forEach { mode ->
                    val tag = SettingsReplayGainPrefix + mode.name.lowercase()
                    onNodeWithTag(tag)
                        .assertIsNotEnabled()
                        .assertHasNoClickAction()
                    onNodeWithTag(tag, useUnmergedTree = true)
                        .assertHasNoClickAction()
                        .performTouchInput { click(center) }
                }
                assertEquals(listOf(SettingsReplayGainMode.Track), requests)
                onNodeWithTag(SettingsReplayGainSavingTestTag)
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.LiveRegion,
                            LiveRegionMode.Polite))

                runOnIdle {
                    busy = false
                    saveFailed = true
                }
                onNodeWithTag(SettingsListTestTag)
                    .performScrollToNode(
                        hasTestTag(SettingsReplayGainErrorTestTag))
                onNodeWithText(failureMessage, useUnmergedTree = true)
                    .assertExists()
                onNodeWithTag(SettingsReplayGainErrorTestTag)
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.LiveRegion,
                            LiveRegionMode.Polite))
                onNodeWithTag(SettingsReplayGainPrefix + "track")
                    .assertIsSelected()
                onNodeWithTag(SettingsReplayGainPrefix + "album")
                    .assertIsEnabled()
                    .performClick()
                assertEquals(
                    listOf(
                        SettingsReplayGainMode.Track,
                        SettingsReplayGainMode.Album),
                    requests)
                runOnIdle {
                    accepted = SettingsReplayGainMode.Album
                    saveFailed = false
                }
                onNodeWithTag(SettingsReplayGainPrefix + "album")
                    .assertIsSelected()
                onNodeWithTag(SettingsReplayGainErrorTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(SettingsReplayGainSavingTestTag)
                    .assertDoesNotExist()
                onNodeWithTag(SettingsListTestTag)
                    .performScrollToNode(
                        hasTestTag(SettingsReplayGainExplanationTestTag))
                onNodeWithText(explanation, useUnmergedTree = true)
                    .assertExists()
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun deviceAudioActionsRespectPermissionAndMutationGate(): Unit =
        runComposeUiTest {
            var variant by
                mutableStateOf<MediaStoreAudioRecovery?>(
                    MediaStoreAudioRecovery.Requestable)
            var enabled by mutableStateOf(true)
            val actions = mutableListOf<String>()
            setContent {
                Box(Modifier.size(600.dp, 400.dp)) {
                    content(
                        mediaStoreAudioRecovery = variant,
                        mutations = enabled,
                        onRequestAudio = { actions += "request" },
                        onOpenAudioSettings = { actions += "settings" },
                        onAddAudio = { actions += "add" },
                    )
                }
            }
            onNodeWithTag("settings-device-audio-action")
                .assertIsEnabled()
                .performClick()
            assertEquals(listOf("request"), actions)
            runOnIdle { variant = MediaStoreAudioRecovery.SettingsRequired }
            onNodeWithTag("settings-device-audio-action").performClick()
            runOnIdle { variant = MediaStoreAudioRecovery.Add }
            onNodeWithTag("settings-device-audio-action").performClick()
            assertEquals(listOf("request", "settings", "add"), actions)
            runOnIdle { enabled = false }
            onNodeWithTag("settings-device-audio-action").assertIsNotEnabled()
            runOnIdle { variant = null }
            onAllNodesWithTag("settings-device-audio-action")
                .assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun reviewOnboardingRowIsAccessibleAndDispatchesExactlyOnce(): Unit =
        runComposeUiTest {
            var dispatches = 0
            lateinit var label: String
            setContent {
                label = stringResource(SettingsRes.string.review_onboarding)
                content(onReviewOnboarding = { dispatches++ })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(
                    hasTestTag(SettingsReviewOnboardingTestTag))
            onNodeWithTag(
                    SettingsReviewOnboardingTestTag, useUnmergedTree = true)
                .assertHasClickAction()
                .assert(hasContentDescription(label))
                .assert(
                    SemanticsMatcher("button role") { node ->
                        node.config.getOrNull(SemanticsProperties.Role) ==
                            Role.Button
                    })
                .performClick()
            assertEquals(1, dispatches)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun reviewOnboardingRowIsReachableInCompactHeight(): Unit =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(400.dp, 600.dp)) {
                    content(
                        playlistSlot = {
                            BasicText("large-content", Modifier.height(1600.dp))
                        },
                        onReviewOnboarding = {})
                }
            }
            onNodeWithTag(SettingsListTestTag, useUnmergedTree = true)
                .performScrollToNode(
                    hasTestTag(SettingsReviewOnboardingTestTag))
            onNodeWithTag(
                    SettingsReviewOnboardingTestTag, useUnmergedTree = true)
                .assertExists()
                .assertHasClickAction()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics():
        Unit = runComposeUiTest {
        var behind = 0
        var visible = 0
        var terminalTop by mutableStateOf(Float.NaN)
        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().clickable { behind++ })
                content(
                    playlistSlot = {
                        Box(
                            Modifier.fillMaxWidth()
                                .height(1600.dp)
                                .testTag("settings-terminal-probe")) {
                                BasicText(
                                    "terminal-probe",
                                    Modifier.align(Alignment.BottomStart)
                                        .onGloballyPositioned {
                                            terminalTop = it.positionInRoot().y
                                        })
                            }
                    },
                    onAdd = { visible++ })
            }
        }
        onNodeWithTag(SettingsRootTestTag, useUnmergedTree = true)
            .assertNoClickOrFocusSemantics()
        onNodeWithTag(SettingsPointerShieldTestTag, useUnmergedTree = true)
            .assertNoClickOrFocusSemantics()
        onNodeWithTag(SettingsPointerShieldTestTag, useUnmergedTree = true)
            .performTouchInput { click(Offset(2f, 2f)) }
        assertEquals(0, behind)
        onNodeWithTag(SettingsListTestTag, useUnmergedTree = true)
            .performScrollToNode(hasTestTag("settings-terminal-probe"))
        waitForIdle()
        val terminalBefore = terminalTop
        onNodeWithTag(SettingsListTestTag, useUnmergedTree = true)
            .performTouchInput { swipe(center, Offset(center.x, 12f)) }
        waitForIdle()
        val terminalAfter = terminalTop
        assertEquals(true, terminalAfter < terminalBefore)
        repeat(5) {
            onNodeWithTag(SettingsListTestTag, useUnmergedTree = true)
                .performTouchInput { swipe(center, Offset(center.x, 12f)) }
        }
        waitForIdle()
        // Scroll by node so the picker is reachable regardless of how tall
        // the sections above it are.
        onNodeWithTag(SettingsListTestTag, useUnmergedTree = true)
            .performScrollToNode(hasTestTag(SettingsPickerTestTag))
        waitForIdle()
        onNodeWithTag(SettingsPickerTestTag, useUnmergedTree = true)
            .performTouchInput { click(center) }
        assertEquals(1, visible)
        assertEquals(0, behind)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun pickerIsHiddenUnavailableAndEnabledFromExplicitInputs(): Unit =
        runComposeUiTest {
            var adds = 0
            setContent {
                content(sourcePickerActionVisible = false, onAdd = { adds++ })
            }
            onNodeWithTag(SettingsPickerTestTag).assertDoesNotExist()
            setContent {
                content(sourcePickerAvailable = false, onAdd = { adds++ })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsPickerTestTag))
            onNodeWithTag(SettingsPickerTestTag)
                .assertIsNotEnabled()
                .assertHasNoClickAction()
            onNodeWithTag(SettingsPickerTestTag, useUnmergedTree = true)
                .assertHasNoClickAction()
                .assert(hasContentDescription(labels().folderPickerUnavailable))
            setContent { content(onAdd = { adds++ }) }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsPickerTestTag))
            onNodeWithTag(SettingsPickerTestTag).performClick()
            assertEquals(1, adds)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun clearIsHiddenRequestsAndRendersNullableSlot(): Unit =
        runComposeUiTest {
            var requests = 0
            setContent { content(hasTracks = false, onClear = { requests++ }) }
            onNodeWithTag(SettingsClearTestTag).assertDoesNotExist()
            setContent {
                content(
                    onClear = { requests++ },
                    clearSlot = { BasicText("clear-slot") })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsClearTestTag))
            onNodeWithTag(SettingsClearTestTag).performClick()
            onNodeWithText("clear-slot").assertExists()
            assertEquals(1, requests)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun clearLibraryActionUsesDangerPanelInBothPalettes(): Unit =
        runComposeUiTest {
            for (palette in listOf(LightHausPalette, DarkHausPalette)) {
                for (mutations in listOf(true, false)) {
                    setContent {
                        CompositionLocalProvider(
                            LocalHausColors provides palette) {
                                Box(Modifier.size(600.dp, 1_200.dp)) {
                                    content(mutations = mutations)
                                }
                            }
                    }
                    onNodeWithTag(SettingsListTestTag)
                        .performScrollToNode(hasTestTag(SettingsClearTestTag))
                    val actionNode =
                        if (mutations) onNodeWithText("Clear")
                        else onNodeWithTag(SettingsClearTestTag)
                    val pixels = actionNode.captureToImage().toPixelMap()
                    val colorCounts = mutableMapOf<Int, Int>()
                    for (y in 0 until pixels.height) {
                        for (x in 0 until pixels.width) {
                            val color = pixels[x, y].toArgb()
                            colorCounts[color] = (colorCounts[color] ?: 0) + 1
                        }
                    }
                    val dominantColor = colorCounts.maxBy { it.value }.key

                    if (mutations) {
                        assertEquals(
                            palette.dangerPanel.toArgb(),
                            dominantColor,
                            "Clear-library action should use the danger panel",
                        )
                        assertTrue(
                            palette.danger.toArgb() in colorCounts,
                            "Clear-library label should use the danger foreground",
                        )
                    } else {
                        actionNode.assertIsNotEnabled()
                        assertEquals(
                            Color(0xFFF2F2F2).toArgb(),
                            dominantColor,
                            "Disabled clear-library action should use the neutral " +
                                "disabled surface",
                        )
                    }
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun disabledMutationsDoNotDispatch(): Unit = runComposeUiTest {
        var calls = 0
        setContent {
            content(
                mutations = false,
                onAdd = { calls++ },
                onClear = { calls++ },
                sources = listOf(item()))
        }
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsPickerTestTag))
        onNodeWithTag(SettingsPickerTestTag)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        onNodeWithTag(SettingsPickerTestTag, useUnmergedTree = true)
            .assertHasNoClickAction()
            .assert(hasContentDescription(labels().addMusicFolder))
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsClearTestTag))
        onNodeWithTag(SettingsClearTestTag)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        onNodeWithTag(SettingsClearTestTag, useUnmergedTree = true)
            .assertHasNoClickAction()
            .assert(hasContentDescription(labels().clearLibrary))
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsRescanPrefix + "one"))
        onNodeWithTag(SettingsRescanPrefix + "one", useUnmergedTree = true)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        assertEquals(0, calls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun sourceRowsDispatchRescanAndRemoveById(): Unit =
        runComposeUiTest {
            var rescan = ""
            var removed = ""
            setContent {
                content(
                    sources = listOf(item()),
                    onRescan = { rescan = it },
                    onRemove = { removed = it })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsRescanPrefix + "one"))
            onNodeWithTag(SettingsRescanPrefix + "one", useUnmergedTree = true)
                .performClick()
            assertEquals("one", rescan)
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsRemoveConfirmTestTag, useUnmergedTree = true)
                .assertExists()
                .performClick()
            assertEquals("one", removed)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun lostAccessSourceRowDispatchesRecoveryAndRemoveById(): Unit =
        runComposeUiTest {
            var recovered = ""
            var removed = ""
            setContent {
                content(
                    sources =
                        listOf(
                            SettingsSourceItem(
                                "one", "One", accessAvailable = false, false)),
                    onRecover = { recovered = it },
                    onRemove = { removed = it })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsRecoverPrefix + "one"))
            onNodeWithTag(SettingsRecoverPrefix + "one", useUnmergedTree = true)
                .performClick()
            assertEquals("one", recovered)
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsRemoveConfirmTestTag, useUnmergedTree = true)
                .assertExists()
                .performClick()
            assertEquals("one", removed)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun sourceRowsRenderLocalizedPresentationWithoutSourceIds(): Unit =
        runComposeUiTest {
            var availableNever = ""
            var lostScanned = ""
            var unnamedScanned = ""
            var unnamedFolder = ""
            setContent {
                val available =
                    stringResource(SettingsRes.string.source_access_available)
                val lost = stringResource(SettingsRes.string.source_access_lost)
                val neverScanned =
                    stringResource(SettingsRes.string.source_never_scanned)
                val scanned =
                    stringResource(SettingsRes.string.source_last_scanned)
                unnamedFolder =
                    stringResource(SettingsRes.string.unnamed_folder)
                availableNever =
                    stringResource(
                        SettingsRes.string.source_status_format,
                        available,
                        neverScanned)
                lostScanned =
                    stringResource(
                        SettingsRes.string.source_status_format, lost, scanned)
                unnamedScanned =
                    stringResource(
                        SettingsRes.string.source_status_format,
                        available,
                        scanned)
                content(
                    sources =
                        listOf(
                            SettingsSourceItem(
                                "opaque-available",
                                "Available folder",
                                true,
                                false),
                            SettingsSourceItem(
                                "opaque-lost", "Lost folder", false, true),
                            SettingsSourceItem("opaque-blank", "", true, true)))
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasText("Available folder"))
            onNodeWithText("Available folder", useUnmergedTree = true)
                .assertExists()
            onNodeWithText(availableNever, useUnmergedTree = true)
                .assertExists()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasText("Lost folder"))
            onNodeWithText("Lost folder", useUnmergedTree = true).assertExists()
            onNodeWithText(lostScanned, useUnmergedTree = true).assertExists()
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasText(unnamedFolder))
            onNodeWithText(unnamedFolder, useUnmergedTree = true).assertExists()
            onNodeWithText(unnamedScanned, useUnmergedTree = true)
                .assertExists()
            onNodeWithText(
                    "opaque-available",
                    substring = true,
                    useUnmergedTree = true)
                .assertDoesNotExist()
            onNodeWithText(
                    "opaque-lost", substring = true, useUnmergedTree = true)
                .assertDoesNotExist()
            onNodeWithText(
                    "opaque-blank", substring = true, useUnmergedTree = true)
                .assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun sourceRemovalDialogOpensDismissesAndConfirms(): Unit =
        runComposeUiTest {
            val name = "folder-" + "x".repeat(80)
            val removed = mutableListOf<String>()
            setContent {
                content(
                    sources = listOf(item(name = name)),
                    onRemove = { removed += it })
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsRemovePrefix + "one"))
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithContentDescription(name, useUnmergedTree = true)
                .assertExists()
            onNodeWithTag(SettingsRemoveDismissTestTag, useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsRemoveConfirmTestTag, useUnmergedTree = true)
                .assertDoesNotExist()
            assertEquals(emptyList(), removed)
            onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
                .performClick()
            onNodeWithTag(SettingsRemoveConfirmTestTag, useUnmergedTree = true)
                .performClick()
            assertEquals(listOf("one"), removed)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun slotsRenderInPlaylistScanAndClearOrder(): Unit =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(600.dp, 1_200.dp)) {
                    content(
                        playlistSlot = { slot("playlist") },
                        scanSlot = { slot("scan") },
                        scanOutcomeSlot = { slot("outcome") },
                        clearSlot = { slot("clear") })
                }
            }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasText("playlist"))
            val renderedSlots =
                onNodeWithTag(SettingsRootTestTag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .renderedTexts()
                    .filter {
                        it == "playlist" ||
                            it == "scan" ||
                            it == "outcome" ||
                            it == "clear"
                    }
            assertEquals(
                listOf("scan", "outcome", "playlist", "clear"), renderedSlots)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun themeSelectionDispatchesSelectedMode(): Unit = runComposeUiTest {
        val selections = mutableListOf<RhythHausThemeMode>()
        var darkLabel = ""
        setContent {
            darkLabel = stringResource(SettingsRes.string.theme_dark_label)
            content(onTheme = { selections += it })
        }
        onNodeWithTag(SettingsThemeTestTag, useUnmergedTree = true)
            .assertHasClickAction()
            .performClick()
        waitForIdle()
        waitUntil(timeoutMillis = 1_000) {
            onAllNodesWithText(darkLabel, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithText(darkLabel, useUnmergedTree = true).performClick()
        assertEquals(listOf(RhythHausThemeMode.Dark), selections)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun aboutNavigationAndBackDispatchCallbacks(): Unit =
        runComposeUiTest {
            var about = 0
            var dismissed = 0
            setContent { content(onAbout = { about++ }) }
            onNodeWithTag(SettingsListTestTag)
                .performScrollToNode(hasTestTag(SettingsAboutTestTag))
            onNodeWithTag(SettingsAboutTestTag).performClick()
            assertEquals(1, about)
            var backLabel = ""
            setContent {
                backLabel = stringResource(CoreUiRes.string.back)
                SettingsAboutScreen({}, { dismissed++ })
            }
            val backControls =
                onAllNodes(
                    hasClickAction() and
                        hasAnyDescendant(hasContentDescription(backLabel)),
                    useUnmergedTree = true)
            backControls.assertCountEquals(1)
            backControls[0].performClick()
            assertEquals(1, dismissed)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun absentNotificationRecoveryRendersNoCardActionOrAccessibilityNode():
        Unit = runComposeUiTest {
        var title = ""
        var body = ""
        var requestLabel = ""
        var settingsLabel = ""
        var requests = 0
        var settings = 0
        setContent {
            title =
                stringResource(SettingsRes.string.notification_permission_title)
            body =
                stringResource(SettingsRes.string.notification_permission_body)
            requestLabel =
                stringResource(
                    SettingsRes.string.notification_permission_request)
            settingsLabel =
                stringResource(
                    SettingsRes.string.notification_permission_settings)
            content(
                notificationRecovery = null,
                onRequestNotificationPermission = { requests++ },
                onOpenNotificationSettings = { settings++ })
        }
        onNodeWithTag(SettingsNotificationRecoveryTestTag).assertDoesNotExist()
        onNodeWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertDoesNotExist()
        onAllNodesWithText(title, useUnmergedTree = true).assertCountEquals(0)
        onAllNodesWithText(body, useUnmergedTree = true).assertCountEquals(0)
        onAllNodesWithText(requestLabel, useUnmergedTree = true)
            .assertCountEquals(0)
        onAllNodesWithText(settingsLabel, useUnmergedTree = true)
            .assertCountEquals(0)
        assertEquals(0, requests)
        assertEquals(0, settings)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun requestableNotificationRecoveryRendersOneCardDispatchingRequestOnly():
        Unit = runComposeUiTest {
        var title = ""
        var body = ""
        var requestLabel = ""
        var settingsLabel = ""
        var requests = 0
        var settings = 0
        setContent {
            title =
                stringResource(SettingsRes.string.notification_permission_title)
            body =
                stringResource(SettingsRes.string.notification_permission_body)
            requestLabel =
                stringResource(
                    SettingsRes.string.notification_permission_request)
            settingsLabel =
                stringResource(
                    SettingsRes.string.notification_permission_settings)
            content(
                notificationRecovery = MediaNotificationRecovery.Requestable,
                onRequestNotificationPermission = { requests++ },
                onOpenNotificationSettings = { settings++ })
        }
        onAllNodesWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertCountEquals(1)
        onNodeWithText(title, useUnmergedTree = true).assertExists()
        onNodeWithText(body, useUnmergedTree = true).assertExists()
        onNodeWithText(requestLabel, useUnmergedTree = true).assertExists()
        onAllNodesWithText(settingsLabel, useUnmergedTree = true)
            .assertCountEquals(0)
        onNodeWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertHasClickAction()
            .performClick()
        assertEquals(1, requests)
        assertEquals(0, settings)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun settingsRequiredNotificationRecoveryRendersOneCardDispatchingSettingsOnly():
        Unit = runComposeUiTest {
        var requestLabel = ""
        var settingsLabel = ""
        var requests = 0
        var settings = 0
        setContent {
            requestLabel =
                stringResource(
                    SettingsRes.string.notification_permission_request)
            settingsLabel =
                stringResource(
                    SettingsRes.string.notification_permission_settings)
            content(
                notificationRecovery =
                    MediaNotificationRecovery.SettingsRequired,
                onRequestNotificationPermission = { requests++ },
                onOpenNotificationSettings = { settings++ })
        }
        onAllNodesWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertCountEquals(1)
        onNodeWithText(settingsLabel, useUnmergedTree = true).assertExists()
        onAllNodesWithText(requestLabel, useUnmergedTree = true)
            .assertCountEquals(0)
        onNodeWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertHasClickAction()
            .performClick()
        assertEquals(0, requests)
        assertEquals(1, settings)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun notificationRecoveryActionRemainsEnabledWhileMutationsDisabled():
        Unit = runComposeUiTest {
        var requests = 0
        var settings = 0
        setContent {
            content(
                mutations = false,
                sources = listOf(item()),
                notificationRecovery = MediaNotificationRecovery.Requestable,
                onRequestNotificationPermission = { requests++ },
                onOpenNotificationSettings = { settings++ })
        }
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(hasTestTag(SettingsPickerTestTag))
        onNodeWithTag(SettingsPickerTestTag).assertIsNotEnabled()
        onNodeWithTag(SettingsListTestTag)
            .performScrollToNode(
                hasTestTag(SettingsNotificationRecoveryTestTag))
        onNodeWithTag(
                SettingsNotificationRecoveryTestTag, useUnmergedTree = true)
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()
        assertEquals(1, requests)
        assertEquals(0, settings)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    public fun publicProjectionRendersWithoutSharedTypes(): Unit =
        runComposeUiTest {
            setContent {
                content(
                    sources =
                        listOf(SettingsSourceItem("one", "One", true, false)))
            }
            onNodeWithText("One", useUnmergedTree = true).assertExists()
        }
}

@Composable
private fun slot(name: String): Unit =
    BasicText(name, Modifier.semantics { contentDescription = name })

@Composable
private fun content(
    sourcePickerActionVisible: Boolean = true,
    sourcePickerAvailable: Boolean = true,
    replayGainMode: SettingsReplayGainMode = SettingsReplayGainMode.Off,
    replayGainBusy: Boolean = false,
    replayGainError: Boolean = false,
    crossfadeDuration: SettingsCrossfadeDuration =
        SettingsCrossfadeDuration.Off,
    crossfadeBusy: Boolean = false,
    crossfadeError: Boolean = false,
    gaplessMode: SettingsGaplessMode = SettingsGaplessMode.Off,
    gaplessBusy: Boolean = false,
    gaplessError: Boolean = false,
    mutations: Boolean = true,
    hasTracks: Boolean = true,
    sources: List<SettingsSourceItem> = emptyList(),
    playlistSlot: @Composable () -> Unit = {},
    scanSlot: (@Composable () -> Unit)? = null,
    scanOutcomeSlot: (@Composable () -> Unit)? = null,
    clearSlot: (@Composable () -> Unit)? = null,
    notificationRecovery: MediaNotificationRecovery? = null,
    mediaStoreAudioRecovery: MediaStoreAudioRecovery? = null,
    onRequestAudio: () -> Unit = {},
    onOpenAudioSettings: () -> Unit = {},
    onAddAudio: () -> Unit = {},
    onTheme: (RhythHausThemeMode) -> Unit = {},
    onReplayGain: (SettingsReplayGainMode) -> Unit = {},
    onCrossfade: (SettingsCrossfadeDuration) -> Unit = {},
    onGapless: (SettingsGaplessMode) -> Unit = {},
    onAdd: () -> Unit = {},
    onClear: () -> Unit = {},
    onRescan: (String) -> Unit = {},
    onRecover: (String) -> Unit = {},
    onRemove: (String) -> Unit = {},
    onAbout: () -> Unit = {},
    onReviewOnboarding: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onDismiss: () -> Unit = {},
): Unit =
    SettingsScreen(
        labels = labels(),
        currentThemeMode = RhythHausThemeMode.System,
        replayGainMode = replayGainMode,
        replayGainBusy = replayGainBusy,
        replayGainError = replayGainError,
        crossfadeDuration = crossfadeDuration,
        crossfadeBusy = crossfadeBusy,
        crossfadeError = crossfadeError,
        gaplessMode = gaplessMode,
        gaplessBusy = gaplessBusy,
        gaplessError = gaplessError,
        sources = sources,
        sourcePickerActionVisible = sourcePickerActionVisible,
        sourcePickerAvailable = sourcePickerAvailable,
        importMessage = null,
        mutationsEnabled = mutations,
        hasImportedTracks = hasTracks,
        playlistBackupContent = playlistSlot,
        activeScanContent = scanSlot,
        scanOutcomeContent = scanOutcomeSlot,
        clearLibraryDialog = clearSlot,
        notificationRecovery = notificationRecovery,
        mediaStoreAudioRecovery = mediaStoreAudioRecovery,
        onRequestMediaStoreAudio = onRequestAudio,
        onOpenMediaStoreSettings = onOpenAudioSettings,
        onAddMediaStoreAudio = onAddAudio,
        onThemeModeSelected = onTheme,
        onReplayGainModeSelected = onReplayGain,
        onCrossfadeDurationSelected = onCrossfade,
        onGaplessModeSelected = onGapless,
        onAddMusicFolder = onAdd,
        onRescanSource = onRescan,
        onRecoverSource = onRecover,
        onRemoveSource = onRemove,
        onRequestClearLibrary = onClear,
        onAboutClick = onAbout,
        onReviewOnboarding = onReviewOnboarding,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onDismiss = onDismiss)

private fun labels(): SettingsSharedLabels =
    SettingsSharedLabels(
        "Settings", "Add", "Unavailable", "Clear", "Cancel", "Remove")

private fun item(name: String = "One"): SettingsSourceItem =
    SettingsSourceItem("one", name, true, false)

private fun SemanticsNodeInteraction.assertNoClickOrFocusSemantics(): Unit {
    assertHasNoClickAction()
    assertEquals(
        null,
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused))
    assertEquals(
        null,
        fetchSemanticsNode().config.getOrNull(SemanticsActions.RequestFocus))
}

private fun SemanticsNode.renderedTexts(): List<String> =
    (config.getOrNull(SemanticsProperties.Text)?.map { it.text }
        ?: emptyList()) + children.flatMap(SemanticsNode::renderedTexts)
