package com.eterocell.rhythhaus.taglib

interface TagLibReader {
    fun readPath(path: String): TagReadResult
    fun readFd(fd: Int, displayName: String): TagReadResult = TagReadResult.Unsupported("Native TagLib reader does not support file descriptors")

    fun readProperties(path: String): Map<String, String>
}

expect fun createTagLibReader(): TagLibReader

object RhythHausTagLib {
    fun readPath(
        path: String,
        reader: TagLibReader = createTagLibReader(),
    ): TagReadResult = reader.readPath(path)

    fun readFd(
        fd: Int,
        displayName: String,
        reader: TagLibReader = createTagLibReader(),
    ): TagReadResult = reader.readFd(fd, displayName)

    fun readProperties(
        path: String,
        reader: TagLibReader = createTagLibReader(),
    ): Map<String, String> = reader.readProperties(path)
}
