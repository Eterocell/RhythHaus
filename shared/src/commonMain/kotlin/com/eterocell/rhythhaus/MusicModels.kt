package com.eterocell.rhythhaus

import kotlin.math.max
import com.eterocell.rhythhaus.library.LibraryTrack

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val accent: TrackAccent,
    val source: AudioSource,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean = other is Track &&
        id == other.id && title == other.title && artist == other.artist &&
        album == other.album && durationSeconds == other.durationSeconds &&
        accent == other.accent && source == other.source &&
        trackNumber == other.trackNumber && discNumber == other.discNumber &&
        artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + accent.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class TrackAccent(
    val start: Long,
    val end: Long,
)

data class LibrarySnapshot(
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val nowPlayingTrackId: String?,
) {
    val nowPlaying: Track? = tracks.firstOrNull { it.id == nowPlayingTrackId }
    val totalDurationSeconds: Int = tracks.sumOf { max(0, it.durationSeconds) }
}

fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = max(0, totalSeconds)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun accentForIndex(index: Int): TrackAccent {
    val hues = listOf(
        0xFF111018L to 0xFF776F66L,
        0xFF1A1422L to 0xFF794A4AL,
        0xFF14202AL to 0xFF4B6B7AL,
        0xFF1A1E1AL to 0xFF5C784CL,
        0xFF201A16L to 0xFF7A6448L,
        0xFF161A24L to 0xFF4B5C7AL,
        0xFF1A1420L to 0xFF6E4B7AL,
    )
    val (start, end) = hues[index % hues.size]
    return TrackAccent(start = start, end = end)
}

fun librarySnapshot(tracks: List<LibraryTrack>): LibrarySnapshot {
    val uiTracks = tracks.mapIndexed { index, track ->
        Track(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = ((track.durationMillis ?: 0L) / 1_000L).toInt(),
            accent = accentForIndex(index),
            source = track.audioSource,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            artworkBytes = track.artworkBytes,
        )
    }
    return LibrarySnapshot(
        title = "RhythHaus",
        subtitle = "",
        tracks = uiTracks,
        nowPlayingTrackId = null,
    )
}

fun Track.toPlayableTrack(): PlayableTrack = PlayableTrack(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMillis = durationSeconds.takeIf { it > 0 }?.times(1_000L),
    source = source,
    artworkBytes = artworkBytes,
)
