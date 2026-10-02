package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot

/** Result of exporting one static playlist to an interoperable document. */
public sealed interface PlaylistInteroperabilityExportResult {
    /** Encoded document bytes. */
    public data class Success(val bytes: ByteArray) :
        PlaylistInteroperabilityExportResult

    /** Export could not resolve [trackId], if known. */
    public data class Failure(val trackId: String?) :
        PlaylistInteroperabilityExportResult
}

/** Export one static playlist; smart rules are intentionally not serialized. */
public fun exportPlaylistInteroperability(
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
    existingPlaylistNames: List<String>,
    importedSuffix: String,
    libraryRevision: Long,
): PlaylistImportPlan =
    planPlaylistImport(
        playlists =
            listOf(
                PlaylistBackupPlaylist(
                    name = document.name,
                    entries =
                        document.entries.map { entry ->
                            PlaylistBackupEntry(
                                title = entry.title,
                                artist = entry.artist,
                                album = entry.album,
                                durationSeconds = entry.durationSeconds,
                            )
                        },
                ),
            ),
        destinationTracks = destinationTracks,
        existingPlaylistNames = existingPlaylistNames,
        importedSuffix = importedSuffix,
        libraryRevision = libraryRevision,
    )

private fun AudioSource.interoperabilityPath(): String =
    when (this) {
        is AudioSource.FilePath -> path
        is AudioSource.Uri -> value
        is AudioSource.FileDescriptor -> displayName
    }
