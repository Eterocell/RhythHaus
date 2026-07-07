package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eterocell.rhythhaus.theme.HausInk

@Composable
fun Modifier.hausClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = HausInk),
        onClick = onClick,
    )
}