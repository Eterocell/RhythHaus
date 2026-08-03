package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.session.PlaybackSessionController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PlaybackContractTest {
    @Test
    fun controllerUsesTheExplicitEngineAndExposesTheSessionPort() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackController(engine)

        assertSame(controller, engine.listener)
        assertSame(controller, controller as PlaybackSessionController)
        assertEquals(PlaybackStatus.Idle, controller.state.value.status)
    }
}
