package com.eterocell.rhythhaus.taglib

object RhythHausTagLib {
    fun read(bytes: ByteArray): TagReadResult = TagReadResult.Unsupported("No supported tag header found")
}
