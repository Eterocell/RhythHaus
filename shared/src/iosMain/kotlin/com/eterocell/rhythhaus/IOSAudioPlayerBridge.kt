package com.eterocell.rhythhaus

/**
 * Swift-owned iOS audio backend. AVAudioPlayerDelegate is more reliable in Swift than polling
 * AVAudioPlayer from Kotlin/Native after the app is backgrounded/locked.
 */
interface IOSAudioPlayerCompletionHandler {
    fun onPlaybackCompleted()
}

interface IOSAudioPlayerProvider {
    var completionHandler: IOSAudioPlayerCompletionHandler?

    fun load(filePath: String): Boolean
    fun play(): Boolean
    fun pause()
    fun stop()
    fun seekTo(positionMillis: Long)
    fun currentPositionMillis(): Long
    fun currentDurationMillis(): Long?
    fun isPlaying(): Boolean
    fun fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float)
}

object IOSAudioPlayerBridge {
    var provider: IOSAudioPlayerProvider? = null
}

internal enum class IOSAudioBackend {
    SwiftAVAudioPlayerDelegate,
}

internal val iosAudioBackend: IOSAudioBackend = IOSAudioBackend.SwiftAVAudioPlayerDelegate
