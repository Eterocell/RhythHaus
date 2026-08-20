package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.AudioScanCandidate
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformSourceAccess

/** Creates the platform source access implementation for this platform. */
expect fun createPlatformSourceAccess(): PlatformSourceAccess

/**
 * Builds a scan event for one source file, filtering unsupported audio types.
 */
internal fun audioCandidateForSourceFile(
    source: LibrarySource,
    sourceLocalKey: String,
    displayPath: String,
    displayName: String,
    audioSource: AudioSource,
    metadataAudioSource: AudioSource = audioSource,
    cleanupMetadataAudioSource: (() -> Unit)? = null,
    sizeBytes: Long? = null,
    modifiedAtEpochMillis: Long? = null,
): PlatformScanEvent =
    if (isSupportedAudioName(displayName)) {
        PlatformScanEvent.AudioCandidate(
            AudioScanCandidate(
                sourceId = source.id,
                sourceLocalKey = sourceLocalKey.normalizedSourceLocalKey(),
                displayPath = displayPath,
                displayName = displayName,
                audioSource = audioSource,
                metadataAudioSource = metadataAudioSource,
                cleanupMetadataAudioSource = cleanupMetadataAudioSource,
                sizeBytes = sizeBytes,
                modifiedAtEpochMillis = modifiedAtEpochMillis,
            ),
        )
    } else {
        PlatformScanEvent.Skipped(
            sourceLocalKey = sourceLocalKey.normalizedSourceLocalKey(),
            displayPath = displayPath,
            reason = "Unsupported audio type",
            recoverable = false,
        )
    }

/** Joins the given path segments into one normalized source-local key. */
fun sourceLocalKey(vararg pathSegments: String): String =
    pathSegments.asList().sourceLocalKey()

/**
 * Joins this sequence of path segments into one normalized source-local key.
 */
fun Iterable<String>.sourceLocalKey(): String =
    joinToString("/") { segment ->
            segment.trim().trim('/').trim('\\')
        }
        .normalizedSourceLocalKey()

/** Normalizes this path into a stable source-local key. */
fun String.normalizedSourceLocalKey(): String =
    replace('\\', '/').split('/').filter { it.isNotBlank() }.joinToString("/")
