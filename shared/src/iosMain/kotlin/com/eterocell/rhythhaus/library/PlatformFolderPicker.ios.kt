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

@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val currentOnResult = rememberUpdatedState(onResult)
    val message = stringResource(Res.string.folder_picker_error_prepare)
    return remember { IosLibraryImportLauncher(message) { currentOnResult.value(it) } }
}

/** Non-Compose owner of the iOS import lifecycle, directly covered by iosTest. */
internal class IosLibraryImportLauncher(
    private val couldNotPrepareMessage: String,
    private val onResult: (PlatformFolderPickResult) -> Unit,
) : PlatformFolderPickerLauncher {
    private val importActive = mutableStateOf(false)

    override val isAvailable: Boolean
        get() = IOSLibraryImportBridge.provider != null

    override val supportsAdditionalSources: Boolean = false

    override val isImportActive: Boolean
        get() = importActive.value

    override fun launch() {
        if (!iosImportLaunchAllowed(importActive.value)) return
        val destinationPath =
            runCatching { ensureAppLocalMusicFolder() }
                .getOrElse {
                    onResult(PlatformFolderPickResult.Failure(couldNotPrepareMessage))
                    return
                }
        val provider = IOSLibraryImportBridge.provider
        if (provider == null) {
            onResult(PlatformFolderPickResult.Unavailable(couldNotPrepareMessage))
            return
        }
        importActive.value = true
        val completion = object : IOSLibraryImportCompletion {
            override fun complete(
                status: Int,
                imported: Int,
                duplicates: Int,
                unsupported: Int,
                failed: Int,
                message: String?,
            ) {
                importActive.value = false
                onResult(
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
        runCatching { provider.importAudio(destinationPath, completion) }.onFailure {
            importActive.value = false
            onResult(PlatformFolderPickResult.Failure(couldNotPrepareMessage))
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
