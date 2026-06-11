package com.eterocell.rhythhaus.taglib

interface TagLibReader {
    fun readPath(path: String): TagReadResult
}

expect fun createTagLibReader(): TagLibReader

object RhythHausTagLib {
    fun readPath(
        path: String,
        reader: TagLibReader = createTagLibReader(),
    ): TagReadResult = reader.readPath(path)
}
