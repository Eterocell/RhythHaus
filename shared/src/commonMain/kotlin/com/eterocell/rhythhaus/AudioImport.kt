package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable

sealed interface AudioImportResult {
    data class Success(val files: List<ImportedAudioFile>) : AudioImportResult
    data class Unavailable(val message: String) : AudioImportResult
    data class Failure(val message: String, val cause: String? = null) : AudioImportResult
}

data class ImportedAudioFile(
    val displayName: String,
    val source: AudioSource,
    val durationMillis: Long? = null,
    val metadata: AudioMetadata? = null,
)

interface AudioImportLauncher {
    val isAvailable: Boolean
    fun launch()
}

@Composable
expect fun rememberAudioImportLauncher(onResult: (AudioImportResult) -> Unit): AudioImportLauncher

fun importedLibrarySnapshot(importedFiles: List<ImportedAudioFile>): LibrarySnapshot {
    val tracks = importedFiles.mapIndexed { index, file -> file.toTrack(index) }
    return LibrarySnapshot(
        title = "RhythHaus",
        subtitle = if (tracks.isEmpty()) {
            "Choose local audio files to start listening"
        } else {
            "${tracks.size} imported local ${if (tracks.size == 1) "track" else "tracks"}"
        },
        tracks = tracks,
        nowPlayingTrackId = tracks.firstOrNull()?.id,
    )
}

fun ImportedAudioFile.toTrack(index: Int): Track {
    val title = metadata?.title ?: displayName.toDisplayTitle()
    val duration = metadata?.durationMillis ?: durationMillis
    return Track(
        id = "imported-${source.stableKey.stableHash()}-$index",
        title = title,
        artist = metadata?.artist ?: "Local file",
        album = metadata?.album ?: "Imported audio",
        durationSeconds = ((duration ?: 0L) / 1_000L).toInt(),
        accent = importedAccent(index),
        source = source,
    )
}

fun String.toDisplayTitle(): String {
    val withoutExtension = substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.', missingDelimiterValue = this)
    val normalized = withoutExtension
        .replace('-', ' ')
        .replace('_', ' ')
        .trim()
        .trim('.')
        .trim()
    return normalized.takeIf { it.isNotBlank() } ?: "Untitled audio"
}

private fun importedAccent(index: Int): TrackAccent {
    val accents = listOf(
        TrackAccent(start = 0xFF9C6CFF, end = 0xFFFF7A90),
        TrackAccent(start = 0xFF52D6C5, end = 0xFF4C8DFF),
        TrackAccent(start = 0xFFFFB86B, end = 0xFFFF6F3C),
        TrackAccent(start = 0xFF7DE37B, end = 0xFF15B8A6),
        TrackAccent(start = 0xFFFF6FD8, end = 0xFF3813C2),
    )
    return accents[index % accents.size]
}

private fun String.stableHash(): String {
    var hash = 0
    for (character in this) {
        hash = hash * 31 + character.code
    }
    return hash.toUInt().toString(16)
}
