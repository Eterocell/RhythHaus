package com.eterocell.rhythhaus.library.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import java.io.File

/** Android SAF-based [PlatformSourceAccess] for library sources. */
class AndroidSafSourceAccess(
    private val context: Context,
) : PlatformSourceAccess {
    /**
     * Returns the access status for the given source.
     *
     * @param source the library source to inspect.
     */
    override fun accessStatus(
        source: LibrarySource
    ): LibrarySourceAccessStatus {
        if (source.platformKind != LibraryPlatformKind.AndroidSafTree)
            return LibrarySourceAccessStatus.LostAccess
        val hasPersistedPermission =
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.isReadPermission &&
                    permission.uri.toString() == source.handle
            }
        return if (hasPersistedPermission ||
            DocumentFile.fromTreeUri(context, Uri.parse(source.handle))
                ?.canRead() == true) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    /**
     * Releases access held for the given source.
     *
     * @param source the library source to release.
     */
    override fun releaseAccess(source: LibrarySource) {
        if (source.platformKind != LibraryPlatformKind.AndroidSafTree) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(source.handle),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    /**
     * Scans the given SAF tree source.
     *
     * @param source the library source to scan.
     */
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        sequence {
            require(source.platformKind == LibraryPlatformKind.AndroidSafTree) {
                "AndroidSafSourceAccess can only scan AndroidSafTree sources"
            }
            removeLegacyPersistentMetadataCache(source)
            val rootUri = Uri.parse(source.handle)
            val root =
                DocumentFile.fromTreeUri(context, rootUri)
                    ?: error("Cannot open SAF tree: ${source.handle}")
            require(root.canRead()) {
                "No read access to SAF tree: ${source.displayName}"
            }
            yieldAll(scanDocumentTree(context, source, root, emptyList()))
        }
}

private fun scanDocumentTree(
    context: Context,
    source: LibrarySource,
    document: DocumentFile,
    pathSegments: List<String>,
): Sequence<PlatformScanEvent> = sequence {
    if (document.isDirectory) {
        val displayPath =
            pathSegments.joinToString("/").ifBlank { source.displayName }
        yield(PlatformScanEvent.FolderVisited(displayPath))
        document
            .listFiles()
            .sortedWith(
                compareBy<DocumentFile> { !it.isDirectory }
                    .thenBy { it.name.orEmpty().lowercase() })
            .forEach { child ->
                val name = child.name ?: child.uri.lastPathSegment ?: "unnamed"
                yieldAll(
                    scanDocumentTree(
                        context, source, child, pathSegments + name))
            }
    } else if (document.isFile) {
        val name =
            pathSegments.lastOrNull()
                ?: document.name
                ?: document.uri.lastPathSegment
                ?: "unnamed"
        val key =
            pathSegments.sourceLocalKey().ifBlank { document.uri.toString() }
        val displayPath = pathSegments.joinToString("/").ifBlank { name }
        val playbackSource = AudioSource.Uri(document.uri.toString())
        val metadataDescriptor =
            if (isSupportedAudioName(name)) {
                openDocumentForMetadata(context, document)
            } else {
                null
            }
        yield(
            audioCandidateForSourceFile(
                source = source,
                sourceLocalKey = key,
                displayPath = displayPath,
                displayName = name,
                audioSource = playbackSource,
                metadataAudioSource =
                    metadataDescriptor?.let {
                        AudioSource.FileDescriptor(it.fd, name)
                    } ?: playbackSource,
                cleanupMetadataAudioSource =
                    metadataDescriptor?.let { descriptor ->
                        { descriptor.close() }
                    },
                sizeBytes = document.length().takeIf { it >= 0L },
                modifiedAtEpochMillis =
                    document.lastModified().takeIf { it > 0L },
            ),
        )
    }
}

private fun openDocumentForMetadata(
    context: Context,
    document: DocumentFile,
): android.os.ParcelFileDescriptor? = runCatching {
    context.contentResolver.openFileDescriptor(document.uri, "r")
}
    .getOrNull()

private fun removeLegacyPersistentMetadataCache(source: LibrarySource) {
    File(
            LibraryDatabaseContext.applicationContext.cacheDir,
            "rhythhaus-taglib/${source.id}")
        .deleteRecursively()
}

/** Creates the Android SAF source access implementation. */
actual fun createPlatformSourceAccess(): PlatformSourceAccess {
    val context = LibraryDatabaseContext.applicationContext
    return AndroidSafSourceAccess(context)
}
