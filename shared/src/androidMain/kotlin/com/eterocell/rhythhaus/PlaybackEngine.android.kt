package com.eterocell.rhythhaus

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private var rhythHausAndroidContext: Context? = null

fun setRhythHausAndroidContext(context: Context) {
    rhythHausAndroidContext = context.applicationContext
}

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = AndroidPlaybackEngine()

/**
 * Android playback engine backed by a Media3 [MediaController] connected to
 * [RhythHausPlaybackService]. Hosting the player in a `MediaSessionService` is what lets the OS
 * deliver hardware media-button events (wired cable inline remote, Bluetooth headset) and
 * lock-screen/notification transport controls to the session.
 *
 * The shared [PlaybackController] owns the queue; this engine drives a single media item at a time
 * and relays the controller's transport callbacks. Hardware next/previous arrive through
 * [RhythHausTransportBridge] (routed by the service's ForwardingPlayer) and are forwarded to the
 * shared listener so queue navigation stays in one place.
 */
private class AndroidPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
        set(value) {
            field = value
            // Route hardware/system skip transport (delivered to the service) into the shared listener.
            RhythHausTransportBridge.onSkipToNext = { value?.onSkipToNext() }
            RhythHausTransportBridge.onSkipToPrevious = { value?.onSkipToPrevious() }
        }

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var loadedTrackDurationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    /** True once [release] has run; guards async connection callbacks from resurrecting the engine. */
    private var disposed: Boolean = false

    /**
     * Actions queued while the async MediaController connection is still pending, run in order once
     * connected. A FIFO queue (not a single slot) is required because the shared controller issues
     * compound sequences such as load (setMediaItem + prepare) immediately followed by play — a
     * single slot would drop the load and leave the player with no media item.
     */
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()

    private fun withController(action: (MediaController) -> Unit) {
        if (disposed) return
        val connected = controller
        if (connected != null) {
            action(connected)
            return
        }
        pendingActions.addLast(action)
        ensureControllerConnecting()
    }

    private fun ensureControllerConnecting() {
        if (controllerFuture != null) return
        val context = requireNotNull(rhythHausAndroidContext) {
            "Android playback context is not configured"
        }
        val token = SessionToken(context, ComponentName(context, RhythHausPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                // The engine may have been released while the connection was in flight.
                if (disposed) {
                    runCatching { future.get() }.getOrNull()?.release()
                    return@addListener
                }
                val connected = try {
                    future.get()
                } catch (t: Throwable) {
                    listener?.onPlaybackError(
                        PlaybackError(
                            message = "Android could not start the playback service.",
                            cause = t.message ?: t::class.simpleName,
                        ),
                    )
                    controllerFuture = null
                    pendingActions.clear()
                    return@addListener
                }
                connected.addListener(AndroidPlayerListener())
                controller = connected
                while (pendingActions.isNotEmpty()) {
                    pendingActions.removeFirst().invoke(connected)
                }
            },
            // Run the completion callback on the main thread (controller is single-threaded).
            { runnable -> scope.launch { runnable.run() } },
        )
    }

    override fun load(track: PlayableTrack) {
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        loadedTrackDurationMillis = track.durationMillis
        withController { controller ->
            controller.setMediaItem(buildAndroidPlaybackMediaItem(track))
            controller.prepare()
        }
    }

    override fun play() {
        withController { controller ->
            controller.play()
            listener?.onPlaybackStatus(PlaybackStatus.Playing)
            publishProgress(controller)
            startProgressLoop()
        }
    }

    override fun pause() {
        progressJob?.cancel()
        withController { controller ->
            controller.pause()
            publishProgress(controller)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
        }
    }

    override fun stop() {
        progressJob?.cancel()
        val connected = controller
        if (connected != null) {
            connected.pause()
            connected.seekTo(0L)
            publishProgress(connected)
        } else {
            listener?.onPlaybackProgress(0L, loadedTrackDurationMillis)
        }
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        withController { controller ->
            controller.seekTo(positionMillis)
            publishProgress(controller)
        }
    }

    override fun release() {
        disposed = true
        progressJob?.cancel()
        RhythHausTransportBridge.onSkipToNext = null
        RhythHausTransportBridge.onSkipToPrevious = null
        pendingActions.clear()
        val connected = controller
        if (connected != null) {
            // Connection completed: release the controller we hold.
            connected.release()
        } else {
            // Still connecting: cancel/release the in-flight future. The connection callback
            // checks `disposed` and releases any controller it produces after this point.
            controllerFuture?.let(MediaController::releaseFuture)
        }
        controller = null
        controllerFuture = null
        loadedTrackDurationMillis = null
        scope.cancel()
    }

    private fun publishProgress(controller: MediaController) {
        listener?.onPlaybackProgress(
            positionMillis = controller.currentPosition.coerceAtLeast(0L),
            durationMillis = controller.duration.takeIf { it > 0L } ?: loadedTrackDurationMillis,
        )
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(250)
                val c = controller ?: break
                if (!c.isPlaying) continue
                listener?.onPlaybackProgress(
                    positionMillis = c.currentPosition.coerceAtLeast(0L),
                    durationMillis = c.duration.takeIf { it > 0L } ?: loadedTrackDurationMillis,
                )
            }
        }
    }

    private inner class AndroidPlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> listener?.onPlaybackStatus(PlaybackStatus.Buffering)

                Player.STATE_READY -> {
                    publishProgress(c)
                    listener?.onPlaybackStatus(if (c.playWhenReady) PlaybackStatus.Playing else PlaybackStatus.Paused)
                }

                Player.STATE_ENDED -> listener?.onPlaybackCompleted()

                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listener?.onPlaybackStatus(if (isPlaying) PlaybackStatus.Playing else PlaybackStatus.Paused)
            controller?.let(::publishProgress)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onPlaybackError(
                PlaybackError(
                    message = "Android could not play this audio file.",
                    cause = error.message ?: error.errorCodeName,
                ),
            )
        }
    }
}

internal fun buildAndroidPlaybackMediaItem(track: PlayableTrack): MediaItem = MediaItem.Builder()
    .setMediaId(track.id)
    .setUri(track.source.androidUri())
    .setMediaMetadata(buildAndroidPlaybackMediaMetadata(track))
    .build()

internal fun buildAndroidPlaybackMediaMetadata(track: PlayableTrack): MediaMetadata {
    val builder = MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
        .setAlbumTitle(track.album)
    track.artworkBytes?.let { artwork ->
        builder.setArtworkData(artwork)
    }
    return builder.build()
}

private fun AudioSource.androidUri(): Uri = when (this) {
    is AudioSource.FilePath -> Uri.fromFile(java.io.File(path))
    is AudioSource.Uri -> Uri.parse(value)
}
