package com.eterocell.rhythhaus.library

/**
 * ABI-stable integer terminal statuses supplied by the iOS Files import
 * provider and consumed by [iosLibraryImportPickResult].
 *
 * This object lives in the Shared facade (not the library feature) so the
 * generated framework exports it to Swift; the numeric values are part of
 * the Swift/Kotlin ABI and must not change. Swift reports them through
 * `IOSLibraryImportCompletion.complete`.
 */
public object IOSLibraryImportStatus {
    /** Supported audio was imported or already present in managed storage. */
    public const val SUCCESS: Int = 0

    /** The user cancelled the Files picker; no error occurred. */
    public const val CANCELLED: Int = 1

    /** The picker or import provider is unavailable. */
    public const val UNAVAILABLE: Int = 2

    /** Another import operation was already active. */
    public const val OVERLAP: Int = 3

    /** The picker or a copy failed. */
    public const val FAILURE: Int = 4
}

/**
 * Maps an iOS import completion onto the common folder-picker seam result.
 *
 * Success requires at least one imported or duplicate file so an empty or
 * unsupported-only selection never publishes a source; the failure default
 * distinguishes files that failed to copy from files that were unsupported.
 * Cancellation is a distinct no-error terminal outcome and maps to
 * [PlatformFolderPickResult.Cancelled]: no source and no message to surface.
 * Unavailable, overlap, and failure map to recoverable picker results
 * carrying the provider message or a stable default.
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
): PlatformFolderPickResult =
    when (status) {
        IOSLibraryImportStatus.SUCCESS ->
            if (imported > 0 || duplicates > 0) {
                PlatformFolderPickResult.Success(
                    iosAppLocalImportSource(destinationFolderPath),
                    importSummary =
                        LibraryImportSummary(
                            imported = imported,
                            duplicates = duplicates,
                            unsupported = unsupported,
                            failed = failed,
                        ),
                )
            } else if (failed > 0) {
                // Nothing was imported or already present because every copy
                // failed; do not claim the selection was unsupported.
                PlatformFolderPickResult.Failure(
                    message ?: "Selected audio files could not be copied",
                )
            } else {
                PlatformFolderPickResult.Failure(
                    message ?: "No supported audio files were imported",
                )
            }

        IOSLibraryImportStatus.CANCELLED ->
            PlatformFolderPickResult.Cancelled

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
 * Formats a terminal iOS import summary into the transient import message.
 *
 * Plain interim copy that reports every aggregate count; localized copy for
 * the import result is owned by the resource work that follows this seam.
 *
 * @param summary the aggregate counts reported by the import completion.
 */
internal fun iosImportSummaryMessage(summary: LibraryImportSummary): String =
    "Import complete: imported ${summary.imported}, " +
        "duplicates ${summary.duplicates}, " +
        "unsupported ${summary.unsupported}, " +
        "failed ${summary.failed}"

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
 * [ManagedImportFallbackFileName]. The Swift copy policy mirrors this
 * function exactly.
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
 * byte-identical content at the same destination name is a duplicate, and
 * destination identity is case-insensitive because the managed app-local
 * folder lives on APFS by default (`song.mp3` and `Song.mp3` collide).
 * The Swift copy policy mirrors this function exactly.
 */
internal sealed interface ManagedImportDestinationPlan {
    /** The destination name is free and the file should be copied. */
    data class Fresh(val fileName: String) : ManagedImportDestinationPlan

    /**
     * Byte-identical content already exists at the destination name; the
     * reported name is the managed (existing) file identity that matched.
     */
    data class Duplicate(val fileName: String) : ManagedImportDestinationPlan

    /**
     * The destination name holds different content; a deterministic numeric
     * suffix was chosen instead, preserving the incoming name's casing.
     */
    data class Suffixed(val fileName: String) : ManagedImportDestinationPlan
}

/**
 * Plans a managed destination for one imported file without touching disk.
 *
 * Occupancy and duplicate lookups ignore case. When the destination name is
 * occupied by different content, the smallest deterministic numeric suffix
 * (2, 3, ...) that is free (case-insensitively) in [managedFiles] is
 * selected, preserving the extension and the incoming casing. The caller
 * folds each plan back into the managed state between files so batch
 * imports stay deterministic.
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
    val existingName =
        managedFiles.keys.firstOrNull {
            it.equals(destinationName, ignoreCase = true)
        }
    if (existingName == null) {
        return ManagedImportDestinationPlan.Fresh(destinationName)
    }
    if (managedFiles.getValue(existingName).contentEquals(sourceContent)) {
        return ManagedImportDestinationPlan.Duplicate(existingName)
    }
    var suffixIndex = 2
    while (true) {
        val candidate = destinationName.withNumericSuffix(suffixIndex)
        val occupied =
            managedFiles.keys.any { it.equals(candidate, ignoreCase = true) }
        if (!occupied) {
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
