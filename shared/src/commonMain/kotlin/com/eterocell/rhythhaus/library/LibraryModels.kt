package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack

enum class LibraryPlatformKind { AndroidSafTree, JvmFolder, IosAppLocal }
enum class LibrarySourceAccessStatus { Available, LostAccess }
enum class ScanStatus { Idle, Scanning, Cancelling, Completed, Cancelled, Failed }

data class LibrarySource(
    val id: String,
    val platformKind: LibraryPlatformKind,
    val displayName: String,
    val handle: String,
    val createdAtEpochMillis: Long,
    val lastScanAtEpochMillis: Long? = null,
    val accessStatus: LibrarySourceAccessStatus = LibrarySourceAccessStatus.Available,
)

data class LibraryTrack(
    val id: String,
    val sourceId: String,
    val sourceLocalKey: String,
    val audioSource: AudioSource,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long?,
    val sizeBytes: Long?,
    val modifiedAtEpochMillis: Long?,
    val lastSeenScanId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toPlayableTrack(): PlayableTrack = PlayableTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMillis = durationMillis,
        source = audioSource,
    )
}

data class AudioScanCandidate(
    val sourceId: String,
    val sourceLocalKey: String,
    val displayPath: String,
    val displayName: String,
    val audioSource: AudioSource,
    val sizeBytes: Long? = null,
    val modifiedAtEpochMillis: Long? = null,
)

data class ScanSession(
    val id: String,
    val sourceId: String,
    val status: ScanStatus,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val foldersVisited: Int = 0,
    val filesVisited: Int = 0,
    val tracksAdded: Int = 0,
    val tracksUpdated: Int = 0,
    val filesSkipped: Int = 0,
    val terminalMessage: String? = null,
)

data class ScanProgress(
    val session: ScanSession? = null,
    val latestItem: String? = null,
) {
    val isActive: Boolean = session?.status in setOf(ScanStatus.Scanning, ScanStatus.Cancelling)
}

data class ScanError(
    val id: String,
    val scanId: String,
    val sourceLocalKey: String,
    val displayPath: String,
    val reason: String,
    val recoverable: Boolean,
    val createdAtEpochMillis: Long,
)
