package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.folder_picker_error_select
import rhythhaus.feature.library.generated.resources.folder_picker_no_folder_selected

/**
 * Creates the JVM native folder picker launcher.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val noFolderSelectedMessage =
        stringResource(Res.string.folder_picker_no_folder_selected)
    val couldNotSelectMessage =
        stringResource(Res.string.folder_picker_error_select)
    return remember(onResult) {
        object : PlatformFolderPickerLauncher {
            override val isAvailable: Boolean = true
            override val supportsAdditionalSources: Boolean = true

            override fun launch() {
                val result = runCatching {
                    openNativeFolderDialog()
                }
                    .fold(
                        onSuccess = { folder ->
                            if (folder == null) {
                                PlatformFolderPickResult.Unavailable(
                                    noFolderSelectedMessage)
                            } else {
                                PlatformFolderPickResult.Success(
                                    folder.toJvmFolderSource())
                            }
                        },
                        onFailure = {
                            PlatformFolderPickResult.Failure(
                                message = couldNotSelectMessage)
                        },
                    )
                onResult(result)
            }
        }
    }
}

private fun openNativeFolderDialog(initialDirectory: String? = null): File? {
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    val dialog =
        FileDialog(null as Frame?, "Choose music folder", FileDialog.LOAD)
            .apply {
                directory = initialDirectory ?: System.getProperty("user.home")
            }
    return try {
        dialog.isVisible = true
        val selected =
            dialog.files?.firstOrNull()
                ?: dialog.file?.let { File(dialog.directory ?: "", it) }
        selected?.takeIf { it.isDirectory }
    } finally {
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
        dialog.dispose()
    }
}

private fun File.toJvmFolderSource(): LibrarySource =
    LibrarySource(
        id = jvmFolderSourceId(canonicalPath),
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = name.ifBlank { canonicalPath },
        handle = canonicalPath,
        createdAtEpochMillis = System.currentTimeMillis(),
    )
