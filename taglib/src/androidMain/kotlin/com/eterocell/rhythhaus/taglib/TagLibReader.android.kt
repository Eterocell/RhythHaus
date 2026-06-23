package com.eterocell.rhythhaus.taglib

actual fun createTagLibReader(): TagLibReader = AndroidNativeTagLibReader()

private class AndroidNativeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = try {
        NativeTagLibBridge().readPathNative(path).toTagReadResult()
    } catch (error: NativeTagLibUnavailableException) {
        TagReadResult.Unsupported(error.message ?: ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE)
    } catch (error: UnsatisfiedLinkError) {
        TagReadResult.Unsupported("$ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE (${error.message ?: "unknown linker error"})")
    }

    override fun readProperties(path: String): Map<String, String> = try {
        NativeTagLibBridge().readPropertiesNative(path).orEmpty()
    } catch (error: NativeTagLibUnavailableException) {
        emptyMap()
    } catch (error: UnsatisfiedLinkError) {
        emptyMap()
    }
}

private class NativeTagLibBridge {
    init {
        NativeTagLibLibrary.load()
    }

    external fun readPathNative(path: String): NativeTagLibReadResult
    external fun readPropertiesNative(path: String): Map<String, String>?
}

internal data class NativeTagLibReadResult(
    val status: Int,
    val errorMessage: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val comment: String?,
    val year: Int,
    val track: Int,
    val trackTotal: Int,
    val discNumber: Int,
    val discTotal: Int,
    val durationSeconds: Int,
    val bitrate: Int,
    val sampleRate: Int,
    val channels: Int,
) {
    fun toTagReadResult(): TagReadResult = when (status) {
        STATUS_FOUND -> TagReadResult.Found(
            TagMetadata(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                comment = comment,
                year = year.positiveOrNull(),
                trackNumber = track.positiveOrNull(),
                trackTotal = trackTotal.positiveOrNull(),
                discNumber = discNumber.positiveOrNull(),
                discTotal = discTotal.positiveOrNull(),
                durationMillis = durationSeconds.positiveOrNull()?.times(1_000L),
                bitrate = bitrate.positiveOrNull(),
                sampleRate = sampleRate.positiveOrNull(),
                channels = channels.positiveOrNull(),
            ),
        )
        STATUS_UNSUPPORTED -> TagReadResult.Unsupported(errorMessage ?: "Native TagLib reader does not support this Android path")
        STATUS_FAILED -> TagReadResult.Failed(errorMessage ?: "Native TagLib reader failed on Android")
        else -> TagReadResult.Failed("Native TagLib reader returned unknown Android status: $status")
    }

    private fun Int.positiveOrNull(): Int? = takeIf { it > 0 }

    private companion object {
        const val STATUS_FOUND = 0
        const val STATUS_UNSUPPORTED = 1
        const val STATUS_FAILED = 2
    }
}

private object NativeTagLibLibrary {
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        try {
            System.loadLibrary("rhythhaus_taglib")
            loaded = true
        } catch (error: UnsatisfiedLinkError) {
            throw NativeTagLibUnavailableException("$ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE (${error.message ?: "unknown linker error"})")
        }
    }
}

private const val ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE =
    "Native TagLib Android binding is scaffolded, but no Android TagLib source/prebuilt library is packaged yet; " +
        "add the Android TagLib build or ABI prebuilts before enabling metadata reads"

internal class NativeTagLibUnavailableException(message: String) : RuntimeException(message)
