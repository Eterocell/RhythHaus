@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.AudioSource
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Probes readable regular files already accessible to the application sandbox.
 */
@Composable
public actual fun rememberPlaylistTrackReadability(): (AudioSource) -> Boolean =
    remember {
        val documents =
            NSFileManager.defaultManager
                .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
                .firstOrNull() as? NSURL
        val managedDirectory = documents?.path?.trimEnd('/')
        return@remember { source: AudioSource ->
            isIosPlaylistTrackReadable(source, managedDirectory)
        }
    }

internal fun isIosPlaylistTrackReadable(
    source: AudioSource,
    managedDirectory: String?
): Boolean {
    val path =
        when (source) {
            is AudioSource.FilePath ->
                if (source.path.startsWith("/")) source.path
                else managedDirectory?.let { "$it/${source.path}" }
            is AudioSource.Uri ->
                NSURL(string = source.value)?.takeIf { it.isFileURL() }?.path
            is AudioSource.FileDescriptor -> null
        }
    return path != null &&
        NSFileManager.defaultManager.isReadableFileAtPath(path) &&
        NSFileManager.defaultManager
            .attributesOfItemAtPath(path, null)
            ?.get(NSFileType) == NSFileTypeRegular
}
