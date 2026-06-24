package com.eterocell.rhythhaus

import kotlin.math.max

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
