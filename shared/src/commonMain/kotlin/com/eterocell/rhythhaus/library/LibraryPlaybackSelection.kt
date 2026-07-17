package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.QueueOccurrence

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

internal fun selectOccurrenceForPlayback(
    playbackController: PlaybackController,
    visibleQueue: List<QueueOccurrence>,
    selectedOccurrenceId: String,
) {
    if (visibleQueue.none { it.id == selectedOccurrenceId }) return
    if (playbackController.state.value.currentOccurrenceId == selectedOccurrenceId) {
        playbackController.restartCurrentTrack()
    } else {
        playbackController.setOccurrenceQueue(visibleQueue, selectedOccurrenceId)
        playbackController.play()
    }
}
