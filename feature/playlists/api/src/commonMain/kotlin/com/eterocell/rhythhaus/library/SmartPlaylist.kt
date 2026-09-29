package com.eterocell.rhythhaus.library

/**
 * Persisted rule definition; membership is derived from current library state.
 */
public sealed interface SmartPlaylistRule {
    /** Currently favored tracks. */
    public data object Favorites : SmartPlaylistRule

    /** Most recently played tracks, bounded by [count]. */
    public data class RecentlyPlayed(val count: Int) : SmartPlaylistRule

    /** Most recently imported tracks, bounded by [count]. */
    public data class RecentlyAdded(val count: Int) : SmartPlaylistRule

    /** Tracks attributed to the exact [artist]. */
    public data class Artist(val artist: String) : SmartPlaylistRule

    /** Tracks in the exact [album] by [artist]. */
    public data class Album(val artist: String, val album: String) :
        SmartPlaylistRule

    /**
     * Entries from a static playlist; duplicates retain their own identities.
     */
    public data class SavedPlaylist(val playlistId: String) : SmartPlaylistRule
}

/** User-named smart rule, independent of static playlist entries. */
public data class SmartPlaylistSummary(
    val id: String,
    val name: String,
    val rule: SmartPlaylistRule,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
