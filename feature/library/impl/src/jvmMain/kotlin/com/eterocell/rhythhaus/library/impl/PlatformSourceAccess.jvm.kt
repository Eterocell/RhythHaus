package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.droppedFilesFromSource
import com.eterocell.rhythhaus.library.isJvmDroppedFilesSource
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * JVM filesystem [PlatformSourceAccess] for folders and dropped audio files.
 */
class JvmFolderSourceAccess : PlatformSourceAccess {
    /**
     * Returns the access status for the given source.
     *
     * @param source the library source to inspect.
     */
    override fun accessStatus(
        source: LibrarySource
    ): LibrarySourceAccessStatus {
        if (isJvmDroppedFilesSource(source)) {
            val hasReadableFile = runCatching {
                droppedFilesFromSource(source)
            }
                .getOrDefault(emptyList())
                .any { file -> file.isFile && file.canRead() }
            return if (hasReadableFile) {
                LibrarySourceAccessStatus.Available
            } else {
                LibrarySourceAccessStatus.LostAccess
            }
        }
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
        if (isJvmDroppedFilesSource(source)) {
            scanJvmDroppedFilesSource(source)
        } else {
            scanJvmFolderSource(source)
        }
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

/** Scans the fixed original file paths of a desktop dropped-files source. */
fun scanJvmDroppedFilesSource(
    source: LibrarySource,
): Sequence<PlatformScanEvent> = sequence {
    droppedFilesFromSource(source).forEach { originalFile ->
        val file = originalFile
        if (!file.isFile || !file.canRead()) {
            val path = originalFile.path
            yield(
                PlatformScanEvent.Skipped(
                    sourceLocalKey = path.normalizedSourceLocalKey(),
                    displayPath = path,
                    reason = "Cannot read dropped audio file",
                    recoverable = true,
                    identityObserved =
                        !Files.notExists(
                            file.toPath(), LinkOption.NOFOLLOW_LINKS),
                ),
            )
        } else {
            yield(
                audioCandidateForSourceFile(
                    source = source,
                    sourceLocalKey = file.path,
                    displayPath = file.path,
                    displayName = file.name,
                    audioSource = AudioSource.FilePath(file.path),
                    sizeBytes = file.length(),
                    modifiedAtEpochMillis =
                        file.lastModified().takeIf { it > 0L },
                ),
            )
        }
    }
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
