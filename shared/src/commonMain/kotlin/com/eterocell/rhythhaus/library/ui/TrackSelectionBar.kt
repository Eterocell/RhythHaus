package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.HausButton as Button
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.selection_add_to_playlist
import rhythhaus.shared.generated.resources.selection_cancel
import rhythhaus.shared.generated.resources.selection_count_many
import rhythhaus.shared.generated.resources.selection_count_one
import rhythhaus.shared.generated.resources.selection_select_all
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun TrackSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    require(selectedCount > 0)
    val countDescription =
        stringResource(
            if (selectedCount == 1) Res.string.selection_count_one
            else Res.string.selection_count_many,
            selectedCount,
        )
    val cancelDescription = stringResource(Res.string.selection_cancel)
    val selectAllDescription = stringResource(Res.string.selection_select_all)
    val addDescription = stringResource(Res.string.selection_add_to_playlist)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = HausSpacing.lg,
                    end = HausSpacing.lg,
                    bottom = HausSpacing.md,
                )
                .background(
                    HausColors.current.panel,
                    RoundedCornerShape(HausShapes.overlay),
                )
                .padding(HausSpacing.md),
        verticalArrangement = Arrangement.spacedBy(HausSpacing.md),
    ) {
        Text(
            text = countDescription,
            modifier =
                Modifier.semantics {
                    contentDescription = countDescription
                },
            color = HausColors.current.ink,
            fontSize = HausTypography.title.fontSize,
            fontWeight = HausTypography.title.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (interactive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HausSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionBarButton(
                    text = cancelDescription,
                    contentDescription = cancelDescription,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    emphasized = false,
                )
                SelectionBarButton(
                    text = selectAllDescription,
                    contentDescription = selectAllDescription,
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f),
                    emphasized = false,
                )
                SelectionBarButton(
                    text = addDescription,
                    contentDescription = addDescription,
                    onClick = onAddToPlaylist,
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun SelectionBarButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    emphasized: Boolean,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier.heightIn(min = 48.dp).semantics {
                this.contentDescription = contentDescription
            },
        cornerRadius = HausShapes.control,
        insideMargin =
            PaddingValues(
                horizontal = HausSpacing.md,
                vertical = HausSpacing.sm,
            ),
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (emphasized) HausColors.current.pulse
                    else HausColors.current.panelStrong,
                contentColor =
                    if (emphasized) HausColors.current.onPulse
                    else HausColors.current.ink,
            ),
    ) {
        Text(
            text,
            fontSize = HausTypography.label.fontSize,
            fontWeight = HausTypography.label.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
