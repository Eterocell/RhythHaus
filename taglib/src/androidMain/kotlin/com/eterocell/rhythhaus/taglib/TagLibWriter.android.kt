package com.eterocell.rhythhaus.taglib

actual fun createTagLibWriter(): TagLibWriter = AndroidNativeTagLibWriter()

private class AndroidNativeTagLibWriter : TagLibWriter {
    override fun writePath(path: String, meta: WriteMeta): WriteResult =
        try {
            val bridge = NativeWriteBridge()
            val status = bridge.writePathNative(path, meta)
            when (status) {
                STATUS_SUCCESS -> WriteResult.Success
                STATUS_UNSUPPORTED ->
                    WriteResult.Unsupported(
                        bridge.lastError ?: "Native write unsupported")
                STATUS_FAILED ->
                    WriteResult.Failed(
                        bridge.lastError ?: "Native write failed")
                else -> WriteResult.Failed("Unknown write status: $status")
            }
        } catch (error: NativeTagLibUnavailableException) {
            WriteResult.Unsupported(
                error.message ?: ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE)
        } catch (error: UnsatisfiedLinkError) {
            WriteResult.Unsupported(
                "$ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE (${error.message ?: "unknown"})")
        }
}

private class NativeWriteBridge {
    var lastError: String? = null
        private set

    init {
        NativeWriteLibrary.load()
    }

    external fun writePathNative(path: String, meta: WriteMeta): Int
}

private object NativeWriteLibrary {
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        try {
            System.loadLibrary("rhythhaus_taglib")
            loaded = true
        } catch (error: UnsatisfiedLinkError) {
            throw NativeTagLibUnavailableException(
                "$ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE (${error.message ?: "unknown"})")
        }
    }
}

private const val STATUS_SUCCESS = 0
private const val STATUS_UNSUPPORTED = 1
private const val STATUS_FAILED = 2

private const val ANDROID_TAGLIB_NOT_PACKAGED_MESSAGE =
    "Native TagLib writer is not available on this Android device"
