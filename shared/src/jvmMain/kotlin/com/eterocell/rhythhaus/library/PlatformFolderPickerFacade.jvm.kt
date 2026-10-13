package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable

/**
 * Creates the JVM native folder picker launcher.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
    onRecoveryResult: (String, LibrarySource) -> Unit,
): PlatformFolderPickerLauncher =
    rememberJvmPlatformFolderPickerLauncher { result ->
        val success = result as? PlatformFolderPickResult.Success
        val recoverySourceId = success?.recoverySourceId
        if (success != null && recoverySourceId != null) {
            onRecoveryResult(recoverySourceId, success.source)
        } else {
            onResult(result)
        }
    }
