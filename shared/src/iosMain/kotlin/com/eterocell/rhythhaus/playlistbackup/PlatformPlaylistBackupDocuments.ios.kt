package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

object IOSPlaylistBackupDocumentStatus {
    const val SUCCESS = 0
    const val CANCELLED = 1
    const val TOO_LARGE = 2
    const val FAILURE = 3
    const val UNAVAILABLE = 4
}

interface IOSPlaylistBackupDocumentCompletion {
    fun complete(status: Int, bytes: ByteArray?, message: String?)
}

interface IOSPlaylistBackupDocumentProvider {
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

object IOSPlaylistBackupDocumentBridge {
    var provider: IOSPlaylistBackupDocumentProvider? = null
}

@Composable
actual fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlatformPlaylistBackupDocumentLauncher {
    val currentSaveResult = rememberUpdatedState(onSaveResult)
    val currentOpenResult = rememberUpdatedState(onOpenResult)
    return remember {
        object : PlatformPlaylistBackupDocumentLauncher {
            override val isAvailable: Boolean
                get() = IOSPlaylistBackupDocumentBridge.provider != null

            override fun save(suggestedFileName: String, bytes: ByteArray) {
                val provider = IOSPlaylistBackupDocumentBridge.provider
                if (provider == null) {
                    currentSaveResult.value(
                        iosPlaylistBackupUnavailableSaveResult())
                    return
                }
                provider.saveDocument(
                    playlistBackupFileName(suggestedFileName),
                    bytes,
                    object : IOSPlaylistBackupDocumentCompletion {
                        override fun complete(
                            status: Int,
                            bytes: ByteArray?,
                            message: String?
                        ) {
                            currentSaveResult.value(
                                iosPlaylistBackupSaveResult(status, message))
                        }
                    },
                )
            }

            override fun open() {
                val provider = IOSPlaylistBackupDocumentBridge.provider
                if (provider == null) {
                    currentOpenResult.value(
                        iosPlaylistBackupUnavailableOpenResult())
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
                            currentOpenResult.value(
                                iosPlaylistBackupOpenResult(
                                    status, bytes, message))
                        }
                    },
                )
            }
        }
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
