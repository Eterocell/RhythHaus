package com.eterocell.rhythhaus

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmPlaybackEngineTest {
    @Test
    fun macOSNowPlayingInfoUpdateAcceptsTrackMetadata() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.updateNowPlayingInfo(
                title = "Night Drive",
                artist = "Rhyth Haus",
                album = "Local Sessions",
                durationMillis = 181_000L,
                positionMillis = 42_000L,
            )
            bridge.clearNowPlayingInfo()
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSNowPlayingPlaybackStateUpdatesForControlCenterVisibility() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Stopped)
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun macOSNowPlayingRegistersRemoteCommandsForControlCenter() {
        val bridge = MacAudioPlayerBridge()
        try {
            bridge.registerNowPlayingRemoteCommands()
        } finally {
            bridge.releasePlayer()
        }
    }

    @Test
    fun nativeMacPlaybackEngineLoadsGeneratedWavFile() {
        val wavPath = createSilentWavFile()
        val engine = createPlatformPlaybackEngine()
        val events = mutableListOf<PlaybackStatus>()
        var latestDuration: Long? = null
        var latestError: PlaybackError? = null
        engine.listener = object : PlaybackEngineListener {
            override fun onPlaybackStatus(status: PlaybackStatus) {
                events += status
            }

            override fun onPlaybackProgress(positionMillis: Long, durationMillis: Long?) {
                latestDuration = durationMillis
            }

            override fun onPlaybackCompleted() = Unit

            override fun onPlaybackError(error: PlaybackError) {
                latestError = error
            }
        }

        try {
            engine.load(
                PlayableTrack(
                    id = "generated-wav",
                    title = "Generated WAV",
                    artist = "Test",
                    album = null,
                    durationMillis = null,
                    source = AudioSource.FilePath(wavPath.toString()),
                ),
            )
            engine.play()
            engine.pause()
            engine.seekTo(10L)
            engine.stop()

            assertEquals(null, latestError)
            assertTrue(PlaybackStatus.Loading in events)
            assertTrue(PlaybackStatus.Paused in events)
            assertTrue(PlaybackStatus.Playing in events)
            assertTrue(PlaybackStatus.Stopped in events)
            assertNotNull(latestDuration)
            assertTrue(latestDuration!! > 0L)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    @Test
    fun nativeMacPlaybackEnginePublishesProgressWhilePlaying() {
        val wavPath = createSilentWavFile(durationMillis = 800)
        val engine = createPlatformPlaybackEngine()
        val progressPositions = mutableListOf<Long>()
        val progressLatch = CountDownLatch(2)
        var latestError: PlaybackError? = null
        engine.listener = object : PlaybackEngineListener {
            override fun onPlaybackStatus(status: PlaybackStatus) = Unit

            override fun onPlaybackProgress(positionMillis: Long, durationMillis: Long?) {
                if (positionMillis > 0L) {
                    progressPositions += positionMillis
                    progressLatch.countDown()
                }
            }

            override fun onPlaybackCompleted() = Unit

            override fun onPlaybackError(error: PlaybackError) {
                latestError = error
            }
        }

        try {
            engine.load(
                PlayableTrack(
                    id = "generated-wav-progress",
                    title = "Generated WAV Progress",
                    artist = "Test",
                    album = null,
                    durationMillis = null,
                    source = AudioSource.FilePath(wavPath.toString()),
                ),
            )
            engine.play()

            assertTrue(progressLatch.await(1, TimeUnit.SECONDS), "Expected periodic playback progress events while playing")
            assertEquals(null, latestError)
            assertTrue(progressPositions.maxOrNull()!! > 0L)
        } finally {
            engine.release()
            wavPath.deleteIfExists()
        }
    }

    private fun createSilentWavFile(durationMillis: Int = 100) = createTempFile(prefix = "rhythhaus-silence", suffix = ".wav").also { path ->
        val sampleRate = 8_000
        val sampleCount = sampleRate * durationMillis / 1_000
        val dataSize = sampleCount * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        repeat(sampleCount) { buffer.putShort(0) }
        Files.write(path, buffer.array())
    }
}
