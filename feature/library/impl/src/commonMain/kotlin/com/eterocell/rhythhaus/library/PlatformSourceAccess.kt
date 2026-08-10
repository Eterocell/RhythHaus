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
