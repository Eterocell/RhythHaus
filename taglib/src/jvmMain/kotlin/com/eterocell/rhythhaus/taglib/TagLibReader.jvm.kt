package com.eterocell.rhythhaus.taglib

import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun createTagLibReader(): TagLibReader = JvmNativeTagLibReader()

private class JvmNativeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = try {
        NativeTagLibBridge().readPathNative(path).toTagReadResult()
    } catch (error: NativeTagLibUnavailableException) {
        TagReadResult.Unsupported(error.message ?: "Native TagLib reader is not available for JVM")
    } catch (error: UnsatisfiedLinkError) {
        TagReadResult.Unsupported("Native TagLib reader could not be loaded: ${error.message ?: "unknown linker error"}")
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
        STATUS_UNSUPPORTED -> TagReadResult.Unsupported(errorMessage ?: "Native TagLib reader does not support this path")
        STATUS_FAILED -> TagReadResult.Failed(errorMessage ?: "Native TagLib reader failed")
        else -> TagReadResult.Failed("Native TagLib reader returned unknown status: $status")
    }

    private fun Int.positiveOrNull(): Int? = takeIf { it > 0 }

    private companion object {
        const val STATUS_FOUND = 0
        const val STATUS_UNSUPPORTED = 1
        const val STATUS_FAILED = 2
    }
}

private object NativeTagLibLibrary {
    private const val LIBRARY_NAME = "librhythhaus_taglib.dylib"
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val resourcePath = nativeResourcePath()
        val resource = NativeTagLibLibrary::class.java.getResourceAsStream(resourcePath)
            ?: throw NativeTagLibUnavailableException("Native TagLib helper resource not found: $resourcePath")

        val libraryPath = Files.createTempFile("rhythhaus-taglib", ".dylib")
        resource.use { input ->
            Files.copy(input, libraryPath, StandardCopyOption.REPLACE_EXISTING)
        }
        libraryPath.toFile().deleteOnExit()
        System.load(libraryPath.toAbsolutePath().toString())
        loaded = true
    }

    private fun nativeResourcePath(): String {
        val osName = System.getProperty("os.name")
        val architecture = System.getProperty("os.arch").lowercase()
        val platform = when {
            osName.contains("Mac", ignoreCase = true) && architecture in setOf("aarch64", "arm64") -> "macos-aarch64"
            osName.contains("Mac", ignoreCase = true) && architecture == "x86_64" -> "macos-x64"
            else -> throw NativeTagLibUnavailableException(
                "Native TagLib helper is only packaged for macOS JVM, current os=$osName arch=${System.getProperty("os.arch")}",
            )
        }
        return "/native/$platform/$LIBRARY_NAME"
    }
}

internal class NativeTagLibUnavailableException(message: String) : RuntimeException(message)
