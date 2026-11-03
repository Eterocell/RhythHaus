package com.eterocell.rhythhaus.theme

import androidx.compose.runtime.Composable

/** Whether the current platform can provide wallpaper-derived appearance. */
expect fun isPlatformDynamicColorAvailable(): Boolean

/** Current platform color roles for the resolved light or dark appearance. */
@Composable
expect fun platformDynamicColorRoles(isDark: Boolean): HausDynamicColorRoles?
