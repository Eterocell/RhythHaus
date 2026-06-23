package com.eterocell.rhythhaus.taglib

interface TagLibReader {
    fun readPath(path: String): TagReadResult
    fun readProperties(path: String): Map<String, String>
}

expect fun createTagLibReader(): TagLibReader

object RhythHausTagLib {
    fun readPath(
        path: String,
        reader: TagLibReader = createTagLibReader(),
    ): TagReadResult = reader.readPath(path)

    fun readProperties(
        path: String,
        reader: TagLibReader = createTagLibReader(),
    ): Map<String, String> = reader.readProperties(path)
}
