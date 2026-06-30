package com.eterocell.rhythhaus

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhythHausTransportBridgeTest {

    @AfterTest
    fun tearDown() {
        RhythHausTransportBridge.onSkipToNext = null
        RhythHausTransportBridge.onSkipToPrevious = null
    }

    @Test
    fun skipToNextInvokesRegisteredHandler() {
        var calls = 0
        RhythHausTransportBridge.onSkipToNext = { calls++ }

        RhythHausTransportBridge.skipToNext()

        assertEquals(1, calls)
    }

    @Test
    fun skipToPreviousInvokesRegisteredHandler() {
        var calls = 0
        RhythHausTransportBridge.onSkipToPrevious = { calls++ }

        RhythHausTransportBridge.skipToPrevious()

        assertEquals(1, calls)
    }

    @Test
    fun skipIsNoOpWhenNoHandlerRegistered() {
        // No handler set; must not throw.
        RhythHausTransportBridge.skipToNext()
        RhythHausTransportBridge.skipToPrevious()
        assertTrue(true)
    }

    @Test
    fun engineListenerSetterWiresBridgeToListenerSkipCallbacks() {
        // Setting the engine's listener must route hardware/system skip transport (delivered to the
        // service and surfaced via the bridge) into that listener, so the shared controller's queue
        // navigation runs. This verifies the wiring without needing a live MediaController/service.
        var nextCalls = 0
        var prevCalls = 0
        val listener = object : PlaybackEngineListener {
            override fun onPlaybackStatus(status: PlaybackStatus) = Unit
            override fun onPlaybackProgress(positionMillis: Long, durationMillis: Long?) = Unit
            override fun onPlaybackCompleted() = Unit
            override fun onPlaybackError(error: PlaybackError) = Unit
            override fun onSkipToNext() {
                nextCalls++
            }
            override fun onSkipToPrevious() {
                prevCalls++
            }
        }

        val engine = createPlatformPlaybackEngine()
        engine.listener = listener

        RhythHausTransportBridge.skipToNext()
        RhythHausTransportBridge.skipToPrevious()

        assertEquals(1, nextCalls)
        assertEquals(1, prevCalls)

        engine.release()
    }
}
