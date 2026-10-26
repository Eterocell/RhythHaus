package com.eterocell.rhythhaus.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HausDialogSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dismissSemanticsPreservesLabelAndInvokesTheExistingDismissCallbackOnce() =
        runComposeUiTest {
            var visible by mutableStateOf(true)
            var dismissCount = 0
            setContent {
                if (visible)
                    HausDialog(
                        title = "Dialog",
                        dismissLabel = "Cancel",
                        onDismiss = {
                            dismissCount += 1
                            visible = false
                        },
                        body = {},
                        actions = {})
            }
            val dismissMatcher =
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
            onNode(dismissMatcher)
                .assertExists()
                .assert(
                    SemanticsMatcher("Dismiss action label is Cancel") { node ->
                        node.config
                            .getOrNull(SemanticsActions.Dismiss)
                            ?.label == "Cancel"
                    })
                .performSemanticsAction(SemanticsActions.Dismiss)
            waitForIdle()
            assertFalse(visible)
            assertEquals(1, dismissCount)
            onNode(dismissMatcher).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun foregroundDialogOwnsSemanticsAndRestoresFocusToItsInvoker() =
        runComposeUiTest {
            val invokerFocusRequester = FocusRequester()
            lateinit var publishOverlay: HausWindowOverlayPublisher
            var invokerActivations = 0
            var panelActivations = 0
            var dialogDismissals = 0
            setContent {
                HausWindowOverlayHost {
                    publishOverlay =
                        checkNotNull(LocalHausWindowOverlayPublisher.current)
                    Button(
                        modifier =
                            Modifier.testTag("dialog-invoker")
                                .focusRequester(invokerFocusRequester),
                        onClick = {
                            publishOverlay.publish(
                                {
                                    HausDialog(
                                        title = "Foreground dialog",
                                        dismissLabel = "Close",
                                        onDismiss = {
                                            dialogDismissals += 1
                                            publishOverlay.publish(null, null)
                                        },
                                        body = {
                                            Button(
                                                modifier =
                                                    Modifier.testTag(
                                                        "dialog-panel-action"),
                                                onClick = {
                                                    panelActivations += 1
                                                },
                                            ) {
                                                Text("Panel action")
                                            }
                                        },
                                        actions = {},
                                    )
                                },
                                null)
                            invokerActivations += 1
                        },
                    ) {
                        Text("Open dialog")
                    }
                }
            }

            runOnIdle { invokerFocusRequester.requestFocus() }
            val invokerBounds =
                onNodeWithTag("dialog-invoker")
                    .fetchSemanticsNode()
                    .boundsInRoot
            onNodeWithTag("dialog-invoker").assertIsFocused().performClick()
            waitForIdle()

            onNodeWithTag("dialog-invoker").assertDoesNotExist()
            val dialogMatcher =
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
            onNode(dialogMatcher, useUnmergedTree = true)
                .assertExists()
                .assertIsFocused()
            onNodeWithTag("dialog-panel-action").performTouchInput {
                click()
            }
            waitForIdle()
            assertEquals(1, panelActivations)
            assertEquals(0, dialogDismissals)
            onRoot().performTouchInput {
                click(
                    Offset(
                        invokerBounds.center.x,
                        invokerBounds.center.y,
                    ),
                )
            }
            waitForIdle()

            assertEquals(1, invokerActivations)
            assertEquals(1, dialogDismissals)
            onNodeWithTag("dialog-invoker").assertIsFocused()

            onNodeWithTag("dialog-invoker").performClick()
            onNode(dialogMatcher, useUnmergedTree = true)
                .assertIsFocused()
                .performSemanticsAction(SemanticsActions.Dismiss)
            waitForIdle()
            assertEquals(2, invokerActivations)
            assertEquals(2, dialogDismissals)
            onNodeWithTag("dialog-invoker").assertIsFocused()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inlineDialogUsesWindowHostAndRestoresFocusToItsInvoker() =
        runComposeUiTest {
            val invokerFocusRequester = FocusRequester()
            var dialogVisible by mutableStateOf(false)
            setContent {
                HausWindowOverlayHost {
                    Button(
                        modifier =
                            Modifier.testTag("inline-dialog-invoker")
                                .focusRequester(invokerFocusRequester),
                        onClick = { dialogVisible = true },
                    ) {
                        Text("Open inline dialog")
                    }
                    if (dialogVisible) {
                        HausDialog(
                            title = "Inline dialog",
                            dismissLabel = "Close",
                            onDismiss = { dialogVisible = false },
                            body = {},
                            actions = {},
                        )
                    }
                }
            }

            runOnIdle { invokerFocusRequester.requestFocus() }
            onNodeWithTag("inline-dialog-invoker")
                .assertIsFocused()
                .performClick()
            waitForIdle()

            onNodeWithTag("inline-dialog-invoker").assertDoesNotExist()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
                .assertIsFocused()
                .performSemanticsAction(SemanticsActions.Dismiss)
            waitForIdle()

            onNodeWithTag("inline-dialog-invoker").assertIsFocused()
        }
}
