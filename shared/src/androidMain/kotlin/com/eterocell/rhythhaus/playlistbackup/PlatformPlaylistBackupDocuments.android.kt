package com.eterocell.rhythhaus.playlistbackup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@Composable
actual fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlatformPlaylistBackupDocumentLauncher {
    val context = LocalContext.current
    val currentSaveResult = rememberUpdatedState(onSaveResult)
    val currentOpenResult = rememberUpdatedState(onOpenResult)
    val pendingSaveBytes = remember { arrayOfNulls<ByteArray>(1) }
    val operationGate = remember { PlaylistBackupDocumentOperationGate() }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PlaylistBackupMimeType)) { uri ->
        val bytes = pendingSaveBytes[0]
        pendingSaveBytes[0] = null
        operationGate.finish()
        currentSaveResult.value(
            if (bytes == null) {
                PlaylistBackupDocumentSaveResult.Failure("No playlist backup payload was pending")
            } else {
                saveAndroidPlaylistBackupDocument(uri, bytes) { selected ->
                    context.contentResolver.openOutputStream(selected, "wt")
                }
            },
        )
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        operationGate.finish()
        currentOpenResult.value(
            openAndroidPlaylistBackupDocument(uri) { selected -> context.contentResolver.openInputStream(selected) },
        )
    }

    return remember(saveLauncher, openLauncher) {
        object : PlatformPlaylistBackupDocumentLauncher {
            override val isAvailable: Boolean = true

            override fun save(suggestedFileName: String, bytes: ByteArray) {
                if (!operationGate.tryStart()) {
                    currentSaveResult.value(PlaylistBackupDocumentSaveResult.Unavailable("Another document operation is active"))
                    return
                }
                pendingSaveBytes[0] = bytes
                try {
                    saveLauncher.launch(playlistBackupFileName(suggestedFileName))
                } catch (exception: Exception) {
                    pendingSaveBytes[0] = null
                    operationGate.finish()
                    currentSaveResult.value(
                        PlaylistBackupDocumentSaveResult.Failure(exception.message ?: "Could not present save document panel"),
                    )
                }
            }

            override fun open() {
                if (!operationGate.tryStart()) {
                    currentOpenResult.value(PlaylistBackupDocumentOpenResult.Unavailable("Another document operation is active"))
                    return
                }
                try {
                    openLauncher.launch(arrayOf(PlaylistBackupMimeType, PlaylistBackupJsonMimeType))
                } catch (exception: Exception) {
                    operationGate.finish()
                    currentOpenResult.value(
                        PlaylistBackupDocumentOpenResult.Failure(exception.message ?: "Could not present open document panel"),
                    )
                }
            }
        }
    }
}

internal fun <T> saveAndroidPlaylistBackupDocument(
    uri: T?,
    bytes: ByteArray,
    openOutput: (T) -> OutputStream?,
): PlaylistBackupDocumentSaveResult {
    if (uri == null) return PlaylistBackupDocumentSaveResult.Cancelled
    return try {
        val output = openOutput(uri) ?: return PlaylistBackupDocumentSaveResult.Unavailable("Selected document cannot be written")
        output.use { it.write(bytes) }
        PlaylistBackupDocumentSaveResult.Success
    } catch (exception: Exception) {
        PlaylistBackupDocumentSaveResult.Failure(exception.message ?: "Could not save playlist backup")
    }
}

internal fun <T> openAndroidPlaylistBackupDocument(
    uri: T?,
    openInput: (T) -> InputStream?,
): PlaylistBackupDocumentOpenResult {
    if (uri == null) return PlaylistBackupDocumentOpenResult.Cancelled
    return try {
        val input = openInput(uri) ?: return PlaylistBackupDocumentOpenResult.Unavailable("Selected document cannot be read")
        val bytes = input.use(::readAndroidPlaylistBackupBounded)
        if (bytes.size > PlaylistBackupMaxBytes) {
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes)
        } else {
            PlaylistBackupDocumentOpenResult.Success(bytes)
        }
    } catch (exception: Exception) {
        PlaylistBackupDocumentOpenResult.Failure(exception.message ?: "Could not open playlist backup")
    }
}

internal fun readAndroidPlaylistBackupBounded(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var remaining = PlaylistBackupMaxBytes + 1
    while (remaining > 0) {
        val count = input.read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) break
        if (count == 0) {
            val byte = input.read()
            if (byte < 0) break
            output.write(byte)
            remaining -= 1
            continue
        }
        output.write(buffer, 0, count)
        remaining -= count
    }
    return output.toByteArray()
}
