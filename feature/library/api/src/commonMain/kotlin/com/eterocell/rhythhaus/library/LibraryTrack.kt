package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack

/** A track indexed from a [LibrarySource]. */
public data class LibraryTrack(
    /** Stable track identifier. */
    public val id: String,
    /** Owning source identifier. */
    public val sourceId: String,
    /** Source-local stable key. */
    public val sourceLocalKey: String,
    /** Audio location used for playback. */
    public val audioSource: AudioSource,
    /** User-visible file name. */
    public val displayName: String,
    /** Track title. */
    public val title: String,
    /** Track artist. */
    public val artist: String,
    /** Track album. */
    public val album: String,
    /** Duration in milliseconds when known. */
    public val durationMillis: Long?,
    /** File size in bytes when known. */
    public val sizeBytes: Long?,
    /** Last modification time in epoch milliseconds when known. */
    public val modifiedAtEpochMillis: Long?,
    /** Scan that last observed this track. */
    public val lastSeenScanId: String?,
    /** Creation time in epoch milliseconds. */
    public val createdAtEpochMillis: Long,
    /** Last update time in epoch milliseconds. */
    public val updatedAtEpochMillis: Long,
    /** Track number when known. */
    public val trackNumber: Int? = null,
    /** Disc number when known. */
    public val discNumber: Int? = null,
    /** Embedded artwork bytes when available. */
    public val artworkBytes: ByteArray? = null,
    /** MIME type for [artworkBytes]. */
    public val artworkMimeType: String? = null,
) {
    /** Converts this library projection to a playable track. */
    public fun toPlayableTrack(): PlayableTrack =
        PlayableTrack(
            id, title, artist, album, durationMillis, audioSource, artworkBytes)

    override fun equals(other: Any?): Boolean =
        other is LibraryTrack &&
            id == other.id &&
            sourceId == other.sourceId &&
            sourceLocalKey == other.sourceLocalKey &&
            audioSource == other.audioSource &&
            displayName == other.displayName &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            durationMillis == other.durationMillis &&
            sizeBytes == other.sizeBytes &&
            modifiedAtEpochMillis == other.modifiedAtEpochMillis &&
            lastSeenScanId == other.lastSeenScanId &&
            createdAtEpochMillis == other.createdAtEpochMillis &&
            updatedAtEpochMillis == other.updatedAtEpochMillis &&
            trackNumber == other.trackNumber &&
            discNumber == other.discNumber &&
            artworkMimeType == other.artworkMimeType &&
            artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + sourceLocalKey.hashCode()
        result = 31 * result + audioSource.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (sizeBytes?.hashCode() ?: 0)
        result = 31 * result + (modifiedAtEpochMillis?.hashCode() ?: 0)
        result = 31 * result + (lastSeenScanId?.hashCode() ?: 0)
        result = 31 * result + createdAtEpochMillis.hashCode()
        result = 31 * result + updatedAtEpochMillis.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

/** Artwork associated with a track. */
public data class TrackArtwork(
    /** Encoded artwork bytes. */
    public val bytes: ByteArray,
    /** Artwork MIME type when known. */
    public val mimeType: String?,
) {
    override fun equals(other: Any?): Boolean =
        other is TrackArtwork &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int =
        31 * bytes.contentHashCode() + (mimeType?.hashCode() ?: 0)
}
