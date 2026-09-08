package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.impl.PlatformAudioScanner

/** Aggregate terminal counts of an iOS Files import completion. */
data class LibraryImportSummary(
    /** Number of supported files copied into managed storage. */
    val imported: Int,
    /** Number of byte-identical files already present in managed storage. */
    val duplicates: Int,
    /** Number of selected files with unsupported types. */
    val unsupported: Int,
    /** Number of selected files that could not be copied. */
    val failed: Int,
)

/** Outcome of a platform folder-picker launch. */
sealed interface PlatformFolderPickResult {
    /**
     * A folder was picked and converted to a library source.
     *
     * @param importSummary terminal aggregate counts when the picker imported
     *   files into managed storage (the iOS Files import launcher); folder
     *   pickers on other platforms leave it null.
     */
    data class Success(
        val source: LibrarySource,
        val importSummary: LibraryImportSummary? = null,
    ) : PlatformFolderPickResult

    /** The user cancelled the picker; no source and no error occurred. */
    data object Cancelled : PlatformFolderPickResult

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

    /**
     * Whether a platform import operation is currently active (picker shown
     * or files being copied into managed storage).
     *
     * The App folds this into source-mutation gating so competing mutations
     * cannot start while an import is in flight, before the follow-up scan is
     * even admitted. Android and JVM folder pickers always report false.
     */
    val isImportActive: Boolean
        get() = false

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
