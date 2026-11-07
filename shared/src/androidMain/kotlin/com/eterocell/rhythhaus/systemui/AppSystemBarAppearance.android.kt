package com.eterocell.rhythhaus.systemui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun AppSystemBarAppearance(
    statusBarSurface: Color,
    navigationBarSurface: Color,
) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivityWindow() }
    val controller =
        remember(window, view) {
            window?.let { WindowCompat.getInsetsController(it, view) }
        }
    val darkStatusBarIcons = !systemBarUsesLightIcons(statusBarSurface)
    val darkNavigationBarIcons = !systemBarUsesLightIcons(navigationBarSurface)
    SideEffect {
        if (controller != null) {
            if (controller.isAppearanceLightStatusBars != darkStatusBarIcons) {
                controller.isAppearanceLightStatusBars = darkStatusBarIcons
            }
            if (controller.isAppearanceLightNavigationBars !=
                darkNavigationBarIcons) {
                controller.isAppearanceLightNavigationBars =
                    darkNavigationBarIcons
            }
        }
    }
}

private fun Context.findActivityWindow(): android.view.Window? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current.window
        val wrapper = current as? ContextWrapper ?: return null
        val baseContext = wrapper.baseContext
        if (baseContext === current) return null
        current = baseContext
    }
    return null
}
