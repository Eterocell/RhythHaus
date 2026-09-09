package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable

/**
 * Creates the platform folder picker launcher for this platform.
 *
 * The application shell composes this seam from the Shared facade. Android and
 * JVM actuals delegate to the library feature implementations; the iOS actual
 * launches the retained Swift Files.app import provider declared next to this
 * expect in the Shared framework ABI (see `IOSLibraryImport.kt`).
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
expect fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher

/** Whether an iOS launcher may admit a new operation. */
internal fun iosImportLaunchAllowed(importActive: Boolean): Boolean =
    !importActive
