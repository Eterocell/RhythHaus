package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.LibraryTrack

fun normalizePortableText(value: String): String {
    val normalized = StringBuilder(value.length)
    var pendingSpace = false
    value.lowercase().forEach { character ->
        if (character.isWhitespace()) {
            if (normalized.isNotEmpty()) pendingSpace = true
        } else {
            if (pendingSpace) normalized.append(' ')
            normalized.append(character)
            pendingSpace = false
        }
    }
    return normalized.toString()
}

sealed interface PlaylistBackupMatch {
    data class Unique(val trackId: String) : PlaylistBackupMatch
    data object Unmatched : PlaylistBackupMatch
    data class Ambiguous(val trackIds: List<String>) : PlaylistBackupMatch
}

class PlaylistBackupMatcher(destinationTracks: List<LibraryTrack>) {
    private data class TextKey(
        val title: String,
        val artist: String,
        val album: String,
    )

    private data class Candidate(
        val trackId: String,
        val durationSeconds: Long,
    )

    private val candidatesByText = destinationTracks.mapNotNull { track ->
        track.durationMillis?.takeIf { it >= 0 }?.let { durationMillis ->
            textKey(track.title, track.artist, track.album) to Candidate(track.id, durationMillis / 1_000)
        }
    }.groupBy(keySelector = { it.first }, valueTransform = { it.second })

    fun match(entry: PlaylistBackupEntry): PlaylistBackupMatch {
        val candidates = candidatesByText[textKey(entry.title, entry.artist, entry.album)]
            .orEmpty()
            .filter { candidate ->
                candidate.durationSeconds in (entry.durationSeconds.toLong() - DURATION_TOLERANCE_SECONDS)..(entry.durationSeconds.toLong() + DURATION_TOLERANCE_SECONDS)
            }
        return when (candidates.size) {
            0 -> PlaylistBackupMatch.Unmatched
            1 -> PlaylistBackupMatch.Unique(candidates.single().trackId)
            else -> PlaylistBackupMatch.Ambiguous(candidates.map(Candidate::trackId))
        }
    }

    private fun textKey(title: String, artist: String, album: String) = TextKey(
        normalizePortableText(title),
        normalizePortableText(artist),
        normalizePortableText(album),
    )

    private companion object {
        const val DURATION_TOLERANCE_SECONDS = 2L
    }
}
