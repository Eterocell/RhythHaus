package com.eterocell.rhythhaus.library

/** Reason a remove-missing request was refused before any track mutation. */
public enum class RemoveMissingTracksRejectionReason {
    /** The source is not known to the repository. */
    UnknownSource,
    /** The requested scan is not known to the repository. */
    UnknownScan,
    /** The requested scan belongs to another source. */
    ForeignSource,
    /** The requested scan is not a completed terminal scan. */
    NotCompleted,
    /** The completed scan has no completion timestamp. */
    MissingCompletionTimestamp,
    /** A newer valid completed scan exists for the source. */
    StaleCompletedScan,
}

/** Outcome of validating and removing tracks missing from a completed scan. */
public sealed interface RemoveMissingTracksResult {
    /** Number of tracks removed after successful validation. */
    public data class Removed(
        /** Number of deleted tracks. */
        public val count: Int,
    ) : RemoveMissingTracksResult

    /** Request was rejected and no tracks were deleted. */
    public data class Rejected(
        /** Validation reason for the rejected request. */
        public val reason: RemoveMissingTracksRejectionReason,
    ) : RemoveMissingTracksResult
}
