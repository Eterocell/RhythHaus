package com.eterocell.rhythhaus.session

import com.eterocell.rhythhaus.PlayableTrack
import kotlinx.coroutines.flow.Flow

internal interface PlaybackSessionController {
    val checkpoints: Flow<PlaybackCheckpoint>
    fun sessionSnapshot(): PlaybackSessionSnapshot
    suspend fun restoreSession(snapshot: PlaybackSessionSnapshot, tracks: List<PlayableTrack>)
    suspend fun reconcileSession(tracks: List<PlayableTrack>)
    fun setCommandsEnabled(enabled: Boolean)
}
