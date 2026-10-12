package com.eterocell.rhythhaus

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult

internal const val RHYTHHAUS_ENGINE_COMMAND =
    "com.eterocell.rhythhaus.playback.ENGINE_COMMAND"
internal const val RHYTHHAUS_ENGINE_EVENT =
    "com.eterocell.rhythhaus.playback.ENGINE_EVENT"

internal fun androidEngineSessionCommand(): SessionCommand =
    SessionCommand(RHYTHHAUS_ENGINE_COMMAND, Bundle.EMPTY)

internal fun androidEngineEventCommand(): SessionCommand =
    SessionCommand(RHYTHHAUS_ENGINE_EVENT, Bundle.EMPTY)

/**
 * The sole service command channel carries local configuration as well as
 * metadata.
 */
internal fun AndroidServiceEngineCommand.toAndroidBundle(): Bundle =
    Bundle().apply {
        when (val command = this@toAndroidBundle) {
            is AndroidServiceEngineCommand.LoadPaused -> {
                putString("operation", "load")
                putBundle(
                    "item",
                    command.item.toBundleIncludeLocalConfiguration(
                        MediaLibraryInfo.INTERFACE_VERSION))
                command.gain?.let { putFloat("gain", it) }
            }
            is AndroidServiceEngineCommand.PrepareIncoming -> {
                putString("operation", "prepareIncoming")
                putBundle(
                    "item",
                    command.item.toBundleIncludeLocalConfiguration(
                        MediaLibraryInfo.INTERFACE_VERSION))
                putString("token", command.outgoingToken?.encode())
            }
            is AndroidServiceEngineCommand.Play -> {
                putString("operation", "play")
                putLong("run", command.playbackRunId)
                putString("token", command.token?.encode())
            }
            is AndroidServiceEngineCommand.StartIncoming -> {
                putString("operation", "startIncoming")
                putLong("run", command.playbackRunId)
                putString("token", command.token?.encode())
                putBoolean("gapless", command.gapless)
            }
            is AndroidServiceEngineCommand.Gain -> {
                putString("operation", "gain")
                putFloat("gain", command.gain)
                putString("token", command.token?.encode())
            }
            is AndroidServiceEngineCommand.CrossfadeGains -> {
                putString("operation", "crossfadeGains")
                putFloat("outgoing", command.outgoing)
                putFloat("incoming", command.incoming)
                putString("token", command.token?.encode())
            }
            is AndroidServiceEngineCommand.Seek -> {
                putString("operation", "seek")
                putLong("position", command.positionMillis)
                putString("token", command.token?.encode())
            }
            is AndroidServiceEngineCommand.CancelLoad -> {
                putString("operation", "cancelLoad")
                putString("token", command.token.encode())
            }
            is AndroidServiceEngineCommand.CancelIncoming -> {
                putString("operation", "cancelIncoming")
                putString("token", command.token.encode())
            }
            is AndroidServiceEngineCommand.RetireOutgoing -> {
                putString("operation", "retireOutgoing")
                putString("token", command.token.encode())
            }
            AndroidServiceEngineCommand.Pause -> putString("operation", "pause")
            AndroidServiceEngineCommand.Stop -> putString("operation", "stop")
            AndroidServiceEngineCommand.Clear -> putString("operation", "clear")
            AndroidServiceEngineCommand.Release ->
                putString("operation", "release")
        }
    }

internal fun androidServiceEngineCommand(
    args: Bundle
): AndroidServiceEngineCommand {
    val token =
        args.getString("token")?.let {
            requireNotNull(Media3RequestToken.decode(it))
        }
    fun item(): MediaItem =
        MediaItem.fromBundle(
            requireNotNull(args.getBundle("item")),
            MediaLibraryInfo.INTERFACE_VERSION)
    return when (args.getString("operation")) {
        "load" ->
            AndroidServiceEngineCommand.LoadPaused(
                item(),
                args.takeIf { it.containsKey("gain") }?.getFloat("gain"))
        "prepareIncoming" ->
            AndroidServiceEngineCommand.PrepareIncoming(item(), token)
        "play" -> AndroidServiceEngineCommand.Play(args.getLong("run"), token)
        "startIncoming" ->
            AndroidServiceEngineCommand.StartIncoming(
                args.getLong("run"), token, args.getBoolean("gapless"))
        "gain" -> AndroidServiceEngineCommand.Gain(args.getFloat("gain"), token)
        "crossfadeGains" ->
            AndroidServiceEngineCommand.CrossfadeGains(
                args.getFloat("outgoing"), args.getFloat("incoming"), token)
        "seek" ->
            AndroidServiceEngineCommand.Seek(args.getLong("position"), token)
        "cancelLoad" ->
            AndroidServiceEngineCommand.CancelLoad(requireNotNull(token))
        "cancelIncoming" ->
            AndroidServiceEngineCommand.CancelIncoming(requireNotNull(token))
        "retireOutgoing" ->
            AndroidServiceEngineCommand.RetireOutgoing(requireNotNull(token))
        "pause" -> AndroidServiceEngineCommand.Pause
        "stop" -> AndroidServiceEngineCommand.Stop
        "clear" -> AndroidServiceEngineCommand.Clear
        "release" -> AndroidServiceEngineCommand.Release
        else -> error("Unknown Android engine operation")
    }
}

internal fun AndroidServiceEngineResult.toAndroidSessionResult():
    SessionResult =
    SessionResult(
        when {
            permissionDenied -> SessionResult.RESULT_ERROR_PERMISSION_DENIED
            failure != null -> SessionResult.RESULT_ERROR_IO
            else -> SessionResult.RESULT_SUCCESS
        },
        Bundle().apply {
            durationMillis?.let { putLong("duration", it) }
            failure?.let { putAndroidPlaybackFailure(it) }
        },
    )

internal fun SessionResult.toAndroidEngineResult(): AndroidServiceEngineResult =
    AndroidServiceEngineResult(
        durationMillis =
            extras.takeIf { it.containsKey("duration") }?.getLong("duration"),
        failure =
            extras.androidPlaybackFailure()
                ?: if (resultCode != SessionResult.RESULT_SUCCESS) {
                    PlaybackError(
                        "Android could not complete the playback operation.",
                        "Media3 session result: $resultCode")
                } else null,
        permissionDenied =
            resultCode == SessionResult.RESULT_ERROR_PERMISSION_DENIED,
    )

internal fun AndroidServicePlaybackEvent.toAndroidBundle(): Bundle =
    Bundle().apply {
        putString("token", token.encode())
        playbackRunId?.let { putLong("run", it) }
        status?.let { putString("status", it.name) }
        positionMillis?.let { putLong("position", it) }
        durationMillis?.let { putLong("duration", it) }
        putBoolean("completed", completed)
        failure?.let { putAndroidPlaybackFailure(it) }
    }

internal fun androidServicePlaybackEvent(
    args: Bundle
): AndroidServicePlaybackEvent? {
    val token =
        Media3RequestToken.decode(args.getString("token").orEmpty())
            ?: return null
    return AndroidServicePlaybackEvent(
        token = token,
        playbackRunId = args.takeIf { it.containsKey("run") }?.getLong("run"),
        status =
            args.getString("status")?.let { name ->
                PlaybackStatus.entries.find { it.name == name }
            },
        positionMillis =
            args.takeIf { it.containsKey("position") }?.getLong("position"),
        durationMillis =
            args.takeIf { it.containsKey("duration") }?.getLong("duration"),
        completed = args.getBoolean("completed"),
        failure = args.androidPlaybackFailure(),
    )
}

private fun Bundle.putAndroidPlaybackFailure(failure: PlaybackError) {
    putString("failureMessage", failure.message)
    putString("failureCause", failure.cause)
    putString("failureKind", failure.kind.name)
}

private fun Bundle.androidPlaybackFailure(): PlaybackError? {
    val message = getString("failureMessage") ?: return null
    return PlaybackError(
        message,
        cause = getString("failureCause"),
        kind =
            PlaybackFailureKind.entries.find {
                it.name == getString("failureKind")
            } ?: PlaybackFailureKind.Unknown,
    )
}
