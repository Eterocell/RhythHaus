package com.eterocell.rhythhaus.playlistbackup

internal const val PlaylistBackupFileExtension = ".rhythhaus-playlists.json"
internal const val PlaylistBackupMimeType =
    "application/vnd.rhythhaus.playlists+json"
internal const val PlaylistBackupJsonMimeType = "application/json"
internal const val PlaylistBackupMaxBytes = 4 * 1024 * 1024

/** Supported user-facing playlist document formats. */
public enum class PlaylistDocumentFormat(
    /** Default file extension used by save panels. */
    public val extension: String,
    /** MIME types accepted by open panels, in preference order. */
    public val mimeTypes: List<String>,
) {
    RhythHausJson(
        ".rhythhaus-playlists.json",
        listOf(PlaylistBackupMimeType, PlaylistBackupJsonMimeType),
    ),
    M3u(".m3u", listOf("audio/x-mpegurl", "audio/mpegurl")),
    M3u8(".m3u8", listOf("audio/x-mpegurl", "application/vnd.apple.mpegurl")),
    Pls(".pls", listOf("audio/x-scpls")),
}

/**
 * Immutable terminal save-panel outcome.
 *
 * Its variants represent exactly one terminal callback from the platform panel;
 * value-bearing variants use value equality for their diagnostic messages.
 */
public sealed interface PlaylistBackupDocumentSaveResult {
    /** The platform persisted the requested backup document. */
    public data object Success : PlaylistBackupDocumentSaveResult

    /** The user dismissed the save panel without persisting a document. */
    public data object Cancelled : PlaylistBackupDocumentSaveResult

    /**
     * The save panel could not be presented; [message] identifies the
     * unavailable capability.
     */
    public data class Unavailable(val message: String) :
        PlaylistBackupDocumentSaveResult

    /**
     * The save attempt failed after presentation; [message] is its user-visible
     * diagnostic.
     */
    public data class Failure(val message: String) :
        PlaylistBackupDocumentSaveResult
}

/**
 * Immutable terminal open-panel outcome.
 *
 * Its variants represent exactly one terminal callback from the platform panel;
 * [Success] retains the selected bytes and value-bearing variants use value
 * equality.
 */
public sealed interface PlaylistBackupDocumentOpenResult {
    /** The selected document was read as [bytes]. */
    public data class Success(val bytes: ByteArray) :
        PlaylistBackupDocumentOpenResult

    /** The user dismissed the open panel without selecting a document. */
    public data object Cancelled : PlaylistBackupDocumentOpenResult

    /**
     * The open panel could not be presented; [message] identifies the
     * unavailable capability.
     */
    public data class Unavailable(val message: String) :
        PlaylistBackupDocumentOpenResult

    /** The selected document exceeded the enforced [maxBytes] bound. */
    public data class TooLarge(val maxBytes: Int) :
        PlaylistBackupDocumentOpenResult

    /**
     * Reading the selected document failed; [message] is its user-visible
     * diagnostic.
     */
    public data class Failure(val message: String) :
        PlaylistBackupDocumentOpenResult
}

/**
 * Presents platform document UI while controller state remains feature-owned.
 *
 * [save] and [open] only request presentation. Shared routes each terminal
 * platform callback exactly once to the feature controller rather than exposing
 * callbacks through this port.
 */
public interface PlaylistBackupDocumentLauncher {
    /** Whether this launcher can currently present a backup document panel. */
    public val isAvailable: Boolean

    /**
     * Requests a save panel for validated [bytes] using [suggestedFileName] as
     * its hint.
     */
    public fun save(suggestedFileName: String, bytes: ByteArray)

    /**
     * Requests a save panel filtered for [format]; legacy launchers may use
     * their default filter.
     */
    public fun save(
        suggestedFileName: String,
        bytes: ByteArray,
        format: PlaylistDocumentFormat,
    ) = save(suggestedFileName, bytes)

    /**
     * Requests an open panel whose eventual terminal result is routed by
     * Shared.
     */
    public fun open()

    /**
     * Requests an open panel filtered for [format]; legacy launchers may use
     * their default filter.
     */
    public fun open(format: PlaylistDocumentFormat) = open()
}

/**
 * Creates the serialized controller for one [launcher] and authoritative
 * [revisionGuard].
 *
 * The controller gates overlapping operations, suppresses duplicate terminal
 * delivery, bounds documents to 4 MiB through the platform results, maps
 * ordinary failures, and rethrows cancellation without settlement.
 */
public fun createPlaylistBackupController(
    owner: com.eterocell.rhythhaus.library.ui.PlaylistStateOwner,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    launcher: PlaylistBackupDocumentLauncher,
    revisionGuard: PlaylistBackupRevisionGuard,
): PlaylistBackupController =
    PlaylistBackupController(owner, dispatcher, launcher, revisionGuard)

internal fun playlistBackupFileName(suggestedFileName: String): String {
    val safe =
        suggestedFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "rhythhaus-playlists"
    return if (safe.endsWith(PlaylistBackupFileExtension, true)) safe
    else safe + PlaylistBackupFileExtension
}

internal fun playlistDocumentFileName(
    suggestedFileName: String,
    format: PlaylistDocumentFormat,
): String {
    val safe =
        suggestedFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "rhythhaus-playlist"
    return if (safe.endsWith(format.extension, ignoreCase = true)) safe
    else safe + format.extension
}

internal class PlaylistBackupDocumentOperationGate {
    private var active = false
    val isActive
        get() = active

    fun tryStart() = (!active).also { if (it) active = true }

    fun finish() {
        active = false
    }
}
