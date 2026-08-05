package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * ABI-stable integer terminal statuses supplied by the iOS document provider.
 */
public object IOSPlaylistBackupDocumentStatus {
    const val SUCCESS = 0
    const val CANCELLED = 1
    const val TOO_LARGE = 2
    const val FAILURE = 3
    const val UNAVAILABLE = 4
}

/** ABI completion preserving nullable terminal bytes and messages. */
public interface IOSPlaylistBackupDocumentCompletion {
    fun complete(status: Int, bytes: ByteArray?, message: String?)
}

/**
 * ABI provider implemented by the iOS host to present save and open documents.
 */
public interface IOSPlaylistBackupDocumentProvider {
    fun saveDocument(
        fileName: String,
        bytes: ByteArray,
        completion: IOSPlaylistBackupDocumentCompletion
    )

    fun openDocument(
        maxBytes: Int,
        completion: IOSPlaylistBackupDocumentCompletion
    )
}

/** ABI singleton retaining the current injected iOS document provider. */
public object IOSPlaylistBackupDocumentBridge {
    var provider: IOSPlaylistBackupDocumentProvider? = null
}

@Composable
actual fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlaylistBackupDocumentLauncher {
    val currentSaveResult = rememberUpdatedState(onSaveResult)
    val currentOpenResult = rememberUpdatedState(onOpenResult)
    return remember {
        iosPlaylistBackupDocumentLauncher(
            onSaveResult = { currentSaveResult.value(it) },
            onOpenResult = { currentOpenResult.value(it) },
        )
    }
}

/**
 * Adapts the retained Shared iOS ABI provider to neutral feature launcher
 * results.
 */
internal fun iosPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlaylistBackupDocumentLauncher =
    object : PlaylistBackupDocumentLauncher {
        override val isAvailable: Boolean
            get() = IOSPlaylistBackupDocumentBridge.provider != null

        override fun save(suggestedFileName: String, bytes: ByteArray) {
            val provider = IOSPlaylistBackupDocumentBridge.provider
            if (provider == null) {
                onSaveResult(iosPlaylistBackupUnavailableSaveResult())
                return
            }
            provider.saveDocument(
                iosPlaylistBackupFileName(suggestedFileName),
                bytes,
                object : IOSPlaylistBackupDocumentCompletion {
                    override fun complete(
                        status: Int,
                        bytes: ByteArray?,
                        message: String?
                    ) {
                        onSaveResult(
                            iosPlaylistBackupSaveResult(status, message))
                    }
                },
            )
        }

        override fun open() {
            val provider = IOSPlaylistBackupDocumentBridge.provider
            if (provider == null) {
                onOpenResult(iosPlaylistBackupUnavailableOpenResult())
                return
            }
            provider.openDocument(
                PlaylistBackupMaxBytes,
                object : IOSPlaylistBackupDocumentCompletion {
                    override fun complete(
                        status: Int,
                        bytes: ByteArray?,
                        message: String?
                    ) {
                        onOpenResult(
                            iosPlaylistBackupOpenResult(status, bytes, message))
                    }
                },
            )
        }
    }

internal fun iosPlaylistBackupUnavailableSaveResult() =
    PlaylistBackupDocumentSaveResult.Unavailable(
        "iOS document provider is unavailable")

internal fun iosPlaylistBackupUnavailableOpenResult() =
    PlaylistBackupDocumentOpenResult.Unavailable(
        "iOS document provider is unavailable")

internal fun iosPlaylistBackupSaveResult(
    status: Int,
    message: String?
): PlaylistBackupDocumentSaveResult =
    when (status) {
        IOSPlaylistBackupDocumentStatus.SUCCESS ->
            PlaylistBackupDocumentSaveResult.Success

        IOSPlaylistBackupDocumentStatus.CANCELLED ->
            PlaylistBackupDocumentSaveResult.Cancelled

        IOSPlaylistBackupDocumentStatus.UNAVAILABLE ->
            PlaylistBackupDocumentSaveResult.Unavailable(
                message ?: "iOS document provider is unavailable",
            )

        else ->
            PlaylistBackupDocumentSaveResult.Failure(
                message ?: "Could not save playlist backup")
    }

internal fun iosPlaylistBackupOpenResult(
    status: Int,
    bytes: ByteArray?,
    message: String?,
): PlaylistBackupDocumentOpenResult =
    when (status) {
        IOSPlaylistBackupDocumentStatus.SUCCESS ->
            bytes?.let(PlaylistBackupDocumentOpenResult::Success)
                ?: PlaylistBackupDocumentOpenResult.Failure(
                    message ?: "Document provider returned no bytes")

        IOSPlaylistBackupDocumentStatus.CANCELLED ->
            PlaylistBackupDocumentOpenResult.Cancelled

        IOSPlaylistBackupDocumentStatus.TOO_LARGE ->
            PlaylistBackupDocumentOpenResult.TooLarge(PlaylistBackupMaxBytes)

        IOSPlaylistBackupDocumentStatus.UNAVAILABLE ->
            PlaylistBackupDocumentOpenResult.Unavailable(
                message ?: "iOS document provider is unavailable",
            )

        else ->
            PlaylistBackupDocumentOpenResult.Failure(
                message ?: "Could not open playlist backup")
    }

internal fun iosPlaylistBackupFileName(suggestedFileName: String): String {
    val safe =
        suggestedFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .ifBlank { "rhythhaus-playlists" }
    val extension = ".rhythhaus-playlists.json"
    return if (safe.endsWith(extension, ignoreCase = true)) safe
    else safe + extension
}
