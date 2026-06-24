package com.eterocell.rhythhaus.taglib

import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun createTagLibWriter(): TagLibWriter = JvmNativeTagLibWriter()

private class JvmNativeTagLibWriter : TagLibWriter {
    override fun writePath(path: String, meta: WriteMeta): WriteResult = try {
        val bridge = NativeWriteBridge()
        val status = bridge.writePathNative(path, meta)
        when (status) {
            STATUS_SUCCESS -> WriteResult.Success
            STATUS_UNSUPPORTED -> WriteResult.Unsupported(bridge.lastError ?: "Native write unsupported")
            STATUS_FAILED -> WriteResult.Failed(bridge.lastError ?: "Native write failed")
            else -> WriteResult.Failed("Unknown write status: $status")
        }
    } catch (error: NativeTagLibUnavailableException) {
        WriteResult.Unsupported(error.message ?: "Native TagLib writer not available")
    } catch (error: UnsatisfiedLinkError) {
        WriteResult.Unsupported("Native TagLib writer not loaded: ${error.message ?: "unknown"}")
    }
}

private class NativeWriteBridge {
    var lastError: String? = null
        private set

    init {
        NativeWriteLibrary.load()
    }

    external fun writePathNative(path: String, meta: WriteMeta): Int
}

private object NativeWriteLibrary {
    private const val LIBRARY_NAME = "librhythhaus_taglib.dylib"
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val resourcePath = nativeResourcePath()
        val resource = NativeWriteLibrary::class.java.getResourceAsStream(resourcePath)
            ?: throw NativeTagLibUnavailableException("Native TagLib helper resource not found: $resourcePath")

        val libraryPath = Files.createTempFile("rhythhaus-taglib-writer", ".dylib")
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

private const val STATUS_SUCCESS = 0
private const val STATUS_UNSUPPORTED = 1
private const val STATUS_FAILED = 2
