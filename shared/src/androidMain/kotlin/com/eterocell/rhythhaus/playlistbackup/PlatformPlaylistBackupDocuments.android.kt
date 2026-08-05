package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformPlaylistBackupDocumentLauncher(
    onSaveResult: (PlaylistBackupDocumentSaveResult) -> Unit,
    onOpenResult: (PlaylistBackupDocumentOpenResult) -> Unit,
): PlaylistBackupDocumentLauncher =
    rememberAndroidPlaylistBackupDocumentLauncher(onSaveResult, onOpenResult)
