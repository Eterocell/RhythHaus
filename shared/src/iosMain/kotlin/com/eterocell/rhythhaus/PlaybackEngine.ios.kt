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
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSThread
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
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
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
    private var player: AVAudioPlayer? = null
    private var loadedTrack: PlayableTrack? = null
    private var durationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var completionReported: Boolean = false
    private var remoteCommandsRegistered: Boolean = false
    private val remoteCommandHandlerTokens = mutableListOf<Any?>()
    private var artworkTrackId: String? = null

    override fun load(track: PlayableTrack) {
        releaseForTrackSwitch()
        log.d { "Loading track: ${track.title}" }
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        configureAudioSession()
        val url = track.source.iosUrl()
        log.d { "Player URL: ${url.absoluteString}" }

        val audioPlayer = try {
            AVAudioPlayer(contentsOfURL = url, error = null)
        } catch (t: Throwable) {
            val errorMsg = "Could not create player: ${track.title} (${t.message})"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = url.absoluteString))
            return
        }

        // On unsupported formats, AVAudioPlayer returns a nil-like object.
        // The tried-and-true approach: attempt prepareToPlay and check if it works.
        if (!audioPlayer.prepareToPlay()) {
            val errorMsg = "Cannot play: ${track.title}"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = url.absoluteString))
            return
        }

        player = audioPlayer
        loadedTrack = track
        durationMillis = track.durationMillis ?: (audioPlayer.duration * 1_000.0).toLong().takeIf { it > 0L }
        completionReported = false
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(0L, durationMillis)
        log.d { "Loaded OK: duration=${durationMillis}ms" }
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        val audioPlayer = requireNotNull(player) { "No player loaded" }
        log.d { "Playing: ${loadedTrack?.title}" }
        if (!audioPlayer.play()) {
            val errorMsg = "Could not start playback: ${loadedTrack?.title}"
            log.e { errorMsg }
            listener?.onPlaybackError(PlaybackError(errorMsg, cause = null))
            return
        }
        // Re-probe duration: AVAudioPlayer.duration may return 0 during prepareToPlay()
        // but become valid once playback starts and the decoder processes frames.
        if (durationMillis == null) {
            val probedDuration = (audioPlayer.duration * 1_000.0).toLong().takeIf { it > 0L }
            if (probedDuration != null) {
                durationMillis = probedDuration
                log.d { "Re-probed duration after play(): ${probedDuration}ms (was null at load time)" }
            }
        }
        updateNowPlayingInfo(positionMillis = (audioPlayer.currentTime * 1_000.0).toLong())
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        listener?.onPlaybackProgress((audioPlayer.currentTime * 1_000.0).toLong(), durationMillis)
        startProgressLoop()
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(250)
                val p = player ?: break
                val pos = (p.currentTime * 1_000.0).toLong()
                if (p.isPlaying()) {
                    listener?.onPlaybackProgress(pos, durationMillis)
                    if (!completionReported && durationMillis != null && pos >= durationMillis!!) {
                        completionReported = true
                        listener?.onPlaybackCompleted()
                    }
                } else if (!completionReported && durationMillis != null && pos > 0L && pos >= durationMillis!! - 500L) {
                    completionReported = true
                    listener?.onPlaybackCompleted()
                }
            }
        }
    }

    override fun pause() {
        progressJob?.cancel()
        val audioPlayer = player
        audioPlayer?.pause()
        updateNowPlayingInfo(positionMillis = ((audioPlayer?.currentTime ?: 0.0) * 1_000.0).toLong(), playbackRate = 0.0)
        listener?.onPlaybackProgress(((audioPlayer?.currentTime ?: 0.0) * 1_000.0).toLong(), durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        progressJob?.cancel()
        val audioPlayer = player
        audioPlayer?.stop()
        audioPlayer?.currentTime = 0.0
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        player?.currentTime = positionMillis.toDouble() / 1_000.0
        updateNowPlayingInfo(positionMillis = positionMillis)
        listener?.onPlaybackProgress(positionMillis, durationMillis)
    }

    override fun release() {
        progressJob?.cancel()
        player?.stop()
        player = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun releaseForTrackSwitch() {
        progressJob?.cancel()
        player?.let { currentPlayer ->
            currentPlayer.setVolume(IOS_TRACK_SWITCH_SILENT_VOLUME, fadeDuration = IOS_TRACK_SWITCH_FADE_SECONDS)
            NSThread.sleepForTimeInterval(IOS_TRACK_SWITCH_FADE_SECONDS)
            currentPlayer.stop()
        }
        player = null
        loadedTrack = null
        durationMillis = null
        artworkTrackId = null
    }

    private fun configureAudioSession() {
        // AVAudioSession category + .longFormAudio policy is configured in Swift
        // (iOSApp.init) because the pre-iOS-13 setCategory(error:) API available
        // via cinterop resets the route sharing policy to .default, which would
        // undo the .longFormAudio policy that iOS needs to treat this app as a
        // primary Now Playing source.
        val session = AVAudioSession.sharedInstance()
        session.setActive(true, error = null)
        registerRemoteCommands()
    }

    private fun registerRemoteCommands() {
        if (remoteCommandsRegistered) return
        remoteCommandsRegistered = true
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        configureIOSRemoteCommandAvailability(commandCenter)

        remoteCommandHandlerTokens += commandCenter.playCommand.addTargetWithHandler { _ ->
            val p = player
            if (p != null) {
                p.play()
                updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong(), playbackRate = 1.0)
                listener?.onPlaybackStatus(PlaybackStatus.Playing)
                startProgressLoop()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.pauseCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            player?.pause()
            val pos = ((player?.currentTime ?: 0.0) * 1_000.0).toLong()
            updateNowPlayingInfo(positionMillis = pos, playbackRate = 0.0)
            listener?.onPlaybackProgress(pos, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            val p = player
            if (p != null) {
                if (p.isPlaying()) {
                    progressJob?.cancel()
                    p.pause()
                    updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong(), playbackRate = 0.0)
                    listener?.onPlaybackStatus(PlaybackStatus.Paused)
                } else {
                    p.play()
                    updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong(), playbackRate = 1.0)
                    listener?.onPlaybackStatus(PlaybackStatus.Playing)
                    startProgressLoop()
                }
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.stopCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            player?.stop()
            player?.currentTime = 0.0
            updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
            listener?.onPlaybackProgress(0L, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Stopped)
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandHandlerTokens += commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            if (event is MPChangePlaybackPositionCommandEvent) {
                val seekSeconds = event.positionTime
                player?.currentTime = seekSeconds
                val pos = (seekSeconds * 1_000.0).toLong()
                updateNowPlayingInfo(positionMillis = pos, playbackRate = if (player?.isPlaying() == true) 1.0 else 0.0)
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
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = buildIOSNowPlayingDictionary(
            track = track,
            positionMillis = positionMillis,
            durationMillis = durationMillis,
            playbackRate = playbackRate,
            existingArtwork = existingArtwork,
        )
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
        put(MPNowPlayingInfoPropertyIsLiveStream, false)
    } else {
        put(MPNowPlayingInfoPropertyIsLiveStream, true)
    }
    put(MPNowPlayingInfoPropertyElapsedPlaybackTime, positionMillis.coerceAtLeast(0L).toDouble() / 1_000.0)
    put(MPNowPlayingInfoPropertyPlaybackRate, playbackRate)
}

private fun AudioSource.iosUrl(): NSURL = when (this) {
    is AudioSource.FilePath -> {
        // Container UUID changes on every Xcode install — resolve relative paths
        val resolved = if (path.startsWith("/")) path else "${appLocalMusicFolderPath()}/$path"
        NSURL.fileURLWithPath(resolved)
    }

    is AudioSource.Uri -> NSURL.URLWithString(value) ?: error("Invalid iOS audio URL: $value")
    is AudioSource.FileDescriptor -> error("File descriptor audio sources are metadata-only and cannot be played")
}
