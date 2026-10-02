package com.eterocell.rhythhaus.library.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import java.io.File
import java.io.IOException

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
        if (source.platformKind == LibraryPlatformKind.AndroidMediaStoreAudio) {
            return if (hasMediaStoreAudioPermission(context))
                LibrarySourceAccessStatus.Available
            else LibrarySourceAccessStatus.LostAccess
        }
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
            if (source.platformKind ==
                LibraryPlatformKind.AndroidMediaStoreAudio) {
                yieldAll(scanMediaStoreAudio(context, source))
                return@sequence
            }
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

private fun hasMediaStoreAudioPermission(context: Context): Boolean {
    val permission =
        if (Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun scanMediaStoreAudio(
    context: Context,
    source: LibrarySource,
): Sequence<PlatformScanEvent> = sequence {
    require(hasMediaStoreAudioPermission(context)) {
        "Android audio permission is required for MediaStore scanning"
    }
    val projection =
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    context.contentResolver
        .query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media._ID} ASC",
        )
        ?.use { cursor ->
            val idIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val modifiedIndex =
                cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeIndex =
                cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name =
                        cursor.getString(nameIndex).orEmpty().ifBlank {
                            "audio-$id"
                        }
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    add(
                        MediaStoreAudioRow(
                            id = id,
                            name = name,
                            uri = uri.toString(),
                            mimeType =
                                mimeIndex
                                    .takeIf { it >= 0 }
                                    ?.let(cursor::getString),
                            sizeBytes =
                                sizeIndex
                                    .takeIf { it >= 0 && !cursor.isNull(it) }
                                    ?.let(cursor::getLong),
                            modifiedAtEpochMillis =
                                modifiedIndex
                                    .takeIf { it >= 0 && !cursor.isNull(it) }
                                    ?.let(cursor::getLong)
                                    ?.takeIf { it in 1..Long.MAX_VALUE / 1000 }
                                    ?.times(1000L),
                        ))
                }
            }
        }
        ?.let { rows ->
            yieldAll(
                mediaStoreAudioEvents(source, rows) { uri ->
                    context.contentResolver
                        .openFileDescriptor(Uri.parse(uri), "r")
                        ?.use { true }
                        ?: throw IOException(
                            "Cannot read MediaStore audio: $uri")
                })
        } ?: error("MediaStore audio query returned no cursor")
}

internal data class MediaStoreAudioRow(
    val id: Long,
    val name: String,
    val uri: String,
    val sizeBytes: Long?,
    val modifiedAtEpochMillis: Long?,
    val mimeType: String? = null,
)

// Query ownership ends before the sequence suspends: cancellation cannot strand
// a cursor.
internal fun mediaStoreAudioEvents(
    source: LibrarySource,
    rows: List<MediaStoreAudioRow>,
    requireReadable: (String) -> Unit,
): Sequence<PlatformScanEvent> = sequence {
    require(source.platformKind == LibraryPlatformKind.AndroidMediaStoreAudio)
    for (row in rows) {
        val supportedMime =
            row.mimeType?.lowercase() in mediaStoreAudioMimeTypes
        if (isSupportedAudioName(row.name) || supportedMime) {
            try {
                requireReadable(row.uri)
            } catch (failure: IOException) {
                yield(
                    PlatformScanEvent.Skipped(
                        "mediastore:${row.id}",
                        row.name,
                        failure.message ?: "Unreadable MediaStore audio",
                        true))
                continue
            }
        }
        yield(
            audioCandidateForSourceFile(
                source = source,
                sourceLocalKey = "mediastore:${row.id}",
                displayPath = row.name,
                displayName = row.name,
                audioSource = AudioSource.Uri(row.uri),
                sizeBytes = row.sizeBytes,
                modifiedAtEpochMillis = row.modifiedAtEpochMillis,
                supportedByMimeType = supportedMime,
            ),
        )
    }
}

private val mediaStoreAudioMimeTypes =
    setOf(
        "audio/mpeg",
        "audio/mp4",
        "audio/aac",
        "audio/flac",
        "audio/x-flac",
        "audio/ogg",
        "application/ogg",
        "audio/wav",
        "audio/x-wav",
        "audio/vnd.wave",
        "audio/aiff",
        "audio/x-aiff",
        "audio/basic",
    )

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
