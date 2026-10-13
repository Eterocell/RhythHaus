package com.eterocell.rhythhaus.library

/** Platform family that owns a library source handle. */
public enum class LibraryPlatformKind {
    /** Android Storage Access Framework tree. */
    AndroidSafTree,

    /** Android read-only MediaStore audio collection. */
    AndroidMediaStoreAudio,

    /** JVM filesystem folder. */
    JvmFolder,

    /** iOS app-local folder. */
    IosAppLocal,
}

/** Current accessibility of a library source. */
public enum class LibrarySourceAccessStatus {
    /** The source can be accessed. */
    Available,

    /** Permission or handle access was lost. */
    LostAccess,
}

/** A configured local library source. */
public data class LibrarySource(
    /** Stable source identifier. */
    public val id: String,
    /** Platform family for [handle]. */
    public val platformKind: LibraryPlatformKind,
    /** User-visible source name. */
    public val displayName: String,
    /** Platform-specific source handle. */
    public val handle: String,
    /** Creation time in epoch milliseconds. */
    public val createdAtEpochMillis: Long,
    /** Most recent scan time in epoch milliseconds. */
    public val lastScanAtEpochMillis: Long? = null,
    /** Current permission/access state. */
    public val accessStatus: LibrarySourceAccessStatus =
        LibrarySourceAccessStatus.Available,
)

/** Result of attempting to rebind a configured source to a new handle. */
public sealed interface SourceRebindResult {
    /** The source was updated without changing its stable identity. */
    public data class Rebound(
        /** The updated source row. */
        public val source: LibrarySource,
    ) : SourceRebindResult

    /** The requested source is not persisted. */
    public data object UnknownSource : SourceRebindResult

    /** The replacement belongs to a different platform family. */
    public data object PlatformMismatch : SourceRebindResult

    /** Another configured source already owns the replacement handle. */
    public data object HandleConflict : SourceRebindResult
}

/** The stable read-only Android MediaStore audio source. */
public fun androidMediaStoreAudioSource(
    createdAtEpochMillis: Long,
): LibrarySource =
    LibrarySource(
        id = "android-mediastore-audio",
        platformKind = LibraryPlatformKind.AndroidMediaStoreAudio,
        displayName = "Device audio",
        handle = "android-mediastore-audio",
        createdAtEpochMillis = createdAtEpochMillis,
    )
