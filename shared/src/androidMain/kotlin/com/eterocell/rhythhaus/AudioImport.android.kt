package com.eterocell.rhythhaus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val files = uris.map { uri ->
            ImportedAudioFile(
                displayName = uri.lastPathSegment ?: uri.toString().substringAfterLast('/'),
                source = AudioSource.Uri(uri.toString()),
            )
        }
        onResult(AudioImportResult.Success(files))
    }

    return remember(launcher, onResult) {
        object : AudioImportLauncher {
            override val isAvailable: Boolean = true
            override fun launch() {
                launcher.launch(arrayOf("audio/*"))
            }
        }
    }
}
