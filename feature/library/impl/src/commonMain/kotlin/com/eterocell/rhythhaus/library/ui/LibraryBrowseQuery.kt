package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.TrackPlayHistory

/** The metadata field used to order a flat Library presentation. */
public enum class LibrarySort {
    /** Track title. */
    Title,
    /** Track artist. */
    Artist,
    /** Track album. */
    Album,
    /** Library creation time. */
    Added,
    /** Last library metadata modification time. */
    Modified,
    /** Number of recorded plays. */
    PlayCount,
    /** Favorite membership, with favorites first when ascending. */
    Favorite,
}

/** The direction applied to the complete Library sort comparator. */
public enum class LibrarySortDirection {
    /** Lowest or alphabetically first values first. */
    Ascending,
    /** Highest or alphabetically last values first. */
    Descending,
}

/**
 * Ephemeral sorting and filtering preferences for the Library Home surface.
 *
 * The value is feature-owned and intentionally contains no persistence or
 * repository state. A new Library shell starts with [Title] in ascending
 * order and all filters disabled.
 */
public data class LibraryBrowseQuery(
    /** The field used for flat-track ordering. */
    public val sort: LibrarySort = LibrarySort.Title,
    /** The direction applied to the complete comparator. */
    public val direction: LibrarySortDirection = LibrarySortDirection.Ascending,
    /** Whether only tracks in the authoritative favorite set are eligible. */
    public val favoriteOnly: Boolean = false,
    /** Whether only tracks with non-empty artwork bytes are eligible. */
    public val artworkOnly: Boolean = false,
    /** The authoritative source identifier to match, or null for all sources. */
    public val sourceId: String? = null,
)

/**
 * One configured source choice available to the Library Home source filter.
 *
 * [id] stays internal to query callbacks; [displayName] is the authoritative
 * user-visible label supplied by the configured source.
 */
public data class LibraryBrowseSourceOption(
    /** Stable configured source identifier used by the filter callback. */
    public val id: String,
    /** User-visible configured source name. */
    public val displayName: String,
)

/**
 * Projects authoritative tracks into the visible flat Home sequence.
 *
 * Mode membership and query filters are resolved before sorting. A default
 * query preserves the established Favorites, RecentlyPlayed, and
 * RecentlyAdded order; changing the sort key or direction applies the query
 * comparator to that mode's eligible tracks. Albums and Artists are grouped
 * surfaces, so they pass through unchanged; their grouping and detail
 * ordering remain owned by the existing browser helpers.
 * Missing optional map entries remain eligible and sort after known values in
 * ascending order (before them when the complete comparator is reversed).
 */
internal fun visibleTracksForBrowseQuery(
    tracks: List<Track>,
    browseMode: BrowseMode,
    query: LibraryBrowseQuery = LibraryBrowseQuery(),
    favoriteTrackIds: Set<String> = emptySet(),
    sourceIdByTrackId: Map<String, String?> = emptyMap(),
    createdAtByTrackId: Map<String, Long?> = emptyMap(),
    modifiedAtByTrackId: Map<String, Long?> = emptyMap(),
    playHistory: Map<String, TrackPlayHistory> = emptyMap(),
): List<Track> {
    if (!browseMode.isFlatHomeBrowseMode()) {
        return tracks
    }

    val modeTracks =
        when (browseMode) {
            BrowseMode.Favorites -> tracks.filter { it.id in favoriteTrackIds }
            BrowseMode.RecentlyPlayed -> tracks.filter { it.id in playHistory }
            BrowseMode.RecentlyAdded -> tracks
            BrowseMode.Songs -> tracks
            BrowseMode.Albums,
            BrowseMode.Artists,
            -> tracks
        }

    val filteredTracks =
        modeTracks.filter { track ->
            (!query.favoriteOnly || track.id in favoriteTrackIds) &&
                (!query.artworkOnly || track.artworkBytes?.isNotEmpty() == true) &&
                (query.sourceId == null || sourceIdByTrackId[track.id] == query.sourceId)
        }

    val preservesModeOrder =
        browseMode != BrowseMode.Songs &&
            query.sort == LibrarySort.Title &&
            query.direction == LibrarySortDirection.Ascending
    if (preservesModeOrder) {
        return when (browseMode) {
            BrowseMode.Favorites -> filteredTracks
            BrowseMode.RecentlyPlayed ->
                filteredTracks.sortedWith(recentlyPlayedComparator(playHistory))
            BrowseMode.RecentlyAdded ->
                filteredTracks.sortedWith(recentlyAddedComparator(createdAtByTrackId))
            BrowseMode.Albums,
            BrowseMode.Artists,
            BrowseMode.Songs,
            -> filteredTracks
        }
    }

    return filteredTracks.sortedWith(
        queryComparator(
            query = query,
            favoriteTrackIds = favoriteTrackIds,
            createdAtByTrackId = createdAtByTrackId,
            modifiedAtByTrackId = modifiedAtByTrackId,
            playHistory = playHistory,
        ),
    )
}

private fun queryComparator(
    query: LibraryBrowseQuery,
    favoriteTrackIds: Set<String>,
    createdAtByTrackId: Map<String, Long?>,
    modifiedAtByTrackId: Map<String, Long?>,
    playHistory: Map<String, TrackPlayHistory>,
): Comparator<Track> {
    val ascendingComparator =
        Comparator<Track> { left, right ->
            comparePrimaryValues(
                left = left,
                right = right,
                sort = query.sort,
                favoriteTrackIds = favoriteTrackIds,
                createdAtByTrackId = createdAtByTrackId,
                modifiedAtByTrackId = modifiedAtByTrackId,
                playHistory = playHistory,
            )
                .takeUnless { it == 0 }
                ?: compareTieBreakers(left, right)
        }

    return if (query.direction == LibrarySortDirection.Ascending) {
        ascendingComparator
    } else {
        Comparator { left, right -> ascendingComparator.compare(right, left) }
    }
}

private fun comparePrimaryValues(
    left: Track,
    right: Track,
    sort: LibrarySort,
    favoriteTrackIds: Set<String>,
    createdAtByTrackId: Map<String, Long?>,
    modifiedAtByTrackId: Map<String, Long?>,
    playHistory: Map<String, TrackPlayHistory>,
): Int =
    when (sort) {
        LibrarySort.Title -> compareIgnoreCase(left.title, right.title)
        LibrarySort.Artist -> compareIgnoreCase(left.artist, right.artist)
        LibrarySort.Album -> compareIgnoreCase(left.album, right.album)
        LibrarySort.Added ->
            compareOptionalLong(
                createdAtByTrackId[left.id],
                createdAtByTrackId[right.id],
            )
        LibrarySort.Modified ->
            compareOptionalLong(
                modifiedAtByTrackId[left.id],
                modifiedAtByTrackId[right.id],
            )
        LibrarySort.PlayCount ->
            compareOptionalLong(
                playHistory[left.id]?.playCount,
                playHistory[right.id]?.playCount,
            )
        LibrarySort.Favorite ->
            compareFavorites(
                left.id in favoriteTrackIds,
                right.id in favoriteTrackIds,
            )
    }

private fun compareTieBreakers(left: Track, right: Track): Int {
    var comparison = compareIgnoreCase(left.title, right.title)
    if (comparison != 0) return comparison

    comparison = compareIgnoreCase(left.artist, right.artist)
    if (comparison != 0) return comparison

    comparison = compareIgnoreCase(left.album, right.album)
    if (comparison != 0) return comparison

    comparison = compareIgnoreCase(left.id, right.id)
    if (comparison != 0) return comparison

    return left.id.compareTo(right.id)
}

private fun compareFavorites(leftIsFavorite: Boolean, rightIsFavorite: Boolean): Int =
    when {
        leftIsFavorite == rightIsFavorite -> 0
        leftIsFavorite -> -1
        else -> 1
    }

private fun compareOptionalLong(left: Long?, right: Long?): Int =
    when {
        left == null && right == null -> 0
        left == null -> 1
        right == null -> -1
        else -> left.compareTo(right)
    }

private fun compareIgnoreCase(left: String, right: String): Int =
    left.compareTo(right, ignoreCase = true)

private fun recentlyPlayedComparator(
    playHistory: Map<String, TrackPlayHistory>,
): Comparator<Track> =
    Comparator { left, right ->
        var comparison =
            compareOptionalLong(
                playHistory[right.id]?.lastPlayedAtEpochMillis,
                playHistory[left.id]?.lastPlayedAtEpochMillis,
            )
        if (comparison != 0) return@Comparator comparison
        comparison = compareTieBreakers(left, right)
        comparison
    }

private fun recentlyAddedComparator(
    createdAtByTrackId: Map<String, Long?>,
): Comparator<Track> =
    Comparator { left, right ->
        var comparison =
            compareOptionalLong(
                createdAtByTrackId[right.id],
                createdAtByTrackId[left.id],
            )
        if (comparison != 0) return@Comparator comparison
        comparison = compareTieBreakers(left, right)
        comparison
    }
