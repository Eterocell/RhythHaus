package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.HausColorPalette
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults

internal data class HausDialogPresentation(
    val panelColor: Color,
    val scrimColor: Color,
    val outerPadding: Dp = HausSpacing.xl,
    val cornerRadius: Dp = HausShapes.overlay,
    val maxPanelHeight: Dp = 480.dp
)

private const val DarkHausDialogScrimAlpha = 0.72f

internal fun hausDialogPresentation(
    palette: HausColorPalette
): HausDialogPresentation =
    HausDialogPresentation(
        palette.panel,
        if (palette == DarkHausPalette)
            palette.paper.copy(alpha = DarkHausDialogScrimAlpha)
        else palette.ink.copy(alpha = 0.36f))

/**
 * Presents a standard RhythHaus dialog while retaining its established dismiss
 * semantics.
 *
 * [maxPanelWidth] constrains wide editors and [maxPanelHeight] allows compact
 * editors to use the available height. The body scrolls without moving actions.
 */
@NonSkippableComposable
@Composable
public fun HausDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    maxPanelWidth: Dp = Dp.Infinity,
    maxPanelHeight: Dp = 480.dp,
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
): Unit =
    HausDialogContent(
        title,
        onDismiss,
        dismissLabel,
        maxPanelWidth,
        maxPanelHeight,
        body,
        actions)

/**
 * Presents a scrollable RhythHaus dialog while retaining its established
 * dismiss semantics.
 */
@NonSkippableComposable
@Composable
public fun HausLazyDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    bodyModifier: Modifier = Modifier,
    body: LazyListScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
): Unit {
    val presentation = hausDialogPresentation(HausColors.current)
    PresentHausDialog {
        val focusRequester = rememberHausDialogFocusRequester()
        Box(
            modifier =
                dialogModifier(
                    title,
                    onDismiss,
                    dismissLabel,
                    presentation,
                    focusRequester,
                ),
            contentAlignment = Alignment.Center) {
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = presentation.maxPanelHeight)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {})
                            },
                    cornerRadius = presentation.cornerRadius,
                    colors =
                        CardDefaults.defaultColors(
                            color = presentation.panelColor)) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(HausSpacing.xl)) {
                                LazyColumn(
                                    modifier =
                                        bodyModifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false),
                                    content = body)
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(top = HausSpacing.lg),
                                    content = actions)
                            }
                    }
            }
    }
}

@NonSkippableComposable
@Composable
private fun HausDialogContent(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String?,
    maxPanelWidth: Dp,
    maxPanelHeight: Dp,
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
): Unit {
    val presentation = hausDialogPresentation(HausColors.current)
    PresentHausDialog {
        val focusRequester = rememberHausDialogFocusRequester()
        Box(
            modifier =
                dialogModifier(
                    title,
                    onDismiss,
                    dismissLabel,
                    presentation,
                    focusRequester,
                ),
            contentAlignment = Alignment.Center) {
                Card(
                    modifier =
                        Modifier.widthIn(max = maxPanelWidth)
                            .fillMaxWidth()
                            .heightIn(max = maxPanelHeight)
                            .testTag("HausDialogPanel")
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {})
                            },
                    cornerRadius = presentation.cornerRadius,
                    colors =
                        CardDefaults.defaultColors(
                            color = presentation.panelColor)) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(HausSpacing.xl)) {
                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .verticalScroll(
                                                rememberScrollState()),
                                    content = body)
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(top = HausSpacing.lg),
                                    content = actions)
                            }
                    }
            }
    }
}

@NonSkippableComposable
@Composable
private fun PresentHausDialog(content: @Composable () -> Unit) {
    val publisher = LocalHausWindowOverlayPublisher.current
    if (publisher == null) {
        content()
        return
    }
    // The host stores this lambda outside this call site; republish even when
    // Compose keeps the lambda identity stable across captured-state updates.
    SideEffect {
        publisher.publish(content, null)
    }
    DisposableEffect(publisher) {
        onDispose { publisher.publish(null, null) }
    }
}

@Composable
private fun rememberHausDialogFocusRequester(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    return focusRequester
}

private fun dialogModifier(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String?,
    presentation: HausDialogPresentation,
    focusRequester: FocusRequester,
): Modifier =
    Modifier.fillMaxSize()
        .background(presentation.scrimColor)
        .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
        .focusRequester(focusRequester)
        .focusable()
        .semantics {
            dialog()
            paneTitle = title
            dismiss(label = dismissLabel) {
                onDismiss()
                true
            }
        }
        .padding(presentation.outerPadding)
