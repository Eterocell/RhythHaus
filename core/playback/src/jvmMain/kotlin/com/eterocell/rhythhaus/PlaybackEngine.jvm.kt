package com.eterocell.rhythhaus

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Creates the JVM platform playback engine. */
public fun createJvmPlaybackEngine(): PlatformPlaybackEngine =
    MacOSNativePlaybackEngine()

internal fun createJvmPlaybackEngine(bridge: MacAudioPlayerBridge): PlatformPlaybackEngine =
    MacOSNativePlaybackEngine(bridge)

internal class MacOSNativePlaybackEngine(
    private val bridge: MacAudioPlayerBridge = MacAudioPlayerBridge(),
) : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private val progressExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "rhythhaus-macos-playback-progress").apply {
                isDaemon = true
            }
        }
    private var progressTask: ScheduledFuture<*>? = null
    private var durationMillis: Long? = null
    private var completionReported: Boolean = false
    private var activeGeneration: Long = 0L
    private var sourceVersion: Long = 0L
    private val playbackStateLock = Any()

    override suspend fun loadPaused(
        track: PlayableTrack,
        generation: Long
    ): LoadedPlayback {
        val loadedState = synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            activeGeneration = generation
            sourceVersion++
            bridge.resetPlayer()
            val loaded = bridge.load(track.source.jvmFile().absolutePath)
            require(loaded) { "Could not load native macOS audio player" }
            durationMillis = track.durationMillis ?: bridge.durationMillis().takeIf { it > 0L }
            completionReported = false
            bridge.setArtwork(track.artworkBytes)
            bridge.registerNowPlayingRemoteCommands()
            bridge.updateNowPlayingInfo(track.title, track.artist, track.album, durationMillis, 0L)
            LoadedPlayback(generation, durationMillis) to sourceVersion
        }
        val (loadedPlayback, version) = loadedState
        emitIfCurrent(generation, version) {
            listener?.onPlaybackStatus(generation, PlaybackStatus.Loading)
        }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackProgress(generation, 0L, loadedPlayback.durationMillis)
        }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
        }
        return loadedPlayback
    }

    override fun clear(generation: Long) {
        synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            activeGeneration = generation
            sourceVersion++
            bridge.resetPlayer()
            durationMillis = null
            completionReported = false
        }
    }

    override fun setUserTransportEnabled(enabled: Boolean) {
        synchronized(playbackStateLock) { bridge.setTransportEnabled(enabled) }
    }

    override fun play() {
        val transition = synchronized(playbackStateLock) {
            require(bridge.play()) { "No native macOS player has been loaded" }
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Playing)
            activeGeneration to sourceVersion
        }
        emitIfCurrent(transition.first, transition.second) {
            listener?.onPlaybackStatus(transition.first, PlaybackStatus.Playing)
        }
        publishProgress(transition.first, transition.second)
        synchronized(playbackStateLock) {
            if (isCurrentLocked(transition.first, transition.second)) startProgressUpdatesLocked(transition.first, transition.second)
        }
    }

    override fun pause() {
        val transition = synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            bridge.pause()
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
            activeGeneration to sourceVersion
        }
        publishProgress(transition.first, transition.second)
        if (isCurrent(transition.first, transition.second)) listener?.onPlaybackStatus(transition.first, PlaybackStatus.Paused)
    }

    override fun stop() {
        val event = synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            bridge.stop()
            bridge.updateNowPlayingPlaybackState(PlaybackStatus.Stopped)
            Triple(activeGeneration, sourceVersion, durationMillis)
        }
        emitIfCurrent(event.first, event.second) {
            listener?.onPlaybackProgress(event.first, 0L, event.third)
        }
        emitIfCurrent(event.first, event.second) {
            listener?.onPlaybackStatus(event.first, PlaybackStatus.Stopped)
        }
    }

    override fun seekTo(positionMillis: Long) {
        val transition = synchronized(playbackStateLock) {
            bridge.seekTo(positionMillis)
            activeGeneration to sourceVersion
        }
        publishProgress(transition.first, transition.second)
    }

    override fun release() {
        synchronized(playbackStateLock) {
            stopProgressUpdatesLocked()
            sourceVersion++
            bridge.releasePlayer()
            progressExecutor.shutdownNow()
            durationMillis = null
        }
    }

    private fun startProgressUpdatesLocked(generation: Long, version: Long) {
        stopProgressUpdatesLocked()
        progressTask =
            progressExecutor.scheduleAtFixedRate(
                { publishProgress(generation, version) },
                100L,
                100L,
                TimeUnit.MILLISECONDS,
            )
    }

    private fun stopProgressUpdatesLocked() {
        progressTask?.cancel(false)
        progressTask = null
    }

    private fun publishProgress(generation: Long, version: Long) {
        val event = synchronized(playbackStateLock) {
            if (!isCurrentLocked(generation, version)) return
            if (bridge.consumeRouteDisconnected()) {
                stopProgressUpdatesLocked()
                val progress = bridge.readAndUpdateProgress(durationMillis)
                bridge.pause()
                bridge.updateNowPlayingPlaybackState(PlaybackStatus.Paused)
                ProgressEvent(progress, routeLoss = true, completed = false)
            } else {
                val progress = bridge.readAndUpdateProgress(durationMillis)
                val completed = !completionReported && progress.durationMillis != null && progress.positionMillis >= progress.durationMillis
                if (completed) completionReported = true
                ProgressEvent(progress, routeLoss = false, completed = completed)
            }
        }
        emitIfCurrent(generation, version) {
            listener?.onPlaybackProgress(generation, event.progress.positionMillis, event.progress.durationMillis)
        }
        if (event.routeLoss) emitIfCurrent(generation, version) {
            listener?.onPlaybackStatus(generation, PlaybackStatus.Paused)
        } else if (event.completed) emitIfCurrent(generation, version) {
            listener?.onPlaybackCompleted(generation)
        }
    }

    internal fun publishProgressForTest() {
        val transition = synchronized(playbackStateLock) { activeGeneration to sourceVersion }
        publishProgress(transition.first, transition.second)
    }

    internal fun pauseProgressUpdatesForTest() {
        synchronized(playbackStateLock) { stopProgressUpdatesLocked() }
    }

    private fun isCurrent(generation: Long, version: Long): Boolean = synchronized(playbackStateLock) { isCurrentLocked(generation, version) }
    private inline fun emitIfCurrent(generation: Long, version: Long, event: () -> Unit) {
        if (isCurrent(generation, version)) event()
    }
    private fun isCurrentLocked(generation: Long, version: Long): Boolean = generation == activeGeneration && version == sourceVersion
    private data class ProgressEvent(val progress: MacProgressSample, val routeLoss: Boolean, val completed: Boolean)
}

internal class MacProgressPublicationGate {
    private var activeGeneration: Long = 0L
    private var activeSourceVersion: Long = 0L

    @Synchronized
    fun activate(generation: Long, sourceVersion: Long) {
        activeGeneration = generation
        activeSourceVersion = sourceVersion
    }

    @Synchronized
    fun isCurrent(generation: Long, sourceVersion: Long): Boolean =
        generation == activeGeneration && sourceVersion == activeSourceVersion

    fun publish(
        generation: Long,
        sourceVersion: Long,
        beforeEmit: () -> Unit,
        emitProgress: (Long) -> Unit,
        emitCompletion: (Long) -> Unit,
    ) {
        if (!isCurrent(generation, sourceVersion)) return
        beforeEmit()
        if (!isCurrent(generation, sourceVersion)) return
        emitProgress(generation)
        if (!isCurrent(generation, sourceVersion)) return
        emitCompletion(generation)
    }
}

internal data class MacProgressSample(
    val positionMillis: Long,
    val durationMillis: Long?,
)

internal class MacAudioPlayerBridge {
    private val lifetimeLock = Any()
    private var transportEnabled: Boolean = true
    private var handle: Long = 0L
    private var lifetimeIdentity: Long = 0L
    private var released: Boolean = false

    init {
        synchronized(lifetimeLock) {
            handle = createConfiguredHandleLocked()
        }
    }

    fun load(path: String): Boolean = withHandle { nativeLoad(it, path) }

    fun play(): Boolean = withHandle(::nativePlay)

    fun pause() = withHandle(::nativePause)

    fun stop() = withHandle(::nativeStop)

    fun seekTo(positionMillis: Long) = withHandle {
        nativeSeekTo(it, positionMillis)
    }

    fun currentPositionMillis(): Long =
        withHandle(::nativeCurrentPositionMillis)

    fun durationMillis(): Long = withHandle(::nativeDurationMillis)

    fun updateNowPlayingInfo(
        title: String,
        artist: String,
        album: String?,
        durationMillis: Long?,
        positionMillis: Long,
    ) = withHandle {
        nativeUpdateNowPlayingInfo(
            it, title, artist, album, durationMillis ?: 0L, positionMillis)
    }

    fun updateNowPlayingPosition(positionMillis: Long, durationMillis: Long?) =
        withHandle {
            nativeUpdateNowPlayingPosition(
                it, positionMillis, durationMillis ?: 0L)
        }

    fun updateNowPlayingPlaybackState(status: PlaybackStatus) = withHandle {
        nativeUpdateNowPlayingPlaybackState(it, status.macosPlaybackStateCode())
    }

    fun registerNowPlayingRemoteCommands() =
        withHandle(::nativeRegisterNowPlayingRemoteCommands)

    fun clearNowPlayingInfo() = withHandle(::nativeClearNowPlayingInfo)

    fun setArtwork(artworkBytes: ByteArray?) = withHandle {
        nativeSetArtwork(it, artworkBytes)
    }

    fun setTransportEnabled(enabled: Boolean) {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            transportEnabled = enabled
            nativeSetTransportEnabled(requireHandleLocked(), enabled)
        }
    }

    internal fun invokeRemotePlayForTest(): Boolean =
        withHandle(::nativeInvokeRemotePlayForTest)

    internal fun invokeRemotePauseForTest(): Boolean =
        withHandle(::nativeInvokeRemotePauseForTest)

    internal fun invokeRemoteToggleForTest(): Boolean =
        withHandle(::nativeInvokeRemoteToggleForTest)

    internal fun invokeRemoteStopForTest(): Boolean =
        withHandle(::nativeInvokeRemoteStopForTest)

    internal fun invokeRemoteSeekForTest(positionMillis: Long): Boolean =
        withHandle {
            nativeInvokeRemoteSeekForTest(it, positionMillis)
        }

    internal fun isPlayingForTest(): Boolean =
        withHandle(::nativeIsPlayingForTest)

    internal fun liveRemoteHandlerCountForTest(): Long =
        synchronized(lifetimeLock) { nativeLiveRemoteHandlerCountForTest() }

    internal fun invokeRouteDisconnectForTest(): Boolean =
        withHandle(::nativeInvokeRouteDisconnectForTest)

    internal fun setRouteExpectedActiveForTest(active: Boolean) =
        withHandle { nativeSetRouteExpectedActiveForTest(it, active) }

    internal fun routeLifecyclePartialRegistrationForTest(): Boolean =
        withHandle(::nativeRouteLifecyclePartialRegistrationForTest)

    internal fun consumeRouteDisconnected(): Boolean =
        withHandle(::nativeConsumeRouteDisconnected)

    internal fun simulateRouteSnapshotForTest(
        availableDeviceIds: LongArray,
        defaultOutputDeviceId: Long,
    ): Boolean = withHandle {
        nativeSimulateRouteSnapshotForTest(it, availableDeviceIds, defaultOutputDeviceId)
    }

    internal fun liveRouteListenerCountForTest(): Long =
        synchronized(lifetimeLock) { nativeLiveRouteListenerCountForTest() }

    internal fun nowPlayingPositionMillisForTest(): Long =
        withHandle(::nativeNowPlayingPositionMillisForTest)

    internal fun readAndUpdateProgress(
        fallbackDurationMillis: Long?
    ): MacProgressSample = withHandle { ownedHandle ->
        val positionMillis =
            nativeCurrentPositionMillis(ownedHandle).coerceAtLeast(0L)
        val durationMillis =
            nativeDurationMillis(ownedHandle).takeIf { it > 0L }
                ?: fallbackDurationMillis
        nativeUpdateNowPlayingPosition(
            ownedHandle, positionMillis, durationMillis ?: 0L)
        MacProgressSample(positionMillis, durationMillis)
    }

    fun resetPlayer() {
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            if (handle != 0L) nativeRelease(handle)
            handle = createConfiguredHandleLocked()
        }
    }

    fun releasePlayer() {
        synchronized(lifetimeLock) {
            if (released) return
            released = true
            if (handle != 0L) {
                nativeRelease(handle)
                handle = 0L
            }
        }
    }

    @Suppress("ProtectedInFinal")
    protected fun finalize() {
        synchronized(lifetimeLock) {
            if (released) return
            released = true
            if (handle != 0L) {
                nativeRelease(handle)
                handle = 0L
            }
        }
    }

    internal fun <T> withLifetimeBoundaryForTest(operation: (Long) -> T): T =
        synchronized(lifetimeLock) {
            requireHandleLocked()
            operation(lifetimeIdentity)
        }

    internal fun currentHandleIdentityForTest(): Long =
        synchronized(lifetimeLock) {
            if (handle == 0L) 0L else lifetimeIdentity
        }

    private inline fun <T> withHandle(operation: (Long) -> T): T =
        synchronized(lifetimeLock) {
            requireNotReleasedLocked()
            operation(requireHandleLocked())
        }

    private fun requireNotReleasedLocked() {
        require(!released) { "Native macOS audio bridge has been released" }
    }

    private fun requireHandleLocked(): Long {
        requireNotReleasedLocked()
        return require(handle != 0L) {
                "Native macOS audio bridge has been released"
            }
            .let { handle }
    }

    private fun createConfiguredHandleLocked(): Long =
        nativeCreate().also {
            lifetimeIdentity++
            nativeSetTransportEnabled(it, transportEnabled)
        }

    private external fun nativeCreate(): Long

    private external fun nativeLoad(handle: Long, path: String): Boolean

    private external fun nativePlay(handle: Long): Boolean

    private external fun nativePause(handle: Long)

    private external fun nativeStop(handle: Long)

    private external fun nativeSeekTo(handle: Long, positionMillis: Long)

    private external fun nativeCurrentPositionMillis(handle: Long): Long

    private external fun nativeDurationMillis(handle: Long): Long

    private external fun nativeUpdateNowPlayingInfo(
        handle: Long,
        title: String,
        artist: String,
        album: String?,
        durationMillis: Long,
        positionMillis: Long
    )

    private external fun nativeUpdateNowPlayingPosition(
        handle: Long,
        positionMillis: Long,
        durationMillis: Long
    )

    private external fun nativeUpdateNowPlayingPlaybackState(
        handle: Long,
        playbackStateCode: Int
    )

    private external fun nativeRegisterNowPlayingRemoteCommands(handle: Long)

    private external fun nativeClearNowPlayingInfo(handle: Long)

    private external fun nativeSetArtwork(
        handle: Long,
        artworkBytes: ByteArray?
    )

    private external fun nativeSetTransportEnabled(
        handle: Long,
        enabled: Boolean
    )

    private external fun nativeInvokeRemotePlayForTest(handle: Long): Boolean

    private external fun nativeInvokeRemotePauseForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteToggleForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteStopForTest(handle: Long): Boolean

    private external fun nativeInvokeRemoteSeekForTest(
        handle: Long,
        positionMillis: Long
    ): Boolean

    private external fun nativeIsPlayingForTest(handle: Long): Boolean

    private external fun nativeLiveRemoteHandlerCountForTest(): Long

    private external fun nativeInvokeRouteDisconnectForTest(handle: Long): Boolean

    private external fun nativeSetRouteExpectedActiveForTest(handle: Long, active: Boolean)

    private external fun nativeRouteLifecyclePartialRegistrationForTest(handle: Long): Boolean

    private external fun nativeConsumeRouteDisconnected(handle: Long): Boolean

    private external fun nativeSimulateRouteSnapshotForTest(
        handle: Long,
        availableDeviceIds: LongArray,
        defaultOutputDeviceId: Long,
    ): Boolean

    private external fun nativeLiveRouteListenerCountForTest(): Long

    private external fun nativeNowPlayingPositionMillisForTest(handle: Long): Long

    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            MacAudioNativeLibrary.load()
        }
    }
}

private fun PlaybackStatus.macosPlaybackStateCode(): Int =
    when (this) {
        PlaybackStatus.Playing -> 1
        PlaybackStatus.Paused -> 2
        else -> 0
    }

private object MacAudioNativeLibrary {
    private const val LIBRARY_NAME = "librhythhaus_audio.dylib"

    fun load() {
        val resourcePath = nativeResourcePath()
        val resource =
            requireNotNull(
                MacAudioNativeLibrary::class
                    .java
                    .getResourceAsStream(resourcePath)) {
                    "Native macOS audio helper resource not found: $resourcePath"
                }
        val libraryPath = Files.createTempFile("rhythhaus-audio", ".dylib")
        resource.use { input ->
            Files.copy(input, libraryPath, StandardCopyOption.REPLACE_EXISTING)
        }
        libraryPath.toFile().deleteOnExit()
        System.load(libraryPath.toAbsolutePath().toString())
    }

    private fun nativeResourcePath(): String {
        val architecture = System.getProperty("os.arch").lowercase()
        val platform =
            when {
                System.getProperty("os.name")
                    .contains("Mac", ignoreCase = true) &&
                    architecture in setOf("aarch64", "arm64") -> "macos-aarch64"
                System.getProperty("os.name")
                    .contains("Mac", ignoreCase = true) &&
                    architecture == "x86_64" -> "macos-x64"
                else ->
                    error(
                        "Native macOS audio helper is only available on macOS, current os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
            }
        return "/native/$platform/$LIBRARY_NAME"
    }
}

private fun AudioSource.jvmFile(): File =
    when (this) {
        is AudioSource.FilePath -> File(path)
        is AudioSource.Uri ->
            if (value.startsWith("file:")) File(java.net.URI(value))
            else File(value)
        is AudioSource.FileDescriptor ->
            error(
                "File descriptor audio sources are metadata-only and cannot be played")
    }
