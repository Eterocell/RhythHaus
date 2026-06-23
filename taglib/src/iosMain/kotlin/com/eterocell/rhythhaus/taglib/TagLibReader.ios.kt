package com.eterocell.rhythhaus.taglib

import com.eterocell.rhythhaus.taglib.cinterop.rh_taglib_free_result
import com.eterocell.rhythhaus.taglib.cinterop.rh_taglib_read_path
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
actual fun createTagLibReader(): TagLibReader = IosNativeTagLibReader()

@OptIn(ExperimentalForeignApi::class)
private class IosNativeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult {
        val result = rh_taglib_read_path(path)
        val status = result.useContents { status }
        val tagResult = when (status) {
            STATUS_FOUND -> TagReadResult.Found(
                result.useContents {
                    TagMetadata(
                        title = metadata.title?.toKString(),
                        artist = metadata.artist?.toKString(),
                        album = metadata.album?.toKString(),
                        albumArtist = metadata.album_artist?.toKString(),
                        genre = metadata.genre?.toKString(),
                        year = metadata.year.positiveOrNull(),
                        trackNumber = metadata.track.positiveOrNull(),
                        durationMillis = metadata.duration_seconds.positiveOrNull()?.times(1_000L),
                        bitrate = metadata.bitrate.positiveOrNull(),
                        sampleRate = metadata.sample_rate.positiveOrNull(),
                        channels = metadata.channels.positiveOrNull(),
                    )
                },
            )
            STATUS_UNSUPPORTED -> TagReadResult.Unsupported(
                result.useContents { error_message?.toKString() }
                    ?: "Native TagLib reader does not support this path",
            )
            STATUS_FAILED -> TagReadResult.Failed(
                result.useContents { error_message?.toKString() }
                    ?: "Native TagLib reader failed",
            )
            else -> TagReadResult.Failed("Native TagLib reader returned unknown status: $status")
        }
        rh_taglib_free_result(result)
        return tagResult
    }
}

private fun Int.positiveOrNull(): Int? = takeIf { it > 0 }

private const val STATUS_FOUND = 0
private const val STATUS_UNSUPPORTED = 1
private const val STATUS_FAILED = 2
