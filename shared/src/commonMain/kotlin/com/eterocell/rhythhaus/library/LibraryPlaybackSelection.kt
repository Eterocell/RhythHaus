package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController

internal fun selectLibraryTrackForPlayback(
    playbackController: PlaybackController,
    visibleQueue: List<PlayableTrack>,
    selectedTrackId: String,
) {
    if (visibleQueue.none { it.id == selectedTrackId }) return
    if (playbackController.state.value.currentTrack?.id == selectedTrackId) {
        playbackController.restartCurrentTrack()
    } else {
        playbackController.setQueue(visibleQueue, selectedTrackId)
        playbackController.play()
    }
}
