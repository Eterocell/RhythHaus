package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import java.io.File

/** JVM filesystem-folder-based [PlatformSourceAccess] for library sources. */
class JvmFolderSourceAccess : PlatformSourceAccess {
    /**
     * Returns the access status for the given source.
     *
     * @param source the library source to inspect.
     */
    override fun accessStatus(
        source: LibrarySource
    ): LibrarySourceAccessStatus {
        val folder = File(source.handle)
        return if (source.platformKind == LibraryPlatformKind.JvmFolder &&
            folder.isDirectory &&
            folder.canRead()) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    /**
     * Scans the given JVM folder source.
     *
     * @param source the library source to scan.
     */
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        scanJvmFolderSource(source)
}

/**
 * Scans a JVM folder source into scan events.
 *
 * @param source the JVM folder library source to scan.
 */
fun scanJvmFolderSource(source: LibrarySource): Sequence<PlatformScanEvent> =
    sequence {
        require(source.platformKind == LibraryPlatformKind.JvmFolder) {
            "JvmFolderSourceAccess can only scan JvmFolder sources"
        }
        val root = File(source.handle)
        require(root.isDirectory && root.canRead()) {
            "Cannot read folder: ${source.handle}"
        }
        yieldAll(scanFolder(source, root, root))
    }

private fun scanFolder(
    source: LibrarySource,
    root: File,
    folder: File,
): Sequence<PlatformScanEvent> = sequence {
    yield(
        PlatformScanEvent.FolderVisited(
            folder.displayPath(root, source.displayName)))
    folder
        .listFiles()
        ?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        ?.forEach { child ->
            when {
                child.isDirectory -> yieldAll(scanFolder(source, root, child))
                child.isFile -> yield(child.toScanEvent(source, root))
            }
        }
}

private fun File.toScanEvent(
    source: LibrarySource,
    root: File
): PlatformScanEvent {
    val key = relativeTo(root).invariantSeparatorsPath
    return audioCandidateForSourceFile(
        source = source,
        sourceLocalKey = key,
        displayPath = key,
        displayName = name,
        audioSource = AudioSource.FilePath(absolutePath),
        sizeBytes = length(),
        modifiedAtEpochMillis = lastModified().takeIf { it > 0L },
    )
}

private fun File.displayPath(root: File, fallback: String): String {
    if (canonicalPath == root.canonicalPath) return fallback
    return relativeTo(root).invariantSeparatorsPath.ifBlank { fallback }
}

/** Creates the JVM folder source access implementation. */
actual fun createPlatformSourceAccess(): PlatformSourceAccess =
    JvmFolderSourceAccess()
