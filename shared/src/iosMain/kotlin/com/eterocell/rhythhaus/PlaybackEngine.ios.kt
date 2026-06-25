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
        updateNowPlayingInfo(positionMillis = 0L)
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
        updateNowPlayingInfo(positionMillis = ((audioPlayer?.currentTime ?: 0.0) * 1_000.0).toLong())
        listener?.onPlaybackProgress(((audioPlayer?.currentTime ?: 0.0) * 1_000.0).toLong(), durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        progressJob?.cancel()
        val audioPlayer = player
        audioPlayer?.stop()
        audioPlayer?.currentTime = 0.0
        updateNowPlayingInfo(positionMillis = 0L)
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
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    private fun updateNowPlayingInfo(positionMillis: Long) {
        val track = loadedTrack ?: return
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = buildIOSNowPlayingDictionary(track, positionMillis, durationMillis)
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
): Map<Any?, Any?> = buildMap {
    put(MPMediaItemPropertyTitle, track.title)
    put(MPMediaItemPropertyArtist, track.artist)
    track.album?.let { put(MPMediaItemPropertyAlbumTitle, it) }
    durationMillis?.let { put(MPMediaItemPropertyPlaybackDuration, it.toDouble() / 1_000.0) }
    put(MPNowPlayingInfoPropertyElapsedPlaybackTime, positionMillis.coerceAtLeast(0L).toDouble() / 1_000.0)
}

private fun AudioSource.iosUrl(): NSURL = when (this) {
    is AudioSource.FilePath -> {
        // Container UUID changes on every Xcode install — resolve relative paths
        val resolved = if (path.startsWith("/")) path else "${appLocalMusicFolderPath()}/$path"
        NSURL.fileURLWithPath(resolved)
    }
    is AudioSource.Uri -> NSURL.URLWithString(value) ?: error("Invalid iOS audio URL: $value")
}
