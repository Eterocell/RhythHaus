package com.eterocell.rhythhaus

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = JvmPlaybackEngine()

private class JvmPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var clip: Clip? = null
    private var stream: AudioInputStream? = null
    private var durationMillis: Long? = null

    override fun load(track: PlayableTrack) {
        releaseClip()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val file = track.source.jvmFile()
        stream = AudioSystem.getAudioInputStream(file)
        val audioClip = AudioSystem.getClip()
        audioClip.open(stream)
        clip = audioClip
        durationMillis = track.durationMillis ?: audioClip.microsecondLength.takeIf { it > 0L }?.div(1_000L)
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        val audioClip = requireNotNull(clip) { "No JVM/macOS player has been loaded" }
        audioClip.start()
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        listener?.onPlaybackProgress(audioClip.microsecondPosition / 1_000L, durationMillis)
    }

    override fun pause() {
        val audioClip = clip
        audioClip?.stop()
        listener?.onPlaybackProgress(audioClip?.microsecondPosition?.div(1_000L) ?: 0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        clip?.let { audioClip ->
            audioClip.stop()
            audioClip.microsecondPosition = 0L
        }
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        clip?.microsecondPosition = positionMillis * 1_000L
        listener?.onPlaybackProgress(positionMillis, durationMillis)
    }

    override fun release() {
        releaseClip()
    }

    private fun releaseClip() {
        clip?.stop()
        clip?.close()
        clip = null
        stream?.close()
        stream = null
        durationMillis = null
    }
}

private fun AudioSource.jvmFile(): File = when (this) {
    is AudioSource.FilePath -> File(path)
    is AudioSource.Uri -> if (value.startsWith("file:")) File(java.net.URI(value)) else File(value)
}
