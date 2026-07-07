package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IOSAudioPlayerBridgeTest {

    @Test
    fun swiftAudioPlayerProviderRetainsCompletionHandlerAndForwardsNativeCompletion() {
        val provider = FakeIOSAudioPlayerProvider()
        val events = mutableListOf<String>()
        val handler = object : IOSAudioPlayerCompletionHandler {
            override fun onPlaybackCompleted() {
                events += "completed"
            }
        }

        provider.completionHandler = handler
        provider.simulateNativeCompletion()

        assertEquals(listOf("completed"), events)
        assertSame(handler, provider.completionHandler)
    }

    @Test
    fun iosPlaybackEngineUsesSwiftNativeAudioProvider() {
        assertEquals(IOSAudioBackend.SwiftAVAudioPlayerDelegate, iosAudioBackend)
    }
}

private class FakeIOSAudioPlayerProvider : IOSAudioPlayerProvider {
    override var completionHandler: IOSAudioPlayerCompletionHandler? = null
    private var positionMillis: Long = 0L
    private var durationMillis: Long? = null
    private var playing = false

    override fun load(filePath: String): Boolean {
        durationMillis = 1_000L
        positionMillis = 0L
        playing = false
        return filePath.isNotBlank()
    }

    override fun play(): Boolean {
        playing = true
        return true
    }

    override fun pause() {
        playing = false
    }

    override fun stop() {
        playing = false
        positionMillis = 0L
    }

    override fun seekTo(positionMillis: Long) {
        this.positionMillis = positionMillis
    }

    override fun currentPositionMillis(): Long = positionMillis

    override fun currentDurationMillis(): Long? = durationMillis

    override fun isPlaying(): Boolean = playing

    override fun fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float) {
        playing = false
        positionMillis = 0L
    }

    fun simulateNativeCompletion() {
        completionHandler?.onPlaybackCompleted()
    }
}
