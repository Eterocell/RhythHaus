package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher = remember(onResult) {
    object : AudioImportLauncher {
        override val isAvailable: Boolean = false
        override fun launch() {
            onResult(
                AudioImportResult.Unavailable(
                    "iOS import needs a document-picker bridge. Playback is ready once a file URL is supplied.",
                ),
            )
        }
    }
}
