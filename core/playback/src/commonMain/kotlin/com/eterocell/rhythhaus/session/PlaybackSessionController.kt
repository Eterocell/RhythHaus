package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import kotlinx.coroutines.flow.Flow

/** Operations used to coordinate persisted playback sessions. */
public interface PlaybackSessionController {
    /** Ordered checkpoints that a single persistence owner consumes. */
    public val checkpoints: Flow<PlaybackCheckpoint>

    /** Returns the current session state without persisting it. */
    public fun sessionSnapshot(): PlaybackSessionSnapshot

    /** Restores [snapshot] using the available [tracks]. */
    public suspend fun restoreSession(
        snapshot: PlaybackSessionSnapshot,
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot

    /** Reconciles the current session with the available [tracks]. */
    public suspend fun reconcileSession(
        tracks: List<PlayableTrack>
    ): RevisionedPlaybackSessionSnapshot

    /** Suspends until previously emitted checkpoints have crossed the fence. */
    public suspend fun awaitCheckpointFence()

    /** Enables or disables playback commands during session coordination. */
    public fun setCommandsEnabled(enabled: Boolean)
}

/** Associates a persisted session state with its monotonic revision. */
public data class RevisionedPlaybackSessionSnapshot(
    /** Session state captured at [revision]. */
    public val snapshot: PlaybackSessionSnapshot,
    /** Revision associated with [snapshot], when one was persisted. */
    public val revision: Long?,
)
