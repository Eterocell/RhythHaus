package com.eterocell.rhythhaus.taglib.internal

internal class ByteCursor(private val bytes: ByteArray) {
    fun ascii(start: Int, length: Int): String? {
        if (start < 0 || length < 0 || start + length > bytes.size) return null
        return bytes.decodeToString(start, start + length)
    }

    fun unsignedByte(index: Int): Int? = bytes.getOrNull(index)?.toInt()?.and(0xFF)

    fun slice(start: Int, length: Int): ByteArray {
        if (start < 0 || length < 0 || start + length > bytes.size) return ByteArray(0)
        return bytes.copyOfRange(start, start + length)
    }
}
