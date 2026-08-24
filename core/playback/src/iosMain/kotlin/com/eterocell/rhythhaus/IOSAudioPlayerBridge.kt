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

/**
 * Receives system-initiated interruption and route events from the audio
 * bridge.
 */
public interface IOSAudioInterruptionHandler {
    /** System interrupted an actively-playing session. */
    public fun onInterruptionBegan()

    /**
     * Ends an interruption; [shouldResume] is the system resume recommendation.
     */
    public fun onInterruptionEnded(shouldResume: Boolean)

    /** The active output route disconnected. */
    public fun onRouteDisconnected()
}

/**
 * Receives the result of asynchronous audio-session activation and playback
 * start.
 */
public interface IOSAudioPlayerPlaybackStartHandler {
    /** Native playback is active. */
    public fun onPlaybackStarted()

    /** Audio-session activation or native playback failed. */
    public fun onPlaybackStartFailed()
}

/** Receives the result of asynchronous native audio-player preparation. */
public interface IOSAudioPlayerLoadHandler {
    /** The audio player has been prepared and installed. */
    public fun onAudioLoaded()

    /** Native audio-player construction or preparation failed. */
    public fun onAudioLoadFailed()
}

/** Swift-owned operations exposed to the Kotlin playback engine. */
public interface IOSAudioPlayerProvider {
    /** Receives playback-completion notifications. */
    public var completionHandler: IOSAudioPlayerCompletionHandler?

    /** Receives system interruption and route notifications. */
    public var interruptionHandler: IOSAudioInterruptionHandler?

    /**
     * Loads and prepares the audio file without blocking the calling thread.
     */
    public fun loadAsync(filePath: String, handler: IOSAudioPlayerLoadHandler)

    /**
     * Starts playback without blocking the calling thread on audio-session
     * activation.
     */
    public fun playAsync(handler: IOSAudioPlayerPlaybackStartHandler)

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
    private var confinedProvider: IOSAudioPlayerProvider? = null
    public var provider: IOSAudioPlayerProvider?
        get() = withIOSPlaybackMainThread { confinedProvider }
        set(value) = withIOSPlaybackMainThread { confinedProvider = value }
}

internal enum class IOSAudioBackend {
    SwiftAVAudioPlayerDelegate,
}

internal val iosAudioBackend: IOSAudioBackend =
    IOSAudioBackend.SwiftAVAudioPlayerDelegate
