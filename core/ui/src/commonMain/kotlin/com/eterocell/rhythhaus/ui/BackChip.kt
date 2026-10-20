package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import com.eterocell.rhythhaus.theme.HausTypography
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res
import rhythhaus.core.ui.generated.resources.back
import rhythhaus.core.ui.generated.resources.back_button
import top.yukonga.miuix.kmp.basic.Text

private val BackChipShape = RoundedCornerShape(HausShapes.control)

/** Renders the compact Back control with its existing accessibility label. */
@Composable
public fun BackChip(onClick: () -> Unit, modifier: Modifier = Modifier): Unit {
    val backContentDescription = stringResource(Res.string.back)
    Box(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .clip(BackChipShape)
                .hausClickable(onClick = onClick)
                .semantics { contentDescription = backContentDescription },
        contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier.background(
                            HausColors.current.navigation, BackChipShape)
                        .padding(
                            horizontal = HausSpacing.md,
                            vertical = HausSpacing.sm,
                        ),
                contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.back_button),
                        color = HausColors.current.ink,
                        fontSize = HausTypography.label.fontSize,
                        fontWeight = HausTypography.label.fontWeight)
                }
        }
}
