package com.eterocell.rhythhaus.taglib.internal.id3

internal object Id3TextEncoding {
    fun decode(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val encoding = payload[0].toInt() and 0xFF
        val body = payload.copyOfRange(1, payload.size).trimTerminator()
        return when (encoding) {
            0, 3 -> body.decodeToString().trim().takeIf { it.isNotBlank() }
            else -> body.decodeToString().trim().takeIf { it.isNotBlank() }
        }
    }

    private fun ByteArray.trimTerminator(): ByteArray {
        var end = size
        while (end > 0 && this[end - 1].toInt() == 0) end--
        return copyOfRange(0, end)
    }
}
