package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.AudioSource
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher = remember(onResult) {
    object : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = true

        override fun launch() {
            val result = runCatching {
                PlatformFolderPickResult.Success(appLocalMusicSource().also {
                    println("[RhythHaus] Scanner source created: ${it.displayName} at ${it.handle}")
                })
            }.getOrElse { throwable ->
                println("[RhythHaus] ERROR creating scanner source: ${throwable.message}")
                PlatformFolderPickResult.Failure(
                    message = "Could not prepare the app-local music folder",
                    cause = throwable.message ?: throwable::class.simpleName,
                )
            }
            onResult(result)
        }
    }
}

class IOSAppLocalSourceAccess :
    PlatformSourceAccess,
    PlatformAudioScanner {
    override fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus {
        if (source.platformKind != LibraryPlatformKind.IosAppLocal) return LibrarySourceAccessStatus.LostAccess
        return if (NSFileManager.defaultManager.fileExistsAtPath(source.handle)) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        require(source.platformKind == LibraryPlatformKind.IosAppLocal) {
            "IOSAppLocalSourceAccess can only scan IosAppLocal sources"
        }
        require(NSFileManager.defaultManager.fileExistsAtPath(source.handle)) {
            "Cannot read app-local music folder: ${source.handle}"
        }
        yieldAll(scanIosFolder(source, source.handle, emptyList()))
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
        id = "ios-app-local-${folder.hashCode().toUInt().toString(16)}",
        platformKind = LibraryPlatformKind.IosAppLocal,
        displayName = "RhythHaus",
        handle = folder,
        createdAtEpochMillis = 0L,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun scanIosFolder(
    source: LibrarySource,
    folderPath: String,
    pathSegments: List<String>,
): Sequence<PlatformScanEvent> = sequence {
    val displayPath = pathSegments.joinToString("/").ifBlank { source.displayName }
    yield(PlatformScanEvent.FolderVisited(displayPath))
    val children = NSFileManager.defaultManager.contentsOfDirectoryAtPath(folderPath, error = null)
        ?.filterIsInstance<String>()
        ?.sortedBy { it.lowercase() }
        .orEmpty()
    children.forEach { name ->
        val path = "$folderPath/$name"
        if (isDirectory(path)) {
            yieldAll(scanIosFolder(source, path, pathSegments + name))
        } else {
            val relativeSegments = pathSegments + name
            val relativePath = relativeSegments.sourceLocalKey()
            yield(
                audioCandidateForSourceFile(
                    source = source,
                    sourceLocalKey = relativePath,
                    displayPath = relativePath,
                    displayName = name,
                    audioSource = AudioSource.FilePath(path),
                    sizeBytes = fileSize(path),
                    modifiedAtEpochMillis = null,
                ),
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun appLocalMusicFolderPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask,
    )
    val documentsUrl = urls.firstOrNull() as? NSURL
        ?: error("iOS documents directory is unavailable")
    return documentsUrl.path.orEmpty().trimEnd('/')
}

@OptIn(ExperimentalForeignApi::class)
private fun isDirectory(path: String): Boolean {
    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null) ?: return false
    return attributes[platform.Foundation.NSFileType] == platform.Foundation.NSFileTypeDirectory
}

@OptIn(ExperimentalForeignApi::class)
private fun fileSize(path: String): Long? {
    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null) ?: return null
    return (attributes[NSFileSize] as? Number)?.toLong()?.takeIf { it >= 0L }
}

actual fun createPlatformSourceAccess(): PlatformSourceAccess = IOSAppLocalSourceAccess()
