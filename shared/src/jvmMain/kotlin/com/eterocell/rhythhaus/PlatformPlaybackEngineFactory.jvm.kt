package com.eterocell.rhythhaus

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine =
    createJvmPlaybackEngine()
