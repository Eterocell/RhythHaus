package com.eterocell.rhythhaus

/** Listener-selected overlap duration; Off preserves ordinary playback. */
public enum class CrossfadeDuration(
    /** Requested overlap in milliseconds; zero disables overlap. */
    public val millis: Long,
) {
    Off(0),
    Seconds3(3000),
    Seconds5(5000),
    Seconds8(8000),
}

/**
 * A single system playback session with a silent staged and a retiring voice.
 */
public interface CrossfadePlaybackEngine : PlatformPlaybackEngine {
    /**
     * Stages [track] under [generation] without changing the audible current
     * voice.
     */
    public suspend fun prepareIncoming(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback

    /**
     * Starts the prepared voice with [playbackRunId] and makes it logical
     * current.
     */
    public fun startIncoming(playbackRunId: Long)

    /**
     * Sets finite, unit-range [outgoingGain] and [incomingGain] independently.
     */
    public fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float)

    /**
     * Releases the retiring voice without affecting logical current playback.
     */
    public fun finishCrossfade()

    /**
     * Releases a staged voice or rolls back a started incoming voice while
     * outgoing is retained, restoring outgoing identity/run/gain without
     * restarting audio. Only the controller admits promotion: even a native
     * Playing event may have lost a cancellation race. Accepted promotions use
     * [finishCrossfade] instead and can never roll back on failure.
     */
    public fun cancelPreparedIncoming()
}

/** Shares the controller's injectable monotonic scheduling boundary. */
internal class CrossfadeRuntime(private val runtime: SleepTimerRuntime) {
    fun nowMillis(): Long = runtime.nowMillis()

    suspend fun delay(millis: Long) = runtime.delay(millis)
}

/**
 * Computes a safe overlap from [requestedMillis], [outgoingMillis],
 * [incomingMillis], and [remainingMillis].
 */
public fun effectiveCrossfadeWindowMillis(
    requestedMillis: Long,
    outgoingMillis: Long,
    incomingMillis: Long,
    remainingMillis: Long,
): Long? {
    if (requestedMillis <= 0 ||
        outgoingMillis <= 0 ||
        incomingMillis <= 0 ||
        remainingMillis <= 0)
        return null
    val window =
        minOf(
            requestedMillis,
            outgoingMillis / 2,
            incomingMillis / 2,
            remainingMillis)
    return window.takeIf { it >= 250 }
}
