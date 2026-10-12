package com.eterocell.rhythhaus

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

internal fun interface AndroidControllerExecutor {
    fun execute(action: () -> Unit)
}

internal val androidControllerExecutor: AndroidControllerExecutor by lazy {
    runCatching {
        val handler = Handler(Looper.getMainLooper())
        AndroidControllerExecutor { action ->
            if (Looper.myLooper() == handler.looper) action()
            else handler.post(action)
        }
    }
        .getOrElse {
            val hostExecutor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "rhythhaus-android-controller-host").apply {
                    isDaemon = true
                }
            }
            AndroidControllerExecutor { action -> hostExecutor.execute(action) }
        }
}

internal class AndroidControllerOperations(
    private val executor: AndroidControllerExecutor = androidControllerExecutor,
) {
    fun dispatch(action: () -> Unit) = executor.execute(action)
}

internal class AndroidControllerLifecycle(
    private val operations: AndroidControllerOperations,
) {
    private val releaseRequested = AtomicBoolean(false)

    fun dispatchControllerWork(action: () -> Unit) {
        dispatchControllerWork(action, onDiscard = {})
    }

    fun dispatchControllerWork(action: () -> Unit, onDiscard: () -> Unit) {
        if (releaseRequested.get()) {
            onDiscard()
            return
        }
        operations.dispatch {
            if (!releaseRequested.get()) action() else onDiscard()
        }
    }

    fun release(teardown: () -> Unit) {
        if (!releaseRequested.compareAndSet(false, true)) return
        operations.dispatch(teardown)
    }
}

internal const val FULL_ANDROID_PLAYBACK_GAIN: Float = 1f
internal const val RHYTHHAUS_ENGINE_CONTROLLER_HINT: String =
    "com.eterocell.rhythhaus.playback.ENGINE_CONTROLLER"

/**
 * Retains the app-local gain sent before source preparation. The owning
 * [AndroidPlaybackEngine] confines mutation to its controller executor.
 */
internal class AndroidPlaybackGainState {
    var intendedGain: Float = FULL_ANDROID_PLAYBACK_GAIN
        private set

    fun set(gain: Float) {
        require(gain.isFinite() && gain >= 0f && gain <= 1f) {
            "Playback gain must be finite and within 0..1, was $gain"
        }
        intendedGain = gain
    }

    fun reset() {
        intendedGain = FULL_ANDROID_PLAYBACK_GAIN
    }
}

internal fun resetAndroidPlaybackGain(player: Player) {
    // Player.volume is Media3's app playback gain, not device volume.
    player.volume = FULL_ANDROID_PLAYBACK_GAIN
}

private var rhythHausAndroidContext: Context? = null

/** Sets the application context used by Android playback integration. */
public fun setRhythHausAndroidContext(context: Context) {
    rhythHausAndroidContext = context.applicationContext
}

/** Creates the Android platform playback engine. */
public fun createAndroidPlaybackEngine(): PlatformPlaybackEngine =
    AndroidPlaybackEngine()

/**
 * Android playback engine backed by a Media3 [MediaController] connected to
 * [RhythHausPlaybackService]. Hosting the player in a `MediaSessionService` is
 * what lets the OS deliver hardware media-button events (wired cable inline
 * remote, Bluetooth headset) and lock-screen/notification transport controls to
 * the session.
 *
 * The shared [PlaybackController] owns the queue; this engine drives a single
 * media item at a time and relays the controller's transport callbacks.
 * Hardware next/previous arrive through [RhythHausTransportBridge] (routed by
 * the service's ForwardingPlayer) and are forwarded to the shared listener so
 * queue navigation stays in one place.
 */
internal class AndroidPlaybackEngine(
    controllerExecutor: AndroidControllerExecutor = androidControllerExecutor,
    private val sendServiceCommand:
        ((AndroidServiceEngineCommand) -> ListenableFuture<
                AndroidServiceEngineResult>)? =
        null,
    private val mediaItemFactory:
        (PlayableTrack, Media3RequestToken) -> MediaItem =
        ::buildAndroidPlaybackMediaItem,
) : CrossfadePlaybackEngine {
    override var listener: PlaybackEngineListener? = null
        set(value) {
            field = value
            // Route hardware/system skip transport (delivered to the service)
            // into the shared listener.
            RhythHausTransportBridge.onSkipToNext = {
                value?.onSkipToNext(activeGeneration)
            }
            RhythHausTransportBridge.onSkipToPrevious = {
                value?.onSkipToPrevious(activeGeneration)
            }
            RhythHausTransportBridge.onPlay = {
                value?.onPlayRequested(activeGeneration)
            }
            RhythHausTransportBridge.onPause = {
                value?.onPauseRequested(activeGeneration)
            }
            RhythHausTransportBridge.onStop = {
                value?.onStopRequested(activeGeneration)
            }
            RhythHausTransportBridge.onSeek = {
                value?.onSeekRequested(activeGeneration, it)
            }
        }

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerOperations =
        AndroidControllerOperations(controllerExecutor)
    private val controllerLifecycle =
        AndroidControllerLifecycle(controllerOperations)
    private var activeGeneration: Long = 0L
    private val playbackGain = AndroidPlaybackGainState()

    private class Voice(
        val token: Media3RequestToken,
        val fallbackDurationMillis: Long?,
        val preparation: CompletableDeferred<LoadedPlayback>,
        var gain: Float,
    ) {
        var playbackRunId: Long? = null
        var completedRunId: Long? = null
        var durationMillis: Long? = fallbackDurationMillis
        var terminal = false
    }

    private var current: Voice? = null
    private var preparedIncoming: Voice? = null
    private var retiring: Voice? = null
    private var outgoingGainBeforeIncoming: Float? = null
    private var tokenNonce = 0L

    private data class PendingCommand(
        val operation: AndroidServiceEngineCommand,
        val result: (AndroidServiceEngineResult) -> Unit,
    )

    private fun command(
        operation: AndroidServiceEngineCommand,
        result: (AndroidServiceEngineResult) -> Unit = {},
    ) {
        val request = PendingCommand(operation, result)
        val nativeSender = sendServiceCommand
        if (nativeSender != null) {
            send(request, nativeSender)
            return
        }
        val connected = controller
        if (connected != null) {
            send(request) { command ->
                Futures.transform(
                    connected.sendCustomCommand(
                        androidEngineSessionCommand(),
                        command.toAndroidBundle()),
                    { requireNotNull(it).toAndroidEngineResult() },
                    { runnable ->
                        controllerLifecycle.dispatchControllerWork {
                            runnable.run()
                        }
                    },
                )
            }
        } else {
            pendingCommands.addLast(request)
            ensureControllerConnecting()
        }
    }

    private fun send(
        request: PendingCommand,
        sender:
            (AndroidServiceEngineCommand) -> ListenableFuture<
                    AndroidServiceEngineResult>,
    ) {
        val future =
            try {
                sender(request.operation)
            } catch (failure: Exception) {
                request.result(commandFailure(failure))
                return
            }
        future.addListener(
            {
                val response =
                    try {
                        future.get()
                    } catch (failure: Exception) {
                        commandFailure(failure)
                    }
                request.result(response)
            },
            { runnable ->
                controllerLifecycle.dispatchControllerWork { runnable.run() }
            })
    }

    private fun commandFailure(failure: Exception): AndroidServiceEngineResult =
        AndroidServiceEngineResult(
            failure =
                PlaybackError(
                    "Android playback service command failed.",
                    failure.message),
        )

    /**
     * Receives source/run-owned events, never observations of MediaController's
     * mutable player.
     */
    internal fun serviceEvent(event: AndroidServicePlaybackEvent) {
        controllerLifecycle.dispatchControllerWork {
            val voice =
                when (event.token) {
                    current?.token -> current
                    retiring?.token -> retiring
                    else -> null
                } ?: return@dispatchControllerWork
            if (voice.terminal || event.playbackRunId != voice.playbackRunId)
                return@dispatchControllerWork
            if (event.failure != null) {
                failVoice(voice, event.failure)
                return@dispatchControllerWork
            }
            if (current === voice) {
                event.status?.let {
                    listener?.onPlaybackStatus(voice.token.generation, it)
                }
                if (current !== voice || voice.terminal)
                    return@dispatchControllerWork
                event.positionMillis?.let {
                    event.durationMillis?.let { duration ->
                        voice.durationMillis = duration
                    }
                    listener?.onPlaybackProgress(
                        voice.token.generation,
                        it,
                        event.durationMillis ?: voice.durationMillis)
                }
            }
            if (event.completed &&
                (current === voice || retiring === voice) &&
                !voice.terminal) {
                val run = event.playbackRunId ?: return@dispatchControllerWork
                if (voice.playbackRunId == run && voice.completedRunId != run) {
                    voice.completedRunId = run
                    listener?.onPlaybackCompleted(voice.token.generation, run)
                }
            }
        }
    }

    private fun failVoice(voice: Voice, failure: PlaybackError) {
        if ((current !== voice && retiring !== voice) || voice.terminal) return
        voice.terminal = true
        voice.preparation.completeExceptionally(
            PlaybackFailureException(failure))
        // Outgoing can still be controller-current until incoming Playing is
        // admitted. Forward its identity and leave cancel/finish to the
        // controller.
        listener?.onPlaybackError(voice.token.generation, failure)
    }

    private var disposed: Boolean = false

    /**
     * FIFO settlement also ensures connection failure completes every
     * outstanding load.
     */
    private val pendingCommands = ArrayDeque<PendingCommand>()

    private fun ensureControllerConnecting() {
        if (controllerFuture != null) return
        val future =
            try {
                val context =
                    requireNotNull(rhythHausAndroidContext) {
                        "Android playback context is not configured"
                    }
                val token =
                    SessionToken(
                        context,
                        ComponentName(
                            context, RhythHausPlaybackService::class.java))
                val hints =
                    Bundle().apply {
                        putBoolean(RHYTHHAUS_ENGINE_CONTROLLER_HINT, true)
                    }
                MediaController.Builder(context, token)
                    .setConnectionHints(hints)
                    .setListener(
                        object : MediaController.Listener {
                            override fun onCustomCommand(
                                controller: MediaController,
                                command: SessionCommand,
                                args: Bundle
                            ): ListenableFuture<SessionResult> {
                                if (command.customAction ==
                                    RHYTHHAUS_ENGINE_EVENT)
                                    androidServicePlaybackEvent(args)
                                        ?.let(::serviceEvent)
                                return Futures.immediateFuture(
                                    SessionResult(SessionResult.RESULT_SUCCESS))
                            }
                        })
                    .buildAsync()
            } catch (failure: Exception) {
                connectionFailed(failure)
                return
            }
        controllerFuture = future
        future.addListener(
            {
                // The engine may have been released while the connection was in
                // flight.
                if (disposed) {
                    runCatching { future.get() }.getOrNull()?.release()
                    return@addListener
                }
                val connected =
                    try {
                        future.get()
                    } catch (failure: Exception) {
                        connectionFailed(failure)
                        return@addListener
                    }
                controller = connected
                while (pendingCommands.isNotEmpty()) {
                    val request = pendingCommands.removeFirst()
                    command(request.operation, request.result)
                }
            },
            // Run the completion callback on the main thread (controller is
            // single-threaded).
            { runnable ->
                controllerLifecycle.dispatchControllerWork { runnable.run() }
            },
        )
    }

    private fun connectionFailed(failure: Exception) {
        controllerFuture = null
        val response =
            AndroidServiceEngineResult(
                failure =
                    PlaybackError(
                        "Android could not start the playback service.",
                        failure.message))
        // Detach before settlement: a callback may enqueue a new connection
        // attempt.
        val failed = pendingCommands.toList()
        pendingCommands.clear()
        failed.forEach { it.result(response) }
    }

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val result = CompletableDeferred<LoadedPlayback>()
        controllerLifecycle.dispatchControllerWork(
            action = {
                if (result.isCancelled) return@dispatchControllerWork
                invalidateVoices("Superseded Android playback load")
                val voice =
                    Voice(
                        Media3RequestToken(generation, ++tokenNonce),
                        track.durationMillis,
                        result,
                        playbackGain.intendedGain)
                current = voice
                activeGeneration = generation
                listener?.onPlaybackStatus(generation, PlaybackStatus.Loading)
                val item =
                    try {
                        mediaItemFactory(track, voice.token)
                    } catch (failure: Exception) {
                        settlePreparation(voice, commandFailure(failure))
                        return@dispatchControllerWork
                    }
                command(
                    AndroidServiceEngineCommand.LoadPaused(item, voice.gain)) {
                        response ->
                        if (current !== voice) return@command
                        settlePreparation(voice, response)
                        if (response.failure == null &&
                            !voice.terminal &&
                            voice.playbackRunId == null) {
                            listener?.onPlaybackStatus(
                                generation, PlaybackStatus.Paused)
                        }
                    }
            },
            onDiscard = {
                result.cancel(
                    CancellationException("Android playback engine released"))
            })
        return awaitPreparation(result, incoming = false)
    }

    private fun settlePreparation(
        voice: Voice,
        response: AndroidServiceEngineResult
    ) {
        if (response.failure != null) {
            voice.terminal = true
            voice.preparation.completeExceptionally(
                PlaybackFailureException(response.failure))
        } else {
            voice.durationMillis =
                response.durationMillis ?: voice.fallbackDurationMillis
            voice.preparation.complete(
                LoadedPlayback(
                    voice.token.generation,
                    response.durationMillis ?: voice.fallbackDurationMillis))
        }
    }

    override suspend fun prepareIncoming(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val result = CompletableDeferred<LoadedPlayback>()
        controllerLifecycle.dispatchControllerWork(
            action = {
                if (result.isCancelled) return@dispatchControllerWork
                if (retiring != null || current == null) {
                    result.completeExceptionally(
                        IllegalStateException(
                            "Android outgoing playback cannot stage an incoming voice"))
                    return@dispatchControllerWork
                }
                cancelIncoming()
                val outgoing = requireNotNull(current)
                val voice =
                    Voice(
                        Media3RequestToken(generation, ++tokenNonce),
                        track.durationMillis,
                        result,
                        0f)
                preparedIncoming = voice
                outgoingGainBeforeIncoming = outgoing.gain
                val item =
                    try {
                        mediaItemFactory(track, voice.token)
                    } catch (failure: Exception) {
                        settlePreparation(voice, commandFailure(failure))
                        return@dispatchControllerWork
                    }
                command(
                    AndroidServiceEngineCommand.PrepareIncoming(
                        item, outgoing.token)) { response ->
                        if (preparedIncoming === voice)
                            settlePreparation(voice, response)
                    }
            },
            onDiscard = {
                result.cancel(
                    CancellationException("Android playback engine released"))
            })
        return awaitPreparation(result, incoming = true)
    }

    private suspend fun awaitPreparation(
        result: CompletableDeferred<LoadedPlayback>,
        incoming: Boolean
    ): LoadedPlayback =
        try {
            result.await()
        } catch (failure: Throwable) {
            result.cancel(
                CancellationException(
                    "Android playback preparation cancelled", failure))
            controllerLifecycle.dispatchControllerWork {
                if (incoming) {
                    val voice =
                        preparedIncoming ?: current?.takeIf { retiring != null }
                    if (voice?.preparation === result) cancelIncoming()
                } else {
                    val voice = current
                    if (voice != null &&
                        voice.preparation === result &&
                        failure is CancellationException) {
                        current = null
                        command(
                            AndroidServiceEngineCommand.CancelLoad(voice.token))
                    }
                }
            }
            throw failure
        }

    override fun startIncoming(playbackRunId: Long) {
        startIncomingInternal(playbackRunId, gapless = false)
    }

    override fun startIncomingGapless(playbackRunId: Long) {
        startIncomingInternal(playbackRunId, gapless = true)
    }

    private fun startIncomingInternal(playbackRunId: Long, gapless: Boolean) {
        controllerLifecycle.dispatchControllerWork {
            val incoming =
                requireNotNull(preparedIncoming) {
                    "No Android incoming voice is prepared"
                }
            check(incoming.preparation.isCompleted && !incoming.terminal) {
                "Android incoming voice is not ready"
            }
            incoming.playbackRunId = playbackRunId
            retiring = current
            preparedIncoming = null
            current = incoming
            activeGeneration = incoming.token.generation
            playbackGain.set(incoming.gain)
            command(
                AndroidServiceEngineCommand.StartIncoming(
                    playbackRunId, incoming.token, gapless)) { response ->
                    if (incoming.playbackRunId == playbackRunId)
                        response.failure?.let { failVoice(incoming, it) }
                }
        }
    }

    override fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        require(
            outgoingGain.isFinite() &&
                outgoingGain in 0f..1f &&
                incomingGain.isFinite() &&
                incomingGain in 0f..1f)
        controllerLifecycle.dispatchControllerWork {
            val incoming =
                preparedIncoming ?: current ?: return@dispatchControllerWork
            val outgoing = if (preparedIncoming != null) current else retiring
            outgoing?.gain = outgoingGain
            incoming.gain = incomingGain
            if (preparedIncoming == null) playbackGain.set(incomingGain)
            command(
                AndroidServiceEngineCommand.CrossfadeGains(
                    outgoingGain, incomingGain, incoming.token)) { response ->
                    response.failure?.let { failVoice(incoming, it) }
                }
        }
    }

    override fun finishCrossfade() {
        controllerLifecycle.dispatchControllerWork {
            val outgoing = retiring ?: return@dispatchControllerWork
            retiring = null
            outgoingGainBeforeIncoming = null
            command(AndroidServiceEngineCommand.RetireOutgoing(outgoing.token))
        }
    }

    override fun finishGapless() {
        finishCrossfade()
    }

    override fun cancelPreparedIncoming() {
        controllerLifecycle.dispatchControllerWork(::cancelIncoming)
    }

    private fun cancelIncoming() {
        val incoming =
            preparedIncoming ?: current?.takeIf { retiring != null } ?: return
        val outgoing = retiring ?: current
        preparedIncoming = null
        retiring = null
        if (outgoing != null) {
            outgoingGainBeforeIncoming?.let { outgoing.gain = it }
            current = outgoing
            activeGeneration = outgoing.token.generation
            playbackGain.set(outgoing.gain)
        }
        outgoingGainBeforeIncoming = null
        incoming.preparation.cancel(
            CancellationException("Android incoming playback cancelled"))
        command(AndroidServiceEngineCommand.CancelIncoming(incoming.token))
    }

    private fun invalidateVoices(reason: String) {
        val cause = CancellationException(reason)
        current?.preparation?.cancel(cause)
        preparedIncoming?.preparation?.cancel(cause)
        retiring?.preparation?.cancel(cause)
        current = null
        preparedIncoming = null
        retiring = null
        outgoingGainBeforeIncoming = null
    }

    override fun clear(generation: Long) {
        controllerLifecycle.dispatchControllerWork {
            invalidateVoices("Android playback cleared")
            activeGeneration = generation
            playbackGain.reset()
            command(AndroidServiceEngineCommand.Clear)
        }
    }

    override fun setUserTransportEnabled(enabled: Boolean) {
        RhythHausTransportBridge.setTransportEnabled(enabled)
    }

    override fun setPlaybackGain(gain: Float) {
        require(gain.isFinite() && gain in 0f..1f) {
            "Playback gain must be finite and within 0..1, was $gain"
        }
        controllerLifecycle.dispatchControllerWork {
            playbackGain.set(gain)
            val voice = current ?: return@dispatchControllerWork
            voice.gain = gain
            command(AndroidServiceEngineCommand.Gain(gain, voice.token)) {
                response ->
                response.failure?.let { failVoice(voice, it) }
            }
        }
    }

    override fun play(playbackRunId: Long) {
        controllerLifecycle.dispatchControllerWork {
            val voice = current ?: return@dispatchControllerWork
            voice.playbackRunId = playbackRunId
            command(
                AndroidServiceEngineCommand.Play(playbackRunId, voice.token)) {
                    response ->
                    if (voice.playbackRunId == playbackRunId)
                        response.failure?.let { failVoice(voice, it) }
                }
        }
    }

    override fun pause() {
        controllerLifecycle.dispatchControllerWork {
            val voice = current
            command(AndroidServiceEngineCommand.Pause) { response ->
                if (voice != null)
                    response.failure?.let { failVoice(voice, it) }
            }
        }
    }

    override fun stop() {
        controllerLifecycle.dispatchControllerWork {
            preparedIncoming
                ?.preparation
                ?.cancel(CancellationException("Android playback stopped"))
            retiring = null
            preparedIncoming = null
            outgoingGainBeforeIncoming = null
            current?.playbackRunId = null
            playbackGain.reset()
            current?.gain = FULL_ANDROID_PLAYBACK_GAIN
            command(AndroidServiceEngineCommand.Stop)
        }
    }

    override fun seekTo(positionMillis: Long) {
        controllerLifecycle.dispatchControllerWork {
            val voice = current ?: return@dispatchControllerWork
            preparedIncoming
                ?.preparation
                ?.cancel(
                    CancellationException(
                        "Android incoming playback cancelled by seek"))
            preparedIncoming = null
            retiring = null
            outgoingGainBeforeIncoming = null
            if (voice.durationMillis?.let { positionMillis >= it } == true)
                voice.playbackRunId = null
            command(
                AndroidServiceEngineCommand.Seek(
                    positionMillis, voice.token)) { response ->
                    response.failure?.let { failVoice(voice, it) }
                }
        }
    }

    override fun release() {
        RhythHausTransportBridge.onSkipToNext = null
        RhythHausTransportBridge.onSkipToPrevious = null
        RhythHausTransportBridge.onPlay = null
        RhythHausTransportBridge.onPause = null
        RhythHausTransportBridge.onStop = null
        RhythHausTransportBridge.onSeek = null
        controllerLifecycle.release {
            if (disposed) return@release
            disposed = true
            invalidateVoices("Android playback engine released")
            playbackGain.reset()
            pendingCommands.clear()
            val connected = controller
            if (sendServiceCommand != null) {
                sendServiceCommand.invoke(AndroidServiceEngineCommand.Release)
            } else if (connected != null) {
                val result =
                    connected.sendCustomCommand(
                        androidEngineSessionCommand(),
                        AndroidServiceEngineCommand.Release.toAndroidBundle())
                result.addListener(
                    { connected.release() },
                    { runnable ->
                        controllerOperations.dispatch { runnable.run() }
                    })
            } else {
                controllerFuture?.let(MediaController::releaseFuture)
            }
            controller = null
            controllerFuture = null
        }
    }
}

/**
 * Maps a Media3 [PlaybackException] into the structured playback contract.
 *
 * Classification uses only documented Media3 error codes. When the load
 * references a concrete local file that no longer exists, that direct absence
 * evidence takes precedence so an opaque IO code cannot hide a missing file.
 * Service-connection and other unrelated failures fall through to
 * [PlaybackFailureKind.Unknown].
 */
internal fun androidPlaybackError(
    error: PlaybackException,
    sourceFile: java.io.File? = null,
): PlaybackError =
    androidPlaybackError(error.errorCode, error.message, sourceFile)

/**
 * Pure classification entry point used by the engine-facing overload and by
 * host tests (constructing a Media3 [PlaybackException] requires the Android
 * framework clock).
 */
internal fun androidPlaybackError(
    errorCode: Int,
    message: String?,
    sourceFile: java.io.File? = null,
): PlaybackError {
    val kind =
        if (sourceFile != null && !sourceFile.exists()) {
            PlaybackFailureKind.MissingFile
        } else {
            androidFailureKind(errorCode)
        }
    return PlaybackError(
        message = "Android could not play this audio file.",
        cause = message ?: PlaybackException.getErrorCodeName(errorCode),
        kind = kind,
    )
}

/** Maps a documented Media3 error code to a playback failure kind. */
internal fun androidFailureKind(errorCode: Int): PlaybackFailureKind =
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            PlaybackFailureKind.MissingFile
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
            PlaybackFailureKind.AccessLost
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
            PlaybackFailureKind.UnsupportedFormat
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED ->
            PlaybackFailureKind.DecoderFailure
        else -> PlaybackFailureKind.Unknown
    }

internal data class Media3RequestToken(val generation: Long, val nonce: Long) {
    fun encode(): String = "$generation:$nonce"

    companion object {
        fun decode(value: String): Media3RequestToken? {
            val separator = value.indexOf(':')
            if (separator <= 0 || separator == value.lastIndex) return null
            return Media3RequestToken(
                generation =
                    value.substring(0, separator).toLongOrNull() ?: return null,
                nonce =
                    value.substring(separator + 1).toLongOrNull()
                        ?: return null,
            )
        }
    }
}

internal fun buildAndroidPlaybackMediaItem(
    track: PlayableTrack,
    requestToken: Media3RequestToken
): MediaItem =
    MediaItem.Builder()
        .setMediaId(requestToken.encode())
        .setUri(track.source.androidUri())
        .setMediaMetadata(buildAndroidPlaybackMediaMetadata(track))
        .build()

internal fun buildAndroidPlaybackMediaMetadata(
    track: PlayableTrack
): MediaMetadata {
    val builder =
        MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
    track.artworkBytes?.let { artwork ->
        builder.setArtworkData(
            artwork,
            MediaMetadata.PICTURE_TYPE_FRONT_COVER,
        )
    }
    return builder.build()
}

private fun AudioSource.androidUri(): Uri =
    when (this) {
        is AudioSource.FilePath -> Uri.fromFile(java.io.File(path))
        is AudioSource.Uri -> Uri.parse(value)
        is AudioSource.FileDescriptor ->
            error(
                "File descriptor audio sources are metadata-only and cannot be played")
    }
