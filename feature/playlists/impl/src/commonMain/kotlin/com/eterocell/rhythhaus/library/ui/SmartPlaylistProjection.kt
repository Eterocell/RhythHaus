package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import com.eterocell.rhythhaus.library.TrackPlayHistory

/** One derived occurrence; its identity survives membership reordering. */
public data class SmartPlaylistRow(
    val occurrenceId: String,
    val trackId: String
)

/** Derived current members and an explicit missing static-source condition. */
public data class SmartPlaylistProjection(
    val rows: List<SmartPlaylistRow>,
    val sourceMissing: Boolean = false,
)

/** Evaluates a stored rule against one immutable authoritative library view. */
public fun projectSmartPlaylist(
    playlist: SmartPlaylistSummary,
    tracks: List<LibraryTrack>,
    favoriteTrackIds: Set<String>,
    playHistory: Map<String, TrackPlayHistory>,
    snapshot: PlaylistSnapshot,
): SmartPlaylistProjection {
    val rule = playlist.rule
    if (rule is SmartPlaylistRule.SavedPlaylist) {
        if (snapshot.playlist(rule.playlistId) == null) {
            return SmartPlaylistProjection(emptyList(), sourceMissing = true)
        }
        val currentIds = tracks.mapTo(HashSet(tracks.size), LibraryTrack::id)
        return SmartPlaylistProjection(
            snapshot.entries(rule.playlistId).mapNotNull { entry ->
                if (entry.trackId in currentIds)
                    SmartPlaylistRow(
                        smartOccurrenceId(playlist.id, entry.id), entry.trackId)
                else null
            })
    }
    val titleOrder =
        compareBy<LibraryTrack> { it.title.lowercase() }.thenBy { it.id }
    val eligible =
        when (rule) {
            SmartPlaylistRule.Favorites ->
                tracks
                    .filter { it.id in favoriteTrackIds }
                    .sortedWith(titleOrder)
            is SmartPlaylistRule.Artist ->
                tracks
                    .filter { it.artist == rule.artist }
                    .sortedWith(titleOrder)
            is SmartPlaylistRule.Album ->
                tracks
                    .filter {
                        it.artist == rule.artist && it.album == rule.album
                    }
                    .sortedWith(titleOrder)
            is SmartPlaylistRule.RecentlyPlayed ->
                tracks
                    .filter { it.id in playHistory }
                    .sortedWith(
                        compareByDescending<LibraryTrack> {
                                playHistory
                                    .getValue(it.id)
                                    .lastPlayedAtEpochMillis
                            }
                            .thenBy { it.id })
                    .take(rule.count)
            is SmartPlaylistRule.RecentlyAdded ->
                tracks
                    .sortedWith(
                        compareByDescending<LibraryTrack> {
                                it.createdAtEpochMillis
                            }
                            .thenBy { it.id })
                    .take(rule.count)
            is SmartPlaylistRule.SavedPlaylist ->
                error("Saved playlist rule already handled")
        }
    return SmartPlaylistProjection(
        eligible.map {
            SmartPlaylistRow(smartOccurrenceId(playlist.id, it.id), it.id)
        })
}

private fun smartOccurrenceId(playlistId: String, memberId: String): String =
    "smart:${playlistId.length}:$playlistId:$memberId"
