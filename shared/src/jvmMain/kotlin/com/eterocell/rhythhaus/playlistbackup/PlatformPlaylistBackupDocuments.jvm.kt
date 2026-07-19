package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

@Composable
actual fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlatformPlaylistBackupDocumentLauncher = remember(onSaveResult, onOpenResult) {
    object : PlatformPlaylistBackupDocumentLauncher {
        override val isAvailable: Boolean = true

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            onSaveResult(
                saveJvmPlaylistBackupDocument(
                    bytes = bytes,
                    selectFile = { openJvmPlaylistBackupDialog(FileDialog.SAVE, playlistBackupFileName(suggestedFileName)) },
                    writeFile = { file, payload -> file.outputStream().use { it.write(payload) } },
                ),
            )
        }

        override fun open() {
            onOpenResult(
                openJvmPlaylistBackupDocument(
                    selectFile = { openJvmPlaylistBackupDialog(FileDialog.LOAD, null) },
                    readFile = { file -> file.inputStream().use(::readJvmPlaylistBackupBounded) },
                ),
            )
        }
    }
}

internal fun saveJvmPlaylistBackupDocument(
    bytes: ByteArray,
    selectFile: () -> File?,
    writeFile: (File, ByteArray) -> Unit,
): PlaylistBackupDocumentSaveResult = try {
    val selected = selectFile() ?: return PlaylistBackupDocumentSaveResult.Cancelled
    val destination = File(selected.parentFile, playlistBackupFileName(selected.name))
    writeFile(destination, bytes)
    PlaylistBackupDocumentSaveResult.Success
} catch (exception: Exception) {
    PlaylistBackupDocumentSaveResult.Failure(exception.message ?: "Could not save playlist backup")
}

internal fun openJvmPlaylistBackupDocument(
    selectFile: () -> File?,
    readFile: (File) -> ByteArray,
): PlaylistBackupDocumentOpenResult = try {
    val selected = selectFile() ?: return PlaylistBackupDocumentOpenResult.Cancelled
    val bytes = readFile(selected)
    if (bytes.size > PlaylistBackupMaxBytes) {
        PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes)
    } else {
        PlaylistBackupDocumentOpenResult.Success(bytes)
    }
} catch (exception: Exception) {
    PlaylistBackupDocumentOpenResult.Failure(exception.message ?: "Could not open playlist backup")
}

internal interface JvmSystemPropertyAccess {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun clear(key: String)
}

private object RealJvmSystemPropertyAccess : JvmSystemPropertyAccess {
    override fun get(key: String): String? = System.getProperty(key)
    override fun set(key: String, value: String) {
        System.setProperty(key, value)
    }
    override fun clear(key: String) {
        System.clearProperty(key)
    }
}

internal fun <T> withJvmDocumentDialogMode(
    properties: JvmSystemPropertyAccess = RealJvmSystemPropertyAccess,
    block: () -> T,
): T {
    val key = "apple.awt.fileDialogForDirectories"
    val previous = properties.get(key)
    properties.clear(key)
    return try {
        block()
    } finally {
        if (previous == null) properties.clear(key) else properties.set(key, previous)
    }
}

private fun openJvmPlaylistBackupDialog(mode: Int, suggestedFileName: String?): File? = withJvmDocumentDialogMode {
    val dialog = FileDialog(null as Frame?, if (mode == FileDialog.SAVE) "Export playlist backup" else "Import playlist backup", mode)
    try {
        dialog.directory = System.getProperty("user.home")
        dialog.file = suggestedFileName
        dialog.isVisible = true
        dialog.files.firstOrNull() ?: dialog.file?.let { File(dialog.directory ?: "", it) }
    } finally {
        dialog.dispose()
    }
}

internal fun readJvmPlaylistBackupBounded(input: InputStream): ByteArray {
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
