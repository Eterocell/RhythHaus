package com.eterocell.rhythhaus.taglib

data class TagMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val durationMillis: Long? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val artwork: EmbeddedArtwork? = null,
)

data class EmbeddedArtwork(
    val mimeType: String?,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is EmbeddedArtwork &&
        mimeType == other.mimeType && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * (mimeType?.hashCode() ?: 0) + bytes.contentHashCode()
}
