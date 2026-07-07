package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent

enum class BrowseMode { Albums, Artists, Songs }

fun albumGridColumnsForWidth(widthDp: Float): Int = when {
    widthDp >= 900f -> 4
    widthDp >= 560f -> 3
    else -> 2
}

data class AlbumGroup(
    val album: String,
    val tracks: List<Track>,
    val artist: String? = tracks.firstOrNull()?.artist,
)

data class ArtistGroup(
    val artist: String,
    val tracks: List<Track>,
    val albumCount: Int = tracks.map { it.album }.distinct().size,
)

// ----- Repository-level grouping (LibraryTrack) -----

fun groupByAlbum(tracks: List<LibraryTrack>): List<AlbumGroup> = tracks.groupBy { it.album }
    .map { (album, trackList) ->
        AlbumGroup(
            album = album,
            tracks = trackList.sortedWith(
                compareBy<LibraryTrack> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            ).map { it.toUiTrack() },
            artist = trackList.firstOrNull()?.artist,
        )
    }
    .sortedBy { it.album.lowercase() }

fun groupByArtist(tracks: List<LibraryTrack>): List<ArtistGroup> = tracks.groupBy { it.artist }
    .map { (artist, trackList) ->
        ArtistGroup(
            artist = artist,
            tracks = trackList.sortedWith(
                compareBy<LibraryTrack> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            ).map { it.toUiTrack() },
        )
    }
    .sortedBy { it.artist.lowercase() }

// ----- UI-level grouping (Track) -----

fun groupTracksByAlbum(tracks: List<Track>): List<AlbumGroup> = tracks.groupBy { it.album }
    .map { (album, trackList) ->
        AlbumGroup(
            album = album,
            tracks = trackList.sortedWith(
                compareBy<Track> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            ),
            artist = trackList.firstOrNull()?.artist,
        )
    }
    .sortedBy { it.album.lowercase() }

fun groupTracksByArtist(tracks: List<Track>): List<ArtistGroup> = tracks.groupBy { it.artist }
    .map { (artist, trackList) ->
        ArtistGroup(
            artist = artist,
            tracks = trackList.sortedWith(
                compareBy<Track> { it.discNumber ?: 0 }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase() },
            ),
        )
    }
    .sortedBy { it.artist.lowercase() }

// ----- Private helpers -----

internal fun LibraryTrack.toUiTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationSeconds = durationMillis?.div(1_000L)?.toInt() ?: 0,
    accent = TrackAccent(start = 0xFF111018, end = 0xFF776F66),
    source = audioSource,
    trackNumber = trackNumber,
    discNumber = discNumber,
    artworkBytes = artworkBytes,
)
