package com.eterocell.rhythhaus.taglib

import com.eterocell.rhythhaus.taglib.internal.TagTypeDetector

object RhythHausTagLib {
    fun read(bytes: ByteArray): TagReadResult = when (TagTypeDetector.detect(bytes)) {
        null -> TagReadResult.Unsupported("No supported tag header found")
        else -> TagReadResult.Unsupported("Detected tag format is not implemented yet")
    }
}
