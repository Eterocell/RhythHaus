package com.eterocell.rhythhaus.library

/** App-local metadata corrections. Null inherits the latest scanned field. */
public data class TrackMetadataOverride(
    /** Replacement title, or null to inherit the scanned title. */
    public val title: String? = null,
    /** Replacement artist, or null to inherit the scanned artist. */
    public val artist: String? = null,
    /** Replacement album, or null to inherit the scanned album. */
    public val album: String? = null,
    /** Replacement track number, or null to inherit the scanned number. */
    public val trackNumber: Int? = null,
    /** Replacement disc number, or null to inherit the scanned number. */
    public val discNumber: Int? = null,
)

/**
 * Scanned tags and the currently persisted corrections for an indexed track.
 */
public data class TrackMetadataEditorData(
    /** Latest tags read from the media source. */
    public val scannedTrack: LibraryTrack,
    /** Persisted app-local corrections. */
    public val overrides: TrackMetadataOverride,
)
