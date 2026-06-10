package com.eterocell.rhythhaus

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = IOSPlaybackEngine()

@OptIn(ExperimentalForeignApi::class)
private class IOSPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var player: AVAudioPlayer? = null
    private var durationMillis: Long? = null

    override fun load(track: PlayableTrack) {
        release()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        configureAudioSession()
        val audioPlayer = AVAudioPlayer(contentsOfURL = track.source.iosUrl(), error = null)
        audioPlayer.prepareToPlay()
        player = audioPlayer
        durationMillis = track.durationMillis ?: (audioPlayer.duration * 1_000.0).toLong().takeIf { it > 0L }
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        val audioPlayer = requireNotNull(player) { "No iOS player has been loaded" }
        audioPlayer.play()
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        listener?.onPlaybackProgress((audioPlayer.currentTime * 1_000.0).toLong(), durationMillis)
    }

    override fun pause() {
        val audioPlayer = player
        audioPlayer?.pause()
        listener?.onPlaybackProgress(((audioPlayer?.currentTime ?: 0.0) * 1_000.0).toLong(), durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        val audioPlayer = player
        audioPlayer?.stop()
        audioPlayer?.currentTime = 0.0
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        player?.currentTime = positionMillis.toDouble() / 1_000.0
        listener?.onPlaybackProgress(positionMillis, durationMillis)
    }

    override fun release() {
        player?.stop()
        player = null
        durationMillis = null
    }

    private fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }
}

private fun AudioSource.iosUrl(): NSURL = when (this) {
    AudioSource.DemoTone -> error("Demo tracks do not include audio files yet. Import or scan a local file before playback.")
    is AudioSource.FilePath -> NSURL.fileURLWithPath(path)
    is AudioSource.Uri -> NSURL.URLWithString(value) ?: error("Invalid iOS audio URL: $value")
}
