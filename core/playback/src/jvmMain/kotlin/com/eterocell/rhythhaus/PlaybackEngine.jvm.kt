package com.eterocell.rhythhaus

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Creates the JVM platform playback engine. */
public fun createJvmPlaybackEngine(): PlatformPlaybackEngine =
    MacOSNativePlaybackEngine()

internal fun createJvmPlaybackEngine(
    bridge: MacAudioPlayerBridge
): PlatformPlaybackEngine = MacOSNativePlaybackEngine(bridge)

internal class MacOSNativePlaybackEngine(
    private val bridge: MacAudioPlayerBridge = MacAudioPlayerBridge(),
) : CrossfadePlaybackEngine {
    @Volatile override var listener: PlaybackEngineListener? = null
    private val progressExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "rhythhaus-macos-playback-progress").apply {
                isDaemon = true
            }
        }
    private var progressTask: ScheduledFuture<*>? = null
    private var durationMillis: Long? = null
    private var completionReported: Boolean = false
    @Volatile private var activeGeneration: Long = 0L
    private var activePlaybackRunId: Long? = null
    private var sourceVersion: Long = 0L
    private var currentTrack: PlayableTrack? = null
    private var currentPlayerLoaded: Boolean = false
    private var preparedIncoming: PreparedMacVoice? = null
    private var outgoingVoice: RetiringMacVoice? = null
    private var pendingPromotion: PendingMacPromotion? = null
    private val playbackStateLock = Any()

    init {
        bridge.remoteTransportHandler = { command, positionMillis ->
            val currentListener = listener
            if (currentListener == null) false
            else {
                val generation = activeGeneration
                when (command) {
                    1 ->
                        currentListener.onPlayRequested(generation).let { true }
                    2 ->
                        currentListener.onPauseRequested(generation).let {
                            true
                        }
                    3 ->
                        currentListener.onStopRequested(generation).let { true }
                    4 ->
                        currentListener
                            .onSeekRequested(generation, positionMillis)
                            .let { true }
                    else -> false
                }
            }
        }
    }

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val sourceFile = track.source.jvmFile()
        val managedLocalFile = track.source.isManagedLocalFile
        val loadedState =
            synchronized(playbackStateLock) {
                stopProgressUpdatesLocked()
                activeGeneration = generation
                activePlaybackRunId = null
                sourceVersion++
                currentTrack = null
                currentPlayerLoaded = false
                preparedIncoming = null
                outgoingVoice = null
                pendingPromotion = null
                bridge.resetPlayer()
                if (managedLocalFile && !sourceFile.exists()) {
                    throw PlaybackFailureException(
                        macosLoadFailureError(
                            sourceFile, managedFileMissing = true))
                }
                val loaded = bridge.load(sourceFile.absolutePath, generation)
                if (!loaded) {
                    throw PlaybackFailureException(
                        macosLoadFailureError(
                            sourceFile, managedFileMissing = false))
                }
                durationMillis =
                    track.durationMillis
                        ?: bridge.durationMillis().takeIf { it > 0L }
                currentTrack = track
                currentPlayerLoaded = true
                completionReported = false
                bridge.setArtwork(track.artworkBytes)
                bridge.registerNowPlayingRemoteCommands()
                bridge.updateNowPlayingInfo(
                    track.title, track.artist, track.album, durationMillis, 0L)
                LoadedPlayback(generation, durationMillis) to sourceVersion
            }
        val (loadedPlayback, version) = loadedState
        emitIfCurrent(generation, version) {
            listener?.onPlaybackStatus(generation, PlaybackStatus.Loading)
        }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackProgress(
                generation, 0L, loadedPlayback.durationMillis)
        }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
        }
        return loadedPlayback
    }

    override suspend fun prepareIncoming(
        track: PlayableTrack,
        generation: Long,
    ): LoadedPlayback =
        synchronized(playbackStateLock) {
            require(currentPlayerLoaded && outgoingVoice == null) {
                "Crossfade requires a current player and no retiring voice"
            }
            bridge.cancelPreparedIncoming()
            preparedIncoming = null
            val sourceFile = track.source.jvmFile()
            if (track.source.isManagedLocalFile && !sourceFile.exists()) {
                throw PlaybackFailureException(
                    macosLoadFailureError(
                        sourceFile, managedFileMissing = true))
            }
            if (!bridge.prepareIncoming(sourceFile.absolutePath, generation)) {
                throw PlaybackFailureException(
                    macosLoadFailureError(
                        sourceFile, managedFileMissing = false))
            }
            val duration =
                track.durationMillis
                    ?: bridge.incomingDurationMillis().takeIf { it > 0L }
            preparedIncoming = PreparedMacVoice(track, generation, duration)
            LoadedPlayback(generation, duration)
        }

    override fun startIncoming(playbackRunId: Long) {
        val transition =
            synchronized(playbackStateLock) {
                val incoming =
                    requireNotNull(preparedIncoming) {
                        "No incoming macOS player has been prepared"
                    }
                if (!bridge.startIncoming(playbackRunId)) {
                    preparedIncoming = null
                    throw PlaybackFailureException(
                        PlaybackError(
                            "The incoming audio file could not be played.",
                            bridge.lastStartFailure(),
                        ))
                }
                outgoingVoice =
                    RetiringMacVoice(
                        activeGeneration,
                        activePlaybackRunId,
                        completionReported,
                    )
                pendingPromotion =
                    PendingMacPromotion(
                        requireNotNull(currentTrack),
                        durationMillis,
                        requireNotNull(outgoingVoice),
                        progressTask != null,
                    )
                preparedIncoming = null
                currentTrack = incoming.track
                activeGeneration = incoming.generation
                durationMillis = incoming.durationMillis
                activePlaybackRunId = playbackRunId
                completionReported = false
                sourceVersion++
                bridge.setArtwork(incoming.track.artworkBytes)
                bridge.updateNowPlayingInfo(
                    incoming.track.title,
                    incoming.track.artist,
                    incoming.track.album,
                    durationMillis,
                    bridge.currentPositionMillis(),
                )
                bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
                activeGeneration to sourceVersion
            }
        publishPlaying(transition.first, transition.second)
        publishProgress(transition.first, transition.second)
        synchronized(playbackStateLock) {
            if (isCurrentLocked(transition.first, transition.second))
                startProgressUpdatesLocked(transition.first, transition.second)
        }
    }

    override fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        synchronized(playbackStateLock) {
            bridge.setCrossfadeGains(outgoingGain, incomingGain)
        }
    }

    override fun finishCrossfade() {
        synchronized(playbackStateLock) {
            bridge.finishCrossfade()
            outgoingVoice = null
            pendingPromotion = null
        }
    }

    override fun cancelPreparedIncoming() {
        synchronized(playbackStateLock) {
            bridge.cancelPreparedIncoming()
            preparedIncoming = null
            val promotion = pendingPromotion ?: return
            stopProgressUpdatesLocked()
            currentTrack = promotion.track
            activeGeneration = promotion.outgoing.generation
            activePlaybackRunId = promotion.outgoing.playbackRunId
            durationMillis = promotion.durationMillis
            completionReported = promotion.outgoing.completionReported
            outgoingVoice = null
            pendingPromotion = null
            sourceVersion++
            bridge.setArtwork(promotion.track.artworkBytes)
            bridge.updateNowPlayingInfo(
                promotion.track.title,
                promotion.track.artist,
                promotion.track.album,
                durationMillis,
                bridge.currentPositionMillis(),
            )
            bridge.updateNowPlayingPlaybackState(
                if (promotion.resumeProgress) PlaybackStatus.Playing
                else PlaybackStatus.Paused)
            if (promotion.resumeProgress)
                startProgressUpdatesLocked(activeGeneration, sourceVersion)
        }
    }

    override fun clear(generation: Long) {
        synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            activeGeneration = generation
            activePlaybackRunId = null
            sourceVersion++
            bridge.resetPlayerToUnityGain()
            durationMillis = null
            completionReported = false
            currentTrack = null
            currentPlayerLoaded = false
            preparedIncoming = null
            outgoingVoice = null
            pendingPromotion = null
        }
    }

    override fun setUserTransportEnabled(enabled: Boolean) {
        synchronized(playbackStateLock) { bridge.setTransportEnabled(enabled) }
    }

    override fun refreshLoadedMetadata(
        track: PlayableTrack,
        generation: Long,
    ): Boolean =
        synchronized(playbackStateLock) {
            if (generation != activeGeneration) return@synchronized false
            currentTrack = track
            bridge.updateNowPlayingInfo(
                track.title,
                track.artist,
                track.album,
                durationMillis,
                bridge.currentPositionMillis(),
            )
            true
        }

    override fun setPlaybackGain(gain: Float) {
        synchronized(playbackStateLock) { bridge.setPlaybackGain(gain) }
    }

    override fun play(playbackRunId: Long) {
        val transition =
            synchronized(playbackStateLock) {
                if (!currentPlayerLoaded) {
                    val track =
                        requireNotNull(currentTrack) {
                            "No native macOS track has been loaded"
                        }
                    val file = track.source.jvmFile()
                    if (!bridge.load(file.absolutePath, activeGeneration)) {
                        throw PlaybackFailureException(
                            macosLoadFailureError(
                                file,
                                managedFileMissing =
                                    track.source.isManagedLocalFile &&
                                        !file.exists(),
                            ))
                    }
                    currentPlayerLoaded = true
                    completionReported = false
                }
                if (!bridge.play(playbackRunId)) {
                    throw PlaybackFailureException(
                        PlaybackError(
                            "The audio file could not be played.",
                            bridge.lastStartFailure(),
                        ))
                }
                activePlaybackRunId = playbackRunId
                bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
                activeGeneration to sourceVersion
            }
        publishPlaying(transition.first, transition.second)
        publishProgress(transition.first, transition.second)
        synchronized(playbackStateLock) {
            if (isCurrentLocked(transition.first, transition.second))
                startProgressUpdatesLocked(transition.first, transition.second)
        }
    }

    override fun pause() {
        val transition =
            synchronized(playbackStateLock) {
                stopProgressUpdatesLocked()
                pendingPromotion?.resumeProgress = false
                bridge.pause()
                bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
                activeGeneration to sourceVersion
            }
        publishProgress(transition.first, transition.second)
        if (isCurrent(transition.first, transition.second))
            listener?.onPlaybackStatus(transition.first, PlaybackStatus.Paused)
    }

    override fun stop() {
        val event =
            synchronized(playbackStateLock) {
                activePlaybackRunId = null
                stopProgressUpdatesLocked()
                sourceVersion++
                bridge.stop()
                currentPlayerLoaded = false
                completionReported = false
                preparedIncoming = null
                outgoingVoice = null
                pendingPromotion = null
                bridge.updateNowPlayingPlaybackState(PlaybackStatus.Stopped)
                Triple(activeGeneration, sourceVersion, durationMillis)
            }
        emitIfCurrent(event.first, event.second) {
            listener?.onPlaybackProgress(event.first, 0L, event.third)
        }
        emitIfCurrent(event.first, event.second) {
            listener?.onPlaybackStatus(event.first, PlaybackStatus.Stopped)
        }
    }

    override fun seekTo(positionMillis: Long) {
        val transition =
            synchronized(playbackStateLock) {
                bridge.finishCrossfade()
                bridge.cancelPreparedIncoming()
                outgoingVoice = null
                preparedIncoming = null
                pendingPromotion = null
                bridge.seekTo(positionMillis)
                // Seeking to the endpoint is not a natural completion. Also
                // invalidate progress work captured before this playback cycle.
                sourceVersion++
                completionReported =
                    durationMillis?.let { positionMillis >= it } == true
                if (progressTask != null)
                    startProgressUpdatesLocked(activeGeneration, sourceVersion)
                activeGeneration to sourceVersion
            }
        publishProgress(transition.first, transition.second)
    }

    override fun release() {
        bridge.remoteTransportHandler = null
        listener = null
        synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            sourceVersion++
            bridge.releasePlayer()
            progressExecutor.shutdownNow()
            durationMillis = null
            currentTrack = null
            currentPlayerLoaded = false
            preparedIncoming = null
            outgoingVoice = null
            pendingPromotion = null
        }
    }

    private fun startProgressUpdatesLocked(generation: Long, version: Long) {
        stopProgressUpdatesLocked()
        progressTask =
            progressExecutor.scheduleAtFixedRate(
                { publishProgress(generation, version) },
                100L,
                100L,
                TimeUnit.MILLISECONDS,
            )
    }

    private fun stopProgressUpdatesLocked() {
        progressTask?.cancel(false)
        progressTask = null
    }

    private fun publishPlaying(generation: Long, version: Long) {
        val publishedPromotion =
            synchronized(playbackStateLock) {
                if (!isCurrentLocked(generation, version)) return
                if (pendingPromotion == null) false
                else {
                    // The controller may reject this callback after
                    // cancellation.
                    // Retain rollback state until its explicit finish/cancel
                    // decision.
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Playing)
                    true
                }
            }
        if (!publishedPromotion)
            emitIfCurrent(generation, version) {
                listener?.onPlaybackStatus(generation, PlaybackStatus.Playing)
            }
    }

    private fun publishProgress(generation: Long, version: Long) {
        val event =
            synchronized(playbackStateLock) {
                if (!isCurrentLocked(generation, version)) return
                if (bridge.consumeRouteDisconnected()) {
                    stopProgressUpdatesLocked()
                    pendingPromotion?.resumeProgress = false
                    val progress = bridge.readAndUpdateProgress(durationMillis)
                    bridge.pause()
                    bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
                    ProgressEvent(
                        progress,
                        routeLoss = true,
                        currentTerminal = null,
                        outgoingTerminal = null,
                        retiringVoice = outgoingVoice)
                } else {
                    val progress = bridge.readAndUpdateProgress(durationMillis)
                    val retiring = outgoingVoice
                    var currentTerminal: MacPlaybackEvent? = null
                    var outgoingTerminal: MacPlaybackEvent? = null
                    while (true) {
                        val nativeEvent = bridge.consumePlaybackEvent() ?: break
                        if (nativeEvent.generation == activeGeneration &&
                            nativeEvent.playbackRunId == activePlaybackRunId &&
                            !completionReported) {
                            completionReported = true
                            currentTerminal = nativeEvent
                        } else if (retiring != null &&
                            nativeEvent.generation == retiring.generation &&
                            nativeEvent.playbackRunId ==
                                retiring.playbackRunId &&
                            !retiring.completionReported) {
                            retiring.completionReported = true
                            outgoingTerminal = nativeEvent
                        }
                    }
                    if (!completionReported &&
                        activePlaybackRunId != null &&
                        progress.durationMillis != null &&
                        progress.positionMillis >= progress.durationMillis) {
                        completionReported = true
                        currentTerminal =
                            MacPlaybackEvent(
                                generation,
                                requireNotNull(activePlaybackRunId),
                                false)
                    }
                    if (retiring != null &&
                        !retiring.completionReported &&
                        retiring.playbackRunId != null &&
                        bridge.outgoingHasCompleted()) {
                        retiring.completionReported = true
                        outgoingTerminal =
                            MacPlaybackEvent(
                                retiring.generation,
                                retiring.playbackRunId,
                                false)
                    }
                    if (currentTerminal?.failed == true) {
                        stopProgressUpdatesLocked()
                        bridge.pause()
                        bridge.updateNowPlayingPlaybackState(
                            PlaybackStatus.Error)
                    }
                    ProgressEvent(
                        progress,
                        routeLoss = false,
                        currentTerminal = currentTerminal,
                        outgoingTerminal = outgoingTerminal,
                        retiringVoice = retiring)
                }
            }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackProgress(
                generation,
                event.progress.positionMillis,
                event.progress.durationMillis)
        }
        if (event.routeLoss)
            emitIfCurrent(generation, version) {
                listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            }
        else {
            event.outgoingTerminal?.let { terminal ->
                val stillRetiring =
                    synchronized(playbackStateLock) {
                        outgoingVoice === event.retiringVoice
                    }
                if (stillRetiring) emitTerminal(terminal)
            }
            emitIfCurrent(generation, version) {
                event.currentTerminal?.let { terminal ->
                    val stillCurrentRun =
                        synchronized(playbackStateLock) {
                            activePlaybackRunId == terminal.playbackRunId
                        }
                    if (stillCurrentRun) emitTerminal(terminal)
                }
            }
        }
    }

    private fun emitTerminal(event: MacPlaybackEvent) {
        if (event.failed)
            listener?.onPlaybackError(
                event.generation,
                PlaybackError("This audio file could not be played."),
            )
        else
            listener?.onPlaybackCompleted(event.generation, event.playbackRunId)
    }

    internal fun publishProgressForTest() {
        val transition =
            synchronized(playbackStateLock) {
                activeGeneration to sourceVersion
            }
        publishProgress(transition.first, transition.second)
    }

    internal fun pauseProgressUpdatesForTest() {
        synchronized(playbackStateLock) { stopProgressUpdatesLocked() }
    }

    private fun isCurrent(generation: Long, version: Long): Boolean =
        synchronized(playbackStateLock) { isCurrentLocked(generation, version) }

    private inline fun emitIfCurrent(
        generation: Long,
        version: Long,
        event: () -> Unit
    ) {
        if (isCurrent(generation, version)) event()
    }

    private fun isCurrentLocked(generation: Long, version: Long): Boolean =
        generation == activeGeneration && version == sourceVersion

    private data class ProgressEvent(
        val progress: MacProgressSample,
        val routeLoss: Boolean,
        val currentTerminal: MacPlaybackEvent?,
        val outgoingTerminal: MacPlaybackEvent?,
        val retiringVoice: RetiringMacVoice?,
    )

    private data class PreparedMacVoice(
        val track: PlayableTrack,
        val generation: Long,
        val durationMillis: Long?,
    )

    private class RetiringMacVoice(
        val generation: Long,
        val playbackRunId: Long?,
        var completionReported: Boolean,
    )

    private class PendingMacPromotion(
        val track: PlayableTrack,
        val durationMillis: Long?,
        val outgoing: RetiringMacVoice,
        var resumeProgress: Boolean,
    )
}

internal class MacProgressPublicationGate {
    private var activeGeneration: Long = 0L
    private var activeSourceVersion: Long = 0L

    @Synchronized
    fun activate(generation: Long, sourceVersion: Long) {
        activeGeneration = generation
        activeSourceVersion = sourceVersion
    }

    @Synchronized
    fun isCurrent(generation: Long, sourceVersion: Long): Boolean =
        generation == activeGeneration && sourceVersion == activeSourceVersion

    fun publish(
        generation: Long,
        sourceVersion: Long,
        beforeEmit: () -> Unit,
        emitProgress: (Long) -> Unit,
        emitCompletion: (Long) -> Unit,
    ) {
        if (!isCurrent(generation, sourceVersion)) return
        beforeEmit()
        if (!isCurrent(generation, sourceVersion)) return
        emitProgress(generation)
        if (!isCurrent(generation, sourceVersion)) return
        emitCompletion(generation)
    }
}

internal data class MacProgressSample(
    val positionMillis: Long,
    val durationMillis: Long?,
)

internal data class MacPlaybackEvent(
    val generation: Long,
    val playbackRunId: Long,
    val failed: Boolean,
)

internal class MacAudioPlayerBridge {
    private val lifetimeLock = Any()
    @Volatile
    internal var remoteTransportHandler: ((Int, Long) -> Boolean)? = null
    private var transportEnabled: Boolean = true
    private var playbackGain: Float = 1f
    private var handle: Long = 0L
    private var lifetimeIdentity: Long = 0L
    private var released: Boolean = false
    private val playbackEventBuffer = LongArray(3)

    @Suppress("unused") // Called from macOS MPRemoteCommandCenter via JNI.
    private fun onRemoteTransport(command: Int, positionMillis: Long): Boolean =
        remoteTransportHandler?.invoke(command, positionMillis) ?: false

    init {
        synchronized(lifetimeLock) {
            handle = createConfiguredHandleLocked()
        }
    }

    fun load(path: String, generation: Long = 0L): Boolean = withHandle {
        nativeLoad(it, path, generation)
    }

    fun play(playbackRunId: Long = 0L): Boolean = withHandle {
        nativePlay(it, playbackRunId)
    }

    fun lastStartFailure(): String? = withHandle(::nativeLastStartFailure)

    fun prepareIncoming(path: String, generation: Long): Boolean = withHandle {
        nativePrepareIncoming(it, path, generation)
    }

    fun incomingDurationMillis(): Long =
        withHandle(::nativeIncomingDurationMillis)

    fun startIncoming(playbackRunId: Long): Boolean = withHandle {
        nativeStartIncoming(it, playbackRunId).also { started ->
            if (started) playbackGain = nativePlaybackGain(it)
        }
    }

    fun setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        requireUnitGain(outgoingGain)
        requireUnitGain(incomingGain)
        withHandle {
            nativeSetCrossfadeGains(it, outgoingGain, incomingGain)
            playbackGain = nativePlaybackGain(it)
        }
    }

    fun finishCrossfade() = withHandle(::nativeFinishCrossfade)

    fun cancelPreparedIncoming() = withHandle {
        nativeCancelPreparedIncoming(it)
        playbackGain = nativePlaybackGain(it)
    }

    fun outgoingHasCompleted(): Boolean =
        withHandle(::nativeOutgoingHasCompleted)

    fun consumePlaybackEvent(): MacPlaybackEvent? = withHandle {
        if (nativeConsumePlaybackEvent(it, playbackEventBuffer))
            MacPlaybackEvent(
                playbackEventBuffer[0],
                playbackEventBuffer[1],
                playbackEventBuffer[2] == 2L,
            )
        else null
    }

    fun pause() = withHandle(::nativePause)

    fun stop() {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            nativeStop(requireHandleLocked())
            playbackGain = 1f
        }
    }

    fun seekTo(positionMillis: Long) = withHandle {
        nativeSeekTo(it, positionMillis)
    }

    fun currentPositionMillis(): Long =
        withHandle(::nativeCurrentPositionMillis)

    fun durationMillis(): Long = withHandle(::nativeDurationMillis)

    fun setPlaybackGain(gain: Float) {
        requireUnitGain(gain)
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            playbackGain = gain
            nativeSetPlaybackGain(requireHandleLocked(), gain)
        }
    }

    fun updateNowPlayingInfo(
        title: String,
        artist: String,
        album: String?,
        durationMillis: Long?,
        positionMillis: Long,
    ) = withHandle {
        nativeUpdateNowPlayingInfo(
            it, title, artist, album, durationMillis ?: 0L, positionMillis)
    }

    fun updateNowPlayingPosition(positionMillis: Long, durationMillis: Long?) =
        withHandle {
            nativeUpdateNowPlayingPosition(
                it, positionMillis, durationMillis ?: 0L)
        }

    fun updateNowPlayingPlaybackState(status: PlaybackStatus) = withHandle {
        nativeUpdateNowPlayingPlaybackState(it, status.macosPlaybackStateCode())
    }

    fun registerNowPlayingRemoteCommands() =
        withHandle(::nativeRegisterNowPlayingRemoteCommands)

    fun clearNowPlayingInfo() = withHandle(::nativeClearNowPlayingInfo)

    fun setArtwork(artworkBytes: ByteArray?) = withHandle {
        nativeSetArtwork(it, artworkBytes)
    }

    fun setTransportEnabled(enabled: Boolean) {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            transportEnabled = enabled
            nativeSetTransportEnabled(requireHandleLocked(), enabled)
        }
    }

    internal fun invokeRemotePlayForTest(): Boolean =
        withHandle(::nativeInvokeRemotePlayForTest)

    internal fun invokeRemotePauseForTest(): Boolean =
        withHandle(::nativeInvokeRemotePauseForTest)

    internal fun invokeRemoteToggleForTest(): Boolean =
        withHandle(::nativeInvokeRemoteToggleForTest)

    internal fun invokeRemoteStopForTest(): Boolean =
        withHandle(::nativeInvokeRemoteStopForTest)

    internal fun invokeRemoteSeekForTest(positionMillis: Long): Boolean =
        withHandle {
            nativeInvokeRemoteSeekForTest(it, positionMillis)
        }

    internal fun isPlayingForTest(): Boolean =
        withHandle(::nativeIsPlayingForTest)

    internal fun liveRemoteHandlerCountForTest(): Long =
        synchronized(lifetimeLock) { nativeLiveRemoteHandlerCountForTest() }

    internal fun invokeRouteDisconnectForTest(): Boolean =
        withHandle(::nativeInvokeRouteDisconnectForTest)

    internal fun setRouteExpectedActiveForTest(active: Boolean) = withHandle {
        nativeSetRouteExpectedActiveForTest(it, active)
    }

    internal fun routeLifecyclePartialRegistrationForTest(): Boolean =
        withHandle(::nativeRouteLifecyclePartialRegistrationForTest)

    internal fun consumeRouteDisconnected(): Boolean =
        withHandle(::nativeConsumeRouteDisconnected)

    internal fun simulateRouteSnapshotForTest(
        availableDeviceIds: LongArray,
        defaultOutputDeviceId: Long,
    ): Boolean = withHandle {
        nativeSimulateRouteSnapshotForTest(
            it, availableDeviceIds, defaultOutputDeviceId)
    }

    internal fun liveRouteListenerCountForTest(): Long =
        synchronized(lifetimeLock) { nativeLiveRouteListenerCountForTest() }

    internal fun nowPlayingPositionMillisForTest(): Long =
        withHandle(::nativeNowPlayingPositionMillisForTest)

    internal fun playerVolumeForTest(): Float =
        withHandle(::nativePlayerVolumeForTest)

    internal fun readAndUpdateProgress(
        fallbackDurationMillis: Long?
    ): MacProgressSample = withHandle { ownedHandle ->
        val positionMillis =
            nativeCurrentPositionMillis(ownedHandle).coerceAtLeast(0L)
        val durationMillis =
            nativeDurationMillis(ownedHandle).takeIf { it > 0L }
                ?: fallbackDurationMillis
        nativeUpdateNowPlayingPosition(
            ownedHandle, positionMillis, durationMillis ?: 0L)
        MacProgressSample(positionMillis, durationMillis)
    }

    fun resetPlayer() {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            resetPlayerLocked(retainNativeGain = true)
        }
    }

    fun resetPlayerToUnityGain() {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            playbackGain = 1f
            resetPlayerLocked(retainNativeGain = false)
        }
    }

    fun releasePlayer() {
        synchronized(lifetimeLock) {
            if (released) return
            released = true
            releaseHandleLocked()
        }
    }

    @Suppress("ProtectedInFinal")
    protected fun finalize() {
        synchronized(lifetimeLock) {
            if (released) return
            released = true
            releaseHandleLocked()
        }
    }

    internal fun <T> withLifetimeBoundaryForTest(operation: (Long) -> T): T =
        synchronized(lifetimeLock) {
            requireHandleLocked()
            operation(lifetimeIdentity)
        }

    internal fun currentHandleIdentityForTest(): Long =
        synchronized(lifetimeLock) {
            if (handle == 0L) 0L else lifetimeIdentity
        }

    private inline fun <T> withHandle(operation: (Long) -> T): T =
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            operation(requireHandleLocked())
        }

    private fun requireNotReleasedLocked() {
        require(!released) { "Native macOS audio bridge has been released" }
    }

    private fun requireUnitGain(gain: Float) {
        require(gain.isFinite() && gain >= 0f && gain <= 1f) {
            "Playback gain must be finite and within 0..1, was $gain"
        }
    }

    private fun requireHandleLocked(): Long {
        requireNotReleasedLocked()
        return require(handle != 0L) {
                "Native macOS audio bridge has been released"
            }
            .let { handle }
    }

    private fun createConfiguredHandleLocked(): Long =
        nativeCreate().also {
            lifetimeIdentity++
            nativeSetTransportEnabled(it, transportEnabled)
            if (playbackGain != 1f) nativeSetPlaybackGain(it, playbackGain)
        }

    private fun resetPlayerLocked(retainNativeGain: Boolean) {
        if (handle != 0L) {
            if (retainNativeGain) playbackGain = nativePlaybackGain(handle)
            nativeRelease(handle)
        }
        handle = createConfiguredHandleLocked()
    }

    private fun releaseHandleLocked() {
        playbackGain = 1f
        if (handle != 0L) {
            nativeRelease(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(): Long

    private external fun nativeLoad(
        handle: Long,
        path: String,
        generation: Long
    ): Boolean

    private external fun nativePlay(handle: Long, playbackRunId: Long): Boolean

    private external fun nativeLastStartFailure(handle: Long): String?

    private external fun nativePrepareIncoming(
        handle: Long,
        path: String,
        generation: Long
    ): Boolean

    private external fun nativeIncomingDurationMillis(handle: Long): Long

    private external fun nativeStartIncoming(
        handle: Long,
        playbackRunId: Long
    ): Boolean

    private external fun nativeSetCrossfadeGains(
        handle: Long,
        outgoingGain: Float,
        incomingGain: Float
    )

    private external fun nativeFinishCrossfade(handle: Long)

    private external fun nativeCancelPreparedIncoming(handle: Long)

    private external fun nativeOutgoingHasCompleted(handle: Long): Boolean

    private external fun nativeConsumePlaybackEvent(
        handle: Long,
        event: LongArray
    ): Boolean

    private external fun nativePause(handle: Long)

    private external fun nativeStop(handle: Long)

    private external fun nativeSeekTo(handle: Long, positionMillis: Long)

    private external fun nativeCurrentPositionMillis(handle: Long): Long

    private external fun nativeDurationMillis(handle: Long): Long

    private external fun nativePlaybackGain(handle: Long): Float

    private external fun nativeSetPlaybackGain(handle: Long, gain: Float)

    private external fun nativeUpdateNowPlayingInfo(
        handle: Long,
        title: String,
        artist: String,
        album: String?,
        durationMillis: Long,
        positionMillis: Long
    )

    private external fun nativeUpdateNowPlayingPosition(
        handle: Long,
        positionMillis: Long,
        durationMillis: Long
    )

    private external fun nativeUpdateNowPlayingPlaybackState(
        handle: Long,
        playbackStateCode: Int
    )

    private external fun nativeRegisterNowPlayingRemoteCommands(handle: Long)

    private external fun nativeClearNowPlayingInfo(handle: Long)

    private external fun nativeSetArtwork(
        handle: Long,
        artworkBytes: ByteArray?
    )

    private external fun nativeSetTransportEnabled(
        handle: Long,
        enabled: Boolean
    )

    private external fun nativeInvokeRemotePlayForTest(handle: Long): Boolean

    private external fun nativeInvokeRemotePauseForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteToggleForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteStopForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteSeekForTest(
        handle: Long,
        positionMillis: Long
    ): Boolean

    private external fun nativeIsPlayingForTest(handle: Long): Boolean

    private external fun nativeLiveRemoteHandlerCountForTest(): Long

    private external fun nativeInvokeRouteDisconnectForTest(
        handle: Long
    ): Boolean

    private external fun nativeSetRouteExpectedActiveForTest(
        handle: Long,
        active: Boolean
    )

    private external fun nativeRouteLifecyclePartialRegistrationForTest(
        handle: Long
    ): Boolean

    private external fun nativeConsumeRouteDisconnected(handle: Long): Boolean

    private external fun nativeSimulateRouteSnapshotForTest(
        handle: Long,
        availableDeviceIds: LongArray,
        defaultOutputDeviceId: Long,
    ): Boolean

    private external fun nativeLiveRouteListenerCountForTest(): Long

    private external fun nativeNowPlayingPositionMillisForTest(
        handle: Long
    ): Long

    private external fun nativePlayerVolumeForTest(handle: Long): Float

    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            MacAudioNativeLibrary.load()
        }
    }
}

private fun PlaybackStatus.macosPlaybackStateCode(): Int =
    when (this) {
        PlaybackStatus.Playing -> 1
        PlaybackStatus.Paused -> 2
        else -> 0
    }

private object MacAudioNativeLibrary {
    private const val LIBRARY_NAME = "librhythhaus_audio.dylib"

    fun load() {
        val resourcePath = nativeResourcePath()
        val resource =
            requireNotNull(
                MacAudioNativeLibrary::class
                    .java
                    .getResourceAsStream(resourcePath)) {
                    "Native macOS audio helper resource not found: $resourcePath"
                }
        val libraryPath = Files.createTempFile("rhythhaus-audio", ".dylib")
        resource.use { input ->
            Files.copy(input, libraryPath, StandardCopyOption.REPLACE_EXISTING)
        }
        libraryPath.toFile().deleteOnExit()
        System.load(libraryPath.toAbsolutePath().toString())
    }

    private fun nativeResourcePath(): String {
        val architecture = System.getProperty("os.arch").lowercase()
        val platform =
            when {
                System.getProperty("os.name")
                    .contains("Mac", ignoreCase = true) &&
                    architecture in setOf("aarch64", "arm64") -> "macos-aarch64"
                System.getProperty("os.name")
                    .contains("Mac", ignoreCase = true) &&
                    architecture == "x86_64" -> "macos-x64"
                else ->
                    error(
                        "Native macOS audio helper is only available on macOS, current os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
            }
        return "/native/$platform/$LIBRARY_NAME"
    }
}

private fun AudioSource.jvmFile(): File =
    when (this) {
        is AudioSource.FilePath -> File(path)
        is AudioSource.Uri ->
            if (value.startsWith("file:")) File(java.net.URI(value))
            else File(value)
        is AudioSource.FileDescriptor ->
            error(
                "File descriptor audio sources are metadata-only and cannot be played")
    }

/**
 * True when this source is addressed by a local managed file path whose absence
 * on disk is direct evidence of a missing file. Remote or descriptor sources
 * carry no local-path evidence.
 */
private val AudioSource.isManagedLocalFile: Boolean
    get() =
        this is AudioSource.FilePath ||
            (this is AudioSource.Uri && value.startsWith("file:"))

/**
 * Builds the structured failure for a native macOS load that did not produce a
 * playable player. Direct absence of the managed local file is the only
 * evidence the engine can classify as [PlaybackFailureKind.MissingFile]; a bare
 * Boolean failure from the native bridge over an existing file stays
 * [PlaybackFailureKind.Unknown].
 */
internal fun macosLoadFailureError(
    sourceFile: File,
    managedFileMissing: Boolean,
): PlaybackError {
    return PlaybackError(
        message =
            if (managedFileMissing) {
                "This audio file is no longer available."
            } else {
                "This audio file could not be played."
            },
        cause = sourceFile.absolutePath,
        kind =
            if (managedFileMissing) PlaybackFailureKind.MissingFile
            else PlaybackFailureKind.Unknown,
    )
}
