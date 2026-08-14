package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.library.impl.appLocalMusicFolderPath
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSFileManager
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.folder_picker_error_prepare

/**
 * Creates the iOS app-local folder picker launcher.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val couldNotPrepareMessage =
        stringResource(Res.string.folder_picker_error_prepare)
    return remember(onResult) {
        object : PlatformFolderPickerLauncher {
            override val isAvailable: Boolean = true
            override val supportsAdditionalSources: Boolean = false

            override fun launch() {
                val result = runCatching {
                    PlatformFolderPickResult.Success(appLocalMusicSource())
                }
                    .getOrElse {
                        PlatformFolderPickResult.Failure(
                            message = couldNotPrepareMessage)
                    }
                onResult(result)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun appLocalMusicSource(): LibrarySource {
    val folder = appLocalMusicFolderPath()
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = folder,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return LibrarySource(
        id = "ios-app-local",
        platformKind = LibraryPlatformKind.IosAppLocal,
        displayName = "RhythHaus",
        handle = folder,
        createdAtEpochMillis = 0L,
    )
}
