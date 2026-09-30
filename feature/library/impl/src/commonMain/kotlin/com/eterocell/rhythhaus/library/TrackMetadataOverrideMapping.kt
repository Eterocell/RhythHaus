package com.eterocell.rhythhaus.library

/** Validate a complete replacement before changing any persisted field. */
internal fun TrackMetadataOverride.normalizedForStorage():
    TrackMetadataOverride {
    val normalizedTrackNumber = trackNumber
    val normalizedDiscNumber = discNumber
    require(normalizedTrackNumber == null || normalizedTrackNumber > 0) {
        "Track number must be positive"
    }
    require(normalizedDiscNumber == null || normalizedDiscNumber > 0) {
        "Disc number must be positive"
    }
    return copy(
        title = title?.trim()?.ifEmpty { null },
        artist = artist?.trim()?.ifEmpty { null },
        album = album?.trim()?.ifEmpty { null },
    )
}

internal fun TrackMetadataOverride.applyTo(track: LibraryTrack): LibraryTrack =
    track.copy(
        title = title ?: track.title,
        artist = artist ?: track.artist,
        album = album ?: track.album,
        trackNumber = trackNumber ?: track.trackNumber,
        discNumber = discNumber ?: track.discNumber,
    )
