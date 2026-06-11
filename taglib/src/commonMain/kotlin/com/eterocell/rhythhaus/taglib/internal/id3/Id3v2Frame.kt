package com.eterocell.rhythhaus.taglib.internal.id3

internal data class Id3v2Frame(
    val id: String,
    val payload: ByteArray,
)
