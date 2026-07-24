package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable

sealed interface PlatformFolderPickResult {
    data class Success(val source: LibrarySource) : PlatformFolderPickResult

    data class Unavailable(val message: String) : PlatformFolderPickResult

    data class Failure(val message: String, val cause: String? = null) :
        PlatformFolderPickResult
}

interface PlatformFolderPickerLauncher {
    val isAvailable: Boolean
    val supportsAdditionalSources: Boolean

    fun launch()
}

fun sourcePickerActionVisible(
    supportsAdditionalSources: Boolean,
    sourceCount: Int,
): Boolean = supportsAdditionalSources || sourceCount == 0

fun sourceMutationsAllowed(
    isProgressActive: Boolean,
    isJobActive: Boolean,
): Boolean = !isProgressActive && !isJobActive

fun emptyLibrarySourceMutationsAllowed(
    isProgressActive: Boolean,
    isJobActive: Boolean,
): Boolean = sourceMutationsAllowed(isProgressActive, isJobActive)

fun normalizePickedSource(
    pickedSource: LibrarySource,
    existingSources: List<LibrarySource>,
): LibrarySource {
    val existingSource =
        existingSources.firstOrNull { it.handle == pickedSource.handle }
            ?: return pickedSource
    return pickedSource.copy(
        id = existingSource.id,
        createdAtEpochMillis = existingSource.createdAtEpochMillis,
    )
}

fun androidSafSourceId(stableUri: String): String = buildString {
    append("android-saf-uri-")
    stableUri.encodeToByteArray().forEach { byte ->
        append(byte.toUByte().toString(16).padStart(2, '0'))
    }
}

fun jvmFolderSourceId(stableCanonicalPath: String): String = buildString {
    append("jvm-folder-path-")
    stableCanonicalPath.encodeToByteArray().forEach { byte ->
        append(byte.toUByte().toString(16).padStart(2, '0'))
    }
}

@Composable
expect fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher

interface PlatformSourceAccess : PlatformAudioScanner {
    fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus =
        LibrarySourceAccessStatus.Available

    fun releaseAccess(source: LibrarySource) = Unit
}

fun audioCandidateForSourceFile(
    source: LibrarySource,
    sourceLocalKey: String,
    displayPath: String,
    displayName: String,
    audioSource: com.eterocell.rhythhaus.AudioSource,
    metadataAudioSource: com.eterocell.rhythhaus.AudioSource = audioSource,
    cleanupMetadataAudioSource: (() -> Unit)? = null,
    sizeBytes: Long? = null,
    modifiedAtEpochMillis: Long? = null,
): PlatformScanEvent =
    if (isSupportedAudioName(displayName)) {
        PlatformScanEvent.AudioCandidate(
            AudioScanCandidate(
                sourceId = source.id,
                sourceLocalKey = sourceLocalKey.normalizedSourceLocalKey(),
                displayPath = displayPath,
                displayName = displayName,
                audioSource = audioSource,
                metadataAudioSource = metadataAudioSource,
                cleanupMetadataAudioSource = cleanupMetadataAudioSource,
                sizeBytes = sizeBytes,
                modifiedAtEpochMillis = modifiedAtEpochMillis,
            ),
        )
    } else {
        PlatformScanEvent.Skipped(
            sourceLocalKey = sourceLocalKey.normalizedSourceLocalKey(),
            displayPath = displayPath,
            reason = "Unsupported audio type",
            recoverable = false,
        )
    }

fun sourceLocalKey(vararg pathSegments: String): String =
    pathSegments.asList().sourceLocalKey()

fun Iterable<String>.sourceLocalKey(): String =
    joinToString("/") { segment ->
            segment.trim().trim('/').trim('\\')
        }
        .normalizedSourceLocalKey()

fun String.normalizedSourceLocalKey(): String =
    replace('\\', '/').split('/').filter { it.isNotBlank() }.joinToString("/")

expect fun createPlatformSourceAccess(): PlatformSourceAccess
