package com.eterocell.rhythhaus

import androidx.compose.ui.window.ComposeUIViewController
import com.eterocell.rhythhaus.di.startRhythHausKoin
import platform.UIKit.UIViewController

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    startRhythHausKoin()
    return ComposeUIViewController { App() }
}
