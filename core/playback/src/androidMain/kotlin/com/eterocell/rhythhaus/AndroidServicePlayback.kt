package com.eterocell.rhythhaus

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

/**
 * Engine-only operations admitted by the existing same-UID, engine-hint
 * connection.
 */
internal sealed interface AndroidServiceEngineCommand {
    data class LoadPaused(
        val item: MediaItem,
        val gain: Float? = null,
    ) : AndroidServiceEngineCommand

    data class PrepareIncoming(
        val item: MediaItem,
        val outgoingToken: Media3RequestToken? = null,
    ) : AndroidServiceEngineCommand

    data class Play(
        val playbackRunId: Long,
        val token: Media3RequestToken? = null
    ) : AndroidServiceEngineCommand

    data class StartIncoming(
        val playbackRunId: Long,
        val token: Media3RequestToken? = null,
        val gapless: Boolean = false,
    ) : AndroidServiceEngineCommand

    data class Gain(val gain: Float, val token: Media3RequestToken? = null) :
        AndroidServiceEngineCommand

    data class CrossfadeGains(
        val outgoing: Float,
        val incoming: Float,
        val token: Media3RequestToken? = null,
    ) : AndroidServiceEngineCommand

    data class Seek(
        val positionMillis: Long,
        val token: Media3RequestToken? = null
    ) : AndroidServiceEngineCommand

    data class CancelLoad(val token: Media3RequestToken) :
        AndroidServiceEngineCommand

    data class CancelIncoming(val token: Media3RequestToken) :
        AndroidServiceEngineCommand

    data class RetireOutgoing(val token: Media3RequestToken) :
        AndroidServiceEngineCommand

    data object Pause : AndroidServiceEngineCommand

    data object Stop : AndroidServiceEngineCommand

    data object Clear : AndroidServiceEngineCommand

    /**
     * Detaches an engine's voices; the service's single session remains
     * reusable.
     */
    data object Release : AndroidServiceEngineCommand
}

internal data class AndroidServiceEngineResult(
    val durationMillis: Long? = null,
    val failure: PlaybackError? = null,
    val permissionDenied: Boolean = false,
) {
    companion object {
        val Success = AndroidServiceEngineResult()
        val PermissionDenied =
            AndroidServiceEngineResult(permissionDenied = true)
    }
}

/**
 * Provenance is captured at the native source/run, not read from a mutable
 * session player.
 */
internal data class AndroidServicePlaybackEvent(
    val token: Media3RequestToken,
    val playbackRunId: Long? = null,
    val status: PlaybackStatus? = null,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null,
    val completed: Boolean = false,
    val failure: PlaybackError? = null,
)

internal fun androidEngineControllerAllowed(
    serviceUid: Int,
    controllerUid: Int,
    engineHint: Boolean,
): Boolean = controllerUid == serviceUid && engineHint

/**
 * The service callback and host tests use the same admission/dispatch boundary.
 */
internal class AndroidServiceCommandRouter(
    private val playback: AndroidServicePlayback,
    private val serviceUid: Int,
) {
    fun dispatch(
        controllerUid: Int,
        engineHint: Boolean,
        command: AndroidServiceEngineCommand,
    ): ListenableFuture<AndroidServiceEngineResult> {
        if (!androidEngineControllerAllowed(
            serviceUid, controllerUid, engineHint)) {
            return Futures.immediateFuture(
                AndroidServiceEngineResult.PermissionDenied)
        }
        return try {
            playback.execute(command)
        } catch (failure: Exception) {
            Futures.immediateFuture(
                AndroidServiceEngineResult(
                    failure =
                        PlaybackError(
                            "Android could not play this audio file.",
                            failure.message),
                ),
            )
        }
    }
}

/**
 * Two native voices underneath one service session. Every method and native
 * callback runs on the service player's application looper. The service owns
 * focus/noisy handling once; neither voice independently acquires audio focus.
 */
internal class AndroidServicePlayback(
    private val createPlayer: () -> Player,
    private val onCurrentPlayerChanged: (Player) -> Unit,
    private val onEvent: (AndroidServicePlaybackEvent) -> Unit,
    private val requestFocus: () -> Boolean = { true },
    private val abandonFocus: () -> Unit = {},
) {
    private class Voice(
        val player: Player,
        val item: MediaItem?,
        val token: Media3RequestToken?,
        var gain: Float,
        var preparation: SettableFuture<AndroidServiceEngineResult>? = null,
    ) {
        var callbacks: Player.Listener? = null
        var ready = false
        var prepared = false
        var disposed = false
        var terminal = false
        var playbackRunId: Long? = null
        var completedRunId: Long? = null
    }

    private var current: Voice? = null
    private var preparedIncoming: Voice? = null
    private var retiring: Voice? = null
    private var outgoingGainBeforeIncoming: Float? = null
    private var intendedGain = FULL_ANDROID_PLAYBACK_GAIN
    private var released = false
    private var pausing = false

    fun initialize(): Player {
        check(!released)
        return current?.player ?: replaceCurrent(null).player
    }

    fun execute(
        command: AndroidServiceEngineCommand
    ): ListenableFuture<AndroidServiceEngineResult> {
        check(!released) { "Android service playback has been released" }
        when (command) {
            is AndroidServiceEngineCommand.LoadPaused -> {
                command.gain?.let(::requireGain)
                command.gain?.let { intendedGain = it }
                return loadPaused(command.item)
            }
            is AndroidServiceEngineCommand.PrepareIncoming -> {
                checkCurrentToken(command.outgoingToken)
                return prepareIncoming(command.item)
            }
            is AndroidServiceEngineCommand.Play -> {
                checkCurrentToken(command.token)
                play(command.playbackRunId)
            }
            is AndroidServiceEngineCommand.StartIncoming ->
                startIncoming(
                    command.playbackRunId, command.token, command.gapless)
            is AndroidServiceEngineCommand.Gain -> {
                checkCurrentToken(command.token)
                setPlaybackGain(command.gain)
            }
            is AndroidServiceEngineCommand.CrossfadeGains -> {
                check(
                    command.token == null ||
                        (preparedIncoming ?: current)?.token == command.token) {
                        "Android crossfade command no longer owns the incoming voice"
                    }
                setCrossfadeGains(command.outgoing, command.incoming)
            }
            is AndroidServiceEngineCommand.Seek -> {
                checkCurrentToken(command.token)
                seekTo(command.positionMillis)
            }
            is AndroidServiceEngineCommand.CancelLoad ->
                if (current?.token == command.token) clear()
            is AndroidServiceEngineCommand.CancelIncoming ->
                cancelPreparedIncoming(command.token)
            is AndroidServiceEngineCommand.RetireOutgoing ->
                finishCrossfade(command.token)
            AndroidServiceEngineCommand.Pause -> pause()
            AndroidServiceEngineCommand.Stop -> stop()
            AndroidServiceEngineCommand.Clear -> clear()
            AndroidServiceEngineCommand.Release -> clear()
        }
        return Futures.immediateFuture(AndroidServiceEngineResult.Success)
    }

    private fun checkCurrentToken(token: Media3RequestToken?) {
        check(token == null || current?.token == token) {
            "Android playback command no longer owns the current voice"
        }
    }

    private fun loadPaused(
        item: MediaItem
    ): ListenableFuture<AndroidServiceEngineResult> {
        disposeOverlap()
        dispose(current)
        abandonFocus()
        val result = SettableFuture.create<AndroidServiceEngineResult>()
        val voice = replaceCurrent(item, result)
        prepare(voice)
        return result
    }

    private fun replaceCurrent(
        item: MediaItem?,
        result: SettableFuture<AndroidServiceEngineResult>? = null,
    ): Voice {
        val voice = newVoice(item, intendedGain, result)
        current = voice
        onCurrentPlayerChanged(voice.player)
        return voice
    }

    private fun newVoice(
        item: MediaItem?,
        gain: Float,
        result: SettableFuture<AndroidServiceEngineResult>? = null,
    ): Voice {
        val token = item?.let {
            requireNotNull(Media3RequestToken.decode(it.mediaId))
        }
        val voice = Voice(createPlayer(), item, token, gain, result)
        // Set gain before any source preparation or playback, including
        // reconnect/resume.
        voice.player.volume = gain
        if (item != null) voice.player.setMediaItem(item)
        installCallbacks(voice)
        return voice
    }

    private fun prepare(voice: Voice) {
        try {
            voice.prepared = true
            voice.player.prepare()
        } catch (failure: Exception) {
            preparationFailed(
                voice,
                PlaybackError(
                    "Android could not play this audio file.", failure.message))
        }
    }

    private fun prepareIncoming(
        item: MediaItem
    ): ListenableFuture<AndroidServiceEngineResult> {
        check(retiring == null) {
            "A retiring Android voice still owns the overlap"
        }
        check(current?.item != null) { "There is no outgoing Android voice" }
        cancelPreparedIncoming()
        outgoingGainBeforeIncoming = current?.gain
        val result = SettableFuture.create<AndroidServiceEngineResult>()
        val incoming = newVoice(item, gain = 0f, result)
        preparedIncoming = incoming
        prepare(incoming)
        return result
    }

    private fun startIncoming(
        playbackRunId: Long,
        token: Media3RequestToken?,
        gapless: Boolean = false,
    ) {
        val incoming =
            requireNotNull(preparedIncoming) {
                "No Android incoming voice is prepared"
            }
        check(token == null || incoming.token == token) {
            "Android incoming voice was replaced"
        }
        check(incoming.ready && !incoming.terminal) {
            "Android incoming voice is not ready"
        }
        check(requestFocus()) { "Android audio focus was not granted" }
        // The source callback installed below captures this run before native
        // play.
        incoming.playbackRunId = playbackRunId
        installCallbacks(incoming)
        retiring = current
        preparedIncoming = null
        current = incoming
        intendedGain = incoming.gain
        onCurrentPlayerChanged(incoming.player)
        incoming.player.volume = incoming.gain
        try {
            incoming.player.play()
            if (gapless) finishCrossfade(retiring?.token)
        } catch (failure: Exception) {
            incoming.terminal = true
            incoming.player.pause()
            throw failure
        }
    }

    fun play(playbackRunId: Long? = null) {
        val voice = current ?: return
        if (voice.item == null || voice.terminal) return
        check(requestFocus()) { "Android audio focus was not granted" }
        if (playbackRunId != null) {
            voice.playbackRunId = playbackRunId
            installCallbacks(voice)
        }
        // A resume never overwrites either retained crossfade gain with unity.
        retiring?.let {
            it.player.volume = it.gain
            if (!it.terminal && it.completedRunId != it.playbackRunId)
                it.player.play()
        }
        voice.player.volume = voice.gain
        if (!voice.prepared) prepare(voice)
        if (!voice.terminal) voice.player.play()
    }

    fun pause() {
        if (pausing || released) return
        pausing = true
        try {
            current?.player?.pause()
            retiring?.player?.pause()
            abandonFocus()
            current
                ?.takeUnless { it.terminal }
                ?.let { emitStatus(it, PlaybackStatus.Paused) }
        } finally {
            pausing = false
        }
    }

    fun pauseForSystem() = pause()

    fun isPlaying(): Boolean =
        current?.player?.isPlaying == true && current?.terminal == false

    fun currentItemIdentity(): String? =
        current?.item?.localConfiguration?.uri?.toString()

    /**
     * Only the native logical current source/run may change its progress
     * schedule.
     */
    fun ownsCurrentEvent(event: AndroidServicePlaybackEvent): Boolean {
        val voice = current ?: return false
        return !voice.disposed &&
            voice.token == event.token &&
            voice.playbackRunId == event.playbackRunId
    }

    /**
     * Progress is sourced by the service, with the source/run captured before
     * publication.
     */
    fun publishProgress(): Boolean {
        val voice = current ?: return false
        val run = voice.playbackRunId ?: return false
        if (voice.disposed || voice.terminal || !voice.player.isPlaying)
            return false
        emitProgress(voice, run)
        return current === voice &&
            !voice.disposed &&
            !voice.terminal &&
            voice.playbackRunId == run &&
            voice.player.isPlaying
    }

    fun seekTo(positionMillis: Long) {
        finishCrossfade()
        cancelPreparedIncoming()
        val voice = current ?: return
        val duration = voice.player.duration.takeIf { it > 0L }
        if (duration != null && positionMillis >= duration) {
            voice.playbackRunId = null
            installCallbacks(voice)
        }
        voice.player.seekTo(positionMillis)
        emitProgress(voice)
    }

    fun stop() {
        val item = current?.item
        val token = current?.token
        disposeOverlap()
        dispose(current)
        intendedGain = FULL_ANDROID_PLAYBACK_GAIN
        abandonFocus()
        // Dispose decoder/audio resources, retaining only the selected source
        // for ordinary replay.
        val idle = replaceCurrent(item)
        if (token != null) {
            onEvent(
                AndroidServicePlaybackEvent(
                    token,
                    status = PlaybackStatus.Stopped,
                    positionMillis = 0L))
        }
        idle.playbackRunId = null
    }

    fun clear() {
        disposeOverlap()
        dispose(current)
        intendedGain = FULL_ANDROID_PLAYBACK_GAIN
        abandonFocus()
        replaceCurrent(null)
    }

    fun setPlaybackGain(gain: Float) {
        requireGain(gain)
        intendedGain = gain
        current?.let {
            it.gain = gain
            it.player.volume = gain
        }
    }

    private fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        requireGain(outgoingGain)
        requireGain(incomingGain)
        val incoming = preparedIncoming ?: current
        val outgoing = if (preparedIncoming != null) current else retiring
        outgoing?.let {
            it.gain = outgoingGain
            it.player.volume = outgoingGain
        }
        incoming?.let {
            it.gain = incomingGain
            it.player.volume = incomingGain
        }
        if (preparedIncoming == null) intendedGain = incomingGain
    }

    fun finishCrossfade(token: Media3RequestToken? = null) {
        val outgoing = retiring ?: return
        if (token != null && outgoing.token != token) return
        retiring = null
        outgoingGainBeforeIncoming = null
        dispose(outgoing)
    }

    fun cancelPreparedIncoming(token: Media3RequestToken? = null) {
        val incoming =
            preparedIncoming ?: current?.takeIf { retiring != null } ?: return
        if (token != null && incoming.token != token) return
        val outgoing = retiring ?: current
        preparedIncoming = null
        retiring = null
        if (outgoing != null) {
            outgoingGainBeforeIncoming?.let { outgoing.gain = it }
            intendedGain = outgoing.gain
            outgoing.player.volume = outgoing.gain
            current = outgoing
            if (incoming !== outgoing) {
                onCurrentPlayerChanged(outgoing.player)
                if (outgoing.terminal || !outgoing.player.playWhenReady)
                    abandonFocus()
            }
        }
        outgoingGainBeforeIncoming = null
        dispose(incoming)
    }

    fun release() {
        if (released) return
        released = true
        disposeOverlap()
        dispose(current)
        current = null
        intendedGain = FULL_ANDROID_PLAYBACK_GAIN
        abandonFocus()
    }

    private fun disposeOverlap() {
        val staged = preparedIncoming
        val outgoing = retiring
        preparedIncoming = null
        retiring = null
        outgoingGainBeforeIncoming = null
        dispose(staged)
        dispose(outgoing)
    }

    private fun dispose(voice: Voice?) {
        if (voice == null || voice.disposed) return
        voice.disposed = true
        voice.callbacks?.let(voice.player::removeListener)
        voice.preparation?.set(
            AndroidServiceEngineResult(
                failure =
                    PlaybackError("Android audio preparation was cancelled.")))
        voice.preparation = null
        // This is per-player app attenuation, never device/system volume.
        resetAndroidPlaybackGain(voice.player)
        voice.player.release()
    }

    private fun preparationFailed(voice: Voice, failure: PlaybackError) {
        voice.terminal = true
        voice.preparation?.set(AndroidServiceEngineResult(failure = failure))
        voice.preparation = null
        if (preparedIncoming === voice) {
            preparedIncoming = null
            outgoingGainBeforeIncoming = null
            dispose(voice)
        } else if (current === voice) {
            voice.player.pause()
            voice.token?.let {
                onEvent(AndroidServicePlaybackEvent(it, failure = failure))
            }
            if (retiring == null && current === voice) abandonFocus()
        }
    }

    private fun installCallbacks(voice: Voice) {
        voice.callbacks?.let(voice.player::removeListener)
        val token = voice.token
        val run = voice.playbackRunId
        val callbacks =
            object : Player.Listener {
                private fun ownsSource(): Boolean =
                    !voice.disposed &&
                        (current === voice ||
                            preparedIncoming === voice ||
                            retiring === voice)

                private fun ownsRun(): Boolean =
                    ownsSource() && voice.playbackRunId == run

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!ownsRun() || voice.terminal) return
                    when (playbackState) {
                        Player.STATE_READY -> {
                            voice.ready = true
                            val duration =
                                voice.player.duration.takeIf { it > 0L }
                            voice.preparation?.set(
                                AndroidServiceEngineResult(
                                    durationMillis = duration))
                            voice.preparation = null
                            if (current === voice && run == null)
                                emitProgress(voice, run)
                        }
                        Player.STATE_BUFFERING ->
                            if (current === voice && run != null)
                                emitStatus(voice, PlaybackStatus.Buffering, run)
                        Player.STATE_ENDED ->
                            if (ownsRun() &&
                                run != null &&
                                voice.completedRunId != run) {
                                voice.completedRunId = run
                                if (token != null)
                                    onEvent(
                                        AndroidServicePlaybackEvent(
                                            token, run, completed = true))
                            }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!ownsRun() ||
                        voice.terminal ||
                        current !== voice ||
                        run == null)
                        return
                    emitStatus(
                        voice,
                        if (isPlaying) PlaybackStatus.Playing
                        else PlaybackStatus.Paused,
                        run)
                    if (ownsRun() && current === voice) emitProgress(voice, run)
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int
                ) {
                    if (!ownsRun()) return
                    if (!playWhenReady &&
                        (reason ==
                            Player
                                .PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS ||
                            reason ==
                                Player
                                    .PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY))
                        pauseForSystem()
                }

                override fun onPlaybackSuppressionReasonChanged(
                    playbackSuppressionReason: Int
                ) {
                    if (ownsRun() &&
                        playbackSuppressionReason !=
                            Player.PLAYBACK_SUPPRESSION_REASON_NONE)
                        pauseForSystem()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (!ownsRun() || voice.terminal) return
                    val localFile =
                        voice.item?.localConfiguration?.uri?.let { uri ->
                            if (uri.scheme == "file")
                                uri.path?.let { java.io.File(it) }
                            else null
                        }
                    val failure = androidPlaybackError(error, localFile)
                    if (preparedIncoming === voice || run == null) {
                        preparationFailed(voice, failure)
                    } else {
                        voice.terminal = true
                        voice.player.pause()
                        if ((current === voice || retiring === voice) &&
                            token != null) {
                            onEvent(
                                AndroidServicePlaybackEvent(
                                    token, run, failure = failure))
                            // A native failure does not decide whether
                            // promotion won the
                            // controller race. Retain outgoing for
                            // cancel/finish authority.
                            if (retiring == null && current === voice)
                                abandonFocus()
                        }
                    }
                }
            }
        voice.callbacks = callbacks
        voice.player.addListener(callbacks)
    }

    private fun emitStatus(
        voice: Voice,
        status: PlaybackStatus,
        run: Long? = voice.playbackRunId
    ) {
        voice.token?.let {
            onEvent(AndroidServicePlaybackEvent(it, run, status = status))
        }
    }

    private fun emitProgress(voice: Voice, run: Long? = voice.playbackRunId) {
        voice.token?.let {
            onEvent(
                AndroidServicePlaybackEvent(
                    it,
                    run,
                    positionMillis =
                        voice.player.currentPosition.coerceAtLeast(0L),
                    durationMillis =
                        voice.player.duration.takeIf { duration ->
                            duration > 0L
                        },
                ))
        }
    }

    private fun requireGain(gain: Float) {
        require(gain.isFinite() && gain in 0f..1f) {
            "Playback gain must be finite and within 0..1, was $gain"
        }
    }
}
