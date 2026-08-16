package com.eterocell.rhythhaus

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import platform.Foundation.NSThread

internal actual val playbackEngineDispatcher: CoroutineDispatcher =
    Dispatchers.Default

/**
 * Runs an iOS playback operation on the single owner of native playback state.
 */
internal inline fun <T> withIOSPlaybackMainThread(
    crossinline block: () -> T
): T {
    if (NSThread.isMainThread) return block()
    return runBlocking(Dispatchers.Main.immediate) { block() }
}

/** Runs a cancellable suspending iOS playback operation on the main owner. */
internal suspend fun <T> withIOSPlaybackMainContext(block: () -> T): T =
    withContext(Dispatchers.Main.immediate) { block() }
