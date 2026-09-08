package com.eterocell.rhythhaus.library

import androidx.compose.runtime.Composable
import com.eterocell.rhythhaus.library.impl.PlatformAudioScanner

/** Outcome of a platform folder-picker launch. */
sealed interface PlatformFolderPickResult {
    /** A folder was picked and converted to a library source. */
    data class Success(val source: LibrarySource) : PlatformFolderPickResult

    /** The picker is unavailable on this platform. */
    data class Unavailable(val message: String) : PlatformFolderPickResult

    /** The picker failed while choosing a folder. */
    data class Failure(val message: String, val cause: String? = null) :
        PlatformFolderPickResult
}

/** Launches the platform folder picker and reports its result. */
interface PlatformFolderPickerLauncher {
    /** Whether the platform picker can be shown. */
    val isAvailable: Boolean

    /** Whether additional sources can be picked after the first one. */
    val supportsAdditionalSources: Boolean

    /** Launches the folder picker. */
    fun launch()
}

/**
 * Whether the source picker action is visible for the given source count.
 *
 * @param supportsAdditionalSources whether the platform allows extra sources.
 * @param sourceCount the number of configured library sources.
 */
fun sourcePickerActionVisible(
    supportsAdditionalSources: Boolean,
    sourceCount: Int,
): Boolean = supportsAdditionalSources || sourceCount == 0

/**
 * Whether source mutations are allowed given active scan and job state.
 *
 * @param isProgressActive whether a scan is currently active.
 * @param isJobActive whether a scan job is currently active.
 */
fun sourceMutationsAllowed(
    isProgressActive: Boolean,
    isJobActive: Boolean,
): Boolean = !isProgressActive && !isJobActive

/**
 * Whether empty-library source mutations are allowed.
 *
 * @param isProgressActive whether a scan is currently active.
 * @param isJobActive whether a scan job is currently active.
 */
fun emptyLibrarySourceMutationsAllowed(
    isProgressActive: Boolean,
    isJobActive: Boolean,
): Boolean = sourceMutationsAllowed(isProgressActive, isJobActive)

/**
 * Reuses the identity of an existing source when the picked source matches it.
 *
 * @param pickedSource the source produced by the folder picker.
 * @param existingSources the currently configured library sources.
 */
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

/**
 * Derives a stable source identifier for an Android SAF tree URI.
 *
 * @param stableUri the persisted SAF tree URI string.
 */
fun androidSafSourceId(stableUri: String): String = buildString {
    append("android-saf-uri-")
    stableUri.encodeToByteArray().forEach { byte ->
        append(byte.toUByte().toString(16).padStart(2, '0'))
    }
}

/**
 * Derives a stable source identifier for a JVM folder path.
 *
 * @param stableCanonicalPath the canonical path of the chosen folder.
 */
fun jvmFolderSourceId(stableCanonicalPath: String): String = buildString {
    append("jvm-folder-path-")
    stableCanonicalPath.encodeToByteArray().forEach { byte ->
        append(byte.toUByte().toString(16).padStart(2, '0'))
    }
}

/**
 * Creates the platform folder picker launcher for this platform.
 *
 * @param onResult callback invoked with the folder-pick result.
 */
@Composable
expect fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher

/** Platform access to a library source, including scanning its contents. */
interface PlatformSourceAccess : PlatformAudioScanner {
    /**
     * Returns the current access status for the given source.
     *
     * @param source the library source to inspect.
     */
    fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus =
        LibrarySourceAccessStatus.Available

    /**
     * Releases platform access held for the given source.
     *
     * @param source the library source to release.
     */
    fun releaseAccess(source: LibrarySource) = Unit
}

/**
 * ABI-stable integer terminal statuses supplied by the iOS Files import
 * provider and consumed by [iosLibraryImportPickResult].
 *
 * The numeric values are part of the Swift/Kotlin ABI and must not change;
 * Swift reports them through `IOSLibraryImportCompletion.complete`.
 */
object IOSLibraryImportStatus {
    /** Supported audio was imported or already present in managed storage. */
    const val SUCCESS = 0

    /** The user cancelled the Files picker; no error occurred. */
    const val CANCELLED = 1

    /** The picker or import provider is unavailable. */
    const val UNAVAILABLE = 2

    /** Another import operation was already active. */
    const val OVERLAP = 3

    /** The picker or a copy failed. */
    const val FAILURE = 4
}

/**
 * Maps an iOS import completion onto the common folder-picker seam result.
 *
 * Success requires at least one imported or duplicate file so an empty or
 * unsupported-only selection never publishes a source. Cancellation is a
 * distinct no-error terminal outcome and returns null: no source and no
 * error to surface. Unavailable, overlap, and failure map to recoverable
 * picker results carrying the provider message or a stable default.
 *
 * @param destinationFolderPath the managed app-local folder the provider
 * copied into; becomes the returned source handle on success.
 * @param status the terminal [IOSLibraryImportStatus] reported by the
 * provider.
 * @param imported the number of supported files copied.
 * @param duplicates the number of byte-identical files already managed.
 * @param unsupported the number of selected files with unsupported types.
 * @param failed the number of files that could not be copied.
 * @param message an optional provider message for error statuses.
 */
internal fun iosLibraryImportPickResult(
    destinationFolderPath: String,
    status: Int,
    imported: Int,
    duplicates: Int,
    unsupported: Int,
    failed: Int,
    message: String?,
): PlatformFolderPickResult? =
    when (status) {
        IOSLibraryImportStatus.SUCCESS ->
            if (imported > 0 || duplicates > 0) {
                PlatformFolderPickResult.Success(
                    iosAppLocalImportSource(destinationFolderPath),
                )
            } else {
                PlatformFolderPickResult.Failure(
                    message ?: "No supported audio files were imported",
                )
            }

        IOSLibraryImportStatus.CANCELLED -> null

        IOSLibraryImportStatus.UNAVAILABLE ->
            PlatformFolderPickResult.Unavailable(
                message ?: "iOS import provider is unavailable",
            )

        IOSLibraryImportStatus.OVERLAP ->
            PlatformFolderPickResult.Failure(
                message ?: "Another import is already active",
            )

        IOSLibraryImportStatus.FAILURE ->
            PlatformFolderPickResult.Failure(
                message ?: "Could not import audio from Files",
            )

        else ->
            PlatformFolderPickResult.Failure(
                message ?: "Unknown iOS import status: $status",
            )
    }

/**
 * Builds the managed iOS app-local source for a successful import.
 *
 * @param managedFolderPath the managed folder the provider copied into.
 */
private fun iosAppLocalImportSource(managedFolderPath: String): LibrarySource =
    LibrarySource(
        id = "ios-app-local",
        platformKind = LibraryPlatformKind.IosAppLocal,
        displayName = "RhythHaus",
        handle = managedFolderPath,
        createdAtEpochMillis = 0L,
    )

/** Fallback managed file name when a source name sanitizes to nothing. */
internal const val ManagedImportFallbackFileName = "imported-audio"

/**
 * Reduces an external source file name to one safe managed file name.
 *
 * Only the last path segment is kept, so separators and traversal never
 * escape the managed folder; the extension and other useful characters are
 * preserved. Blank, separator-only, and traversal-only names fall back to
 * [ManagedImportFallbackFileName].
 *
 * @param sourceFileName the external file name supplied by the picker.
 */
internal fun managedImportFileName(sourceFileName: String): String {
    val lastSegment =
        sourceFileName
            .replace('\\', '/')
            .substringAfterLast('/')
            .trim()
    return when {
        lastSegment.isEmpty() ||
            lastSegment == "." ||
            lastSegment == ".." -> ManagedImportFallbackFileName

        else -> lastSegment
    }
}

/**
 * Pure destination decision for one imported source file.
 *
 * Duplicate safety is defined by the managed destination identity: only
 * byte-identical content at the same destination name is a duplicate.
 */
internal sealed interface ManagedImportDestinationPlan {
    /** The destination name is free and the file should be copied. */
    data class Fresh(val fileName: String) : ManagedImportDestinationPlan

    /** Byte-identical content already exists at the destination name. */
    data class Duplicate(val fileName: String) : ManagedImportDestinationPlan

    /**
     * The destination name holds different content; a deterministic numeric
     * suffix was chosen instead.
     */
    data class Suffixed(val fileName: String) : ManagedImportDestinationPlan
}

/**
 * Plans a managed destination for one imported file without touching disk.
 *
 * When the destination name is occupied by different content, the smallest
 * deterministic numeric suffix (2, 3, ...) that is free in [managedFiles] is
 * selected, preserving the file extension. The caller folds the plan back
 * into the managed state between files so batch imports stay deterministic.
 *
 * @param sourceFileName the external file name supplied by the picker.
 * @param sourceContent the bytes about to be imported.
 * @param managedFiles the current managed destination name to content map.
 */
internal fun managedImportDestinationPlan(
    sourceFileName: String,
    sourceContent: ByteArray,
    managedFiles: Map<String, ByteArray>,
): ManagedImportDestinationPlan {
    val destinationName = managedImportFileName(sourceFileName)
    val existingContent = managedFiles[destinationName]
    if (existingContent == null) {
        return ManagedImportDestinationPlan.Fresh(destinationName)
    }
    if (existingContent.contentEquals(sourceContent)) {
        return ManagedImportDestinationPlan.Duplicate(destinationName)
    }
    var suffixIndex = 2
    while (true) {
        val candidate = destinationName.withNumericSuffix(suffixIndex)
        if (!managedFiles.containsKey(candidate)) {
            return ManagedImportDestinationPlan.Suffixed(candidate)
        }
        suffixIndex += 1
    }
}

private fun String.withNumericSuffix(index: Int): String {
    val extensionStart = lastIndexOf('.')
    return if (extensionStart > 0) {
        substring(0, extensionStart) + "-$index" + substring(extensionStart)
    } else {
        "$this-$index"
    }
}
