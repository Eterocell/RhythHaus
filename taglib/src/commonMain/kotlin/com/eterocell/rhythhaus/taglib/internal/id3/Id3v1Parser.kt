package com.eterocell.rhythhaus.taglib.internal.id3

import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult

internal object Id3v1Parser {
    fun parse(bytes: ByteArray): TagReadResult {
        if (bytes.size < 128) return TagReadResult.Failed("ID3v1 tag requires at least 128 bytes")
        val start = bytes.size - 128
        if (bytes.decodeToString(start, start + 3) != "TAG") {
            return TagReadResult.Failed("ID3v1 footer marker not found")
        }

        return TagReadResult.Found(
            metadata = TagMetadata(
                title = readText(bytes, start + 3, 30),
                artist = readText(bytes, start + 33, 30),
                album = readText(bytes, start + 63, 30),
                year = readText(bytes, start + 93, 4)?.toIntOrNull(),
                genre = null,
            ),
            format = com.eterocell.rhythhaus.taglib.TagFormat.ID3V1,
        )
    }

    private fun readText(bytes: ByteArray, start: Int, length: Int): String? {
        val raw = bytes.copyOfRange(start, start + length)
        val end = raw.indexOf(0).let { if (it >= 0) it else raw.size }
        return raw.decodeToString(0, end).trim().takeIf { it.isNotBlank() }
    }
}
