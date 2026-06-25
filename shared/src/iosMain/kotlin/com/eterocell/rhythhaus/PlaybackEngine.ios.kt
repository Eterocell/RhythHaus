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
import com.eterocell.rhythhaus.library.appLocalMusicFolderPath
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = IOSPlaybackEngine()

@OptIn(ExperimentalForeignApi::class)
private class IOSPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var player: AVAudioPlayer? = null
    private var loadedTrack: PlayableTrack? = null
    private var durationMillis: Long? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var completionReported: Boolean = false

    override fun load(track: PlayableTrack) {
        release()
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
        updateNowPlayingInfo(positionMillis = (audioPlayer.currentTime * 1_000.0).toLong())
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        MPNowPlayingInfoCenter.defaultCenter().playbackState = 1uL  // MPNowPlayingPlaybackStatePlaying
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
        MPNowPlayingInfoCenter.defaultCenter().playbackState = 2uL  // MPNowPlayingPlaybackStatePaused
    }

    override fun stop() {
        progressJob?.cancel()
        val audioPlayer = player
        audioPlayer?.stop()
        audioPlayer?.currentTime = 0.0
        updateNowPlayingInfo(positionMillis = 0L, playbackRate = 0.0)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
        MPNowPlayingInfoCenter.defaultCenter().playbackState = 0uL  // MPNowPlayingPlaybackStateStopped
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
        MPNowPlayingInfoCenter.defaultCenter().playbackState = 0uL
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
        registerRemoteCommands()
    }

    private fun registerRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        commandCenter.playCommand.setEnabled(true)
        commandCenter.pauseCommand.setEnabled(true)
        commandCenter.togglePlayPauseCommand.setEnabled(true)
        commandCenter.stopCommand.setEnabled(true)
        commandCenter.changePlaybackPositionCommand.setEnabled(true)
        commandCenter.previousTrackCommand.setEnabled(true)
        commandCenter.nextTrackCommand.setEnabled(true)

        commandCenter.playCommand.addTargetWithHandler { _ ->
            val p = player
            if (p != null) {
                p.play()
                updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong())
                listener?.onPlaybackStatus(PlaybackStatus.Playing)
                startProgressLoop()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.pauseCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            player?.pause()
            val pos = ((player?.currentTime ?: 0.0) * 1_000.0).toLong()
            updateNowPlayingInfo(positionMillis = pos)
            listener?.onPlaybackProgress(pos, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            val p = player
            if (p != null) {
                if (p.isPlaying()) {
                    progressJob?.cancel()
                    p.pause()
                    updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong())
                    listener?.onPlaybackStatus(PlaybackStatus.Paused)
                } else {
                    p.play()
                    updateNowPlayingInfo(positionMillis = (p.currentTime * 1_000.0).toLong())
                    listener?.onPlaybackStatus(PlaybackStatus.Playing)
                    startProgressLoop()
                }
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.stopCommand.addTargetWithHandler { _ ->
            progressJob?.cancel()
            player?.stop()
            player?.currentTime = 0.0
            updateNowPlayingInfo(positionMillis = 0L)
            listener?.onPlaybackProgress(0L, durationMillis)
            listener?.onPlaybackStatus(PlaybackStatus.Stopped)
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            if (event is MPChangePlaybackPositionCommandEvent) {
                val seekSeconds = event.positionTime
                player?.currentTime = seekSeconds
                val pos = (seekSeconds * 1_000.0).toLong()
                updateNowPlayingInfo(positionMillis = pos)
                listener?.onPlaybackProgress(pos, durationMillis)
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.previousTrackCommand.addTargetWithHandler { _ ->
            listener?.onSkipToPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.nextTrackCommand.addTargetWithHandler { _ ->
            listener?.onSkipToNext()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo(positionMillis: Long, playbackRate: Double = 1.0) {
        val track = loadedTrack ?: return
        // Note: MPMediaItemPropertyArtwork is not set here — Kotlin/Native cinterop
        // for ByteArray → NSData → UIImage → MPMediaItemArtwork is unavailable
        // in the current KMP version. Artwork in the app's own Compose UI works fine.
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = buildIOSNowPlayingDictionary(track, positionMillis, durationMillis, playbackRate)
    }
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
private fun buildIOSNowPlayingDictionary(
    track: PlayableTrack,
    positionMillis: Long,
    durationMillis: Long?,
    playbackRate: Double,
): Map<Any?, Any?> = buildMap {
    put(MPMediaItemPropertyTitle, track.title)
    put(MPMediaItemPropertyArtist, track.artist)
    track.album?.let { put(MPMediaItemPropertyAlbumTitle, it) }
    durationMillis?.let { put(MPMediaItemPropertyPlaybackDuration, it.toDouble() / 1_000.0) }
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
}
