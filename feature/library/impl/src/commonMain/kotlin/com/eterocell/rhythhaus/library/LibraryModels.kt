package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource

/** An audio file discovered by a platform scanner. */
data class AudioScanCandidate(
    val sourceId: String,
    val sourceLocalKey: String,
    val displayPath: String,
    val displayName: String,
    val audioSource: AudioSource,
    val metadataAudioSource: AudioSource = audioSource,
    val cleanupMetadataAudioSource: (() -> Unit)? = null,
    val sizeBytes: Long? = null,
    val modifiedAtEpochMillis: Long? = null,
)

/** Progress emitted while a source scan is running. */
data class ScanProgress(
    val session: ScanSession? = null,
    val latestItem: String? = null,
) {
    val isActive: Boolean =
        session?.status in setOf(ScanStatus.Scanning, ScanStatus.Cancelling)
}
