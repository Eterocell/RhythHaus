package com.eterocell.rhythhaus.nowplaying

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun TestQueueContent(onClose: () -> Unit) {
    Column {
        Text("Queue fixture")
        Button(onClick = onClose) {
            Text("Back to player")
        }
    }
}
