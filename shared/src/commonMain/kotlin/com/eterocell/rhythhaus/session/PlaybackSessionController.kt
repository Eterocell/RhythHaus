package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import kotlinx.coroutines.flow.Flow

internal interface PlaybackSessionController {
    val checkpoints: Flow<PlaybackCheckpoint>
    fun sessionSnapshot(): PlaybackSessionSnapshot
    suspend fun restoreSession(
        snapshot: PlaybackSessionSnapshot,
        tracks: List<PlayableTrack>,
    ): RevisionedPlaybackSessionSnapshot
    suspend fun reconcileSession(tracks: List<PlayableTrack>): RevisionedPlaybackSessionSnapshot
    suspend fun awaitCheckpointFence()
    fun setCommandsEnabled(enabled: Boolean)
}

data class RevisionedPlaybackSessionSnapshot(
    val snapshot: PlaybackSessionSnapshot,
    val revision: Long?,
)
