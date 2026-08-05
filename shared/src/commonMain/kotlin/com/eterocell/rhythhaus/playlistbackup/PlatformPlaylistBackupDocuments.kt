package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable

/** ABI-stable MIME type for exported playlist backup documents. */
public const val PlaylistBackupMimeType: String =
    "application/vnd.rhythhaus.playlists+json"

/** ABI-stable maximum bytes accepted by playlist backup document adapters. */
public const val PlaylistBackupMaxBytes: Int = 4 * 1024 * 1024

@Composable
expect fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlaylistBackupDocumentLauncher
