package com.eterocell.rhythhaus.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.eterocell.rhythhaus.AudioSource
import java.io.File
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.folder_picker_error_access

@Composable
actual fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher {
    val context = LocalContext.current
    val couldNotAccessMessage = stringResource(Res.string.folder_picker_error_access)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            persistTreePermission(context, uri)
            PlatformFolderPickResult.Success(uri.toAndroidSafSource(context))
        }.getOrElse { throwable ->
            PlatformFolderPickResult.Failure(
                message = couldNotAccessMessage,
                cause = throwable.message ?: throwable::class.simpleName,
            )
        }
        onResult(result)
    }

    return remember(launcher) {
        object : PlatformFolderPickerLauncher {
            override val isAvailable: Boolean = true
            override fun launch() {
                launcher.launch(null)
            }
        }
    }
}

private fun persistTreePermission(context: Context, uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        .recoverCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
}

private fun Uri.toAndroidSafSource(context: Context): LibrarySource {
    val document = DocumentFile.fromTreeUri(context, this)
    val displayName = document?.name ?: lastPathSegment ?: toString()
    return LibrarySource(
        id = "android-saf-${toString().hashCode().toUInt().toString(16)}",
        platformKind = LibraryPlatformKind.AndroidSafTree,
        displayName = displayName,
        handle = toString(),
        createdAtEpochMillis = System.currentTimeMillis(),
    )
}

class AndroidSafSourceAccess(
    private val context: Context,
) : PlatformSourceAccess,
    PlatformAudioScanner {
    override fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus {
        if (source.platformKind != LibraryPlatformKind.AndroidSafTree) return LibrarySourceAccessStatus.LostAccess
        val hasPersistedPermission = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.uri.toString() == source.handle
        }
        return if (hasPersistedPermission || DocumentFile.fromTreeUri(context, Uri.parse(source.handle))?.canRead() == true) {
            LibrarySourceAccessStatus.Available
        } else {
            LibrarySourceAccessStatus.LostAccess
        }
    }

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = sequence {
        require(source.platformKind == LibraryPlatformKind.AndroidSafTree) {
            "AndroidSafSourceAccess can only scan AndroidSafTree sources"
        }
        removeLegacyPersistentMetadataCache(source)
        val rootUri = Uri.parse(source.handle)
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: error("Cannot open SAF tree: ${source.handle}")
        require(root.canRead()) { "No read access to SAF tree: ${source.displayName}" }
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
        val displayPath = pathSegments.joinToString("/").ifBlank { source.displayName }
        yield(PlatformScanEvent.FolderVisited(displayPath))
        document.listFiles()
            .sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
            .forEach { child ->
                val name = child.name ?: child.uri.lastPathSegment ?: "unnamed"
                yieldAll(scanDocumentTree(context, source, child, pathSegments + name))
            }
    } else if (document.isFile) {
        val name = pathSegments.lastOrNull() ?: document.name ?: document.uri.lastPathSegment ?: "unnamed"
        val key = pathSegments.sourceLocalKey().ifBlank { document.uri.toString() }
        val displayPath = pathSegments.joinToString("/").ifBlank { name }
        val playbackSource = AudioSource.Uri(document.uri.toString())
        val metadataDescriptor = if (isSupportedAudioName(name)) {
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
                metadataAudioSource = metadataDescriptor?.let { AudioSource.FileDescriptor(it.fd, name) } ?: playbackSource,
                cleanupMetadataAudioSource = metadataDescriptor?.let { descriptor -> { descriptor.close() } },
                sizeBytes = document.length().takeIf { it >= 0L },
                modifiedAtEpochMillis = document.lastModified().takeIf { it > 0L },
            ),
        )
    }
}

private fun openDocumentForMetadata(
    context: Context,
    document: DocumentFile,
): android.os.ParcelFileDescriptor? = runCatching {
    context.contentResolver.openFileDescriptor(document.uri, "r")
}.getOrNull()

private fun removeLegacyPersistentMetadataCache(source: LibrarySource) {
    File(LibraryDatabaseContext.applicationContext.cacheDir, "rhythhaus-taglib/${source.id}").deleteRecursively()
}

actual fun createPlatformSourceAccess(): PlatformSourceAccess {
    val context = LibraryDatabaseContext.applicationContext
    return AndroidSafSourceAccess(context)
}
