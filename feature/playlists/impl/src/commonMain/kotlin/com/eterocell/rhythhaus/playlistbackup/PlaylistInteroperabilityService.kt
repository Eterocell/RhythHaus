package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot

internal sealed interface PlaylistInteroperabilityExportResult {
    data class Success(val bytes: ByteArray) :
        PlaylistInteroperabilityExportResult

    data class Failure(val trackId: String?) :
        PlaylistInteroperabilityExportResult
}

internal data class PlaylistInteroperabilityImportPlan(
    val name: String,
    val trackIds: List<String>,
    val unmatched: Int,
    val ambiguous: Int,
)

/** Export one static playlist; smart rules are intentionally not serialized. */
internal fun exportPlaylistInteroperability(
    snapshot: PlaylistSnapshot,
    playlistId: String,
    tracks: List<LibraryTrack>,
    format: PlaylistInteroperabilityFormat,
): PlaylistInteroperabilityExportResult {
    val playlist =
        snapshot.playlist(playlistId)
            ?: return PlaylistInteroperabilityExportResult.Failure(null)
    val byId = tracks.associateBy(LibraryTrack::id)
    val entries =
        snapshot.entries(playlistId).map { occurrence ->
            val track =
                byId[occurrence.trackId]
                    ?: return PlaylistInteroperabilityExportResult.Failure(
                        occurrence.trackId)
            val durationMillis =
                track.durationMillis
                    ?: return PlaylistInteroperabilityExportResult.Failure(
                        track.id)
            val durationSeconds = durationMillis / 1_000
            if (durationMillis < 0L ||
                durationSeconds !in
                    0L..PlaylistBackupLimits.MAX_DURATION_SECONDS.toLong()) {
                return PlaylistInteroperabilityExportResult.Failure(track.id)
            }
            PlaylistInteroperabilityEntry(
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationSeconds = durationSeconds.toInt(),
                path = track.audioSource.interoperabilityPath(),
            )
        }
    return try {
        PlaylistInteroperabilityExportResult.Success(
            PlaylistInteroperabilityCodec.encode(
                PlaylistInteroperabilityDocument(
                    entries, format, playlist.name),
                format))
    } catch (_: IllegalArgumentException) {
        PlaylistInteroperabilityExportResult.Failure(null)
    }
}

internal fun planPlaylistInteroperabilityImport(
    document: PlaylistInteroperabilityDocument,
    destinationTracks: List<LibraryTrack>,
): PlaylistInteroperabilityImportPlan {
    val matcher = PlaylistBackupMatcher(destinationTracks)
    val ids = mutableListOf<String>()
    var unmatched = 0
    var ambiguous = 0
    document.entries.forEach { entry ->
        when (val match =
            matcher.match(
                PlaylistBackupEntry(
                    entry.title,
                    entry.artist,
                    entry.album,
                    entry.durationSeconds))) {
            is PlaylistBackupMatch.Unique -> ids += match.trackId
            PlaylistBackupMatch.Unmatched -> unmatched++
            is PlaylistBackupMatch.Ambiguous -> ambiguous++
        }
    }
    return PlaylistInteroperabilityImportPlan(
        document.name, ids, unmatched, ambiguous)
}

internal fun PlaylistInteroperabilityImportPlan.toMutation():
    PlaylistImportMutation = PlaylistImportMutation(name, trackIds)

private fun AudioSource.interoperabilityPath(): String =
    when (this) {
        is AudioSource.FilePath -> path
        is AudioSource.Uri -> value
        is AudioSource.FileDescriptor -> displayName
    }
