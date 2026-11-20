package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable

@Composable
internal actual fun WithAppStartupDrawReporter(content: @Composable () -> Unit) = content()
