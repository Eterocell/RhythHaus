package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.taglib.RhythHausTagLib
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.taglib.createTagLibReader

/**
 * Read-only metadata extracted from an audio source, including optional
 * ReplayGain facts.
 */
data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMillis: Long? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
    val replayGainTrackGainDb: Double? = null,
    val replayGainTrackPeak: Double? = null,
    val replayGainAlbumGainDb: Double? = null,
    val replayGainAlbumPeak: Double? = null,
    val lyrics: String? = null,
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
            artworkBytes.contentEquals(other.artworkBytes) &&
            (replayGainTrackGainDb?.equals(other.replayGainTrackGainDb)
                ?: (other.replayGainTrackGainDb == null)) &&
            (replayGainTrackPeak?.equals(other.replayGainTrackPeak)
                ?: (other.replayGainTrackPeak == null)) &&
            (replayGainAlbumGainDb?.equals(other.replayGainAlbumGainDb)
                ?: (other.replayGainAlbumGainDb == null)) &&
            (replayGainAlbumPeak?.equals(other.replayGainAlbumPeak)
                ?: (other.replayGainAlbumPeak == null)) &&
            lyrics == other.lyrics

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        result = 31 * result + (replayGainTrackGainDb?.hashCode() ?: 0)
        result = 31 * result + (replayGainTrackPeak?.hashCode() ?: 0)
        result = 31 * result + (replayGainAlbumGainDb?.hashCode() ?: 0)
        result = 31 * result + (replayGainAlbumPeak?.hashCode() ?: 0)
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        return result
    }
}

/** Reads audio metadata from file paths, descriptors, and URIs. */
class AudioMetadataReader
internal constructor(
    private val tagLibReader: TagLibReader,
    private val platformMetadataReader: (AudioSource) -> AudioMetadata?,
    private val uriDescriptorOpener:
        (AudioSource.Uri) -> AudioMetadataUriDescriptor?,
) {
    constructor(
        tagLibReader: TagLibReader = createTagLibReader(),
        platformMetadataReader: (AudioSource) -> AudioMetadata? =
            ::readPlatformAudioMetadata,
    ) : this(
        tagLibReader,
        platformMetadataReader,
        ::openPlatformAudioMetadataUri,
    )

    fun read(source: AudioSource): AudioMetadata? {
        val result =
            when (source) {
                is AudioSource.FilePath ->
                    RhythHausTagLib.readPath(source.path, tagLibReader)
                is AudioSource.FileDescriptor ->
                    RhythHausTagLib.readFd(
                        source.fd, source.displayName, tagLibReader)
                is AudioSource.Uri -> readUriTags(source)
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

    private fun readUriTags(source: AudioSource.Uri): TagReadResult {
        return try {
            val descriptor =
                uriDescriptorOpener(source)
                    ?: return TagReadResult.Unsupported(
                        "URI descriptor unavailable")
            try {
                RhythHausTagLib.readFd(
                    descriptor.source.fd,
                    descriptor.source.displayName,
                    tagLibReader)
            } finally {
                descriptor.close()
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            TagReadResult.Unsupported("URI tag read unavailable")
        }
    }
}

// Metadata enrichment handled by LibraryScanner using AudioMetadataReader
// directly.

internal expect fun readPlatformAudioMetadata(
    source: AudioSource
): AudioMetadata?

/** An opened URI descriptor owned by the metadata reader, not the scanner. */
internal class AudioMetadataUriDescriptor(
    val source: AudioSource.FileDescriptor,
    val close: () -> Unit,
)

internal expect fun openPlatformAudioMetadataUri(
    source: AudioSource.Uri,
): AudioMetadataUriDescriptor?

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
        replayGainTrackGainDb = parseReplayGainDb(replayGainTrackGain),
        replayGainTrackPeak = parseReplayGainPeak(replayGainTrackPeak),
        replayGainAlbumGainDb = parseReplayGainDb(replayGainAlbumGain),
        replayGainAlbumPeak = parseReplayGainPeak(replayGainAlbumPeak),
        lyrics = lyrics.boundedLyricsOrNull(),
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
        lyrics = lyrics ?: fallback.lyrics,
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
/**
 * Reads audio metadata from an absolute file path.
 *
 * @param path the absolute path to the audio file.
 */
fun readAudioMetadata(path: String): AudioMetadata? =
    AudioMetadataReader().read(AudioSource.FilePath(path))

private fun String?.normalizedOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun String?.boundedLyricsOrNull(): String? {
    val normalized = normalizedOrNull() ?: return null
    return normalized.takeIf {
        it.encodeToByteArray().size <= MAX_EMBEDDED_LYRICS_BYTES
    }
}

private const val MAX_EMBEDDED_LYRICS_BYTES = 512 * 1024
