package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.AudioSource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher = remember(onResult) {
    object : PlatformFolderPickerLauncher {
        override val isAvailable: Boolean = true

        override fun launch() {
            val result = runCatching { openNativeFolderDialog() }
                .fold(
                    onSuccess = { folder ->
                        if (folder == null) {
                            PlatformFolderPickResult.Unavailable("No folder selected")
                        } else {
                            PlatformFolderPickResult.Success(folder.toJvmFolderSource())
                        }
                    },
                    onFailure = { throwable ->
                        PlatformFolderPickResult.Failure(
                            message = "Could not select music folder",
                            cause = throwable.message ?: throwable::class.simpleName,
                        )
                    },
                )
            onResult(result)
        }
    }
}

private fun openNativeFolderDialog(initialDirectory: String? = null): File? {
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    val dialog = FileDialog(null as Frame?, "Choose music folder", FileDialog.LOAD).apply {
        directory = initialDirectory ?: System.getProperty("user.home")
    }
    return try {
        dialog.isVisible = true
        val selected = dialog.files?.firstOrNull()
            ?: dialog.file?.let { File(dialog.directory ?: "", it) }
        selected?.takeIf { it.isDirectory }
    } finally {
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
        dialog.dispose()
    }
}

private fun File.toJvmFolderSource(): LibrarySource = LibrarySource(
    id = "jvm-folder-${canonicalPath.hashCode().toUInt().toString(16)}",
    platformKind = LibraryPlatformKind.JvmFolder,
    displayName = name.ifBlank { canonicalPath },
    handle = canonicalPath,
    createdAtEpochMillis = System.currentTimeMillis(),
)

class JvmFolderSourceAccess : PlatformSourceAccess, PlatformAudioScanner {
    override fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus {
        val folder = File(source.handle)
        return if (source.platformKind == LibraryPlatformKind.JvmFolder && folder.isDirectory && folder.canRead()) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = scanJvmFolderSource(source)
}

fun scanJvmFolderSource(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
    require(source.platformKind == LibraryPlatformKind.JvmFolder) {
        "JvmFolderSourceAccess can only scan JvmFolder sources"
    }
    val root = File(source.handle)
    require(root.isDirectory && root.canRead()) { "Cannot read folder: ${source.handle}" }
    yieldAll(scanFolder(source, root, root))
}

private fun scanFolder(
    source: LibrarySource,
    root: File,
    folder: File,
): Sequence<PlatformScanEvent> = sequence {
    yield(PlatformScanEvent.FolderVisited(folder.displayPath(root, source.displayName)))
    folder.listFiles()
        ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        ?.forEach { child ->
            when {
                child.isDirectory -> yieldAll(scanFolder(source, root, child))
                child.isFile -> yield(child.toScanEvent(source, root))
            }
        }
}

private fun File.toScanEvent(source: LibrarySource, root: File): PlatformScanEvent {
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
