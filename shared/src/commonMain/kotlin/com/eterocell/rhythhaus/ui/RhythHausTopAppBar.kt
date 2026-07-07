package com.eterocell.rhythhaus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.back
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = HausColors.current.paper,
        titleColor = HausColors.current.ink,
        subtitle = subtitle,
        subtitleColor = HausColors.current.muted,
        defaultWindowInsetsPadding = false,
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    backgroundColor = Color.Transparent,
                    minWidth = 44.dp,
                    minHeight = 44.dp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = HausColors.current.ink,
                    )
                }
            }
        },
    )
}
