package com.eterocell.rhythhaus

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.media3.common.Player

class RhythHausTransportBridgeTest {

    @AfterTest
    fun tearDown() {
        RhythHausTransportBridge.onSkipToNext = null
        RhythHausTransportBridge.onSkipToPrevious = null
        RhythHausTransportBridge.setTransportEnabled(true)
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
            override fun onPlaybackStatus(generation: Long, status: PlaybackStatus) = Unit
            override fun onPlaybackProgress(generation: Long, positionMillis: Long, durationMillis: Long?) = Unit
            override fun onPlaybackCompleted(generation: Long) = Unit
            override fun onPlaybackError(generation: Long, error: PlaybackError) = Unit
            override fun onSkipToNext(generation: Long) {
                nextCalls++
            }
            override fun onSkipToPrevious(generation: Long) {
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

    @Test
    fun serviceBridgeRejectsPlayAndSeekWhenTransportDisabled() {
        val bridge = RhythHausTransportBridge.forHostTest()
        bridge.setTransportEnabled(false)

        assertFalse(bridge.handleServicePlayForTest())
        assertFalse(bridge.handleServiceSeekForTest(2_000L))
        assertFalse(bridge.isCommandAvailableForTest(Player.COMMAND_PLAY_PAUSE))
        assertFalse(bridge.isCommandAvailableForTest(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertEquals(emptyList(), bridge.forwardedActionsForTest())
    }

    @Test
    fun serviceBridgePreservesEnabledPlaySeekAndSkipPaths() {
        val bridge = RhythHausTransportBridge.forHostTest()
        bridge.setTransportEnabled(true)

        assertTrue(bridge.handleServicePlayForTest())
        assertTrue(bridge.handleServiceSeekForTest(2_000L))
        assertTrue(bridge.handleServiceNextForTest())
        assertTrue(bridge.isCommandAvailableForTest(Player.COMMAND_PLAY_PAUSE))
        assertEquals(listOf("play", "seek:2000", "next"), bridge.forwardedActionsForTest())
    }
}
