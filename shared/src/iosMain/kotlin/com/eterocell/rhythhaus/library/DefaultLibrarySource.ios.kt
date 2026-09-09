package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.impl.appLocalMusicFolderPath
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

/** Creates and exposes RhythHaus's Files-visible Documents directory. */
@OptIn(ExperimentalForeignApi::class)
internal actual fun defaultPlatformLibrarySource(): LibrarySource? =
    runCatching {
        val folder = appLocalMusicFolderPath()
        val fileManager = NSFileManager.defaultManager
        val available =
            fileManager.fileExistsAtPath(folder) ||
                fileManager.createDirectoryAtPath(
                    path = folder,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
        if (!available) return null
        LibrarySource(
            id = "ios-app-local",
            platformKind = LibraryPlatformKind.IosAppLocal,
            displayName = "RhythHaus",
            handle = folder,
            createdAtEpochMillis = currentTimeMillis(),
        )
    }
    .getOrNull()
