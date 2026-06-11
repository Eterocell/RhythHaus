package com.eterocell.rhythhaus.taglib.internal

import com.eterocell.rhythhaus.taglib.TagFormat

internal object TagTypeDetector {
    fun detect(bytes: ByteArray): TagFormat? {
        val cursor = ByteCursor(bytes)
        val header = cursor.ascii(0, 3)
        if (header == "ID3") {
            return when (cursor.unsignedByte(3)) {
                2 -> TagFormat.ID3V22
                3 -> TagFormat.ID3V23
                4 -> TagFormat.ID3V24
                else -> null
            }
        }

        if (bytes.size >= 128) {
            val tagStart = bytes.size - 128
            if (cursor.ascii(tagStart, 3) == "TAG") return TagFormat.ID3V1
        }

        return null
    }
}
