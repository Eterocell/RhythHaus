package com.eterocell.rhythhaus

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine =
    createAndroidPlaybackEngine()
