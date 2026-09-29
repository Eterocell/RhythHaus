package com.eterocell.rhythhaus

import kotlin.time.TimeSource

/** Selects the boundary that ends an armed sleep timer. */
public enum class SleepTimerMode {
    /** Ends playback after a monotonic elapsed interval. */
    Timed,
    /** Ends playback after the selected number of natural completions. */
    TrackCount,
}

/** Immutable, process-local projection of the currently armed sleep timer. */
public data class SleepTimerState(
    /** Active timer mode, or null when no timer is armed. */
    public val mode: SleepTimerMode? = null,
    /** Monotonic time remaining for [SleepTimerMode.Timed], when armed. */
    public val remainingMillis: Long? = null,
    /**
     * Natural completions remaining for [SleepTimerMode.TrackCount], when
     * armed.
     */
    public val remainingTracks: Int? = null,
    /**
     * Whether the active timer fades playback output before its stop boundary.
     */
    public val fadeEnabled: Boolean = false,
)

/**
 * Controller-private time and wake-up seam. Production uses monotonic elapsed
 * time; core behavior tests drive this seam without real-time sleeps.
 */
internal interface SleepTimerRuntime {
    fun nowMillis(): Long

    suspend fun delay(millis: Long)
}

internal object MonotonicSleepTimerRuntime : SleepTimerRuntime {
    private val origin = TimeSource.Monotonic.markNow()

    override fun nowMillis(): Long = origin.elapsedNow().inWholeMilliseconds

    override suspend fun delay(millis: Long) {
        kotlinx.coroutines.delay(millis)
    }
}

internal const val sleepTimerFadeWindowMillis: Long = 10_000L
internal const val sleepTimerStateUpdateMillis: Long = 1_000L
internal const val sleepTimerFadeUpdateMillis: Long = 250L
