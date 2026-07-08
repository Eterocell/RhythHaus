package com.eterocell.rhythhaus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.eterocell.rhythhaus.di.startRhythHausKoin

fun main() {
    startRhythHausKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RhythHaus",
        ) {
            App()
        }
    }
}
