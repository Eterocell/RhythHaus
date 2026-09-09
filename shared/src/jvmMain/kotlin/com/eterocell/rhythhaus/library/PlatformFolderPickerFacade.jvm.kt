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
): PlatformFolderPickerLauncher =
    rememberJvmPlatformFolderPickerLauncher(onResult)
