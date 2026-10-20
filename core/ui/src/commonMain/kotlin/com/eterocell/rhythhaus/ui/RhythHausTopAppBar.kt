package com.eterocell.rhythhaus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausSpacing
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res
import rhythhaus.core.ui.generated.resources.back
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

/** Renders the standard small RhythHaus top app bar. */
@Composable
public fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    color: Color = HausColors.current.navigation,
    titleColor: Color = HausColors.current.ink,
    subtitleColor: Color = HausColors.current.muted,
    defaultWindowInsetsPadding: Boolean = false,
    titlePadding: Dp = HausSpacing.lg,
    navigationIconPadding: Dp = HausSpacing.sm,
    actionIconPadding: Dp = HausSpacing.sm
): Unit {
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = color,
        titleColor = titleColor,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = titlePadding,
        navigationIconPadding = navigationIconPadding,
        actionIconPadding = actionIconPadding,
        navigationIcon = {
            if (onBack != null)
                IconButton(
                    onClick = onBack,
                    backgroundColor = Color.Transparent,
                    minWidth = 48.dp,
                    minHeight = 48.dp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                stringResource(Res.string.back),
                            tint = titleColor)
                    }
        })
}
