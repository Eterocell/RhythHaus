package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher = remember(onResult) {
    object : AudioImportLauncher {
        override val isAvailable: Boolean = true

        override fun launch() {
            try {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Import local audio"
                    isMultiSelectionEnabled = true
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    fileFilter = FileNameExtensionFilter(
                        "Audio files (wav, aiff, au, mp3, m4a, flac, ogg)",
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
                }
                val selection = chooser.showOpenDialog(null)
                if (selection != JFileChooser.APPROVE_OPTION) {
                    onResult(AudioImportResult.Success(emptyList()))
                    return
                }
                onResult(AudioImportResult.Success(chooser.selectedFiles.map { it.toImportedAudioFile() }))
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

private fun File.toImportedAudioFile(): ImportedAudioFile = ImportedAudioFile(
    displayName = name,
    source = AudioSource.FilePath(absolutePath),
)
