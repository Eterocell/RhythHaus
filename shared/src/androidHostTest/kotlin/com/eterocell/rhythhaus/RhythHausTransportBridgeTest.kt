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
        val actions = mutableListOf<String>()
        val bridge = ServiceTransportRouter()
        bridge.setTransportEnabled(true)

        assertTrue(bridge.play { actions += "play" })
        assertTrue(bridge.pause { actions += "pause" })
        assertTrue(bridge.stop { actions += "stop" })
        assertTrue(bridge.seekTo(2_000L) { actions += "seek:-1:$it" })
        assertTrue(bridge.seekTo(4, 3_000L) { index, position -> actions += "seek:$index:$position" })
        assertTrue(bridge.next { actions += "next" })
        assertTrue(bridge.previous { actions += "previous" })
        assertTrue(bridge.isCommandAvailable(Player.COMMAND_PLAY_PAUSE, delegateAvailable = true))
        assertEquals(
            listOf("play", "pause", "stop", "seek:-1:2000", "seek:4:3000", "next", "previous"),
            actions,
        )
    }

    @Test
    fun productionRouterRejectsEveryGatedOperationWhenDisabled() {
        val actions = mutableListOf<String>()
        val bridge = ServiceTransportRouter()
        bridge.setTransportEnabled(false)

        assertFalse(bridge.play { actions += "play" })
        assertFalse(bridge.pause { actions += "pause" })
        assertFalse(bridge.stop { actions += "stop" })
        assertFalse(bridge.seekTo(2_000L) { actions += "seek:-1:$it" })
        assertFalse(bridge.seekTo(4, 3_000L) { index, position -> actions += "seek:$index:$position" })
        assertFalse(bridge.next { actions += "next" })
        assertFalse(bridge.previous { actions += "previous" })
        assertFalse(bridge.isCommandAvailable(Player.COMMAND_PLAY_PAUSE, delegateAvailable = true))
        assertFalse(bridge.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT, delegateAvailable = true))
        assertEquals(emptyList(), actions)
    }
}
