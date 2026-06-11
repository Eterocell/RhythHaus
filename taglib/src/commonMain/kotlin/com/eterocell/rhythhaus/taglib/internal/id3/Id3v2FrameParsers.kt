package com.eterocell.rhythhaus.taglib.internal.id3

internal object Id3v2FrameParsers {
    fun text(frame: Id3v2Frame): String? = Id3TextEncoding.decode(frame.payload)

    fun numberPair(value: String?): Pair<Int?, Int?> {
        val parts = value?.split('/') ?: return null to null
        return parts.getOrNull(0)?.toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
    }

    fun year(value: String?): Int? = value?.take(4)?.toIntOrNull()
}
