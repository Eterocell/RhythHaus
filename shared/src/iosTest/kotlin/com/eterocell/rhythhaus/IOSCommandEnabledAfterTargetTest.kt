package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

/**
 * Tests that isEnabled remains true AFTER addTargetWithHandler is called.
 *
 * The existing test
 * (remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls)
 * checks isEnabled AFTER configureIOSRemoteCommandAvailability but BEFORE
 * addTargetWithHandler. The real engine code calls addTargetWithHandler AFTER
 * setEnabled(true). If addTargetWithHandler resets isEnabled to false, that
 * would explain why play/pause (registered first) works but prev/next
 * (registered later) are greyed.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSCommandEnabledAfterTargetTest {

    @Test
    fun command_isEnabled_remainsTrue_afterAddTargetWithHandler() {
        val cc = MPRemoteCommandCenter.sharedCommandCenter()

        // Step 1: Enable all commands (same as
        // configureIOSRemoteCommandAvailability)
        cc.playCommand.setEnabled(true)
        cc.pauseCommand.setEnabled(true)
        cc.togglePlayPauseCommand.setEnabled(true)
        cc.stopCommand.setEnabled(true)
        cc.changePlaybackPositionCommand.setEnabled(true)
        cc.previousTrackCommand.setEnabled(true)
        cc.nextTrackCommand.setEnabled(true)

        // Verify all enabled BEFORE addTarget
        assertTrue(
            cc.playCommand.enabled, "play should be enabled before addTarget")
        assertTrue(
            cc.previousTrackCommand.enabled,
            "prev should be enabled before addTarget")
        assertTrue(
            cc.nextTrackCommand.enabled,
            "next should be enabled before addTarget")
        assertTrue(
            cc.changePlaybackPositionCommand.enabled,
            "changePos should be enabled before addTarget")

        // Step 2: Add targets (same order as registerRemoteCommands in
        // IOSPlaybackEngine)
        cc.playCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.pauseCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.togglePlayPauseCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.stopCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.changePlaybackPositionCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.previousTrackCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.nextTrackCommand.addTargetWithHandler {
            MPRemoteCommandHandlerStatusSuccess
        }

        // Step 3: CRITICAL — Check if isEnabled is STILL true after
        // addTargetWithHandler
        println("[CMD-TEST] AFTER addTargetWithHandler:")
        println("[CMD-TEST] play.enabled=${cc.playCommand.enabled}")
        println("[CMD-TEST] pause.enabled=${cc.pauseCommand.enabled}")
        println(
            "[CMD-TEST] toggle.enabled=${cc.togglePlayPauseCommand.enabled}")
        println("[CMD-TEST] stop.enabled=${cc.stopCommand.enabled}")
        println(
            "[CMD-TEST] changePos.enabled=${cc.changePlaybackPositionCommand.enabled}")
        println("[CMD-TEST] prev.enabled=${cc.previousTrackCommand.enabled}")
        println("[CMD-TEST] next.enabled=${cc.nextTrackCommand.enabled}")

        assertTrue(
            cc.playCommand.enabled, "play must remain enabled after addTarget")
        assertTrue(
            cc.pauseCommand.enabled,
            "pause must remain enabled after addTarget")
        assertTrue(
            cc.togglePlayPauseCommand.enabled,
            "toggle must remain enabled after addTarget")
        assertTrue(
            cc.stopCommand.enabled, "stop must remain enabled after addTarget")
        assertTrue(
            cc.changePlaybackPositionCommand.enabled,
            "changePos must remain enabled after addTarget")
        assertTrue(
            cc.previousTrackCommand.enabled,
            "prev must remain enabled after addTarget — IF FALSE, THIS IS THE ROOT CAUSE")
        assertTrue(
            cc.nextTrackCommand.enabled,
            "next must remain enabled after addTarget — IF FALSE, THIS IS THE ROOT CAUSE")
    }
}
