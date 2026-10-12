package com.eterocell.rhythhaus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Service progress scheduling follows the current native voice, not retiring
 * terminal events.
 */
internal class AndroidServiceProgressPublisher(
    private val playback: AndroidServicePlayback,
    private val postDelayed: (Runnable) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
) : Runnable {
    override fun run() {
        if (playback.publishProgress()) postDelayed(this)
    }

    fun restart() {
        cancel()
        postDelayed(this)
    }

    fun cancel() {
        removeCallbacks(this)
    }

    fun onPlaybackEvent(event: AndroidServicePlaybackEvent) {
        if (event.status == null && event.failure == null && !event.completed)
            return
        if (!playback.ownsCurrentEvent(event)) return
        if (event.status == PlaybackStatus.Playing) restart() else cancel()
    }
}

/**
 * Foreground-capable Media3 service that hosts two audio voices and one
 * [MediaSession] for RhythHaus.
 *
 * Hosting the player in a [MediaSessionService] (rather than building a
 * standalone session in an Activity) is what lets the OS route hardware media
 * buttons — the play/pause control on a wired cable inline remote or a
 * Bluetooth headset — and lock-screen/notification transport controls to the
 * session, and lets playback continue in the background. The session is created
 * in [onCreate] and reused for the process lifetime.
 *
 * The shared [PlaybackController] owns the playback queue, so the session
 * player is wrapped in a [SkipRoutingPlayer] that advertises next/previous as
 * available commands and routes those events to [RhythHausTransportBridge]
 * instead of advancing an internal playlist. This keeps queue state in exactly
 * one place.
 */
public class RhythHausPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var playback: AndroidServicePlayback
    private lateinit var commandRouter: AndroidServiceCommandRouter
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var hasAudioFocus = false
    private var noisyReceiverRegistered = false
    private val serviceHandler = Handler(Looper.getMainLooper())
    private lateinit var progressPublisher: AndroidServiceProgressPublisher
    private val noisyReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    playback.pauseForSystem()
            }
        }

    /** Creates the process-owned Media3 session and player. */
    public override fun onCreate(): Unit {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(
                            android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(
                    { change ->
                        when (change) {
                            AudioManager.AUDIOFOCUS_GAIN -> hasAudioFocus = true
                            AudioManager.AUDIOFOCUS_LOSS,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                hasAudioFocus = false
                                playback.pauseForSystem()
                            }
                        }
                    },
                    serviceHandler)
                .build()
        playback =
            AndroidServicePlayback(
                createPlayer = {
                    ExoPlayer.Builder(this)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                            /* handleAudioFocus= */ false,
                        )
                        .setHandleAudioBecomingNoisy(false)
                        .build()
                },
                onCurrentPlayerChanged = { player ->
                    mediaSession?.setPlayer(sessionPlayer(player))
                    progressPublisher.restart()
                },
                onEvent = ::publishEngineEvent,
                requestFocus = {
                    if (!hasAudioFocus) {
                        hasAudioFocus =
                            audioManager.requestAudioFocus(audioFocusRequest) ==
                                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    }
                    hasAudioFocus
                },
                abandonFocus = ::abandonAudioFocus,
            )
        progressPublisher =
            AndroidServiceProgressPublisher(
                playback,
                postDelayed = { serviceHandler.postDelayed(it, 250L) },
                removeCallbacks = serviceHandler::removeCallbacks,
            )
        commandRouter =
            AndroidServiceCommandRouter(playback, applicationInfo.uid)
        val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(
                noisyReceiver, noisyFilter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(noisyReceiver, noisyFilter)
        noisyReceiverRegistered = true

        val sessionActivityIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                launchIntent ->
                PendingIntent.getActivity(
                    this,
                    /* requestCode= */ 0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            }

        mediaSession =
            MediaSession.Builder(
                    this,
                    sessionPlayer(playback.initialize()),
                )
                .setCallback(
                    object : MediaSession.Callback {
                        override fun onConnectAsync(
                            session: MediaSession,
                            controller: MediaSession.ControllerInfo,
                        ): ListenableFuture<MediaSession.ConnectionResult> {
                            val internal = isEngineController(controller)
                            val sessionCommands =
                                MediaSession.ConnectionResult
                                    .DEFAULT_SESSION_COMMANDS
                                    .buildUpon()
                                    .apply {
                                        if (internal)
                                            add(androidEngineSessionCommand())
                                    }
                                    .build()
                            val playerCommands =
                                MediaSession.ConnectionResult
                                    .DEFAULT_PLAYER_COMMANDS
                                    .buildUpon()
                                    .apply {
                                        // Source/gain/run mutations only use
                                        // the fenced custom
                                        // command channel, even on the trusted
                                        // connection.
                                        remove(Player.COMMAND_SET_MEDIA_ITEM)
                                        remove(
                                            Player.COMMAND_CHANGE_MEDIA_ITEMS)
                                        remove(Player.COMMAND_PREPARE)
                                        remove(Player.COMMAND_SET_VOLUME)
                                        remove(Player.COMMAND_SET_REPEAT_MODE)
                                        remove(Player.COMMAND_SET_SHUFFLE_MODE)
                                    }
                                    .build()
                            return Futures.immediateFuture(
                                MediaSession.ConnectionResult
                                    .AcceptedResultBuilder(session, controller)
                                    .setAvailableSessionCommands(
                                        sessionCommands)
                                    .setAvailablePlayerCommands(playerCommands)
                                    .build())
                        }

                        override fun onCustomCommand(
                            session: MediaSession,
                            controller: MediaSession.ControllerInfo,
                            customCommand: SessionCommand,
                            args: Bundle,
                        ): ListenableFuture<SessionResult> {
                            if (customCommand.customAction !=
                                RHYTHHAUS_ENGINE_COMMAND) {
                                return Futures.immediateFuture(
                                    SessionResult(
                                        SessionResult
                                            .RESULT_ERROR_NOT_SUPPORTED))
                            }
                            // Check identity before decoding a native operation
                            // or touching either voice.
                            if (!isEngineController(controller)) {
                                return Futures.immediateFuture(
                                    SessionResult(
                                        SessionResult
                                            .RESULT_ERROR_PERMISSION_DENIED))
                            }
                            val command =
                                try {
                                    androidServiceEngineCommand(args)
                                } catch (_: IllegalArgumentException) {
                                    return Futures.immediateFuture(
                                        SessionResult(
                                            SessionResult
                                                .RESULT_ERROR_BAD_VALUE))
                                } catch (_: IllegalStateException) {
                                    return Futures.immediateFuture(
                                        SessionResult(
                                            SessionResult
                                                .RESULT_ERROR_BAD_VALUE))
                                }
                            val result =
                                commandRouter.dispatch(
                                    controller.uid, true, command)
                            return Futures.transform(
                                result,
                                { it!!.toAndroidSessionResult() },
                                MoreExecutors.directExecutor())
                        }

                        override fun onPlayerCommandRequest(
                            session: MediaSession,
                            controller: MediaSession.ControllerInfo,
                            playerCommand: Int,
                        ): Int =
                            if (sessionPlayerCommandAllowed(
                                isEngineController(controller), playerCommand))
                                SessionResult.RESULT_SUCCESS
                            else SessionResult.RESULT_ERROR_PERMISSION_DENIED
                    },
                )
                .apply { sessionActivityIntent?.let { setSessionActivity(it) } }
                .build()
    }

    private fun sessionPlayer(player: Player): SkipRoutingPlayer =
        SkipRoutingPlayer(
            player,
            playback = playback,
            isInternalController = {
                val request = mediaSession?.controllerForCurrentRequest
                request != null && isEngineController(request)
            },
        )

    private fun publishEngineEvent(event: AndroidServicePlaybackEvent) {
        val session = mediaSession ?: return
        progressPublisher.onPlaybackEvent(event)
        val args = event.toAndroidBundle()
        session.connectedControllers.forEach { controller ->
            if (isEngineController(controller))
                session.sendCustomCommand(
                    controller, androidEngineEventCommand(), args)
        }
    }

    private fun abandonAudioFocus() {
        if (!::audioManager.isInitialized) return
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
    }

    private fun isEngineController(
        controller: MediaSession.ControllerInfo
    ): Boolean =
        androidEngineControllerAllowed(
            applicationInfo.uid,
            controller.uid,
            controller.connectionHints.getBoolean(
                RHYTHHAUS_ENGINE_CONTROLLER_HINT))

    /** Returns the session exposed to Media3 controllers. */
    public override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    /** Stops the service after task removal only when playback is inactive. */
    public override fun onTaskRemoved(rootIntent: Intent?): Unit {
        // Intentionally does not call super: when media is actively playing we
        // keep the foreground
        // service alive after the task is swiped away (the canonical media3
        // pattern). Only tear down
        // when nothing is playing. See
        // developer.android.com/media/media3/session/background-playback.
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    /** Releases the Media3 session and player during service teardown. */
    public override fun onDestroy(): Unit {
        if (::progressPublisher.isInitialized) progressPublisher.cancel()
        mediaSession?.release()
        mediaSession = null
        if (::playback.isInitialized) playback.release()
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
        super.onDestroy()
    }
}

/**
 * A [androidx.media3.common.ForwardingPlayer] that always advertises the
 * seek-to-next/previous commands and forwards them to
 * [RhythHausTransportBridge], so hardware and system transport controls can
 * drive queue navigation owned by the shared [PlaybackController]. Internal
 * transport affects both retained voices; native source/gain mutations use the
 * trusted custom-command protocol rather than external player commands.
 */
internal class SkipRoutingPlayer(
    player: Player,
    private val isInternalController: () -> Boolean = { true },
    private val playback: AndroidServicePlayback? = null,
) : androidx.media3.common.ForwardingPlayer(player) {
    private val transportRouter = ServiceTransportRouter()

    override fun getAvailableCommands(): Player.Commands =
        transportAvailableCommands(super.getAvailableCommands())

    override fun isCommandAvailable(command: Int): Boolean =
        transportRouter.isCommandAvailable(
            command, super.isCommandAvailable(command))

    override fun play() {
        transportRouter.play(isInternalController()) {
            if (playback != null) playback.play() else super.play()
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady)
            transportRouter.play(isInternalController()) {
                if (playback != null) playback.play()
                else super.setPlayWhenReady(true)
            }
        else
            transportRouter.pause(isInternalController()) {
                if (playback != null) playback.pause()
                else super.setPlayWhenReady(false)
            }
    }

    override fun pause() {
        transportRouter.pause(isInternalController()) {
            if (playback != null) playback.pause() else super.pause()
        }
    }

    override fun stop() {
        transportRouter.stop(isInternalController()) {
            if (playback != null) playback.stop() else super.stop()
        }
    }

    override fun seekTo(positionMs: Long) {
        transportRouter.seekTo(positionMs, isInternalController()) {
            if (playback != null) playback.seekTo(it) else super.seekTo(it)
        }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        transportRouter.seekTo(
            mediaItemIndex, positionMs, isInternalController()) {
                index,
                position ->
                if (playback != null) playback.seekTo(position)
                else super.seekTo(index, position)
            }
    }

    override fun seekBack() {
        transportRouter.seekTo(
            (currentPosition - seekBackIncrement).coerceAtLeast(0L),
            isInternalController(),
        ) {
            if (playback != null) playback.seekTo(it) else super.seekBack()
        }
    }

    override fun seekForward() {
        transportRouter.seekTo(
            (currentPosition + seekForwardIncrement).coerceAtMost(
                duration.takeIf { it > 0 } ?: Long.MAX_VALUE),
            isInternalController(),
        ) {
            if (playback != null) playback.seekTo(it) else super.seekForward()
        }
    }

    override fun seekToDefaultPosition() {
        transportRouter.seekTo(0L, isInternalController()) {
            if (playback != null) playback.seekTo(0L)
            else super.seekToDefaultPosition()
        }
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        transportRouter.seekTo(mediaItemIndex, 0L, isInternalController()) {
            index,
            _ ->
            if (playback != null) playback.seekTo(0L)
            else super.seekToDefaultPosition(index)
        }
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

    override fun setVolume(volume: Float) {
        if (!isInternalController()) return
        if (playback != null) playback.setPlaybackGain(volume)
        else super.setVolume(volume)
    }

    override fun release() {
        if (playback != null) playback.release() else super.release()
    }
}
