package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
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
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
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
    public fun reviewOnboardingRowIsAccessibleAndDispatchesExactlyOnce(): Unit =
        runComposeUiTest {
            var dispatches = 0
            lateinit var label: String
            setContent {
                label = stringResource(SettingsRes.string.review_onboarding)
                content(onReviewOnboarding = { dispatches++ })
            }
            onNodeWithTag(SettingsReviewOnboardingTestTag, useUnmergedTree = true)
                .assertHasClickAction()
                .assert(hasContentDescription(label))
                .assert(
                    SemanticsMatcher("button role") { node ->
                        node.config.getOrNull(SemanticsProperties.Role) == Role.Button
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
                .performScrollToNode(hasTestTag(SettingsReviewOnboardingTestTag))
            onNodeWithTag(SettingsReviewOnboardingTestTag, useUnmergedTree = true)
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
            onNodeWithTag(SettingsPickerTestTag)
                .assertIsNotEnabled()
                .assertHasNoClickAction()
            onNodeWithTag(SettingsPickerTestTag, useUnmergedTree = true)
                .assertHasNoClickAction()
                .assert(hasContentDescription(labels().folderPickerUnavailable))
            setContent { content(onAdd = { adds++ }) }
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
            onNodeWithTag(SettingsClearTestTag).performClick()
            onNodeWithText("clear-slot").assertExists()
            assertEquals(1, requests)
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
        onNodeWithTag(SettingsPickerTestTag)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        onNodeWithTag(SettingsPickerTestTag, useUnmergedTree = true)
            .assertHasNoClickAction()
            .assert(hasContentDescription(labels().addMusicFolder))
        onNodeWithTag(SettingsClearTestTag)
            .assertIsNotEnabled()
            .assertHasNoClickAction()
        onNodeWithTag(SettingsClearTestTag, useUnmergedTree = true)
            .assertHasNoClickAction()
            .assert(hasContentDescription(labels().clearLibrary))
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
            onNodeWithText("Available folder", useUnmergedTree = true)
                .assertExists()
            onNodeWithText(availableNever, useUnmergedTree = true)
                .assertExists()
            onNodeWithText("Lost folder", useUnmergedTree = true).assertExists()
            onNodeWithText(lostScanned, useUnmergedTree = true).assertExists()
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
                content(
                    playlistSlot = { slot("playlist") },
                    scanSlot = { slot("scan") },
                    scanOutcomeSlot = { slot("outcome") },
                    clearSlot = { slot("clear") })
            }
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
        onNodeWithTag(SettingsPickerTestTag).assertIsNotEnabled()
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
    mutations: Boolean = true,
    hasTracks: Boolean = true,
    sources: List<SettingsSourceItem> = emptyList(),
    playlistSlot: @Composable () -> Unit = {},
    scanSlot: (@Composable () -> Unit)? = null,
    scanOutcomeSlot: (@Composable () -> Unit)? = null,
    clearSlot: (@Composable () -> Unit)? = null,
    notificationRecovery: MediaNotificationRecovery? = null,
    onTheme: (RhythHausThemeMode) -> Unit = {},
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
        onThemeModeSelected = onTheme,
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
}

private fun SemanticsNode.renderedTexts(): List<String> =
    (config.getOrNull(SemanticsProperties.Text)?.map { it.text }
        ?: emptyList()) + children.flatMap(SemanticsNode::renderedTexts)
