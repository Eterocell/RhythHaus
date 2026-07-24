package com.eterocell.rhythhaus

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground-capable Media3 service that hosts the ExoPlayer and [MediaSession] for RhythHaus.
 *
 * Hosting the player in a [MediaSessionService] (rather than building a standalone session in an
 * Activity) is what lets the OS route hardware media buttons — the play/pause control on a wired
 * cable inline remote or a Bluetooth headset — and lock-screen/notification transport controls to
 * the session, and lets playback continue in the background. The session is created in [onCreate]
 * and reused for the process lifetime.
 *
 * The shared [PlaybackController] owns the playback queue, so the session player is wrapped in a
 * [SkipRoutingPlayer] that advertises next/previous as available commands and routes those events
 * to [RhythHausTransportBridge] instead of advancing an internal playlist. This keeps queue state
 * in exactly one place.
 */
class RhythHausPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivityIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { launchIntent ->
                PendingIntent.getActivity(
                    this,
                    /* requestCode= */
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            }

        mediaSession = MediaSession.Builder(this, SkipRoutingPlayer(exoPlayer))
            .apply { sessionActivityIntent?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Intentionally does not call super: when media is actively playing we keep the foreground
        // service alive after the task is swiped away (the canonical media3 pattern). Only tear down
        // when nothing is playing. See developer.android.com/media/media3/session/background-playback.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

/**
 * A [androidx.media3.common.ForwardingPlayer] that always advertises the seek-to-next/previous
 * commands and forwards them to [RhythHausTransportBridge], so hardware and system transport
 * controls can drive queue navigation owned by the shared [PlaybackController]. All other commands
 * delegate to the wrapped ExoPlayer unchanged.
 */
internal class SkipRoutingPlayer(player: Player) : androidx.media3.common.ForwardingPlayer(player) {
    private val transportRouter = ServiceTransportRouter()

    override fun getAvailableCommands(): Player.Commands = transportAvailableCommands(super.getAvailableCommands())

    override fun isCommandAvailable(command: Int): Boolean = transportRouter.isCommandAvailable(command, super.isCommandAvailable(command))

    override fun play() {
        transportRouter.play { super.play() }
    }

    override fun pause() {
        transportRouter.pause { super.pause() }
    }

    override fun stop() {
        transportRouter.stop { super.stop() }
    }

    override fun seekTo(positionMs: Long) {
        transportRouter.seekTo(positionMs) { super.seekTo(it) }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        transportRouter.seekTo(mediaItemIndex, positionMs) { index, position -> super.seekTo(index, position) }
    }

    override fun seekToNext() {
        transportRouter.next { RhythHausTransportBridge.skipToNext() }
    }

    override fun seekToNextMediaItem() {
        transportRouter.next { RhythHausTransportBridge.skipToNext() }
    }

    override fun seekToPrevious() {
        transportRouter.previous { RhythHausTransportBridge.skipToPrevious() }
    }

    override fun seekToPreviousMediaItem() {
        transportRouter.previous { RhythHausTransportBridge.skipToPrevious() }
    }
}
