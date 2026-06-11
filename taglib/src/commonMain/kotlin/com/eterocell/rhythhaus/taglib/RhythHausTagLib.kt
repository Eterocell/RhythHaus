package com.eterocell.rhythhaus.taglib

import com.eterocell.rhythhaus.taglib.internal.TagTypeDetector
import com.eterocell.rhythhaus.taglib.internal.id3.Id3v1Parser
import com.eterocell.rhythhaus.taglib.internal.id3.Id3v2Parser

object RhythHausTagLib {
    fun read(bytes: ByteArray): TagReadResult = when (val format = TagTypeDetector.detect(bytes)) {
        TagFormat.ID3V1 -> Id3v1Parser.parse(bytes)
        TagFormat.ID3V22, TagFormat.ID3V23, TagFormat.ID3V24 -> Id3v2Parser.parse(bytes, format)
        null -> TagReadResult.Unsupported("No supported tag header found")
    }
}
