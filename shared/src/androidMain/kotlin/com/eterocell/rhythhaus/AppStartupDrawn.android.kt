package com.eterocell.rhythhaus

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal actual fun WithAppStartupDrawReporter(content: @Composable () -> Unit) {
    val drawn = remember { mutableStateOf(false) }
    ReportDrawnWhen(remember(drawn) { { drawn.value } })
    CompositionLocalProvider(LocalAppStartupDrawn provides drawn, content = content)
}
