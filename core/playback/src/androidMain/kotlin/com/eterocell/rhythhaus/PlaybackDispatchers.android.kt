package com.eterocell.rhythhaus

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

internal actual val playbackEngineDispatcher: CoroutineDispatcher =
    object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            androidControllerExecutor.execute(block::run)
        }
    }
