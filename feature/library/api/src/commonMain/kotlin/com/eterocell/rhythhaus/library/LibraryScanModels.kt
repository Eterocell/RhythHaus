package com.eterocell.rhythhaus.library

/** Lifecycle status for a library scan. */
public enum class ScanStatus {
    /** No scan is active. */
    Idle,
    /** A scan is running. */
    Scanning,
    /** A running scan is being cancelled. */
    Cancelling,
    /** A scan completed successfully. */
    Completed,
    /** A scan was cancelled. */
    Cancelled,
    /** A scan failed. */
    Failed,
}

/** Persisted progress and outcome for one source scan. */
public data class ScanSession(
    /** Stable scan identifier. */
    public val id: String,
    /** Source scanned by this session. */
    public val sourceId: String,
    /** Current lifecycle status. */
    public val status: ScanStatus,
    /** Start time in epoch milliseconds. */
    public val startedAtEpochMillis: Long,
    /** Completion time in epoch milliseconds when terminal. */
    public val completedAtEpochMillis: Long? = null,
    /** Number of folders visited. */
    public val foldersVisited: Int = 0,
    /** Number of files visited. */
    public val filesVisited: Int = 0,
    /** Number of tracks added. */
    public val tracksAdded: Int = 0,
    /** Number of tracks updated. */
    public val tracksUpdated: Int = 0,
    /** Number of files skipped. */
    public val filesSkipped: Int = 0,
    /** Terminal status message when available. */
    public val terminalMessage: String? = null,
)

/** A recoverable or terminal error encountered while scanning. */
public data class ScanError(
    /** Stable error identifier. */
    public val id: String,
    /** Scan that produced the error. */
    public val scanId: String,
    /** Source-local item key. */
    public val sourceLocalKey: String,
    /** User-visible item path. */
    public val displayPath: String,
    /** Failure reason. */
    public val reason: String,
    /** Whether scanning can continue. */
    public val recoverable: Boolean,
    /** Creation time in epoch milliseconds. */
    public val createdAtEpochMillis: Long,
)
