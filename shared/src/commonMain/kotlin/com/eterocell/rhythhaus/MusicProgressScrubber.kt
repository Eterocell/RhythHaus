package com.eterocell.rhythhaus

internal fun scrubberFractionForOffset(offsetX: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return (offsetX / widthPx).coerceIn(0f, 1f)
}

internal fun scrubberPositionForFraction(fraction: Float, durationMillis: Long): Long {
    if (durationMillis <= 0L) return 0L
    return (durationMillis * fraction.coerceIn(0f, 1f)).toLong().coerceIn(0L, durationMillis)
}

internal class MusicScrubInteractionState(
    positionMillis: Long,
    durationMillis: Long,
) {
    private var playbackPositionMillis: Long = positionMillis.coerceAtLeast(0L)
    private var playbackDurationMillis: Long = durationMillis.coerceAtLeast(0L)
    private var scrubFraction: Float? = null

    val displayPositionMillis: Long
        get() = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
            ?: playbackPositionMillis.coerceIn(0L, playbackDurationMillis.takeIf { it > 0L } ?: Long.MAX_VALUE)

    val displayFraction: Float
        get() {
            val duration = playbackDurationMillis
            if (duration <= 0L) return 0f
            return scrubFraction ?: (playbackPositionMillis.coerceIn(0L, duration).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    fun updatePlaybackPosition(positionMillis: Long, durationMillis: Long) {
        playbackPositionMillis = positionMillis.coerceAtLeast(0L)
        playbackDurationMillis = durationMillis.coerceAtLeast(0L)
    }

    fun startScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun updateScrub(fraction: Float) {
        scrubFraction = fraction.coerceIn(0f, 1f)
    }

    fun finishScrub(): Long? {
        val target = scrubFraction?.let { scrubberPositionForFraction(it, playbackDurationMillis) }
        scrubFraction = null
        return target
    }

    fun cancelScrub() {
        scrubFraction = null
    }
}
