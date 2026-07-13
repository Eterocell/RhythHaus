package com.eterocell.rhythhaus

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val playbackEngineDispatcher: CoroutineDispatcher = Dispatchers.Default
