## ADDED Requirements

### Requirement: Track-list artwork uses cached thumbnails

Track-list row artwork SHALL use a cached thumbnail decode path instead of decoding full embedded artwork for each row composition.

#### Scenario: Track row thumbnail rendering
- **WHEN** a track row has embedded artwork bytes
- **THEN** the row artwork surface uses a thumbnail-sized decoded image
- **AND** repeated compositions of the same artwork bytes can reuse the cached thumbnail image
- **AND** the row visual fallback remains unchanged when artwork cannot be decoded

### Requirement: Full artwork remains available for expanded playback UI

Expanded Now Playing artwork SHALL continue to decode the full embedded artwork bytes instead of the row thumbnail.

#### Scenario: Expanded now-playing artwork
- **WHEN** the expanded Now Playing screen renders a track with artwork bytes
- **THEN** it uses the full-size artwork decode path
- **AND** compact list/bar thumbnail caching does not replace the full-size artwork entry

### Requirement: Thumbnail optimization is memory-only and schema-free

The thumbnail optimization SHALL NOT change the persisted library schema or remove original artwork bytes from track/playback models.

#### Scenario: Library model compatibility
- **WHEN** tracks are scanned, loaded from the repository, or mapped to playable tracks
- **THEN** original `artworkBytes` remain available for platform media metadata and full-screen artwork
- **AND** no SQLDelight schema migration is required for this change
