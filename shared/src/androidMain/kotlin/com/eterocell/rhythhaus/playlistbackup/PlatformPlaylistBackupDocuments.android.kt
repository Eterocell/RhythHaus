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
    val coordinator = remember {
        AndroidPlaylistBackupDocumentCoordinator(
            onSaveResult = { currentSaveResult.value(it) },
            onOpenResult = { currentOpenResult.value(it) },
        )
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PlaylistBackupMimeType)) { uri ->
        coordinator.completeSave(uri) { selected, _ -> context.contentResolver.openOutputStream(selected, "wt") }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        coordinator.completeOpen(uri) { selected -> context.contentResolver.openInputStream(selected) }
    }

    return remember(saveLauncher, openLauncher) {
        object : PlatformPlaylistBackupDocumentLauncher {
            override val isAvailable: Boolean = true

            override fun save(suggestedFileName: String, bytes: ByteArray) {
                coordinator.launchSave(bytes) {
                    saveLauncher.launch(playlistBackupFileName(suggestedFileName))
                }
            }

            override fun open() {
                coordinator.launchOpen {
                    openLauncher.launch(arrayOf(PlaylistBackupMimeType, PlaylistBackupJsonMimeType))
                }
            }
        }
    }
}

internal class AndroidPlaylistBackupDocumentCoordinator(
    private val onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    private val onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
) {
    private val operationGate = PlaylistBackupDocumentOperationGate()
    private var pendingSaveBytes: ByteArray? = null

    val isActive: Boolean
        get() = operationGate.isActive
    val hasPendingSavePayload: Boolean
        get() = pendingSaveBytes != null

    fun launchSave(bytes: ByteArray, launch: () -> Unit) {
        if (!operationGate.tryStart()) {
            onSaveResult(PlaylistBackupDocumentSaveResult.Unavailable("Another document operation is active"))
            return
        }
        pendingSaveBytes = bytes
        try {
            launch()
        } catch (exception: Exception) {
            pendingSaveBytes = null
            operationGate.finish()
            onSaveResult(PlaylistBackupDocumentSaveResult.Failure(exception.message ?: "Could not present save document panel"))
        }
    }

    fun launchOpen(launch: () -> Unit) {
        if (!operationGate.tryStart()) {
            onOpenResult(PlaylistBackupDocumentOpenResult.Unavailable("Another document operation is active"))
            return
        }
        try {
            launch()
        } catch (exception: Exception) {
            operationGate.finish()
            onOpenResult(PlaylistBackupDocumentOpenResult.Failure(exception.message ?: "Could not present open document panel"))
        }
    }

    fun <T> completeSave(uri: T?, openOutput: (T, ByteArray) -> OutputStream?) {
        val bytes = pendingSaveBytes
        pendingSaveBytes = null
        operationGate.finish()
        onSaveResult(
            if (bytes == null) {
                PlaylistBackupDocumentSaveResult.Failure("No playlist backup payload was pending")
            } else {
                saveAndroidPlaylistBackupDocument(uri, bytes) { selected -> openOutput(selected, bytes) }
            },
        )
    }

    fun <T> completeOpen(uri: T?, openInput: (T) -> InputStream?) {
        operationGate.finish()
        onOpenResult(openAndroidPlaylistBackupDocument(uri, openInput))
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
