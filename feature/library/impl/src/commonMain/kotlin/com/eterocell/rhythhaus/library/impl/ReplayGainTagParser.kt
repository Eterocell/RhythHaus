package com.eterocell.rhythhaus.library.impl

private val replayGainNumberPattern =
    Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")

internal fun parseReplayGainDb(value: String?): Double? {
    val trimmed = value?.trim() ?: return null
    val number =
        if (trimmed.endsWith("dB", ignoreCase = true)) {
            trimmed.dropLast(2).trimEnd()
        } else {
            trimmed
        }
    return parseFiniteReplayGainNumber(number)
}

internal fun parseReplayGainPeak(value: String?): Double? =
    value?.trim()?.let(::parseFiniteReplayGainNumber)?.takeIf { it > 0.0 }

private fun parseFiniteReplayGainNumber(value: String): Double? {
    if (!replayGainNumberPattern.matches(value)) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() }
}
