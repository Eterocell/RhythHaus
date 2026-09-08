package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.eterocell.rhythhaus.library.impl.appLocalMusicFolderPath
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSFileManager
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.folder_picker_error_prepare

/**
 * Creates the iOS import launcher backed by the retained Swift Files.app
 * provider.
 *
 * The managed app-local music folder is passed to the provider as the copy
 * destination; terminal statuses are mapped onto the common picker result so
 * only a successful or duplicate-only import returns the existing
 * [LibraryPlatformKind.IosAppLocal] source. Cancellation delivers no source
 * and no error.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val currentOnResult = rememberUpdatedState(onResult)
    val couldNotPrepareMessage =
        stringResource(Res.string.folder_picker_error_prepare)
    return remember {
        object : PlatformFolderPickerLauncher {
            override val isAvailable: Boolean
                get() = IOSLibraryImportBridge.provider != null

            override val supportsAdditionalSources: Boolean = false

            override fun launch() {
                val destinationPath =
                    runCatching { ensureAppLocalMusicFolder() }
                        .getOrElse {
                            currentOnResult.value(
                                PlatformFolderPickResult.Failure(
                                    message = couldNotPrepareMessage,
                                ),
                            )
                            return
                        }
                val provider = IOSLibraryImportBridge.provider
                if (provider == null) {
                    currentOnResult.value(
                        PlatformFolderPickResult.Unavailable(
                            message = couldNotPrepareMessage,
                        ),
                    )
                    return
                }
                val completion =
                    object : IOSLibraryImportCompletion {
                        override fun complete(
                            status: Int,
                            imported: Int,
                            duplicates: Int,
                            unsupported: Int,
                            failed: Int,
                            message: String?,
                        ) {
                            iosLibraryImportPickResult(
                                destinationFolderPath = destinationPath,
                                status = status,
                                imported = imported,
                                duplicates = duplicates,
                                unsupported = unsupported,
                                failed = failed,
                                message = message,
                            )?.let(currentOnResult.value)
                        }
                    }
                runCatching {
                    provider.importAudio(destinationPath, completion)
                }.onFailure {
                    currentOnResult.value(
                        PlatformFolderPickResult.Failure(
                            message = couldNotPrepareMessage,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ensureAppLocalMusicFolder(): String {
    val folder = appLocalMusicFolderPath()
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = folder,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return folder
}
