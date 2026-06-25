package com.eterocell.rhythhaus.library

actual fun resolvePathForMetadata(path: String): String =
    if (path.startsWith("/")) path else "${appLocalMusicFolderPath()}/$path"
