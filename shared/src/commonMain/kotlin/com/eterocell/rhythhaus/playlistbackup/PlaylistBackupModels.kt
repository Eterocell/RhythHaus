package com.eterocell.rhythhaus.playlistbackup

data class PlaylistBackupPayload(
    val exportedAtEpochMillis: Long,
    val playlists: List<PlaylistBackupPlaylist>,
)

data class PlaylistBackupDocument(
    val format: String,
    val version: Int,
    val exportedAtEpochMillis: Long,
    val playlists: List<PlaylistBackupPlaylist>,
    val checksumCrc32: String,
)

data class PlaylistBackupPlaylist(
    val name: String,
    val entries: List<PlaylistBackupEntry>,
)

data class PlaylistBackupEntry(
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
)

sealed interface PlaylistBackupDecodeResult {
    data class Success(val document: PlaylistBackupDocument) : PlaylistBackupDecodeResult
    data class Invalid(val error: PlaylistBackupValidationError) : PlaylistBackupDecodeResult
}

enum class PlaylistBackupValidationError {
    INPUT_TOO_LARGE,
    MALFORMED_UTF8,
    MALFORMED_JSON,
    DUPLICATE_FIELD,
    UNKNOWN_FIELD,
    MISSING_FIELD,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_VERSION,
    INVALID_CHECKSUM,
    INVALID_INTEGER,
    NUMERIC_OVERFLOW,
    TRAILING_CONTENT,
    NON_CANONICAL_JSON,
    PLAYLIST_LIMIT_EXCEEDED,
    PLAYLIST_ENTRY_LIMIT_EXCEEDED,
    TOTAL_ENTRY_LIMIT_EXCEEDED,
    STRING_LIMIT_EXCEEDED,
    BLANK_PLAYLIST_NAME,
    INVALID_DURATION,
}

internal object PlaylistBackupLimits {
    const val MAX_BYTES = 4 * 1024 * 1024
    const val MAX_PLAYLISTS = 1_000
    const val MAX_ENTRIES_PER_PLAYLIST = 10_000
    const val MAX_TOTAL_ENTRIES = 100_000
    const val MAX_STRING_CODE_POINTS = 1_024
    const val MAX_DURATION_SECONDS = 604_800
}
