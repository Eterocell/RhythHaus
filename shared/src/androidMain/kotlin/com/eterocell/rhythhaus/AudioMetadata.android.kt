package com.eterocell.rhythhaus

import android.media.MediaMetadataRetriever
import com.eterocell.rhythhaus.library.LibraryDatabaseContext

internal actual fun readPlatformAudioMetadata(
    source: AudioSource
): AudioMetadata? =
    when (source) {
        is AudioSource.FileDescriptor ->
            readAndroidMediaMetadataFromFileDescriptor(source.fd)
        is AudioSource.Uri -> readAndroidMediaMetadataFromUri(source.value)
        is AudioSource.FilePath -> null
    }

private fun readAndroidMediaMetadataFromFileDescriptor(
    fd: Int
): AudioMetadata? = readAndroidMediaMetadata {
    setDataSource("/proc/self/fd/$fd")
}

private fun readAndroidMediaMetadataFromUri(uri: String): AudioMetadata? =
    readAndroidMediaMetadata {
        setDataSource(
            LibraryDatabaseContext.applicationContext,
            android.net.Uri.parse(uri))
    }

private fun readAndroidMediaMetadata(
    configure: MediaMetadataRetriever.() -> Unit
): AudioMetadata? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.configure()
        val metadata =
            AudioMetadata(
                title =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_TITLE)
                        .normalizedOrNull(),
                artist =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        .normalizedOrNull(),
                album =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        .normalizedOrNull(),
                durationMillis =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.takeIf { it > 0L },
                trackNumber =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                        .firstPositiveNumberOrNull(),
                discNumber =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                        .firstPositiveNumberOrNull(),
                artworkBytes =
                    retriever.embeddedPicture?.takeIf { it.isNotEmpty() },
            )
        metadata.takeIf { it.hasUsefulValues() }
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun AudioMetadata.hasUsefulValues(): Boolean =
    title != null ||
        artist != null ||
        album != null ||
        durationMillis != null ||
        trackNumber != null ||
        discNumber != null ||
        artworkBytes != null

private fun String?.normalizedOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun String?.firstPositiveNumberOrNull(): Int? =
    this?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it > 0 }
