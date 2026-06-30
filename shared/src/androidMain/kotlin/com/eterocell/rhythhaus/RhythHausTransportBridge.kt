package com.eterocell.rhythhaus

/**
 * Process-level bridge that lets the Media3 [RhythHausPlaybackService] route hardware/system
 * "skip to next/previous" transport commands back to the shared [PlaybackController] without the
 * service holding a direct reference to it.
 *
 * The shared controller owns the playback queue (it is the single source of truth that also drives
 * the Compose UI). The Media3 player in the service therefore advertises next/previous as available
 * commands but does not advance its own single-item playlist; instead it invokes these handlers so
 * the controller selects the adjacent queue track and re-loads the engine. This keeps queue state
 * in exactly one place and avoids two diverging playlists.
 */
internal object RhythHausTransportBridge {
    @Volatile
    var onSkipToNext: (() -> Unit)? = null

    @Volatile
    var onSkipToPrevious: (() -> Unit)? = null

    fun skipToNext() {
        onSkipToNext?.invoke()
    }

    fun skipToPrevious() {
        onSkipToPrevious?.invoke()
    }
}
