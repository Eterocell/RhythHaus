package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eterocell.rhythhaus.theme.HausInk

/** Applies the standard bounded RhythHaus click indication. */
@Composable
public fun Modifier.hausClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = HausInk),
        onClick = onClick)
}

/** Applies the standard bounded RhythHaus click and long-click indication. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
public fun Modifier.hausCombinedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = HausInk),
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel)
}
