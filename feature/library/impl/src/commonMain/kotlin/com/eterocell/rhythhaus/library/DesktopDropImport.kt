package com.eterocell.rhythhaus.library

/** One recoverable reason an item in a desktop drop was not admitted. */
public enum class DesktopDropFailureReason {
    Unsupported,
    Unreadable,
    NotRegular,
    LimitExceeded,
}

/** Diagnostic for one desktop-drop item that cannot become a library source. */
public data class DesktopDropFailure(
    /** Bounded diagnostic path for the rejected item. */
    public val path: String,
    /** Recoverable admission failure category. */
    public val reason: DesktopDropFailureReason,
)

/**
 * Validated desktop-drop inputs, ready for the App-owned registration and scan
 * coordinator.
 *
 * Sources preserve folder drop order followed by the one stable dropped-files
 * source. Invalid siblings never remove valid sources from this result.
 */
public data class DesktopDropResult(
    /** Validated reference-backed sources in admission order. */
    public val sources: List<LibrarySource>,
    /** Rejected items that do not discard readable siblings. */
    public val failures: List<DesktopDropFailure>,
    /** Canonical duplicate input count. */
    public val duplicatePathCount: Int,
    /** Number of excess inputs rejected without reading their paths. */
    public val overflowEntryCount: Int = 0,
) {
    /** Total rejected inputs, including unstored overflow diagnostics. */
    public val rejectedEntryCount: Int
        get() = failures.size + overflowEntryCount

    /** Whether at least one source was admitted. */
    public val accepted: Boolean
        get() = sources.isNotEmpty()
}

/**
 * Validates local filesystem paths received from a desktop drag event.
 *
 * JVM validates and constructs the source handles. Other platforms expose an
 * empty result because their application shells do not send desktop drop
 * events.
 *
 * @param paths local filesystem paths received from the native window.
 * @param createdAtEpochMillis creation timestamp for newly admitted sources.
 */
public expect fun validateDesktopDrop(
    paths: List<String>,
    createdAtEpochMillis: Long,
): DesktopDropResult
