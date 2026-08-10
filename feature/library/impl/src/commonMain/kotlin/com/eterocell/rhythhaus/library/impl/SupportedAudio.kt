package com.eterocell.rhythhaus.library.impl

private val supportedAudioExtensions =
    setOf(
        "wav",
        "wave",
        "aif",
        "aiff",
        "au",
        "mp3",
        "m4a",
        "aac",
        "flac",
        "ogg",
    )

/**
 * Whether the given file name has a supported audio extension.
 *
 * @param name the file name to inspect.
 */
fun isSupportedAudioName(name: String): Boolean {
    val extension =
        name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedAudioExtensions
}
