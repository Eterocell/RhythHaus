package com.eterocell.rhythhaus.playlistbackup

/**
 * Detects a supported interchange grammar without trusting a file extension.
 */
internal fun detectPlaylistInteroperabilityFormat(
    bytes: ByteArray,
): PlaylistInteroperabilityFormat? {
    val candidates =
        listOf(
            PlaylistInteroperabilityFormat.M3U,
            PlaylistInteroperabilityFormat.PLS,
        )
    return candidates.firstOrNull { format ->
        PlaylistInteroperabilityCodec.decode(bytes, format) is
            PlaylistInteroperabilityDecodeResult.Success
    }
}
