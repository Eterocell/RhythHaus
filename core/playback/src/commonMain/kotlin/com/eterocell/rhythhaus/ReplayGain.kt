package com.eterocell.rhythhaus

import kotlin.math.pow

/** Selects the ReplayGain tag pair used for attenuation-only normalization. */
public enum class ReplayGainMode(
    /** Stable lowercase value stored in listener preferences. */
    public val serialized: String,
) {
    /** Leaves media at its original application gain. */
    Off("off"),
    /** Uses the current track's gain and corresponding peak. */
    Track("track"),
    /**
     * Uses the album pair, or the track pair when album gain is unavailable.
     */
    Album("album");

    /** Restores persisted normalization selections. */
    public companion object {
        /**
         * Restores a known mode, defaulting invalid or missing values to Off.
         */
        public fun fromSerialized(value: String?): ReplayGainMode =
            when (value) {
                "track" -> Track
                "album" -> Album
                else -> Off
            }
    }
}

/**
 * Computes finite application attenuation without amplifying media. A missing
 * selected gain leaves playback unchanged, even when a peak is available.
 */
public fun replayGainFactor(
    track: PlayableTrack?,
    mode: ReplayGainMode
): Float {
    if (track == null || mode == ReplayGainMode.Off) return 1f
    val album =
        mode == ReplayGainMode.Album &&
            track.replayGainAlbumGainDb?.isFinite() == true
    val gainDb =
        (if (album) track.replayGainAlbumGainDb
            else track.replayGainTrackGainDb)
            ?.takeIf { it.isFinite() } ?: return 1f
    val peak =
        (if (album) track.replayGainAlbumPeak else track.replayGainTrackPeak)
            ?.takeIf { it.isFinite() && it > 0.0 }
    // Clamp before exponentiation so even the largest finite positive tag
    // cannot overflow. Tiny peaks cannot limit attenuation, so never invert
    // them (their reciprocal may overflow).
    val gain = 10.0.pow(gainDb.coerceAtMost(0.0) / 20.0)
    val bounded =
        if (peak != null && peak > 1.0) minOf(gain, 1.0 / peak) else gain
    return bounded.toFloat()
}
