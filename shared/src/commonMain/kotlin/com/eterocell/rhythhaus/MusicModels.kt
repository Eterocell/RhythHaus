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
)

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
