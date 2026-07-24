package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performSemanticsAction
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
                if (visible) {
                    HausDialog(
                        title = "Dialog",
                        dismissLabel = "Cancel",
                        onDismiss = {
                            dismissCount += 1
                            visible = false
                        },
                        body = {},
                        actions = {},
                    )
                }
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
                    },
                )
                .performSemanticsAction(SemanticsActions.Dismiss)
            waitForIdle()

            assertFalse(visible)
            assertEquals(1, dismissCount)
            onNode(dismissMatcher).assertDoesNotExist()
        }
}
