package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalStartupLibraryReady = compositionLocalOf { false }
internal val LocalAppStartupDrawn =
    staticCompositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun MarkAppStartupDrawn() {
    val drawn = LocalAppStartupDrawn.current ?: return
    if (!drawn.value) SideEffect { drawn.value = true }
}

@Composable
internal expect fun WithAppStartupDrawReporter(content: @Composable () -> Unit)
