package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryTrack

/**
 * Selects internally grouped album, artist, or authoritative song rendering.
 */
public enum class BrowseMode {
    /** Grouped album browsing. */
    Albums,
    /** Grouped artist browsing. */
    Artists,
    /** Flat authoritative song list. */
    Songs
}

/**
 * Returns the album grid column count for the given available width.
 *
 * @param widthDp the available width in density-independent pixels.
 */
public fun albumGridColumnsForWidth(widthDp: Float): Int =
    when {
        widthDp >= 900f -> 4
        widthDp >= 560f -> 3
        else -> 2
    }

internal data class AlbumGroup(
    val album: String,
    val tracks: List<Track>,
    val artist: String? = tracks.firstOrNull()?.artist,
)

internal data class ArtistGroup(
    val artist: String,
    val tracks: List<Track>,
    val albumCount: Int = tracks.map { it.album }.distinct().size,
)

// ----- Repository-level grouping (LibraryTrack) -----

internal fun groupByAlbum(tracks: List<LibraryTrack>): List<AlbumGroup> =
    tracks
        .groupBy { it.album }
        .map { (album, trackList) ->
            AlbumGroup(
                album = album,
                tracks =
                    trackList
                        .sortedWith(
                            compareBy<LibraryTrack> { it.discNumber ?: 0 }
                                .thenBy { it.trackNumber ?: 0 }
                                .thenBy { it.title.lowercase() },
                        )
                        .map { it.toUiTrack() },
                artist = trackList.firstOrNull()?.artist,
            )
        }
        .sortedBy { it.album.lowercase() }

internal fun groupByArtist(tracks: List<LibraryTrack>): List<ArtistGroup> =
    tracks
        .groupBy { it.artist }
        .map { (artist, trackList) ->
            ArtistGroup(
                artist = artist,
                tracks =
                    trackList
                        .sortedWith(
                            compareBy<LibraryTrack> { it.discNumber ?: 0 }
                                .thenBy { it.trackNumber ?: 0 }
                                .thenBy { it.title.lowercase() },
                        )
                        .map { it.toUiTrack() },
            )
        }
        .sortedBy { it.artist.lowercase() }

// ----- UI-level grouping (Track) -----

internal fun groupTracksByAlbum(tracks: List<Track>): List<AlbumGroup> =
    tracks
        .groupBy { it.album }
        .map { (album, trackList) ->
            AlbumGroup(
                album = album,
                tracks =
                    trackList.sortedWith(
                        compareBy<Track> { it.discNumber ?: 0 }
                            .thenBy { it.trackNumber ?: 0 }
                            .thenBy { it.title.lowercase() },
                    ),
                artist = trackList.firstOrNull()?.artist,
            )
        }
        .sortedBy { it.album.lowercase() }

internal fun groupTracksByArtist(tracks: List<Track>): List<ArtistGroup> =
    tracks
        .groupBy { it.artist }
        .map { (artist, trackList) ->
            ArtistGroup(
                artist = artist,
                tracks =
                    trackList.sortedWith(
                        compareBy<Track> { it.discNumber ?: 0 }
                            .thenBy { it.trackNumber ?: 0 }
                            .thenBy { it.title.lowercase() },
                    ),
            )
        }
        .sortedBy { it.artist.lowercase() }

/** Formats a duration in seconds as `m:ss`, clamping negatives to zero. */
internal fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = kotlin.math.max(0, totalSeconds)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Identifies the Shared-owned selection projection destination. */
public sealed interface LibrarySelectionPage {
    /** Shared songs-home selection page. */
    public data object HomeSongs : LibrarySelectionPage

    /** Exact album-name selection page. */
    public data class Album(
        /** The exact album name. */
        public val name: String,
    ) : LibrarySelectionPage

    /** Exact artist-name selection page. */
    public data class Artist(
        /** The exact artist name. */
        public val name: String,
    ) : LibrarySelectionPage
}

/**
 * Raw route data whose feature-owned rendering selects localized detail
 * wording.
 */
public sealed interface LibraryDetailSummary {
    /**
     * Album counts and optional raw artist; null resolves to feature unknown
     * artist.
     */
    public data class Album(
        /** The resolved track count. */
        public val trackCount: Int,
        /** The raw album artist, or null when unknown. */
        public val artist: String?,
    ) : LibraryDetailSummary

    /**
     * Artist counts passed unchanged to feature-owned localized subtitle
     * formatting.
     */
    public data class Artist(
        /** The distinct album count. */
        public val albumCount: Int,
        /** The resolved track count. */
        public val trackCount: Int,
    ) : LibraryDetailSummary
}

/**
 * Shared-owned localized wording consumed by [LibraryHomeContent] and
 * [DrillDownView]. Value equality makes unchanged labels stable across
 * recomposition; callers provide already-localized text and no generated
 * resource handle crosses the boundary.
 *
 * @property addMusicFolder Label for the add-music-folder action.
 * @property folderPickerUnavailable Label shown when the platform picker is
 *   unavailable.
 * @property clearLibrary Label for clearing the library.
 * @property cancel Label for cancelling an active scan.
 * @property playlists Label for the playlists entry point.
 * @property playlistsAccessibility Accessibility description for the playlists
 *   button.
 * @property libraryQueue Heading label for the queue/playlists section.
 * @property albumArt Accessibility label for track-row album thumbnails.
 * @property albumArtwork Accessibility label for album/detail artwork.
 * @property nowPlayingBadge Badge label for the now-playing track.
 * @property selectTrack Composably resolves the localized long-press/content
 *   description for a track title.
 * @property trackArtistAlbum Composably resolves the localized artist-album
 *   subtitle for a track.
 */
public data class LibrarySharedLabels(
    /** Label for the add-music-folder action. */
    public val addMusicFolder: String,
    /** Label shown when the platform picker is unavailable. */
    public val folderPickerUnavailable: String,
    /** Label for clearing the library. */
    public val clearLibrary: String,
    /** Label for cancelling an active scan. */
    public val cancel: String,
    /** Label for the playlists entry point. */
    public val playlists: String,
    /** Accessibility description for the playlists button. */
    public val playlistsAccessibility: String,
    /** Heading label for the queue/playlists section. */
    public val libraryQueue: String,
    /** Accessibility label for track-row album thumbnails. */
    public val albumArt: String,
    /** Accessibility label for album/detail artwork. */
    public val albumArtwork: String,
    /** Badge label for the now-playing track. */
    public val nowPlayingBadge: String,
    /** Composably resolves the localized long-press/content description. */
    public val selectTrack: @Composable (String) -> String,
    /** Composably resolves the localized artist-album subtitle. */
    public val trackArtistAlbum: @Composable (String, String) -> String,
)

// ----- Private helpers -----

internal fun LibraryTrack.toUiTrack(): Track =
    Track(
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
