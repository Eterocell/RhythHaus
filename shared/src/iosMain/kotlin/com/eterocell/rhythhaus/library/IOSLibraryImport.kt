package com.eterocell.rhythhaus.library

/**
 * Swift-owned iOS Files.app import provider retained by [IOSLibraryImportBridge].
 *
 * Declared in the Shared framework facade (alongside the playlist-backup
 * document bridge in `shared/src/iosMain`) so the generated `Shared`
 * framework exports it to Swift. The iOS host implements this interface and
 * registers it before Compose startup. Swift owns picker presentation,
 * security-scoped access, recursive enumeration, and copying into the managed
 * app-local music folder; Kotlin owns status mapping and orchestration.
 * External security-scoped URLs never cross this ABI: the provider receives
 * only the managed [destinationPath] and reports aggregate counters.
 */
public interface IOSLibraryImportProvider {
    /**
     * Imports supported audio found in Files.app into [destinationPath].
     *
     * Implementations MUST invoke [completion] exactly once on the main
     * queue with an [IOSLibraryImportStatus] value and aggregate counters.
     *
     * @param destinationPath the managed app-local music folder path.
     * @param completion the terminal completion callback.
     */
    public fun importAudio(
        destinationPath: String,
        completion: IOSLibraryImportCompletion,
    )
}

/**
 * Terminal callback delivered once by [IOSLibraryImportProvider.importAudio].
 *
 * All parameters are primitives so the callback stays Swift-compatible; the
 * Kotlin facade maps them with `iosLibraryImportPickResult` and never
 * receives external file URLs.
 */
public interface IOSLibraryImportCompletion {
    /**
     * Completes an import operation.
     *
     * @param status a stable [IOSLibraryImportStatus] value.
     * @param imported the number of supported files copied.
     * @param duplicates the number of byte-identical files already managed.
     * @param unsupported the number of selected files with unsupported types.
     * @param failed the number of files that could not be copied.
     * @param message an optional user-facing message for error statuses.
     */
    public fun complete(
        status: Int,
        imported: Int,
        duplicates: Int,
        unsupported: Int,
        failed: Int,
        message: String?,
    )
}

/**
 * Retains the currently injected iOS import provider.
 *
 * The iOS bootstrap assigns [provider] before the Compose UI starts, mirroring
 * the playlist backup document bridge in the same facade.
 */
public object IOSLibraryImportBridge {
    /** The Swift-owned import provider, or null before registration. */
    public var provider: IOSLibraryImportProvider? = null
}
