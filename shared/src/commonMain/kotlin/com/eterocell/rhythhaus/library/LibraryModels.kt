package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack

enum class LibraryPlatformKind {
    AndroidSafTree,
    JvmFolder,
    IosAppLocal
}

enum class LibrarySourceAccessStatus {
    Available,
    LostAccess
}

enum class ScanStatus {
    Idle,
    Scanning,
    Cancelling,
    Completed,
    Cancelled,
    Failed
}

data class LibrarySource(
    val id: String,
    val platformKind: LibraryPlatformKind,
    val displayName: String,
    val handle: String,
    val createdAtEpochMillis: Long,
    val lastScanAtEpochMillis: Long? = null,
    val accessStatus: LibrarySourceAccessStatus =
        LibrarySourceAccessStatus.Available,
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
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val artworkBytes: ByteArray? = null,
    val artworkMimeType: String? = null,
) {
    fun toPlayableTrack(): PlayableTrack =
        PlayableTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMillis = durationMillis,
            source = audioSource,
            artworkBytes = artworkBytes,
        )

    override fun equals(other: Any?): Boolean =
        other is LibraryTrack &&
            id == other.id &&
            sourceId == other.sourceId &&
            sourceLocalKey == other.sourceLocalKey &&
            audioSource == other.audioSource &&
            displayName == other.displayName &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            durationMillis == other.durationMillis &&
            sizeBytes == other.sizeBytes &&
            modifiedAtEpochMillis == other.modifiedAtEpochMillis &&
            lastSeenScanId == other.lastSeenScanId &&
            createdAtEpochMillis == other.createdAtEpochMillis &&
            updatedAtEpochMillis == other.updatedAtEpochMillis &&
            trackNumber == other.trackNumber &&
            discNumber == other.discNumber &&
            artworkMimeType == other.artworkMimeType &&
            artworkBytes.contentEquals(other.artworkBytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + sourceLocalKey.hashCode()
        result = 31 * result + audioSource.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (sizeBytes?.hashCode() ?: 0)
        result = 31 * result + (modifiedAtEpochMillis?.hashCode() ?: 0)
        result = 31 * result + (lastSeenScanId?.hashCode() ?: 0)
        result = 31 * result + createdAtEpochMillis.hashCode()
        result = 31 * result + updatedAtEpochMillis.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class TrackArtwork(
    val bytes: ByteArray,
    val mimeType: String?,
) {
    override fun equals(other: Any?): Boolean =
        other is TrackArtwork &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}

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
    val isActive: Boolean =
        session?.status in setOf(ScanStatus.Scanning, ScanStatus.Cancelling)
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
