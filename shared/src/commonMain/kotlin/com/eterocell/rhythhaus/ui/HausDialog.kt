package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.HausColorPalette
import com.eterocell.rhythhaus.theme.HausColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults

internal data class HausDialogPresentation(
    val panelColor: Color,
    val scrimColor: Color,
    val outerPadding: Dp = 24.dp,
    val cornerRadius: Dp = 24.dp,
    val maxPanelHeight: Dp = 480.dp,
)

private const val DarkHausDialogScrimAlpha = 0.72f

internal fun hausDialogPresentation(palette: HausColorPalette): HausDialogPresentation = HausDialogPresentation(
    panelColor = palette.panel,
    scrimColor = if (palette == DarkHausPalette) {
        palette.paper.copy(alpha = DarkHausDialogScrimAlpha)
    } else {
        palette.ink.copy(alpha = 0.36f)
    },
)

@Composable
internal fun HausDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val presentation = hausDialogPresentation(HausColors.current)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(presentation.scrimColor)
            .pointerInput(onDismiss) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .semantics {
                dialog()
                paneTitle = title
                dismiss(label = dismissLabel) {
                    onDismiss()
                    true
                }
            }
            .padding(presentation.outerPadding),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = presentation.maxPanelHeight)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            cornerRadius = presentation.cornerRadius,
            colors = CardDefaults.defaultColors(color = presentation.panelColor),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = body,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    content = actions,
                )
            }
        }
    }
}

@Composable
internal fun HausLazyDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    bodyModifier: Modifier = Modifier,
    body: LazyListScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val presentation = hausDialogPresentation(HausColors.current)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(presentation.scrimColor)
            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
            .semantics {
                dialog()
                paneTitle = title
                dismiss(label = dismissLabel) {
                    onDismiss()
                    true
                }
            }
            .padding(presentation.outerPadding),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = presentation.maxPanelHeight)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            cornerRadius = presentation.cornerRadius,
            colors = CardDefaults.defaultColors(color = presentation.panelColor),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                LazyColumn(
                    modifier = bodyModifier.fillMaxWidth().weight(1f, fill = false),
                    content = body,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    content = actions,
                )
            }
        }
    }
}
