package com.eterocell.rhythhaus.library

/** Persisted playback aggregate for one library track. */
public data class TrackPlayHistory(
    /** Stable library track identifier. */
    public val trackId: String,
    /** Number of recorded plays. */
    public val playCount: Long,
    /** Timestamp of the most recently recorded play. */
    public val lastPlayedAtEpochMillis: Long,
)

/**
 * Stable persistence boundary for Library sources, tracks, artwork, and scans.
 */
public interface LibraryRepository {
    /** Stores or updates a source. */
    public fun upsertSource(source: LibrarySource)

    /** Returns all sources. */
    public fun sources(): List<LibrarySource>

    /** Stores or updates a track. */
    public fun upsertTrack(track: LibraryTrack): TrackUpsertResult

    /** Returns all tracks. */
    public fun tracks(): List<LibraryTrack>

    /** Returns the IDs of currently favorited tracks. */
    public fun favoriteTrackIds(): Set<String>

    /** Returns persisted playback history keyed by track identifier. */
    public fun playHistory(): Map<String, TrackPlayHistory>

    /**
     * Records one play for an existing track.
     *
     * @return `true` when [trackId] identifies an existing track; `false` when
     *   no such track exists and no history row is stored.
     */
    public fun recordTrackPlayed(
        trackId: String,
        playedAtEpochMillis: Long,
    ): Boolean

    /**
     * Stores the requested favorite state for an existing track.
     *
     * Repeating a request for the already-stored state is accepted without
     * changing the resulting favorite membership.
     *
     * @return `true` when [trackId] identifies an existing track; `false` when
     *   no such track exists and no favorite state is stored.
     */
    public fun setTrackFavorite(trackId: String, favorite: Boolean): Boolean

    /** Returns tracks belonging to a source. */
    public fun tracksForSource(sourceId: String): List<LibraryTrack>

    /** Returns artwork for a track when available. */
    public fun artworkForTrack(trackId: String): TrackArtwork?

    /** Stores a scan session. */
    public fun insertScanSession(session: ScanSession)

    /** Updates a scan session. */
    public fun updateScanSession(session: ScanSession)

    /** Stores a scan error. */
    public fun insertScanError(error: ScanError)

    /** Returns errors recorded for a scan. */
    public fun scanErrors(scanId: String): List<ScanError>

    /**
     * Removes tracks for [sourceId] that were not observed by the authoritative
     * completed scan.
     *
     * [requestedScanId] is authoritative: the request is accepted only when it
     * identifies the latest valid completed scan for [sourceId]. Validation and
     * removal use deterministic ordering, and a rejected request leaves all
     * tracks unchanged.
     */
    public fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String,
    ): RemoveMissingTracksResult

    /**
     * Returns the globally latest terminal scan session. Callers load errors
     * with [scanErrors].
     */
    public fun latestTerminalScanSession(): ScanSession?

    /** Removes a source and its owned records. */
    public fun removeSource(sourceId: String)

    /** Clears all library records. */
    public fun clearAll()
}

/** Result of storing a library track. */
public enum class TrackUpsertResult {
    /** A new track was stored. */
    Added,

    /** An existing track was updated. */
    Updated,
}
