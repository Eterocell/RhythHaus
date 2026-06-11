package com.eterocell.rhythhaus.taglib

import com.eterocell.rhythhaus.taglib.internal.TagTypeDetector
import com.eterocell.rhythhaus.taglib.internal.id3.Id3v1Parser

object RhythHausTagLib {
    fun read(bytes: ByteArray): TagReadResult = when (TagTypeDetector.detect(bytes)) {
        TagFormat.ID3V1 -> Id3v1Parser.parse(bytes)
        TagFormat.ID3V22, TagFormat.ID3V23, TagFormat.ID3V24 -> TagReadResult.Unsupported("Detected tag format is not implemented yet")
        null -> TagReadResult.Unsupported("No supported tag header found")
    }
}
