package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackSessionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun interface PlaybackSessionRestorer {
    suspend fun restoreOnce(tracks: List<PlayableTrack>)
}

internal class PlaybackProcessLifecycle(
    private val coordinator: PlaybackSessionCoordinator,
    private val processScope: CoroutineScope,
) : PlaybackSessionRestorer {
    private val restoreMutex = Mutex()
    private var restoreAttempt: Deferred<Unit>? = null

    override suspend fun restoreOnce(tracks: List<PlayableTrack>) {
        val sharedAttempt = restoreMutex.withLock {
            restoreAttempt ?: processScope.async {
                coordinator.restoreOnce(tracks)
            }.also { restoreAttempt = it }
        }
        sharedAttempt.await()
    }
}
