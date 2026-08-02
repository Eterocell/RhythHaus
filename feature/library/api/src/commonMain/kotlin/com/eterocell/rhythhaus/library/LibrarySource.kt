package com.eterocell.rhythhaus.library

/** Platform family that owns a library source handle. */
public enum class LibraryPlatformKind {
    /** Android Storage Access Framework tree. */
    AndroidSafTree,

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
