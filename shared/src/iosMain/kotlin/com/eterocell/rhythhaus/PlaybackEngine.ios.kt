package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.appLocalMusicFolderPath
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
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = IOSPlaybackEngine()

internal enum class IOSTrackSwitchTeardown {
    SoftFade,
}

internal val iosTrackSwitchTeardown: IOSTrackSwitchTeardown = IOSTrackSwitchTeardown.SoftFade
internal const val IOS_TRACK_SWITCH_FADE_SECONDS: Double = 0.05
internal const val IOS_TRACK_SWITCH_SILENT_VOLUME: Float = 0.0f

@OptIn(ExperimentalForeignApi::class)
private class IOSPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var audioProvider: IOSAudioPlayerProvider? = null
    private var loadedTrack: PlayableTrack? = null
    private var durationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var completionReported: Boolean = false
    private var remoteCommandsRegistered: Boolean = false
    private val remoteCommandHandlerTokens = mutableListOf<Any?>()
    private var artworkTrackId: String? = null

    init {
        // MPRemoteCommandCenter must be configured on the main thread so the
        // Lock Screen UI layer picks up the enabled command state. Registration
        // from Dispatchers.Default routes events (AirPods work) but the UI
        // does not reflect it (prev/next + slider remain greyed).
        registerRemoteCommands()
    }

    private val completionHandler = object : IOSAudioPlayerCompletionHandler {
        override fun onPlaybackCompleted() {
            if (completionReported) return
            completionReported = true
            progressJob?.cancel()
            val pos = audioProvider?.currentPositionMillis() ?: durationMillis ?: 0L
            listener?.onPlaybackProgress(pos, durationMillis)
            listener?.onPlaybackCompleted()
        }
    }

    override fun load(track: PlayableTrack) {
        releaseForTrackSwitch()
        log.d { "Loading track: ${track.title}" }
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        configureAudioSession()
        val path = try {
            track.source.iosFilePath()
        } catch (t: Throwable) {
            val errorMsg = "Could not resolve player path: ${track.title} (${t.message})"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = null))
            return
        }
        log.d { "Player path: $path" }

        val provider = IOSAudioPlayerBridge.provider
        if (provider == null) {
            val errorMsg = "iOS audio player provider is unavailable"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = null))
            return
        }
        provider.completionHandler = completionHandler

        if (!provider.load(path)) {
            val errorMsg = "Cannot play: ${track.title}"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = path))
            return
        }

        audioProvider = provider
        loadedTrack = track
        durationMillis = track.durationMillis ?: provider.currentDurationMillis()
        completionReported = false
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(0L, durationMillis)
        log.d { "Loaded OK: duration=${durationMillis}ms" }
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        val provider = requireNotNull(audioProvider) { "No player loaded" }
        log.d { "Playing: ${loadedTrack?.title}" }
        if (!provider.play()) {
            val errorMsg = "Could not start playback: ${loadedTrack?.title}"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = null))
            return
        }
        if (durationMillis == null) {
            val probedDuration = provider.currentDurationMillis()
            if (probedDuration != null) {
                durationMillis = probedDuration
                log.d { "Re-probed duration after play(): ${probedDuration}ms (was null at load time)" }
            }
        }
        updateNowPlayingInfo(positionMillis = provider.currentPositionMillis())
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        listener?.onPlaybackProgress(provider.currentPositionMillis(), durationMillis)
        startProgressLoop()
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(250)
                val provider = audioProvider ?: break
                val pos = provider.currentPositionMillis()
                if (provider.isPlaying()) {
                    listener?.onPlaybackProgress(pos, durationMillis)
                }
            }
        }
    }

    override fun pause() {
        progressJob?.cancel()
        val provider = audioProvider
        provider?.pause()
        val pos = provider?.currentPositionMillis() ?: 0L
        updateNowPlayingInfo(positionMillis = pos, playbackRate = 0.0)
        listener?.onPlaybackProgress(pos, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        progressJob?.cancel()
        audioProvider?.stop()
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        audioProvider?.seekTo(positionMillis)
        updateNowPlayingInfo(positionMillis = positionMillis)
        listener?.onPlaybackProgress(positionMillis, durationMillis)
    }

    override fun release() {
        progressJob?.cancel()
        audioProvider?.stop()
        audioProvider?.completionHandler = null
        audioProvider = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun releaseForTrackSwitch() {
        progressJob?.cancel()
        audioProvider?.fadeOutAndStop(
            fadeDurationSeconds = IOS_TRACK_SWITCH_FADE_SECONDS,
            silentVolume = IOS_TRACK_SWITCH_SILENT_VOLUME,
        )
        audioProvider?.completionHandler = null
        audioProvider = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
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

        remoteCommandHandlerTokens += commandCenter.playCommand.addTargetWithHandler { _ ->
            val provider = audioProvider
            if (provider != null) {
                provider.play()
                updateNowPlayingInfo(positionMillis = provider.currentPositionMillis(), playbackRate = 1.0)
                listener?.onPlaybackStatus(PlaybackStatus.Playing)
                startProgressLoop()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.pauseCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            audioProvider?.pause()
            val pos = audioProvider?.currentPositionMillis() ?: 0L
            updateNowPlayingInfo(positionMillis = pos, playbackRate = 0.0)
            listener?.onPlaybackProgress(pos, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            val provider = audioProvider
            if (provider != null) {
                if (provider.isPlaying()) {
                    progressJob?.cancel()
                    provider.pause()
                    updateNowPlayingInfo(positionMillis = provider.currentPositionMillis(), playbackRate = 0.0)
                    listener?.onPlaybackStatus(PlaybackStatus.Paused)
                } else {
                    provider.play()
                    updateNowPlayingInfo(positionMillis = provider.currentPositionMillis(), playbackRate = 1.0)
                    listener?.onPlaybackStatus(PlaybackStatus.Playing)
                    startProgressLoop()
                }
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.stopCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            audioProvider?.stop()
            updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
            listener?.onPlaybackProgress(0L, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Stopped)
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            if (event is MPChangePlaybackPositionCommandEvent) {
                val seekSeconds = event.positionTime
                val pos = (seekSeconds * 1_000.0).toLong()
                audioProvider?.seekTo(pos)
                updateNowPlayingInfo(positionMillis = pos, playbackRate = if (audioProvider?.isPlaying() == true) 1.0 else 0.0)
                listener?.onPlaybackProgress(pos, durationMillis)
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.previousTrackCommand.addTargetWithHandler { _ ->
            listener?.onSkipToPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.nextTrackCommand.addTargetWithHandler { _ ->
            listener?.onSkipToNext()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo(positionMillis: Long, playbackRate: Double = 1.0) {
        val track = loadedTrack ?: return
        val existingArtwork = MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo?.get(MPMediaItemPropertyArtwork)
        val dict = buildIOSNowPlayingDictionary(
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
        // Only re-decode/re-assign artwork when the track actually changes — this
        // function runs on every progress tick, play/pause, and lockscreen slider
        // drag (changePlaybackPositionCommand), and re-setting the artwork on every
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

/**
 * Configures which lock-screen / Control Center remote commands are enabled.
 *
 * iOS prefers the skip-interval commands (skip/seek forward/backward) over the previous/next
 * TRACK commands when the interval commands are left enabled. Because this app has no notion of
 * a fixed skip interval — only a track queue — the interval commands must be explicitly disabled,
 * otherwise the lock screen renders them (greyed out, since nothing handles them) instead of the
 * working previous/next track buttons.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun configureIOSRemoteCommandAvailability(commandCenter: MPRemoteCommandCenter) {
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
        put(MPMediaItemPropertyPlaybackDuration, durationMillis.toDouble() / 1_000.0)
        // Do NOT put MPNowPlayingInfoPropertyIsLiveStream at all when duration is known.
        // Apple docs: "When this key is set, the system doesn't display the scrubber."
        // The key's PRESENCE (not just value=true) may disable the slider + prev/next.
    } else {
        put(MPNowPlayingInfoPropertyIsLiveStream, true)
    }
    put(MPNowPlayingInfoPropertyElapsedPlaybackTime, positionMillis.coerceAtLeast(0L).toDouble() / 1_000.0)
    put(MPNowPlayingInfoPropertyPlaybackRate, playbackRate)
}

private fun AudioSource.iosFilePath(): String = when (this) {
    is AudioSource.FilePath -> {
        // Container UUID changes on every Xcode install — resolve relative paths.
        if (path.startsWith("/")) path else "${appLocalMusicFolderPath()}/$path"
    }

    is AudioSource.Uri -> NSURL.URLWithString(value)?.path ?: error("Invalid iOS audio URL: $value")
    is AudioSource.FileDescriptor -> error("File descriptor audio sources are metadata-only and cannot be played")
}
