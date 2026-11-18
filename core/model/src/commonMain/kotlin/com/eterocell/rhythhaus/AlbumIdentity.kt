package com.eterocell.rhythhaus

/**
 * Stable identity of an album projection.
 *
 * Two releases sharing a title are distinct when their album artists differ.
 * [albumArtist] is the effective album artist: the album artist tag when it has
 * content, otherwise the track artist.
 */
public data class AlbumIdentity(
    /** Display album title. */
    public val album: String,
    /** Effective album artist used to distinguish releases. */
    public val albumArtist: String,
) {
    /** Identity helpers for raw tag values. */
    public companion object {
        /**
         * Builds an identity, falling back to [trackArtist] when the tag is
         * blank.
         */
        public fun of(
            album: String,
            albumArtist: String?,
            trackArtist: String,
        ): AlbumIdentity =
            AlbumIdentity(
                album = album,
                albumArtist = effectiveAlbumArtist(albumArtist, trackArtist),
            )
    }
}

/**
 * Returns the trimmed [albumArtist] when it has content, otherwise the trimmed
 * [trackArtist].
 */
public fun effectiveAlbumArtist(
    albumArtist: String?,
    trackArtist: String,
): String =
    albumArtist?.trim()?.takeIf { it.isNotEmpty() } ?: trackArtist.trim()

/** Album identity of this library track. */
public val Track.albumIdentity: AlbumIdentity
    get() = AlbumIdentity.of(album, albumArtist, artist)

/** Album identity of this playable track, or null when it has no album. */
public val PlayableTrack.albumIdentity: AlbumIdentity?
    get() = album?.let { AlbumIdentity.of(it, albumArtist, artist) }
