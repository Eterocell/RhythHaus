package com.eterocell.rhythhaus

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = MacOSNativePlaybackEngine()

private class MacOSNativePlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private val bridge = MacAudioPlayerBridge()
    private val progressExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rhythhaus-macos-playback-progress").apply { isDaemon = true }
    }
    private var progressTask: ScheduledFuture<*>? = null
    private var durationMillis: Long? = null
    private var completionReported: Boolean = false

    override fun load(track: PlayableTrack) {
        stopProgressUpdates()
        bridge.resetPlayer()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val loaded = bridge.load(track.source.jvmFile().absolutePath)
        require(loaded) { "Could not load native macOS audio player" }
        durationMillis = track.durationMillis ?: bridge.durationMillis().takeIf { it > 0L }
        completionReported = false
        bridge.setArtwork(track.artworkBytes)
        bridge.registerNowPlayingRemoteCommands()
        bridge.updateNowPlayingInfo(track.title, track.artist, track.album, durationMillis, positionMillis = 0L)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        require(bridge.play()) { "No native macOS player has been loaded" }
        bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        publishProgress()
        startProgressUpdates()
    }

    override fun pause() {
        stopProgressUpdates()
        bridge.pause()
        bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
        publishProgress()
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        stopProgressUpdates()
        bridge.stop()
        bridge.updateNowPlayingPlaybackState(PlaybackStatus.Stopped)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        bridge.seekTo(positionMillis)
        publishProgress()
    }

    override fun release() {
        stopProgressUpdates()
        bridge.releasePlayer()
        progressExecutor.shutdownNow()
        durationMillis = null
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressTask = progressExecutor.scheduleAtFixedRate(
            { publishProgress() },
            100L,
            100L,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun stopProgressUpdates() {
        progressTask?.cancel(false)
        progressTask = null
    }

    private fun publishProgress() {
        val positionMillis = bridge.currentPositionMillis().coerceAtLeast(0L)
        val latestDurationMillis = bridge.durationMillis().takeIf { it > 0L } ?: durationMillis
        bridge.updateNowPlayingPosition(positionMillis, latestDurationMillis)
        listener?.onPlaybackProgress(
            positionMillis = positionMillis,
            durationMillis = latestDurationMillis,
        )
        if (!completionReported && latestDurationMillis != null && positionMillis >= latestDurationMillis) {
            completionReported = true
            listener?.onPlaybackCompleted()
        }
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
    fun updateNowPlayingInfo(
        title: String,
        artist: String,
        album: String?,
        durationMillis: Long?,
        positionMillis: Long,
    ) = nativeUpdateNowPlayingInfo(requireHandle(), title, artist, album, durationMillis ?: 0L, positionMillis)
    fun updateNowPlayingPosition(positionMillis: Long, durationMillis: Long?) = nativeUpdateNowPlayingPosition(requireHandle(), positionMillis, durationMillis ?: 0L)
    fun updateNowPlayingPlaybackState(status: PlaybackStatus) = nativeUpdateNowPlayingPlaybackState(requireHandle(), status.macosPlaybackStateCode())
    fun registerNowPlayingRemoteCommands() = nativeRegisterNowPlayingRemoteCommands(requireHandle())
    fun clearNowPlayingInfo() = nativeClearNowPlayingInfo(requireHandle())
    fun setArtwork(artworkBytes: ByteArray?) = nativeSetArtwork(requireHandle(), artworkBytes)

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
    private external fun nativeUpdateNowPlayingInfo(handle: Long, title: String, artist: String, album: String?, durationMillis: Long, positionMillis: Long)
    private external fun nativeUpdateNowPlayingPosition(handle: Long, positionMillis: Long, durationMillis: Long)
    private external fun nativeUpdateNowPlayingPlaybackState(handle: Long, playbackStateCode: Int)
    private external fun nativeRegisterNowPlayingRemoteCommands(handle: Long)
    private external fun nativeClearNowPlayingInfo(handle: Long)
    private external fun nativeSetArtwork(handle: Long, artworkBytes: ByteArray?)
    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            MacAudioNativeLibrary.load()
        }
    }
}

private fun PlaybackStatus.macosPlaybackStateCode(): Int = when (this) {
    PlaybackStatus.Playing -> 1
    PlaybackStatus.Paused -> 2
    else -> 0
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
    is AudioSource.FileDescriptor -> error("File descriptor audio sources are metadata-only and cannot be played")
}
