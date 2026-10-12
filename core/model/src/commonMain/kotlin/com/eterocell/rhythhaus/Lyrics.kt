package com.eterocell.rhythhaus

/** One line of a local lyric document; untimed text has a null timestamp. */
public data class LyricLine(
    /** Effective playback-relative timestamp, or null for plain text. */
    public val timestampMillis: Long?,
    /** Display text for this lyric line. */
    public val text: String,
)

/**
 * Parses LRC timestamps, multiple timestamp tags, and a signed millisecond
 * offset.
 */
public fun parseLyrics(value: String): List<LyricLine> {
    val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{2,3}))?]")
    val offsetTag =
        Regex("(?m)^\\[offset:([+-]?\\d+)]\\s*$", RegexOption.IGNORE_CASE)
    val offset =
        offsetTag.find(value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val output = mutableListOf<Pair<Int, LyricLine>>()
    value.lineSequence().forEachIndexed { index, rawLine ->
        if (offsetTag.matches(rawLine.trim())) return@forEachIndexed
        val matches = timestamp.findAll(rawLine).toList()
        if (matches.isEmpty()) {
            if (rawLine.isNotBlank())
                output += index to LyricLine(null, rawLine)
            return@forEachIndexed
        }
        val text = timestamp.replace(rawLine, "").trim()
        val valid = matches.mapNotNull { match ->
            val minutes =
                match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds =
                match.groupValues[2].toLongOrNull()?.takeIf { it < 60 }
                    ?: return@mapNotNull null
            val fraction = match.groupValues[3]
            val fractionMillis =
                when (fraction.length) {
                    2 -> fraction.toLongOrNull()?.times(10L)
                    3 -> fraction.toLongOrNull()
                    else -> 0L
                } ?: return@mapNotNull null
            val timestampMillis =
                minutes * 60_000L + seconds * 1_000L + fractionMillis
            when {
                offset > 0L && timestampMillis > Long.MAX_VALUE - offset ->
                    Long.MAX_VALUE
                offset < 0L && timestampMillis < Long.MIN_VALUE - offset -> 0L
                else -> (timestampMillis + offset).coerceAtLeast(0L)
            }
        }
        if (valid.isEmpty()) {
            if (rawLine.isNotBlank())
                output += index to LyricLine(null, rawLine)
        } else if (text.isNotBlank()) {
            valid.forEach { time -> output += index to LyricLine(time, text) }
        }
    }
    return output
        .sortedWith(
            compareBy<Pair<Int, LyricLine>> {
                    it.second.timestampMillis ?: Long.MAX_VALUE
                }
                .thenBy { it.first })
        .map { it.second }
}
