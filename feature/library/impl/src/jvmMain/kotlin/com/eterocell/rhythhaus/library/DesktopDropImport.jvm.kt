package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.impl.isSupportedAudioName
import java.io.File
import java.security.MessageDigest

private const val MaximumDesktopDropEntries = 128
private const val MaximumDroppedFilesHandleLength = 32 * 1024
private const val MaximumDiagnosticPathLength = 512
private const val DroppedFilesHandlePrefix = "rhythhaus:jvm-dropped-files:v1:"

/**
 * Builds stable local sources from one bounded, read-only desktop drop.
 *
 * @param paths local filesystem paths received from the native window.
 * @param createdAtEpochMillis creation timestamp for new sources.
 */
public actual fun validateDesktopDrop(
    paths: List<String>,
    createdAtEpochMillis: Long,
): DesktopDropResult {
    val sources = mutableListOf<LibrarySource>()
    val droppedFiles = mutableListOf<File>()
    val failures = mutableListOf<DesktopDropFailure>()
    val seenPaths = mutableSetOf<String>()
    var duplicatePathCount = 0
    var droppedFilesHandleLength = DroppedFilesHandlePrefix.length

    val admittedInputCount = minOf(paths.size, MaximumDesktopDropEntries)
    for (index in 0 until admittedInputCount) {
        val rawPath = paths[index]
        if (rawPath.isEmpty()) {
            failures +=
                DesktopDropFailure(
                    path = rawPath.diagnosticPath(),
                    reason = DesktopDropFailureReason.NotRegular,
                )
            continue
        }
        val file = runCatching { File(rawPath).canonicalFile }.getOrNull()
        if (file == null) {
            failures +=
                DesktopDropFailure(
                    path = rawPath.diagnosticPath(),
                    reason = DesktopDropFailureReason.Unreadable,
                )
            continue
        }
        if (!seenPaths.add(file.path)) {
            duplicatePathCount++
            continue
        }

        when {
            file.isDirectory && file.canRead() ->
                sources += file.toJvmFolderSource(createdAtEpochMillis)

            !file.exists() || !file.canRead() ->
                failures +=
                    DesktopDropFailure(
                        path = file.path.diagnosticPath(),
                        reason = DesktopDropFailureReason.Unreadable,
                    )

            file.isFile && isSupportedAudioName(file.name) -> {
                val encodedPathLength =
                    file.path.length.toString().length + 1 + file.path.length
                if (droppedFilesHandleLength + encodedPathLength >
                    MaximumDroppedFilesHandleLength) {
                    failures +=
                        DesktopDropFailure(
                            path = file.path.diagnosticPath(),
                            reason = DesktopDropFailureReason.LimitExceeded,
                        )
                } else {
                    droppedFiles += file
                    droppedFilesHandleLength += encodedPathLength
                }
            }

            file.isFile ->
                failures +=
                    DesktopDropFailure(
                        path = file.path.diagnosticPath(),
                        reason = DesktopDropFailureReason.Unsupported,
                    )

            else ->
                failures +=
                    DesktopDropFailure(
                        path = file.path.diagnosticPath(),
                        reason = DesktopDropFailureReason.NotRegular,
                    )
        }
    }

    if (droppedFiles.isNotEmpty()) {
        sources += droppedFilesSource(droppedFiles, createdAtEpochMillis)
    }
    return DesktopDropResult(
        sources = sources,
        failures = failures,
        duplicatePathCount = duplicatePathCount,
        overflowEntryCount = paths.size - admittedInputCount,
    )
}

/** Whether [source] represents the bounded fixed file set created by a drop. */
internal fun isJvmDroppedFilesSource(source: LibrarySource): Boolean =
    source.platformKind == LibraryPlatformKind.JvmFolder &&
        source.handle.startsWith(DroppedFilesHandlePrefix)

/** Decodes the ordered canonical file paths from a dropped-files source. */
internal fun droppedFilesFromSource(source: LibrarySource): List<File> {
    require(isJvmDroppedFilesSource(source)) {
        "Source is not a JVM dropped-files source: ${source.id}"
    }
    val handle = source.handle
    require(handle.length <= MaximumDroppedFilesHandleLength) {
        "Dropped-files handle exceeds limit"
    }
    var cursor = DroppedFilesHandlePrefix.length
    val files = mutableListOf<File>()
    while (cursor < handle.length) {
        require(files.size < MaximumDesktopDropEntries) {
            "Dropped-files count exceeds limit"
        }
        val delimiter = handle.indexOf(':', startIndex = cursor)
        require(delimiter > cursor) {
            "Malformed JVM dropped-files source handle"
        }
        val pathLength = handle.substring(cursor, delimiter).toIntOrNull()
        require(pathLength != null && pathLength > 0) {
            "Malformed JVM dropped-files source handle"
        }
        val pathStart = delimiter + 1
        require(pathLength <= handle.length - pathStart) {
            "Malformed JVM dropped-files source handle"
        }
        val pathEnd = pathStart + pathLength
        val file = File(handle.substring(pathStart, pathEnd))
        require(file.isAbsolute && '\u0000' !in file.path) {
            "Invalid dropped-file path"
        }
        files += file
        cursor = pathEnd
    }
    require(files.isNotEmpty()) { "JVM dropped-files source has no files" }
    return files
}

private fun File.toJvmFolderSource(createdAtEpochMillis: Long): LibrarySource =
    LibrarySource(
        id = jvmFolderSourceId(path),
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = name.ifBlank { path },
        handle = path,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private fun droppedFilesSource(
    files: List<File>,
    createdAtEpochMillis: Long,
): LibrarySource {
    val canonicalPaths = files.map(File::getCanonicalPath)
    val identityPaths = canonicalPaths.sorted()
    val digest =
        MessageDigest.getInstance("SHA-256")
            .digest(identityPaths.joinToString("\u0000").toByteArray())
            .joinToString(separator = "") { byte ->
                byte.toUByte().toString(radix = 16).padStart(2, '0')
            }
    val handle = buildString {
        append(DroppedFilesHandlePrefix)
        canonicalPaths.forEach { path ->
            append(path.length)
            append(':')
            append(path)
        }
    }
    return LibrarySource(
        id = "jvm-dropped-files:$digest",
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = "Dropped audio files",
        handle = handle,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}

private fun String.diagnosticPath(): String = take(MaximumDiagnosticPathLength)
