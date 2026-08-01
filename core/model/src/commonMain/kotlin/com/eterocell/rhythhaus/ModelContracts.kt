package com.eterocell.rhythhaus

import kotlin.math.max

/** Immutable metadata used to render and select a library track. */
public data class Track(
    /** Stable track identifier. */
    public val id: String,
    /** Display title. */
    public val title: String,
    /** Display artist. */
    public val artist: String,
    /** Display album. */
    public val album: String,
    /** Track duration in seconds. */
    public val durationSeconds: Int,
    /** Accent colors associated with the track. */
    public val accent: TrackAccent,
    /** Source used to access the track audio. */
    public val source: AudioSource,
    /** Optional track position on its disc. */
    public val trackNumber: Int? = null,
    /** Optional disc position in the album. */
    public val discNumber: Int? = null,
    /** Optional artwork bytes. */
    public val artworkBytes: ByteArray? = null,
) {
    /** Compares all metadata and artwork byte content. */
    public override fun equals(other: Any?): Boolean =
        other is Track &&
            id == other.id &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            durationSeconds == other.durationSeconds &&
            accent == other.accent &&
            source == other.source &&
            trackNumber == other.trackNumber &&
            discNumber == other.discNumber &&
            artworkBytes.contentEquals(other.artworkBytes)

    /**
     * Returns a hash code derived from all metadata and artwork byte content.
     */
    public override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + accent.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

/** Pair of color values used as a track accent. */
public data class TrackAccent(
    /** Accent start color value. */
    public val start: Long,
    /** Accent end color value. */
    public val end: Long,
)

/** Immutable library projection for shared presentation. */
public data class LibrarySnapshot(
    /** Library title. */
    public val title: String,
    /** Library subtitle. */
    public val subtitle: String,
    /** Tracks included in the projection. */
    public val tracks: List<Track>,
    /** Identifier of the currently playing track, if any. */
    public val nowPlayingTrackId: String?,
) {
    /** Track currently playing in this snapshot, if present. */
    public val nowPlaying: Track? = tracks.firstOrNull {
        it.id == nowPlayingTrackId
    }

    /** Sum of non-negative track durations in seconds. */
    public val totalDurationSeconds: Int = tracks.sumOf {
        max(0, it.durationSeconds)
    }
}

/** Immutable track details accepted by the playback system. */
public data class PlayableTrack(
    /** Stable track identifier. */
    public val id: String,
    /** Display title. */
    public val title: String,
    /** Display artist. */
    public val artist: String,
    /** Optional display album. */
    public val album: String?,
    /** Optional known duration in milliseconds. */
    public val durationMillis: Long?,
    /** Source used to access the track audio. */
    public val source: AudioSource,
    /** Optional artwork bytes. */
    public val artworkBytes: ByteArray? = null,
) {
    /** Compares all metadata and artwork byte content. */
    public override fun equals(other: Any?): Boolean =
        other is PlayableTrack &&
            id == other.id &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            durationMillis == other.durationMillis &&
            source == other.source &&
            artworkBytes.contentEquals(other.artworkBytes)

    /**
     * Returns a hash code derived from all metadata and artwork byte content.
     */
    public override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + source.hashCode()
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

/** Stable reference to audio that can be opened by a platform player. */
public sealed interface AudioSource {
    /** Persistent identity used to compare or store this source. */
    public val stableKey: String

    /** Source addressed by a filesystem path. */
    public data class FilePath(
        /** Filesystem path. */
        public val path: String,
    ) : AudioSource {
        /** Persistent identity for this source. */
        public override val stableKey: String = path
    }

    /** Source addressed by a URI string. */
    public data class Uri(
        /** URI value. */
        public val value: String,
    ) : AudioSource {
        /** Persistent identity for this source. */
        public override val stableKey: String = value
    }

    /** Source addressed by an open file descriptor. */
    public data class FileDescriptor(
        /** Platform file descriptor. */
        public val fd: Int,
        /** Display name for the descriptor. */
        public val displayName: String,
        /** Persistent identity for the descriptor. */
        public override val stableKey: String = displayName,
    ) : AudioSource
}
