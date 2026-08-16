package com.eterocell.rhythhaus

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
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
private class IOSPlaybackEngine(
    private val relativeFilePathResolver: IOSRelativeFilePathResolver,
) : PlatformPlaybackEngine {
    private var confinedListener: PlaybackEngineListener? = null
    override var listener: PlaybackEngineListener?
        get() = withIOSPlaybackMainThread { confinedListener }
        set(value) = withIOSPlaybackMainThread { confinedListener = value }

    private var audioProvider: IOSAudioPlayerProvider? = null
    private var loadedTrack: PlayableTrack? = null
    private var durationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var completionReported: Boolean = false
    private var playbackActive: Boolean = false
    private var wasPlayingBeforeInterruption: Boolean = false
    private var remoteCommandsRegistered: Boolean = false
    private val remoteCommandHandlerTokens = mutableListOf<Any?>()
    private var artworkTrackId: String? = null
    private var activeGeneration: Long = 0L
    private var sourceVersion: Long = 0L
    private val remoteTransportGate = IOSRemoteTransportGate()

    init {
        // MPRemoteCommandCenter must be configured on the main thread so the
        // Lock Screen UI layer picks up the enabled command state. Registration
        // from Dispatchers.Default routes events (AirPods work) but the UI
        // does not reflect it (prev/next + slider remain greyed).
        registerRemoteCommands()
    }

    private fun completionHandler(generation: Long, version: Long) =
        object : IOSAudioPlayerCompletionHandler {
            override fun onPlaybackCompleted() {
                withIOSPlaybackMainThread completion@{
                    if (!isCurrentSource(generation, version)) return@completion
                    if (completionReported) return@completion
                    completionReported = true
                    playbackActive = false
                    wasPlayingBeforeInterruption = false
                    progressJob?.cancel()
                    val pos =
                        audioProvider?.currentPositionMillis()
                            ?: durationMillis
                            ?: 0L
                    listener?.onPlaybackProgress(
                        generation, pos, durationMillis)
                    listener?.onPlaybackCompleted(generation)
                }
            }
        }

    private fun interruptionHandler(generation: Long, version: Long) =
        object : IOSAudioInterruptionHandler {
            override fun onInterruptionBegan() {
                withIOSPlaybackMainThread began@{
                    if (!isCurrentSource(generation, version) ||
                        !playbackActive)
                        return@began
                    wasPlayingBeforeInterruption = true
                    playbackActive = false
                    progressJob?.cancel()
                    val provider = audioProvider ?: return@began
                    provider.pause()
                    val pos = provider.currentPositionMillis()
                    updateNowPlayingInfo(
                        positionMillis = pos, playbackRate = 0.0)
                    listener?.onPlaybackProgress(
                        generation, pos, durationMillis)
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Paused)
                }
            }

            override fun onInterruptionEnded(shouldResume: Boolean) {
                withIOSPlaybackMainThread ended@{
                    if (!isCurrentSource(generation, version)) return@ended
                    val resume = shouldResume && wasPlayingBeforeInterruption
                    wasPlayingBeforeInterruption = false
                    if (!resume) return@ended
                    val provider = audioProvider ?: return@ended
                    if (!provider.play()) return@ended
                    playbackActive = true
                    updateNowPlayingInfo(
                        positionMillis = provider.currentPositionMillis(),
                        playbackRate = 1.0,
                    )
                    listener?.onPlaybackStatus(
                        generation, PlaybackStatus.Playing)
                    startProgressLoop(generation, version)
                }
            }

            override fun onRouteDisconnected() {
                withIOSPlaybackMainThread route@{
                    if (!isCurrentSource(generation, version)) return@route
                    wasPlayingBeforeInterruption = false
                    if (!playbackActive) return@route
                    playbackActive = false
                    progressJob?.cancel()
                    val provider = audioProvider ?: return@route
                    provider.pause()
                    val pos = provider.currentPositionMillis()
                    updateNowPlayingInfo(
                        positionMillis = pos, playbackRate = 0.0)
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
    ): LoadedPlayback = withIOSPlaybackMainContext {
        loadPausedSerialized(track, generation)
    }

    private fun loadPausedSerialized(
        track: PlayableTrack,
        generation: Long,
    ): LoadedPlayback {
        activeGeneration = generation
        val version = ++sourceVersion
        releaseForTrackSwitch()
        wasPlayingBeforeInterruption = false
        playbackActive = false
        playbackLog.d { "Loading track: ${track.title}" }
        listener?.onPlaybackStatus(generation, PlaybackStatus.Loading)
        configureAudioSession()
        val provider = IOSAudioPlayerBridge.provider
        if (provider == null) {
            val errorMsg = "iOS audio player provider is unavailable"
            playbackLog.e { errorMsg }
            val error = PlaybackError(errorMsg, cause = null)
            listener?.onPlaybackError(generation, error)
            error(errorMsg)
        }

        try {
            val path =
                try {
                    track.source.iosFilePath(relativeFilePathResolver)
                } catch (t: Throwable) {
                    val errorMsg =
                        "Could not resolve player path: ${track.title} (${t.message})"
                    playbackLog.e { errorMsg }
                    listener?.onPlaybackError(
                        generation, PlaybackError(errorMsg, cause = null))
                    throw t
                }
            playbackLog.d { "Player path: $path" }
            provider.completionHandler = completionHandler(generation, version)
            provider.interruptionHandler =
                interruptionHandler(generation, version)
            if (!provider.load(path)) {
                val errorMsg = "Cannot play: ${track.title}"
                playbackLog.e { errorMsg }
                val error = PlaybackError(errorMsg, cause = path)
                listener?.onPlaybackError(generation, error)
                error(errorMsg)
            }
            audioProvider = provider
            loadedTrack = track
            durationMillis =
                track.durationMillis ?: provider.currentDurationMillis()
            completionReported = false
            updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
            provider.pause()
            listener?.onPlaybackProgress(generation, 0L, durationMillis)
            playbackLog.d { "Loaded OK: duration=${durationMillis}ms" }
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
            return LoadedPlayback(generation, durationMillis)
        } catch (t: Throwable) {
            clearFailedLoad(provider)
            throw t
        }
    }

    private fun clearFailedLoad(provider: IOSAudioPlayerProvider) {
        provider.completionHandler = null
        provider.interruptionHandler = null
        progressJob?.cancel()
        audioProvider = null
        loadedTrack = null
        durationMillis = null
        completionReported = false
        playbackActive = false
        wasPlayingBeforeInterruption = false
        artworkTrackId = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    override fun clear(generation: Long) {
        withIOSPlaybackMainThread {
            activeGeneration = generation
            sourceVersion++
            releaseForTrackSwitch()
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        }
    }

    override fun setUserTransportEnabled(enabled: Boolean) {
        withIOSPlaybackMainThread { remoteTransportGate.setEnabled(enabled) }
    }

    override fun play() {
        withIOSPlaybackMainThread { playSerialized() }
    }

    private fun playSerialized() {
        val provider = requireNotNull(audioProvider) { "No player loaded" }
        playbackLog.d { "Playing: ${loadedTrack?.title}" }
        if (!provider.play()) {
            val errorMsg = "Could not start playback: ${loadedTrack?.title}"
            playbackLog.e { errorMsg }
            listener?.onPlaybackError(
                activeGeneration, PlaybackError(errorMsg, cause = null))
            return
        }
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
        updateNowPlayingInfo(positionMillis = provider.currentPositionMillis())
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Playing)
        listener?.onPlaybackProgress(
            activeGeneration, provider.currentPositionMillis(), durationMillis)
        startProgressLoop(activeGeneration, sourceVersion)
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
        withIOSPlaybackMainThread { pauseSerialized() }
    }

    private fun pauseSerialized() {
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        val provider = audioProvider
        provider?.pause()
        val pos = provider?.currentPositionMillis() ?: 0L
        updateNowPlayingInfo(positionMillis = pos, playbackRate = 0.0)
        listener?.onPlaybackProgress(activeGeneration, pos, durationMillis)
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Paused)
    }

    override fun stop() {
        withIOSPlaybackMainThread { stopSerialized() }
    }

    private fun stopSerialized() {
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        audioProvider?.stop()
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(activeGeneration, 0L, durationMillis)
        listener?.onPlaybackStatus(activeGeneration, PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        withIOSPlaybackMainThread {
            audioProvider?.seekTo(positionMillis)
            updateNowPlayingInfo(positionMillis = positionMillis)
            listener?.onPlaybackProgress(
                activeGeneration, positionMillis, durationMillis)
        }
    }

    override fun release() {
        withIOSPlaybackMainThread { releaseSerialized() }
    }

    private fun releaseSerialized() {
        sourceVersion++
        playbackActive = false
        wasPlayingBeforeInterruption = false
        progressJob?.cancel()
        audioProvider?.stop()
        audioProvider?.completionHandler = null
        audioProvider?.interruptionHandler = null
        audioProvider = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun releaseForTrackSwitch() {
        progressJob?.cancel()
        playbackActive = false
        wasPlayingBeforeInterruption = false
        audioProvider?.fadeOutAndStop(
            fadeDurationSeconds = IOS_TRACK_SWITCH_FADE_SECONDS,
            silentVolume = IOS_TRACK_SWITCH_SILENT_VOLUME,
        )
        audioProvider?.completionHandler = null
        audioProvider?.interruptionHandler = null
        audioProvider = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setActive(true, error = null)
    }

    private fun registerRemoteCommands() {
        if (remoteCommandsRegistered) return
        remoteCommandsRegistered = true
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        configureIOSRemoteCommandAvailability(commandCenter)

        remoteCommandHandlerTokens +=
            commandCenter.playCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.play {
                        val provider = audioProvider
                        if (provider != null && provider.play()) {
                            playbackActive = true
                            updateNowPlayingInfo(
                                positionMillis =
                                    provider.currentPositionMillis(),
                                playbackRate = 1.0)
                            listener?.onPlaybackStatus(
                                activeGeneration, PlaybackStatus.Playing)
                            startProgressLoop()
                        }
                    }
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.pauseCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.perform { pauseSerialized() }
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.perform {
                        val provider = audioProvider
                        if (provider != null) {
                            if (provider.isPlaying()) pauseSerialized()
                            else playSerialized()
                        }
                    }
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.stopCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.perform { stopSerialized() }
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.changePlaybackPositionCommand.addTargetWithHandler {
                event ->
                if (event is MPChangePlaybackPositionCommandEvent) {
                    val seekSeconds = event.positionTime
                    val pos = (seekSeconds * 1_000.0).toLong()
                    withIOSPlaybackMainThread {
                        remoteTransportGate.seek(pos) {
                            audioProvider?.seekTo(it)
                            updateNowPlayingInfo(
                                positionMillis = it,
                                playbackRate =
                                    if (audioProvider?.isPlaying() == true) 1.0
                                    else 0.0)
                            listener?.onPlaybackProgress(
                                activeGeneration, it, durationMillis)
                        }
                    }
                } else {
                    MPRemoteCommandHandlerStatusCommandFailed
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.previousTrackCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.perform {
                        listener?.onSkipToPrevious(activeGeneration)
                    }
                }
            }
        remoteCommandHandlerTokens +=
            commandCenter.nextTrackCommand.addTargetWithHandler { _ ->
                withIOSPlaybackMainThread {
                    remoteTransportGate.perform {
                        listener?.onSkipToNext(activeGeneration)
                    }
                }
            }
    }

    private fun isCurrentSource(generation: Long, version: Long): Boolean =
        generation == activeGeneration && version == sourceVersion

    private fun updateNowPlayingInfo(
        positionMillis: Long,
        playbackRate: Double = 1.0
    ) {
        val track = loadedTrack ?: return
        val existingArtwork =
            MPNowPlayingInfoCenter.defaultCenter()
                .nowPlayingInfo
                ?.get(MPMediaItemPropertyArtwork)
        val dict =
            buildIOSNowPlayingDictionary(
                track = track,
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                playbackRate = playbackRate,
                existingArtwork = existingArtwork,
            )
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = dict
        // Artwork is set via the Swift-native bridge — cinterop doesn't expose
        // NSData(bytes:length:) so the ByteArray→UIImage→MPMediaItemArtwork
        // chain runs in Swift where KotlinByteArray.toData() is available.
        // Only re-decode/re-assign artwork when the track actually changes —
        // this
        // function runs on every progress tick, play/pause, and lockscreen
        // slider
        // drag (changePlaybackPositionCommand), and re-setting the artwork on
        // every
        // call causes the lockscreen art to visibly reload while scrubbing.
        if (artworkTrackId != track.id) {
            artworkTrackId = track.id
            NowPlayingArtworkBridge.provider?.setArtwork(
                trackTitle = track.title,
                artist = track.artist,
                album = track.album,
                artworkBytes = track.artworkBytes,
            )
        }
    }
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
