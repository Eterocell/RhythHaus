package com.eterocell.rhythhaus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

/** Creates the iOS platform playback engine. */
public fun createIOSPlaybackEngine(
    resolver: IOSRelativeFilePathResolver,
): PlatformPlaybackEngine = IOSPlaybackEngine(resolver)

internal enum class IOSTrackSwitchTeardown {
    SoftFade,
}

internal val iosTrackSwitchTeardown: IOSTrackSwitchTeardown =
    IOSTrackSwitchTeardown.SoftFade
internal const val IOS_TRACK_SWITCH_FADE_SECONDS: Double = 0.05
internal const val IOS_TRACK_SWITCH_SILENT_VOLUME: Float = 0.0f

@OptIn(ExperimentalForeignApi::class)
public class IOSPlaybackEngine(
    private val relativeFilePathResolver: IOSRelativeFilePathResolver,
) : CrossfadePlaybackEngine, PlaybackOutputStatusProvider {
    private val outputState =
        kotlinx.coroutines.flow.MutableStateFlow<PlaybackOutputStatus>(
            PlaybackOutputStatus.Unavailable)
    override val outputStatus:
        kotlinx.coroutines.flow.StateFlow<PlaybackOutputStatus> =
        outputState
    private var confinedListener: PlaybackEngineListener? = null
    override var listener: PlaybackEngineListener?
        get() = withIOSPlaybackMainThread { confinedListener }
        set(value) = withIOSPlaybackMainThread { confinedListener = value }

    private var audioProvider: IOSAudioPlayerProvider? = null
    private var loadedTrack: PlayableTrack? = null
    private var durationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var activeRun: VoiceRun? = null
    private var retiringRun: VoiceRun? = null
    private var outgoingSnapshot: OutgoingSnapshot? = null
    private var preparedIncoming: PendingIncoming? = null
    private var playbackActive: Boolean = false
    private var playbackGain: Float = 1.0f
    private var wasPlayingBeforeInterruption: Boolean = false
    private var automaticResumeAllowed: Boolean = true
    private var activeGeneration: Long = 0L

    override fun refreshOutputStatus() {
        withIOSPlaybackMainThread {
            val names =
                audioProvider?.currentOutputNames()?.names.orEmpty().filter {
                    it.isNotBlank()
                }
            outputState.value =
                if (names.isEmpty()) PlaybackOutputStatus.Unavailable
                else PlaybackOutputStatus.Available(names)
        }
    }

    private var sourceVersion: Long = 0L
    private var sourceSequence: Long = 0L
    private var playRequestSequence: Long = 0L
    private var pendingPlayRequest: Long? = null
    private var pendingGaplessPlayRequest: Long? = null

    private class VoiceRun(
        val generation: Long,
        val version: Long,
        val playbackRunId: Long,
    ) {
        var terminal: Boolean = false
    }

    private data class OutgoingSnapshot(
        val track: PlayableTrack?,
        val duration: Long?,
        val generation: Long,
        val version: Long,
        val run: VoiceRun?,
        val gain: Float,
    )

    private class PendingIncoming(
        val provider: IOSAudioPlayerProvider,
        val path: String,
        val track: PlayableTrack,
        val generation: Long,
        val version: Long,
        val outgoingGeneration: Long,
        val outgoingVersion: Long,
    ) {
        var durationMillis: Long? = null
        var ready: Boolean = false
        var gain: Float = 0f
        var startingRequest: Long? = null
    }

    private data class PendingTrackLoad(
        val provider: IOSAudioPlayerProvider,
        val path: String,
        val track: PlayableTrack,
        val generation: Long,
        val version: Long,
        /**
         * True when the track is addressed by a local managed file path
         * ([AudioSource.FilePath]) whose absence on disk is direct evidence of
         * a missing file. Non-file sources carry no local-path evidence.
         */
        val isManagedLocalFile: Boolean,
    )

    private fun completionHandler(run: VoiceRun) =
        object : IOSAudioPlayerCompletionHandler {
            override fun onPlaybackCompleted() {
                withIOSPlaybackMainThread completion@{
                    if (run.terminal) return@completion
                    if (retiringRun === run) {
                        run.terminal = true
                        listener?.onPlaybackCompleted(
                            run.generation, run.playbackRunId)
                        return@completion
                    }
                    if (activeRun !== run ||
                        !isCurrentSource(run.generation, run.version))
                        return@completion
                    run.terminal = true
                    if (preparedIncoming?.startingRequest == null)
                        pendingPlayRequest = null
                    if (outgoingSnapshot == null) {
                        cancelPreparedIncomingSerialized()
                    }
                    playbackActive = false
                    wasPlayingBeforeInterruption = false
                    progressJob?.cancel()
                    val pos =
                        audioProvider?.currentPositionMillis()
                            ?: durationMillis
                            ?: 0L
                    listener?.onPlaybackProgress(
                        run.generation, pos, durationMillis)
                    listener?.onPlaybackCompleted(
                        run.generation, run.playbackRunId)
                }
            }

            override fun onPlaybackFailed() {
                withIOSPlaybackMainThread failed@{
                    if (run.terminal) return@failed
                    if (activeRun !== run && retiringRun !== run) return@failed
                    run.terminal = true
                    if (activeRun === run) {
                        if (preparedIncoming?.startingRequest == null)
                            pendingPlayRequest = null
                        playbackActive = false
                        wasPlayingBeforeInterruption = false
                        progressJob?.cancel()
                        if (outgoingSnapshot == null) {
                            cancelPreparedIncomingSerialized()
                            pendingGaplessPlayRequest = null
                        }
                    }
                    listener?.onPlaybackError(
                        run.generation,
                        PlaybackError(
                            "iOS audio playback ended unsuccessfully"))
                }
            }
        }

    private fun interruptionHandler(generation: Long, version: Long) =
        object : IOSAudioInterruptionHandler {
            override fun onInterruptionBegan() {
                withIOSPlaybackMainThread began@{
                    if (!isCurrentSource(generation, version) ||
                        (!playbackActive &&
                            preparedIncoming?.startingRequest == null))
                        return@began
                    wasPlayingBeforeInterruption = true
                    if (preparedIncoming?.startingRequest == null)
                        pendingPlayRequest = null
                    playbackActive = false
                    progressJob?.cancel()
                    val provider = audioProvider ?: return@began
                    provider.pause()
                    val pos = provider.currentPositionMillis()
                    listener?.onPlaybackProgress(
                        generation, pos, durationMillis)
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Paused)
                }
            }

            override fun onInterruptionEnded(shouldResume: Boolean) {
                withIOSPlaybackMainThread ended@{
                    if (!isCurrentSource(generation, version)) return@ended
                    val resume =
                        automaticResumeAllowed &&
                            shouldResume &&
                            wasPlayingBeforeInterruption
                    wasPlayingBeforeInterruption = false
                    if (!resume) return@ended
                    if (audioProvider != null)
                        listener?.onPlayRequested(generation)
                }
            }

            override fun onRouteChanged() {
                withIOSPlaybackMainThread {
                    if (isCurrentSource(generation, version)) {
                        refreshOutputStatus()
                    }
                }
            }

            override fun onRouteDisconnected() {
                withIOSPlaybackMainThread route@{
                    if (!isCurrentSource(generation, version)) return@route
                    refreshOutputStatus()
                    wasPlayingBeforeInterruption = false
                    if (preparedIncoming?.startingRequest == null)
                        pendingPlayRequest = null
                    if (!playbackActive &&
                        preparedIncoming?.startingRequest == null)
                        return@route
                    playbackActive = false
                    progressJob?.cancel()
                    val provider = audioProvider ?: return@route
                    provider.pause()
                    val pos = provider.currentPositionMillis()
                    listener?.onPlaybackProgress(
                        generation, pos, durationMillis)
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Paused)
                }
            }
        }

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val load = withIOSPlaybackMainContext {
            beginTrackLoad(track, generation)
        }
        val result = CompletableDeferred<Boolean>()
        return try {
            withIOSPlaybackMainContext {
                if (!isCurrentSource(load.generation, load.version)) {
                    throw CancellationException("iOS track load became stale")
                }
                if (load.isManagedLocalFile &&
                    !iosFileExistsAtPath(load.path)) {
                    val failure =
                        iosLoadFailureError(
                            path = load.path,
                            managedFileMissing = true,
                        )
                    playbackLog.e {
                        "Cannot play: ${load.track.title} (file is missing)"
                    }
                    listener?.onPlaybackError(load.generation, failure)
                    throw PlaybackFailureException(failure)
                }
            }
            withIOSPlaybackMainThread {
                if (!isCurrentSource(load.generation, load.version)) {
                    throw CancellationException("iOS track load became stale")
                }
                load.provider.loadAsync(
                    load.path,
                    object : IOSAudioPlayerLoadHandler {
                        override fun onAudioLoaded() {
                            result.complete(true)
                        }

                        override fun onAudioLoadFailed() {
                            result.complete(false)
                        }
                    })
            }
            if (!result.await()) {
                withIOSPlaybackMainContext {
                    if (!isCurrentSource(load.generation, load.version)) {
                        throw CancellationException(
                            "iOS track load became stale")
                    }
                    val managedFileMissing =
                        load.isManagedLocalFile &&
                            !iosFileExistsAtPath(load.path)
                    val failure =
                        iosLoadFailureError(
                            path = load.path,
                            managedFileMissing = managedFileMissing,
                        )
                    playbackLog.e { "Cannot play: ${track.title}" }
                    listener?.onPlaybackError(generation, failure)
                    throw PlaybackFailureException(failure)
                }
            }
            withIOSPlaybackMainContext {
                if (!isCurrentSource(load.generation, load.version)) {
                    throw CancellationException("iOS track load became stale")
                }
                finishTrackLoad(load)
            }
        } catch (t: Throwable) {
            withContext(NonCancellable) {
                withIOSPlaybackMainContext {
                    if (isCurrentSource(load.generation, load.version)) {
                        clearFailedLoad(load.provider)
                    }
                }
            }
            throw t
        }
    }

    override suspend fun prepareIncoming(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val incoming = withIOSPlaybackMainContext {
            val provider = requireNotNull(audioProvider) { "No player loaded" }
            check(retiringRun == null) { "A retiring voice is still retained" }
            cancelPreparedIncomingSerialized()
            val path =
                try {
                    track.source.iosFilePath(relativeFilePathResolver)
                } catch (failure: Throwable) {
                    throw PlaybackFailureException(
                        PlaybackError(
                            "Could not resolve player path: ${track.title} (${failure.message})"))
                }
            PendingIncoming(
                    provider = provider,
                    path = path,
                    track = track,
                    generation = generation,
                    version = ++sourceSequence,
                    outgoingGeneration = activeGeneration,
                    outgoingVersion = sourceVersion,
                )
                .also { preparedIncoming = it }
        }
        val result = CompletableDeferred<Boolean>()
        try {
            withIOSPlaybackMainContext {
                if (!ownsPendingIncoming(incoming))
                    throw CancellationException(
                        "iOS incoming load became stale")
                if (incoming.track.source is AudioSource.FilePath &&
                    !iosFileExistsAtPath(incoming.path)) {
                    throw PlaybackFailureException(
                        iosLoadFailureError(
                            incoming.path, managedFileMissing = true))
                }
                incoming.provider.prepareIncomingAsync(
                    incoming.path,
                    object : IOSAudioPlayerLoadHandler {
                        override fun onAudioLoaded() {
                            result.complete(true)
                        }

                        override fun onAudioLoadFailed() {
                            result.complete(false)
                        }
                    },
                )
            }
            val loaded = result.await()
            return withIOSPlaybackMainContext {
                if (!ownsPendingIncoming(incoming))
                    throw CancellationException(
                        "iOS incoming load became stale")
                if (!loaded) {
                    throw PlaybackFailureException(
                        iosLoadFailureError(
                            incoming.path,
                            managedFileMissing =
                                incoming.track.source is AudioSource.FilePath &&
                                    !iosFileExistsAtPath(incoming.path),
                        ))
                }
                incoming.durationMillis =
                    track.durationMillis
                        ?: incoming.provider.incomingDurationMillis()
                incoming.ready = true
                LoadedPlayback(generation, incoming.durationMillis)
            }
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                withIOSPlaybackMainContext {
                    if (preparedIncoming === incoming)
                        cancelPreparedIncomingSerialized()
                }
            }
            throw failure
        }
    }

    override fun startIncoming(playbackRunId: Long) {
        startIncomingInternal(playbackRunId, gapless = false)
    }

    override fun startIncomingGapless(playbackRunId: Long) {
        startIncomingInternal(playbackRunId, gapless = true)
    }

    private fun startIncomingInternal(playbackRunId: Long, gapless: Boolean) {
        withIOSPlaybackMainThread {
            val incoming =
                requireNotNull(preparedIncoming) {
                    "No incoming voice prepared"
                }
            check(incoming.ready && ownsPendingIncoming(incoming)) {
                "Incoming voice is not ready"
            }
            check(incoming.startingRequest == null && playbackActive) {
                "Outgoing voice is not playing"
            }
            val request = ++playRequestSequence
            incoming.startingRequest = request
            pendingPlayRequest = request
            pendingGaplessPlayRequest = request.takeIf { gapless }
            outgoingSnapshot =
                OutgoingSnapshot(
                    loadedTrack,
                    durationMillis,
                    activeGeneration,
                    sourceVersion,
                    activeRun,
                    playbackGain,
                )
            val run =
                VoiceRun(incoming.generation, incoming.version, playbackRunId)
            incoming.provider.startIncomingAsync(
                completionHandler(run),
                object : IOSAudioPlayerPlaybackStartHandler {
                    override fun onPlaybackStarted() {
                        withIOSPlaybackMainThread {
                            if (!ownsPendingIncoming(incoming) ||
                                pendingPlayRequest != request ||
                                incoming.startingRequest != request)
                                return@withIOSPlaybackMainThread
                            retiringRun = activeRun
                            activeRun = run
                            activeGeneration = incoming.generation
                            sourceVersion = incoming.version
                            loadedTrack = incoming.track
                            durationMillis = incoming.durationMillis
                            playbackGain = incoming.gain
                            preparedIncoming = null
                            wasPlayingBeforeInterruption = false
                            incoming.provider.interruptionHandler =
                                interruptionHandler(
                                    activeGeneration, sourceVersion)
                            finishPlayRequest(
                                request,
                                activeGeneration,
                                sourceVersion,
                                incoming.provider)
                            if (pendingGaplessPlayRequest == request) {
                                pendingGaplessPlayRequest = null
                                finishGaplessSerialized()
                            }
                        }
                    }

                    override fun onPlaybackStartFailed() {
                        withIOSPlaybackMainThread {
                            if (!ownsPendingIncoming(incoming) ||
                                pendingPlayRequest != request ||
                                incoming.startingRequest != request)
                                return@withIOSPlaybackMainThread
                            cancelPreparedIncomingSerialized()
                            listener?.onPlaybackError(
                                incoming.generation,
                                PlaybackError(
                                    "Could not start playback: ${incoming.track.title}"),
                            )
                        }
                    }
                },
            )
        }
    }

    override fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        require(
            outgoingGain.isFinite() &&
                outgoingGain >= 0f &&
                outgoingGain <= 1f) {
                "Outgoing gain must be finite and within 0..1"
            }
        require(
            incomingGain.isFinite() &&
                incomingGain >= 0f &&
                incomingGain <= 1f) {
                "Incoming gain must be finite and within 0..1"
            }
        withIOSPlaybackMainThread {
            playbackGain =
                if (retiringRun != null) incomingGain else outgoingGain
            preparedIncoming?.gain = incomingGain
            audioProvider?.setCrossfadeGains(outgoingGain, incomingGain)
        }
    }

    override fun finishCrossfade() {
        withIOSPlaybackMainThread { finishCrossfadeSerialized() }
    }

    override fun finishGapless() {
        withIOSPlaybackMainThread { finishGaplessSerialized() }
    }

    private fun finishGaplessSerialized() {
        outgoingSnapshot = null
        retiringRun = null
        audioProvider?.finishGapless()
    }

    private fun finishCrossfadeSerialized() {
        outgoingSnapshot = null
        retiringRun = null
        audioProvider?.finishCrossfade()
    }

    private fun retireCrossfadeForTransportSerialized() {
        // Ordinary transport targets logical current. Commit any native handoff
        // before
        // cancelling staged work so cancellation cannot restore an accepted old
        // source.
        finishCrossfadeSerialized()
        cancelPreparedIncomingSerialized()
    }

    override fun cancelPreparedIncoming() {
        withIOSPlaybackMainThread { cancelPreparedIncomingSerialized() }
    }

    private fun cancelPreparedIncomingSerialized() {
        val incoming = preparedIncoming
        val outgoing = outgoingSnapshot
        if (incoming == null && outgoing == null) return
        preparedIncoming = null
        pendingPlayRequest = null
        pendingGaplessPlayRequest = null
        (incoming?.provider ?: audioProvider)?.cancelPreparedIncoming()
        outgoingSnapshot = null
        if (outgoing != null) {
            loadedTrack = outgoing.track
            durationMillis = outgoing.duration
            activeGeneration = outgoing.generation
            sourceVersion = outgoing.version
            activeRun = outgoing.run
            retiringRun = null
            playbackGain = outgoing.gain
            audioProvider?.let { provider ->
                provider.setPlaybackGain(outgoing.gain)
                provider.interruptionHandler =
                    interruptionHandler(activeGeneration, sourceVersion)
                playbackActive = provider.isPlaying()
                startProgressLoop(activeGeneration, sourceVersion)
            }
        }
    }

    private fun ownsPendingIncoming(incoming: PendingIncoming): Boolean =
        preparedIncoming === incoming &&
            isCurrentSource(
                incoming.outgoingGeneration, incoming.outgoingVersion)

    private fun beginTrackLoad(
        track: PlayableTrack,
        generation: Long,
    ): PendingTrackLoad {
        activeGeneration = generation
        val version = ++sourceSequence
        sourceVersion = version
        releaseForTrackSwitch()
        wasPlayingBeforeInterruption = false
        playbackActive = false
        playbackLog.d { "Loading track: ${track.title}" }
        listener?.onPlaybackStatus(generation, PlaybackStatus.Loading)
        val provider = IOSAudioPlayerBridge.provider
        if (provider == null) {
            val failure =
                PlaybackError(
                    message = "iOS audio player provider is unavailable",
                    cause = null,
                )
            playbackLog.e { failure.message }
            listener?.onPlaybackError(generation, failure)
            throw PlaybackFailureException(failure)
        }
        val path =
            try {
                track.source.iosFilePath(relativeFilePathResolver)
            } catch (t: Throwable) {
                val failure =
                    PlaybackError(
                        message =
                            "Could not resolve player path: ${track.title} (${t.message})",
                        cause = null,
                    )
                playbackLog.e { failure.message }
                listener?.onPlaybackError(generation, failure)
                throw PlaybackFailureException(failure)
            }
        playbackLog.d { "Player path: $path" }
        provider.completionHandler = null
        provider.interruptionHandler = interruptionHandler(generation, version)
        provider.setPlaybackGain(playbackGain)
        return PendingTrackLoad(
            provider = provider,
            path = path,
            track = track,
            generation = generation,
            version = version,
            isManagedLocalFile = track.source is AudioSource.FilePath,
        )
    }

    private fun finishTrackLoad(load: PendingTrackLoad): LoadedPlayback {
        val provider = load.provider
        audioProvider = provider
        refreshOutputStatus()
        loadedTrack = load.track
        durationMillis =
            load.track.durationMillis ?: provider.currentDurationMillis()
        activeRun = null
        provider.setPlaybackGain(playbackGain)
        provider.pause()
        listener?.onPlaybackProgress(load.generation, 0L, durationMillis)
        playbackLog.d { "Loaded OK: duration=${durationMillis}ms" }
        listener?.onPlaybackStatus(load.generation, PlaybackStatus.Paused)
        return LoadedPlayback(load.generation, durationMillis)
    }

    private fun clearFailedLoad(provider: IOSAudioPlayerProvider) {
        provider.stop()
        playbackGain = 1.0f
        provider.setPlaybackGain(playbackGain)
        provider.completionHandler = null
        provider.interruptionHandler = null
        retireCrossfadeForTransportSerialized()
        activeRun = null
        retiringRun = null
        progressJob?.cancel()
        audioProvider = null
        outputState.value = PlaybackOutputStatus.Unavailable
        loadedTrack = null
        durationMillis = null
        playbackActive = false
        wasPlayingBeforeInterruption = false
    }

    override fun clear(generation: Long) {
        withIOSPlaybackMainThread {
            activeGeneration = generation
            sourceVersion = ++sourceSequence
            releaseForTrackSwitch()
            restorePlaybackGain()
        }
    }

    /**
     * Transport availability is projected to the app-owned system-media
     * publisher.
     */
    override fun setUserTransportEnabled(enabled: Boolean): Unit = Unit

    override fun setPlaybackGain(gain: Float) {
        require(gain.isFinite() && gain >= 0f && gain <= 1f) {
            "Playback gain must be finite and within 0..1, was $gain"
        }
        withIOSPlaybackMainThread { setPlaybackGainSerialized(gain) }
    }

    override fun refreshLoadedMetadata(
        track: PlayableTrack,
        generation: Long,
    ): Boolean = withIOSPlaybackMainThread {
        if (generation != activeGeneration || loadedTrack?.id != track.id)
            return@withIOSPlaybackMainThread false
        loadedTrack = track
        true
    }

    private fun setPlaybackGainSerialized(gain: Float) {
        playbackGain = gain
        (audioProvider ?: IOSAudioPlayerBridge.provider)?.setPlaybackGain(gain)
    }

    private fun restorePlaybackGain() {
        setPlaybackGainSerialized(1.0f)
    }

    override fun play(playbackRunId: Long) {
        withIOSPlaybackMainThread {
            val provider = requireNotNull(audioProvider) { "No player loaded" }
            val run = VoiceRun(activeGeneration, sourceVersion, playbackRunId)
            activeRun = run
            provider.completionHandler = completionHandler(run)
            provider.interruptionHandler =
                interruptionHandler(activeGeneration, sourceVersion)
            requestPlaySerialized(activeGeneration, sourceVersion)
        }
    }

    private fun requestPlaySerialized(generation: Long, version: Long) {
        val provider = requireNotNull(audioProvider) { "No player loaded" }
        playbackLog.d { "Playing: ${loadedTrack?.title}" }
        val request = ++playRequestSequence
        pendingPlayRequest = request
        provider.setPlaybackGain(playbackGain)
        provider.playAsync(
            object : IOSAudioPlayerPlaybackStartHandler {
                override fun onPlaybackStarted() {
                    withIOSPlaybackMainThread {
                        finishPlayRequest(
                            request, generation, version, provider)
                    }
                }

                override fun onPlaybackStartFailed() {
                    withIOSPlaybackMainThread {
                        failPlayRequest(request, generation, version)
                    }
                }
            })
    }

    private fun finishPlayRequest(
        request: Long,
        generation: Long,
        version: Long,
        provider: IOSAudioPlayerProvider,
    ) {
        if (pendingPlayRequest != request ||
            !isCurrentSource(generation, version))
            return
        pendingPlayRequest = null
        playbackActive = true
        if (durationMillis == null) {
            val probedDuration = provider.currentDurationMillis()
            if (probedDuration != null) {
                durationMillis = probedDuration
                playbackLog.d {
                    "Re-probed duration after play(): ${probedDuration}ms (was null at load time)"
                }
            }
        }
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Playing)
        if (!isCurrentSource(generation, version)) return
        listener?.onPlaybackProgress(
            generation, provider.currentPositionMillis(), durationMillis)
        startProgressLoop(generation, version)
    }

    private fun failPlayRequest(
        request: Long,
        generation: Long,
        version: Long
    ) {
        if (pendingPlayRequest != request ||
            !isCurrentSource(generation, version))
            return
        pendingPlayRequest = null
        val errorMsg = "Could not start playback: ${loadedTrack?.title}"
        playbackLog.e { errorMsg }
        listener?.onPlaybackError(
            generation, PlaybackError(errorMsg, cause = null))
    }

    private fun startProgressLoop(
        generation: Long = activeGeneration,
        version: Long = sourceVersion
    ) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(250)
                val current = withIOSPlaybackMainThread {
                    if (!isCurrentSource(generation, version))
                        return@withIOSPlaybackMainThread false
                    val provider =
                        audioProvider ?: return@withIOSPlaybackMainThread false
                    val pos = provider.currentPositionMillis()
                    if (provider.isPlaying()) {
                        listener?.onPlaybackProgress(
                            generation, pos, durationMillis)
                    }
                    true
                }
                if (!current) break
            }
        }
    }

    override fun pause() {
        withIOSPlaybackMainThread {
            wasPlayingBeforeInterruption = false
            pauseSerialized()
        }
    }

    /** Applies the user's persisted interruption-resume policy. */
    public fun setAutomaticResumeAllowed(allowed: Boolean) {
        withIOSPlaybackMainThread {
            automaticResumeAllowed = allowed
            if (!allowed) wasPlayingBeforeInterruption = false
        }
    }

    private fun pauseSerialized() {
        // A pending incoming activation belongs to the controller transaction.
        // Paused
        // settles that unconfirmed transaction via explicit cancel; until then
        // retain
        // its native outcome instead of orphaning the controller's started
        // latch.
        if (preparedIncoming?.startingRequest == null) pendingPlayRequest = null
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        val provider = audioProvider
        provider?.pause()
        val pos = provider?.currentPositionMillis() ?: 0L
        listener?.onPlaybackProgress(activeGeneration, pos, durationMillis)
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Paused)
    }

    override fun stop() {
        withIOSPlaybackMainThread { stopSerialized() }
    }

    private fun stopSerialized() {
        pendingPlayRequest = null
        sourceVersion = ++sourceSequence
        retireCrossfadeForTransportSerialized()
        activeRun = null
        retiringRun = null
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        (audioProvider ?: IOSAudioPlayerBridge.provider)?.stop()
        restorePlaybackGain()
        listener?.onPlaybackProgress(activeGeneration, 0L, durationMillis)
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        withIOSPlaybackMainThread {
            retireCrossfadeForTransportSerialized()
            audioProvider?.seekTo(positionMillis)
            listener?.onPlaybackProgress(
                activeGeneration, positionMillis, durationMillis)
        }
    }

    override fun release() {
        withIOSPlaybackMainThread { releaseSerialized() }
    }

    private fun releaseSerialized() {
        pendingPlayRequest = null
        sourceVersion = ++sourceSequence
        retireCrossfadeForTransportSerialized()
        activeRun = null
        retiringRun = null
        playbackActive = false
        wasPlayingBeforeInterruption = false
        progressJob?.cancel()
        val provider = audioProvider ?: IOSAudioPlayerBridge.provider
        provider?.stop()
        restorePlaybackGain()
        provider?.completionHandler = null
        provider?.interruptionHandler = null
        audioProvider = null
        outputState.value = PlaybackOutputStatus.Unavailable
        loadedTrack = null
        durationMillis = null
    }

    private fun releaseForTrackSwitch() {
        pendingPlayRequest = null
        retireCrossfadeForTransportSerialized()
        activeRun = null
        retiringRun = null
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        if (audioProvider == null) {
            // Also invalidate an async load that has not transferred ownership
            // to audioProvider yet.
            IOSAudioPlayerBridge.provider?.stop()
        } else {
            audioProvider?.fadeOutAndStop(
                fadeDurationSeconds = IOS_TRACK_SWITCH_FADE_SECONDS,
                silentVolume = IOS_TRACK_SWITCH_SILENT_VOLUME,
            )
        }
        audioProvider?.completionHandler = null
        audioProvider?.interruptionHandler = null
        audioProvider = null
        loadedTrack = null
        durationMillis = null
    }

    private fun isCurrentSource(generation: Long, version: Long): Boolean =
        generation == activeGeneration && version == sourceVersion
}

internal class IOSRemoteTransportGate {
    private var enabled: Boolean = true

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun play(action: () -> Unit): Long = perform(action)

    fun seek(positionMillis: Long, action: (Long) -> Unit): Long = perform {
        action(positionMillis)
    }

    fun perform(action: () -> Unit): Long {
        if (!enabled) return MPRemoteCommandHandlerStatusCommandFailed
        action()
        return MPRemoteCommandHandlerStatusSuccess
    }
}

/**
 * Configures which lock-screen / Control Center remote commands are enabled.
 *
 * iOS prefers the skip-interval commands (skip/seek forward/backward) over the
 * previous/next TRACK commands when the interval commands are left enabled.
 * Because this app has no notion of a fixed skip interval — only a track queue
 * — the interval commands must be explicitly disabled, otherwise the lock
 * screen renders them (greyed out, since nothing handles them) instead of the
 * working previous/next track buttons.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun configureIOSRemoteCommandAvailability(
    commandCenter: MPRemoteCommandCenter
) {
    commandCenter.playCommand.setEnabled(true)
    commandCenter.pauseCommand.setEnabled(true)
    commandCenter.togglePlayPauseCommand.setEnabled(true)
    commandCenter.stopCommand.setEnabled(true)
    commandCenter.changePlaybackPositionCommand.setEnabled(true)
    commandCenter.previousTrackCommand.setEnabled(true)
    commandCenter.nextTrackCommand.setEnabled(true)
    commandCenter.skipForwardCommand.setEnabled(false)
    commandCenter.skipBackwardCommand.setEnabled(false)
    commandCenter.seekForwardCommand.setEnabled(false)
    commandCenter.seekBackwardCommand.setEnabled(false)
}

internal fun buildIOSNowPlayingInfo(
    track: PlayableTrack,
    positionMillis: Long,
    durationMillis: Long?,
): Map<String, Any> = buildMap {
    put("title", track.title)
    put("artist", track.artist)
    track.album?.let { put("albumTitle", it) }
    durationMillis?.let { put("durationSeconds", it.toDouble() / 1_000.0) }
    put("elapsedSeconds", positionMillis.coerceAtLeast(0L).toDouble() / 1_000.0)
}

@OptIn(ExperimentalForeignApi::class)
internal fun buildIOSNowPlayingDictionary(
    track: PlayableTrack,
    positionMillis: Long,
    durationMillis: Long?,
    playbackRate: Double,
    existingArtwork: Any? = null,
): Map<Any?, Any?> = buildMap {
    put(MPMediaItemPropertyTitle, track.title)
    put(MPMediaItemPropertyArtist, track.artist)
    track.album?.let { put(MPMediaItemPropertyAlbumTitle, it) }
    existingArtwork?.let { put(MPMediaItemPropertyArtwork, it) }
    if (durationMillis != null && durationMillis > 0L) {
        put(
            MPMediaItemPropertyPlaybackDuration,
            durationMillis.toDouble() / 1_000.0)
        // Do NOT put MPNowPlayingInfoPropertyIsLiveStream at all when duration
        // is known.
        // Apple docs: "When this key is set, the system doesn't display the
        // scrubber."
        // The key's PRESENCE (not just value=true) may disable the slider +
        // prev/next.
    } else {
        put(MPNowPlayingInfoPropertyIsLiveStream, true)
    }
    put(
        MPNowPlayingInfoPropertyElapsedPlaybackTime,
        positionMillis.coerceAtLeast(0L).toDouble() / 1_000.0)
    put(MPNowPlayingInfoPropertyPlaybackRate, playbackRate)
}

private fun AudioSource.iosFilePath(
    relativeFilePathResolver: IOSRelativeFilePathResolver,
): String =
    when (this) {
        is AudioSource.FilePath -> {
            // Container UUID changes on every Xcode install — resolve relative
            // paths.
            if (path.startsWith("/")) path
            else relativeFilePathResolver.resolve(path)
        }

        is AudioSource.Uri ->
            NSURL.URLWithString(value)?.path
                ?: error("Invalid iOS audio URL: $value")

        is AudioSource.FileDescriptor ->
            error(
                "File descriptor audio sources are metadata-only and cannot be played")
    }

/** True when a file exists at the given absolute path. */
internal fun iosFileExistsAtPath(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

/**
 * Builds the structured failure for a native iOS audio load that did not
 * produce playable media. Absence of the managed local file is the only direct
 * evidence the engine can classify; every other provider failure stays
 * [PlaybackFailureKind.Unknown].
 */
internal fun iosLoadFailureError(
    path: String,
    managedFileMissing: Boolean,
): PlaybackError {
    return PlaybackError(
        message =
            if (managedFileMissing) {
                "This audio file is no longer available."
            } else {
                "This audio file could not be played."
            },
        cause = path,
        kind =
            if (managedFileMissing) PlaybackFailureKind.MissingFile
            else PlaybackFailureKind.Unknown,
    )
}
