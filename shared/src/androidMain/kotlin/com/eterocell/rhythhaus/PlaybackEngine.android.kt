package com.eterocell.rhythhaus

import android.media.MediaPlayer

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = AndroidPlaybackEngine()

private class AndroidPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var player: MediaPlayer? = null

    override fun load(track: PlayableTrack) {
        releasePlayer()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val source = track.source.androidDataSource()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setDataSource(source)
        mediaPlayer.setOnPreparedListener { prepared ->
            listener?.onPlaybackProgress(
                positionMillis = prepared.currentPosition.toLong(),
                durationMillis = prepared.duration.takeIf { it > 0 }?.toLong() ?: track.durationMillis,
            )
            listener?.onPlaybackStatus(PlaybackStatus.Paused)
        }
        mediaPlayer.setOnCompletionListener {
            listener?.onPlaybackCompleted()
        }
        mediaPlayer.setOnErrorListener { _, what, extra ->
            listener?.onPlaybackError(
                PlaybackError(
                    message = "Android could not play this audio file.",
                    cause = "MediaPlayer error what=$what extra=$extra",
                ),
            )
            true
        }
        mediaPlayer.prepare()
    }

    override fun play() {
        val mediaPlayer = requireNotNull(player) { "No Android player has been loaded" }
        mediaPlayer.start()
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        listener?.onPlaybackProgress(
            positionMillis = mediaPlayer.currentPosition.toLong(),
            durationMillis = mediaPlayer.duration.takeIf { it > 0 }?.toLong(),
        )
    }

    override fun pause() {
        player?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            listener?.onPlaybackProgress(
                positionMillis = mediaPlayer.currentPosition.toLong(),
                durationMillis = mediaPlayer.duration.takeIf { it > 0 }?.toLong(),
            )
        }
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        player?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }
        listener?.onPlaybackProgress(0L, player?.duration?.takeIf { it > 0 }?.toLong())
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        player?.seekTo(positionMillis.toInt())
        listener?.onPlaybackProgress(positionMillis, player?.duration?.takeIf { it > 0 }?.toLong())
    }

    override fun release() {
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}

private fun AudioSource.androidDataSource(): String = when (this) {
    AudioSource.DemoTone -> error("Demo tracks do not include audio files yet. Import or scan a local file before playback.")
    is AudioSource.FilePath -> path
    is AudioSource.Uri -> value
}
