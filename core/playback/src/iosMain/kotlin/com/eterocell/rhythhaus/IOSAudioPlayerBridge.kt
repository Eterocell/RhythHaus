package com.eterocell.rhythhaus

/**
 * Swift-owned iOS audio backend. AVAudioPlayerDelegate is more reliable in
 * Swift than polling AVAudioPlayer from Kotlin/Native after the app is
 * backgrounded/locked.
 */
public interface IOSAudioPlayerCompletionHandler {
    /** Notifies Kotlin that playback completed. */
    public fun onPlaybackCompleted(): Unit
}

/** Receives system-initiated interruption and route events from the audio bridge. */
public interface IOSAudioInterruptionHandler {
    /** System interrupted an actively-playing session. */
    public fun onInterruptionBegan()

    /** Ends an interruption; [shouldResume] is the system resume recommendation. */
    public fun onInterruptionEnded(shouldResume: Boolean)

    /** The active output route disconnected. */
    public fun onRouteDisconnected()
}

/** Swift-owned operations exposed to the Kotlin playback engine. */
public interface IOSAudioPlayerProvider {
    /** Receives playback-completion notifications. */
    public var completionHandler: IOSAudioPlayerCompletionHandler?

    /** Receives system interruption and route notifications. */
    public var interruptionHandler: IOSAudioInterruptionHandler?

    /** Loads the audio file at [filePath]. */
    public fun load(filePath: String): Boolean

    /** Starts playback. */
    public fun play(): Boolean

    /** Pauses playback. */
    public fun pause(): Unit

    /** Stops playback. */
    public fun stop(): Unit

    /** Seeks to [positionMillis]. */
    public fun seekTo(positionMillis: Long): Unit

    /** Returns the current playback position. */
    public fun currentPositionMillis(): Long

    /** Returns the current media duration when known. */
    public fun currentDurationMillis(): Long?

    /** Returns whether playback is active. */
    public fun isPlaying(): Boolean

    /** Fades out then stops playback. */
    public fun fadeOutAndStop(
        fadeDurationSeconds: Double,
        silentVolume: Float
    ): Unit
}

/** Holds the Swift-provided audio backend. */
public object IOSAudioPlayerBridge {
    /** Swift-owned audio backend used by the engine. */
    public var provider: IOSAudioPlayerProvider? = null
}

internal enum class IOSAudioBackend {
    SwiftAVAudioPlayerDelegate,
}

internal val iosAudioBackend: IOSAudioBackend =
    IOSAudioBackend.SwiftAVAudioPlayerDelegate
