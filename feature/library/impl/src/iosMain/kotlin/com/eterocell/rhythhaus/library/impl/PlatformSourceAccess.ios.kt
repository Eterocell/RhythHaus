package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** iOS app-local folder [PlatformSourceAccess] for library sources. */
class IOSAppLocalSourceAccess : PlatformSourceAccess {
    /**
     * Returns the access status for the given source.
     *
     * @param source the library source to inspect.
     */
    override fun accessStatus(
        source: LibrarySource
    ): LibrarySourceAccessStatus {
        if (source.platformKind != LibraryPlatformKind.IosAppLocal)
            return LibrarySourceAccessStatus.LostAccess
        return if (NSFileManager.defaultManager.fileExistsAtPath(
            source.handle)) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    /**
     * Scans the given app-local folder source.
     *
     * @param source the library source to scan.
     */
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        sequence {
            require(source.platformKind == LibraryPlatformKind.IosAppLocal) {
                "IOSAppLocalSourceAccess can only scan IosAppLocal sources"
            }
            require(
                NSFileManager.defaultManager.fileExistsAtPath(source.handle)) {
                    "Cannot read app-local music folder: ${source.handle}"
                }
            yieldAll(scanIosFolder(source, source.handle, emptyList()))
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun scanIosFolder(
    source: LibrarySource,
    folderPath: String,
    pathSegments: List<String>,
): Sequence<PlatformScanEvent> = sequence {
    val displayPath =
        pathSegments.joinToString("/").ifBlank { source.displayName }
    yield(PlatformScanEvent.FolderVisited(displayPath))
    val children =
        NSFileManager.defaultManager
            .contentsOfDirectoryAtPath(folderPath, error = null)
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
                    audioSource = AudioSource.FilePath(relativePath),
                    sizeBytes = fileSize(path),
                    modifiedAtEpochMillis = null,
                ),
            )
        }
    }
}

/** Returns the app-local music folder path. */
@OptIn(ExperimentalForeignApi::class)
fun appLocalMusicFolderPath(): String {
    val urls =
        NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        )
    val documentsUrl =
        urls.firstOrNull() as? NSURL
            ?: error("iOS documents directory is unavailable")
    return documentsUrl.path.orEmpty().trimEnd('/')
}

@OptIn(ExperimentalForeignApi::class)
private fun isDirectory(path: String): Boolean {
    val attributes =
        NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
            ?: return false
    return attributes[platform.Foundation.NSFileType] ==
        platform.Foundation.NSFileTypeDirectory
}

@OptIn(ExperimentalForeignApi::class)
private fun fileSize(path: String): Long? {
    val attributes =
        NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
            ?: return null
    return (attributes[NSFileSize] as? Number)?.toLong()?.takeIf { it >= 0L }
}

/** Creates the iOS app-local source access implementation. */
actual fun createPlatformSourceAccess(): PlatformSourceAccess =
    IOSAppLocalSourceAccess()
