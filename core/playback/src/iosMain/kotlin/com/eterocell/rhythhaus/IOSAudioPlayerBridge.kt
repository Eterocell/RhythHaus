package com.eterocell.rhythhaus

/**
 * Swift-owned iOS audio backend. AVAudioPlayerDelegate is more reliable in
 * Swift than polling AVAudioPlayer from Kotlin/Native after the app is
 * backgrounded/locked.
 */
public interface IOSAudioPlayerCompletionHandler {
    /** Notifies Kotlin that playback completed. */
    public fun onPlaybackCompleted(): Unit

    /**
     * Native playback ended unsuccessfully; this is not a natural completion.
     */
    public fun onPlaybackFailed(): Unit
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
    /**
     * Captured by the logical current voice at the next [playAsync] request.
     */
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

    /**
     * Prepares a silent incoming voice without replacing the audible current.
     */
    public fun prepareIncomingAsync(
        filePath: String,
        handler: IOSAudioPlayerLoadHandler
    )

    /**
     * Returns the prepared incoming duration without changing logical current.
     */
    public fun incomingDurationMillis(): Long?

    /**
     * Starts the staged voice at its retained gain and promotes it only after
     * native playback succeeds. [completionHandler] belongs to that voice/run.
     */
    public fun startIncomingAsync(
        completionHandler: IOSAudioPlayerCompletionHandler,
        handler: IOSAudioPlayerPlaybackStartHandler,
    )

    /** Applies finite unit-range gains to the outgoing and incoming voices. */
    public fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float)

    /**
     * Commits the controller-admitted incoming voice by disposing only retiring
     * resources.
     */
    public fun finishCrossfade()

    /**
     * Cancels staging or rolls back a started incoming voice while outgoing is
     * retained. Native Playing does not commit controller admission. Restore
     * outgoing player, run, gain and position without replay; after
     * [finishCrossfade] cancellation cannot roll back.
     */
    public fun cancelPreparedIncoming()

    /** Pauses both audible voices without changing their gains. */
    public fun pause(): Unit

    /**
     * Disposes both voices; retains logical media for a later ordinary play.
     */
    public fun stop(): Unit

    /** Retires outgoing and seeks logical current to [positionMillis]. */
    public fun seekTo(positionMillis: Long): Unit

    /** Returns the current playback position. */
    public fun currentPositionMillis(): Long

    /** Returns the current media duration when known. */
    public fun currentDurationMillis(): Long?

    /** Returns whether playback is active. */
    public fun isPlaying(): Boolean

    /**
     * Applies a finite playback-only gain between 0 and 1 to logical current.
     */
    public fun setPlaybackGain(gain: Float): Unit

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
