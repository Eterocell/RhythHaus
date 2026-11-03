package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable

actual fun isPlatformDynamicColorAvailable(): Boolean = false

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun platformDynamicColorRoles(isDark: Boolean): HausDynamicColorRoles? =
    null
