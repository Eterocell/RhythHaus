package com.eterocell.rhythhaus

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private var rhythHausAndroidContext: Context? = null

fun setRhythHausAndroidContext(context: Context) {
    rhythHausAndroidContext = context.applicationContext
}

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = AndroidPlaybackEngine()

private class AndroidPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var player: ExoPlayer? = null
    private var loadedTrackDurationMillis: Long? = null

    override fun load(track: PlayableTrack) {
        releasePlayer()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val context = requireNotNull(rhythHausAndroidContext) {
            "Android playback context is not configured"
        }
        loadedTrackDurationMillis = track.durationMillis
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer
        exoPlayer.addListener(AndroidPlayerListener())
        exoPlayer.setMediaItem(MediaItem.fromUri(track.source.androidUri()))
        exoPlayer.prepare()
    }

    override fun play() {
        val exoPlayer = requireNotNull(player) { "No Android Media3 player has been loaded" }
        exoPlayer.play()
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        publishProgress(exoPlayer)
    }

    override fun pause() {
        player?.let { exoPlayer ->
            exoPlayer.pause()
            publishProgress(exoPlayer)
        }
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        player?.let { exoPlayer ->
            exoPlayer.pause()
            exoPlayer.seekTo(0L)
            publishProgress(exoPlayer)
        } ?: listener?.onPlaybackProgress(0L, loadedTrackDurationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        player?.let { exoPlayer ->
            exoPlayer.seekTo(positionMillis)
            publishProgress(exoPlayer)
        }
    }

    override fun release() {
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        loadedTrackDurationMillis = null
    }

    private fun publishProgress(exoPlayer: ExoPlayer) {
        listener?.onPlaybackProgress(
            positionMillis = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMillis = exoPlayer.duration.takeIf { it > 0L } ?: loadedTrackDurationMillis,
        )
    }

    private inner class AndroidPlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val exoPlayer = player ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> listener?.onPlaybackStatus(PlaybackStatus.Buffering)
                Player.STATE_READY -> {
                    publishProgress(exoPlayer)
                    listener?.onPlaybackStatus(if (exoPlayer.playWhenReady) PlaybackStatus.Playing else PlaybackStatus.Paused)
                }
                Player.STATE_ENDED -> listener?.onPlaybackCompleted()
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listener?.onPlaybackStatus(if (isPlaying) PlaybackStatus.Playing else PlaybackStatus.Paused)
            player?.let(::publishProgress)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onPlaybackError(
                PlaybackError(
                    message = "Android could not play this audio file.",
                    cause = error.message ?: error.errorCodeName,
                ),
            )
        }
    }
}

private fun AudioSource.androidUri(): Uri = when (this) {
    is AudioSource.FilePath -> Uri.fromFile(java.io.File(path))
    is AudioSource.Uri -> Uri.parse(value)
}
