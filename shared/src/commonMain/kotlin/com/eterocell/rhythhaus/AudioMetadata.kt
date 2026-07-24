package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.taglib.RhythHausTagLib
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.taglib.createTagLibReader

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMillis: Long? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is AudioMetadata &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            durationMillis == other.durationMillis &&
            trackNumber == other.trackNumber &&
            discNumber == other.discNumber &&
            artworkMimeType == other.artworkMimeType &&
            artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

class AudioMetadataReader(
    private val tagLibReader: TagLibReader = createTagLibReader(),
    private val platformMetadataReader: (AudioSource) -> AudioMetadata? =
        ::readPlatformAudioMetadata,
) {
    fun read(source: AudioSource): AudioMetadata? {
        val result =
            when (source) {
                is AudioSource.FilePath ->
                    RhythHausTagLib.readPath(source.path, tagLibReader)
                is AudioSource.FileDescriptor ->
                    RhythHausTagLib.readFd(
                        source.fd, source.displayName, tagLibReader)
                is AudioSource.Uri -> return platformMetadataReader(source)
            }

        val tagMetadata =
            when (result) {
                is TagReadResult.Found -> result.metadata.toAudioMetadata()
                is TagReadResult.Unsupported -> null
                is TagReadResult.Failed -> null
            }
        val fallbackMetadata =
            if (tagMetadata == null || tagMetadata.needsFallbackMetadata()) {
                platformMetadataReader(source)
            } else {
                null
            }
        return tagMetadata.mergeMissing(fallbackMetadata) ?: fallbackMetadata
    }
}

// Metadata enrichment handled by LibraryScanner using AudioMetadataReader
// directly.

internal expect fun readPlatformAudioMetadata(
    source: AudioSource
): AudioMetadata?

private fun TagMetadata.toAudioMetadata(): AudioMetadata =
    AudioMetadata(
        title = title.normalizedOrNull(),
        artist = artist.normalizedOrNull(),
        album = album.normalizedOrNull(),
        durationMillis = durationMillis?.takeIf { it > 0L },
        trackNumber = trackNumber?.takeIf { it > 0 },
        discNumber = discNumber?.takeIf { it > 0 },
        artworkBytes = artwork?.bytes?.takeIf { it.isNotEmpty() },
        artworkMimeType = artwork?.mimeType,
    )

private fun AudioMetadata?.mergeMissing(
    fallback: AudioMetadata?
): AudioMetadata? {
    if (this == null) return fallback
    if (fallback == null) return this
    return copy(
        title = title ?: fallback.title,
        artist = artist ?: fallback.artist,
        album = album ?: fallback.album,
        durationMillis = durationMillis ?: fallback.durationMillis,
        trackNumber = trackNumber ?: fallback.trackNumber,
        discNumber = discNumber ?: fallback.discNumber,
        artworkBytes = artworkBytes ?: fallback.artworkBytes,
        artworkMimeType = artworkMimeType ?: fallback.artworkMimeType,
    )
}

private fun AudioMetadata.needsFallbackMetadata(): Boolean =
    title == null ||
        artist == null ||
        album == null ||
        durationMillis == null ||
        artworkBytes == null

// Simple top-level function for Swift interop — reads metadata from an absolute
// file path.
fun readAudioMetadata(path: String): AudioMetadata? =
    AudioMetadataReader().read(AudioSource.FilePath(path))

private fun String?.normalizedOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
