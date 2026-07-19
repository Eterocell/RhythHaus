package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot

enum class PlaylistBackupExportError {
    MISSING_TRACK,
    MISSING_DURATION,
    INVALID_DURATION,
    CODEC_BOUNDS,
}

sealed interface PlaylistBackupExportResult {
    data class Success(val bytes: ByteArray) : PlaylistBackupExportResult {
        override fun equals(other: Any?): Boolean = other is Success && bytes.contentEquals(other.bytes)
        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class Failure(
        val error: PlaylistBackupExportError,
        val trackId: String? = null,
    ) : PlaylistBackupExportResult
}

data class PlaylistImportCounts(
    val restorable: Int,
    val unmatched: Int,
    val ambiguous: Int,
) {
    operator fun plus(other: PlaylistImportCounts) = PlaylistImportCounts(
        restorable + other.restorable,
        unmatched + other.unmatched,
        ambiguous + other.ambiguous,
    )
}

data class PlaylistImportPlaylist(
    val sourcePlaylistIndex: Int,
    val name: String,
    val trackIds: List<String>,
)

data class PlaylistImportPlaylistReport(
    val sourcePlaylistIndex: Int,
    val sourceName: String,
    val plannedName: String?,
    val counts: PlaylistImportCounts,
)

enum class PlaylistImportIssueKind { UNMATCHED, AMBIGUOUS }

data class PlaylistImportIssue(
    val playlistIndex: Int,
    val entryIndex: Int,
    val entry: PlaylistBackupEntry,
    val kind: PlaylistImportIssueKind,
    val candidateTrackIds: List<String>,
)

data class PlaylistImportTotals(
    val playlistsToCreate: Int,
    val playlistsSkipped: Int,
    val entries: PlaylistImportCounts,
)

data class PlaylistImportPlan(
    val libraryRevision: Long,
    val playlists: List<PlaylistImportPlaylist>,
    val reports: List<PlaylistImportPlaylistReport>,
    val totals: PlaylistImportTotals,
    val issues: List<PlaylistImportIssue>,
)

fun exportPlaylistBackup(
    snapshot: PlaylistSnapshot,
    authoritativeTracks: List<LibraryTrack>,
    exportedAtEpochMillis: Long,
): PlaylistBackupExportResult {
    val tracksById = authoritativeTracks.associateBy(LibraryTrack::id)
    val playlists = mutableListOf<PlaylistBackupPlaylist>()
    snapshot.playlists.forEach { playlist ->
        val entries = mutableListOf<PlaylistBackupEntry>()
        snapshot.entries(playlist.id).forEach { playlistEntry ->
            val track = tracksById[playlistEntry.trackId]
                ?: return PlaylistBackupExportResult.Failure(
                    PlaylistBackupExportError.MISSING_TRACK,
                    playlistEntry.trackId,
                )
            val durationMillis = track.durationMillis
                ?: return PlaylistBackupExportResult.Failure(
                    PlaylistBackupExportError.MISSING_DURATION,
                    track.id,
                )
            val durationSeconds = durationMillis / 1_000
            if (durationMillis < 0 || durationSeconds !in 0..PlaylistBackupLimits.MAX_DURATION_SECONDS.toLong()) {
                return PlaylistBackupExportResult.Failure(PlaylistBackupExportError.INVALID_DURATION, track.id)
            }
            entries += PlaylistBackupEntry(
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationSeconds = durationSeconds.toInt(),
            )
        }
        playlists += PlaylistBackupPlaylist(playlist.name, entries)
    }
    return try {
        PlaylistBackupExportResult.Success(
            PlaylistBackupCodec.encode(PlaylistBackupPayload(exportedAtEpochMillis, playlists)),
        )
    } catch (_: IllegalArgumentException) {
        PlaylistBackupExportResult.Failure(PlaylistBackupExportError.CODEC_BOUNDS)
    }
}

fun planPlaylistImport(
    document: PlaylistBackupDocument,
    destinationTracks: List<LibraryTrack>,
    existingPlaylistNames: List<String>,
    importedSuffix: String,
    libraryRevision: Long,
): PlaylistImportPlan {
    require(importedSuffix.isNotBlank())
    val matcher = PlaylistBackupMatcher(destinationTracks)
    val reservedNames = existingPlaylistNames.mapTo(mutableSetOf(), ::normalizePortableText)
    val plannedPlaylists = mutableListOf<PlaylistImportPlaylist>()
    val reports = mutableListOf<PlaylistImportPlaylistReport>()
    val issues = mutableListOf<PlaylistImportIssue>()
    var totalCounts = PlaylistImportCounts(0, 0, 0)

    document.playlists.forEachIndexed { playlistIndex, playlist ->
        val trackIds = mutableListOf<String>()
        var unmatched = 0
        var ambiguous = 0
        playlist.entries.forEachIndexed { entryIndex, entry ->
            when (val match = matcher.match(entry)) {
                is PlaylistBackupMatch.Unique -> trackIds += match.trackId
                PlaylistBackupMatch.Unmatched -> {
                    unmatched++
                    issues += PlaylistImportIssue(
                        playlistIndex,
                        entryIndex,
                        entry,
                        PlaylistImportIssueKind.UNMATCHED,
                        emptyList(),
                    )
                }
                is PlaylistBackupMatch.Ambiguous -> {
                    ambiguous++
                    issues += PlaylistImportIssue(
                        playlistIndex,
                        entryIndex,
                        entry,
                        PlaylistImportIssueKind.AMBIGUOUS,
                        match.trackIds,
                    )
                }
            }
        }
        val counts = PlaylistImportCounts(trackIds.size, unmatched, ambiguous)
        totalCounts += counts
        val plannedName = if (trackIds.isEmpty()) null else reserveImportName(
            playlist.name,
            importedSuffix,
            reservedNames,
        )
        if (plannedName != null) {
            plannedPlaylists += PlaylistImportPlaylist(playlistIndex, plannedName, trackIds.toList())
        }
        reports += PlaylistImportPlaylistReport(playlistIndex, playlist.name, plannedName, counts)
    }

    return PlaylistImportPlan(
        libraryRevision = libraryRevision,
        playlists = plannedPlaylists.toList(),
        reports = reports.toList(),
        totals = PlaylistImportTotals(
            playlistsToCreate = plannedPlaylists.size,
            playlistsSkipped = document.playlists.size - plannedPlaylists.size,
            entries = totalCounts,
        ),
        issues = issues.toList(),
    )
}

private fun reserveImportName(
    sourceName: String,
    importedSuffix: String,
    reservedNames: MutableSet<String>,
): String {
    if (reservedNames.add(normalizePortableText(sourceName))) return sourceName
    var suffixNumber = 1
    while (true) {
        val suffix = if (suffixNumber == 1) importedSuffix else "$importedSuffix $suffixNumber"
        val candidate = "$sourceName ($suffix)"
        if (reservedNames.add(normalizePortableText(candidate))) return candidate
        suffixNumber++
    }
}
