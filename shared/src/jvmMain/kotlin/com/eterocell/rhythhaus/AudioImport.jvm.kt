package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher = remember(onResult) {
    object : AudioImportLauncher {
        override val isAvailable: Boolean = true

        override fun launch() {
            try {
                val selectedFiles = openNativeAudioFileDialog()
                onResult(AudioImportResult.Success(selectedFiles.map { it.toImportedAudioFile() }))
            } catch (throwable: Throwable) {
                onResult(
                    AudioImportResult.Failure(
                        message = "Could not import local audio files",
                        cause = throwable.message ?: throwable::class.simpleName,
                    ),
                )
            }
        }
    }
}

private fun openNativeAudioFileDialog(): List<File> {
    val dialog = FileDialog(null as Frame?, "Import local audio", FileDialog.LOAD).apply {
        isMultipleMode = true
        filenameFilter = { _, name -> name.hasAudioExtension() }
    }
    dialog.isVisible = true

    return dialog.files?.toList().orEmpty()
}

private fun String.hasAudioExtension(): Boolean {
    val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedAudioExtensions
}

private val supportedAudioExtensions = setOf(
    "wav",
    "wave",
    "aif",
    "aiff",
    "au",
    "mp3",
    "m4a",
    "aac",
    "flac",
    "ogg",
)

private fun File.toImportedAudioFile(): ImportedAudioFile = ImportedAudioFile(
    displayName = name,
    source = AudioSource.FilePath(absolutePath),
)
