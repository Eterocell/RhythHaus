package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausIconButton
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res as CoreUiRes
import rhythhaus.core.ui.generated.resources.back
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.more_actions

internal data class AppShellRouteHeader(
    val title: String,
    val onBack: (() -> Unit)?,
    val actions: List<AppShellHeaderAction>,
)

internal data class AppShellHeaderAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

private const val VisibleHeaderActionCount = 2

/**
 * Renders the Shared route header at the shell's text-scale-aware height, so
 * the reserved content region beneath it always matches the rendered chrome.
 */
@Composable
internal fun AppShellHeader(
    model: AppShellRouteHeader,
    layout: AppNavigationLayout,
    safeTopInset: Dp,
    modifier: Modifier = Modifier,
) {
    val headerHeight = appShellHeaderHeight(layout)
    val titleStyle = appShellHeaderTitleStyle(layout)
    val moreActionsLabel = stringResource(Res.string.more_actions)
    var overflowExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(headerHeight + safeTopInset)
                .padding(top = safeTopInset)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        model.onBack?.let { onBack ->
            HausIconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("AppShellHeaderBack"),
                backgroundColor = Color.Transparent,
                minWidth = 48.dp,
                minHeight = 48.dp,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreUiRes.string.back),
                    tint = HausColors.current.textPrimary,
                )
            }
        }

        Text(
            text = model.title,
            modifier =
                Modifier.weight(1f)
                    .semantics {
                        contentDescription = model.title
                    }
                    .testTag("AppShellHeaderTitle"),
            color = HausColors.current.textPrimary,
            style = titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        for (index in
            0 until minOf(model.actions.size, VisibleHeaderActionCount)) {
            val action = model.actions[index]
            HausIconButton(
                onClick = action.onClick,
                enabled = action.enabled,
                modifier =
                    Modifier.size(48.dp)
                        .testTag(
                            action.testTag ?: "AppShellHeaderAction$index"),
                backgroundColor = Color.Transparent,
                minWidth = 48.dp,
                minHeight = 48.dp,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = HausColors.current.textPrimary,
                )
            }
        }

        if (model.actions.size > VisibleHeaderActionCount) {
            HausIconButton(
                onClick = { overflowExpanded = true },
                modifier =
                    Modifier.size(48.dp).testTag("AppShellHeaderOverflow"),
                backgroundColor = Color.Transparent,
                minWidth = 48.dp,
                minHeight = 48.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = moreActionsLabel,
                    tint = HausColors.current.textPrimary,
                )
            }
            DropdownMenu(
                modifier = Modifier.semantics { paneTitle = moreActionsLabel },
                expanded = overflowExpanded,
                onDismissRequest = { overflowExpanded = false },
            ) {
                for (index in
                    VisibleHeaderActionCount until model.actions.size) {
                    val action = model.actions[index]
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        enabled = action.enabled,
                        onClick = {
                            overflowExpanded = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}
