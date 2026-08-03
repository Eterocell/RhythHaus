package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.appLocalMusicFolderPath

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine =
    createIOSPlaybackEngine(
        object : IOSRelativeFilePathResolver {
            override fun resolve(relativePath: String): String =
                "${appLocalMusicFolderPath()}/$relativePath"
        },
    )
