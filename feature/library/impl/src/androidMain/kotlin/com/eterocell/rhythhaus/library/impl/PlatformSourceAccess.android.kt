package com.eterocell.rhythhaus.library.impl

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryDatabaseContext
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import java.io.ByteArrayOutputStream
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
            val rootEntry =
                SafEntry(
                    documentId = DocumentsContract.getTreeDocumentId(rootUri),
                    name = root.name ?: source.displayName,
                    isDirectory = true,
                    uri = rootUri.toString(),
                )
            yieldAll(
                safTreeScanEvents(
                    source = source,
                    reader = ContentResolverSafTreeReader(context, rootUri),
                    root = rootEntry,
                    rootDisplayName = source.displayName,
                ),
            )
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

/**
 * Snapshot of one SAF document.
 *
 * Names and types come from the single listing query that discovered the
 * document, so scanning never re-queries a provider per file.
 */
internal data class SafEntry(
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val uri: String,
    val sizeBytes: Long? = null,
    val lastModifiedEpochMillis: Long? = null,
)

/** Readable Android file descriptor with its owning close action. */
internal class SafFileDescriptor(
    val fd: Int,
    private val onClose: () -> Unit,
) {
    /** Releases this descriptor. */
    fun close() = onClose()
}

/** Directory enumeration and document reads required by a SAF tree scan. */
internal interface SafTreeReader {
    /** Lists [directory] exactly once, returning cached name/type snapshots. */
    fun children(directory: SafEntry): List<SafEntry>

    /** Opens a readable metadata descriptor for [entry], if accessible. */
    fun openMetadataDescriptor(entry: SafEntry): SafFileDescriptor?

    /**
     * Reads at most `maxBytes + 1` bytes of [entry], or null when unreadable.
     */
    fun readSidecar(entry: SafEntry, maxBytes: Long): ByteArray?
}

private const val MAX_LRC_BYTES = 512 * 1024L

private val safChildOrdering =
    compareBy<SafEntry> { !it.isDirectory }.thenBy { it.name.lowercase() }

/**
 * Scans one already-open SAF tree.
 *
 * Every directory is enumerated exactly once: the returned snapshots carry the
 * names, types, sizes, and timestamps needed for ordering and candidate fields.
 * Same-stem `.lrc` siblings are indexed per directory from that same listing,
 * and their bytes are read only for supported audio entries.
 */
internal fun safTreeScanEvents(
    source: LibrarySource,
    reader: SafTreeReader,
    root: SafEntry,
    rootDisplayName: String,
): Sequence<PlatformScanEvent> =
    if (root.isDirectory) {
        scanSafDirectory(source, reader, root, rootDisplayName, emptyList())
    } else {
        sequenceOf(safFileEvent(source, reader, root, listOf(root.name), null))
    }

private fun scanSafDirectory(
    source: LibrarySource,
    reader: SafTreeReader,
    directory: SafEntry,
    rootDisplayName: String,
    pathSegments: List<String>,
): Sequence<PlatformScanEvent> = sequence {
    val displayPath = pathSegments.joinToString("/").ifBlank { rootDisplayName }
    yield(PlatformScanEvent.FolderVisited(displayPath))
    val children = reader.children(directory).sortedWith(safChildOrdering)
    val sidecarsByStem = linkedMapOf<String, SafEntry>()
    for (child in children) {
        if (child.isDirectory) continue
        val stem = child.name.lrcStem() ?: continue
        sidecarsByStem.getOrPut(stem) { child }
    }
    for (child in children) {
        if (child.isDirectory) {
            yieldAll(
                scanSafDirectory(
                    source,
                    reader,
                    child,
                    rootDisplayName,
                    pathSegments + child.name,
                ),
            )
        } else if (!child.name.isLrcFileName()) {
            yield(
                safFileEvent(
                    source,
                    reader,
                    child,
                    pathSegments + child.name,
                    child.name.audioStem()?.let(sidecarsByStem::get),
                ),
            )
        }
    }
}

private fun safFileEvent(
    source: LibrarySource,
    reader: SafTreeReader,
    entry: SafEntry,
    pathSegments: List<String>,
    sidecar: SafEntry?,
): PlatformScanEvent {
    val name = pathSegments.lastOrNull() ?: entry.name
    val key = pathSegments.sourceLocalKey().ifBlank { entry.uri }
    val displayPath = pathSegments.joinToString("/").ifBlank { name }
    val supported = isSupportedAudioName(name)
    val descriptor =
        if (supported) reader.openMetadataDescriptor(entry) else null
    val sidecarLyrics =
        if (supported && sidecar != null) reader.readSidecarText(sidecar)
        else null
    return audioCandidateForSourceFile(
        source = source,
        sourceLocalKey = key,
        displayPath = displayPath,
        displayName = name,
        audioSource = AudioSource.Uri(entry.uri),
        metadataAudioSource =
            descriptor?.let { AudioSource.FileDescriptor(it.fd, name) }
                ?: AudioSource.Uri(entry.uri),
        cleanupMetadataAudioSource =
            descriptor?.let { open -> { open.close() } },
        sizeBytes = entry.sizeBytes,
        modifiedAtEpochMillis = entry.lastModifiedEpochMillis,
        sidecarLyrics = sidecarLyrics,
    )
}

private fun SafTreeReader.readSidecarText(sidecar: SafEntry): String? {
    val knownSize = sidecar.sizeBytes
    if (knownSize != null && knownSize > MAX_LRC_BYTES) return null
    val bytes =
        runCatching { readSidecar(sidecar, MAX_LRC_BYTES) }.getOrNull()
            ?: return null
    if (bytes.size.toLong() > MAX_LRC_BYTES) return null
    return bytes.toString(Charsets.UTF_8)
}

private fun String.isLrcFileName(): Boolean =
    contains('.') && substringAfterLast('.').equals("lrc", ignoreCase = true)

private fun String.lrcStem(): String? =
    if (isLrcFileName()) {
        substringBeforeLast('.').lowercase().takeIf { it.isNotEmpty() }
    } else {
        null
    }

private fun String.audioStem(): String? =
    if (contains('.')) {
        substringBeforeLast('.').lowercase().takeIf { it.isNotEmpty() }
    } else {
        null
    }

/** Reads SAF directories and documents through the content resolver. */
private class ContentResolverSafTreeReader(
    private val context: Context,
    private val treeUri: Uri,
) : SafTreeReader {
    override fun children(directory: SafEntry): List<SafEntry> {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                directory.documentId,
            )
        return context.contentResolver
            .query(childrenUri, safChildProjection, null, null, null)
            ?.use { cursor ->
                // A partial listing must not mark the missing tracks as
                // deleted.
                if (cursor.extras.getBoolean(DocumentsContract.EXTRA_LOADING)) {
                    throw IOException(
                        "Folder listing is still loading: ${directory.name}")
                }
                val rows = SafChildCursor(cursor, treeUri)
                buildList {
                    while (cursor.moveToNext()) add(rows.readEntry())
                }
            } ?: throw IOException("Cannot list folder: ${directory.name}")
    }

    override fun openMetadataDescriptor(entry: SafEntry): SafFileDescriptor? =
        runCatching {
            context.contentResolver
                .openFileDescriptor(Uri.parse(entry.uri), "r")
                ?.let { descriptor ->
                    SafFileDescriptor(descriptor.fd) { descriptor.close() }
                }
        }
        .getOrNull()

    override fun readSidecar(
        entry: SafEntry,
        maxBytes: Long,
    ): ByteArray? = runCatching {
        context.contentResolver.openInputStream(Uri.parse(entry.uri))?.use {
            input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (total <= maxBytes) {
                val room = maxBytes + 1 - total
                val read =
                    input.read(
                        buffer,
                        0,
                        minOf(buffer.size.toLong(), room).toInt(),
                    )
                if (read < 0) break
                total += read
                bytes.write(buffer, 0, read)
            }
            bytes.toByteArray()
        }
    }
        .getOrNull()

    private companion object {
        val safChildProjection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
    }
}

/**
 * Reads SAF child rows from one already-positioned listing cursor.
 *
 * Column indexes are resolved once per directory query so scanning never
 * performs an extra provider round trip per document.
 */
private class SafChildCursor(
    private val cursor: Cursor,
    private val treeUri: Uri,
) {
    private val idIndex =
        cursor.getColumnIndexOrThrow(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID)
    private val nameIndex =
        cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    private val mimeIndex =
        cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
    private val sizeIndex =
        cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
    private val modifiedIndex =
        cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

    /** Reads the row under the cursor as a cached document snapshot. */
    fun readEntry(): SafEntry {
        val documentId = cursor.getString(idIndex).orEmpty()
        return SafEntry(
            documentId = documentId,
            name =
                stringAt(nameIndex)?.takeIf { it.isNotBlank() }
                    ?: documentId.substringAfterLast('/'),
            isDirectory =
                stringAt(mimeIndex) == DocumentsContract.Document.MIME_TYPE_DIR,
            uri =
                DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    .toString(),
            sizeBytes = longAt(sizeIndex)?.takeIf { it >= 0L },
            lastModifiedEpochMillis = longAt(modifiedIndex)?.takeIf { it > 0L },
        )
    }

    private fun stringAt(index: Int): String? =
        index.takeIf { it >= 0 }?.let(cursor::getString)

    private fun longAt(index: Int): Long? =
        index.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
}

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
