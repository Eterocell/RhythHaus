package com.eterocell.rhythhaus

import kotlinx.coroutines.flow.StateFlow

/** Platform-verified description of the currently active audio output. */
public sealed interface PlaybackOutputStatus {
    /** One or more names reported for the active route. */
    public data class Available(
        /** Names returned by the active route query. */
        public val names: List<String>,
    ) : PlaybackOutputStatus

    /** The platform cannot identify an active route reliably. */
    public data object Unavailable : PlaybackOutputStatus
}

/** Narrow read-only route projection implemented by platform playback hosts. */
public interface PlaybackOutputStatusProvider {
    /** Current route state. */
    public val outputStatus: StateFlow<PlaybackOutputStatus>

    /** Refreshes the route query from the native host. */
    public fun refreshOutputStatus()
}
