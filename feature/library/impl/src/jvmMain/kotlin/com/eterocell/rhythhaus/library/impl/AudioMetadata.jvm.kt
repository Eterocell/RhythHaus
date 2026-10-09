package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource

internal actual fun openPlatformAudioMetadataUri(
    source: AudioSource.Uri,
): AudioMetadataUriDescriptor? = null

internal actual fun readPlatformAudioMetadata(
    source: AudioSource
): AudioMetadata? = null
