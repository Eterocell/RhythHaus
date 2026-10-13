package com.eterocell.rhythhaus

/**
 * Process-level bridge that lets the Media3 [RhythHausPlaybackService] route
 * hardware/system "skip to next/previous" transport commands back to the shared
 * [PlaybackController] without the service holding a direct reference to it.
 *
 * The shared controller owns the playback queue (it is the single source of
 * truth that also drives the Compose UI). The Media3 player in the service
 * therefore advertises next/previous as available commands but does not advance
 * its own single-item playlist; instead it invokes these handlers so the
 * controller selects the adjacent queue track and re-loads the engine. This
 * keeps queue state in exactly one place and avoids two diverging playlists.
 */
internal object RhythHausTransportBridge {
    @Volatile private var transportEnabled: Boolean = true

    @Volatile var onSkipToNext: (() -> Unit)? = null

    @Volatile var onSkipToPrevious: (() -> Unit)? = null

    @Volatile var onPlay: (() -> Unit)? = null

    @Volatile var onPause: (() -> Unit)? = null

    @Volatile var onStop: (() -> Unit)? = null

    @Volatile var onExplicitTransport: (() -> Unit)? = null

    @Volatile var onSeek: ((Long) -> Unit)? = null

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
}

internal inline fun routeTransport(action: () -> Unit): Boolean {
    if (!RhythHausTransportBridge.isTransportEnabled()) return false
    action()
    return true
}

internal class ServiceTransportRouter {
    fun play(internalController: Boolean, action: () -> Unit): Boolean =
        routeSessionTransport(internalController, action, explicit = true) {
            RhythHausTransportBridge.onExplicitTransport?.invoke()
            RhythHausTransportBridge.onPlay?.invoke()
        }

    fun pause(internalController: Boolean, action: () -> Unit): Boolean =
        routeSessionTransport(internalController, action, explicit = true) {
            RhythHausTransportBridge.onExplicitTransport?.invoke()
            RhythHausTransportBridge.onPause?.invoke()
        }

    fun stop(internalController: Boolean, action: () -> Unit): Boolean =
        routeSessionTransport(internalController, action, explicit = true) {
            RhythHausTransportBridge.onExplicitTransport?.invoke()
            RhythHausTransportBridge.onStop?.invoke()
        }

    fun seekTo(
        positionMillis: Long,
        internalController: Boolean,
        action: (Long) -> Unit
    ): Boolean =
        routeSessionTransport(
            internalController,
            { action(positionMillis) },
            explicit = true,
        ) {
            RhythHausTransportBridge.onSeek?.invoke(positionMillis)
        }

    fun seekTo(
        mediaItemIndex: Int,
        positionMillis: Long,
        internalController: Boolean,
        action: (Int, Long) -> Unit
    ): Boolean =
        routeSessionTransport(
            internalController,
            { action(mediaItemIndex, positionMillis) },
        ) {
            RhythHausTransportBridge.onSeek?.invoke(positionMillis)
        }

    private inline fun routeSessionTransport(
        internalController: Boolean,
        internalAction: () -> Unit,
        explicit: Boolean = false,
        externalAction: () -> Unit,
    ): Boolean {
        if (internalController) {
            if (explicit) RhythHausTransportBridge.onExplicitTransport?.invoke()
            internalAction()
            return true
        }
        return routeTransport(externalAction)
    }

    fun next(action: () -> Unit): Boolean = routeTransport(action)

    fun previous(action: () -> Unit): Boolean = routeTransport(action)

    fun isCommandAvailable(command: Int, delegateAvailable: Boolean): Boolean =
        command in skipTransportCommands || delegateAvailable
}

internal fun transportCommandAllowed(command: Int): Boolean =
    RhythHausTransportBridge.isTransportEnabled() ||
        command !in gatedTransportCommands

internal fun sessionPlayerCommandAllowed(
    internalController: Boolean,
    command: Int,
): Boolean =
    command !in engineProtocolPlayerCommands &&
        (internalController || transportCommandAllowed(command))

private val engineProtocolPlayerCommands =
    setOf(
        androidx.media3.common.Player.COMMAND_SET_MEDIA_ITEM,
        androidx.media3.common.Player.COMMAND_CHANGE_MEDIA_ITEMS,
        androidx.media3.common.Player.COMMAND_PREPARE,
        androidx.media3.common.Player.COMMAND_SET_VOLUME,
        androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE,
        androidx.media3.common.Player.COMMAND_SET_SHUFFLE_MODE,
    )

private val skipTransportCommands =
    setOf(
        androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
        androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
        androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
    )

private val gatedTransportCommands =
    setOf(
        androidx.media3.common.Player.COMMAND_PLAY_PAUSE,
        androidx.media3.common.Player.COMMAND_STOP,
        androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        androidx.media3.common.Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
        androidx.media3.common.Player.COMMAND_SEEK_TO_MEDIA_ITEM,
        androidx.media3.common.Player.COMMAND_SEEK_BACK,
        androidx.media3.common.Player.COMMAND_SEEK_FORWARD,
    ) + skipTransportCommands

internal fun transportAvailableCommands(
    base: androidx.media3.common.Player.Commands
): androidx.media3.common.Player.Commands {
    val builder =
        base
            .buildUpon()
            .add(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
            .add(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(
                androidx.media3.common.Player
                    .COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
    return builder.build()
}
