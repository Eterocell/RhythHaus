package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
 * [LibraryPlatformKind.IosAppLocal] source. Cancellation delivers a distinct
 * silent [PlatformFolderPickResult.Cancelled]. While the picker is shown or
 * files are being copied, [isImportActive] reports true so the App can fold
 * the import window into source-mutation gating.
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
            private val importActive = mutableStateOf(false)

            override val isAvailable: Boolean
                get() = IOSLibraryImportBridge.provider != null

            override val supportsAdditionalSources: Boolean = false

            override val isImportActive: Boolean
                get() = importActive.value

            override fun launch() {
                // Reject a second launch while an import is active. Without
                // this guard the re-entrant provider call would answer with
                // an OVERLAP terminal, and that terminal would clear the
                // first operation's active gate below before its real copy
                // finished. The App already gates the import affordance via
                // mutationsEnabled (which folds in isImportActive); this
                // guard keeps the launcher safe even if launch() is invoked
                // through an ungated path.
                if (!iosImportLaunchAllowed(importActive.value)) return
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
                importActive.value = true
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
                            importActive.value = false
                            currentOnResult.value(
                                iosLibraryImportPickResult(
                                    destinationFolderPath = destinationPath,
                                    status = status,
                                    imported = imported,
                                    duplicates = duplicates,
                                    unsupported = unsupported,
                                    failed = failed,
                                    message = message,
                                ),
                            )
                        }
                    }
                runCatching {
                    provider.importAudio(destinationPath, completion)
                }.onFailure {
                    importActive.value = false
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
