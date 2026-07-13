package com.eterocell.rhythhaus

/**
 * Process-level bridge that lets the Media3 [RhythHausPlaybackService] route hardware/system
 * "skip to next/previous" transport commands back to the shared [PlaybackController] without the
 * service holding a direct reference to it.
 *
 * The shared controller owns the playback queue (it is the single source of truth that also drives
 * the Compose UI). The Media3 player in the service therefore advertises next/previous as available
 * commands but does not advance its own single-item playlist; instead it invokes these handlers so
 * the controller selects the adjacent queue track and re-loads the engine. This keeps queue state
 * in exactly one place and avoids two diverging playlists.
 */
internal object RhythHausTransportBridge {
    @Volatile
    private var transportEnabled: Boolean = true

    @Volatile
    var onSkipToNext: (() -> Unit)? = null

    @Volatile
    var onSkipToPrevious: (() -> Unit)? = null

    fun setTransportEnabled(enabled: Boolean) {
        transportEnabled = enabled
    }

    fun isTransportEnabled(): Boolean = transportEnabled

    fun skipToNext() {
        if (!transportEnabled) return
        onSkipToNext?.invoke()
    }

    fun skipToPrevious() {
        if (!transportEnabled) return
        onSkipToPrevious?.invoke()
    }

    internal fun forHostTest(): RhythHausServiceTransportTestBridge = RhythHausServiceTransportTestBridge()
}

internal class RhythHausServiceTransportTestBridge {
    private val actions = mutableListOf<String>()

    fun setTransportEnabled(enabled: Boolean) = RhythHausTransportBridge.setTransportEnabled(enabled)

    fun handleServicePlayForTest(): Boolean = routeTransport { actions += "play" }

    fun handleServiceSeekForTest(positionMillis: Long): Boolean = routeTransport { actions += "seek:$positionMillis" }

    fun handleServiceNextForTest(): Boolean = routeTransport { actions += "next" }

    fun isCommandAvailableForTest(command: Int): Boolean = transportCommandAllowed(command)

    fun forwardedActionsForTest(): List<String> = actions.toList()
}

internal inline fun routeTransport(action: () -> Unit): Boolean {
    if (!RhythHausTransportBridge.isTransportEnabled()) return false
    action()
    return true
}

internal fun transportCommandAllowed(command: Int): Boolean =
    RhythHausTransportBridge.isTransportEnabled() || command !in gatedTransportCommands

private val gatedTransportCommands = setOf(
    androidx.media3.common.Player.COMMAND_PLAY_PAUSE,
    androidx.media3.common.Player.COMMAND_STOP,
    androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
    androidx.media3.common.Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
    androidx.media3.common.Player.COMMAND_SEEK_TO_MEDIA_ITEM,
    androidx.media3.common.Player.COMMAND_SEEK_BACK,
    androidx.media3.common.Player.COMMAND_SEEK_FORWARD,
    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
)

internal fun transportAvailableCommands(base: androidx.media3.common.Player.Commands): androidx.media3.common.Player.Commands {
    val builder = base.buildUpon()
        .add(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
        .add(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        .add(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
        .add(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
    if (!RhythHausTransportBridge.isTransportEnabled()) {
        gatedTransportCommands.forEach(builder::remove)
    }
    return builder.build()
}
