package com.eterocell.rhythhaus

import kotlin.math.max

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val accent: TrackAccent,
    val source: AudioSource = AudioSource.DemoTone,
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

fun demoLibrarySnapshot(): LibrarySnapshot = LibrarySnapshot(
    title = "RhythHaus",
    subtitle = "Local-first music, tuned for your own files",
    nowPlayingTrackId = "midnight-index",
    tracks = listOf(
        Track(
            id = "midnight-index",
            title = "Midnight Index",
            artist = "The Archive Lights",
            album = "Basement Takes",
            durationSeconds = 247,
            accent = TrackAccent(start = 0xFF9C6CFF, end = 0xFFFF7A90),
        ),
        Track(
            id = "soft-static",
            title = "Soft Static on Tape",
            artist = "North Room",
            album = "Field Notes",
            durationSeconds = 214,
            accent = TrackAccent(start = 0xFF52D6C5, end = 0xFF4C8DFF),
        ),
        Track(
            id = "analog-rain",
            title = "Analog Rain",
            artist = "Cassette Garden",
            album = "Window Seat",
            durationSeconds = 301,
            accent = TrackAccent(start = 0xFFFFB86B, end = 0xFFFF6F3C),
        ),
        Track(
            id = "low-light",
            title = "Low Light Transit",
            artist = "Mica Loop",
            album = "Night Bus",
            durationSeconds = 186,
            accent = TrackAccent(start = 0xFF7DE37B, end = 0xFF15B8A6),
        ),
    ),
)
