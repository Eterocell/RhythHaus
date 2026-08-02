package com.eterocell.rhythhaus.library

/** An ordered track occurrence in a playlist. */
public data class PlaylistEntry(
    /** Stable entry identifier. */
    public val id: String,
    /** Owning playlist identifier. */
    public val playlistId: String,
    /** Referenced track identifier. */
    public val trackId: String,
    /** Zero-based playlist position. */
    public val position: Int,
    /** Creation time in epoch milliseconds. */
    public val createdAtEpochMillis: Long,
)

/** One playlist requested by an import operation. */
public data class PlaylistImportMutation(
    /** Requested playlist name. */
    public val name: String,
    /** Ordered referenced track identifiers. */
    public val trackIds: List<String>,
)

/** Public playlist projection independent of the persistence row type. */
public data class PlaylistSummary(
    /** Stable playlist identifier. */
    public val id: String,
    /** User-visible playlist name. */
    public val name: String,
    /** Creation time in epoch milliseconds. */
    public val createdAtEpochMillis: Long,
    /** Last update time in epoch milliseconds. */
    public val updatedAtEpochMillis: Long,
)

/** Stable persistence boundary for playlists and ordered entries. */
public interface PlaylistRepository {
    /** Returns all playlists. */
    public fun playlists(): List<PlaylistSummary>

    /** Returns one playlist when it exists. */
    public fun playlist(id: String): PlaylistSummary?

    /** Returns ordered entries for a playlist. */
    public fun entries(playlistId: String): List<PlaylistEntry>

    /** Creates an empty playlist. */
    public fun create(name: String): PlaylistSummary

    /** Creates a playlist with ordered entries. */
    public fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): PlaylistSummary

    /** Imports playlists and their ordered entries. */
    public fun importPlaylists(
        playlists: List<PlaylistImportMutation>
    ): List<PlaylistSummary>

    /** Renames a playlist. */
    public fun rename(id: String, name: String)

    /** Deletes a playlist and its entries. */
    public fun delete(id: String)

    /** Appends entries to a playlist. */
    public fun append(playlistId: String, trackIds: List<String>)

    /** Removes one playlist entry. */
    public fun removeEntry(entryId: String)

    /** Reorders every entry in a playlist. */
    public fun reorder(playlistId: String, entryIds: List<String>)
}
