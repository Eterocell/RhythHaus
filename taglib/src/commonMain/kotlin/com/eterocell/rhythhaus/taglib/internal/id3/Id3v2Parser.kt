package com.eterocell.rhythhaus.taglib.internal.id3

import com.eterocell.rhythhaus.taglib.TagFormat
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult

internal object Id3v2Parser {
    fun parse(bytes: ByteArray, format: TagFormat): TagReadResult {
        if (bytes.size < 10) return TagReadResult.Failed("ID3v2 header requires 10 bytes")
        val major = bytes[3].toInt() and 0xFF
        val tagSize = syncSafeInt(bytes, 6)
        val framesEnd = minOf(bytes.size, 10 + tagSize)
        val frames = parseFrames(bytes.copyOfRange(10, framesEnd), major)
        val text = frames.associate { it.id to Id3v2FrameParsers.text(it) }
        val track = Id3v2FrameParsers.numberPair(text["TRCK"] ?: text["TRK"])
        val disc = Id3v2FrameParsers.numberPair(text["TPOS"] ?: text["TPA"])

        return TagReadResult.Found(
            metadata = TagMetadata(
                title = text["TIT2"] ?: text["TT2"],
                artist = text["TPE1"] ?: text["TP1"],
                albumArtist = text["TPE2"] ?: text["TP2"],
                album = text["TALB"] ?: text["TAL"],
                genre = text["TCON"] ?: text["TCO"],
                year = Id3v2FrameParsers.year(text["TDRC"] ?: text["TYER"] ?: text["TYE"]),
                trackNumber = track.first,
                trackTotal = track.second,
                discNumber = disc.first,
                discTotal = disc.second,
            ),
            format = format,
        )
    }

    private fun parseFrames(bytes: ByteArray, major: Int): List<Id3v2Frame> {
        val frames = mutableListOf<Id3v2Frame>()
        val headerSize = if (major == 2) 6 else 10
        var offset = 0
        while (offset + headerSize <= bytes.size) {
            val idLength = if (major == 2) 3 else 4
            val id = bytes.decodeToString(offset, offset + idLength).trim('\u0000')
            if (id.isBlank()) break
            val size = if (major == 2) {
                ((bytes[offset + 3].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 4].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 5].toInt() and 0xFF)
            } else if (major == 4) {
                syncSafeInt(bytes, offset + 4)
            } else {
                int32(bytes, offset + 4)
            }
            val payloadStart = offset + headerSize
            val payloadEnd = payloadStart + size
            if (size <= 0 || payloadEnd > bytes.size) break
            frames += Id3v2Frame(id, bytes.copyOfRange(payloadStart, payloadEnd))
            offset = payloadEnd
        }
        return frames
    }

    private fun syncSafeInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun int32(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}
