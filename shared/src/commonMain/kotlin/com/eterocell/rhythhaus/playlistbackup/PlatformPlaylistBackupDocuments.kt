package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable

const val PlaylistBackupFileExtension = ".rhythhaus-playlists.json"
const val PlaylistBackupMimeType = "application/vnd.rhythhaus.playlists+json"
const val PlaylistBackupJsonMimeType = "application/json"
const val PlaylistBackupMaxBytes = 4 * 1024 * 1024

sealed interface PlaylistBackupDocumentSaveResult {
    data object Success : PlaylistBackupDocumentSaveResult
    data object Cancelled : PlaylistBackupDocumentSaveResult
    data class Unavailable(val message: String) : PlaylistBackupDocumentSaveResult
    data class Failure(val message: String) : PlaylistBackupDocumentSaveResult
}

sealed interface PlaylistBackupDocumentOpenResult {
    data class Success(val bytes: ByteArray) : PlaylistBackupDocumentOpenResult
    data object Cancelled : PlaylistBackupDocumentOpenResult
    data class Unavailable(val message: String) : PlaylistBackupDocumentOpenResult
    data class TooLarge(val maxBytes: Int) : PlaylistBackupDocumentOpenResult
    data class Failure(val message: String) : PlaylistBackupDocumentOpenResult
}

interface PlatformPlaylistBackupDocumentLauncher {
    val isAvailable: Boolean
    fun save(suggestedFileName: String, bytes: ByteArray)
    fun open()
}

fun playlistBackupFileName(suggestedFileName: String): String {
    val safeBaseName = suggestedFileName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeUnless { it.isBlank() || it == "." || it == ".." }
        ?: "rhythhaus-playlists"
    return if (safeBaseName.endsWith(PlaylistBackupFileExtension, ignoreCase = true)) {
        safeBaseName
    } else {
        safeBaseName + PlaylistBackupFileExtension
    }
}

internal class PlaylistBackupDocumentOperationGate {
    private var active = false

    val isActive: Boolean
        get() = active

    fun tryStart(): Boolean {
        if (active) return false
        active = true
        return true
    }

    fun finish() {
        active = false
    }
}

@Composable
expect fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlatformPlaylistBackupDocumentLauncher
