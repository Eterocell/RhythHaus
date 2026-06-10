package com.eterocell.rhythhaus

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = MacOSNativePlaybackEngine()

private class MacOSNativePlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private val bridge = MacAudioPlayerBridge()
    private var durationMillis: Long? = null

    override fun load(track: PlayableTrack) {
        bridge.resetPlayer()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val loaded = bridge.load(track.source.jvmFile().absolutePath)
        require(loaded) { "Could not load native macOS audio player" }
        durationMillis = track.durationMillis ?: bridge.durationMillis().takeIf { it > 0L }
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        require(bridge.play()) { "No native macOS player has been loaded" }
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        publishProgress()
    }

    override fun pause() {
        bridge.pause()
        publishProgress()
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        bridge.stop()
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        bridge.seekTo(positionMillis)
        publishProgress()
    }

    override fun release() {
        bridge.releasePlayer()
        durationMillis = null
    }

    private fun publishProgress() {
        listener?.onPlaybackProgress(
            positionMillis = bridge.currentPositionMillis().coerceAtLeast(0L),
            durationMillis = bridge.durationMillis().takeIf { it > 0L } ?: durationMillis,
        )
    }
}

internal class MacAudioPlayerBridge {
    private var handle: Long = nativeCreate()

    fun load(path: String): Boolean = nativeLoad(requireHandle(), path)
    fun play(): Boolean = nativePlay(requireHandle())
    fun pause() = nativePause(requireHandle())
    fun stop() = nativeStop(requireHandle())
    fun seekTo(positionMillis: Long) = nativeSeekTo(requireHandle(), positionMillis)
    fun currentPositionMillis(): Long = nativeCurrentPositionMillis(requireHandle())
    fun durationMillis(): Long = nativeDurationMillis(requireHandle())

    fun resetPlayer() {
        if (handle != 0L) {
            nativeRelease(handle)
        }
        handle = nativeCreate()
    }

    fun releasePlayer() {
        if (handle != 0L) {
            nativeRelease(handle)
            handle = 0L
        }
    }

    @Suppress("ProtectedInFinal")
    protected fun finalize() {
        if (handle != 0L) {
            nativeRelease(handle)
            handle = 0L
        }
    }

    private fun requireHandle(): Long = require(handle != 0L) { "Native macOS audio bridge has been released" }.let { handle }

    private external fun nativeCreate(): Long
    private external fun nativeLoad(handle: Long, path: String): Boolean
    private external fun nativePlay(handle: Long): Boolean
    private external fun nativePause(handle: Long)
    private external fun nativeStop(handle: Long)
    private external fun nativeSeekTo(handle: Long, positionMillis: Long)
    private external fun nativeCurrentPositionMillis(handle: Long): Long
    private external fun nativeDurationMillis(handle: Long): Long
    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            MacAudioNativeLibrary.load()
        }
    }
}

private object MacAudioNativeLibrary {
    private const val LIBRARY_NAME = "librhythhaus_audio.dylib"

    fun load() {
        val resourcePath = nativeResourcePath()
        val resource = requireNotNull(MacAudioNativeLibrary::class.java.getResourceAsStream(resourcePath)) {
            "Native macOS audio helper resource not found: $resourcePath"
        }
        val libraryPath = Files.createTempFile("rhythhaus-audio", ".dylib")
        resource.use { input ->
            Files.copy(input, libraryPath, StandardCopyOption.REPLACE_EXISTING)
        }
        libraryPath.toFile().deleteOnExit()
        System.load(libraryPath.toAbsolutePath().toString())
    }

    private fun nativeResourcePath(): String {
        val architecture = System.getProperty("os.arch").lowercase()
        val platform = when {
            System.getProperty("os.name").contains("Mac", ignoreCase = true) && architecture in setOf("aarch64", "arm64") -> "macos-aarch64"
            System.getProperty("os.name").contains("Mac", ignoreCase = true) && architecture == "x86_64" -> "macos-x64"
            else -> error("Native macOS audio helper is only available on macOS, current os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
        }
        return "/native/$platform/$LIBRARY_NAME"
    }
}

private fun AudioSource.jvmFile(): File = when (this) {
    is AudioSource.FilePath -> File(path)
    is AudioSource.Uri -> if (value.startsWith("file:")) File(java.net.URI(value)) else File(value)
}
