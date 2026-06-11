package com.eterocell.rhythhaus.library

private val supportedAudioExtensions = setOf(
    "wav", "wave", "aif", "aiff", "au", "mp3", "m4a", "aac", "flac", "ogg",
)

fun isSupportedAudioName(name: String): Boolean {
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedAudioExtensions
}
