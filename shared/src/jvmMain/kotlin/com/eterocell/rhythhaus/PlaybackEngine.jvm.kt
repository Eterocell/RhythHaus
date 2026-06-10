package com.eterocell.rhythhaus

import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.io.File

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine = MacOSAvFoundationPlaybackEngine()

private class MacOSAvFoundationPlaybackEngine : PlatformPlaybackEngine {
    override var listener: PlaybackEngineListener? = null
    private var player: Long = 0L
    private var durationMillis: Long? = null

    override fun load(track: PlayableTrack) {
        releasePlayer()
        listener?.onPlaybackStatus(PlaybackStatus.Loading)
        val url = ObjC.fileUrl(track.source.jvmFile())
        player = ObjC.allocInitWithUrl(className = "AVAudioPlayer", url = url)
        require(player != 0L) { "Could not create native macOS AVAudioPlayer" }
        ObjC.sendBoolean(player, "prepareToPlay")
        durationMillis = track.durationMillis ?: ObjC.sendDouble(player, "duration").secondsToMillis()
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun play() {
        val audioPlayer = requireLoadedPlayer()
        ObjC.sendBoolean(audioPlayer, "play")
        listener?.onPlaybackStatus(PlaybackStatus.Playing)
        publishProgress(audioPlayer)
    }

    override fun pause() {
        val audioPlayer = player
        if (audioPlayer != 0L) {
            ObjC.sendVoid(audioPlayer, "pause")
            publishProgress(audioPlayer)
        }
        listener?.onPlaybackStatus(PlaybackStatus.Paused)
    }

    override fun stop() {
        val audioPlayer = player
        if (audioPlayer != 0L) {
            ObjC.sendVoid(audioPlayer, "stop")
            ObjC.sendVoid(audioPlayer, "setCurrentTime:", 0.0)
        }
        listener?.onPlaybackProgress(0L, durationMillis)
        listener?.onPlaybackStatus(PlaybackStatus.Stopped)
    }

    override fun seekTo(positionMillis: Long) {
        val audioPlayer = player
        if (audioPlayer != 0L) {
            ObjC.sendVoid(audioPlayer, "setCurrentTime:", positionMillis.toDouble() / 1_000.0)
            publishProgress(audioPlayer)
        }
    }

    override fun release() {
        releasePlayer()
    }

    private fun requireLoadedPlayer(): Long = require(player != 0L) { "No native macOS player has been loaded" }.let { player }

    private fun publishProgress(audioPlayer: Long) {
        listener?.onPlaybackProgress(
            positionMillis = ObjC.sendDouble(audioPlayer, "currentTime").secondsToMillis() ?: 0L,
            durationMillis = ObjC.sendDouble(audioPlayer, "duration").secondsToMillis() ?: durationMillis,
        )
    }

    private fun releasePlayer() {
        if (player != 0L) {
            ObjC.sendVoid(player, "stop")
            ObjC.sendVoid(player, "release")
            player = 0L
        }
        durationMillis = null
    }
}

private fun AudioSource.jvmFile(): File = when (this) {
    is AudioSource.FilePath -> File(path)
    is AudioSource.Uri -> if (value.startsWith("file:")) File(java.net.URI(value)) else File(value)
}

private fun Double.secondsToMillis(): Long? = takeIf { it.isFinite() && it > 0.0 }?.times(1_000.0)?.toLong()

private object ObjC {
    private val objc: NativeLibrary = NativeLibrary.getInstance("objc")
    private val objcGetClass: Function = objc.getFunction("objc_getClass")
    private val selRegisterName: Function = objc.getFunction("sel_registerName")
    private val objcMsgSend: Function = objc.getFunction("objc_msgSend")

    init {
        NativeLibrary.getInstance("/System/Library/Frameworks/Foundation.framework/Foundation")
        NativeLibrary.getInstance("/System/Library/Frameworks/AVFoundation.framework/AVFoundation")
    }

    fun fileUrl(file: File): Long {
        val absolutePath = file.absoluteFile.path
        val pathString = nsString(absolutePath)
        return sendLong(classByName("NSURL"), "fileURLWithPath:", pathString)
    }

    fun allocInitWithUrl(className: String, url: Long): Long {
        val allocated = sendLong(classByName(className), "alloc")
        return sendLong(allocated, "initWithContentsOfURL:error:", url, 0L)
    }

    fun sendVoid(receiver: Long, selectorName: String, vararg args: Any) {
        objcMsgSend.invokeVoid(arrayOf(receiver.toPointer(), selector(selectorName), *args.toMsgSendArgs()))
    }

    fun sendBoolean(receiver: Long, selectorName: String, vararg args: Any): Boolean =
        objcMsgSend.invokeInt(arrayOf(receiver.toPointer(), selector(selectorName), *args.toMsgSendArgs())) != 0

    fun sendDouble(receiver: Long, selectorName: String, vararg args: Any): Double =
        objcMsgSend.invokeDouble(arrayOf(receiver.toPointer(), selector(selectorName), *args.toMsgSendArgs()))

    private fun sendLong(receiver: Long, selectorName: String, vararg args: Any): Long =
        Pointer.nativeValue(objcMsgSend.invokePointer(arrayOf(receiver.toPointer(), selector(selectorName), *args.toMsgSendArgs())))

    private fun nsString(value: String): Long = sendLong(classByName("NSString"), "stringWithUTF8String:", value)

    private fun classByName(name: String): Long = Pointer.nativeValue(objcGetClass.invokePointer(arrayOf(name)))

    private fun selector(name: String): Pointer = selRegisterName.invokePointer(arrayOf(name))

    private fun Long.toPointer(): Pointer = Pointer(this)

    private fun Array<out Any>.toMsgSendArgs(): Array<Any> = map { argument ->
        when (argument) {
            is Long -> argument.toPointer()
            else -> argument
        }
    }.toTypedArray()
}
