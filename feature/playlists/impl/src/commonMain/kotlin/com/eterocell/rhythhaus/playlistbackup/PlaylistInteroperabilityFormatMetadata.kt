package com.eterocell.rhythhaus.playlistbackup

internal fun playlistInteroperabilityFileExtension(
    format: PlaylistInteroperabilityFormat,
): String =
    when (format) {
        PlaylistInteroperabilityFormat.M3U -> ".m3u"
        PlaylistInteroperabilityFormat.M3U8 -> ".m3u8"
        PlaylistInteroperabilityFormat.PLS -> ".pls"
    }

internal fun playlistInteroperabilityMimeTypes(
    format: PlaylistInteroperabilityFormat,
): List<String> =
    when (format) {
        PlaylistInteroperabilityFormat.M3U ->
            listOf("audio/x-mpegurl", "audio/mpegurl")
        PlaylistInteroperabilityFormat.M3U8 ->
            listOf("audio/x-mpegurl", "application/vnd.apple.mpegurl")
        PlaylistInteroperabilityFormat.PLS -> listOf("audio/x-scpls")
    }
