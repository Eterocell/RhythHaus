package com.eterocell.rhythhaus

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

internal actual val playbackEngineDispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        androidControllerExecutor.execute(block::run)
    }
}
