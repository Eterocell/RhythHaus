package com.eterocell.rhythhaus.playlistbackup

object Crc32 {
    fun hex(bytes: ByteArray): String {
        var crc = 0xffffffffu
        for (byte in bytes) {
            crc = crc xor byte.toUByte().toUInt()
            repeat(8) {
                crc =
                    if ((crc and 1u) != 0u) (crc shr 1) xor 0xedb88320u
                    else crc shr 1
            }
        }
        return (crc xor 0xffffffffu).toString(16).padStart(8, '0')
    }
}
